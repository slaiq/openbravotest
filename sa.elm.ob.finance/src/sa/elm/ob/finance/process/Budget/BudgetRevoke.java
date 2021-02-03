/*All Rights Reserved By Qualian Technologies Pvt Ltd.*/
package sa.elm.ob.finance.process.Budget;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.ad_process.budget.BudgetDAO;

/**
 * 
 * @author Gowtham.V
 * 
 */

public class BudgetRevoke implements Process {
  /**
   * This process allow the user to edit the submitted budget.
   */
  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = (String) bundle.getContext().getOrganization();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString(), appstatus = "";
    int count = 0;

    if (LOG.isDebugEnabled())
      LOG.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    final String budgetId = (String) bundle.getParams().get("Efin_Budget_ID").toString();

    // After Approve or Rework by approver if submiter is try to Revoke the same record then throw
    // error
    EFINBudget Budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
    if ((Budget.getAlertStatus().equals("APP")) || (Budget.getAlertStatus().equals("REW"))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    try {
      OBContext.setAdminMode(true);
      EFINBudget headerCheck = OBDal.getInstance().get(EFINBudget.class, budgetId);
      if (!headerCheck.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }

      EFINBudget header = OBDal.getInstance().get(EFINBudget.class, budgetId);
      count = BudgetDAO.updateRevokeStatus(header, clientId, orgId, roleId, userId, comments,
          appstatus, vars, Lang);

      if (count > 0 && !StringUtils.isEmpty(header.getId())) {
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("Efin_Budg_Revoke"));
      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Efin_Budg_RevokeNot"));
      }
      bundle.setResult(obError);

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isErrorEnabled())
        LOG.error("Exception While Revoke budget :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
