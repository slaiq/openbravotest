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
 * All portions are Copyright (C) 2014-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_ReconciliationProcess;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.financialmgmt.payment.FIN_ReconciliationLine_v;
import org.openbravo.model.financialmgmt.payment.MatchingAlgorithm;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APRM_MatchingUtility {
  private static final Logger log4j = LoggerFactory.getLogger(APRM_MatchingUtility.class);

  /**
   * Get reconciliation lines of a reconciliation that had been matched manually
   * 
   * @param reconciliation
   *          Reconciliation to be checked
   * @return List of transaction manually reconciled (without bank statement)
   */
  private static List<FIN_FinaccTransaction> getManualReconciliationLines(
      FIN_Reconciliation reconciliation) {
    List<FIN_FinaccTransaction> result = new ArrayList<FIN_FinaccTransaction>();
    OBContext.setAdminMode(false);
    try {
      final OBCriteria<FIN_ReconciliationLine_v> obc = OBDal.getInstance().createCriteria(
          FIN_ReconciliationLine_v.class);
      obc.add(Restrictions.eq(FIN_ReconciliationLine_v.PROPERTY_RECONCILIATION, reconciliation));
      obc.add(Restrictions.isNull(FIN_ReconciliationLine_v.PROPERTY_BANKSTATEMENTLINE));
      for (FIN_ReconciliationLine_v recLine : obc.list()) {
        result.add(recLine.getFinancialAccountTransaction());
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Fix wrong financial transactions of a reconciliation. If the fix is not possible, it launches
   * an OBException
   * 
   * @param reconciliation
   *          The reconciliation whose lines are to be fixed
   */
  public static void fixMixedLines(FIN_Reconciliation reconciliation) {
    List<FIN_FinaccTransaction> mixedLines = APRM_MatchingUtility
        .getManualReconciliationLines(reconciliation);
    if (mixedLines.size() > 0) {
      // Fix mixing Reconciliation and log the issue
      log4j
          .warn("Mixing Reconciliations: An error occured which left an inconsistent status for the current reconciliation: "
              + reconciliation.getIdentifier());
      OBContext.setAdminMode(false);
      try {
        for (FIN_FinaccTransaction mixedLine : mixedLines) {
          APRM_MatchingUtility.fixMixedLine(mixedLine);
          log4j
              .warn("Fixing Mixed Line (transaction appears as cleared but no bank statement line is linked to it): "
                  + mixedLine.getLineNo() + " - " + mixedLine.getIdentifier());
        }
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    // Check if problem remains
    mixedLines = APRM_MatchingUtility.getManualReconciliationLines(reconciliation);

    if (!mixedLines.isEmpty()) {
      throw new OBException(OBMessageUtils.messageBD("@APRM_ReconciliationMixed@"));
    }
  }

  /**
   * Fix wrong financial transaction of a reconciliation
   * 
   * @param mixedLine
   *          The financial transaction to be fixed
   */
  public static void fixMixedLine(FIN_FinaccTransaction mixedLine) {
    boolean isReceipt = mixedLine.getDepositAmount().compareTo(BigDecimal.ZERO) != 0;
    mixedLine.setStatus(isReceipt ? APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED
        : APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    mixedLine.setReconciliation(null);
    OBDal.getInstance().save(mixedLine);
    if (mixedLine.getFinPayment() != null) {
      mixedLine.getFinPayment().setStatus(
          isReceipt ? APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED
              : APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
      OBDal.getInstance().save(mixedLine.getFinPayment());
    }
  }

  /**
   * Process to process a reconciliation
   * 
   * @param strAction
   *          String with the action of the process. {P, D, R}
   * @param reconciliation
   *          Reconciliation that needs to be processed
   * @return returns error/success message
   * @throws Exception
   */
  public static OBError processReconciliation(String strAction, FIN_Reconciliation reconciliation)
      throws Exception {
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    ConnectionProvider conn = new DalConnectionProvider();
    ProcessBundle pb = new ProcessBundle("FF8080812E2F8EAE012E2F94CF470014", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("action", strAction);
    parameters.put("FIN_Reconciliation_ID", reconciliation.getId());
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_ReconciliationProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }

  public static FIN_BankStatementLine getLockedBSL(String id) {
    try {
      return OBDal.getInstance().get(FIN_BankStatementLine.class,
          APRMUtilityData.getLockedBSL(new DalConnectionProvider(false), id));
    } catch (ServletException e) {
      log4j.error("Error executing select for update for this record:" + id, e);
    }
    return null;
  }

  /**
   * Match a bank statement line with a financial account transaction. If the bank statement has
   * associated a transaction, it is first unmatched and then matched against the given transaction.
   * 
   * If the bank statement line amount is different from the transaction amount, the process will
   * automatically split the bank statement line in two (see
   * {@link #splitBankStatementLine(FIN_Reconciliation, FIN_BankStatementLine, FIN_FinaccTransaction)}
   * 
   * 
   * @return If success, the method automatically flushes. In case of exceptions, the method will
   *         either throw the exception or return false. This behavior is controlled by the
   *         throwException boolean parameter
   * 
   */
  public static boolean matchBankStatementLine(final FIN_BankStatementLine _bankStatementLine,
      final FIN_FinaccTransaction transaction, final FIN_Reconciliation reconciliation,
      final String matchLevel, boolean throwException) {
    try {
      OBContext.setAdminMode(true);
      FIN_BankStatementLine bankStatementLine = getLockedBSL(_bankStatementLine.getId());

      if (transaction != null) {
        // Unmatch the previous line
        if (bankStatementLine.getFinancialAccountTransaction() != null) {
          log4j.warn("Bank Statement Line Already Matched: " + bankStatementLine.getIdentifier());
          unmatch(bankStatementLine);
        }

        // Split if necessary (bank line amount != transaction amount)
        splitBankStatementLine(reconciliation, bankStatementLine, transaction);

        // Match the transaction
        bankStatementLine.setFinancialAccountTransaction(transaction);
        bankStatementLine
            .setMatchingtype(StringUtils.isBlank(matchLevel) ? FIN_MatchedTransaction.MANUALMATCH
                : matchLevel);
        bankStatementLine.setMatchedDocument(getMatchedDocument(transaction));
        transaction.setStatus(APRMConstants.PAYMENT_STATUS_PAYMENT_CLEARED);
        transaction.setReconciliation(reconciliation);
        if (transaction.getFinPayment() != null) {
          transaction.getFinPayment().setStatus(APRMConstants.PAYMENT_STATUS_PAYMENT_CLEARED);
        }
        OBDal.getInstance().save(transaction);
        OBDal.getInstance().save(bankStatementLine);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log4j.error("Error during matchBankStatementLine, performing a rollback");
      OBDal.getInstance().rollbackAndClose();
      if (throwException) {
        throw new OBException(e.getMessage());
      } else {
        return false;
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return true;
  }

  private static String getMatchedDocument(FIN_FinaccTransaction transaction) {
    final String MATCHED_AGAINST_TRANSACTION = "T";
    final String MATCHED_AGAINST_PAYMENT = "P";
    final String MATCHED_AGAINST_CREDIT = "C";
    final String MATCHED_AGAINST_INVOICE = "I";
    final String MATCHED_AGAINST_ORDER = "O";
    if (!transaction.isCreatedByAlgorithm() || transaction.getFinPayment() == null) {
      return MATCHED_AGAINST_TRANSACTION;
    } else if (transaction.getFinPayment() != null
        && !transaction.getFinPayment().isCreatedByAlgorithm()) {
      return MATCHED_AGAINST_PAYMENT;
    } else if (transaction.getFinPayment() != null
        && transaction.isCreatedByAlgorithm()
        && transaction.getFinPayment().isCreatedByAlgorithm()
        && transaction.getFinPayment().getFINPaymentDetailList().get(0)
            .getFINPaymentScheduleDetailList().get(0).getInvoicePaymentSchedule() == null
        && transaction.getFinPayment().getFINPaymentDetailList().get(0)
            .getFINPaymentScheduleDetailList().get(0).getOrderPaymentSchedule() == null) {
      return MATCHED_AGAINST_CREDIT;
    } else if (transaction.getFinPayment() != null
        && transaction.isCreatedByAlgorithm()
        && transaction.getFinPayment().isCreatedByAlgorithm()
        && transaction.getFinPayment().getFINPaymentDetailList().get(0)
            .getFINPaymentScheduleDetailList().get(0).getInvoicePaymentSchedule() != null) {
      return MATCHED_AGAINST_INVOICE;
    } else {
      return MATCHED_AGAINST_ORDER;
    }
  }

  /**
   * Remove the match of a bank statement line with a transaction
   * 
   * @param _bsline
   *          Bank Statement Line to be unmatched from a transaction
   */
  public static void unmatch(final FIN_BankStatementLine _bsline) {
    try {
      OBContext.setAdminMode(true);
      FIN_BankStatementLine bsline = getLockedBSL(_bsline.getId());
      final FIN_FinaccTransaction finTrans = bsline.getFinancialAccountTransaction();
      if (finTrans != null) {
        finTrans.setReconciliation(null);
        bsline.setFinancialAccountTransaction(null);
        bsline.setMatchingtype(null);
        bsline.setMatchedDocument(null);

        OBDal.getInstance().save(finTrans);
        OBDal.getInstance().save(bsline);

        // merge if the bank statement line was split before
        mergeBankStatementLine(bsline);

        boolean isReceipt = false;
        if (finTrans.getFinPayment() != null) {
          isReceipt = finTrans.getFinPayment().isReceipt();
          finTrans.getFinPayment().setStatus(
              isReceipt ? APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED
                  : APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
        } else {
          isReceipt = finTrans.getDepositAmount().compareTo(finTrans.getPaymentAmount()) > 0;
        }
        finTrans.setStatus(isReceipt ? APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED
            : APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
        // Execute un-matching logic defined by algorithm
        final MatchingAlgorithm ma = bsline.getBankStatement().getAccount().getMatchingAlgorithm();
        final FIN_MatchingTransaction matchingTransaction = new FIN_MatchingTransaction(
            ma.getJavaClassName());
        if (finTrans.isCreatedByAlgorithm()) {
          matchingTransaction.unmatch(finTrans);
        }
        OBDal.getInstance().flush();

        // Do not allow bank statement lines of 0
        if (bsline.getCramount().compareTo(BigDecimal.ZERO) == 0
            && bsline.getDramount().compareTo(BigDecimal.ZERO) == 0) {
          FIN_BankStatement bs = bsline.getBankStatement();
          bs.setProcessed(false);
          OBDal.getInstance().save(bs);
          OBDal.getInstance().remove(bsline);
          // bs is set a true, because it was the status at the beginning of this function
          bs.setProcessed(true);
          OBDal.getInstance().save(bs);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Merges given bank statement line with other bank statement lines with the same line number and
   * not matched with any transaction.
   * 
   * @param bsline
   *          Bank Statement Line.
   */
  private static void mergeBankStatementLine(FIN_BankStatementLine bsline) {
    BigDecimal totalCredit = bsline.getCramount();
    BigDecimal totalDebit = bsline.getDramount();
    FIN_BankStatement bs = bsline.getBankStatement();
    OBCriteria<FIN_BankStatementLine> obc = OBDal.getInstance().createCriteria(
        FIN_BankStatementLine.class);
    obc.add(Restrictions.eq(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT, bsline.getBankStatement()));
    obc.add(Restrictions.eq(FIN_BankStatementLine.PROPERTY_LINENO, bsline.getLineNo()));
    obc.add(Restrictions.ne(FIN_BankStatementLine.PROPERTY_ID, bsline.getId()));
    obc.add(Restrictions.isNull(FIN_BankStatementLine.PROPERTY_FINANCIALACCOUNTTRANSACTION));
    // Following list should contain just 2 elements maximum
    final List<FIN_BankStatementLine> splitLines = obc.list();

    if (!splitLines.isEmpty()) {
      // bs is set a false, because it is processed bs cannot be edited
      bs.setProcessed(false);
      OBDal.getInstance().save(bs);
      OBDal.getInstance().flush();

      for (final FIN_BankStatementLine bsl : splitLines) {
        totalCredit = totalCredit.add(bsl.getCramount());
        totalDebit = totalDebit.add(bsl.getDramount());
        OBDal.getInstance().remove(bsl);
      }

      if (totalCredit.compareTo(BigDecimal.ZERO) != 0 && totalDebit.compareTo(BigDecimal.ZERO) != 0) {
        BigDecimal total = totalCredit.subtract(totalDebit);
        if (total.compareTo(BigDecimal.ZERO) == -1) {
          bsline.setCramount(BigDecimal.ZERO);
          bsline.setDramount(total.abs());
        } else {
          bsline.setCramount(total);
          bsline.setDramount(BigDecimal.ZERO);
        }
      } else {
        bsline.setCramount(totalCredit);
        bsline.setDramount(totalDebit);
      }
      // Required to persist current bank statement line merges so that it is not rollbacked
      // afterwards because of a future error
      OBDal.getInstance().save(bsline);
      OBDal.getInstance().flush();

      // bs is set a true, because it was the status at the beginning of this function
      bs.setProcessed(true);
      OBDal.getInstance().save(bs);
      OBDal.getInstance().flush();
    }

  }

  /**
   * Removes a financial account transaction
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param transaction
   *          Financial Account Transaction to be removed
   */
  public static void removeTransaction(VariablesSecureApp vars, ConnectionProvider conn,
      FIN_FinaccTransaction transaction) {
    final String FIN_FINACC_TRANSACTION_TABLE = "4D8C3B3C31D1410DA046140C9F024D17";
    try {
      if ("Y".equals(transaction.getPosted())) {
        List<AccountingFact> accountingEntries = FIN_Utility.getAllInstances(
            AccountingFact.class,
            false,
            false,
            new Value(AccountingFact.PROPERTY_TABLE, OBDal.getInstance().get(Table.class,
                FIN_FINACC_TRANSACTION_TABLE)), new Value(AccountingFact.PROPERTY_RECORDID,
                transaction.getId()));
        for (AccountingFact accountingEntry : accountingEntries) {
          OBDal.getInstance().remove(accountingEntry);
        }
        transaction.setPosted("N");
        OBDal.getInstance().save(transaction);
        // the transaction must have the value posted=N to execute processTransaction function
        OBDal.getInstance().flush();
      }
      OBError msg = processTransaction(vars, conn, "R", transaction);
      if ("Success".equals(msg.getType())) {
        OBContext.setAdminMode(false);
        try {
          OBDal.getInstance().remove(transaction);
          // to ensure that the transaction has been deleted
          OBDal.getInstance().flush();
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    } catch (Exception e) {
      throw new OBException("Process failed deleting the financial account Transaction", e);
    }
  }

  /**
   * Removes a payment
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param payment
   *          Payment to be removed
   */
  public static void removePayment(VariablesSecureApp vars, ConnectionProvider conn,
      FIN_Payment payment) {
    final String FIN_PAYMENT_TABLE = "D1A97202E832470285C9B1EB026D54E2";
    try {
      if ("Y".equals(payment.getPosted())) {
        List<AccountingFact> accountingEntries = FIN_Utility.getAllInstances(
            AccountingFact.class,
            false,
            false,
            new Value(AccountingFact.PROPERTY_TABLE, OBDal.getInstance().get(Table.class,
                FIN_PAYMENT_TABLE)), new Value(AccountingFact.PROPERTY_RECORDID, payment.getId()));
        for (AccountingFact accountingEntry : accountingEntries) {
          OBDal.getInstance().remove(accountingEntry);
        }
        payment.setPosted("N");
        OBDal.getInstance().save(payment);
        // the payment must have the value posted=N to execute processPayment function
        OBDal.getInstance().flush();
      }
      OBError msg = FIN_AddPayment.processPayment(vars, conn, "R", payment);
      if ("Success".equals(msg.getType())) {
        OBContext.setAdminMode(false);
        try {
          OBDal.getInstance().remove(payment);
          // to ensure that the payment has been deleted
          OBDal.getInstance().flush();
        } finally {
          OBContext.restorePreviousMode();
        }
      }
    } catch (Exception e) {
      throw new OBException("Process failed deleting payment", e);
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
  public static OBError processTransaction(VariablesSecureApp vars, ConnectionProvider conn,
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

  /**
   * Get the bank statement lines that are not yet matched to a transaction for the given financial
   * account. If the reconciliation passed as parameter is the last one, the method filters by the
   * bank statement lines with transaction date less or equal than the reconciliation's ending date
   */
  public static ScrollableResults getPendingToBeMatchedBankStatementLines(
      String strFinancialAccountId, String strReconciliationId) {
    try {
      OBContext.setAdminMode(true);
      FIN_Reconciliation reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class,
          strReconciliationId);
      boolean isLastReconciliation = MatchTransactionDao.islastreconciliation(reconciliation);
      final StringBuilder whereClause = new StringBuilder();
      whereClause.append(" as bsl ");
      whereClause.append(" where bsl.").append(FIN_BankStatementLine.PROPERTY_BANKSTATEMENT);
      whereClause.append(".").append(FIN_BankStatement.PROPERTY_ACCOUNT).append(".id = :account");
      whereClause.append(" and bsl.bankStatement.processed = 'Y'");
      if (!isLastReconciliation) {
        whereClause.append(" and  bsl.").append(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE)
            .append(" <= :endingdate");
      }
      whereClause.append("   and bsl.financialAccountTransaction is null");
      whereClause.append(" order by bsl.").append(FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE);
      whereClause.append(", bsl.").append(FIN_BankStatementLine.PROPERTY_LINENO);
      whereClause.append(", bsl.").append(FIN_BankStatementLine.PROPERTY_BPARTNERNAME);
      final OBQuery<FIN_BankStatementLine> obData = OBDal.getInstance().createQuery(
          FIN_BankStatementLine.class, whereClause.toString());
      obData.setNamedParameter("account", strFinancialAccountId);
      if (!isLastReconciliation) {
        obData.setNamedParameter("endingdate", reconciliation.getEndingDate());
      }
      return obData.scroll(ScrollMode.FORWARD_ONLY);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Creates a new draft reconciliation for the given financial account
   */
  public static FIN_Reconciliation addNewDraftReconciliation(
      final FIN_FinancialAccount financialAccount) {
    final FIN_Reconciliation newData = OBProvider.getInstance().get(FIN_Reconciliation.class);
    try {
      OBContext.setAdminMode(true);
      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(financialAccount.getClient().getId());
      parameters.add(financialAccount.getOrganization().getId());
      parameters.add("REC");
      final String strDocType = (String) CallStoredProcedure.getInstance().call("AD_GET_DOCTYPE",
          parameters, null);
      if (strDocType == null || strDocType.equals("")) {
        throw new OBException(OBMessageUtils.messageBD("APRM_NoDocTypeRec"));
      }
      DocumentType docType = OBDal.getInstance().get(DocumentType.class, strDocType);
      final String strDocumentNo = FIN_Utility.getDocumentNo(financialAccount.getOrganization(),
          docType.getDocumentCategory(), "FIN_Reconciliation", true);
      if (strDocumentNo == null || strDocumentNo.equals("")) {
        throw new OBException(OBMessageUtils.messageBD("APRM_NoDocNumberDocType"));
      }
      String strDocStatus = "DR";
      newData.setActive(true);
      newData.setOrganization(financialAccount.getOrganization());
      newData.setClient(financialAccount.getClient());
      newData.setAccount(financialAccount);
      newData.setDocumentNo(strDocumentNo);
      newData.setDocumentType(MatchTransactionDao.getObject(DocumentType.class, strDocType));
      newData.setDocumentStatus(strDocStatus);
      Date endingDate = MatchTransactionDao.getBankStatementLineMaxDate(financialAccount);
      newData.setTransactionDate(endingDate != null ? endingDate : new Date());
      newData.setEndingDate(endingDate != null ? endingDate : new Date());
      BigDecimal startingBalance = MatchTransactionDao
          .getReconciliationLastAmount(financialAccount);
      OBDal.getInstance().save(newData);
      OBDal.getInstance().flush();
      newData.setEndingBalance(MatchTransactionDao.getEndingBalance(newData));
      newData.setStartingbalance(startingBalance != null ? startingBalance : BigDecimal.ZERO);

      OBDal.getInstance().save(newData);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }

    return newData;
  }

  /**
   * Updates the dates of a given reconciliation and if the process parameter is true it processes
   * the reconciliation
   */
  public static boolean updateReconciliation(final FIN_Reconciliation reconciliation,
      final FIN_FinancialAccount financialAccount, boolean process) {
    try {
      OBContext.setAdminMode(true);

      Date maxDate = MatchTransactionDao.islastreconciliation(reconciliation) ? MatchTransactionDao
          .getBankStatementLineMaxDate(financialAccount) : getClearItemsMaxDate(reconciliation);
      if (maxDate == null) {
        maxDate = new Date();
      }
      reconciliation.setEndingDate(maxDate);
      reconciliation.setTransactionDate(maxDate);
      reconciliation.setEndingBalance(MatchTransactionDao.getEndingBalance(reconciliation));

      if (!process) {
        reconciliation.setProcessed(false);
        reconciliation.setDocumentStatus("DR");
        reconciliation.setAPRMProcessReconciliation("P");
        reconciliation.setAprmProcessRec("P");
      }
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();
      if (process) {
        // Process Reconciliation
        OBError myError = APRM_MatchingUtility.processReconciliation("P", reconciliation);
        if (myError != null && myError.getType().equalsIgnoreCase("error")) {
          throw new OBException(myError.getMessage());
        }
      }
    } catch (Exception ex) {
      throw new OBException(ex);
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  private static Date getClearItemsMaxDate(FIN_Reconciliation reconciliation) {
    OBCriteria<FIN_ReconciliationLine_v> obc = OBDal.getInstance().createCriteria(
        FIN_ReconciliationLine_v.class);
    obc.createAlias(FIN_ReconciliationLine_v.PROPERTY_BANKSTATEMENTLINE, "bsl");
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    obc.add(Restrictions.eq(FIN_ReconciliationLine_v.PROPERTY_RECONCILIATION, reconciliation));
    obc.addOrder(Order.desc("bsl." + FIN_BankStatementLine.PROPERTY_TRANSACTIONDATE));
    obc.setMaxResults(1);
    FIN_ReconciliationLine_v line = ((FIN_ReconciliationLine_v) obc.uniqueResult());
    // If there are no lines use reconciliation ending Date
    return line != null ? line.getBankStatementLine().getTransactionDate() : reconciliation
        .getEndingDate();
  }

  /**
   * Split the given bank statement line only when it does not match with the amount of the given
   * transaction. It will create a clone of the given bank statement line with the difference
   * amount. The original bank statement line amounts will be set equal to the amounts in the
   * transaction
   * 
   */
  private static void splitBankStatementLine(final FIN_Reconciliation reconciliation,
      final FIN_BankStatementLine bankStatementLine, final FIN_FinaccTransaction transaction) {
    try {
      OBContext.setAdminMode(true);
      if (reconciliation == null || bankStatementLine == null || transaction == null) {
        throw new OBException("splitBankStatementLine method requires not null parameters");
      }

      final BigDecimal bslAmount = bankStatementLine.getCramount().subtract(
          bankStatementLine.getDramount());
      final BigDecimal trxAmount = transaction.getDepositAmount().subtract(
          transaction.getPaymentAmount());

      // If amounts don't match we continue with the split
      if (bslAmount.compareTo(trxAmount) != 0) {
        if ("Y".equals(reconciliation.getPosted())) {
          // reconciliation posted not possible to split a row
          throw new OBException(OBMessageUtils.messageBD("APRM_SplitBSLReconciliationPosted"));
        }
        if (bankStatementLine.getFinancialAccountTransaction() != null
            && bankStatementLine.getFinancialAccountTransaction().getReconciliation() != null) {
          throw new OBException(OBMessageUtils.messageBD("APRM_SplitBSLAlreadyMatched"));
        }

        // prevent trigger
        FIN_BankStatement bs = bankStatementLine.getBankStatement();
        bs.setProcessed(false);
        OBDal.getInstance().save(bs);
        OBDal.getInstance().flush();

        // Duplicate bank statement line with pending amount
        FIN_BankStatementLine clonedBSLine = (FIN_BankStatementLine) DalUtil.copy(
            bankStatementLine, true);
        final BigDecimal credit = bankStatementLine.getCramount().subtract(
            transaction.getDepositAmount());
        final BigDecimal debit = bankStatementLine.getDramount().subtract(
            transaction.getPaymentAmount());
        clonedBSLine.setCramount(credit);
        clonedBSLine.setDramount(debit);

        if (credit.compareTo(BigDecimal.ZERO) != 0 && debit.compareTo(BigDecimal.ZERO) != 0) {
          BigDecimal total = credit.subtract(debit);
          if (total.compareTo(BigDecimal.ZERO) == -1) {
            clonedBSLine.setCramount(BigDecimal.ZERO);
            clonedBSLine.setDramount(total.abs());
          } else {
            clonedBSLine.setCramount(total);
            clonedBSLine.setDramount(BigDecimal.ZERO);
          }
        } else {
          if (credit.compareTo(BigDecimal.ZERO) == -1) {
            clonedBSLine.setCramount(BigDecimal.ZERO);
            clonedBSLine.setDramount(credit.abs());
          }
          if (debit.compareTo(BigDecimal.ZERO) == -1) {
            clonedBSLine.setDramount(BigDecimal.ZERO);
            clonedBSLine.setCramount(debit.abs());
          }

        }

        // Set bankstatement line amounts with the matched transaction amounts
        bankStatementLine.setCramount(transaction.getDepositAmount());
        bankStatementLine.setDramount(transaction.getPaymentAmount());

        bs.setProcessed(true);

        // Save
        OBDal.getInstance().save(bs);
        OBDal.getInstance().save(clonedBSLine);
        OBDal.getInstance().save(bankStatementLine);
      }

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns a JSONArray with a message to be shown in the process view
   * 
   * @param messageSearchKey
   *          String with the message to translate (see
   *          {@link OBMessageUtils#translateError(String)}
   * @param msgType
   *          error, warning, success, info
   * @param messageParams
   *          parameters to be applied to the message text using
   *          {@link String#format(String, Object...)}
   * @return a JSONArray with a message to be shown in the process view
   * @throws JSONException
   */
  public static JSONArray createMessageInProcessView(final String messageSearchKey,
      final String msgType, Object... messageParams) throws JSONException {
    final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    final String message = Utility.parseTranslation(new DalConnectionProvider(false), vars,
        vars.getLanguage(), messageSearchKey);

    final JSONObject msg = new JSONObject();
    msg.put("msgType", msgType);
    msg.put("msgTitle", "");
    msg.put("msgText", String.format(message, messageParams));
    msg.put("force", true);

    final JSONObject msgTotalAction = new JSONObject();
    msgTotalAction.put("showMsgInProcessView", msg);

    final JSONArray actions = new JSONArray();
    actions.put(msgTotalAction);
    return actions;
  }

  /**
   * Creates a new financial transaction from the given parameters. If the strFinBankStatementLineId
   * is not blank, then it tries to match the new transaction to the bank statement line
   * 
   */
  public static void createAndMatchFinancialTransaction(String strFinancialAccountId,
      String strTransactionType, Date transactionDate, String strFinBankStatementLineId,
      Organization organization, final FIN_FinancialAccount account, FIN_Payment payment,
      String description, GLItem glItem, boolean isReceipt, BigDecimal depositAmt,
      BigDecimal paymentAmt, Currency paymentCurrency, BigDecimal convertRate,
      BigDecimal sourceAmount, Campaign campaign, Project project, ABCActivity activity,
      SalesRegion salesRegion, Product product, BusinessPartner businessPartner,
      UserDimension1 user1, UserDimension2 user2, Costcenter costcenter,
      final FIN_BankStatementLine bankStatementLine, VariablesSecureApp vars,
      ConnectionProvider conn, boolean throwException) throws Exception {
    final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    final FIN_FinaccTransaction finTrans = dao
        .getNewFinancialTransaction(organization, account, TransactionsDao
            .getTransactionMaxLineNo(account) + 10, payment, description, transactionDate, glItem,
            isReceipt ? APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED
                : APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED, depositAmt, paymentAmt,
            project, campaign, activity, StringUtils.equals(strTransactionType,
                APRMConstants.TRXTYPE_BankFee) ? APRMConstants.TRXTYPE_BankFee
                : isReceipt ? APRMConstants.TRXTYPE_BPDeposit : APRMConstants.TRXTYPE_BPWithdrawal,
            transactionDate, paymentCurrency, convertRate, sourceAmount, businessPartner, product,
            salesRegion, user1, user2, costcenter);
    final OBError processTransactionError = processTransaction(vars, conn, "P", finTrans);
    if (processTransactionError != null && "Error".equals(processTransactionError.getType())) {
      throw new OBException(processTransactionError.getMessage());
    }

    if (StringUtils.isNotBlank(strFinBankStatementLineId)) {
      final FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(OBDal
          .getInstance().get(FIN_FinancialAccount.class, strFinancialAccountId), "N");
      APRM_MatchingUtility.matchBankStatementLine(bankStatementLine, finTrans, reconciliation,
          "AD", true);
    }
  }

}
