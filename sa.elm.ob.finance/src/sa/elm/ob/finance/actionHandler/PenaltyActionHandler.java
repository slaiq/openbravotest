package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Divya 11/12/2017
 *
 */

public class PenaltyActionHandler extends BaseProcessActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PenaltyActionHandler.class);

  /**
   * This class is used to handle add line process in Penalty Action.
   */
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      int result = 1;
      if (result == 1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } 
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Penalty Action Handler :", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

}
