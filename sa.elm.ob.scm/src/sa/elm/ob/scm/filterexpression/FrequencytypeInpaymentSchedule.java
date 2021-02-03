package sa.elm.ob.scm.filterexpression;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.scm.ESCMPaymentSchedule;

/**
 * 
 * This class is to apply the default value in frequency list reference
 * 
 * @author poongodi on 15/07/2020
 */
public class FrequencytypeInpaymentSchedule implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    // TODO Auto-generated method stub
    requestMap = _requestMap;
    try {
      OBContext.setAdminMode();

      JSONObject client = new JSONObject(requestMap.get("context"));
      String orderId = client.getString("inpcOrderId");

      ESCMPaymentSchedule objPS = null;
      OBQuery<ESCMPaymentSchedule> paymentSchedule = OBDal.getInstance()
          .createQuery(ESCMPaymentSchedule.class, " as e where  e.documentNo.id=:orderId");
      paymentSchedule.setNamedParameter("orderId", orderId);

      List<ESCMPaymentSchedule> paymentoutList = paymentSchedule.list();

      if (paymentoutList.size() > 0) {
        objPS = paymentoutList.get(0);
      }
      if (objPS != null) {
        return "MT";
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
