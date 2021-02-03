package sa.elm.ob.scm.actionHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.procurement.Requisition;
import org.springframework.util.StringUtils;

import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * This class is used to send notification(Alert) for user selected
 * 
 * This process will execute after approval of PR get finished by clicking notify user button
 * 
 * @author Sathishkumar.P
 *
 */

public class PRNotificationActionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(PRNotificationActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    JSONObject json = new JSONObject();
    JSONObject successMessage = new JSONObject();

    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");

      String requistionId = jsonRequest.getString("M_Requisition_ID");
      String userId = jsonparams.getString("user");
      String note = "null".equals(jsonparams.getString("note")) ? "" : jsonparams.getString("note");
      String clientId = jsonRequest.getString("inpadClientId");
      String orgId = jsonRequest.getString("inpadOrgId");
      String currentRoleId = OBContext.getOBContext().getRole().getId();
      String currentUserId = OBContext.getOBContext().getUser().getId();
      String alertWindow = AlertWindow.PurchaseRequisition;

      ArrayList<String> includeRecipient = new ArrayList<String>();

      if (StringUtils.isEmpty(userId)) {
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_Userismandatory"));
        json.put("message", successMessage);
        return json;
      }

      Requisition requistionObj = OBDal.getInstance().get(Requisition.class, requistionId);
      User userObj = OBDal.getInstance().get(User.class, userId);

      if (requistionObj != null) {
        if (userObj != null && userObj.getBusinessPartner() != null) {
          if (requistionObj.isEscmSendnotification()) {

            requistionObj.setEscmNotifyuser(userObj.getId());
            requistionObj.setEscmSendnotification(false);

            OBDal.getInstance().save(requistionObj);

            String notificationUser = userObj.getBusinessPartner().getSearchKey() + "-"
                + userObj.getBusinessPartner().getName();
            String notificationDescription = String.format(
                OBMessageUtils.messageBD("ESCM_Pr_Approved"), requistionObj.getDocumentNo());

            for (UserRoles userRole : userObj.getADUserRolesList()) {
              if (userRole.getRole() != null) {
                includeRecipient.add(userRole.getRole().getId());
                AlertUtility.alertInsertionRoleWithNotes(requistionObj.getId(),
                    requistionObj.getDocumentNo() + "-" + requistionObj.getDescription(),
                    userRole.getRole().getId(), userRole.getUserContact().getId(),
                    requistionObj.getClient().getId(), notificationDescription, "NEW", alertWindow,
                    "scm.pr.approved", Constants.GENERIC_TEMPLATE, note);
              }
            }

            HashSet<String> includedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = includedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
            }

            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", currentRoleId);
            historyData.put("UserId", currentUserId);
            historyData.put("HeaderId", requistionId);
            historyData.put("Comments",
                String.format(OBMessageUtils.messageBD("ESCM_Notifieduser"), notificationUser)
                    + note);
            historyData.put("Status", "NS");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);
            OBDal.getInstance().flush();

            successMessage.put("severity", "success");
            successMessage.put("text", OBMessageUtils.messageBD("ESCM_notificationsuccess"));
            json.put("message", successMessage);
            return json;
          } else {
            OBDal.getInstance().rollbackAndClose();
            successMessage.put("severity", "error");
            successMessage.put("text", OBMessageUtils.messageBD("ESCM_notificationfailure"));
            json.put("message", successMessage);
            return json;
          }
        }
      }

    } catch (Exception e) {
      log.debug("Exception while sending notifcation" + e.getMessage(), e);
      try {
        OBDal.getInstance().rollbackAndClose();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_notificationnotsent"));
        json.put("message", successMessage);
        return json;
      } catch (Exception ex) {
        // this case wont happen
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return successMessage;
  }
}
