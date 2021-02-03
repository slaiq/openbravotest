package sa.elm.ob.finance.actionHandler.RdvHoldRelease;

import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAO;
import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAOImpl;

public class POHoldReleaseRevert extends BaseProcessActionHandler {
  private static final Logger log = LoggerFactory.getLogger(HoldReleaseVersion.class);

  @SuppressWarnings("unused")
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    JSONObject json = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      HoldReleaseLineHandlerDAO dao = new HoldReleaseLineHandlerDAOImpl();
      String newTxnId = jsonRequest.getString("inpefinRdvtxnId");
      JSONObject penaltyLines = jsonparams.getJSONObject("Rel_Revert");
      JSONArray selectedLines = penaltyLines.getJSONArray("_selection");
      BudgetHoldPlanReleaseDAO holdPlanReleaseDAO = new BudgetHoldPlanReleaseDAOImpl();
      Boolean isAlreadyAmtReleased = holdPlanReleaseDAO.releaseRevertValidatio(selectedLines);

      if (isAlreadyAmtReleased) {
        OBDal.getInstance().rollbackAndClose();
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text",
            OBMessageUtils.messageBD("EFIN_HoldRelEntAmtNotGrtThanRemRelAmt"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject result = holdPlanReleaseDAO.releaseRevert(selectedLines);
        if (result.has("result")) {
          if (result.getString("result").equals("1")) {
            OBDal.getInstance().flush();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "success");
            errorMessage.put("text", OBMessageUtils.messageBD("Efin_Release_RevertSuccess"));
            json.put("message", errorMessage);
            return json;
          } else {
            OBDal.getInstance().rollbackAndClose();
            JSONObject successMessage = new JSONObject();
            successMessage.put("severity", "error");
            successMessage.put("text", OBMessageUtils.messageBD("Efin_Release_RevertNotSuccess"));
            json.put("message", successMessage);
            return json;
          }
        } else {
          OBDal.getInstance().rollbackAndClose();
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text", OBMessageUtils.messageBD("Efin_Release_RevertNotSuccess"));
          json.put("message", successMessage);
          return json;
        }
      }

    } catch (Exception e) {
      log.error(" Exception in HoldReleaseVersion() " + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
  }
}
