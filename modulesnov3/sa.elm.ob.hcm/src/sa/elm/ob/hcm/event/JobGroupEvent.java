package sa.elm.ob.hcm.event;

import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.hcm.EhcmJobGroup;
import sa.elm.ob.hcm.Jobs;

/**
 * 
 * @author Gopalakrishnan on 12/10/2016
 * 
 */

public class JobGroupEvent extends EntityPersistenceEventObserver {
  /**
   * Business Event on Table Ehcm_job_group
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EhcmJobGroup.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EhcmJobGroup objJobGrp = (EhcmJobGroup) event.getTargetInstance();
      final Property enabled = entities[0].getProperty(Jobs.PROPERTY_ACTIVE);
      final Property endDate = entities[0].getProperty(Jobs.PROPERTY_ENDDATE);
      if (event.getCurrentState(endDate) != null) {
        event.setCurrentState(enabled, false);
      }
      Date startDate = objJobGrp.getStartDate();
      Date enddate = objJobGrp.getEndDate();

      if (startDate != null && enddate != null) {
        if (enddate.compareTo(startDate) == -1) {
          throw new OBException(OBMessageUtils.messageBD("Ehcm_Enddate"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating JobGroups   ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
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
      EhcmJobGroup objJobGrp = (EhcmJobGroup) event.getTargetInstance();
      final Property enabled = entities[0].getProperty(Jobs.PROPERTY_ACTIVE);
      final Property endDate = entities[0].getProperty(Jobs.PROPERTY_ENDDATE);
      if (event.getCurrentState(endDate) != null) {
        event.setCurrentState(enabled, false);
      }
      Date startDate = objJobGrp.getStartDate();
      Date enddate = objJobGrp.getEndDate();

      if (startDate != null && enddate != null) {
        if (enddate.compareTo(startDate) == -1) {
          throw new OBException(OBMessageUtils.messageBD("Ehcm_Enddate"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating JobGroups   ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
