package org.openbravo.erpCommon.ad_forms;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

public class FinTrxPostingDAOImpl implements FinAccountTransactionTemplateDAO {

  private Connection conn = null;
  private static final Logger log = Logger.getLogger(FinTrxPostingDAOImpl.class);

  public FinTrxPostingDAOImpl(Connection connection) {
    this.conn = connection;
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getInvoice(String strRecordId) {
    String strInvoiceId = "";
    Query query = null;
    StringBuilder queryBuilder = null;

    try {
      queryBuilder = new StringBuilder();

      queryBuilder.append(" select  schd.invoice.id  from FIN_Finacc_Transaction trx ");
      queryBuilder.append(" join trx.finPayment payment, FIN_Payment_Detail_V det ");
      queryBuilder.append(" join det.paymentPlanInvoice schd ");
      queryBuilder.append(" where det.payment.id = payment.id and trx.id= :transactionID");

      query = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      query.setString("transactionID", strRecordId);

      if (query != null) {
        List<String> invoiceIds = query.list();

        if (invoiceIds.size() > 0) {
          strInvoiceId = invoiceIds.get(0);
        }
      }
    } catch (Exception e) {
      log.error("Exception while getInvoice :" + e);
      e.printStackTrace();
    }

    return strInvoiceId;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<FIN_Payment> getListOfPayments(String strInvoiceId) {
    List<FIN_Payment> payments = null;
    StringBuilder builder = new StringBuilder();
    Query paymentsQuery = null;

    try {
      builder.append(
          " select payment from  FIN_Payment_Schedule pay join pay.fINPaymentDetailVPaymentPlanInvoiceList plan ");
      builder.append(
          " join plan.payment payment where pay.invoice.id = :invoiceID and payment.status <> 'EFIN_CAN' ");

      paymentsQuery = OBDal.getInstance().getSession().createQuery(builder.toString());
      paymentsQuery.setString("invoiceID", strInvoiceId);

      if (paymentsQuery != null) {
        payments = paymentsQuery.list();
      }

    } catch (Exception e) {
      log.error("Exception while getListOfPayments :" + e);
      e.printStackTrace();
    }
    return payments;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<FIN_FinaccTransaction> getListOfFinancialAccountTrx(String strInvoiceId) {
    List<FIN_FinaccTransaction> transactions = null;
    StringBuilder queryBuilder = new StringBuilder();
    Query transactionsQuery = null;
    try {

      queryBuilder.append(" select trx from  FIN_Payment_Schedule pay ");
      queryBuilder.append(" join pay.fINPaymentDetailVPaymentPlanInvoiceList plan ");
      queryBuilder.append(" join plan.payment payment  join payment.fINFinaccTransactionList trx ");
      queryBuilder.append(" where pay.invoice.id = :invoiceID");

      transactionsQuery = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());
      transactionsQuery.setString("invoiceID", strInvoiceId);

      if (transactionsQuery != null) {
        transactions = transactionsQuery.list();
      }
    } catch (Exception e) {
      log.error("Exception while getListOfPayments :" + e);
      e.printStackTrace();
    }
    return transactions;
  }

  @Override
  public Boolean allowPosting(String strRecordId) {
    Boolean validTrx = Boolean.FALSE;
    SQLQuery query = null;
    try {
      log.debug("Record Id: " + strRecordId);

      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append(
          " select      case when inv.ispaid ='N' then 'N'      when ((select fin_payment_id from fin_payment_scheduledetail ");
      queryBuilder.append(
          " join fin_payment_schedule on fin_payment_scheduledetail.fin_payment_schedule_invoice = fin_payment_schedule.fin_payment_schedule_id ");
      queryBuilder.append(
          " join fin_payment_detail on fin_payment_detail.fin_payment_detail_id = fin_payment_scheduledetail.fin_payment_detail_id ");
      queryBuilder.append(
          " where fin_payment_schedule.c_invoice_id = psd.c_invoice_id and fin_payment_scheduledetail.updated = (select  max(fin_payment_scheduledetail.updated) from fin_payment_scheduledetail ");
      queryBuilder.append(
          " join fin_payment_schedule on fin_payment_scheduledetail.fin_payment_schedule_invoice = fin_payment_schedule.fin_payment_schedule_id ");
      queryBuilder.append(
          " where fin_payment_schedule.c_invoice_id = psd.c_invoice_id)) = trx.fin_payment_id  ) then 'Y'  else 'N' end ");
      queryBuilder.append(
          " from fin_finacc_transaction  trx  join fin_payment p on p.fin_payment_id = trx.fin_payment_id join fin_payment_detail_v dt on dt.fin_payment_id = p.fin_payment_id ");
      queryBuilder.append(
          " join fin_payment_sched_inv_v psd on psd.fin_payment_sched_inv_v_id = dt.fin_payment_sched_inv_v_id join c_invoice inv on inv.c_invoice_id = psd.c_invoice_id where trx.fin_finacc_transaction_id  = '")
          .append(strRecordId).append("'");

      query = OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
      if (query != null && query.list().size() > 0) {
        validTrx = query.list().get(0).toString().equals("Y") ? Boolean.TRUE : Boolean.FALSE;
      }
    } catch (Exception e) {
      log.error("Exception while allowPosting: " + e);
      e.printStackTrace();
    }
    return validTrx;
  }
}
