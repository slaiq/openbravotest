package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class RFPSalesVoucher extends IRTabIconVariables {
  Logger log = Logger.getLogger(RFPSalesVoucher.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");

      /* RFP sales Voucher-Header */
      if (!recordId.equals("")) {
        Escmsalesvoucher salesVouch = OBDal.getInstance().get(Escmsalesvoucher.class, recordId);
        if (salesVouch != null) {
          if (salesVouch.getDocumentStatus().equals("CO"))
            enable = 1;
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
