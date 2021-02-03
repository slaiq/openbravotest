package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;

/**
 * 
 * 
 */
public class EscmLOCertficate extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmLOCertficate.class);
  /**
   * Callout to update the line Details in
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpCertificateName = vars.getStringParameter("inpcertificatename");

    try {
      if (inpLastFieldChanged.equals("inpcertificatename")) {
        if (inpCertificateName != null) {
          ESCMDefLookupsTypeLn loLookUp = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
              inpCertificateName);
          if (loLookUp.getSearchKey().equals("LO")) {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('LO_Sequenceno').show()");
          } else {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('LO_Sequenceno').hide()");
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in EscmLOCertficate callout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
