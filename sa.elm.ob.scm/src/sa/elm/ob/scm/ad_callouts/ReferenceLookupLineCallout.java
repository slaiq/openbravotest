package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * 
 * @author Mouli K
 * @implNote This callout is to copy same as the Advance Payment % to Retainage % & Recoupment %
 *
 */

public class ReferenceLookupLineCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(ReferenceLookupLineCallout.class);
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpadvpayper = vars.getStringParameter("inpadvancePaymentPer");

    try {
      OBContext.setAdminMode();
      if (inpLastFieldChanged.equals("inpadvancePaymentPer")) {
        info.addResult("inpretainagePer", inpadvpayper);
        info.addResult("inprecoupmentPer", inpadvpayper);
      }
    } catch (Exception e) {
      log.error("Exception in ReferenceLookupLineCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
