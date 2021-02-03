package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

import sa.elm.ob.finance.ActualTransaction;
import sa.elm.ob.finance.AppliedPrepaymentInvoice;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.util.Utility;

public class PostUtilsDAO {
  private static final Logger log = Logger.getLogger(PostUtilsDAO.class);

  public static EfinManualEncumInvoice getReservedInvoice(InvoiceLine invoiceLine) {
    EfinManualEncumInvoice encumInvoice = null;

    try {
      OBQuery<EfinManualEncumInvoice> invoiceQuery = OBDal.getInstance().createQuery(
          EfinManualEncumInvoice.class,
          " where invoiceLine.id= :invoiceLineId and manualEncumbranceLines.id = :encumbranceLinesId");

      invoiceQuery.setNamedParameter("invoiceLineId", invoiceLine.getId());
      invoiceQuery.setNamedParameter("encumbranceLinesId", invoiceLine.getEfinBudgmanuencumln());

      if (invoiceQuery.list().size() > 0) {
        encumInvoice = invoiceQuery.list().get(0);
      }

    } catch (Exception e) {
      log.error("Exception while getBudgetInitialiation: " + e);
      e.printStackTrace();
    }
    return encumInvoice;
  }

  public static EfinBudgetInquiry getBudgetEnquiryLine(InvoiceLine invoiceLine) {
    EfinBudgetInquiry inquiry = null;

    try {
      String strValidCombinationId = "";

      strValidCombinationId = invoiceLine.getEfinCValidcombination().getId();

      OBQuery<EfinBudgetInquiry> inquiryQuery = OBDal.getInstance().createQuery(
          EfinBudgetInquiry.class,
          " where accountingCombination.id = :combinationId and efinBudgetint.id = :initializationId ");

      inquiryQuery.setNamedParameter("combinationId", strValidCombinationId);
      inquiryQuery.setNamedParameter("initializationId",
          invoiceLine.getInvoice().getEfinBudgetint().getId());

      if (inquiryQuery != null && inquiryQuery.list().size() > 0) {
        inquiry = inquiryQuery.list().get(0);
      }

    } catch (Exception e) {
      log.error("Exception while getBudgetEnquiryLine: " + e);
      e.printStackTrace();
    }
    return inquiry;
  }

  public static EfinBudgetManencum getFundsEncumbrance(EfinBudgetManencumv efinManualencumbrance) {
    EfinBudgetManencum manencum = null;
    try {
      OBQuery<EfinBudgetManencum> manualEncum = OBDal.getInstance()
          .createQuery(EfinBudgetManencum.class, " where costEncumbrance.id = :costEncumbranceId ");
      manualEncum.setNamedParameter("costEncumbranceId", efinManualencumbrance.getId());

      if (manualEncum != null && manualEncum.list().size() > 0) {
        manencum = manualEncum.list().get(0);
      }

    } catch (Exception e) {
      log.error("Exception while getBudgetEnquiryLine: " + e);
      e.printStackTrace();
    }
    return manencum;
  }

  public static EfinBudgetInquiry getFundsBudgetEnquiryLine(InvoiceLine invoiceLine,
      EfinBudgetManencum manencum) {
    EfinBudgetInquiry inquiry = null;
    AccountingCombination fundsUniquecode = null;

    try {
      fundsUniquecode = PurchaseInvoiceSubmitUtils
          .getFundsUniquecode(invoiceLine.getEfinBudgmanuencumln().getId(), manencum);

      OBQuery<EfinBudgetInquiry> inquiryQuery = OBDal.getInstance().createQuery(
          EfinBudgetInquiry.class,
          " where accountingCombination.id = :combinationId and efinBudgetint.id = :initializationId ");
      inquiryQuery.setNamedParameter("combinationId", fundsUniquecode.getId());
      inquiryQuery.setNamedParameter("initializationId",
          invoiceLine.getInvoice().getEfinBudgetint().getId());

      if (inquiryQuery != null && inquiryQuery.list().size() > 0) {
        inquiry = inquiryQuery.list().get(0);
      }

    } catch (Exception e) {
      log.error("Exception while getFundsBudgetEnquiryLine: " + e);
      e.printStackTrace();
    }

    return inquiry;
  }

