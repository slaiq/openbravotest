package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class ReturnTrnAndCustodyTransac extends IRTabIconVariables {
  Logger log = Logger.getLogger(ReturnTrnAndCustodyTransac.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");

      /* Return Transaction-Header and Custody Transfer-Custody Transfer */
      if (!recordId.equals("")) {
        ShipmentInOut poreceipt = OBDal.getInstance().get(ShipmentInOut.class, recordId);
        if (poreceipt != null) {
          if (poreceipt.getDocumentStatus().equals("CO")
              || poreceipt.getDocumentStatus().equals("DR")) {
            OBQuery<ShipmentInOutLine> returntranline = OBDal.getInstance()
                .createQuery(ShipmentInOutLine.class, "shipmentReceipt.id='" + recordId + "'");
            if (returntranline.list() != null && returntranline.list().size() > 0) {
              enable = 1;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
