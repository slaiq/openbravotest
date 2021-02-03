/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */

package sa.elm.ob.finance.process.Budget;

import java.sql.ResultSet;
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

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
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
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gowtham.V
 */
public class BudgetRework implements Process {
  private static final Logger log = Logger.getLogger(BudgetRework.class);
  private final OBError obError = new OBError();

  /**
   * This class is used to perform reject operation in budget.
   */
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("rework the budget");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    final String budgetId = (String) bundle.getParams().get("Efin_Budget_ID").toString();
    EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budget.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String comments = (String) bundle.getParams().get("comments").toString();
    EFINBudget headerId = null;
    String appstatus = "", alertRuleId = "", alertWindow = AlertWindow.Budget;
    ResultSet rs = null;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    Boolean allowReject = false;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = budget.getEUTForwardReqmoreinfo();

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = true;
    boolean allowUpdate = false, allowDelegation = false;
    String errorMsg = "";
    int count = 0;
    Date currentDate = new Date();

    log.debug("budgetId:" + budgetId);

    // If one user Approved the record and second user try to Rework same record with same role
    // then throw error
    if (budget.getAlertStatus().equals("APP")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    // After Revoked by submiter if approver is try to Rework the same record then throw error
    if (budget.getAlertStatus().equals("OP")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    // If the record is Forwarded or given RMI then throw error when any other user tries to
    // reject the record without refreshing the page
    if (budget.getEUTForwardReqmoreinfo() != null) {
      allowReject = forwardReqMoreInfoDAO.allowApproveReject(budget.getEUTForwardReqmoreinfo(),
          userId, roleId, Resource.BUDGET_ENTRY_RULE);
    }
    if (budget.getEUTReqmoreinfo() != null
        || ((budget.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          // get old nextrole line user and role list
          HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
              .getNextRoleLineList(budget.getEUTNextRole(), Resource.BUDGET_ENTRY_RULE);

          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          OBQuery<EFINBudgetLines> lines = OBDal.getInstance().createQuery(EFINBudgetLines.class,
              "efinBudget.id ='" + budgetId + "'");
          count = lines.list().size();

          if (count > 0) {
            if (budget.getEUTNextRole() != null) {
              java.util.List<EutNextRoleLine> li = budget.getEUTNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            // check current role is a delegated role or not
            if (budget.getEUTNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                  DocumentTypeE.BUDGET_ENTRY.getDocumentTypeCode());
              // allowDelegation = BudgetDAO.isDelegatedRole(roleId);
            }
            if (allowUpdate || allowDelegation) {
              // Removing Forward and RMI Id
              if (budget.getEUTForwardReqmoreinfo() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(budget.getEUTForwardReqmoreinfo());
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(budget.getId(),
                    Constants.BUDGET);
              }
              if (budget.getEUTReqmoreinfo() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(budget.getEUTReqmoreinfo());
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(budget.getId(), Constants.BUDGET);
              }

              budget.setUpdated(new java.util.Date());
              budget.setUpdatedBy(OBContext.getOBContext().getUser());
              budget.setAlertStatus("REW");
              budget.setSubmit(false);
              budget.setEUTNextRole(null);
              log.debug("header:" + budget.toString());
              OBDal.getInstance().save(budget);
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.BUDGET_ENTRY_RULE);
              headerId = budget;
              log.debug("headerId:" + headerId.getId());
              if (!StringUtils.isEmpty(headerId.getId())) {
                appstatus = "REJ";
                JSONObject historyData = new JSONObject();
                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId.getId());
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.Budget_History);
                historyData.put("HeaderColumn", ApprovalTables.Budget_History_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.Budget_History_DOCACTION_COLUMN);

                count = Utility.InsertApprovalHistory(historyData);
              }

              if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
                Role objCreatedRole = null;
                if (budget.getCreatedBy().getADUserRolesList().size() > 0) {
                  if (budget.getRole() != null)
                    objCreatedRole = budget.getRole();
                  else
                    objCreatedRole = budget.getCreatedBy().getADUserRolesList().get(0).getRole();
                }
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
                forwardReqMoreInfoDAO.getAlertForForwardedUser(budget.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT, budget.getDocumentNo(), Lang,
                    vars.getRole(), forwardObj, Resource.BUDGET_ENTRY_RULE, alertReceiversMap);
                // delete alert for approval alerts
                /*
                 * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                 * "as e where e.referenceSearchKey='" + budget.getId() +
                 * "' and e.alertStatus='NEW'");
                 * 
                 * if (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
                 * objAlert.setAlertStatus("SOLVED"); } }
                 */

                String Description = sa.elm.ob.finance.properties.Resource
                    .getProperty("finance.Budget.rejected", Lang) + " " + objUser.getName();
                // delete the unused nextroles in eut_next_role table.
                DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                    Resource.BUDGET_ENTRY_RULE);
                try {
                  AlertUtility.alertInsertionRole(budget.getId(), budget.getDocumentNo(), "",
                      budget.getCreatedBy().getId(), budget.getClient().getId(), Description, "NEW",
                      alertWindow, "finance.Budget.rejected", Constants.GENERIC_TEMPLATE);
                } catch (Exception e) {

                }
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@EFIN_budgetReworkSuccess@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
              throw new OBException(errorMsg);
            }
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
      try {
        if (rs != null)
          rs.close();
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
  }

}
