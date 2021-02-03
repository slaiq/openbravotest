package sa.elm.ob.scm.ad_forms.updatesequence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.DocumentSequence;

public class UpdateSequenceDAO {
  private static Logger log4j = Logger.getLogger(UpdateSequenceDAO.class);
  private Connection connection = null;

  public UpdateSequenceDAO(Connection conn) {
    this.connection = conn;
  }

  public JSONObject getPrefix(String clientId) {
    final JSONObject prefixObj = new JSONObject();
    PreparedStatement st = null;
    ResultSet rs = null;

    try {
      String sql = "select extract(year from now())||'' as DefaultValue from dual";

      st = connection.prepareStatement(sql);
      rs = st.executeQuery();
      if (rs.next()) {
        prefixObj.put("Prefix", rs.getString("DefaultValue"));
      }
    } catch (Exception e) {
      log4j.error("Exception in getPrefix:", e);
      return prefixObj;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
      }
    }
    return prefixObj;
  }

  public JSONObject getOrg(String clientId, String roleId) {
    final JSONObject orgObj = new JSONObject();
    PreparedStatement st = null;
    ResultSet rs = null;

    try {
      String sql = " select o.ad_org_id as DefaultValue from ad_org o join ad_orgtype a on a.ad_orgtype_id=o.ad_orgtype_id "
          + " and a.istransactionsallowed='Y' where o.ad_org_id in (select ad_org_id "
          + "from ad_role_orgaccess " + " where ad_role_id ='" + roleId + "') "
          + " and o.ad_client_id='" + clientId + "'  "// and o.isready='Y'
          + " order by o.name desc limit 1";

      st = connection.prepareStatement(sql);
      rs = st.executeQuery();
      if (rs.next()) {
        orgObj.put("Organization", rs.getString("DefaultValue"));
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrg :", e);
      return orgObj;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
      }
    }
    return orgObj;
  }

  @SuppressWarnings("resource")
  public synchronized JSONObject getOrgList(String clientId, String roleId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(org.ad_org_id) as count  ");
      selectQuery.append(" select org.ad_org_id, org.name, org.value ");
      fromQuery.append(
          " from ad_org org left join ad_orgtype orgtyp on orgtyp.ad_orgtype_id=org.ad_orgtype_id "
              + " where org.ad_org_id<>'0' and org.ad_org_id in (select ad_org_id from ad_role_orgaccess rol where rol.ad_role_id='"
              + roleId + "') " + " and istransactionsallowed='Y' and isready='Y' ");

      /*
       * if (searchTerm != null && !searchTerm.equals("")) fromQuery.append(" and name ilike '%" +
       * searchTerm.toLowerCase() + "%'");
       */

      st = connection.prepareStatement(countQuery.append(fromQuery).toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = connection.prepareStatement((selectQuery.append(fromQuery)).toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("OrgId", rs.getString("ad_org_id"));
          jsonData.put("OrgName", rs.getString("name"));
          jsonArray.put(jsonData);
        }
      }
      jsob.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getOrgList :", e);
      return jsob;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
      }
    }
    return jsob;
  }

  public JSONObject updateSequence(String orgId, String prefix) {
    Boolean errorFlag = Boolean.FALSE;
    JSONObject json = new JSONObject();
    try {
      OBQuery<DocumentSequence> sequenceQuery = OBDal.getInstance().createQuery(
          DocumentSequence.class,
          "as e where e.organization.id='" + orgId + "' and e.istransaction='Y'");
      if (sequenceQuery.list().size() > 0) {
        for (DocumentSequence objSequence : sequenceQuery.list()) {
          if (new BigDecimal(objSequence.getPrefix()).compareTo(new BigDecimal(prefix)) == 1) {
            errorFlag = Boolean.TRUE;
          }
          if (!errorFlag) {
            objSequence.setPrefix(prefix);
            // objSequence.setNextAssignedNumber(objSequence.getStartingNo());
            objSequence.setStartingNo(objSequence.getNextAssignedNumber());
            log4j.debug("setStartingNo>" + objSequence.getNextAssignedNumber());
            OBDal.getInstance().save(objSequence);
          }
        }
      }
      if (errorFlag) {
        json.put("severity", "error");
        json.put("text", OBMessageUtils.messageBD("ESCM_Update_Prefix(Error)"));
      } else {
        OBDal.getInstance().flush();
        json.put("severity", "success");
        json.put("text", OBMessageUtils.messageBD("ProcessOK"));
      }
      return json;
    } catch (Exception e) {
      log4j.error("Exception in  SequenceUpdate:", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    }

  }
}
