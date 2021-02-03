package sa.elm.ob.scm.ad_process.IssueRequest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.MaterialIssueRequest;

public class UpdateOnHandQty {
  private static final Logger log4j = Logger.getLogger(UpdateOnHandQty.class);

  /**
   * Update on hand quantity
   * 
   * @param warehouseId
   * @return boolean
   */
  public static boolean updateOnHandQty(String warehouseId, String mirId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    boolean update = false;
    try {
      ps = OBDal.getInstance().getConnection().prepareStatement(
          "select sum(qtyonhand) as qtyonhand, m_product_id from m_storage_detail strdt "
              + " left join m_locator loc on loc.m_locator_id=strdt.m_locator_id "
              + " where loc.m_warehouse_id = ? group by m_warehouse_id, m_product_id  ");
      ps.setString(1, warehouseId);
      log4j.debug("onhnd qty>" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        ps = OBDal.getInstance().getConnection().prepareStatement(
            "update escm_material_reqln set onhand_qty=?, updated=now() where m_product_id=? and escm_material_request_id=?");
        ps.setBigDecimal(1, rs.getBigDecimal("qtyonhand"));
        ps.setString(2, rs.getString("m_product_id"));
        ps.setString(3, mirId);
        log4j.debug("Update matln>" + ps.toString());
        ps.executeUpdate();
        update = true;
      }
    } catch (final Exception e) {
      log4j.error("Exception in updateOnHandQty() : ", e);
      return update;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
    return update;
  }

  /**
   * Check Access for MIR on hand qty update
   * 
   * @param roleId
   * @param mirId
   * @param clientId
   * @param orgId
   * @param userId
   * @return boolean
   */
  public static boolean checkMIROnHandQtyUpdateAccess(String roleId, String clientId, String orgId,
      String userId, String mirId) {
    boolean enable = false;
    MaterialIssueRequest objRequest = null;
    String preferenceValue = "";
    try {
      objRequest = OBDal.getInstance().get(MaterialIssueRequest.class, mirId);
      if (objRequest.getEUTNextRole() == null) {
        enable = true;
      } else {
        try {
          preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true, clientId,
              orgId, userId, roleId, "800092");
          if (!preferenceValue.equals("") && preferenceValue.equals("Y")) {
            enable = true;
          }
        } catch (PropertyException e) {
          log4j.debug("Exeception in checkMIROnHandQtyUpdateAccess:" + e);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in identifyBPartner() : ", e);
      return false;
    }
    return enable;
  }
}