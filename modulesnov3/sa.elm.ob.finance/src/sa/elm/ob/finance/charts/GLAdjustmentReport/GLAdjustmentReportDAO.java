package sa.elm.ob.finance.charts.GLAdjustmentReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

/**
 * @author Priyanka Ranjan on 13/10/2016
 */

public class GLAdjustmentReportDAO {
  Connection conn = null;
  private static Logger log4j = Logger.getLogger(GLAdjustmentReportDAO.class);

  public GLAdjustmentReportDAO(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * 
   * @param inphDoctypelist
   * @param orgId
   * @param ClientId
   * @return DocumentNo List
   */
  public JSONArray getDocumentNo(String inphDoctypelist, String orgId, String ClientId) {
    JSONArray jArray = new JSONArray();
    SQLQuery query = null;
    StringBuilder sqlBuilder = null;
    JSONObject jsonObject = null;

    try {
      OBContext.setAdminMode();

      sqlBuilder = new StringBuilder();

      if (inphDoctypelist.equals("gi")) {
        sqlBuilder.append("select DocumentNo from gl_journal where  ad_client_id ='" + ClientId
            + "' and ad_org_id='" + orgId + "' and Posted='Y' order by DocumentNo ASC");

      } else if (inphDoctypelist.equals("apa")) {
        sqlBuilder.append(
            "select documentno from c_invoice inv left join c_doctype doctp on doctp.c_doctype_id=inv.c_doctypetarget_id where doctp.EM_Efin_Isprepayinvapp='Y' and inv.posted='Y' and DocStatus='CO' and inv.ad_org_id='"
                + orgId + "' and  inv.ad_client_id ='" + ClientId + "' order by documentno ASC");

      }

      else if (inphDoctypelist.equals("recon")) {
        sqlBuilder.append("select DocumentNo  from fin_reconciliation where ad_client_id ='"
            + ClientId + "' and  ad_org_id='" + orgId
            + "' and Docstatus='CO' and Posted='Y' order by DocumentNo ASC");

      }

      else if (inphDoctypelist.equals("prje")) {
        sqlBuilder.append(
            "select  EM_Efin_Document_No as DocumentNo  from fin_finacc_transaction where  ad_client_id ='"
                + ClientId + "' and ad_org_id='" + orgId
                + "' and posted='Y' and status='EFIN_CAN' order by EM_Efin_Document_No ASC");

      } else if (inphDoctypelist.equals("urje")) {
        sqlBuilder.append("select DocumentNo  from fin_reconciliation where  ad_client_id ='"
            + ClientId + "' and ad_org_id='" + orgId
            + "' and  Docstatus='EFIN_UREC' and Posted='Y' order by DocumentNo ASC");

      } else if (inphDoctypelist.equals("AR")) {
        sqlBuilder.append(
            "select distinct(em_efin_generalsequence) as DocumentNo  from fin_payment where  ad_client_id ='"
                + ClientId + "' and ad_org_id='" + orgId
                + "' and  posted ='Y' order by em_efin_generalsequence ASC");

      }
      log4j.debug("Sql Query :" + sqlBuilder.toString());
      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());
      if (query != null && query.list().size() > 0) {
        for (int i = 0; i < query.list().size(); i++) {
          jsonObject = new JSONObject();
          jsonObject.put("DocumentNo", query.list().get(i));

          jArray.put(jsonObject);
        }
      }
    } catch (Exception e) {
      log4j.debug("Exception in getDocumentNo :" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jArray;
  }

  /**
   * 
   * @param ClientId
   * @return OrganizationList
   */
  public JSONArray getOrganization(String ClientId) {
    JSONArray jArray = new JSONArray();
    try {
      String sql = "";
      PreparedStatement st = null;
      ResultSet rs = null;
      JSONObject jsonObject = null;

      sql = "select org.ad_org_id as id , org.name as orgname from ad_org org"
          + " left join AD_Orgtype orgtype on  org.AD_Orgtype_ID=orgtype.AD_Orgtype_ID"
          + " where org.AD_Orgtype_ID='1' and org.ad_client_id ='" + ClientId + "'";

      st = conn.prepareStatement(sql);
      rs = st.executeQuery();
      while (rs.next()) {
        jsonObject = new JSONObject();
        jsonObject.put("id", rs.getString("id"));
        jsonObject.put("orgname", rs.getString("orgname"));
        jArray.put(jsonObject);
      }

    } catch (Exception e) {
      log4j.debug("Exception getOrganization :" + e);
    }
    return jArray;
  }
}