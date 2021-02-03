package sa.elm.ob.finance.ad_callouts.dao.purchaseinvoice;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class PurchaseInvDAOImpl implements PurchaseInvDAO {
  private static Logger log4j = Logger.getLogger(PurchaseInvDAOImpl.class);

  public String checkCostCenterPref(String clientId, String orgId, String userId, String roleId,
      String costCenterId) {
    String sql = null;
    Query qry = null;
    String ismumtalakatdpt = "N";
    try {
      OBContext.setAdminMode();
      sql = " select case when (select count(*)>0 from c_salesregion "
          + "where ad_client_id=:clientId and C_Salesregion_id=:costCenterId "
          + "and value in (SELECT replace(unnest(string_to_array((select value from ad_preference "
          + "where property='EFIN_MumtalakatDept' and ispropertylist='Y' "
          + "and ((ad_user_id is not null and ad_user_id=:userId) or ad_user_id is null) "
          + "and ((visibleat_client_id is not null and visibleat_client_id=:clientId) or visibleat_client_id is null) "
          + "and ((visibleat_org_id is not null and visibleat_org_id=:orgId) or visibleat_org_id is null) "
          + "and ((visibleat_role_id is not null and visibleat_role_id=:roleId) or visibleat_role_id is null)), ',')),' ','')))=true "
          + "then 'Y' else 'N' end as ismumtalakatdpt ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sql);
      qry.setParameter("clientId", clientId);
      qry.setParameter("costCenterId", costCenterId);
      qry.setParameter("userId", userId);
      qry.setParameter("orgId", orgId);
      qry.setParameter("roleId", roleId);

      log4j.debug("" + qry);
      if (qry.list().size() > 0) {
        Object row = (Object) qry.list().get(0);
        ismumtalakatdpt = (String) row;
      }
    } catch (Exception e) {
      log4j.error("Exception in checkCostCenterPref" + e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return ismumtalakatdpt;
  }
}