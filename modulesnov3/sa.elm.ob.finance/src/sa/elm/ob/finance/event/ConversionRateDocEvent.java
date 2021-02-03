package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sathishkumar P
 * 
 *         This file is handle the save, update event happens in c_conversion_rate_document table
 * 
 */
public class ConversionRateDocEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ConversionRateDoc.ENTITY_NAME) };

  private static final Logger LOG = LoggerFactory.getLogger(ConversionRateDoc.class);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      ConversionRateDoc exchange = (ConversionRateDoc) event.getTargetInstance();

      if (exchange.getCurrency() == exchange.getToCurrency()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FromToCurrency_same"));
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while saving Exchange rate: " + e);
      }
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
      ConversionRateDoc exchange = (ConversionRateDoc) event.getTargetInstance();

      if (exchange.getCurrency() == exchange.getToCurrency()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FromToCurrency_same"));
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while update Exchange rate: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
