package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.UserRoles;

/**
 * 
 * 
 * @author sathish kumar.p
 *
 */
public class UserRoleEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in AD_User_Roles table
   */

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(UserRoles.ENTITY_NAME) };

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
      UserRoles userRoles = (UserRoles) event.getTargetInstance();
      if (userRoles.getRole() != null) {
        if (userRoles.getRole().isEscmIshrlinemanager() != null
            && userRoles.getRole().isEscmIshrlinemanager()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_cannotsave_userHrrole"));
        }

        if (userRoles.getRole().isEscmIsspecializeddept() != null
            && userRoles.getRole().isEscmIsspecializeddept()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_cannotsave_userspcldeptrole"));
        }

      }

    } catch (OBException e) {
      log.error(" Exception while save user in role: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while save user in role: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
