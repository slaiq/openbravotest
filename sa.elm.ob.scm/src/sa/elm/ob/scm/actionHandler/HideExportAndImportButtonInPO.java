package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;

/**
 * 
 * @author DivyaPrakash JS on 15-05-2019
 *
 */
public class HideExportAndImportButtonInPO extends BaseActionHandler {
  private static Logger log4j = Logger.getLogger(HideExportAndImportButtonInPO.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {
      result.put("result", 0);
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, (String) parameters.get("recordId"));
      if (order != null && order.getOrderLineList().size() > 0) {
        result.put("result", 1);
      }
    } catch (Exception e) {
      log4j.error("Exception in HideExportAndImportButtonInPO :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
