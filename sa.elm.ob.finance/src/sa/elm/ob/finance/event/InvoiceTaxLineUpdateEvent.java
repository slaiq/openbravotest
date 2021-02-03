package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.finance.event.dao.InvoiceTaxLineUpdateEventDAO;

public class InvoiceTaxLineUpdateEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(InvoiceTaxLineUpdateEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();

      Boolean isValid = Boolean.TRUE;
      Invoice invoice = (Invoice) event.getTargetInstance();
      // Property isTax = entities[0].getProperty(Invoice.PROPERTY_EFINISTAX);
      // BigDecimal tax = invoice.getEfinTaxAmount();
      if (!invoice.isSalesTransaction()) {
        // if (event.getPreviousState(isTax) != null && event.getPreviousState(isTax) ==
        // Boolean.TRUE
        // && !(event.getPreviousState(isTax).equals(event.getCurrentState(isTax)))
        // && (((tax != null) && tax.compareTo(BigDecimal.ZERO) == 0)) || (tax == null)) {

        isValid = InvoiceTaxLineUpdateEventDAO.canUpdateInvoice(invoice);
        // }

        if (!isValid) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_PurInvSecondBenfCannotBeEmpty"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating invoice header :" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating invoice header: " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
