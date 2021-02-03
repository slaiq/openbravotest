package sa.elm.ob.scm.filterexpression;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class RequisitionFilterExpression implements FilterExpression {

  @SuppressWarnings("unused")
  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strContext = requestMap.get("context");
    String strCurrentParam = "";
    Logger log4j = Logger.getLogger(RequisitionFilterExpression.class);

    try {
      JSONObject context = new JSONObject(strContext);

      // JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");
      String strDateQuery = "select eut_convert_to_hijri(to_char(now(),'YYYY-MM-DD')) as DefaultValue";
      if (strCurrentParam.equals("canceldate")) {
        try {
          SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(strDateQuery);
          if (query != null && query.list().size() > 0) {
            return (String) query.list().get(0);
          }
          // return cal.toString();
        } catch (Exception e) {
          log4j.debug("error while getting current Date " + e.getMessage());
          e.printStackTrace();
        }
      }

      if (strCurrentParam.equals("ad_user_id")) {
        return OBContext.getOBContext().getUser().getName();
      }

    } catch (Exception e) {
      log4j.debug("Error getting the default value for requisition cancel" + strCurrentParam + " "
          + e.getMessage());
      e.printStackTrace();
      return null;
    } finally {

    }
    return null;
  }
}