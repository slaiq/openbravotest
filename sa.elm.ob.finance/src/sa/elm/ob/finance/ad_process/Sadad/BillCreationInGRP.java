package sa.elm.ob.finance.ad_process.Sadad;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINSadadbilConfig;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinLookupLine;
import sa.elm.ob.scm.ESCM_Certificates;
import sa.elm.ob.utility.sadad.producer.SadadBillDetail;
import sa.elm.ob.utility.sadad.producer.SadadBillRequest;
import sa.elm.ob.utility.sadad.producer.SadadBillResponse;

/**
 * This class is responsible for creating order to receive in our GRP from any external Source This
 * also will send created ordetoreceive to saddad and then get status of the bill, if it is paid
 * then we will do addreceipt
 * 
 * @author Sathishkumar
 *
 */
public class BillCreationInGRP {
  private static final Logger log = LoggerFactory.getLogger(BillCreationInGRP.class);

  public static SadadBillResponse billCreation(SadadBillRequest request, String strClientId) {

    try {
      OBContext.setAdminMode();

      String strDocumentNo = "", error;
      String clientId = strClientId;
      StringBuilder errorMsg = new StringBuilder("");
      int count = 10;

      EfinLookupLine customerType, applicationType;
      ESCM_Certificates customerIdType;
      ElementValue mainAccount;
      Currency currency;
      PriceList pricelist;

      Client client = BillCreationInGrpDao.getClient(strClientId);
      if (client == null) {
        return BillCreationInGrpDao.createResponse(true, ReceiveBillErrorConstant.CLIENT_ERROR, 0);
      }

      EFINSadadbilConfig sadadBill = BillCreationInGrpDao.getSadadBillConfigObj(client);
      if (sadadBill == null) {
        return BillCreationInGrpDao.createResponse(true,
            ReceiveBillErrorConstant.CONFIGURATION_ERROR, 0);
      }

      User user = BillCreationInGrpDao.getUser(ReceiveBillErrorConstant.USER_ID, clientId);
      if (user == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.USER_ERROR);
      }

      BusinessPartner bpartner = BillCreationInGrpDao.getBpartner(request.getIDNo().getValue(),
          client.getId());
      Location location = null;
      if (bpartner == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.BP_ERROR);
      } else {
        location = bpartner.getBusinessPartnerLocationList() != null
            ? bpartner.getBusinessPartnerLocationList().get(0)
            : null;
        if (location == null) {
          errorMsg.append("  " + ReceiveBillErrorConstant.LOCATION_ERROR);
        }
      }

