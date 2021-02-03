package sa.elm.ob.finance.ad_process.BudgetPreparation;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetPreparation;
import sa.elm.ob.finance.process.Budget.BudgetRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;

/**
 * 
 * @author qualian
 * 
 */

public class BudgetPreprationRevoke implements Process {
  /**
   * This process allow the user to revoke the submitted budget preparation.
   */
  private static final Logger log = Logger.getLogger(BudgetRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    final String budgetpreparationId = (String) bundle.getParams().get("Efin_Budget_Preparation_ID")
        .toString();
    EfinBudgetPreparation budgetpreparation = OBDal.getInstance().get(EfinBudgetPreparation.class,
        budgetpreparationId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgetpreparation.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EfinBudgetPreparation headerId = null;

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    // After Approve or Rework by approver if submiter is try to Revoke the same record then throw
    // error
    if ((budgetpreparation.getAlertStatus().equals("APP"))
        || (budgetpreparation.getAlertStatus().equals("RW"))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    int count = 0;
    boolean errorFlag = true;
    try {
      EfinBudgetPreparation headerCheck = OBDal.getInstance().get(EfinBudgetPreparation.class,
          budgetpreparationId);
      if (!headerCheck.isBudgetprepareRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }
      OBContext.setAdminMode(true);
      if (errorFlag) {
        EfinBudgetPreparation header = OBDal.getInstance().get(EfinBudgetPreparation.class,
            budgetpreparationId);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAlertStatus("O");
        header.setAction("CO");
        header.setBudgetprepareRevoke(false);
        header.setNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.BUDGET_PREPARATION_RULE);
        headerId = header;
        if (!StringUtils.isEmpty(headerId.getId())) {
          count = BudgetPreparationRework.insertBudgetPreparationApprover(
              OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, headerId,
              comments, "REV");
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);
        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("EFin_BudgetPre_Revoke"));
        }

        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      } else if (!errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("");
      }
      bundle.setResult(obError);
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke budget Preparation :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
