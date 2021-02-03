package sa.elm.ob.finance.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

public class OrderToReceiveEvent extends EntityPersistenceEventObserver {

  /**
   * Event to handle exception in order to receive.
   */

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InvoiceLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      InvoiceLine line = (InvoiceLine) event.getTargetInstance();

      if (line.getEfinReceiptType() != null && line.getEfinReceiptType().equals("pp")) {
        if (line.getEFINPrepayment() != null) {
          Invoice inv = OBDal.getInstance().get(Invoice.class, line.getEFINPrepayment().getId());
          if (inv.getEfinPreRemainingamount().compareTo(line.getLineNetAmount()) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_amt_ls_invoice"));
          }
          if (line.getLineNetAmount().compareTo(new BigDecimal("0")) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_amt_ls_invoice_negat"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("Efin_prepayment_mandatory"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating order to receive line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      InvoiceLine line = (InvoiceLine) event.getTargetInstance();

      if (line.getEfinReceiptType() != null && line.getEfinReceiptType().equals("pp")) {
        if (line.getEFINPrepayment() != null) {
          Invoice inv = OBDal.getInstance().get(Invoice.class, line.getEFINPrepayment().getId());
          if (inv.getEfinPreRemainingamount().compareTo(line.getLineNetAmount()) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_amt_ls_invoice"));
          }
          if (line.getLineNetAmount().compareTo(new BigDecimal("0")) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_amt_ls_invoice_negat"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("Efin_prepayment_mandatory"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while adding order to receive line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
