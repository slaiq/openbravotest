package sa.elm.ob.scm.event;

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
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public class TransactionRegistry extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(MaterialTransaction.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      MaterialTransaction transaction = (MaterialTransaction) event.getTargetInstance();
      if (transaction.getMovementType() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_TransactionReg_Save"));

      }

    } catch (OBException e) {
      log.error("exception while creating transaction", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating transaction", e);
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
      MaterialTransaction transaction = (MaterialTransaction) event.getTargetInstance();
      if (transaction.getMovementType() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_TransactionReg_Save"));
      }
    } catch (OBException e) {
      log.error("exception while updating transaction", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating transaction", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
