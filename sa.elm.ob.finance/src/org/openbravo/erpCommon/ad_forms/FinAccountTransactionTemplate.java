package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_Payment_Credit;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.util.Utility;

public class FinAccountTransactionTemplate extends DocFINFinAccTransactionTemplate {
  private static final Logger log4j = Logger.getLogger(FinAccountTransactionTemplate.class);
  static String SeqNo = "0";

  @Override
  public Fact createFact(DocFINFinAccTransaction docFAT, AcctSchema as, ConnectionProvider conn,
      Connection con, VariablesSecureApp vars) throws ServletException {
    Fact fact = new Fact(docFAT, as, Fact.POST_Actual);
    try {
      FIN_FinaccTransaction transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class,
          docFAT.Record_ID);
      if (DocFINFinAccTransaction.TRXTYPE_BankFee.equals(transaction.getTransactionType()))
        fact = docFAT.createFactFee(transaction, as, conn, fact);
      else if (transaction.getFinPayment() != null) {
        fact = createFactPaymentDetails(as, conn, fact, docFAT);
      } else
        fact = docFAT.createFactGLItem(as, conn, fact);
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in template: " + e);
    }
    return fact;
  }

  @SuppressWarnings("unused")
  public Fact createFactPaymentDetails(AcctSchema as, ConnectionProvider conn, Fact fact,
      DocFINFinAccTransaction docFAT) throws ServletException, NoConnectionAvailableException {
    FIN_FinaccTransaction transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class,
        docFAT.Record_ID);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    String Fact_Acct_Group_ID2 = SequenceIdData.getUUID();
    String Fact_Acct_Group_ID3 = SequenceIdData.getUUID();
    boolean isReceipt = transaction.getFinPayment().isReceipt();
    Currency paymentCurrency = transaction.getFinPayment().getCurrency();
    String strValidCombination = "";
    boolean allowPosting = Boolean.FALSE;
    BigDecimal totalAmount = BigDecimal.ZERO;
    FinTrxPostingDAOImpl postingUtils = new FinTrxPostingDAOImpl(conn.getConnection());
    String strInvoiceId = "";
    List<FIN_Payment> payments = new ArrayList<FIN_Payment>();
    List<FIN_FinaccTransaction> transactions = new ArrayList<FIN_FinaccTransaction>();

    strInvoiceId = postingUtils.getInvoice(docFAT.Record_ID);
    if (StringUtils.isNotEmpty(strInvoiceId)) {

      payments = postingUtils.getListOfPayments(strInvoiceId);
      transactions = postingUtils.getListOfFinancialAccountTrx(strInvoiceId);

      if (payments != null && transactions != null && (payments.size() == transactions.size())) {
        allowPosting = postingUtils.allowPosting(docFAT.Record_ID);
      }
    }

    if (allowPosting) {
      if (!docFAT.getDocumentPaymentConfirmation(transaction.getFinPayment())) {
        for (int i = 0; docFAT.p_lines != null && i < docFAT.p_lines.length; i++) {
          DocLine_FINFinAccTransaction line = (DocLine_FINFinAccTransaction) docFAT.p_lines[i];
          boolean isPrepayment = "Y".equals(line.getIsPrepayment());
          String bpartnerId = (line.m_C_BPartner_ID == null || line.m_C_BPartner_ID.equals(""))
              ? docFAT.C_BPartner_ID
              : line.m_C_BPartner_ID;
          BigDecimal bpamount = new BigDecimal(line.getAmount());
          if (!"".equals(line.getWriteOffAmt())
              && BigDecimal.ZERO.compareTo(new BigDecimal(line.getWriteOffAmt())) != 0) {
            Account account = isReceipt
                ? docFAT.getAccountWriteOffBPartner(AcctServer.ACCTTYPE_WriteOff,
                    line.m_C_BPartner_ID, as, conn)
                : docFAT.getAccountWriteOffBPartner(AcctServer.ACCTTYPE_WriteOff_Revenue,
                    line.m_C_BPartner_ID, as, conn);
            if (account == null) {
              account = isReceipt ? docFAT.getAccount(AcctServer.ACCTTYPE_WriteOffDefault, as, conn)
                  : docFAT.getAccount(AcctServer.ACCTTYPE_WriteOffDefault_Revenue, as, conn);
            }
            // Write off amount is generated at payment time so conversion is calculated taking into
            // account conversion at payment date to calculate gains or losses
            BigDecimal writeOffAmt = docFAT.convertAmount(new BigDecimal(line.getWriteOffAmt()),
                isReceipt, docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Transaction,
                transaction.getId(), paymentCurrency.getId(), as.m_C_Currency_ID, line, as, fact,
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);
            fact.createLine(line, account, paymentCurrency.getId(),
                (isReceipt ? writeOffAmt.toString() : ""),
                (isReceipt ? "" : writeOffAmt.toString()), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                docFAT.DocumentType, conn);
            bpamount = bpamount.add(new BigDecimal(line.getWriteOffAmt()));
          }
          if (!"".equals(line.cGlItemId)) {
            // FIXME Should this be posted taking into account payment date?? Diferences among
            // currencies
            fact.createLine(line,
                docFAT.getAccountGLItem(OBDal.getInstance().get(GLItem.class, line.getCGlItemId()),
                    as, isReceipt, conn),
                paymentCurrency.getId(), isReceipt ? "" : line.getAmount(),
                isReceipt ? line.getAmount() : "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
                docFAT.DocumentType, conn);
          } else {
            BigDecimal bpAmountConverted = bpamount, invLineAmountConverted = null;
            Invoice invoice = line.getInvoice();
            if (!isPrepayment && invoice != null) {
              // To force opposite posting isReceipt is opposite as well. this is required when
              // looking backwards
              bpAmountConverted = docFAT.convertAmount(bpAmountConverted, !isReceipt,
                  docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Invoice, invoice.getId(),
                  paymentCurrency.getId(), as.m_C_Currency_ID, line, as, fact, Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), conn);
              // Cash VAT
              SeqNo = CashVATUtil.createFactCashVAT(as, conn, fact, Fact_Acct_Group_ID, line,
                  invoice, docFAT.DocumentType, SeqNo);
            }
            if (isPrepayment) {
              // To force opposite posting isReceipt is opposite as well. this is required when
              // looking backwards. When prepayments date for event is always date for PAYMENT
              bpAmountConverted = docFAT.convertAmount(bpAmountConverted, !isReceipt,
                  docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Payment,
                  transaction.getFinPayment().getId(), paymentCurrency.getId(), as.m_C_Currency_ID,
                  line, as, fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);
            }

            if (line.getDoubtFulDebtAmount().signum() != 0) {
              /*
               * BigDecimal doubtFulDebtAmount = docFAT.convertAmount(line.getDoubtFulDebtAmount(),
               * isReceipt, docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Invoice,
               * invoice.getId(), docFAT.C_Currency_ID, as.m_C_Currency_ID, line, as, fact,
               * Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn, false); fact.createLine(line,
               * docFAT.getAccountBPartner(bpartnerId, as, true, false, true, conn),
               * docFAT.C_Currency_ID, "", doubtFulDebtAmount.toString(), Fact_Acct_Group_ID,
               * nextSeqNo(SeqNo), docFAT.DocumentType, conn); bpAmountConverted =
               * bpAmountConverted.subtract(doubtFulDebtAmount); fact.createLine(line,
               * docFAT.getAccountBPartnerAllowanceForDoubtfulDebt(bpartnerId, as, conn),
               * docFAT.C_Currency_ID, doubtFulDebtAmount.toString(), "", Fact_Acct_Group_ID2,
               * nextSeqNo(SeqNo), docFAT.DocumentType, conn);
               * 
               * // Assign expense to the dimensions of the invoice lines BigDecimal assignedAmount
               * = BigDecimal.ZERO; DocDoubtfulDebtData[] data = DocDoubtfulDebtData.select(conn,
               * invoice.getId()); Currency currency = OBDal.getInstance().get(Currency.class,
               * docFAT.C_Currency_ID); for (int j = 0; j < data.length; j++) { BigDecimal
               * lineAmount = doubtFulDebtAmount .multiply(new BigDecimal(data[j].percentage))
               * .setScale(currency.getStandardPrecision().intValue(), BigDecimal.ROUND_HALF_UP); if
               * (j == data.length - 1) { lineAmount = doubtFulDebtAmount.subtract(assignedAmount);
               * } DocLine lineDD = new DocLine(docFAT.DocumentType, docFAT.Record_ID, "");
               * lineDD.m_A_Asset_ID = data[j].aAssetId; lineDD.m_M_Product_ID = data[j].mProductId;
               * lineDD.m_C_Project_ID = data[j].cProjectId; lineDD.m_C_BPartner_ID =
               * data[j].cBpartnerId; lineDD.m_C_Costcenter_ID = data[j].cCostcenterId;
               * lineDD.m_C_Campaign_ID = data[j].cCampaignId; lineDD.m_C_Activity_ID =
               * data[j].cActivityId; lineDD.m_C_Glitem_ID = data[j].mCGlitemId; lineDD.m_User1_ID =
               * data[j].user1id; lineDD.m_User2_ID = data[j].user2id; lineDD.m_AD_Org_ID =
               * data[j].adOrgId; fact.createLine(lineDD, docFAT.getAccountBPartnerBadDebt(
               * (lineDD.m_C_BPartner_ID == null || lineDD.m_C_BPartner_ID.equals("")) ? bpartnerId
               * : lineDD.m_C_BPartner_ID, false, as, conn), docFAT.C_Currency_ID, "",
               * lineAmount.toString(), Fact_Acct_Group_ID2, nextSeqNo(SeqNo), docFAT.DocumentType,
               * conn); assignedAmount = assignedAmount.add(lineAmount); }
               */}

            if (invoice != null) {
              String paymentId = "";
              Connection con = conn.getConnection();
              String sql = "";
              PreparedStatement st = null;
              ResultSet rs = null;

              sql = "select trx.fin_payment_id from fin_finacc_transaction trx"
                  + " join fin_payment pay on pay.fin_payment_id =trx.fin_payment_id"
                  + " join FIN_Payment_Detail_V pdv on pdv.fin_payment_id= pay.fin_payment_id"
                  + " join fin_payment_sched_inv_v psiv on psiv.fin_payment_sched_inv_v_id=pdv.fin_payment_sched_inv_v_id"
                  + " join c_invoice inv on inv.c_invoice_id=psiv.c_invoice_id"
                  + " where inv.c_invoice_id =?" + " order by trx.created desc limit 1 ";

              try {
                st = con.prepareStatement(sql);
                st.setString(1, invoice.getId());
                rs = st.executeQuery();

                if (rs.next()) {
                  paymentId = rs.getString("fin_payment_id");
                }
              } catch (SQLException e) {
                e.printStackTrace();
              }

              List<EfinBudgetInquiry> budgetLinesList = new ArrayList<EfinBudgetInquiry>();

              List<String> invoiceLineIds = new ArrayList<String>();
              Boolean isCostEncumbrance = Boolean.FALSE;
              EfinBudgetManencum manencum = null;
              AccountingCombination fundsCombination = null;

              isCostEncumbrance = PurchaseInvoiceSubmitUtils.isCostEncumbrance(invoice);
              EfinBudgetInquiry budgetInquiry = null;
              AccountingCombination combination = null;
              EfinBudgetIntialization intialization = invoice.getEfinBudgetint();
              EfinBudgetManencum encumbrance = null;
              String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
              Currency currency = FinanceUtils.getCurrency(invoice.getOrganization().getId(),
                  invoice);
              BigDecimal conversionRate = FinanceUtils.getConversionRate(conn.getConnection(),
                  invoice.getOrganization().getId(), invoice, currency);
              HashMap<String, BigDecimal> lineMap = PurchaseInvoiceSubmitUtils
                  .getExpenseLines(invoice, conversionRate, strInvoiceType, isCostEncumbrance);
              EfinBudgetManencumlines encumbranceLine = null;
              BigDecimal uniqueCodeTotal = BigDecimal.ZERO;

              for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
                strValidCombination = invoiceLine.getEfinCValidcombination().getId();

                /*
                 * if ("PPI".equals(strInvoiceType)) { strValidCombination =
                 * invoiceLine.getEfinExpenseAccount().getId(); }
                 */

                AccountingCombination lineAccount = Utility.getObject(AccountingCombination.class,
                    strValidCombination);

                if (invoiceLine.getEfinDistributionLines() == null) {

                  budgetInquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                      invoiceLine.getEfinCValidcombination(), invoice.getEfinBudgetint());
                  combination = invoiceLine.getEfinCValidcombination();
                  if (invoice.getEfinManualencumbrance() != null) {
                    encumbrance = Utility.getObject(EfinBudgetManencum.class,
                        invoice.getEfinManualencumbrance().getId());
                  }

                  if (isCostEncumbrance && "E".equals(lineAccount.getEfinDimensiontype())) {
                    combination = combination.getEfinFundscombination();
                    budgetInquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(combination,
                        intialization);
                    encumbrance = invoice.getEfinFundsEncumbrance();
                    manencum = invoice.getEfinFundsEncumbrance();
                    fundsCombination = combination;

                    if (fundsCombination != null)
                      strValidCombination = fundsCombination.getId();
                  }
                }

                Account account = new Account(conn, strValidCombination);
                invLineAmountConverted = docFAT.convertAmount(invoiceLine.getLineNetAmount(),
                    !isReceipt, docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Invoice,
                    invoice.getId(), invoice.getCurrency().getId(), as.m_C_Currency_ID, line, as,
                    fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);

                /*
                 * log4j.info(" m_AmtAcctDr: "+line.m_AmtAcctDr);
                 * log4j.info(" Acct Balance: "+fact.getAcctBalance());
                 * log4j.info(" m_AmtSourceCr: "+line.m_AmtSourceCr);
                 * log4j.info(" m_AmtSourceDr: "+line.m_AmtSourceDr);
                 * log4j.info(" amount : "+line.getAmount());
                 */
                line.m_Record_Id2 = invoiceLine.getId();
                line.m_TrxLine_ID = invoiceLine.getId();

                fact.createLine(line, account, paymentCurrency.getId(),
                    (isReceipt ? "" : invLineAmountConverted.toString()),
                    (isReceipt ? invLineAmountConverted.toString() : ""), Fact_Acct_Group_ID,
                    nextSeqNo(SeqNo), docFAT.DocumentType, conn);

                totalAmount = fact.getAcctBalance();
                Boolean isDuplicate = Boolean.FALSE;

                if (invoiceLineIds.contains(invoiceLine.getEfinCElementvalue().getId())) {
                  isDuplicate = Boolean.TRUE;
                } else {
                  invoiceLineIds.add(invoiceLine.getEfinCElementvalue().getId());
                  isDuplicate = Boolean.FALSE;
                }

                /*
                 * cost encumbrance -> cost budget encumbrance changes. funds budget Posting.
                 */

                if (lineAccount != null && "E".equals(lineAccount.getEfinDimensiontype())) {
                  uniqueCodeTotal = lineMap.get(strValidCombination);
                  if (uniqueCodeTotal.compareTo(BigDecimal.ZERO) > 0) {

                    invLineAmountConverted = FinanceUtils
                        .getConvertedAmount(invoiceLine.getLineNetAmount(), conversionRate);

                    /*
                     * budgetInquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(combination,
                     * invoice.getEfinBudgetint());
                     */

                    if (invoiceLine.getEfinBudgmanuencumln() != null) {

                      PostUtilsDAO.moveReservedToActual(invoiceLine, encumbrance,
                          transaction.getFinPayment(), combination);

                      PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(combination,
                          invLineAmountConverted, intialization, Boolean.TRUE, Boolean.FALSE);

                      PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(combination,
                          invLineAmountConverted, intialization, Boolean.TRUE, Boolean.FALSE);
                    } else if (budgetInquiry != null
                        && invoiceLine.getEfinDistributionLines() == null) {

                      if ((!"RDV".equals(strInvoiceType) && !"POM".equals(strInvoiceType))
                          || "F".equals(invoice.getEfinBudgetType())) {
                        EfinBudgetManencum invoiceEncumbrance = Utility.getObject(
                            EfinBudgetManencum.class, invoice.getEfinManualencumbrance().getId());
                        PostUtilsDAO.moveReservedToActual(invoiceLine, invoiceEncumbrance,
                            transaction.getFinPayment(), combination);

                        PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(combination,
                            invLineAmountConverted, invoice.getEfinBudgetint(), Boolean.TRUE,
                            Boolean.FALSE);

                        PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(combination,
                            invLineAmountConverted, invoice.getEfinBudgetint(), Boolean.TRUE,
                            Boolean.FALSE);
                      } else {

                        manencum = manencum == null ? encumbrance : manencum;
                        if (manencum != null) {
                          String uniqueCode = PostUtilsDAO.getFundsUniquecode(budgetInquiry,
                              manencum);

                          PostUtilsDAO.moveReservedToActual(invoiceLine, manencum,
                              transaction.getFinPayment(), combination);

                          PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
                              invoiceLine.getEfinCValidcombination().getEfinFundscombination(),
                              invLineAmountConverted, invoice.getEfinBudgetint(), Boolean.TRUE,
                              Boolean.FALSE);

                          PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(
                              invoiceLine.getEfinCValidcombination().getEfinFundscombination(),
                              invLineAmountConverted, invoice.getEfinBudgetint(), Boolean.TRUE,
                              Boolean.FALSE);

                        }
                      }
                    }
                  } else {
                    budgetInquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                        invoiceLine.getEfinCValidcombination(), invoice.getEfinBudgetint());
                    PostUtilsDAO.insertActuals(invoiceLine, invoice.getEfinBudgetint(),
                        invLineAmountConverted);
                  }
                }
              }

              if (invoice.isPaymentComplete()) {
                invoice.setPosted("Y");
                OBDal.getInstance().save(invoice);
              }

              /*
               * More than one payment, then update posted flag for all the previous payments too.
               */
              PostUtilsDAO.updatePaymentPostStatus(invoice, transaction);
              OBDal.getInstance().flush();
            } else {
              fact.createLine(line,
                  docFAT.getAccountBPartner(bpartnerId, as, isReceipt,
                      (isPrepayment || line.isPrepaymentAgainstInvoice) ? true : false, conn),
                  paymentCurrency.getId(), (isReceipt ? "" : bpAmountConverted.toString()),
                  (isReceipt ? bpAmountConverted.toString() : ""), Fact_Acct_Group_ID,
                  nextSeqNo(SeqNo), docFAT.DocumentType, conn);
            }
            // If payment date is prior to invoice date book invoice as a pre-payment not as a
            // regular
            // Receivable/Payable
            if (line.isPrepaymentAgainstInvoice()) {
              /*
               * DocLine line2 = new DocLine(docFAT.DocumentType, docFAT.Record_ID,
               * line.m_TrxLine_ID); line2.copyInfo(line); line2.m_DateAcct =
               * OBDateUtils.formatDate(invoice.getAccountingDate()); // checking if the prepayment
               * account and ReceivablesNo account in the Business // Partner // is the same.In this
               * case we do not need to create more accounting lines if
               * (!docFAT.getAccountBPartner(bpartnerId, as, isReceipt, true, conn).Account_ID
               * .equals(docFAT.getAccountBPartner(bpartnerId, as, isReceipt, false,
               * conn).Account_ID)) { fact.createLine(line2, docFAT.getAccountBPartner(bpartnerId,
               * as, isReceipt, false, conn), paymentCurrency.getId(), (isReceipt ? "" :
               * bpAmountConverted.toString()), (isReceipt ? bpAmountConverted.toString() : ""),
               * Fact_Acct_Group_ID3, nextSeqNo(SeqNo), docFAT.DocumentType, conn);
               * fact.createLine(line2, docFAT.getAccountBPartner(bpartnerId, as, isReceipt, true,
               * conn), paymentCurrency.getId(), (!isReceipt ? "" : bpAmountConverted.toString()),
               * (!isReceipt ? bpAmountConverted.toString() : ""), Fact_Acct_Group_ID3,
               * nextSeqNo(SeqNo), docFAT.DocumentType, conn); }
               */}
          }

          if (i % 100 == 0) {
            OBDal.getInstance().getSession().clear();
          }
        }
        // Pre-payment is consumed when Used Credit Amount not equals Zero. When consuming Credit no
        // credit is generated
        if (transaction.getFinPayment().getUsedCredit().compareTo(BigDecimal.ZERO) != 0
            && transaction.getFinPayment().getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0) {
          List<FIN_Payment_Credit> creditPayments = transaction.getFinPayment()
              .getFINPaymentCreditList();
          BigDecimal amtDiff = BigDecimal.ZERO;
          for (FIN_Payment_Credit creditPayment : creditPayments) {
            boolean isReceiptPayment = creditPayment.getCreditPaymentUsed().isReceipt();
            String creditAmountConverted = docFAT.convertAmount(creditPayment.getAmount(),
                isReceiptPayment, docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Payment,
                creditPayment.getCreditPaymentUsed().getId(),
                creditPayment.getCreditPaymentUsed().getCurrency().getId(), as.m_C_Currency_ID,
                null, as, fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn, false).toString();
            fact.createLine(null,
                docFAT.getAccountBPartner(docFAT.C_BPartner_ID, as, isReceiptPayment, true, conn),
                creditPayment.getCreditPaymentUsed().getCurrency().getId(),
                (isReceiptPayment ? creditAmountConverted : ""),
                (isReceiptPayment ? "" : creditAmountConverted), Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), docFAT.DocumentType, conn);
            amtDiff = amtDiff.add(creditPayment.getAmount())
                .subtract(new BigDecimal(creditAmountConverted));
          }
          if (!transaction.getFinPayment().isReceipt() && amtDiff.compareTo(BigDecimal.ZERO) == 1
              || transaction.getFinPayment().isReceipt()
                  && amtDiff.compareTo(BigDecimal.ZERO) == -1) {
            fact.createLine(null,
                docFAT.getAccount(AcctServer.ACCTTYPE_ConvertGainDefaultAmt, as, conn),
                transaction.getCurrency().getId(), "", amtDiff.abs().toString(), Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), docFAT.DocumentType, conn);
          } else {
            fact.createLine(null,
                docFAT.getAccount(AcctServer.ACCTTYPE_ConvertChargeDefaultAmt, as, conn),
                transaction.getCurrency().getId(), amtDiff.abs().toString(), "", Fact_Acct_Group_ID,
                nextSeqNo(SeqNo), docFAT.DocumentType, conn);
          }
          if (creditPayments.isEmpty()) {
            fact.createLine(null,
                docFAT.getAccountBPartner(
                    docFAT.C_BPartner_ID, as, transaction.getFinPayment().isReceipt(), true, conn),
                paymentCurrency.getId(),
                (transaction.getFinPayment().isReceipt()
                    ? transaction.getFinPayment().getUsedCredit().toString()
                    : ""),
                (transaction.getFinPayment().isReceipt() ? ""
                    : transaction.getFinPayment().getUsedCredit().toString()),
                Fact_Acct_Group_ID, nextSeqNo(SeqNo), docFAT.DocumentType, conn);
          }
        }
      } else {
        BigDecimal convertedAmount = docFAT.convertAmount(transaction.getFinPayment().getAmount(),
            !isReceipt, docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Payment,
            transaction.getFinPayment().getId(), paymentCurrency.getId(), as.m_C_Currency_ID, null,
            as, fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);
        DocLine_FINFinAccTransaction line = new DocLine_FINFinAccTransaction(docFAT.DocumentType,
            transaction.getId(), "");
        line.m_description = transaction.getFinPayment().getDescription();
        line.m_Record_Id2 = transaction.getFinPayment().getId();
        fact.createLine(line,
            docFAT.getAccountPayment(conn, transaction.getFinPayment().getPaymentMethod(),
                transaction.getFinPayment().getAccount(), as,
                transaction.getFinPayment().isReceipt()),
            paymentCurrency.getId(), !isReceipt ? convertedAmount.toString() : "",
            isReceipt ? convertedAmount.toString() : "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            docFAT.DocumentType, conn);
      }
      DocLine_FINFinAccTransaction line = new DocLine_FINFinAccTransaction(docFAT.DocumentType,
          transaction.getId(), "");
      line.m_description = transaction.getFinPayment().getDescription();
      line.m_Record_Id2 = transaction.getId();
      DocLine_FINFinAccTransaction trxLine = (DocLine_FINFinAccTransaction) docFAT.p_lines[0];
      Invoice invoice = trxLine.getInvoice();

      BigDecimal convertedAmount = BigDecimal.ZERO;
      if (invoice == null) {
        convertedAmount = docFAT.convertAmount(transaction.getFinPayment().getAmount(), !isReceipt,
            docFAT.DateAcct, DocFINFinAccTransaction.TABLEID_Payment,
            transaction.getFinPayment().getId(), paymentCurrency.getId(), as.m_C_Currency_ID, null,
            as, fact, Fact_Acct_Group_ID, nextSeqNo(SeqNo), conn);
      } else {
        convertedAmount = totalAmount;
      }

      /*
       * log4j.info("As Currency : "+as.m_C_Currency_ID);
       * log4j.info("convertedAmount: "+convertedAmount);
       * log4j.info("docFAT.exceptionPosting: "+docFAT.exceptionPosting);
       * log4j.info("Invoice Amount: "+invoice.getGrandTotalAmount());
       * log4j.info("Payment Currecy: "+paymentCurrency.getId());
       * log4j.info("Total Amount: "+totalAmount);
       */

      if (docFAT.exceptionPosting) {
        // The Payment FinAcct and Transaction FinAcct are different. To post the transaction
        // the amount of the payment need to be moved from destiny account of the payment of
        // FinAcct1
        // to destiny of the payment of the FinAcct2
        fact.createLine(line,
            docFAT.getAccountPayment(conn, transaction.getFinPayment().getPaymentMethod(),
                transaction.getAccount(), as, transaction.getFinPayment().isReceipt()),
            docFAT.C_Currency_ID, transaction.getDepositAmount().toString(),
            transaction.getPaymentAmount().toString(), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
            docFAT.DocumentType, conn);
      } else {
        fact.createLine(line,
            docFAT.getAccountUponDepositWithdrawal(conn,
                transaction.getFinPayment().getPaymentMethod(), transaction.getAccount(), as,
                transaction.getFinPayment().isReceipt()),
            docFAT.C_Currency_ID, transaction.getDepositAmount().toString(),
            convertedAmount.toString(), Fact_Acct_Group_ID, nextSeqNo(SeqNo), docFAT.DocumentType,
            conn);
      }
    } else {
      docFAT.setStatus("EFIN_PP");
      docFAT.strMessage = OBMessageUtils.messageBD("EFIN_PartPayment");
      docFAT.setMessageResult(conn, "EFIN_PP", "error", null);
    }
    SeqNo = "0";
    return fact;
  }

  public String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }
}