package sa.elm.ob.finance.ad_process.FinancialTransactionCancellation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.Note;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.AppliedPrepaymentInvoice;
import sa.elm.ob.finance.EfinPoApproval;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmit;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.PurchaseInvoiceCancellation.PurchaseInvoiceCancellationDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author gopalakrishnan on 02-09-2016
 * 
 */
public class FinanancialTransactionCancellation implements Process {
  /**
   * Financial Transaction Cancellation from Financial Account Table(fin_finacc_transaction)
   * 
   */

  private static final Logger log = Logger.getLogger(FinanancialTransactionCancellation.class);
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PO_DOCUMENT = "POM";
  private static final String RDV_DOCUMENT = "RDV";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug(" Financial Transaction Cancellation ");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      String transctionId = (String) bundle.getParams().get("Fin_Finacc_Transaction_ID");
      FIN_FinaccTransaction finTransaction = OBDal.getInstance().get(FIN_FinaccTransaction.class,
          transctionId);
      String sql = "", whereClause = "", sqlfinal = "", sqlmultiple = "";
      PreparedStatement ps = null;
      ResultSet rs = null;
      Connection connect = OBDal.getInstance().getConnection();
      PurchaseInvoiceCancellationDAO dao = new PurchaseInvoiceCancellationDAO(connect);
      final String orgId = finTransaction.getOrganization().getId();
      final String clientId = finTransaction.getClient().getId();
      final String roleId = vars.getRole();
      final String userId = finTransaction.getUpdatedBy().getId();
      final String invoiceNo = finTransaction.getEfinDocumentNo();
      Date trxnDate = finTransaction.getTransactionDate();
      Boolean multipleTrxCancellation = false;
      Boolean updateInvoice = true;
      Boolean updatePayment = true;
      Period period = null;

      List<FIN_FinaccTransaction> transactionList = null;
      ConnectionProvider conn = bundle.getConnection();
      sql = " select trx.fin_finacc_transaction_id,pay.amount as amount,inv.documentno,inv.c_invoice_id,scd.fin_payment_scheduledetail_id,sc.fin_payment_schedule_id from fin_finacc_transaction trx  join fin_payment pay on trx.fin_payment_id=pay.fin_payment_id "
          + " join fin_payment_detail det on det.fin_payment_id=pay.fin_payment_id "
          + " join fin_payment_scheduledetail scd on scd.fin_payment_detail_id=det.fin_payment_detail_id  "
          + " join fin_payment_schedule sc on sc.fin_payment_schedule_id=scd.fin_payment_schedule_invoice  "
          + " join c_invoice inv on inv.c_invoice_id=sc.c_invoice_id  where trx.fin_finacc_transaction_id='"
          + transctionId + "' ";

