package sa.elm.ob.finance.ad_process.PurchaseInvoice.vat;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

/**
 * @author Gopinagh.R on 26-04-2018
 */

public class TaxLinesHandler implements Process {

  private static final Logger log4j = Logger.getLogger(TaxLinesHandler.class);
  private OBError obError = new OBError();
  private static final String DRAFT = "DR";

  @Override
  public synchronized void execute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      // Connection connection = null;
      TaxLineHandlerDAO dao = null;

      // try {
      //
      // ConnectionProvider provider = bundle.getConnection();
      // connection = provider.getConnection();
      // dao = new TaxLineHandlerImpl(connection);
      //
      // } catch (NoConnectionAvailableException e) {
      //
      // log4j.error("No Database Connection Available.Exception:" + e);
      // throw new RuntimeException(e);
      // }
      dao = new TaxLineHandlerImpl();
      String strInvoiceId = (String) bundle.getParams().get("C_Invoice_ID");

      if (StringUtils.isNotEmpty(strInvoiceId)) {
        Invoice invoice = dao.getInvoice(strInvoiceId);

        if (!DRAFT.equals(invoice.getDocumentStatus())) {
          obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_AlreadyPreocessed@");
          bundle.setResult(obError);
          return;
        }

        if (!(invoice.getGrandTotalAmount().compareTo(BigDecimal.ZERO) > 0)) {
          obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_Grandtotal<0@");
          bundle.setResult(obError);
          return;
        }
        if (invoice.isEfinIstax()) {

          if (invoice.getEfinTaxMethod() == null) {
            obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_NoTaxMethod@");
            bundle.setResult(obError);
            return;
          }
          obError = dao.insertTaxLines(strInvoiceId);
        } else {
          obError = dao.removeTaxLines(strInvoiceId);
        }
        bundle.setResult(obError);
        return;
      }

    } catch (Exception e) {
      log4j.error("Exception while adding tax lines : " + e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}