package sa.elm.ob.scm.event.dao;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Priyanka Ranjan on 21/02/2018
 */

// Location Event DAO
public class CountryRegionLocationEventDAO implements EscmLocationEventDAO {
  private static final Logger LOG = LoggerFactory.getLogger(CountryRegionLocationEventDAO.class);

  /**
   * 
   * @param countryId
   * @param regionId
   * @param clientId
   * @return city count
   */
  public int getCityWithRegion(String countryId, String regionId, String clientId) {
    int citycount = 0;
    try {

      OBQuery<City> city = OBDal.getInstance().createQuery(City.class,
          " as e where e.region.id=:regionID and e.country.id =:countryId");
      city.setNamedParameter("regionID", regionId);
      city.setNamedParameter("countryId", countryId);

      List<City> cityList = city.list();
      if (cityList != null && cityList.size() > 0) {
        citycount = cityList.size();
      }
    } catch (OBException e) {
      LOG.error("Exception while getCityWithRegion:" + e);
      throw new OBException(e.getMessage());
    }
    return citycount;
  }

  /**
   * 
   * @param cityId
   * @param clientId
   * @return regionId
   */
  public String getRegion(String cityId, String clientId) {
    String regionId = "";
    try {

      OBQuery<City> city = OBDal.getInstance().createQuery(City.class, " as e where e.id=:cityID");
      city.setNamedParameter("cityID", cityId);
      List<City> cityList = city.list();
      if (cityList != null && cityList.size() > 0) {
        if (cityList.get(0).getRegion() != null && !cityList.get(0).getRegion().getId().isEmpty()) {
          regionId = cityList.get(0).getRegion().getId();
        } else {
          regionId = "";
        }

      }
    } catch (OBException e) {
      LOG.error("Exception while getRegion:", e);
      throw new OBException(e.getMessage());
    }
    return regionId;
  }

  /**
   * 
   * @param locationId
   * @param clientId
   * @return true/false
   */
  public boolean checklocationlinkedwithorg(String locationId, String clientId) {
    List<Organization> ls = new ArrayList<Organization>();
    try {
      OBQuery<Organization> count = OBDal.getInstance().createQuery(Organization.class,
          " as e where e.ehcmEscmLoc.id=:locId ");
      count.setNamedParameter("locId", locationId);

      ls = count.list();
      if (ls.size() > 0) {
        return true;
      }
    } catch (Exception e) {
      LOG.error("error while checklocationlinkedwithorg", e);
      return false;
    }
    return false;
  }
}