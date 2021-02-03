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

import sa.elm.ob.finance.actionHandler.dao.PaymentTrackingStatusDAO;

/**
 * 
 * @author Poongodi 18/12/2017
 *
 */

public class PaymentTrackingStatus extends BaseProcessActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentTrackingStatus.class);

  /**
   * This class is used to tracking the payment status
   */
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String paymentId = jsonRequest.getString("inpfinPaymentId");
      String bankName = jsonparams.getString("Bank_Name");
      String chequeNo = jsonparams.getString("Cheque_No");
      String chequeStatus = jsonparams.getString("Cheque_Status");
      String chequeDate = jsonparams.getString("Cheque_Date");
      String bankNote = jsonparams.getString("Bank_Note");
      String bankSentDate = jsonparams.getString("Bank_Sent_Date");
      String receiveChequeDate = jsonparams.getString("EM_Efin_Receive_Cheque_Date");

      int result = 0;
      result = PaymentTrackingStatusDAO.insertlineinpaymenttracking(paymentId, bankName, chequeNo,
          chequeDate, chequeStatus, bankNote, bankSentDate, receiveChequeDate);
      if (result == 1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } else if (result == -1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_FutureDate_Payment"));
        json.put("message", successMessage);
        return json;
      } else if (result == -2) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_Payissue_Man"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_Process_Failure"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in PaymentTrackingStatus:", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
