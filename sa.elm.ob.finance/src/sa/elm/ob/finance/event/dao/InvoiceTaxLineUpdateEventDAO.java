package sa.elm.ob.finance.event.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

/**
 * @author Gopinagh. R on 09-04-2018
 * 
 */
public class InvoiceTaxLineUpdateEventDAO {
  private static final Logger log = Logger.getLogger(InvoiceTaxLineUpdateEventDAO.class);

  public static Boolean canUpdateInvoice(Invoice invoice) {
    Boolean isValid = Boolean.TRUE;
    try {
      if (invoice != null) {
        List<InvoiceLine> invoiceLines = invoice.getInvoiceLineList();
        BusinessPartner headerPartner = invoice.getBusinessPartner();

        if (invoiceLines.size() > 0) {

          int count = (int) (invoiceLines.stream()
              .filter(line -> line.getBusinessPartner() == (headerPartner)
                  && line.getEfinSecondaryBeneficiary() == null)
              // task no.- 7506 note-
              // 19775
              .count());

          if (count > 0)
            isValid = Boolean.FALSE;
        }
      }
    } catch (Exception e) {
      log.error("Exception while canUpdateInvoice: " + e);
    }
    return isValid;
  }

}
