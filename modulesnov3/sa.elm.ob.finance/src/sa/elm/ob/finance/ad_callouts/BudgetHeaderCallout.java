package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.finance.ad_process.budget.BudgetDAO;

/**
 * @author Gopalakrishnan on 11/05/2106
 */

public class BudgetHeaderCallout extends SimpleCallout {

  /**
   * Callout to update the ToPeriod and FromPeriod field Information in BudgetHeader Window
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(BudgetDAO.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String budgetInitId = vars.getStringParameter("inpefinBudgetintId");
    String inpTransactionDate = vars.getStringParameter("inptransactionDate"), gregorianmonth = "";

    // Assign transaction period basde on transaction date.
    if (inpLastFieldChanged.equals("inptransactionDate")
        || inpLastFieldChanged.equals("inpadOrgId")) {
      try {
        gregorianmonth = HijiridateDAO.getGregorianPeriod(inpTransactionDate);
        info.addResult("inptransactionPeriod", gregorianmonth);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error whie getting transaction period in budget:", e);
        }
      }
    }
    // Assign year, from period ,to period based on budget initialization.
    if (inpLastFieldChanged.equals("inpefinBudgetintId")) {
      try {
        EfinBudgetIntialization init = OBDal.getInstance().get(EfinBudgetIntialization.class,
            budgetInitId);
        info.addResult("inpcYearId", init.getYear().getId());
        info.addResult("inpfrmperiod", init.getFromperiod().getId());
        info.addResult("inptoperiod", init.getToPeriod().getId());
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Exception in insertBudegtLines() " + e, e);
        }
      }
    }

  }

}
