package sa.elm.ob.scm.ad_process.Requisition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMPurchaseReqAppHist;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gokul 20/06/20
 *
 */

public class ReNotifyUser extends DalBaseProcess {
  /**
   * This class will re notify the user after the notify user returned
   */
  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    final Logger log = LoggerFactory.getLogger(ReNotifyUser.class);
    final OBError obError = new OBError();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User user = null;
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);

      OBQuery<ESCMPurchaseReqAppHist> acthistqry = OBDal.getInstance().createQuery(
          ESCMPurchaseReqAppHist.class, " as e where e.requisition.id = '" + strRequisitionId
              + "' and e.purchasereqaction='RETURN' order by e.creationDate desc");
      acthistqry.setMaxResult(1);

      ESCMPurchaseReqAppHist acthist = OBDal.getInstance().get(ESCMPurchaseReqAppHist.class,
          acthistqry.list().get(0).getId());
      user = OBDal.getInstance().get(User.class, acthist.getCreatedBy().getId());
      // User user = objRequisition.getEscmNotifyuser();
      ArrayList<String> includeRecipient = new ArrayList<String>();
      String currentRoleId = OBContext.getOBContext().getRole().getId();
      String currentUserId = OBContext.getOBContext().getUser().getId();
      String alertWindow = AlertWindow.PurchaseRequisition;
      String note = "";

      if (objRequisition != null) {
        if (user != null && user.getBusinessPartner() != null) {
          if (!objRequisition.isEscmSendnotification()) {

            objRequisition.setEscmNotifyuser(user.getId());
            objRequisition.setEscmSendnotification(false);
            objRequisition.setEscmPrReturn(false);

            OBDal.getInstance().save(objRequisition);

            // solve alerts
            AlertUtility.solveAlerts(objRequisition.getId());

            String notificationUser = user.getBusinessPartner().getSearchKey() + "-"
                + user.getBusinessPartner().getName();
            String notificationDescription = String.format(
                OBMessageUtils.messageBD("ESCM_Pr_Approved"), objRequisition.getDocumentNo());

            for (UserRoles userRole : user.getADUserRolesList()) {
              if (userRole.getRole() != null) {
                includeRecipient.add(userRole.getRole().getId());
                AlertUtility.alertInsertionRoleWithNotes(objRequisition.getId(),
                    objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                    userRole.getRole().getId(), userRole.getUserContact().getId(),
                    objRequisition.getClient().getId(), notificationDescription, "NEW", alertWindow,
                    "scm.pr.renotify", Constants.GENERIC_TEMPLATE, note);
              }
            }

            HashSet<String> includedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = includedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null,
                  objRequisition.getClient().getId(), alertWindow);
            }

            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", objRequisition.getClient().getId());
            historyData.put("OrgId", objRequisition.getOrganization().getId());
            historyData.put("RoleId", currentRoleId);
            historyData.put("UserId", currentUserId);
            historyData.put("HeaderId", objRequisition.getId());
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

            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@ESCM_notificationsuccess@");
            bundle.setResult(result);
            return;

          } else {
            objRequisition.setEscmSendnotification(true);
            objRequisition.setEscmPrReturn(false);

            OBDal.getInstance().save(objRequisition);

            // solve alerts
            AlertUtility.solveAlerts(objRequisition.getId());

            String notificationUser = user.getBusinessPartner().getSearchKey() + "-"
                + user.getBusinessPartner().getName();
            String notificationDescription = String.format(
                OBMessageUtils.messageBD("ESCM_Pr_Approved"), objRequisition.getDocumentNo());

            for (UserRoles userRole : user.getADUserRolesList()) {
              if (userRole.getRole() != null) {
                includeRecipient.add(userRole.getRole().getId());
                AlertUtility.alertInsertionRoleWithNotes(objRequisition.getId(),
                    objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                    userRole.getRole().getId(), userRole.getUserContact().getId(),
                    objRequisition.getClient().getId(), notificationDescription, "NEW", alertWindow,
                    "scm.pr.renotify", Constants.GENERIC_TEMPLATE, note);
              }
            }

            HashSet<String> includedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = includedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null,
                  objRequisition.getClient().getId(), alertWindow);
            }

            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", objRequisition.getClient().getId());
            historyData.put("OrgId", objRequisition.getOrganization().getId());
            historyData.put("RoleId", currentRoleId);
            historyData.put("UserId", currentUserId);
            historyData.put("HeaderId", objRequisition.getId());
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

            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@ESCM_notificationsuccess@");
            bundle.setResult(result);
            return;
          }
        }
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while ReNotifyUser: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      log.debug("Exeception in Requisition Submit:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBDal.getInstance().commitAndClose();
      // close db connection

      OBContext.restorePreviousMode();
    }

  }

}
