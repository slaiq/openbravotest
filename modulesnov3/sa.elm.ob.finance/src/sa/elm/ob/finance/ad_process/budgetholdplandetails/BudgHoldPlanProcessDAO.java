package sa.elm.ob.finance.ad_process.budgetholdplandetails;

import java.sql.Connection;
import java.util.Date;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;

public interface BudgHoldPlanProcessDAO {

  /**
   * Update Budget Hold Plan Details Status Based on next role and if final approver apply hold amt
   * on weightage concept and update the same amt in rdv trx- reduce net match amt and send alert to
   * budget user
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param rdvtransaction
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramnextApproval
   * @param Lang
   * @param bundle
   * @param budgHold
   * @return
   */
  public JSONObject updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinRDVTransaction rdvtransaction, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle, EFINRdvBudgHold budgHold);

  /**
   * check approver is direct approver or not
   * 
   * @param rdvtrxid
   * @param roleId
   * @return
   */
  public boolean isDirectApproval(String rdvtrxid, String roleId);

  /**
   * Reactivate Process will delete the hold action, insert approval history and change the record
   * status.
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param rdvtransaction
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramnextApproval
   * @param Lang
   * @param bundle
   * @param budgHold
   * @return
   */
  public JSONObject reactivateHeader(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinRDVTransaction rdvtransaction, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle, EFINRdvBudgHold budgHold);

}
