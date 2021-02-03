/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.budget;

import java.sql.Connection;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * @author Gowtham.V
 */

public class BudgetAddLines implements Process {
  /**
   * This process allow the user to add lines with available valid combination of dimensions.
   */
  private static final Logger LOG = LoggerFactory.getLogger(BudgetAddLines.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    final String budgetId = (String) bundle.getParams().get("Efin_Budget_ID").toString(),
        clientId = (String) bundle.getContext().getClient();
    final EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
    String department = "", status = "", message = "", organization = "";
    int resultSet = 0;
    EfinBudgetControlParam param = null;
    Connection connection = OBDal.getInstance().getConnection();
    try {
      // get HQ Budget control unit from maintain budget control parameters window.
      final OBQuery<EfinBudgetControlParam> budgetControl = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, "");
      if (budgetControl != null && budgetControl.list().size() > 0) {
        budgetControl.setMaxResult(1);
        param = budgetControl.list().get(0);
        department = param.getBudgetcontrolunit().getId();
        organization = param.getAgencyHqOrg().getId();
        // Insert valid combinations in line
        resultSet = BudgetDAO.insertBudegtLines(clientId, budget, department, organization,
            connection);

        // Message
        if (resultSet == 0) {
          status = "error";
          message = "@Efin_NoBudegtLine@";
        } else if (resultSet == 1) {
          status = "success";
          message = "@Efin_BudgetLineAdded@";
        } else {
          status = "error";
          message = "@Efin_BugdetAddError@";
        }
      } else {
        status = "error";
        message = "@Efin_No_BudgetControlParameter@";
      }

      final OBError result = OBErrorBuilder.buildMessage(null, status, message);
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Budget add lines process " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
  }
}
