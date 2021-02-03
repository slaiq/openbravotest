package sa.elm.ob.finance.ad_process.PurchaseInvoiceCancellation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.AppliedPrepaymentInvoice;
import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.actionHandler.InvoiceRevokeDAO;
import sa.elm.ob.finance.ad_process.FinancialTransactionCancellation.FinanancialTransactionCancellation;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmit;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;

public class PurchaseInvoiceCancellation implements Process {
  private static final Logger log = Logger.getLogger(PurchaseInvoiceCancellation.class);
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";
  private static final String PO_DOCUMENT = "POM";
  private static final String RDV_DOCUMENT = "RDV";

  @SuppressWarnings("unused")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection connection = null;
    try {
      OBContext.setAdminMode();

      String invoiceId = (String) bundle.getParams().get("C_Invoice_ID");
      // get the invoice details
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

      String sql = "", paymentId = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      final String clientId = bundle.getContext().getClient();
      final String orgId = invoice.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
      final String roleId = bundle.getContext().getRole();
      String comments = bundle.getParams().get("comments").toString();
      Connection connect = OBDal.getInstance().getConnection();
      Boolean reserve = false, reverseActual = false, reversejournal = false;
      int count = 0;
      PurchaseInvoiceCancellationDAO dao = new PurchaseInvoiceCancellationDAO(connect);
      List<AppliedPrepaymentInvoice> appliedPrepayments = new ArrayList<AppliedPrepaymentInvoice>();
      Boolean hasInProcessInvoices = Boolean.FALSE;

      // unpaid
      if (log.isDebugEnabled()) {
        log.debug("paycom:" + invoice.isPaymentComplete());
        log.debug("invoice type:" + strInvoiceType);
        log.debug("getTotalPaid:" + invoice.getTotalPaid());
      }
      if ("DR".equals(invoice.getDocumentStatus())
          || "EFIN_CA".equals(invoice.getDocumentStatus())) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AlreadyPreocessed@");
        bundle.setResult(result);
        return;
      }

