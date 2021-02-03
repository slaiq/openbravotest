/*
 * @author Qualian Technologies.
 */
package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProposalMgmtLetter;
import sa.elm.ob.scm.ESCMProposalMgmtLineV;
import sa.elm.ob.scm.ESCM_Proposal_CommentAttr;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalStatusHistory;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.utility.util.AttachmentProcessDao;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.copyattachments.CopyAttachmentsImpl;
import sa.elm.ob.utility.util.copyattachments.CopyAttachmentsService;

/**
 * This class is used to create new version in proposal management.
 */
public class ProposalManagementVersion extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementVersion.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean Status = false;
    boolean errorFlag = false;
    boolean headerUpdate = false;
    Boolean fromPR = false;
    HashMap<Long, Long> childParentLineNoMap = new HashMap<Long, Long>();
    final String userId = (String) bundle.getContext().getUser();
    String preferenceValue = "N";
    List<Attachment> fileList = new ArrayList<Attachment>();
    HashMap<Long, Long> parentLineMap = new HashMap<>();
    EscmProposalmgmtLine objClonePropLine = null;
    try {
      OBContext.setAdminMode();
      final String proposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
      EscmProposalMgmt proposalmgmt = Utility.getObject(EscmProposalMgmt.class, proposalId);

      ProposalManagementProcessDAO processDAO = new ProposalManagementProcessDAOImpl();
      CopyAttachmentsService copyAttachmentDAO = new CopyAttachmentsImpl();
      // final String approve = (String) bundle.getParams().get("Approve").toString();
      String approve = bundle.getParams().get("approve") != null
          ? (String) bundle.getParams().get("approve").toString()
          : "N";
      if (approve.equals("N")) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_ChackIsApprove@");
        bundle.setResult(result);
        return;
      }

      /* Create New Version Copy starts */
      // check duplicate version
      if (processDAO.checkDuplicateVersion(proposalId)) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NoDupPOVersion@");
        bundle.setResult(result);
        return;
      }
      // Copy Header
      EscmProposalMgmt objCloneProp = (EscmProposalMgmt) DalUtil.copy(proposalmgmt, false);
      proposalmgmt.setVersion(true);
      OBDal.getInstance().save(proposalmgmt);
      // OBDal.getInstance().flush();
      if (proposalmgmt.getEscmBaseproposal() == null) {
        Long revNo = proposalmgmt.getVersionNo(); // processDAO.checkBaseProposal(proposalmgmt.getId());
        objCloneProp.setVersionNo(revNo + 1);
      } else if (proposalmgmt.getEscmBaseproposal() != null) {
        Long revNo = processDAO.getRevisionNo(proposalmgmt.getEscmBaseproposal().getId());
        objCloneProp.setVersionNo(revNo + 1);
      }
      // objCloneProp.setEfinEncumbrance(null);
      objCloneProp.setEfinIsbudgetcntlapp(false);
      objCloneProp.setProposalappstatus("REA");
      objCloneProp.setEscmDocaction("SA");
      objCloneProp.setEscmOldproposal(proposalmgmt);

      if (proposalmgmt.getEscmBaseproposal() == null) {
        objCloneProp.setEscmBaseproposal(proposalmgmt);
      } else {
        objCloneProp.setEscmBaseproposal(proposalmgmt.getEscmBaseproposal());
      }

      objCloneProp.setCreationDate(new Date());
      objCloneProp.setUpdated(new Date());
      // Update session user id in Createdby field
      if (userId != null) {
        User usr = OBDal.getInstance().get(User.class, userId);
        if (usr != null) {
          objCloneProp.setCreatedBy(usr);
        }
      }
      OBDal.getInstance().save(objCloneProp);

      // Copy Proposal Lines
      for (EscmProposalmgmtLine objPropLine : proposalmgmt.getEscmProposalmgmtLineList()) {
        if (!OBContext.getOBContext().isInAdministratorMode())
          OBContext.setAdminMode();

        childParentLineNoMap.put(objPropLine.getLineNo(),
            objPropLine.getParentLineNo() == null ? null
                : objPropLine.getParentLineNo().getLineNo());

        objClonePropLine = (EscmProposalmgmtLine) DalUtil.copy(objPropLine, false);
        objClonePropLine.setEfinBudgmanencumline(null);
        objClonePropLine.setEscmProposalmgmt(objCloneProp);
        // objClonePropLine.setParentLineNo(null);
        if (objPropLine.getParentLineNo() != null)
          parentLineMap.put(objPropLine.getLineNo(), objPropLine.getParentLineNo().getLineNo());
        objClonePropLine.setEscmOldProposalline(objPropLine);
        objClonePropLine.setCreationDate(new Date());
        objClonePropLine.setUpdated(new Date());

        // Update session user id in Createdby field
        if (userId != null) {
          User usr = OBDal.getInstance().get(User.class, userId);
          if (usr != null) {
            objClonePropLine.setCreatedBy(usr);
          }
        }

        OBDal.getInstance().save(objClonePropLine);
        OBDal.getInstance().flush();

        if (objPropLine.getEscmProposalsourceRefList() != null) {
          for (EscmProposalsourceRef objPropSourceRef : objPropLine
              .getEscmProposalsourceRefList()) {
            EscmProposalsourceRef objClonePropSourceRef = (EscmProposalsourceRef) DalUtil
                .copy(objPropSourceRef, false);
            objClonePropSourceRef.setEscmProposalmgmtLine(objClonePropLine);
            objClonePropSourceRef.setCreationDate(new Date());
            objClonePropSourceRef.setUpdated(new Date());
            OBDal.getInstance().save(objClonePropSourceRef);
          }
        }
      }

      // Update parent line id
      for (Long lineNo : parentLineMap.keySet()) {
        OBQuery<EscmProposalmgmtLine> lineQry = OBDal.getInstance().createQuery(
            EscmProposalmgmtLine.class,
            " as e where e.lineNo = :lineNo and e.escmProposalmgmt.id = :proposalId ");
        lineQry.setNamedParameter("lineNo", lineNo);
        lineQry.setNamedParameter("proposalId", objCloneProp.getId());
        List<EscmProposalmgmtLine> lineList = lineQry.list();
        if (lineList.size() > 0) {
          EscmProposalmgmtLine line = lineList.get(0);

          EscmProposalmgmtLine parentLine = getParent(parentLineMap.get(lineNo),
              objCloneProp.getId());
          if (parentLine != null) {
            ESCMProposalMgmtLineV parentLineV = OBDal.getInstance().get(ESCMProposalMgmtLineV.class,
                parentLine.getId());
            line.setParentLineNo(parentLineV);
            OBDal.getInstance().flush();
          }
        }
      }

      // // Copy Open Envelop Event
      // for (EscmProposalenvEvent objPropEnvEvent : proposalmgmt.getEscmProposalenvEventList()) {
      // EscmProposalenvEvent objClonePropEnvEvent = (EscmProposalenvEvent) DalUtil
      // .copy(objPropEnvEvent, false);
      // objClonePropEnvEvent.setEscmProposalmgmt(objCloneProp);
      // objClonePropEnvEvent.setCreationDate(new Date());
      // objClonePropEnvEvent.setUpdated(new Date());
      // OBDal.getInstance().save(objClonePropEnvEvent);
      // }

      // Copy Proposal Regulation
      /*
       * for (EscmProposalRegulation objPropRegDoc : proposalmgmt.getEscmProposalRegulationList()) {
       * EscmProposalRegulation objClonePropRegDoc = (EscmProposalRegulation) DalUtil
       * .copy(objPropRegDoc, false); objClonePropRegDoc.setEscmProposalmgmt(objCloneProp);
       * objClonePropRegDoc.setCreationDate(new Date()); objClonePropRegDoc.setUpdated(new Date());
       * OBDal.getInstance().save(objClonePropRegDoc); }
       */
      // Copy Committee Comments
      for (ESCM_Proposal_CommentAttr objPropCommAttr : proposalmgmt
          .getESCMProposalCommentAttrList()) {
        ESCM_Proposal_CommentAttr objClonePropCommAttr = (ESCM_Proposal_CommentAttr) DalUtil
            .copy(objPropCommAttr, false);
        objClonePropCommAttr.setEscmProposalmgmt(objCloneProp);
        objClonePropCommAttr.setCreationDate(new Date());
        objClonePropCommAttr.setUpdated(new Date());
        OBDal.getInstance().save(objClonePropCommAttr);
      }
      // // Copy Action History
      // for (EscmProposalmgmtHist objPropActionHist : proposalmgmt.getEscmProposalmgmtHistList()) {
      // EscmProposalmgmtHist objClonePropActionHist = (EscmProposalmgmtHist) DalUtil
      // .copy(objPropActionHist, false);
      // objClonePropActionHist.setEscmProposalmgmt(objCloneProp);
      // objClonePropActionHist.setCreationDate(new Date());
      // objClonePropActionHist.setUpdated(new Date());
      // OBDal.getInstance().save(objClonePropActionHist);
      // OBDal.getInstance().flush();
      // }
      // Copy Letters
      for (ESCMProposalMgmtLetter objPropLetters : proposalmgmt.getESCMProposalMgmtLetterList()) {
        ESCMProposalMgmtLetter objClonePropLetters = (ESCMProposalMgmtLetter) DalUtil
            .copy(objPropLetters, false);
        objClonePropLetters.setEscmProposalmgmt(objCloneProp);
        objClonePropLetters.setCreationDate(new Date());
        objClonePropLetters.setUpdated(new Date());
        OBDal.getInstance().save(objClonePropLetters);
        OBDal.getInstance().flush();
      }
      // Copy Proposal Status History
      for (EscmProposalStatusHistory objPropStatHist : proposalmgmt
          .getEscmProposalStatusHistList()) {
        EscmProposalStatusHistory objClonePropStatHist = (EscmProposalStatusHistory) DalUtil
            .copy(objPropStatHist, false);
        objClonePropStatHist.setEscmProposalmgmt(objCloneProp);
        objClonePropStatHist.setCreationDate(new Date());
        objClonePropStatHist.setUpdated(new Date());
        OBDal.getInstance().save(objClonePropStatHist);
        OBDal.getInstance().flush();
      }

      // Update header records
      for (EscmProposalmgmtLine line : objCloneProp.getEscmProposalmgmtLineList()) {
        Long parentLineNo = childParentLineNoMap.get(line.getLineNo());
        if (parentLineNo != null) {
          OBQuery<EscmProposalmgmtLine> parentLine = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class,
              " lineNo =:parentlineNo and escmProposalmgmt.id=:proposalID ");
          parentLine.setNamedParameter("parentlineNo", parentLineNo);
          parentLine.setNamedParameter("proposalID", objCloneProp.getId());

          if (parentLine != null && parentLine.list().size() > 0) {
            line.setParentLineNo(
                Utility.getObject(ESCMProposalMgmtLineV.class, parentLine.list().get(0).getId()));
            OBDal.getInstance().save(line);
          }
        }
      }
      // copying the attachment from old version
      java.util.List<Preference> prefs = AttachmentProcessDao.getPreferences(
          "EUT_Attachment_Process", true, objCloneProp.getClient().getId(), null, null, null,
          Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
      for (Preference preference : prefs) {
        if (preference.getSearchKey() != null && preference.getSearchKey().equals("Y")) {
          preferenceValue = "Y";
        }
      }
      if (preferenceValue != null && preferenceValue.equals("Y")) {
        OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
            " as e where e.record=:recordId");
        file.setNamedParameter("recordId", objCloneProp.getEscmOldproposal().getId());
        fileList = file.list();
        if (fileList != null && fileList.size() > 0) {
          copyAttachmentDAO.getCopyAttachments(objCloneProp.getEscmOldproposal().getId(),
              objCloneProp.getId(), Constants.PROPOSAL_MANAGEMENT_T, objCloneProp.getVersionNo());
        }

      }

      /* Create New Version Copy ends */

      // Encumbrance Revert
      /*
       * ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl(); JSONObject resultEncum
       * = null; EfinBudgetManencum encumbrance = null; List<EfinEncControl> enccontrollist = new
       * ArrayList<EfinEncControl>(); enccontrollist =
       * ProposalManagementActionMethod.getEncControleList(proposalmgmt); if (enccontrollist.size()
       * > 0) { // check budget controller approved or not , if approved do the prevalidation if
       * (proposalmgmt.isEfinIsbudgetcntlapp()) { // check lines added from pr ( direct PR-
       * proposal) for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
       * List<EscmProposalsourceRef> proposalsrcref = proposalDAO
       * .checkLinesAddedFromPR(line.getId()); if (proposalsrcref != null && proposalsrcref.size() >
       * 0) { fromPR = true; break; } }
       * 
       * // if lines not added from PR then do the further validation if (!fromPR) { // if both auto
       * & manual encumbrance with proposal encumbrance type if (proposalmgmt.getEfinEncumbrance()
       * != null && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) { // check
       * encumbrance used or not based on used amount - for both manual & auto if
       * (proposalmgmt.getEfinEncumbrance() != null) errorFlag = ProposalManagementRejectMethods
       * .chkManualEncumbranceRejValid(proposalmgmt); if (errorFlag) {
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", "@Efin_Encum_Used_Cannot_Rej@"); bundle.setResult(result); return; } } // if
       * proposal is added by using bid managment then do the further validation if
       * (proposalmgmt.getEscmBidmgmt() != null) { if
       * (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) { // check encumbrance used or not
       * based on used amount - for proposal with bid if (proposalmgmt.getEfinEncumbrance() != null)
       * errorFlag = ProposalManagementRejectMethods .chkManualEncumbranceRejValid(proposalmgmt); if
       * (errorFlag) { OBError result = OBErrorBuilder.buildMessage(null, "error",
       * "@Efin_Encum_Used_Cannot_Rej@"); bundle.setResult(result); return; } // check pre
       * validation , if encumbrance lines having decrease , increase or // unique // code changes,
       * then check with funds available errorFlag =
       * ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposalmgmt, true, true, null);
       * // if error flag is true then throw the error - please check the line info. if (errorFlag)
       * { OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
       * bundle.setResult(result); return; } } } } // if proposal line is associate with PR else {
       * if (proposalmgmt.getEfinEncumbrance() != null) { encumbrance =
       * proposalmgmt.getEfinEncumbrance(); // get the detail about Purchase requsition fromt the
       * Proposal line-Source ref resultEncum =
       * ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(proposalmgmt); // if full qty
       * only used then remove the encumbrance reference from the proposal and // change the //
       * encumencumbrance stage as previous Stage if (resultEncum != null &&
       * resultEncum.has("isAssociatePREncumbrance") &&
       * resultEncum.getBoolean("isAssociatePREncumbrance") && resultEncum.has("isFullQtyUsed") &&
       * !resultEncum.getBoolean("isFullQtyUsed")) { // check if associate pr qty does not use full
       * qty then while reject check funds // available (case: if unique code is change from pr to
       * proposal) errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(null,
       * proposalmgmt, null); } else if (resultEncum.has("isAssociatePREncumbrance") &&
       * !resultEncum.getBoolean("isAssociatePREncumbrance")) { // check encumbrance used or not
       * based on used amount - for both manual & auto if (proposalmgmt.getEfinEncumbrance() !=
       * null) errorFlag = ProposalManagementRejectMethods
       * .chkManualEncumbranceRejValid(proposalmgmt); if (errorFlag) {
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", "@Efin_Encum_Used_Cannot_Rej@"); bundle.setResult(result); return; } } else {
       * errorFlag = ProposalManagementActionMethod .chkAndUpdateforProposalPRFullQty(proposalmgmt,
       * encumbrance, true, true); } if (errorFlag) { OBError result1 =
       * OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed(Reason)@");
       * bundle.setResult(result1); return; } } } } } // get old nextrole line user and role list
       * headerUpdate = ProposalManagementRejectMethods
       * .updateproposalmanagementheaderforReject(proposalmgmt);
       * 
       * if (headerUpdate) { OBDal.getInstance().save(proposalmgmt); // Task No.5925 if
       * (enccontrollist.size() > 0 && proposalmgmt.isEfinIsbudgetcntlapp()) { // if associate
       * proposal line does not have PR if (!fromPR) { // if proposal is manual encumbrance then
       * reverse applied amount if (proposalmgmt.getEfinEncumbrance() != null) { if
       * (proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) { if
       * (proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
       * ProposalManagementRejectMethods.updateManualEncumAppAmt(proposalmgmt, false);
       * proposalmgmt.setEfinIsbudgetcntlapp(false); OBDal.getInstance().save(proposalmgmt); } // if
       * auto the delete the new encumbrance and update the budget inquiry funds // available else {
       * // remove encum EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();
       * ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt, false); //
       * remove encum reference in proposal lines. List<EscmProposalmgmtLine> proline =
       * proposalmgmt.getEscmProposalmgmtLineList(); for (EscmProposalmgmtLine proLineList :
       * proline) { proLineList.setEfinBudgmanencumline(null);
       * OBDal.getInstance().save(proLineList); } OBDal.getInstance().flush();
       * OBDal.getInstance().remove(encum); // update the budget controller flag and encumbrance ref
       * proposalmgmt.setEfinEncumbrance(null); proposalmgmt.setEfinIsbudgetcntlapp(false);
       * OBDal.getInstance().save(proposalmgmt); } } // if associate encumbrance type is not
       * proposal award encumbrance - then // encumbrance // associate with bid . so need to change
       * the encumbrance stage as "Bid Encumbrance" // else { // // } // change the encumbrance
       * stage as "Bid Encumbrance" if (proposalmgmt.getEfinEncumbrance() != null) { encumbrance =
       * proposalmgmt.getEfinEncumbrance(); encumbrance.setEncumStage("BE");
       * encumbrance.setBusinessPartner(null); OBDal.getInstance().save(encumbrance); } } if
       * (proposalmgmt.getEscmBidmgmt() != null) { if
       * (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {
       * ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposalmgmt, false, true,
       * null); proposalmgmt.setEfinIsbudgetcntlapp(false); OBDal.getInstance().save(proposalmgmt);
       * } } } else { // if Proposal is associate with Purchase Requisition if (resultEncum != null
       * && resultEncum.has("isAssociatePREncumbrance") &&
       * resultEncum.getBoolean("isAssociatePREncumbrance") && resultEncum.has("isFullQtyUsed") &&
       * resultEncum.getBoolean("isFullQtyUsed")) { encumbrance = proposalmgmt.getEfinEncumbrance();
       * encumbrance.setEncumStage("PRE"); encumbrance.setBusinessPartner(null);
       * OBDal.getInstance().save(encumbrance);
       * ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(proposalmgmt, encumbrance,
       * false, true); proposalmgmt.setEfinIsbudgetcntlapp(false);
       * proposalmgmt.setEfinEncumbrance(null); } else { // reactive the new encumbrance changes
       * while did split and merge if (resultEncum.has("type") &&
       * resultEncum.getString("type").equals("SPLIT")) {
       * ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposalmgmt, false, null);
       * } if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
       * ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposalmgmt, false, null);
       * } // if pr is skip the encumbrance if (resultEncum.has("isAssociatePREncumbrance") &&
       * !resultEncum.getBoolean("isAssociatePREncumbrance")) { // remove encum EfinBudgetManencum
       * encum = proposalmgmt.getEfinEncumbrance();
       * ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt, false); //
       * remove encum reference in proposal lines. List<EscmProposalmgmtLine> proline =
       * proposalmgmt.getEscmProposalmgmtLineList(); for (EscmProposalmgmtLine proLineList :
       * proline) { proLineList.setEfinBudgmanencumline(null);
       * OBDal.getInstance().save(proLineList); } OBDal.getInstance().flush();
       * OBDal.getInstance().remove(encum); // update the budget controller flag and encumbrance ref
       * proposalmgmt.setEfinEncumbrance(null); proposalmgmt.setEfinIsbudgetcntlapp(false);
       * OBDal.getInstance().save(proposalmgmt); } } } } }
       */
      // Encumbrance Revert Ends
      // headerUpdate = ProposalManagementRejectMethods
      // .updateproposalmanagementheaderforReject(proposalmgmt);
      /* Proposal Backup */
      Status = ProposalManagementActionMethod.proposalmgmtbackup(vars, proposalmgmt);
      OBDal.getInstance().flush();
      if (Status) {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      log.error("Exception in ProposalManagementVersion:", e);
      e.printStackTrace();
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param lineNo
   * @param proposalId
   * @return Proposal Line parent proposal for that line
   */

  public static EscmProposalmgmtLine getParent(Long lineNo, String proposalId) {
    EscmProposalmgmtLine proposalLine = null;
    try {
      OBQuery<EscmProposalmgmtLine> parentLine = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          " as e where e.lineNo = :lineNo and e.escmProposalmgmt.id = :proposalId ");
      parentLine.setNamedParameter("lineNo", lineNo);
      parentLine.setNamedParameter("proposalId", proposalId);

      List<EscmProposalmgmtLine> parentLinesList = parentLine.list();
      if (parentLinesList != null && parentLinesList.size() > 0) {
        proposalLine = parentLinesList.get(0);
      }
    } catch (Exception e) {
      log.error("Exception in getParent", e);
    }
    return proposalLine;
  }

}
