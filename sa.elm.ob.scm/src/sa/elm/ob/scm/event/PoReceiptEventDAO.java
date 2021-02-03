package sa.elm.ob.scm.event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmCOrderV;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopinagh. R
 * 
 */

public class PoReceiptEventDAO {
  public static DateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static Logger log4j = Logger.getLogger(PoReceiptEventDAO.class);

  /**
   * Checks if the PO Movement date is before the corresponding PO Order date.
   * 
   * @param poReceipt
   *          - {@link ShipmentInOut} Object
   * 
   * @return {@link Boolean} true if the date is after order date
   * 
   */

  public static Boolean isMovementDateBeforeOrderDate(ShipmentInOut poReceipt) {
    Boolean isValidMovementDate = Boolean.TRUE;

    try {
      String strOrderId = "";
      strOrderId = poReceipt.getSalesOrder().getId();

      Order order = getOrder(strOrderId);

      if (yearFormat.parse(yearFormat.format(poReceipt.getMovementDate()))
          .compareTo(yearFormat.parse(yearFormat.format(order.getOrderDate()))) < 0) {
        isValidMovementDate = Boolean.FALSE;
      }

    } catch (Exception e) {
      log4j.error("Exception while isMovementDateBeforeOrderDate: ", e);
    }
    return isValidMovementDate;
  }

  /**
   * Returns Order object
   * 
   * @param strOrderId
   *          {@link EscmCOrderV} ID
   */

  public static Order getOrder(String strOrderId) {
    Order order = null;
    try {
      order = Utility.getObject(Order.class, strOrderId);
    } catch (Exception e) {
      log4j.error("Exception while getOrder: ", e);
    }
    return order;
  }
}