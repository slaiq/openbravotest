package sa.elm.ob.finance.ad_process;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.SQLQuery;
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
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinPropertyCompensation;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gopalakrishnan
 *
 */

public class GenerateAmarsarafFromPropComp extends DalBaseProcess {

  /**
   * Generate amarsaraf from property compensation
   */
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(GenerateAmarsarafFromPropComp.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      AccountingCombination uniqueCode = null;

      String string_prop_comp_id = (String) bundle.getParams().get("Efin_Property_Compensation_ID");
      FIN_PaymentMethod payMethod = null;
      PaymentTerm payTerm = null;
      PriceList prcList = null;
      DocumentType docType = null;
      EfinPropertyCompensation objPropertyComp = OBDal.getInstance()
          .get(EfinPropertyCompensation.class, string_prop_comp_id);

      String ClientId = objPropertyComp.getClient().getId();
      String CalendarId = "";
      String AccountDate = "";

      // have document type
      OBQuery<DocumentType> transactionDoc = OBDal.getInstance().createQuery(DocumentType.class,
          "as e where e.documentCategory = 'API'");
      transactionDoc.setMaxResult(1);
      if (transactionDoc.list() != null && transactionDoc.list().size() > 0) {
        docType = transactionDoc.list().get(0);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RdvInv_DocType@");
        bundle.setResult(result);
        return;
      }

      // check have payment method or not
      SQLQuery paymentMethodQuery = OBDal.getInstance().getSession().createSQLQuery(
          "select pm.FIN_PaymentMethod_ID from FIN_PaymentMethod pm where pm.FIN_PaymentMethod_ID in "
              + "(select fin.FIN_PaymentMethod_ID from FIN_FinAcc_PaymentMethod fin where Payout_Allow = 'Y' and ad_client_id = '"
              + ClientId + "') and EM_EFIN_IsDefault = 'Y' order by updated desc limit 1");
      if (paymentMethodQuery != null) {
        @SuppressWarnings("unchecked")
        List<String> paymentMethodQueryList = paymentMethodQuery.list();
        if (paymentMethodQueryList.size() > 0) {
          String paymentId = (String) paymentMethodQuery.list().get(0);
          payMethod = OBDal.getInstance().get(FIN_PaymentMethod.class, paymentId);
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RdvInv_PaymentNo@");
          bundle.setResult(result);
          return;
        }
      }

      // check have payment term or not.
      OBQuery<PaymentTerm> paymentTerm = OBDal.getInstance().createQuery(PaymentTerm.class,
          " default = 'Y' and client.id = '" + ClientId + "' order by updated desc");
      paymentTerm.setMaxResult(1);
      if (paymentTerm.list() != null && paymentTerm.list().size() > 0) {
        payTerm = paymentTerm.list().get(0);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RdvInv_PaymentTermNo@");
        bundle.setResult(result);
        return;
      }

      // check have pricelist
      OBQuery<PriceList> pricelist = OBDal.getInstance().createQuery(PriceList.class, "");
      pricelist.setMaxResult(1);
      if (pricelist.list() != null && pricelist.list().size() > 0) {
        prcList = pricelist.list().get(0);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Rdvinv_PriceList@");
        bundle.setResult(result);
        return;
      }

      // check already invoice is generated or not

      Invoice invoiceObj = objPropertyComp.getInvoice();
      if (invoiceObj != null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyInvoiced_Amarsaraf@");
        bundle.setResult(result);
        return;
      }

      /* Assign the Bundle Parameters */
      final String orgId = objPropertyComp.getOrganization().getId();
      Invoice newinvoice = null;
      Organization org = null;
      org = OBDal.getInstance().get(Organization.class, objPropertyComp.getOrganization().getId());
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

      if (objPropertyComp.getOrganization().getCalendar() != null) {
        CalendarId = objPropertyComp.getOrganization().getCalendar().getId();

      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession()
            .createSQLQuery("select eut_parent_org ('" + objPropertyComp.getOrganization().getId()
                + "','" + objPropertyComp.getClient().getId() + "')");
        // List<String> list = query.list();
        Object list = query.list().get(0);
        orgIds = ((String) list).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();
            break;
          }
        }
      }

      String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "GS", CalendarId, orgId, true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoGeneralSequence"));
      }

      // generate invoice for rdv version
      newinvoice = insertInvoice(uniqueCode, objPropertyComp, orgId, payMethod, payTerm, prcList,
          docType);
      if (newinvoice != null) {
        objPropertyComp.setInvoice(newinvoice);
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Success@");
        bundle.setResult(result);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@HB_INTERNAL_ERROR@");
        bundle.setResult(result);
      }

      return;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      e.printStackTrace();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param uniqueCode
   * @param objPropertyComp
   * @param orgId
   * @param payMethod
   * @param payTerm
   * @param prcList
   * @param docType
   * @return Invoice object once the invoice created successfully
   */
  private Invoice insertInvoice(AccountingCombination uniqueCode,
      EfinPropertyCompensation objPropertyComp, String orgId, FIN_PaymentMethod payMethod,
      PaymentTerm payTerm, PriceList prcList, DocumentType docType) {
    // TODO Auto-generated method stub
    try {
      OBContext.setAdminMode();

      Invoice invoice = OBProvider.getInstance().get(Invoice.class);
      invoice.setOrganization(objPropertyComp.getOrganization());
      invoice.setTransactionDocument(docType);

      // find Budget Definition Based On Date

      String budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(new Date(),
          objPropertyComp.getClient().getId(), "");

      DocumentType doctype = OBDal.getInstance().get(DocumentType.class, "0");
      invoice.setDocumentType(doctype);

      // check the cost center 0202600
      OBQuery<SalesRegion> dept = OBDal.getInstance().createQuery(SalesRegion.class,
          "searchKey= :searchKey");
      dept.setNamedParameter("searchKey", "0202600");
      dept.setMaxResult(1);
      invoice.setEfinCSalesregion(dept.list().get(0));
      // invoice.setEfinCSalesregion();
      invoice.setBusinessPartner(objPropertyComp.getBusinessPartner());
      invoice.setPartnerAddress(
          objPropertyComp.getBusinessPartner().getBusinessPartnerLocationList().get(0));
      invoice.setInvoiceDate(new Date());
      invoice.setAccountingDate(new Date());
      invoice.setEfinEncumtype("A");
      invoice.setEfinEncumbranceType("AEE");
      invoice
          .setEfinBudgetint(OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitialId));

      invoice.setPaymentMethod(payMethod);
      invoice.setPaymentTerms(payTerm);
      Currency currency = OBDal.getInstance().get(Currency.class, "317");
      invoice.setCurrency(currency);
      // Set the budget type as fund
      invoice.setEfinBudgetType("F");
      invoice.setEfinInvoicetypeTxt("API");
      // invoice.setEfinCSalesregion(uniqueCode.getSalesRegion());
      // invoice.setEfinIsrdv(true);
      invoice.setEfinAdRole(OBContext.getOBContext().getRole());
      invoice.setSalesTransaction(false);
      invoice.setPriceList(prcList);
      invoice.setEfinInwarddate(objPropertyComp.getAwardDate());
      invoice.setEfinInwardno(objPropertyComp.getAwardNo());
      invoice.setDescription(objPropertyComp.getDescription());

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      return invoice;
    } catch (Exception e) {
      e.printStackTrace();
      LOG.error(" Exception while inserting invoice fro Property Comp: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
