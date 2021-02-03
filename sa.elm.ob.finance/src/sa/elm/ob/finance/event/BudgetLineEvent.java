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
import sa.elm.ob.finance.EFINBudgetLines;

public class BudgetLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINBudgetLines.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger log = LoggerFactory.getLogger(BudgetLineEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EFINBudgetLines budgline = (EFINBudgetLines) event.getTargetInstance();
      final Property amount = entities[0].getProperty(EFINBudgetLines.PROPERTY_AMOUNT);// getting
                                                                                       // current
                                                                                       // entered
                                                                                       // value
      Object currentAmount = event.getCurrentState(amount);
      Object previousAmount = event.getPreviousState(amount);
      final Property lineno = entities[0].getProperty(EFINBudgetLines.PROPERTY_LINENO);// getting
                                                                                       // current
                                                                                       // entered
                                                                                       // value
      Object currentlineno = event.getCurrentState(lineno);
      Object prevoiuslineno = event.getPreviousState(lineno);
      if (currentAmount != previousAmount) {
        if (budgline.getAmount().signum() < 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetLine_Amt"));
        }
      }

      // to check duplicate line number
      if (currentlineno != prevoiuslineno) {
        OBQuery<EFINBudgetLines> duplicate1 = OBDal.getInstance().createQuery(EFINBudgetLines.class,
            "lineNo = '" + budgline.getLineNo() + "'" + " and efinBudget.id='"
                + budgline.getEfinBudget().getId() + "'");
        if (duplicate1.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("@Efin_lineexist@"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating line in Budget: " + e);
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
      EFINBudgetLines budgetline = (EFINBudgetLines) event.getTargetInstance();

      if (budgetline.getAmount() != null && budgetline.getAmount().signum() < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budgetline_Error_Msg"));
      }
      // to check duplicate line number
      OBQuery<EFINBudgetLines> duplicate1 = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          "lineNo = '" + budgetline.getLineNo() + "'" + " and efinBudget.id='"
              + budgetline.getEfinBudget().getId() + "'");
      if (duplicate1.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("@Efin_lineexist@"));
      }
    } catch (OBException e) {
      log.error(" Exception while creating line in Budget Revision:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
