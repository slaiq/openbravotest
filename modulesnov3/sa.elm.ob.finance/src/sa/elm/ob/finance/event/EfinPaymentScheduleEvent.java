package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.finance.EfinInvoicePaymentSch;

public class EfinPaymentScheduleEvent extends EntityPersistenceEventObserver {

  /**
   * @author Gopalakrishnan
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinInvoicePaymentSch.ENTITY_NAME) };
  Order order = null;

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
      EfinInvoicePaymentSch paymentSchedule = (EfinInvoicePaymentSch) event.getTargetInstance();
      if (!paymentSchedule.getInvoice().getDocumentStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_PaymentSchedule_Delete "));
      }

    } catch (Exception e) {
      log.error(" Exception while Deleting EfinPaymentScheduleEvent  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
