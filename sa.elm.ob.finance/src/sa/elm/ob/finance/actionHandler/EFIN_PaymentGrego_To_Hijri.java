package sa.elm.ob.finance.actionHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;

import sa.elm.ob.finance.date.DateConverter;

public class EFIN_PaymentGrego_To_Hijri extends BaseActionHandler {
  Logger log4j = Logger.getLogger(EFIN_PaymentGrego_To_Hijri.class);

  @SuppressWarnings("deprecation")
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      final HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);

      SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      DateConverter dtConverter = new DateConverter(vars);

      final JSONObject jsonData = new JSONObject(data);

      // get the data as json
      String paymentdate = (jsonData.getString("paymentdate").equals("null") ? df.format(now)
          : jsonData.getString("paymentdate"));

      Date initDate = new SimpleDateFormat("yyyy-mm-dd").parse(paymentdate);
      SimpleDateFormat formatter = new SimpleDateFormat("dd-mm-yyyy");
      String paymentDate = formatter.format(initDate);
      String hijridate = null;

      // create the result
      JSONObject json = new JSONObject();
      hijridate = dtConverter.convertGregoriantoHijri(paymentDate);
      json.put("hijridate", hijridate);

      // and return it
      return json;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}