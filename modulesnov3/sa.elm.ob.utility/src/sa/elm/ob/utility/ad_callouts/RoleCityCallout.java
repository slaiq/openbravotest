package sa.elm.ob.utility.ad_callouts;

import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.geography.City;

/**
 * 
 * @author Gokul 27/03/2020
 *
 */
public class RoleCityCallout extends SimpleCallout {
  /**
   * Callout for Region and city access in role.
   */
  private static Logger log = Logger.getLogger(RoleCityCallout.class);

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpCity = vars.getStringParameter("inpeutCCityId");
    String inpRegion = vars.getStringParameter("inpeutCRegionId");
    try {
      OBContext.setAdminMode();
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EUT_C_Region_ID').enable()");
      if (inpLastFieldChanged.equals("inpeutCCityId")) {
        if (inpCity != null && !inpCity.equals("")) {
          String regionId = RoleCityCalloutDAO.getRegion(inpCity);
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EUT_C_Region_ID').disable()");
          if (regionId != null) {
            info.addResult("inpeutCRegionId", regionId);
            List<City> cityList = RoleCityCalloutDAO.getCityforSelection(regionId);
            if (cityList.size() > 0) {
              info.addSelect("inpeutCCityId");
              for (City city : cityList) {
                info.addSelectResult(city.getId(), city.getName());
              }
              info.endSelect();
            }
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EUT_C_City_ID').setValue('" + inpCity + "');");

          }
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EUT_C_Region_ID').enable()");
        }
      }
      if (inpLastFieldChanged.equals("inpeutCRegionId")) {
        if (inpRegion != null && !inpRegion.equals("")) {
          List<City> cityList = RoleCityCalloutDAO.getCityforSelection(inpRegion);
          if (cityList.size() > 0) {
            info.addSelect("inpeutCCityId");
            for (City city : cityList) {
              info.addSelectResult(city.getId(), city.getName());
            }
            info.endSelect();
          } else {
            info.addSelect("inpeutCCityId");
            info.endSelect();
          }
        } else {
          List<City> cityList = RoleCityCalloutDAO.getCityforSelection(inpRegion);
          if (cityList.size() > 0) {
            info.addSelect("inpeutCCityId");
            for (City city : cityList) {
              info.addSelectResult(city.getId(), city.getName());
            }
            info.endSelect();
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in RoleCityCallout:", e);
    }
  }
}
