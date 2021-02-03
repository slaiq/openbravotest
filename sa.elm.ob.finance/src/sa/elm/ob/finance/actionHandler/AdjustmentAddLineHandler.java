package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.actionHandler.dao.AdjustmentAddLineHandlerDao;

/**
 * 
 * @author Gowtham.V
 *
 */
public class AdjustmentAddLineHandler extends BaseProcessActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AdjustmentAddLineHandler.class);

  /**
   * This class is used to handle add line process in adjustment window.
   */
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("lines");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      final String adjustmentId = jsonRequest.getString("inpefinBudgetadjId");
      int result = 0;
      result = AdjustmentAddLineHandlerDao.insertAdjustmentLines(selectedlines, adjustmentId);
      if (result == 1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } else if (result == -1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_Line_Not_Selected"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_AdjAddline_Error"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in adjustment add line handler :", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
