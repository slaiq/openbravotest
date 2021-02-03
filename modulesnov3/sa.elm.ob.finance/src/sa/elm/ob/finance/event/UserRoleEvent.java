package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.UserRoles;

public class UserRoleEvent extends EntityPersistenceEventObserver {

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
      String refname = null, status = null;
      if (userRoles.getRole() != null) {
        if (userRoles.getRole().getEfinOrgbcumanger() != null) {
          OBQuery<org.openbravo.model.ad.domain.List> reflist = OBDal.getInstance().createQuery(
              org.openbravo.model.ad.domain.List.class,
              " as e where e.reference.id='2AAFBB18E3144C50A7F237935649847F' and e.searchKey='"
                  + userRoles.getRole().getEfinOrgbcumanger() + "'");
          if (reflist.list().size() > 0) {
            refname = reflist.list().get(0).getName();
          }
          status = OBMessageUtils.messageBD("EFIN_cannotsave_OrgBCUMang");
          status = status.replaceAll("%", refname);
          throw new OBException(status);
        }
      }

    } catch (Exception e) {
      log.error(" Exception while save user in role: " + e);
      throw new OBException(e.getMessage());
    }
  }

  @SuppressWarnings("rawtypes")
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      UserRoles userRoles = (UserRoles) event.getTargetInstance();
      SQLQuery delegated = OBDal.getInstance().getSession()
          .createSQLQuery("select eut_docapp_delegateln_id from eut_docapp_delegateln ln "
              + " join eut_docapp_delegate hd on hd.eut_docapp_delegate_id = ln.eut_docapp_delegate_id "
              + " where ln.ad_role_id = '" + userRoles.getRole().getId() + "' and ln.ad_user_id = '"
              + userRoles.getUserContact().getId()
              + "' and now() between hd.from_date and hd.to_date");
      List count = delegated.list();
      if (count.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Userrole_Delete"));
      }
    } catch (Exception e) {
      log.error(" Exception while Delete role in user: " + e);
      throw new OBException(e.getMessage());
    }
  }
}
