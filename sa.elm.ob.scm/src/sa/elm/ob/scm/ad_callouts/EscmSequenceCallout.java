package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * 
 * @author Gopalakrishnan on 15/04/2017
 * 
 */
public class EscmSequenceCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmSequenceCallout.class);
  /**
   * Callout to update next assigned number in Sequence Window
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strStartNo = vars.getStringParameter("inpstartno");
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpstartno")) {
        if (strStartNo != null) {
          info.addResult("inpcurrentnext", strStartNo);
        }
      }
    } catch (Exception e) {
      log.error("Exception in EscmSequenceCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
