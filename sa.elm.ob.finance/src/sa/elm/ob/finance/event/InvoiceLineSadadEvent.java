
package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

/**
 * This class is used to handle [save, update] event of invoice line table. Mainly for saddad
 * related fields
 * 
 * @author sathishkumar.P
 *
 */

public class InvoiceLineSadadEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InvoiceLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      InvoiceLine invoiceline = (InvoiceLine) event.getTargetInstance();
      Invoice invoice = invoiceline.getInvoice();
      // This check only for order to receive
      if (invoice.isSalesTransaction() && invoice.getTransactionDocument().isEfinIssaddad()) {
        if (invoiceline.getEfinServiceitem() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_serviceitemnotempty"));
        }
      }

    } catch (Exception e) {
      log.error(" Exception while saving invoiceline : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      InvoiceLine invoiceline = (InvoiceLine) event.getTargetInstance();
      Invoice invoice = invoiceline.getInvoice();

      // This check only for order to receive
      if (invoice.isSalesTransaction() && invoice.getTransactionDocument().isEfinIssaddad()) {
        if (invoiceline.getEfinServiceitem() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_serviceitemnotempty"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while saving invoiceline : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
