package sa.elm.ob.scm.actionHandler;

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
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ESCMCustodytransferHist;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class ReturnTransactionRevoke implements Process {
  private static final Logger log = Logger.getLogger(MaterialIssueRequestRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    OBContext.setAdminMode();
    final String inoutId = (String) bundle.getParams().get("M_InOut_ID").toString();
    ShipmentInOut returnTran = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = returnTran.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();

    String appstatus = "", alertWindow = AlertWindow.ReturnTransaction;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    String Description = "", lastWaitingRoleId = "";
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    List<AlertRule> alertruleList = new ArrayList<AlertRule>();

    try {
      OBContext.setAdminMode();

      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id=:clientID and e.eSCMProcessType=:processType");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("processType", alertWindow);
      alertruleList = queryAlertRule.list();
      if (alertruleList.size() > 0) {
        AlertRule objRule = alertruleList.get(0);
        alertRuleId = objRule.getId();
      }
      OBQuery<ESCMCustodytransferHist> history = OBDal.getInstance().createQuery(
          ESCMCustodytransferHist.class,
          " as e where e.goodsShipment.id=:returnTransID order by e.creationDate desc ");
      history.setNamedParameter("returnTransID", returnTran);
      history.setMaxResult(1);
      if (history.list().size() > 0) {
        ESCMCustodytransferHist apphistory = history.list().get(0);
        if (apphistory.getRequestreqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }

      if (returnTran.getEscmDocstatus().equals("ESCM_TR")
          || returnTran.getEscmDocstatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      int count = 0;
      boolean errorFlag = true;
      if (errorFlag) {

        returnTran.setUpdated(new java.util.Date());
        returnTran.setUpdatedBy(OBContext.getOBContext().getUser());
        returnTran.setEscmDocstatus("DR");
        returnTran.setEscmDocaction("CO");
        returnTran.setEutNextRole(null);
        OBDal.getInstance().save(returnTran);
        OBDal.getInstance().flush();
        // delete the unused nextroles in eut_next_role table.
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.Return_Transaction);

        // Removing forwardRMI id
        if (returnTran.getEutForward() != null) {
          // Removing the Role Access given to the forwarded user
          // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
          // set status as DR in forward Record
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(returnTran.getEutForward());
          // Removing Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(returnTran.getId(),
              Constants.RETURN_TRANSACTION);

        }
        if (returnTran.getEutReqmoreinfo() != null) {
          // access remove
          // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmiId, conn);
          // set status as DR in forward Record
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(returnTran.getEutReqmoreinfo());
          // Remove Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(returnTran.getId(),
              Constants.RETURN_TRANSACTION);

        }

        // -------------
        if (!StringUtils.isEmpty(returnTran.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", returnTran.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log.debug("headerId:" + returnTran.getId());
        log.debug("count:" + returnTran);

        if (count > 0 && !StringUtils.isEmpty(returnTran.getId())) {
          Role objCreatedRole = null;
          if (returnTran.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = returnTran.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          alertWindow = AlertWindow.ReturnTransaction;
          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey=:inoutID and e.alertStatus='NEW'");
          alertQuery.setNamedParameter("inoutID", inoutId);
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              lastWaitingRoleId = objAlert.getRole().getId();
              OBDal.getInstance().save(objAlert);
            }
          }
          // check and insert alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id=:alertRuleID ");
          receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          if (objCreatedRole != null)
            includeRecipient.add(objCreatedRole.getId());
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }
          /*
           * NextRoleByRuleVO nextApproval = NextRoleByRule.getMIRRevokeRequesterNextRole(OBDal
           * .getInstance().getConnection(), clientId, orgId, roleId, userId,
           * sa.elm.ob.utility.properties.Resource.Return_Transaction, returnTran.getEscmAdRole()
           * .getId());
           */
          /*
           * EutNextRole nextRole = null; nextRole = OBDal.getInstance().get(EutNextRole.class,
           * nextApproval.getNextRoleId());
           */

          Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.rt.revoked", Lang) + " "
              + returnTran.getCreatedBy().getName();

          // set alert for next approver
          AlertUtility.alertInsertionRole(returnTran.getId(), returnTran.getDocumentNo(),
              lastWaitingRoleId, "", returnTran.getClient().getId(), Description, "NEW",
              alertWindow, "scm.rt.revoked", Constants.GENERIC_TEMPLATE);

          /*
           * // set revoke alert to last approved role for (EutNextRoleLine objNextRoleLine :
           * nextRole.getEutNextRoleLineList()) {
           * AlertUtility.alertInsertionRole(returnTran.getId(), returnTran.getDocumentNo(),
           * objNextRoleLine.getRole().getId(), "", returnTran.getClient().getId(), Description,
           * "NEW", alertWindow);
           * 
           * obError.setType("Success"); obError.setTitle("Success");
           * obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke")); }
           */

          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke"));

          OBDal.getInstance().save(returnTran);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }
        bundle.setResult(obError);

      }
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke return transaction Revoke :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
