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
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author gopalakrishnan on 01/08/2017
 * 
 */

public class POContractSummaryCancel implements Process {
  /**
   * This servlet class is responsible to Cancel PO and Contract Summary Records
   */
  private static Logger log = Logger.getLogger(POContractSummaryCancel.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      String strOrderId = (String) bundle.getParams().get("C_Order_ID");
      String comments = (String) bundle.getParams().get("reason");
      String Lang = vars.getLanguage();
      String alertWindow = AlertWindow.PurchaseOrderContract;

      User objUser = OBDal.getInstance().get(User.class, vars.getUser());
      Order objOrder = OBDal.getInstance().get(Order.class, strOrderId);

      final String clientId = bundle.getContext().getClient();
      final String orgId = objOrder.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
      final String roleId = bundle.getContext().getRole();
      String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
      BigDecimal encumAmt = BigDecimal.ZERO;

      boolean fromPR = false, fromProposal = false, errorFlag = false, fundsused = false;
      JSONObject resultEncum = null;

      List<EfinBudgetManencumlines> encumLinesList = null;
      EfinBudgetManencum encumbrance = null;
      @SuppressWarnings("unchecked")
      List<OrderLine> orderlineList = (objOrder.getOrderLineList() != null)
          ? (List<OrderLine>) objOrder.getOrderLineList().stream()
              .filter(a -> a.getEscmQtycanceled().compareTo(BigDecimal.ZERO) == 0
                  && a.getEscmAmtcanceled().compareTo(BigDecimal.ZERO) == 0)
              .collect(Collectors.toList())
          : null;

      ;

      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();

      if (objOrder != null) {
        // Check used in invoice for POM, Then should not allow to canceled
        List<Invoice> inv = POContractSummaryDAO.getPoUsed(strOrderId);
        if (inv != null && inv.size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Po_Used@");
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

        // Task No.5925 check maintain encumbrance control Enable flag- if yes then only process the
        // encumbrance split flow
        enccontrollist = POContractSummaryDAO.getEncControleList(objOrder);
        // End Task No.5925

        // Task No.5925 getting encumbrance line list from the order
        if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")) {
          OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
              .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID ");
          encumLines.setNamedParameter("encumID", objOrder.getEfinBudgetManencum().getId());
          if (encumLines.list() != null && encumLines.list().size() > 0) {
            encumLinesList = encumLines.list();
          }

          // other than 0th version cancel
          if (objOrder.getEscmBaseOrder() != null && objOrder.isEfinEncumbered()
              && objOrder.getEscmOldOrder().getEfinBudgetManencum() != null) {
            // manual encumbrance cancel
            if (objOrder.getEfinBudgetManencum() != null
                && objOrder.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              // validation
              errorFlag = POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                  objOrder.getEscmBaseOrder(), true, true, null);
              // if validation failure throw error
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              }
              // if validation sucess update the remaining & applied amt in manual encumbrance
              else {
                POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                    objOrder.getEscmBaseOrder(), false, true, null);
                errorFlag = true;
              }
            }
            // auto case cancel
            else {
              // validation
              JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(objOrder,
                  objOrder.getEscmBaseOrder(), true, null);
              // funds validation.
              errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                  objOrder.getEfinBudgetint(), "PO", false);
              // if validation failure throw error
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              } else {
                // if validation sucess insert opposite modification entry
                POContractSummaryDAO.doRejectPOVersionMofifcationInEncumbrance(objOrder,
                    objOrder.getEscmBaseOrder(), true, null);
              }
            }
          }
          // 0th version cancel
          else {

            // set the flag of fromPR and fromProposal , if PO added by using Purchase requisition
            // then fromPR flag will be "True" else fromProposal will be "True"
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

            if (orderlineList.size() > 0) {
              for (OrderLine objOrderLine : orderlineList) {
                // check line already cancelled or not. if canceled qty is greater than zero then
                // already line was cancelled
                if (!objOrderLine.isEscmIssummarylevel() && ((objOrder.getEscmReceivetype().equals(
                    "AMT") && objOrderLine.getEscmAmtcanceled().compareTo(BigDecimal.ZERO) == 0)
                    || (objOrder.getEscmReceivetype().equals("QTY")
                        && objOrderLine.getEscmQtycanceled().compareTo(BigDecimal.ZERO) == 0))) {
                  // check associate encumbrance already used in some other process or not.
                  errorFlag = POContractSummaryDAO.checkFundsForCancel(objOrder, encumLinesList,
                      objOrderLine);
                  if (errorFlag) {
                    fundsused = true;
                  }
                  if (fromPR) {
                    // check pr utilized fully in PO or not
                    resultEncum = POContractSummaryDAO.checkStageMovement(objOrder, objOrderLine);
                    encumbrance = objOrder.getEfinBudgetManencum();
                    // if PR full qty used in PO
                    if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                        && resultEncum.getBoolean("isAssociatePREncumbrance")
                        && resultEncum.has("isFullQtyUsed")
                        && resultEncum.getBoolean("isFullQtyUsed")) {

                      // before cancel check the validation (funds available sufficient or not)
                      errorFlag = POContractSummaryDAO.chkAndUpdateforOrderCancelPRFullQty(objOrder,
                          encumbrance, true, true, objOrderLine);
                    }
                    // if PR full Qty not used in PO
                    else if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                        && resultEncum.getBoolean("isAssociatePREncumbrance")
                        && resultEncum.has("isFullQtyUsed")
                        && !resultEncum.getBoolean("isFullQtyUsed")) {
                      // before cancel check the validation (funds available sufficient or not)
                      errorFlag = POContractSummaryDAO
                          .chkFundsAvailforCancelOldEncumbrance(objOrder, objOrderLine);
                    }
                    if (errorFlag)
                      fundsused = true;
                  }
                }
              }
            }
            // if funds used or funds not sufficient then throw error.
            if (fundsused) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProcessFailed(Reason)@");
              bundle.setResult(result);
              return;
            }
            if (orderlineList.size() > 0) {
              encumAmt = orderlineList.get(0).getEfinBudEncumlines().getManualEncumbrance()
                  .getAppliedAmount();
              for (OrderLine objOrderLine : orderlineList) {
                // check child lines already cancelled or not
                if (!objOrderLine.isEscmIssummarylevel() && ((objOrder.getEscmReceivetype().equals(
                    "AMT") && objOrderLine.getEscmAmtcanceled().compareTo(BigDecimal.ZERO) == 0)
                    || (objOrder.getEscmReceivetype().equals("QTY")
                        && objOrderLine.getEscmQtycanceled().compareTo(BigDecimal.ZERO) == 0))) {
                  // effect in encumbrance.
                  if (objOrder.isEfinEncumbered() && !fromPR && !fromProposal) {
                    // validation manual Encumbrance
                    if (objOrder.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                      // call method to reduce amt in maual encumbrance.
                      POContractSummaryDAO.updateManualEncumAmountRej(objOrder, encumLinesList,
                          true, objOrderLine.getId());
                    } else {
                      // call method to reduce amt in auto encumbrance.
                      POContractSummaryDAO.updateAmtInEnquiryRej(objOrder.getId(), encumLinesList,
                          true, objOrderLine.getId());
                    }
                  } else if (fromPR) {
                    // reactivate the merge and splitencumbrance if full qty only used then remove
                    // the
                    // encumbrance reference and change the encumencumbrance stage as PR Stage
                    if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                        && resultEncum.getBoolean("isAssociatePREncumbrance")
                        && resultEncum.has("isFullQtyUsed")
                        && resultEncum.getBoolean("isFullQtyUsed")) {
                      encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                          resultEncum.getString("encumbrance"));
                      log.debug("encumbrance:" + resultEncum);
                      POContractSummaryDAO.chkAndUpdateforOrderCancelPRFullQty(objOrder,
                          encumbrance, false, true, objOrderLine);

                    } else {
                      if (resultEncum.has("type")
                          && resultEncum.getString("type").equals("SPLIT")) {
                        POContractSummaryDAO.reactivatelineSplitPR(resultEncum, objOrder, true,
                            objOrderLine, false, encumAmt);
                      }
                      if (resultEncum.has("type")
                          && resultEncum.getString("type").equals("MERGE")) {
                        POContractSummaryDAO.reactivatelineSplitPR(resultEncum, objOrder, true,
                            objOrderLine, false, encumAmt);
                      }
                      // if pr is skip the encumbrance
                      if (resultEncum.has("isAssociatePREncumbrance")
                          && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                        // call method to reduce amt in auto encumbrance.
                        POContractSummaryDAO.updateAmtInEnquiryRej(objOrder.getId(), encumLinesList,
                            true, objOrderLine.getId());
                      }
                    }
                  }

                  else if (fromProposal) {
                    boolean alllineCancel = true;
                    boolean checkException = false;
                    if (objOrder.getEfinBudgetManencum().getEncumType().equals("POE")) {
                      // newly created so delete new and increase in old.
                      errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(objOrder,
                          objOrderLine);
                      if (errorFlag) {
                        OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                            "@ESCM_ProcessFailed(Reason)@");
                        bundle.setResult(result1);
                        return;
                      } else {
                        POContractSummaryDAO.reactivatelineSplitPR(resultEncum, objOrder, true,
                            objOrderLine, true, encumAmt);
                        errorFlag = true;
                      }
                    } else if (objOrder.getEfinBudgetManencum().getEncumStage().equals("POE")) {
                      // auto case only if po have diff than proposal, manual case if proposal and
                      // po have same amount

                      // check diff between proposal and order, make impact in encumbrance
                      BigDecimal diff = objOrderLine.getLineNetAmount()
                          .subtract(objOrderLine.getEscmProposalmgmtLine().getLineTotal());
                      if (diff.compareTo(BigDecimal.ZERO) < 0) {
                        // check funds available
                        JSONObject fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                            objOrder.getEfinBudgetint(), objOrderLine.getEFINUniqueCode(),
                            diff.negate());
                        if (fundsCheckingObject.has("errorFlag")) {
                          if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                            checkException = Boolean.TRUE;
                            String status = fundsCheckingObject.getString("message");
                            objOrderLine.setEfinFailureReason(status);
                            OBDal.getInstance().save(objOrderLine);
                          } else {
                            // insert modification
                            POContractSummaryDAO.insertEncumbranceModificationDiff(
                                objOrderLine.getEfinBudEncumlines(), objOrderLine, diff);
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
                          POContractSummaryDAO.insertEncumbranceModificationDiff(
                              objOrderLine.getEfinBudEncumlines(), objOrderLine, diff);
                        } else {
                          EfinBudgetManencumlines encumbranceline = objOrderLine
                              .getEfinBudEncumlines();
                          encumbranceline.setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                          OBDal.getInstance().save(encumbranceline);
                        }

                      }
                      objOrderLine.setEfinBudEncumlines(null);
                      OBDal.getInstance().save(objOrderLine);

                      for (OrderLine ordline : orderlineList) {
                        if (ordline.getEfinBudEncumlines() != null) {
                          alllineCancel = false;
                          break;
                        }
                      }

                      if (alllineCancel) {
                        objOrder.getEfinBudgetManencum().setEncumStage("PAE");
                        objOrder.setEfinEncumbered(false);
                        OBDal.getInstance().save(objOrder);
                      }
                      // POContractSummaryDAO.reactivatePOProposal(objOrder);
                      errorFlag = true;
                    } else {
                      // old encum just reduce value.
                      objOrder.getEfinBudgetManencum().setEncumStage("PAE");
                      objOrder.setEfinEncumbered(false);
                      OBDal.getInstance().save(objOrder);
                      OBDal.getInstance().flush();
                    }
                  }
                }
                encumAmt = encumAmt.subtract(objOrderLine.getLineNetAmount());
              }
            }
          }
        }
        // End Task No.5925
        if (orderlineList.size() > 0) {
          BigDecimal cancelledQty = BigDecimal.ZERO;
          BigDecimal oldQty = BigDecimal.ZERO;
          BigDecimal oldAmt = BigDecimal.ZERO;
          for (OrderLine objOrderLine : orderlineList) {
            // revert pending qty in requisition Line

            if (!objOrderLine.isEscmIssummarylevel()) {

              if (objOrder.getEscmReceivetype().equals("AMT")) {
                if (objOrderLine.getEscmAmtcanceled().equals(BigDecimal.ZERO)) {
                  BigDecimal legacyDelAmt = objOrderLine.getEscmLegacyAmtDelivered() != null
                      ? objOrderLine.getEscmLegacyAmtDelivered()
                      : BigDecimal.ZERO;
                  BigDecimal poreceivedAmt = objOrderLine.getEscmAmtporec()
                      .subtract(objOrderLine.getEscmAmtreturned());
                  BigDecimal cancelledAmt = objOrderLine.getLineNetAmount().subtract(poreceivedAmt)
                      .subtract(objOrderLine.getEscmAmtcanceled()).subtract(legacyDelAmt);
                  if (objOrderLine.getEscmOldOrderline() != null) {
                    OrderLine oldorderline = objOrderLine.getEscmOldOrderline();
                    oldAmt = oldorderline.getLineNetAmount().subtract(poreceivedAmt)
                        .subtract(oldorderline.getEscmAmtcanceled());
                  }
                  objOrderLine.setEscmAmtcanceled(cancelledAmt);
                  cancelledAmt = cancelledAmt.subtract(oldAmt);

                  // To update Release amt based on ReleasedAmt Subtracted by Cancelled Amt
                  if (objOrder.getEscmOrdertype().equals("PUR_REL")) {
                    BigDecimal releaseAmt = objOrderLine.getEscmAgreementLine().getEscmReleaseamt()
                        .subtract(cancelledAmt);
                    objOrderLine.getEscmAgreementLine().setEscmReleaseamt(releaseAmt);
                  }
                }
              } else {
                if (objOrderLine.getEscmQtycanceled().equals(BigDecimal.ZERO)) {
                  BigDecimal poreceivedQty = objOrderLine.getEscmQtyporec()
                      .subtract(objOrderLine.getEscmQtyreturned())
                      .subtract(objOrderLine.getEscmQtyrejected())
                      .subtract(objOrderLine.getEscmQtyirr());
                  cancelledQty = objOrderLine.getOrderedQuantity().subtract(poreceivedQty)
                      .subtract(objOrderLine.getEscmQtycanceled());
                  if (objOrderLine.getEscmOldOrderline() != null) {
                    OrderLine oldorderline = objOrderLine.getEscmOldOrderline();
                    oldQty = oldorderline.getOrderedQuantity().subtract(poreceivedQty)
                        .subtract(oldorderline.getEscmQtycanceled());
                  }

                  objOrderLine.setEscmQtycanceled(cancelledQty);
                  cancelledQty = cancelledQty.subtract(oldQty);

                  // To update Release qty based on ReleasedQty Subtracted by Cancelled Qty
                  if (objOrder.getEscmOrdertype().equals("PUR_REL")) {
                    BigDecimal releaseQty = objOrderLine.getEscmAgreementLine().getEscmReleaseqty()
                        .subtract(cancelledQty);
                    log.debug("releaseQty:" + releaseQty);
                    objOrderLine.getEscmAgreementLine().setEscmReleaseqty(releaseQty);
                  }
                }
              }
              objOrderLine.setUpdated(new java.util.Date());
              objOrderLine.setUpdatedBy(OBContext.getOBContext().getUser());
              objOrderLine.setEscmCanceldate(new java.util.Date());
              objOrderLine.setEscmCancelreason(comments);
              objOrderLine.setESCMCancelledBy(OBContext.getOBContext().getUser());
              OBDal.getInstance().save(objOrderLine);

              if (objOrderLine.getEscmParentline() != null) {
                OrderLine objParent = null;
                int parentSize;
                String parentId = objOrderLine.getEscmParentline().getId();

                if (objOrder.getEscmReceivetype().equals("AMT")) {
                  OBQuery<OrderLine> checkQry = OBDal.getInstance().createQuery(OrderLine.class,
                      "as e where e.escmParentline.id=:parentID and e.escmAmtcanceled=0");
                  checkQry.setNamedParameter("parentID", parentId);
                  parentSize = checkQry.list().size();
                  if (parentSize == 1) {
                    objParent = OBDal.getInstance().get(OrderLine.class,
                        objOrderLine.getEscmParentline().getId());
                    BigDecimal parentPoRecAmt = objParent.getEscmAmtporec()
                        .subtract(objParent.getEscmAmtreturned());
                    BigDecimal parentCancelledAmt = objParent.getLineNetAmount()
                        .subtract(parentPoRecAmt).subtract(objParent.getEscmAmtcanceled());
                    objParent.setEscmAmtcanceled(parentCancelledAmt);

                  }
                } else {
                  OBQuery<OrderLine> checkQry = OBDal.getInstance().createQuery(OrderLine.class,
                      "as e where e.escmParentline.id=:parentID and e.escmQtycanceled=0");
                  checkQry.setNamedParameter("parentID", parentId);

                  parentSize = checkQry.list().size();
                  if (parentSize == 1) {
                    objParent = OBDal.getInstance().get(OrderLine.class,
                        objOrderLine.getEscmParentline().getId());
                    BigDecimal parentPoRecQty = objParent.getEscmQtyporec()
                        .subtract(objParent.getEscmQtyreturned())
                        .subtract(objParent.getEscmQtyrejected())
                        .subtract(objParent.getEscmQtyirr());
                    BigDecimal parentCancelledQty = objParent.getOrderedQuantity()
                        .subtract(parentPoRecQty).subtract(objParent.getEscmQtycanceled());
                    objParent.setEscmQtycanceled(parentCancelledQty);

                  }
                }
                if (parentSize == 1) {
                  objParent.setEscmCanceldate(new java.util.Date());
                  objParent.setUpdated(new java.util.Date());
                  objParent.setUpdatedBy(OBContext.getOBContext().getUser());
                  objParent.setEscmCancelreason(comments);
                  objParent.setESCMCancelledBy(OBContext.getOBContext().getUser());
                  OBDal.getInstance().save(objParent);
                }

              }
              // revert quantity in PR line
              if (objOrderLine.getSalesOrder().getEscmOldOrder() == null) {
                Boolean isReleased = Utility.releasePROrderQty(objOrderLine.getId(), cancelledQty);
                if (!isReleased) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Escm_PO_Cancel_Failed@");
                  bundle.setResult(result);
                  return;
                }
              }
            } else {
              if (objOrder.getEscmReceivetype().equals("AMT")) {
                objOrderLine.setEscmAmtcanceled(new BigDecimal(1));
                log.debug("objOrderLine:" + objOrderLine.getEscmAmtcanceled());
              } else {
                objOrderLine.setEscmQtycanceled(new BigDecimal(1));
                log.debug("objOrderLine:" + objOrderLine.getEscmQtycanceled());
              }
              objOrderLine.setUpdated(new java.util.Date());
              objOrderLine.setUpdatedBy(OBContext.getOBContext().getUser());
              objOrderLine.setEscmCanceldate(new java.util.Date());
              objOrderLine.setEscmCancelreason(comments);
              objOrderLine.setESCMCancelledBy(OBContext.getOBContext().getUser());
              OBDal.getInstance().save(objOrderLine);
            }
          }
        }

        // update order status as cancelled
        objOrder.setUpdated(new java.util.Date());
        objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
        objOrder.setEscmAppstatus("ESCM_CA");
        objOrder.setEutNextRole(null);
        if (objOrder.getEscmOldOrder() != null)
          objOrder.getEscmOldOrder().setActive(true);

        if (!StringUtils.isEmpty(objOrder.getId())) {
          String appResource = null;
          if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
            appResource = "scm.pac.cancelled";
          } else {
            appResource = "scm.poc.cancelled";
          }

          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", objOrder.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", "CA");
          historyData.put("NextApprover", "");
          historyData.put("Revision", objOrder.getEscmRevision());
          historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
          historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

          // int count = Utility.InsertApprovalHistory(historyData);
          int count = POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);
          if (count > 0 && !StringUtils.isEmpty(objOrder.getId())) {
            String Description = sa.elm.ob.scm.properties.Resource.getProperty(appResource, Lang)
                + " " + objUser.getName();

            // set alert for Budget Controller
            if (objOrder.isEfinEncumbered()) {
              AlertUtility.alertInsertionPreference(objOrder.getId(), objOrder.getDocumentNo() + "-"
                  + objOrder.getEscmRevision()
                  + ((objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                      && !objOrder.getEscmNotes().equals("")) ? "-" + objOrder.getEscmNotes() : ""),
                  "ESCM_BudgetControl", objOrder.getClient().getId(), Description, "NEW",
                  alertWindow, appResource, Constants.GENERIC_TEMPLATE, windowId, null);
            }
          }

        }
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Order_Cancelled@");
        bundle.setResult(result);
        return;
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in Purchase Order Cancel:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}