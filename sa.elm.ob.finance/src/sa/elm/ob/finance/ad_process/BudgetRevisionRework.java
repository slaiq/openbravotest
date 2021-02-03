package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
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

public class BudgetRevisionRework implements Process {
  private static final Logger log = Logger.getLogger(BudgetRevisionRework.class);
  private final OBError obError = new OBError();

  @SuppressWarnings("unused")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("rework the budget");
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String BudRevId = (String) bundle.getParams().get("Efin_Budget_Transfertrx_ID");
    EfinBudgetTransfertrx efinBudgetRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
        BudRevId);

    String alertRuleId = "", alertWindow = AlertWindow.BudgetRevision;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = efinBudgetRev.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String headerId = null;
    String appstatus = "";
    String Lang = vars.getLanguage();

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    Connection con = OBDal.getInstance().getConnection();
    EfinBudgetManencum manualId = null;
    String errorMsg = "";
    Date currentDate = new Date();
    String sql = "";
    String rolesequenceno = null;
    boolean isfundserrorFlag = true;
    log.debug("budgetId:" + BudRevId);
    PreparedStatement ps = null;
    ResultSet rs = null;
    String forwardId = null, requestMoreInfoId = null;

    EutForwardReqMoreInfo forwardObj = efinBudgetRev.getEUTForwardReqmoreinfo();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    if (efinBudgetRev.getDocStatus().equals("CO")) {
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

          HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
              .getNextRoleLineList(efinBudgetRev.getNextRole(), Resource.BUDGET_REVISION_RULE);

          Boolean allowReject = false;

          if (efinBudgetRev.getEUTForwardReqmoreinfo() != null) {
            allowReject = forwardReqMoreInfoDAO.allowApproveReject(
                efinBudgetRev.getEUTForwardReqmoreinfo(), userId, roleId,
                Resource.BUDGET_REVISION_RULE);
          }
          if (efinBudgetRev.getEUTReqmoreinfo() != null
              || ((efinBudgetRev.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }

          EfinBudgetTransfertrx header = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              BudRevId);
          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          sql = "select eut_documentrule_lines.rolesequenceno as role  from eut_documentrule_header  left join eut_documentrule_lines on \n"
              + "eut_documentrule_header.eut_documentrule_header_id = eut_documentrule_lines.eut_documentrule_header_id\n"
              + "where document_type = 'EUT_104' and eut_documentrule_header.ad_client_id = '"
              + clientId + "'\n" + "and eut_documentrule_lines.ad_role_id = '" + roleId
              + "'and eut_documentrule_header.ad_org_id = '" + orgId + "' ";
          ps = con.prepareStatement(sql);
          rs = ps.executeQuery();
          log.debug("Que" + sql.toString());
          while (rs.next()) {
            rolesequenceno = rs.getString("role");

          }
          log.debug("rolesequenceno" + rolesequenceno);
          // if (rolesequenceno != null) {
          // sql = "select eut_documentrule_lines.allowreservation as allowreservation from
          // eut_documentrule_header left join eut_documentrule_lines on \n"
          // + "eut_documentrule_header.eut_documentrule_header_id =
          // eut_documentrule_lines.eut_documentrule_header_id\n"
          // + "where document_type = 'EUT_104' and eut_documentrule_header.ad_client_id = '"
          // + clientId + "'\n" + "and eut_documentrule_lines.rolesequenceno < " + rolesequenceno
          // + " and eut_documentrule_lines.allowreservation = 'Y' and
          // eut_documentrule_header.ad_org_id = '"
          // + orgId + "' ";
          // ps = con.prepareStatement(sql);
          // rs = ps.executeQuery();
          //
          // if (rs.next()) {
          // allowreservation = rs.getString("allowreservation");
          // allowreserve = true;
          // }
          // }

          // No need to do manually update budget enquiry, will happen in trigger.
          // count = RevertBudgetInquiry(BudRevId);
          OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance().createQuery(
              EfinBudgetManencum.class, " as e where e.sourceref = '" + BudRevId + "'");

          if (chkLinePresent != null && chkLinePresent.list().size() > 0) {
            manualId = chkLinePresent.list().get(0);
            EfinBudgetManencum manual = manualId;
            manual.setDocumentStatus("DR");
            OBDal.getInstance().save(manual);
            for (EfinBudgetManencumlines reqln : manualId.getEfinBudgetManencumlinesList()) {
              OBDal.getInstance().remove(reqln);
            }
            OBDal.getInstance().remove(manualId);
          }
          efinBudgetRev.setManualEncumbrance(null);

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

          if (header.getNextRole() != null) {
            java.util.List<EutNextRoleLine> li = header.getNextRole().getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
            // check current role is a delegated role or not
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                DocumentTypeE.BUDGET_REVISION.getDocumentTypeCode());
            /*
             * sql =
             * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
             * + CurrentDate + "' and to_date >='" + CurrentDate + "' and document_type='EUT_104'";
             * ps = con.prepareStatement(sql); rs = ps.executeQuery(); if (rs.next()) { String
             * roleid = rs.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation =
             * true; } }
             */

            if (allowUpdate || allowDelegation) {

              // remove eut_forward_rmi id from record
              if (efinBudgetRev.getNextRole() != null) {
                // Give Role Access to Receiver
                if (efinBudgetRev.getEUTForwardReqmoreinfo() != null) {
                  forwardId = efinBudgetRev.getEUTForwardReqmoreinfo().getId();
                  // update status as "DR"
                  forwardReqMoreInfoDAO
                      .setForwardStatusAsDraft(efinBudgetRev.getEUTForwardReqmoreinfo());

                  // Remove Forward_Rmi id from transaction screens
                  forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(efinBudgetRev.getId(),
                      Constants.BUDGET_REVISION);

                }
                if (efinBudgetRev.getEUTReqmoreinfo() != null) {

                  requestMoreInfoId = efinBudgetRev.getEUTReqmoreinfo().getId();
                  // update status as "DR"
                  forwardReqMoreInfoDAO.setForwardStatusAsDraft(efinBudgetRev.getEUTReqmoreinfo());

                  // Remove Forward_Rmi id from transaction screens
                  forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(efinBudgetRev.getId(),
                      Constants.BUDGET_REVISION);
                }
              }

              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setDocStatus("RW");
              header.setAction("CO");
              header.setNextRole(null);
              header.setRevoke(false);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.BUDGET_REVISION_RULE);
              headerId = header.getId();
              if (!StringUtils.isEmpty(header.getId())) {
                appstatus = "REW";
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
              includeRecipient.add(objCreatedRole != null ? objCreatedRole.getId() : "");
              // avoid duplicate recipient
              HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
              Iterator<String> iterator = incluedSet.iterator();
              while (iterator.hasNext()) {
                AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
              }

              forwardReqMoreInfoDAO.getAlertForForwardedUser(header.getId(), alertWindow,
                  alertRuleId, objUser, clientId, Constants.REJECT, header.getDocumentNo(), Lang,
                  vars.getRole(), forwardObj, Resource.BUDGET_REVISION_RULE, alertReceiversMap);

              /*
               * // delete alert for approval alerts OBQuery<Alert> alertQuery =
               * OBDal.getInstance().createQuery(Alert.class, "as e where e.referenceSearchKey='" +
               * header.getId() + "' and e.alertStatus='NEW'");
               * 
               * if (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
               * objAlert.setAlertStatus("SOLVED"); } }
               */

              String Description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.revision.rejected", vars.getLanguage()) + " " + objUser.getName();
              log.debug("Description:" + Description);
              // delete the unused nextroles in eut_next_role table.
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.BUDGET_REVISION_RULE);
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "",
                  header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                  alertWindow, "finance.revision.rejected", Constants.GENERIC_TEMPLATE);

              if (!StringUtils.isEmpty(header.getId())) {

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Efin_Budg_Rev_Rework@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
              throw new OBException(errorMsg);
            }
          }

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
      strquery = "select revline.increase as increase, revline.decrease as decrease,inq.efin_budgetinquiry_id as inqId from efin_budget_transfertrxline revline\n"
          + "join efin_budget_transfertrx revHeader on revline.efin_budget_transfertrx_id = revHeader.efin_budget_transfertrx_id\n"
          + "join efin_budgetinquiry inq on revline.c_validcombination_id = inq.c_validcombination_id and revHeader.efin_budgetint_id = inq.efin_budgetint_id\n"
          + "where revHeader.efin_budget_transfertrx_id = ? and revline.decrease > 0";
      ps = conn.prepareStatement(strquery);
      ps.setString(1, BudRevId);
      rs = ps.executeQuery();
      log.debug("strquery" + strquery);
      while (rs.next()) {
        query = "  update efin_budgetinquiry set encumbrance =encumbrance- ?, revinc_amt =revinc_amt -? where efin_budgetinquiry_id=? ";
        ps2 = conn.prepareStatement(query);
        ps2.setBigDecimal(1, rs.getBigDecimal("decrease"));
        ps2.setBigDecimal(2, rs.getBigDecimal("increase"));
        ps2.setString(3, rs.getString("inqId"));
        ps2.executeUpdate();
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
