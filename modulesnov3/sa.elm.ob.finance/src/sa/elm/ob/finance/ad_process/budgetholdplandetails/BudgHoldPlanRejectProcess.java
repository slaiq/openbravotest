package sa.elm.ob.finance.ad_process.budgetholdplandetails;

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

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
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
 * 
 * @author Kousalya on 27/11/2019
 *
 */
public class BudgHoldPlanRejectProcess implements Process {
  /**
   * This Servlet Class responsible to reject records in rdv
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(BudgHoldPlanRejectProcess.class);

  @SuppressWarnings("unused")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub
    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    final String rdvbudgholdId = (String) bundle.getParams().get("Efin_Rdv_Budghold_ID").toString();
    EFINRdvBudgHold budgHold = Utility.getObject(EFINRdvBudgHold.class, rdvbudgholdId);
    EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class,
        budgHold.getEfinRdvtxn().getId());

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgHold.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EFINRdvBudgHold headerId = null;
    Boolean IsAdvance = false;
    String appstatus = "", alertWindow = AlertWindow.RDVBudgetHold;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    Boolean allowReject = false;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    // EutForwardReqMoreInfo forwardObj = transaction.getEUTForwardReqmoreinfo();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    try {
      OBContext.setAdminMode(true);

      // get old nextrole line user and role list
      HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
          .getNextRoleLineList(budgHold.getNextRole(), Resource.RDV_BudgHoldDtl);

      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      if (errorFlag) {
        if (budgHold.getNextRole() != null) {
          java.util.List<EutNextRoleLine> li = budgHold.getNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (budgHold.getNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId,
              DocumentTypeE.RDVBH.getDocumentTypeCode());
        }
        if (allowUpdate || allowDelegation) {
          budgHold.setUpdated(new java.util.Date());
          budgHold.setUpdatedBy(OBContext.getOBContext().getUser());
          budgHold.setAction("CO");
          budgHold.setStatus("REJ");
          budgHold.setNextRole(null);
          OBDal.getInstance().save(budgHold);
          OBDal.getInstance().flush();

          // if (transaction.isAdvancetransaction()
          // && transaction.getEfinRDVTxnlineList().size() > 0) {
          // EfinRDVTxnline txnLineObj = transaction.getEfinRDVTxnlineList().get(0);
          // txnLineObj.setAction("CO");
          // txnLineObj.setApprovalStatus("DR");
          // OBDal.getInstance().save(txnLineObj);
          // }

          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.RDV_BudgHoldDtl);
          headerId = budgHold;
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
            historyData.put("HistoryTable", ApprovalTables.RDV_BudgHold_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_BudgHold_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_BudgHold_DOCACTION_COLUMN);
            count = Utility.InsertApprovalHistory(historyData);
          }
          if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
            String Description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.rdvbudghold.rejected", Lang) + " " + objUser.getName();

            Role objCreatedRole = null;
            if (budgHold.getCreatedBy().getADUserRolesList().size() > 0) {
              if (budgHold.getRole() != null)
                objCreatedRole = budgHold.getRole();
              else
                objCreatedRole = budgHold.getCreatedBy().getADUserRolesList().get(0).getRole();
            }

            // solving approval alerts - TaskNo:7618
            AlertUtility.solveAlerts(budgHold.getId());

            // check and insert alert recipient
            OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

            if (receipientQuery.list().size() > 0) {
              for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
                includeRecipient.add(objAlertReceipient.getRole().getId());
                OBDal.getInstance().remove(objAlertReceipient);
              }
            }
            if (includeRecipient != null)
              includeRecipient.add(objCreatedRole.getId());
            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
            }

            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.RDV_BudgHoldDtl);
            boolean ex = AlertUtility.alertInsertionRole(budgHold.getId(),
                budgHold.getSalesOrder().getDocumentNo() + "-"
                    + budgHold.getEfinRdvtxn().getDocumentNo(),
                "", budgHold.getCreatedBy().getId(), budgHold.getClient().getId(), Description,
                "NEW", alertWindow, "finance.rdvbudghold.rejected", Constants.GENERIC_TEMPLATE);
            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Reject@");
            bundle.setResult(result);
            return;
          }
          OBDal.getInstance().flush();
        } else {
          errorFlag = false;
          errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
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
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}