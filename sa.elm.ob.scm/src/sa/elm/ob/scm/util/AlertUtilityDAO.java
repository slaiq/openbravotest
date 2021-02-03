package sa.elm.ob.scm.util;

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
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * @author Gopalakrishnan on 21/02/2017
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
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String window,
      String alertKey, String mailTemplate, String windowId, String createdUserId) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:processType "
              + " order by e.creationDate desc");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("processType", window);
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id=:alertRuleID");
      receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
      List<String> ruleList = getVisibleAtRole(property, clientId, windowId, createdUserId);
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
   * @param alertKey
   * @param mailTemplate
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
          "as e where e.client.id=:clientID and e.eSCMProcessType=:processType "
              + " order by e.creationDate desc");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("processType", Window);
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      // imported via data set
      objAlert.setDescription(description);
      objAlert.setEutMailTmplt(mailTemplate);
      objAlert.setEutAlertKey(alertKey);
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
    String role_id = roleId;
    String user_id = userId;
    String client_id = clientId;
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    int count = 0;
    try {

      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:processType "
              + " order by e.creationDate desc");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("processType", Window);
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      if (userId != null) {
        count = checkUserPresentedInRole(role_id, user_id, client_id);
      }
      if ((userId == null && count == 0) || (userId != null && count > 0)) {
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
      }

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in insertAlertRecipient", e);
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
   * @param alertKey
   * @param mailTemplate
   * @param notes
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRoleWithNotes(String DocumentId, String DocumentNo,
      String roleId, String userId, String clientId, String description, String status,
      String Window, String alertKey, String mailTemplate, String notes) {
    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";

    try {
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:processType "
              + " order by e.creationDate desc");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("processType", Window);

      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      // imported via data set
      objAlert.setDescription(description);
      objAlert.setEutMailTmplt(mailTemplate);
      objAlert.setEutAlertKey(alertKey);
      if (!roleId.isEmpty() && !roleId.equals("")) {
        objAlert.setRole(OBDal.getInstance().get(Role.class, roleId));
      }
      if (!userId.isEmpty() && !userId.equals("")) {
        objAlert.setUserContact(OBDal.getInstance().get(User.class, userId));
      }
      objAlert.setRecordID(DocumentNo);
      objAlert.setReferenceSearchKey(DocumentId);
      objAlert.setAlertStatus(status);
      objAlert.setComments(notes);

      OBDal.getInstance().save(objAlert);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception in alertInsertionRole", e);
    }
    return isSuccess;
  }

  public static int checkUserPresentedInRole(String roleId, String userId, String clientId) {
    int count = 0;
    try {
      OBQuery<UserRoles> checkUserRoles = OBDal.getInstance().createQuery(UserRoles.class,
          "as e where (e.role.id=:roleId or e.userContact.id=:userId) and e.client.id=:clientId");
      checkUserRoles.setNamedParameter("roleId", roleId);
      checkUserRoles.setNamedParameter("userId", userId);
      checkUserRoles.setNamedParameter("clientId", clientId);

      count = checkUserRoles.list().size();
    } catch (Exception e) {
      log4j.error("Exception in insertAlertRecipient", e);
    }
    return count;
  }

  /**
   * Returns list of VisibleAtRoles from Preference configuration based on Property
   * 
   * @param property
   * @param clientId
   * @param windowId
   * @param createdUserId
   * @return List of Strings containing VisibleAtRoleIds
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
   * Method to get alert rule Id
   * 
   * @param clientId
   * @param alertWindow
   * @return alertRuleId in String
   */
  public static String getAlertRule(String clientId, String alertWindow) {
    String alertRuleId = "";
    try {
      List<AlertRule> alertRuleList = new ArrayList<AlertRule>();

      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:alertwindow ");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("alertwindow", alertWindow);
      alertRuleList = queryAlertRule.list();

      if (alertRuleList.size() > 0) {
        AlertRule objRule = alertRuleList.get(0);
        alertRuleId = objRule.getId();
      }
    } catch (OBException e) {
      log4j.error("Exception while getAlertRule:", e);
      throw new OBException(e.getMessage());
    }
    return alertRuleId;
  }

  /**
   * Method to get alert recipients for sending alert
   * 
   * @param alertRuleId
   * @return recipients in list
   */
  public static List<AlertRecipient> getAlertReceipient(String alertRuleId) {
    List<AlertRecipient> alertRecList = new ArrayList<AlertRecipient>();
    try {
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id=:alertRuleID ");
      receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
      alertRecList = receipientQuery.list();
    } catch (OBException e) {
      log4j.error("Exception while getAlertReceipient:", e);
    }
    return alertRecList;
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

  /**
   * Method to delete alerts
   * 
   * @param alertId
   */
  public static Boolean deleteAlert(String recordId) {
    Boolean isSuccess = Boolean.TRUE;
    try {
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey=:recordID and e.alertStatus='NEW'");
      alertQuery.setNamedParameter("recordID", recordId);

      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          OBDal.getInstance().remove(objAlert);
        }
      }
    } catch (OBException e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception while solveAlerts:", e);
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
   * @return
   */
  public static Boolean alertInsertBasedonPreference(String DocumentId, String DocumentNo,
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
      OBContext.setAdminMode();
      // get alertruleID
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + window
              + "' order by e.creationDate desc");
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // get alert recipients
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

      ruleList = getVisibleAtRoleAndUser(property, clientId, windowId, createdUserId);
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

  public static JSONObject getVisibleAtRoleAndUser(String property, String clientId,
      String windowId, String createdUserId) {
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
    }
    return json;
  }

  /**
   * 
   * @param recordId
   * @param alertRuleId
   * @return
   */
  public static Boolean deleteAlertPreference(String recordId, String alertRuleId) {
    Boolean isSuccess = Boolean.TRUE;
    try {
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey=:recordID and e.alertStatus='NEW' and e.alertRule.id = :alertRule");
      alertQuery.setNamedParameter("recordID", recordId);
      alertQuery.setNamedParameter("alertRule", alertRuleId);
      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          OBDal.getInstance().remove(objAlert);
        }
      }
    } catch (OBException e) {
      isSuccess = Boolean.FALSE;
      log4j.error("Exception while deleteAlert:", e);
    }
    return isSuccess;
  }

  /**
   * This method is used to sent the alerts to who are all approve this proposal
   * 
   * @param proposalId
   * @return JSONObject
   */

  public static JSONObject getAlertForReactivate(String proposalId) {
    String sqlQuery = null;
    SQLQuery query = null;
    JSONObject json = new JSONObject();
    JSONArray arr = new JSONArray();
    try {
      sqlQuery = "select ad_role_id, createdby from escm_proposalmgmt_hist where escm_proposalmgmt_id =? and (requestreqaction ='AP' or requestreqaction='SUB') and created >=( "
          + "select created from escm_proposalmgmt_hist where escm_proposalmgmt_id =? and requestreqaction='SUB' order by created desc limit 1) ";

      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposalId);
      query.setParameter(1, proposalId);

      if (query != null) {
        @SuppressWarnings("rawtypes")
        List approverList = query.list();
        if (approverList != null && approverList.size() > 0) {
          for (Object row : approverList) {
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
      log4j.error("Exception in getAlertForReactivate :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }
}