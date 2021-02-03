package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.City;

import sa.elm.ob.scm.event.dao.CityEventDAO;

/**
 * @author Priyanka Ranjan on 21/02/2018
 */

// City Event
public class CityEvent extends EntityPersistenceEventObserver {
  private static final Logger LOG = Logger.getLogger(CityEvent.class);

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(City.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      City city = (City) event.getTargetInstance();
      int count = 0;
      if (city.getId() != null) {
        // not allow to delete city , if city is linked with any location
        count = CityEventDAO.getLocationForCity(city.getId(), city.getClient().getId());
        if (count > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CannotDelete_City"));
        }
      }

    } catch (Exception e) {
      LOG.error(" Exception while deleting city: ", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