  public static EfinManualEncumInvoice getReservedFundsInvoice(Invoice invoice,
      EfinBudgetInquiry fundsInquiry, EfinBudgetManencum manencum) {
    EfinManualEncumInvoice encumInvoice = null;
    EfinBudgetManencumlines manencumlines = null;

    try {
      OBQuery<EfinManualEncumInvoice> invoiceQuery = OBDal.getInstance().createQuery(
          EfinManualEncumInvoice.class,
          " where invoice.id= :invoiceId and manualEncumbranceLines.id = :encumbranceLinesId");

      OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " where uniquecode = :uniquecode and manualEncumbrance.id = :manencum ");
      encumLines.setNamedParameter("uniquecode", fundsInquiry.getUniqueCode());
      encumLines.setNamedParameter("manencum", manencum.getId());

      if (encumLines != null && encumLines.list().size() > 0) {
        manencumlines = encumLines.list().get(0);
      }
      invoiceQuery.setNamedParameter("invoiceId", invoice.getId());
      invoiceQuery.setNamedParameter("encumbranceLinesId", manencumlines.getId());

      if (invoiceQuery != null && invoiceQuery.list().size() > 0) {
        encumInvoice = invoiceQuery.list().get(0);
      }

    } catch (Exception e) {
      log.error("Exception while getBudgetInitialiation: " + e);
      e.printStackTrace();
    }
    return encumInvoice;
  }

  public static String getFundsUniquecode(EfinBudgetInquiry budgetInquiry,
      EfinBudgetManencum manencum) {
    String strUniqcode = "", strFBudgetTypeValue = "", fundsUniqueCode = "";
    String[] costDimensions = null;
    try {
      strFBudgetTypeValue = manencum.getSalesCampaign().getSearchKey();

      strUniqcode = budgetInquiry.getAccountingCombination().getEfinUniqueCode();
      costDimensions = strUniqcode.split("-");
      costDimensions[4] = strFBudgetTypeValue;
      fundsUniqueCode = String.join("-", costDimensions);
    } catch (Exception e) {
      log.error("Exception while getFundsUniquecode: " + e);
      e.printStackTrace();
    }
    return fundsUniqueCode;
  }

  public static void moveReservedToActual(InvoiceLine invoiceLine,
      EfinBudgetManencum invoiceEncumbrance, FIN_Payment payment,
      AccountingCombination combination) {
    try {

      for (EfinBudgetManencumlines manencumlines : invoiceEncumbrance
          .getEfinBudgetManencumlinesList()) {
        for (EfinManualEncumInvoice reservedInvoice : manencumlines
            .getEfinManualEncumInvoiceList()) {
          if (reservedInvoice.getManualEncumbranceLines().getAccountingCombination().getId()
              .equals(combination.getId())) {

            reservedInvoice.setPaymentComplete(Boolean.TRUE);
            reservedInvoice.setPayment(payment);

            OBDal.getInstance().save(reservedInvoice);

          }
        }
      }

      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception while getFundsUniquecode: " + e);
      e.printStackTrace();
    }
  }

  @SuppressWarnings("rawtypes")
  public static void updatePaymentPostStatus(Invoice invoice, FIN_FinaccTransaction transaction) {
    try {

      List paymentList = new ArrayList();
      StringBuffer sqlQuery = new StringBuffer();
      String strTrxId = "";
      FIN_FinaccTransaction fin_FinaccTransaction = null;

      sqlQuery.append(" select trx.fin_finacc_transaction_id from fin_finacc_transaction trx ");
      sqlQuery.append(
          " join fin_payment pay on pay.fin_payment_id =trx.fin_payment_id join FIN_Payment_Detail_V pdv on pdv.fin_payment_id= pay.fin_payment_id");
      sqlQuery.append(
          " join fin_payment_sched_inv_v psiv on psiv.fin_payment_sched_inv_v_id=pdv.fin_payment_sched_inv_v_id join c_invoice inv on inv.c_invoice_id=psiv.c_invoice_id  ");
      sqlQuery.append(" where inv.c_invoice_id ='").append(invoice.getId())
          .append("' and trx.fin_finacc_transaction_id not in ('").append(transaction.getId())
          .append("') ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery.toString());

      if (query != null) {
        paymentList = query.list();
        if (paymentList.size() > 0) {
          for (Object object : paymentList) {
            strTrxId = object.toString();
            fin_FinaccTransaction = Utility.getObject(FIN_FinaccTransaction.class, strTrxId);

            fin_FinaccTransaction.setPosted("Y");
            fin_FinaccTransaction.setProcessed(Boolean.TRUE);

            OBDal.getInstance().save(fin_FinaccTransaction);
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while updatePaymentPostStatus: " + e);
      e.printStackTrace();
    }
  }

  public static void insertActuals(InvoiceLine invoiceLine, EfinBudgetIntialization efinBudgetint,
      BigDecimal amount) {
    try {
      ActualTransaction transaction = OBProvider.getInstance().get(ActualTransaction.class);

      transaction.setClient(invoiceLine.getClient());
      transaction.setOrganization(invoiceLine.getOrganization());
      transaction.setCreatedBy(OBContext.getOBContext().getUser());
      transaction.setUpdatedBy(OBContext.getOBContext().getUser());
      transaction.setPost(Boolean.TRUE);
      transaction.setInvoice(invoiceLine.getInvoice());
      transaction.setInvoiceLine(invoiceLine);
      transaction.setBudgetInitialization(efinBudgetint);
      transaction.setAccountingCombination(invoiceLine.getEfinCValidcombination());
      transaction.setAmount(amount);
      transaction.setDocumentNo(invoiceLine.getInvoice().getDocumentNo());
      transaction.setAccountingDate(invoiceLine.getInvoice().getAccountingDate());
      transaction.setInvoiceDate(invoiceLine.getInvoice().getInvoiceDate());
      transaction.setDescription(invoiceLine.getInvoice().getDescription());

      OBDal.getInstance().save(transaction);

      PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(invoiceLine.getEfinCValidcombination(), amount,
          efinBudgetint, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);

    } catch (Exception e) {
      log.error("Exception while insertActuals: " + e);
      e.printStackTrace();
    }
  }

  @SuppressWarnings("rawtypes")
  public static List getEncumbranceObject(Invoice invoice) {
    List encumbranceObjects = new ArrayList();
    try {
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(
          " select sum(invl.linenetamt) as amount, encumlines.efin_budget_manencumlines_id  ");
      queryBuilder.append(" from c_invoice inv ");
      queryBuilder.append(" join c_invoiceline invl on invl.c_invoice_id = inv.c_invoice_id ");
      queryBuilder
          .append(" join efin_applied_prepayment ppi on ppi.c_invoice_id = inv.c_invoice_id ");
      queryBuilder.append(" join c_invoice app on app.c_invoice_id = efin_applied_invoice ");
      queryBuilder.append(" join c_invoiceline appl on appl.c_invoice_id = app.c_invoice_id  ");
      queryBuilder.append(
          " join efin_budget_manencumlines encumlines on encumlines.efin_budget_manencum_id = app.em_efin_manualencumbrance_id ");
      queryBuilder.append(" and appl.em_efin_expense_account = encumlines.c_validcombination_id ");
      queryBuilder
          .append(" where invl.em_efin_c_validcombination_id = appl.em_efin_expense_account ");
      queryBuilder.append(" and  ppi.c_invoice_id='").append(invoice.getId()).append("'");
      queryBuilder.append(" group by encumlines.efin_budget_manencumlines_id ");

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());

      if (query != null) {
        encumbranceObjects = query.list();
      }
    } catch (Exception e) {
      log.error("Exception while getEncumbranceObject: " + e);
      e.printStackTrace();
    }
    return encumbranceObjects;
  }

  public static void markInvoiceReferencesAsPaid(EfinBudgetManencumlines encumbranceLine,
      Invoice invoice, BigDecimal convertedAmount) {
    try {
      Invoice appliedInvoice = null;
      BigDecimal advanceAmount = convertedAmount;
      BigDecimal remainingReservedAmount = BigDecimal.ZERO;

      List<EfinManualEncumInvoice> invoiceRefs = new ArrayList<EfinManualEncumInvoice>();
      for (AppliedPrepaymentInvoice prepayments : invoice.getEfinAppliedPrepaymentList()) {
        appliedInvoice = prepayments.getEfinAppliedInvoice();

        OBQuery<EfinManualEncumInvoice> invoiceRefQuery = OBDal.getInstance().createQuery(
            EfinManualEncumInvoice.class,
            " where manualEncumbranceLines.id = :encumbranceLineId and invoice.id =:invoiceId and paymentComplete='N' order by invamount asc ");

        invoiceRefQuery.setNamedParameter("encumbranceLineId", encumbranceLine.getId());
        invoiceRefQuery.setNamedParameter("invoiceId", appliedInvoice.getId());

        if (invoiceRefQuery != null) {
          invoiceRefs = invoiceRefQuery.list();
          if (invoiceRefs.size() > 0) {
            for (EfinManualEncumInvoice invoiceRef : invoiceRefs) {
              BigDecimal reservedAmount = invoiceRef.getInvamount();

              invoiceRef.setPaymentComplete(Boolean.TRUE);
              invoiceRef.setInvamount(advanceAmount);
              invoiceRef.setPrepaymeninvoice(appliedInvoice);
              // invoiceRef.setInvoice(invoice);

              OBDal.getInstance().save(invoiceRef);
              if (reservedAmount.compareTo(advanceAmount) > 0) {

                remainingReservedAmount = reservedAmount.subtract(advanceAmount);
                PostUtilsDAO.InsertInvoicesInEncumbrance(appliedInvoice, encumbranceLine,
                    remainingReservedAmount, Boolean.TRUE);

                break;
              }
              advanceAmount = reservedAmount.subtract(advanceAmount);
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while getEncumbranceObject: " + e);
      e.printStackTrace();
    }
  }

  public static void InsertInvoicesInEncumbrance(Invoice invoice,
      EfinBudgetManencumlines encumbranceLines, BigDecimal invoiceAmount, Boolean isSplit) {
    try {
      EfinManualEncumInvoice efinManEncInv = OBProvider.getInstance()
          .get(EfinManualEncumInvoice.class);

      efinManEncInv.setClient(OBContext.getOBContext().getCurrentClient());
      efinManEncInv.setOrganization(invoice.getOrganization());
      efinManEncInv.setActive(true);
      efinManEncInv.setUpdatedBy(OBContext.getOBContext().getUser());
      efinManEncInv.setCreationDate(new java.util.Date());
      efinManEncInv.setCreatedBy(OBContext.getOBContext().getUser());
      efinManEncInv.setUpdated(new java.util.Date());
      efinManEncInv.setInvamount(invoiceAmount);
      efinManEncInv.setInvoiceDate(invoice.getInvoiceDate());
      efinManEncInv.setAccountingDate(invoice.getAccountingDate());
      efinManEncInv.setDescription(invoice.getDescription());
      efinManEncInv.setInvoice(invoice);
      efinManEncInv.setManualEncumbranceLines(encumbranceLines);
      efinManEncInv.setDocumentNo(invoice.getDocumentNo());
      efinManEncInv.setPaymentComplete(Boolean.FALSE);
      efinManEncInv.setSplit(isSplit);

      OBDal.getInstance().save(efinManEncInv);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log.error("Exception while InsertInvoicesInEncumbrance: " + e);
      e.printStackTrace();
    }
  }
}
