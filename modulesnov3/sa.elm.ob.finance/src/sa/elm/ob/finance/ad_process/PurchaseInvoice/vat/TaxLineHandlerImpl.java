/**
 * 
 */
package sa.elm.ob.finance.ad_process.PurchaseInvoice.vat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopinagh. R
 *
 */
public class TaxLineHandlerImpl implements TaxLineHandlerDAO {
  // private Connection conn = null;
  private static final Logger log4j = Logger.getLogger(TaxLineHandlerImpl.class);
  BigDecimal PERCENT = new BigDecimal("0.01");
  Long increment = Long.parseLong("5");
  private Boolean isPriceInclOfTax = Boolean.FALSE;
  BigDecimal totalTaxAmount = BigDecimal.ZERO;
  BusinessPartner paymentBeneficiary = null;
  BusinessPartner secondaryBeneficiary = null;

  Map<String, BigDecimal> deductionTaxWeightage = new HashMap<String, BigDecimal>();
  private static final String EXPENSE_ACCOUNT = "E";
  Integer roundoffConst = 3;

  // public TaxLineHandlerImpl() {
  // }
  //
  // public TaxLineHandlerImpl(Connection connection) {
  // // this.conn = connection;
  // }

  @Override
  public Invoice getInvoice(String strInvoiceId) {
    Invoice invoice = null;
    try {
      invoice = Utility.getObject(Invoice.class, strInvoiceId);
    } catch (Exception e) {

      log4j.error("Exception while getInvoice: " + e);
    }
    return invoice;
  }

