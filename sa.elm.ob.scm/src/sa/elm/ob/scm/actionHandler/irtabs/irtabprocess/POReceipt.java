package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class POReceipt extends IRTabIconVariables {
  Logger log = Logger.getLogger(POReceipt.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String btnName = jsonData.optString("button") == "" ? null
          : jsonData.getString("button");
      final String tabId = jsonData.getString("tabId");

      if (!recordId.equals("")) {
        ShipmentInOut poreceipt = OBDal.getInstance().get(ShipmentInOut.class, recordId);
        if (poreceipt != null) {
          if (!poreceipt.getDocumentStatus().equals("DR")
              && !poreceipt.getEscmReceivingtype().equals("IRT")
              && !poreceipt.getEscmReceivingtype().equals("RET")
              && !poreceipt.getEscmReceivingtype().equals("INR"))
            enable = 1;
          receivingType = poreceipt.getEscmReceivingtype();
        } else {
          ShipmentInOutLine inoutline = OBDal.getInstance().get(ShipmentInOutLine.class, recordId);
          if (inoutline != null) {
            ShipmentInOut inout = inoutline.getShipmentReceipt();
            if (inout != null) {
              receivingType = inout.getEscmReceivingtype();
            }
          }
        }

        if (poreceipt != null && poreceipt.getDocumentType().isEscmIsporeceipt()
            && (btnName == null || !btnName.equals("delete"))) {
          enable = 1;
        }
        if (tabId.equals("296")) {
          if (btnName != null && btnName.equals("print")) {
            if (poreceipt != null && StringUtils.isNotEmpty(recordId)) {
              if (poreceipt.getEscmReceivingtype().equals("PROJ")
                  || poreceipt.getEscmReceivingtype().equals("RET"))
                enable = 0;
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