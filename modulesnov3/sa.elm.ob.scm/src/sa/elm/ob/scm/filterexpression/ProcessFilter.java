package sa.elm.ob.scm.filterexpression;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;

public class ProcessFilter implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(ProcessFilter.class);

  @SuppressWarnings("unused")
  @Override
  public String getExpression(Map<String, String> requestMap) {
    // TODO Auto-generated method stub
    String strCurrentParam = "";
    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");
      log4j.debug("strCurrentParam:" + strCurrentParam);
      log4j.debug("strCurrentParam:" + requestMap);
      // JSONObject orgId = context.get("inpadOrgId");
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      Connection conn = OBDal.getInstance().getConnection();
      String DateQuery = "select extract(year from now())||'' as DefaultValue from dual";
      String orgQuery = " select o.ad_org_id as DefaultValue from ad_org o join ad_orgtype a on a.ad_orgtype_id=o.ad_orgtype_id "
          + " and a.istransactionsallowed='Y' where o.ad_org_id in (select ad_org_id "
          + "from ad_role_orgaccess " + " where ad_role_id ='" + vars.getRole() + "') "
          + " and o.ad_client_id='6522D733E2334B11A4706BA51CB597D3' and o.isready='Y' "
          + " order by o.name desc limit 1";

      // Get the Prefix
      if (strCurrentParam.equals("prefix")) {
        try {
          ps = conn.prepareStatement(DateQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (ps != null)
              ps.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in ProcessFilter ", e);
          }
        }
      }
      if (strCurrentParam.equals("ad_org_id")) {
        try {
          ps = conn.prepareStatement(orgQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

    } catch (JSONException e) {
      // TODO Auto-generated catch block
      log4j.debug("Error getting the default value of Organization" + strCurrentParam + " "
          + e.getMessage());
      return null;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return null;
  }
}
