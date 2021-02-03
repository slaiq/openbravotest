package sa.elm.ob.scm.ad_process.POandContract;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Gokul 02/01/2019
 *
 */
public class PoDeleteNewVersion extends BaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(PoDeleteNewVersion.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    boolean Status = false;
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);
      String recordId = jsonData.getString("recordId");
      JSONObject statusMessage = new JSONObject();
      Status = PoDeleteNewVersionDAO.deleteNewVersion(recordId);
      if (Status) {
        statusMessage.put("severity", "Success");
        statusMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        result.put("message", statusMessage);
      }
    } catch (Exception e) {
      log.error("Exception in PO Delete New version :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
