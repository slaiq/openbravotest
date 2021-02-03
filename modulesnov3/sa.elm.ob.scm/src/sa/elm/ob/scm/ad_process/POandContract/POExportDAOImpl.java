package sa.elm.ob.scm.ad_process.POandContract;

import java.util.HashMap;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.util.Constants;

/**
 * @author Rashika.V.S on 24-04-2019
 */

public class POExportDAOImpl implements POExportDAO {
  private static final Logger log4j = LoggerFactory.getLogger(POExportDAOImpl.class);
  private static final String PURCHASE_ORDER = "PUR";
  private static final String CONTRACT = "CR";

  public HashMap<Integer, String> getPoCellStyle(String orderLineId) {
    HashMap<Integer, String> txtStyleMap = new HashMap<>();
    try {
      OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, orderLineId);
      Order order = orderLine.getSalesOrder();
      for (int i = 0; i <= 15; i++) {

        if (order.getEscmOrdertype().equals(PURCHASE_ORDER)
            || order.getEscmOrdertype().equals(CONTRACT)) {
          // Add Requisition
          if (orderLine.getEfinMRequisitionline() != null) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if (i == 1 || i == 2 || orderLine.isEscmIssummarylevel()) {
                txtStyleMap.put(i, "NumericLock");
              } else {
                txtStyleMap.put(i, "NumericUnlock");
              }
            } else if (i == 11 || i == 13 || i == 14
                || (i == 15 && !orderLine.isEscmIssummarylevel())) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }

          // Direct PO
          else if (order.getEscmRevision() == 0) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if (orderLine.isEscmIssummarylevel() || i == 1) {
                txtStyleMap.put(i, "NumericLock");
              } else {
                txtStyleMap.put(i, "NumericUnlock");
              }
            } else if (i == 3 || i == 4 || ((i == 5 || i == 6) && orderLine.getProduct() == null)
                || i == 11 || i == 13 || i == 14
                || (i == 15 && !orderLine.isEscmIssummarylevel())) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }

          // Direct PO Version
          else if (order.getEscmRevision() > 0) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if (i == 1 || i == 2 || orderLine.isEscmIssummarylevel()) {
                txtStyleMap.put(i, "NumericLock");
              } else {
                txtStyleMap.put(i, "NumericUnlock");
              }
            } else if (i == 11 || i == 13 || (i == 15 && !orderLine.isEscmIssummarylevel())
                || (i == 5 && orderLine.isEscmIssummarylevel())) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }
        } else if (order.getEscmOrdertype().equals(Constants.PURCHASE_AGREEMENT)) {

          // Purchase Agreement
          if (order.getEscmRevision() == 0) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if (orderLine.isEscmIssummarylevel() || i == 1) {
                txtStyleMap.put(i, "NumericLock");
              } else {
                txtStyleMap.put(i, "NumericUnlock");
              }
            } else if (i == 3 || i == 4 || i == 13 || i == 14
                || (i == 15 && !orderLine.isEscmIssummarylevel())
                || ((i == 5 || i == 6) && orderLine.getProduct() == null)) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }

          // Agreement version
          else if (order.getEscmRevision() > 0) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if (i == 1 || i == 2 || orderLine.isEscmIssummarylevel()) {
                txtStyleMap.put(i, "NumericLock");
              } else {
                txtStyleMap.put(i, "NumericUnlock");
              }
            } else if (i == 3 || i == 4 || i == 13 || i == 14
                || ((i == 5 || i == 6) && orderLine.getProduct() == null)) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }
        } else if (order.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {

          // Release
          if (order.getEscmRevision() == 0) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if ((i == 7 && order.getEscmReceivetype().equals(Constants.QTY_BASED)
                  && !orderLine.isEscmIssummarylevel())
                  || (i == 8 && order.getEscmReceivetype().equals(Constants.AMOUNT_BASED)
                      && !orderLine.isEscmIssummarylevel())) {
                txtStyleMap.put(i, "NumericUnlock");
              } else {
                txtStyleMap.put(i, "NumericLock");
              }
            } else if (i >= 11 && (i != 15 || !orderLine.isEscmIssummarylevel())) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }

          // Release Version
          else if (order.getEscmRevision() > 0) {
            if (i == 1 || i == 2 || i == 7 || i == 8) {
              if ((i == 7 && order.getEscmReceivetype().equals(Constants.QTY_BASED)
                  && !orderLine.isEscmIssummarylevel())
                  || (i == 8 && order.getEscmReceivetype().equals(Constants.AMOUNT_BASED)
                      && !orderLine.isEscmIssummarylevel())) {
                txtStyleMap.put(i, "NumericUnlock");
              } else {
                txtStyleMap.put(i, "NumericLock");
              }
            } else if (i >= 13 || i == 11) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getPoCellStyle() Method : ", e);
    }
    return txtStyleMap;
  }

  public HashMap<Integer, String> getOeeProposalCellStyle(String propAttrId) {
    HashMap<Integer, String> txtStyleMap = new HashMap<>();
    try {
      for (int i = 0; i <= 10; i++) {
        if (i == 1) {
          txtStyleMap.put(i, "NumericLock");
          txtStyleMap.put(i, "TextLock");
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getOeeProposalCellStyle() Method : ", e);
    }
    return txtStyleMap;

  }
}
