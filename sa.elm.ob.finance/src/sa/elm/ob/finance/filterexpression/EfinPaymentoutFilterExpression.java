package sa.elm.ob.finance.filterexpression;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * @author Qualian
 * 
 */
public class EfinPaymentoutFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(EfinPaymentoutFilterExpression.class);

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");
      if (strCurrentParam.equals("EM_Efin_PaymentBeneficiary") && context.has("inpfinPaymentId")) {
        final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class,
            context.getString("inpfinPaymentId").toString());
        if (payment.getEFINPaymentBeneficiary() != null) {
          log4j.debug("PaymentBeneficiary:" + payment.getEFINPaymentBeneficiary().getId());
          return payment.getEFINPaymentBeneficiary().getId();
        }
      }
    } catch (JSONException e) {
      log4j.debug(
          "Error getting the default value in paymentout" + strCurrentParam + " " + e.getMessage());
      return null;
    }
    return null;
  }
}
