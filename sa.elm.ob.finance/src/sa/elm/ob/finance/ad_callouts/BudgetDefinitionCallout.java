package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.calendar.Period;

import sa.elm.ob.finance.ad_callouts.dao.BudgetDefinitionCalloutDAO;

/**
 * 
 * 
 * @author sathish kumar.p created on 04-10-2017
 *
 */

public class BudgetDefinitionCallout extends SimpleCallout {

  /**
   * This callout is used to set toperiod based on year
   */

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String inpYearId = info.vars.getStringParameter("inpcYearId");
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");

    if (inpLastFieldChanged.equals("inpcYearId")) {

      Period per = BudgetDefinitionCalloutDAO.getToPeriod(inpYearId);
      if (per != null) {
        info.addResult("inptoperiod", per.getId());
      }
    }
  }

}
