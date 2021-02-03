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
 * @author Divya 29/09/2020
 *
 */
public class UserCityCallout extends SimpleCallout {
  /**
   * Callout for Region and city access in user.
   */
  private static Logger log = Logger.getLogger(UserCityCallout.class);

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpCity = vars.getStringParameter("inpcCityId");
    String inpRegion = vars.getStringParameter("inpcRegionId");
    try {
      OBContext.setAdminMode();
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Region_ID').enable()");
      if (inpLastFieldChanged.equals("inpcCityId")) {
        if (inpCity != null && !inpCity.equals("")) {
          String regionId = RoleCityCalloutDAO.getRegion(inpCity);
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Region_ID').disable()");
          if (regionId != null) {
            info.addResult("inpcRegionId", regionId);
            List<City> cityList = RoleCityCalloutDAO.getCityforSelection(regionId);
            if (cityList.size() > 0) {
              info.addSelect("inpcCityId");
              for (City city : cityList) {
                info.addSelectResult(city.getId(), city.getName());
              }
              info.endSelect();
            }
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('C_City_ID').setValue('" + inpCity + "');");

          }
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Region_ID').enable()");
        }
      }
      if (inpLastFieldChanged.equals("inpcRegionId")) {
        if (inpRegion != null && !inpRegion.equals("")) {
          List<City> cityList = RoleCityCalloutDAO.getCityforSelection(inpRegion);
          if (cityList.size() > 0) {
            info.addSelect("inpcCityId");
            for (City city : cityList) {
              info.addSelectResult(city.getId(), city.getName());
            }
            info.endSelect();
          } else {
            info.addSelect("inpcCityId");
            info.endSelect();
          }
        } else {
          List<City> cityList = RoleCityCalloutDAO.getCityforSelection(inpRegion);
          if (cityList.size() > 0) {
            info.addSelect("inpcCityId");
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
