package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtHist;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementRejectMethods;
import sa.elm.ob.scm.ad_process.ProposalManagement.UnifiedProposalActionMethod;
import sa.elm.ob.scm.ad_process.ProposalManagement.UnifiedProposalRejectMethods;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 * 
 */

public class ProposalManagementRevoke implements Process {

  /**
   * This class is used to revoke the proposal management.
   */
  private static final Logger log = Logger.getLogger(ProposalManagementRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    final String propId = bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
    EscmProposalMgmt prop = Utility.getObject(EscmProposalMgmt.class, propId);
    final String clientId = bundle.getContext().getClient();
    final String orgId = prop.getOrganization().getId();
    final String userId = bundle.getContext().getUser();
    final String roleId = bundle.getContext().getRole();
    String comments = bundle.getParams().get("comments").toString();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
    Boolean isAlreadyRevoked = true;
    List<EscmProposalsourceRef> srcrefList = new ArrayList<EscmProposalsourceRef>();
    BigDecimal soucrRef_Qty = BigDecimal.ZERO;
    BigDecimal total_award_qty = BigDecimal.ZERO;
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    EscmProposalMgmt headerId = null;
    String appstatus = "";// , alertWindow = AlertWindow.ProposalManagement;
    // String alertRuleId = "";
    // ArrayList<String> includeRecipient = new ArrayList<String>();
    // HttpServletRequest request = RequestContext.get().getRequest();
    // VariablesSecureApp vars = new VariablesSecureApp(request);
    // String Lang = vars.getLanguage();
    // String Description = "", lastWaitingRoleId = "";

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();
      List<EscmProposalmgmtHist> history = proposalDAO.getProposalHist(propId);
      if (history.size() > 0) {
        EscmProposalmgmtHist apphistory = history.get(0);
        if (apphistory.getRequestreqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      EscmProposalMgmt headerCheck = OBDal.getInstance().get(EscmProposalMgmt.class, propId);

      if (headerCheck.getProposalstatus().equals("CD")
          || headerCheck.getProposalstatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      Boolean fromPR = false;
      boolean errorFlag = false;
      EfinBudgetManencum encumbrance = null;
      JSONObject resultEncum = null;

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(headerCheck);
      // End Task No.5925

      if (headerCheck.getProposalappstatus().equals("REJ")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // To check whether record is already revoked
      isAlreadyRevoked = ProposalManagementRejectMethods.isAlreadyRevoked(propId, userId, roleId);
      if (isAlreadyRevoked) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // pre validation before reject the Proposal
      if (enccontrollist.size() > 0) {
        // check budget controller approved or not , if approved do the prevalidation
        if (headerCheck.isEfinIsbudgetcntlapp()) {
          if (headerCheck.getEscmBaseproposal() != null && headerCheck.isEfinIsbudgetcntlapp()
              && headerCheck.getEscmOldproposal().getEfinEncumbrance() != null) {
            if (headerCheck.getEfinEncumbrance() != null
                && headerCheck.getEfinEncumbrance().getEncumMethod().equals("M")) {
              errorFlag = ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(
                  headerCheck, headerCheck.getEscmBaseproposal(), true, true, null);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
            } else {
              JSONObject object = ProposalManagementActionMethod
                  .getUniquecodeListforProposalVerAuto(headerCheck,
                      headerCheck.getEscmBaseproposal(), true, null);
              // funds validation.
              errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                  headerCheck.getEfinBudgetinitial(), "PM", false);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
            }
          } else {
            // check lines added from pr ( direct PR- proposal)
            for (EscmProposalmgmtLine line : headerCheck.getEscmProposalmgmtLineList()) {
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
              if (headerCheck.getEfinEncumbrance() != null
                  && headerCheck.getEfinEncumbrance().getEncumType().equals("PAE")) {

                // check encumbrance used or not based on used amount - for both manual & auto
                errorFlag = ProposalManagementRejectMethods
                    .chkManualEncumbranceRejValid(headerCheck);
                if (errorFlag) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Encum_Used_Cannot_Rej@");
                  bundle.setResult(result);
                  return;
                }

              }
              // if proposal is added by using bid managment then do the further validation
              if (headerCheck.getEscmBidmgmt() != null) {
                if (headerCheck.getEscmBidmgmt().getEncumbrance() != null) {

                  if (headerCheck.getProposalstatus().equals("AWD")) {
                    // check encumbrance used or not based on used amount - for proposal with bid
                    errorFlag = ProposalManagementRejectMethods
                        .chkManualEncumbranceRejValid(headerCheck);
                    if (errorFlag) {
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@Efin_Encum_Used_Cannot_Rej@");
                      bundle.setResult(result);
                      return;
                    }

                    // Check Encumbrance Amount is Zero Or Negative
                    if (headerCheck.getEfinEncumbrance() != null)
                      encumLinelist = headerCheck.getEfinEncumbrance()
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
                    // check pre validation , if encumbrance lines having decrease , increase or
                    // unique
                    // code changes , then check with funds available
                    errorFlag = ProposalManagementRejectMethods
                        .getProposaltoBidDetailsRej(headerCheck, true, true, null);

                    // if error flag is true then throw the error - please check the line info.
                    if (errorFlag) {
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@Efin_Chk_Line_Info@");
                      bundle.setResult(result);
                      return;
                    }
                  } else {
                    // Partial award
                    if (headerCheck.getEscmBidmgmt() != null
                        && headerCheck.getEscmBidmgmt().getEncumbrance() != null) {
                      OBError error1 = UnifiedProposalRejectMethods.checkSplitEncumProp(headerCheck,
                          true);
                      if (error1.getType().equals("error")) {
                        bundle.setResult(error1);
                        return;
                      }
                    }
                  }
                }
              }
            }
            // if proposal line is associate with PR
            else {

              if (headerCheck.getEfinEncumbrance() != null) {
                encumbrance = headerCheck.getEfinEncumbrance();

                // get the detail about Purchase requsition fromt the Proposal line-Source ref
                resultEncum = ProposalManagementActionMethod
                    .checkFullPRQtyUitlizeorNot(headerCheck);

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
                      headerCheck, null);
                } else if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                  // check encumbrance used or not based on used amount - for both manual & auto
                  errorFlag = ProposalManagementRejectMethods
                      .chkManualEncumbranceRejValid(headerCheck);
                  if (errorFlag) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Efin_Encum_Used_Cannot_Rej@");
                    bundle.setResult(result);
                    return;
                  }
                } else {
                  errorFlag = ProposalManagementActionMethod
                      .chkAndUpdateforProposalPRFullQty(headerCheck, encumbrance, true, true);
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

      // Task No.5925
      if (enccontrollist.size() > 0 && headerCheck.isEfinIsbudgetcntlapp()) {
        if (headerCheck.getEscmBaseproposal() != null && headerCheck.isEfinIsbudgetcntlapp()
            && headerCheck.getEscmOldproposal().getEfinEncumbrance() != null) {
          // New version encumbrance update
          // it will insert modification in existing encumbrance when amount is differ in new
          // version
          if (headerCheck.getEfinEncumbrance() != null
              && headerCheck.getEfinEncumbrance().getEncumMethod().equals("A")) {
            ProposalManagementActionMethod.doRejectPOVersionMofifcationInEncumbrance(headerCheck,
                headerCheck.getEscmBaseproposal(), false, null);
          } else {
            ProposalManagementActionMethod.chkNewVersionManualEncumbranceValidation(headerCheck,
                headerCheck.getEscmBaseproposal(), false, true, null);
          }
        } else {
          OBInterceptor.setPreventUpdateInfoChange(true);

          // if associate proposal line does not have PR
          if (!fromPR) {

            if (headerCheck.getProposalstatus().equals("AWD") || !headerCheck.isNeedEvaluation()) {
              // if proposal is manual encumbrance then reverse applied amount
              if (headerCheck.getEfinEncumbrance() != null) {
                if (headerCheck.getEfinEncumbrance().getEncumType().equals("PAE")) {
                  if (headerCheck.getEfinEncumbrance().getEncumMethod().equals("M")) {
                    ProposalManagementRejectMethods.updateManualEncumAppAmt(headerCheck, false);
                    headerCheck.setEfinIsbudgetcntlapp(false);
                    OBDal.getInstance().save(headerCheck);

                  }
                  // if auto the delete the new encumbrance and update the budget inquiry funds
                  // available
                  else {

                    // remove encum
                    EfinBudgetManencum encum = headerCheck.getEfinEncumbrance();

                    ProposalManagementRejectMethods.updateAutoEncumbrancechanges(headerCheck,
                        false);

                    // Check Encumbrance Amount is Zero Or Negative
                    if (headerCheck.getEfinEncumbrance() != null)
                      encumLinelist = headerCheck.getEfinEncumbrance()
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
                    List<EscmProposalmgmtLine> proline = headerCheck.getEscmProposalmgmtLineList();
                    for (EscmProposalmgmtLine proLineList : proline) {
                      proLineList.setEfinBudgmanencumline(null);
                      OBDal.getInstance().save(proLineList);
                    }

                    encumLinelist = new ArrayList<EfinBudgetManencumlines>();
                    // OBDal.getInstance().flush();
                    OBDal.getInstance().remove(encum);
                    // update the budget controller flag and encumbrance ref
                    headerCheck.setEfinEncumbrance(null);
                    headerCheck.setEfinIsbudgetcntlapp(false);
                    OBDal.getInstance().save(headerCheck);
                  }
                }
                // if associate encumbrance type is not proposal award encumbrance - then
                // encumbrance
                // associate with bid . so need to change the encumbrance stage as "Bid Encumbrance"
                else {
                  if (headerCheck.getEscmBidmgmt() != null) {
                    if (headerCheck.getEscmBidmgmt().getEncumbrance() != null) {
                      ProposalManagementRejectMethods.getProposaltoBidDetailsRej(headerCheck, false,
                          true, null);
                      headerCheck.setEfinIsbudgetcntlapp(false);
                      OBDal.getInstance().save(headerCheck);
                    }
                    if (headerCheck.getEscmBidmgmt().getEncumbrance() != null) {
                      OBInterceptor.setPreventUpdateInfoChange(true);
                      encumbrance = headerCheck.getEscmBidmgmt().getEncumbrance();
                      encumbrance.setEncumStage("BE");
                      OBDal.getInstance().save(encumbrance);
                      OBDal.getInstance().flush();
                      OBInterceptor.setPreventUpdateInfoChange(false);

                    }
                  }
                }
                // change the encumbrance stage as "Bid Encumbrance"
                if (headerCheck.getEfinEncumbrance() != null
                    && (headerCheck.getEscmBidmgmt() != null
                        && headerCheck.getEscmBidmgmt().getEncumbrance() != null)) {
                  OBInterceptor.setPreventUpdateInfoChange(true);
                  encumbrance = headerCheck.getEfinEncumbrance();
                  encumbrance.setEncumStage("BE");
                  encumbrance.setBusinessPartner(null);
                  OBDal.getInstance().save(encumbrance);
                  OBDal.getInstance().flush();
                  OBInterceptor.setPreventUpdateInfoChange(false);
                }
              }
            } else {
              // Partial award
              // if proposal is manual encumbrance then reverse applied amount
              if (headerCheck.getEfinEncumbrance() != null) {
                if (headerCheck.getEscmBidmgmt() != null
                    && headerCheck.getEscmBidmgmt().getEncumbrance() == null
                    && headerCheck.getEfinEncumbrance().getEncumType().equals("PAE")) {
                  if (headerCheck.getEfinEncumbrance().getEncumMethod().equals("M")) {
                    ProposalManagementRejectMethods.updateManualEncumAppAmt(headerCheck, false);
                    headerCheck.setEfinIsbudgetcntlapp(false);
                    OBDal.getInstance().save(headerCheck);

                  }
                  // if auto the delete the new encumbrance and update the budget inquiry funds
                  // available
                  else {

                    // remove encum
                    EfinBudgetManencum encum = headerCheck.getEfinEncumbrance();

                    ProposalManagementRejectMethods.updateAutoEncumbrancechanges(headerCheck,
                        false);

                    // Check Encumbrance Amount is Zero Or Negative
                    if (headerCheck.getEfinEncumbrance() != null)
                      encumLinelist = headerCheck.getEfinEncumbrance()
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
                    List<EscmProposalmgmtLine> proline = headerCheck.getEscmProposalmgmtLineList();
                    for (EscmProposalmgmtLine proLineList : proline) {
                      proLineList.setEfinBudgmanencumline(null);
                      OBDal.getInstance().save(proLineList);
                    }

                    encumLinelist = new ArrayList<EfinBudgetManencumlines>();
                    // OBDal.getInstance().flush();
                    OBDal.getInstance().remove(encum);

                    // update the budget controller flag and encumbrance ref
                    headerCheck.setEfinEncumbrance(null);
                    if (headerCheck.getEfinEncumbrance() != null) {
                      encumbrance = headerCheck.getEfinEncumbrance();
                      encumbrance.setBusinessPartner(null);
                      OBDal.getInstance().save(encumbrance);
                    }

                    headerCheck.setEfinIsbudgetcntlapp(false);
                    OBDal.getInstance().save(headerCheck);
                  }
                } else if (headerCheck.getEscmBidmgmt() != null
                    && headerCheck.getEscmBidmgmt().getEncumbrance() != null) {

                  List<String> proposalList = new ArrayList<String>();
                  proposalList.add(headerCheck.getId());

                  // If amount fully awarded, revert stage
                  boolean isFullyAwarded = UnifiedProposalActionMethod
                      .isProposalFullyAwarded(proposalList);
                  if (isFullyAwarded) {

                    OBError error1 = UnifiedProposalRejectMethods.changeEncumStageRej(headerCheck,
                        vars);
                    if (error1.getType().equals("error")) {
                      OBDal.getInstance().rollbackAndClose();
                      bundle.setResult(error1);
                      return;
                    }

                  } else {
                    // reactivate split bid encumbrance
                    UnifiedProposalRejectMethods.reactivateSplitBid(headerCheck, false,
                        proposalList);

                    OBError error1 = UnifiedProposalRejectMethods.checkSplitEncumProp(headerCheck,
                        false);
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
                && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
              encumbrance = headerCheck.getEfinEncumbrance();
              encumbrance.setEncumStage("PRE");
              encumbrance.setBusinessPartner(null);
              OBDal.getInstance().save(encumbrance);

              ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(headerCheck,
                  encumbrance, false, true);

              // Check Encumbrance Amount is Zero Or Negative
              if (headerCheck.getEfinEncumbrance() != null)
                encumLinelist = headerCheck.getEfinEncumbrance().getEfinBudgetManencumlinesList();
              if (encumLinelist.size() > 0)
                checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

              if (checkEncumbranceAmountZero) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
                bundle.setResult(result);
                return;
              }

              headerCheck.setEfinIsbudgetcntlapp(false);
              headerCheck.setEfinEncumbrance(null);
              encumLinelist = new ArrayList<EfinBudgetManencumlines>();

            } else {

              // reactive the new encumbrance changes while did split and merge
              if (resultEncum != null && resultEncum.has("type")
                  && resultEncum.getString("type").equals("SPLIT")) {
                ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, headerCheck, false,
                    null);
              }
              if (resultEncum != null && resultEncum.has("type")
                  && resultEncum.getString("type").equals("MERGE")) {
                ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, headerCheck, false,
                    null);
              }

              // if pr is skip the encumbrance
              if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                // remove encum
                EfinBudgetManencum encum = headerCheck.getEfinEncumbrance();

                ProposalManagementRejectMethods.updateAutoEncumbrancechanges(headerCheck, false);

                // remove encum reference in proposal lines.
                List<EscmProposalmgmtLine> proline = headerCheck.getEscmProposalmgmtLineList();
                for (EscmProposalmgmtLine proLineList : proline) {
                  proLineList.setEfinBudgmanencumline(null);
                  OBDal.getInstance().save(proLineList);
                }

                // OBDal.getInstance().flush();
                OBDal.getInstance().remove(encum);
                // update the budget controller flag and encumbrance ref
                headerCheck.setEfinEncumbrance(null);
                headerCheck.setEfinIsbudgetcntlapp(false);
                OBDal.getInstance().save(headerCheck);
              }
            }
          }
        }
        OBDal.getInstance().flush();
        OBInterceptor.setPreventUpdateInfoChange(false);
      }

      if (!prop.isNeedEvaluation()) {
        for (EscmProposalmgmtLine line : prop.getEscmProposalmgmtLineList()) {
          srcrefList = proposalDAO.checkLinesAddedFromPR(line.getId());
          if (srcrefList.size() > 0) {
            for (EscmProposalsourceRef srfRef : srcrefList) {
              if (srfRef.getRequisition() != null) {
                RequisitionLine objRequisition = srfRef.getRequisitionLine();
                Requisition obj_requisition = objRequisition.getRequisition();
                String str_docno = obj_requisition.getDocumentNo();
                soucrRef_Qty = srfRef.getReservedQuantity();
                // total_award_qty = soucrRef_Qty.subtract(objRequisition.getEscmAwardedQty());
                total_award_qty = objRequisition.getEscmAwardedQty().subtract(soucrRef_Qty);
                objRequisition.setEscmAwardedQty(total_award_qty);
                OBDal.getInstance().save(objRequisition);
              }

            }
          }
        }
      }
      int count = 0;
      if (!errorFlag) {
        EscmProposalMgmt header = OBDal.getInstance().get(EscmProposalMgmt.class, propId);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        if (header.isNeedEvaluation())
          if (header.getAwardamount().compareTo(BigDecimal.ZERO) != 0) {
            header.setProposalstatus("PAWD");
          } else {
            header.setProposalstatus("AWD");
          }
        else
          header.setProposalstatus("DR");
        if (header.getEscmBaseproposal() == null)
          header.setProposalappstatus("INC");
        else
          header.setProposalappstatus("REA");
        header.setEscmDocaction("SA");
        header.setEUTNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PROPOSAL_MANAGEMENT);
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PROPOSAL_MANAGEMENT_DIRECT);

        headerId = header;

        if (!StringUtils.isEmpty(headerId.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", headerId.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
          historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
          historyData.put("ActionColumn",
              ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }

        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);

        // --------

        // Removing forwardRMI id
        if (header.getEUTForwardReqmoreinfo() != null) {
          // Removing the Role Access given to the forwarded user
          // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
          // set status as DR in forward Record
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
          // Removing Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
              Constants.PROPOSAL_MANAGEMENT);

        }
        if (header.getEUTReqmoreinfo() != null) {
          // set status as DR in forward Record
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
          // access remove
          // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmiId, conn);
          // Remove Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
              Constants.PROPOSAL_MANAGEMENT);

        }

        // -------------

        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {

          /*
           * Role objCreatedRole = null; if (header.getCreatedBy().getADUserRolesList().size() > 0)
           * { objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole(); }
           */
          // alertWindow = AlertWindow.ProposalManagement;

          // remove approval alert

          // Task No:7618
          AlertUtility.solveAlerts(propId);

          // Check Encumbrance Amount is Zero Or Negative
          if (header.getEfinEncumbrance() != null)
            encumLinelist = header.getEfinEncumbrance().getEfinBudgetManencumlinesList();
          if (encumLinelist.size() > 0)
            checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

          if (checkEncumbranceAmountZero) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
            bundle.setResult(result);
            return;
          }

          // check and insert alert recipient
          /*
           * OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
           * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'"); if
           * (receipientQuery.list().size() > 0) { for (AlertRecipient objAlertReceipient :
           * receipientQuery.list()) { includeRecipient.add(objAlertReceipient.getRole().getId());
           * OBDal.getInstance().remove(objAlertReceipient); } } if (objCreatedRole != null)
           * includeRecipient.add(objCreatedRole.getId());
           */
          // avoid duplicate recipient
          /*
           * HashSet<String> incluedSet = new HashSet<String>(includeRecipient); Iterator<String>
           * iterator = incluedSet.iterator(); while (iterator.hasNext()) {
           * AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow); }
           * NextRoleByRuleVO nextApproval = NextRoleByRule.getMIRRevokeRequesterNextRole(OBDal
           * .getInstance().getConnection(), clientId, orgId, roleId, userId,
           * Resource.Bid_Management, header.getRole().getId()); EutNextRole nextRole = null;
           * nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
           * 
           * // set alert for next approver Description =
           * sa.elm.ob.scm.properties.Resource.getProperty("scm.BidMgmt.revoked", Lang) + " " +
           * header.getCreatedBy().getName();
           * 
           * // set revoke alert to last waiting role
           * AlertUtility.alertInsertionRole(header.getId(), header.getBidno(), lastWaitingRoleId,
           * "", header.getClient().getId(), Description, "NEW", alertWindow); for (EutNextRoleLine
           * objNextRoleLine : nextRole.getEutNextRoleLineList()) { AlertUtility
           * .alertInsertionRole(header.getId(), header.getBidno(), objNextRoleLine.getRole()
           * .getId(), "", header.getClient().getId(), Description, "NEW", alertWindow);
           * 
           * obError.setType("Success"); obError.setTitle("Success");
           * obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke")); }
           */
          encumLinelist = new ArrayList<EfinBudgetManencumlines>();
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("ESCM_PropMang_RevokeSuccess"));

      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("ESCM_PropMang_RevokeNotSuccess"));
      }
      bundle.setResult(obError);
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke Proposal Management Revoke :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
