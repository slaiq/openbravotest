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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.actionHandler;

//org.openbravo.advpaymentmngt.actionHandler
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.Note;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinPoApproval;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.dao.AdvPaymentMngtDao;
import sa.elm.ob.finance.process.po_approval.FIN_AddPayment;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class AddPaymentActionHandler extends BaseProcessActionHandler {
  final private static Logger log = LoggerFactory.getLogger(AddPaymentActionHandler.class);
  private static final String RDV_DOCUMENT = "RDV";
  private static final String PO_DOCUMENT = "POM";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    OBContext.setAdminMode(true);

    try {
      log.debug("payment handler");
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      SimpleDateFormat ddMMyyyyFormat = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat yyyyMMddFormat = new SimpleDateFormat("yyyy-MM-dd");

      // Get Params
      JSONObject jsonRequest = new JSONObject(content);
      log.debug(" jsonRequest: " + jsonRequest);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String strOrgId = jsonRequest.getString("inpadOrgId");
      Organization org = OBDal.getInstance().get(Organization.class, strOrgId);
      boolean isReceipt = jsonparams.getBoolean("issotrx");
      log.debug(" Is Receipt : " + isReceipt);
      // Action to do
      final String strActionId = jsonparams.getString("document_action");
      final org.openbravo.model.ad.domain.List actionList = OBDal.getInstance()
          .get(org.openbravo.model.ad.domain.List.class, strActionId);
      final String strAction = actionList.getSearchKey();
      final String strInvoiceId = jsonRequest.has("C_Invoice_ID")
          ? (jsonRequest.getString("C_Invoice_ID") == null ? ""
              : jsonRequest.getString("C_Invoice_ID"))
          : "";
      final String strCurrencyId = jsonparams.getString("c_currency_id");
      Currency currency = OBDal.getInstance().get(Currency.class, strCurrencyId);
      final String strBPartnerID = jsonparams.getString("received_from");
      BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class,
          strBPartnerID);
      String strActualPayment = jsonparams.getString("actual_payment");

      // Format Date
      String strPaymentDate = jsonparams.getString("payment_date");
      Date paymentDate = JsonUtils.createDateFormat().parse(strPaymentDate);

      // Check Posting Sequence
      String AccountDate = ddMMyyyyFormat.format(paymentDate);
      Date gregPaymentDate = yyyyMMddFormat.parse(Utility.convertToGregorian(AccountDate));
      String gregorianAcctDate = ddMMyyyyFormat.format(gregPaymentDate);

      String CalendarId = "";
      boolean isPeriodOpen = false;

      if (org.getCalendar() != null) {
        CalendarId = org.getCalendar().getId();

      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
            "select eut_parent_org ('" + org.getId() + "','" + org.getClient().getId() + "')");
        Object parentOrg = query.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();
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

      // OverPayment action
      String strDifferenceAction = "";
      BigDecimal differenceAmount = BigDecimal.ZERO;

      if (jsonparams.get("difference") != JSONObject.NULL) {
        differenceAmount = new BigDecimal(jsonparams.getString("difference"));
        strDifferenceAction = jsonparams.getString("overpayment_action");

        log.debug(" strDifferenceAction :" + strDifferenceAction);

        if ("RE".equals(strDifferenceAction)) {
          strDifferenceAction = "refund";
        } else {
          strDifferenceAction = "credit";
        }
      }

      BigDecimal exchangeRate = BigDecimal.ZERO;
      BigDecimal convertedAmount = BigDecimal.ZERO;

      if (jsonparams.get("conversion_rate") != JSONObject.NULL) {
        exchangeRate = new BigDecimal(jsonparams.getString("conversion_rate"));
      }
      if (jsonparams.get("converted_amount") != JSONObject.NULL) {
        convertedAmount = new BigDecimal(jsonparams.getString("converted_amount"));
      }

      List<String> pdToRemove = new ArrayList<String>();
      FIN_Payment payment = null;

      log.debug("paymentId: " + jsonparams.get("fin_payment_id"));
      if (jsonparams.get("fin_payment_id") != JSONObject.NULL) {
        // Payment is already created. Load it.
        final String strFinPaymentID = jsonparams.getString("fin_payment_id");

        // Check transaction period is opened or not
        final OBQuery<FIN_Payment> finqry = OBDal.getInstance().createQuery(FIN_Payment.class,
            "as e where e.id=:payId");
        finqry.setNamedParameter("payId", strFinPaymentID);
        if (finqry.list().size() > 0) {
          Date payDate = finqry.list().get(0).getPaymentDate();
          String doctypeId = finqry.list().get(0).getDocumentType().getId();
          isPeriodOpen = Utility.checkOpenPeriodCore(payDate, strOrgId, null, doctypeId);
          if (!isPeriodOpen) {
            throw new OBException(OBMessageUtils.messageBD("PeriodNotAvailable"));
          }
        }

        // check if payment is already submitted
        String whereClause = "";
        whereClause = " status!='RPAP' and id=:payId ";
        final OBQuery<FIN_Payment> qry = OBDal.getInstance().createQuery(FIN_Payment.class,
            whereClause.toString());
        qry.setNamedParameter("payId", strFinPaymentID);
        if (qry.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AddPay_ActHandler"));
        }
        payment = OBDal.getInstance().get(FIN_Payment.class, strFinPaymentID);
        String strReferenceNo = "";
        if (jsonparams.get("reference_no") != JSONObject.NULL) {
          strReferenceNo = jsonparams.getString("reference_no");
        }

        Role submittedRoleObj = null;
        String submittedRoleOrgId = null;
        String orgId = payment.getOrganization().getId();
        // Task #8198
        // check submitted role have the branch details or not
        if (payment.getStatus().equals("RPAP")) {
          submittedRoleObj = OBContext.getOBContext().getRole();
          if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
            throw new OBException(OBMessageUtils.messageBD("Efin_RoleBranchNotDefine"));
          }
        }
        // find the submitted role org/branch details

        if (payment.getEutNextRole() != null) {
          if (payment.getEfinSubmittedRole() != null
              && payment.getEfinSubmittedRole().getEutReg() != null) {
            submittedRoleOrgId = payment.getEfinSubmittedRole().getEutReg().getId();
          } else {
            submittedRoleOrgId = orgId;
          }
        } else if (payment.getEutNextRole() == null) {
          submittedRoleObj = OBContext.getOBContext().getRole();
          if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
            submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
          } else {
            submittedRoleOrgId = orgId;
          }
        }
        if (!payment.isReceipt()) {
          NextRoleByRuleVO nextRoleByRuleVO = NextRoleByRule.getNextRole(
              OBDal.getInstance().getConnection(), vars.getClient(), submittedRoleOrgId,
              vars.getRole(), vars.getUser(), Resource.PAYMENT_OUT_RULE, strActualPayment);
          EutNextRole nextRole = null;

          if (nextRoleByRuleVO != null && nextRoleByRuleVO.hasApproval()) {
            log.debug("next role : " + nextRoleByRuleVO.getNextRoleId());
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextRoleByRuleVO.getNextRoleId());
            payment.setEutNextRole(nextRole);
            payment.setStatus("EFIN_WFA");
            payment.setEfinDocumentAction(jsonparams.getString("document_action"));
            log.debug("status:" + payment.getStatus());
          }

          else {
            payment.setEutNextRole(null);
          }
        }

        payment.setReferenceNo(strReferenceNo);

        // Load existing lines to be deleted.
        pdToRemove = OBDao.getIDListFromOBObject(payment.getFINPaymentDetailList());
      } else {
        try {
          log.debug("Creating a new payment");
          payment = createNewPayment(jsonparams, isReceipt, org, businessPartner, gregPaymentDate,
              currency, exchangeRate, convertedAmount, strActualPayment, strInvoiceId);
        } catch (OBException e) {
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", e.getMessage());
          jsonResponse.put("message", errorMessage);
          return jsonResponse;
        }
      }
      payment.setAmount(new BigDecimal(strActualPayment));

      // update paid amt in po contract Task No.7470
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
                ordObj.setEfinPaidAmt(ordObj.getEfinPaidAmt().add(payment.getAmount()));
                OBDal.getInstance().save(ordObj);
              }
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);
            }
          }

        }
      }

      FIN_AddPayment.setFinancialTransactionAmountAndRate(vars, payment, exchangeRate,
          convertedAmount);
      OBDal.getInstance().save(payment);

      addCredit(payment, jsonparams);
      addSelectedPSDs(payment, jsonparams, pdToRemove);
      addGLItems(payment, jsonparams);

      removeNotSelectedPaymentDetails(payment, pdToRemove);

      if (strAction.equals("PRP") || strAction.equals("PPP") || strAction.equals("PRD")
          || strAction.equals("PPW")) {

        if (log.isDebugEnabled()) {
          log.debug(" strDifferenceAction " + strDifferenceAction);
          log.debug(" differenceAmount " + differenceAmount);
          log.debug(" exchangeRate " + exchangeRate);
          log.debug(" jsonparams " + jsonparams);
        }

        insertApproval(payment, vars);

        OBError message = processPayment(payment, strAction, strDifferenceAction, differenceAmount,
            exchangeRate, jsonparams);
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", message.getType().toLowerCase());
        errorMessage.put("title", message.getTitle());
        errorMessage.put("text", message.getMessage());
        jsonResponse.put("message", errorMessage);
        log.debug(" Payment Amount" + payment.getAmount());
        log.debug(" Json Response : " + errorMessage.toString());
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception handling the new payment", e);

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);

      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug(" Returned Response " + jsonResponse);
    return jsonResponse;
  }

  private FIN_Payment createNewPayment(JSONObject jsonparams, boolean isReceipt, Organization org,
      BusinessPartner bPartner, Date paymentDate, Currency currency, BigDecimal conversionRate,
      BigDecimal convertedAmt, String strActualPayment, String strInvoiceId)
      throws OBException, JSONException {
    log.debug("payment here");
    String strPaymentDocumentNo = jsonparams.getString("payment_documentno");
    String strReferenceNo = "";
    if (jsonparams.get("reference_no") != JSONObject.NULL) {
      strReferenceNo = jsonparams.getString("reference_no");
    }
    String strFinancialAccountId = jsonparams.getString("fin_financial_account_id");
    FIN_FinancialAccount finAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
        strFinancialAccountId);
    String strPaymentMethodId = jsonparams.getString("fin_paymentmethod_id");
    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
        strPaymentMethodId);

    boolean paymentDocumentEnabled = getDocumentConfirmation(finAccount, paymentMethod, isReceipt,
        strActualPayment, true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

    NextRoleByRuleVO nextRoleByRuleVO = NextRoleByRule.getNextRole(
        OBDal.getInstance().getConnection(), vars.getClient(), vars.getOrg(), vars.getRole(),
        vars.getUser(), Resource.PAYMENT_OUT_RULE, strActualPayment);
    EutNextRole nextRole = null;

    if (nextRoleByRuleVO != null && nextRoleByRuleVO.hasApproval()) {
      log.debug("next role : " + nextRoleByRuleVO.getNextRoleId());
      nextRole = OBDal.getInstance().get(EutNextRole.class, nextRoleByRuleVO.getNextRoleId());
    }

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
      // get DocumentNo
      strPaymentDocumentNo = FIN_Utility.getDocumentNo(documentType, "FIN_Payment");
    }

    FIN_Payment payment = (new AdvPaymentMngtDao()).getNewPayment(isReceipt, org, documentType,
        strPaymentDocumentNo, bPartner, paymentMethod, finAccount, strPaymentAmount, paymentDate,
        strReferenceNo, currency, conversionRate, convertedAmt, nextRole, jsonparams, strInvoiceId);
    return payment;
  }

  private void insertApproval(FIN_Payment payment, VariablesSecureApp vars) {
    EfinPoApproval approval = null;
    Client client = null;
    Organization organization = null;
    User user = null;
    Role role = null;
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    Note notes = null;
    Table table = null;

    try {
      OBContext.setAdminMode();
      log.debug("Approval");

      client = dao.getObject(Client.class, vars.getClient());
      organization = dao.getObject(Organization.class, vars.getOrg());
      user = dao.getObject(User.class, vars.getUser());
      role = dao.getObject(Role.class, vars.getRole());

      approval = OBProvider.getInstance().get(EfinPoApproval.class);

      approval.setClient(client);
      approval.setOrganization(organization);
      approval.setCreatedBy(user);
      approval.setUpdatedBy(user);
      approval.setUserContact(user);
      approval.setRole(role);
      approval.setApproveddate(new Date());
      approval.setPayment(payment);
      approval.setAlertStatus("SUB");

      OBDal.getInstance().save(approval);

      if (payment.getEfinNotes() != null && !payment.getEfinNotes().isEmpty()) {
        table = dao.getObject(Table.class, "D1A97202E832470285C9B1EB026D54E2");

        notes = OBProvider.getInstance().get(Note.class);

        notes.setClient(client);
        notes.setOrganization(organization);
        notes.setCreatedBy(user);
        notes.setUpdatedBy(user);
        notes.setRecord(approval.getId());
        notes.setNote(payment.getEfinNotes());
        notes.setTable(table);
        OBDal.getInstance().save(notes);

        approval.setObuiappNote(notes);
      }

      OBDal.getInstance().save(approval);
      OBDal.getInstance().flush();
      // OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception while insertApproval ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void addSelectedPSDs(FIN_Payment payment, JSONObject jsonparams, List<String> pdToRemove)
      throws JSONException {
    log.debug(" Adding PSDs");
    JSONObject orderInvoiceGrid = jsonparams.getJSONObject("order_invoice");
    JSONArray selectedPSDs = orderInvoiceGrid.getJSONArray("_selection");

    BigDecimal conversionRate = BigDecimal.ONE;
    BigDecimal convertedAmount = BigDecimal.ONE;

    if (selectedPSDs.length() > 1) {
      throw new OBException(OBMessageUtils.messageBD("Efin_PaymentOutDonotAllowMultipleInvoices"));
    }
    for (int i = 0; i < selectedPSDs.length(); i++) {
      JSONObject psdRow = selectedPSDs.getJSONObject(i);
      String strPSDIds = psdRow.getString("id");
      String strPaidAmount = psdRow.getString("amount");

      BigDecimal paidAmount = new BigDecimal(strPaidAmount);

      FIN_PaymentScheduleDetail detail = Utility.getObject(FIN_PaymentScheduleDetail.class,
          strPSDIds);
      FIN_PaymentSchedule schedule = detail.getInvoicePaymentSchedule();
      Invoice invoice = schedule.getInvoice();
      if (payment.getEfinInvoice() == null) {
        payment.setEfinInvoice(invoice);
      }

      Currency currency = FinanceUtils.getCurrency(invoice.getOrganization().getId(), invoice);
      conversionRate = FinanceUtils.getConversionRate(OBDal.getInstance().getConnection(),
          strPaidAmount, invoice, currency);
      convertedAmount = FinanceUtils.getConvertedAmount(paidAmount, conversionRate);
      boolean isWriteOff = psdRow.getBoolean("writeoff");
      // psdIds can be grouped
      String[] psdIds = strPSDIds.replaceAll(" ", "").split(",");
      List<FIN_PaymentScheduleDetail> psds = getOrderedPaymentScheduleDetails(psdIds);
      BigDecimal outstandingAmount = BigDecimal.ZERO;
      BigDecimal remainingAmount = paidAmount;
      for (FIN_PaymentScheduleDetail psd : psds) {
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
          pdToRemove.remove(psd.getPaymentDetails().getId());
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

  private void addCredit(FIN_Payment payment, JSONObject jsonparams) throws JSONException {
    // Credit to Use Grid
    JSONObject creditToUseGrid = jsonparams.getJSONObject("credit_to_use");
    JSONArray selectedCreditLines = creditToUseGrid.getJSONArray("_selection");
    String strSelectedCreditLinesIds = null;
    if (selectedCreditLines.length() > 0) {
      strSelectedCreditLinesIds = getSelectedCreditLinesIds(selectedCreditLines);
      List<FIN_Payment> selectedCreditPayment = FIN_Utility.getOBObjectList(FIN_Payment.class,
          strSelectedCreditLinesIds);
      HashMap<String, BigDecimal> selectedCreditPaymentAmounts = getSelectedCreditLinesAndAmount(
          selectedCreditLines, selectedCreditPayment);

      for (final FIN_Payment creditPayment : selectedCreditPayment) {
        final BigDecimal usedCreditAmt = selectedCreditPaymentAmounts.get(creditPayment.getId());
        final StringBuffer description = new StringBuffer();
        if (creditPayment.getDescription() != null && !creditPayment.getDescription().equals("")) {
          description.append(creditPayment.getDescription()).append("\n");
        }
        description.append(String.format(OBMessageUtils.messageBD("APRM_CreditUsedPayment"),
            payment.getDocumentNo()));
        String truncateDescription = (description.length() > 255)
            ? description.substring(0, 251).concat("...").toString()
            : description.toString();
        creditPayment.setDescription(truncateDescription);
        // Set Used Credit = Amount + Previous used credit introduced by the user
        creditPayment.setUsedCredit(usedCreditAmt.add(creditPayment.getUsedCredit()));
        ;
        FIN_PaymentProcess.linkCreditPayment(payment, usedCreditAmt, creditPayment);
        OBDal.getInstance().save(creditPayment);
      }
    }
  }

  private void addGLItems(FIN_Payment payment, JSONObject jsonparams)
      throws JSONException, ServletException {
    // Add GL Item lines
    JSONObject gLItemsGrid = jsonparams.getJSONObject("glitem");
    JSONArray addedGLITemsArray = gLItemsGrid.getJSONArray("_allRows");
    boolean isReceipt = payment.isReceipt();
    for (int i = 0; i < addedGLITemsArray.length(); i++) {
      JSONObject glItem = addedGLITemsArray.getJSONObject(i);
      BigDecimal glItemOutAmt = BigDecimal.ZERO;
      BigDecimal glItemInAmt = BigDecimal.ZERO;

      if (glItem.has("paidOut") && glItem.get("paidOut") != JSONObject.NULL) {
        glItemOutAmt = new BigDecimal(glItem.getString("paidOut"));
      }
      if (glItem.has("receivedIn") && glItem.get("receivedIn") != JSONObject.NULL) {
        glItemInAmt = new BigDecimal(glItem.getString("receivedIn"));
      }

      BigDecimal glItemAmt = BigDecimal.ZERO;
      if (isReceipt) {
        glItemAmt = glItemInAmt.subtract(glItemOutAmt);
      } else {
        glItemAmt = glItemOutAmt.subtract(glItemInAmt);
      }
      String strGLItemId = null;
      if (glItem.has("gLItem") && glItem.get("gLItem") != JSONObject.NULL) {
        strGLItemId = glItem.getString("gLItem");
        checkID(strGLItemId);
      }

      // Accounting Dimensions
      BusinessPartner businessPartnerGLItem = null;
      if (glItem.has("businessPartner") && glItem.get("businessPartner") != JSONObject.NULL) {
        final String strElement_BP = glItem.getString("businessPartner");
        checkID(strElement_BP);
        businessPartnerGLItem = OBDal.getInstance().get(BusinessPartner.class, strElement_BP);
      }
      Product product = null;
      if (glItem.has("product") && glItem.get("product") != JSONObject.NULL) {
        final String strElement_PR = glItem.getString("product");
        checkID(strElement_PR);
        product = OBDal.getInstance().get(Product.class, strElement_PR);
      }
      Project project = null;
      if (glItem.has("project") && glItem.get("project") != JSONObject.NULL) {
        final String strElement_PJ = glItem.getString("project");
        checkID(strElement_PJ);
        project = OBDal.getInstance().get(Project.class, strElement_PJ);
      }
      ABCActivity activity = null;
      if (glItem.has("cActivityDim") && glItem.get("cActivityDim") != JSONObject.NULL) {
        final String strElement_AY = glItem.getString("cActivityDim");
        checkID(strElement_AY);
        activity = OBDal.getInstance().get(ABCActivity.class, strElement_AY);
      }
      Costcenter costCenter = null;
      if (glItem.has("costCenter") && glItem.get("costCenter") != JSONObject.NULL) {
        final String strElement_CC = glItem.getString("costCenter");
        checkID(strElement_CC);
        costCenter = OBDal.getInstance().get(Costcenter.class, strElement_CC);
      }
      Campaign campaign = null;
      if (glItem.has("cCampaignDim") && glItem.get("cCampaignDim") != JSONObject.NULL) {
        final String strElement_MC = glItem.getString("cCampaignDim");
        checkID(strElement_MC);
        campaign = OBDal.getInstance().get(Campaign.class, strElement_MC);
      }
      UserDimension1 user1 = null;
      if (glItem.has("stDimension") && glItem.get("stDimension") != JSONObject.NULL) {
        final String strElement_U1 = glItem.getString("stDimension");
        checkID(strElement_U1);
        user1 = OBDal.getInstance().get(UserDimension1.class, strElement_U1);
      }
      UserDimension2 user2 = null;
      if (glItem.has("ndDimension") && glItem.get("ndDimension") != JSONObject.NULL) {
        final String strElement_U2 = glItem.getString("ndDimension");
        checkID(strElement_U2);
        user2 = OBDal.getInstance().get(UserDimension2.class, strElement_U2);
      }
      FIN_AddPayment.saveGLItem(payment, glItemAmt,
          OBDal.getInstance().get(GLItem.class, strGLItemId), businessPartnerGLItem, product,
          project, campaign, activity, null, costCenter, user1, user2);
    }
  }

  private void removeNotSelectedPaymentDetails(FIN_Payment payment, List<String> pdToRemove) {
    // OBContext.setAdminMode();
    try {
      for (String pdId : pdToRemove) {
        FIN_PaymentDetail pd = OBDal.getInstance().get(FIN_PaymentDetail.class, pdId);

        List<String> pdsIds = OBDao.getIDListFromOBObject(pd.getFINPaymentScheduleDetailList());
        for (String strPDSId : pdsIds) {
          FIN_PaymentScheduleDetail psd = OBDal.getInstance().get(FIN_PaymentScheduleDetail.class,
              strPDSId);

          if (pd.getGLItem() == null) {
            List<FIN_PaymentScheduleDetail> outStandingPSDs = FIN_AddPayment
                .getOutstandingPSDs(psd);
            if (outStandingPSDs.size() == 0) {
              FIN_PaymentScheduleDetail newOutstanding = (FIN_PaymentScheduleDetail) DalUtil
                  .copy(psd, false);
              newOutstanding.setPaymentDetails(null);
              newOutstanding.setWriteoffAmount(BigDecimal.ZERO);
              newOutstanding.setAmount(psd.getAmount().add(psd.getWriteoffAmount()));
              OBDal.getInstance().save(newOutstanding);
            } else {
              FIN_PaymentScheduleDetail outStandingPSD = outStandingPSDs.get(0);
              // First make sure outstanding amount is not equal zero
              if (outStandingPSD.getAmount().add(psd.getAmount()).add(psd.getWriteoffAmount())
                  .signum() == 0) {
                OBDal.getInstance().remove(outStandingPSD);
              } else {
                // update existing PD with difference
                outStandingPSD.setAmount(
                    outStandingPSD.getAmount().add(psd.getAmount()).add(psd.getWriteoffAmount()));
                outStandingPSD.setDoubtfulDebtAmount(
                    outStandingPSD.getDoubtfulDebtAmount().add(psd.getDoubtfulDebtAmount()));
                OBDal.getInstance().save(outStandingPSD);
              }
            }
          }

          pd.getFINPaymentScheduleDetailList().remove(psd);
          OBDal.getInstance().save(pd);
          OBDal.getInstance().remove(psd);
        }
        payment.getFINPaymentDetailList().remove(pd);
        OBDal.getInstance().save(payment);
        OBDal.getInstance().remove(pd);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(payment);
      }
    } catch (Exception e) {
      log.error("Exception in Payment details : ", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  private OBError processPayment(FIN_Payment payment, String strAction, String strDifferenceAction,
      BigDecimal refundAmount, BigDecimal exchangeRate, JSONObject jsonparams) throws Exception {

    ConnectionProvider conn = new DalConnectionProvider(true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    OBError message = null;
    String strNewPaymentMessage = null;
    // OBContext.setAdminMode();

    try {
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
      /*
       * if(payment.getEutNextRole() != null) { message = new OBError() message.setType("Success");
       * strNewPaymentMessage =
       * OBMessageUtils.parseTranslation("@QPOE_Payment_Submitted@").replace("xx",
       * payment.getDocumentNo()); } else { message = FIN_AddPayment.processPayment(vars, conn,
       * (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", payment);
       * strNewPaymentMessage = OBMessageUtils.parseTranslation("@PaymentCreated@" + " " +
       * payment.getDocumentNo()) + "."; }
       */
      message = FIN_AddPayment.processPayment(vars, conn,
          (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", payment);
      if (message.getType().equals("Error")) {
        return message;
      } else {
        if (payment.isReceipt()) {
          strNewPaymentMessage = OBMessageUtils
              .parseTranslation("@Efin_create_receipt@" + " " + payment.getDocumentNo()) + ".";
        } else {
          if (payment.getEutNextRole() != null) {
            message = new OBError();
            message.setType("Success");
            strNewPaymentMessage = OBMessageUtils.parseTranslation("@EFIN_Payment_Submitted@")
                .replace("xx", payment.getDocumentNo());
          } else {
            strNewPaymentMessage = OBMessageUtils
                .parseTranslation("@PaymentCreated@" + " " + payment.getDocumentNo()) + ".";
          }
        }
      }

      if (!"Error".equalsIgnoreCase(message.getType())) {
        message.setMessage(strNewPaymentMessage + " " + message.getMessage());
        message.setType(message.getType().toLowerCase());
      } else {
        conn = new DalConnectionProvider(true);
        OBDal.getInstance().getSession().clear();
        payment = OBDal.getInstance().get(FIN_Payment.class, payment.getId());
        addCredit(payment, jsonparams);
      }

      log.debug(" strDifferenceAction: " + strDifferenceAction);

      if (!strDifferenceAction.equals("refund")) {
        return message;
      }

      boolean newPayment = !payment.getFINPaymentDetailList().isEmpty();

      FIN_Payment refundPayment = FIN_AddPayment.createRefundPayment(conn, vars, payment,
          refundAmount.negate(), exchangeRate);
      OBError auxMessage = FIN_AddPayment.processPayment(vars, conn,
          (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", refundPayment);

      if (newPayment && !"Error".equalsIgnoreCase(auxMessage.getType())) {
        final String strNewRefundPaymentMessage = OBMessageUtils
            .parseTranslation("@APRM_RefundPayment@" + ": " + refundPayment.getDocumentNo()) + ".";
        message.setMessage(strNewRefundPaymentMessage + " " + message.getMessage());
        if (payment.getGeneratedCredit().compareTo(BigDecimal.ZERO) != 0) {
          payment.setDescription(payment.getDescription() + strNewRefundPaymentMessage + "\n");
          OBDal.getInstance().save(payment);
          OBDal.getInstance().flush();
        }
      } else {
        message = auxMessage;
      }
    } catch (Exception e) {
      log.error(" Exception in create detail: ", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return message;
  }

  private List<FIN_PaymentScheduleDetail> getOrderedPaymentScheduleDetails(String[] psdSet) {
    OBCriteria<FIN_PaymentScheduleDetail> orderedPSDs = OBDal.getInstance()
        .createCriteria(FIN_PaymentScheduleDetail.class);
    orderedPSDs.add(Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_ID, psdSet));
    orderedPSDs.addOrderBy(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS, true);
    orderedPSDs.addOrderBy(FIN_PaymentScheduleDetail.PROPERTY_AMOUNT, true);
    return orderedPSDs.list();
  }

  private void checkID(final String id) throws ServletException {
    if (!IsIDFilter.instance.accept(id)) {
      log.error("Input: " + id + " not accepted by filter: IsIDFilter");
      throw new ServletException("Input: " + id + " is not an accepted input");
    }
  }

  /**
   * @param allselection
   *          Selected Rows in Credit to use grid
   * @return a String with the concatenation of the selected rows ids
   */
  private String getSelectedCreditLinesIds(JSONArray allselection) throws JSONException {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (int i = 0; i < allselection.length(); i++) {
      JSONObject selectedRow = allselection.getJSONObject(i);
      sb.append(selectedRow.getString("id") + ",");
    }
    sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1, ")");
    return sb.toString();
  }

  private HashMap<String, BigDecimal> getSelectedCreditLinesAndAmount(JSONArray allselection,
      List<FIN_Payment> _selectedCreditPayments) throws JSONException {
    HashMap<String, BigDecimal> selectedCreditLinesAmounts = new HashMap<String, BigDecimal>();

    for (FIN_Payment creditPayment : _selectedCreditPayments) {
      for (int i = 0; i < allselection.length(); i++) {
        JSONObject selectedRow = allselection.getJSONObject(i);
        if (selectedRow.getString("id").equals(creditPayment.getId())) {
          selectedCreditLinesAmounts.put(creditPayment.getId(),
              new BigDecimal(selectedRow.getString("paymentAmount")));
        }
      }
    }
    return selectedCreditLinesAmounts;
  }

  private boolean getDocumentConfirmation(FIN_FinancialAccount finAccount,
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
}
