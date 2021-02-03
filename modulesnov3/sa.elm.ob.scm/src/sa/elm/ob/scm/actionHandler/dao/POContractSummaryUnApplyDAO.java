package sa.elm.ob.scm.actionHandler.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.utility.util.Utility;

public class POContractSummaryUnApplyDAO {
  private static final Logger log = LoggerFactory.getLogger(POContractSummaryUnApplyDAO.class);

  /**
   * unapply po change values to all child lines and calculate line net amt
   * 
   * @param Order
   * @return int
   */
  public static int unApplyChangeValueToAllLines(Order ord) {
    int setValue = 0;
    List<OrderLine> ordLnLs = null;
    try {
      OBContext.setAdminMode();
      ordLnLs = POContractSummaryTotPOChangeDAO.getOrderChildLineList(ord.getId());

      if (ordLnLs != null && ordLnLs.size() > 0) {

        for (int i = 0; i < ordLnLs.size(); i++) {
          OrderLine ordLn = ordLnLs.get(i);

          ordLn.setEscmPoChangeType(null);
          ordLn.setEscmPoChangeFactor(null);
          ordLn.setEscmPoChangeValue(BigDecimal.ZERO);
          ordLn.setEscmLineTotalUpdated(ordLn.getOrderedQuantity().multiply(ordLn.getUnitPrice()));
          ordLn.setLineNetAmount(ordLn.getOrderedQuantity().multiply(ordLn.getUnitPrice()));
          ordLn.setEscmUnitpriceAfterchag(BigDecimal.ZERO);

          OBDal.getInstance().save(ordLn);
        }
        OBDal.getInstance().flush();
      } else {
        setValue = 1;// no line
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      setValue = 2;// exception
      log.error("Exception while unApplyChangeValueToAllLines:" + e);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      setValue = 2;
      log.error("Exception while unApplyChangeValueToAllLines:" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return setValue;
  }

  /**
   * Unapply value to child lines from its parent id
   * 
   * @param parentId
   *          in list
   * @param ESCMDefLookupsTypeLn
   *          poChangeType, poChangeFact
   * @param lineChangeValue
   * @return
   */
  public static int unApplyChangeValuesToChildLines(List<String> parentID) {
    StringBuffer query = null;
    Query lnQuery = null;
    int valueSet = 0;
    List<String> parentId = parentID;
    try {
      OBContext.setAdminMode();
      while (parentId != null && !parentId.isEmpty()) {
        query = new StringBuffer();
        query.append(
            "SELECT ordLn.id FROM OrderLine ordLn where ordLn.escmParentline.id in (:parentId)");
        lnQuery = OBDal.getInstance().getSession().createQuery(query.toString());
        lnQuery.setParameterList("parentId", parentId);
        if (lnQuery.list().size() > 0) {
          parentId = new ArrayList<String>();
          for (@SuppressWarnings("rawtypes")
          Iterator iterator = lnQuery.iterate(); iterator.hasNext();) {
            Object objects = iterator.next();
            String chldLnId = objects == null ? "" : objects.toString();
            OrderLine orderLine = Utility.getObject(OrderLine.class, chldLnId);

            if (orderLine.isEscmIssummarylevel()) {
              parentId.add(orderLine.getId());
            } else {
              orderLine.setEscmPoChangeType(null);
              orderLine.setEscmPoChangeFactor(null);
              orderLine.setEscmPoChangeValue(BigDecimal.ZERO);
              orderLine.setEscmLineTotalUpdated(
                  orderLine.getOrderedQuantity().multiply(orderLine.getUnitPrice()));
              orderLine.setLineNetAmount(
                  orderLine.getOrderedQuantity().multiply(orderLine.getUnitPrice()));
              OBDal.getInstance().save(orderLine);
            }
          }
        }
      }
    } catch (Exception e) {
      valueSet = 2;
      log.error("Exception while unApplyChangeValuesToChildLines:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return valueSet;
  }

}
