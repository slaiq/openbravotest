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

import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * 
 * @author Divya.J
 * 
 */
public class BudgetControlParametersEvent extends EntityPersistenceEventObserver {
  /**
   * this process handle the event in Budget Control Parameters.
   */
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      EfinBudgetControlParam.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  /**
   * Delete Event
   * 
   * @param event
   */
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinBudgetControlParam budgContrlParam = (EfinBudgetControlParam) event.getTargetInstance();
      // if its ready then dont allow to delete the record.
      if (budgContrlParam.isReady()) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_BudgCntrlParam_CantDel"));
      }
    } catch (Exception e) {
      log.error(" Exception while Delete the Budget Control Parameter: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
