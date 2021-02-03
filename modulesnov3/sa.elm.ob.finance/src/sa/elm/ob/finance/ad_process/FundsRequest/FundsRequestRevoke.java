package sa.elm.ob.finance.ad_process.FundsRequest;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian
 * 
 */

public class FundsRequestRevoke implements Process {
  /**
   * This process allow the user to edit the submitted budget.
   */
  private static final Logger log = Logger.getLogger(FundsRequestRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = (String) bundle.getContext().getOrganization();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    final String fundsReqId = (String) bundle.getParams().get("Efin_Fundsreq_ID").toString();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    Connection conn = OBDal.getInstance().getConnection();
    ArrayList<String> includeRecipient = new ArrayList<String>();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    String comments = (String) bundle.getParams().get("comments").toString(), docType = null;
    String appstatus = "", alertWindow = AlertWindow.BudgetDistribution, alertRuleId = null;
    final EFINFundsReq req = OBDal.getInstance().get(EFINFundsReq.class, fundsReqId);
    // get alert rule id
    OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
        "as e where e.client.id= :clientID and e.efinProcesstype= :ProcesstypeID ");
    queryAlertRule.setNamedParameter("clientID", clientId);
    queryAlertRule.setNamedParameter("ProcesstypeID", alertWindow);
    List<AlertRule> queryAlertRuleList = queryAlertRule.list();
    if (queryAlertRuleList.size() > 0) {
      AlertRule objRule = queryAlertRuleList.get(0);
      alertRuleId = objRule.getId();
    }

    // based on transaction type set the document rule BCUR means BCU Funds Request Approval flow
    // or else ORG Funds Request Approval flow
    if (req.getTransactionType().equals("BCUR")) {
      docType = Resource.BCU_BUDGET_DISTRIBUTION;
    } else
      docType = Resource.ORG_BUDGET_DISTRIBUTION;

    // After Approve or Rework by approver if submiter is try to Revoke the same record then throw
    // error
    if ((req.getDocumentStatus().equals("AP")) || (req.getDocumentStatus().equals("RW"))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    try {
      OBContext.setAdminMode(true);
      if (!req.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }

      /*
       * ischkReserveIsDoneorNot = UtilityDAO.chkReserveIsDoneorNot(conn, clientId, orgId, roleId,
       * userId, docType, BigDecimal.ZERO);
       */
      // if reserve then reactivate the budget inquiry changes
      if (req.isReserve()) {
        FundsRequestActionDAO.reactivateBudgetInqchanges(conn, fundsReqId, req.isReserve(), false);
      }
      // update the funds request header status as draft
      req.setUpdated(new java.util.Date());
      req.setUpdatedBy(OBContext.getOBContext().getUser());
      req.setDocumentStatus("DR");
      req.setAction("CO");
      req.setNextRole(null);
      req.setRevoke(false);
      req.setReserve(false);

      log.debug("header:" + req.toString());
      OBDal.getInstance().save(req);
      OBDal.getInstance().flush();

      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), docType);
      if (!StringUtils.isEmpty(fundsReqId)) {
        appstatus = "REV";

        // insert into funds request approval history
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", fundsReqId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.Budget_Distribution_History);
        historyData.put("HeaderColumn", ApprovalTables.Budget_Distribution_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Budget_Distribution_DOCACTION_COLUMN);
        Utility.InsertApprovalHistory(historyData);
      }
      if (!StringUtils.isEmpty(fundsReqId)) {

        Role objCreatedRole = null;
        if (req.getCreatedBy().getADUserRolesList().size() > 0) {
          if (req.getRole() != null)
            objCreatedRole = req.getRole();
          else
            objCreatedRole = req.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        // check and insert alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.referenceSearchKey= :referenceSearchKey and e.alertStatus='NEW' ");
        alertQuery.setNamedParameter("referenceSearchKey", req.getId());
        List<Alert> alertQueryList = alertQuery.list();
        if (alertQueryList.size() > 0) {
          for (Alert objAlert : alertQueryList) {
            objAlert.setAlertStatus("SOLVED");
          }
        }

        String Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.fundsreq.revoked", vars.getLanguage()) + " " + objUser.getName();
        // delete the unused nextroles in eut_next_role table.
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), docType);
        AlertUtility.alertInsertionRole(req.getId(), req.getDocumentNo(), "",
            req.getCreatedBy().getId(), req.getClient().getId(), Description, "NEW", alertWindow,
            "finance.fundsreq.revoked", Constants.GENERIC_TEMPLATE);

        // Removing forwardRMI id
        if (req.getEUTForward() != null) {
          // Removing the Role Access given to the forwarded user
          // update status as "DR"
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(req.getEUTForward());
          // Removing Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(req.getId(),
              Constants.FundsReqMgmt);

        }
        if (req.getEUTReqmoreinfo() != null) {
          // access remove
          // update status as "DR"
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(req.getEUTReqmoreinfo());

          // Remove Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(req.getId(), Constants.FundsReqMgmt);

        }

        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("EFIN_FundsReq_RevokeSuccess"));
      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Efin_FundsReq_RevokeNotSuccess"));
      }
      bundle.setResult(obError);

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke budget :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