  @Override
  public Boolean isTaxLine(String strInvoiceLineId) {
    Boolean isTax = Boolean.FALSE;
    try {
      if (StringUtils.isNotEmpty(strInvoiceLineId)) {
        InvoiceLine line = Utility.getObject(InvoiceLine.class, strInvoiceLineId);

        if (line != null) {
          isTax = line.isEFINIsTaxLine() == null ? Boolean.FALSE : line.isEFINIsTaxLine();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getInvoice: " + e);
    }
    return isTax;
  }

  @Override
  public BigDecimal getTaxPercent(String strInvoiceId) {
    BigDecimal taxPercent = BigDecimal.ZERO, tax = BigDecimal.ZERO;

    try {
      Invoice invoice = null;
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        invoice = Utility.getObject(Invoice.class, strInvoiceId);

        if (invoice != null) {
          tax = new BigDecimal(invoice.getEfinTaxMethod().getTaxpercent());
          taxPercent = tax.multiply(PERCENT);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getTaxPercent: " + e);
    }

    return taxPercent;
  }

  @Override
  public Boolean isPriceInclusiveOfTax(String strInvoiceId) {
    Boolean isInclusive = Boolean.FALSE;

    try {
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        Invoice invoice = Utility.getObject(Invoice.class, strInvoiceId);
        isInclusive = invoice.getEfinTaxMethod().isPriceIncludesTax();
      }
    } catch (Exception e) {
      log4j.error("Exception while isPriceInclusiveOfTax: " + e);
    }

    return isInclusive;
  }

  @Override
  public List<InvoiceLine> getLines(String strInvoiceId) {
    List<InvoiceLine> lines = null;

    try {
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        Invoice invoice = Utility.getObject(Invoice.class, strInvoiceId);
        OBDal.getInstance().refresh(invoice);
        lines = invoice.getInvoiceLineList().stream()
            .filter(a -> !a.isEFINIsTaxLine() && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0
                && (a.isEfinCalculateTax() == null || !a.isEfinCalculateTax()))
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      log4j.error("Exception while getLines: " + e);
    }

    return lines;
  }

  @Override
  public OBError insertTaxLines(String strInvoiceId) {
    OBError result = new OBError();
    Boolean hasExpenseLines = Boolean.FALSE;

    try {
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        List<InvoiceLine> taxableLines = new ArrayList<InvoiceLine>();
        Invoice invoice = getInvoice(strInvoiceId);
        hasExpenseLines = hasExpenseLines(invoice);
        roundoffConst = invoice.getCurrency().getStandardPrecision().intValue();

        if (hasExpenseLines) {
          isPriceInclOfTax = isPriceInclusiveOfTax(strInvoiceId);
          deleteRecalculateTaxes(invoice);
          OBDal.getInstance().refresh(invoice);
          taxableLines = getLines(strInvoiceId);
          getDeductionTaxes(strInvoiceId);
          paymentBeneficiary = invoice.getEfinTaxMethod().getBusinessPartner();
          secondaryBeneficiary = invoice.getBusinessPartner();

        } else {
          result = OBErrorBuilder.buildMessage(null, "info", "@EFIN_NoExpenseCode@");
        }

        if (taxableLines.size() > 0) {
          for (InvoiceLine line : taxableLines) {
            result = insertInvoiceLines(strInvoiceId, line);
            if ("error".equals(result.getType()))
              break;
          }

          invoice.setEfinTaxAmount(totalTaxAmount.setScale(roundoffConst, RoundingMode.HALF_UP));
          invoice.setProcessNow(Boolean.TRUE);

          if (!"error".equals(result.getType())) {
            OBDal.getInstance().save(invoice);
            OBDal.getInstance().flush();
            invoice.setProcessNow(Boolean.FALSE);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();

            result = OBErrorBuilder.buildMessage(null, "success", "@EFIN_Added Taxes@");
          }
        }
      }
    } catch (OBException e) {
      result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      log4j.error("Exception while getTaxWeightage: " + e);

    } catch (Exception e) {
      result = OBErrorBuilder.buildMessage(null, "error", "@HB_INTERNAL_ERROR@");
      log4j.error("Exception while insertTaxLines: " + e);
    }

    return result;
  }

  @Override
  public OBError insertInvoiceLines(String strInvoiceId, InvoiceLine sourceLine) {
    OBError result = new OBError();
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal lineNetAmount = BigDecimal.ZERO;
    BigDecimal baseAmount = BigDecimal.ZERO;
    BigDecimal deductionAmount = BigDecimal.ZERO;

    Long lineNo = 0l;
    try {
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        JSONObject taxObject = calculateTaxAmount(strInvoiceId, sourceLine);

        taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
        lineNetAmount = new BigDecimal(taxObject.get("LineAmount").toString());
        baseAmount = new BigDecimal(taxObject.get("TaxBaseAmount").toString());
        deductionAmount = new BigDecimal(taxObject.get("DeductionAmount").toString());
        BigDecimal fundsAvailable = BigDecimal.ZERO;
        JSONObject fundsCheckingObject = null;
        Invoice invoice = sourceLine.getInvoice();

        // insert tax lines only if tax amount is not zero!!
        if (invoice != null && taxAmount.compareTo(BigDecimal.ZERO) != 0) {

          lineNo = getLineNumber(sourceLine.getLineNo());
          InvoiceLine invoiceLine = (InvoiceLine) DalUtil.copy(sourceLine);

          invoiceLine.setEFINIsTaxLine(Boolean.TRUE);
          invoiceLine.setLineNetAmount(taxAmount);
          invoiceLine.setEfinAmtinvoiced(taxAmount);
          invoiceLine.setEfinCInvoiceline(sourceLine);
          invoiceLine.setTaxAmount(BigDecimal.ZERO);
          invoiceLine.setLineNo(lineNo);
          invoiceLine.setUpdated(new Date());
          invoiceLine.setTaxableAmount(BigDecimal.ZERO);
          invoiceLine.setEfinRecalculateTax(Boolean.FALSE);
          invoiceLine.setUpdatedBy(OBContext.getOBContext().getUser());
          invoiceLine.setBusinessPartner(paymentBeneficiary);
          invoiceLine.setEfinSecondaryBeneficiary(
              sourceLine.getBusinessPartner() == null ? secondaryBeneficiary
                  : sourceLine.getBusinessPartner());
          if (invoice.getTransactionDocument().isEfinIspomatch()
              && sourceLine.getEfinAmtinvoiced().compareTo(BigDecimal.ZERO) > 0) {
            sourceLine.setLineNetAmount(lineNetAmount);
            sourceLine.setEfinAmtinvoiced(lineNetAmount);
          } else {
            sourceLine.setLineNetAmount(lineNetAmount);
          }
          sourceLine.setEfinDeductionTaxamt(deductionAmount);
          sourceLine.setTaxableAmount(baseAmount);
          sourceLine.setTaxAmount(taxAmount.add(deductionAmount.negate()));
          sourceLine.setUpdatedBy(OBContext.getOBContext().getUser());
          sourceLine.setEfinRecalculateTax(Boolean.FALSE);
          sourceLine.setEfinCalculateTax(Boolean.TRUE);

          totalTaxAmount = totalTaxAmount.add(taxAmount);

          OBDal.getInstance().save(invoiceLine);
          OBDal.getInstance().save(sourceLine);

          if (invoiceLine.getInvoice().getEfinBudgetType().equals("C")) {
            AccountingCombination accCombination = OBDal.getInstance()
                .get(AccountingCombination.class, invoiceLine.getEfinCValidcombination().getId());
            EfinBudgetIntialization budgetIntialization = Utility.getObject(
                EfinBudgetIntialization.class, invoiceLine.getInvoice().getEfinBudgetint().getId());
            fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                accCombination.getEfinFundscombination());
            fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
            if (fundsAvailable != null) {
              invoiceLine.setEfinFbFundsAvailable(fundsAvailable);
            }
          }
        }
      }
    } catch (OBException e) {
      result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      log4j.error("Exception while insertInvoiceLines: " + e);
    } catch (Exception e) {
      result = OBErrorBuilder.buildMessage(null, "error", "@HB_INTERNAL_ERROR@");
      log4j.error("Exception while insertInvoiceLines: " + e);
    }