      // Check partial payment
      whereClause = " and inv.grandtotal > pay.amount ";
      sqlfinal = sql + whereClause;
      ps = conn.getPreparedStatement(sqlfinal);
      rs = ps.executeQuery();
      if (rs.next()) {
        sqlmultiple = " select em_efin_document_no as documentno from fin_finacc_transaction trx  join fin_payment pay on trx.fin_payment_id=pay.fin_payment_id "
            + " join fin_payment_detail det on det.fin_payment_id=pay.fin_payment_id "
            + " join fin_payment_scheduledetail scd on scd.fin_payment_detail_id=det.fin_payment_detail_id  "
            + " join fin_payment_schedule sc on sc.fin_payment_schedule_id=scd.fin_payment_schedule_invoice  "
            + " join c_invoice inv on inv.c_invoice_id=sc.c_invoice_id  where trx.em_efin_document_no='"
            + invoiceNo + "'  group by em_efin_document_no,inv.grandtotal \n"
            + " having  inv.grandtotal - sum(pay.amount) =0";
        ps = conn.getPreparedStatement(sqlmultiple);
        rs = ps.executeQuery();
        if (!rs.next()) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("Efin_FIn_Trx_Cancel_Partial").replace("@", invoiceNo));
          bundle.setResult(result);
          return;
        } else {
          multipleTrxCancellation = true;
        }
      }

      if (!multipleTrxCancellation) {
        // check multiple payment plan
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          OBQuery<FIN_PaymentSchedule> scheduleList = OBDal.getInstance().createQuery(
              FIN_PaymentSchedule.class,
              "as e where e.invoice.id ='" + rs.getString("c_invoice_id") + "'");
          if (scheduleList.list().size() > 1) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                OBMessageUtils.messageBD("Efin_FIn_Trx_Cancel_Schedule").replace("@", invoiceNo));
            bundle.setResult(result);
            return;
          }
        }
      }
      // check if period is closed
      String sDate = new SimpleDateFormat("dd-MM-yyyy").format(trxnDate);
      String period_id = sa.elm.ob.utility.util.Utility.getPeriod(sDate, orgId);
      if (!StringUtils.isEmpty(period_id)) {
        period = OBDal.getInstance().get(Period.class, period_id);
        if (!period.getStatus().equals("M") && !period.getStatus().equals("O")) {
          // period not opened
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }

      // Reverse the fact account entry
      if (multipleTrxCancellation) {
        // Multiple financial transaction cancellation
        OBQuery<FIN_FinaccTransaction> transactionQry = OBDal.getInstance()
            .createQuery(FIN_FinaccTransaction.class, " as e where e.efinDocumentNo =:invoiceno");
        transactionQry.setNamedParameter("invoiceno", invoiceNo);
        transactionList = transactionQry.list();
        for (FIN_FinaccTransaction transaction : transactionList) {
          reversalFactAccounting(transaction, bundle);
        }
      } else {
        reversalFactAccounting(finTransaction, bundle);
      }

      // update the invoice only for with drawn not cleared Transaction

      if (multipleTrxCancellation) {
        sql = " select trx.fin_finacc_transaction_id,pay.amount as amount,inv.documentno,inv.c_invoice_id,scd.fin_payment_scheduledetail_id,sc.fin_payment_schedule_id from fin_finacc_transaction trx  join fin_payment pay on trx.fin_payment_id=pay.fin_payment_id "
            + " join fin_payment_detail det on det.fin_payment_id=pay.fin_payment_id "
            + " join fin_payment_scheduledetail scd on scd.fin_payment_detail_id=det.fin_payment_detail_id  "
            + " join fin_payment_schedule sc on sc.fin_payment_schedule_id=scd.fin_payment_schedule_invoice  "
            + " join c_invoice inv on inv.c_invoice_id=sc.c_invoice_id  where trx.em_efin_document_no='"
            + invoiceNo + "' ";
      }

      if (finTransaction.getStatus().equals("PWNC")) {
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        while (rs.next()) {
          FIN_PaymentScheduleDetail scdetail = OBDal.getInstance()
              .get(FIN_PaymentScheduleDetail.class, rs.getString("fin_payment_scheduledetail_id"));
          scdetail.setCanceled(true);
          scdetail.setAmount(BigDecimal.ZERO);
          OBDal.getInstance().save(scdetail);
          FIN_PaymentSchedule schedule = OBDal.getInstance().get(FIN_PaymentSchedule.class,
              rs.getString("fin_payment_schedule_id"));
          schedule.setPaidAmount(
              schedule.getPaidAmount().subtract(new BigDecimal(rs.getString("amount"))));
          schedule.setOutstandingAmount(
              schedule.getOutstandingAmount().add(new BigDecimal(rs.getString("amount"))));
          OBDal.getInstance().save(schedule);
          OBDal.getInstance().flush();
          if (updateInvoice) {
            Invoice invoice = OBDal.getInstance().get(Invoice.class, rs.getString("c_invoice_id"));
            // reverse the actual
            // dao.reverseActual(vars, clientId, orgId, roleId, userId, invoice);
            // reverse the reservation

            String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
            List<AppliedPrepaymentInvoice> appliedPrepayments = new ArrayList<AppliedPrepaymentInvoice>();

            if (PPI_DOCUMENT.equals(strInvoiceType)) {

              appliedPrepayments = PurchaseInvoiceCancellationDAO.getAppliedPrepayments(invoice);
              /*
               * hasInProcessInvoices = PurchaseInvoiceCancellationDAO
               * .hasInProcessInvoices(appliedPrepayments);
               */

              if (appliedPrepayments.size() > 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_PurInv_PrePayInvApp_Can@");
                bundle.setResult(result);
                return;
              }
            }
            // if invoice cancel, then need to reduce qty in po reference invoiced Qty
            if (PO_DOCUMENT.equals(strInvoiceType)) {
              OBInterceptor.setPreventUpdateInfoChange(true);

              if (invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")) {
                for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
                  if (invLine.isEfinIspom()) {
                    OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                        invLine.getSalesOrderLine().getId());
                    ordLine.setEfinAmtinvoiced(
                        ordLine.getEfinAmtinvoiced().subtract(invLine.getEfinAmtinvoiced()));
                    OBDal.getInstance().save(ordLine);
                  }
                }
              } else {
                for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
                  if (invLine.isEfinIspom()) {
                    OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                        invLine.getSalesOrderLine().getId());
                    ordLine.setInvoicedQuantity(
                        ordLine.getInvoicedQuantity().subtract(invLine.getInvoicedQuantity()));
                    OBDal.getInstance().save(ordLine);
                  }
                }
              }
            }

            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);

            dao.reverseReservation(vars, clientId, orgId, roleId, userId, invoice, null);

            // update the invoice Header
            dao.updateInvHeader(invoice);

            // remove the prepayment invoice entry in manual encumbrance --> prepayment invoice
            if (invoice.getTransactionDocument().isEfinIsprepayinv()) {
              dao.removePrepayInvoice(invoice);
            }

            // insert the Approval History
            PurchaseInvoiceSubmit.insertInvoiceApprover(invoice,
                "Cancelled From Financial Transaction", "CA", null);
            updateInvoice = false;
          }
        }
      }
      if (!multipleTrxCancellation) {
        finTransaction.setStatus("EFIN_CAN");
        finTransaction.setDateAcct(trxnDate);
        OBDal.getInstance().save(finTransaction);
        // update the Financial Account current balance
        FIN_FinancialAccount finact = finTransaction.getAccount();
        finact.setCurrentBalance(finact.getCurrentBalance().add(finTransaction.getPaymentAmount()));
        OBDal.getInstance().save(finact);
        // update the description in corresponding payment
        FIN_Payment payment = finTransaction.getFinPayment();
        payment.setStatus("EFIN_CAN");
        payment.setDescription("Reversal entry of ".concat(finTransaction.getEfinDocumentNo()));
        insertApprovalHistory(vars, "CA", payment, "CA", orgId);
        OBDal.getInstance().save(payment);

        // reduce the paid amt in po contract Task No.7470
        if (payment != null && payment.getEfinInvoice() != null) {
          String strInvoiceType = PurchaseInvoiceSubmitUtils
              .getInvoiceType(payment.getEfinInvoice());
          Invoice invoice = payment.getEfinInvoice();
          if (RDV_DOCUMENT.equals(strInvoiceType) || PO_DOCUMENT.equals(strInvoiceType)) {
            Order order = null;
            if (PO_DOCUMENT.equals(strInvoiceType)) {
              if (invoice.getEfinCOrder() != null)
                order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
            } else if (RDV_DOCUMENT.equals(strInvoiceType)) {
              if (invoice.getSalesOrder() != null)
                order = OBDal.getInstance().get(Order.class, invoice.getSalesOrder().getId());
            }
            if (order != null) {
              // order = PurchaseInvoiceSubmitUtils.getLatestOrder(order);
              List<Order> orderList = PurchaseInvoiceSubmitUtils.getGreaterRevisionOrdList(order);
              if (orderList.size() > 0) {
                OBInterceptor.setPreventUpdateInfoChange(true);
                for (Order ordObj : orderList) {
                  ordObj.setEfinInvoiceAmt(
                      ordObj.getEfinInvoiceAmt().subtract(invoice.getGrandTotalAmount()));
                  ordObj.setEfinRemainingAmt(
                      ordObj.getEfinRemainingAmt().add(invoice.getGrandTotalAmount()));
                  ordObj.setEfinPaidAmt(ordObj.getEfinPaidAmt().subtract(payment.getAmount()));
                  OBDal.getInstance().save(ordObj);
                }
                OBDal.getInstance().flush();
                OBInterceptor.setPreventUpdateInfoChange(false);
              }
            }
          }
        }

        OBDal.getInstance().commitAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_FIN_Trx_Cancelled@");
        bundle.setResult(result);
        return;
      } else {
        for (FIN_FinaccTransaction trx : transactionList) {
          trx.setStatus("EFIN_CAN");
          trx.setDateAcct(trxnDate);
          OBDal.getInstance().save(trx);

          // update the Financial Account current balance
          FIN_FinancialAccount finact = trx.getAccount();
          finact.setCurrentBalance(finact.getCurrentBalance().add(trx.getPaymentAmount()));
          OBDal.getInstance().save(finact);

          // update the description in corresponding payment
          FIN_Payment payment = trx.getFinPayment();
          payment.setStatus("EFIN_CAN");
          payment.setDescription("Reversal entry of ".concat(trx.getEfinDocumentNo()));
          insertApprovalHistory(vars, "CA", payment, "CA", orgId);
          OBDal.getInstance().save(payment);

          if (updatePayment) {
            // reduce the paid amt in po contract Task No.7470
            if (payment != null && payment.getEfinInvoice() != null) {
              String strInvoiceType = PurchaseInvoiceSubmitUtils
                  .getInvoiceType(payment.getEfinInvoice());
              Invoice invoice = payment.getEfinInvoice();
              if (RDV_DOCUMENT.equals(strInvoiceType) || PO_DOCUMENT.equals(strInvoiceType)) {
                Order order = null;
                if (PO_DOCUMENT.equals(strInvoiceType)) {
                  if (invoice.getEfinCOrder() != null)
                    order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
                } else if (RDV_DOCUMENT.equals(strInvoiceType)) {
                  if (invoice.getSalesOrder() != null)
                    order = OBDal.getInstance().get(Order.class, invoice.getSalesOrder().getId());
                }
                if (order != null) {
                  // order = PurchaseInvoiceSubmitUtils.getLatestOrder(order);
                  List<Order> orderList = PurchaseInvoiceSubmitUtils
                      .getGreaterRevisionOrdList(order);
                  if (orderList.size() > 0) {
                    OBInterceptor.setPreventUpdateInfoChange(true);
                    for (Order ordObj : orderList) {
                      ordObj.setEfinInvoiceAmt(
                          ordObj.getEfinInvoiceAmt().subtract(invoice.getGrandTotalAmount()));
                      ordObj.setEfinRemainingAmt(
                          ordObj.getEfinRemainingAmt().add(invoice.getGrandTotalAmount()));
                      ordObj.setEfinPaidAmt(ordObj.getEfinPaidAmt().subtract(payment.getAmount()));
                      OBDal.getInstance().save(ordObj);
                    }
                    OBDal.getInstance().flush();
                    OBInterceptor.setPreventUpdateInfoChange(false);
                  }
                }
              }
            }
            updatePayment = false;
          }
        }
        OBDal.getInstance().commitAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_FIN_Trx_Cancelled@");
        bundle.setResult(result);
        return;
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param vars
   * @param status
   * @param payment
   * @param strComments
   * @param inpOrgId
   *          Insert records in approval history with cancelled status
   */
  public static void insertApprovalHistory(VariablesSecureApp vars, String status,
      FIN_Payment payment, String strComments, String inpOrgId) {
    EfinPoApproval poApproval = null;
    User user = null;
    Note notes = null;
    try {
      OBContext.setAdminMode();
      poApproval = OBProvider.getInstance().get(EfinPoApproval.class);
      user = OBDal.getInstance().get(User.class, vars.getUser());
      poApproval.setClient(payment.getClient());
      poApproval.setOrganization(payment.getOrganization());
      poApproval.setCreatedBy(user);
      poApproval.setUpdatedBy(user);
      poApproval.setAlertStatus(status);
      poApproval.setRole(OBDal.getInstance().get(Role.class, vars.getRole()));
      poApproval.setUserContact(user);
      poApproval.setPayment(payment);
      poApproval.setApproveddate(new Date());
      OBDal.getInstance().save(poApproval);
      poApproval.setObuiappNote(notes);
      OBDal.getInstance().save(poApproval);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log.error(" Exception while adding in approvalHistory ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to reverse the fact acct entry created for the transaction
   * 
   * @param finTransaction
   * @param bundle
   */
  public static void reversalFactAccounting(FIN_FinaccTransaction finTransaction,
      ProcessBundle bundle) {

    final String calendarId = finTransaction.getOrganization().getCalendar().getId();
    final String orgId = finTransaction.getOrganization().getId();
    String AccountDate = "";

    // check withdrawn not cleared and posted record and insert reverse entry in fact_acct
    if (finTransaction.getPosted().equals("Y") && finTransaction.getStatus().equals("PWNC")) {
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(finTransaction.getDateAcct());
      String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "PS", calendarId, orgId, true);
      if (SequenceNo.equals("0")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
        bundle.setResult(result);
        return;
        // throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
      }
      OBQuery<AccountingFact> acctfactList = OBDal.getInstance().createQuery(AccountingFact.class,
          "as e where e.recordID='" + finTransaction.getId() + "'");
      if (acctfactList.list().size() > 0) {
        for (AccountingFact accountFact : acctfactList.list()) {
          AccountingFact objCloneFact = (AccountingFact) DalUtil.copy(accountFact, false);
          objCloneFact.setForeignCurrencyCredit(accountFact.getForeignCurrencyDebit());
          objCloneFact.setForeignCurrencyDebit(accountFact.getForeignCurrencyCredit());
          objCloneFact.setEfinAcctseq(SequenceNo);
          objCloneFact.setGroupID(SequenceIdData.getUUID());
          objCloneFact.setDocumentCategory("PRJ");
          objCloneFact.setDescription("Reversal of" + accountFact.getDescription());
          objCloneFact.setDebit(accountFact.getCredit());
          objCloneFact.setCredit(accountFact.getDebit());
          objCloneFact.setUpdated(new java.util.Date());
          objCloneFact.setUpdatedBy(OBContext.getOBContext().getUser());
          objCloneFact.setCreationDate(new java.util.Date());
          objCloneFact.setCreatedBy(OBContext.getOBContext().getUser());
          objCloneFact.setAccountingDate(new java.util.Date());
          OBDal.getInstance().save(objCloneFact);
          OBDal.getInstance().flush();
          OBDal.getInstance().refresh(objCloneFact);
        }
      }
    }
  }

}
