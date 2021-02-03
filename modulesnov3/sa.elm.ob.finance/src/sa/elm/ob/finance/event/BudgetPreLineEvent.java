package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgPrepLines;

public class BudgetPreLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgPrepLines.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  private static final Logger log = LoggerFactory.getLogger(BudgetPreLineEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EfinBudgPrepLines budgrevline = (EfinBudgPrepLines) event.getTargetInstance();

      final Property UniqueCode = entities[0].getProperty(EfinBudgPrepLines.PROPERTY_UNIQUECODE);// getting
                                                                                                 // current
                                                                                                 // entered
                                                                                                 // value
      Object currentUniqueCode = event.getCurrentState(UniqueCode);
      Object previousUniqueCode = event.getPreviousState(UniqueCode);
      if (currentUniqueCode != previousUniqueCode) {
        OBQuery<EfinBudgPrepLines> budgrevchk = OBDal.getInstance().createQuery(
            EfinBudgPrepLines.class,
            " efinBudgetPreparation.id ='" + budgrevline.getEfinBudgetPreparation().getId()
                + "' and client.id = '" + budgrevline.getClient().getId() + "' and uniqueCode='"
                + budgrevline.getUniqueCode() + "'"); // getting already saved values

        if (budgrevchk.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetPreLine_Error_Msg"));

        }
      }

    } catch (OBException e) {

      log.error(" Exception while updating line in Budget Preparation: " + e);
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
      EfinBudgPrepLines budgrevline = (EfinBudgPrepLines) event.getTargetInstance();

      OBQuery<EfinBudgPrepLines> budgrevchk = OBDal.getInstance().createQuery(
          EfinBudgPrepLines.class,
          " efinBudgetPreparation.id ='" + budgrevline.getEfinBudgetPreparation().getId()
              + "' and client.id = '" + budgrevline.getClient().getId() + "' and uniqueCode='"
              + budgrevline.getUniqueCode() + "'"); // getting already saved values
      if (budgrevchk.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_BudgetPreLine_Error_Msg"));

      }
    } catch (OBException e) {
      log.error(" Exception while creating line in Budget Preparation:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
