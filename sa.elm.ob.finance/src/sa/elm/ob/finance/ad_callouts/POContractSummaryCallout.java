package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Priyanka Ranjan 14-12-2017
 */

public class POContractSummaryCallout extends SimpleCallout {

  /**
   * Callout to update the fields Information in PO Contract and Summary Window
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(POContractSummaryCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    final String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    try {
      // while changing encumbrance method remove value from encumbrance and Unique Code Field
      if (inpLastFieldChanged.equals("inpemEfinEncumMethod")) {
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('EM_Efin_Budget_Manencum_ID').setValue('')");
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error whie getting PO Contract and Summary callout:", e);
      }
    }
  }
}
