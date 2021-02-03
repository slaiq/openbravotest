package sa.elm.ob.finance.ad_process.Sadad;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.dao.AdvPaymentMngtDao;
import sa.elm.ob.finance.process.po_approval.FIN_AddPayment;
import sa.elm.ob.utility.util.UtilityDAO;

public class AddReceiptInOrdertoReceive {
  final private static Logger log = LoggerFactory.getLogger(AddReceiptInOrdertoReceive.class);
  final SimpleDateFormat jsDateFormat = JsonUtils.createDateFormat();
  private final static String ACTION_PROCESS_TRANSACTION = "P";
  private static String SeqNo = "0";

  public static boolean addReceiptARInvoice(Invoice invoice) {
    OBContext.setAdminMode(true);
    try {
      log.debug("payment handler");
      SimpleDateFormat ddMMyyyyFormat = new SimpleDateFormat("dd-MM-yyyy");
      Organization org = invoice.getOrganization();
      boolean isReceipt = true;
      final String strAction = "PRP";
      final String strInvoiceId = invoice.getId();
      Currency currency = invoice.getCurrency();
      BusinessPartner businessPartner = invoice.getBusinessPartner();
      String strActualPayment = invoice.getGrandTotalAmount().toString();

      // Check Posting Sequence
      Date gregPaymentDate = new Date();
      String gregorianAcctDate = ddMMyyyyFormat.format(gregPaymentDate);

      String CalendarId = "";

      if (org.getCalendar() != null) {
        CalendarId = org.getCalendar().getId();
      } else {

        // get parent organization list
        String[] orgIds = null;
        Organization parentOrganization = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
            "select eut_parent_org ('" + org.getId() + "','" + org.getClient().getId() + "')");
        Object parentOrg = query.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          parentOrganization = OBDal.getInstance().get(Organization.class,
              orgIds[i].replace("'", ""));
          if (parentOrganization.getCalendar() != null) {
            CalendarId = parentOrganization.getCalendar().getId();
            break;
          }
        }

      }
      String SequenceNo1 = UtilityDAO.getGeneralSequence(gregorianAcctDate, "GS", CalendarId,
          org.getId(), false);
      if (SequenceNo1.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoGeneralSequence"));
      }

