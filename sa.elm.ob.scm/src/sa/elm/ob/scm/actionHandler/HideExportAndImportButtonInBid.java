package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmBidMgmt;

/**
 * 
 * @author Gokul on 21-05-2020
 *
 */
public class HideExportAndImportButtonInBid extends BaseActionHandler {
  private static Logger log4j = Logger.getLogger(HideExportAndImportButtonInBid.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {
      result.put("result", 0);
      OBContext.setAdminMode();
      EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class,
          (String) parameters.get("recordId"));
      if (bid != null && bid.getEscmBidmgmtLineList().size() > 0) {
        result.put("result", 1);
      }
    } catch (Exception e) {
      log4j.error("Exception in HideExportAndImportButtonInBid :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
