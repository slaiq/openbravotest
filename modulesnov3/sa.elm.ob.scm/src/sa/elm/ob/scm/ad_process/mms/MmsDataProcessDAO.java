package sa.elm.ob.scm.ad_process.mms;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dao.CreatePoReceiptDAO;
import sa.elm.ob.scm.webservice.dao.ResponseDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.utility.util.Constants;

public class MmsDataProcessDAO {
  private static final Logger log = LoggerFactory.getLogger(MmsDataProcessDAO.class);

  @SuppressWarnings("unchecked")
  public static JSONObject processMMSData(VariablesSecureApp vars) throws JSONException {
    JSONObject resultObj = new JSONObject();
    try {
      String sql = "";
      List<Object> RequestData = new ArrayList<Object>();
      sql = " select request_no,parameters from escm_mms_integrationinput where isactive='N' order by created ";

      SQLQuery ps = OBDal.getInstance().getSession().createSQLQuery(sql);
      if (ps != null) {
        RequestData = ps.list();

        if (RequestData.size() > 0) {
          for (Object resultData : RequestData) {
            Object[] details = (Object[]) resultData;
            resultObj = new JSONObject();
            JSONObject json0 = new JSONObject();
            String message = "";
            boolean status = true;
            try {
              JSONObject jsonObject = new JSONObject(details[1].toString());
              JSONArray lineArray = jsonObject.getJSONArray("lineDTO");
              json0 = lineArray.getJSONObject(0);
              String contractNo = jsonObject.get("poContractNo").toString();
              Order order = null;

              String orderId = "";
              List idList = CreatePoReceiptDAO.getOrderIdbyContractNo(contractNo);
              if (idList.size() > 1) {
                resultObj.put("status", false);
                resultObj.put("message",
                    String.format(OBMessageUtils.messageBD("ESCM_ordernotexists"), contractNo));
                resultObj.put("orderId", "000");
                resultObj.put("reqNo", details[0].toString());
                return resultObj;
              } else if (idList.size() > 0) {
                Object row = (Object) idList.get(0);
                orderId = (String) row;
              } else {
                resultObj.put("status", false);
                resultObj.put("message",
                    String.format(OBMessageUtils.messageBD("ESCM_ordernotexists"), contractNo));
                resultObj.put("orderId", "000");
                resultObj.put("reqNo", details[0].toString());
                return resultObj;
              }
              order = OBDal.getInstance().get(Order.class, orderId);
              jsonObject.put("orderId", order.getId());
              jsonObject.put("poContractNo", order.getEscmMaintenanceCntrctNo());

              json0.put("itemDescription", order.getOrderLineList().get(0).getEscmProdescription());
              json0.put("itemNo", JSONObject.NULL);
              BigDecimal amt0 = BigDecimal.ZERO;

              for (int i = 1; i < lineArray.length(); i++) {
                JSONObject json = lineArray.getJSONObject(i);
                BigDecimal amtRel0 = new BigDecimal(json0.getString("amountToRelease"));
                BigDecimal qtyRel0 = new BigDecimal(json0.getString("qtyToRelease"));
                BigDecimal unitPrice0 = new BigDecimal(json0.getString("unitPrice"));
                if (i == 1) {
                  amt0 = qtyRel0.multiply(unitPrice0);
                } else {
                  amt0 = qtyRel0;
                }

                // convert multiple line into singleLine
                if (amtRel0.compareTo(BigDecimal.ZERO) > 0) {
                  BigDecimal amtrelease = new BigDecimal(json.getString("amountToRelease"));
                  json0.put("amountToRelease", amtRel0.add(amtrelease));
                  json0.put("qtyToRelease", amtRel0.add(amtrelease));
                } else {
                  BigDecimal qtyRelease = new BigDecimal(json.getString("qtyToRelease"));
                  BigDecimal unitPrice = new BigDecimal(json.getString("unitPrice"));
                  json0.put("qtyToRelease", amt0.add(qtyRelease.multiply(unitPrice)));
                }

                // move Hold to first line
                if (json.has("holdDTO") && !json.isNull("holdDTO")) {
                  JSONArray holdJsonArray = json.getJSONArray("holdDTO");
                  if (json0.has("holdDTO") && !json0.isNull("holdDTO")
                      && json0.getJSONArray("holdDTO") != null) {
                    for (int j = 0; j < holdJsonArray.length(); j++) {
                      JSONObject holdJson = holdJsonArray.getJSONObject(j);
                      json0.getJSONArray("holdDTO").put(holdJson);
                    }
                  } else {
                    json0.put("holdDTO", holdJsonArray);
                  }
                }

                // move Penalty to first line
                /*
                 * if (json.has("penaltyDTO") && !json.isNull("penaltyDTO")) { JSONArray
                 * penaltyJsonArray = json.getJSONArray("penaltyDTO"); for (int k = 0; k <
                 * penaltyJsonArray.length(); k++) { JSONObject penaltyJson =
                 * penaltyJsonArray.getJSONObject(k); if (json0.has("penaltyDTO") &&
                 * !json0.isNull("penaltyDTO") && json0.getJSONArray("penaltyDTO") != null) {
                 * json0.getJSONArray("penaltyDTO").put(penaltyJson); } else {
                 * json0.put("penaltyDTO", penaltyJson); } } }
                 */

                // move Penalty to first line
                if (json.has("penaltyDTO") && !json.isNull("penaltyDTO")) {
                  JSONArray penaltyJsonArray = json.getJSONArray("penaltyDTO");
                  if (json0.has("penaltyDTO") && !json0.isNull("penaltyDTO")
                      && json0.getJSONArray("penaltyDTO") != null) {
                    for (int k = 0; k < penaltyJsonArray.length(); k++) {
                      JSONObject penaltyJson = penaltyJsonArray.getJSONObject(k);
                      json0.getJSONArray("penaltyDTO").put(penaltyJson);
                    }
                  } else {
                    json0.put("penaltyDTO", penaltyJsonArray);
                  }
                }
              }

              // remove other than 1st line in json
              JSONArray tmp = new JSONArray();
              JSONArray jsonArray = lineArray;
              if (jsonArray != null) {
                tmp.put(jsonArray.get(0));
              }
              lineArray = tmp;
              jsonObject.put("lineDTO", lineArray);

              log.info("json Mapper input " + jsonObject.toString());

              // calling webservice
              ObjectMapper mapper = new ObjectMapper();
              MmsCreatePoReceipt receiptController = new MmsCreatePoReceipt();
              PoReceiptHeaderDTO originalRequest = mapper.readValue(jsonObject.toString(),
                  PoReceiptHeaderDTO.class);
              ResponseEntity<ResponseDTO> response = receiptController
                  .createPoReceipt(originalRequest, vars);
              if (response.getBody().getStatus().equals(WebserviceConstants.SUCCESS)) {
                // set active 'Y' for processed records.
                PreparedStatement st = null;
                st = OBDal.getInstance().getConnection().prepareStatement(
                    "update escm_mms_integrationinput set isactive='Y' where request_no=?");
                st.setString(1, details[0].toString());
                st.executeUpdate();
                OBDal.getInstance().flush();
              } else {
                message = response.getBody().getResponseNo() + ":"
                    + response.getBody().getErrorMsg();
                status = false;
                resultObj.put("status", status);
                resultObj.put("message", message);
                resultObj.put("orderId", order.getId());
                resultObj.put("reqNo", details[0].toString());
                return resultObj;
              }
            } catch (Exception err) {
              OBDal.getInstance().rollbackAndClose();
              err.printStackTrace();
              log.error("Error while parsing json data ReqNo:", details[0].toString());
              log.error("Error while parsing json data", err.toString());
              message = "Process failed for :" + details[0].toString()
                  + " ,Please contact your System Admin";
              status = false;
              resultObj.put("status", status);
              resultObj.put("message", message);
              resultObj.put("orderId", "000");
              resultObj.put("reqNo", details[0].toString());
              return resultObj;
            }
          }
        }
      }
      resultObj.put("status", true);
      resultObj.put("message", "Process completed");
      return resultObj;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while MmsDataProcessDAO:ProcessData" + e);
      resultObj.put("status", false);
      resultObj.put("message", "Process Failed");
      resultObj.put("orderId", "000");
      resultObj.put("reqNo", "000");
      return resultObj;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * insert alert for mms script if any error.
   * 
   * @param jsonResult
   * @param alertWindow
   * @param clientId
   */
  public static void insertAlertsforMMS(JSONObject jsonResult, String alertWindow,
      String clientId) {
    String alertRuleId = "";
    String appResource = "scm.mms.script.error";
    List<AlertRecipient> recipientList = new ArrayList<>();
    try {
      alertRuleId = getAlertRule(clientId, alertWindow);
      if (StringUtils.isNotEmpty(alertRuleId)) {
        OBQuery<AlertRecipient> alertRecipientList = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "alertRule.id=:alertRuleId");
        alertRecipientList.setNamedParameter("alertRuleId", alertRuleId);
        alertRecipientList.setFilterOnReadableOrganization(false);
        alertRecipientList.setFilterOnReadableClients(false);
        recipientList = alertRecipientList.list();
        if (recipientList.size() > 0) {
          for (AlertRecipient recipient : recipientList) {
            // insert alert
            alertInsertionRole(jsonResult.get("orderId").toString(),
                jsonResult.get("reqNo").toString(),
                recipient.getRole() == null ? "" : recipient.getRole().getId(),
                recipient.getUserContact() == null ? "" : recipient.getUserContact().getId(),
                clientId, jsonResult.get("message").toString(), "NEW", alertWindow, appResource,
                Constants.GENERIC_TEMPLATE);
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while MmsDataProcessDAO:insertAlertsforMMS" + e);
    }
  }

  /**
   * This method only for MMS Scheduler Script Alert Process
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
      // get mms Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:processType "
              + " order by e.creationDate desc");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("processType", Window);
      queryAlertRule.setFilterOnReadableOrganization(false);
      queryAlertRule.setFilterOnReadableClients(false);
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
      OBDal.getInstance().save(objAlert);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      log.error("Exception in alertInsertionRole", e);
    }
    return isSuccess;
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
      queryAlertRule.setFilterOnReadableClients(false);
      queryAlertRule.setFilterOnReadableOrganization(false);
      alertRuleList = queryAlertRule.list();

      if (alertRuleList.size() > 0) {
        AlertRule objRule = alertRuleList.get(0);
        alertRuleId = objRule.getId();
      }
    } catch (OBException e) {
      log.error("Exception while getAlertRule:", e);
      throw new OBException(e.getMessage());
    }
    return alertRuleId;
  }

}