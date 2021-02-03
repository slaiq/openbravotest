package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAOImpl;

/**
 * @author Qualian technologies pvt ltd.
 */

/*
 * Proposal status will be changed according to action and maintain status history in proposal
 * status history tab.
 */
public class ProposalMgmtStatusAction extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(ProposalMgmtStatusAction.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean Status = false, errorFlag = false;
    try {
      OBContext.setAdminMode();
      // Variable declaration
      final String Proposalstatus = (String) bundle.getParams().get("proposalstatus").toString();
      final String Comments = (String) bundle.getParams().get("comments").toString();
      String proposalAttrId = (String) bundle.getParams().get("Escm_Proposal_Attr_ID");
      final String clientId = (String) bundle.getContext().getClient();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String proposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID");
      String baseProposalId = "";
      EscmProposalMgmt proposal = null;
      BigDecimal soucrRef_Qty = BigDecimal.ZERO;
      BigDecimal total_award_qty = BigDecimal.ZERO;
      Boolean fromPR = false;
      JSONObject resultEncum = null;
      EfinBudgetManencum encumbrance = null;
      Object values = null;
      ProposalTaxCalculationDAO taxHandler = new ProposalTaxCalculationDAOImpl();
      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
      ProposalManagementProcessDAO proposalPrDAO = new ProposalManagementProcessDAOImpl();
      Boolean recalculateTax = Boolean.FALSE;

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      // get the proposal attribute Id by using proposal.
      if (proposalAttrId == null) {
        values = proposalPrDAO.getProposalAttr(proposalId);
        if (values != null) {
          proposalAttrId = (String) values;
        }
      }
      // update purchase requisition lines
      if (Proposalstatus.equals("AWD") && StringUtils.isNotEmpty(proposalAttrId)) {
        EscmProposalAttribute obj_attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalAttrId);
        if (obj_attr.getEscmProposalmgmt().isTaxLine()
            && obj_attr.getEscmProposalmgmt().getEfinTaxMethod() != null
            && obj_attr.getEscmProposalmgmt().getTotalTaxAmount().compareTo(BigDecimal.ZERO) == 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "ESCM_PMCalcTax");
          bundle.setResult(result);
          return;
        }
        if (obj_attr.getEscmProposalmgmt().getContractType() == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ContractCatgCantBeEmpty@");
          bundle.setResult(result);
          return;
        }
        // get requisition line is already awarded
        // full quantity exceeds
        // awarding quantity ????
        if (obj_attr.getEscmProposalmgmt() != null) {
          EscmProposalMgmt obj_proposal = obj_attr.getEscmProposalmgmt();
          for (EscmProposalmgmtLine obj_proposal_line : obj_proposal
              .getEscmProposalmgmtLineList()) {
            for (EscmProposalsourceRef obj_sourceRef_line : obj_proposal_line
                .getEscmProposalsourceRefList()) {
              if (obj_sourceRef_line.getRequisitionLine() != null) {
                RequisitionLine objRequisition = obj_sourceRef_line.getRequisitionLine();
                Requisition obj_requisition = objRequisition.getRequisition();
                String str_docno = obj_requisition.getDocumentNo();
                soucrRef_Qty = obj_sourceRef_line.getReservedQuantity();
                total_award_qty = soucrRef_Qty.add(objRequisition.getEscmAwardedQty());
                if (total_award_qty.compareTo(objRequisition.getQuantity()) == 1) {
                  String message = OBMessageUtils.messageBD("Escm_Award_Qty_NotAvailable")
                      .replace("@", str_docno).concat(",Requisition Line: ")
                      .concat(objRequisition.getLineNo().toString())
                      .concat(" ,Requisition Quantity ")
                      .concat(objRequisition.getQuantity().toString());
                  OBError result = OBErrorBuilder.buildMessage(null, "error", message);
                  bundle.setResult(result);
                  return;
                } else {
                  objRequisition.setEscmAwardedQty(total_award_qty);
                  OBDal.getInstance().save(objRequisition);
                }
              }
            }
          }
        }
      }

      if (proposalId != null)
        proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

      if (proposalId == null && proposalAttrId != null) {
        EscmProposalAttribute obj_attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalAttrId);
        proposal = obj_attr.getEscmProposalmgmt();
        proposalId = obj_attr.getEscmProposalmgmt().getId();
      }
      // Task No.5925
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposal);

      // Validate if PO created when cancel / discard / withdrawn
      if (Proposalstatus.equals("CL") || Proposalstatus.equals("DIS")
          || Proposalstatus.equals("WD")) {
        List<Order> pmOrder = proposalPrDAO.checkPOCreated(proposalId);
        if (pmOrder != null && pmOrder.size() > 0) {
          // Order ord = pmOrder.list().get(0);
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_POCreated@");
          bundle.setResult(result);
          return;
        }
      }

      // check whether tax is calculated for the proposal
      if (Proposalstatus.equals("AWD")) {
        recalculateTax = taxHandler.checkproposalTaxCalculated(proposal);
        if (recalculateTax) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NeedToCalTax@");
          bundle.setResult(result);
          return;
        }
      }

      if ((Proposalstatus.equals("CL") || Proposalstatus.equals("DIS")
          || Proposalstatus.equals("WD"))
          && (proposal.isEfinIsbudgetcntlapp() || proposal.getEscmBaseproposal() != null)) {
        // checking funds validation
        if ((enccontrollist.size() > 0 && proposal.isEfinIsbudgetcntlapp())
            || (enccontrollist.size() > 0 && !proposal.isEfinIsbudgetcntlapp()
                && proposal.getEscmBaseproposal() != null)) {

          // check lines added from pr ( direct PR- proposal)
          for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
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
            if (proposal.getEfinEncumbrance() != null
                && proposal.getEfinEncumbrance().getEncumType().equals("PAE")) {

              if (proposal.getEfinEncumbrance().getEncumMethod().equals("M")) {
                // check encumbrance used or not based on used amount - for both manual & auto
                errorFlag = ProposalManagementRejectMethods.chkManualEncumbranceRejValid(proposal);
                if (errorFlag) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Encum_Used_Cannot_Canl@");
                  bundle.setResult(result);
                  return;
                }
              } else if (proposal.getEfinEncumbrance().getEncumMethod().equals("A")) {
                errorFlag = ProposalManagementRejectMethods.chkAutoEncumbranceValid(proposal);
                if (errorFlag) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Fundsavailabe_Negative@");
                  bundle.setResult(result);
                  return;
                }
              }

            }
            // if proposal is added by using bid managment then do the further validation
            if (proposal.getEscmBidmgmt() != null) {
              if (proposal.getEscmBidmgmt().getEncumbrance() != null) {

                // check encumbrance used or not based on used amount - for proposal with bid
                errorFlag = ProposalManagementRejectMethods.chkManualEncumbranceRejValid(proposal);
                if (errorFlag) {
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Encum_Used_Cannot_Canl@");
                  bundle.setResult(result);
                  return;
                }
                // check pre validation , if encumbrance lines having decrease , increase or unique
                // code changes , then check with funds available
                errorFlag = ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposal,
                    true, true, null);

                // if error flag is true then throw the error - please check the line info.
                if (errorFlag) {
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Chk_Line_Info@");
                  bundle.setResult(result);
                  return;
                }
              }
            }
          } else {
            if (proposal.getEfinEncumbrance() != null) {
              encumbrance = proposal.getEfinEncumbrance();

              // check encumbrance used or not based on used amount - for both manual & auto
              errorFlag = ProposalManagementRejectMethods.chkManualEncumbranceRejValid(proposal);
              if (errorFlag) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Encum_Used_Cannot_Canl@");
                bundle.setResult(result);
                return;
              }

              // get the detail about Purchase requsition fromt the Proposal line-Source ref
              resultEncum = ProposalManagementActionMethod.checkFullPRQtyUitlizeorNot(proposal);

              // if full qty only used then remove the encumbrance reference from the proposal and
              // change the encumencumbrance stage as previous Stage

              if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && resultEncum.getBoolean("isAssociatePREncumbrance")
                  && resultEncum.has("isFullQtyUsed") && !resultEncum.getBoolean("isFullQtyUsed")) {

                // check if associate pr qty does not use full qty then while reject check funds
                // available (case: if unique code is change from pr to proposal)
                errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(null, proposal,
                    null);
              } else if (resultEncum.has("isAssociatePREncumbrance")
                  && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                // check encumbrance used or not based on used amount - for both manual & auto
                errorFlag = ProposalManagementRejectMethods.chkManualEncumbranceRejValid(proposal);
                if (errorFlag) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Encum_Used_Cannot_Canl@");
                  bundle.setResult(result);
                  return;
                }
              } else {
                errorFlag = ProposalManagementActionMethod
                    .chkAndUpdateforProposalPRFullQty(proposal, encumbrance, true, true);
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
        if (enccontrollist.size() > 0 && proposal.isEfinIsbudgetcntlapp()
            || (enccontrollist.size() > 0 && !proposal.isEfinIsbudgetcntlapp()
                && proposal.getEscmBaseproposal() != null)) {
          // if associate proposal line does not have PR
          if (!fromPR) {
            // if proposal is manual encumbrance then reverse applied amount
            if (proposal.getEfinEncumbrance() != null) {
              if (proposal.getEfinEncumbrance().getEncumType().equals("PAE")) {
                if (proposal.getEfinEncumbrance().getEncumMethod().equals("M")) {
                  ProposalManagementRejectMethods.updateManualEncumAppAmt(proposal, true);
                }
                // if auto the delete the new encumbrance and update the budget inquiry funds
                // available
                else {

                  // remove encum
                  ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposal, true);
                }
              } else {
                // if associate encumbrance type is not proposal award encumbrance - then
                // encumbrance
                // associate with bid . so need to change the encumbrance stage as "Bid Encumbrance"
                if (proposal.getEscmBidmgmt() != null) {
                  if (proposal.getEscmBidmgmt().getEncumbrance() != null) {
                    if (proposal.getEfinEncumbrance() != null) {
                      encumbrance = proposal.getEfinEncumbrance();
                      encumbrance.setEncumStage("BE");
                      encumbrance.setBusinessPartner(null);
                      OBDal.getInstance().save(encumbrance);
                    }
                    ProposalManagementRejectMethods.getProposaltoBidDetailsRej(proposal, false,
                        true, null);

                    // remove encum reference in proposal lines.
                    List<EscmProposalmgmtLine> proline = proposal.getEscmProposalmgmtLineList();
                    for (EscmProposalmgmtLine proLineList : proline) {
                      proLineList.setEfinBudgmanencumline(null);
                      OBDal.getInstance().save(proLineList);
                    }

                    proposal.setProposalstatus(Proposalstatus);
                    proposal.setEfinEncumbrance(null);
                    OBDal.getInstance().save(proposal);

                  }
                }
              }
            }
          } else {

            // if Proposal is associate with Purchase Requisition
            if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && resultEncum.getBoolean("isAssociatePREncumbrance")
                && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {

              ProposalManagementActionMethod.chkAndUpdateforProposalPRFullQty(proposal, encumbrance,
                  false, true);
              // change the encum stage.
              encumbrance.setEncumStage("PRE");
              OBDal.getInstance().save(encumbrance);
              // make the business partner as null
              proposal.getEfinEncumbrance().setBusinessPartner(null);
              proposal.setEfinEncumbrance(null);
              OBDal.getInstance().save(proposal);

            } else {
              encumbrance = proposal.getEfinEncumbrance();
              // reactive the new encumbrance changes while did split and merge
              if (resultEncum != null && resultEncum.has("type")
                  && resultEncum.getString("type").equals("SPLIT")) {
                ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposal, true,
                    null);
              }
              if (resultEncum != null && resultEncum.has("type")
                  && resultEncum.getString("type").equals("MERGE")) {
                ProposalManagementRejectMethods.reactivateSplitPR(resultEncum, proposal, true,
                    null);
              }
              // if pr is skip the encumbrance
              if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                // remove encum
                ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposal, true);
              }
            }
          }
        }
      }
      if ((Proposalstatus.equals("CL") || Proposalstatus.equals("DIS")
          || Proposalstatus.equals("WD")) && StringUtils.isNotEmpty(proposalAttrId)) {
        EscmProposalMgmt newProposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        EscmProposalAttribute obj_attr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalAttrId);
        if (obj_attr.getEscmProposalmgmt() != null) {
          if (newProposal.getEscmBaseproposal() != null && newProposal.getEscmBaseproposal().getId()
              .equals(obj_attr.getEscmProposalmgmt().getId())) {
            // EscmProposalMgmt obj_proposal = obj_attr.getEscmProposalmgmt();
            EscmProposalMgmt obj_proposal = newProposal;
            for (EscmProposalmgmtLine obj_proposal_line : obj_proposal
                .getEscmProposalmgmtLineList()) {
              if (obj_proposal_line.getEscmBidmgmtLine() != null) {
                obj_proposal_line.setEscmBidmgmtLine(null);
                OBDal.getInstance().save(obj_proposal_line);
              }
            }
            if (obj_proposal.getEscmBidmgmt() != null) {
              if (obj_proposal.getNotes() != null) {
                obj_proposal.setNotes(obj_proposal.getNotes().concat(" related bidno")
                    + obj_proposal.getEscmBidmgmt().getBidno());
              } else {
                obj_proposal.setNotes("related bidno" + obj_proposal.getEscmBidmgmt().getBidno());
              }
              // obj_proposal.setEscmBidmgmt(null);
              OBDal.getInstance().save(obj_proposal);

            }

            for (EscmProposalmgmtLine obj_proposal_line : obj_proposal
                .getEscmProposalmgmtLineList()) {
              if (obj_proposal_line.getStatus() == null
                  || obj_proposal_line.getStatus().equals("null")) {
                for (EscmProposalsourceRef obj_sourceRef_line : obj_proposal_line
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
              }
            }

          }
        }
        OBDal.getInstance().flush();
        if (newProposal.getEscmBidmgmt() != null) {
          newProposal.setProposalstatus(Proposalstatus);
          newProposal
              .setBidName(newProposal.getBidName() + " " + newProposal.getEscmBidmgmt().getBidno());
          newProposal.setEscmBidmgmt(null);
          OBDal.getInstance().save(newProposal);
          OBDal.getInstance().flush();
        }

      } else if ((Proposalstatus.equals("CL") || Proposalstatus.equals("DIS")
          || Proposalstatus.equals("WD")) && StringUtils.isNotEmpty(proposalId)) {
        // reduce already awarded quantity from requisition line
        EscmProposalMgmt obj_proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        for (EscmProposalmgmtLine obj_proposal_line : obj_proposal.getEscmProposalmgmtLineList()) {
          if (obj_proposal_line.getStatus() == null
              || obj_proposal_line.getStatus().equals("null")) {
            for (EscmProposalsourceRef obj_sourceRef_line : obj_proposal_line
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
          }
        }

      }

      /*
       * Task No:5336 if (Proposalstatus.equals("AWD") && proposal != null &
       * proposal.getEscmBidmgmt() != null) { errorFlag =
       * BGWorkbenchDAO.checkBGAmtlessthanCal(proposalId, null); } if (errorFlag) {
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", "@ESCM_BGHundPerValidPro@"); bundle.setResult(result); return; } else {
       */
      // update status of proposal and insert in proposal action history.
      Status = ProposalManagementActionMethod.changeProposalStatusAndMaintainHistory(Proposalstatus,
          Comments, proposalId, proposalAttrId, clientId, roleId, userId);
      if (Status) {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Already_Processed@");
        bundle.setResult(result);
        return;
      }
      // }
    } catch (Exception e) {
      log.error("Exeception in ProposalManagement status Action:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }
}
