package sa.elm.ob.finance.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * @author Gopalakrishnan on 09/10/2017
 * 
 */
public class AlertUtilityDAO {
  private static final Logger log4j = Logger.getLogger(AlertUtilityDAO.class);

  /**
   * This method only for Purchase Requisition Alert Process With Preference Configuration
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  @SuppressWarnings("unchecked")
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String window,
      String alertKey, String mailTemplate) {
    String sqlQuery = "";
    SQLQuery query = null;
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + window
              + "'    order by e.creationDate desc");
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

      sqlQuery = "select visibleat_role_id  from ad_preference where property='" + property
          + "' and ad_client_id='" + clientId + "' ";
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);

      List<String> ruleList = (ArrayList<String>) query.list();
      if (ruleList != null && ruleList.size() > 0) {
        for (int i = 0; i < ruleList.size(); i++) {
          String object = (String) ruleList.get(i);
          Alert objAlert = OBProvider.getInstance().get(Alert.class);
          objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
          objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
          objAlert.setEutMailTmplt(mailTemplate);
          objAlert.setEutAlertKey(alertKey);
          // imported via data set
          objAlert.setDescription(description);
          objAlert.setRole(OBDal.getInstance().get(Role.class, object.toString()));
          objAlert.setRecordID(DocumentNo);
          objAlert.setReferenceSearchKey(DocumentId);
          objAlert.setAlertStatus(status);
          OBDal.getInstance().save(objAlert);
          OBDal.getInstance().flush();
          includeRecipient.add(object.toString());
        }
        isSuccess = Boolean.TRUE;
      }
      // check and insert Recipient
      if (receipientQuery.list().size() > 0) {
        for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
          includeRecipient.add(objAlertReceipient.getRole().getId());
          OBDal.getInstance().remove(objAlertReceipient);
        }
      }
      // avoid duplicate recipient
      HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
      Iterator<String> iterator = incluedSet.iterator();
      while (iterator.hasNext()) {
        insertAlertRecipient(iterator.next(), null, clientId, window);
      }

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      e.printStackTrace();
      log4j.error("Exception in alertInsertionPreference", e);
    }
    return isSuccess;
  }

  /**
   * This method only for Purchase Requisition Alert Process
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param roleId
   * @param clientId
   * @param description
   * @param status
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + Window
              + "'  order by e.creationDate desc");
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlert.setEutMailTmplt(mailTemplate);
      objAlert.setEutAlertKey(alertKey);
      // imported via data set
      objAlert.setDescription(description);
      if (StringUtils.isEmpty(userId) && StringUtils.isEmpty(roleId)) {
        // when the role id and user id is empty then we have to send the alert to openbravo user
        objAlert.setUserContact(OBDal.getInstance().get(User.class, "100"));
      } else {
        if (!roleId.isEmpty() && !roleId.equals("")) {
          objAlert.setRole(OBDal.getInstance().get(Role.class, roleId));
        }
        if (!userId.isEmpty() && !userId.equals("")) {
          objAlert.setUserContact(OBDal.getInstance().get(User.class, userId));
        }
      }

      objAlert.setRecordID(DocumentNo);
      objAlert.setReferenceSearchKey(DocumentId);
      objAlert.setAlertStatus(status);
      OBDal.getInstance().save(objAlert);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in alertInsertionRole", e);
    }
    return isSuccess;
  }

  /**
   * 
   * @param roleId
   * @param clientId
   * @return True --Alert Recipient Created, False --Error
   */
  public static Boolean insertAlertRecipient(String roleId, String userId, String clientId,
      String Window) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {

      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + Window
              + "'  order by e.creationDate desc");
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      AlertRecipient objAlertRecipient = OBProvider.getInstance().get(AlertRecipient.class);
      objAlertRecipient.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlertRecipient.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlertRecipient.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlertRecipient.setRole(OBDal.getInstance().get(Role.class, roleId));
      objAlertRecipient.setSendEMail(false);
      if (userId != null)
        objAlertRecipient.setUserContact(OBDal.getInstance().get(User.class, userId));
      OBDal.getInstance().save(objAlertRecipient);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in insertAlertRecipient", e);
    }
    return isSuccess;
  }

  /**
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param property
   * @param clientId
   * @param description
   * @param status
   * @param window
   * @param alertKey
   * @param mailTemplate
   * @param windowId
   * @param createdUserId
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionPreferenceBudUser(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String window,
      String alertKey, String mailTemplate, String windowId, String createdUserId) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    JSONObject ruleList, json = null;
    JSONArray arr = null;
    String tempRole = null;
    String tempUser = null;
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + window
              + "' order by e.creationDate desc");
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

      ruleList = getVisibleAtUser(property, clientId, windowId, createdUserId);
      if (ruleList != null && ruleList.length() > 0) {
        arr = ruleList.getJSONArray("roleList");
        if (arr.length() > 0) {
          for (int i = 0; i < arr.length(); i++) {
            json = arr.getJSONObject(i);

            if (tempRole == null || (tempRole != null && ((!tempRole.equals(json.getString("Role")))
                || (tempRole.equals(json.getString("Role")) && !tempUser.equals("0")
                    && !tempUser.equals(json.has("User") ? "0" : json.getString("User")))))) {
              tempRole = json.getString("Role");
              tempUser = json.has("User") ? json.getString("User") : "0";
              // List<String> ruleList = getVisibleAtRole(property, clientId, windowId,
              // createdUserId);
              // if (ruleList != null && ruleList.size() > 0) {
              // for (int i = 0; i < ruleList.size(); i++) {
              // String object = (String) ruleList.get(i);

              Alert objAlert = OBProvider.getInstance().get(Alert.class);
              objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
              objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
              objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
              objAlert.setEutMailTmplt(mailTemplate);
              objAlert.setEutAlertKey(alertKey);
              // imported via data set
              objAlert.setDescription(description);
              if (json.has("User")) {
                objAlert
                    .setUserContact(OBDal.getInstance().get(User.class, json.getString("User")));
              }
              if (json.has("Role")) {
                objAlert.setRole(OBDal.getInstance().get(Role.class, json.getString("Role")));
              }
              objAlert.setRecordID(DocumentNo);
              objAlert.setReferenceSearchKey(DocumentId);
              objAlert.setAlertStatus(status);
              OBDal.getInstance().save(objAlert);
              OBDal.getInstance().flush();
              includeRecipient.add(json.getString("Role"));
            }
          }
        }
        isSuccess = Boolean.TRUE;
      }

      // check and insert Recipient
      if (receipientQuery.list().size() > 0) {
        for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
          includeRecipient.add(objAlertReceipient.getRole().getId());
          OBDal.getInstance().remove(objAlertReceipient);
        }
      }
      log4j.debug(includeRecipient);
      // avoid duplicate recipient
      HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
      Iterator<String> iterator = incluedSet.iterator();
      while (iterator.hasNext()) {
        insertAlertRecipient(iterator.next(), null, clientId, window);
      }

    } catch (

    Exception e) {
      isSuccess = Boolean.FALSE;
      e.printStackTrace();
      log4j.error("Exception in alertInsertionPreferenceBudUser", e);
    }
    return isSuccess;
  }

  /**
   * 
   * @param property
   * @param clientId
   * @param windowId
   * @param createdUserId
   * @return
   */
  public static JSONObject getVisibleAtUser(String property, String clientId, String windowId,
      String createdUserId) {
    String sqlQuery = null;
    SQLQuery query = null;
    JSONObject json = new JSONObject();
    JSONArray arr = new JSONArray();
    try {
      sqlQuery = "select distinct coalesce(rol.ad_role_id,coalesce(prrol.ad_role_id,prrol1.ad_role_id)),coalesce(usr.ad_user_id,usr1.ad_user_id) as user from ad_preference pref "
          + " left join ad_user usr on usr.ad_user_id=pref.ad_user_id and pref.visibleat_role_id is  null"
          + " left join ad_user_roles usroles on usroles.ad_user_id=usr.ad_user_id "
          + " left join ad_role rol on rol.ad_role_id=usroles.ad_role_id "
          + " left join ad_role prrol on prrol.ad_role_id=pref.visibleat_role_id and pref.ad_user_id is  null "
          + " left join ad_role prrol1 on prrol1.ad_role_id=pref.visibleat_role_id  and pref.ad_user_id is not  null "
          + "left join ad_user usr1 on usr1.ad_user_id=pref.ad_user_id and pref.visibleat_role_id is  not null"
          + " where property='" + property + "' and value='Y' and pref.ad_client_id='" + clientId
          + "'  and (ad_window_id='" + windowId
          + "' or ad_window_id is null) order by coalesce(rol.ad_role_id,coalesce(prrol.ad_role_id,prrol1.ad_role_id)),coalesce(usr.ad_user_id,usr1.ad_user_id) desc";
      if (createdUserId != null) {
        // sqlQuery = sqlQuery + " and prrol.ad_role_id not in (select ad_role_id from ad_user_roles
        // "
        // + " where ad_user_id='" + createdUserId + "')";
      }
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      if (query != null) {
        @SuppressWarnings("rawtypes")
        List visibleList = query.list();
        if (visibleList != null && visibleList.size() > 0) {
          for (Object row : visibleList) {
            Object[] o = (Object[]) row;
            JSONObject result = new JSONObject();
            if (o[0] != null) {
              result.put("Role", o[0].toString());
            }
            if (o[1] != null) {
              result.put("User", o[1].toString());
            }
            arr.put(result);
          }
        }
        json.put("roleList", arr);

      }

    } catch (Exception e) {
      log4j.error("Exception in getVisibleAtUser :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

  /**
   * 
   * @param property
   * @param clientId
   * @param windowId
   * @param createdUserId
   * @return
   */
  @SuppressWarnings("unchecked")
  public static List<String> getVisibleAtRole(String property, String clientId, String windowId,
      String createdUserId) {
    String sqlQuery = null;
    SQLQuery query = null;

    sqlQuery = "select distinct coalesce(rol.ad_role_id,prrol.ad_role_id) from ad_preference pref "
        + " left join ad_user usr on usr.ad_user_id=pref.ad_user_id and pref.visibleat_role_id is  null"
        + " left join ad_user_roles usroles on usroles.ad_user_id=usr.ad_user_id "
        + " left join ad_role rol on rol.ad_role_id=usroles.ad_role_id "
        + " left join ad_role prrol on prrol.ad_role_id=pref.visibleat_role_id "
        + " where property='" + property + "' and value='Y' and pref.ad_client_id='" + clientId
        + "'  and (ad_window_id='" + windowId + "' or ad_window_id is null) ";
    if (createdUserId != null) {
      // sqlQuery = sqlQuery + " and prrol.ad_role_id not in (select ad_role_id from ad_user_roles "
      // + " where ad_user_id='" + createdUserId + "')";
    }
    query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
    List<String> ruleList = (ArrayList<String>) query.list();
    return ruleList;
  }

  /**
   * Method to solve alerts
   * 
   * @param alertId
   * @return
   */
  public static Boolean solveAlert(String recordId) {
    Boolean isSuccess = Boolean.TRUE;
    try {
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey=:recordID and e.alertStatus='NEW'");
      alertQuery.setNamedParameter("recordID", recordId);

      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          objAlert.setAlertStatus("SOLVED");
          OBDal.getInstance().save(objAlert);
        }
      }
    } catch (OBException e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception while solveAlerts:", e);
    }
    return isSuccess;
  }
}