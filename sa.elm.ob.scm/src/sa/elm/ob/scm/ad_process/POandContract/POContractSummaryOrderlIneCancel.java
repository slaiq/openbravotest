package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
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
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author gopalakrishnan on 01/08/2017
 * 
 */

public class POContractSummaryOrderlIneCancel implements Process {
  /**
   * This servlet class is responsible to Cancel PO and Contract Summary Lines Records
   */
  private static Logger log = Logger.getLogger(POContractSummaryOrderlIneCancel.class);

  @SuppressWarnings("unchecked")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      String strOrderLineId = (String) bundle.getParams().get("C_OrderLine_ID");
      String comments = bundle.getParams().get("reason").toString();
      BigDecimal qtyCount = BigDecimal.ZERO, cancelledQty = BigDecimal.ZERO;
      OrderLine objOrderLine = OBDal.getInstance().get(OrderLine.class, strOrderLineId);
      Order objOrder = objOrderLine.getSalesOrder();
      final String clientId = bundle.getContext().getClient();
      final String orgId = objOrderLine.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
      final String roleId = bundle.getContext().getRole();
      List<EfinBudgetManencumlines> encumLinesList = null;
      boolean fromPR = false, fromProposal = false, errorFlag = false;
      JSONObject resultEncum = null;
      EfinBudgetManencum encumbrance = null;
      BigDecimal encumAmt = objOrderLine.getEfinBudEncumlines().getManualEncumbrance()
          .getAppliedAmount();

      // Task No.5925
      // check maintain encumbrance control Enable flag- if yes then only process the encumbrance
      // split flow
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = POContractSummaryDAO.getEncControleList(objOrder);
      log.debug("size:" + enccontrollist.size());

