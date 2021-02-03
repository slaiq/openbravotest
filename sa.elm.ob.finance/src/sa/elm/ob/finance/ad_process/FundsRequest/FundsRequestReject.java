package sa.elm.ob.finance.ad_process.FundsRequest;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class FundsRequestReject implements Process {
  private static final Logger log = Logger.getLogger(FundsRequestReject.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("rework the budget");
    Connection conn = OBDal.getInstance().getConnection();
    final String fundsReqId = (String) bundle.getParams().get("Efin_Fundsreq_ID").toString();
    final String clientId = (String) bundle.getContext().getClient();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String appstatus = "", alertWindow = AlertWindow.BudgetDistribution, alertRuleId = null;
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    EFINFundsReq fundsReq = OBDal.getInstance().get(EFINFundsReq.class, fundsReqId);
    ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());

    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", docType = null;
    int count = 0;
    Date CurrentDate = new Date();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = fundsReq.getEUTForward();

    String Lang = vars.getLanguage();

    /*
     * // If one user Approved the record and second user try to Rework same record with same role
     * // then throw error if (fundsReq.getDocumentStatus().equals("WFA")) {
     * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
     * "error", "@Efin_AlreadyPreocessed_Approve@"); bundle.setResult(result); return; }
     */
    // based on transaction type set the document rule BCUR means BCU Funds Request Approval flow
    // or else ORG Funds Request Approval flow

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          Boolean allowReject = false;
          if (fundsReq.getEUTForward() != null) {
            allowReject = forwardReqMoreInfoDAO.allowApproveReject(fundsReq.getEUTForward(), userId,
                roleId, docType);
          }
          if (fundsReq.getEUTReqmoreinfo() != null
              || ((fundsReq.getEUTForward() != null) && (!allowReject))) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }

          final String orgId = fundsReq.getOrganization().getId();

          log.debug("budgetId:" + fundsReq);

          if (fundsReq.getTransactionType().equals("BCUR")) {
            docType = Resource.BCU_BUDGET_DISTRIBUTION;
          } else
            docType = Resource.ORG_BUDGET_DISTRIBUTION;

          // After Revoked by submiter if approver is try to Rework the same record then throw error
          if (fundsReq.getDocumentStatus().equals("REV")) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_AlreadyPreocessed_Approve@");
            bundle.setResult(result);
            return;
          }
          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }
          // check current role is present in next role line or not
          EFINFundsReq header = OBDal.getInstance().get(EFINFundsReq.class, fundsReqId);
          if (header.getNextRole() != null) {
            java.util.List<EutNextRoleLine> li = header.getNextRole().getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
          }
          // check current role is delegated role or not
          if (header.getNextRole() != null) {
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId, docType);
            /*
             * String sql = ""; sql = "select dll.ad_role_id from eut_docapp_delegate dl" +
             * " join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
             * + CurrentDate + "' and to_date >='" + CurrentDate + "' and document_type='" + docType
             * + "'"; st = conn.prepareStatement(sql); rs = st.executeQuery(); while (rs.next()) {
             * String roleid = rs.getString("ad_role_id"); if (roleid.equals(roleId)) {
             * allowDelegation = true; break; } }
             */
          }
          if (allowUpdate || allowDelegation) {
            if (header.getEUTForward() != null) {

              // Removing the Role Access given to the forwarded user
              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForward());

              // Removing Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                  Constants.FundsReqMgmt);

            }
            if (header.getEUTReqmoreinfo() != null) {
              // access remove

              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                  Constants.FundsReqMgmt);

            }

            /*
             * ischkReserveIsDoneorNot = UtilityDAO.chkReserveIsDoneorNot(conn, clientId, orgId,
             * roleId, userId, docType, BigDecimal.ZERO); log.debug("ischkReserveIsDoneorNot:" +
             * ischkReserveIsDoneorNot);
             */
            // if reserve then reactivate the budget inquiry changes
            if (header.isReserve()) {
              FundsRequestActionDAO.reactivateBudgetInqchanges(conn, fundsReqId, header.isReserve(),
                  false);
            }
            // get old nextrole line user and role list
            HashMap<String, String> alertReceiversMap = forwardDao
                .getNextRoleLineList(header.getNextRole(), docType);
            // update the funds request header status as draft
            header.setUpdated(new java.util.Date());
            header.setUpdatedBy(OBContext.getOBContext().getUser());
            header.setDocumentStatus("RW");
            header.setAction("CO");
            header.setNextRole(null);
            header.setReserve(false);
            log.debug("header:" + header.toString());
            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();

            // delete the unused next roles
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), docType);
            if (!StringUtils.isEmpty(fundsReqId)) {
              appstatus = "REJ";

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
              count = Utility.InsertApprovalHistory(historyData);
            }

            Role objCreatedRole = null;
            if (header.getCreatedBy().getADUserRolesList().size() > 0) {
              if (header.getRole() != null)
                objCreatedRole = header.getRole();
              else
                objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            log.debug("objCreatedRole:" + objCreatedRole);

            // check and insert alert recipient
            OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

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

            // // delete alert for approval alerts
            // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            // "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'");
            //
            // if (alertQuery.list().size() > 0) {
            // for (Alert objAlert : alertQuery.list()) {
            // objAlert.setAlertStatus("SOLVED");
            // }
            // }

            forwardReqMoreInfoDAO.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId,
                objUser, clientId, Constants.REJECT, header.getDocumentNo(), Lang, vars.getRole(),
                forwardObj, docType, alertReceiversMap);

            String Description = sa.elm.ob.finance.properties.Resource.getProperty(
                "finance.fundsreq.rejected", vars.getLanguage()) + " " + objUser.getName();
            log.debug("Description:" + Description);
            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), docType);
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "",
                header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                alertWindow, "finance.fundsreq.rejected", Constants.GENERIC_TEMPLATE);

            if (count > 0 && !StringUtils.isEmpty(fundsReqId)) {
              obError.setType("Success");
              obError.setTitle("Success");
              obError.setMessage(OBMessageUtils.messageBD("EFIN_FundsReqRejectSuccess"));
            }
          }

          else {
            errorFlag = false;
            errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
            throw new OBException(errorMsg);
          }
        } catch (Exception e) {
          log.error("exception :", e);
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);
          OBDal.getInstance().rollbackAndClose();

        }
      } else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
