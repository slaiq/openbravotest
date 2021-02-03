package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.scm.EscmInitialReceipt;

/**
 * @author Gopinagh. R
 * 
 */
public class IRCompleteDAO {

  private static Logger log4j = Logger.getLogger(IRCompleteDAO.class);

  /**
   * Updates the delivered qty and accepted qty in {@link EscmInitialReceipt}
   * 
   * @param deliveryMapper
   *          Inout line object.
   */
  public static void updateDeliveredQty(Map<ShipmentInOutLine, EscmInitialReceipt> deliveryMapper) {
    try {
      if (!deliveryMapper.isEmpty()) {
        ShipmentInOutLine inOutLine = null;
        EscmInitialReceipt updinitial = null;

        for (Map.Entry<ShipmentInOutLine, EscmInitialReceipt> entry : deliveryMapper.entrySet()) {
          inOutLine = entry.getKey();
          updinitial = entry.getValue();

          updinitial.setAcceptedQty(
              updinitial.getAcceptedQty().subtract(inOutLine.getMovementQuantity()));
          updinitial
              .setDeliveredQty(updinitial.getDeliveredQty().add(inOutLine.getMovementQuantity()));
          log4j.debug("getAcceptedQty:" + updinitial.getAcceptedQty());
          log4j.debug("getDeliveredQty:" + updinitial.getDeliveredQty());
          OBDal.getInstance().save(updinitial);

        }
      }
    } catch (Exception e) {
      log4j.error("Exception while updateDeliveredQty: ", e);
    }
  }

  public static void updateRemainingQtyAndAmt(ShipmentInOut inout) {
    try {
      for (EscmInitialReceipt ir : inout.getEscmInitialReceiptList()) {
        if (!ir.isSummaryLevel() && ir.getSalesOrderLine() != null) {
          OrderLine objOrderLine = OBDal.getInstance().get(OrderLine.class,
              ir.getSalesOrderLine().getId());

          if (inout.getEscmReceivetype().equals("QTY")) {
            ir.setRemainingQuantity(objOrderLine.getOrderedQuantity()
                .subtract((objOrderLine.getEscmQtyporec() == null ? BigDecimal.ZERO
                    : objOrderLine.getEscmQtyporec())
                        .subtract(objOrderLine.getEscmQtyirr() == null ? BigDecimal.ZERO
                            : objOrderLine.getEscmQtyirr())
                        .subtract(objOrderLine.getEscmQtyrejected() == null ? BigDecimal.ZERO
                            : objOrderLine.getEscmQtyrejected())
                        .subtract(objOrderLine.getEscmQtyreturned() == null ? BigDecimal.ZERO
                            : objOrderLine.getEscmQtyreturned()))
                .subtract(objOrderLine.getEscmQtycanceled() == null ? BigDecimal.ZERO
                    : objOrderLine.getEscmQtycanceled())
                .subtract(objOrderLine.getEscmLegacyQtyDelivered() == null ? BigDecimal.ZERO
                    : objOrderLine.getEscmLegacyQtyDelivered()));
          } else if (inout.getEscmReceivetype().equals("AMT")) {
            ir.setRemainingAmt(objOrderLine.getLineNetAmount()
                .subtract((objOrderLine.getEscmAmtporec() == null ? BigDecimal.ZERO
                    : objOrderLine.getEscmAmtporec())
                        .subtract(objOrderLine.getEscmAmtreturned() == null ? BigDecimal.ZERO
                            : objOrderLine.getEscmAmtreturned()))
                .subtract(objOrderLine.getEscmAmtcanceled() == null ? BigDecimal.ZERO
                    : objOrderLine.getEscmAmtcanceled())
                .subtract(objOrderLine.getEscmLegacyAmtDelivered() == null ? BigDecimal.ZERO
                    : objOrderLine.getEscmLegacyAmtDelivered()));
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while updateRemainingQtyAndAmt: ", e);
    }
  }

  // Check whether PO is already used in some other transaction type.
  public static boolean checkPOIsAleardyUsed(ShipmentInOut receipt) {
    boolean isAlreadyUsed = false;
    List<ShipmentInOut> inoutList = new ArrayList<ShipmentInOut>();
    try {
      if (receipt != null) {
        OBQuery<ShipmentInOut> inout = OBDal.getInstance().createQuery(ShipmentInOut.class,
            "as e where e.id != :headerId and e.salesOrder.id = :orderId and e.escmReceivingtype != :receivingtype"
                + " and e.escmReceivingtype in ('PROJ','SR','IR') and e.documentStatus = :documentStatus");
        inout.setNamedParameter("headerId", receipt.getId());
        inout.setNamedParameter("orderId", receipt.getSalesOrder().getId());
        inout.setNamedParameter("receivingtype", receipt.getEscmReceivingtype());
        inout.setNamedParameter("documentStatus", "CO");
        inoutList = inout.list();
        if (inoutList.size() > 0) {
          isAlreadyUsed = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while checkPOIsAleardyUsed: ", e);
    }
    return isAlreadyUsed;
  }

  /**
   * Check whether new version is created for selected PO and status is in Draft, In progress,
   * Require re-approval, Reject
   * 
   * @param orderId
   * @return
   */
  public static boolean isNewVersionCreated(String orderId) {

    boolean isNewVersionCreated = false;

    Order order = OBDal.getInstance().get(Order.class, orderId);
    if (order != null) {
      OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class,
          " as e where e.documentNo = :documentNo order by created desc");
      orderQry.setNamedParameter("documentNo", order.getDocumentNo());
      orderQry.setMaxResult(1);

      if (orderQry != null) {
        List<Order> orderList = orderQry.list();
        if (orderList.size() > 0) {
          Order latestOrder = orderList.get(0);
          if (latestOrder != null) {
            if ("DR".equals(latestOrder.getEscmAppstatus())
                || "ESCM_REJ".equals(latestOrder.getEscmAppstatus())
                || "ESCM_IP".equals(latestOrder.getEscmAppstatus())
                || "ESCM_RA".equals(latestOrder.getEscmAppstatus())) {

              isNewVersionCreated = true;
              return isNewVersionCreated;
            }
          }
        }
      }
    }
    return isNewVersionCreated;
  }
}
