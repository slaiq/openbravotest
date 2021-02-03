package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.actionHandler.POcontractAddproposalDAO;
import sa.elm.ob.scm.ad_callouts.PurchaseAgreementCalloutDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Divya on 29/11/2017
 */

// Create PO from Proposal
public class CreatePOFromProposal implements Process {

  private static Logger log = Logger.getLogger(CreatePOFromProposal.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      // Variable declaration
      // Variable declaration
      String proposalId = null;
      String proposalattrId = null;
      if (bundle.getParams().get("Escm_Proposalmgmt_ID") != null) {
        proposalId = bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
      }
      if (bundle.getParams().get("Escm_Proposal_Attr_ID") != null) {
        proposalattrId = bundle.getParams().get("Escm_Proposal_Attr_ID").toString();
        EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalattrId);
        proposalId = proposalAttr.getEscmProposalmgmt().getId();
      }
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      final String clientId = bundle.getContext().getClient();
      final String orgId = proposalmgmt.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
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

      String motContactPerson = "";
      String motContactPosition = "";
      String description = null;
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.contractUser;
      String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
      NextRoleByRuleVO nextApproval = null;

      ProposalManagementProcessDAO proposalDAO = new ProposalManagementProcessDAOImpl();
      // get the connection
      Connection conn = OBDal.getInstance().getConnection();

      boolean isMinProposalApproved = false;
      if (proposalmgmt.getProposalstatus().equals("PAWD")) {
        isMinProposalApproved = proposalDAO.isMinProposalApproved(proposalmgmt);
      }

      if ((proposalmgmt.getProposalappstatus().equals("APP")
          && proposalmgmt.getProposalstatus().equals("AWD"))
          || (proposalmgmt.getProposalstatus().equals("PAWD")
              && (isMinProposalApproved || proposalmgmt.getProposalappstatus().equals("APP")))) {

        // based on configuration minvalue , getting purchase order type is purchase order /contract
        if (proposalmgmt.getProposalstatus().equals("PAWD")) {
          config = proposalDAO.getPOTypeBasedOnValue(orgId, proposalmgmt.getAwardamount());
        } else {
          config = proposalDAO.getPOTypeBasedOnValue(orgId, proposalmgmt.getTotalamount());
        }

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

        // Throw error if contract category is inactive
        if (proposalmgmt != null && proposalmgmt.getContractType() != null
            && !proposalmgmt.getContractType().isActive()) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_ContractCategoryInactive@");
          bundle.setResult(result);
          return;
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
            // OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_YearClosed_Err@");
            // bundle.setResult(result);
            // return;
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
          // order.setEscmOnboarddateh(new java.util.Date());
          order.setEscmOrdertype(purchaseOrderType);
          // order.setEscmOnboarddategreg(d1.format(new Date()));
          order.setPartnerAddress(bplocation);
          order.setEscmContractduration(null);
          // order.setEscmPeriodtype("DT");
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
          order.setEscmAdvpaymntPercntge(BigDecimal.ZERO);
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
          // order.setEscmContractstartdate(new java.util.Date());
          // order.setEscmContractenddate(new java.util.Date());
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
          if (proposalmgmt.getSecondsupplier() != null)
            order.setEscmSecondsupplier(proposalmgmt.getSecondsupplier());
          if (proposalmgmt.getSecondBranchname() != null)
            order.setEscmSecondBranchname(proposalmgmt.getSecondBranchname());
          order.setEscmIssecondsupplier(proposalmgmt.isSecondsupplier());
          if (proposalmgmt.getIBAN() != null)
            order.setEscmSecondIban(proposalmgmt.getIBAN());
          if (proposalmgmt.getSubcontractors() != null)
            order.setEscmSubcontractors(proposalmgmt.getSubcontractors());
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

          OBDal.getInstance().save(order);

          proposalmgmt.setDocumentNo(order);
          OBDal.getInstance().save(proposalmgmt);

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

          if (ordercount == 1) {
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
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception in CreatePOFromProposal: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      log.error("Exeception in CreatePOFromProposal Process:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
