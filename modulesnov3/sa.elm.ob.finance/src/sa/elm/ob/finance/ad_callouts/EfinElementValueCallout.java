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

/**
 * @author Priyanka Ranjan on 13/10/2017
 */

public class EfinElementValueCallout extends SimpleCallout {

  /**
   * Callout to update in Element Value tab in Account tree Window
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    // while change "Summary Level" Set "Department Fund" Checkbox as 'N'
    if (inpLastFieldChanged.equals("inpissummary")) {
      info.addResult("inpemEfinIsdeptfund", false);
    }

  }
}