    return result;
  }

  @Override
  public Long getLineNumber(Long lineNumber) {
    Long lineNo = 0l;
    try {
      lineNo = lineNumber + increment;
    } catch (Exception e) {
      lineNo = 10l;
      log4j.error("Exception while getLineNumber: " + e);
    }
    return lineNo;
  }

  @Override
  public JSONObject calculateTaxAmount(String strInvoiceLineId, InvoiceLine sourceLine) {
    JSONObject taxObject = new JSONObject();
    BigDecimal taxPercent = BigDecimal.ZERO;
    BigDecimal taxFactor = BigDecimal.ONE;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal totalDeduction = BigDecimal.ZERO;
    BigDecimal sourceLineAmount = BigDecimal.ZERO;
    String strInvoiceId = "";
    BigDecimal lineAmtWithDeduction = BigDecimal.ZERO;
    BigDecimal taxDeducted = BigDecimal.ZERO;
    BigDecimal taxOnDeductions = BigDecimal.ZERO;
    try {
      sourceLineAmount = sourceLine.getLineNetAmount();
      strInvoiceId = sourceLine.getInvoice().getId();

      taxPercent = getTaxPercent(strInvoiceId);
      totalDeduction = getBeneficiaryBasedTotalLineAmount(sourceLine);

      List<Object[]> deductions = getDeductionList(strInvoiceId, sourceLine);

      if (deductions != null) {
        taxOnDeductions = getTaxWeightage(deductions);
        taxDeducted = calculateDeductionAmount(sourceLine, totalDeduction, taxOnDeductions);
      }

      lineAmtWithDeduction = sourceLine.getLineNetAmount().add(taxDeducted.negate());

      if (isPriceInclOfTax) {
        taxFactor = taxFactor.add(taxPercent);
        taxAmount = calculateInclusiveTax(strInvoiceId, sourceLine, taxFactor);

        taxObject.put("LineAmount", lineAmtWithDeduction.subtract(taxAmount));
        taxObject.put("TaxBaseAmount", sourceLine.getLineNetAmount());

      } else {
        taxAmount = sourceLineAmount.multiply(taxPercent);
        // Task No:7806 in exclusive case taxbase amt is same as linenetamt , should not subtract
        // the tax amount
        taxObject.put("TaxBaseAmount", sourceLine.getLineNetAmount().add(taxDeducted.negate()));// .subtract(taxAmount)
        taxObject.put("LineAmount", sourceLine.getLineNetAmount());
      }

      taxAmount = taxAmount.add(taxDeducted);

      taxObject.put("TaxAmount", taxAmount.setScale(roundoffConst, RoundingMode.DOWN));
      taxObject.put("DeductionAmount", taxDeducted);
    } catch (Exception e) {
      try {
        taxObject.put("LineAmount", BigDecimal.ZERO);
        taxObject.put("TaxBaseAmount", BigDecimal.ZERO);
        taxObject.put("TaxAmount", BigDecimal.ZERO);
        taxObject.put("DeductionAmount", BigDecimal.ZERO);
      } catch (JSONException e1) {
      }

      log4j.error("Exception while calculateTaxAmount: " + e);
    }

    return taxObject;
  }

  public BigDecimal getTaxWeightage(List<Object[]> deductions) {
    BigDecimal totalTax = BigDecimal.ZERO;
    String deductionLineId = "";
    try {

      if (!deductionTaxWeightage.isEmpty()) {
        for (Object[] deductionObj : deductions) {
          deductionLineId = (String) deductionObj[3];
          if (StringUtils.isNotEmpty(deductionLineId))
            totalTax = totalTax.add(deductionTaxWeightage.get(deductionLineId));
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while getTaxWeightage: " + e);
    }

    return totalTax;
  }

  @SuppressWarnings("unchecked")
  public List<Object[]> getDeductionList(String strInvoiceId, InvoiceLine sourceLine) {
    StringBuilder queryBuilder = new StringBuilder();
    List<Object[]> deductionObj = new ArrayList<Object[]>();
    try {

      queryBuilder.append(
          "select c_bpartner_id, em_efin_beneficiary2_id,deduction, a.c_invoiceline_id from c_invoiceline l");
      queryBuilder.append(" left join ( select coalesce(c_bpartner_id,'') as payment ");
      queryBuilder.append(" ,coalesce(em_efin_beneficiary2_id,'') as secondary,");
      queryBuilder.append(" coalesce(linenetamt,0) as deduction, c_invoice_id ,c_invoiceline_id ");
      queryBuilder.append(" from c_invoiceline  where c_invoice_id  =  :invoiceId  ");
      queryBuilder.append(" and em_efin_istax  = 'N' and linenetamt < 0 )a ");
      queryBuilder.append(" on coalesce(l.c_bpartner_id,'') = a.payment  ");
      queryBuilder.append(" and coalesce(l.em_efin_beneficiary2_id,'') = a.secondary  ");
      queryBuilder.append(" where l.c_invoiceline_id = :invoiceLineId ");
      queryBuilder.append(" and em_efin_istax  = 'N' and linenetamt > 0 ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      query.setString("invoiceId", strInvoiceId);
      query.setString("invoiceLineId", sourceLine.getId());

      if (query != null) {
        deductionObj = query.list();

      }

    } catch (Exception e) {
      log4j.error("Exception while getDeductionList: " + e);
    }

    return deductionObj;
  }

  @Override
  public BigDecimal calculateInclusiveTax(String strInvoiceId, InvoiceLine sourceLine,
      BigDecimal taxFactor) {

    BigDecimal totalTaxAmount = BigDecimal.ZERO;
    BigDecimal baseAmount = BigDecimal.ZERO;
    BigDecimal grandTotal = BigDecimal.ZERO;

    BigDecimal lineTaxAmount = BigDecimal.ZERO;

    try {

      grandTotal = sourceLine.getInvoice().getGrandTotalAmount();
      baseAmount = grandTotal.divide(taxFactor, roundoffConst, RoundingMode.HALF_UP);

      totalTaxAmount = grandTotal.subtract(baseAmount);

      // get line tax by line weight.
      lineTaxAmount = (sourceLine.getLineNetAmount().multiply(totalTaxAmount)).divide(grandTotal,
          roundoffConst, RoundingMode.HALF_UP);

    } catch (Exception e) {
      log4j.error("Exception while calculateInclusiveTax: " + e);
    }

    return lineTaxAmount;
  }

  @SuppressWarnings("unchecked")
  @Override
  public BigDecimal getBeneficiaryBasedTotalLineAmount(InvoiceLine line) {
    BigDecimal totalAmount = BigDecimal.ZERO;
    StringBuilder queryBuilder = new StringBuilder();
    try {
      List<Object> lines = new ArrayList<Object>();

      queryBuilder.append(" select sum(linenetamt) from c_invoiceline  l ");
      queryBuilder.append(" where l.c_invoice_id = :invoiceID ");
      queryBuilder.append(" and em_efin_istax  = 'N' and linenetamt > 0 ");
      queryBuilder.append(" and coalesce(l.c_bpartner_id,'0') = :paymentBeneficiary ");
      queryBuilder.append(" and coalesce(l.em_efin_beneficiary2_id,'0') = :secondaryBeneficiary ");
      queryBuilder.append(" group by l.c_invoice_id ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      query.setString("invoiceID", line.getInvoice().getId());
      query.setString("paymentBeneficiary",
          line.getBusinessPartner() == null ? "0" : line.getBusinessPartner().getId());
      query.setString("secondaryBeneficiary", line.getEfinSecondaryBeneficiary() == null ? "0"
          : line.getEfinSecondaryBeneficiary().getId());

      if (query != null) {
        lines = query.list();
        if (lines.size() > 0) {
          totalAmount = (BigDecimal) lines.get(0);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getBeneficiaryBasedTotalLineAmount: " + e);
    }

    return totalAmount;
  }

  @Override
  public BigDecimal calculateDeductionAmount(InvoiceLine sourceLine, BigDecimal TotalDeductions,
      BigDecimal taxOnDeductions) {
    BigDecimal deduction = BigDecimal.ZERO;

    try {
      deduction = sourceLine.getLineNetAmount()
          .divide(TotalDeductions, roundoffConst, RoundingMode.HALF_UP).multiply(taxOnDeductions);
    } catch (Exception e) {
      log4j.error("Exception while calculateDeductionAmount() : " + e);
    }
    return deduction;
  }

  @Override
  public Map<String, BigDecimal> getDeductionTaxes(String strInvoiceId) {
    List<InvoiceLine> lines = new ArrayList<InvoiceLine>();
    BigDecimal taxPercent = BigDecimal.ZERO, taxFactor = BigDecimal.ONE;
    BigDecimal taxAmount = BigDecimal.ZERO;

    try {

      lines = getDeductionLines(strInvoiceId);
      taxPercent = getTaxPercent(strInvoiceId);
      taxFactor = taxFactor.add(taxPercent);

      for (InvoiceLine line : lines) {
        if (isPriceInclOfTax) {
          taxAmount = calculateInclusiveTax(strInvoiceId, line, taxFactor);
        } else {
          taxAmount = line.getLineNetAmount().multiply(taxPercent);
        }

        deductionTaxWeightage.put(line.getId(), taxAmount);
      }
    } catch (Exception e) {

      log4j.error("Exception while getDeductionTaxes() : " + e);
    }

    return deductionTaxWeightage;
  }

  @Override
  public List<InvoiceLine> getDeductionLines(String strInvoiceId) {
    List<InvoiceLine> lines = null;

    try {
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        Invoice invoice = Utility.getObject(Invoice.class, strInvoiceId);
        lines = invoice.getInvoiceLineList().stream()
            .filter(
                a -> !a.isEFINIsTaxLine() && a.getLineNetAmount().compareTo(BigDecimal.ZERO) < 0)
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      log4j.error("Exception while getDeductionLines: " + e);
    }

    return lines;
  }

  private void deleteRecalculateTaxes(Invoice invoice) {
    try {
      List<InvoiceLine> lines = null;
      InvoiceLine source = null;

      lines = invoice.getInvoiceLineList().stream().filter(a -> a.isEFINIsTaxLine())
          .collect(Collectors.toList());
      isPriceInclOfTax = isPriceInclusiveOfTax(invoice.getId());

      if (CollectionUtils.isNotEmpty(lines)) {
        for (InvoiceLine taxLine : lines) {
          source = taxLine.getEfinCInvoiceline();
          source.getInvoice().getInvoiceLineList().remove(taxLine);
          source.getInvoiceLineEMEfinCInvoicelineIDList().clear();

          /*
           * if (isPriceInclOfTax) {
           * source.setLineNetAmount(source.getLineNetAmount().add(source.getTaxAmount())
           * .add(source.getEfinDeductionTaxamt())); } else {
           * source.setLineNetAmount(source.getTaxableAmount().add(source.getTaxAmount())
           * .add(source.getEfinDeductionTaxamt())); }
           */

          OBDal.getInstance().save(source);
          OBDal.getInstance().remove(taxLine);
        }
        OBDal.getInstance().flush();
      }

    } catch (Exception e) {
      log4j.error("Exception while deleteRecalculateTaxes: " + e);
    }

  }

  @Override
  public Boolean requiresTaxRecalculation(Invoice invoice) {
    Boolean recalculate = Boolean.FALSE;
    Boolean hasExpenseLines = Boolean.FALSE;
    int size = 0;
    StringBuilder queryBuilder = new StringBuilder();
    BigDecimal invLineAmt = BigDecimal.ZERO;
    List<Object[]> linebenfi = new ArrayList<Object[]>();
    try {

      List<InvoiceLine> lines = new ArrayList<InvoiceLine>();
      List<InvoiceLine> lines1 = new ArrayList<InvoiceLine>();
      List<InvoiceLine> taxLines = new ArrayList<InvoiceLine>();

      lines = invoice
          .getInvoiceLineList().stream().filter(a -> !a.isEFINIsTaxLine()
              && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0 && !a.isEfinIsNotax())
          .collect(Collectors.toList());

      size = lines.size();

      queryBuilder.append(
          " select sum(linenetamt),l.c_bpartner_id, l.em_efin_beneficiary2_id   from c_invoiceline  l ");
      queryBuilder.append(" where l.c_invoice_id = :invoiceID ");
      queryBuilder.append(" and em_efin_istax  = 'N' ");
      queryBuilder.append(" group by l.c_bpartner_id, l.em_efin_beneficiary2_id ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      query.setString("invoiceID", invoice.getId());

      if (query != null) {
        linebenfi = query.list();
        if (linebenfi.size() > 0) {
          for (Object[] obj : linebenfi) {
            BigDecimal sumofLineAmt = (BigDecimal) obj[0];
            if (sumofLineAmt.compareTo(BigDecimal.ZERO) == 0) {
              if (obj[1] == null && obj[2] == null) {
                lines1 = invoice.getInvoiceLineList().stream()
                    .filter(a -> a.getBusinessPartner() == null
                        && a.getEfinSecondaryBeneficiary() == null
                        && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0
                        && !a.isEFINIsTaxLine())
                    .collect(Collectors.toList());
              }
              if (obj[1] != null && obj[2] == null) {
                lines1 = invoice.getInvoiceLineList().stream()
                    .filter(a -> a.getBusinessPartner().getId().equals(obj[1].toString())
                        && a.getEfinSecondaryBeneficiary() == null
                        && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0
                        && !a.isEFINIsTaxLine())
                    .collect(Collectors.toList());
              }
              if (obj[1] != null && obj[2] != null) {
                lines1 = invoice.getInvoiceLineList().stream()
                    .filter(a -> a.getBusinessPartner().getId().equals(obj[1].toString())
                        && a.getEfinSecondaryBeneficiary().getId().equals(obj[2].toString())
                        && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0
                        && !a.isEFINIsTaxLine())
                    .collect(Collectors.toList());
              }
              if (obj[1] == null && obj[2] != null) {
                lines1 = invoice.getInvoiceLineList().stream()
                    .filter(a -> a.getBusinessPartner() == null
                        && a.getEfinSecondaryBeneficiary().getId().equals(obj[2].toString())
                        && a.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0
                        && !a.isEFINIsTaxLine())
                    .collect(Collectors.toList());
              }
              size = lines.size() - lines1.size();
            }
          }
        }
      }

      taxLines = invoice.getInvoiceLineList().stream().filter(a -> a.isEFINIsTaxLine())
          .collect(Collectors.toList());

      hasExpenseLines = hasExpenseLines(invoice);

      if (taxLines.size() != size && hasExpenseLines)
        recalculate = Boolean.TRUE;

      if (!recalculate && hasExpenseLines) {
        isPriceInclOfTax = isPriceInclusiveOfTax(invoice.getId());
        for (InvoiceLine line : lines) {

          if (line.isEfinRecalculateTax()) {
            recalculate = Boolean.TRUE;
          }

          if (!recalculate) {
            if (isPriceInclOfTax) {
              // (linenetamt+taxamt+em_efin_deduction_taxamt )

              if (line.getTaxableAmount().compareTo(line.getLineNetAmount().add(line.getTaxAmount())
                  .add(line.getEfinDeductionTaxamt())) != 0) {
                recalculate = Boolean.TRUE;
              }
            } else {
              // (taxbaseamt+taxamt+em_efin_deduction_taxamt )
              // Task No:7806 in exclusive case taxbase amt is same as linenetamt , should not add
              // the tax amount for verify the linenetamt so removed .add(line.getTaxAmount())
              // linenetamt=(taxbaseamt+em_efin_deduction_taxamt )
              if (line.getLineNetAmount()
                  .compareTo(line.getTaxableAmount()
                      .add(line.getEfinDeductionTaxamt() == null ? BigDecimal.ZERO
                          : line.getEfinDeductionTaxamt())) != 0) {// .add(line.getTaxAmount())
                recalculate = Boolean.TRUE;
              } else {
                recalculate = Boolean.FALSE;
              }
            }
          }
          if (recalculate)
            break;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while requiresTaxRecalculation(): " + e);
    }

    return recalculate;
  }

  @Override
  public Boolean hasTaxLines(Invoice invoice) {
    Boolean hasTaxLines = Boolean.FALSE;

    try {
      Long taxLinesCount = 0l;
      taxLinesCount = invoice.getInvoiceLineList().stream().filter(a -> a.isEFINIsTaxLine())
          .count();
      if (taxLinesCount > 0) {
        hasTaxLines = Boolean.TRUE;
      }

    } catch (Exception e) {
      log4j.error("Exception while hasTaxLines(): " + e);
    }
    return hasTaxLines;
  }

  @Override
  public Boolean isExclusiveTaxInvoice(Invoice invoice) {

    Boolean isExclusiveTax = Boolean.FALSE;

    try {
      if (invoice != null) {
        if (invoice.isEfinIstax() && !invoice.getEfinTaxMethod().isPriceIncludesTax()) {
          isExclusiveTax = Boolean.TRUE;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while isExclusiveTaxInvoice(): " + e);
    }

    return isExclusiveTax;
  }

  @Override
  public Map<String, BigDecimal> getTaxLineCodesAndAmount(Invoice invoice) {

    Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();

    try {
      List<InvoiceLine> taxLines = new ArrayList<>();
      BigDecimal taxAmount = BigDecimal.ZERO;
      String strUniqueCodeId = "";

      taxLines = invoice.getInvoiceLineList().stream().filter(a -> a.isEFINIsTaxLine())
          .collect(Collectors.toList());

      if (CollectionUtils.isNotEmpty(taxLines)) {
        for (InvoiceLine line : taxLines) {

          strUniqueCodeId = line.getEfinCValidcombination().getId();

          if (taxLinesMap.containsKey(strUniqueCodeId)) {
            taxAmount = taxLinesMap.get(strUniqueCodeId).add(line.getLineNetAmount());
            taxLinesMap.put(strUniqueCodeId, taxAmount);

          } else {
            taxLinesMap.put(strUniqueCodeId, line.getLineNetAmount());
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getTaxLineCodesAndAmount(): " + e);
    }

    return taxLinesMap;
  }

  @Override
  public Boolean isTaxCalculated(InvoiceLine invoiceLine) {
    Boolean hasTaxLine = Boolean.FALSE;
    try {

      OBQuery<InvoiceLine> taxQuery = OBDal.getInstance().createQuery(InvoiceLine.class,
          " where efinCInvoiceline.id = :sourceLineId  and efinIssplitedLine='N' ");
      taxQuery.setNamedParameter("sourceLineId", invoiceLine.getId());

      if (taxQuery != null && taxQuery.list().size() > 0) {
        hasTaxLine = Boolean.TRUE;
      }

    } catch (Exception e) {
      log4j.error("Exception while isTaxCalculated(): " + e);
    }
    return hasTaxLine;
  }

  @Override
  public Boolean hasExpenseLines(Invoice invoice) {
    Boolean hasExpenseLine = Boolean.FALSE;
    try {
      List<InvoiceLine> expenseLines = new ArrayList<InvoiceLine>();

      expenseLines = invoice.getInvoiceLineList().stream()
          .filter(a -> !a.isEFINIsTaxLine()
              && a.getEfinCValidcombination().getEfinDimensiontype().equals(EXPENSE_ACCOUNT))
          .collect(Collectors.toList());

      if (expenseLines.size() > 0)
        hasExpenseLine = Boolean.TRUE;

    } catch (Exception e) {
      log4j.error("Exception while isTaxCalculated(): " + e);
      e.printStackTrace();
    }

    return hasExpenseLine;
  }

  @Override
  public OBError removeTaxLines(String strInvoiceId) {
    OBError result = new OBError();

    try {
      if (StringUtils.isNotEmpty(strInvoiceId)) {
        Invoice invoice = getInvoice(strInvoiceId);

        if (hasTaxLines(invoice)) {

          deleteRecalculateTaxes(invoice);
          OBDal.getInstance().refresh(invoice);
          invoice.setEfinTaxMethod(null);
          OBDal.getInstance().save(invoice);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }
        result = OBErrorBuilder.buildMessage(null, "success", "@EFIN_Removed_Taxes@");
      }
    } catch (OBException e) {
      result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      log4j.error("Exception while removeTaxLines: " + e);

    } catch (Exception e) {
      result = OBErrorBuilder.buildMessage(null, "error", "@HB_INTERNAL_ERROR@");
      log4j.error("Exception while removeTaxLines: " + e);
    }

    return result;
  }

  @Override
  public Map<String, BigDecimal> getTaxLineCodesAndAmountForRDV(Invoice invoice) {

    Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
    Map<String, BigDecimal> encumLinesMap = new HashMap<String, BigDecimal>();

    try {
      List<InvoiceLine> taxLines = new ArrayList<>();
      String strUniqueCodeId = "";
      Boolean isFullyMatched = false;
      EfinBudgetManencum encumbrance = null;
      Order order = invoice.getEfinRdvtxn().getEfinRdv().getSalesOrder();
      if (invoice.getEfinManualencumbrance() != null) {
        encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
            invoice.getEfinManualencumbrance().getId());
      }

      // If encumbrance in order is equal to encumbrance in invoice, then all the amount is matched
      // in rdv
      if (order.getEfinBudgetManencum() == encumbrance) {
        isFullyMatched = true;
      }

      taxLines = invoice.getInvoiceLineList().stream().filter(a -> a.isEFINIsTaxLine())
          .collect(Collectors.toList());

      // Constructing map with uniquecode having tax with corresponding sum amount in invoice
      if (CollectionUtils.isNotEmpty(taxLines)) {
        for (InvoiceLine line : taxLines) {
          strUniqueCodeId = line.getEfinCValidcombination().getId();
          BigDecimal sumLineNetAmt = invoice.getInvoiceLineList().stream()
              .filter(a -> a.getEfinCValidcombination().getId()
                  .equals(line.getEfinCValidcombination().getId()))
              .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (!taxLinesMap.containsKey(strUniqueCodeId)) {
            taxLinesMap.put(strUniqueCodeId, sumLineNetAmt);
          }
        }
      }

      // Constructing map with uniquecode having tax with corresponding sum amount in encumbrance
      if (encumbrance != null && CollectionUtils.isNotEmpty(taxLines)) {
        for (InvoiceLine line : taxLines) {
          strUniqueCodeId = line.getEfinCValidcombination().getId();
          BigDecimal sumLineNetAmt = encumbrance.getEfinBudgetManencumlinesList().stream()
              .filter(a -> a.getAccountingCombination().getId()
                  .equals(line.getEfinCValidcombination().getId()))
              .map(a -> a.getRevamount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (!encumLinesMap.containsKey(strUniqueCodeId)) {
            encumLinesMap.put(strUniqueCodeId, sumLineNetAmt);
            BigDecimal invoicelineAmt = taxLinesMap.get(strUniqueCodeId);
            if (!isFullyMatched) {
              taxLinesMap.put(strUniqueCodeId, invoicelineAmt.subtract(sumLineNetAmt));
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while getTaxLineCodesAndAmountForRDV(): " + e);
    }

    return taxLinesMap;
  }

}