      // getting encumbrance line list from the order
      if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")) {
        OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
            .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID");
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
                objOrder.getEscmBaseOrder(), true, true, objOrderLine);
            // if validation failure throw error
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            }
            // if validation sucess update the remaining & applied amt in manual encumbrance
            else {
              POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                  objOrder.getEscmBaseOrder(), false, true, objOrderLine);
              errorFlag = true;
            }
          }
          // auto case cancel
          else {
            // validation
            JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(objOrder,
                objOrder.getEscmBaseOrder(), true, objOrderLine);
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
                  objOrder.getEscmBaseOrder(), true, objOrderLine);
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

          // check associate encumbrance already used in some other process or not.
          errorFlag = POContractSummaryDAO.checkFundsForCancel(objOrder, encumLinesList,
              objOrderLine);
          if (errorFlag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProcessFailed(Reason)@");
            bundle.setResult(result);
            return;
          }

          if (objOrder.isEfinEncumbered() && !fromPR && !fromProposal) {
            // validation
            // manula Encumbrance
            if (objOrder.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              // call method to reduce amt in maual encumbrance.
              POContractSummaryDAO.updateManualEncumAmountRej(objOrder, encumLinesList, true,
                  objOrderLine.getId());
            } else {
              // call method to reduce amt in auto encumbrance.
              POContractSummaryDAO.updateAmtInEnquiryRej(objOrder.getId(), encumLinesList, true,
                  objOrderLine.getId());
            }
          } else if (fromPR) {
            // reactivate the merge and splitencumbrance
            resultEncum = POContractSummaryDAO.checkStageMovement(objOrder, objOrderLine);
            log.debug("resultEncum:" + resultEncum);
            errorFlag = true;
            // if full qty only used then remove the encumbrance reference and change the
            // encumencumbrance stage as PR Stage
            if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && resultEncum.getBoolean("isAssociatePREncumbrance")
                && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
              encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                  resultEncum.getString("encumbrance"));
              errorFlag = POContractSummaryDAO.chkAndUpdateforOrderCancelPRFullQty(objOrder,
                  encumbrance, true, true, objOrderLine);
              if (errorFlag) {
                OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_ProcessFailed(Reason)@");
                bundle.setResult(result1);
                return;
              } else {
                POContractSummaryDAO.chkAndUpdateforOrderCancelPRFullQty(objOrder, encumbrance,
                    false, true, objOrderLine);
              }

            } else if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
              // call method to reduce amt in auto encumbrance.
              POContractSummaryDAO.updateAmtInEnquiryRej(objOrder.getId(), encumLinesList, true,
                  objOrderLine.getId());
            }

            else {
              errorFlag = POContractSummaryDAO.chkFundsAvailforCancelOldEncumbrance(objOrder,
                  objOrderLine);
              if (errorFlag) {
                OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_ProcessFailed(Reason)@");
                bundle.setResult(result1);
                return;
              } else {
                if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                  POContractSummaryDAO.reactivatelineSplitPR(resultEncum, objOrder, true,
                      objOrderLine, false, encumAmt);
                }
                if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                  POContractSummaryDAO.reactivatelineSplitPR(resultEncum, objOrder, true,
                      objOrderLine, false, encumAmt);
                }
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
                    objOrder.getEfinBudgetint(), objOrderLine.getEFINUniqueCode(), diff.negate());
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
                  EfinBudgetManencumlines encumbranceline = objOrderLine.getEfinBudEncumlines();
                  encumbranceline.setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                  OBDal.getInstance().save(encumbranceline);
                }

              }
              objOrderLine.setEfinBudEncumlines(null);
              OBDal.getInstance().save(objOrderLine);

              for (OrderLine ordline : objOrder.getOrderLineList()) {
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
      }
      // End Task No.5925

      String sqlQuery = " select  count(distinct line.c_orderline_id) from c_order ord "
          + " join c_orderline line on line.c_order_id=ord.c_order_id ";
      if (objOrder.getEscmReceivetype().equals("AMT")) {
        sqlQuery += "  where (coalesce(linenetamt,0)-(coalesce(em_escm_amtporec,0)- coalesce(em_escm_amtreturned,0))-coalesce(em_escm_amtcanceled ,0)) > 0 ";
      } else {
        sqlQuery += " where (coalesce(qtyordered,0)-(coalesce(em_escm_qtyporec,0)- coalesce(em_escm_qtyreturned,0)-coalesce(em_escm_qtyrejected,0)-coalesce(em_escm_qtyirr,0))-coalesce(em_escm_qtycanceled ,0)) > 0  ";
      }
      sqlQuery += " and ord.c_order_id='" + objOrder.getId()
          + "' and line.em_escm_issummarylevel='N' ";
      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      if (queryList != null) {
        List<Object> rows = queryList.list();
        if (rows.size() > 0) {
          qtyCount = new BigDecimal(Integer.valueOf(rows.get(0).toString()));
        }

      }
      if (objOrderLine != null) {
        BigDecimal oldQty = BigDecimal.ZERO;
        BigDecimal oldAmt = BigDecimal.ZERO;
        /*
         * // revert pending qty in requisition Line for (EscmOrderSourceRef orderSource :
         * objOrderLine.getEscmOrdersourceRefList()) { RequisitionLine line =
         * orderSource.getRequisitionLine(); line.setUpdated(new java.util.Date());
         * line.setUpdatedBy(OBContext.getOBContext().getUser());
         * line.setEscmPoQty(line.getEscmPoQty().subtract(orderSource.getReservedQuantity()));
         * OBDal.getInstance().save(line); }
         */
        // update cancelled Qty
        /*
         * for (Escmordershipment orderShipment : objOrderLine.getEscmOrdershipmentList()) {
         * BigDecimal poreceivedQty = orderShipment.getQuantityporec()
         * .subtract(orderShipment.getQuantityreturned())
         * .subtract(orderShipment.getQuantityrejected()) .subtract(orderShipment.getQuantityirr());
         * BigDecimal cancelledQty = orderShipment.getMovementQuantity().subtract(poreceivedQty)
         * .subtract(orderShipment.getQuantitycanceled());
         * orderShipment.setQuantitycanceled(cancelledQty); orderShipment.setUpdated(new
         * java.util.Date()); orderShipment.setUpdatedBy(OBContext.getOBContext().getUser());
         * OBDal.getInstance().save(orderShipment);
         * 
         * // revert quantity in PR line Boolean isReleased =
         * Utility.releasePROrderQty(objOrderLine.getId(), cancelledQty); if (!isReleased) {
         * OBDal.getInstance().rollbackAndClose(); OBError result =
         * OBErrorBuilder.buildMessage(null, "error", "@Escm_PO_Cancel_Failed@");
         * bundle.setResult(result); return; } }
         */
        // get encumline list to make cancel impact.

        // To set cancelled amount or quantity bases on order
        if (objOrder.getEscmReceivetype().equals("AMT")) {
          if (objOrderLine.getEscmAmtcanceled().equals(BigDecimal.ZERO)) {
            BigDecimal poreceivedAmt = objOrderLine.getEscmAmtporec()
                .subtract(objOrderLine.getEscmAmtreturned());
            BigDecimal cancelledAmt = objOrderLine.getLineNetAmount().subtract(poreceivedAmt)
                .subtract(objOrderLine.getEscmAmtcanceled());
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
              log.debug("releaseAmt:" + releaseAmt);
              objOrderLine.getEscmAgreementLine().setEscmReleaseamt(releaseAmt);
            }
          }
        } else {
          if (objOrderLine.getEscmQtycanceled().equals(BigDecimal.ZERO)) {
            BigDecimal poreceivedQty = objOrderLine.getEscmQtyporec()
                .subtract(objOrderLine.getEscmQtyreturned())
                .subtract(objOrderLine.getEscmQtyrejected()).subtract(objOrderLine.getEscmQtyirr());
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

        // BigDecimal poreceivedQty = objOrderLine.getEscmQtyporec()
        // .subtract(objOrderLine.getEscmQtyreturned()).subtract(objOrderLine.getEscmQtyrejected())
        // .subtract(objOrderLine.getEscmQtyirr());
        // BigDecimal cancelledQty = objOrderLine.getOrderedQuantity().subtract(poreceivedQty)
        // .subtract(objOrderLine.getEscmQtycanceled());

        objOrderLine.setEscmCanceldate(new java.util.Date());
        objOrderLine.setUpdated(new java.util.Date());
        objOrderLine.setUpdatedBy(OBContext.getOBContext().getUser());
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
              BigDecimal parentCancelledAmt = objParent.getLineNetAmount().subtract(parentPoRecAmt)
                  .subtract(objParent.getEscmAmtcanceled());
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
                  .subtract(objParent.getEscmQtyreturned()).subtract(objParent.getEscmQtyrejected())
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
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PO_Cancel_Failed@");
            bundle.setResult(result);
            return;
          }
        }
        if (qtyCount.compareTo(BigDecimal.ONE) == 0) {
          objOrder.setUpdated(new java.util.Date());
          objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
          objOrder.setEscmAppstatus("ESCM_CA");
          objOrder.setEutNextRole(null);
          if (objOrder.getEscmOldOrder() != null)
            objOrder.getEscmOldOrder().setActive(true);

          if (!StringUtils.isEmpty(objOrder.getId())) {
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", objOrder.getId());
            historyData.put("Comments", "");
            historyData.put("Status", "CA");
            historyData.put("NextApprover", "");
            historyData.put("Revision", objOrder.getEscmRevision());
            historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
            historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

            POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);
            // task no 6093
            // Utility.InsertApprovalHistory(historyData);
          }

        }
        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_OrderLine_Cancelled@");
        bundle.setResult(result);
        return;
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in Purchase Order Line Cancel:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      // OBDal.getInstance().getSession().clear();
    }
  }
}