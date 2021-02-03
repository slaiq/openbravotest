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
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
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

public class MaterialIssueRequestRevoke implements Process {
  private static final Logger log = Logger.getLogger(MaterialIssueRequestRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    // Connection connection = null;
    /*
     * try { ConnectionProvider provider = bundle.getConnection(); connection =
     * provider.getConnection(); } catch (NoConnectionAvailableException e) {
     * log.error("No Database Connection Available.Exception:" + e); throw new RuntimeException(e);
     * }
     */
    final String Material_ID = (String) bundle.getParams().get("Escm_Material_Request_ID")
        .toString();
    MaterialIssueRequest Mrequest = OBDal.getInstance().get(MaterialIssueRequest.class,
        Material_ID);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = Mrequest.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();

    MaterialIssueRequest headerId = null;
    String appstatus = "", alertWindow = AlertWindow.IssueRequest;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    String Description = "", lastWaitingRoleId = "";
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    try {
      OBContext.setAdminMode();

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      OBQuery<MaterialIssueRequestHistory> history = OBDal.getInstance().createQuery(
          MaterialIssueRequestHistory.class,
          " as e where e.escmMaterialRequest.id=:mirID order by e.creationDate desc ");
      history.setNamedParameter("mirID", Material_ID);
      history.setMaxResult(1);
      if (history.list().size() > 0) {
        MaterialIssueRequestHistory apphistory = history.list().get(0);
        if (apphistory.getRequestreqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      MaterialIssueRequest headerCheck = OBDal.getInstance().get(MaterialIssueRequest.class,
          Material_ID);

      if (headerCheck.getAlertStatus().equals("ESCM_TR")
          || headerCheck.getAlertStatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      int count = 0;
      boolean errorFlag = true;
      if (errorFlag) {
        MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
            Material_ID);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAlertStatus("DR");
        if (header.isSiteissuereq()) {
          header.setEscmSmirAction("CO");

        } else {
          header.setEscmAction("CO");
        }
        // task 4813 #14218
        header.setEUTNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.MATERIAL_ISSUE_REQUEST_IT);

        // Removing Forward and RMI Id
        if (header.getEUTForward() != null) {
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForward());
          forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
              Constants.MATERIAL_ISSUE_REQUEST);
        }
        if (header.getEUTReqmoreinfo() != null) {
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
          forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
              Constants.MATERIAL_ISSUE_REQUEST);
        }

        headerId = header;
        if (!StringUtils.isEmpty(headerId.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", headerId.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          if (header.isSiteissuereq()) {
            alertWindow = AlertWindow.SiteIssueRequest;
          } else {
            alertWindow = AlertWindow.IssueRequest;
          }
          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey=:mirID and e.alertStatus='NEW'");
          alertQuery.setNamedParameter("mirID", Material_ID);
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              lastWaitingRoleId = objAlert.getRole().getId();
              OBDal.getInstance().save(objAlert);
            }
          }
          // get alert recipients - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

          // check and insert alert recipient
          if (alrtRecList.size() > 0) {
            for (AlertRecipient objAlertReceipient : alrtRecList) {
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
          NextRoleByRuleVO nextApproval = NextRoleByRule.getMIRRevokeRequesterNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
              Mrequest.getEscmDocumenttype(), header.getRole().getId());
          EutNextRole nextRole = null;
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

          // set alert for next approver
          if (header.isSiteissuereq()) {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.smir.revoked", Lang)
                + " " + header.getCreatedBy().getName();
          } else {
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.mir.revoked", Lang)
                + " " + header.getCreatedBy().getName();
          }
          // set revoke alert to last waiting role

          AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), lastWaitingRoleId,
              "", header.getClient().getId(), Description, "NEW", alertWindow, "scm.mir.revoked",
              Constants.GENERIC_TEMPLATE);
          String lastWaitingRoleIdTemp = "";
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            lastWaitingRoleIdTemp = objNextRoleLine.getRole().getId();

            // validating the role id whether to insert alert or not, in order to avoid duplicate
            if (lastWaitingRoleId.equals(lastWaitingRoleIdTemp)) {

            } else {
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                  objNextRoleLine.getRole().getId(), "", header.getClient().getId(), Description,
                  "NEW", alertWindow, "scm.mir.revoked", Constants.GENERIC_TEMPLATE);
            }

            obError.setType("Success");
            obError.setTitle("Success");
            obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke"));
          }

          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }
        bundle.setResult(obError);

      }
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke Material Issue Request Revoke :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
