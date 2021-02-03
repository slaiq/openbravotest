package sa.elm.ob.finance.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

/**
 * 
 * @author sathishkumar 10-05-2018
 *
 */

public class RDVSummaryDisableProcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(RDVSummaryDisableProcess.class);

  /**
   * This class is used to disable print button based on status
   * 
   */

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      // final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      if (tabId.equals("A0F3A7D17A834A93B3BD4D2C40E77AFE")
          || tabId.equals("FDBA56F9D57A4F988F4CC6F3577428B9")) {

        // EfinRDVTransaction rdv = OBDal.getInstance().get(EfinRDVTransaction.class, recordId);
        // handle based on status
        enable = 1;
      }
    } catch (Exception e) {
      log.error("Exception in EncumbranceCopyIconDisableProcess: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
