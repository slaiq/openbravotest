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

import sa.elm.ob.utility.EUT_ListAccess;

public class EutListAccessEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EUT_ListAccess.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EUT_ListAccess listaccess = (EUT_ListAccess) event.getTargetInstance();
      OBQuery<EUT_ListAccess> listquery = OBDal.getInstance().createQuery(EUT_ListAccess.class,
          " window.id=:winID and reference.id=:refID and listReference.id=:listRefID and role.id =:roleID");
      listquery.setNamedParameter("winID", listaccess.getWindow().getId());
      listquery.setNamedParameter("refID", listaccess.getReference().getId());
      listquery.setNamedParameter("listRefID", listaccess.getListReference().getId());
      listquery.setNamedParameter("roleID", listaccess.getRole().getId());

      if (listquery.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Listaccess_Unique"));
      }

    } catch (OBException e) {
      log.error("exception while creating listaccess", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating listaccess", e);
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
      EUT_ListAccess listaccess = (EUT_ListAccess) event.getTargetInstance();

      final Property window = entities[0].getProperty(EUT_ListAccess.PROPERTY_WINDOW);
      final Property reference = entities[0].getProperty(EUT_ListAccess.PROPERTY_REFERENCE);
      final Property listreference = entities[0].getProperty(EUT_ListAccess.PROPERTY_LISTREFERENCE);

      if (!event.getCurrentState(window).equals(event.getPreviousState(window))
          || !event.getCurrentState(reference).equals(event.getPreviousState(reference))
          || !event.getCurrentState(listreference).equals(event.getPreviousState(listreference))) {

        OBQuery<EUT_ListAccess> listquery = OBDal.getInstance().createQuery(EUT_ListAccess.class,
            " window.id=:winID and reference.id=:refID and listReference.id=:listRefID and role.id =:roleID ");
        listquery.setNamedParameter("winID", listaccess.getWindow().getId());
        listquery.setNamedParameter("refID", listaccess.getReference().getId());
        listquery.setNamedParameter("listRefID", listaccess.getListReference().getId());
        listquery.setNamedParameter("roleID", listaccess.getRole().getId());
        if (listquery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Listaccess_Unique"));
        }

      }
    } catch (OBException e) {
      log.error("exception while updating listaccess", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating listaccess", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
