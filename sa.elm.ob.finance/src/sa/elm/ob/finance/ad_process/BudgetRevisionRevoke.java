package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class BudgetRevisionRevoke implements Process {

  private static final Logger log = Logger.getLogger(BudgetRevisionRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("rework the budget");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    EutNextRole nextRole = null;
    Date currentDate = new Date();

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = (String) bundle.getContext().getOrganization();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments");
    String headerId = null;
    String appstatus = "";
    String alertRuleId = "", alertWindow = AlertWindow.BudgetRevision;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    EfinBudgetManencum manualId = null;
    boolean errorFlag = true;

    boolean isfundserrorFlag = true;
    final String TransferId = (String) bundle.getParams().get("Efin_Budget_Transfertrx_ID");
    log.debug("TransferId:" + TransferId);
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    try {
      OBContext.setAdminMode(true);
      EfinBudgetTransfertrx headerCheck = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          TransferId);
      if (!headerCheck.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }
      if (errorFlag) {
        try {

          EfinBudgetTransfertrx header = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              TransferId);

          // revert the changes in budget enquiry
          for (EFINFundsReq fundreqmgmt : header.getEFINFundsReqList()) {
            isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
                OBDal.getInstance().getConnection(), fundreqmgmt.getId(), false, true);
          }
          if (!isfundserrorFlag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EFIN_FundsReq_Rev_Error@");
            bundle.setResult(result);
            return;
          }

          OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance().createQuery(
              EfinBudgetManencum.class, " as e where e.sourceref = '" + TransferId + "'");
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
          }
          headerCheck.setManualEncumbrance(null);

          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                  + "'");
          if (log.isDebugEnabled()) {
            log.debug("queryAlertRule" + queryAlertRule.getWhereAndOrderBy());
          }
          List<AlertRule> queryAlertRuleList = queryAlertRule.list();
          if (queryAlertRuleList.size() > 0) {
            AlertRule objRule = queryAlertRuleList.get(0);
            alertRuleId = objRule.getId();
          }

          nextRole = header.getNextRole();

          // get alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

          // set alerts for next roles
          if (nextRole.getEutNextRoleLineList().size() > 0) {
            // delete alert for approval alerts
            OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'");
            if (alertQuery.list().size() > 0) {
              for (Alert objAlert : alertQuery.list()) {
                objAlert.setAlertStatus("SOLVED");
              }
            }

            String description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.revision.revoked", vars.getLanguage()) + " "
                + header.getCreatedBy().getName();

            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              try {
                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                    objNextRoleLine.getRole().getId(),
                    (objNextRoleLine.getUserContact() == null ? ""
                        : objNextRoleLine.getUserContact().getId()),
                    header.getClient().getId(), description, "NEW", alertWindow,
                    "finance.revision.revoked", Constants.GENERIC_TEMPLATE);
              } catch (Exception e) {

              }
              // get user name for delegated user to insert on approval history.
              OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                  EutDocappDelegateln.class,
                  " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                      + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                      + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_104'");
              if (delegationln != null && delegationln.list().size() > 0) {
                includeRecipient.add(delegationln.list().get(0).getRole().getId());
              }
              // add next role recipient
              includeRecipient.add(objNextRoleLine.getRole().getId());

            }
          }
          // existing Recipient
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }

          header.setUpdated(new java.util.Date());
          header.setUpdatedBy(OBContext.getOBContext().getUser());
          header.setDocStatus("DR");
          header.setAction("CO");
          header.setNextRole(null);
          log.debug("header:" + header.toString());
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.BUDGET_REVISION_RULE);
          headerId = header.getId();
          log.debug("headerId:" + header.getId());
          if (!StringUtils.isEmpty(header.getId())) {
            appstatus = "REV";
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", headerId);
            historyData.put("Comments", comments);
            historyData.put("Status", appstatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.BUDGET_REVISION_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.BUDGET_REVISION_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.BUDGET_REVISION_DOCACTION_COLUMN);
            Utility.InsertApprovalHistory(historyData);

          }
          if (!StringUtils.isEmpty(header.getId())) {

            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.BUDGET_REVISION_RULE);
            // Removing the forwardRMI id
            if (header.getEUTForwardReqmoreinfo() != null) {

              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());

              // Removing Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                  Constants.BUDGET_REVISION);

            }
            if (header.getEUTReqmoreinfo() != null) {

              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());

              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                  Constants.BUDGET_REVISION);

            }

            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Efin_BudgetRevision_Revoke@");
            bundle.setResult(result);
            return;
          }

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);
          OBDal.getInstance().rollbackAndClose();

        }
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

  public static int RevertBudgetInquiry(String BudRevId) {

    int count = 0;
    String strquery, query = "";
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps2 = null;
    ResultSet rs = null;

    try {
      OBContext.setAdminMode(true);
      strquery = "select revline.increase as increase, revline.decrease as decrease, inq.efin_budgetinquiry_id as inqId from efin_budget_transfertrxline revline\n"
          + "join efin_budget_transfertrx revHeader on revline.efin_budget_transfertrx_id = revHeader.efin_budget_transfertrx_id\n"
          + "join efin_budgetinquiry inq on revline.c_validcombination_id = inq.c_validcombination_id and revHeader.efin_budgetint_id = inq.efin_budgetint_id\n"
          + "where revHeader.efin_budget_transfertrx_id = ?";
      ps = conn.prepareStatement(strquery);
      ps.setString(1, BudRevId);
      rs = ps.executeQuery();
      log.debug("strquery" + strquery);
      while (rs.next()) {
        if (rs.getBigDecimal("decrease") != null) {
          query = "  update efin_budgetinquiry set encumbrance =encumbrance- ?, revinc_amt =revinc_amt -? where efin_budgetinquiry_id=? ";
          ps2 = conn.prepareStatement(query);
          ps2.setBigDecimal(1, rs.getBigDecimal("decrease"));
          ps2.setBigDecimal(2, rs.getBigDecimal("increase"));
          ps2.setString(3, rs.getString("inqId"));
          ps2.executeUpdate();
        } else {
          query = "  update efin_budgetinquiry set revdec_amt =revdec_amt- ?, revinc_amt =revinc_amt -? where efin_budgetinquiry_id=? ";
          ps2 = conn.prepareStatement(query);
          ps2.setBigDecimal(1, rs.getBigDecimal("decrease"));
          ps2.setBigDecimal(2, rs.getBigDecimal("increase"));
          ps2.setString(3, rs.getString("inqId"));
          ps2.executeUpdate();
        }
      }
      count = 1;
    } catch (Exception e) {
      log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
        }
      }
      OBContext.restorePreviousMode();
    }
    return count;
  }

}
