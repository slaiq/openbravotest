package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinBudgetAdd;

/*Budget Addition Event for tracking already a same budget added in budget addition and status  in Open and In Approval. */

public class BudgetAdditionEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetAdd.ENTITY_NAME) };

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
      EfinBudgetAdd budgetAdd = (EfinBudgetAdd) event.getTargetInstance();
      final Property budgetID = entities[0].getProperty(EfinBudgetAdd.PROPERTY_BUDGET);

      if (!event.getPreviousState(budgetID).equals(event.getCurrentState(budgetID))) {
        OBQuery<EfinBudgetAdd> budgetchk = OBDal.getInstance().createQuery(EfinBudgetAdd.class,
            " budget.id='" + budgetAdd.getBudget().getId() + "' and client.id = '"
                + budgetAdd.getClient().getId() + "' and status in ('O','RW','IA') ");
        if (budgetchk.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetAdd_Exists"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating record in Budget Addition: " + e);
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
      EfinBudgetAdd budgetAdd = (EfinBudgetAdd) event.getTargetInstance();

      if (budgetAdd.getBudget().getId() != null) {
        OBQuery<EfinBudgetAdd> budgetchk = OBDal.getInstance().createQuery(EfinBudgetAdd.class,
            " budget.id='" + budgetAdd.getBudget().getId() + "' and client.id = '"
                + budgetAdd.getClient().getId() + "' and status in ('O','RW','IA') ");
        if (budgetchk.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetAdd_Exists"));
        }
      }
    }

    catch (OBException e) {
      log.error(" Exception while creating record in Budget Addition: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

    } catch (Exception e) {
      log.error(" Exception while Delete record in Budget Addition: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
