package sa.elm.ob.scm.event.dao;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmLocation;

/**
 * @author Priyanka Ranjan on 21/02/2018
 */

// City Event DAO
public class CityEventDAO {
  private static final Logger LOG = LoggerFactory.getLogger(CityEventDAO.class);

  /**
   * 
   * @param cityId
   * @param clientId
   * @return location count
   */
  public static int getLocationForCity(String cityId, String clientId) {
    int locationcount = 0;
    try {

      OBQuery<EscmLocation> loc = OBDal.getInstance().createQuery(EscmLocation.class,
          " as e where e.city.id='" + cityId + "' and e.client.id = '" + clientId + "'");
      if (loc.list() != null && loc.list().size() > 0) {
        locationcount = loc.list().size();
      }
    } catch (OBException e) {
      LOG.error("Exception while getLocationForCity:" + e);
      throw new OBException(e.getMessage());
    }
    return locationcount;
  }
}