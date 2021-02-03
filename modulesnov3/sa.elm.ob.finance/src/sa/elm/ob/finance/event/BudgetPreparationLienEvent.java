package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinBudgPrepLines;
import sa.elm.ob.finance.EfinBudgetPreparation;

public class BudgetPreparationLienEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgPrepLines.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinBudgPrepLines Preparation = (EfinBudgPrepLines) event.getTargetInstance();
      OBQuery<EfinBudgetPreparation> preparation = OBDal.getInstance().createQuery(
          EfinBudgetPreparation.class,
          "as e where e.id='" + Preparation.getEfinBudgetPreparation().getId() + "'");
      log.debug("prep:" + preparation.list().size());
      if (preparation.list().size() > 0) {
        EfinBudgetPreparation prep = preparation.list().get(0);
        log.debug("getAlertStatus:" + prep.getAlertStatus());
        if (!prep.getAlertStatus().equals("O") && !prep.getAlertStatus().equals("RW"))
          throw new OBException(OBMessageUtils.messageBD("Efin_BugPrepara_Delete"));
      }

    } catch (OBException e) {
      log.error(" Exception while creating BudgetPreparationLineEvent: " + e);
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
      EfinBudgPrepLines Preparation = (EfinBudgPrepLines) event.getTargetInstance();
      OBQuery<EfinBudgetPreparation> preparation = OBDal.getInstance().createQuery(
          EfinBudgetPreparation.class,
          "as e where e.id='" + Preparation.getEfinBudgetPreparation().getId() + "'");
      if (preparation.list().size() > 0) {
        EfinBudgetPreparation prep = preparation.list().get(0);
        if (!prep.getAlertStatus().equals("O") && !prep.getAlertStatus().equals("RW"))
          throw new OBException(OBMessageUtils.messageBD("Efin_BugPrepara_Delete"));
      }

    } catch (Exception e) {
      log.error(" Exception while Delete Budget preparation: " + e);
      throw new OBException(e.getMessage());
    }
  }
}
