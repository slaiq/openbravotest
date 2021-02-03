package sa.elm.ob.utility.dms.notifyuser.service;

import sa.elm.ob.utility.dms.consumer.dto.DMSNotifyRequestDTO;
import sa.elm.ob.utility.dms.notifyuser.exceptions.DmsNotifyUserException;

public interface DmsNotifyUserService {

  /**
   * Service Layer to insert the alert notification to the user
   * 
   * @param dmsIntegrationLogId
   * @return
   * @throws DmsNotifyUserException
   */
  Boolean insertAlertNotificationForUser(String dmsIntegrationLogId) throws DmsNotifyUserException;

  public void updateDMSIntegrationLog(DMSNotifyRequestDTO response);

}
