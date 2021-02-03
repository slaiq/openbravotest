package sa.elm.ob.utility.ad_process.alertNotifications;

import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @author DivyaPrakash JS on 26-11-2019
 *
 */
public class AlertNotificationProcess extends DalBaseProcess {
  static int counter = 0;
  private ProcessLogger logger;
  private final static String userID = "100";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      String clientId = bundle.getContext().getClient();
      JSONObject result = null;
      boolean isEmailSent = false;
      String message = "Kindly Verify the new alerts dated on "
          + (new SimpleDateFormat("dd-mm-yyyy").format(new java.util.Date())).toString()
          + " in validation alerts notification window </br></br> ";
      logger = bundle.getLogger();
      logger.log("Starting background product transaction Loop " + counter + "\n");
      AlertNotificationProcessDAO dao = new AlertNotificationProcessDAOImpl();
      result = dao.insertAlertNotifocation(clientId);
      if (result.getBoolean("processResult")) {
        logger.log(result.getString("headerResult") + "\n");
        if (result.has("lineResult") && result.getJSONArray("lineResult").length() > 0) {
          for (int i = 0; i < result.getJSONArray("lineResult").length(); i++) {
            logger.log(result.getJSONArray("lineResult").getString(i) + "\n");
            message = message.concat(StringUtils.removeEndIgnoreCase(
                result.getJSONArray("lineResult").getString(i), " inserted successfully"));
            message = message.replace("Alert Name:", String.valueOf((i + 1)).concat("."))
                .concat("</br>");
          }
          logger.log("Totally : " + result.getJSONArray("lineResult").length()
              + " Alerts inserted successfully" + "\n");

          User user = OBDal.getInstance().get(User.class, userID);
          if (user != null) {
            String userMailId = user.getEmail();
            if (!org.apache.commons.lang.StringUtils.isEmpty(userMailId)) {
              isEmailSent = dao.isEmailSent(userMailId, message);
              if (isEmailSent) {
                logger.log("Email sent to " + userMailId + " successfully" + "\n");
              } else {
                logger.log("Email sent failed" + "\n");
              }
            } else {
              logger
                  .log("Please configure email address for currentUser in User Window to Send mail"
                      + "\n");
            }
          }
        } else {
          logger.log(result.getString("emptyLineResult") + "\n");
        }
        logger.log("Process completed successfully" + "\n");

      } else {
        logger.log("Process failed" + "\n");
      }

    } catch (Exception e) {
      // catch any possible exception and throw it as a Quartz
      // JobExecutionException
      OBDal.getInstance().rollbackAndClose();
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
