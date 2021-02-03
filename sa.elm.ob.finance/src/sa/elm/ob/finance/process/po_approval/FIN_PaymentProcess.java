/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package sa.elm.ob.finance.process.po_approval;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.exception.NoExecutionProcessFoundException;
import org.openbravo.advpaymentmngt.process.FIN_ExecutePayment;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Payment_Credit;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinPoApproval;
import sa.elm.ob.finance.dao.AdvPaymentMngtDao;

public class FIN_PaymentProcess implements org.openbravo.scheduling.Process {
  private static AdvPaymentMngtDao dao;

  public BigDecimal ZERO = BigDecimal.ZERO;

  @SuppressWarnings("static-access")
  public void execute(ProcessBundle bundle) throws Exception {
    dao = new AdvPaymentMngtDao();
    final String language = bundle.getContext().getLanguage();

    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(Utility.messageBD(bundle.getConnection(), "Success", language));

    try {
      // retrieve custom params
      final String strAction = (String) bundle.getParams().get("action");
      // retrieve standard params
      final String recordID = (String) bundle.getParams().get("Fin_Payment_ID");
      final FIN_Payment payment = dao.getObject(FIN_Payment.class, recordID);
      final VariablesSecureApp vars = bundle.getContext().toVars();

      final ConnectionProvider conProvider = bundle.getConnection();
      final boolean isReceipt = payment.isReceipt();
      if (strAction.equals("P") || strAction.equals("D")) {
        if (payment.getBusinessPartner() != null) {
          if (FIN_Utility.isBlockedBusinessPartner(payment.getBusinessPartner().getId(), isReceipt,
              4)) {
            // If the Business Partner is blocked for Payments, the Payment will not be completed.
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(conProvider, "Error", language));
            msg.setMessage(OBMessageUtils.messageBD("ThebusinessPartner") + " "
                + payment.getBusinessPartner().getIdentifier() + " "
                + OBMessageUtils.messageBD("BusinessPartnerBlocked"));
            bundle.setResult(msg);
            OBDal.getInstance().rollbackAndClose();
            return;
          }
        } else {
          OBContext.setAdminMode(true);
          try {
            for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
              for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
                BusinessPartner bPartner = null;
                if (psd.getInvoicePaymentSchedule() != null) {
                  bPartner = psd.getInvoicePaymentSchedule().getInvoice().getBusinessPartner();
                } else if (psd.getOrderPaymentSchedule() != null) {
                  bPartner = psd.getOrderPaymentSchedule().getOrder().getBusinessPartner();
                }
                if (bPartner != null && FIN_Utility.isBlockedBusinessPartner(bPartner.getId(),
                    payment.isReceipt(), 4)) {
                  // If the Business Partner is blocked for Payments, the Payment will not be
                  // completed.
                  msg.setType("Error");
                  msg.setTitle(Utility.messageBD(conProvider, "Error", language));
                  msg.setMessage(OBMessageUtils.messageBD("ThebusinessPartner") + " "
                      + bPartner.getIdentifier() + " "
                      + OBMessageUtils.messageBD("BusinessPartnerBlocked"));
                  bundle.setResult(msg);
                  OBDal.getInstance().rollbackAndClose();
                  return;
                }
              }
            }
          } finally {
            OBContext.restorePreviousMode();
          }
        }
      }

      OBDal.getInstance().flush();
      if (strAction.equals("P") || strAction.equals("D")) {
        // Guess if this is a refund payment
        boolean isRefund = false;
        OBContext.setAdminMode(false);
        try {
          if (payment.getFINPaymentDetailList().size() > 0) {
            for (FIN_PaymentDetail det : payment.getFINPaymentDetailList()) {
              if (det.isRefund()) {
                isRefund = true;
                break;
              }
            }
          }
        } finally {
          OBContext.restorePreviousMode();
        }
        if (!isRefund) {
          // Undo Used credit as it will be calculated again
          payment.setUsedCredit(BigDecimal.ZERO);
          OBDal.getInstance().save(payment);
        }
        // Set APRM_Ready preference
        if (vars.getSessionValue("APRMT_MigrationToolRunning", "N").equals("Y")
            && !dao.existsAPRMReadyPreference()) {
          dao.createAPRMReadyPreference();
        }

        boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(payment.TABLE_NAME,
            payment.getId(), payment.TABLE_NAME + "_ID", "LE");
        boolean documentEnabled = getDocumentConfirmation(conProvider, payment.getId());
        if (documentEnabled && !FIN_Utility.isPeriodOpen(payment.getClient().getId(),
            payment.getDocumentType().getDocumentCategory(), payment.getOrganization().getId(),
            OBDateUtils.formatDate(payment.getPaymentDate())) && orgLegalWithAccounting) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(
              Utility.parseTranslation(conProvider, vars, language, "@PeriodNotAvailable@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }
        Set<String> documentOrganizations = OBContext.getOBContext()
            .getOrganizationStructureProvider(payment.getClient().getId())
            .getNaturalTree(payment.getOrganization().getId());
        if (!documentOrganizations.contains(payment.getAccount().getOrganization().getId())) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@APRM_FinancialAccountNotInNaturalTree@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }
        Set<String> invoiceDocNos = new TreeSet<String>();
        Set<String> orderDocNos = new TreeSet<String>();
        Set<String> glitems = new TreeSet<String>();
        BigDecimal paymentAmount = BigDecimal.ZERO;
        BigDecimal paymentWriteOfAmount = BigDecimal.ZERO;

        // removed when new security implementation is done
        OBContext.setAdminMode();
        try {
          String strRefundCredit = "";
          // update payment schedule amount
          List<FIN_PaymentDetail> paymentDetails = payment.getFINPaymentDetailList();

          // Show error message when payment has no lines
          if (paymentDetails.size() == 0) {
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(conProvider, "Error", language));
            msg.setMessage(
                Utility.parseTranslation(conProvider, vars, language, "@APRM_PaymentNoLines@"));
            bundle.setResult(msg);
            OBDal.getInstance().rollbackAndClose();
            return;
          }
          for (FIN_PaymentDetail paymentDetail : paymentDetails) {
            for (FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                .getFINPaymentScheduleDetailList()) {
              paymentAmount = paymentAmount.add(paymentScheduleDetail.getAmount());
              BigDecimal writeoff = paymentScheduleDetail.getWriteoffAmount();
              if (writeoff == null)
                writeoff = BigDecimal.ZERO;
              paymentWriteOfAmount = paymentWriteOfAmount.add(writeoff);
              if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                final Invoice invoice = paymentScheduleDetail.getInvoicePaymentSchedule()
                    .getInvoice();
                invoiceDocNos
                    .add(FIN_Utility.getDesiredDocumentNo(payment.getOrganization(), invoice));
              }
              if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
                orderDocNos.add(
                    paymentScheduleDetail.getOrderPaymentSchedule().getOrder().getDocumentNo());
              }
              if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
                  && paymentScheduleDetail.getOrderPaymentSchedule() == null
                  && paymentScheduleDetail.getPaymentDetails().getGLItem() == null) {
                if (paymentDetail.isRefund())
                  strRefundCredit = Utility.messageBD(conProvider, "APRM_RefundAmount", language);
                else {
                  strRefundCredit = Utility.messageBD(conProvider, "APRM_CreditAmount", language);
                  payment.setGeneratedCredit(paymentDetail.getAmount());
                }
                strRefundCredit += ": " + paymentDetail.getAmount().toString();
              }
            }
            if (paymentDetail.getGLItem() != null)
              glitems.add(paymentDetail.getGLItem().getName());
          }
          // Set description
          if (bundle.getParams().get("isPOSOrder") == null
              || !bundle.getParams().get("isPOSOrder").equals("Y")) {
            StringBuffer description = new StringBuffer();

            if (payment.getDescription() != null && !payment.getDescription().equals(""))
              description.append(payment.getDescription()).append("\n");
            if (!invoiceDocNos.isEmpty()) {
              description.append(Utility.messageBD(conProvider, "InvoiceDocumentno", language));
              description.append(": ").append(
                  invoiceDocNos.toString().substring(1, invoiceDocNos.toString().length() - 1));
              description.append("\n");
            }
            if (!orderDocNos.isEmpty()) {
              description.append(Utility.messageBD(conProvider, "OrderDocumentno", language));
              description.append(": ")
                  .append(orderDocNos.toString().substring(1, orderDocNos.toString().length() - 1));
              description.append("\n");
            }
            if (!glitems.isEmpty()) {
              description.append(Utility.messageBD(conProvider, "APRM_GLItem", language));
              description.append(": ")
                  .append(glitems.toString().substring(1, glitems.toString().length() - 1));
              description.append("\n");
            }
            if (!"".equals(strRefundCredit))
              description.append(strRefundCredit).append("\n");

            String truncateDescription = (description.length() > 255)
                ? description.substring(0, 251).concat("...").toString()
                : description.toString();
            payment.setDescription(truncateDescription);
          }

          if (paymentAmount.compareTo(payment.getAmount()) != 0) {
            payment.setUsedCredit(paymentAmount.subtract(payment.getAmount()));
          }
          if (payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
            updateUsedCredit(payment);
          }

          payment.setWriteoffAmount(paymentWriteOfAmount);
          payment.setProcessed(true);
          payment.setAPRMProcessPayment("RE");
          if (payment.getGeneratedCredit() == null) {
            payment.setGeneratedCredit(BigDecimal.ZERO);
          }
          if (BigDecimal.ZERO.compareTo(payment.getUsedCredit()) != 0
              || BigDecimal.ZERO.compareTo(payment.getGeneratedCredit()) != 0) {
            BusinessPartner businessPartner = payment.getBusinessPartner();
            if (businessPartner == null) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@APRM_CreditWithoutBPartner@"));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
            String currency = null;
            if (businessPartner.getCurrency() == null) {
              currency = payment.getCurrency().getId();
              businessPartner.setCurrency(payment.getCurrency());
            } else {
              currency = businessPartner.getCurrency().getId();
            }
            if (!payment.getCurrency().getId().equals(currency)) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(String.format(
                  Utility.parseTranslation(conProvider, vars, language, "@APRM_CreditCurrency@"),
                  businessPartner.getCurrency().getISOCode()));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
          }
          // Execution Process
          if (dao.isAutomatedExecutionPayment(payment.getAccount(), payment.getPaymentMethod(),
              payment.isReceipt())) {
            try {
              payment.setStatus("RPAE");

              if (dao.hasNotDeferredExecutionProcess(payment.getAccount(),
                  payment.getPaymentMethod(), payment.isReceipt())) {
                PaymentExecutionProcess executionProcess = dao.getExecutionProcess(payment);
                if (dao.isAutomaticExecutionProcess(executionProcess)) {
                  final List<FIN_Payment> payments = new ArrayList<FIN_Payment>(1);
                  payments.add(payment);
                  FIN_ExecutePayment executePayment = new FIN_ExecutePayment();
                  executePayment.init("APP", executionProcess, payments, null,
                      payment.getOrganization());
                  OBError result = executePayment.execute();
                  if ("Error".equals(result.getType())) {
                    msg.setType("Warning");
                    msg.setMessage(
                        Utility.parseTranslation(conProvider, vars, language, result.getMessage()));
                  } else if (!"".equals(result.getMessage())) {
                    String execProcessMsg = Utility.parseTranslation(conProvider, vars, language,
                        result.getMessage());
                    if (!"".equals(msg.getMessage()))
                      msg.setMessage(msg.getMessage() + "<br>");
                    msg.setMessage(msg.getMessage() + execProcessMsg);
                  }
                }
              }
            } catch (final NoExecutionProcessFoundException e) {
              e.printStackTrace(System.err);
              msg.setType("Warning");
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@NoExecutionProcessFound@"));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            } catch (final Exception e) {
              e.printStackTrace(System.err);
              msg.setType("Warning");
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@IssueOnExecutionProcess@"));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
          } else {
            BusinessPartner businessPartner = payment.getBusinessPartner();
            // When credit is used (consumed) we compensate so_creditused as this amount is already
            // included in the payment details. Credit consumed should not affect to so_creditused
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
              if (isReceipt) {
                increaseCustomerCredit(businessPartner, payment.getUsedCredit());
              } else {
                decreaseCustomerCredit(businessPartner, payment.getUsedCredit());
              }
            }
            for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
              // Get payment schedule detail list ordered by amount asc.
              // First negative if they exist and then positives
              OBCriteria<FIN_PaymentScheduleDetail> obcPSD = OBDal.getInstance()
                  .createCriteria(FIN_PaymentScheduleDetail.class);
              obcPSD.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS,
                  paymentDetail));
              obcPSD.addOrderBy(FIN_PaymentScheduleDetail.PROPERTY_AMOUNT, true);

              for (FIN_PaymentScheduleDetail paymentScheduleDetail : obcPSD.list()) {
                BigDecimal amount = paymentScheduleDetail.getAmount()
                    .add(paymentScheduleDetail.getWriteoffAmount());
                // Do not restore paid amounts if the payment is awaiting execution.
                boolean invoicePaidAmounts = (FIN_Utility
                    .seqnumberpaymentstatus(isReceipt ? "RPR" : "PPM")) >= (FIN_Utility
                        .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
                paymentScheduleDetail.setInvoicePaid(false);
                // Payment = 0 when the payment is generated by a invoice that consume credit
                if (invoicePaidAmounts
                    | (payment.getAmount().compareTo(new BigDecimal("0.00")) == 0)) {
                  if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                    // BP SO_CreditUsed
                    businessPartner = paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                        .getBusinessPartner();

                    // Payments update credit opposite to invoices
                    BigDecimal paidAmount = BigDecimal.ZERO;
                    Invoice invoiceForConversion = paymentScheduleDetail
                        .getInvoicePaymentSchedule() != null
                            ? paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                            : null;
                    paidAmount = BigDecimal.ZERO;
                    String fromCurrency = payment.getCurrency().getId();
                    String toCurrency = businessPartner.getCurrency().getId();
                    if (!fromCurrency.equals(toCurrency)) {
                      BigDecimal exchangeRate = BigDecimal.ZERO;
                      // check at invoice document level
                      List<ConversionRateDoc> conversionRateDocumentForInvoice = getConversionRateDocumentForInvoice(
                          invoiceForConversion, isReceipt);
                      if (conversionRateDocumentForInvoice.size() > 0) {
                        exchangeRate = conversionRateDocumentForInvoice.get(0).getRate();
                      } else {
                        // global
                        exchangeRate = getConversionRate(payment.getOrganization().getId(),
                            fromCurrency, toCurrency,
                            invoiceForConversion != null ? invoiceForConversion.getInvoiceDate()
                                : payment.getPaymentDate());
                      }
                      if (exchangeRate == BigDecimal.ZERO) {
                        msg.setType("Error");
                        msg.setTitle(Utility.messageBD(conProvider, "Error", language));
                        msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                            "@NoCurrencyConversion@"));
                        bundle.setResult(msg);
                        OBDal.getInstance().rollbackAndClose();
                        return;
                      }
                      paidAmount = amount.multiply(exchangeRate);
                    } else {
                      paidAmount = amount;
                    }
                    if (isReceipt) {
                      decreaseCustomerCredit(businessPartner, paidAmount);
                    } else {
                      increaseCustomerCredit(businessPartner, paidAmount);
                    }
                    FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                        paymentScheduleDetail.getInvoicePaymentSchedule(),
                        paymentScheduleDetail.getAmount(),
                        paymentScheduleDetail.getWriteoffAmount());
                    paymentScheduleDetail.setInvoicePaid(true);
                  }

                  if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
                    FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                        paymentScheduleDetail.getOrderPaymentSchedule(),
                        paymentScheduleDetail.getAmount(),
                        paymentScheduleDetail.getWriteoffAmount());
                  }
                  // when generating credit for a BP SO_CreditUsed is also updated
                  if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
                      && paymentScheduleDetail.getOrderPaymentSchedule() == null
                      && paymentScheduleDetail.getPaymentDetails().getGLItem() == null
                      && !paymentDetail.isRefund()) {
                    // BP SO_CreditUsed
                    if (isReceipt) {
                      decreaseCustomerCredit(businessPartner, amount);
                    } else {
                      increaseCustomerCredit(businessPartner, amount);
                    }
                  }
                }
              }
            }
            payment.setStatus(isReceipt ? "RPR" : "PPM");

            if ((strAction.equals("D") || FIN_Utility.isAutomaticDepositWithdrawn(payment))
                && payment.getAmount().compareTo(BigDecimal.ZERO) != 0) {
              OBError result = triggerAutomaticFinancialAccountTransaction(vars, conProvider,
                  payment);
              if ("Error".equals(result.getType())) {
                OBDal.getInstance().rollbackAndClose();
                bundle.setResult(result);
                return;
              }
            }
          }
          if (!payment.getAccount().getCurrency().equals(payment.getCurrency())
              && getConversionRateDocument(payment).size() == 0) {
            insertConversionRateDocument(payment);
          }
        } finally {
          OBDal.getInstance().flush();
          OBContext.restorePreviousMode();
        }

        // ***********************
        // Reverse Payment
        // ***********************
      } else if (strAction.equals("RV")) {
        /*
         * if(payment.getQmrCheckleaf() != null) {
         * if(StringUtils.equals(payment.getQmrCheckleaf().getStatus(), "P") ||
         * StringUtils.equals(payment.getQmrCheckleaf().getStatus(), "I")) {
         * payment.getQmrCheckleaf().setStatus("C");
         * payment.getQmrCheckleaf().setCancelledddate(date); } }
         */
        FIN_Payment reversedPayment = (FIN_Payment) DalUtil.copy(payment, false);
        final String paymentDate = (String) bundle.getParams().get("paymentdate");
        OBContext.setAdminMode();
        try {
          if (BigDecimal.ZERO.compareTo(payment.getGeneratedCredit()) != 0
              && BigDecimal.ZERO.compareTo(payment.getUsedCredit()) != 0) {
            throw new OBException("@APRM_CreditConsumed@");
          } else if (BigDecimal.ZERO.compareTo(payment.getGeneratedCredit()) != 0
              && BigDecimal.ZERO.compareTo(payment.getUsedCredit()) == 0) {
            reversedPayment.setUsedCredit(payment.getGeneratedCredit());
            reversedPayment.setGeneratedCredit(BigDecimal.ZERO);
          } else {
            reversedPayment.setUsedCredit(BigDecimal.ZERO);
            reversedPayment.setGeneratedCredit(BigDecimal.ZERO);
          }
          reversedPayment.setDocumentNo(
              "*R*" + FIN_Utility.getDocumentNo(payment.getDocumentType(), "FIN_Payment"));
          reversedPayment.setPaymentDate(FIN_Utility.getDate(paymentDate));
          reversedPayment.setDescription("");
          reversedPayment.setProcessed(false);
          reversedPayment.setPosted("N");
          reversedPayment.setProcessNow(false);
          reversedPayment.setAPRMProcessPayment("P");
          reversedPayment.setStatus("RPAP");
          // reversedPayment.setQmrCheckleaf(payment.getQmrCheckleaf());
          // reversedPayment.setQmrChequeno(payment.getQmrChequeno());
          // reversedPayment.setQMLTDSProcessed(false);

          // Amounts
          reversedPayment.setAmount(payment.getAmount().negate());
          reversedPayment.setWriteoffAmount(payment.getWriteoffAmount().negate());
          reversedPayment
              .setFinancialTransactionAmount(payment.getFinancialTransactionAmount().negate());
          OBDal.getInstance().save(reversedPayment);

          List<FIN_PaymentDetail> reversedDetails = new ArrayList<FIN_PaymentDetail>();

          OBDal.getInstance().save(reversedPayment);
          List<FIN_Payment_Credit> credits = payment.getFINPaymentCreditList();

          for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
            FIN_PaymentDetail reversedPaymentDetail = (FIN_PaymentDetail) DalUtil.copy(pd, false);
            reversedPaymentDetail.setFinPayment(reversedPayment);
            reversedPaymentDetail.setAmount(pd.getAmount().negate());
            reversedPaymentDetail.setWriteoffAmount(pd.getWriteoffAmount().negate());
            if (pd.isRefund()) {
              reversedPaymentDetail.setPrepayment(true);
              reversedPaymentDetail.setRefund(false);
              reversedPayment
                  .setGeneratedCredit(reversedPayment.getGeneratedCredit().add(pd.getAmount()));
              credits = new ArrayList<FIN_Payment_Credit>();
              OBDal.getInstance().save(reversedPayment);
            } else if (pd.isPrepayment()
                && pd.getFINPaymentScheduleDetailList().get(0).getOrderPaymentSchedule() == null) {
              reversedPaymentDetail.setPrepayment(true);
              reversedPaymentDetail.setRefund(true);
            }
            List<FIN_PaymentScheduleDetail> reversedSchedDetails = new ArrayList<FIN_PaymentScheduleDetail>();
            OBDal.getInstance().save(reversedPaymentDetail);
            // Create or update PSD of orders and invoices to set the new outstanding amount
            for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
              if (psd.getInvoicePaymentSchedule() != null
                  || psd.getOrderPaymentSchedule() != null) {
                OBCriteria<FIN_PaymentScheduleDetail> unpaidSchedDet = OBDal.getInstance()
                    .createCriteria(FIN_PaymentScheduleDetail.class);
                if (psd.getInvoicePaymentSchedule() != null)
                  unpaidSchedDet.add(
                      Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
                          psd.getInvoicePaymentSchedule()));
                if (psd.getOrderPaymentSchedule() != null)
                  unpaidSchedDet
                      .add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE,
                          psd.getOrderPaymentSchedule()));
                unpaidSchedDet
                    .add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
                List<FIN_PaymentScheduleDetail> openPSDs = unpaidSchedDet.list();
                // If invoice/order not fully paid, update outstanding amount
                if (openPSDs.size() > 0) {
                  FIN_PaymentScheduleDetail openPSD = openPSDs.get(0);
                  BigDecimal openAmount = openPSD.getAmount()
                      .add(payment.getAmount().add(payment.getWriteoffAmount()));
                  if (openAmount.compareTo(BigDecimal.ZERO) == 0) {
                    OBDal.getInstance().remove(openPSD);
                  } else {
                    openPSD.setAmount(openAmount);
                  }
                } else {
                  // If invoice is fully paid create a new schedule detail.
                  FIN_PaymentScheduleDetail openPSD = (FIN_PaymentScheduleDetail) DalUtil.copy(psd,
                      false);
                  openPSD.setPaymentDetails(null);
                  // Amounts
                  openPSD.setWriteoffAmount(BigDecimal.ZERO);
                  openPSD.setAmount(pd.getAmount().add(pd.getWriteoffAmount()));

                  openPSD.setCanceled(false);
                  OBDal.getInstance().save(openPSD);
                }
              }

              FIN_PaymentScheduleDetail reversedPaymentSchedDetail = (FIN_PaymentScheduleDetail) DalUtil
                  .copy(psd, false);
              reversedPaymentSchedDetail.setPaymentDetails(reversedPaymentDetail);
              // Amounts
              reversedPaymentSchedDetail.setWriteoffAmount(psd.getWriteoffAmount().negate());
              reversedPaymentSchedDetail.setAmount(psd.getAmount().negate());
              OBDal.getInstance().save(reversedPaymentSchedDetail);
              reversedSchedDetails.add(reversedPaymentSchedDetail);

              if ((FIN_Utility.invoicePaymentStatus(reversedPayment)
                  .equals(reversedPayment.getStatus()))) {
                reversedPaymentSchedDetail.setInvoicePaid(true);

              } else {
                reversedPaymentSchedDetail.setInvoicePaid(false);
              }
              OBDal.getInstance().save(reversedPaymentSchedDetail);

            }

            reversedPaymentDetail.setFINPaymentScheduleDetailList(reversedSchedDetails);
            OBDal.getInstance().save(reversedPaymentDetail);
            reversedDetails.add(reversedPaymentDetail);
          }
          reversedPayment.setFINPaymentDetailList(reversedDetails);
          OBDal.getInstance().save(reversedPayment);

          List<FIN_Payment_Credit> reversedCredits = new ArrayList<FIN_Payment_Credit>();
          for (FIN_Payment_Credit pc : credits) {
            FIN_Payment_Credit reversedPaymentCredit = (FIN_Payment_Credit) DalUtil.copy(pc, false);
            reversedPaymentCredit.setAmount(pc.getAmount().negate());
            reversedPaymentCredit.setCreditPaymentUsed(pc.getCreditPaymentUsed());
            pc.getCreditPaymentUsed().setUsedCredit(
                pc.getCreditPaymentUsed().getUsedCredit().add(pc.getAmount().negate()));
            reversedPaymentCredit.setPayment(reversedPayment);
            OBDal.getInstance().save(pc.getCreditPaymentUsed());
            OBDal.getInstance().save(reversedPaymentCredit);
            reversedCredits.add(reversedPaymentCredit);
          }

          reversedPayment.setFINPaymentCreditList(reversedCredits);
          OBDal.getInstance().save(reversedPayment);

          List<ConversionRateDoc> conversions = new ArrayList<ConversionRateDoc>();
          for (ConversionRateDoc cr : payment.getCurrencyConversionRateDocList()) {
            ConversionRateDoc reversedCR = (ConversionRateDoc) DalUtil.copy(cr, false);
            reversedCR.setForeignAmount(cr.getForeignAmount().negate());
            reversedCR.setPayment(reversedPayment);
            OBDal.getInstance().save(reversedCR);
            conversions.add(reversedCR);
          }
          reversedPayment.setCurrencyConversionRateDocList(conversions);
          OBDal.getInstance().save(reversedPayment);

          OBDal.getInstance().flush();
        } finally {
          OBContext.restorePreviousMode();
        }

        payment.setReversedPayment(reversedPayment);
        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();

        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("Fin_Payment_ID", reversedPayment.getId());
        parameterMap.put("action", "P");
        parameterMap.put("isReversedPayment", "Y");
        bundle.setParams(parameterMap);
        execute(bundle);

        return;

        // ***********************
        // Reactivate Payment
        // ***********************
      } else if (strAction.equals("R") || strAction.equals("RE")) {
        // Already Posted Document
        if ("Y".equals(payment.getPosted())) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@PostedDocument@" + ": " + payment.getDocumentNo()));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }
        // Reversed Payment
        if (payment.getReversedPayment() != null) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(
              Utility.parseTranslation(conProvider, vars, language, "@APRM_PaymentReversed@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }
        // Reverse Payment
        if (strAction.equals("RE") && FIN_Utility.isReversePayment(payment)) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(
              Utility.parseTranslation(conProvider, vars, language, "@APRM_ReversePayment@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }
        if (strAction.equals("R") && FIN_Utility.isReversePayment(payment)) {
          if (!OBContext.getOBContext().getUser().getId().equals("100")) {
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(conProvider, "Error", language));
            msg.setMessage(
                Utility.parseTranslation(conProvider, vars, language, "@APRM_ReversePayment@"));
            bundle.setResult(msg);
            OBDal.getInstance().rollbackAndClose();
            return;
          }
        }

        // Do not reactive the payment if it is tax payment
        if (payment.getFinancialMgmtTaxPaymentList().size() != 0) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@APRM_TaxPaymentReactivation@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }

        // Transaction exists
        if (hasTransaction(payment)) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(
              Utility.parseTranslation(conProvider, vars, language, "@APRM_TransactionExists@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }
        // Payment with generated credit already used on other payments.
        if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 1
            && payment.getUsedCredit().compareTo(BigDecimal.ZERO) == 1) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
              "@APRM_PaymentGeneratedCreditIsUsed@"));
          bundle.setResult(msg);
          OBDal.getInstance().rollbackAndClose();
          return;
        }

        /*
         * // Do not reactive the payment if it is TDSG/L Item if(payment.isQMLTDSProcessed() ==
         * true) { msg.setType("Error"); msg.setTitle(Utility.messageBD(conProvider, "Error",
         * language)); msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
         * "@QPOE_TdsPayment@")); bundle.setResult(msg); OBDal.getInstance().rollbackAndClose();
         * return; } if(payment.isQMLTDSProcessed() == false) { try { OBCriteria<QMLTDSDetails>
         * TdsHeaderlist = OBDal.getInstance().createCriteria(QMLTDSDetails.class);
         * TdsHeaderlist.add(Restrictions.eq(QMLTDSDetails.PROPERTY_PAYMENT + ".id",
         * payment.getId())); if(TdsHeaderlist != null && TdsHeaderlist.list().size() > 0) { for
         * (QMLTDSDetails tdsHeader : TdsHeaderlist.list()) { OBDal.getInstance().remove(tdsHeader);
         * } }
         * 
         * } catch (Exception e) { e.printStackTrace(); } }
         */
        // Do not reactive the payment if it is Direct VAT
        /*
         * if(payment.getQMLVATProcessed().equals("Y")) { msg.setType("Error");
         * msg.setTitle(Utility.messageBD(conProvider, "Error", language));
         * msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
         * "@QPOE_VatPayment@")); bundle.setResult(msg); OBDal.getInstance().rollbackAndClose();
         * return; } if(payment.getQMLVATProcessed().equals("N")) { try { OBCriteria<QMLVATDETAILS>
         * vatDetailslist = OBDal.getInstance().createCriteria(QMLVATDETAILS.class);
         * vatDetailslist.add(Restrictions.eq(QMLTDSDetails.PROPERTY_PAYMENT + ".id",
         * payment.getId())); if(vatDetailslist != null && vatDetailslist.list().size() > 0) { for
         * (QMLVATDETAILS vatDetail : vatDetailslist.list()) {
         * OBDal.getInstance().remove(vatDetail); } }
         * 
         * } catch (Exception e) { e.printStackTrace(); } }
         */
        // Do not restore paid amounts if the payment is awaiting execution.
        boolean restorePaidAmounts = (FIN_Utility
            .seqnumberpaymentstatus(payment.getStatus())) == (FIN_Utility
                .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
        // Initialize amounts
        payment.setProcessed(false);
        OBDal.getInstance().save(payment);
        OBDal.getInstance().flush();
        payment.setWriteoffAmount(BigDecimal.ZERO);

        payment.setDescription("");

        // if all line are deleted then update amount to zero
        if (strAction.equals("R")) {
          payment.setAmount(BigDecimal.ZERO);
        }

        payment.setStatus("RPAP");
        payment.setAPRMProcessPayment("P");
        OBDal.getInstance().save(payment);
        /*
         * if(payment.getQmrCheckleaf() != null) { QMRCheckLeaf checkLeaf = null; User user = null;
         * 
         * try { OBContext.setAdminMode(); checkLeaf = payment.getQmrCheckleaf();
         * 
         * user = dao.getObject(User.class, vars.getUser());
         * 
         * checkLeaf.setCancelled(true); checkLeaf.setStatus("C"); checkLeaf.setUpdatedBy(user);
         * checkLeaf.setCancelledddate(new Date()); checkLeaf.setCancelledby(user.getLastName() ==
         * null ? user.getName() : user.getName() + " " + user.getLastName());
         * OBDal.getInstance().save(checkLeaf);
         * 
         * payment.setQmrIsprinted(false); payment.setQmrCheckleaf(null);
         * payment.setUpdatedBy(user); payment.setQmrChequeno(null);
         * 
         * OBDal.getInstance().save(payment);
         * 
         * } catch (Exception e) { System.err.println(" Exception while check cancellation: " + e);
         * } finally { OBContext.restorePreviousMode(); } }
         */
        OBDal.getInstance().flush();
        final List<FIN_PaymentDetail> removedPD = new ArrayList<FIN_PaymentDetail>();
        List<FIN_PaymentScheduleDetail> removedPDS = new ArrayList<FIN_PaymentScheduleDetail>();
        final List<String> removedPDIds = new ArrayList<String>();
        // removed when new security implementation is done
        OBContext.setAdminMode();
        try {
          BusinessPartner businessPartner = payment.getBusinessPartner();
          // When credit is used (consumed) we compensate so_creditused as this amount is already
          // included in the payment details. Credit consumed should not affect to so_creditused
          BigDecimal paidAmount = BigDecimal.ZERO;
          String fromCurrency = payment.getCurrency().getId();
          String toCurrency = "";
          if (!(businessPartner == null)) {
            // When credit is used (consumed) we compensate so_creditused as this amount is already
            // included in the payment details. Credit consumed should not affect to so_creditused
            toCurrency = businessPartner.getCurrency().getId();
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
              if (isReceipt) {
                decreaseCustomerCredit(businessPartner, payment.getUsedCredit());
              } else {
                increaseCustomerCredit(businessPartner, payment.getUsedCredit());
              }
            }
          }
          List<FIN_PaymentDetail> paymentDetails = payment.getFINPaymentDetailList();
          List<ConversionRateDoc> conversionRates = payment.getCurrencyConversionRateDocList();
          Set<String> invoiceDocNos = new HashSet<String>();
          // Undo Reversed payment relationship
          List<FIN_Payment> revPayments = new ArrayList<FIN_Payment>();
          for (FIN_Payment reversedPayment : payment.getFINPaymentReversedPaymentList()) {
            reversedPayment.setReversedPayment(null);
            OBDal.getInstance().save(reversedPayment);
          }
          payment.setFINPaymentReversedPaymentList(revPayments);
          OBDal.getInstance().save(payment);
          for (FIN_PaymentDetail paymentDetail : paymentDetails) {
            removedPDS = new ArrayList<FIN_PaymentScheduleDetail>();
            for (FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                .getFINPaymentScheduleDetailList()) {
              Boolean invoicePaidold = paymentScheduleDetail.isInvoicePaid();
              if (invoicePaidold | paymentScheduleDetail.getInvoicePaymentSchedule() == null) {
                BigDecimal psdWriteoffAmount = paymentScheduleDetail.getWriteoffAmount();
                BigDecimal psdAmount = paymentScheduleDetail.getAmount();
                BigDecimal amount = psdAmount.add(psdWriteoffAmount);
                if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                  // Remove invoice description related to the credit payments
                  final Invoice invoice = paymentScheduleDetail.getInvoicePaymentSchedule()
                      .getInvoice();
                  invoiceDocNos.add(invoice.getDocumentNo());
                  final String invDesc = invoice.getDescription();
                  if (invDesc != null) {
                    final String creditMsg = Utility.messageBD(new DalConnectionProvider(),
                        "APRM_InvoiceDescUsedCredit", vars.getLanguage());
                    if (creditMsg != null) {
                      StringBuffer newDesc = new StringBuffer();
                      for (final String line : invDesc.split("\n")) {
                        if (!line.startsWith(creditMsg.substring(0, creditMsg.lastIndexOf("%s")))) {
                          newDesc.append(line);
                          if (!"".equals(line))
                            newDesc.append("\n");
                        }
                      }
                      if (newDesc.length() > 255) {
                        newDesc = newDesc.delete(251, newDesc.length());
                        newDesc = newDesc.append("...\n");
                      }
                      invoice.setDescription(newDesc.toString());

                    }
                  }
                  if (restorePaidAmounts) {
                    FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                        paymentScheduleDetail.getInvoicePaymentSchedule(), psdAmount.negate(),
                        psdWriteoffAmount.negate());
                    paymentScheduleDetail.setInvoicePaid(false);
                    OBDal.getInstance().save(paymentScheduleDetail);
                    // BP SO_CreditUsed
                    businessPartner = paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                        .getBusinessPartner();
                    Invoice invoiceForConversion = paymentScheduleDetail
                        .getInvoicePaymentSchedule() != null
                            ? paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                            : null;
                    paidAmount = BigDecimal.ZERO;
                    fromCurrency = payment.getCurrency().getId();
                    toCurrency = "";
                    if (!(businessPartner == null)) {
                      toCurrency = businessPartner.getCurrency().getId();
                      if (!fromCurrency.equals(toCurrency)) {
                        BigDecimal exchangeRate = BigDecimal.ZERO;
                        // check at invoice document level
                        List<ConversionRateDoc> conversionRateDocumentForInvoice = getConversionRateDocumentForInvoice(
                            invoiceForConversion, isReceipt);
                        if (conversionRateDocumentForInvoice.size() > 0) {
                          exchangeRate = conversionRateDocumentForInvoice.get(0).getRate();
                        } else {
                          // global
                          exchangeRate = getConversionRate(payment.getOrganization().getId(),
                              fromCurrency, toCurrency,
                              invoiceForConversion != null ? invoiceForConversion.getInvoiceDate()
                                  : payment.getPaymentDate());
                        }
                        if (exchangeRate == BigDecimal.ZERO) {
                          msg.setType("Error");
                          msg.setTitle(Utility.messageBD(conProvider, "Error", language));
                          msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                              "@NoCurrencyConversion@"));
                          bundle.setResult(msg);
                          OBDal.getInstance().rollbackAndClose();
                          return;
                        }
                        paidAmount = amount.multiply(exchangeRate);
                      } else {
                        paidAmount = amount;
                      }
                      if (isReceipt) {
                        increaseCustomerCredit(businessPartner, paidAmount);
                      } else {
                        decreaseCustomerCredit(businessPartner, paidAmount);

                      }

                    }
                  }
                }
                if (paymentScheduleDetail.getOrderPaymentSchedule() != null && restorePaidAmounts) {
                  FIN_AddPayment.updatePaymentScheduleAmounts(paymentDetail,
                      paymentScheduleDetail.getOrderPaymentSchedule(), psdAmount.negate(),
                      psdWriteoffAmount.negate());
                }
                if (restorePaidAmounts) {
                  // when generating credit for a BP SO_CreditUsed is also updated
                  if (paymentScheduleDetail.getInvoicePaymentSchedule() == null
                      && paymentScheduleDetail.getOrderPaymentSchedule() == null
                      && paymentScheduleDetail.getPaymentDetails().getGLItem() == null
                      && restorePaidAmounts && !paymentDetail.isRefund()) {
                    // BP SO_CreditUsed
                    if (isReceipt) {
                      increaseCustomerCredit(businessPartner, amount);
                    } else {
                      decreaseCustomerCredit(businessPartner, amount);
                    }
                  }
                }
              }

              if (strAction.equals("R") || (strAction.equals("RE")
                  && paymentScheduleDetail.getInvoicePaymentSchedule() == null
                  && paymentScheduleDetail.getOrderPaymentSchedule() == null
                  && paymentScheduleDetail.getPaymentDetails().getGLItem() == null)) {
                FIN_AddPayment.mergePaymentScheduleDetails(paymentScheduleDetail);
                removedPDS.add(paymentScheduleDetail);
              }

            }
            paymentDetail.getFINPaymentScheduleDetailList().removeAll(removedPDS);
            if (strAction.equals("R")) {
              OBDal.getInstance().getSession().refresh(paymentDetail);
            }
            // If there is any schedule detail with amount zero, those are deleted
            for (FIN_PaymentScheduleDetail psd : removedPDS) {
              if (BigDecimal.ZERO.compareTo(psd.getAmount()) == 0
                  && BigDecimal.ZERO.compareTo(psd.getWriteoffAmount()) == 0) {
                paymentDetail.getFINPaymentScheduleDetailList().remove(psd);
                OBDal.getInstance().getSession().refresh(paymentDetail);
                if (psd.getInvoicePaymentSchedule() != null) {
                  psd.getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList().remove(psd);
                }
                if (psd.getOrderPaymentSchedule() != null) {
                  psd.getOrderPaymentSchedule()
                      .getFINPaymentScheduleDetailOrderPaymentScheduleList().remove(psd);
                }
                OBDal.getInstance().remove(psd);
              }
            }
            if (paymentDetail.getFINPaymentScheduleDetailList().size() == 0) {
              removedPD.add(paymentDetail);
              removedPDIds.add(paymentDetail.getId());
            }
            OBDal.getInstance().save(paymentDetail);
          }
          for (String pdToRm : removedPDIds) {
            OBDal.getInstance().remove(OBDal.getInstance().get(FIN_PaymentDetail.class, pdToRm));
          }
          payment.getFINPaymentDetailList().removeAll(removedPD);
          if (strAction.equals("R")) {
            payment.getCurrencyConversionRateDocList().removeAll(conversionRates);
            payment.setFinancialTransactionConvertRate(BigDecimal.ZERO);
          }
          OBDal.getInstance().save(payment);

          if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
              && payment.getUsedCredit().compareTo(BigDecimal.ZERO) != 0) {
            undoUsedCredit(payment, vars, invoiceDocNos);
          }

          List<FIN_Payment> creditPayments = new ArrayList<FIN_Payment>();
          for (final FIN_Payment_Credit pc : payment.getFINPaymentCreditList()) {
            creditPayments.add(pc.getCreditPaymentUsed());
          }
          for (final FIN_Payment creditPayment : creditPayments) {
            // Update Description
            final String payDesc = creditPayment.getDescription();
            if (payDesc != null) {
              final String invoiceDocNoMsg = Utility.messageBD(new DalConnectionProvider(),
                  "APRM_CreditUsedinInvoice", vars.getLanguage());
              if (invoiceDocNoMsg != null) {
                final StringBuffer newDesc = new StringBuffer();
                for (final String line : payDesc.split("\n")) {
                  boolean include = true;
                  if (line.startsWith(
                      invoiceDocNoMsg.substring(0, invoiceDocNoMsg.lastIndexOf("%s")))) {
                    for (final String docNo : invoiceDocNos) {
                      if (line.indexOf(docNo) > 0) {
                        include = false;
                        break;
                      }
                    }
                  }
                  if (include) {
                    newDesc.append(line);
                    if (!"".equals(line))
                      newDesc.append("\n");
                  }
                }
                // Truncate Description to keep length as 255
                creditPayment.setDescription(
                    newDesc.toString().length() > 255 ? newDesc.toString().substring(0, 255)
                        : newDesc.toString());
              }
            }
          }

          payment.getFINPaymentCreditList().clear();
          if (payment.isReceipt() || strAction.equals("R")) {
            payment.setGeneratedCredit(BigDecimal.ZERO);
            // payment.setQPOECGlitem(null);
          }
          if (strAction.equals("R")) {
            payment.setUsedCredit(BigDecimal.ZERO);
            for (FIN_PaymentScheduleDetail psd : removedPDS) {
              List<FIN_PaymentPropDetail> ppds = psd.getFINPaymentPropDetailList();
              if (ppds.size() > 0) {
                for (FIN_PaymentPropDetail ppd : ppds) {
                  FIN_PaymentProposal paymentProposal = OBDal.getInstance()
                      .get(FIN_PaymentProposal.class, ppd.getFinPaymentProposal().getId());
                  paymentProposal.setProcessed(false);
                  OBDal.getInstance().save(paymentProposal);
                  OBDal.getInstance().remove(ppd);
                  OBDal.getInstance().flush();
                  paymentProposal.setProcessed(true);
                }
              }
            }
          }
          // Task No:2936
          if (strAction.equals("R") || strAction.equals("RE")) {
            EfinPoApproval paymentApproval = OBProvider.getInstance().get(EfinPoApproval.class);
            paymentApproval.setClient(OBContext.getOBContext().getCurrentClient());
            paymentApproval.setOrganization(OBContext.getOBContext().getCurrentOrganization());
            paymentApproval.setActive(true);
            paymentApproval.setCreatedBy(OBContext.getOBContext().getUser());
            paymentApproval.setCreationDate(new java.util.Date());
            paymentApproval.setUpdated(new java.util.Date());
            paymentApproval.setUpdatedBy(OBContext.getOBContext().getUser());
            paymentApproval.setUserContact(OBContext.getOBContext().getUser());
            paymentApproval.setRole(OBContext.getOBContext().getRole());
            paymentApproval.setApproveddate(new Date());
            paymentApproval.setPayment(payment);
            paymentApproval.setAlertStatus("REACT");
            OBDal.getInstance().save(paymentApproval);
            OBDal.getInstance().flush();
          }
          // Task No:2936
        } finally {
          OBDal.getInstance().flush();
          OBContext.restorePreviousMode();
        }

      } else if (strAction.equals("V")) {
        // Void
        OBContext.setAdminMode();
        try {
          if (payment.isProcessed()) {
            // Already Posted Document
            if ("Y".equals(payment.getPosted())) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@PostedDocument@" + ": " + payment.getDocumentNo()));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
            // Transaction exists
            if (hasTransaction(payment)) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@APRM_TransactionExists@"));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
            // Payment with generated credit already used on other payments.
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 1
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) == 1) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@APRM_PaymentGeneratedCreditIsUsed@"));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
            // Payment not in Awaiting Execution
            boolean restorePaidAmounts = (FIN_Utility
                .seqnumberpaymentstatus(payment.getStatus())) < (FIN_Utility
                    .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(payment)));
            if (!restorePaidAmounts) {
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(conProvider, "Error", language));
              msg.setMessage(Utility.parseTranslation(conProvider, vars, language,
                  "@APRM_PaymentNotRPAE_NotVoid@"));
              bundle.setResult(msg);
              OBDal.getInstance().rollbackAndClose();
              return;
            }

            /*
             * Void the payment
             */
            payment.setStatus("RPVOID");

            /*
             * Cancel all payment schedule details related to the payment
             */
            final List<FIN_PaymentScheduleDetail> removedPDS = new ArrayList<FIN_PaymentScheduleDetail>();
            Set<String> invoiceDocNos = new HashSet<String>();
            for (final FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
              for (final FIN_PaymentScheduleDetail paymentScheduleDetail : paymentDetail
                  .getFINPaymentScheduleDetailList()) {
                Boolean invoicePaidold = paymentScheduleDetail.isInvoicePaid();
                if (invoicePaidold | paymentScheduleDetail.getInvoicePaymentSchedule() == null) {
                  paymentScheduleDetail.setInvoicePaid(false);
                }
                BigDecimal outStandingAmt = BigDecimal.ZERO;

                if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
                  // Related to invoices
                  for (final FIN_PaymentScheduleDetail invScheDetail : paymentScheduleDetail
                      .getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
                    if (invScheDetail.isCanceled()) {
                      continue;
                    }
                    if (invScheDetail.getPaymentDetails() == null) {
                      outStandingAmt = outStandingAmt.add(invScheDetail.getAmount())
                          .add(invScheDetail.getWriteoffAmount());
                      removedPDS.add(invScheDetail);
                    } else if (invScheDetail.equals(paymentScheduleDetail)) {
                      outStandingAmt = outStandingAmt.add(invScheDetail.getAmount())
                          .add(invScheDetail.getWriteoffAmount());
                      paymentScheduleDetail.setCanceled(true);
                    }
                    invoiceDocNos.add(paymentScheduleDetail.getInvoicePaymentSchedule().getInvoice()
                        .getDocumentNo());
                  }
                  // Create merged Payment Schedule Detail with the pending to be paid amount
                  if (outStandingAmt.compareTo(BigDecimal.ZERO) != 0) {
                    final FIN_PaymentScheduleDetail mergedScheduleDetail = dao
                        .getNewPaymentScheduleDetail(payment.getOrganization(), outStandingAmt);
                    mergedScheduleDetail.setInvoicePaymentSchedule(
                        paymentScheduleDetail.getInvoicePaymentSchedule());
                    mergedScheduleDetail
                        .setOrderPaymentSchedule(paymentScheduleDetail.getOrderPaymentSchedule());
                    OBDal.getInstance().save(mergedScheduleDetail);
                  }
                } else if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
                  // Related to orders
                  for (final FIN_PaymentScheduleDetail ordScheDetail : paymentScheduleDetail
                      .getOrderPaymentSchedule()
                      .getFINPaymentScheduleDetailOrderPaymentScheduleList()) {
                    if (ordScheDetail.isCanceled()) {
                      continue;
                    }
                    if (ordScheDetail.getPaymentDetails() == null) {
                      outStandingAmt = outStandingAmt.add(ordScheDetail.getAmount())
                          .add(ordScheDetail.getWriteoffAmount());
                      removedPDS.add(ordScheDetail);
                    } else if (ordScheDetail.equals(paymentScheduleDetail)) {
                      outStandingAmt = outStandingAmt.add(ordScheDetail.getAmount())
                          .add(ordScheDetail.getWriteoffAmount());
                      paymentScheduleDetail.setCanceled(true);
                    }
                  }
                  // Create merged Payment Schedule Detail with the pending to be paid amount
                  if (outStandingAmt.compareTo(BigDecimal.ZERO) != 0) {
                    final FIN_PaymentScheduleDetail mergedScheduleDetail = dao
                        .getNewPaymentScheduleDetail(payment.getOrganization(), outStandingAmt);
                    mergedScheduleDetail
                        .setOrderPaymentSchedule(paymentScheduleDetail.getOrderPaymentSchedule());
                    OBDal.getInstance().save(mergedScheduleDetail);
                  }
                } else if (paymentDetail.getGLItem() != null) {
                  paymentScheduleDetail.setCanceled(true);
                } else if (paymentScheduleDetail.getOrderPaymentSchedule() == null
                    && paymentScheduleDetail.getInvoicePaymentSchedule() == null) {
                  // Credit payment
                  payment.setGeneratedCredit(
                      payment.getGeneratedCredit().subtract(paymentScheduleDetail.getAmount()));
                  removedPDS.add(paymentScheduleDetail);
                }

                OBDal.getInstance().save(payment);
                OBDal.getInstance().flush();
              }
              paymentDetail.getFINPaymentScheduleDetailList().removeAll(removedPDS);
              for (FIN_PaymentScheduleDetail removedPD : removedPDS) {
                if (removedPD.getOrderPaymentSchedule() != null) {
                  removedPD.getOrderPaymentSchedule()
                      .getFINPaymentScheduleDetailOrderPaymentScheduleList().remove(removedPD);
                  OBDal.getInstance().save(removedPD.getOrderPaymentSchedule());
                }
                if (removedPD.getInvoicePaymentSchedule() != null) {
                  removedPD.getInvoicePaymentSchedule()
                      .getFINPaymentScheduleDetailInvoicePaymentScheduleList().remove(removedPD);
                  OBDal.getInstance().save(removedPD.getInvoicePaymentSchedule());
                }
                OBDal.getInstance().remove(removedPD);
              }
              OBDal.getInstance().flush();
              removedPDS.clear();

            }
            if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) == 0
                && payment.getUsedCredit().compareTo(BigDecimal.ZERO) == 1) {
              undoUsedCredit(payment, vars, invoiceDocNos);
            }
            payment.getFINPaymentCreditList().clear();
            payment.setUsedCredit(BigDecimal.ZERO);
          }
          OBDal.getInstance().flush();
        } finally {
          OBContext.restorePreviousMode();
        }
      }

      bundle.setResult(msg);

    } catch (final Exception e) {
      e.printStackTrace(System.err);
      msg.setType("Error");
      msg.setTitle(
          Utility.messageBD(bundle.getConnection(), "Error", bundle.getContext().getLanguage()));
      msg.setMessage(Utility.translateError(bundle.getConnection(), null,
          bundle.getContext().getLanguage(), FIN_Utility.getExceptionMessage(e)).getMessage());
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Method used to update the credit used when the user doing invoice processing or payment
   * processing
   * 
   * @param amount
   *          Payment amount
   */
  private void updateCustomerCredit(BusinessPartner businessPartner, BigDecimal amount,
      boolean add) {
    BigDecimal creditUsed = businessPartner.getCreditUsed();
    if (add) {
      creditUsed = creditUsed.add(amount);
    } else {
      creditUsed = creditUsed.subtract(amount);
    }
    businessPartner.setCreditUsed(creditUsed);
    OBDal.getInstance().save(businessPartner);
    // OBDal.getInstance().flush();
  }

  private void increaseCustomerCredit(BusinessPartner businessPartner, BigDecimal amount) {
    updateCustomerCredit(businessPartner, amount, true);
  }

  private void decreaseCustomerCredit(BusinessPartner businessPartner, BigDecimal amount) {
    updateCustomerCredit(businessPartner, amount, false);
  }

  private OBError triggerAutomaticFinancialAccountTransaction(VariablesSecureApp vars,
      ConnectionProvider connectionProvider, FIN_Payment payment) {
    FIN_FinaccTransaction transaction = TransactionsDao.createFinAccTransaction(payment);
    try {
      return processTransaction(vars, connectionProvider, "P", transaction);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace(System.err);
      OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle(e.getMessage());
      return msg;
    }
  }

  private static boolean hasTransaction(FIN_Payment payment) {
    OBCriteria<FIN_FinaccTransaction> transaction = OBDal.getInstance()
        .createCriteria(FIN_FinaccTransaction.class);
    transaction.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
    List<FIN_FinaccTransaction> list = transaction.list();
    if (list == null || list.size() == 0)
      return false;
    return true;
  }

  private void updateUsedCredit(FIN_Payment newPayment) {
    if (newPayment.getFINPaymentCreditList().isEmpty()) {
      // We process the payment from the Payment In/Out window (not from the Process Invoice flow)
      final BigDecimal usedAmount = newPayment.getUsedCredit();
      final BusinessPartner bp = newPayment.getBusinessPartner();
      final boolean isReceipt = newPayment.isReceipt();
      final Organization Org = newPayment.getOrganization();

      List<FIN_Payment> creditPayments = dao.getCustomerPaymentsWithCredit(Org, bp, isReceipt);
      BigDecimal pendingToAllocateAmount = usedAmount;
      for (FIN_Payment creditPayment : creditPayments) {
        BigDecimal availableAmount = creditPayment.getGeneratedCredit()
            .subtract(creditPayment.getUsedCredit());
        if (pendingToAllocateAmount.compareTo(availableAmount) == 1) {
          creditPayment.setUsedCredit(creditPayment.getUsedCredit().add(availableAmount));
          pendingToAllocateAmount = pendingToAllocateAmount.subtract(availableAmount);
          linkCreditPayment(newPayment, availableAmount, creditPayment);
          OBDal.getInstance().save(creditPayment);
        } else {
          creditPayment.setUsedCredit(creditPayment.getUsedCredit().add(pendingToAllocateAmount));
          linkCreditPayment(newPayment, pendingToAllocateAmount, creditPayment);
          OBDal.getInstance().save(creditPayment);
          break;
        }
      }
    }
  }

  public static void linkCreditPayment(FIN_Payment newPayment, BigDecimal usedAmount,
      FIN_Payment creditPayment) {
    final FIN_Payment_Credit creditInfo = OBProvider.getInstance().get(FIN_Payment_Credit.class);
    creditInfo.setPayment(newPayment);
    creditInfo.setAmount(usedAmount);
    creditInfo.setCurrency(newPayment.getCurrency());
    creditInfo.setCreditPaymentUsed(creditPayment);
    creditInfo.setOrganization(newPayment.getOrganization());
    creditInfo.setClient(newPayment.getClient());
    newPayment.getFINPaymentCreditList().add(creditInfo);
  }

  private void undoUsedCredit(FIN_Payment myPayment, VariablesSecureApp vars,
      Set<String> invoiceDocNos) {
    final List<FIN_Payment> payments = new ArrayList<FIN_Payment>();
    for (final FIN_Payment_Credit pc : myPayment.getFINPaymentCreditList()) {
      final FIN_Payment creditPaymentUsed = pc.getCreditPaymentUsed();
      creditPaymentUsed.setUsedCredit(creditPaymentUsed.getUsedCredit().subtract(pc.getAmount()));
      payments.add(creditPaymentUsed);
    }

    for (final FIN_Payment payment : payments) {
      // Update Description
      final String payDesc = payment.getDescription();
      if (payDesc != null) {
        final String invoiceDocNoMsg = Utility.messageBD(new DalConnectionProvider(),
            "APRM_CreditUsedinInvoice", vars.getLanguage());
        if (invoiceDocNoMsg != null) {
          final StringBuffer newDesc = new StringBuffer();
          for (final String line : payDesc.split("\n")) {
            boolean include = true;
            if (line.startsWith(invoiceDocNoMsg.substring(0, invoiceDocNoMsg.lastIndexOf("%s")))) {
              for (final String docNo : invoiceDocNos) {
                if (line.indexOf(docNo) > 0) {
                  include = false;
                  break;
                }
              }
            }
            if (include) {
              newDesc.append(line);
              if (!"".equals(line))
                newDesc.append("\n");
            }
          }
          // Truncate Description to keep length as 255
          payment.setDescription(
              newDesc.toString().length() > 255 ? newDesc.toString().substring(0, 255)
                  : newDesc.toString());
        }
      }
    }
  }

  private List<ConversionRateDoc> getConversionRateDocument(FIN_Payment payment) {
    OBContext.setAdminMode();
    try {
      OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, payment.getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
          payment.getAccount().getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_PAYMENT, payment));
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<ConversionRateDoc> getConversionRateDocumentForInvoice(Invoice invoice,
      boolean isReceipt) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, invoice.getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY,
          isReceipt ? invoice.getBusinessPartner().getCurrency()
              : invoice.getBusinessPartner().getPurchasePricelist().getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE, invoice));
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ConversionRateDoc insertConversionRateDocument(FIN_Payment payment) {
    OBContext.setAdminMode();
    try {
      ConversionRateDoc newConversionRateDoc = OBProvider.getInstance()
          .get(ConversionRateDoc.class);
      newConversionRateDoc.setOrganization(payment.getOrganization());
      newConversionRateDoc.setCurrency(payment.getCurrency());
      newConversionRateDoc.setToCurrency(payment.getAccount().getCurrency());
      newConversionRateDoc.setRate(payment.getFinancialTransactionConvertRate());
      newConversionRateDoc.setForeignAmount(payment.getFinancialTransactionAmount());
      newConversionRateDoc.setPayment(payment);
      newConversionRateDoc.setClient(payment.getClient());
      OBDal.getInstance().save(newConversionRateDoc);
      OBDal.getInstance().flush();
      return newConversionRateDoc;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It calls the Transaction Process for the given transaction and action.
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param strAction
   *          String with the action of the process. {P, D, R}
   * @param transaction
   *          FIN_FinaccTransaction that needs to be processed.
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  private OBError processTransaction(VariablesSecureApp vars, ConnectionProvider conn,
      String strAction, FIN_FinaccTransaction transaction) throws Exception {
    ProcessBundle pb = new ProcessBundle("F68F2890E96D4D85A1DEF0274D105BCE", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("action", strAction);
    parameters.put("Fin_FinAcc_Transaction_ID", transaction.getId());
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_TransactionProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }

  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    OBContext.setAdminMode();
    try {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strRecordId);
      OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, payment.getAccount()));
      obCriteria.add(
          Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, payment.getPaymentMethod()));
      obCriteria.setFilterOnReadableClients(false);
      obCriteria.setFilterOnReadableOrganization(false);
      List<FinAccPaymentMethod> lines = obCriteria.list();
      List<FIN_FinancialAccountAccounting> accounts = payment.getAccount()
          .getFINFinancialAccountAcctList();
      for (FIN_FinancialAccountAccounting account : accounts) {
        if (confirmation)
          return confirmation;
        if (payment.isReceipt()) {
          if (("INT").equals(lines.get(0).getUponReceiptUse())
              && account.getInTransitPaymentAccountIN() != null)
            confirmation = true;
          else if (("DEP").equals(lines.get(0).getUponReceiptUse())
              && account.getDepositAccount() != null)
            confirmation = true;
          else if (("CLE").equals(lines.get(0).getUponReceiptUse())
              && account.getClearedPaymentAccount() != null)
            confirmation = true;
        } else {
          if (("INT").equals(lines.get(0).getUponPaymentUse())
              && account.getFINOutIntransitAcct() != null)
            confirmation = true;
          else if (("WIT").equals(lines.get(0).getUponPaymentUse())
              && account.getWithdrawalAccount() != null)
            confirmation = true;
          else if (("CLE").equals(lines.get(0).getUponPaymentUse())
              && account.getClearedPaymentAccountOUT() != null)
            confirmation = true;
        }
        // For payments with Amount ZERO always create an entry as no transaction will be created
        if (payment.getAmount().compareTo(ZERO) == 0) {
          confirmation = true;
        }
      }
    } catch (Exception e) {
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    return confirmation;
  }

  private BigDecimal getConversionRate(String strOrgId, String strFromCurrencyId,
      String strToCurrencyId, Date conversionDate) {
    BigDecimal exchangeRate = BigDecimal.ZERO;
    // Apply default conversion rate
    int conversionRatePrecision = FIN_Utility
        .getConversionRatePrecision(RequestContext.get().getVariablesSecureApp());
    Organization organization = OBDal.getInstance().get(Organization.class, strOrgId);
    Currency fromCurrency = OBDal.getInstance().get(Currency.class, strFromCurrencyId);
    Currency toCurrency = OBDal.getInstance().get(Currency.class, strToCurrencyId);
    final ConversionRate conversionRate = FIN_Utility.getConversionRate(fromCurrency, toCurrency,
        conversionDate, organization);
    if (conversionRate != null) {
      exchangeRate = conversionRate.getMultipleRateBy().setScale(conversionRatePrecision,
          RoundingMode.HALF_UP);
    } else {
      exchangeRate = BigDecimal.ZERO;
    }
    return exchangeRate;
  }

}
