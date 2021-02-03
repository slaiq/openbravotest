package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

/**
 * @author Priyanka Ranjan on 10/04/2017
 */

public class EscmBusinessPartnerCallout extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    if (inpLastFieldChanged.equals("inpemEfinZakatcertificateno")) {
      info.addResult("inpemEfinZakatexpirydate", "");
    }
    if (inpLastFieldChanged.equals("inpemEfinSagiano")) {
      info.addResult("inpemEfinSagiaexpirydate", "");
    }
    if (inpLastFieldChanged.equals("inpemEfinIqamano")) {
      info.addResult("inpemEfinIqamaexpirydate", "");
    }
    if (inpLastFieldChanged.equals("inpemEscmCrnumber")) {
      info.addResult("inpemEscmCrexpirydate", "");
    }
  }
}