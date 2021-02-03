package sa.elm.ob.scm.event;

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
import org.openbravo.model.ad.access.User;

public class BusinessPartnerContactEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(User.ENTITY_NAME) };

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
      User usr = (User) event.getTargetInstance();
      validateFields(usr);
    } catch (OBException e) {
      log.error(" Exception while update contact ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while update contact ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
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
      User usr = (User) event.getTargetInstance();
      validateFields(usr);
    } catch (OBException e) {
      log.error(" Exception while save contact ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while save contact ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void validateFields(User usr) {
    try {
      String authNo = usr.getEscmAuthorizationNumber();
      if (usr.getBusinessPartner() != null && usr.getFirstName() == null
          && usr.getLastName() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_NameNotEmpty"));
      }
      if (authNo != null) {
        // check authNo has special characters
        boolean isValid = false;
        String regex = "\\d+";
        isValid = authNo.matches(regex);
        if (!isValid) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BPContactAuthNoNotValid"));
        }
      }
    } catch (OBException e) {
      log.error("exception in validateFields() while saving contact", e);
      throw new OBException(e.getMessage());
    }
  }
}
