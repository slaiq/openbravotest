package sa.elm.ob.finance.ad_process.PurchaseInvoice.vat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

/**
 * @author Gopinagh. R
 *
 */
public interface TaxLineHandlerDAO {

  /**
   * Checks whether the given {@link InvoiceLine} is a tax line.
   * 
   * @param strInvoiceLineId
   *          Invoice line ID.
   * @return {@link Boolean}
   * 
   */
  public Boolean isTaxLine(String strInvoiceLineId);

  /**
   * Calculates the tax percentage applicable for the invoice
   * 
   * @param Invoice
   *          ID
   * @return tax percentage.
   */
  public BigDecimal getTaxPercent(String strInvoiceId);

  /**
   * Checks if the line amounts in the invoice is inclusive of tax
   * 
   * @param Invoice
   *          ID
   * 
   * @return {@link Boolean}
   */

  public Boolean isPriceInclusiveOfTax(String strInvoiceId);

  /**
   * Get list on regular non tax lines (excludes tax lines and deduction lines)
   * 
   * @param Invoice
   *          ID
   * @return {@link List} of {@link InvoiceLine} objects
   */
  public List<InvoiceLine> getLines(String strInvoiceId);

  /**
   * Method to calculate tax and create tax lines applicable for the invoice.
   * 
   * @param Invoice
   *          ID
   * 
   * @return {@link OBError}
   */
  public OBError insertTaxLines(String strInvoiceId);

  /**
   * Method to insert actual tax lines in the invoice.
   * 
   * @param strInvoiceId
   *          {@linkplain Invoice} Id
   * @param line
   *          {@linkplain InvoiceLine} object
   * 
   * @return {@link OBError}
   */
  public OBError insertInvoiceLines(String strInvoiceId, InvoiceLine line);

  /**
   * 
   * Method to calculate the next line number.
   * 
   * @param lineNumber
   *          - line number of the invoice line for which the tax is being calculated
   * @return calculated line number
   * 
   */
  public Long getLineNumber(Long lineNumber);

  /**
   * method to get the calculated tax and deduction amounts.
   * 
   * @param strInvoiceLineId
   *          {@linkplain InvoiceLine} ID.
   * @param sourceLine
   *          {@linkplain InvoiceLine} object
   * @return {@link JSONObject}
   */
  public JSONObject calculateTaxAmount(String strInvoiceLineId, InvoiceLine sourceLine);

  /**
   * Method to calculate the inclusive tax amount.
   * 
   * @param strInvoiceId
   *          {@linkplain Invoice} Id
   * @param sourceLine
   *          {@linkplain InvoiceLine} object
   * @param taxFactor
   *          (1+ tax percentage) to calculate tax amount
   */
  public BigDecimal calculateInclusiveTax(String strInvoiceId, InvoiceLine sourceLine,
      BigDecimal taxFactor);

  /**
   * method to get total line net amount grouped by beneficiary and secondary beneficiaries
   * 
   * @param line
   *          {@linkplain InvoiceLine} object
   * 
   * @return totalLineNetamount
   */
  public BigDecimal getBeneficiaryBasedTotalLineAmount(InvoiceLine line);

  /**
   * Calculate the deduction amount
   * 
   * @param sourceLine
   *          {@linkplain InvoiceLine} object
   * @param TotalDeductions
   *          TotalDeduction amount applicable
   * @param taxOnDeductions
   *          tax contributions on deduction lines
   * @return {@linkplain BigDecimal} amount to be deducted.
   */
  public BigDecimal calculateDeductionAmount(InvoiceLine sourceLine, BigDecimal TotalDeductions,
      BigDecimal taxOnDeductions);

  /**
   * Method to calculate the taxes on all deductions
   * 
   * @param strInvoiceId
   *          {@linkplain Invoice} ID.
   * 
   * @return {@linkplain Map} of taxes applicable for each and every invoice line
   * 
   */
  public Map<String, BigDecimal> getDeductionTaxes(String strInvoiceId);

  /**
   * Method to get the list of deduction lines in the invoice.
   * 
   * @param strInvoiceId
   *          {@linkplain Invoice} ID.
   * @return {@linkplain List} of deduction lines.
   * 
   */
  public List<InvoiceLine> getDeductionLines(String strInvoiceId);

  /**
   * Method to check if the tax needs to be recalculated before submitting the invoice .
   * 
   * @param invoice
   *          {@linkplain Invoice}
   * 
   * @return {@link Boolean}
   */
  public Boolean requiresTaxRecalculation(Invoice invoice);

  /**
   * Checks whether the invoice is a tax invoice and the amount is exclusive of tax.
   * 
   * @param invoice
   *          {@linkplain Invoice}
   * 
   * @return {@link Boolean}
   */
  public Boolean isExclusiveTaxInvoice(Invoice invoice);

  /**
   * Method to return a {@linkplain Map} of Unique code Id's and the corresponding tax amount in a
   * particular Invoice.
   * 
   * @param invoice
   *          {@linkplain Invoice}
   * 
   * @return {@link Boolean}
   */
  public Map<String, BigDecimal> getTaxLineCodesAndAmount(Invoice invoice);

  /**
   * Checks whether the provided invoice has any taxlines.
   * 
   * @param invoice
   * @return
   */
  public Boolean hasTaxLines(Invoice invoice);

  /**
   * Get {@link Invoice} object with specified invoice id.
   * 
   * @param strInvoiceId
   * @return
   */
  public Invoice getInvoice(String strInvoiceId);

  /**
   * Checks whether the provided invoice has any taxlines.
   * 
   * @param invoice
   * @return
   */
  public Boolean isTaxCalculated(InvoiceLine invoiceLine);

  /**
   * Checks whether the invoice has atleast one expense line.
   * 
   * @param invoice
   * @return
   */
  public Boolean hasExpenseLines(Invoice invoice);

  /**
   * Method to remove tax lines applicable for the invoice.
   * 
   * @param Invoice
   *          ID
   * 
   * @return {@link OBError}
   */
  public OBError removeTaxLines(String strInvoiceId);

  /**
   * Method to return a {@linkplain Map} of Unique code Id's and the corresponding tax amount in a
   * particular Invoice.
   * 
   * @param invoice
   *          {@linkplain Invoice}
   * 
   * @return {@linkplain Map}
   */
  public Map<String, BigDecimal> getTaxLineCodesAndAmountForRDV(Invoice invoice);

}
