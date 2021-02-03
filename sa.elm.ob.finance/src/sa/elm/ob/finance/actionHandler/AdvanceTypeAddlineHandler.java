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

import sa.elm.ob.finance.actionHandler.dao.AdvanceTypeHandlerDAO;

/**
 * 
 * @author Poongodi 15/12/2017
 *
 */

public class AdvanceTypeAddlineHandler extends BaseProcessActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AdvanceTypeAddlineHandler.class);

  /**
   * This class is used to handle add line process in advance type window
   */
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      final String distributionId = jsonRequest.getString("inpefinDistributionId");
      int result = 0;
      result = AdvanceTypeHandlerDAO.insertlineinadvancetype(selectedlines, distributionId);
      if (result == 1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } else if (result == -1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_insert_Addline"));
        json.put("message", successMessage);
        return json;
      }

      else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_advAddline_Error"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in AdvanceTypeAddlineHandler:", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
