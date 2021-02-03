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

import sa.elm.ob.scm.ESCMDefLookupsType;

public class DefineLookupsEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMDefLookupsType.ENTITY_NAME) };

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
      ESCMDefLookupsType lookup = (ESCMDefLookupsType) event.getTargetInstance();

      OBQuery<ESCMDefLookupsType> typelist = OBDal.getInstance()
          .createQuery(ESCMDefLookupsType.class,
              " reference='" + lookup.getReference() + "' and client.id='"
                  + lookup.getClient().getId() + "' and organization.id='"
                  + lookup.getOrganization().getId() + "'");
      log.debug("typelist.size" + typelist.list().size());
      if (typelist.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Org_Ref_Lookup_Unique"));
      }

    } catch (OBException e) {
      log.debug("exception while creating lookup" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating lookup" + e);
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
      ESCMDefLookupsType lookup = (ESCMDefLookupsType) event.getTargetInstance();

      final Property reference = entities[0].getProperty(ESCMDefLookupsType.PROPERTY_REFERENCE);
      final Property organization = entities[0]
          .getProperty(ESCMDefLookupsType.PROPERTY_ORGANIZATION);
      if (!event.getCurrentState(reference).equals(event.getPreviousState(reference))
          || !event.getCurrentState(organization).equals(event.getPreviousState(organization))) {

        OBQuery<ESCMDefLookupsType> typelist = OBDal.getInstance().createQuery(
            ESCMDefLookupsType.class,
            " reference='" + lookup.getReference() + "' and client.id='"
                + lookup.getClient().getId() + "' and organization.id='"
                + lookup.getOrganization().getId() + "'");
        log.debug("typelist.size" + typelist.list().size());
        if (typelist.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Org_Ref_Lookup_Unique"));
        }

      }
    } catch (OBException e) {
      log.debug("exception while updating lookup" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while updating lookup" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