      String SequenceNo = UtilityDAO.getGeneralSequence(gregorianAcctDate, "PS", CalendarId,
          org.getId(), false);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
      }

      String strDifferenceAction = "";
      BigDecimal differenceAmount = BigDecimal.ZERO;
      BigDecimal exchangeRate = BigDecimal.ONE;
      BigDecimal convertedAmount = BigDecimal.ZERO;

      FIN_Payment payment = null;
      try {
        log.debug("Creating a new payment");
        payment = createNewPayment(invoice, isReceipt, org, businessPartner, gregPaymentDate,
            currency, exchangeRate, convertedAmount, strActualPayment, strInvoiceId);
      } catch (OBException e) {
        e.printStackTrace();
        log.debug("Error while creating new payment in " + e);
        return false;
      }
      payment.setAmount(new BigDecimal(strActualPayment));
      addSelectedPSDs(payment, invoice, null);

      FIN_AddPayment.setFinancialTransactionAmountAndRate(null, payment, exchangeRate,
          convertedAmount);
      OBDal.getInstance().save(payment);

      OBError message = processPayment(payment, strAction, strDifferenceAction, differenceAmount,
          exchangeRate, invoice);
      payment.setDescription("Invoice no:" + invoice.getDocumentNo());
      payment.setGeneratedCredit(BigDecimal.ZERO);
      OBDal.getInstance().save(payment);

      if ("Success".equals(message.getType())) {
        FIN_FinaccTransaction transaction = createAndProcessTransactionFromPayment(payment,
            new Date(), new Date(), invoice.getEfinFinFinacct().getId());
        if (transaction != null) {
          transaction.setEfinDocumentNo(invoice.getDocumentNo());
          OBDal.getInstance().save(transaction);
          OBDal.getInstance().flush();
          return true;
        }
        return false;
      } else {
        return false;
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while creating adding receipt in financial account " + e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to create new payment for the invoice
   * 
   * @param invoice
   * @param isReceipt
   * @param org
   * @param bPartner
   * @param paymentDate
   * @param currency
   * @param conversionRate
   * @param convertedAmt
   * @param strActualPayment
   * @param strInvoiceId
   * @return FIn_payment object
   * @throws OBException
   * @throws JSONException
   */

  private static FIN_Payment createNewPayment(Invoice invoice, boolean isReceipt, Organization org,
      BusinessPartner bPartner, Date paymentDate, Currency currency, BigDecimal conversionRate,
      BigDecimal convertedAmt, String strActualPayment, String strInvoiceId)
      throws OBException, JSONException {
    log.debug("payment here");
    String strPaymentDocumentNo = FIN_Utility.getDocumentNo(org, isReceipt ? "ARR" : "APP",
        "FIN_Payment", true);

    String strFinancialAccountId = invoice.getEfinFinFinacct().getId();
    FIN_FinancialAccount finAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
        strFinancialAccountId);
    FIN_PaymentMethod paymentMethod = invoice.getPaymentMethod();

    boolean paymentDocumentEnabled = getDocumentConfirmation(finAccount, paymentMethod, isReceipt,
        strActualPayment, true);

    String strAction = (isReceipt ? "PRP" : "PPP");
    boolean documentEnabled = true;
    if ((strAction.equals("PRD") || strAction.equals("PPW")
        || FIN_Utility.isAutomaticDepositWithdrawn(finAccount, paymentMethod, isReceipt))
        && new BigDecimal(strActualPayment).signum() != 0) {
      documentEnabled = paymentDocumentEnabled
          || getDocumentConfirmation(finAccount, paymentMethod, isReceipt, strActualPayment, false);
    } else {
      documentEnabled = paymentDocumentEnabled;
    }

    DocumentType documentType = FIN_Utility.getDocumentType(org, isReceipt ? "ARR" : "APP");
    String strDocBaseType = documentType.getDocumentCategory();

    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId());
    boolean orgLegalWithAccounting = osp.getLegalEntityOrBusinessUnit(org).getOrganizationType()
        .isLegalEntityWithAccounting();
    if (documentEnabled
        && !FIN_Utility.isPeriodOpen(OBContext.getOBContext().getCurrentClient().getId(),
            strDocBaseType, org.getId(), OBDateUtils.formatDate(paymentDate))
        && orgLegalWithAccounting) {
      String messag = OBMessageUtils.messageBD("Efin_AddPay_ActPeriod");
      throw new OBException(messag);
    }

    String strPaymentAmount = "0";
    if (strPaymentDocumentNo.startsWith("<")) {
      strPaymentDocumentNo = FIN_Utility.getDocumentNo(documentType, "FIN_Payment");
    }

    FIN_Payment payment = getNewPayment(isReceipt, org, documentType, strPaymentDocumentNo,
        bPartner, paymentMethod, finAccount, strPaymentAmount, paymentDate, null, currency,
        conversionRate, convertedAmt, invoice);
    return payment;
  }

  public static FIN_Payment getNewPayment(boolean isReceipt, Organization organization,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, String referenceNo, Currency paymentCurrency, BigDecimal finTxnConvertRate,
      BigDecimal finTxnAmount, Invoice invoice) {
    try {
      final FIN_Payment newPayment = OBProvider.getInstance().get(FIN_Payment.class);

      newPayment.setReceipt(isReceipt);
      newPayment.setDocumentType(docType);
      newPayment.setDocumentNo(strPaymentDocumentNo);
      newPayment.setOrganization(organization);
      newPayment.setClient(organization.getClient());
      newPayment.setStatus("RPAP");
      newPayment.setBusinessPartner(businessPartner);
      newPayment.setPaymentMethod(paymentMethod);
      newPayment.setAccount(finAccount);

      final BigDecimal paymentAmount = new BigDecimal(strPaymentAmount);

      newPayment.setAmount(paymentAmount);
      newPayment.setPaymentDate(paymentDate);
      if (paymentCurrency != null) {
        newPayment.setCurrency(paymentCurrency);
      } else {
        newPayment.setCurrency(finAccount.getCurrency());
      }
      newPayment.setReferenceNo(referenceNo);
      newPayment.setFinancialTransactionConvertRate(finTxnConvertRate);
      newPayment.setFinancialTransactionAmount(finTxnAmount);

      if (isReceipt) {
        final String strCustomerName = invoice.getEfinCusname();
        final String strCustomerNo = invoice.getEfinCusno();
        final String strCustomerLoc = invoice.getEfinCuslocation();

        newPayment.setEFINName(strCustomerName);
        newPayment.setEFINNumber(strCustomerNo);
        newPayment.setEFINLocation(strCustomerLoc);

      }

      OBDal.getInstance().save(newPayment);
      return newPayment;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }

  /**
   * This method is used to process the newly created payments
   * 
   * @param payment
   * @param strAction
   * @param strDifferenceAction
   * @param refundAmount
   * @param exchangeRate
   * @param invoice
   * @return OBError
   * @throws Exception
   */
  public static OBError processPayment(FIN_Payment payment, String strAction,
      String strDifferenceAction, BigDecimal refundAmount, BigDecimal exchangeRate, Invoice invoice)
      throws Exception {

    ConnectionProvider conn = new DalConnectionProvider(true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    OBError message = null;

    try {
      OBContext.setAdminMode();
      AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
      BigDecimal assignedAmount = BigDecimal.ZERO;

      for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
        assignedAmount = assignedAmount.add(paymentDetail.getAmount());
      }

      if (assignedAmount.compareTo(payment.getAmount()) == -1) {
        FIN_PaymentScheduleDetail refundScheduleDetail = dao.getNewPaymentScheduleDetail(
            payment.getOrganization(), payment.getAmount().subtract(assignedAmount));
        dao.getNewPaymentDetail(payment, refundScheduleDetail,
            payment.getAmount().subtract(assignedAmount), BigDecimal.ZERO, false, null);
      }

      message = FIN_AddPayment.processPayment(vars, conn,
          (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", payment);
      if (message.getType().equals("Error")) {
      } else {
      }
      return message;
    } catch (Exception e) {
      log.error(" Exception in create detail: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return message;
  }

  private static boolean getDocumentConfirmation(FIN_FinancialAccount finAccount,
      FIN_PaymentMethod finPaymentMethod, boolean isReceipt, String strPaymentAmount,
      boolean isPayment) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, finAccount));
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, finPaymentMethod));
      obCriteria.setFilterOnReadableClients(false);
      obCriteria.setFilterOnReadableOrganization(false);
      obCriteria.setMaxResults(1);
      FinAccPaymentMethod finAccPayMethod = (FinAccPaymentMethod) obCriteria.uniqueResult();
      String uponUse = "";
      if (isPayment) {
        if (isReceipt) {
          uponUse = finAccPayMethod.getUponReceiptUse();
        } else {
          uponUse = finAccPayMethod.getUponPaymentUse();
        }
      } else {
        if (isReceipt) {
          uponUse = finAccPayMethod.getUponDepositUse();
        } else {
          uponUse = finAccPayMethod.getUponWithdrawalUse();
        }
      }
      for (FIN_FinancialAccountAccounting account : finAccount.getFINFinancialAccountAcctList()) {
        if (confirmation) {
          return confirmation;
        }
        if (isReceipt) {
          if ("INT".equals(uponUse) && account.getInTransitPaymentAccountIN() != null) {
            confirmation = true;
          } else if ("DEP".equals(uponUse) && account.getDepositAccount() != null) {
            confirmation = true;
          } else if ("CLE".equals(uponUse) && account.getClearedPaymentAccount() != null) {
            confirmation = true;
          }
        } else {
          if ("INT".equals(uponUse) && account.getFINOutIntransitAcct() != null) {
            confirmation = true;
          } else if ("WIT".equals(uponUse) && account.getWithdrawalAccount() != null) {
            confirmation = true;
          } else if ("CLE".equals(uponUse) && account.getClearedPaymentAccountOUT() != null) {
            confirmation = true;
          }
        }
        // For payments with Amount ZERO always create an entry as no transaction will be created
        if (isPayment) {
          BigDecimal amount = new BigDecimal(strPaymentAmount);
          if (amount.signum() == 0) {
            confirmation = true;
          }
        }
      }
    } catch (Exception e) {
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    return confirmation;
  }

  /**
   * This method is used to create transaction in financial account
   * 
   * @param paymentJS
   * @param transactionDate
   * @param acctDate
   * @param strAccountId
   * @return
   * @throws JSONException
   */
  private static FIN_FinaccTransaction createAndProcessTransactionFromPayment(
      final FIN_Payment paymentJS, final Date transactionDate, final Date acctDate,
      String strAccountId) throws JSONException {

    try {
      OBContext.setAdminMode(true);
      final FIN_Payment payment = paymentJS;
      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strAccountId);

      if (payment != null) {
        final FIN_FinaccTransaction transaction = TransactionsDao.createFinAccTransaction(payment);
        transaction.setTransactionDate(transactionDate);
        transaction.setDateAcct(acctDate);
        transaction.setAccount(account);
        transaction.setPosted("N");

        FIN_TransactionProcess.doTransactionProcess(ACTION_PROCESS_TRANSACTION, transaction);
        return transaction;
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  public static String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }

  private static void addSelectedPSDs(FIN_Payment payment, Invoice invoice, List<String> pdToRemove)
      throws JSONException {
    log.debug(" Adding PSDs");
    BigDecimal conversionRate = BigDecimal.ONE;
    BigDecimal convertedAmount = BigDecimal.ONE;

    List<FIN_PaymentSchedule> paymentScheduleList = invoice.getFINPaymentScheduleList();

    for (FIN_PaymentSchedule paymentSchedule : paymentScheduleList) {
      boolean isWriteOff = false;
      List<FIN_PaymentScheduleDetail> psds = paymentSchedule
          .getFINPaymentScheduleDetailInvoicePaymentScheduleList();
      BigDecimal outstandingAmount = BigDecimal.ZERO;
      for (FIN_PaymentScheduleDetail psd : psds) {
        BigDecimal remainingAmount = psd.getAmount();

        BigDecimal assignAmount = BigDecimal.ZERO;

        if (psd.getPaymentDetails() != null) {
          // This schedule detail comes from an edited payment so outstanding amount needs to be
          // properly calculated
          List<FIN_PaymentScheduleDetail> outStandingPSDs = FIN_AddPayment.getOutstandingPSDs(psd);
          if (outStandingPSDs.size() > 0) {
            outstandingAmount = psd.getAmount().add(outStandingPSDs.get(0).getAmount());
          } else {
            outstandingAmount = psd.getAmount();
          }
        } else {
          outstandingAmount = psd.getAmount();
        }
        // Manage negative amounts
        if ((remainingAmount.signum() > 0 && remainingAmount.compareTo(outstandingAmount) >= 0)
            || ((remainingAmount.signum() < 0 && outstandingAmount.signum() < 0)
                && (remainingAmount.compareTo(outstandingAmount) <= 0))) {
          assignAmount = outstandingAmount;
          remainingAmount = remainingAmount.subtract(outstandingAmount);
        } else {
          assignAmount = remainingAmount;
          remainingAmount = BigDecimal.ZERO;
        }

        payment.setFinancialTransactionConvertRate(conversionRate);
        payment.setFinancialTransactionAmount(convertedAmount);

        FIN_AddPayment.updatePaymentDetail(psd, payment, assignAmount, isWriteOff);
      }
    }
    log.debug("Completed adding PSDs");
  }

}