      // cancel validation. payment out should cancel.
      int a = invoice.getFINPaymentEMEfinInvoiceIDList().size();
      for (FIN_Payment paymentOut : invoice.getFINPaymentEMEfinInvoiceIDList()) {
        if (!paymentOut.getStatus().equals("EFIN_CAN")) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_PaymentOutNot_Cancel@");
          bundle.setResult(result);
          return;
        }
      }

      // RDV Validation
      if (invoice.isEfinIsrdv()) {

        // if next version is created and current version have advance then should not allow to
        // delete.
        List<EfinRDVTxnline> rdvTxnList = invoice.getEfinRdvtxn().getEfinRDVTxnlineList();
        for (EfinRDVTxnline rdvTxnln : rdvTxnList) {
          if (rdvTxnln.isAdvance()) {
            if (rdvTxnln.getEfinRdv().getEfinRDVTxnList()
                .size() > rdvTxnln.getEfinRdvtxn().getTXNVersion() + 1) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_Have_Adv_Deduction_Can@");
              bundle.setResult(result);
              return;
            }
          }
        }

        // if current version have penalty and applied penalty release in further verison then
        // should not allow.
        EfinRDVTransaction rdvTxn = invoice.getEfinRdvtxn();
        OBQuery<EfinPenaltyAction> penalty = OBDal.getInstance()
            .createQuery(EfinPenaltyAction.class, "efinRdvtxnline.efinRdvtxn.id=:rdvTxnId");
        penalty.setNamedParameter("rdvTxnId", rdvTxn.getId());
        List<EfinPenaltyAction> penaltyList = penalty.list();
        for (EfinPenaltyAction penaltyRel : penaltyList) {
          if (penaltyRel.getEfinPenaltyActionPenaltyRelIDList().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Have_PenaltyRelease_cancel"));
          }
        }
      }

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

      // reduce qty in order for POM.
      if (PO_DOCUMENT.equals(strInvoiceType)) {
        OBInterceptor.setPreventUpdateInfoChange(true);
        if (invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")) {
          Order order = null;
          for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
            if (invLine.isEfinIspom()) {
              if (invoice.getEfinCOrder() != null)
                order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());

              OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                  invLine.getSalesOrderLine().getId());
              if (!invLine.isEFINIsTaxLine() || (invLine.isEFINIsTaxLine() && order != null
                  && ((order.isEscmIstax() && invoice.isEfinIstax())
                      || (!order.isEscmIstax() && invoice.isEfinIstax()
                          && invoice.getEfinTaxMethod().isPriceIncludesTax())))) {
                // ps = connect.prepareStatement(
                // "update c_orderline set em_efin_amtinvoiced=? where c_orderline_id=? ");
                // ps.setBigDecimal(1,
                // ordLine.getEfinAmtinvoiced().subtract(invLine.getEfinAmtinvoiced()));
                // ps.setString(2, ordLine.getId());
                // ps.executeUpdate();

                ordLine.setEfinAmtinvoiced(
                    ordLine.getEfinAmtinvoiced().subtract(invLine.getEfinAmtinvoiced()));
              }
              OBDal.getInstance().save(ordLine);
            }
          }
        } else {
          for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
            if (invLine.isEfinIspom() && !invLine.isEFINIsTaxLine()) {
              OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                  invLine.getSalesOrderLine().getId());
              //
              // ps = connect
              // .prepareStatement("update c_orderline set qtyinvoiced=? where c_orderline_id=? ");
              // ps.setBigDecimal(1,
              // ordLine.getInvoicedQuantity().subtract(invLine.getInvoicedQuantity()));
              // ps.setString(2, ordLine.getId());
              // ps.executeUpdate();
              ordLine.setInvoicedQuantity(
                  ordLine.getInvoicedQuantity().subtract(invLine.getInvoicedQuantity()));
              OBDal.getInstance().save(ordLine);
            }
          }
        }
      }

      OBDal.getInstance().flush();
      OBInterceptor.setPreventUpdateInfoChange(false);

      // revert invoice amt & Remaining amt Task No.7470
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
              // ps = connect.prepareStatement(
              // "update c_order set em_efin_invoice_amt=?, em_efin_remaining_amt=? "
              // + " where c_order_id=?");
              // ps.setBigDecimal(1, ordObj.getEfinInvoiceAmt().add(invoice.getGrandTotalAmount()));
              // ps.setBigDecimal(2,
              // ordObj.getEfinRemainingAmt().subtract(invoice.getGrandTotalAmount()));
              // ps.setString(3, ordObj.getId());
              // ps.executeUpdate();

              ordObj.setEfinInvoiceAmt(
                  ordObj.getEfinInvoiceAmt().subtract(invoice.getGrandTotalAmount()));
              ordObj.setEfinRemainingAmt(
                  ordObj.getEfinRemainingAmt().add(invoice.getGrandTotalAmount()));
              OBDal.getInstance().save(ordObj);
            }
          }
          OBDal.getInstance().flush();
          OBInterceptor.setPreventUpdateInfoChange(false);
        }

      }

      if (invoice.getTotalPaid().compareTo(BigDecimal.ZERO) == 0) { // !invoice.isPaymentComplete()
                                                                    // && (
        log.debug("getPosted:" + invoice.getPosted());
        log.debug("Invoice type:" + strInvoiceType);

        if (invoice.getPosted() != null && invoice.getPosted().equals("Y")
            && PPA_DOCUMENT.equals(strInvoiceType)) {
          // create the reverse journal entries
          reversejournal = dao.reversejournal(vars, connect, clientId, orgId, roleId, userId,
              invoice, null);
          // delete the entries in budget actual
          // reverseActual = dao.reverseActual(vars, clientId, orgId, roleId, userId, invoice);
        }
        // invoice created through the po hold plan
        if (RDV_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
          InvoiceRevokeDAO.releaseTempEncumbrance(invoice);
        }

        if (invoice.isEfinIsreserved()) {
          // reverse the reservation
          reserve = dao.reverseReservation(vars, clientId, orgId, roleId, userId, invoice,
              comments);
        }

        // update the invoice Header
        count = dao.updateInvHeader(invoice);

        // remove the prepayment invoice entry in manual encumbrance --> prepayment invoice
        if (PPI_DOCUMENT.equals(strInvoiceType)) {
          count = dao.removePrepayInvoice(invoice);
        }

        // insert the Approval History
        count = PurchaseInvoiceSubmit.insertInvoiceApprover(invoice, comments, "CA", null);
        if (count > 0 && invoice.getDocumentStatus().equals("EFIN_CA")) {
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PurInv_Cancel@");
          bundle.setResult(result);
          return;
        }
      }
      log.debug("getOutstandingAmount:" + invoice.getOutstandingAmount());
      // full paid
      if (invoice.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {

        // get payment details
        OBQuery<FIN_PaymentSchedule> schedule = OBDal.getInstance()
            .createQuery(FIN_PaymentSchedule.class, " invoice.id= :invoiceID ");
        schedule.setNamedParameter("invoiceID", invoice.getId());
        List<FIN_PaymentSchedule> scheduleList = schedule.list();
        log.debug("schedule:" + scheduleList.size());
        if (scheduleList.size() > 0) {
          for (FIN_PaymentSchedule sche : scheduleList) {
            OBQuery<FIN_PaymentScheduleDetail> paymentdetail = OBDal.getInstance().createQuery(
                FIN_PaymentScheduleDetail.class,
                " invoicePaymentSchedule.id= :scheduleID AND paymentDetails.id is not null");
            paymentdetail.setNamedParameter("scheduleID", sche.getId());
            List<FIN_PaymentScheduleDetail> paymentdetailList = paymentdetail.list();
            log.debug("paymentdetail:" + paymentdetailList.size());
            if (paymentdetailList.size() > 0) {
              for (FIN_PaymentScheduleDetail detail : paymentdetailList) {

                FIN_PaymentDetail paydetails = OBDal.getInstance().get(FIN_PaymentDetail.class,
                    detail.getPaymentDetails().getId());
                paymentId = paydetails.getFinPayment().getId();
                log.debug("paymentId:" + paymentId);

                FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
                log.debug("paymentId:" + payment.getStatus());
                if (payment.getStatus().equals("PWNC") || payment.getStatus().equals("RPPC")) {

                  OBQuery<FIN_FinaccTransaction> tran = OBDal.getInstance()
                      .createQuery(FIN_FinaccTransaction.class, " finPayment.id= :finPaymentID");
                  tran.setNamedParameter("finPaymentID", paymentId);
                  List<FIN_FinaccTransaction> tranList = tran.list();
                  if (tranList.size() > 0) {
                    for (FIN_FinaccTransaction trans : tranList) {
                      FIN_FinaccTransaction transaction = OBDal.getInstance()
                          .get(FIN_FinaccTransaction.class, trans.getId());
                      log.debug("getTotalPaid:" + invoice.getTotalPaid());
                      // checking part payment
                      sql = "select count(*) from fin_finacc_transaction  trx "
                          + "join fin_payment p on p.fin_payment_id = trx.fin_payment_id "
                          + "join fin_payment_detail_v dt on dt.fin_payment_id = p.fin_payment_id "
                          + "join fin_payment_sched_inv_v psd on psd.fin_payment_sched_inv_v_id = dt.fin_payment_sched_inv_v_id "
                          + "where psd.c_invoice_id='" + invoice.getId()
                          + "' and trx.fin_reconciliation_id is not null ";
                      ps = connect.prepareStatement(sql);
                      rs = ps.executeQuery();
                      log.debug("qry:" + sql.toString());

                      while (rs.next()) {
                        log.debug("count:" + rs.getInt("count"));
                        if (rs.getInt("count") == 0) {
                          if (invoice.getTotalPaid().compareTo(BigDecimal.ZERO) > 0
                              && transaction.getReconciliation() == null) {
                            // else if(transaction.getPosted().equals("N")) {
                            transaction.setStatus("EFIN_CAN");
                            OBDal.getInstance().save(transaction);
                            // update the Financial Account current balance
                            FIN_FinancialAccount finact = transaction.getAccount();
                            finact.setCurrentBalance(
                                finact.getCurrentBalance().add(transaction.getPaymentAmount()));
                            OBDal.getInstance().save(finact);
                            log.debug("transaction:" + transaction.getPosted());
                            if (transaction.getPosted().equals("Y")) {
                              // create the reverse journal entries
                              reversejournal = dao.reversejournal(vars, connect, clientId, orgId,
                                  roleId, userId, invoice, transaction);

                              // delete the entries in budget actual
                              // reverseActual = dao.reverseActual(vars, clientId, orgId, roleId,
                              // userId, invoice);
                            }

                            payment.setStatus("EFIN_CAN");
                            OBDal.getInstance().save(payment);
                            FinanancialTransactionCancellation.insertApprovalHistory(vars, "CA",
                                payment, "CA", orgId);

                          }

                          else if (transaction.getPosted().equals("Y")
                              && invoice.isPaymentComplete()
                              && transaction.getReconciliation() != null) {
                            OBError result = OBErrorBuilder.buildMessage(null, "error",
                                "@Efin_PurInv_CannotCancel@");
                            bundle.setResult(result);
                            return;
                          }
                        }

                        else {
                          OBError result = OBErrorBuilder.buildMessage(null, "error",
                              "@Efin_PurInv_CannotCancel@");
                          bundle.setResult(result);
                          return;
                        }
                      }
                    }
                  }
                }

                else {
                  payment.setStatus("EFIN_CAN");
                  FinanancialTransactionCancellation.insertApprovalHistory(vars, "CA", payment,
                      "CA", orgId);
                  OBDal.getInstance().save(payment);

                }
                detail.setCanceled(true);
                detail.setAmount(BigDecimal.ZERO);
                OBDal.getInstance().save(detail);
                OBDal.getInstance().flush();
              }
            }
          }
        }
        // invoice created via the po hold plan
        if (RDV_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
          InvoiceRevokeDAO.releaseTempEncumbrance(invoice);
        }

        if (invoice.isEfinIsreserved()) {
          reserve = dao.reverseReservation(vars, clientId, orgId, roleId, userId, invoice,
              comments);
        }

        // update the invoice Header
        count = dao.updateInvHeader(invoice);

        // remove the prepayment invoice entry in manual encumbrance --> prepayment invoice
        if (PPI_DOCUMENT.equals(strInvoiceType)) {
          count = dao.removePrepayInvoice(invoice);
        }

        // insert the Approval History
        count = PurchaseInvoiceSubmit.insertInvoiceApprover(invoice, comments, "CA", null);

        if (count > 0 && invoice.getDocumentStatus().equals("EFIN_CA")) {
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.AP_INVOICE_RULE);
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PurInv_Cancel@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (

    Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}