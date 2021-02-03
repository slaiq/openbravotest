
package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

@SuppressWarnings("serial")
public class DocumentTypeHeaderCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(DocumentTypeHeaderCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String docCategory = vars.getStringParameter("inpdocbasetype");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    try {

      if (inpLastFieldChanged.equals("inpdocbasetype")) {
        info.addResult("inpemEscmDoccategory", docCategory);
      }

    } catch (Exception e) {
      log.error("Exception in DocumentTypeHeaderCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
