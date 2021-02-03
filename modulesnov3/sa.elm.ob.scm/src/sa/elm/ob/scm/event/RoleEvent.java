package sa.elm.ob.scm.event;

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
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;

/**
 * 
 * 
 * @author sathish kumar.p
 *
 */

public class RoleEvent extends EntityPersistenceEventObserver {

  /**
   * This Class is used to handle the events in ad_role table
   */

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Role.ENTITY_NAME) };

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
      Role role = (Role) event.getTargetInstance();
      Property isHrmanager = entities[0].getProperty(Role.PROPERTY_ESCMISHRLINEMANAGER);
      Property isSpecialDpt = entities[0].getProperty(Role.PROPERTY_ESCMISSPECIALIZEDDEPT);

      if (event.getCurrentState(isHrmanager) != event.getPreviousState(isHrmanager)) {
        if (role.isEscmIshrlinemanager() != null && role.isEscmIshrlinemanager()) {
          OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
              "escmIshrlinemanager='Y' and  client.id =:clientID");
          result.setNamedParameter("clientID", OBContext.getOBContext().getCurrentClient().getId());
          if (result.list() != null && result.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_onlyoneHrRole"));
          }
          if (role.getADUserRolesList() != null && role.getADUserRolesList().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NoUser_HrRole"));
          }
        }
      }
      // For client only one role can be selected as Specialized dept role
      if (event.getCurrentState(isSpecialDpt) != event.getPreviousState(isSpecialDpt)) {
        if (role.isEscmIsspecializeddept() != null && role.isEscmIsspecializeddept()) {
          OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
              "escmIsspecializeddept='Y' and  client.id =:clientID");
          result.setNamedParameter("clientID", OBContext.getOBContext().getCurrentClient().getId());
          if (result.list() != null && result.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_onlyoneSpclDeptRole"));
          }
          if (role.getADUserRolesList() != null && role.getADUserRolesList().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NoUser_SpclDeptRole"));
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Role  ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating Role  ", e);
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
      Role role = (Role) event.getTargetInstance();
      if (role.isEscmIshrlinemanager() != null && role.isEscmIshrlinemanager()) {
        OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
            "escmIshrlinemanager='Y' and  client.id =:clientID");
        result.setNamedParameter("clientID", OBContext.getOBContext().getCurrentClient().getId());
        if (result.list() != null && result.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_onlyoneHrRole"));
        }
      }
      // For client only one role can be selected as Specialized dept role
      if (role.isEscmIsspecializeddept() != null && role.isEscmIsspecializeddept()) {
        OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
            "escmIsspecializeddept='Y' and  client.id =:clientID");
        result.setNamedParameter("clientID", OBContext.getOBContext().getCurrentClient().getId());
        if (result.list() != null && result.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_onlyoneSpclDeptRole"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating Department head in Role window: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating Department head in Role window: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