      DocumentType doctype = sadadBill.getDocumentType();
      if (doctype != null) {
        Sequence sequence = doctype.getDocumentSequence();
        if (sequence != null && doctype.getDocumentSequence().getNextAssignedNumber() != null) {
          strDocumentNo = doctype.getDocumentSequence().getNextAssignedNumber().toString();
          sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + 1);
        } else {
          errorMsg.append("  " + ReceiveBillErrorConstant.SEQUENCE_ERROR);
        }
      }

      EfinBudgetIntialization budgetInt = BillCreationInGrpDao.getBudgetDefintion(clientId);
      if (budgetInt == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.BUDGETDEF_ERROR);
      }
      mainAccount = BillCreationInGrpDao.getElementValue(String.valueOf(request.getMainAccount()),
          clientId);
      if (mainAccount == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.MAINACCOUNT_ERROR);
      }

      customerIdType = BillCreationInGrpDao.getCustomerReferenceLookup(
          request.getIDType() != null ? String.valueOf(request.getIDType()) : "", clientId);
      if (customerIdType == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.CUSTOMERIDTYPE_ERROR);
      }

      customerType = BillCreationInGrpDao.getReferenceLookup(ReceiveBillErrorConstant.CUSTOMER_TYPE,
          String.valueOf(request.getCustomerType()), clientId);
      if (customerType == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.CUSTOMERTYPE_ERROR);
      }

      applicationType = BillCreationInGrpDao.getReferenceLookup(
          ReceiveBillErrorConstant.APPLICATION_TYPE, String.valueOf(request.getApplicationType()),
          clientId);
      if (applicationType == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.APPLICATION_ERROR);
      }

      currency = BillCreationInGrpDao.getCurrency(clientId);
      if (currency == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.CURRENCY_ERROR);
      }

      pricelist = BillCreationInGrpDao.getPriceList(clientId);
      if (currency == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.CURRENCY_ERROR);
      }

      GLItem item = BillCreationInGrpDao.getGLItem(clientId);

      if (StringUtils.isNotEmpty(errorMsg.toString())) {
        OBDal.getInstance().rollbackAndClose();
        return BillCreationInGrpDao.createResponse(true, errorMsg.toString(), 0);
      }

      // Creation of bill in our ERP
      Invoice invoice = OBProvider.getInstance().get(Invoice.class);
      invoice.setClient(client);
      invoice.setOrganization(sadadBill.getOrganization());
      invoice.setCreatedBy(user);
      invoice.setUpdatedBy(user);
      invoice.setCreationDate(new Date());
      invoice.setUpdated(new Date());
      invoice.setSalesTransaction(true);
      invoice.setDocumentType(sadadBill.getDocumentType());
      invoice.setTransactionDocument(sadadBill.getDocumentType());
      invoice.setDocumentNo(strDocumentNo);
      invoice.setBusinessPartner(bpartner);
      invoice.setPartnerAddress(location);
      invoice.setPaymentTerms(sadadBill.getPaymentTerms());
      invoice.setDescription(request.getNotes().getValue());
      invoice.setEfinAccounttype(sadadBill.getAccountType());
      invoice.setInvoiceDate(new Date());
      invoice.setPriceList(pricelist);
      invoice.setAccountingDate(new Date());
      invoice.setPaymentMethod(sadadBill.getPaymentMethod());
      invoice.setEfinFinFinacct(sadadBill.getFinacct());
      invoice.setEfinBudgetint(budgetInt);
      invoice.setEfinApplicationtype(applicationType);
      invoice.setEfinIdno(customerIdType.getCertificateName().getItemvalue().toString());
      invoice.setEfinCustomeridtype(customerIdType);
      invoice.setEfinCustomertype(customerType);
      invoice.setEfinElementvalue(mainAccount);
      invoice.setEfinMobileno(request.getMobileNo().getValue());
      invoice.setEfinDeptauthcode(sadadBill.getDeptauthcode());
      invoice.setEfinDeptbenefitcode(sadadBill.getDeptbenefitcode());
      invoice.setDocumentAction("CO");
      invoice.setDocumentStatus("DR");
      invoice.setProcessed(false);
      invoice.setProcessNow(false);
      invoice.setCurrency(currency);
      invoice.setGrandTotalAmount(request.getTotalBillAmount());
      invoice.setTotalPaid(request.getTotalBillAmount());
      invoice.setSummedLineAmount(BigDecimal.ZERO);
      invoice.setEfinCreatedfromsadad(true);
      invoice.setOutstandingAmount(BigDecimal.ZERO);
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();

      // create invoiceline
      List<SadadBillDetail> sadadDetailList = request.getSaddadBillDetail();
      AccountingCombination uniqueCode = BillCreationInGrpDao.getUniqueCodeObj(invoice, sadadBill);
      if (uniqueCode == null) {
        errorMsg.append("  " + ReceiveBillErrorConstant.UNIQUECODE_ERROR);
      }

      if (StringUtils.isNotEmpty(errorMsg.toString())) {
        OBDal.getInstance().rollbackAndClose();
        return BillCreationInGrpDao.createResponse(true, errorMsg.toString(), 0);
      }

      for (SadadBillDetail detail : sadadDetailList) {
        EfinLookupLine serviceItem = BillCreationInGrpDao
            .getServiceItemConfig(detail.getMOTSubAccount(), invoice, sadadBill);
        if (serviceItem == null) {
          OBDal.getInstance().rollbackAndClose();
          return BillCreationInGrpDao.createResponse(true,
              ReceiveBillErrorConstant.SERVICEITEM_ERROR, 0);
        }
        org.openbravo.model.common.invoice.InvoiceLine invoiceline = OBProvider.getInstance()
            .get(org.openbravo.model.common.invoice.InvoiceLine.class);
        invoiceline.setId(SequenceIdData.getUUID());
        invoiceline.setNewOBObject(true);
        invoiceline.setClient(client);
        invoiceline.setOrganization(invoice.getOrganization());
        invoiceline.setBusinessPartner(bpartner);
        invoiceline.setDescription(invoice.getDescription());
        invoiceline.setEfinCValidcombination(uniqueCode);
        invoiceline.setEfinCSalesregion(uniqueCode.getSalesRegion());
        invoiceline.setEfinCActivity(uniqueCode.getActivity());
        invoiceline.setEfinCBpartner(uniqueCode.getBusinessPartner());
        invoiceline.setEfinCCampaign(uniqueCode.getSalesCampaign());
        invoiceline.setEfinCElementvalue(uniqueCode.getAccount());
        invoiceline.setEfinServiceitem(serviceItem);
        invoiceline.setEFINUniqueCode(uniqueCode.getEfinUniqueCode());
        invoiceline.setEFINUniqueCodeName(uniqueCode.getEfinUniquecodename());
        invoiceline.setGrossAmount(detail.getBillAmount());
        invoiceline.setInvoice(invoice);
        invoiceline.setProject(uniqueCode.getProject());
        invoiceline.setEfinCElementvalue(uniqueCode.getAccount());
        invoiceline.setLineNo((long) count);
        invoiceline.setNdDimension(uniqueCode.getNdDimension());
        invoiceline.setStDimension(uniqueCode.getStDimension());
        invoiceline.setUnitPrice(detail.getBillAmount());
        invoiceline.setLineNetAmount(detail.getBillAmount());
        invoiceline.setUOM(OBDal.getInstance().get(UOM.class, ReceiveBillErrorConstant.USER_ID));

        invoiceline.setAccount(item);
        invoiceline.setFinancialInvoiceLine(true);
        OBDal.getInstance().save(invoiceline);
        OBDal.getInstance().flush();
      }

      OBDal.getInstance().flush();

      error = BillCreationInGrpDao.completeInvoice(invoice);

      if (StringUtils.isNotEmpty(error)) {
        OBDal.getInstance().rollbackAndClose();
        return BillCreationInGrpDao.createResponse(true, error, 0);
      }

      // billRequest = AddSadadBillValidation.createSadadBillRequest(invoice);
      //
      // // Call to sadad create bill webservice
      // SadadIntegrationService service = new SadadIntegrationServiceImpl();
      // billResponse = service.createNewBill(billRequest);
      // log.debug("billResponse:" + billResponse);
      //
      // // set response from Saddad to corresponding fields
      // if (billResponse != null) {
      // isValid = AddSadadBillValidation.setFieldValue(billResponse, invoice);
      // } else {
      // return BillCreationInGrpDao.createResponse(true,
      // OBMessageUtils.messageBD(ReceiveBillErrorConstant.EMPTY_RESPONSE), 0);
      // }

      try {
        Integer.parseInt(invoice.getDocumentNo());
      } catch (Exception e) {
        return BillCreationInGrpDao.createResponse(true, ReceiveBillErrorConstant.DOCUMENTNO_ERROR,
            0);
      }
      return BillCreationInGrpDao.createResponse(false, "",
          Integer.parseInt(invoice.getDocumentNo()));
    } catch (

    Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      String errorMsg = e.getMessage();
      if (errorMsg.contains(ReceiveBillErrorConstant.INTERNAL_SERVER_ERRORCODE)
          || errorMsg.contains(ReceiveBillErrorConstant.NO_CONNECTION)) {
        return BillCreationInGrpDao.createResponse(true, errorMsg, 0);
      }
      log.debug("Error while creating bil in grp" + e.getMessage());
      return BillCreationInGrpDao.createResponse(true,
          OBMessageUtils.messageBD("HB_INTERNAL_ERROR"), 0);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
