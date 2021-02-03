package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.scm.ESCMBGDocumentnoV;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.actionHandler.dao.BidManagementAddLinesDAO;
import sa.elm.ob.scm.ad_callouts.PurchaseAgreementCalloutDAO;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.ad_process.POandContract.POContractSummaryAction;
import sa.elm.ob.scm.ad_process.ProposalManagement.CreatePoAttachmentDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAOImpl;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gokul 10/06/2020
 *
 */

public class CreatePOSubmit extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CreatePOSubmit.class);
  private static String RECORD_ID = "Escm_Proposalmgmt_ID";
  private static String PROPOSAL_ATTR_ID = "Escm_Proposal_Attr_ID";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    JSONObject jsonResponse = new JSONObject();
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    EscmProposalMgmt proposalMgmt = null;
    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      // Variable declaration
      String proposalId = null;
      String proposalattrId = null;
      String notes = "";
      if (jsonRequest.has(RECORD_ID) && jsonRequest.getString(RECORD_ID) != null) {
        proposalId = jsonRequest.getString(RECORD_ID);

      }
      if (jsonRequest.has(PROPOSAL_ATTR_ID) && jsonRequest.getString(PROPOSAL_ATTR_ID) != null) {
        proposalattrId = jsonRequest.getString(PROPOSAL_ATTR_ID);
        EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalattrId);
        proposalId = proposalAttr.getEscmProposalmgmt().getId();
      }
      if (jsonparams.getString("ponotes").equals("null"))
        notes = "";
      else
        notes = jsonparams.getString("ponotes");
      String signatureDate = jsonparams.getString("signaturedate");
      String city = jsonparams.getString("city");
      String contractStartDate = jsonparams.getString("contractstartdate");
      String contractEndDate = jsonparams.getString("contractenddate");
      String onBoardDateh = jsonparams.getString("onboarddateh");
      String onBoardDateg = jsonparams.getString("onboarddateg");
      String contractDuration = jsonparams.getString("contractduration");
      String periodType = jsonparams.getString("periodtype");
      String advPaymentPer = jsonparams.getString("advpaymentper");
      String advPaymentAmt = jsonparams.getString("advpaymentamt");
      String bgId = jsonparams.getString("ESCM_BGWorkbench");
      OBError error = null;
      Date signDate = null;
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      final String orgId = proposalmgmt.getOrganization().getId();
      User user = OBDal.getInstance().get(User.class, vars.getUser());
      String purchaseOrderType = "PUR";
      List<EscmPurchaseOrderConfiguration> config = new ArrayList<EscmPurchaseOrderConfiguration>();
      List<Location> bploclist = new ArrayList<Location>();
      List<Warehouse> warehouselist = new ArrayList<Warehouse>();
      List<PriceList> priceListlist = new ArrayList<PriceList>();
      List<PaymentTerm> paymentTermList = new ArrayList<PaymentTerm>();
      Location bplocation = null;
      String yearId = null, transdoctypeId = null;
      SimpleDateFormat d1 = new SimpleDateFormat("dd-MM-yyyy");
      Warehouse warehouse = null;
      PriceList priceList = null;
      PaymentTerm paymentTerm = null;
      String startingDate = "", budgetReferenceId = null;
      List<String> fieldList = null;

      ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
      // get the connection
      Connection conn = OBDal.getInstance().getConnection();
      String cityId = "1B2831FE7D5446E686E853A92D3983C4";
      String sDate = "5676EE7CE5D046F1A4F16A2E33E6152F";
      String notesId = "B9F8167CFA5C4D75BC3F8C994893128C";
      List<String> list2 = new ArrayList<String>(Arrays.asList("1B2831FE7D5446E686E853A92D3983C4",
          "5676EE7CE5D046F1A4F16A2E33E6152F", "B9F8167CFA5C4D75BC3F8C994893128C"));
      String description = null;
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.contractUser;
      String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
      NextRoleByRuleVO nextApproval = null;
      String motContactPerson = "";
      String motContactPosition = "";
      Date conStDate = null;
      Date conEndDate = null;
      Date onBdDateg = null;
      Date onBoardDatehj = null;
      fieldList = proposalDAO.checkMandatoryfields(proposalId);

      Boolean checkOrderIsContract = proposalDAO.checkOrderIsContract(proposalmgmt.getTotalamount(),
          orgId, vars.getClient());

      if ("Add Attachment".equals(jsonRequest.getString("_buttonValue"))) {
        JSONObject openTabAction = new JSONObject();
        JSONObject result = new JSONObject();
        JSONArray actions = new JSONArray();
        openTabAction.put("openAttachmentInProposal", jsonRequest);
        actions.put(openTabAction);
        result.put("responseActions", actions);
        result.put("retryExecution", true);
        result.put("refreshParent", true);
        return result;
      }

      if ("View Attachment".equals(jsonRequest.getString("_buttonValue"))) {
        JSONObject result = new JSONObject();
        JSONObject messageObj = new JSONObject();

        try {
          OBContext.setAdminMode();
          OBQuery<Attachment> attachQry = OBDal.getInstance().createQuery(Attachment.class,
              "as e where e.eutSourceid=:proposalId and e.table.id ='259' ");
          attachQry.setNamedParameter("proposalId", proposalId);
          List<Attachment> fileList = attachQry.list();
          if (fileList.size() > 0) {
            StringBuilder message = new StringBuilder();
            int i = 1;

            for (Attachment attach : fileList) {
              message.append(i + ". ");
              message.append(attach.getName());
              message.append("<br/>");
              i++;
            }
            messageObj.put("message", message.toString());

          }

          JSONObject openTabAction = new JSONObject();
          JSONArray actions = new JSONArray();
          openTabAction.put("openAttachmentInPopup", messageObj);
          actions.put(openTabAction);
          result.put("responseActions", actions);
          result.put("retryExecution", true);
          result.put("refreshParent", true);
        } catch (Exception e) {
          log.error("Eror while viewing atachment" + e.getMessage());
        }
        return result;
      }

      boolean isMinProposalApproved = false;
      if (proposalmgmt.getProposalstatus().equals("PAWD")) {
        isMinProposalApproved = proposalDAO.isMinProposalApproved(proposalmgmt);
      }

      if ((proposalmgmt.getProposalappstatus().equals("APP")
          && proposalmgmt.getProposalstatus().equals("AWD"))
          || (proposalmgmt.getProposalstatus().equals("PAWD")
              && (isMinProposalApproved || proposalmgmt.getProposalappstatus().equals("APP")))) {

        if (fieldList.contains(cityId) && (city.equals("null"))) {
           OBDal.getInstance().rollbackAndClose();
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_city_mandatory"));
          json.put("message", erorMessage);
          return json;
        }
        if (fieldList.contains(sDate) && (signatureDate == null)) {
           OBDal.getInstance().rollbackAndClose();
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_signdate_mand"));
          json.put("message", erorMessage);
          return json;
        }
        if (fieldList.contains(notesId) && (notes == null)) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_notes_mandatory"));
          json.put("message", erorMessage);
          return json;
        }

        fieldList.removeAll(list2);

        if (fieldList.size() > 0) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_PO_mandatory_up"));
          json.put("message", erorMessage);
          return json;
        }

        // If proposal type is Limited/Tender, bank guarantee details is mandatory
        if (proposalmgmt.getProposalType().equals("TR")
            || proposalmgmt.getProposalType().equals("LD")) {
          if (bgId.equals("null")) {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("Escm_SelectBGDetails"));
            json.put("retryExecution", true);
            json.put("message", erorMessage);
            return json;
          }
        }

       // Throw error if contract category is inactive
        if (proposalmgmt != null && proposalmgmt.getContractType() != null
            && !proposalmgmt.getContractType().isActive()) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_ContractCategoryInactive"));
          json.put("retryExecution", true);
          json.put("message", erorMessage);
          return json;
        }

        // based on configuration minvalue , getting purchase order type is purchase order /contract
        if (proposalmgmt.getProposalstatus().equals("PAWD")) {
          config = proposalDAO.getPOTypeBasedOnValue(orgId, proposalmgmt.getAwardamount());
        } else {
          config = proposalDAO.getPOTypeBasedOnValue(orgId, proposalmgmt.getTotalamount());
        }

        if (config.size() > 0) {
          purchaseOrderType = config.get(0).getOrdertype();

          if (purchaseOrderType.equals("CR")) {
            if (contractStartDate.equals("null") || contractEndDate.equals("null")
                || onBoardDateh.equals("null") || onBoardDateg.equals("null")
                || periodType.equals("null")) {
              JSONObject erorMessage = new JSONObject();
              erorMessage.put("severity", "error");
              erorMessage.put("text", OBMessageUtils.messageBD("Escm_contractcon_mandatry"));
              json.put("message", erorMessage);
              return json;
            }
          }

          // Setting mot contact person and position

          EscmPurchaseOrderConfiguration configuration = config.get(0);
          motContactPosition = configuration.getMOTContactPosition();
          motContactPerson = configuration.getMOTContactPerson() != null
              ? configuration.getMOTContactPerson().getName()
              : null;

        } else {

          // Setting mot contact person and position

          EscmPurchaseOrderConfiguration configuration = PurchaseAgreementCalloutDAO
              .checkDocTypeConfig(OBContext.getOBContext().getCurrentClient().getId(),
                  proposalmgmt.getOrganization().getId(), purchaseOrderType);
          if (configuration != null)
            motContactPosition = configuration.getMOTContactPosition();
          else
            motContactPosition = null;
          if (configuration != null)
            motContactPerson = configuration.getMOTContactPerson() != null
                ? configuration.getMOTContactPerson().getName()
                : null;
          else
            motContactPerson = null;

        }
        startingDate = proposalDAO.getPeriodStartDate(vars.getClient());
        budgetReferenceId = proposalDAO.getBudgetFromPeriod(vars.getClient(), startingDate);

        if ("".equals(budgetReferenceId)) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Efin_Budget_Init_Mandatory"));
          json.put("message", erorMessage);
          return json;
        }

        if (budgetReferenceId != null) {
          if (!proposalmgmt.getEfinBudgetinitial().getId().equals(budgetReferenceId)) {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("Escm_YearClosed_Err"));
            json.put("message", erorMessage);
            return json;
          }
        }

        // fetching finacial year
        yearId = proposalDAO.getFinancialYear(vars.getClient());
        if ("".equals(yearId)) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_FinancialYear_NotDefine"));
          json.put("message", erorMessage);
          return json;
        }

        // fetching warehouse
        warehouselist = proposalDAO.getWarehouse(proposalmgmt.getClient().getId());
        if (warehouselist.size() > 0) {
          warehouse = warehouselist.get(0);
        } else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_Warehouse_NotDefine"));
          json.put("message", erorMessage);
          return json;
        }

        // fetching bplocation
        bploclist = proposalDAO.getLocation(proposalmgmt.getSupplier().getId());
        if (bploclist.size() > 0)
          bplocation = bploclist.get(0);
        else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_SuppLoc_NotDefine"));
          json.put("message", erorMessage);
          return json;
        }

        // fetching pricelist
        priceListlist = proposalDAO.getPriceList(proposalmgmt.getClient().getId());
        if (priceListlist.size() > 0)
          priceList = priceListlist.get(0);
        else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_PriceList_NotDefine"));
          json.put("message", erorMessage);
          return json;

        }

        // fetching payment term
        paymentTermList = proposalDAO.getPaymentTerm(proposalmgmt.getClient().getId());
        if (paymentTermList.size() > 0)
          paymentTerm = paymentTermList.get(0);
        else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_PaymentTerm_NotDefine"));
          json.put("message", erorMessage);
          return json;
        }
        if (user.getBusinessPartner() == null) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_CrtPOfrmProsal_NotBP"));
          json.put("message", erorMessage);
          return json;
        }
        Object transdoctype = proposalDAO.getTransactionDoc(orgId, vars.getClient());
        if (transdoctype != null) {
          transdoctypeId = (String) transdoctype;
        } else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_PODocType_NotDefine"));
          json.put("message", erorMessage);
          return json;
        }

        if (transdoctypeId != null && paymentTerm != null && priceList != null && warehouse != null
            && bplocation != null && yearId != null) {

          if (signatureDate != null && !signatureDate.equals("null")
              && StringUtils.isNotEmpty(signatureDate)) {
            signatureDate = signatureDate.split("-")[0] + signatureDate.split("-")[1]
                + signatureDate.split("-")[2];
            signatureDate = BidManagementAddLinesDAO.convertToGregorianDate(signatureDate);
            format = new SimpleDateFormat("yyyy-MM-dd");
            signDate = format.parse(signatureDate);
          }
          if (contractStartDate != null && !contractStartDate.equals("null")
              && StringUtils.isNotEmpty(contractStartDate)) {
            contractStartDate = contractStartDate.split("-")[0] + contractStartDate.split("-")[1]
                + contractStartDate.split("-")[2];
            contractStartDate = BidManagementAddLinesDAO.convertToGregorianDate(contractStartDate);
            format = new SimpleDateFormat("yyyy-MM-dd");
            conStDate = format.parse(contractStartDate);
          }
          if (contractEndDate != null && !contractEndDate.equals("null")) {
            contractEndDate = contractEndDate.split("-")[0] + contractEndDate.split("-")[1]
                + contractEndDate.split("-")[2];
            contractEndDate = BidManagementAddLinesDAO.convertToGregorianDate(contractEndDate);
            conEndDate = format.parse(contractEndDate);
          }
          if (onBoardDateh != null && !onBoardDateh.equals("null")) {
            onBoardDateh = onBoardDateh.split("-")[0] + onBoardDateh.split("-")[1]
                + onBoardDateh.split("-")[2];
            // onBoardDatehj = format.parse(onBoardDateh);
            onBoardDateg = BidManagementAddLinesDAO.convertToGregorianDate(onBoardDateh);
            onBoardDatehj = format.parse(onBoardDateg);
            // onBdDateg = format.parse(onBoardDateg);
          }
          Long cotDuration = 1L;
          cotDuration = Long.parseLong(contractDuration);

          City cityObj = OBDal.getInstance().get(City.class, city);
          Order order = OBProvider.getInstance().get(Order.class);
          order.setClient(proposalmgmt.getClient());
          order.setOrganization(proposalmgmt.getOrganization());
          order.setCreatedBy(user);
          order.setUpdatedBy(user);
          order.setSalesTransaction(false);
          order.setDocumentType(OBDal.getInstance().get(DocumentType.class, "0"));
          order.setTransactionDocument(OBDal.getInstance().get(DocumentType.class, transdoctypeId));
          order.setDocumentNo(UtilityDAO.getSequenceNo(conn, vars.getClient(),
              order.getTransactionDocument().getDocumentSequence().getName(), true));

          order.setBusinessPartner(proposalmgmt.getSupplier());
          order.setEscmNotes(notes);
          order.setEscmSignaturedate(signDate);
          order.setEscmCCity(cityObj);
          order.setEscmIscreateposubmit(true);
          if (purchaseOrderType.equals("CR")) {
            order.setEscmOrdertype("CR");
            BigDecimal advpayper = new BigDecimal(advPaymentPer);
            BigDecimal advpayamt = new BigDecimal(advPaymentAmt);
            order.setEscmContractstartdate(conStDate);
            order.setEscmContractenddate(conEndDate);
            order.setEscmOnboarddategreg(onBoardDateg);
            order.setEscmOnboarddateh(onBoardDatehj);// onBoardDateg
            order.setEscmContractduration(cotDuration);
            order.setEscmPeriodtype(periodType);
            order.setEscmAdvpaymntAmt(advpayamt);
            order.setEscmAdvpaymntPercntge(advpayper);
            order.setEscmIsadvancepayment(true);
          } else {
            order.setEscmOrdertype("PUR");
            order.setEscmContractstartdate(null);
            order.setEscmContractenddate(null);
            order.setEscmOnboarddategreg(null);
            order.setEscmOnboarddateh(null);
            order.setEscmContractduration(null);
            order.setEscmPeriodtype(null);
            order.setEscmAdvpaymntAmt(BigDecimal.ZERO);
            order.setEscmAdvpaymntPercntge(BigDecimal.ZERO);
            order.setEscmIsadvancepayment(true);
          }
          order.setDocumentStatus("DR");
          order.setDocumentAction("CO");
          order.setAccountingDate(new java.util.Date());
          order.setOrderDate(new java.util.Date());
          order.setBusinessPartner(proposalmgmt.getSupplier());
          order.setEscmRevision(0L);
          order.setEscmAppstatus("DR");
          order.setEscmFinanyear(OBDal.getInstance().get(Year.class, yearId));
          if (proposalmgmt.getEscmBidmgmt() != null
              && proposalmgmt.getEscmBidmgmt().getBidname() != null) {
            order.setEscmProjectname(proposalmgmt.getEscmBidmgmt().getBidname());
          } else {
            order.setEscmProjectname(proposalmgmt.getBidName());
          }
          order.setPartnerAddress(bplocation);
          order.setWarehouse(warehouse);
          order.setPriceList(priceList);
          order.setPaymentTerms(paymentTerm);
          order.setInvoiceTerms("D");
          order.setDeliveryTerms("A");
          order.setFreightCostRule("I");
          order.setFormOfPayment("B");
          order.setDeliveryMethod("P");
          order.setPriority("5");
          order.setEscmAdRole(OBContext.getOBContext().getRole());
          order.setEscmAppstatus("DR");
          Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
          order.setCurrency(proposalmgmt.getOrganization().getCurrency() == null ? objCurrency
              : proposalmgmt.getOrganization().getCurrency());

          if (proposalmgmt.getProposalstatus().equals("PAWD")) {
            order.setGrandTotalAmount(proposalmgmt.getAwardamount());
          } else {
            order.setGrandTotalAmount(proposalmgmt.getTotalamount());
          }

          order.setDocumentStatus("DR");
          order.setEscmDocaction("CO");
          order.setEscmBuyername(user);
          order.setEscmRatedategre(d1.format(new Date()));
          if (budgetReferenceId != null)
            order.setEfinBudgetint(
                OBDal.getInstance().get(EfinBudgetIntialization.class, budgetReferenceId));
          order.setEscmProposalmgmt(proposalmgmt);
          // order.setEscmMotcontperson(user.getBusinessPartner().getName());
          // order.setEscmMotcontposition(user.getBusinessPartner().getEhcmPosition());
          order.setEscmMotcontperson(motContactPerson);
          order.setEscmMotcontposition(motContactPosition);
          if (proposalmgmt.isTaxLine()) {
            order.setEscmIstax(proposalmgmt.isTaxLine());

          }
          if (proposalmgmt.getEfinTaxMethod() != null) {
            order.setEscmTaxMethod(proposalmgmt.getEfinTaxMethod());
          }
          order.setEscmCalculateTaxlines(true);
          if (proposalmgmt.getSecondsupplier() != null)
            order.setEscmSecondsupplier(proposalmgmt.getSecondsupplier());
          if (proposalmgmt.getSecondBranchname() != null)
            order.setEscmSecondBranchname(proposalmgmt.getSecondBranchname());
          order.setEscmIssecondsupplier(proposalmgmt.isSecondsupplier());
          if (proposalmgmt.getIBAN() != null)
            order.setEscmSecondIban(proposalmgmt.getIBAN());
          if (proposalmgmt.getSubcontractors() != null)
            order.setEscmSubcontractors(proposalmgmt.getSubcontractors());
          if (proposalmgmt.getContractType() != null) {
            order.setEscmContactType(proposalmgmt.getContractType());
            if (proposalmgmt.getContractType().getReceiveType().getSearchKey().equals("AMT")) {
              order.setEscmReceivetype("AMT");
            } else {
              order.setEscmReceivetype("QTY");
            }
          } else {
            order.setEscmContactType(null);
          }
          OBQuery<EscmProposalsourceRef> sourceRef = OBDal.getInstance().createQuery(
              EscmProposalsourceRef.class,
              "as e where e.escmProposalmgmtLine.id in (select ln.id from "
                  + "Escm_Proposalmgmt_Line ln where ln.escmProposalmgmt.id=:propId)");
          sourceRef.setNamedParameter("propId", proposalmgmt.getId());
          List<EscmProposalsourceRef> propSrclist = sourceRef.list();
          if (propSrclist.size() > 0) {
            EscmProposalsourceRef propSrcRef = propSrclist.get(0);
            if (propSrcRef.getRequisition() != null) {
              order.setEscmMaintenanceProject(
                  propSrcRef.getRequisition().getEscmMaintenanceProject());
            }
            if (propSrcRef.getRequisition() != null) {
              order.setEscmMaintenanceCntrctNo(
                  propSrcRef.getRequisition().getESCMMaintenanceContractNo());
            }
          }
          if (proposalmgmt.getEscmBidmgmt() != null) {
            OBQuery<Escmbidsourceref> bidSrcref = OBDal.getInstance().createQuery(
                Escmbidsourceref.class,
                "as e where  e.escmBidmgmtLine.id in (select bid.id from escm_bidmgmt_line bid where bid.escmBidmgmt.id=:bidId)");
            bidSrcref.setNamedParameter("bidId", proposalmgmt.getEscmBidmgmt().getId());
            List<Escmbidsourceref> bidSrcList = bidSrcref.list();
            if (bidSrcList.size() > 0) {
              Escmbidsourceref bidSrcRef = bidSrcList.get(0);
              if (bidSrcRef.getRequisition() != null) {
                order.setEscmMaintenanceProject(
                    bidSrcRef.getRequisition().getEscmMaintenanceProject());
                order.setEscmMaintenanceCntrctNo(
                    bidSrcRef.getRequisition().getESCMMaintenanceContractNo());
              }
            }
          }
          order.setBusinessPartner(proposalmgmt.getSupplier());
          proposalmgmt.setDocumentNo(order);
          OBDal.getInstance().save(order);

          // add attachment in the order which is selected in popup
          CreatePoAttachmentDAO.copyAttachment(order.getEscmProposalmgmt().getId(), order.getId(),
              "259");

          // Updating the PO reference in PEE(Proposal Attribute)
          // Fetching the PEE irrespective of Proposal Version
          OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
              EscmProposalAttribute.class,
              " as a  join a.escmProposalevlEvent b where b.status='CO' and a.escmProposalmgmt.proposalno= :proposalID ");
          proposalAttr.setNamedParameter("proposalID", proposalmgmt.getProposalno());
          List<EscmProposalAttribute> proposalAttrList = proposalAttr.list();
          if (proposalAttrList.size() > 0) {
            EscmProposalAttribute proposalAttrObj = proposalAttrList.get(0);
            proposalAttrObj.setOrder(order);
            OBDal.getInstance().save(proposalAttrObj);
          }

          OBDal.getInstance().flush();

          int ordercount = POcontractAddproposalDAO.insertOrderline(conn, proposalmgmt, order);
          OBDal.getInstance().flush();

          // Update order details in selected BG
          if (bgId != null && StringUtils.isNotEmpty(bgId) && !bgId.equals("null")) {
            BigDecimal documentAmount;
            String bidId = null, bidName = null;

            ESCMBGWorkbench bgWorkbench = OBDal.getInstance().get(ESCMBGWorkbench.class, bgId);
            ESCMBGDocumentnoV bgDocNoV = OBDal.getInstance().get(ESCMBGDocumentnoV.class,
                order.getId());
            bgWorkbench.setDocumentNo(bgDocNoV);

            JSONObject obj = BGWorkbenchDAO.getDocDetails(order.getId(), "POC");
            try {
              if (obj.has("linetotal"))
                documentAmount = new BigDecimal(obj.getString("linetotal"));
              else
                documentAmount = new BigDecimal(0.00);

              bgWorkbench.setDocumentAmount(documentAmount);

              if (obj.has("bidId")) {
                bidId = obj.getString("bidId");
                bidName = obj.getString("bidName");

                EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, bidId);
                bgWorkbench.setBidNo(bidMgmt);
                bgWorkbench.setBidName(bidName);
              } else {
                bidId = null;
              }

              if (obj.has("SupplierId")) {
                String supplierId = obj.getString("SupplierId");
                BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class,
                    supplierId);
                bgWorkbench.setVendorName(bPartner);
              }

              if (obj.has("currencyId")) {
                String currencyId = obj.getString("currencyId");
                Currency currency = OBDal.getInstance().get(Currency.class, currencyId);
                bgWorkbench.setCurrency(currency);
              }

              String bpLocId = BGWorkbenchDAO.getbpartnerId(order.getId(), "POC");
              if (bpLocId != null) {
                bgWorkbench.setPartnerAddress(OBDal.getInstance().get(Location.class, bpLocId));
              }

              if (order.getEscmProposalmgmt() != null) {

                EscmProposalAttribute proposalAtt = BankGuaranteeDetailEventDAO
                    .getProposalAttribute(proposalmgmt.getId());

                bgWorkbench.setEscmProposalmgmt(proposalmgmt);

                if (proposalAtt != null) {
                  bgWorkbench.setEscmProposalAttr(proposalAtt);
                }
              }

              bgWorkbench.setSalesOrder(order);

              if (bidId != null) {
                String bidTermValue = BGWorkbenchDAO.getbidTermsValue(bidId, bgWorkbench.getType(),
                    bgWorkbench.getClient().getId());
                bgWorkbench.setInitialBG(bidTermValue);
              }
              OBDal.getInstance().flush();
            } catch (JSONException e) {
              log.error("Exception while getting the Document Details in BG:", e);
              throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
            }
          }

          String processId = "06F0902CF5CB4FE1A70A994E34950FF7";
          ProcessBundle pb = new ProcessBundle(processId, vars);
          pb.setCloseConnection(false);
          HashMap<String, Object> parameters1 = new HashMap<String, Object>();
          parameters1.put("C_Order_ID", order.getId());
          parameters1.put("notes", notes);
          pb.setParams(parameters1);
          POContractSummaryAction orderSubmit = new POContractSummaryAction();
          orderSubmit.doExecute(pb);
          error = (OBError) pb.getResult();
          if (error.getType().equals("error")) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_CreatePOForProsalNotSuccess@");
            JSONObject successMessage = new JSONObject();
            successMessage.put("severity", "error");
            successMessage.put("text",
                OBMessageUtils.messageBD("ESCM_CreatePOForProsalNotSuccess"));
            successMessage.put("text", OBMessageUtils.messageBD("ESCM_CreatePOForProsalNotSuccess")
                + "" + OBMessageUtils.messageBD(error.getMessage().replace("@", "")));
            json.put("message", successMessage);
            return json;
          }

          if (ordercount == 1 && error.getMessage().equals("@Escm_Ir_complete_success@")) {

            // send an alert to contract user when po is created
            description = sa.elm.ob.scm.properties.Resource
                .getProperty("scm.contractuser.alert", vars.getLanguage())
                .concat("" + proposalmgmt.getProposalno());
            AlertUtility.alertInsertBasedonPreference(order.getId(),
                order.getDocumentNo()
                    + ((order.getEscmNotes() != null && !order.getEscmNotes().equals("null")
                        && !order.getEscmNotes().equals("")) ? "-" + order.getEscmNotes() : ""),
                "ESCM_Contract_User", order.getClient().getId(), description, "NEW", alertWindow,
                "scm.contractuser.alert", Constants.GENERIC_TEMPLATE, windowId, null);
            JSONObject successMessage = new JSONObject();
            String msg = OBMessageUtils.messageBD("ESCM_CreatePOForProposal_Success");
            successMessage.put("severity", "success");

            successMessage.put("text", msg.replace("%", order.getDocumentNo()));
            json.put("message", successMessage);
            return json;
          } else {
            JSONObject successMessage = new JSONObject();
            successMessage.put("severity", "failure");
            successMessage.put("text",
                OBMessageUtils.messageBD("ESCM_CreatePOForProsalNotSuccess"));
            json.put("message", successMessage);
            return json;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Exception in CreatePOSubmit :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }
}
