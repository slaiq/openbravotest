package sa.elm.ob.utility.ad_process.alertNotifications;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.email.EmailEventException;
import org.openbravo.email.EmailUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.utils.FormatUtilities;

import sa.elm.ob.utility.EutAlertNotifyHdr;
import sa.elm.ob.utility.EutAlertNotifyLn;

/**
 * 
 * @author DivyaPrakash JS on 26-11-2019
 *
 */
public class AlertNotificationProcessDAOImpl implements AlertNotificationProcessDAO {
  private static final Logger log4j = Logger.getLogger(AlertNotificationProcessDAOImpl.class);

  @SuppressWarnings("unused")
  @Override
  public JSONObject insertAlertNotifocation(String clientId) {

    JSONObject result = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    List<AlertRule> alertRuleList = new ArrayList<AlertRule>();
    Long count = new Long(0);
    try {
      OBContext.setAdminMode(true);
      OBQuery<AlertRule> alertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.eutIsbackgroundvalidation = 'Y' and e.client.id =:clientID ");
      alertRule.setNamedParameter("clientID", clientId);

      if (alertRule != null) {
        EutAlertNotifyHdr alertNotifyHdr = insertAlertNotifyHdr();
        result.put("processResult", true);
        result.put("headerResult", "Alert Notification Header Inserted Succefully");
        alertRuleList = alertRule.list();
        if (alertRuleList.size() > 0) {
          for (AlertRule ar : alertRuleList) {
            Long new_alert_counts = ar.getADAlertList().stream().filter(
                a -> (a.getAlertStatus().equals("NEW") && a.getClient().getId().equals(clientId)))
                .count();
            if (new_alert_counts > 0) {
              count += new_alert_counts;
              insertAlertNotifyLn(ar, new_alert_counts, alertNotifyHdr);
              jsonArray.put("Alert Name: " + ar.getName() + " with count :" + new_alert_counts
                  + " inserted successfully");
            }
          }
          result.put("lineResult", jsonArray);
          EutAlertNotifyHdr hdr = OBDal.getInstance().get(EutAlertNotifyHdr.class,
              alertNotifyHdr.getId());
          hdr.setDescription("Total No of invalid records : " + count);
          OBDal.getInstance().save(hdr);
        } else {
          result.put("emptyLineResult", "No alert found with count greater than zero");
        }
      } else
        result.put("processResult", false);

    } catch (Exception e) {
      log4j.error("Exception in insertAlertNotifocation : " + e);
      return result;

    } finally {
      OBContext.restorePreviousMode();
    }
    return result;

  }

  /**
   * This method is used to insert lines in alert notification window
   * 
   * @param alertrule
   */
  public void insertAlertNotifyLn(AlertRule alertrule, Long alertCounts,
      EutAlertNotifyHdr alertNotifyHdr) {
    try {

      EutAlertNotifyLn alertNotifyLn = OBProvider.getInstance().get(EutAlertNotifyLn.class);
      alertNotifyLn.setAlertid(alertrule);
      alertNotifyLn.setCount(alertCounts);
      alertNotifyLn.setEUTAlertNotifyHdr(alertNotifyHdr);
      OBDal.getInstance().save(alertNotifyLn);
    }

    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exception in insertAlertNotifyLn : " + e);
    }

  }

  /**
   * This method is used to header lines in alert notification window
   * 
   * @return alertNotifyHdr
   */
  public EutAlertNotifyHdr insertAlertNotifyHdr() {
    EutAlertNotifyHdr alertNotifyHdr = null;
    try {
      alertNotifyHdr = OBProvider.getInstance().get(EutAlertNotifyHdr.class);
      alertNotifyHdr.setProcessedDate(new java.util.Date());
      OBDal.getInstance().save(alertNotifyHdr);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exception in insertAlertNotifyHdr : " + e);
    }
    return alertNotifyHdr;
  }

  @Override
  public boolean isEmailSent(String recipient, String message) throws EmailEventException {
    // Retrieves the Email Server configuration
    Organization currenctOrg = OBContext.getOBContext().getCurrentOrganization();
    final EmailServerConfiguration mailConfig = EmailUtils.getEmailConfiguration(currenctOrg);

    if (mailConfig == null) {
      log4j.warn("Couldn't find email configuarion");
      throw new EmailEventException(
          OBMessageUtils.getI18NMessage("EmailConfigurationNotFound", null));
    }
    try {
      final String username = mailConfig.getSmtpServerAccount();
      final String password = FormatUtilities.encryptDecrypt(mailConfig.getSmtpServerPassword(),
          false);
      final String connSecurity = mailConfig.getSmtpConnectionSecurity();
      final int port = mailConfig.getSmtpPort().intValue();
      final String senderAddress = mailConfig.getSmtpServerSenderAddress();
      final String host = mailConfig.getSmtpServer();
      final boolean auth = mailConfig.isSMTPAuthentification();

      EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
          recipient, null, null, null, sa.elm.ob.utility.util.Constants.SUBJECT, message,
          sa.elm.ob.utility.util.Constants.CONTENT_TYPE, null, null, null);
      return true;
    } catch (Exception e) {
      log4j.error("Exception in isEmailSent : " + e);
      return false;
    }
  }

}
