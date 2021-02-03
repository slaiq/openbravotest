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

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EfinBudgetPreparation;

/**
 * @author Gopalakrishnan on 14/06/2016
 */

public class BudgetPreparationEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetPreparation.ENTITY_NAME) };

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
      EfinBudgetPreparation Preparation = (EfinBudgetPreparation) event.getTargetInstance();
      /*
       * Check if the budget present for the same year,budget type, accounting element
       */
      final Property convertTobudget = entities[0]
          .getProperty(EfinBudgetPreparation.PROPERTY_CONVERTBUDGET);// getting current entered
                                                                     // value
      Object currentToBudget = event.getCurrentState(convertTobudget);
      Object previousToBudget = event.getPreviousState(convertTobudget);

      if ((Preparation.getAlertStatus().equals("O") || Preparation.getAlertStatus().equals("RW"))) {
        if (!currentToBudget.equals(previousToBudget)) {
          OBQuery<EFINBudget> budgetlist = OBDal.getInstance().createQuery(EFINBudget.class,
              "as e where e.accountElement.id='" + Preparation.getAccountElement().getId()
                  + "' and e.year.id='" + Preparation.getYear().getId()
                  + "' and e.salesCampaign.id='" + Preparation.getSalesCampaign().getId() + "'");

          log.debug("listsizeupdatebudget:" + budgetlist.list().size());
          if (budgetlist.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Exists"));
          }
          OBQuery<EfinBudgetPreparation> budgetPrelist = OBDal.getInstance().createQuery(
              EfinBudgetPreparation.class,
              "as e where e.accountElement.id='" + Preparation.getAccountElement().getId()
                  + "' and e.year.id='" + Preparation.getYear().getId()
                  + "' and e.salesCampaign.id='" + Preparation.getSalesCampaign().getId() + "'");

          log.debug("listsizeupdatebudgetprep:" + budgetPrelist.list().size());

          if (budgetPrelist.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_BudgetPreparation_Exists"));
          }

        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating BudgetPreparationEvent: " + e);
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
      EfinBudgetPreparation Preparation = (EfinBudgetPreparation) event.getTargetInstance();
      /*
       * Check if the budget present for the same year,budget type, accounting element
       */
      OBQuery<EFINBudget> budgetlist = OBDal.getInstance().createQuery(EFINBudget.class,
          "as e where e.accountElement.id='" + Preparation.getAccountElement().getId()
              + "' and e.year.id='" + Preparation.getYear().getId() + "' and e.salesCampaign.id='"
              + Preparation.getSalesCampaign().getId() + "'");
      if (budgetlist.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Exists"));
      }
      OBQuery<EfinBudgetPreparation> budgetPrelist = OBDal.getInstance().createQuery(
          EfinBudgetPreparation.class,
          "as e where e.accountElement.id='" + Preparation.getAccountElement().getId()
              + "' and e.year.id='" + Preparation.getYear().getId() + "' and e.salesCampaign.id='"
              + Preparation.getSalesCampaign().getId() + "'");
      if (budgetPrelist.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_BudgetPreparation_Exists"));

      }

    } catch (OBException e) {
      log.error(" Exception while creating BudgetPreparationEvent: " + e);
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
      EfinBudgetPreparation ObjPrep = (EfinBudgetPreparation) event.getTargetInstance();
      if (ObjPrep.getAlertStatus().equals("APP") || ObjPrep.getAlertStatus().equals("IA")) {
        throw new OBException(OBMessageUtils.messageBD("20501"));
      }

    } catch (Exception e) {
      log.error(" Exception while Delete Budget preparation: " + e);
      throw new OBException(e.getMessage());
    }
  }
}
