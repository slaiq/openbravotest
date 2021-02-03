package sa.elm.ob.utility.email;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class EmailProcess extends DalBaseProcess {

  private static Logger log = Logger.getLogger(EmailManager.class);
  private EmailProcessStatus status = new EmailProcessStatus();
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    logger.logln("Email Process Started");
    List<Alert> alertsWithEmail = null;
    try {
      try {
        alertsWithEmail = AlertDAO.getAlertsWithEmail();
      } catch (Exception e) {
        log.error("Error while retreiving ALerts from database." + e.getMessage());
        throw new Exception("Error while retreiving ALerts from database.", e);
      }
      status.setAlertCount(alertsWithEmail.size());
      log.info("Alerts to be sent by email count:" + status.getAlertCount());
      logger.logln("Alerts to be sent by email count:" + status.getAlertCount());
      for (Alert alert : alertsWithEmail) {
        User user = alert.getUserContact();
        if (user != null) {
          sendEmailToUser(alert, user);
        } else {
          sendEmailToRole(alert);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error in Sending Alert email process.", e);
      logger.logln("Error in Sending Alert email process, " + e.getMessage());
    }
    logProcessStatus();
    log.info("Email Process Fininshed.");
    logger.logln("Email Process Fininshed.");
  }

  private void logProcessStatus() {
    StringBuilder statusString = new StringBuilder();
    statusString.append("Total Alert Record to be handeled :" + status.getAlertCount());
    statusString.append(", ");
    statusString.append("Emails sent successfully :" + status.getSuccessfullySent());
    statusString.append(", ");
    statusString.append("Emails failed to be sent :" + status.getFailedSent());
    log.info(statusString.toString());
    logger.logln(statusString.toString());
  }

  private void sendEmailToRole(Alert alert) {
    Role role = alert.getRole();
    log.info("Email to be Sent to all users in role, Role:" + role.getName());
    List<UserRoles> adUserRolesList = role.getADUserRolesList();
    for (UserRoles userRoles : adUserRolesList) {
      User user = userRoles.getUserContact();
      sendEmailToUser(alert, user);
    }
    log.info("Email Sent to all users in role Successfully, Role:" + role.getName());
    logger.logln("Email Sent to all users in role Successfully, Role:" + role.getName());

  }

  private void sendEmailToUser(Alert alert, User user) {
    boolean sent = false;
    boolean checkUtility = false;
    try {
      // Here check if the Email is HTML then remove this Record Id
      EmailTemplateReader emailTemplateReader = new EmailTemplateReader();

      HashMap<String, String> paramMap = new HashMap<>();
      paramMap.put("userNameEn", user.getName());
      paramMap.put("userNameAr", user.getName());
      if (StringUtils.isNotEmpty(alert.getAlertRule().getESCMProcessType())) {
        if (StringUtils.isEmpty(
            sa.elm.ob.scm.properties.Resource.getProperty(alert.getEutAlertKey(), "ar_SA"))) {
          checkUtility = true;
        }
        paramMap.put("alertDescriptionEn",
            sa.elm.ob.scm.properties.Resource.getProperty(alert.getEutAlertKey(), "en_US") + " - "
                + alert.getRecordID());
        paramMap.put("alertDescriptionArabic",
            sa.elm.ob.scm.properties.Resource.getProperty(alert.getEutAlertKey(), "ar_SA") + " - "
                + alert.getRecordID());
      } else if (StringUtils.isNotEmpty(alert.getAlertRule().getEfinProcesstype())) {
        if (StringUtils.isEmpty(
            sa.elm.ob.finance.properties.Resource.getProperty(alert.getEutAlertKey(), "en_US"))) {
          checkUtility = true;
        }
        paramMap.put("alertDescriptionEn",
            sa.elm.ob.finance.properties.Resource.getProperty(alert.getEutAlertKey(), "en_US")
                + " - " + alert.getRecordID());
        paramMap.put("alertDescriptionArabic",
            sa.elm.ob.finance.properties.Resource.getProperty(alert.getEutAlertKey(), "ar_SA")
                + " - " + alert.getRecordID());
      }
      if (checkUtility) {
        paramMap.put("alertDescriptionEn",
            sa.elm.ob.utility.properties.Resource.getProperty(alert.getEutAlertKey(), "en_US")
                + " - " + alert.getRecordID());
        paramMap.put("alertDescriptionArabic",
            alert.getDescription() + " - " + alert.getRecordID());
      }
      String emailText = emailTemplateReader.getFormattedMessage(alert.getEutMailTmplt(), paramMap);
      sent = EmailManager.sendEmail("PR", user.getEmail(), emailText);
      alert.setEutEmailstatus("S");
    } catch (Exception e) {
      log.error("Error sending email to user :" + user.getUsername(), e);
      logger.logln("Error sending email to user :" + e.getStackTrace());
      logger.logln("Error sending email to user :" + e.getMessage());
      alert.setEutEmailstatus("F");
    }
    if (sent) {
      log.info("Email Sent to user Successfully, UserName:" + user.getUsername());
      logger.logln("Error sending email to user :" + user.getUsername());
      alert.setEutEmailstatus("S");
      status.inceaseSuccessfullySent();
    } else {
      log.info("Email not Sent to user, UserName:" + user.getUsername());
      logger.logln("Error sending email to user :" + user.getUsername());
      alert.setEutEmailstatus("F");
      status.inceaseFailedSent();
    }
  }

  class EmailProcessStatus {
    private int alertCount = 0;
    private int successfullySent = 0;
    private int failedSent = 0;

    public int getAlertCount() {
      return alertCount;
    }

    public void setAlertCount(int alertCount) {
      this.alertCount = alertCount;
    }

    public int getSuccessfullySent() {
      return successfullySent;
    }

    public void setSuccessfullySent(int successfullySent) {
      this.successfullySent = successfullySent;
    }

    public int getFailedSent() {
      return failedSent;
    }

    public void setFailedSent(int failedSent) {
      this.failedSent = failedSent;
    }

    public void inceaseSuccessfullySent() {
      successfullySent++;
    }

    public void inceaseFailedSent() {
      failedSent++;

    }
  }

}
