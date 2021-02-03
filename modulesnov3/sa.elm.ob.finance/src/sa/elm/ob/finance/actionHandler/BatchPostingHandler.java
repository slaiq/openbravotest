package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

import sa.elm.ob.finance.ActualTransaction;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * A Handler class that posts the selected receipts as a batch.
 * 
 * @author Gopinagh.R
 */

public class BatchPostingHandler extends BaseProcessActionHandler {
  Logger log = Logger.getLogger(BatchPostingHandler.class);
  String SeqNo = "0";
  private final String SUCCESS = "success";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject(), finalResponse = new JSONObject();
    JSONArray responseArray = new JSONArray();

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String msg = "";
    Boolean isFromMenu = false;

    try {
      OBContext.setAdminMode();

      JSONObject request = new JSONObject(content);
      if (!"Refresh".equals(request.getString("_buttonValue"))) {
        JSONObject jsonparams = request.getJSONObject("_params");
        JSONObject payment = jsonparams.getJSONObject("EM_Efin_Batch_Post");
        JSONArray selectedlines = payment.getJSONArray("_selection");

        String financialAccount_Id = jsonparams.getString("FIN_Financial_Account_ID");
        if (StringUtils.isEmpty(financialAccount_Id) || "null".equals(financialAccount_Id)) {
          isFromMenu = true;
        }

        String strAcctConfigId = "", strGroupId = SequenceIdData.getUUID(), strNpsNo = "",
            strGSNo = "";
        String strPostingdate = jsonparams.getString("Posting_Date");
        final String strBatchName = jsonparams.getString("receipt_batch_name");

        Date batchPostDate = yearFormat.parse(strPostingdate);
        String strBatchPostDate = Utility.convertToGregorian(dateFormat.format(batchPostDate)),
            firstPaymentId = "";
        Date batchPostDateGreg = yearFormat.parse(strBatchPostDate);

        FIN_Payment finPayment = null;
        FIN_FinancialAccountAccounting config = null;
        Invoice invoice = null;
        AccountingFact fact, lineFact = null;
        Table table = Utility.getObject(Table.class, "D1A97202E832470285C9B1EB026D54E2");

        Period period = null;

        if (StringUtils.isEmpty(strBatchName) || "null".equals(strBatchName)) {
          JSONObject errorMessage = new JSONObject();

          errorMessage.put("severity", "error");
          errorMessage.put("title", OBMessageUtils.parseTranslation("@Error@"));
          errorMessage.put("text", OBMessageUtils.messageBD("EFIN_RECEIPTNAMEMANDATORY")
              .replace("payments", "receipts"));

          jsonResponse.put("message", errorMessage);
          jsonResponse.put("retryExecution", true);
          jsonResponse.put("refreshParent", true);
          return jsonResponse;
        }

        if (selectedlines.length() == 0) {
          JSONObject errorMessage = new JSONObject();

          errorMessage.put("severity", "error");
          errorMessage.put("title", OBMessageUtils.parseTranslation("@Error@"));
          errorMessage.put("text", OBMessageUtils.messageBD("APRM_NO_PAYMENTS_SELECTED")
              .replace("payments", "receipts"));

          jsonResponse.put("message", errorMessage);
          jsonResponse.put("retryExecution", true);
          jsonResponse.put("refreshParent", true);
          return jsonResponse;
        }

        for (int i = 0; i < selectedlines.length(); i++) {

          JSONObject selectedRow = selectedlines.getJSONObject(i);

          if (log.isDebugEnabled())
            log.debug("Selected Payments: " + selectedRow.getString("id"));

          finPayment = Utility.getObject(FIN_Payment.class, selectedRow.getString("id"));
          strAcctConfigId = Utility.getAccountingConfig(finPayment.getOrganization().getId(),
              finPayment.getAccount().getId());
          config = Utility.getObject(FIN_FinancialAccountAccounting.class, strAcctConfigId);

          if (config.getInTransitPaymentAccountIN() == null) {
            JSONObject errorMessage = new JSONObject();

            errorMessage.put("severity", "error");
            errorMessage.put("title", OBMessageUtils.parseTranslation("@Error@"));
            errorMessage.put("text", OBMessageUtils.messageBD("InvalidAccount"));

            jsonResponse.put("message", errorMessage);
            return jsonResponse;
          }

          if (i == 0) {
            strNpsNo = Utility.getGeneralSequence(dateFormat.format(batchPostDateGreg), "NPS",
                finPayment.getOrganization().getCalendar().getId(),
                finPayment.getOrganization().getId(), Boolean.TRUE);
            if ("0".equals(strNpsNo)) {
              JSONObject errorMessage = new JSONObject();

              errorMessage.put("severity", "error");
              errorMessage.put("title", OBMessageUtils.parseTranslation("@Error@"));
              errorMessage.put("text", OBMessageUtils.messageBD("Efin_AccountSeq_notdefined"));

              jsonResponse.put("message", errorMessage);
              return jsonResponse;

            }

            strGSNo = Utility.getGeneralSequence(dateFormat.format(batchPostDateGreg), "GS",
                finPayment.getOrganization().getCalendar().getId(),
                finPayment.getOrganization().getId(), Boolean.TRUE);
            firstPaymentId = finPayment.getId();
            period = Utility.getObject(Period.class, Utility.getPeriod(
                dateFormat.format(batchPostDateGreg), finPayment.getOrganization().getId()));
          }

          invoice = finPayment.getFINPaymentDetailVList().get(0).getInvoicePaymentPlan()
              .getInvoice();

          if (finPayment.getFINFinaccTransactionList() != null
              && finPayment.getFINFinaccTransactionList().size() > 0) {
            for (FIN_FinaccTransaction transaction : finPayment.getFINFinaccTransactionList()) {
              FIN_FinaccTransaction tran = transaction;
              tran.setProcessed(true);
              tran.setPosted("Y");
              OBDal.getInstance().save(tran);
            }
            OBDal.getInstance().flush();
          }

          fact = OBProvider.getInstance().get(AccountingFact.class);

          fact.setClient(finPayment.getClient());
          fact.setOrganization(finPayment.getOrganization());
          fact.setCreatedBy(OBContext.getOBContext().getUser());
          fact.setUpdatedBy(OBContext.getOBContext().getUser());
          fact.setAccountingSchema(Utility.getObject(AcctSchema.class,
              Utility.getAccountingSchema(finPayment.getOrganization().getId())));
          fact.setAccount(config.getEfinInIntransitUnique().getAccount());
          fact.setTransactionDate(finPayment.getPaymentDate());
          fact.setAccountingDate(batchPostDateGreg);
          fact.setTable(table);
          fact.setRecordID(firstPaymentId);
          fact.setLineID(config.getId());
          fact.setPostingType("A");
          fact.setCurrency(finPayment.getCurrency());
          fact.setDebit(finPayment.getAmount());
          fact.setForeignCurrencyDebit(finPayment.getAmount());
          fact.setGroupID(strGroupId);
          fact.setSequenceNumber(Long.valueOf(nextSeqNo(SeqNo)));
          fact.setDocumentCategory("ARR");
          fact.setEFINUniqueCode(config.getEfinInIntransitUnique().getEfinUniqueCode());
          fact.setEfinAcctseq(strNpsNo);
          fact.setEfinDocumentno(strGSNo);
          fact.setModify(Boolean.TRUE);
          fact.setProject(config.getEfinInIntransitUnique().getProject());
          fact.setStDimension(config.getEfinInIntransitUnique().getStDimension());
          fact.setNdDimension(config.getEfinInIntransitUnique().getNdDimension());
          fact.setSalesCampaign(config.getEfinInIntransitUnique().getSalesCampaign());
          fact.setSalesRegion(config.getEfinInIntransitUnique().getSalesRegion());
          fact.setActivity(config.getEfinInIntransitUnique().getActivity());
          fact.setPeriod(period);
          fact.setDescription(strBatchName);

          OBDal.getInstance().save(fact);

          for (InvoiceLine line : invoice.getInvoiceLineList()) {

            // check common validation

            msg = checkFundsAvailable(line);

            // If not success then throw the error
            if (!SUCCESS.equals(msg)) {
              JSONObject errorMessage = new JSONObject();

              errorMessage.put("severity", "error");
              errorMessage.put("title", OBMessageUtils.parseTranslation("@Error@"));
              errorMessage.put("text", msg);

              jsonResponse.put("message", errorMessage);
              return jsonResponse;

            }

            lineFact = OBProvider.getInstance().get(AccountingFact.class);

            lineFact.setClient(line.getClient());
            lineFact.setOrganization(line.getOrganization());
            lineFact.setCreatedBy(OBContext.getOBContext().getUser());
            lineFact.setUpdatedBy(OBContext.getOBContext().getUser());
            lineFact.setAccountingSchema(Utility.getObject(AcctSchema.class,
                Utility.getAccountingSchema(finPayment.getOrganization().getId())));
            lineFact.setAccount(line.getEfinCElementvalue());
            lineFact.setTransactionDate(invoice.getInvoiceDate());
            lineFact.setAccountingDate(batchPostDateGreg);
            lineFact.setTable(table);
            lineFact.setRecordID(finPayment.getId());
            lineFact.setLineID(line.getId());
            lineFact.setPostingType("A");
            lineFact.setCurrency(invoice.getCurrency());

            if (line.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {
              lineFact.setCredit(line.getLineNetAmount());
              lineFact.setForeignCurrencyCredit(line.getLineNetAmount());
            } else {
              lineFact.setDebit(line.getLineNetAmount().negate());
              lineFact.setForeignCurrencyDebit(line.getLineNetAmount().negate());
            }

            lineFact.setGroupID(strGroupId);
            lineFact.setSequenceNumber(Long.valueOf(nextSeqNo(SeqNo)));
            lineFact.setDocumentCategory("ARR");
            lineFact.setEFINUniqueCode(line.getEFINUniqueCode());
            lineFact.setEfinDocumentno(invoice.getDocumentNo());
            lineFact.setProject(line.getProject());
            lineFact.setStDimension(line.getStDimension());
            lineFact.setNdDimension(line.getNdDimension());
            lineFact.setSalesCampaign(line.getEfinCCampaign());
            lineFact.setSalesRegion(line.getEfinCSalesregion());
            lineFact.setActivity(line.getEfinCActivity());
            lineFact.setEfinAcctseq(strNpsNo);
            lineFact.setEfinDocumentno(strGSNo);
            lineFact.setPeriod(period);
            lineFact.setModify(Boolean.TRUE);
            lineFact.setDescription(strBatchName);

            OBDal.getInstance().save(lineFact);

            AccountingCombination uniqueCode = line.getEfinCValidcombination();
            if (uniqueCode != null && uniqueCode.getEfinDimensiontype().equals("E")) {

              // for Dept funds='N' and non 990,999 dept
              if (!uniqueCode.isEFINDepartmentFund()) {
                EfinBudgetInquiry budgetInquiry = ManualEncumbaranceSubmitDAO
                    .getBudgetInquiry(uniqueCode.getId(), invoice.getEfinBudgetint().getId());

                if (budgetInquiry != null) {
                  if (line.getLineNetAmount().compareTo(new BigDecimal("0")) > 0) {
                    budgetInquiry
                        .setSpentAmt(budgetInquiry.getSpentAmt().subtract(line.getLineNetAmount()));
                    OBDal.getInstance().save(budgetInquiry);
                  } else {
                    budgetInquiry.setSpentAmt(
                        budgetInquiry.getSpentAmt().add(line.getLineNetAmount().negate()));
                    OBDal.getInstance().save(budgetInquiry);
                  }
                } else {
                  // Get Parent Id for new budget Inquiry
                  EfinBudgetInquiry parentInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                      line.getEfinCValidcombination(), line.getInvoice().getEfinBudgetint());

                  EfinBudgetInquiry budInq = OBProvider.getInstance().get(EfinBudgetInquiry.class);
                  budInq.setEfinBudgetint(line.getInvoice().getEfinBudgetint());
                  budInq.setAccountingCombination(line.getEfinCValidcombination());
                  budInq.setUniqueCodeName(line.getEFINUniqueCode());
                  budInq.setUniqueCode(line.getEfinCValidcombination().getEfinUniqueCode());
                  budInq.setOrganization(line.getOrganization());
                  budInq.setDepartment(line.getEfinCSalesregion());
                  budInq.setAccount(line.getEfinCElementvalue());
                  budInq.setSalesCampaign(line.getEfinCCampaign());
                  budInq.setProject(line.getProject());
                  budInq.setBusinessPartner(line.getBusinessPartner());
                  budInq.setFunctionalClassfication(line.getEfinCActivity());
                  budInq.setFuture1(line.getStDimension());
                  budInq.setNdDimension(line.getNdDimension());
                  budInq.setORGAmt(BigDecimal.ZERO);
                  budInq.setREVAmount(BigDecimal.ZERO);
                  budInq.setFundsAvailable(BigDecimal.ZERO);
                  budInq.setCurrentBudget(BigDecimal.ZERO);
                  budInq.setEncumbrance(BigDecimal.ZERO);

                  budInq.setSpentAmt(line.getLineNetAmount().negate());

                  budInq.setParent(parentInq);
                  budInq.setVirtual(true);
                  OBDal.getInstance().save(budInq);
                }
              }

              EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(uniqueCode,
                  invoice.getEfinBudgetint());
              if (budgetInq != null) {
                if (line.getLineNetAmount().compareTo(new BigDecimal("0")) > 0) {
                  budgetInq.setSpentAmt(budgetInq.getSpentAmt().subtract(line.getLineNetAmount()));
                  OBDal.getInstance().save(budgetInq);
                } else {
                  budgetInq
                      .setSpentAmt(budgetInq.getSpentAmt().add(line.getLineNetAmount().negate()));
                  OBDal.getInstance().save(budgetInq);
                }
              }
            }

            // insert record in actual transaction table
            if (uniqueCode != null && uniqueCode.getEfinDimensiontype() != null
                && uniqueCode.getEfinDimensiontype().equals("E")) {
              ActualTransaction actualTran = OBProvider.getInstance().get(ActualTransaction.class);
              actualTran.setOrganization(invoice.getOrganization());
              actualTran.setAccountingDate(invoice.getAccountingDate());
              actualTran.setInvoiceDate(invoice.getInvoiceDate());
              actualTran.setDescription(line.getDescription());
              actualTran.setInvoiceLine(line);
              actualTran.setInvoice(invoice);
              actualTran.setDocumentNo(invoice.getDocumentNo());
              actualTran.setAccountingCombination(uniqueCode);
              actualTran.setBudgetInitialization(invoice.getEfinBudgetint());
              actualTran.setPost(true);
              if (line.getLineNetAmount().compareTo(new BigDecimal("0")) > 0) {
                actualTran.setAmount(line.getLineNetAmount().multiply(new BigDecimal("-1")));
              } else {
                actualTran.setAmount(line.getLineNetAmount().negate());
              }
              OBDal.getInstance().save(actualTran);
            }

          }

          finPayment.setPosted("Y");
          finPayment.setEFINAccountingSequence(strNpsNo);
          finPayment.setEfinGeneralsequence(strGSNo);

          invoice.setEfinAccseq(strNpsNo);

          OBDal.getInstance().save(finPayment);
          OBDal.getInstance().save(invoice);
        }

        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();

        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "success");
        errormsg.put("text", OBMessageUtils.messageBD("Efin_BudgetPre_Submit"));
        finalResponse.put("message", errormsg);

      }
    } catch (

    Exception e) {
      log.error("Exception while processing batch posting. " + e.getMessage());
      e.printStackTrace();
      jsonResponse = new JSONObject();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        finalResponse.put("message", errorMessage);
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
    } finally {
      try {
        jsonResponse = new JSONObject();
        responseArray.put(jsonResponse);
        if (isFromMenu) {
          finalResponse.put("retryExecution", true);
          finalResponse.put("refreshParent", true);
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
      OBContext.restorePreviousMode();
    }
    return finalResponse;
  }

  private String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }

  /**
   * This method is used to check common validation such as
   * 
   * 1. Amount entered line is negative and the (fundsavailable - amount) less than zero.
   * 
   * @param invoiceline
   * @return
   */

  private String checkFundsAvailable(InvoiceLine line) {

    // check Amount entered line is negative and the (fundsavailable - amount) less than zero..

    AccountingCombination uniqueCode = line.getEfinCValidcombination();
    if (uniqueCode != null && uniqueCode.getEfinDimensiontype().equals("E")) {
      EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(uniqueCode,
          line.getInvoice().getEfinBudgetint());

      if ((line.getLineNetAmount().compareTo(BigDecimal.ZERO) < 0) && (budgetInq.getFundsAvailable()
          .subtract(line.getLineNetAmount()).compareTo(new BigDecimal("0"))) < 0) {
        return OBMessageUtils.messageBD("Efin_Amount>FA");
      }

      if ((line.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0)) {
        BigDecimal lineTotalAmt = line.getInvoice().getInvoiceLineList().stream()
            .filter(a -> a.getEFINUniqueCode() == line.getEFINUniqueCode())
            .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        JSONObject costObj = CommonValidationsDAO.CommonFundsChecking(
            line.getInvoice().getEfinBudgetint(), uniqueCode.getEfinCostcombination(),
            lineTotalAmt);

        try {
          if ("0".equals(costObj.get("errorFlag"))) {
            return OBMessageUtils.messageBD("Efin_batchpostfundscheck");
          }
        } catch (Exception e) {
          log.debug("Error" + e.getMessage());
        }
      }

    }

    return SUCCESS;

  }
}