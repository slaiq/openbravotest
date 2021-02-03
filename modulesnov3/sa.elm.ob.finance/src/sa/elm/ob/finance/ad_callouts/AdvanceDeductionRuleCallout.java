/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class AdvanceDeductionRuleCallout extends SimpleCallout {

  /**
   * Callout to update in Advance Deduction Rule MT Window
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inprules = vars.getStringParameter("inprules");

    if (inpLastFieldChanged.equals("inprules")) {
      if (inprules.equals("WAM")) {
        info.addResult("inpformula", "GM");
      } else if (inprules.equals("AM")) {
        info.addResult("inpformula", "AP");
      } else if (inprules.equals("PO")) {
        info.addResult("inpformula", "DP");
      }
    }
  }
}
