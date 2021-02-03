package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Priyanka Ranjan on 22/06/2017
 */

// Reject Process of Proposal Management
public class ProposalManagementReject implements Process {

  private static Logger log = Logger.getLogger(ProposalManagementReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = Utility.getObject(User.class, vars.getUser());
    try {
      OBContext.setAdminMode();

      // Variable declaration
      final String proposalId = bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      final String clientId = bundle.getContext().getClient();
      final String orgId = proposalmgmt.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
      final String roleId = bundle.getContext().getRole();
      User user = OBDal.getInstance().get(User.class, userId);
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
      EutForwardReqMoreInfo forwardObj = proposalmgmt.getEUTForwardReqmoreinfo();
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
      String comments = bundle.getParams().get("comments").toString(),
          alertWindow = AlertWindow.ProposalManagement, alertRuleId = "", histStatus = "",
          DocStatus = proposalmgmt.getProposalappstatus(), Lang = vars.getLanguage(),
          Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pm.rejected", Lang) + " "
              + user.getName();
      boolean allowUpdate = false;
      boolean allowDelegation = false;
      boolean errorFlag = false, headerUpdate = false;
      Date CurrentDate = new Date();
      // Task No.5768
      Boolean fromPR = false;
      JSONObject resultEncum = null;
      EfinBudgetManencum encumbrance = null;
      int count = 0;
      ProposalManagementDAO propDAO = new ProposalManagementDAOImpl();

      ArrayList<String> includeRecipient = null;
      Role objCreatedRole = null;

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);
      // End Task No.5925

      Boolean allowReject = false;
      String documentType = proposalmgmt.getProposalType().equals("DR") ? "EUT_122" : "EUT_117";

      if (proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(
            proposalmgmt.getEUTForwardReqmoreinfo(), userId, roleId, documentType);
      }
      if (proposalmgmt.getEUTReqmoreinfo() != null
          || ((proposalmgmt.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (DocStatus.equals("REJ")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (proposalmgmt.getEUTNextRole() != null) {
        java.util.List<EutNextRoleLine> li = proposalmgmt.getEUTNextRole().getEutNextRoleLineList();
        for (int i = 0; i < li.size(); i++) {
          String role = li.get(i).getRole().getId();
          if (roleId.equals(role)) {
            allowUpdate = true;
          }
        }
      }
      // check current role is a delegated role or not
      if (proposalmgmt.getEUTNextRole() != null) {
        DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
        allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId, documentType);
      }

      if (!allowUpdate && !allowDelegation) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // pre validation before reject the Proposal
      if (enccontrollist.size() > 0) {
        // check budget controller approved or not , if approved do the prevalidation
        if (proposalmgmt.isEfinIsbudgetcntlapp()) {

          if (proposalmgmt.getEscmBaseproposal() != null && proposalmgmt.isEfinIsbudgetcntlapp()
              && proposalmgmt.getEscmOldproposal().getEfinEncumbrance() != null) {
            if (proposalmgmt.getEfinEncumbrance() != null
                && proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
              errorFlag = ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(
                  proposalmgmt, proposalmgmt.getEscmBaseproposal(), true, true, null);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
            } else {
              JSONObject object = ProposalManagementActionMethod
                  .getUniquecodeListforProposalVerAuto(proposalmgmt,
                      proposalmgmt.getEscmBaseproposal(), true, null);
              // funds validation.
              errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                  proposalmgmt.getEfinBudgetinitial(), "PM", false);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
            }
          } else {
            // check lines added from pr ( direct PR- proposal)
            for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
              List<EscmProposalsourceRef> proposalsrcref = proposalDAO
                  .checkLinesAddedFromPR(line.getId());
              if (proposalsrcref != null && proposalsrcref.size() > 0) {
                fromPR = true;
                break;
              }
            }
            // if lines not added from PR then do the further validation
            if (!fromPR) {

              // if both auto & manual encumbrance with proposal encumbrance type
              if (proposalmgmt.getEfinEncumbrance() != null
                  && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {

                // check encumbrance used or not based on used amount - for both manual & auto
                if (proposalmgmt.getEfinEncumbrance() != null)
                  errorFlag = ProposalManagementRejectMethods
                      .chkManualEncumbranceRejValid(proposalmgmt);
                if (errorFlag) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Encum_Used_Cannot_Rej@");
                  bundle.setResult(result);
                  return;
                }

              }
              // if proposal is added by using bid managment then do the further validation
              if (proposalmgmt.getEscmBidmgmt() != null) {
                if (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {

                  if (proposalmgmt.getProposalstatus().equals("AWD")) {

                    // check encumbrance used or not based on used amount - for proposal with bid
                    if (proposalmgmt.getEfinEncumbrance() != null)
                      errorFlag = ProposalManagementRejectMethods
                          .chkManualEncumbranceRejValid(proposalmgmt);
                    if (errorFlag) {
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@Efin_Encum_Used_Cannot_Rej@");
                      bundle.setResult(result);
                      return;
                    }
                    // check pre validation , if encumbrance lines having decrease , increase or
                    // unique
                    // code changes , then check with funds available
                    errorFlag = ProposalManagementRejectMethods
                        .getProposaltoBidDetailsRej(proposalmgmt, true, true, null);

                    // if error flag is true then throw the error - please check the line info.
                    if (errorFlag) {
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@Efin_Chk_Line_Info@");
                      bundle.setResult(result);
                      return;
                    }
                  } else {
                    // Partial award
                    OBError error1 = UnifiedProposalRejectMethods.checkSplitEncumProp(proposalmgmt,
                        true);
                    if (error1.getType().equals("error")) {
                      bundle.setResult(error1);
                      return;
                    }

                  }
                }
              }

            }
            // if proposal line is associate with PR
            else {

              if (proposalmgmt.getEfinEncumbrance() != null) {
                encumbrance = proposalmgmt.getEfinEncumbrance();

                // get the detail about Purchase requsition fromt the Proposal line-Source ref
                resultEncum = ProposalManagementActionMethod
                    .checkFullPRQtyUitlizeorNot(proposalmgmt);

                // if full qty only used then remove the encumbrance reference from the proposal and
                // change the
                // encumencumbrance stage as previous Stage

                if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && resultEncum.getBoolean("isAssociatePREncumbrance")
                    && resultEncum.has("isFullQtyUsed")
                    && !resultEncum.getBoolean("isFullQtyUsed")) {

                  // check if associate pr qty does not use full qty then while reject check funds
                  // available (case: if unique code is change from pr to proposal)
                  errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(null,
                      proposalmgmt, null);
                } else if (resultEncum.has("isAssociatePREncumbrance")
                    && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                  // check encumbrance used or not based on used amount - for both manual & auto
                  if (proposalmgmt.getEfinEncumbrance() != null)
                    errorFlag = ProposalManagementRejectMethods
                        .chkManualEncumbranceRejValid(proposalmgmt);
                  if (errorFlag) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Efin_Encum_Used_Cannot_Rej@");
                    bundle.setResult(result);
                    return;
                  }
                } else {
                  errorFlag = ProposalManagementActionMethod
                      .chkAndUpdateforProposalPRFullQty(proposalmgmt, encumbrance, true, true);
                }
                if (errorFlag) {
                  OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_ProcessFailed(Reason)@");
                  bundle.setResult(result1);
                  return;
                }
              }
            }
          }
        }
      }

      // If proposal is linked with direct PR, update awarded qty in PR lines
      if (!proposalmgmt.isNeedEvaluation()) {
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          List<EscmProposalsourceRef> srcrefList = new ArrayList<EscmProposalsourceRef>();
          srcrefList = propDAO.checkLinesAddedFromPR(line.getId());
          if (srcrefList.size() > 0) {
            for (EscmProposalsourceRef srfRef : srcrefList) {
              if (srfRef.getRequisition() != null) {
                RequisitionLine objRequisitionLine = srfRef.getRequisitionLine();
                // Awarded qty - src ref qty
                objRequisitionLine.setEscmAwardedQty(
                    objRequisitionLine.getEscmAwardedQty().subtract(srfRef.getReservedQuantity()));
                OBDal.getInstance().save(objRequisitionLine);
              }
            }
          }
        }
      }

