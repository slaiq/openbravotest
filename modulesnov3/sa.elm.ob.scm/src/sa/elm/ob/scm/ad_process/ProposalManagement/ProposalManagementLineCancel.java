/*
 * @author Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;

/**
 * This class is used to cancel the proposal management in line level.
 */

public class ProposalManagementLineCancel extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementLineCancel.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean Status = false, errorFlag = false;
    BigDecimal soucrRef_Qty = BigDecimal.ZERO;
    BigDecimal total_award_qty = BigDecimal.ZERO;
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    ProposalManagementProcessDAO proposalPrDAO = new ProposalManagementProcessDAOImpl();
    ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
    Boolean fromPR = false;
    EfinBudgetManencum encumbrance = null;
    JSONObject resultEncum = null;
    try {
      OBContext.setAdminMode();
      final String proposalLineId = (String) bundle.getParams().get("Escm_Proposalmgmt_Line_ID")
          .toString();
      final String tabId = (String) bundle.getParams().get("tabId").toString();
      EscmProposalmgmtLine proposalmgmtline = OBDal.getInstance().get(EscmProposalmgmtLine.class,
          proposalLineId);
      EscmProposalMgmt proposalmgmt = proposalmgmtline.getEscmProposalmgmt();
      EscmProposalMgmt baseProposalObj = proposalmgmt.getEscmBaseproposal();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);

      // Validate if PO created when cancel
      List<Order> pmOrder = proposalPrDAO.checkPOCreated(proposalmgmt.getId());
      if (pmOrder != null && pmOrder.size() > 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_POCreated@");
        bundle.setResult(result);
        return;
      }

      // checking funds validation
      if (enccontrollist.size() > 0 && proposalmgmt.isEfinIsbudgetcntlapp()
          && baseProposalObj == null) {
        // check lines added from pr ( direct PR- proposal)
        List<EscmProposalsourceRef> proposalsrcref = proposalDAO
            .checkLinesAddedFromPR(proposalLineId);
        if (proposalsrcref != null && proposalsrcref.size() > 0) {
          fromPR = true;
        }

        // if both auto & manual encumbrance with proposal encumbrance type
        if ((proposalmgmt.getEfinEncumbrance() != null
            && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE"))
            || (proposalmgmt.getEscmBidmgmt() != null
                && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null)
            || (proposalmgmt.getEfinEncumbrance() != null)) {
          // check encumbrance used or not based on used amount - for both manual & auto
          errorFlag = proposalPrDAO.chkEncumbranceLineCancelValid(proposalmgmtline);
          if (errorFlag) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Encum_Used_Cannot_Canl@");
            bundle.setResult(result);
            return;
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
              // check pre validation , if encumbrance lines having decrease , increase or unique
              // code changes , then check with funds available
              errorFlag = ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposalmgmt,
                  true, true, null);
              // if error flag is true then throw the error - please check the line info.
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
        // if proposal line is associate with PR
        else {
          if (proposalmgmt.getEfinEncumbrance() != null) {
            encumbrance = proposalmgmt.getEfinEncumbrance();
            // get the detail about Purchase requsition fromt the Proposal line-Source ref
            resultEncum = ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(proposalmgmt);
            // if full qty only used then remove the encumbrance reference from the proposal and
            // change the
            // encumencumbrance stage as previous Stage
            if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && resultEncum.getBoolean("isAssociatePREncumbrance")
                && resultEncum.has("isFullQtyUsed") && !resultEncum.getBoolean("isFullQtyUsed")) {
              // check if associate pr qty does not use full qty then while reject check funds
              // available (case: if unique code is change from pr to proposal)
              errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(null, proposalmgmt,
                  null);
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
      // updating records
      if (!errorFlag) {
        // Delete the Proposal encumbrance
        // if the version is zero
        if (enccontrollist.size() > 0 && proposalmgmt.isEfinIsbudgetcntlapp()
            && baseProposalObj == null) {
          // if associate proposal line does not have PR
          if (!fromPR) {
            // if proposal is manual encumbrance then reverse applied amount
            if (proposalmgmt.getEfinEncumbrance() != null) {
              if (proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {
                if (proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
                  ProposalManagementRejectMethods.updateManualEncumAppAmt(proposalmgmt, false);
                  proposalmgmt.setEfinIsbudgetcntlapp(false);
                  OBDal.getInstance().save(proposalmgmt);
                }
                // if auto the delete the new encumbrance and update the budget inquiry funds
                // available
                else {
                  // remove encum
                  EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();
                  ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt, false);
                  // remove encum reference in proposal lines.
                  List<EscmProposalmgmtLine> proline = proposalmgmt.getEscmProposalmgmtLineList();
                  for (EscmProposalmgmtLine proLineList : proline) {
                    proLineList.setEfinBudgmanencumline(null);
                    OBDal.getInstance().save(proLineList);
                  }

                  OBDal.getInstance().flush();
                  // Remove the encumbrance only
                  // for the first version
                  if (proposalmgmt.getVersionNo() == 0) {
                    // Before removing the encumbrance
                    // remove the link between encumbrance and proposal
                    if (encum.getEscmProposalManagementEMEfinEncumbranceIDList().size() > 0) {
                      for (EscmProposalMgmt proposalMgmt : encum
                          .getEscmProposalManagementEMEfinEncumbranceIDList()) {
                        proposalMgmt.setEfinEncumbrance(null);
                      }
                    }
                    OBDal.getInstance().remove(encum);
                  }

                  // update the budget controller flag and encumbrance ref
                  proposalmgmt.setEfinEncumbrance(null);
                  proposalmgmt.setEfinIsbudgetcntlapp(false);
                  OBDal.getInstance().save(proposalmgmt);
                }
              }
              // if associate encumbrance type is not proposal award encumbrance - then
              // encumbrance
              // associate with bid . so need to change the encumbrance stage as "Bid Encumbrance"
              // else {
              //
              // }
              // change the encumbrance stage as "Bid Encumbrance"
              if (proposalmgmt.getEfinEncumbrance() != null) {
                encumbrance = proposalmgmt.getEfinEncumbrance();
                encumbrance.setEncumStage("BE");
                encumbrance.setBusinessPartner(null);
                OBDal.getInstance().save(encumbrance);
              }
            }
            if (proposalmgmt.getEscmBidmgmt() != null) {
              if (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {
                ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposalmgmt, false,
                    true, null);
                proposalmgmt.setEfinIsbudgetcntlapp(false);
                OBDal.getInstance().save(proposalmgmt);
              }
            }
          } else {
            // if Proposal is associate with Purchase Requisition
            if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && resultEncum.getBoolean("isAssociatePREncumbrance")
                && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
              encumbrance = proposalmgmt.getEfinEncumbrance();
              encumbrance.setEncumStage("PRE");
              encumbrance.setBusinessPartner(null);
              OBDal.getInstance().save(encumbrance);
              ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(proposalmgmt,
                  encumbrance, false, true);
              proposalmgmt.setEfinIsbudgetcntlapp(false);
              proposalmgmt.setEfinEncumbrance(null);
            } else {
              // reactive the new encumbrance changes while did split and merge
              if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposalmgmt, false,
                    null);
              }
              if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposalmgmt, false,
                    null);
              }
              // if pr is skip the encumbrance
              if (resultEncum.has("isAssociatePREncumbrance")
                  && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                // remove encum
                EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();
                ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt, false);
                // remove encum reference in proposal lines.
                List<EscmProposalmgmtLine> proline = proposalmgmt.getEscmProposalmgmtLineList();
                for (EscmProposalmgmtLine proLineList : proline) {
                  proLineList.setEfinBudgmanencumline(null);
                  OBDal.getInstance().save(proLineList);
                }

                OBDal.getInstance().flush();
                OBDal.getInstance().remove(encum);
                // update the budget controller flag and encumbrance ref
                proposalmgmt.setEfinEncumbrance(null);
                proposalmgmt.setEfinIsbudgetcntlapp(false);
                OBDal.getInstance().save(proposalmgmt);
              }
            }
          }
        }
        // Do modification on encumbrance when the
        // proposal have more than one version
        if (baseProposalObj != null) {
          Boolean is_process_not_sucess = ProposalManagementActionMethod
              .multiVersionProposalLineCancelProcess(proposalmgmtline, baseProposalObj);
          if (is_process_not_sucess) {
            OBDal.getInstance().rollbackAndClose();
            OBError result1 = null;
            if (proposalmgmt.getEfinEncumbrance() != null
                && proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
              result1 = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NotSufficientAmt_Encum@");
            } else {
              result1 = OBErrorBuilder.buildMessage(null, "error", "@Efin_Encum_Amt_Error@");
            }
            bundle.setResult(result1);
            return;

          }
        }
      }

      // reduce
      // awarded quantity from requisition
      // while cancel the Proposal Line ?
      for (EscmProposalsourceRef obj_sourceRef_line : proposalmgmtline
          .getEscmProposalsourceRefList()) {
        if (obj_sourceRef_line.getRequisitionLine() != null) {
          RequisitionLine objRequisition = obj_sourceRef_line.getRequisitionLine();
          soucrRef_Qty = obj_sourceRef_line.getReservedQuantity();
          total_award_qty = objRequisition.getEscmAwardedQty().subtract(soucrRef_Qty);
          objRequisition.setEscmAwardedQty(total_award_qty);
          objRequisition.setEscmIsproposal(false);
          OBDal.getInstance().save(objRequisition);
        }
      }

      Status = ProposalManagementActionMethod.cancelline("CL", proposalmgmt, proposalmgmtline, vars,
          tabId);
      if ((proposalmgmt.getEscmBidmgmt() != null
          && proposalmgmt.getEscmBidmgmt().getBidtype().equals("DR"))
          || proposalmgmt.getEscmBidmgmt() == null) {
        ProposalManagementActionMethod.updateProsalAtt("CL", proposalmgmt, proposalmgmtline);
      }

      ProposalManagementActionMethod.updateProsalAttaftercancel("CL", proposalmgmt,
          proposalmgmtline);

      OBDal.getInstance().flush();
      if (Status) {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
        bundle.setResult(result);
        return;
      }

    } catch (Exception e) {
      log.error("Exeception in ProposalLineCancel:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}