package sa.elm.ob.finance.util;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;

/**
 * @auther gopalakrishnan on 09/10/2017
 * 
 *         Utility Class file for Requisition Alert Process
 */

public class AlertUtility {

  /**
   * This method only for Purchase Requisition Alert Process With Preference Configuration
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   * @param alertKey
   * @param mail
   *          template
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate) {
    return AlertUtilityDAO.alertInsertionPreference(DocumentId, DocumentNo, property, clientId,
        description, status, Window, alertKey, mailTemplate);
  }

  /**
   * This method only for Purchase Requisition Alert Process
   * 
   * @param property
   *          preference
   * @param clientId
   * @param description
   * @param status
   *          NEW-new Alert,SOLVED-alert Solved
   * @param alert
   *          Key
   * @param mail
   *          template
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate) {
    return AlertUtilityDAO.alertInsertionRole(DocumentId, DocumentNo, roleId, userId, clientId,
        description, status, Window, alertKey, mailTemplate);
  }

  /**
   * 
   * @param roleId
   * @param clientId
   * @return True --Alert recipient Created, False --Error
   */
  public static Boolean insertAlertRecipient(String roleId, String userId, String clientId,
      String Window) {
    return AlertUtilityDAO.insertAlertRecipient(roleId, userId, clientId, Window);
  }

  /**
   * This method only for PO - Hold Plan Details Alert Process With Preference Configuration
   * 
   * @param DocumentId
   * @param DocumentNo
   * @param property
   * @param clientId
   * @param description
   * @param status
   * @param Window
   * @param alertKey
   * @param mailTemplate
   * @param windowId
   * @param createdUserId
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionPreferenceBudUser(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate, String windowId, String createdUserId) {
    return AlertUtilityDAO.alertInsertionPreferenceBudUser(DocumentId, DocumentNo, property,
        clientId, description, status, Window, alertKey, mailTemplate, windowId, createdUserId);
  }

  /**
   * get visible at user
   * 
   * @param property
   * @param clientId
   * @param windowId
   * @param createdUserId
   * @return
   */
  public static JSONObject getVisibleAtUser(String property, String clientId, String windowId,
      String createdUserId) {
    return AlertUtilityDAO.getVisibleAtUser(property, clientId, windowId, createdUserId);
  }

  /**
   * 
   * @param property
   * @param clientId
   * @param windowId
   * @param createdUserId
   * @return
   */
  public static List<String> getVisibleAtRole(String property, String clientId, String windowId,
      String createdUserId) {
    return AlertUtilityDAO.getVisibleAtRole(property, clientId, windowId, createdUserId);
  }

  /**
   * Method to solve approval alerts
   * 
   * @param recordId
   * @return
   */
  public static Boolean solveAlerts(String recordId) {
    return AlertUtilityDAO.solveAlert(recordId);
  }

}
