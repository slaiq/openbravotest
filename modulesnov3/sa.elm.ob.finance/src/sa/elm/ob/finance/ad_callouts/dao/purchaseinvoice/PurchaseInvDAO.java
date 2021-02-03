package sa.elm.ob.finance.ad_callouts.dao.purchaseinvoice;

/**
 * 
 * @author Kousalya
 *
 */

public interface PurchaseInvDAO {

  /**
   * Check cost center value is in Preference to display fields
   * 
   * @param clientId
   * @param orgId
   * @param userId
   * @param roleId
   * @param costCenterId
   * @return String
   */
  public String checkCostCenterPref(String clientId, String orgId, String userId, String roleId,
      String costCenterId);
}