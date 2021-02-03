package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.actionHandler.POcontractAddproposalDAO;
import sa.elm.ob.scm.ad_callouts.PurchaseAgreementCalloutDAO;
import sa.elm.ob.scm.ad_process.POandContract.POContractSummaryAction;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gokul on 01/06/2020
 */

// Create PO Submit from Proposal
public class CreatePOSubmitFromProposal implements Process {

  private static Logger log = Logger.getLogger(CreatePOFromProposal.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      // Variable declaration
      final String proposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = proposalmgmt.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      User user = OBDal.getInstance().get(User.class, userId);
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
      Date contractStartDate = null;
      Date ContractEndDate = null;
      Date onBoardDateh = null;
      Date onBoardDateg = null;
      Long contractDuration = 0L;
      String poNotes = (String) bundle.getParams().get("ponotes").toString();
      String signatureDatestr = (String) bundle.getParams().get("signaturedate").toString();
      Date signatureDate = UtilityDAO.convertToGregorianDate(signatureDatestr);
      String city = (String) bundle.getParams().get("city").toString();
      String contractStartDatestr = (String) bundle.getParams().get("contractstartdate").toString();
      if (contractStartDatestr != null)
        contractStartDate = UtilityDAO.convertToGregorianDate(contractStartDatestr);
      String ContractEndDatestr = (String) bundle.getParams().get("contractenddate").toString();
      if (ContractEndDatestr != null)
        ContractEndDate = UtilityDAO.convertToGregorianDate(ContractEndDatestr);
      String onBoardDatehstr = (String) bundle.getParams().get("onboarddateh").toString();
      if (onBoardDatehstr != null)
        onBoardDateh = UtilityDAO.convertToGregorianDate(onBoardDatehstr);
      String onBoardDategstr = (String) bundle.getParams().get("onboarddateg").toString();
      if (onBoardDategstr != null)
        onBoardDateg = UtilityDAO.convertToGregorianDate(onBoardDategstr);
      String contractDion = (String) bundle.getParams().get("contractduration").toString();
      if (contractDion != null && !contractDion.equals(""))
        contractDuration = Long.parseLong(contractDion);
      String periodType = (String) bundle.getParams().get("periodtype").toString();
      String advpaymentper = (String) bundle.getParams().get("advpaymentper");
      String advpaymentamt = (String) bundle.getParams().get("advpaymentamt");
      List<String> fieldList = null;
      String motContactPerson = "";
      String motContactPosition = "";
      OBError error = null;
      ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
      // get the connection
      Connection conn = OBDal.getInstance().getConnection();
      String cityId = "1B2831FE7D5446E686E853A92D3983C4";
      String sDate = "5676EE7CE5D046F1A4F16A2E33E6152F";
      String notesId = "B9F8167CFA5C4D75BC3F8C994893128C";

      String description = null;
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.contractUser;
      String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
      NextRoleByRuleVO nextApproval = null;

      fieldList = proposalDAO.checkMandatoryfields(proposalId);
      Boolean checkOrderIsContract = proposalDAO.checkOrderIsContract(proposalmgmt.getTotalamount(),
          orgId, clientId);

      if (proposalmgmt.getProposalappstatus().equals("APP")
          && proposalmgmt.getProposalstatus().equals("AWD")) {

        if (fieldList.contains(cityId) && (city == null)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_city_mandatory@");
          bundle.setResult(result);
          return;
        }
        if (fieldList.contains(sDate) && (signatureDate == null)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_signdate_mand@");
          bundle.setResult(result);
          return;
        }
        if (fieldList.contains(notesId) && (poNotes == null)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_notes_mandatory@");
          bundle.setResult(result);
          return;
        }

        if (checkOrderIsContract && (contractStartDate == null || ContractEndDate == null
            || onBoardDateh == null || onBoardDateg == null || contractDuration == null
            || periodType == null || advpaymentper == null || advpaymentamt == null)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_contractcon_mandatry@");
          bundle.setResult(result);
          return;
        }

        // based on configuration minvalue , getting purchase order type is purchase order /contract
        config = proposalDAO.getPOTypeBasedOnValue(orgId, proposalmgmt.getTotalamount());
        if (config.size() > 0) {
          purchaseOrderType = config.get(0).getOrdertype();

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

          motContactPosition = configuration.getMOTContactPosition();
          motContactPerson = configuration.getMOTContactPerson() != null
              ? configuration.getMOTContactPerson().getName()
              : null;

        }

        startingDate = proposalDAO.getPeriodStartDate(clientId);
        budgetReferenceId = proposalDAO.getBudgetFromPeriod(clientId, startingDate);
        if ("".equals(budgetReferenceId)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Budget_Init_Mandatory@");
          bundle.setResult(result);
          return;
        }
        if (budgetReferenceId != null) {
          if (!proposalmgmt.getEfinBudgetinitial().getId().equals(budgetReferenceId)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_YearClosed_Err@");
            bundle.setResult(result);
            return;
          }
        }

        // fetching finacial year
        yearId = proposalDAO.getFinancialYear(clientId);
        if ("".equals(yearId)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_FinancialYear_NotDefine@");
          bundle.setResult(result);
          return;
        }

        // fetching warehouse
        warehouselist = proposalDAO.getWarehouse(proposalmgmt.getClient().getId());
        if (warehouselist.size() > 0) {
          warehouse = warehouselist.get(0);
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Warehouse_NotDefine@");
          bundle.setResult(result);
          return;
        }

        // fetching bplocation
        bploclist = proposalDAO.getLocation(proposalmgmt.getSupplier().getId());
        if (bploclist.size() > 0)
          bplocation = bploclist.get(0);
        else {
          String message = OBMessageUtils.messageBD("ESCM_SuppLoc_NotDefine");
          message = message.replace("%", proposalmgmt.getSupplier().getName());
          OBError result = OBErrorBuilder.buildMessage(null, "error", message);
          bundle.setResult(result);
          return;
        }

        // fetching pricelist
        priceListlist = proposalDAO.getPriceList(proposalmgmt.getClient().getId());
        if (priceListlist.size() > 0)
          priceList = priceListlist.get(0);
        else {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PriceList_NotDefine@");
          bundle.setResult(result);
          return;
        }

        // fetching payment term
        paymentTermList = proposalDAO.getPaymentTerm(proposalmgmt.getClient().getId());
        if (paymentTermList.size() > 0)
          paymentTerm = paymentTermList.get(0);
        else {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_PaymentTerm_NotDefine@");
          bundle.setResult(result);
          return;
        }
        if (user.getBusinessPartner() == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_CrtPOfrmProsal_NotBP@");
          bundle.setResult(result);
          return;
        }
        // default value brought for mot contact person/position so no need validation.
        /*
         * if (user.getBusinessPartner() != null && user.getBusinessPartner().getEhcmPosition() ==
         * null) { OBError result = OBErrorBuilder.buildMessage(null, "error",
         * "@ESCM_LoggUser_PosNotDef@"); bundle.setResult(result); return; }
         */

        // fetching document type
        Object transdoctype = proposalDAO.getTransactionDoc(orgId, clientId);
        if (transdoctype != null) {
          transdoctypeId = (String) transdoctype;
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PODocType_NotDefine@");
          bundle.setResult(result);
          return;
        }

        if (transdoctypeId != null && paymentTerm != null && priceList != null && warehouse != null
            && bplocation != null && yearId != null) {
          DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
          City cityObj = OBDal.getInstance().get(City.class, city);
          // Date date = format.parse(signatureDate);
          // Date contractSDate = format.parse(contractStartDate);
          // Date ContractEDate = format.parse(ContractEndDate);
          // Date onBdDateh = format.parse(onBoardDateh);
          // Date onBdDateg = format.parse(onBoardDateg);

          Order order = OBProvider.getInstance().get(Order.class);
          order.setClient(proposalmgmt.getClient());
          order.setOrganization(proposalmgmt.getOrganization());
          order.setCreatedBy(user);
          order.setUpdatedBy(user);
          order.setSalesTransaction(false);
          order.setDocumentType(OBDal.getInstance().get(DocumentType.class, "0"));
          order.setTransactionDocument(OBDal.getInstance().get(DocumentType.class, transdoctypeId));
          order.setDocumentNo(UtilityDAO.getSequenceNo(conn, clientId,
              order.getTransactionDocument().getDocumentSequence().getName(), true));

          order.setBusinessPartner(proposalmgmt.getSupplier());
          order.setEscmNotes(poNotes);
          order.setEscmSignaturedate(signatureDate);
          order.setEscmCCity(cityObj);
          order.setEscmOrdertype("PUR");
          if (checkOrderIsContract) {
            order.setEscmOrdertype("CR");
            BigDecimal advpayper = new BigDecimal(advpaymentper);
            BigDecimal advpayamt = new BigDecimal(advpaymentamt);
            order.setEscmContractstartdate(contractStartDate);
            order.setEscmContractenddate(ContractEndDate);
            order.setEscmOnboarddategreg(d1.format(onBoardDateh));
            order.setEscmOnboarddateh(onBoardDateg);
            order.setEscmContractduration(contractDuration);
            order.setEscmPeriodtype(periodType);
            order.setEscmAdvpaymntAmt(advpayper);
            order.setEscmAdvpaymntPercntge(advpayamt);
          } else {
            order.setEscmContractstartdate(null);
            order.setEscmContractenddate(null);
            order.setEscmOnboarddategreg(null);
            order.setEscmOnboarddateh(null);
            order.setEscmContractduration(null);
            order.setEscmPeriodtype(null);
            order.setEscmAdvpaymntAmt(BigDecimal.ZERO);
            order.setEscmAdvpaymntPercntge(BigDecimal.ZERO);
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
          order.setGrandTotalAmount(proposalmgmt.getTotalamount());
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
          order.setBusinessPartner(proposalmgmt.getSupplier());
          proposalmgmt.setDocumentNo(order);
          OBDal.getInstance().save(order);
          OBDal.getInstance().flush();

          int ordercount = POcontractAddproposalDAO.insertOrderline(conn, proposalmgmt, order);
          OBDal.getInstance().flush();
          String processId = "06F0902CF5CB4FE1A70A994E34950FF7";
          ProcessBundle pb = new ProcessBundle(processId, vars);
          pb.setCloseConnection(false);
          HashMap<String, Object> parameters = new HashMap<String, Object>();
          parameters.put("C_Order_ID", order.getId());
          parameters.put("notes", poNotes);
          pb.setParams(parameters);
          POContractSummaryAction orderSubmit = new POContractSummaryAction();
          orderSubmit.doExecute(pb);
          error = (OBError) pb.getResult();

          if (ordercount == 1 && error.getMessage().equals("@Escm_Ir_complete_success@")) {

            // send an alert to contract user when po is created
            description = sa.elm.ob.scm.properties.Resource
                .getProperty("scm.contractuser.alert", vars.getLanguage())
                .concat("" + proposalmgmt.getProposalno());
            AlertUtility.alertInsertBasedonPreference(order.getId(), order.getDocumentNo(),
                "ESCM_Contract_User", order.getClient().getId(), description, "NEW", alertWindow,
                "scm.contractuser.alert", Constants.GENERIC_TEMPLATE, windowId, null);

            String message = OBMessageUtils.messageBD("ESCM_CreatePOForProposal_Success");
            message = message.replace("%", order.getDocumentNo());
            OBError result = OBErrorBuilder.buildMessage(null, "success", message);
            bundle.setResult(result);
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_CreatePOForProsalNotSuccess@");
            bundle.setResult(result);
          }
        }
      }
    } catch (Exception e) {
      log.error("Exeception in CreatePOFromProposal Process:", e);
      e.printStackTrace();
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
