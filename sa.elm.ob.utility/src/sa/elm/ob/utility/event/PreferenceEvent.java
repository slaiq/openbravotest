package sa.elm.ob.utility.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;

public class PreferenceEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Preference.ENTITY_NAME) };
  private static String Disable_RMI = "EUT_DRMI";
  private static String Disable_Forward = "EUT_DFWD";
  private static String Attachment_Access = "EUT_Attachment_Process";

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Preference preferenceObj = (Preference) event.getTargetInstance();
      if (preferenceObj.getProperty() != null) {
        if (preferenceObj.getProperty().equals(Disable_RMI)
            || preferenceObj.getProperty().equals(Disable_Forward)) {
          if (preferenceObj.getWindow() != null || preferenceObj.getVisibleAtClient() != null
              || preferenceObj.getVisibleAtOrganization() != null
              || (preferenceObj.getVisibleAtRole() != null
                  && preferenceObj.getUserContact() == null))
            throw new OBException(OBMessageUtils.messageBD("EUT_Common_Preference"));
          if (preferenceObj.getVisibleAtRole() == null && preferenceObj.getUserContact() == null) {
            throw new OBException(OBMessageUtils.messageBD("EUT_roleUser_Check"));
          }

        }
      }
      // if the property as attachment then window is mandatory
      if (preferenceObj.getProperty() != null) {
        if (preferenceObj.getProperty().equals(Attachment_Access)) {
          if (preferenceObj.getWindow() == null) {
            throw new OBException(OBMessageUtils.messageBD("Eut_Preference_Window"));
          }
        }
      }
    } catch (OBException e) {
      log.error("exception while creating preference", e);
      throw new OBException(e.getMessage());
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
      Preference preferenceObj = (Preference) event.getTargetInstance();
      if (preferenceObj.getProperty() != null) {
        if (preferenceObj.getProperty().equals(Disable_RMI)
            || preferenceObj.getProperty().equals(Disable_Forward)) {
          if (preferenceObj.getWindow() != null || preferenceObj.getVisibleAtClient() != null
              || preferenceObj.getVisibleAtOrganization() != null
              || (preferenceObj.getVisibleAtRole() != null
                  && preferenceObj.getUserContact() == null))
            throw new OBException(OBMessageUtils.messageBD("EUT_Common_Preference"));
          if (preferenceObj.getVisibleAtRole() == null && preferenceObj.getUserContact() == null) {
            throw new OBException(OBMessageUtils.messageBD("EUT_roleUser_Check"));
          }
        }
        // if the property as attachment then window is mandatory
        if (preferenceObj.getProperty() != null) {
          if (preferenceObj.getProperty().equals(Attachment_Access)) {
            if (preferenceObj.getWindow() == null) {
              throw new OBException(OBMessageUtils.messageBD("Eut_Preference_Window"));
            }
          }
        }
      }
    } catch (OBException e) {
      log.error("exception while updating preference", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
