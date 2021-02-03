package sa.elm.ob.scm.ad_process.Requisition;

import org.openbravo.model.procurement.Requisition;

/**
 * 
 * @author DivyaPrakash JS
 * @since 2019-07-22
 *
 */

public interface NotifyUserRevokeInRequisitionDAO {
  /**
   * This method is used to check purchase requisition is created for bid
   * 
   * @param requisitionID
   * @return
   */
  public Boolean isPurchseReqCreatedForBid(String requisitionID);

  /**
   * This method is used to perform alert process for revoke notified user
   * 
   * @param status
   * @param notifyUser
   * @param objRequisition
   * @param alertWindow
   */
  public void alertProcess(String status, String notifyUser, Requisition objRequisition,
      String alertWindow);

  /**
   * This method is used to insert action history for revoke notified user process
   * 
   * @param objRequisition
   * @param currentRoleId
   * @param currentUserId
   */
  public void insertActionHistory(Requisition objRequisition, String currentRoleId,
      String currentUserId);
}
