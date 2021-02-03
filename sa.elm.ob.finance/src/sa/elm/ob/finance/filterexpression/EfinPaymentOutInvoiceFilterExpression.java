
package sa.elm.ob.finance.filterexpression;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

/**
 * @author Sathish kumar
 * 
 *         This class is to filter the payment details based on invoice selected in header of
 *         payment out
 * 
 */
public class EfinPaymentOutInvoiceFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(EfinPaymentoutFilterExpression.class);

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("filterExpressionColumnName");
      if (strCurrentParam.equals("invoiceNo") && context.has("inpemEfinInvoiceId")) {
        if (StringUtils.isNotEmpty(context.getString("inpemEfinInvoiceId"))) {
          final Invoice inv = OBDal.getInstance().get(Invoice.class,
              context.getString("inpemEfinInvoiceId"));
          if (inv != null) {
            log4j.debug("invoice:" + inv);
            return inv.getDocumentNo() != null ? inv.getDocumentNo() : "";
          }
        }
      }
    } catch (JSONException e) {
      log4j.debug("Error getting the filter expression in paymentout" + strCurrentParam + " "
          + e.getMessage());
      return null;
    }
    return null;
  }
}
