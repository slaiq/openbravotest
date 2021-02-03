package sa.elm.ob.utility.dms.notifyuser.dao;

import sa.elm.ob.utility.dms.consumer.dto.DMSNotifyRequestDTO;
import sa.elm.ob.utility.dms.notifyuser.exceptions.DmsNotifyUserException;

public interface DmsNotifyUserDAO {
  /**
   * Method to insert Alert Notification for user
   * 
   * @param dmsIntegrationLogId
   * @return
   * @throws DmsNotifyUserException
   */
  Boolean insertAlertNotificationForUser(String dmsIntegrationLogId) throws DmsNotifyUserException;

  /**
   * Process to insert the alert
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param roleId
   * @param userId
   * @param clientId
   * @param description
   * @param status
   * @param Window
   * @param alertKey
   * @param mailTemplate
   * @return
   */
  public Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate) throws DmsNotifyUserException;

  /**
   * This method is used to update the response in dms integration log
   * 
   * @param notifyDTO
   */
  public void updateDMSIntegrationLog(DMSNotifyRequestDTO notifyDTO);

}
