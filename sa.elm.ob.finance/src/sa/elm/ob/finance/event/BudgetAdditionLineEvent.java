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

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetAddLines;

/*Budget Addition Line Event for Checking same Unique code is exist or not in both Budget Addition & Budget Lines.*/

public class BudgetAdditionLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetAddLines.ENTITY_NAME) };

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
      EfinBudgetAddLines budgetAddLine = (EfinBudgetAddLines) event.getTargetInstance();

      final Property uniquecode = entities[0].getProperty(EfinBudgetAddLines.PROPERTY_UNIQUECODE);
      /* check already Same Unique code is exist or not in Budget Lines */
      if (budgetAddLine.getUniqueCode() != null) {
        if (!event.getPreviousState(uniquecode).equals(event.getCurrentState(uniquecode))) {
          OBQuery<EFINBudgetLines> budgetchk = OBDal.getInstance().createQuery(
              EFINBudgetLines.class,
              "efinBudget.id='" + budgetAddLine.getEfinBudgetadd().getBudget().getId()
                  + "' and client.id = '" + budgetAddLine.getClient().getId() + "' and uniquecode='"
                  + budgetAddLine.getUniqueCode() + "'");
          if (budgetchk.list().size() > 0) {

            throw new OBException(OBMessageUtils.messageBD("Efin_BudgetAddLine_UCExist"));
          }
        }
      }
      /* check already Same Unique code is exist or not in Budget Addition Lines */

      if (budgetAddLine.getUniqueCode() != null) {
        if (!event.getPreviousState(uniquecode).equals(event.getCurrentState(uniquecode))) {
          OBQuery<EfinBudgetAddLines> budgetchk = OBDal.getInstance().createQuery(
              EfinBudgetAddLines.class,
              "efinBudgetadd.id='" + budgetAddLine.getEfinBudgetadd().getId()
                  + "' and client.id = '" + budgetAddLine.getClient().getId() + "' and uniquecode='"
                  + budgetAddLine.getUniqueCode() + "'");
          if (budgetchk.list().size() > 0) {

            throw new OBException(OBMessageUtils.messageBD("Efin_BudgetAddLine_LnUCExist"));
          }
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
      EfinBudgetAddLines budgetAddLine = (EfinBudgetAddLines) event.getTargetInstance();

      /* check already Same Unique code is exist or not in Budget Lines */
      if (budgetAddLine.getUniqueCode() != null) {
        OBQuery<EFINBudgetLines> budgetchk = OBDal.getInstance().createQuery(EFINBudgetLines.class,
            "efinBudget.id='" + budgetAddLine.getEfinBudgetadd().getBudget().getId()
                + "' and client.id = '" + budgetAddLine.getClient().getId() + "' and uniquecode='"
                + budgetAddLine.getUniqueCode() + "'");

        log.debug("getWhereAndOrderBy:" + budgetchk.getWhereAndOrderBy());
        if (budgetchk.list().size() > 0) {

          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetAddLine_UCExist"));
        }
      }

      /* check already Same Unique code is exist or not in Budget Addition Lines */
      if (budgetAddLine.getUniqueCode() != null) {
        OBQuery<EfinBudgetAddLines> budgetchk = OBDal.getInstance().createQuery(
            EfinBudgetAddLines.class,
            "efinBudgetadd.id='" + budgetAddLine.getEfinBudgetadd().getId() + "' and client.id = '"
                + budgetAddLine.getClient().getId() + "' and uniquecode='"
                + budgetAddLine.getUniqueCode() + "'");
        log.debug("budgetchk123:" + budgetchk.getWhereAndOrderBy());
        if (budgetchk.list().size() > 0) {

          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetAddLine_UCExist"));
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
