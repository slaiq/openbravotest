package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;

/**
 * @author Qualian on 31/05/2106
 */
public class BudgetRevision extends DalBaseProcess {

  /**
   * BudgetRevision Transaction submit Tracking on Budget Transfer Table(efin_budget_transfer)
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetRevision.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = "";
    String errorMsg = "", appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false;

    // Create carry Forward
    log.debug("entering into BudgetRevision");
    try {
      OBContext.setAdminMode();
      String BudRevId = (String) bundle.getParams().get("Efin_Budget_Transfertrx_ID");
      EfinBudgetTransfertrx efinBudgetRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          BudRevId);
      String DocStatus = efinBudgetRev.getDocStatus();
      String DocAction = efinBudgetRev.getAction();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = efinBudgetRev.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();

      int count = 0;
      // check current role is present in document rule or not
      if (!efinBudgetRev.getDocStatus().equals("DR")
          && !efinBudgetRev.getDocStatus().equals("RW")) {
        if (efinBudgetRev.getNextRole() != null) {
          java.util.List<EutNextRoleLine> li = efinBudgetRev.getNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (!allowUpdate) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }
      }

      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(efinBudgetRev.getCreatedBy().getId()))
          && efinBudgetRev.getDocStatus().equals("RW")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }
      // throw an error in case if approver try to approving the record while the submiter is
      // already revoked the record
      if ((!vars.getUser().equals(efinBudgetRev.getCreatedBy().getId()))
          && efinBudgetRev.getDocStatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      OBQuery<EfinBudgetTransfertrxline> lines = OBDal.getInstance().createQuery(
          EfinBudgetTransfertrxline.class,
          " as e where e.efinBudgetTransfertrx.id ='" + BudRevId + "'");
      OBQuery<EfinBudgetTransfertrxline> linesZero = OBDal.getInstance()
          .createQuery(EfinBudgetTransfertrxline.class, " as e where e.efinBudgetTransfertrx.id ='"
              + BudRevId + "' and e.decrease=0 and e.increase=0 ");
      if (lines.list().size() == 0) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "Please add lines to submit");
        bundle.setResult(result);
        return;
      }
      if (linesZero.list().size() != 0) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@EFin_Revision_Values_Zero@");
        bundle.setResult(result);
        return;
      }
      if ((DocStatus.equals("DR") || DocStatus.equals("RW")) && DocAction.equals("CO")) {
        /*
         * 
         * Transfer validation
         */
        if (efinBudgetRev.getDocType().equals("TRS")) {
          ps = conn.prepareStatement(
              " select ln.efin_budget_transfertrx_id from efin_budget_transfertrxline ln "
                  + " where ln.efin_budget_transfertrx_id='" + BudRevId
                  + "' group by ln.efin_budget_transfertrx_id "
                  + " having  sum(ln.increase)<>sum(ln.decrease)");
          rs = ps.executeQuery();
          if (rs.next()) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_BudgetRevision_Transfer_Error@");
            bundle.setResult(result);
            return;
          }
        }
        // check decrease amount exceed the cost budget funds available
        query = " select line.efin_budgetlines_id ,line.funds_available as available ,ln.decrease as decrease,ln.efin_budget_transfertrxline_id from efin_budget_transfertrxline ln  join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
            + " where ln.efin_budget_transfertrx_id  ='" + BudRevId
            + "' and ln.decrease >  line.funds_available  and ln.increase = 0  ";
        ps = conn.prepareStatement(query);
        log.debug("lines:" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgetTransfertrxline trxline = OBDal.getInstance()
              .get(EfinBudgetTransfertrxline.class, rs.getString("efin_budget_transfertrxline_id"));
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
          trxline.setStatus(status.replace("@", rs.getString("available")));
          errorFlag = true;
          errorMsg = "@Efin_budget_Rev_Lines_Failed@";

        }
        if (errorFlag) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
        }

        if (!errorFlag) {
          // update the header status
          appstatus = "SUB";

          count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, efinBudgetRev,
              appstatus, comments);
          log.debug("counts" + count);
          if (count == 2) {
            updatebudgetinquiry();
          }
          if (count > 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Efin_Budget_Rev_Submit@");
            bundle.setResult(result);
            return;
          }

        }
      } else if (efinBudgetRev.getAction().equals("AP")
          && efinBudgetRev.getDocStatus().equals("WFA")) {
        appstatus = "APP";
        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, efinBudgetRev, appstatus,
            comments);
        log.debug("count" + count);
        if (count == 2) {
          updatebudgetinquiry();
        }
        if (count > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_Budget_Rev_Submit@");
          bundle.setResult(result);
          return;
        }
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.BUDGET_REVISION_RULE);
      OBDal.getInstance().commitAndClose();
    } catch (

    Exception e) {
      log.debug("Exeception in BudgetRevision:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to update header status
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param efinBudgetRev
   * @param appstatus
   * @param comments
   * @return
   */
  public static int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinBudgetTransfertrx efinBudgetRev, String appstatus, String comments) {
    int count = 0;
    try {
      OBContext.setAdminMode(true);

      EfinBudgetTransfertrx transfertrx = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          efinBudgetRev.getId());

      NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId, roleId,
          userId, Resource.BUDGET_REVISION_RULE, 0.00);
      EutNextRole nextRole = null;
      log.debug("nextrole:" + nextApproval);

      if (nextApproval != null && nextApproval.hasApproval()) {
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        transfertrx.setUpdated(new java.util.Date());
        transfertrx.setUpdatedBy(OBContext.getOBContext().getUser());
        if ((transfertrx.getDocStatus().equals("RW") || transfertrx.getDocStatus().equals("DR"))
            && transfertrx.getAction().equals("CO")) {
          transfertrx.setRevoke(true);
        } else
          transfertrx.setRevoke(false);
        transfertrx.setDocStatus("WFA");
        transfertrx.setNextRole(nextRole);
        transfertrx.setAction("AP");
        log.debug("doc sts:" + transfertrx.getDocStatus() + "action:" + transfertrx.getAction());
        count = 1; // Waiting For Approval flow

      } else {
        if (transfertrx.getEfinBudgetRevVoid() != null) {
          EfinBudgetTransfertrx VoidReference = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              transfertrx.getEfinBudgetRevVoid().getId());
          VoidReference.setDocStatus("VO");
          OBDal.getInstance().save(VoidReference);
        }
        transfertrx.setUpdated(new java.util.Date());
        transfertrx.setUpdatedBy(OBContext.getOBContext().getUser());
        if (transfertrx.getEfinBudgetRevVoid() != null) {
          transfertrx.setDocStatus("VO");
        } else {
          transfertrx.setDocStatus("CO");
        }
        transfertrx.setNextRole(null);
        transfertrx.setAction("PD");
        transfertrx.setRevoke(false);
        count = 2; // Final Approval Flow

      }
      log.debug("approve:" + transfertrx.getTransferSource());
      log.debug("revoke:" + transfertrx.isRevoke());
      OBDal.getInstance().save(transfertrx);
      // if (!StringUtils.isEmpty(transferId)) {
      // insertHistory = BudgetRevisionRework.insertBudgRevHistory(
      // OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, transferId,
      // comments, appstatus, pendingapproval);
      // }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * This method is used to update budget inquiry
   * 
   * @return
   */
  public static int updatebudgetinquiry() {

    int count = 0;
    String strquery, query = "";
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps1 = null, ps2 = null;
    ResultSet rs = null, rs1 = null;
    String validcombination = "";
    String budgetinitial = "";

    try {
      OBContext.setAdminMode(true);
      strquery = " select efin_budget_transfertrxline.c_validcombination_id as validcombination,efin_budget_transfertrx.\n"
          + "efin_budgetint_id as budgetinitial, efin_budget_transfertrxline.increase as \n"
          + "increase,efin_budget_transfertrxline.decrease as decrease from efin_budget_transfertrx left join efin_budget_transfertrxline on efin_budget_transfertrx.efin_budget_transfertrx_id = efin_budget_transfertrxline.efin_budget_transfertrx_id  where efin_budget_transfertrx.\n"
          + "efin_budgetint_id is not null and efin_budget_transfertrxline.c_validcombination_id is not null ";
      ps = conn.prepareStatement(strquery);
      rs = ps.executeQuery();
      log.debug("strquery" + strquery);
      while (rs.next()) {
        validcombination = rs.getString("validcombination");
        budgetinitial = rs.getString("budgetinitial");

        query = "select efin_budgetinquiry_id ,c_validcombination_id from efin_budgetinquiry "
            + "where efin_budgetint_id= '" + budgetinitial + "' and c_validcombination_id= '"
            + validcombination + "' ";
        ps1 = conn.prepareStatement(query);
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          if (rs.getBigDecimal("increase") != null) {
            query = "  update efin_budgetinquiry set revinc_amt=revinc_amt + ? where efin_budgetinquiry_id=? and c_validcombination_id = ?";
            ps2 = conn.prepareStatement(query);
            ps2.setBigDecimal(1, rs.getBigDecimal("increase"));
            ps2.setString(2, rs1.getString("efin_budgetinquiry_id"));
            ps2.setString(3, rs1.getString("c_validcombination_id"));
            ps2.executeUpdate();
          } else if (rs.getBigDecimal("decrese") != null) {
            query = "  update efin_budgetinquiry set revdec_amt =revdec_amt+ ? where efin_budgetinquiry_id=? and c_validcombination_id = ?";
            ps2 = conn.prepareStatement(query);
            ps2.setBigDecimal(1, rs.getBigDecimal("decrese"));
            ps2.setString(2, rs1.getString("efin_budgetinquiry_id"));
            ps2.setString(3, rs1.getString("c_validcombination_id"));
            ps2.executeUpdate();
          }
        }
      }
      count = 1;
    } catch (Exception e) {
      log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
