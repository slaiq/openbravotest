package sa.elm.ob.finance.ad_process.PaymentOutCancellation;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.Note;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinPoApproval;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;

/**
 * 
 * @author gopalakrishnan on 01-09-2016
 * 
 */
public class PaymentOutCancellation implements Process {
  /**
   * Payment Direct Cancellation from PaymentOut Table(fin_payment)
   * 
   */

  private static final Logger log = Logger.getLogger(PaymentOutCancellation.class);
  private static final String RDV_DOCUMENT = "RDV";
  private static final String PO_DOCUMENT = "POM";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("Payment Direct Cancellation");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      String paymentId = (String) bundle.getParams().get("Fin_Payment_ID");
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      String sql = "";
      PreparedStatement ps = null;
      ResultSet rs = null;
      final String orgId = payment.getOrganization().getId();
      ConnectionProvider conn = bundle.getConnection();

      // Restrict to Cancel the payment if that payment out is already added in financial account
      if (payment.getStatus().equals("PWNC")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AlreadyPreocessed@");
        bundle.setResult(result);
        return;
      }

      // get the details of invoice against payment
      sql = "select pay.amount,inv.c_invoice_id,scd.fin_payment_scheduledetail_id,sc.fin_payment_schedule_id from fin_payment pay  join fin_payment_detail det on det.fin_payment_id=pay.fin_payment_id"
          + " join fin_payment_scheduledetail scd on scd.fin_payment_detail_id=det.fin_payment_detail_id "
          + " join fin_payment_schedule sc on sc.fin_payment_schedule_id=scd.fin_payment_schedule_invoice "
          + " join c_invoice inv on inv.c_invoice_id=sc.c_invoice_id where pay.fin_payment_id='"
          + paymentId + "'";
      ps = conn.getPreparedStatement(sql);
      rs = ps.executeQuery();
      while (rs.next()) {
        FIN_PaymentScheduleDetail scdetail = OBDal.getInstance()
            .get(FIN_PaymentScheduleDetail.class, rs.getString("fin_payment_scheduledetail_id"));
        scdetail.setInvoicePaid(false);
        // scdetail.setAmount(scdetail.getAmount().subtract(new
        // BigDecimal(rs.getString("amount"))));
        // scdetail.setPaymentDetails(null);
        scdetail.setWriteoffAmount(scdetail.getAmount());
        OBDal.getInstance().save(scdetail);
        // clone existing schedule Detail
        FIN_PaymentScheduleDetail objCloneScDetail = (FIN_PaymentScheduleDetail) DalUtil
            .copy(scdetail, false);
        objCloneScDetail.setPaymentDetails(null);
        objCloneScDetail.setWriteoffAmount(BigDecimal.ZERO);
        OBDal.getInstance().save(objCloneScDetail);
        OBDal.getInstance().flush();
        FIN_PaymentSchedule schedule = OBDal.getInstance().get(FIN_PaymentSchedule.class,
            rs.getString("fin_payment_schedule_id"));
        schedule.setPaidAmount(
            schedule.getPaidAmount().subtract(new BigDecimal(rs.getString("amount"))));
        schedule.setOutstandingAmount(
            schedule.getOutstandingAmount().add(new BigDecimal(rs.getString("amount"))));
        OBDal.getInstance().save(schedule);
        OBDal.getInstance().flush();
        Invoice invoice = OBDal.getInstance().get(Invoice.class, rs.getString("c_invoice_id"));
        invoice
            .setTotalPaid(invoice.getTotalPaid().subtract(new BigDecimal(rs.getString("amount"))));
        invoice.setOutstandingAmount(
            invoice.getOutstandingAmount().add(new BigDecimal(rs.getString("amount"))));
        if (invoice.isPaymentComplete()) {
          invoice.setPaymentComplete(false);
        }
        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();
      }
      payment.setStatus("EFIN_CAN");
      insertApprovalHistory(vars, "CA", payment, "CA", orgId);

      // REDUCE paid amt in po contract Task No.7470
      if (payment.getEfinInvoice() != null) {
        String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(payment.getEfinInvoice());
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
            OBInterceptor.setPreventUpdateInfoChange(true);
            List<Order> orderList = PurchaseInvoiceSubmitUtils.getGreaterRevisionOrdList(order);
            if (orderList.size() > 0) {
              for (Order ordObj : orderList) {
                ordObj.setEfinPaidAmt(ordObj.getEfinPaidAmt().subtract(payment.getAmount()));
                OBDal.getInstance().save(ordObj);
              }
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);
            }
          }
        }
      }
      OBDal.getInstance().save(payment);
      OBDal.getInstance().commitAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PaymentCancelled@");
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
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
  private void insertApprovalHistory(VariablesSecureApp vars, String status, FIN_Payment payment,
      String strComments, String inpOrgId) {
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
}
