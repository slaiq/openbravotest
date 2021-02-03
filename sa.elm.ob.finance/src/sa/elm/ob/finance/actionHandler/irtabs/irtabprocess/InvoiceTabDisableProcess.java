package sa.elm.ob.finance.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

/**
 * 
 * @author sathishkumar 10-05-2018
 *
 */

public class InvoiceTabDisableProcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(InvoiceTabDisableProcess.class);

  /**
   * This class is used to disable copy button for encumbrance method as auto
   * 
   */

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      if (tabId.equals("290")) {

        Invoice inv = OBDal.getInstance().get(Invoice.class, recordId);
        if (inv.getEfinInvoicetypeTxt() != null && "PPA".equals(inv.getEfinInvoicetypeTxt())
            && "EFIN_CA".equals(inv.getDocumentStatus())) {
          enable = 1;
        } else {
          enable = 0;
        }
      }
    } catch (Exception e) {
      log.error("Exception in EncumbranceCopyIconDisableProcess: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
