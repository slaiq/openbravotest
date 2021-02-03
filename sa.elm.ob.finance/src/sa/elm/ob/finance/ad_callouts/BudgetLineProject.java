package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

/**
 * @author Gopalakrishnan on 11/05/2106
 */

public class BudgetLineProject extends SimpleCallout {

  /**
   * Callout to update the project Information in BudgetLines Window
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpAccount = vars.getStringParameter("inpcElementvalueId");
    String project = "";

    if (!"".equals(inpAccount)) {
      ElementValue Account = OBDal.getInstance().get(ElementValue.class, inpAccount);
      project = Account.getEfinProject().getId();
    }

    if (project.equals("") || project == null) {
      info.addResult("inpefinProjectId", "");
    } else {
      info.addResult("inpefinProjectId", project);
    }

  }

}
