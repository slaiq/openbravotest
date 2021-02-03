package sa.elm.ob.finance.dms.service;

import org.openbravo.model.common.invoice.Invoice;

public interface DMSInvoiceService {

  /**
   * This method is used to do DMS operation during reject, revoke and reactivate
   * 
   * @param invoice
   */
  void rejectAndReactivateOperations(Invoice invoice) throws Exception;

}
