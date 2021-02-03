/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

import sa.elm.ob.utility.util.Utility;

/**
 * A Handler class that posts the selected receipts as a batch.
 * 
 * @author Gopinagh.R
 */

public class AddReceiptsHandler extends BaseProcessActionHandler {
  private static final Logger log = Logger.getLogger(AddReceiptsHandler.class);
  private static final SimpleDateFormat jsDateFormat = JsonUtils.createDateFormat();
  private static final String ACTION_PROCESS_TRANSACTION = "P";
  static String SeqNo = "0";

  SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String data) {
    try {
      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);
      final JSONObject jsonparams = jsonData.getJSONObject("_params");

      final JSONArray selectedPayments = jsonparams.getJSONObject("receipts")
          .getJSONArray("_selection");
      final  Date statementDate = jsDateFormat.parse(jsonparams.getString("statementDate"));
      final Date dateAcct = jsDateFormat.parse(jsonparams.getString("DateAcct"));
      final String strBatchName = jsonparams.getString("receipt_batch_name");
      final String strAccountId = jsonData.getString("Fin_Financial_Account_ID");
      final String strOrgId = jsonData.getString("inpadOrgId");
      final String strAcctConfigId = Utility.getAccountingConfig(strOrgId, strAccountId);
      String strGroupId = SequenceIdData.getUUID(), strNpsNo = "", strGSNo = "";

      final FIN_FinancialAccountAccounting config = Utility
          .getObject(FIN_FinancialAccountAccounting.class, strAcctConfigId);
      FIN_FinaccTransaction transaction = null;
      AccountingFact fact, crFact = null;
      Table table = Utility.getObject(Table.class, "4D8C3B3C31D1410DA046140C9F024D17");
      Period period = null;

      List<FIN_FinaccTransaction> transactionsList = new ArrayList<FIN_FinaccTransaction>();

      int selectedPaymentsLength = selectedPayments.length();
      if (selectedPaymentsLength == 0) {
        // Validation error: No lines selected
        return getErrorMessage(
            OBMessageUtils.messageBD("APRM_NO_PAYMENTS_SELECTED").replace("payments", "receipts"));
      }

      if (config.getDepositAccount() == null || config.getInTransitPaymentAccountIN() == null) {
        return getErrorMessage(OBMessageUtils.messageBD("PostingError-i"));
      }

      String strPostingdate = jsonparams.getString("DateAcct");

      Date batchPostDate = yearFormat.parse(strPostingdate);
      String strBatchPostDate = Utility.convertToGregorian(dateFormat.format(batchPostDate));
      Date batchPostDateGreg = yearFormat.parse(strBatchPostDate);

      strNpsNo = Utility.getGeneralSequence(dateFormat.format(batchPostDateGreg), "NPS",
          config.getAccount().getOrganization().getCalendar().getId(), strOrgId, Boolean.TRUE);
      strGSNo = Utility.getGeneralSequence(dateFormat.format(batchPostDateGreg), "GS",
          config.getAccount().getOrganization().getCalendar().getId(), strOrgId, Boolean.TRUE);
      period = Utility.getObject(Period.class,
          Utility.getPeriod(dateFormat.format(batchPostDateGreg), strOrgId));

      for (int i = 0; i < selectedPaymentsLength; i++) {
        final JSONObject paymentJS = selectedPayments.getJSONObject(i);
        FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
            strAccountId);
        if (account.getType().equals("B") && account.getEfinBank() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Bank_Mandatory_finacct").replace("@",
              account.getName()));
        }

        transaction = createAndProcessTransactionFromPayment(paymentJS, statementDate, dateAcct,
            strAccountId);
        transactionsList.add(transaction);
      }

      if (!config.getAccount().getEfinAcctType().equals("MOF")) {
        for (FIN_FinaccTransaction trx : transactionsList) {
          fact = OBProvider.getInstance().get(AccountingFact.class);
          crFact = OBProvider.getInstance().get(AccountingFact.class);

          fact.setClient(trx.getClient());
          fact.setOrganization(trx.getOrganization());
          fact.setCreatedBy(OBContext.getOBContext().getUser());
          fact.setUpdatedBy(OBContext.getOBContext().getUser());
          fact.setAccountingSchema(Utility.getObject(AcctSchema.class,
              Utility.getAccountingSchema(trx.getOrganization().getId())));
          fact.setAccount(config.getEfinDepositUnique().getAccount());
          fact.setTransactionDate(trx.getTransactionDate());
          fact.setAccountingDate(batchPostDateGreg);
          fact.setTable(table);
          fact.setRecordID(transaction.getId());
          fact.setLineID(config.getId());
          fact.setPostingType("A");
          fact.setCurrency(trx.getCurrency());
          fact.setDebit(trx.getDepositAmount());
          fact.setForeignCurrencyDebit(trx.getForeignAmount());
          fact.setGroupID(strGroupId);
          fact.setSequenceNumber(Long.valueOf(nextSeqNo(SeqNo)));
          fact.setDocumentCategory("FAT");
          fact.setEFINUniqueCode(config.getEfinDepositUnique().getEfinUniqueCode());
          fact.setEfinAcctseq(strNpsNo);
          fact.setEfinDocumentno(strGSNo);
          fact.setProject(config.getEfinDepositUnique().getProject());
          fact.setStDimension(config.getEfinDepositUnique().getStDimension());
          fact.setNdDimension(config.getEfinDepositUnique().getNdDimension());
          fact.setSalesCampaign(config.getEfinDepositUnique().getSalesCampaign());
          fact.setSalesRegion(config.getEfinDepositUnique().getSalesRegion());
          fact.setActivity(config.getEfinDepositUnique().getActivity());
          fact.setPeriod(period);
          fact.setModify(Boolean.TRUE);
          fact.setDescription(strBatchName);

          crFact.setClient(trx.getClient());
          crFact.setOrganization(trx.getOrganization());
          crFact.setCreatedBy(OBContext.getOBContext().getUser());
          crFact.setUpdatedBy(OBContext.getOBContext().getUser());
          crFact.setAccountingSchema(Utility.getObject(AcctSchema.class,
              Utility.getAccountingSchema(trx.getOrganization().getId())));
          crFact.setAccount(config.getEfinInIntransitUnique().getAccount());
          crFact.setTransactionDate(trx.getTransactionDate());
          crFact.setAccountingDate(batchPostDateGreg);
          crFact.setTable(table);
          crFact.setRecordID(transaction.getId());
          crFact.setLineID(config.getId());
          crFact.setPostingType("A");
          crFact.setCurrency(trx.getCurrency());
          crFact.setCredit(trx.getDepositAmount());
          crFact.setForeignCurrencyCredit(trx.getForeignAmount());
          crFact.setGroupID(strGroupId);
          crFact.setSequenceNumber(Long.valueOf(nextSeqNo(SeqNo)));
          crFact.setDocumentCategory("FAT");
          crFact.setEFINUniqueCode(config.getEfinInIntransitUnique().getEfinUniqueCode());
          crFact.setEfinAcctseq(strNpsNo);
          crFact.setEfinDocumentno(strGSNo);
          crFact.setProject(config.getEfinInIntransitUnique().getProject());
          crFact.setStDimension(config.getEfinInIntransitUnique().getStDimension());
          crFact.setNdDimension(config.getEfinInIntransitUnique().getNdDimension());
          crFact.setSalesCampaign(config.getEfinInIntransitUnique().getSalesCampaign());
          crFact.setSalesRegion(config.getEfinInIntransitUnique().getSalesRegion());
          crFact.setActivity(config.getEfinInIntransitUnique().getActivity());
          crFact.setPeriod(period);
          crFact.setModify(Boolean.TRUE);
          crFact.setDescription(strBatchName);

          OBDal.getInstance().save(fact);
          OBDal.getInstance().save(crFact);

          trx.setPosted("Y");
          trx.setEfinAcctSeq(strNpsNo);

          OBDal.getInstance().save(trx);
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      // Success Message
      return getSuccessMessage(String.format(
          OBMessageUtils.messageBD("APRM_MULTIPLE_TRANSACTIONS_ADDED"), selectedPaymentsLength));

    } catch (OBException obex) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception creating multiple transactions from payments", obex);

      try {
        String message = obex.getMessage();
        return getErrorMessage(message);
      } catch (Exception ignore) {
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception creating multiple transactions from payments", e);

      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        return getErrorMessage(message);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return new JSONObject();
  }

  /**
   * Creates a new transaction from the payment and then it processes the transaction
   */
  private FIN_FinaccTransaction createAndProcessTransactionFromPayment(final JSONObject paymentJS,
      final Date transactionDate, final Date acctDate, String strAccountId) throws JSONException {

    try {
      OBContext.setAdminMode(true);
      final String paymentId = paymentJS.getString("id");
      log.debug("Creating transaction for FIN_Payment_ID: " + paymentId);
      final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strAccountId);

      if (payment != null) {
        final FIN_FinaccTransaction transaction = TransactionsDao.createFinAccTransaction(payment);

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formatedtrdate = df.format(transactionDate);
        String formatedactdte = df.format(acctDate);
        String gregTransDate = Utility.convertToGregorian(formatedtrdate);
        String gregAcctDate = Utility.convertToGregorian(formatedactdte);
        Date transDate = null;
        Date actDate = null;
        try {
          DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
          transDate = df1.parse(gregTransDate);
          actDate = df1.parse(gregAcctDate);
        } catch (Exception e) {
          log.error("Exception creating multiple transactions from payments", e);
        }

        transaction.setTransactionDate(transDate);
        transaction.setDateAcct(actDate);
        transaction.setAccount(account);
        transaction.setPosted("Y");
        // throw new OBException("entwering"+gregTransDate+" "+acctDate+"trans"+formatedtrdate + "
        // return"+transDate);

        FIN_TransactionProcess.doTransactionProcess(ACTION_PROCESS_TRANSACTION, transaction);
        return transaction;
      }
    } catch (Exception e) {
      log.error(e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  /**
   * Returns a JSONObject with the success message to be printed
   */
  private static JSONObject getSuccessMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONArray actions = new JSONArray();
      final JSONObject msgInBPTab = new JSONObject();
      msgInBPTab.put("msgType", "success");
      msgInBPTab.put("msgTitle", OBMessageUtils.messageBD("success"));
      msgInBPTab.put("msgText", msgText);
      final JSONObject msgInBPTabAction = new JSONObject();
      msgInBPTabAction.put("showMsgInProcessView", msgInBPTab);
      actions.put(msgInBPTabAction);
      result.put("responseActions", actions);
    } catch (Exception e) {
      log.error(e);
    }

    return result;
  }

  /**
   * Returns a JSONObject with the error message to be printed and retry execution
   */
  private static JSONObject getErrorMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONObject msg = new JSONObject();
      msg.put("severity", "error");
      msg.put("text", msgText);
      result.put("message", msg);
      result.put("retryExecution", true);
    } catch (Exception e) {
      log.error(e);
    }
    return result;
  }

  public String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }
}
