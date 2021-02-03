package sa.elm.ob.finance.ad_process.PurchaseInvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinInvoicePaymentSch;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.EfinPrepaymentInvoice;
import sa.elm.ob.finance.actionHandler.InvoiceRevokeDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.finance.dms.service.DMSInvoiceService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSInvoiceServiceImpl;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;
/**
 * 
 * @author sathishkumar
 * 
 *         This class will handle the reactivation of purchase invoice
 *
 */

public class PurchaseInvoiceReactivate implements Process {

  private static final Logger log = Logger.getLogger(PurchaseInvoiceReactivate.class);
  private final OBError obError = new OBError();
  private static final String API_DOCUMENT = "API";
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";
  private static final String RDV_DOCUMENT = "RDV";
  private static final String PO_DOCUMENT = "POM";

  @SuppressWarnings("unlikely-arg-type")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    final String orgId = bundle.getContext().getOrganization();
    final String clientId = bundle.getContext().getClient();
    final String userId = bundle.getContext().getUser();
    boolean checkEncumbranceAmountZero = false;
    boolean checkEncumbranceAmountZerofunds = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    final String invoiceId = (String) bundle.getParams().get("C_Invoice_ID");
    ConnectionProvider conn = bundle.getConnection();
    Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
    Currency currency = null;
    BigDecimal grandTotal = BigDecimal.ZERO, conversionRate = BigDecimal.ZERO;
    boolean errorFlag = true;
    String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice), encumid = "";
    int count = 0;

    List<EfinManualEncumInvoice> reservedInvoices = new ArrayList<EfinManualEncumInvoice>();
    List<FIN_Payment> paymentList = invoice.getFINPaymentEMEfinInvoiceIDList();
    List<FIN_Payment> nonCancelledPaymentList = invoice.getFINPaymentEMEfinInvoiceIDList();

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          String strEncumbranceId = "";

          // CheckList before doing reactivate

          if (!"CO".equals(invoice.getDocumentStatus())) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AlreadyPreocessed@");
            bundle.setResult(result);
            return;
          }

          if (paymentList != null) {
            nonCancelledPaymentList = paymentList.stream()
                .filter(a -> !"EFIN_CAN".equals(a.getStatus())).collect(Collectors.toList());
          }

          if ((nonCancelledPaymentList != null && nonCancelledPaymentList.size() > 0)
              || (invoice.isPaymentComplete() && !PPA_DOCUMENT.equals(strInvoiceType))) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_associatedwithpayment@");
            bundle.setResult(result);
            return;
          }

          if ("Y".equals(invoice.getPosted()) && PPA_DOCUMENT.equals(strInvoiceType)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_applicationisposted@");
            bundle.setResult(result);
            return;
          }
          // if (invoice.getTransactionDocument().isEfinIspomatch()) {
          // OBQuery<Order> ord = OBDal.getInstance().createQuery(Order.class,
          // " as e where e.escmOldOrder=:OrderId");
          // ord.setNamedParameter("OrderId", invoice.getEfinCOrder());
          // if (ord.list().size() > 0) {
          // OBError result = OBErrorBuilder.buildMessage(null, "error",
          // "@EFIN_PInvCannotReactivate@");
          // bundle.setResult(result);
          // return;
          // }
          // }

          // if (invoice.getTransactionDocument().isEfinIsrdvinv()) {
          // OBQuery<Order> ord = OBDal.getInstance().createQuery(Order.class,
          // " as e where e.escmOldOrder=:OrderId");
          // ord.setNamedParameter("OrderId", invoice.getSalesOrder());
          // if (ord.list().size() > 0) {
          // OBError result = OBErrorBuilder.buildMessage(null, "error",
          // "@EFIN_PInvCannotReactivate@");
          // bundle.setResult(result);
          // return;
          // }
          // }

          // revert payment schedule changes
          if (invoice.getEFINInvoicePaymentSchList().size() > 0) {
            for (EfinInvoicePaymentSch invoicePayObj : invoice.getEFINInvoicePaymentSchList()) {
              BigDecimal invoiceAmount = invoicePayObj.getInvoiceAmount();
              // get respective PO payment Schedule
              ESCMPaymentSchedule poPaySchObj = invoicePayObj.getEscmPaymentSchedule();
              poPaySchObj.setInvoicedAmt(poPaySchObj.getInvoicedAmt().subtract(invoiceAmount));
              OBDal.getInstance().save(poPaySchObj);
              invoicePayObj
                  .setInvoiceAmount(invoicePayObj.getInvoiceAmount().subtract(invoiceAmount));
              invoicePayObj.setUniquecodeamount(BigDecimal.ZERO);
              invoicePayObj.setDistributionamount(BigDecimal.ZERO);
            }
          }
          if (invoice.getInvoiceLineList().size() > 0) {

            String p_instance_id = null, sql = "";
            PreparedStatement ps = null;
            ResultSet rs = null;

            p_instance_id = SequenceIdData.getUUID();
            String error = "", s = "";

            // insert a record in pinstance
            log.debug("p_instance_id:" + p_instance_id);
            sql = " INSERT INTO ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isactive, ad_user_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby,isprocessing)  "
                + "  VALUES ('" + p_instance_id + "', '111','" + invoice.getId() + "', 'Y','"
                + userId + "','" + clientId + "','" + orgId + "', now(),'" + userId + "', now(),'"
                + userId + "','Y')";
            ps = conn.getPreparedStatement(sql);
            log.debug("ps:" + ps.toString());
            count = ps.executeUpdate();
            log.debug("count:" + count);

            String instanceqry = "select ad_pinstance_id from ad_pinstance where ad_pinstance_id=?";
            PreparedStatement pr = conn.getPreparedStatement(instanceqry);
            pr.setString(1, p_instance_id);
            ResultSet set = pr.executeQuery();

            // call the invoice post
            if (set.next()) {
              sql = " select * from  c_invoice_post(?,?)";
              ps = conn.getPreparedStatement(sql);
              ps.setString(1, p_instance_id);
              ps.setString(2, invoice.getId());
              ps.executeQuery();

              log.debug("count12:" + set.getString("ad_pinstance_id"));

              // get the error msg by using pinstance
              sql = " select result, errormsg from ad_pinstance where ad_pinstance_id='"
                  + p_instance_id + "'";
              ps = conn.getPreparedStatement(sql);
              log.debug("ps12:" + ps.toString());
              rs = ps.executeQuery();
              if (rs.next()) {
                log.debug("result:" + rs.getString("result"));

                if (rs.getString("result").equals("0")) {
                  error = rs.getString("errormsg").replace("@ERROR=", "");
                  log.debug("error:" + error);
                  s = error;
                  int start = s.indexOf("@");
                  int end = s.lastIndexOf("@");
                  log.debug("end:" + end);

                  if (end != 0) {
                    try {
                      log.debug("excep:" + s.substring(start + 1, end));
                      sql = " select  msgtext from ad_message where value ='"
                          + s.substring(start + 1, end) + "' ";
                      ps = conn.getPreparedStatement(sql);
                      log.debug("ps12:" + ps.toString());
                      rs = ps.executeQuery();
                      if (rs.next()) {
                        if (rs.getString("msgtext") != null)
                          throw new OBException(rs.getString("msgtext"));
                      }
                    } catch (Exception e) {
                      error = s;
                      log.error("Error in callInvoicePostProcedure", e);
                    }
                  }
                }
              }
            }
            // insert a record in ApprovalHistory

            Invoice header = OBDal.getInstance().get(Invoice.class, invoice.getId());
            header.setUpdated(new java.util.Date());
            header.setUpdatedBy(OBContext.getOBContext().getUser());
            header.setDocumentStatus("DR");
            header.setEutNextRole(null);
            header.setEfinNextapprovers(null);
            header.setDocumentAction("CO");
            header.setEfinDocaction("CO");
            header.setProcessed(false);

            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();

            // revert invoice amt & Remaining amt Task no 7470
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
                    // ps = conn.getPreparedStatement(
                    // "update c_order set em_efin_invoice_amt=?, em_efin_remaining_amt=? "
                    // + " where c_order_id=?");
                    // ps.setBigDecimal(1,
                    // ordObj.getEfinInvoiceAmt().subtract(invoice.getGrandTotalAmount()));
                    // ps.setBigDecimal(2,
                    // ordObj.getEfinRemainingAmt().add(invoice.getGrandTotalAmount()));
                    // ps.setString(3, ordObj.getId());
                    // ps.executeUpdate();
                    ordObj.setEfinInvoiceAmt(
                        ordObj.getEfinInvoiceAmt().subtract(invoice.getGrandTotalAmount()));
                    ordObj.setEfinRemainingAmt(
                        ordObj.getEfinRemainingAmt().add(invoice.getGrandTotalAmount()));
                    OBDal.getInstance().save(ordObj);
                  }
                  OBDal.getInstance().flush();
                  OBInterceptor.setPreventUpdateInfoChange(false);
                }
              }

            }

            // remove payment plan

            for (FIN_PaymentSchedule paymentPlan : header.getFINPaymentScheduleList()) {
              paymentPlan.setInvoice(null);
              OBDal.getInstance().save(paymentPlan);
            }

            // GET PARENT ORG CURRENCY
            currency = FinanceUtils.getCurrency(orgId, invoice);
            // get conversion rate
            conversionRate = FinanceUtils.getConversionRate(conn.getConnection(), orgId, invoice,
                currency);

            // reduce qty in order for POM.
            if (PO_DOCUMENT.equals(strInvoiceType)) {
              Order order = null;
              if (invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")) {
                for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
                  if (invLine.isEfinIspom()) {
                    if (invoice.getEfinCOrder() != null)
                      order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
                    OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                        invLine.getSalesOrderLine().getId());
                    OBInterceptor.setPreventUpdateInfoChange(true);
                    if (!invLine.isEFINIsTaxLine() || (invLine.isEFINIsTaxLine() && order != null
                        && ((order.isEscmIstax() && invoice.isEfinIstax())
                            || (!order.isEscmIstax() && invoice.isEfinIstax()
                                && invoice.getEfinTaxMethod().isPriceIncludesTax())))) {
                      // ps = conn.getPreparedStatement(
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
                  OBInterceptor.setPreventUpdateInfoChange(true);
                  if (invLine.isEfinIspom() && !invLine.isEFINIsTaxLine()) {
                    OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                        invLine.getSalesOrderLine().getId());
                    // ps = conn.getPreparedStatement(
                    // "update c_orderline set qtyinvoiced=? where c_orderline_id=? ");
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

            // remove if reserve was done.
            if (PO_DOCUMENT.equals(strInvoiceType)) {
              OBContext.setAdminMode();
              invoice.setEfinEncumbranceType("POE");
              OBDal.getInstance().save(invoice);
              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);
              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {

                  OBDal.getInstance().remove(reservedInvoice);
                }
                OBDal.getInstance().flush();
                // check Budget type of invoice is cost, if so delete funds encumbrance

                if ("C".equals(invoice.getEfinBudgetType())) {

                  if (invoice.getEfinFundsEncumbrance() != null) {
                    EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();
                    fundsEncum.setDocumentStatus("DR");
                    if (fundsEncum != null) {
                      String strfundEncumbranceId = fundsEncum.getId();
                      EfinBudgetManencum fundEncumbrance = Utility
                          .getObject(EfinBudgetManencum.class, strfundEncumbranceId);

                      if (fundEncumbrance != null) {

                        for (EfinBudgetManencumlines manencumlines : fundEncumbrance
                            .getEfinBudgetManencumlinesList()) {
                          OBDal.getInstance().remove(manencumlines);
                        }
                      }
                      // Check Encumbrance Amount is Zero Or Negative
                      if (invoice.getEfinFundsEncumbrance() != null)
                        encumLinelist = invoice.getEfinFundsEncumbrance()
                            .getEfinBudgetManencumlinesList();
                      if (encumLinelist.size() > 0)
                        checkEncumbranceAmountZerofunds = UtilityDAO
                            .checkEncumbranceAmountZero(encumLinelist);

                      if (invoice.getEfinManualencumbrance() != null) {
                        EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                            invoice.getEfinManualencumbrance().getId());
                        encumLinelist = encum.getEfinBudgetManencumlinesList();
                      }

                      if (encumLinelist.size() > 0)
                        checkEncumbranceAmountZero = UtilityDAO
                            .checkEncumbranceAmountZero(encumLinelist);

                      if (checkEncumbranceAmountZero) {
                        OBDal.getInstance().rollbackAndClose();
                        OBError result = OBErrorBuilder.buildMessage(null, "error",
                            "@ESCM_Encumamt_Neg@");
                        bundle.setResult(result);
                        return;
                      }
                      invoice.setEfinFundsEncumbrance(null);
                      OBDal.getInstance().save(invoice);
                      OBDal.getInstance().remove(fundEncumbrance);
                      OBDal.getInstance().flush();
                      // fundEncumbrance.getInvoiceEMEfinFundsEncumbranceIDList()
                      // .removeAll(fundEncumbrance.getEfinBudgetManencumlinesList());
                    }
                  }

                }

                if (invoice.getEfinManualencumbrance() != null) {

                  BigDecimal invLineAmt = invoice.getInvoiceLineList().stream()
                      .filter(a -> a.isEfinIspom() == true).map(a -> a.getLineNetAmount())
                      .reduce(BigDecimal.ZERO, BigDecimal::add);

                  Order po = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());

                  // Task 7847: PO to PO Match Invoice not creating split encumbrance
                  // If Invoice encumbrance is different from Order encumbrance, then split case
                  boolean isSplitEncumbrance = false;
                  if (invoice.getEfinManualencumbrance() != null
                      && invoice.getEfinCOrder() != null) {
                    isSplitEncumbrance = PurchaseInvoiceSubmitUtils.isEncumbranceDifferent(
                        invoice.getEfinManualencumbrance().getId(),
                        invoice.getEfinCOrder().getId());
                  }

                  if (invLineAmt.compareTo(po.getGrandTotalAmount()) != 0 || isSplitEncumbrance) {

                    // splitted enum
                    encumid = invoice.getEfinManualencumbrance().getId();
                    EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
                        encumid);

                    if (encumbrance != null) {
                      OBQuery<InvoiceLine> invLine = OBDal.getInstance().createQuery(
                          InvoiceLine.class,
                          "invoice.id= :invoiceID and efinBudgmanuencumln.id is not null ");
                      invLine.setNamedParameter("invoiceID", invoice.getId());
                      List<InvoiceLine> invlineList = invLine.list();
                      if (invlineList != null && invlineList.size() > 0) {
                        for (InvoiceLine lineinv : invlineList) {
                          lineinv.setEfinBudgmanuencumln(null);
                          OBDal.getInstance().save(lineinv);
                        }
                      }
                      OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
                          .createQuery(EfinBudManencumRev.class, " as e where e.sRCManencumline.id "
                              + " in ( select e.id from Efin_Budget_Manencumlines e  where e.manualEncumbrance.id= :encumID)");
                      revQuery.setNamedParameter("encumID", encumbrance.getId());
                      TaxLineHandlerDAO taxDAO = new TaxLineHandlerImpl();
                      Boolean isExclusiveTaxInvoice = taxDAO.isExclusiveTaxInvoice(invoice);
                      Map<String, BigDecimal> taxLinesMap = taxDAO
                          .getTaxLineCodesAndAmount(invoice);
                      String strCombinationId = "";
                      @SuppressWarnings("unused")
                      BigDecimal taxAmount = BigDecimal.ZERO;
                      List<EfinBudManencumRev> revQueryList = revQuery.list();
                      if (revQueryList.size() > 0) {
                        for (EfinBudManencumRev rev : revQueryList) {
                          strCombinationId = rev.getManualEncumbranceLines()
                              .getAccountingCombination().getId();
                          taxAmount = BigDecimal.ZERO;

                          EfinBudgetManencumlines srclines = rev.getSRCManencumline();
                          rev.setSRCManencumline(null);
                          OBDal.getInstance().save(rev);
                          EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();

                          /**
                           * in case if we are taking amt from remaining amt when we dont have
                           * enought amt in app amt then while revoke have to give the amt back to
                           * remaining amt
                           **/
                          Order latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(po);
                          /** encumbrance lines updated amt **/
                          BigDecimal encuUpdateAmt = lines.getSystemUpdatedAmt();
                          /** order total line net amt based on encumbrance line uniquecode **/
                          BigDecimal grandTotalAmt = latestOrder.getOrderLineList().stream()
                              .filter(
                                  a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                                      && a.getEFINUniqueCode().getId()
                                          .equals(lines.getAccountingCombination().getId()))
                              .map(a -> a.getLineNetAmount())
                              .reduce(BigDecimal.ZERO, BigDecimal::add);

                          // Get grand total of other POs linked with the same encumbrance
                          BigDecimal otherPoTotal = InvoiceRevokeDAO.getGrandTotalOtherPO(
                              latestOrder, lines.getAccountingCombination().getId());

                          /**
                           * find the differenct amt which we manually added in modification or
                           * remaining amt
                           **/
                          BigDecimal incAmt = encuUpdateAmt
                              .subtract(grandTotalAmt.add(otherPoTotal));
                          /**
                           * diff amt(enc update amt - po amt) greater than zero and po encumbrance
                           * is manual and diff amt not presented in remaining amt place then have
                           * to give the amt back to in the place of remaining
                           **/
                          if (incAmt.compareTo(BigDecimal.ZERO) > 0
                              && incAmt.compareTo(lines.getRemainingAmount()) != 0
                              && po.getEfinBudgetManencum() != null
                              && po.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                            /** subtract remaining amt in the difference **/
                            incAmt = incAmt.subtract(lines.getRemainingAmount());
                            /**
                             * diff amt compare with reduce amt if greater than and equal add reduce
                             * amt itself in remaining amt not to update the app amt
                             **/
                            if (incAmt.compareTo(rev.getRevamount().negate()) >= 0) {
                              lines.setRemainingAmount(
                                  lines.getRemainingAmount().add(rev.getRevamount().negate()));
                            }
                            /**
                             * diff amt compare with reduce amt if lesser than add diff amt in
                             * remaining amt and update app amt also
                             **/
                            else if (incAmt.compareTo(rev.getRevamount().negate()) < 0) {
                              // BigDecimal updRemaAmt =
                              // incAmt.subtract(lines.getRemainingAmount());
                              lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                              lines.setAPPAmt(lines.getAPPAmt()
                                  .add(rev.getRevamount().negate().subtract(incAmt)));
                            }
                          }
                          /** other cases **/
                          else {
                            lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
                          }

                          // lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));

                          if (isExclusiveTaxInvoice && rev.getManualEncumbranceLines()
                              .getManualEncumbrance().getEncumMethod().equals("M")) {
                            if (taxLinesMap.containsKey(strCombinationId)) {
                              taxAmount = taxLinesMap.get(strCombinationId);

                              // lines.setAPPAmt(lines.getAPPAmt().add(taxAmount.negate()));
                              // lines.setRemainingAmount(lines.getRemainingAmount().add(taxAmount));
                            }
                          }
                          lines.getEfinBudManencumRevList().remove(rev);
                          encumbrance.getEfinBudgetManencumlinesList().remove(srclines);
                        }
                        if (po.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                          PurchaseInvoiceSubmitUtils.removePoMatchExtraBudgetAmount(
                              po.getEfinBudgetManencum(), invoice, null);
                        }
                      }
                      // event.setCurrentState(propencum, null);
                      EfinBudgetManencumv POEncumbrance = Utility
                          .getObject(EfinBudgetManencumv.class, po.getEfinBudgetManencum().getId());
                      invoice.setEfinManualencumbrance(POEncumbrance);
                      invoice.setEfinEncumtype(POEncumbrance.getEncumbranceMethod());
                      invoice.setEfinEncumbranceType(POEncumbrance.getEncumbranceType());
                      OBDal.getInstance().save(invoice);
                      encumbrance.setDocumentStatus("DR");
                      OBDal.getInstance().remove(encumbrance);
                      OBDal.getInstance().flush();
                    }

                  } else {
                    // stage alone revert it.
                    encumid = invoice.getEfinManualencumbrance().getId();
                    EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                        encumid);
                    encum.setEncumStage("POE");
                    OBDal.getInstance().save(encum);

                    // update the encumbrance line id is null
                    for (InvoiceLine line : invoice.getInvoiceLineList()) {
                      line.setEfinBudgmanuencumln(null);
                      OBDal.getInstance().save(line);
                    }
                  }
                }
              }
            }

            // remove if reserve was done.
            if ((invoice.getEfinEncumtype().equals("M")
                && (PPA_DOCUMENT.equals(strInvoiceType) | API_DOCUMENT.equals(strInvoiceType)))
                || (RDV_DOCUMENT.equals(strInvoiceType)
                    && invoice.getEfinManualencumbrance() != null
                    && !invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {
              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);

              int resereved_invoice_size = reservedInvoices.size();

              TaxLineHandlerDAO taxHandler = new TaxLineHandlerImpl();

              if (resereved_invoice_size > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {

                  OBDal.getInstance().remove(reservedInvoice);
                }
                OBDal.getInstance().flush();

                // check Budget type of invoice is cost, if so delete funds encumbrance

                if ("C".equals(invoice.getEfinBudgetType())) {

                  if (invoice.getEfinFundsEncumbrance() != null) {
                    EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();

                    EfinBudgetManencum fundEncumbrance = Utility.getObject(EfinBudgetManencum.class,
                        fundsEncum.getId());
                    fundEncumbrance.setDocumentStatus("DR");
                    OBDal.getInstance().save(fundEncumbrance);

                    if (fundEncumbrance != null) {

                      for (EfinBudgetManencumlines manencumlines : fundEncumbrance
                          .getEfinBudgetManencumlinesList()) {
                        OBDal.getInstance().remove(manencumlines);
                      }
                    }
                    // Check Encumbrance Amount is Zero Or Negative
                    if (invoice.getEfinFundsEncumbrance() != null)
                      encumLinelist = invoice.getEfinFundsEncumbrance()
                          .getEfinBudgetManencumlinesList();
                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZerofunds = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (invoice.getEfinManualencumbrance() != null) {
                      EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                          invoice.getEfinManualencumbrance().getId());
                      encumLinelist = encum.getEfinBudgetManencumlinesList();
                    }

                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZero = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (checkEncumbranceAmountZero) {
                      OBDal.getInstance().rollbackAndClose();
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@ESCM_Encumamt_Neg@");
                      bundle.setResult(result);
                      return;
                    }
                    // empty the reference in invoice for funds
                    invoice.setEfinFundsEncumbrance(null);
                    OBDal.getInstance().save(invoice);
                    OBDal.getInstance().remove(fundsEncum);

                  }

                }

                if (RDV_DOCUMENT.equals(strInvoiceType)) {
                  String encum = invoice.getEfinManualencumbrance().getManualEncumbrance();
                  EfinBudgetManencum manEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      encum);
                  manEncum.setEncumStage("MUS");

                  // Add modification to nullify the already created modification in case of
                  // ministry tax invoice

                  Boolean isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);

                  if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0 && invoice
                      .getEfinRDVTxnList().get(0).getLineTaxamt().compareTo(BigDecimal.ZERO) == 0) {
                    Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
                    taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);

                    if (!taxLinesMap.isEmpty()) {

                      EfinBudgetManencum poEncumbrance = PurchaseInvoiceSubmitUtils
                          .getPoEncumbranceFromInvoice(invoice);
                      PurchaseInvoiceSubmitUtils.addEncumbranceModification(taxLinesMap,
                          poEncumbrance, invoice, Boolean.TRUE);
                      // revert the extra amount which is taken from budget enquiry for tax in po
                      // encum.
                      if (poEncumbrance.getEncumMethod().equals("A")) {
                        PurchaseInvoiceSubmitUtils.removePoExtraAmount(poEncumbrance, invoice,
                            false, null);
                      } else {
                        for (Entry<String, BigDecimal> taxEntries : taxLinesMap.entrySet()) {
                          String strUniqueCodeId = taxEntries.getKey();
                          BigDecimal taxAmount = taxEntries.getValue().setScale(2,
                              RoundingMode.HALF_UP);

                          EfinBudgetManencumlines lines = PurchaseInvoiceSubmitUtils
                              .getEncumbranceLine(poEncumbrance.getId(), strUniqueCodeId);

                          /**
                           * in case if we are taking amt from remaining amt when we dont have
                           * enought amt in app amt then while revoke have to give the amt back to
                           * remaining amt
                           **/
                          Order invoicePO = OBDal.getInstance().get(Order.class,
                              invoice.getSalesOrder().getId());
                          Order latestOrder = PurchaseInvoiceSubmitUtils
                              .getLatestOrderComplete(invoicePO);
                          /** encumbrance lines updated amt **/
                          BigDecimal encuUpdateAmt = lines.getSystemUpdatedAmt();

                          /** order total line net amt based on encumbrance line uniquecode **/
                          BigDecimal grandTotalAmt = latestOrder.getOrderLineList().stream()
                              .filter(
                                  a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                                      && a.getEFINUniqueCode().getId()
                                          .equals(lines.getAccountingCombination().getId()))
                              .map(a -> a.getLineNetAmount())
                              .reduce(BigDecimal.ZERO, BigDecimal::add);

                          // Get grand total of other POs linked with the same encumbrance
                          BigDecimal otherPoTotal = InvoiceRevokeDAO.getGrandTotalOtherPO(
                              latestOrder, lines.getAccountingCombination().getId());

                          /**
                           * find the differenct amt which we manually added in modification or
                           * remaining amt
                           **/
                          BigDecimal incAmt = encuUpdateAmt
                              .subtract(grandTotalAmt.add(otherPoTotal));
                          /**
                           * diff amt(enc update amt - po amt) greater than zero and po encumbrance
                           * is manual and diff amt not presented in remaining amt place then have
                           * to give the amt back to in the place of remaining
                           **/
                          if (incAmt.compareTo(BigDecimal.ZERO) > 0
                              && incAmt.compareTo(lines.getRemainingAmount()) != 0
                              && poEncumbrance != null
                              && poEncumbrance.getEncumMethod().equals("M")) {

                            /** subtract remaining amt in the difference **/
                            incAmt = incAmt.subtract(lines.getRemainingAmount());
                            /**
                             * diff amt compare with tax amt if greater than and equal add reduce
                             * amt itself in remaining amt not to update the app amt
                             **/
                            if (incAmt.compareTo(taxAmount) >= 0) {
                              lines.setRemainingAmount(lines.getRemainingAmount().add(taxAmount));
                              lines.setAPPAmt(lines.getAPPAmt().subtract(taxAmount));
                            }
                            /**
                             * diff amt compare with tax amt if lesser than add diff amt in
                             * remaining amt and update app amt also
                             **/
                            else if (incAmt.compareTo(taxAmount) < 0) {
                              // BigDecimal updRemaAmt =
                              // incAmt.subtract(lines.getRemainingAmount());
                              lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                              lines.setAPPAmt((lines.getAPPAmt().subtract(taxAmount))
                                  .add(taxAmount.subtract(incAmt)));
                            }
                          }
                        }
                      }
                      OBDal.getInstance().flush();
                      OBDal.getInstance().refresh(invoice);

                    }
                  }

                  OBDal.getInstance().save(manEncum);
                  OBDal.getInstance().flush();
                }
              }
            }
            if ((invoice.getEfinEncumtype().equals("A") && API_DOCUMENT.equals(strInvoiceType))
                || (RDV_DOCUMENT.equals(strInvoiceType)
                    && invoice.getEfinManualencumbrance() != null
                    && invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {
              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);
              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {
                  OBDal.getInstance().remove(reservedInvoice);
                }
                OBDal.getInstance().flush();
              }

              if (invoice.isEfinIsreserved() && invoice.getEfinManualencumbrance() != null) {
                strEncumbranceId = invoice.getEfinManualencumbrance().getId();
                EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
                    strEncumbranceId);
                encumbrance.setDocumentStatus("DR");
                OBDal.getInstance().save(encumbrance);
                if (encumbrance != null) {
                  for (EfinBudgetManencumlines manencumlines : encumbrance
                      .getEfinBudgetManencumlinesList()) {
                    OBDal.getInstance().remove(manencumlines);
                  }

                }

                if (invoice.getEfinFundsEncumbrance() != null) {
                  String strfundEncumbranceId = invoice.getEfinFundsEncumbrance().getId();
                  EfinBudgetManencum fundEncumbrance = Utility.getObject(EfinBudgetManencum.class,
                      strfundEncumbranceId);
                  fundEncumbrance.setDocumentStatus("DR");
                  OBDal.getInstance().save(fundEncumbrance);
                  if (fundEncumbrance != null) {

                    for (EfinBudgetManencumlines manencumlines : fundEncumbrance
                        .getEfinBudgetManencumlinesList()) {
                      OBDal.getInstance().remove(manencumlines);
                    }
                  }
                  // Check Encumbrance Amount is Zero Or Negative
                  if (invoice.getEfinFundsEncumbrance() != null)
                    encumLinelist = invoice.getEfinFundsEncumbrance()
                        .getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZerofunds = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (invoice.getEfinManualencumbrance() != null) {
                    EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                        invoice.getEfinManualencumbrance().getId());
                    encumLinelist = encum.getEfinBudgetManencumlinesList();
                  }

                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  invoice.setEfinFundsEncumbrance(null);
                  OBDal.getInstance().save(invoice);
                  OBDal.getInstance().remove(fundEncumbrance);

                }
                invoice.setEfinManualencumbrance(null);
                OBDal.getInstance().save(invoice);
                OBDal.getInstance().remove(encumbrance);
                OBDal.getInstance().flush();
              }
            }
            // invoice created through po hold plan
            if (RDV_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
              InvoiceRevokeDAO.releaseTempEncumbrance(invoice);
            }
            if (invoice.getEfinManualencumbrance() != null && invoice.getEfinDistribution() != null
                && PPI_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
              grandTotal = invoice.getGrandTotalAmount();
              grandTotal = FinanceUtils.getConvertedAmount(grandTotal, conversionRate);

              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);

              OBQuery<EfinPrepaymentInvoice> prepayinv = OBDal.getInstance().createQuery(
                  EfinPrepaymentInvoice.class,
                  " manualEncumbrance.id in ( select inv.efinManualencumbrance.id "
                      + " from  Invoice inv  where inv.id= :invoiceID ) ");
              prepayinv.setNamedParameter("invoiceID", invoice.getId());
              List<EfinPrepaymentInvoice> prepayinvList = prepayinv.list();
              if (prepayinvList.size() > 0) {
                for (EfinPrepaymentInvoice pre : prepayinvList) {
                  OBDal.getInstance().remove(pre);
                }
              }

              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {
                  OBDal.getInstance().remove(reservedInvoice);
                }
              }

              if ("A".equals(invoice.getEfinEncumtype())) {
                strEncumbranceId = invoice.getEfinManualencumbrance().getId();
                EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
                    strEncumbranceId);
                encumbrance.setDocumentStatus("DR");

                if (encumbrance != null) {
                  for (EfinBudgetManencumlines manencumlines : encumbrance
                      .getEfinBudgetManencumlinesList()) {
                    OBDal.getInstance().remove(manencumlines);
                  }
                }

                invoice.setEfinManualencumbrance(null);
                OBDal.getInstance().save(invoice);
                OBDal.getInstance().remove(encumbrance);
              }

              OBDal.getInstance().flush();
            }

            if (PPA_DOCUMENT.equals(strInvoiceType)) {
              if (invoice.getEfinEncumtype().equals("M")) {
                PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionRate,
                    true);
                PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(),
                    conversionRate, true);

              } else {
                PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionRate,
                    true);
                PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(),
                    conversionRate, true);
              }

            }

            header.setEfinIsreserved(false);
            OBDal.getInstance().save(header);
            count = 0;
            // insert a record in ApprovalHistory
            count = PurchaseInvoiceSubmit.insertInvoiceApprover(invoice, "", "REACT", null);

            // Check Encumbrance Amount is Zero Or Negative
            if (invoice.getEfinFundsEncumbrance() != null)
              encumLinelist = invoice.getEfinFundsEncumbrance().getEfinBudgetManencumlinesList();
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZerofunds = UtilityDAO
                  .checkEncumbranceAmountZero(encumLinelist);

            if (invoice.getEfinManualencumbrance() != null) {
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  invoice.getEfinManualencumbrance().getId());
              encumLinelist = encum.getEfinBudgetManencumlinesList();
            }

            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (checkEncumbranceAmountZero) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
              bundle.setResult(result);
              return;
            }

            if (count > 0) {

              try {
                // DMS integration
                DMSInvoiceService dmsService = new DMSInvoiceServiceImpl();
                dmsService.rejectAndReactivateOperations(invoice);
              } catch (Exception e) {

              }

              OBError result = OBErrorBuilder.buildMessage(null, "Success",
                  "@Efin_invrectsuccess@");
              bundle.setResult(result);
              return;
            }

          }
        } catch (OBException e) {
          log.debug("Exeception in Reactivating invoice:" + e);
          throw new OBException(e.getMessage());
        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);
          OBDal.getInstance().rollbackAndClose();
        }
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (OBException e) {
      log.debug("Exeception in Reactivating invoice:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("Exeception in Reactivating invoice:", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
