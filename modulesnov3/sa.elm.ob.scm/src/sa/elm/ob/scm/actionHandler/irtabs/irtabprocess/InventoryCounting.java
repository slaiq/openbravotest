package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class InventoryCounting extends IRTabIconVariables {
  Logger log = Logger.getLogger(InventoryCounting.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");

      /* Inventory Counting-Lines */
      if (!recordId.equals("")) {
        InventoryCount Invcount = OBDal.getInstance().get(InventoryCount.class, recordId);
        if (Invcount != null) {
          if (Invcount.getEscmStatus().equals("CO"))
            enable = 1;
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
