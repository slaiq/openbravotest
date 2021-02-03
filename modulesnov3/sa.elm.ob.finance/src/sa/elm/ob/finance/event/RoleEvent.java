package sa.elm.ob.finance.event;

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
import org.openbravo.model.ad.domain.List;

public class RoleEvent extends EntityPersistenceEventObserver {
  /*
   * 
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
      Property deptHead = entities[0].getProperty(Role.PROPERTY_EFINDEPARTMENTHEAD);
      Property orgBCUManager = entities[0].getProperty(Role.PROPERTY_EFINORGBCUMANGER);
      String status = "", refname = null;
      if (role.isEfinDepartmenthead() != event.getPreviousState(deptHead)) {
        if (role.isEfinDepartmenthead() == true) {
          OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
              "efinDepartmenthead='Y'");
          if (result.list() != null && result.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_DeptHead"));
          }
        }
      }

      // check Org Manager / Org Budget Manager already exists in client or not
      if (event.getCurrentState(orgBCUManager) != null
          && !event.getCurrentState(orgBCUManager).equals(event.getPreviousState(orgBCUManager))) {
        if (role.getEfinOrgbcumanger() != null) {
          OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
              "efinOrgbcumanger='" + role.getEfinOrgbcumanger() + "'");
          if (result.list() != null && result.list().size() > 0) {

            OBQuery<List> reflist = OBDal.getInstance().createQuery(List.class,
                " as e where e.reference.id='2AAFBB18E3144C50A7F237935649847F' and e.searchKey='"
                    + role.getEfinOrgbcumanger() + "'");
            if (reflist.list().size() > 0) {
              refname = reflist.list().get(0).getName();
            }
            status = OBMessageUtils.messageBD("EFIN_RoleOrgBCUMangExists");
            status = status.replaceAll("%", refname);
            throw new OBException(status);
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Department head in Role window: " + e);
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
      Role role = (Role) event.getTargetInstance();
      String status = "", refname = null;
      if (role.isEfinDepartmenthead() == true) {
        OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
            "efinDepartmenthead='Y'");
        if (result.list() != null && result.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_DeptHead"));
        }
      }

      // check Org Manager / Org Budget Manager already exists in client or not
      if (role.getEfinOrgbcumanger() != null) {
        OBQuery<Role> result = OBDal.getInstance().createQuery(Role.class,
            "efinOrgbcumanger='" + role.getEfinOrgbcumanger() + "'");
        if (result.list() != null && result.list().size() > 0) {

          OBQuery<List> reflist = OBDal.getInstance().createQuery(List.class,
              " as e where e.reference.id='2AAFBB18E3144C50A7F237935649847F' and e.searchKey='"
                  + role.getEfinOrgbcumanger() + "'");
          if (reflist.list().size() > 0) {
            refname = reflist.list().get(0).getName();
          }
          status = OBMessageUtils.messageBD("EFIN_RoleOrgBCUMangExists");
          status = status.replaceAll("%", refname);
          throw new OBException(status);
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating Department head in Role window: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
