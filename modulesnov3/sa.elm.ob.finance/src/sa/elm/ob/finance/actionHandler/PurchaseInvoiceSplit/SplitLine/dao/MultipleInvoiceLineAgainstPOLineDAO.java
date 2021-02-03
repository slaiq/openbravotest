package sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao;

import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

/**
 * 
 * @author Priyanka Ranjan on 04/05/2019
 * 
 */
// Interface file for Split Invoice Line for a PO

public interface MultipleInvoiceLineAgainstPOLineDAO {

  /**
   * clone the InvoiceLine (Split the Selected Invoice Line)
   * 
   * @param invoiceline
   * @throws Exception
   */
  public void cloneInvoiceLine(InvoiceLine invoiceline) throws Exception;

  /**
   * get next line no. for splited line
   * 
   * @param invoiceline
   * @param clientId
   * @return next line no.
   * @throws Exception
   */
  public Long getNextLineNo(InvoiceLine invoiceline, String clientId) throws Exception;

  /**
   * check invoice lines total amount should not exceed order line amount
   * 
   * @param clientId
   * @param invoice
   * @return true or false
   * @throws Exception
   */
  public Boolean checkAmountValidationForSplit(String clientId, Invoice invoice) throws Exception;

  /**
   * check any line having amount zero
   * 
   * @param invoice
   * @return true or false
   * @throws Exception
   */
  public Boolean checkLineHavingZeroAmt(Invoice invoice) throws Exception;

  /**
   * check already line present in with same payment beneficiary and secondary beneficiary with same
   * order line
   * 
   * @param paymtbeni
   * @param secbene
   * @param clientId
   * @param orderLineId
   * @param invoiceId
   * @param invoicelineId
   * @return true or false
   * @throws Exception
   */
  public Boolean checkAlreadyBPCombinationExist(String paymtbeni, String secbene, String clientId,
      String orderLineId, String invoiceId, String invoicelineId) throws Exception;
}
