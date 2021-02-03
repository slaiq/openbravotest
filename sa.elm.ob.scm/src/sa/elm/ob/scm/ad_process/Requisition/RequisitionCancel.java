package sa.elm.ob.scm.ad_process.Requisition;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 16/02/2017
 */
public class RequisitionCancel implements Process {
  /**
   * This servlet class was responsible for Requisition Cancel Process with Approval
   * 
   */
  private static final Logger log = Logger.getLogger(RequisitionCancel.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Cancel the Requisition");
    @SuppressWarnings("unused")
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
    Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objRequisition.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("notes").toString();
    String headerId = null;
    String appstatus = "", alertWindow = AlertWindow.PurchaseRequisition;
    String windowId = "800092";

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = true;
    String errorMsg = "";
    int count = 0;

    log.debug("Requisition Id:" + strRequisitionId);

    if (objRequisition.getEscmDocStatus().equals("ESCM_CA")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Cancelled_Already@");
      bundle.setResult(result);
      return;
    }

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          objRequisition.setUpdated(new java.util.Date());
          objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
          objRequisition.setEscmDocStatus("ESCM_CA");
          objRequisition.setEscmDocaction("PD");
          objRequisition.setEutNextRole(null);
          OBDal.getInstance().save(objRequisition);
          OBDal.getInstance().flush();
          // delete alert for approval alerts
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey=:reqID and e.alertStatus='NEW'");
          alertQuery.setNamedParameter("reqID", objRequisition.getId());
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              OBDal.getInstance().remove(objAlert);
            }
          }
          // get Requisition Alert
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id=:clientID and e.eSCMProcessType=:processType ");
          queryAlertRule.setNamedParameter("clientID", clientId);
          queryAlertRule.setNamedParameter("processType", alertWindow);

          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            objRule.getId();
          }

          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_REQUISITION);
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_REQUISITION_LIMITED);
          headerId = objRequisition.getId();
          if (!StringUtils.isEmpty(objRequisition.getId())) {
            appstatus = "CA";
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", headerId);
            historyData.put("Comments", comments);
            historyData.put("Status", appstatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

            count = Utility.InsertApprovalHistory(historyData);
          }
          if (count > 0 && !StringUtils.isEmpty(objRequisition.getId())) {
            String description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pr.rejected",
                vars.getLanguage());
            // set alert for Budget Controller
            if (objRequisition.isEfinEncumbered() != null && objRequisition.isEfinEncumbered()) {
              AlertUtility.alertInsertionPreference(objRequisition.getId(),
                  objRequisition.getDocumentNo(), "ESCM_BudgetControl",
                  objRequisition.getClient().getId(), description, "NEW", alertWindow,
                  "scm.pr.rejected", Constants.GENERIC_TEMPLATE, windowId, null);
            }
            // check current role exists in document rule ,if it is not there then delete Delete it
            // why ??? current user only already approved
            /*
             * String checkQuery =
             * "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '" +
             * vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";
             * 
             * OBQuery<Requisition> checkRecipientQry = OBDal.getInstance().createQuery(
             * Requisition.class, checkQuery); if (checkRecipientQry.list().size() == 0) {
             * OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance().createQuery(
             * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId +
             * "' and e.role.id='" + vars.getRole() + "'"); if (currentRoleQuery.list().size() > 0)
             * { for (AlertRecipient delObject : currentRoleQuery.list()) {
             * OBDal.getInstance().remove(delObject); } } }
             */
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@ESCM_Requisition_Cancelled@");
            bundle.setResult(result);
            return;
          }
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);

          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);

          OBDal.getInstance().rollbackAndClose();

        }
      }

      else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }

      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception in Requisition Cancel:", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
