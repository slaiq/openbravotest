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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.Region;

import sa.elm.ob.scm.EscmLocation;
import sa.elm.ob.scm.event.dao.CountryRegionLocationEventDAO;

/**
 * @author Priyanka Ranjan on 21/02/2018
 */

// Location Event
public class CountryRegionLocationEvent extends EntityPersistenceEventObserver {
  private static final Logger LOG = Logger.getLogger(CountryRegionLocationEvent.class);

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmLocation.ENTITY_NAME) };

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
      EscmLocation location = (EscmLocation) event.getTargetInstance();
      final Property locRegion = entities[0].getProperty(EscmLocation.PROPERTY_REGION);
      int count = 0;
      String regionId = "";
      CountryRegionLocationEventDAO dao = new CountryRegionLocationEventDAO();

      if (location.getRegion() != null) {
        Region region = OBDal.getInstance().get(Region.class, location.getRegion().getId());
        // check region is linked with any city or not, if not linked then not allow to save
        // location under region tab
        count = dao.getCityWithRegion(region.getCountry().getId(), location.getRegion().getId(),
            location.getClient().getId());
        if (count == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NoCity_ForRegion"));
        }
      }
      // set region in location tab under city tab
      if (location.getCity() != null) {
        regionId = dao.getRegion(location.getCity().getId(), location.getClient().getId());

        if (!regionId.isEmpty()) {
          Region region = OBDal.getInstance().get(Region.class, regionId);
          event.setCurrentState(locRegion, region);
        }
      }

    } catch (Exception e) {
      LOG.error(" Exception while creating Location: ", e);
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
      EscmLocation location = (EscmLocation) event.getTargetInstance();
      boolean islinked = false;
      CountryRegionLocationEventDAO dao = new CountryRegionLocationEventDAO();

      // Restrict to allow to Deactivate the location if location is linked with any organization
      if (!location.isActive()) {
        islinked = dao.checklocationlinkedwithorg(location.getId(), location.getClient().getId());
        if (islinked) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CannotDeactivate_Location"));
        }
      }
    } catch (Exception e) {
      LOG.error(" Exception while updating Location: ", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
