package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class IssueReturnTransac extends IRTabIconVariables {
  Logger log = Logger.getLogger(IssueReturnTransac.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /* Issue Return Transaction-Header */
      if (!recordId.equals("")) {
        ShipmentInOut poreceipt = OBDal.getInstance().get(ShipmentInOut.class, recordId);
        if (poreceipt != null) {
          if (poreceipt.getDocumentStatus().equals("CO"))
            enable = 1;
        }
      }
      if (tabId.equals("0C0819F5D78A401A916BDD8ADB30E4EF")) {
        receivingType = "INR";
      } else if (tabId.equals("5B16AE5DFDEF47BB9518CDD325F31DFF")) {
        receivingType = "IRT";
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
