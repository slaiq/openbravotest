package sa.elm.ob.finance.filterexpression;

import java.text.DateFormat;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * This class is to apply Saved Bank Sent Date from Payment Out screen
 * 
 * @author Mouli.K
 */
public class PaymentOutStatusSentDate implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    // TODO Auto-generated method stub
    requestMap = _requestMap;
    try {
      OBContext.setAdminMode();

      JSONObject client = new JSONObject(requestMap.get("context"));
      String paymentId = client.getString("inpfinPaymentId");
      DateFormat dateYearFormat = Utility.YearFormat;
      String sentdate = "";

      FIN_Payment objPO = null;
      OBQuery<FIN_Payment> paymentout = OBDal.getInstance().createQuery(FIN_Payment.class,
          " as e where  e.id=:paymentId");
      paymentout.setNamedParameter("paymentId", paymentId);

      List<FIN_Payment> paymentoutList = paymentout.list();

      if (paymentoutList.size() > 0) {
        objPO = paymentoutList.get(0);
      }
      if (objPO != null ) {
        if( objPO.getEfinBanksentdate() != null ) {
          sentdate = dateYearFormat.format(objPO.getEfinBanksentdate());
          sentdate = UtilityDAO.convertTohijriDate(sentdate);
        }
        //sentdate = dateYearFormat.format(objPO.getEfinBanksentdate());
        //objPO.getEfinBanksentdate();
        //sentdate = UtilityDAO.convertTohijriDate(sentdate);

        return sentdate;
      } else {
        return sentdate;
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
