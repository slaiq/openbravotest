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
 * @author Priyanka Ranjan on 10/10/2017
 */

public class MaintainEncumbranceControlCallout extends SimpleCallout {

  /**
   * Callout to update in MaintainEncumbranceControl Window
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    try {
      VariablesSecureApp vars = info.vars;

      String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
      String inpencType = vars.getStringParameter("inpencType");

      if (inpLastFieldChanged.equals("inpisactive")) {
        info.addResult("inpencmethodAuto", false);
        info.addResult("inpencmethodManual", false);
      }
      if (inpLastFieldChanged.equals("inpencType")) {
        if (inpencType.equals("AEE") || inpencType.equals("AAE")) {
          info.addResult("inpencSource", "AP");
        } else if (inpencType.equals("DE") || inpencType.equals("TE")) {
          info.addResult("inpencSource", "BUD");
        } else if (inpencType.equals("PRE") || inpencType.equals("BE") || inpencType.equals("POE")
            || inpencType.equals("PAE")) {
          info.addResult("inpencSource", "PUR");
        } else if (inpencType.equals("AET") || inpencType.equals("MOP") || inpencType.equals("MAP")
            || inpencType.equals("ODP")) {
          info.addResult("inpencSource", "HR");
        }
        if (inpencType.equals("MOP") || inpencType.equals("AET")) {
          info.addResult("inpencmethodAuto", true);
          info.addResult("inpencmethodManual", false);
        } else if (inpencType.equals("MAP") || inpencType.equals("ODP")) {
          info.addResult("inpencmethodManual", true);
          info.addResult("inpencmethodAuto", false);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in MaintainEncumbranceControlCallout: " + e);
    }

  }
}
