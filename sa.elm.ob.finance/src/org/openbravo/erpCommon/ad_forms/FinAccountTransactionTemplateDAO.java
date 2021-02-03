package org.openbravo.erpCommon.ad_forms;

import java.util.List;

import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Interface for invoice posting DAO operations
 * 
 * @author Gopinagh. R
 *
 */
public interface FinAccountTransactionTemplateDAO {

  /**
   * Get the id of the invoice for which the transaction is being posted.
   * 
   * @param strRecordId
   *          - {@link FIN_FinaccTransaction} primary ID
   * 
   */
  String getInvoice(String strRecordId);

  /**
   * Get list of {@linkplain FIN_Payment} associated with an invoice
   * 
   * @param strInvoiceId
   *          {@link Invoice} Id
   * @return list of payments.
   */

  List<FIN_Payment> getListOfPayments(String strInvoiceId);

  /**
   * returns a list of {@link FIN_FinaccTransaction} associated with an invoice
   * 
   * @param strInvoiceId
   *          {@link Invoice} Id
   * @return list of payments.
   */

  List<FIN_FinaccTransaction> getListOfFinancialAccountTrx(String strInvoiceId);

  /**
   * determine whether the record can be posted.
   * 
   * @param strRecordId
   *          - {@link FIN_FinaccTransaction} primary ID
   */

  Boolean allowPosting(String strRecordId);
}
