package sa.elm.ob.utility.dms.notifyuser.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sa.elm.ob.utility.dms.consumer.dto.DMSNotifyRequestDTO;
import sa.elm.ob.utility.dms.notifyuser.dao.DmsNotifyUserDAO;
import sa.elm.ob.utility.dms.notifyuser.exceptions.DmsNotifyUserException;

@Service
public class DmsNotifyUserServiceImpl implements DmsNotifyUserService {

  @Autowired
  private DmsNotifyUserDAO dao;

  @Override
  public Boolean insertAlertNotificationForUser(String dmsIntegrationLogId)
      throws DmsNotifyUserException {
    // TODO Auto-generated method stub
    Boolean is_alert_inserted = dao.insertAlertNotificationForUser(dmsIntegrationLogId);
    return is_alert_inserted;
  }

  @Override
  public void updateDMSIntegrationLog(DMSNotifyRequestDTO response) {
    dao.updateDMSIntegrationLog(response);
  }

}
