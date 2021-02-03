package sa.elm.ob.finance.actionHandler;

import java.util.Date;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Mouli.K
 *
 */
public class PurchaseInvoicePrint extends BaseActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(PurchaseInvoicePrint.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      jsonRequest.put("status", 1);
      LOG.debug(" jsonRequest: " + jsonRequest);
      final String invoiceId = jsonRequest.getString("recordId");
      if (!invoiceId.equals("")) {
        Invoice invObj = OBDal.getInstance().get(Invoice.class, invoiceId);
        if (invObj != null) {
          String currencyId = invObj.getCurrency().getId();
          String fromCur = invObj.getCurrency().getISOCode();
          Date invoiceDate = invObj.getInvoiceDate();
          String clientId = invObj.getClient().getId();
          if (!invObj.getCurrency().equals(invObj.getClient().getCurrency())) {
            OBQuery<ConversionRateDoc> exchangeRateObj = OBDal.getInstance()
                .createQuery(ConversionRateDoc.class, "as a  where a.invoice.id= :PinvoiceId ");
            exchangeRateObj.setNamedParameter("PinvoiceId", invoiceId);
            if (exchangeRateObj.list().size() == 0) {
              OBQuery<ConversionRate> conversionRateObj = OBDal.getInstance().createQuery(
                  ConversionRate.class,
                  "as a  where a.client.id= :PclientId and :PinvoiceDate between a.validFromDate and a.validToDate and a.currency.id = :PcurrencyId and toCurrency.id =:toCurrency ");
              conversionRateObj.setNamedParameter("PclientId", clientId);
              conversionRateObj.setNamedParameter("PinvoiceDate", invoiceDate);
              conversionRateObj.setNamedParameter("PcurrencyId", currencyId);
              conversionRateObj.setNamedParameter("toCurrency",
                  invObj.getClient().getCurrency().getId());
              if (conversionRateObj.list().size() == 0) {
                json.put("fromCurrency", fromCur);
                json.put("status", 0);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Purchase Invoice Print :", e);
      }
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

}
