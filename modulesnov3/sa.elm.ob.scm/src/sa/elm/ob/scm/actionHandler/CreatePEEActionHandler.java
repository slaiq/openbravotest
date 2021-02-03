package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.actionHandler.dao.CreatePEEActionHandlerDAO;
import sa.elm.ob.scm.actionHandler.dao.CreatePEEActionHandlerDAOImpl;

public class CreatePEEActionHandler extends BaseActionHandler {
  private static final Logger log = Logger.getLogger(CreatePEEActionHandler.class);

  private static String PEE_TABID = "61D6CF3612134CAF942B811EC74B1F0B";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    JSONObject result = new JSONObject();
    try {
      JSONObject request = new JSONObject(content);
      final String proposalId = request.getString("Escm_Proposalmgmt_ID");

      // Execute process and prepare an array with actions to be executed
      JSONArray actions = new JSONArray();
      JSONObject recordInfo = new JSONObject();

      // Open the pee tab after clicking Create PEE/Display button
      JSONObject peeTabAction = new JSONObject();
      String msgText = null;

      CreatePEEActionHandlerDAO dao = new CreatePEEActionHandlerDAOImpl();

      // pee creation
      JSONObject resultPEEJson = dao.createPEE(proposalId);
      if (resultPEEJson.has("result")) {
        if (resultPEEJson.getString("result").equals("1")) {
          recordInfo.put("tabId", PEE_TABID);
          recordInfo.put("recordId", resultPEEJson.getString("peeId"));
          recordInfo.put("wait", true);
          peeTabAction.put("openDirectTab", recordInfo);
          actions.put(peeTabAction);
          result.put("responseActions", actions);
          return result;
        } else if (resultPEEJson.getString("result").equals("0")) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMsg = new JSONObject();
          errorMsg.put("severity", "error");
          if (resultPEEJson.getString("errorMsg").contains("@")) {
            msgText = resultPEEJson.getString("errorMsg").replace("@", "");
            msgText = OBMessageUtils.messageBD(msgText);
          } else {
            msgText = resultPEEJson.getString("errorMsg");
          }
          errorMsg.put("text", msgText);
          result.put("message", errorMsg);
          return result;
        }
      }

      return result;
    } catch (JSONException e) {
      log.error("Exception in CreatePEEActionHandler", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        result.put("message", errorMessage);
        return result;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in CreatePEEActionHandler ", e1);
        throw new OBException(e1);
      }
    } catch (OBException e) {
      log.error("Exception in CreatePEEActionHandler", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", e.getMessage());
        result.put("message", errorMessage);
        return result;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in CreatePEEActionHandler ", e1);
        throw new OBException(e1);
      }
    } catch (Exception e) {
      log.error("Exception in CreatePEEActionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        result.put("message", errorMessage);
        return result;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in CreatePEEActionHandler ", e1);
        throw new OBException(e1);

      }
    }
  }
}
