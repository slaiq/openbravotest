package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.AppliedPrepaymentInvoice;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.util.Utility;

public class InvoiceAccountingTemplate extends DocInvoiceTemplate {
  Logger log4jDocInvoice = Logger.getLogger(InvoiceAccountingTemplate.class);
  static String SeqNo = "0";

  @Override
  public Fact createFact(DocInvoice docInvoice, AcctSchema as, ConnectionProvider conn,
      Connection con, VariablesSecureApp vars) throws ServletException {
    Fact fact = new Fact(docInvoice, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    Invoice invoice = OBDal.getInstance().get(Invoice.class, docInvoice.Record_ID);
    String amountConverted = "";
    String strValidCombination = "";
    BigDecimal totalAppliedAmount = BigDecimal.ZERO;
    PreparedStatement ps = null, selectQuery = null, insertQuery = null;

    try {
      OBContext.setAdminMode();
      if (invoice != null && invoice.getDocumentType().isEfinIsprepayinvapp()) {
        List<InvoiceLine> lines = invoice.getInvoiceLineList(), prepaymentLines = null;
        List<AppliedPrepaymentInvoice> prepaymentInvoice = invoice.getEfinAppliedPrepaymentList();
        totalAppliedAmount = invoice.getGrandTotalAmount();

        // CR Prepayment Accounts
        if (prepaymentInvoice.size() > 0) {
          for (AppliedPrepaymentInvoice prepayment : prepaymentInvoice) {
            BigDecimal lineAmount = BigDecimal.ZERO;
            BigDecimal weightage = BigDecimal.ZERO, appliedAmount = BigDecimal.ZERO;
            BigDecimal tempAmount = BigDecimal.ZERO, difference = BigDecimal.ZERO;
            int i = 1;
            prepaymentLines = new ArrayList<InvoiceLine>();
            prepaymentLines = prepayment.getEfinAppliedInvoice().getInvoiceLineList();

            appliedAmount = prepayment.getAppliedAmount();
            weightage = appliedAmount.divide(
                prepayment.getEfinAppliedInvoice().getGrandTotalAmount(), 4, RoundingMode.FLOOR);

            if (prepaymentLines.size() > 0) {

              for (InvoiceLine invoiceLine : prepaymentLines) {
                lineAmount = invoiceLine.getLineNetAmount().multiply(weightage);
                tempAmount = tempAmount.add(lineAmount);

                if (i == prepaymentLines.size()) {
                  difference = appliedAmount.subtract(tempAmount);
                  lineAmount = lineAmount.add(difference);
                }

                strValidCombination = invoiceLine.getEfinCValidcombination().getId();
                Account account = new Account(conn, strValidCombination);
                fact.createLine(null, account, docInvoice.C_Currency_ID, "",
                    AcctServer.getConvertedAmt(lineAmount.toString(),
                        invoiceLine.getInvoice().getCurrency().getId(), docInvoice.C_Currency_ID,
                        docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID,
                        conn),
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
                i = i + 1;
              }

            }
          }
        }

        // DR
        if (lines.size() > 0) {
          int i = 0;
          HashMap<String, BigDecimal> lineMap = new HashMap<String, BigDecimal>();
          EfinBudgetManencumlines encumbranceLine = null;
          BigDecimal conversionRate = BigDecimal.ONE;
          BigDecimal covertedAmount = BigDecimal.ZERO;

          for (InvoiceLine invoiceLine : lines) {
            ConversionRateDoc conversionRateCurrentDoc = docInvoice.getConversionRateDoc(
                AcctServer.TABLEID_Invoice, docInvoice.Record_ID, docInvoice.C_Currency_ID,
                as.m_C_Currency_ID);
            if (conversionRateCurrentDoc != null) {
              amountConverted = AcctServer
                  .applyRate(invoiceLine.getLineNetAmount(), conversionRateCurrentDoc, true)
                  .setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            } else {
              amountConverted = AcctServer.getConvertedAmt(
                  invoiceLine.getLineNetAmount().toString(), docInvoice.C_Currency_ID,
                  as.m_C_Currency_ID, docInvoice.DateAcct, "", docInvoice.AD_Client_ID,
                  docInvoice.AD_Org_ID, conn);
            }
            strValidCombination = invoiceLine.getEfinCValidcombination().getId();
            Account account = new Account(conn, strValidCombination);
            fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, amountConverted, "",
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
            i++;
            if (con.getAutoCommit())
              con.setAutoCommit(false);

            Currency currency = FinanceUtils.getCurrency(invoice.getOrganization().getId(),
                invoice);
            conversionRate = FinanceUtils.getConversionRate(OBDal.getInstance().getConnection(),
                invoice.getOrganization().getId(), invoice, currency);
            covertedAmount = FinanceUtils.getConvertedAmount(invoiceLine.getLineNetAmount(),
                conversionRate);

            if (lineMap.containsKey(strValidCombination)) {
              lineMap.put(strValidCombination,
                  lineMap.get(strValidCombination).add(covertedAmount));
            } else {
              lineMap.put(strValidCombination, covertedAmount);
            }

            if (invoiceLine.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {

              /*
               * for (Object encumbranceObject : encumbranceObjects) {
               * 
               * Object[] details = (Object[]) encumbranceObject; EfinBudgetManencumlines
               * encumbranceLine = Utility .getObject(EfinBudgetManencumlines.class,
               * details[1].toString()); EfinBudgetInquiry inquiry =
               * PurchaseInvoiceSubmitUtils.getBudgetInquiry(
               * encumbranceLine.getAccountingCombination(), invoice.getEfinBudgetint());
               * 
               * if (inquiry != null && encumbranceLine.getAccountingCombination().getId()
               * .equals(invoiceLine.getEfinCValidcombination().getId())) { inquiry.setEncumbrance(
               * inquiry.getEncumbrance().subtract(new BigDecimal(amountConverted)));
               * inquiry.setSpentAmt(inquiry.getSpentAmt().add(new BigDecimal(amountConverted)));
               * 
               * OBDal.getInstance().save(inquiry);
               * 
               * PostUtilsDAO.markInvoiceReferencesAsPaid(encumbranceLine, invoice); } }
               * OBDal.getInstance().flush();
               */

            }
          }

          if (!lineMap.isEmpty()) {
            EfinBudgetInquiry pareInquiry = null;
            for (Map.Entry<String, BigDecimal> entry : lineMap.entrySet()) {
              strValidCombination = entry.getKey();
              covertedAmount = lineMap.get(strValidCombination);
              AccountingCombination combination = Utility.getObject(AccountingCombination.class,
                  strValidCombination);
              encumbranceLine = PurchaseInvoiceSubmitUtils.getEncumbranceLine(
                  invoice.getEfinManualencumbrance().getId(), strValidCombination);

              if (!combination.isEFINDepartmentFund()) {
                // update department funds='N' and other than 990, 999 dept
                EfinBudgetInquiry buddInq = ManualEncumbaranceSubmitDAO
                    .getBudgetInquiry(combination.getId(), invoice.getEfinBudgetint().getId());

                if (buddInq != null) {
                  buddInq.setEncumbrance(buddInq.getEncumbrance().subtract(covertedAmount));
                  buddInq.setSpentAmt(buddInq.getSpentAmt().add(covertedAmount));
                  OBDal.getInstance().save(buddInq);
                }

                EfinBudgetInquiry inquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(combination,
                    invoice.getEfinBudgetint());

                if (inquiry != null) {
                  inquiry.setEncumbrance(inquiry.getEncumbrance().subtract(covertedAmount));
                  inquiry.setSpentAmt(inquiry.getSpentAmt().add(covertedAmount));
                  OBDal.getInstance().save(inquiry);
                }
                /*
                 * pareInquiry = inquiry.getParent(); if (pareInquiry != null) {
                 * pareInquiry.setEncumbrance(pareInquiry.getEncumbrance().subtract(covertedAmount))
                 * ; OBDal.getInstance().save(pareInquiry); }
                 */

              } else {
                EfinBudgetInquiry inquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(combination,
                    invoice.getEfinBudgetint());

                if (inquiry != null) {
                  inquiry.setEncumbrance(inquiry.getEncumbrance().subtract(covertedAmount));
                  inquiry.setSpentAmt(inquiry.getSpentAmt().add(covertedAmount));
                  OBDal.getInstance().save(inquiry);
                }
              }
              PostUtilsDAO.markInvoiceReferencesAsPaid(encumbranceLine, invoice, covertedAmount);
            }

            OBDal.getInstance().flush();
          }
        }
      }
    } catch (SQLException sql) {
      sql.printStackTrace();
      log4jDocInvoice.error(" Exception while create fact: ", sql);
    } catch (Exception e) {
      e.printStackTrace();
      log4jDocInvoice.error(" Exception while create fact: ", e);
    } finally {
      try {
        if (ps != null)
          ps.close();
        if (insertQuery != null)
          insertQuery.close();
        if (selectQuery != null)
          selectQuery.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    return fact;
  }

  public String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }
}