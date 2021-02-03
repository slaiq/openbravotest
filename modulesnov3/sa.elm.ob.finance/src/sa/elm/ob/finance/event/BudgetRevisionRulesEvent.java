/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudgetrevrules;

/**
 * @author Priyanka Ranjan on 14/09/2017
 * 
 */
// Handle the events in Budget Revision Rules window

public class BudgetRevisionRulesEvent extends EntityPersistenceEventObserver {
  // get entities of Budget Revision Rules window
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINBudgetrevrules.ENTITY_NAME) };

  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevisionRulesEvent.class);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EFINBudgetrevrules budrevrule = (EFINBudgetrevrules) event.getTargetInstance();
      final Property enablebudrule = entities[0]
          .getProperty(EFINBudgetrevrules.PROPERTY_ENABLEBUDGETRULE);
      Object currentEnable = event.getCurrentState(enablebudrule);
      Object previousEnable = event.getPreviousState(enablebudrule);
      String operator = budrevrule.getOperators();

      // If Enable Budget Rules is Y then Operator and Percentage fields are mandatory
      if (!currentEnable.equals(previousEnable) && budrevrule.isEnableBudgetRule()) {
        if (StringUtils.isEmpty(operator) || budrevrule.getPercentage() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Operator_Percentage_mandatory"));
        }
      }
    } catch (OBException e) {
      LOG.error(" Exception while updating Budget Revision Rules: " + e, e);
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
      EFINBudgetrevrules budrevrule = (EFINBudgetrevrules) event.getTargetInstance();
      String operator = budrevrule.getOperators();

      // If Enable Budget Rules is Y then Operator and Percentage fields are mandatory
      if (budrevrule.isEnableBudgetRule()) {
        if (StringUtils.isEmpty(operator) || budrevrule.getPercentage() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Operator_Percentage_mandatory"));
        }
      }
    } catch (OBException e) {
      LOG.error(" Exception while creating record in Budget Revision Rules: " + e, e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
