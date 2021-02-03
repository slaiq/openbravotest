package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.ad_process.EncumbranceCancellationImpl;

/**
 * 
 * @author Gopinagh. R on 05-04-2018
 * 
 */
public class EncumbranceTransactionCheckEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetManencum.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(EncumbranceTransactionCheckEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();

      Boolean hasTransactions = Boolean.FALSE;

      EfinBudgetManencum encumbrance = (EfinBudgetManencum) event.getTargetInstance();

      if (encumbrance.getDocumentStatus().equals("CO")) {
        final Property supplier = entities[0]
            .getProperty(EfinBudgetManencum.PROPERTY_BUSINESSPARTNER);

        final Property description = entities[0]
            .getProperty(EfinBudgetManencum.PROPERTY_DESCRIPTION);

        if (event.getPreviousState(supplier) != event.getCurrentState(supplier)) {

          EncumbranceCancellationImpl cancellationImpl = new EncumbranceCancellationImpl();
          hasTransactions = cancellationImpl.isTransactedEncumbrance(encumbrance);

        }

        if ((event.getPreviousState(description) != event.getCurrentState(description))) {

          EncumbranceCancellationImpl cancellationImpl = new EncumbranceCancellationImpl();
          hasTransactions = cancellationImpl.isTransactedEncumbrance(encumbrance);

        }
      }

      if (hasTransactions) {
        // throw new OBException(OBMessageUtils.messageBD("efin_encum_noupdate"));
      }

    } catch (OBException e) {
      LOG.error("Exception while EncumbranceTransactionCheckEvent :" + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      LOG.error("Exception while EncumbranceTransactionCheckEvent :" + e);
      e.printStackTrace();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));

    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
