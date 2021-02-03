package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class CustodyRtnTransaction extends IRTabIconVariables {
  Logger log = Logger.getLogger(CustodyRtnTransaction.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /* Return Transaction-Custody Transaction and Issue Return Transaction-Custody Transaction */
      if (!recordId.equals("")) {
        ShipmentInOutLine shipment = OBDal.getInstance().get(ShipmentInOutLine.class, recordId);
        if (shipment != null) {
          if (shipment.getShipmentReceipt().getDocumentStatus().equals("CO"))
            enable = 1;
        }
      }
      if (tabId.equals("DD6AB8A564D5482795B0976F6A68FBC5")) {
        receivingType = "INR";
      } else if (tabId.equals("D4E9D5A2F73E4A15AEA52FD9A5A57902")) {
        receivingType = "IRT";
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
