package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class PaymentOutDiableProcess extends BaseActionHandler {
  Logger log4j = Logger.getLogger(PaymentOutDiableProcess.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {

      // get the data as json
      final JSONObject jsonData = new JSONObject(data);
      final String recordId = jsonData.getString("recordId");
      log4j.debug("recordid:" + recordId);
      Query InvCount = null;
      // create the result
      JSONObject json = new JSONObject();
      OBContext.setAdminMode(true);
      if (!recordId.equals("")) {
        String strQuery = "select count(*) as total,fin_payment_id from fin_payment "
            + " where fin_payment_id = '" + recordId
            + "' and status  in ('PPM') group by fin_payment_id";
        InvCount = OBDal.getInstance().getSession().createSQLQuery(strQuery.toString());
        log4j.debug("payCount:" + InvCount.list().size());
        if (InvCount.list() != null && InvCount.list().size() > 0) {
          for (Object o : InvCount.list()) {
            Object[] row = (Object[]) o;
            int count = Integer.parseInt(row[0].toString());
            log4j.debug("count:" + count);
            json.put("payCount", count);
          }
        } else
          json.put("PayCount", 0);
      }
      log4j.debug("json:" + json);
      OBContext.restorePreviousMode();
      // and return it
      return json;

    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}