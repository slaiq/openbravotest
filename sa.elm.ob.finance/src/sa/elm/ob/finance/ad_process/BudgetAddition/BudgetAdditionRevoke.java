package sa.elm.ob.finance.ad_process.BudgetAddition;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetAdd;

/**
 * 
 * @author qualian
 * 
 */

public class BudgetAdditionRevoke implements Process {
  /**
   * This process allow the user to revoke the submitted budget Addition.
   */
  private static final Logger log = Logger.getLogger(BudgetAdditionRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    final String budgetAdditionId = (String) bundle.getParams().get("Efin_Budgetadd_ID").toString();
    EfinBudgetAdd budgetpreparation = OBDal.getInstance().get(EfinBudgetAdd.class,
        budgetAdditionId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgetpreparation.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EfinBudgetAdd headerId = null;

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    // After Approve or Rework by approver if submiter is try to Revoke the same record then throw
    // error
    if ((budgetpreparation.getStatus().equals("APP"))
        || (budgetpreparation.getStatus().equals("RW"))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    int count = 0;
    boolean errorFlag = true;
    try {
      OBContext.setAdminMode(true);
      EfinBudgetAdd headerCheck = OBDal.getInstance().get(EfinBudgetAdd.class, budgetAdditionId);
      if (!headerCheck.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }
      if (errorFlag) {
        EfinBudgetAdd header = OBDal.getInstance().get(EfinBudgetAdd.class, budgetAdditionId);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setStatus("O");
        header.setAction("CO");
        header.setNextRole(null);
        OBDal.getInstance().save(header);
        headerId = header;
        if (!StringUtils.isEmpty(headerId.getId())) {
          count = BudgetAdditionProcess.insertBudgAddHistory(OBDal.getInstance().getConnection(),
              clientId, orgId, roleId, userId, header, comments, "REV", null);
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);
        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("Efin_budget_Addition_Revoke"));
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
      log.error("Exception While Revoke budget Addition :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
