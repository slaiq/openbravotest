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
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.geography.City;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.utility.Eutregandcityaccess;

/**
 * @author Priyanka.C
 * @implNote This process is to copy all the data from the selected PO & create new PO, except
 *           Contract Category value
 */

//
public class AddAllCity implements Process {

  private static Logger log = Logger.getLogger(AddAllCity.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      // Variable declaration
      final String roleId = bundle.getParams().get("AD_Role_ID").toString();

      Role role = OBDal.getInstance().get(Role.class, roleId);
      final String userId = bundle.getContext().getUser();
      User user = OBDal.getInstance().get(User.class, userId);

      if (role != null) {
        OBQuery<City> city = OBDal.getInstance().createQuery(City.class,
            " as e where country.id in (select id from Country where iSOCountryCode='SA') and c_city_id not in "
                + "(select eUTCCity.id from eut_regandcity_access where role.id=:roleId)");
        city.setNamedParameter("roleId", role.getId());
        List<City> cityList = city.list();
        for (City cityobj : cityList) {
          Eutregandcityaccess newCityAccess = OBProvider.getInstance()
              .get(Eutregandcityaccess.class);

          newCityAccess.setCreationDate(new java.util.Date());
          newCityAccess.setCreatedBy(user);
          newCityAccess.setUpdated(new java.util.Date());
          newCityAccess.setUpdatedBy(user);
          newCityAccess.setRole(role);
          newCityAccess.setEUTCCity(cityobj);
          newCityAccess.setEUTCRegion(cityobj.getRegion());
          OBDal.getInstance().save(newCityAccess);
        }
        OBDal.getInstance().flush();
        String message = OBMessageUtils.messageBD("ESCM_Add_City_Msg");
        OBError result = OBErrorBuilder.buildMessage(null, "success", message);
        bundle.setResult(result);
      }
    } catch (Exception e) {
      log.error("Exeception in AddAllCity Process:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
