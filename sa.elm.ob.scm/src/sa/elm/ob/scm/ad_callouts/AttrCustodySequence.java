package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class AttrCustodySequence extends SimpleCallout {
  private static Logger log = Logger.getLogger(AttrCustodySequence.class);
  private static final long serialVersionUID = 1L;

  /**
   * Callout to update sequence to empty when checkbox unchecked
   */
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpemEscmIscstdycard = vars.getStringParameter("inpemEscmIscstdycard");
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpemEscmIscstdycard")) {
        if (inpemEscmIscstdycard.equals("N")) {
          info.addResult("inpemEscmSequence", "");
        }
      }
    } catch (Exception e) {
      log.error("Exception in AttrCustodySequence:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
