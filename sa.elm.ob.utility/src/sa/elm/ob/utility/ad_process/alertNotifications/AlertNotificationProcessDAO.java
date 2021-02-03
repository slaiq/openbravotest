package sa.elm.ob.utility.ad_process.alertNotifications;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.email.EmailEventException;

/**
 * 
 * @author DivyaPrakash JS on 26-11-2019
 *
 */
public interface AlertNotificationProcessDAO {

  /**
   * This method is used to insert records in alert Notification window
   */
  public JSONObject insertAlertNotifocation(String clientId);

  /**
   * This method is used to send email to specific user regarding alert details
   * 
   * @return boolean
   */
  public boolean isEmailSent(String recipient, String message) throws EmailEventException;
}
