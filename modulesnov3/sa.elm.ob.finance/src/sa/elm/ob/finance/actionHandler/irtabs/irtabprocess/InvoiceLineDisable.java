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
 * @author Gokul 16/06/2020
 *
 */

public class InvoiceLineDisable extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(InvoiceLineDisable.class);

  @Override
  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    // TODO Auto-generated method stub

    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      if (tabId.equals("291")) {

        Invoice invoiceLine = OBDal.getInstance().get(Invoice.class, recordId);

        if (invoiceLine.getTransactionDocument().isEfinIspomatch()
            || invoiceLine.getTransactionDocument().isEfinIsrdvinv()) {
          enable = 1;
        } else {
          enable = 0;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Exception in InvoiceLineDisable: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
