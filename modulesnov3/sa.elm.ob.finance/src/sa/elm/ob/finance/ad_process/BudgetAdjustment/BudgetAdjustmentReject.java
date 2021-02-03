package sa.elm.ob.finance.ad_process.BudgetAdjustment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
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

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author gopalakrishnan on 10/10/2017
 *
 */
public class BudgetAdjustmentReject implements Process {
  /**
   * This Servlet Class responsible to reject records of Budget Adjustment
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(BudgetAdjustmentReject.class);

  @SuppressWarnings({ "unused", "rawtypes" })
  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    final String BudgetAdjustementid = (String) bundle.getParams().get("Efin_Budgetadj_ID");

    BudgetAdjustment budgetAddjustment = OBDal.getInstance().get(BudgetAdjustment.class,
        BudgetAdjustementid);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgetAddjustment.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    BudgetAdjustment headerId = null;
    Boolean allowReject = false;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = budgetAddjustment.getEUTForward();
    String appstatus = "", alertWindow = AlertWindow.BudgetAdjustment;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    boolean isfundserrorFlag = true;
    String errorMsg = "", alertRuleId = "";
    String fundsreqId = "";
    Date CurrentDate = new Date();
    int count = 0, j = 0;
    EfinBudgetManencum manualId = null;
    try {
      OBContext.setAdminMode(true);

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // reject the record without refreshing the page
      if (budgetAddjustment.getEUTForward() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(budgetAddjustment.getEUTForward(),
            userId, roleId, Resource.BUDGET_ADJUSTMENT_RULE);
      }
      if (budgetAddjustment.getEUTReqmoreinfo() != null
          || ((budgetAddjustment.getEUTForward() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // get old nextrole line user and role list
      HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
          .getNextRoleLineList(budgetAddjustment.getNextRole(), Resource.BUDGET_ADJUSTMENT_RULE);
      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id= :clientID and e.efinProcesstype= :efinProcesstype ");
      queryAlertRule.setNamedParameter("clientID", clientId);
      queryAlertRule.setNamedParameter("efinProcesstype", alertWindow);
      List<AlertRule> queryAlertRuleList = queryAlertRule.list();
      if (queryAlertRuleList.size() > 0) {
        AlertRule objRule = queryAlertRuleList.get(0);
        alertRuleId = objRule.getId();
      }
      if (errorFlag) {
        try {

          OBQuery<BudgetAdjustmentLine> lines = OBDal.getInstance()
              .createQuery(BudgetAdjustmentLine.class, "efinBudgetadj.id = :BudgetAdjustementid ");
          lines.setNamedParameter("BudgetAdjustementid", BudgetAdjustementid);
          count = lines.list().size();

          if (count > 0) {
            BudgetAdjustment header = OBDal.getInstance().get(BudgetAdjustment.class,
                BudgetAdjustementid);

            // revert the changes in budget enquiry
            fundsreqId = FundsRequestActionDAO.getFundsReqId(header, null, null);
            if (StringUtils.isNotEmpty(fundsreqId)) {

              isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
                  OBDal.getInstance().getConnection(), fundsreqId, false, true);

              if (!isfundserrorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@EFIN_FundsReq_Rev_Error@");
                bundle.setResult(result);
                return;
              }
            }
            if (header.getNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (header.getNextRole() != null) {
              String sql = "";
              sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
                  + CurrentDate + "' and to_date >='" + CurrentDate
                  + "' and document_type='EUT_119'";
              st = conn.prepareStatement(sql);
              rs = st.executeQuery();
              if (rs.next()) {
                String roleid = rs.getString("ad_role_id");
                if (roleid.equals(roleId)) {
                  allowDelegation = true;
                }
              }
            }
            OBContext.setAdminMode(true);

            if (allowUpdate || allowDelegation) {

              // Removing Forward and RMI Id
              if (budgetAddjustment.getEUTForward() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(budgetAddjustment.getEUTForward());
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(budgetAddjustment.getId(),
                    Constants.BUDGETADJUSTMENT);
              }
              if (budgetAddjustment.getEUTReqmoreinfo() != null) {
                forwardReqMoreInfoDAO
                    .setForwardStatusAsDraft(budgetAddjustment.getEUTReqmoreinfo());
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(budgetAddjustment.getId(),
                    Constants.BUDGETADJUSTMENT);
              }

              String str_budget_reference = budgetAddjustment.getEfinBudgetint() == null ? ""
                  : budgetAddjustment.getEfinBudgetint().getId();
              String query = " select bt.efin_budgetint_id,bl.efin_budgetinquiry_id ,"
                  + " cv.account_id,al.increase,al.decrease,  "
                  + " cv.c_validcombination_id,al.efin_budgetadjline_id  "
                  + " from efin_budgetadjline al "
                  + "  left join c_validcombination cv on cv.c_validcombination_id = al.c_validcombination_id "
                  + "    left join efin_budgetint bt on bt.efin_budgetint_id=:reference1  "
                  + "    left join efin_budgetinquiry bl on  cv.c_validcombination_id =bl.c_validcombination_id and "
                  + "    bl.efin_budgetint_id=:reference2 "
                  + "    where al.ad_client_id =:clientId "
                  + "    and al.efin_budgetadj_id =:budgetAdjId and al.fundsreserved='Y'";
              SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
              sqlQuery.setParameter("reference1", str_budget_reference);
              sqlQuery.setParameter("reference2", str_budget_reference);
              sqlQuery.setParameter("clientId", clientId);
              sqlQuery.setParameter("budgetAdjId", BudgetAdjustementid);
              List queryList = sqlQuery.list();
              if (sqlQuery != null && queryList.size() > 0) {
                for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
                  Object[] row = (Object[]) iterator.next();
                  if (row[0] != null) {
                    if (row[1] != null) {
                      BudgetAdjustmentLine objAdjLine = OBDal.getInstance()
                          .get(BudgetAdjustmentLine.class, row[6].toString());
                      EfinBudgetInquiry objInquiryLine = OBDal.getInstance()
                          .get(EfinBudgetInquiry.class, row[1].toString());
                      // objInquiryLine.setEncumbrance(
                      // objInquiryLine.getEncumbrance().subtract((BigDecimal) row[4]));
                      objInquiryLine
                          .setObincAmt(objInquiryLine.getObincAmt().subtract((BigDecimal) row[3]));
                      OBDal.getInstance().save(objInquiryLine);
                      objAdjLine.setBudgetInquiryLine(objInquiryLine);
                      objAdjLine.setFundsreserved(false);
                      OBDal.getInstance().save(objAdjLine);

                    }
                  } else {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        OBMessageUtils.messageBD("Efin_Budget_Not_Defined"));
                    bundle.setResult(result);
                    return;
                  }
                  if ((j % 100) == 0) {
                    OBDal.getInstance().flush();
                    OBDal.getInstance().getSession().clear();
                  }
                  j++;
                  // row[1]
                }
              }
              OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance().createQuery(
                  EfinBudgetManencum.class, " as e where e.sourceref = :BudgetAdjustementid ");
              chkLinePresent.setNamedParameter("BudgetAdjustementid", BudgetAdjustementid);
              List<EfinBudgetManencum> chkLinePresentList = chkLinePresent.list();
              if (chkLinePresent != null && chkLinePresentList.size() > 0) {
                manualId = chkLinePresentList.get(0);
                EfinBudgetManencum manual = manualId;
                manual.setDocumentStatus("DR");
                OBDal.getInstance().save(manual);
                for (EfinBudgetManencumlines reqln : manualId.getEfinBudgetManencumlinesList()) {
                  OBDal.getInstance().remove(reqln);
                }
                OBDal.getInstance().remove(manualId);
                header.setManualEncumbrance(null);
              }
              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setAction("CO");
              header.setDocumentStatus("EFIN_RJD");
              header.setProcessed(false);
              header.setNextRole(null);
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.BUDGET_ADJUSTMENT_RULE);
              headerId = header;
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
                historyData.put("HistoryTable", ApprovalTables.Budget_Adjustment_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.Budget_Adjustment_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.Budget_Adjustment_DOCACTION_COLUMN);
                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(header.getId())) {
                String Description = sa.elm.ob.finance.properties.Resource
                    .getProperty("finance.ba.rejected", Lang) + " " + objUser.getName();

                Role objCreatedRole = null;
                User user = OBDal.getInstance().get(User.class, header.getCreatedBy().getId());
                if (user.getADUserRolesList().size() > 0) {
                  objCreatedRole = user.getADUserRolesList().get(0).getRole();
                }
                // check and insert alert recipient
                OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
                    .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRuleID");
                receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
                List<AlertRecipient> receipientQueryList = receipientQuery.list();
                if (receipientQueryList.size() > 0) {
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

                forwardReqMoreInfoDAO.getAlertForForwardedUser(budgetAddjustment.getId(),
                    alertWindow, alertRuleId, objUser, clientId, Constants.REJECT,
                    budgetAddjustment.getDocno(), Lang, vars.getRole(), forwardObj,
                    Resource.BUDGET_ADJUSTMENT_RULE, alertReceiversMap);
                // delete alert for approval alerts
                /*
                 * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                 * "as e where e.referenceSearchKey='" + header.getId() +
                 * "' and e.alertStatus='NEW'");
                 * 
                 * if (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
                 * objAlert.setAlertStatus("SOLVED"); } }
                 */

                // delete the unused nextroles in eut_next_role table.
                DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.BUDGET_ADJUSTMENT_RULE);
                boolean ex = AlertUtility.alertInsertionRole(header.getId(), header.getDocno(),
                    header.getRole().getId(), header.getCreatedBy().getId(),
                    header.getClient().getId(), Description, "NEW", alertWindow,
                    "finance.ba.rejected", Constants.GENERIC_TEMPLATE);
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@EFIN_BudgetAdjuestment_Rejected@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approved");
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
      OBContext.restorePreviousMode();
    }
  }
}
