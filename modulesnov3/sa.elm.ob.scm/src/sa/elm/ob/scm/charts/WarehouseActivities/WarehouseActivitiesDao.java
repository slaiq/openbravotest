package sa.elm.ob.scm.charts.WarehouseActivities;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class WarehouseActivitiesDao {

  Connection conn = null;
  private static Logger log4j = Logger.getLogger(WarehouseActivitiesDao.class);

  public WarehouseActivitiesDao(Connection con) {
    this.conn = con;
  }

  public int getMaterialIssueCount(String clientId, String userId, String roleId, String from,
      String to) {
    String query = "";
    int count = 0;
    try {
      OBContext.setAdminMode(true);
      query = "SELECT count(*) FROM escm_material_request e WHERE 1=1 and (e.escm_material_request_id IN ( ( SELECT COALESCE(a.escm_material_request_id,'') AS id "
          + "FROM escm_material_request a JOIN   eut_next_role r ON r.eut_next_role_id = a.eut_next_role_id JOIN eut_next_role_line l ON r.eut_next_role_id = l.eut_next_role_id WHERE "
          + "l.ad_role_id ='" + roleId
          + "' AND a.status <>'DR')) OR e.escm_material_request_id IN (  SELECT b.escm_material_request_id FROM escm_materialrequest_hist b "
          + "WHERE  b.updatedby = '" + userId + "' ) OR e.createdby='" + userId
          + "' OR e.escm_material_request_id IN ( SELECT req.escm_material_request_id FROM escm_material_request req WHERE  EXISTS "
          + "( SELECT 1 FROM eut_docapp_delegate dl JOIN eut_docapp_delegateln dll ON dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id WHERE  dll.ad_user_id = '"
          + userId + "' AND dll.ad_role_id = '" + roleId + "' "
          + " AND  dll.document_type='EUT_112' AND cast(Now() as date) BETWEEN cast(dl.from_date as date) AND cast(dl.to_date as date) AND dl.ad_role_id IN  (SELECT l.ad_role_id FROM escm_material_request re JOIN eut_next_role r "
          + " ON r.eut_next_role_id = re.eut_next_role_id JOIN   eut_next_role_line l ON r.eut_next_role_id = l.eut_next_role_id)) )) "
          + " AND cast(e.transaction_date as date) BETWEEN cast(coalesce(eut_convertto_gregorian('"
          + from + "'),now()) as date) " + " AND cast(coalesce(eut_convertto_gregorian('" + to
          + "'),now()) as date)  AND e.ad_client_id ='" + clientId + "'";
      log4j.debug("Query: " + query);
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      Object row = (Object) sqlQuery.list().get(0);
      count = Integer.valueOf(row.toString());
      log4j.debug("Count:" + count);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public int getReturnTransactionCount(String clientId, String from, String to) {

    String query = "";
    int count = 0;
    try {
      OBContext.setAdminMode(true);
      query = "select count(*) from m_inout  where em_escm_receivingtype  ='INR' and cast(movementdate as date) between cast(eut_convertto_gregorian('"
          + from + "') as date) and cast(eut_convertto_gregorian('" + to
          + "') as date) and ad_client_id ='" + clientId + "'";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      Object row = (Object) sqlQuery.list().get(0);
      count = Integer.valueOf(row.toString());
      log4j.debug("Count:" + count);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public int getIssueReturnTransactionCount(String clientId, String from, String to) {

    String query = "";
    int count = 0;
    try {
      OBContext.setAdminMode(true);
      query = "select count(*) from m_inout  where em_escm_receivingtype  ='IRT' and cast(movementdate as date) between cast(eut_convertto_gregorian('"
          + from + "') as date) and cast(eut_convertto_gregorian('" + to
          + "') as date) and ad_client_id ='" + clientId + "'";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      Object row = (Object) sqlQuery.list().get(0);
      count = Integer.valueOf(row.toString());
      log4j.debug("Count:" + count);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public int getPOReceiptCount(String clientId, String from, String to) {

    String query = "";
    int count = 0;
    try {
      OBContext.setAdminMode(true);
      query = "select count(*) from m_inout join c_doctype using (c_doctype_id) where movementtype  in  ('V-', 'V+') and islogistic ='N' and isreturn  ='N' "
          + "and cast(movementdate as date) between cast(eut_convertto_gregorian('" + from
          + "') as date) and cast(eut_convertto_gregorian('" + to
          + "') as date) and m_inout.ad_client_id ='" + clientId + "'";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      Object row = (Object) sqlQuery.list().get(0);
      count = Integer.valueOf(row.toString());
      log4j.debug("Count:" + count);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
