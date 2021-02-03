package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
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

/**
 * @author Gopinagh.R on 05-08-2018
 * 
 */

public class InvoiceHeaderEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      Invoice invoice = (Invoice) event.getTargetInstance();
      String onlyNumberRegExp = "[0-9]+";

      if (invoice.isEfinIstax() && invoice.getEfinTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }

      if (invoice.getTransactionDocument().isEfinIssaddad() && invoice.isSalesTransaction()
          && (StringUtils.isEmpty(invoice.getDescription())
              || "null".equals(invoice.getDescription()))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_descriptionMandatory"));
      }

      if (invoice.getTransactionDocument().isEfinIssaddad() && invoice.isSalesTransaction()
          && (StringUtils.isEmpty(invoice.getEfinMobileno())
              || "null".equals(invoice.getEfinMobileno()))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_mobileMandatory"));
      }

      if (invoice.getTransactionDocument().isEfinIssaddad() && invoice.isSalesTransaction()
          && !invoice.getEfinMobileno().matches(onlyNumberRegExp)) {
        throw new OBException(OBMessageUtils.messageBD("Efin_notvalidphone"));
      }

    } catch (OBException e) {
      log.error(" Exception while saving invoice header: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while saving invoice header: " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      Invoice invoice = (Invoice) event.getTargetInstance();
      String onlyNumberRegExp = "[0-9]+";

      if (invoice.isEfinIstax() && invoice.getEfinTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }

      if (invoice.getTransactionDocument().isEfinIssaddad() && invoice.isSalesTransaction()
          && (StringUtils.isEmpty(invoice.getDescription())
              || "null".equals(invoice.getDescription()))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_descriptionMandatory"));
      }

      if (invoice.getTransactionDocument().isEfinIssaddad() && invoice.isSalesTransaction()
          && (StringUtils.isEmpty(invoice.getEfinMobileno())
              || "null".equals(invoice.getEfinMobileno()))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_mobileMandatory"));
      }

      if (invoice.getTransactionDocument().isEfinIssaddad() && invoice.isSalesTransaction()
          && !invoice.getEfinMobileno().matches(onlyNumberRegExp)) {
        throw new OBException(OBMessageUtils.messageBD("Efin_notvalidphone"));
      }

    } catch (OBException e) {
      log.error(" Exception while saving invoice header: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while saving invoice header: " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
