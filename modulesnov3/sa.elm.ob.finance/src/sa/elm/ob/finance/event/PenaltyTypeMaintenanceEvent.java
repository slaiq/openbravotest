/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

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

import sa.elm.ob.finance.EfinPenaltyTypes;

public class PenaltyTypeMaintenanceEvent extends EntityPersistenceEventObserver {
  private static final Logger LOG = LoggerFactory.getLogger(PenaltyTypeMaintenanceEvent.class);
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinPenaltyTypes.ENTITY_NAME) };

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
      EfinPenaltyTypes penalty = (EfinPenaltyTypes) event.getTargetInstance();
      final Property thershold = entities[0].getProperty(EfinPenaltyTypes.PROPERTY_THRESHOLD);
      /*
       * if (penalty.getThreshold() != null) { if (!penalty.isPenaltyOverride()) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Penalty_Man")); } }
       */
      if (event.getCurrentState(thershold) != null
          && !event.getCurrentState(thershold).equals(event.getPreviousState(thershold))) {
        if (penalty.getThreshold().compareTo(BigDecimal.ZERO) == 0) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_PenThreshold"));
        }
      }
    } catch (OBException e) {
      LOG.error(" Exception while updating PenaltyType Control: " + e, e);
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
      EfinPenaltyTypes penalty = (EfinPenaltyTypes) event.getTargetInstance();
      /*
       * if (penalty.getThreshold() != null) { if (!penalty.isPenaltyOverride()) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Penalty_Man")); } }
       */
      if ((penalty.getThreshold() != null)
          && penalty.getThreshold().compareTo(BigDecimal.ZERO) == 0) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_PenThreshold"));
      }
    } catch (OBException e) {
      LOG.error(" Exception while creating PenaltyType Control: " + e, e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
