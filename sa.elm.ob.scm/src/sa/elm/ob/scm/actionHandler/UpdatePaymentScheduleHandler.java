package sa.elm.ob.scm.actionHandler;

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

import sa.elm.ob.scm.actionHandler.dao.UpdatePaymentScheduleHandlerDAO;

/**
 * 
 * @author Mouli K
 *
 */

public class UpdatePaymentScheduleHandler extends BaseProcessActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(UpdatePaymentScheduleHandler.class);

  /**
   * This class is used to handle update process in Payment Schedule window.
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
      final String orderId = jsonRequest.getString("inpcOrderId");
      int result = 0;
      result = UpdatePaymentScheduleHandlerDAO.updatePaymentScheduleLines(selectedlines, orderId);
      if (result == 2) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_PaymentSchedule_Amt_Zero"));
        json.put("message", successMessage);
        return json;
      } else if (result == 3) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_PO_paymentamt_eq_poamt"));
        json.put("message", successMessage);
        return json;
      } else if (result == 4) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_PaymentScheduleAmt_lt_invAmt"));
        json.put("message", successMessage);
        return json;
      } else if (result == 5) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text",
            OBMessageUtils.messageBD("ESCM_PaymentSchedule_FinalPay_eq_Retainage"));
        json.put("message", successMessage);
        return json;
      } else if (result == 6) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_PaySch_UniqueCodeLvl_WC_AmtVal"));
        json.put("message", successMessage);
        return json;
      } else if (result == 7) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text",
            OBMessageUtils.messageBD("ESCM_PaySch_UniqueCodeLvl_WOC_AmtVal"));
        json.put("message", successMessage);
        return json;
      } else if (result == 8) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text",
            OBMessageUtils.messageBD("ESCM_PaySchedule_Needbydate_Mandatory"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Update Payment Schedule handler :", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
