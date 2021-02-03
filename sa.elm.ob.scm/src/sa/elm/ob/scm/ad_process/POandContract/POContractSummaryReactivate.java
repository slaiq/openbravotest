package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementRejectMethods;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Mouli.K
 * 
 */

public class POContractSummaryReactivate implements Process {
  /**
   * This servlet class is responsible to reactivate PO and Contract Summary Records
   */
  private static Logger log = Logger.getLogger(POContractSummaryReactivate.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      String strOrderId = (String) bundle.getParams().get("C_Order_ID");

      Order objOrder = OBDal.getInstance().get(Order.class, strOrderId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objOrder.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
      boolean purchaseRelease = false;
      boolean errorFlagAgmt = false;
      boolean newVersion = false;
      List<EfinRDV> rdvList = new ArrayList<EfinRDV>();
      OBQuery<Order> order = OBDal.getInstance().createQuery(Order.class,
          " as e where e.escmOldOrder.id= :orderId ");
      order.setNamedParameter("orderId", strOrderId);
      if (order.list() != null && order.list().size() > 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Already_Processed@");
        bundle.setResult(result);
        return;
      }

      if (objOrder.getEscmOrdertype().equals("PUR") || objOrder.getEscmOrdertype().equals("CR")
          || objOrder.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {

        OBQuery<Invoice> invoice = OBDal.getInstance().createQuery(Invoice.class,
            " as e where e.efinCOrder.id= :orderId ");
        invoice.setNamedParameter("orderId", strOrderId);
        if (invoice.list() != null && invoice.list().size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_Order_Reactivate_Invoice@");
          bundle.setResult(result);
          return;
        }

        // should not allow to reactivate purchase release if Purchase agreement has new version.
        if (objOrder.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {
          if (objOrder.getEscmPurchaseagreement() != null && POContractSummaryDAO
              .isNewVersionCreatedAgainstPA(objOrder.getEscmPurchaseagreement())) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PAHasNewVersion@");
            bundle.setResult(result);
            return;
          }
        }

        OBQuery<EfinRDV> rdv = OBDal.getInstance().createQuery(EfinRDV.class,
            " as e where e.salesOrder.id= :orderId ");
        rdv.setNamedParameter("orderId", strOrderId);
        if (rdv.list() != null && rdv.list().size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_Order_Reactivate_rdv@");
          bundle.setResult(result);
          return;
        }

        OBQuery<ShipmentInOut> poreceipt = OBDal.getInstance().createQuery(ShipmentInOut.class,
            " as e where e.salesOrder.id= :orderId ");
        poreceipt.setNamedParameter("orderId", strOrderId);
        if (poreceipt.list() != null && poreceipt.list().size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_Order_Reactivate_poreceipt@");
          bundle.setResult(result);
          return;
        }

        if (objOrder.getEscmOldOrder() != null && objOrder.getEscmLegacycontract() == null) {
          BigDecimal advanceAmt = BigDecimal.ZERO;
          OBQuery<EfinRDV> rdvQry = OBDal.getInstance().createQuery(EfinRDV.class,
              " as e where e.salesOrder.id  in ( select a.id from Order a where a.documentNo=:documentno ) and e.salesOrder is not null ");
          rdvQry.setNamedParameter("documentno", objOrder.getDocumentNo());
          rdvList = rdvQry.list();
          if (rdvList.size() > 0) {
            int a1 = 0;
            EfinRDV rdvObj = rdvList.get(0);
            List<EfinRDVTransaction> advanceVersionObj = rdvObj.getEfinRDVTxnList().stream()
                .filter(a -> a.isAdvancetransaction()).collect(Collectors.toList());
            if (advanceVersionObj.size() > 0) {
              EfinRDVTransaction advTxn = advanceVersionObj.get(0);
              advanceAmt = objOrder.getEscmOldOrder().getEscmAdvpaymntAmt();
              if (advanceAmt.compareTo(objOrder.getEscmAdvpaymntAmt()) != 0
                  && advanceAmt.compareTo(advTxn.getNetmatchAmt()) < 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_POCantRdvAdvance@");
                bundle.setResult(result);
                return;
              }

            }

          }
        }

        if (objOrder != null) {

          // ----------------------------------------------------------------------------------------------------
          boolean fromPR = false, fromProposal = false;
          boolean errorFlag = true;
          List<EfinBudgetManencumlines> encumLinesList = null;
          EfinBudgetManencum encum = null;
          JSONObject resultEncum = null;
          EfinBudgetManencum encumbrance = null;

          if (objOrder.getEscmBaseOrder() != null && objOrder.isEfinEncumbered()
              && objOrder.getEscmOldOrder().getEfinBudgetManencum() != null) {
            if (objOrder.getEfinBudgetManencum() != null
                && objOrder.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              errorFlag = POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                  objOrder.getEscmBaseOrder(), true, true, null);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              } else {
                POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                    objOrder.getEscmBaseOrder(), false, true, null);
                errorFlag = true;
              }
            } else {
              JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(objOrder,
                  objOrder.getEscmBaseOrder(), true, null);
              // funds validation.
              errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                  objOrder.getEfinBudgetint(), "PO", false);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              } else {
                POContractSummaryDAO.doRejectPOVersionMofifcationInEncumbrance(objOrder,
                    objOrder.getEscmBaseOrder(), false, null);
                errorFlag = true;
              }
            }
          } else {
            // check from proposal line added case:
            if (objOrder.getEscmProposalmgmt() == null) {
              // check lines added from pr
              OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
                  "salesOrder.id=:orderID and efinMRequisitionline.id is not null");
              orderLine.setNamedParameter("orderID", objOrder.getId());
              if (orderLine.list() != null && orderLine.list().size() > 0) {
                fromPR = true;
              }
            } else {
              fromProposal = true;
            }

