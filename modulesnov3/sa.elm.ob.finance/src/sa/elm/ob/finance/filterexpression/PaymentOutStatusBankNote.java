package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * 
 * This class is to apply Saved Bank Note from Payment Out screen
 * 
 * @author Mouli.K
 */
public class PaymentOutStatusBankNote implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    // TODO Auto-generated method stub
    requestMap = _requestMap;
    try {
      OBContext.setAdminMode();

      JSONObject client = new JSONObject(requestMap.get("context"));
      String paymentId = client.getString("inpfinPaymentId");

      FIN_Payment objPO = null;
      OBQuery<FIN_Payment> paymentout = OBDal.getInstance().createQuery(FIN_Payment.class,
          " as e where  e.id=:paymentId");
      paymentout.setNamedParameter("paymentId", paymentId);

      List<FIN_Payment> paymentoutList = paymentout.list();

      if (paymentoutList.size() > 0) {
        objPO = paymentoutList.get(0);
      }
      if (objPO != null) {
        return objPO.getEfinBanknote();
      } else {
        return "";
      }

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
