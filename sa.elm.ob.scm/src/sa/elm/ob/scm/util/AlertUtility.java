package sa.elm.ob.scm.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openbravo.model.ad.alert.AlertRecipient;

/**
 * @auther gopalakrishnan on 21/02/2017
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
   * @param window
   *          name
   * @param alertKey
   * @param mailTemplate
   *          name
   * @return True --Alert Created, False --Error
   */
  public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate, String windowId, String createdUserId) {
    return AlertUtilityDAO.alertInsertionPreference(DocumentId, DocumentNo, property, clientId,
        description, status, Window, alertKey, mailTemplate, windowId, createdUserId);
  }

  // public static Boolean alertInsertionPreference(String DocumentId, String DocumentNo,
  // String property, String clientId, String description, String status, String Window) {
  // // return AlertUtilityDAO.alertInsertionPreference(DocumentId, DocumentNo, property, clientId,
  // // description, status, Window);
  // return null;
  // }

  // /**
  // * This method only for Purchase Requisition Alert Process
  // *
  // * @param property
  // * preference
  // * @param clientId
  // * @param description
  // * @param status
  // * NEW-new Alert,SOLVED-alert Solved
  // * @return True --Alert Created, False --Error
  // */
  // public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
  // String userId, String clientId, String description, String status, String Window) {
  // // return AlertUtilityDAO.alertInsertionRole(DocumentId, DocumentNo, roleId, userId, clientId,
  // // description, status, Window);
  // return null;
  // }

  /**
   * Method to send alerts and emails
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
   * @param emailTemplate
   * @return
   */
  public static Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window,
      String alertKey, String emailTemplate) {

    return AlertUtilityDAO.alertInsertionRole(DocumentId, DocumentNo, roleId, userId, clientId,
        description, status, Window, alertKey, emailTemplate);

  }

  public static String convertMapToString(HashMap<String, String> input) {
    String json = null;
    try {
      json = new ObjectMapper().writeValueAsString(input);
    } catch (JsonGenerationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonMappingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return json;
  }

  @SuppressWarnings("unchecked")
  public static HashMap<String, String> getMapFromString(String mapStr) {
    HashMap<String, String> result = null;
    try {
      result = new ObjectMapper().readValue(mapStr, HashMap.class);
    } catch (JsonParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonMappingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
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
   * Method to send alerts and emails
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
   * @param emailTemplate
   * @param notes
   * @return
   */
  public static Boolean alertInsertionRoleWithNotes(String DocumentId, String DocumentNo,
      String roleId, String userId, String clientId, String description, String status,
      String Window, String alertKey, String emailTemplate, String notes) {

    return AlertUtilityDAO.alertInsertionRoleWithNotes(DocumentId, DocumentNo, roleId, userId,
        clientId, description, status, Window, alertKey, emailTemplate, notes);

  }

  // public static void main(String[] args) {
  // HashMap<String, String> map = new HashMap<>();
  // map.put("userName", "Gopal");
  // map.put("alertDescription",
  // "Test : gopal : Material Issue Request Waiting for Approval from فيصل العثيمين");
  // System.out.println("String output:" + convertMapToString(map));
  // }

  /**
   * Returns list of VisibleAtRoles from Preference configuration based on Property
   * 
   * @param property
   * @param clientId
   * @param windowId
   * @param createdUserId
   * @return List of Strings containing VisibleAtRoleIds
   */
  public static List<String> getVisibleAtRole(String property, String clientId, String windowId,
      String createdUserId) {
    return AlertUtilityDAO.getVisibleAtRole(property, clientId, windowId, createdUserId);
  }

  /**
   * Method to get alert rule Id
   * 
   * @param clientId
   * @param alertWindow
   * @return
   */
  public static String getAlertRule(String clientId, String alertWindow) {
    return AlertUtilityDAO.getAlertRule(clientId, alertWindow);
  }

  /**
   * Method to get alert rule Id
   * 
   * @param alertRuleId
   * @return
   */
  public static List<AlertRecipient> getAlertReceipient(String alertRuleId) {
    return AlertUtilityDAO.getAlertReceipient(alertRuleId);
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

  /**
   * Method to delete approval alerts
   * 
   * @param recordId
   * @return
   */
  public static Boolean deleteAlerts(String recordId) {
    return AlertUtilityDAO.deleteAlert(recordId);
  }

  /**
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
   * @return
   */
  public static Boolean alertInsertBasedonPreference(String DocumentId, String DocumentNo,
      String property, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate, String windowId, String createdUserId) {
    return AlertUtilityDAO.alertInsertBasedonPreference(DocumentId, DocumentNo, property, clientId,
        description, status, Window, alertKey, mailTemplate, windowId, createdUserId);
  }

}
