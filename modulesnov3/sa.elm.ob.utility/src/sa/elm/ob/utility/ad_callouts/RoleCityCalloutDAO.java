package sa.elm.ob.utility.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleCityCalloutDAO {
  private static final Logger log = LoggerFactory.getLogger(RoleCityCalloutDAO.class);

  /**
   * Method To get the Region based on city selection
   * 
   * @param city
   * @return
   * @throws SQLException
   */
  public static String getRegion(String city) throws SQLException {
    String regionId = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select c_region_id from c_city where c_city_id = ? ");
      st.setString(1, city);
      rs = st.executeQuery();
      if (rs.next()) {
        regionId = rs.getString("c_region_id");
      }

    } catch (OBException e) {
      log.error("Exception while getRegion" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in getRegion() ", e);
      }
    }
    return regionId;
  }

  /**
   * Method To get the City based on Region selection
   * 
   * @param region
   * @return
   * @throws SQLException
   */
  public static String getCity(String region) throws SQLException {
    String regionId = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = OBDal.getInstance().getConnection()
          .prepareStatement("select c_city_id from c_city where c_region_id = ? ");
      st.setString(1, region);
      rs = st.executeQuery();
      while (rs.next()) {
        regionId = rs.getString("c_city_id");
      }

    } catch (OBException e) {
      log.error("Exception while getRegion" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in getCity() ", e);
      }
    }
    return regionId;
  }

  /**
   * Method To get the City based on Region selection
   * 
   * @param region
   * @return
   * @throws SQLException
   */
  public static List<City> getCityforSelection(String region) throws SQLException {
    try {
      OBContext.setAdminMode();
      if (StringUtils.isBlank(region)) {
        OBQuery<City> cityList = OBDal.getInstance().createQuery(City.class, " as e ");
        return cityList.list();
      } else {
        OBQuery<City> cityList = OBDal.getInstance().createQuery(City.class,
            " as e where e.region.id = :regionId");
        cityList.setNamedParameter("regionId", region);
        return cityList.list();
      }
    } catch (OBException e) {
      log.error("Exception while getCityforSelection" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
