package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

/**
 * @author Priyanka Ranjan on 02/08/2016
 */

public class EfinBusinessPartnerCallout extends SimpleCallout {

  /**
   * Callout to update the ON HOLD and BLACKLIST fields Information in BusinessPartner Window
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpvendorBlocking = vars.getStringParameter("inpvendorBlocking");
    String inpefinBlacklist = vars.getStringParameter("inpemEfinBlacklist");
    String inpisvendor = vars.getStringParameter("inpisvendor");
    if (inpLastFieldChanged.equals("inpvendorBlocking")) {
      if (inpvendorBlocking.equals("Y")) {
        info.addResult("inpemEfinBlacklist", false);
        info.addResult("inpemEfinReason", "");
      }
    }
    if (inpLastFieldChanged.equals("inpemEfinBlacklist")) {
      if (inpefinBlacklist.equals("Y")) {
        info.addResult("inpvendorBlocking", false);
        info.addResult("inppoOrderBlocking", true);
        info.addResult("inppoGoodsBlocking", true);
        info.addResult("inppoInvoiceBlocking", true);
        info.addResult("inppoPaymentBlocking", true);
      }
      if (inpefinBlacklist.equals("N")) {
        info.addResult("inpemEfinReason", "");
      }
    }
    if (inpLastFieldChanged.equals("inpisvendor")) {
      if (inpisvendor.equals("N")) {
        info.addResult("inpemEfinBlacklist", false);
        info.addResult("inpemEfinReason", "");
        info.addResult("inpemEfinMulprepaymentEntity", false);
      }
    }
  }
}
