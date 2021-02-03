package sa.elm.ob.utility.event;

import java.util.List;

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

public class DelegationRoleEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Role.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    List<Role> DelegationroleList = null;
    try {
      OBContext.setAdminMode();
      Role Delegationrole = (Role) event.getTargetInstance();
      final Property ismanual = entities[0].getProperty(Role.PROPERTY_MANUAL);
      if (Delegationrole.getEutDocumenttype() != null) {
        event.setCurrentState(ismanual, true);
      }
      OBQuery<Role> Delegationroleobj = OBDal.getInstance().createQuery(Role.class,
          "as e where e.client.id=:clientId and eutDocumenttype is not null and eutDocumenttype=:doctype");
      Delegationroleobj.setNamedParameter("clientId", Delegationrole.getClient().getId());
      Delegationroleobj.setNamedParameter("doctype", Delegationrole.getEutDocumenttype());
      DelegationroleList = Delegationroleobj.list();
      // To check whether same Document Type is used in some other record.
      if (DelegationroleList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("EUT_DOCTYPE_UNIQUE"));
      }
    } catch (OBException e) {
      log.error("exception while creating listaccess", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    List<Role> DelegationroleList = null;
    try {
      OBContext.setAdminMode();
      Role Delegationrole = (Role) event.getTargetInstance();
      final Property ismanual = entities[0].getProperty(Role.PROPERTY_MANUAL);
      if (Delegationrole.getEutDocumenttype() != null) {
        event.setCurrentState(ismanual, true);
      }
      OBQuery<Role> Delegationroleobj = OBDal.getInstance().createQuery(Role.class,
          "as e where e.client.id=:clientId and e.id <>:currentId and eutDocumenttype is not null and eutDocumenttype=:doctype");
      Delegationroleobj.setNamedParameter("clientId", Delegationrole.getClient().getId());
      Delegationroleobj.setNamedParameter("currentId", Delegationrole.getId());
      Delegationroleobj.setNamedParameter("doctype", Delegationrole.getEutDocumenttype());
      DelegationroleList = Delegationroleobj.list();
      // To check whether same Document Type is used in some other record.
      if (DelegationroleList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("EUT_DOCTYPE_UNIQUE"));
      }
    } catch (OBException e) {
      log.error("exception while updating listaccess", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
