package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ad_callouts.dao.SalesVoucherDAO;

/**
 * Callout to update the document date in Bid Management sales vouchers tab
 * 
 * @author qualian
 */
@SuppressWarnings("serial")
public class RFPSalesVoucherCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    String inpDocumentDate = vars.getStringParameter("inpdocumentdateh");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    try {
      if (inpLastFieldChanged.equals("inpdocumentdateh")) {
        info.addResult("inpdocumentdategreg", SalesVoucherDAO.getGregorianDate(inpDocumentDate));
      }
    } catch (Exception e) {
      log4j.error("Exception in RFPSalesVoucherCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
