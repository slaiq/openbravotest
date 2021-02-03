package sa.elm.ob.scm.ad_reports.custodybarcodelabel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class CustodyBarcodeLabelDAO {
  private static Logger log4j = Logger.getLogger(CustodyBarcodeLabelDAO.class);
  private Connection connection = null;

  public CustodyBarcodeLabelDAO(Connection conn) {
    this.connection = conn;
  }

  /*
   * public JSONObject getMIRNos(String strOrgId) { JSONArray mirNos= new JSONArray(); JSONObject
   * mirNoObj = new JSONObject(), issueRequest; try { OBContext.setAdminMode();
   * OBQuery<MaterialIssueRequest> requestQuery =
   * OBDal.getInstance().createQuery(MaterialIssueRequest.class,
   * " where organization.id='"+strOrgId+
   * "' and alertStatus in ('ESCM_TR', 'ESCM_AP') and beneficiaryType in ('S', 'D', 'E') order by documentNo desc"
   * ); if(requestQuery!=null){ List<MaterialIssueRequest> requestList = requestQuery.list();
   * if(requestList.size() > 0){ for( MaterialIssueRequest mir : requestList){ issueRequest = new
   * JSONObject();
   * 
   * issueRequest.put("Id", mir.getId()); issueRequest.put("MIRNo", mir.getDocumentNo());
   * issueRequest.put("Description", mir.getDescription()==null?"":mir.getDescription());
   * issueRequest.put("Beneficiary",
   * mir.getBeneficiaryIDName()==null?"":mir.getBeneficiaryIDName().getCommercialName());
   * mirNos.put(issueRequest); } } } mirNoObj.put("RequestNos", mirNos); } catch (Exception e) {
   * log4j.error("Exception while getMIRNosL "+e); e.printStackTrace(); } finally{
   * OBContext.restorePreviousMode(); } return mirNoObj; }
   */

  @SuppressWarnings("resource")
  public synchronized JSONObject getMIRNos(String strRoldId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(mreq.documentno) as count  ");
      selectQuery.append(
          " select mreq.escm_material_request_id, mreq.specno as specno, mreq.description, benf.name as beneficiary ");
      fromQuery.append(
          " from escm_material_request mreq left join escm_beneficiary_v benf on benf.escm_beneficiary_v_id=mreq.beneficiary_name "
              + " where mreq.ad_org_id in (select ad_org_id from AD_Role_OrgAccess where ad_role_id ='"
              + strRoldId + "')"
              + " and mreq.status in ('ESCM_TR', 'ESCM_AP') and mreq.beneficiary_type in ('S', 'D', 'E') group by mreq.documentno, mreq.escm_material_request_id, benf.name order by mreq.documentno desc ");

      st = connection.prepareStatement(countQuery.append(fromQuery).toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);

      if (totalRecords > 0) {
        st = connection.prepareStatement((selectQuery.append(fromQuery)).toString());
        log4j.info("mirls:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();

          jsonData.put("Id", rs.getString("escm_material_request_id"));
          jsonData.put("MIRNo", rs.getString("specno") == null ? "" : rs.getString("specno"));
          jsonData.put("Description",
              rs.getString("description") == null ? "" : rs.getString("description"));
          jsonData.put("Beneficiary",
              rs.getString("beneficiary") == null ? "" : rs.getString("beneficiary"));

          jsonArray.put(jsonData);
        }
      }
      jsob.put("RequestNos", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getMIRNos :", e);
      return jsob;
    } finally {
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

  @SuppressWarnings("resource")
  public synchronized JSONObject getBeneficiaryTypeList() {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(name) as count  ");
      selectQuery.append(" select value, name ");
      fromQuery.append(
          " from ad_ref_list  where ad_reference_id ='E585F9EEA3024736B3E30F9F6A7C9A09' and value not in ('MA') and isactive='Y' ");

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
        log4j.info("benftypelist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("benftypevalue", rs.getString("value"));
          jsonData.put("benftypename", rs.getString("name"));
          jsonArray.put(jsonData);
        }
      }
      jsob.put("data", jsonArray);

    } catch (final Exception e) {
      log4j.error("Exception in getBeneficiaryType :", e);
      return jsob;
    } finally {
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

  @SuppressWarnings("rawtypes")
  public static JSONObject getTagsList(String roleId, String searchTerm, int pagelimit, int page)
      throws JSONException {
    StringBuffer query = null;
    Query tagQuery = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      query = new StringBuffer();
      query.append(" select mcus.id, mcus.documentNo, mcus.alertStatus, mcus.description "
          + " from Escm_Mrequest_Custody mcus "
          + " where mcus.organization.id in (select organization.id from ADRoleOrganization where role.id = :roleId ) ");
      if (searchTerm != null && !searchTerm.equals(""))
        query.append(" and (lower(mcus.documentNo) like '%" + searchTerm.toLowerCase()
            + "%' or lower(mcus.alertStatus) like '%" + searchTerm.toLowerCase()
            + "%' or lower(mcus.description) like '%" + searchTerm.toLowerCase() + "%')");
      query.append(" order by mcus.documentNo ");
      tagQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      tagQuery.setParameter("roleId", roleId);
      log4j.debug(" Query : " + query.toString());
      if (tagQuery != null) {
        if (tagQuery.list().size() > 0) {
          for (Iterator iterator = tagQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            JSONObject jsonData = new JSONObject();
            String docNo = objects[1] == null ? "" : objects[1].toString();
            String status = objects[2] == null ? ""
                : ("LD").equals(objects[2].toString()) ? "Lost and Damaged"
                    : ("SA").equals(objects[2].toString()) ? "Sale"
                        : ("RW").equals(objects[2].toString()) ? "Reward"
                            : ("IU").equals(objects[2].toString()) ? "In Use"
                                : ("N").equals(objects[2].toString()) ? "Gen Tag"
                                    : ("RET").equals(objects[2].toString()) ? "Returned"
                                        : ("OB").equals(objects[2].toString()) ? "Obsolete"
                                            : ("RI").equals(objects[2].toString()) ? "ReIssued"
                                                : ("MA").equals(objects[2].toString())
                                                    ? "Maintenance"
                                                    : objects[2].toString();
            String description = objects[3] == null ? "" : objects[3].toString();

            jsonData.put("id", objects[1] == null ? "" : objects[1].toString());
            jsonData.put("recordIdentifier", docNo + "-" + status + "-" + description);
            jsonArray.put(jsonData);
          }
        }
      }
      jsob.put("totalRecords", tagQuery.list().size());
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");
    } catch (OBException e) {
      log4j.error("Exception while getTagList:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsob;
  }

  public JSONObject getBenfDetails(String mirId) {
    final JSONObject benfObj = new JSONObject();
    PreparedStatement st = null;
    ResultSet rs = null;

    try {
      String sql = "select beneficiary_type, beneficiary_name from escm_material_request  where escm_material_request_id  = ? ";

      st = connection.prepareStatement(sql);
      st.setString(1, mirId);
      rs = st.executeQuery();
      if (rs.next()) {
        benfObj.put("BenfType", rs.getString("beneficiary_type"));
        benfObj.put("BenfName", rs.getString("beneficiary_name"));
      }
    } catch (Exception e) {
      e.printStackTrace();
      return benfObj;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return benfObj;
  }
}
