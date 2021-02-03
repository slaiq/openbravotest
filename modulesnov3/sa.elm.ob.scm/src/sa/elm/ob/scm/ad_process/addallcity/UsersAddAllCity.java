package sa.elm.ob.scm.ad_process.addallcity;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.geography.City;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.utility.EUT_User_Regandcity_Access;

/**
 * @author Priyanka.C
 */

//
public class UsersAddAllCity implements Process {
  /**
   * This class is used to add all cities of country saudi arabia in User - Region and City Access
   * which are not added already
   */

  private static Logger log = Logger.getLogger(UsersAddAllCity.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      // Variable declaration
      final String userId = bundle.getParams().get("AD_User_ID").toString();
      User user = OBDal.getInstance().get(User.class, userId);

      if (user != null) {
        OBQuery<City> city = OBDal.getInstance().createQuery(City.class,
            " as e where country.id in (select id from Country where iSOCountryCode='SA') and c_city_id not in "
                + "(select city.id from EUT_User_Regandcity_Access where userContact.id=:userId)");
        city.setNamedParameter("userId", user.getId());
        List<City> cityList = city.list();
        for (City cityobj : cityList) {
          EUT_User_Regandcity_Access newCityAccess = OBProvider.getInstance()
              .get(EUT_User_Regandcity_Access.class);

          newCityAccess.setCreationDate(new java.util.Date());
          newCityAccess.setCreatedBy(user);
          newCityAccess.setUpdated(new java.util.Date());
          newCityAccess.setUpdatedBy(user);
          newCityAccess.setUserContact(user);
          newCityAccess.setCity(cityobj);
          newCityAccess.setRegion(cityobj.getRegion());
          OBDal.getInstance().save(newCityAccess);
        }
        OBDal.getInstance().flush();
        String message = OBMessageUtils.messageBD("ESCM_Add_City_Msg");
        OBError result = OBErrorBuilder.buildMessage(null, "success", message);
        bundle.setResult(result);
      }
    } catch (Exception e) {
      log.error("Exeception in UsersAddAllCity Process:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