      // End Task No.5925
      // update proposal Management header status based on reject
      if (!errorFlag) {
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
            .getNextRoleLineList(proposalmgmt.getEUTNextRole(), documentType);

        headerUpdate = ProposalManagementRejectMethods
            .updateproposalmanagementheaderforReject(proposalmgmt);

        if (headerUpdate) {
          OBDal.getInstance().save(proposalmgmt);

          // insert the Action history
          if (!StringUtils.isEmpty(proposalId)) {
            JSONObject historyData = new JSONObject();
            histStatus = "REJ";
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", proposalId);
            historyData.put("Comments", comments);
            historyData.put("Status", histStatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
            historyData.put("HeaderColumn",
                ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
            historyData.put("ActionColumn",
                ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);
          }
          // --------

          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT);
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT_DIRECT);

          // Removing forwardRMI id
          if (proposalmgmt.getEUTForwardReqmoreinfo() != null) {
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // set status as DR in forward Record
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(proposalmgmt.getEUTForwardReqmoreinfo());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(proposalmgmt.getId(),
                Constants.PROPOSAL_MANAGEMENT);

          }
          if (proposalmgmt.getEUTReqmoreinfo() != null) {
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmiId, conn);
            // set status as DR in forward Record
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(proposalmgmt.getEUTReqmoreinfo());
            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(proposalmgmt.getId(),
                Constants.PROPOSAL_MANAGEMENT);

          }

          // -------------

          // alert Process

          // get alert rule id - Task No:7618
          alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

          if (count > 0 && !StringUtils.isEmpty(proposalmgmt.getId())) {
            includeRecipient = new ArrayList<String>();

            // get creater role
            if (proposalmgmt.getCreatedBy().getADUserRolesList().size() > 0) {
              if (proposalmgmt.getRole() != null)
                objCreatedRole = proposalmgmt.getRole();
              else
                objCreatedRole = proposalmgmt.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            // get alert recipient - Task No:7618
            List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

            // check and insert alert recipient
            if (alrtRecList.size() > 0) {
              for (AlertRecipient objAlertReceipient : alrtRecList) {
                if (objAlertReceipient.getUserContact() != null)
                  includeRecipient.add(objAlertReceipient.getRole().getId());
                OBDal.getInstance().remove(objAlertReceipient);
              }
            }
            if (includeRecipient != null)
              includeRecipient.add(objCreatedRole.getId());

            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
            }

            log.debug("doc sts:" + proposalmgmt.getProposalappstatus() + "action:"
                + proposalmgmt.getEscmDocaction());
          }

          else {
            includeRecipient = new ArrayList<String>();
            objCreatedRole = proposalmgmt.getRole();

            // solve approval alerts - Task No:7618
            AlertUtility.solveAlerts(proposalmgmt.getId());

            forwardReqMoreInfoDAO.getAlertForForwardedUser(proposalmgmt.getId(), alertWindow,
                alertRuleId, objUser, clientId, Constants.REJECT,
                proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(), Lang,
                vars.getRole(), forwardObj, documentType, alertReceiversMap);

            // set alert for requester
            AlertUtility.alertInsertionRole(proposalmgmt.getId(),
                proposalmgmt.getProposalno() + "-" + proposalmgmt.getBidName(),
                proposalmgmt.getRole().getId(), proposalmgmt.getCreatedBy().getId(),
                proposalmgmt.getClient().getId(), Description, "NEW", alertWindow,
                "scm.pm.rejected", Constants.GENERIC_TEMPLATE);
            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.PROPOSAL_MANAGEMENT);
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.PROPOSAL_MANAGEMENT_DIRECT);
          }
          // Task No.5925
          if (enccontrollist.size() > 0 && proposalmgmt.isEfinIsbudgetcntlapp()) {
            if (proposalmgmt.getEscmBaseproposal() != null && proposalmgmt.isEfinIsbudgetcntlapp()
                && proposalmgmt.getEscmOldproposal().getEfinEncumbrance() != null) {
              // New version encumbrance update
              // it will insert modification in existing encumbrance when amount is differ in new
              // version
              if (proposalmgmt.getEfinEncumbrance() != null
                  && proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("A")) {
                ProposalManagementActionMethod.doRejectPOVersionMofifcationInEncumbrance(
                    proposalmgmt, proposalmgmt.getEscmBaseproposal(), false, null);
              } else {
                ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(
                    proposalmgmt, proposalmgmt.getEscmBaseproposal(), false, true, null);
              }
            } else {
              OBInterceptor.setPreventUpdateInfoChange(true);

              // if associate proposal line does not have PR
              if (!fromPR) {
                if (proposalmgmt.getProposalstatus().equals("AWD")
                    || !proposalmgmt.isNeedEvaluation()) {
                  // if proposal is manual encumbrance then reverse applied amount
                  if (proposalmgmt.getEfinEncumbrance() != null) {
                    if (proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {
                      if (proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
                        ProposalManagementRejectMethods.updateManualEncumAppAmt(proposalmgmt,
                            false);
                        proposalmgmt.setEfinIsbudgetcntlapp(false);
                        OBDal.getInstance().save(proposalmgmt);

                      }
                      // if auto the delete the new encumbrance and update the budget inquiry funds
                      // available
                      else {

                        // remove encum
                        EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();

                        ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt,
                            false);

                        // Check Encumbrance Amount is Zero Or Negative
                        if (proposalmgmt.getEfinEncumbrance() != null)
                          encumLinelist = proposalmgmt.getEfinEncumbrance()
                              .getEfinBudgetManencumlinesList();
                        if (encumLinelist.size() > 0)
                          checkEncumbranceAmountZero = UtilityDAO
                              .checkEncumbranceAmountZero(encumLinelist);

                        if (checkEncumbranceAmountZero) {
                          OBDal.getInstance().rollbackAndClose();
                          OBError result = OBErrorBuilder.buildMessage(null, "error",
                              "@ESCM_Encumamt_Neg@");
                          bundle.setResult(result);
                          return;
                        }

                        // remove encum reference in proposal lines.
                        List<EscmProposalmgmtLine> proline = proposalmgmt
                            .getEscmProposalmgmtLineList();
                        for (EscmProposalmgmtLine proLineList : proline) {
                          proLineList.setEfinBudgmanencumline(null);
                          OBDal.getInstance().save(proLineList);
                        }

                        encumLinelist = new ArrayList<EfinBudgetManencumlines>();
                        // OBDal.getInstance().flush();
                        OBDal.getInstance().remove(encum);
                        // update the budget controller flag and encumbrance ref
                        proposalmgmt.setEfinEncumbrance(null);
                        if (proposalmgmt.getEfinEncumbrance() != null) {
                          encumbrance = proposalmgmt.getEfinEncumbrance();
                          encumbrance.setBusinessPartner(null);
                          OBDal.getInstance().save(encumbrance);
                        }

                        proposalmgmt.setEfinIsbudgetcntlapp(false);
                        OBDal.getInstance().save(proposalmgmt);
                      }
                    }
                  }

                  if (proposalmgmt.getEscmBidmgmt() != null) {
                    if (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {
                      ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposalmgmt,
                          false, true, null);
                      proposalmgmt.setEfinIsbudgetcntlapp(false);
                      OBDal.getInstance().save(proposalmgmt);

                    }
                    // if associate encumbrance type is not proposal award encumbrance - then
                    // encumbrance
                    // associate with bid . so need to change the encumbrance stage as "Bid
                    // Encumbrance"
                    // else {
                    //
                    // }
                    // change the encumbrance stage as "Bid Encumbrance"
                    if (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {
                      encumbrance = proposalmgmt.getEscmBidmgmt().getEncumbrance();
                      encumbrance.setEncumStage("BE");
                      OBDal.getInstance().save(encumbrance);
                    }
                  }
                } else {
                  // Partial award
                  // if proposal is manual encumbrance then reverse applied amount
                  if (proposalmgmt.getEfinEncumbrance() != null) {
                    if (proposalmgmt.getEscmBidmgmt() != null
                        && proposalmgmt.getEscmBidmgmt().getEncumbrance() == null
                        && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {
                      if (proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
                        ProposalManagementRejectMethods.updateManualEncumAppAmt(proposalmgmt,
                            false);
                        proposalmgmt.setEfinIsbudgetcntlapp(false);
                        OBDal.getInstance().save(proposalmgmt);

                      }
                      // if auto the delete the new encumbrance and update the budget inquiry funds
                      // available
                      else {

                        // remove encum
                        EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();

                        ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt,
                            false);
                        // Check Encumbrance Amount is Zero Or Negative
                        if (proposalmgmt.getEfinEncumbrance() != null)
                          encumLinelist = proposalmgmt.getEfinEncumbrance()
                              .getEfinBudgetManencumlinesList();
                        if (encumLinelist.size() > 0)
                          checkEncumbranceAmountZero = UtilityDAO
                              .checkEncumbranceAmountZero(encumLinelist);

                        if (checkEncumbranceAmountZero) {
                          OBDal.getInstance().rollbackAndClose();
                          OBError result = OBErrorBuilder.buildMessage(null, "error",
                              "@ESCM_Encumamt_Neg@");
                          bundle.setResult(result);
                          return;
                        }
                        // remove encum reference in proposal lines.
                        List<EscmProposalmgmtLine> proline = proposalmgmt
                            .getEscmProposalmgmtLineList();
                        for (EscmProposalmgmtLine proLineList : proline) {
                          proLineList.setEfinBudgmanencumline(null);
                          OBDal.getInstance().save(proLineList);
                        }

                        encumLinelist = new ArrayList<EfinBudgetManencumlines>();
                        // OBDal.getInstance().flush();
                        OBDal.getInstance().remove(encum);

                        // update the budget controller flag and encumbrance ref
                        proposalmgmt.setEfinEncumbrance(null);
                        if (proposalmgmt.getEfinEncumbrance() != null) {
                          encumbrance = proposalmgmt.getEfinEncumbrance();
                          encumbrance.setBusinessPartner(null);
                          OBDal.getInstance().save(encumbrance);
                        }

                        proposalmgmt.setEfinIsbudgetcntlapp(false);
                        OBDal.getInstance().save(proposalmgmt);
                      }
                    } else if (proposalmgmt.getEscmBidmgmt() != null
                        && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {

                      List<String> proposalList = new ArrayList<String>();
                      proposalList.add(proposalId);

                      // If amount fully awarded, revert stage
                      boolean isFullyAwarded = UnifiedProposalActionMethod
                          .isProposalFullyAwarded(proposalList);
                      if (isFullyAwarded) {

                        OBError error1 = UnifiedProposalRejectMethods
                            .changeEncumStageRej(proposalmgmt, vars);
                        if (error1.getType().equals("error")) {
                          OBDal.getInstance().rollbackAndClose();
                          bundle.setResult(error1);
                          return;
                        }

                      } else {
                        // reactivate split bid encumbrance
                        UnifiedProposalRejectMethods.reactivateSplitBid(proposalmgmt, false,
                            proposalList);

                        OBError error1 = UnifiedProposalRejectMethods
                            .checkSplitEncumProp(proposalmgmt, false);
                        if (error1.getType().equals("error")) {
                          OBDal.getInstance().rollbackAndClose();
                          bundle.setResult(error1);
                          return;
                        }
                      }
                    }
                  }
                }
              } else {

                // if Proposal is associate with Purchase Requisition
                if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && resultEncum.getBoolean("isAssociatePREncumbrance")
                    && resultEncum.has("isFullQtyUsed")
                    && resultEncum.getBoolean("isFullQtyUsed")) {
                  encumbrance = proposalmgmt.getEfinEncumbrance();
                  encumbrance.setEncumStage("PRE");
                  encumbrance.setBusinessPartner(null);
                  OBDal.getInstance().save(encumbrance);

                  ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(proposalmgmt,
                      encumbrance, false, true);

                  // Check Encumbrance Amount is Zero Or Negative
                  if (proposalmgmt.getEfinEncumbrance() != null)
                    encumLinelist = proposalmgmt.getEfinEncumbrance()
                        .getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  proposalmgmt.setEfinIsbudgetcntlapp(false);
                  proposalmgmt.setEfinEncumbrance(null);
                  encumLinelist = new ArrayList<EfinBudgetManencumlines>();

                } else {
                  // Check Encumbrance Amount is Zero Or Negative
                  if (proposalmgmt.getEfinEncumbrance() != null)
                    encumLinelist = proposalmgmt.getEfinEncumbrance()
                        .getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  // reactive the new encumbrance changes while did split and merge
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                    ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposalmgmt,
                        false, null);
                  }
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                    ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposalmgmt,
                        false, null);
                  }

                  // if pr is skip the encumbrance
                  if (resultEncum.has("isAssociatePREncumbrance")
                      && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                    // remove encum
                    EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();

                    ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt,
                        false);
                    // Check Encumbrance Amount is Zero Or Negative
                    if (proposalmgmt.getEfinEncumbrance() != null)
                      encumLinelist = proposalmgmt.getEfinEncumbrance()
                          .getEfinBudgetManencumlinesList();
                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZero = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (checkEncumbranceAmountZero) {
                      OBDal.getInstance().rollbackAndClose();
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@ESCM_Encumamt_Neg@");
                      bundle.setResult(result);
                      return;
                    }
                    // remove encum reference in proposal lines.
                    List<EscmProposalmgmtLine> proline = proposalmgmt.getEscmProposalmgmtLineList();
                    for (EscmProposalmgmtLine proLineList : proline) {
                      proLineList.setEfinBudgmanencumline(null);
                      OBDal.getInstance().save(proLineList);
                    }

                    encumLinelist = new ArrayList<EfinBudgetManencumlines>();
                    // OBDal.getInstance().flush();
                    OBDal.getInstance().remove(encum);
                    // update the budget controller flag and encumbrance ref
                    proposalmgmt.setEfinEncumbrance(null);
                    proposalmgmt.setEfinIsbudgetcntlapp(false);
                    OBDal.getInstance().save(proposalmgmt);
                  }
                }
              }
            }
            encumLinelist = new ArrayList<EfinBudgetManencumlines>();
            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);
          }
          // Check Encumbrance Amount is Zero Or Negative
          if (proposalmgmt.getEfinEncumbrance() != null)
            encumLinelist = proposalmgmt.getEfinEncumbrance().getEfinBudgetManencumlinesList();
          if (encumLinelist.size() > 0)
            checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

          if (checkEncumbranceAmountZero) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
            bundle.setResult(result);
            return;
          }
          // End Task No.5925
        }
        encumLinelist = new ArrayList<EfinBudgetManencumlines>();
        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
    } /*
       * catch (OBException e) { log.debug("Exception in Proposal Management Reject :" + e);
       * 
       * }
       */
    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in Proposal Management Reject:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}