            // if after budget control, try to reactivate then check funds for negative impacts.
            if (objOrder.isEfinEncumbered()) {
              OBInterceptor.setPreventUpdateInfoChange(true);

              // get encum line list
              if (objOrder.getEfinBudgetManencum() != null) {
                OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
                    .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID ");
                encumLines.setNamedParameter("encumID", objOrder.getEfinBudgetManencum().getId());
                if (encumLines.list() != null && encumLines.list().size() > 0) {
                  encumLinesList = encumLines.list();
                }

                // validation
                errorFlag = POContractSummaryDAO.checkFundsForReject(objOrder, encumLinesList);
              }
              log.debug("errorFlag:" + errorFlag);
              if (errorFlag) {
                if (objOrder.getEfinBudgetManencum() != null) {
                  if (!fromPR && !fromProposal) {

                    // manual encum
                    if (objOrder.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                      // update amount
                      POContractSummaryDAO.updateManualEncumAmountRej(objOrder, encumLinesList,
                          false, "");
                      objOrder.setEfinEncumbered(false);
                      objOrder.getEfinBudgetManencum().setBusinessPartner(null);
                      OBDal.getInstance().save(objOrder);

                    }
                    // auto encumbrance
                    else {
                      POContractSummaryDAO.updateAmtInEnquiryRej(objOrder.getId(), encumLinesList,
                          false, "");

                      // Check Encumbrance Amount is Zero Or Negative
                      if (objOrder.getEfinBudgetManencum() != null)
                        encumLinelist = objOrder.getEfinBudgetManencum()
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

                      // remove encum
                      encum = objOrder.getEfinBudgetManencum();
                      encum.setDocumentStatus("DR");
                      objOrder.setEfinBudgetManencum(null);
                      objOrder.setEfinEncumbered(false);
                      OBDal.getInstance().save(objOrder);
                      // remove encum reference in lines.
                      List<OrderLine> ordLine = objOrder.getOrderLineList();
                      for (OrderLine ordLineList : ordLine) {
                        ordLineList.setEfinBudEncumlines(null);
                        OBDal.getInstance().save(ordLineList);
                      }
                      OBDal.getInstance().flush();
                      OBDal.getInstance().remove(encum);
                    }
                  } else if (fromPR) {
                    // reactivate the merge and split encumbrance
                    resultEncum = POContractSummaryDAO.checkFullPRQtyUitlizeorNot(objOrder);

                    // if full qty only used then remove the encumbrance reference and change the
                    // encumencumbrance stage as PR Stage
                    if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                        && resultEncum.getBoolean("isAssociatePREncumbrance")
                        && resultEncum.has("isFullQtyUsed")
                        && resultEncum.getBoolean("isFullQtyUsed")) {
                      encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                          resultEncum.getString("encumbrance"));
                      errorFlag = POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(objOrder,
                          encumbrance, true, true);
                      log.debug("errorFlag:" + errorFlag);
                      if (errorFlag) {
                        OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                            "@ESCM_ProcessFailed(Reason)@");
                        bundle.setResult(result1);
                        return;
                      } else {
                        encumbrance.setEncumStage("PRE");

                        POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(objOrder, encumbrance,
                            false, true);

                        // Check Encumbrance Amount is Zero Or Negative
                        if (objOrder.getEfinBudgetManencum() != null)
                          encumLinelist = objOrder.getEfinBudgetManencum()
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

                        if (objOrder.getEfinBudgetManencum() != null)
                          objOrder.getEfinBudgetManencum().setBusinessPartner(null);
                        objOrder.setEfinBudgetManencum(null);
                        objOrder.setEfinEncumbered(false);
                        OBDal.getInstance().save(objOrder);
                        OBDal.getInstance().save(objOrder);

                        errorFlag = true;
                      }
                    }
                    // if pr is skip the encumbrance
                    else if (resultEncum.has("isAssociatePREncumbrance")
                        && !resultEncum.getBoolean("isAssociatePREncumbrance")) {

                      errorFlag = POContractSummaryDAO.checkFundsForReject(objOrder,
                          encumLinesList);
                      if (errorFlag) {
                        POContractSummaryDAO.updateAmtInEnquiryRej(objOrder.getId(), encumLinesList,
                            false, "");

                        // Check Encumbrance Amount is Zero Or Negative
                        if (objOrder.getEfinBudgetManencum() != null)
                          encumLinelist = objOrder.getEfinBudgetManencum()
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
                        // remove encum
                        if (objOrder.getEfinBudgetManencum() != null) {
                          encum = objOrder.getEfinBudgetManencum();
                          encum.setDocumentStatus("DR");
                          objOrder.setEfinBudgetManencum(null);
                          objOrder.setEfinEncumbered(false);
                          OBDal.getInstance().save(objOrder);
                          // remove encum reference in lines.
                          List<OrderLine> ordLine = objOrder.getOrderLineList();
                          for (OrderLine ordLineList : ordLine) {
                            ordLineList.setEfinBudEncumlines(null);
                            OBDal.getInstance().save(ordLineList);
                          }
                          OBDal.getInstance().flush();
                          OBDal.getInstance().remove(encum);
                        }
                      } else {
                        OBError result = OBErrorBuilder.buildMessage(null, "error",
                            "@ESCM_Encum_Used_Cannot_Reactivate@");
                        bundle.setResult(result);
                        return;
                      }
                    }
                    // if full qty not used / manual encumbrance remaining amount and applied amount
                    // will not match / one or more encumbrance used in PO
                    else {
                      errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(objOrder,
                          null);
                      if (errorFlag) {
                        OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                            "@ESCM_ProcessFailed(Reason)@");
                        bundle.setResult(result1);
                        return;
                      } else {
                        if (resultEncum.has("type")
                            && resultEncum.getString("type").equals("SPLIT")) {
                          POContractSummaryDAO.reactivateSplitPR(resultEncum, objOrder);
                        }
                        if (resultEncum.has("type")
                            && resultEncum.getString("type").equals("MERGE")) {
                          POContractSummaryDAO.reactivateSplitPR(resultEncum, objOrder);
                        }
                        errorFlag = true;
                      }
                    }
                  } else if (fromProposal) {
                    boolean checkException = false;
                    if (objOrder.getEfinBudgetManencum().getEncumType().equals("POE")) {
                      // newly created so delete new and increase in old.
                      errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(objOrder,
                          null);
                      if (errorFlag) {
                        OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                            "@ESCM_ProcessFailed(Reason)@");
                        bundle.setResult(result1);
                        return;
                      } else {
                        POContractSummaryDAO.reactivateSplitPR(resultEncum, objOrder);
                        errorFlag = true;
                        POContractSummaryDAO.updateOldProposalEncum(objOrder);
                      }
                    } else if (objOrder.getEfinBudgetManencum().getEncumStage().equals("POE")) {
                      for (OrderLine objOrderLine : objOrder.getOrderLineList()) {
                        if (!objOrderLine.isEscmIssummarylevel()) {
                          // check diff between proposal and order, make impact in encumbrance
                          BigDecimal propLineAmt = objOrderLine.getEscmProposalmgmtLine()
                              .getEscmProposalmgmt().getProposalstatus().equals("PAWD")
                                  ? objOrderLine.getEscmProposalmgmtLine().getAwardedamount()
                                  : objOrderLine.getEscmProposalmgmtLine().getLineTotal();
                          BigDecimal diff = objOrderLine.getLineNetAmount().subtract(propLineAmt);
                          if (diff.compareTo(BigDecimal.ZERO) < 0) {
                            // check funds available
                            JSONObject fundsCheckingObject = CommonValidationsDAO
                                .CommonFundsChecking(objOrder.getEfinBudgetint(),
                                    objOrderLine.getEFINUniqueCode(), diff.negate());
                            if (fundsCheckingObject.has("errorFlag")) {
                              if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                                checkException = Boolean.TRUE;
                                String status = fundsCheckingObject.getString("message");
                                objOrderLine.setEfinFailureReason(status);
                                OBDal.getInstance().save(objOrderLine);
                              } else {
                                // delete modification
                                EfinBudgetManencumlines encumbranceline = objOrderLine
                                    .getEfinBudEncumlines();
                                ProposalManagementRejectMethods.deleteModification(encumbranceline,
                                    diff);
                                encumbranceline
                                    .setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                                OBDal.getInstance().save(encumbranceline);
                              }
                            }
                            if (checkException) {
                              OBDal.getInstance().rollbackAndClose();
                              OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                                  "@ESCM_ProcessFailed(Reason)@");
                              bundle.setResult(result1);
                              return;
                            }
                          } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                            // insert modification
                            if (objOrder.getEfinBudgetManencum().getEncumType().equals("A")) {
                              EfinBudgetManencumlines encumbranceline = objOrderLine
                                  .getEfinBudEncumlines();
                              ProposalManagementRejectMethods.deleteModification(encumbranceline,
                                  diff);
                              encumbranceline
                                  .setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                              OBDal.getInstance().save(encumbranceline);
                            } else {
                              EfinBudgetManencumlines encumbranceline = objOrderLine
                                  .getEfinBudEncumlines();
                              encumbranceline
                                  .setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                              OBDal.getInstance().save(encumbranceline);
                            }
                          }
                        }
                      }

                      objOrder.getEfinBudgetManencum().setEncumStage("PAE");
                      objOrder.setEfinEncumbered(false);
                      OBDal.getInstance().save(objOrder);
                      errorFlag = true;
                      // POContractSummaryDAO.reactivatePOProposal(objOrder);
                      // errorFlag = true;
                    } else {
                      // old encum just reduce value.
                      objOrder.getEfinBudgetManencum().setEncumStage("PAE");
                      objOrder.setEfinEncumbered(false);
                      OBDal.getInstance().save(objOrder);
                      OBDal.getInstance().flush();
                    }
                  }
                }
              } else {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_Encum_Used_Cannot_Reactivate@");
                bundle.setResult(result);
                return;
              }
            }
            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);
          }
          // ----------------------------------------------------------------------------------------------------

          // updating the release amount/quantity if it is a purchase release
          if (objOrder.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {
            POContractSummaryDAO.resetAgreementRelease(objOrder);
          }

          objOrder.setUpdated(new java.util.Date());
          objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
          if (objOrder.getEscmOldOrder() == null) {
            objOrder.setEscmAppstatus("DR");
          } else {
            objOrder.setEscmAppstatus("ESCM_RA");
          }
          objOrder.setEscmDocaction("CO");
          objOrder.setDocumentStatus("DR");
          objOrder.setDocumentAction("CO");
          objOrder.setEutNextRole(null);
          objOrder.setProcessed(false);
          OBDal.getInstance().save(objOrder);
          OBDal.getInstance().flush();

          if (!StringUtils.isEmpty(objOrder.getId())) {
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", objOrder.getId());
            historyData.put("Comments", "");
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("Revision", objOrder.getEscmRevision());
            historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
            historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

            // task no 6093
            // Utility.InsertApprovalHistory(historyData);
            POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);

          }
          if (objOrder.getEscmOldOrder() != null) {
            Order ord = Utility.getObject(Order.class, objOrder.getEscmOldOrder().getId());
            ord.setActive(true);
            OBDal.getInstance().save(objOrder);
            OBDal.getInstance().flush();
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Order_Reactivated@");
          bundle.setResult(result);
          return;
        }
      }

      if (objOrder != null && objOrder.getEscmOrdertype().equals("PUR_AG")
          && objOrder.isEscmIspurchaseagreement()) {
        // if purchase released is created against the purchase agreement then should not allow to
        // do reactivate
        purchaseRelease = POContractSummaryDAO.isPurchaseReleased(objOrder);
        if (purchaseRelease) {
          errorFlagAgmt = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_PurchaseRelease_Reactivate@");
          bundle.setResult(result);
          return;
        }
        // if new version created against this purchase agreement then should not allow to do
        // reactivate
        newVersion = POContractSummaryDAO.isNewVersionCreatedAgainstPA(objOrder);
        if (newVersion) {
          errorFlagAgmt = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_NewVersion_Reactivate@");
          bundle.setResult(result);
          return;
        } else {
          if (!errorFlagAgmt) {
            // ----------------------------------------------------------------------------------------------------

            objOrder.setUpdated(new java.util.Date());
            objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
            if (objOrder.getEscmOldOrder() == null) {
              objOrder.setEscmAppstatus("DR");
            } else {
              objOrder.setEscmAppstatus("ESCM_RA");
            }
            objOrder.setEscmDocaction("CO");
            objOrder.setDocumentStatus("DR");
            objOrder.setDocumentAction("CO");
            objOrder.setEscmProposalmgmt(null);
            objOrder.setProcessed(false);
            objOrder.setEutNextRole(null);
            OBDal.getInstance().save(objOrder);
            OBDal.getInstance().flush();

            if (!StringUtils.isEmpty(objOrder.getId())) {
              JSONObject historyData = new JSONObject();

              historyData.put("ClientId", clientId);
              historyData.put("OrgId", orgId);
              historyData.put("RoleId", roleId);
              historyData.put("UserId", userId);
              historyData.put("HeaderId", objOrder.getId());
              historyData.put("Comments", "");
              historyData.put("Status", "REA");
              historyData.put("NextApprover", "");
              historyData.put("Revision", objOrder.getEscmRevision());
              historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
              historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
              historyData.put("ActionColumn",
                  ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

              // task no 6093
              // Utility.InsertApprovalHistory(historyData);
              POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);

              // Check Encumbrance Amount is Zero Or Negative
              if (objOrder.getEfinBudgetManencum() != null)
                encumLinelist = objOrder.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
              if (encumLinelist.size() > 0)
                checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

              if (checkEncumbranceAmountZero) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
                bundle.setResult(result);
                return;
              }

            }
            if (objOrder.getEscmOldOrder() != null) {
              Order ord = Utility.getObject(Order.class, objOrder.getEscmOldOrder().getId());
              ord.setActive(true);
              OBDal.getInstance().save(objOrder);
              OBDal.getInstance().flush();
            }
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Escm_PurAgreement_Reactivate@");
            bundle.setResult(result);
            return;
          }
        }

      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in Purchase Order Reactivate:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}