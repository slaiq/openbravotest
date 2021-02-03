package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmOrderSourceRef;

/**
 * 
 * @author Gopalakrishnan on 18/07/2017
 * 
 */

public class POContractSourceRefEvent extends EntityPersistenceEventObserver {
  /**
   * Business event in Table Escm_Ordersource_Ref
   */
  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmOrderSourceRef.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmOrderSourceRef orderSource = (EscmOrderSourceRef) event.getTargetInstance();
      Order objOrder = orderSource.getSalesOrderLine().getSalesOrder();
      BigDecimal srcrefQty = orderSource.getReservedQuantity();
      if (objOrder != null) {
        if (objOrder.getEscmOldOrder() == null) {
          if (orderSource.getRequisitionLine() != null
              && srcrefQty.compareTo(BigDecimal.ZERO) > 0) {
            orderSource.setReservedQuantity(BigDecimal.ZERO);

            // / update the requisition line
            RequisitionLine line = orderSource.getRequisitionLine();
            line.setUpdated(new java.util.Date());
            line.setUpdatedBy(OBContext.getOBContext().getUser());
            line.setEscmPoQty(line.getEscmPoQty().subtract(srcrefQty));
            OBDal.getInstance().save(line);

            // update the Order line
            OrderLine objOrderLine = orderSource.getSalesOrderLine();
            objOrderLine.setUpdated(new java.util.Date());
            objOrderLine.setUpdatedBy(OBContext.getOBContext().getUser());
            objOrderLine.setLineNetAmount(objOrderLine.getUnitPrice()
                .multiply(objOrderLine.getOrderedQuantity().subtract(srcrefQty)));
            objOrderLine.setOrderedQuantity(objOrderLine.getOrderedQuantity().subtract(srcrefQty));
            // OBDal.getInstance().save(objOrderLine);

          }
        }
      }
    } catch (OBException e) {
      log.error("exception while deleting Order Source Reference Event", e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting Order Source Reference Event", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
