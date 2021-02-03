package sa.elm.ob.finance.ad_process.PurchaseInvoice;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.PostUtilsDAO;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFIN_TaxMethod;
import sa.elm.ob.finance.EfinBudgetActual;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinEncumInvoiceRef;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.finance.efinpurchaseinapphist;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAO;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAOImpl;
import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAOImpl;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.hook.PurchaseinvoiceCompletionHookCaller;
//import sa.elm.ob.finance.ad_process.PurchaseInvoice.hook.PurchaseInvoiceHookCaller;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.finance.util.DAO.EncumbranceProcessDAO;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.InvoiceApprovalTable;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;
import sa.elm.ob.utility.util.UtilityVO;

/**
 * @author Divya on 17/08/2016
 */

public class PurchaseInvoiceSubmit extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(PurchaseInvoiceSubmit.class);
  private static final String API_DOCUMENT = "API";
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";
  private static final String RDV_DOCUMENT = "RDV";
  private static final String PO_DOCUMENT = "POM";
  private static Boolean hasDelegation = Boolean.FALSE;
  private static String delegatedFromRole = "";
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
  Invoice invoice = null;

  @SuppressWarnings("unused")
  @Override
  public synchronized void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String preferenceValue = "";

    try {
      OBContext.setAdminMode();

      // Declare the Variable
      String invoiceId = (String) bundle.getParams().get("C_Invoice_ID");
      String tabId = (String) bundle.getParams().get("tabId");

      invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      String sql = "", sql1 = "", sql2 = "", sql3 = "", negamtqry = "", errorMessage = "",
          status = "", appstatus = "", encumdoctype = "", pendingapproval = "", linesumqry = "",
          invlineqry = "";
      PreparedStatement ps = null, ps1 = null, ps2 = null, ps3 = null, negamtps = null,
          deptvalps = null, invlinesps = null, linesumps = null, invlineps = null;
      BigDecimal PendingAmt = BigDecimal.ZERO, ReaduceExpAmt = BigDecimal.ZERO,
          grandTotal = BigDecimal.ZERO, conversionrate = BigDecimal.ZERO,
          lineNetAmt = BigDecimal.ZERO, sumLineNetAmt = BigDecimal.ZERO;
      SQLQuery invLineQuery = null;
      ResultSet rs = null, rs1 = null, rs2 = null, rs3 = null, negamtrs = null, deptvalrs = null,
          invlinesrs = null, linesumrs = null, invliners = null;
      ConnectionProvider conn = bundle.getConnection();
      final String clientId = bundle.getContext().getClient();
      final String orgId = invoice.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
      String roleId = bundle.getContext().getRole();
      boolean checkException = false, chkUserIsDeptHead = false, reserve = false,
          allowApprove = false, isValid = Boolean.TRUE;
      String comments = bundle.getParams().get("comments").toString();
      String uniquecode = null, p_instance_id = null, DeptHeadRoleId = null;
      int count = 0, errorcount = 0;
      String headerId = null, doctype = null, encumtype = null, errorMsg = "", currencyId = null,
          alertRuleId = "", alertWindow = "", Description = "", alertKey = "";
      SimpleDateFormat timeYearFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Organization organization = null;
      Currency currency = null;
      Boolean isCostEncumbrance = Boolean.FALSE;
      Date currentDate = new Date();
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date todaydate = dateFormat.parse(dateFormat.format(currentDate));
      Boolean isAdjustementInvoice = Boolean.FALSE, isReserveRoleCrossed = Boolean.FALSE;
      String DocType = "";
      Boolean isExclusiveTaxInvoice = Boolean.FALSE;
      final String costCenterId = invoice.getEfinCSalesregion().getId();
      String createdUserId = null;
      String createdRoleId = null;
      String currentDocStatus = null;
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      boolean isRDVLastVersion = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
      List<EfinBudgetManencumlines> fundsEncumList = new ArrayList<EfinBudgetManencumlines>();

      // Task No.7552

      if (log.isDebugEnabled()) {
        log.debug("action:" + invoice.getDocumentAction());
        log.debug("isSalesTransaction:" + invoice.isSalesTransaction());
      }

      EfinBudgetManencumv encumId = null;
      String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
      isCostEncumbrance = PurchaseInvoiceSubmitUtils.isCostEncumbrance(invoice);
      isAdjustementInvoice = PurchaseInvoiceSubmitUtils.isAdjustmentOnlyInvoice(invoice);
      TaxLineHandlerDAO taxHandler = new TaxLineHandlerImpl();
      boolean allowUpdate = false, allowDelegation = false;
      List<EfinBudgetControlParam> budgCtrlParamList = null;
      List<InvoiceLine> invoiceLinList = null;
      JSONObject invoiceCheck = new JSONObject();
      BigDecimal taxValues = BigDecimal.ZERO;
      String hqOrgId = null;
      BigDecimal taxAmount = BigDecimal.ZERO;
      TaxLineHandlerDAO taxDao = new TaxLineHandlerImpl();
      Boolean checkEncumbranceAmountZero = false;
      // Order ord = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
      JSONObject calculateTaxAmount = new JSONObject();
      Boolean isPrepaymentApp = invoice.getDocumentType().isEfinIsprepayinvapp() != null
          && invoice.getDocumentType().isEfinIsprepayinvapp() ? true : false;

      if (invoice.getDocumentStatus().equals("DR")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // if the corresponding sales order is closed
      // not allowed to raise the invoice against the order
      if (invoice.getSalesOrder() != null && invoice.getDocumentStatus().equals("DR")) {
        Order order = OBDal.getInstance().get(Order.class, invoice.getSalesOrder().getId());
        if ("ESCM_CL".equals(order.getEscmAppstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PO_Closed@");
          bundle.setResult(result);
          return;
        }
      }

      if (invoice.getEutNextRole() != null) {
        if (invoice.getEfinSubmittedRole() != null
            && invoice.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = invoice.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (invoice.getEutNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      if (invoice.getEfinTaxMethod() != null && invoice.getDocumentStatus().equals("DR")
          && !invoice.getEfinTaxMethod().isActive()) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_ Tax_not_active@");
        bundle.setResult(result);
        return;
      }
      // Payment Schedule Validations
      if (invoice.getDocumentStatus().equals("DR") && invoice.isEfinIspaymentSch()) {
        if (invoice.getEFINInvoicePaymentSchList().size() == 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Payment_Schedule_Mandatory@");
          bundle.setResult(result);
          return;
        }
        JSONObject json_payment_sch_valid = PurchaseInvoiceSubmitUtils
            .checkPaymentScheduleValidation(vars, invoice);
        if (json_payment_sch_valid != null
            && StringUtils.isNotEmpty(json_payment_sch_valid.get("message").toString())) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              json_payment_sch_valid.get("message").toString());
          bundle.setResult(result);
          return;
        }
        // update the invoice amount details in payment schedule
        Boolean isUpdated = PurchaseInvoiceSubmitUtils.updatePaymentScheduleDetails(invoice, vars);
        if (!isUpdated) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Payment_Schedule_Not_Updated@");
          bundle.setResult(result);
          return;
        }
        // update the po as closed when the payment schedule have the final payment
        Boolean checkFinalPaymentAndUpdated = PurchaseInvoiceSubmitUtils
            .checkFinalPaymentAndUpdateOrder(invoice, vars);
        if (!checkFinalPaymentAndUpdated) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Order_Not_Closed@");
          bundle.setResult(result);
          return;
        }

      }
      if (strInvoiceType.equals("POM") && (invoice.getSalesOrder() != null
          && !invoice.getSalesOrder().getEscmTaxMethod().isActive())) {
        OBQuery<InvoiceLine> invoiLine = OBDal.getInstance().createQuery(InvoiceLine.class,
            "as e where e.eFINIsTaxLine='N' and e.invoice.id=:invoiceId");
        invoiLine.setNamedParameter("invoiceId", invoice.getId());
        invoiceLinList = invoiLine.list();
        if (invoiceLinList.size() > 0) {
          for (InvoiceLine lineObj : invoiLine.list()) {
            calculateTaxAmount = taxDao.calculateTaxAmount(lineObj.getId(), lineObj);
            taxValues = new BigDecimal(calculateTaxAmount.get("TaxAmount").toString());
            taxAmount = taxAmount.add(taxValues);
          }
          if (invoice.getEfinTaxAmount() != null
              && (taxAmount.intValue() != invoice.getEfinTaxAmount().intValue())) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Tax_not_calculated@");
            bundle.setResult(result);
            return;
          }
        }
      }

      if (invoice.getEfinCOrder() != null) {
        Order ord = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
        if (invoice.getEfinCOrder() != null && ord.getEscmTaxMethod() != null
            && invoice.getEfinTaxMethod() == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Taxmethod_notnull@");
          bundle.setResult(result);
          return;
        }

        // This block is used to check the new tax validation
        if (ord.getEscmTaxMethod() != null) {
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(invoice.getEfinCOrder().getId());
          parameters
              .add(invoice.getEfinTaxMethod() != null ? invoice.getEfinTaxMethod().getId() : "");
          parameters.add(invoice.getId());
          parameters.add(strInvoiceType.equals("RDV") ? "Y" : "N");

          String isSuccess = (String) CallStoredProcedure.getInstance()
              .call("Efin_validatetaxininvoice", parameters, null);

          if (!"Y".equals(isSuccess)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_validatetaxbasedonorder@");
            bundle.setResult(result);
            return;
          }
        }
      }

      if (strInvoiceType.equals("RDV")
          && invoice.getEfinRdvtxn().getLineTaxamt().compareTo(BigDecimal.ZERO) > 0
          && !invoice.isEfinIstax()) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Taxmethod_notnull@");
        bundle.setResult(result);
        return;
      }

      if (strInvoiceType.equals("POM") && invoice.getInvoiceLineList().size() > 0) {
        for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
          OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
              "as e where e.id=:orderId");
          orderLine.setNamedParameter("orderId",
              invoiceLine.getSalesOrderLine() != null ? invoiceLine.getSalesOrderLine().getId()
                  : null);
          if (orderLine.list().size() > 0 && orderLine.list().get(0).getOrderedQuantity()
              .compareTo(invoiceLine.getInvoicedQuantity()) < 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Invqtygt_ordqty@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // Task no:8055

      if (strInvoiceType != null && strInvoiceType.equals("PPI") || strInvoiceType.equals("API")
          || strInvoiceType.equals("PPA")) {
        // get HQ Org
        OBQuery<EfinBudgetControlParam> budgCtrlParam = OBDal.getInstance()
            .createQuery(EfinBudgetControlParam.class, " as a where a.client.id=:clientId ");
        budgCtrlParam.setNamedParameter("clientId", invoice.getClient().getId());
        budgCtrlParamList = budgCtrlParam.list();
        if (budgCtrlParamList.size() > 0) {
          Organization hqOrg = budgCtrlParamList.get(0).getAgencyHqOrg();
          hqOrgId = hqOrg.getId();
        }
        if (hqOrgId != null && !submittedRoleOrgId.equals(hqOrgId)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@EFIN_InvoiceNotAllow_RegionLevel@");
          bundle.setResult(result);
          return;
        }
      }

      MultipleInvoiceLineAgainstPOLineDAO dao = new MultipleInvoiceLineAgainstPOLineDAOImpl();
      Boolean isError = false;
      if (invoice.getEfinBudgetint().getStatus().equals("CL")) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      if (invoice.getEfinBudgetint().isPreclose()) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      if (invoice.getDocumentStatus().equals("DR") && invoice.getEfinRdvtxn() == null) {
        int submitAllowed = CommonValidations.checkUserRoleForSubmit("c_invoice", vars.getUser(),
            vars.getRole(), invoiceId, "c_invoice_id");
        if (submitAllowed == 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      if ("EFIN_CA".equals(invoice.getDocumentStatus())) {
        throw new OBException(OBMessageUtils.messageBD("Efin_AlreadyPreocessed"));
      }

      if (invoice.getGrandTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_InvAmt_Zero"));
      }

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
      if (invoice.getSalesOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class, invoice.getSalesOrder().getId());
        if ("ESCM_WD".equals(order.getEscmAppstatus())
            || "ESCM_OHLD".equals(order.getEscmAppstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_HoldWithdrawnPO@");
          bundle.setResult(result);
          return;
        }
      }
      if (invoice.getEfinCOrder() != null) {
        Order objOrder = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
        if ("ESCM_WD".equals(objOrder.getEscmAppstatus())
            || "ESCM_OHLD".equals(objOrder.getEscmAppstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_HoldWithdrawnPO@");
          bundle.setResult(result);
          return;
        }
      }

      // Inward number and invoice date is mandatory
      if (invoice.getEfinInwarddate() == null || StringUtils.isEmpty(invoice.getEfinInwardno())) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@EFIN_InwardNo_DateMandatory@");
        bundle.setResult(result);
        return;
      }

      if (invoice.isEfinIstax()) {
        Boolean hasTaxLines = taxHandler.hasTaxLines(invoice);
        Boolean isExpenseInvoice = taxHandler.hasExpenseLines(invoice);
        if (!hasTaxLines && isExpenseInvoice && (!invoice.isEfinIstaxpo())) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_NoTaxLines"));
        }

        boolean isOrderHaveTax = PurchaseInvoiceSubmitUtils.getOrderHaveTax(invoice,
            strInvoiceType);

        if (!isOrderHaveTax) {
          Boolean recalculateTax = taxHandler.requiresTaxRecalculation(invoice);

          if (recalculateTax) {
            throw new OBException(OBMessageUtils.messageBD("Efin_RecalculateTax"));
          }
        }
      }

      if (!invoice.isEfinIstax()) {
        Boolean hasTaxLines = taxHandler.hasTaxLines(invoice);
        if (hasTaxLines) {
          throw new OBException(OBMessageUtils.messageBD("Efin_RemoveTaxLines"));
        }
      }

      if (PPI_DOCUMENT.equals(strInvoiceType)) {
        invoiceCheck = PurchaseInvoiceSubmitUtils.isValidPrepaymentInvoice(invoice);

        isValid = invoiceCheck.getBoolean("valid");

        if (!isValid) {
          throw new OBException(invoiceCheck.getString("message"));
        }
      }

      // Check if Invoice organization is different with that of selected PO's organization
      if (PO_DOCUMENT.equals(strInvoiceType) && invoice.getDocumentStatus().equals("DR")) {
        Order order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
        if (order != null) {
          String order_org_id = order.getOrganization().getId();
          String invoice_org_id = invoice.getOrganization().getId();
          if (!order_org_id.equals(invoice_org_id)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_OrderOrgDifferent@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // if reserve funds role crossed then should not allow to submit.
      if (invoice.getEfinInvoicetypeTxt().equals("PPA")) {
        DocType = DocumentTypeE.PREPAYMENT_APPLICATION.getDocumentTypeCode();
      } else if (invoice.getEfinInvoicetypeTxt().equals("PPI")) {
        DocType = DocumentTypeE.PREPAYMENT.getDocumentTypeCode();
      } else {
        DocType = DocumentTypeE.AP_INVOICE.getDocumentTypeCode();
      }

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approve the record without refreshing the page
      if (invoice.getEutForward() != null) {
        allowApprove = forwardDao.allowApproveReject(invoice.getEutForward(), userId, roleId,
            DocType);
      }
      if (invoice.getEutReqmoreinfo() != null
          || ((invoice.getEutForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // -----------------------
      if (!invoice.getDocumentStatus().equals("DR")) {
        if (invoice.getEutNextRole() != null) {
          java.util.List<EutNextRoleLine> li = invoice.getEutNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (invoice.getEutNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId, DocType);
        }
        if (!allowUpdate && !allowDelegation) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AlreadyPreocessed"));
        }
      }
      // -------------------------------------

      if (invoice.getDocumentStatus().equals("DR")) {
        isReserveRoleCrossed = UtilityDAO.chkReserveIsDoneorNot(clientId, orgId, roleId, userId,
            DocType, invoice.getGrandTotalAmount());
        if (isReserveRoleCrossed) {
          throw new OBException(OBMessageUtils.messageBD("Efin_CannotSubmit_ResRoleCrossed"));
        }
      }

      /* check negative amount */
      negamtqry = " SELECT sum(l.linenetamt) AS amount,b.name as bpartner FROM c_invoice inv JOIN c_invoiceline l ON inv.c_invoice_id=l.c_invoice_id "
          + "LEFT JOIN c_bpartner b on b.c_bpartner_id=l.c_bpartner_id"
          + " WHERE l.c_invoice_id = '" + invoiceId + "' GROUP BY l.c_bpartner_id,b.name";
      negamtps = conn.getPreparedStatement(negamtqry);
      negamtrs = negamtps.executeQuery();

      String Bpartner = "";
      String errmsg = "";
      while (negamtrs.next()) {
        if (negamtrs.getBigDecimal("amount").compareTo(BigDecimal.ZERO) < 0) {
          errmsg = "@Efin_grossnegativeamt@" + Bpartner;
          String strBPartner = negamtrs.getString("bpartner") == null ? ""
              : negamtrs.getString("bpartner");

          if (!(strBPartner.equals(""))) {
            if (Bpartner == "" || Bpartner == null) {
              Bpartner = negamtrs.getString("bpartner");
            } else {
              Bpartner += "," + negamtrs.getString("bpartner");

            }
          }
          errmsg = "@Efin_grossnegativeamt@" + Bpartner;

        }
      }
      if (!errmsg.equals("")) {
        throw new OBException(errmsg);
      }

      // Restricting the invoice line having duplicate unicode-Payment Beneficiary-secondary
      // beneficiary and amount combination
      final OBQuery<InvoiceLine> invoLines = OBDal.getInstance().createQuery(InvoiceLine.class,
          "c_invoice_id= '" + invoiceId + "'");
      Boolean isRDV = invoice.isEfinIsrdv();
      StringBuilder ermsgg = new StringBuilder();

      for (InvoiceLine Lines : invoice.getInvoiceLineList()) {
        if ((!PO_DOCUMENT.equals(strInvoiceType)) && (!isRDV) && (!Lines.isEFINIsTaxLine())) {
          List<InvoiceLine> iline = invoice.getInvoiceLineList().stream()
              .filter(a -> a.getEfinCValidcombination() == Lines.getEfinCValidcombination()
                  && a.getBusinessPartner() == Lines.getBusinessPartner()
                  && a.getEfinSecondaryBeneficiary() == Lines.getEfinSecondaryBeneficiary()
                  && !(a.getId().equals(Lines.getId())) && !(a.isEFINIsTaxLine()))
              .collect(Collectors.toList());

          if ((iline.size() > 0) && (Lines.getLineNetAmount().compareTo(new BigDecimal("0")) > 0)) {
            for (InvoiceLine filteredList : iline) {

              if (filteredList.getLineNetAmount().compareTo(new BigDecimal("0")) > 0) {
                ermsgg.append(filteredList.getLineNo());
                ermsgg.append(",");

              }
            }
          }
          if ((iline.size() > 0) && (Lines.getLineNetAmount().compareTo(new BigDecimal("0")) < 0)) {
            for (InvoiceLine filteredList : iline) {

              if (filteredList.getLineNetAmount().compareTo(new BigDecimal("0")) < 0) {
                ermsgg.append(filteredList.getLineNo());
                ermsgg.append(",");

              }
            }
          }
          if (ermsgg.length() > 0) {
            ermsgg.deleteCharAt(ermsgg.length() - 1);
            throw new OBException("Unicode Duplication in Line: " + ermsgg);
          }

        }
      }

      // Restrict to submit a purchase invoice from a role, which is not present in the document
      // rule
      if (API_DOCUMENT.equals(strInvoiceType) || RDV_DOCUMENT.equals(strInvoiceType)
          || PO_DOCUMENT.equals(strInvoiceType)) {
        doctype = Resource.AP_INVOICE_RULE;
        encumtype = "INV";
      } else if (PPI_DOCUMENT.equals(strInvoiceType))
        doctype = Resource.AP_Prepayment_Inv_RULE;
      else if (PPA_DOCUMENT.equals(strInvoiceType)) {
        doctype = Resource.AP_Prepayment_App_RULE;
        encumtype = "PPA";
      }

      DelegatedNextRoleDAO delegationDao = new DelegatedNextRoleDAOImpl();
      hasDelegation = delegationDao.checkDelegation(currentDate, roleId, doctype);

      if (hasDelegation) {
        delegatedFromRole = delegationDao.getDelegatedFromRole(roleId, doctype, userId);
      }

      if (invoice.getDocumentStatus().equals("DR")) {

        Connection con = OBDal.getInstance().getConnection();
        Boolean chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(con, clientId, submittedRoleOrgId,
            userId, roleId, doctype, invoice.getGrandTotalAmount());
        if (!chkRoleIsInDocRul) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Role_NotFundsReserve_submit"));
        }
      }
      // Restrict to submit a purchase invoice from a role which is added after the reservation role
      // in the document rule
      /*
       * if (invoice.getDocumentStatus().equals("DR")) { Boolean checkUserIsDeptHead =
       * UtilityDAO.chkUserIsDeptHeadInvoice(clientId, orgId, userId, invoice.getId()); if
       * (checkUserIsDeptHead) { DeptHeadRoleId = UtilityDAO.getDeptHeadRole(clientId, orgId,
       * doctype, invoice.getGrandTotalAmount()); roleId = DeptHeadRoleId; } SQLQuery role =
       * OBDal.getInstance().getSession().createSQLQuery(
       * "(select count(allowreservation) from eut_documentrule_lines where allowreservation='Y' and rolesequenceno < (select rolesequenceno  from eut_documentrule_lines ln "
       * +
       * " join eut_documentrule_header he on ln.eut_documentrule_header_id=he.eut_documentrule_header_id where ad_role_id='"
       * + roleId + "' " +
       * " and he.document_type = (select case when doc.EM_Efin_Isprepayinv='Y' then 'EUT_110' when doc.EM_Efin_Isprepayinvapp='Y' then 'EUT_109' else 'EUT_101' end "
       * +
       * "  from c_invoice inv     join c_doctype doc on doc.c_doctype_id=inv.c_doctypetarget_id     where inv.c_invoice_id='"
       * + invoiceId + "')" +
       * " and he.ad_org_id=(select ad_org_id from c_invoice where c_invoice_id='" + invoiceId +
       * "' )" + "and ln.allowreservation='N' and he.rulevalue <= 1000" +
       * " order by he.rulevalue desc limit 1))"); log.debug("qry>" + role.toString()); if
       * (role.list().size() > 0) { BigInteger allowres = (BigInteger) role.list().get(0); if
       * (allowres.compareTo(BigInteger.ZERO) > 0) throw new
       * OBException(OBMessageUtils.messageBD("Efin_Role_NotFundsReserve_submit")); } }
       */

      // check total invoice line linenetamt with splited line is greater than PO line linenetamt-
      // TaskNo - 7493
      if (invoice.getDocumentStatus().equals("DR")) {
        isError = dao.checkAmountValidationForSplit(clientId, invoice);
        if (isError) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Amount_Greater"));
        }
      }
      // check if any line with amount zero - TaskNo - 7493
      if (invoice.getDocumentStatus().equals("DR")) {
        isError = dao.checkLineHavingZeroAmt(invoice);
        if (isError) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Line_Amount_Zero"));
        }
      }

      /* If action is Complete then do the following Process */
      if (invoice.getDocumentAction().equals("CO") && !invoice.isSalesTransaction()) {

        // GET PARENT ORG CURRENCY
        currency = FinanceUtils.getCurrency(orgId, invoice);

        // get conversion rate
        conversionrate = FinanceUtils.getConversionRate(conn.getConnection(), orgId, invoice,
            currency);

        // check ap prepayment invoice validations
        // #Taskno :3526
        if (PPI_DOCUMENT.equals(strInvoiceType) && invoice.getDocumentStatus().equals("DR")) {
          // check sum of invoice Lines Amount greater than encumbrance remain amount
          if (invoice.getEfinManualencumbrance() != null) {
            if (invoice.getGrandTotalAmount()
                .compareTo(invoice.getEfinManualencumbrance().getRemainingAmount()) == 1) {
              throw new OBException(OBMessageUtils.messageBD("Efin_PrepayInvoiceAmount_High"));
            }
          }
        }

        // checking the unique code present in manual lines
        if (invoice.getEfinEncumtype().equals("M") && !(PPI_DOCUMENT.equals(strInvoiceType))) {
          String manualId = invoice.getEfinManualencumbrance().getId();
          invlineqry = "SELECT  c_invoiceline_id FROM c_invoiceline WHERE c_invoice_id  ='"
              + invoiceId + "'";
          invlineps = conn.getPreparedStatement(invlineqry);
          invliners = invlineps.executeQuery();
          String uniqueCode = "";
          while (invliners.next()) {
            String invoicelineid = invliners.getString("c_invoiceline_id");
            InvoiceLine invoiceline = OBDal.getInstance().get(InvoiceLine.class, invoicelineid);
            if (invoiceline.getEfinCElementvalue().getAccountType().equals("E")) {
              OBQuery<EfinBudgetManencumlines> manualline = OBDal.getInstance()
                  .createQuery(EfinBudgetManencumlines.class,
                      " accountingCombination.id ='"
                          + invoiceline.getEfinCValidcombination().getId()
                          + "' and efin_budget_manencum_id = '" + manualId + "'");
              log.debug("sizeif" + manualline.list().size());
              if (manualline.list() == null || manualline.list().size() == 0) {
                uniqueCode += "," + invoiceline.getEFINUniqueCode();
              }
            }
          }
          if (StringUtils.isNotEmpty(uniqueCode)) {
            throw new OBException(OBMessageUtils.messageBD("Efin_LineNotExistsIn_EncumLine")
                .replace("@", uniqueCode.replaceFirst(",", "")));
          }
        }

        /*
         * validate if invoice can have lines across departments.
         */
        if (!invoice.isSalesTransaction()) {
          String invlinesqry = "SELECT em_efin_c_salesregion_id,em_efin_c_elementvalue_id FROM c_invoiceline l"
              + " JOIN c_elementvalue ev ON l.em_efin_c_elementvalue_id = ev.c_elementvalue_id"
              + " WHERE c_invoice_id =  '" + invoiceId + "' AND accounttype ='E'";
          invlinesps = conn.getPreparedStatement(invlinesqry);
          invlinesrs = invlinesps.executeQuery();
          // log.debug("invlinesqry" + invlinesqry);
          while (invlinesrs.next()) {
            String deptvalqry = "SELECT d.c_salesregion_id FROM c_salesregion d WHERE ( SELECT (CASE WHEN org.em_efin_allowmultideptinap = 'N' "
                + "THEN  c_salesregion_id = inv.em_efin_c_salesregion_id ELSE 1=1 END ) FROM c_invoice inv "
                + "LEFT JOIN ad_org org ON org.ad_org_id=inv.ad_org_id  WHERE c_invoice_id ='"
                + invoiceId + "' AND" + " d.c_salesregion_id = '"
                + invlinesrs.getString("em_efin_c_salesregion_id") + "') ";
            deptvalps = conn.getPreparedStatement(deptvalqry);
            deptvalrs = deptvalps.executeQuery();

            // log.debug("deptvalqry"+deptvalqry);
            while (!deptvalrs.next()) {
              throw new OBException("@Efin_InvDeptNotMixOrg@");
            }
          }
        }

        Boolean isCrossBudgetType = PurchaseInvoiceSubmitUtils.isCrossBudgetType(invoice);
        if (isCrossBudgetType) {
          throw new OBException("@Efin_CrossBudgets@");
        }
        /* check validation before process the purchase invoice(same like C_Invoice_Post) */
        if (invoice.getBusinessPartner().isVendorBlocking()
            && invoice.getBusinessPartner().isPurchaseInvoice()) {
          throw new OBException("@ThebusinessPartner@" + " "
              + invoice.getBusinessPartner().getName() + " " + "@BusinessPartnerBlocked@");
        }

        /* if Quantities does not match Throw the error */
        sql = " SELECT C_INVOICELINE.C_InvoiceLine_ID, C_INVOICELINE.LinenetAmt  FROM C_INVOICELINE    WHERE C_Invoice_ID ='"
            + invoice.getId() + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps1:" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          sql1 = " SELECT SUM(Amt) as acctAmount   FROM C_INVOICELINE_ACCTDIMENSION      WHERE C_InvoiceLine_ID ='"
              + rs.getString("C_InvoiceLine_ID") + "'";
          ps1 = conn.getPreparedStatement(sql1);
          log.debug("ps2:" + ps1.toString());
          rs1 = ps1.executeQuery();
          if (rs1.next()) {
            if (rs1.getString("acctAmount") != null) {
              if (!rs1.getString("acctAmount").equals(rs.getString("LinenetAmt"))) {
                errorMessage = OBMessageUtils.messageBD("QuantitiesNotMatch");
                throw new OBException("@QuantitiesNotMatch@");
              }
            }
          }
        }
        /* If no invoice lines throw the error */
        sql = "  SELECT COUNT(*) as count FROM C_INVOICE  WHERE C_INVOICE_ID='" + invoice.getId()
            + "' AND (EXISTS (SELECT 1  FROM C_INVOICELINE  WHERE C_INVOICE_ID='" + invoice.getId()
            + "')" + "         OR EXISTS (SELECT 1  FROM C_INVOICETAX  WHERE C_INVOICE_ID='"
            + invoice.getId() + "'))";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps3:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") == 0) {
            throw new OBException("@InvoicesNeedLines@");
          }
        }

        /* CannotUseGenericProduct */
        sql = "  SELECT count(*) as count  FROM dual    WHERE EXISTS ( SELECT 1  FROM c_invoiceline il JOIN m_product p ON il.m_product_id = p.m_product_id "
            + "        WHERE p.isgeneric = 'Y' AND il.c_invoice_id='" + invoice.getId() + "')";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps4:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") > 0) {
            sql1 = "   SELECT max(p.name) as productname  FROM c_invoiceline il JOIN m_product p ON il.m_product_id = p.m_product_id "
                + "     WHERE p.isgeneric = 'Y'         AND il.c_invoice_id ='" + invoice.getId()
                + "'";
            ps1 = conn.getPreparedStatement(sql1);
            log.debug("ps5:" + ps1.toString());
            rs1 = ps1.executeQuery();
            if (rs1.next()) {
              throw new OBException("@CannotUseGenericProduct@" + rs1.getString("productname"));
            }
          }
        }

        /* Check the cash vat flag for all the taxes matches the invoice one */
        sql = "   select count(1) as count    from c_invoicetax it inner join c_tax t on (it.c_tax_id = t.c_tax_id) "
            + " where it.c_invoice_id='" + invoice.getId()
            + "'   and t.iswithholdingtax = 'N'    and t.rate <> 0     and t.IsCashVat <> '"
            + invoice.isCashVAT() + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps6:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") > 0) {
            throw new OBException("@CashVATNotMatch@");
          }
        }

        /* Check active business partner */
        if (!invoice.getBusinessPartner().isActive()) {
          throw new OBException("@InActiveBusinessPartner@");
        }

        /*
         * Avoids repeating the same documentno for the same organization tree within the same
         * fiscal year
         */
        sql = "SELECT COUNT(*) as count     FROM (SELECT Y.C_CALENDAR_ID, Y.C_YEAR_ID,    MIN(P.STARTDATE) AS PERIODSTARTDATE, MAX(P.ENDDATE) AS PERIODENDDATE"
            + " FROM C_YEAR Y, C_PERIOD P   WHERE Y.C_YEAR_ID = P.C_YEAR_ID  AND Y.ISACTIVE = 'Y'   AND P.ISACTIVE = 'Y' "
            + "   AND Y.C_CALENDAR_ID = (SELECT O.C_CALENDAR_ID    FROM AD_ORG O   WHERE AD_ORG_ID = AD_ORG_GETCALENDAROWNER('"
            + invoice.getOrganization().getId() + "')) "
            + "          GROUP BY Y.C_CALENDAR_ID, Y.C_YEAR_ID) A    WHERE PERIODSTARTDATE <='"
            + invoice.getInvoiceDate() + "'AND PERIODENDDATE+1 > '" + invoice.getInvoiceDate()
            + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps7:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") != 0) {
            sql1 = " SELECT PERIODSTARTDATE, PERIODENDDATE+1 as PERIODENDDATE  FROM (SELECT Y.C_CALENDAR_ID, Y.C_YEAR_ID, "
                + "  MIN(P.STARTDATE) AS PERIODSTARTDATE, MAX(P.ENDDATE) AS PERIODENDDATE   FROM C_YEAR Y, C_PERIOD P "
                + "  WHERE Y.C_YEAR_ID = P.C_YEAR_ID   AND Y.ISACTIVE = 'Y' AND P.ISACTIVE = 'Y' "
                + "     AND Y.C_CALENDAR_ID = (SELECT O.C_CALENDAR_ID  FROM AD_ORG O "
                + "WHERE AD_ORG_ID = AD_ORG_GETCALENDAROWNER('" + invoice.getOrganization().getId()
                + "')) GROUP BY Y.C_CALENDAR_ID, Y.C_YEAR_ID) A "
                + "      WHERE PERIODSTARTDATE <= '" + invoice.getInvoiceDate()
                + "' AND PERIODENDDATE+1 > '" + invoice.getInvoiceDate() + "' ";
            ps1 = conn.getPreparedStatement(sql1);
            log.debug("ps8:" + ps1.toString());
            rs1 = ps1.executeQuery();
            if (rs1.next()) {
              if (rs1.getString("PERIODSTARTDATE") != null
                  && rs1.getString("PERIODENDDATE") != null) {
                sql2 = "  SELECT COUNT(*) as count   FROM C_INVOICE I "
                    + "    WHERE I.DOCUMENTNO ='" + invoice.getDocumentNo()
                    + "'   AND I.C_DOCTYPETARGET_ID ='" + invoice.getTransactionDocument().getId()
                    + "'" + "        AND I.DATEINVOICED >'" + rs1.getTimestamp("PERIODSTARTDATE")
                    + "'        AND I.DATEINVOICED < '" + rs1.getTimestamp("PERIODENDDATE")
                    + "' AND I.C_INVOICE_ID <> '" + invoice.getId() + "'"
                    + "         AND AD_ISORGINCLUDED(I.AD_ORG_ID,'"
                    + invoice.getTransactionDocument().getOrganization().getId()
                    + "', I.AD_CLIENT_ID) <> -1        AND I.AD_CLIENT_ID ='"
                    + invoice.getClient().getId() + "'";
                ps2 = conn.getPreparedStatement(sql2);
                log.debug("ps9:" + ps2.toString());
                rs2 = ps2.executeQuery();
                if (rs2.next()) {
                  if (rs2.getInt("count") > 0) {
                    throw new OBException("@DifferentDocumentNo@");
                  }
                }
              }
            }
          }
        }
        /* Check that quantities are negative for return invoices */
        if (invoice.getTransactionDocument().isReturn()) {
          sql = " SELECT count(*) as count   FROM c_invoiceline  WHERE c_invoice_discount_id IS NULL  AND qtyinvoiced > 0 "
              + "   AND c_invoice_id ='" + invoice.getId()
              + "'  AND NOT EXISTS (SELECT 1 FROM c_invoiceline L "
              + "LEFT JOIN M_PRODUCT P ON L.M_PRODUCT_ID = P.M_PRODUCT_ID       JOIN C_DISCOUNT CD ON CD.M_PRODUCT_ID=P.M_PRODUCT_ID "
              + "       WHERE P.M_PRODUCT_ID=C_INVOICELINE.M_PRODUCT_ID)";
          ps = conn.getPreparedStatement(sql);
          log.debug("ps10:" + ps.toString());
          rs = ps.executeQuery();
          if (rs.next()) {
            if (rs.getInt("count") != 0) {
              throw new OBException("@ReturnInvoiceNegativeQty@");
            }
          }
        }
        /* If invoice is process with others throw the error */
        if (invoice.isProcessNow())
          throw new OBException("@OtherProcessActive@");

        /* If Organization does not match with header and lines */
        sql = " SELECT COUNT(*) as count     FROM C_INVOICE I, C_INVOICELINE IL     WHERE I.C_INVOICE_ID = IL.C_INVOICE_ID "
            + "        AND AD_ISORGINCLUDED(IL.AD_Org_ID, I.AD_Org_ID, I.AD_Client_ID) = -1 "
            + "        AND I.C_INVOICE_ID ='" + invoice.getId() + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps11:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") > 0) {
            throw new OBException("@NotCorrectOrgLines@");
          }
        }
        /* If Organization does not match with header and document type */
        sql = " SELECT COUNT(*) as count  FROM C_INVOICE C, C_DOCTYPE  WHERE C_DOCTYPE.DocBaseType IN ( select docbasetype from c_doctype "
            + " where ad_table_id='318' and isactive='Y' and ad_client_id=C.AD_Client_ID) AND C_DOCTYPE.IsSOTrx=C.ISSOTRX "
            + "  AND Ad_Isorgincluded(C.AD_Org_ID,C_DOCTYPE.AD_Org_ID, C.AD_Client_ID) <> -1 "
            + "  AND C.C_DOCTYPETARGET_ID = C_DOCTYPE.C_DOCTYPE_ID   AND C.C_INVOICE_ID ='"
            + invoice.getId() + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps12:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") == 0) {
            throw new OBException("@NotCorrectOrgDoctypeInvoice@");
          }
        }
        /* OrgHeaderNotReady */
        sql = " SELECT AD_Org.IsReady as ready, Ad_OrgType.IsTransactionsAllowed as allow  FROM C_INVOICE, AD_Org, AD_OrgType "
            + "    WHERE AD_Org.AD_Org_ID=C_INVOICE.AD_Org_ID    AND AD_Org.AD_OrgType_ID=AD_OrgType.AD_OrgType_ID  AND C_INVOICE.C_INVOICE_ID='"
            + invoice.getId() + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps13:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getString("ready").equals("N")) {
            throw new OBException("@OrgHeaderNotReady@");
          }
          if (rs.getString("allow").equals("N")) {
            throw new OBException("@OrgHeaderNotTransAllowed@");
          }
        }

        /* LinesAndHeaderDifferentLEorBU */
        sql = " SELECT AD_ORG_CHK_DOCUMENTS('C_INVOICE', 'C_INVOICELINE','" + invoice.getId()
            + "', 'C_INVOICE_ID', 'C_INVOICE_ID') as included FROM dual";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps14:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("included") == -1) {
            throw new OBException("@LinesAndHeaderDifferentLEorBU@");
          }
        }
        // -- Check the period control is opened (only if it is legal entity with accounting)
        // -- Gets the BU or LE of the document
        sql = " SELECT AD_GET_DOC_LE_BU('C_INVOICE', '" + invoice.getId()
            + "', 'C_INVOICE_ID', 'LE') as org_bule_id FROM DUAL";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps15:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          sql1 = "  SELECT AD_OrgType.IsAcctLegalEntity AS isacctle  FROM AD_OrgType, AD_Org   WHERE AD_Org.AD_OrgType_ID = AD_OrgType.AD_OrgType_ID"
              + "        AND AD_Org.AD_Org_ID='" + rs.getString("org_bule_id") + "'";
          ps1 = conn.getPreparedStatement(sql1);
          log.debug("ps16:" + ps1.toString());
          rs1 = ps1.executeQuery();
          if (rs1.next()) {
            if (rs1.getString("isacctle").equals("Y")) {
              sql2 = " SELECT C_CHK_OPEN_PERIOD('" + invoice.getOrganization().getId() + "','"
                  + invoice.getAccountingDate() + "', NULL, '"
                  + invoice.getTransactionDocument().getId() + "') as available_period FROM DUAL";
              ps2 = conn.getPreparedStatement(sql2);
              log.debug("ps17:" + ps2.toString());
              rs2 = ps2.executeQuery();
              if (rs2.next()) {
                if (rs2.getInt("available_period") != 1) {
                  if (invoice.getDocumentStatus().equals("DR")) {
                    throw new OBException("@PeriodNotAvailable@");
                  } else {
                    // check the status of record ,status is other than draft check current date and
                    // accounting date year is
                    // same then check period is open for the current date then allow by updating
                    // the accounting date as current date,then do not
                    // allow to submit

                    if (UtilityDAO.getYearId(currentDate, clientId)
                        .equals(UtilityDAO.getYearId(invoice.getAccountingDate(), clientId))) {
                      sql3 = " SELECT C_CHK_OPEN_PERIOD('" + invoice.getOrganization().getId()
                          + "','" + currentDate + "', NULL, '"
                          + invoice.getTransactionDocument().getId()
                          + "') as available_period FROM DUAL";
                      ps3 = conn.getPreparedStatement(sql3);
                      rs3 = ps3.executeQuery();
                      if (rs3.next()) {
                        if (rs3.getInt("available_period") != 1) {
                          throw new OBException("@PeriodNotAvailable@");
                        } else {

                          invoice.setAccountingDate(todaydate);
                        }
                      } else {
                        invoice.setAccountingDate(todaydate);
                      }
                    } else {
                      throw new OBException("@PeriodNotAvailable@");
                    }
                  }
                }

              }
            }
          }
        }

        OBDal.getInstance().save(invoice);
        OBDal.getInstance().refresh(invoice);

        /* NotCorrectOrgBpartnerInvoice */
        sql = "   SELECT COUNT(*) as count    FROM C_INVOICE c, C_BPARTNER bp   WHERE c.C_BPARTNER_ID=bp.C_BPARTNER_ID  AND Ad_Isorgincluded(c.AD_ORG_ID, bp.AD_ORG_ID, bp.AD_CLIENT_ID)=-1 "
            + "  AND c.C_Invoice_ID='" + invoice.getId() + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps18:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") > 0)
            throw new OBException("@NotCorrectOrgBpartnerInvoice@");
        }

        // NotPossibleCompleteInvoice
        if (!PO_DOCUMENT.equals(strInvoiceType)) {
          sql = "SELECT c_orderline_id ,line FROM C_INVOICELINE  WHERE C_INVOICE_ID='"
              + invoice.getId() + "'  ORDER BY line ";
          ps = conn.getPreparedStatement(sql);
          log.debug("ps19:" + ps.toString());
          rs = ps.executeQuery();
          while (rs.next()) {
            if (rs.getString("c_orderline_id") != null) {
              sql1 = " SELECT o.documentno, (ABS(coalesce(ol.qtyordered,0)) - ABS(coalesce(ol.qtyinvoiced,0))) as finalvalue, coalesce(p.isquantityvariable,'N') as isquantityvariable"
                  + "            FROM c_order o, c_orderline ol            LEFT JOIN m_product p ON p.m_product_id = ol.m_product_id"
                  + "            WHERE o.c_order_id = ol.c_order_id    AND ol.c_orderline_id ='"
                  + rs.getString("c_orderline_id") + "'";
              ps1 = conn.getPreparedStatement(sql1);
              rs1 = ps1.executeQuery();
              if (rs1.next()) {
                if (rs1.getString("isquantityvariable").equals("Y") && rs1.getInt("finalvalue") < 0)
                  throw new OBException(
                      "@NotPossibleCompleteInvoice@" + " " + invoice.getDocumentNo() + " "
                          + "@line@" + rs.getString("line") + "@OrderDocumentno@"
                          + rs1.getString("documentno") + "@QtyInvoicedHigherOrdered@");
              }
            }
          }
        }

        /* check already reserve is done for particular invoice */
        OBQuery<efinbudgetencum> encumlist = OBDal.getInstance().createQuery(efinbudgetencum.class,
            " invoice.id='" + invoice.getId() + "'");
        if (encumlist.list().size() == 0) {
          // get invoice line
          sql = " select em_efin_c_elementvalue_id,linenetamt,em_efin_uniquecode,em_efin_budgetlines_id,c_invoiceline_id,em_efin_budgmanuencumln_id from c_invoice inv  left join c_invoiceline ln on ln.c_invoice_id= inv.c_invoice_id where inv.c_invoice_id ='"
              + invoice.getId() + "' ";
          ps = conn.getPreparedStatement(sql);
          log.debug("ps20:" + ps.toString());
          rs = ps.executeQuery();
          while (rs.next()) {
            ElementValue acct = OBDal.getInstance().get(ElementValue.class,
                rs.getString("em_efin_c_elementvalue_id"));
            InvoiceLine line = OBDal.getInstance().get(InvoiceLine.class,
                rs.getString("c_invoiceline_id"));
            /* check if line net amount greater than manual Encumbrance remaining amount */
            log.debug("getAccountType:" + acct.getAccountType());
            log.debug("getEfinEncumtype:" + invoice.getEfinEncumtype());

            if (acct.getAccountType().equals("E") && !invoice.isEfinIsreserved()
                && "m".equals(invoice.getEfinEncumtype())) {
              if ("E".equals(line.getEfinCValidcombination().getEfinDimensiontype())) {
                JSONObject fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                    invoice.getEfinBudgetint(), line.getEfinCValidcombination(),
                    line.getLineNetAmount());
                uniquecode = line.getEfinCValidcombination().getEfinUniqueCode();
                if (fundsCheckingObject.has("errorFlag")) {
                  if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                    checkException = Boolean.TRUE;
                    status = fundsCheckingObject.getString("message");
                  }
                }
              }
            }
          }
          /*
           * If line net amount greater than Manual Encumbrance remaining amount or budget lines
           * funds available throw the error with uniquecode
           */
          if (checkException) {
            throw new OBException(status);
          }
          // check ap prepayment application invoice validations
          // #Taskno :3526
          if (PPA_DOCUMENT.equals(strInvoiceType) && invoice.getDocumentStatus().equals("DR")) {

            // #Taskno:0003656
            // check sum of invoice Lines Amount greater than encumbrance remain amount
            /*
             * boolean lessencumAmount =
             * UtilityDAO.checkManEncumbranceAmount(invoice.getEfinManualencumbrance().getId(),
             * invoiceId); if(lessencumAmount) { throw new
             * OBException("@Efin_Less_Encum_RemainAmt@"); }
             */

            // check sum of applied amount > applied prepayment invoice amount
            String appliedPrepayment = UtilityDAO.checkAppliedPrepayment(invoiceId, conversionrate);
            if (!appliedPrepayment.equals("")) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Applied_PrepaymentLess")
                  .replace("@invoice@", appliedPrepayment));
            }
            // check sum of applied amount =application invoice total amount
            String applicationInvAmtcheck = UtilityDAO.checkApplicationInvoiceAmount(invoiceId);
            if (!applicationInvAmtcheck.equals("1")) {
              throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_Equal"));
            }

            sql = " select string_agg(prepay.documentno,',') as preinvoice "
                + " from efin_applied_prepayment app join (select app1.efin_applied_invoice from c_invoice inv "
                + " join efin_applied_prepayment app1 on app1.efin_applied_invoice=inv.c_invoice_id "
                + " where app1.c_invoice_id='" + invoiceId
                + "') as preinv on preinv.efin_applied_invoice=app.efin_applied_invoice "
                + " join c_invoice prepay on app.efin_applied_invoice=prepay.c_invoice_id "
                + "join c_invoice apinv on apinv.c_invoice_id=app.c_invoice_id "
                + "where apinv.docstatus='DR'"
                + " group by app.efin_applied_invoice,prepay.c_invoice_id "
                + " having sum(app.applied_amount) > prepay.em_efin_pre_remainingamount";
            ps = conn.getPreparedStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Applied_PrepaymentLess")
                  .replace("@invoice@", rs.getString("preinvoice")));
            }
          }

          // Validate funds available and remaining amount in case of ap prepayment invoice

          if (invoice.getEfinDistribution() != null && PPI_DOCUMENT.equals(strInvoiceType)) {
            grandTotal = FinanceUtils.getConvertedAmount(invoice.getGrandTotalAmount(),
                conversionrate);
            if (!invoice.isEfinIsreserved()) {
              if (invoice.getEfinEncumtype().equals("M")
                  && invoice.getEfinManualencumbrance() != null) {
                if (PurchaseInvoiceSubmitUtils.checkRemainingAmount(invoice, grandTotal) == -1) {
                  OBError result = OBErrorBuilder.buildMessage(null, "Error",
                      "@Efin_Remamt_lesser@");
                  bundle.setResult(result);
                  return;
                }
              } else {
                Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();
                EfinBudgetInquiry budgetinq = null;
                List<InvoiceLine> invoiceLineList = invoice.getInvoiceLineList();
                for (InvoiceLine invLine : invoiceLineList) {
                  budgetinq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                      invLine.getEfinExpenseAccount(), invoice.getEfinBudgetint());
                  if (budgetinq == null) {
                    OBError result = OBErrorBuilder.buildMessage(null, "Error",
                        "@Efin_Reservation_Nofunds@");
                    bundle.setResult(result);
                    return;
                  } else {
                    BigDecimal totalLineAmt = invoiceLineList.stream()
                        .filter(a -> a.getEfinExpenseAccount() == invLine.getEfinExpenseAccount())
                        .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                    if (budgetinq.getFundsAvailable().compareTo(totalLineAmt) < 0) {
                      OBError result = OBErrorBuilder.buildMessage(null, "Error",
                          "@Efin_Reservation_Nofunds@");
                      bundle.setResult(result);
                      return;
                    }

                  }
                }
              }
            }
          }

          /* If invoice gross amount greater than Manual Encumbrance remaining amount */
          if (invoice.getEfinEncumtype().equals("M")) {
            // check manual Encumbrance Amount
            // check only expense act lines sum amount

            linesumqry = "select sum(linenetamt) as amt from c_invoiceline  join c_elementvalue on  em_efin_c_elementvalue_id  = c_elementvalue_id "
                + "where accounttype='E' and c_invoice_id='" + invoice.getId() + "'";
            linesumps = conn.getPreparedStatement(linesumqry);
            linesumrs = linesumps.executeQuery();
            while (linesumrs.next()) {
              BigDecimal sumval = new BigDecimal(linesumrs.getInt("amt"));
              log.debug("sumval:" + sumval);
              sumval = FinanceUtils.getConvertedAmount(sumval, conversionrate);
              sql = "   select amount as manEncumamount from efin_budget_manencum where efin_budget_manencum_id = '"
                  + invoice.getEfinManualencumbrance().getId() + "'";
              ps = conn.getPreparedStatement(sql);
              rs = ps.executeQuery();

              if (rs.next()) {
                log.debug("manEncumamount:" + rs.getBigDecimal("manEncumamount"));
                if (rs.getBigDecimal("manEncumamount").compareTo(sumval) < 0)
                  throw new OBException("@Efin_PI_GrsAmtExMEAmt@");
              }
            }
          }
        }

        OBQuery<InvoiceLine> invlines = OBDal.getInstance().createQuery(InvoiceLine.class,
            " invoice.id='" + invoice.getId() + "'");

        // If lines other than selected advance type
        /*
         * if (PPI_DOCUMENT.equals(strInvoiceType)) { if (invoice.getEfinEncumtype().equals("M")) {
         * if (invlines.list().size() > 0) { for (InvoiceLine lines : invlines.list()) { if
         * (lines.getEfinDistributionLines() == null) { throw new
         * OBException("@Efin_PurInv_APP_ManLine@"); } } } } }
         */

        if (!invoice.isEfinIsreserved() && API_DOCUMENT.equals(strInvoiceType)) {
          // check used amount is negative or not
          if (invoice.getEfinEncumtype().equals("M")) {
            if (invlines.list().size() > 0) {
              for (InvoiceLine lines : invlines.list()) {
                if (lines.getEfinBudgmanuencumln() != null) {
                  EfinBudgetManencumlines encumlines = OBDal.getInstance()
                      .get(EfinBudgetManencumlines.class, lines.getEfinBudgmanuencumln().getId());
                  lineNetAmt = lines.getLineNetAmount();
                  sumLineNetAmt = invlines.list().stream()
                      .filter(a -> a.getEfinCValidcombination().getId()
                          .equals(lines.getEfinCValidcombination().getId()))
                      .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                  if (!invoice.getCurrency().getId().equals(currency.getId())) {
                    lineNetAmt = conversionrate.multiply(lineNetAmt);
                    sumLineNetAmt = conversionrate.multiply(sumLineNetAmt);
                  }
                  if (lines.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    if ((encumlines.getUsedAmount().add(sumLineNetAmt))
                        .compareTo(BigDecimal.ZERO) < 0) {
                      throw new OBException("@Efin_Manencum_UseamtNeg@");
                    }

                    // check encumbrance remaining amount should not be greater than encumbrance
                    // original amount

                    if ((encumlines.getRemainingAmount().subtract(sumLineNetAmt))
                        .compareTo(encumlines.getRevamount()) > 0) {
                      throw new OBException("@Efin_ManEnc_RemAmt@");
                    }

                    // check invoice linenetamt is greater than the encumbrance remaining amount, if
                    // yes throw error
                    if (encumlines.getRemainingAmount().compareTo(sumLineNetAmt) < 0) {
                      String erMsg = OBMessageUtils.messageBD("EFIN_PurInv_ManEncumRemAmt");
                      throw new OBException(erMsg.replace("%",
                          encumlines.getAccountingCombination().getEfinUniqueCode()));
                    }
                  }
                }

              }
            }
          }
        }
        if (!invoice.isEfinIsreserved() && PPA_DOCUMENT.equals(strInvoiceType)) {
          String strUniqueCode = PurchaseInvoiceSubmitUtils.isValidPPA(invoice, conversionrate);
          if (strUniqueCode.length() > 0) {
            throw new OBException("@Efin_appliedamount<lineamt@" + strUniqueCode);
          }
        }
        /*
         * if (PPA_DOCUMENT.equals(strInvoiceType)) { BigDecimal grandtotal = BigDecimal.ZERO; if
         * (invoice.getEfinManualencumbrance() != null) { EfinBudgetManencum manecnum =
         * OBDal.getInstance().get(EfinBudgetManencum.class,
         * invoice.getEfinManualencumbrance().getId()); grandtotal =
         * conversionrate.multiply(invoice.getGrandTotalAmount()); if
         * (grandtotal.compareTo(manecnum.getRemainingamt()) > 0) { throw new
         * OBException("@Efin_PI_GrsAmtExMEAmt@"); } } }
         */
        if (API_DOCUMENT.equals(strInvoiceType) && !invoice.isEfinIsreserved()) {
          // check budgetline encumbrance amount.
          if (invoice.getEfinEncumtype().equals("A")) {
            if (invlines.list().size() > 0) {
              if (!isAdjustementInvoice) {
                Boolean isValidInvoice = PurchaseInvoiceSubmitUtils.validateAutoInvoice(invoice,
                    conversionrate);
                if (!isValidInvoice) {
                  throw new OBException("@Efin_Amount>FA@");
                }
              }

              for (InvoiceLine lines : invlines.list()) {
                if (lines.getEfinBudgetlines() != null) {
                  if (lines.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    EFINBudgetLines budgline = OBDal.getInstance().get(EFINBudgetLines.class,
                        lines.getEfinBudgetlines().getId());
                    lineNetAmt = FinanceUtils.getConvertedAmount(lines.getLineNetAmount(),
                        conversionrate);
                    sumLineNetAmt = invlines.list().stream()
                        .filter(a -> a.getEfinCValidcombination().getId()
                            .equals(lines.getEfinCValidcombination().getId()))
                        .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                    sumLineNetAmt = FinanceUtils.getConvertedAmount(sumLineNetAmt, conversionrate);
                    if ((budgline.getEncumbrance().add(sumLineNetAmt))
                        .compareTo(BigDecimal.ZERO) < 0) {
                      throw new OBException("@Efin_BudgLine_EncumNeg@");
                    }
                  }
                }
              }
            }
          }
        }

        // currency chk
        if (invoice.getBusinessPartner().getCurrency() != null) {
          if (!invoice.getCurrency().getId()
              .equals(invoice.getBusinessPartner().getCurrency().getId())) {
            sql = " SELECT COUNT(C_Conversion_Rate_Document_id) as  v_Count    FROM C_Conversion_Rate_Document  WHERE C_Invoice_ID ='"
                + invoice.getId() + "'   AND C_Currency_ID ='" + invoice.getCurrency().getId()
                + "'  AND C_Currency_Id_To ='" + invoice.getBusinessPartner().getCurrency().getId()
                + "'";
            ps = conn.getPreparedStatement(sql);
            log.debug("stconvers:" + ps.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
              if (rs.getInt("v_Count") == 0) {
                sql = " SELECT count(C_Conversion_Rate_id) as v_count  FROM C_Conversion_Rate   WHERE C_Currency_ID ='"
                    + invoice.getCurrency().getId() + "'    AND C_Currency_ID_To ='"
                    + invoice.getBusinessPartner().getCurrency().getId()
                    + "'    AND ConversionRateType = 'S'    AND ('" + invoice.getInvoiceDate()
                    + "') BETWEEN ValidFrom AND ValidTo    " + "     AND AD_Client_ID IN ('0', '"
                    + invoice.getClient().getId() + "')      AND AD_Org_ID IN ('0', '"
                    + invoice.getOrganization().getId() + "')    AND IsActive = 'Y' ";
                ps = conn.getPreparedStatement(sql);
                log.debug("conver:" + ps.toString());
                log.debug("formatDate:" + Utility.formatDate(invoice.getInvoiceDate()));
                rs = ps.executeQuery();
                if (rs.next()) {
                  if (rs.getInt("v_Count") == 0) {
                    errorMsg = OBMessageUtils.messageBD("NoConversionRate") + " ("
                        + invoice.getCurrency().getISOCode() + "-"
                        + invoice.getCurrency().getSymbol() + " ) " + OBMessageUtils.messageBD("to")
                        + " ( " + invoice.getBusinessPartner().getCurrency().getISOCode() + "-"
                        + invoice.getBusinessPartner().getCurrency().getSymbol() + ") "
                        + OBMessageUtils.messageBD("ForDate") + " ' "
                        + UtilityDAO
                            .convertTohijriDate(timeYearFormat.format(invoice.getInvoiceDate()))
                        + "' " + OBMessageUtils.messageBD("Client") + " '"
                        + invoice.getClient().getName() + "' " + OBMessageUtils.messageBD("And")
                        + " " + OBMessageUtils.messageBD("ACCS_AD_ORG_ID_D") + " '"
                        + invoice.getOrganization().getName() + "'.";
                    log.debug("errorMsg:" + errorMsg);
                    throw new OBException(errorMsg);
                  }
                }
              }
            }
          }
        } else {
          throw new OBException("@Efin_PurInv_BP_CurNotDefin@");
        }

        if (!invoice.getCurrency().getId().equals(currency.getId())) {
          sql = " SELECT COUNT(C_Conversion_Rate_Document_id) as  v_Count    FROM C_Conversion_Rate_Document  WHERE C_Invoice_ID ='"
              + invoice.getId() + "'   AND C_Currency_ID ='" + invoice.getCurrency().getId()
              + "'  AND C_Currency_Id_To ='" + currency.getId() + "'";
          ps = conn.getPreparedStatement(sql);
          log.debug("stconvers:" + ps.toString());
          rs = ps.executeQuery();
          if (rs.next()) {
            if (rs.getInt("v_Count") == 0) {
              sql = " SELECT count(C_Conversion_Rate_id) as v_count  FROM C_Conversion_Rate   WHERE C_Currency_ID ='"
                  + invoice.getCurrency().getId() + "'    AND C_Currency_ID_To ='"
                  + currency.getId() + "'    AND ConversionRateType = 'S'    AND ('"
                  + invoice.getInvoiceDate() + "') BETWEEN ValidFrom AND ValidTo    "
                  + "     AND AD_Client_ID IN ('0', '" + invoice.getClient().getId()
                  + "')      AND AD_Org_ID IN ('0', '" + invoice.getOrganization().getId()
                  + "')    AND IsActive = 'Y' ";
              ps = conn.getPreparedStatement(sql);
              log.debug("conver:" + ps.toString());
              log.debug("formatDate:" + Utility.formatDate(invoice.getInvoiceDate()));
              rs = ps.executeQuery();
              if (rs.next()) {
                if (rs.getInt("v_Count") == 0) {
                  errorMsg = OBMessageUtils.messageBD("NoConversionRate") + " ("
                      + invoice.getCurrency().getISOCode() + "-" + invoice.getCurrency().getSymbol()
                      + " ) " + OBMessageUtils.messageBD("to") + " ( " + currency.getISOCode() + "-"
                      + currency.getSymbol() + ") " + OBMessageUtils.messageBD("ForDate") + " ' "
                      + UtilityDAO
                          .convertTohijriDate(timeYearFormat.format(invoice.getInvoiceDate()))
                      + "' " + OBMessageUtils.messageBD("Client") + " '"
                      + invoice.getClient().getName() + "' " + OBMessageUtils.messageBD("And") + " "
                      + OBMessageUtils.messageBD("ACCS_AD_ORG_ID_D") + " '"
                      + invoice.getOrganization().getName() + "'.";
                  log.debug("errorMsg:" + errorMsg);
                  throw new OBException(errorMsg);
                }
              }
            }
          }
        }

        if (PO_DOCUMENT.equals(strInvoiceType) && invoice.getDocumentStatus().equals("DR")) {

          // check new version created in PO
          int versionCount = 0;
          Order order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
          if (order.getEscmBaseOrder() != null) {
            versionCount = order.getEscmBaseOrder().getOrderEMEscmBaseOrderList().size();
          } else {
            versionCount = order.getOrderEMEscmBaseOrderList().size();
          }
          if (order.getEscmRevision() < versionCount) {
            // throw new OBException("@Efin_New_Po_Created@");
          }

          // check same order is used in other invoice.
          if (invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")) {
            List<InvoiceLine> invlineExcessQty = invoice.getInvoiceLineList().stream()
                .filter(a -> a.isEfinIspom() && a.getSalesOrderLine() != null
                    && a.getEfinAmtinvoiced()
                        .compareTo(a.getSalesOrderLine().getLineNetAmount()
                            .subtract(a.getSalesOrderLine().getEfinAmtinvoiced())) > 0)
                .collect(Collectors.toList());

            if (invlineExcessQty != null && invlineExcessQty.size() > 0) {
              throw new OBException("@Efin_AmtUsedMore@");
            }
          } else {
            List<InvoiceLine> invlineExcessQty = invoice.getInvoiceLineList().stream()
                .filter(a -> a.isEfinIspom() && a.getSalesOrderLine() != null
                    && a.getInvoicedQuantity()
                        .compareTo(a.getSalesOrderLine().getOrderedQuantity()
                            .subtract(a.getSalesOrderLine().getInvoicedQuantity())) > 0)
                .collect(Collectors.toList());

            if (invlineExcessQty != null && invlineExcessQty.size() > 0) {
              throw new OBException("@Efin_QtyUsedMore@");
            }
          }

          // check manual added lines is in positive amount.
          List<InvoiceLine> line = invoice.getInvoiceLineList().stream()
              .filter(a -> !a.isEfinIspom() && a.getLineNetAmount().compareTo(BigDecimal.ZERO) >= 0)
              .collect(Collectors.toList());
          if (line.size() > 0) {
            throw new OBException("@Efin_AdjAcct_Negative@");
          }
        }

        // impact invoicedqty in order for POM
        /*
         * if (PO_DOCUMENT.equals(strInvoiceType) && invoice.getDocumentStatus().equals("DR")) {
         * Order order = null; for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
         * 
         * if (invLine.isEfinIspom() && invoice.getEfinCOrder().getEscmReceivetype().equals("AMT"))
         * {
         * 
         * if (invoice.getEfinCOrder() != null) order = OBDal.getInstance().get(Order.class,
         * invoice.getEfinCOrder().getId());
         * 
         * OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
         * invLine.getSalesOrderLine().getId()); List<OrderLine> ordLineList =
         * PurchaseInvoiceSubmitUtils .getGreaterRevisionOrdLineList(order, ordLine); for (OrderLine
         * ln : ordLineList) { if (!invLine.isEFINIsTaxLine() || (invLine.isEFINIsTaxLine() && order
         * != null && ((order.isEscmIstax() && invoice.isEfinIstax()) || (!order.isEscmIstax() &&
         * invoice.isEfinIstax() && invoice.getEfinTaxMethod().isPriceIncludesTax())))) { ps =
         * conn.getPreparedStatement(
         * "update c_orderline set em_efin_amtinvoiced=? where c_orderline_id=? ");
         * ps.setBigDecimal(1, ln.getEfinAmtinvoiced().add(invLine.getEfinAmtinvoiced()));
         * ps.setString(2, ln.getId()); ps.executeUpdate(); //
         * ln.setEfinAmtinvoiced(ln.getEfinAmtinvoiced().add(invLine.getEfinAmtinvoiced())); } } //
         * OBDal.getInstance().save(ordLine);
         * 
         * } else { if ((!invLine.isEFINIsTaxLine()) &&
         * (invLine.getEfinCElementvalue().getAccountType().equals("E"))) { OrderLine ordLine =
         * OBDal.getInstance().get(OrderLine.class, invLine.getSalesOrderLine().getId()); ps =
         * conn.getPreparedStatement(
         * "update c_orderline set qtyinvoiced=? where c_orderline_id=? "); ps.setBigDecimal(1,
         * ordLine.getInvoicedQuantity().add(invLine.getInvoicedQuantity())); ps.setString(2,
         * ordLine.getId()); ps.executeUpdate(); // ordLine.setInvoicedQuantity( //
         * ordLine.getInvoicedQuantity().add(invLine.getInvoicedQuantity())); //
         * OBDal.getInstance().save(ordLine); } } } }
         */

        // task no 7551
        if (invoice.getEfinManualencumbrance() != null) {
          encumId = invoice.getEfinManualencumbrance();
        }
        if (!invoice.isEfinIsreserved()) {
          if ((invoice.getEfinEncumtype().equals("M")
              && (PPA_DOCUMENT.equals(strInvoiceType) | API_DOCUMENT.equals(strInvoiceType)))
              || (RDV_DOCUMENT.equals(strInvoiceType) && encumId != null)) {
            /* Apply modification */

            Boolean isValidateTax = false;
            Order latestOrder = PurchaseInvoiceSubmitUtils
                .getLatestOrderComplete(invoice.getSalesOrder());
            EFIN_TaxMethod invoiceTaxmethod = invoice.getEfinTaxMethod();
            EFIN_TaxMethod orderTaxmethod = null;
            if (latestOrder != null)
              orderTaxmethod = latestOrder.getEscmTaxMethod();
            long invoiceTaxpercent = invoiceTaxmethod != null ? invoiceTaxmethod.getTaxpercent()
                : 0;
            long orderTaxpercent = orderTaxmethod != null ? orderTaxmethod.getTaxpercent() : 0;

            if (RDV_DOCUMENT.equals(strInvoiceType) && latestOrder != null) {
              isValidateTax = invoiceTaxpercent > orderTaxpercent ? true : false;
            }

            isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
            Boolean hasAmountForTax = Boolean.TRUE;
            if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0 && (invoice
                .getEfinRDVTxnList().get(0).getLineTaxamt().compareTo(BigDecimal.ZERO) == 0)
                || isValidateTax) {
              Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
              if (RDV_DOCUMENT.equals(strInvoiceType) && isValidateTax) {
                taxLinesMap = taxHandler.getTaxLineCodesAndAmountForRDV(invoice);
              } else {
                taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);
              }

              if (!taxLinesMap.isEmpty()) {
                hasAmountForTax = PurchaseInvoiceSubmitUtils.checkRemainingAmountForTaxEncumbrance(
                    invoice, taxLinesMap, Boolean.TRUE, Boolean.FALSE);

                if (!hasAmountForTax) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@EFIN_NoRemainingAmt@");
                  bundle.setResult(result);
                  return;
                }
              }
            }
          } else if (PO_DOCUMENT.equals(strInvoiceType)) {
            isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
            if (isExclusiveTaxInvoice) {
              Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
              taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);
              if (!taxLinesMap.isEmpty()) {
                Boolean hasAmountForTax = PurchaseInvoiceSubmitUtils
                    .checkRemainingAmountForTaxEncumbrance(invoice, taxLinesMap, Boolean.FALSE,
                        Boolean.FALSE);

                if (!hasAmountForTax) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@EFIN_NoRemainingAmt@");
                  bundle.setResult(result);
                  return;
                }
              }
            } else {
              Boolean hasAmountForTax = PurchaseInvoiceSubmitUtils
                  .checkRemainingAmountForPOMEncumbrance(invoice, Boolean.FALSE);

              if (!hasAmountForTax) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@EFIN_NoRemainingAmt@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
        // get document rule
        Connection con = OBDal.getInstance().getConnection();
        NextRoleByRuleVO nextApproval = null;
        Invoice header = OBDal.getInstance().get(Invoice.class, invoice.getId());
        boolean inDocRule = false;
        boolean depthead = false;

        /* Get the Document Type based on Selected Invoice Transaction Document */
        if (API_DOCUMENT.equals(strInvoiceType) || RDV_DOCUMENT.equals(strInvoiceType)
            || PO_DOCUMENT.equals(strInvoiceType)) {
          doctype = Resource.AP_INVOICE_RULE;
          encumtype = "INV";
          alertWindow = AlertWindow.PurchaseInvoice;
        } else if (PPI_DOCUMENT.equals(strInvoiceType)) {
          doctype = Resource.AP_Prepayment_Inv_RULE;
          alertWindow = AlertWindow.PIAPPrepaymentInvoice;
        } else if (PPA_DOCUMENT.equals(strInvoiceType)) {
          doctype = Resource.AP_Prepayment_App_RULE;
          encumtype = "PPA";
          alertWindow = AlertWindow.PIAPPrepaymentApplication;
        }
        // get alert rule id
        OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
            "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                + "'");
        if (queryAlertRule.list().size() > 0) {
          AlertRule objRule = queryAlertRule.list().get(0);
          alertRuleId = objRule.getId();
        }

        if (invoice.getGrandTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
          grandTotal = invoice.getGrandTotalAmount().multiply(new BigDecimal(-1));
        } else
          grandTotal = invoice.getGrandTotalAmount();

        /* Check is direct approval */
        Boolean dirapp = isDirectApproval(invoiceId, roleId, userId);
        /* checki in doc rule */
        inDocRule = UtilityDAO.chkRoleIsInDocRul(con, clientId, submittedRoleOrgId, userId, roleId,
            doctype, grandTotal);
        /* get the next role based on document type */
        inDocRule = UtilityDAO.chkRoleIsInDocRul(con, clientId, submittedRoleOrgId, userId, roleId,
            doctype, grandTotal);

        boolean isDummyRole = false;
        JSONObject fromUserandRoleJson = new JSONObject();
        String fromUser = userId;
        String fromRole = roleId;

        if ((invoice.getEutNextRole() != null)) {
          fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
              invoice.getEutNextRole(), userId, roleId, clientId, submittedRoleOrgId, doctype,
              isDummyRole, dirapp);
          if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
            if (fromUserandRoleJson.has("fromUser"))
              fromUser = fromUserandRoleJson.getString("fromUser");
            if (fromUserandRoleJson.has("fromRole"))
              fromRole = fromUserandRoleJson.getString("fromRole");
            if (fromUserandRoleJson.has("isDirectApproval"))
              dirapp = fromUserandRoleJson.getBoolean("isDirectApproval");
          }
        } else {
          fromUser = userId;
          fromRole = roleId;
        }
        /* check dept head */
        depthead = UtilityDAO.chkUserIsDeptHead(clientId, orgId, fromUser, costCenterId);

        if (invoice.getEutNextRole() == null) {
          invoice.setEfinSubmittedRole(submittedRoleObj);
          nextApproval = NextRoleByRule.getInvNextRole(con, clientId, submittedRoleOrgId, fromRole,
              fromUser, doctype, grandTotal, invoiceId);
        } else {
          if ((dirapp) || (depthead) /* || (inDocRule) */) {

            nextApproval = NextRoleByRule.getInvNextRole(con, clientId, submittedRoleOrgId,
                fromRole, fromUser, doctype, grandTotal, invoiceId);
          } else {
            UtilityVO vo = UtilityDAO.roleInAppDelegation(con, clientId, orgId, fromUser, fromRole,
                invoiceId);
            HashMap<String, String> roles = NextRoleByRule.getDelegatedFromAndToRolesInvoice(con,
                vars.getClient(), submittedRoleOrgId, vars.getUser(), doctype, null, fromRole,
                vo.getRoleId(), vo.getUserId(), grandTotal, costCenterId);
            String delegatedFromRole = roles.get("FromUserRoleId");
            String delegatedToRole = roles.get("ToUserRoleId");
            if (delegatedFromRole != null && delegatedToRole != null)
              nextApproval = NextRoleByRule.getDelegatedNextRole(con, vars.getClient(),
                  submittedRoleOrgId, delegatedFromRole, delegatedToRole, vars.getUser(), doctype,
                  grandTotal);
          }
        }
        EutNextRole nextRole = null;
        currentDocStatus = header.getDocumentStatus();
        if (header.getDocumentStatus().equals("DR")) {
          status = "Submit";
          appstatus = "SUB";
        } else if (header.getDocumentStatus().equals("EFIN_WFA")) {
          status = "Approve";
          appstatus = "APP";
        }

        // Check whether Funds budget is defined and Funds available is greater than the line net
        // amount for the lines
        PurchaseInvoiceSubmitUtils.preValidation(fromRole, delegatedFromRole, hasDelegation,
            doctype, invoice, strInvoiceType, conversionrate, nextApproval);

        if (log.isDebugEnabled()) {
          if (nextApproval != null) {
            log.debug("nextApproval123:" + nextApproval);
            log.debug("getNextRoleId:" + nextApproval.getNextRoleId());
            log.debug("getErrorMsg:" + nextApproval.getErrorMsg());
          }
        }
        /* If Next approval is there set the header status */
        if (nextApproval != null && nextApproval.hasApproval()) {
          ArrayList<String> includeRecipient = new ArrayList<String>();

          // get old nextrole line user and role list
          HashMap<String, String> alertReceiversMap = forwardDao
              .getNextRoleLineList(header.getEutNextRole(), doctype);

          nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
          header.setUpdated(new java.util.Date());
          header.setUpdatedBy(OBContext.getOBContext().getUser());
          header.setDocumentStatus("EFIN_WFA");
          header.setEutNextRole(nextRole);
          header.setEfinDocaction("AP");
          header.setEfinDocactionfinal("AP");
          header.setEfinNextapprovers(nextApproval.getNextApprover());

          pendingapproval = nextApproval.getStatus();

          User objUser = Utility.getObject(User.class, vars.getUser());

          forwardDao.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE, header.getDocumentNo(), vars.getLanguage(),
              vars.getRole(), header.getEutForward(), doctype, alertReceiversMap);

          // Alert
          // get alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRuleID ");
          receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
          List<AlertRecipient> receipientQueryList = receipientQuery.list();
          // set alerts for next roles
          if (nextRole.getEutNextRoleLineList().size() > 0) {
            // delete alert for approval alerts
            // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            // "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'");
            // if (alertQuery.list().size() > 0) {
            // for (Alert objAlert : alertQuery.list()) {
            // objAlert.setAlertStatus("SOLVED");
            // }
            // }
            // set the description for alert based on Document type
            if (alertWindow.equals("API")) {
              Description = sa.elm.ob.finance.properties.Resource
                  .getProperty("finance.purchaseinvoice.waiting.for.approval", vars.getLanguage());
              alertKey = "finance.purchaseinvoice.waiting.for.approval";

            } else if (alertWindow.equals("APPI")) {
              Description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.apprepaymentinvoice.waiting.for.approval", vars.getLanguage());
              alertKey = "finance.apprepaymentinvoice.waiting.for.approval";

            } else if (alertWindow.equals("APPA")) {
              Description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.apprepaymentapplication.waiting.for.approval", vars.getLanguage());
              alertKey = "finance.apprepaymentapplication.waiting.for.approval";

            }

            if (pendingapproval == null)
              pendingapproval = nextApproval.getStatus();

            // Filtering 'nextRoleList' to avoid duplicate alerts for same role and user.
            ArrayList<EutNextRoleLine> nextRoleList = new ArrayList<EutNextRoleLine>();
            HashSet<String> nextRoleSet = new HashSet<String>();
            Integer initial = 0;
            for (EutNextRoleLine ln : nextRole.getEutNextRoleLineList()) {
              if (initial == 0) {
                nextRoleSet.add(ln.getRole().getId()
                    .concat(ln.getUserContact() == null ? "0" : ln.getUserContact().getId()));
                nextRoleList.add(ln);
                initial++;
              } else {
                if (!nextRoleSet.contains(ln.getRole().getId()
                    .concat(ln.getUserContact() == null ? "0" : ln.getUserContact().getId()))) {
                  nextRoleList.add(ln);
                }
              }
            }
            for (EutNextRoleLine objNextRoleLine : nextRoleList) {
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                  objNextRoleLine.getRole().getId(),
                  (objNextRoleLine.getUserContact() == null ? ""
                      : objNextRoleLine.getUserContact().getId()),
                  header.getClient().getId(), Description, "NEW", alertWindow, alertKey,
                  Constants.GENERIC_TEMPLATE);
              // get user name for delegated user to insert on approval history.
              OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance()
                  .createQuery(EutDocappDelegateln.class,
                      " as e left join e.eUTDocappDelegate as hd where hd.role.id = :roleID"
                          + " and hd.fromDate <= :fromDate and hd.date >= :currentDate "
                          + (objNextRoleLine.getUserContact() != null ? " and hd.userContact.id='"
                              + objNextRoleLine.getUserContact().getId() + "'" : " ")
                          + " and e.documentType= :documentType");
              delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
              delegationln.setNamedParameter("fromDate", currentDate);
              delegationln.setNamedParameter("currentDate", currentDate);
              delegationln.setNamedParameter("documentType", doctype);
              List<EutDocappDelegateln> delegationlnList = delegationln.list();
              Role role = OBDal.getInstance().get(Role.class, objNextRoleLine.getRole().getId());
              if (delegationln != null && delegationlnList.size() > 0) {

                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                    delegationlnList.get(0).getRole().getId(),
                    (delegationlnList.get(0).getUserContact() == null ? ""
                        : delegationlnList.get(0).getUserContact().getId()),
                    header.getClient().getId(), Description, "NEW", alertWindow, alertKey,
                    Constants.GENERIC_TEMPLATE);
                if (pendingapproval != null)
                  pendingapproval += "/" + delegationlnList.get(0).getUserContact().getName();
                else
                  pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                      delegationlnList.get(0).getUserContact().getName());
                includeRecipient.add(delegationlnList.get(0).getRole().getId());
              }
              // add next role recipient
              includeRecipient.add(objNextRoleLine.getRole().getId());

            }
          }

          // existing Recipient
          if (receipientQueryList.size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQueryList) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }

        }

        /*
         * If next approval is not there for the current role and no error msg then call the
         * Invoice_Post0
         */

        else if (nextApproval != null && nextApproval.getNextRoleId() == null
            && nextApproval.getErrorMsg() == null) {
          p_instance_id = SequenceIdData.getUUID();
          String error = "", s = "";
          CallableStatement cs = null;

          log.debug("p_instance_id:" + p_instance_id);
          sql = " INSERT INTO ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isactive, ad_user_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby,isprocessing)  "
              + "  VALUES ('" + p_instance_id + "', '111','" + invoice.getId() + "', 'Y','" + userId
              + "','" + clientId + "','" + orgId + "', now(),'" + userId + "', now(),'" + userId
              + "','Y')";
          ps = conn.getPreparedStatement(sql);
          log.debug("ps:" + ps.toString());
          count = ps.executeUpdate();
          log.debug("count:" + count);

          String instanceqry = "select ad_pinstance_id from ad_pinstance where ad_pinstance_id=?";
          PreparedStatement pr = conn.getPreparedStatement(instanceqry);
          pr.setString(1, p_instance_id);
          ResultSet set = pr.executeQuery();

          if (set.next()) {

            sql = " select * from  efin_invoice_post0(?)";
            ps = conn.getPreparedStatement(sql);
            ps.setString(1, p_instance_id);
            // ps.setString(2, invoice.getId());
            ps.executeQuery();

            log.debug("count12:" + set.getString("ad_pinstance_id"));

            sql = " select result, errormsg from ad_pinstance where ad_pinstance_id='"
                + p_instance_id + "'";
            ps = conn.getPreparedStatement(sql);
            log.debug("ps12:" + ps.toString());
            rs = ps.executeQuery();
            if (rs.next()) {
              log.debug("result:" + rs.getString("result"));

              if (rs.getString("result").equals("0")) {
                error = rs.getString("errormsg").replace("@ERROR=", "");
                log.debug("error:" + error);
                s = error;
                int start = s.indexOf("@");
                int end = s.lastIndexOf("@");

                if (log.isDebugEnabled()) {
                  log.debug("start:" + start);
                  log.debug("end:" + end);
                }

                if (end != 0) {
                  sql = " select  msgtext from ad_message where value ='"
                      + s.substring(start + 1, end) + "'";
                  ps = conn.getPreparedStatement(sql);
                  log.debug("ps12:" + ps.toString());
                  rs = ps.executeQuery();
                  if (rs.next()) {
                    if (rs.getString("msgtext") != null)
                      throw new OBException(error);

                  }
                }
              } else if (rs.getString("result").equals("1")) {

              }
            }
          }

          // get old nextrole line user and role list
          HashMap<String, String> alertReceiversMap = forwardDao
              .getNextRoleLineList(header.getEutNextRole(), doctype);

          header.setUpdated(new java.util.Date());
          header.setUpdatedBy(OBContext.getOBContext().getUser());
          header.setEutNextRole(null);
          header.setEfinNextapprovers(null);
          header.setEfinDocaction("AP");
          header.setEfinDocactionfinal("AP");
          if (PPA_DOCUMENT.equals(strInvoiceType)) {
            header.setPaymentComplete(true);
          }
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();

          // update order invoice amount Task No.7470
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
              List<Order> orderList = PurchaseInvoiceSubmitUtils.getGreaterRevisionOrdList(order);
              if (orderList.size() > 0) {
                OBInterceptor.setPreventUpdateInfoChange(true);
                for (Order ordObj : orderList) {
                  // ps = conn.getPreparedStatement(
                  // "update c_order set em_efin_invoice_amt=?, em_efin_remaining_amt=? "
                  // + " where c_order_id=?");
                  // ps.setBigDecimal(1,
                  // ordObj.getEfinInvoiceAmt().add(invoice.getGrandTotalAmount()));
                  // ps.setBigDecimal(2,
                  // ordObj.getEfinRemainingAmt().subtract(invoice.getGrandTotalAmount()));
                  // ps.setString(3, ordObj.getId());
                  // ps.executeUpdate();
                  ordObj.setEfinInvoiceAmt(
                      ordObj.getEfinInvoiceAmt().add(invoice.getGrandTotalAmount()));
                  ordObj.setEfinRemainingAmt(
                      ordObj.getEfinRemainingAmt().subtract(invoice.getGrandTotalAmount()));
                  OBDal.getInstance().save(ordObj);
                }
              }
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);
            }

          }
          log.debug("nextrolecomplete:" + header.getEutNextRole());
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(con, doctype);

          // final approval alert
          Role objCreatedRole = null;
          ArrayList<String> includeRecipient = new ArrayList<String>();
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            if (header.getEfinAdRole() != null)
              objCreatedRole = header.getEfinAdRole();
            else
              objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }

          User objUser = Utility.getObject(User.class, vars.getUser());

          forwardDao.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE, header.getDocumentNo(), vars.getLanguage(),
              vars.getRole(), header.getEutForward(), doctype, alertReceiversMap);

          // delete alert for approval alerts
          // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          // "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'");
          // if (alertQuery.list().size() > 0) {
          // for (Alert objAlert : alertQuery.list()) {
          // objAlert.setAlertStatus("SOLVED");
          // }
          // }
          // get alert recipient
          OBQuery<AlertRecipient> receipientQuery1 = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
          // check and insert recipient
          if (receipientQuery1.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery1.list()) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          includeRecipient.add(objCreatedRole == null ? null : objCreatedRole.getId());

          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }
          // set the description for alert based on Document type
          if (alertWindow.equals("API")) {
            Description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.purchaseinvoice.approved", vars.getLanguage());
            alertKey = "finance.purchaseinvoice.approved";

          } else if (alertWindow.equals("APPI")) {
            Description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.apprepaymentinvoice.approved", vars.getLanguage());
            alertKey = "finance.apprepaymentinvoice.approved";

          } else if (alertWindow.equals("APPA")) {
            Description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.apprepaymentapplication.approved", vars.getLanguage());
            alertKey = "finance.apprepaymentapplication.approved";
          }
          // set alert for requester

          JSONObject SubmitterDetailJson = Utility.getSubmitterDetail(alertWindow, header.getId());
          if (SubmitterDetailJson != null && SubmitterDetailJson.length() > 0) {
            if (SubmitterDetailJson.has("createrUser"))
              createdUserId = SubmitterDetailJson.getString("createrUser");
            if (SubmitterDetailJson.has("createrRole"))
              createdRoleId = SubmitterDetailJson.getString("createrRole");
          }

          if (createdRoleId != null) {
            objCreatedRole = OBDal.getInstance().get(Role.class, createdRoleId);
          }

          AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
              objCreatedRole == null ? "" : objCreatedRole.getId(),
              createdUserId == null ? header.getCreatedBy().getId() : createdUserId,
              header.getClient().getId(), Description, "NEW", alertWindow, alertKey,
              Constants.GENERIC_TEMPLATE);

        }

        // after approved by forwarded user removing the forward and rmi id
        if (header.getEutForward() != null) {
          forwardDao.setForwardStatusAsDraft(header.getEutForward());
          header.setEutForward(null);
        }
        if (header.getEutReqmoreinfo() != null) {
          forwardDao.setForwardStatusAsDraft(header.getEutReqmoreinfo());
          header.setEutReqmoreinfo(null);
          header.setEfinReqMoreInfo("N");
        }

        /*
         * If next approval is not there for the current role and any error msg then throw the error
         */
        else if (nextApproval != null && nextApproval.getNextRoleId() == null
            && nextApproval.getErrorMsg() != null) {
          log.error("exception:" + nextApproval.getErrorMsg());
          throw new OBException(OBMessageUtils.messageBD(nextApproval.getErrorMsg()));
        }

        OBDal.getInstance().save(header);
        headerId = header.getId();
        log.debug("headerId:" + headerId);

        // insert a record in ApprovalHistory
        count = insertInvoiceApprover(invoice, comments, appstatus, pendingapproval);
        String campaign = "";
        // get invoicelines
        OBQuery<InvoiceLine> invline = OBDal.getInstance().createQuery(InvoiceLine.class,
            "invoice.id='" + invoice.getId() + "'");
        if (invline.list().size() > 0) {
          for (InvoiceLine line1 : invline.list()) {
            campaign = line1.getEfinCCampaign().getId();

          }
        }

        // before reserve the funds do the following validation

        // based on current user check that particular user is department head or not
        // chkUserIsDeptHead = UtilityDAO.chkUserIsDeptHeadInvoice(clientId, orgId, fromUser,
        // invoice.getId());
        //
        // log.debug("chkUserIsDeptHeadsubmit:" + chkUserIsDeptHead);
        // /* if user is department manager then get the department head role from the document rule
        // */
        // if (chkUserIsDeptHead) {
        // DeptHeadRoleId = UtilityDAO.getDeptHeadRole(clientId, orgId, doctype,
        // invoice.getGrandTotalAmount());
        //
        // if (DeptHeadRoleId != null && !DeptHeadRoleId.equals("2")) {
        //
        // reserve = UtilityDAO.getReserveFundsRole(doctype, DeptHeadRoleId,
        // invoice.getOrganization().getId(), invoice.getId(), invoice.getGrandTotalAmount());
        //
        // }
        // /* If more than one department head role having in particular client throw the error */
        // else if (DeptHeadRoleId != null && DeptHeadRoleId.equals("2")) {
        // throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_MoreThanDeptRole"));
        // }
        // }
        /* make the reservation if Current role is reserver */

        // else
        // if (!chkUserIsDeptHead) {
        reserve = UtilityDAO.getReserveFundsRole(doctype, fromRole,
            invoice.getOrganization().getId(), invoiceId, invoice.getGrandTotalAmount());

        if (hasDelegation && !reserve) {
          reserve = UtilityDAO.getReserveFundsRole(DocType, delegatedFromRole,
              invoice.getOrganization().getId(), invoiceId, invoice.getGrandTotalAmount());
        }
        // }
        log.debug("reserve:" + reserve);
        EfinBudgetManencumlines manualline = null;
        Boolean isValidCombination = Boolean.TRUE;
        Campaign budgetType = PurchaseInvoiceSubmitUtils.getBudgetType(invoice.getEfinBudgetType());

        encumId = invoice.getEfinManualencumbrance();
        if (reserve && !invoice.isEfinIsreserved()) {

          if (PPA_DOCUMENT.equals(strInvoiceType) && (invoice.getDocumentStatus().equals("EFIN_WFA")
              || invoice.getDocumentStatus().equals("DR"))) {
            // check sum of invoice Lines Amount greater than encumbrance remain amount
            if (!invoice.isEfinIsreserved() && PPA_DOCUMENT.equals(strInvoiceType)) {
              String strUniqueCode = PurchaseInvoiceSubmitUtils.isValidPPA(invoice, conversionrate);
              if (strUniqueCode.length() > 0) {
                throw new OBException("@Efin_appliedamount<lineamt@" + strUniqueCode);
              }

              // check sum of applied amount > applied prepayment invoice amount
              String appliedPrepayment = UtilityDAO.checkAppliedPrepayment(invoiceId,
                  conversionrate);
              if (!appliedPrepayment.equals("")) {
                throw new OBException(OBMessageUtils.messageBD("Efin_Applied_PrepaymentLess")
                    .replace("@invoice@", appliedPrepayment));
              }
              // check sum of applied amount =application invoice total amount
              String applicationInvAmtcheck = UtilityDAO.checkApplicationInvoiceAmount(invoiceId);
              if (!applicationInvAmtcheck.equals("1")) {
                throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_Equal"));
              }
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("getTransactionDocumentapp:" + strInvoiceType);
            log.debug("getDocumentStatus:" + invoice.getDocumentStatus());
          }

          // POM transaction type
          if (PO_DOCUMENT.equals(strInvoiceType)) {
            EfinBudgetManencum fundsEncum = null;
            boolean isCost = false;

            invoice.setEfinEncumbranceType("AEE");
            OBDal.getInstance().save(invoice);

            // validation need to rewrite, if cost then check for funds alone, if funds no need
            if (header.getEfinBudgetType().equals("C")) {
              isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(invoice,
                  conversionrate);
              isCost = false;
              if (!isValidCombination) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Reservation_Nofunds@");
                bundle.setResult(result);
                return;
              }
            }

            BigDecimal invLineAmt = invoice.getInvoiceLineList().stream()
                .filter(a -> a.isEfinIspom() == true).map(a -> a.getLineNetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            Order po = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());

            isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
            if (isExclusiveTaxInvoice) {
              Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
              taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);
              if (!taxLinesMap.isEmpty()) {
                Boolean hasAmountForTax = PurchaseInvoiceSubmitUtils
                    .checkRemainingAmountForTaxEncumbrance(invoice, taxLinesMap, Boolean.FALSE,
                        Boolean.TRUE);

                if (!hasAmountForTax) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@EFIN_NoRemainingAmt@");
                  bundle.setResult(result);
                  return;
                }
              }
            } else {
              Boolean hasAmountForTax = PurchaseInvoiceSubmitUtils
                  .checkRemainingAmountForPOMEncumbrance(invoice, Boolean.TRUE);

              if (!hasAmountForTax) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@EFIN_NoRemainingAmt@");
                bundle.setResult(result);
                return;
              }
            }

            // Task 7847: PO to PO Match Invoice not creating split encumbrance
            // Check Encumbrance is having remaining amount and unused unique codes
            boolean isSplitEncumbrance = false;
            if (invoice.getEfinCOrder() != null) {
              isSplitEncumbrance = PurchaseInvoiceSubmitUtils
                  .checkSplitEncumbrance(invoice.getEfinCOrder().getId());
            }

            if (invLineAmt.compareTo(po.getGrandTotalAmount()) == 0 && !isSplitEncumbrance) {
              // stage move
              String encumID = invoice.getEfinManualencumbrance() != null
                  ? invoice.getEfinManualencumbrance().getId()
                  : "";
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumID);
              encum.setEncumStage("AEE");
              OBDal.getInstance().save(encum);

              // insert invoice reference in new encum lines.
              PurchaseInvoiceSubmitUtils.insertEncumbranceInvoiceReference(invoice, encum, isCost,
                  strInvoiceType, conversionrate);

              // update encumbrance lines for invoiceline
              for (EfinBudgetManencumlines line : encum.getEfinBudgetManencumlinesList()) {

                List<InvoiceLine> lineList = header.getInvoiceLineList().stream()
                    .filter(
                        a -> a.getEfinCValidcombination() != null && a.getEfinCValidcombination()
                            .getId().equals(line.getAccountingCombination().getId()))
                    .collect(Collectors.toList());
                if (lineList.size() > 0) {
                  for (InvoiceLine lineObj : lineList) {
                    lineObj.setEfinBudgmanuencumln(line);
                    OBDal.getInstance().save(line);
                  }
                }
              }

              // // insert invoice reference in new encum lines.
              // for (EfinBudgetManencumlines encln : encum.getEfinBudgetManencumlinesList()) {
              // PurchaseInvoiceSubmitUtils.InsertInvoicesInEncumbrancePPI(invoice, encln,
              // encln.getAmount(), conversionrate);
              // }

              // if cost encum then create new funds encm
              if (header.getEfinBudgetType().equals("C")) {
                if (isCostEncumbrance && !isAdjustementInvoice) {
                  Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();

                  fundsEncum = PurchaseInvoiceSubmitUtils.getNewFundsEnumbrance(fundsBudgetType,
                      invoice.getEfinManualencumbrance(), invoice, conversionrate);

                  if (fundsEncum != null) {
                    PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, fundsEncum,
                        Boolean.TRUE, strInvoiceType, conversionrate);
                  }

                }
              }

            } else {
              // split old PO encum and create new AEE Encum
              isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
              // incase amt is not enough to submit the purchase invoice. need to take amt in budget
              // inquiry and add the amt in po encumbrance. not only exclusive case
              if (invoice.getEfinManualencumbrance().getEncumbranceMethod().equals("A")) {

                PurchaseInvoiceSubmitUtils.insertModificationForAutoInPoMatch(invoice);

              }

              String newPOMencum = PurchaseInvoiceSubmitUtils.splitPoencum(header);
              EfinBudgetManencum POMencum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  newPOMencum);

              // insert invoice reference in new encum lines.
              PurchaseInvoiceSubmitUtils.insertEncumbranceInvoiceReference(invoice, POMencum,
                  isCost, strInvoiceType, conversionrate);

              // insert invoice reference in new encum lines.
              // for (EfinBudgetManencumlines encln : POMencum.getEfinBudgetManencumlinesList()) {
              // PurchaseInvoiceSubmitUtils.InsertInvoicesInEncumbrancePPI(invoice, encln,
              // encln.getAmount(), conversionrate);
              //
              // insertEncumbranceInvoiceReference(invoice, POMencum, isCost, strInvoiceType,
              // conversionrate);
              // }

              // if cost encum then create new funds encm
              if (header.getEfinBudgetType().equals("C")) {
                if (isCostEncumbrance && !isAdjustementInvoice) {
                  Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();

                  fundsEncum = PurchaseInvoiceSubmitUtils.getNewFundsEnumbrance(fundsBudgetType,
                      invoice.getEfinManualencumbrance(), invoice, conversionrate);

                  if (fundsEncum != null) {
                    PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, fundsEncum,
                        Boolean.TRUE, strInvoiceType, conversionrate);
                  }

                }
              }
            }
          }

          // if ap invoice or rdv(direct receipt) auto

          // IN case of RDV invoice, Check budget type if it is cost create cost encumbrance and
          // then create funds encumbrance
          // if budget type is funds, then create only funds encumbrance
          if (((invoice.getEfinEncumtype().equals("A") && API_DOCUMENT.equals(strInvoiceType))
              || RDV_DOCUMENT.equals(strInvoiceType)) && encumId == null) {

            if (budgetType.getEfinBudgettype().equals("C")
                || (RDV_DOCUMENT.equals(strInvoiceType))) {
              isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(invoice,
                  conversionrate);

              if (!isValidCombination) {
                OBDal.getInstance().rollbackAndClose();

                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Reservation_Nofunds@");
                bundle.setResult(result);
                return;
              }
            }

            // MANUAL HEADER
            Campaign fundsBudgetType = null;
            EfinBudgetManencum manual = null;
            if (!isAdjustementInvoice) {
              fundsBudgetType = PurchaseInvoiceSubmitUtils
                  .getBudgetType(invoice.getEfinBudgetType());
              manual = PurchaseInvoiceSubmitUtils.getNewFundsEnumbrance(fundsBudgetType, null,
                  invoice, conversionrate);

              PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, manual, Boolean.FALSE,
                  strInvoiceType, conversionrate);
            }

            OBDal.getInstance().flush();

            // update manual encumbrance field

            String encumid = manual == null ? "" : manual.getId();
            EfinBudgetManencumv manenc = OBDal.getInstance().get(EfinBudgetManencumv.class,
                encumid);
            invoice.setEfinManualencumbrance(manenc);

            OBDal.getInstance().save(invoice);
            OBDal.getInstance().flush();

            if (budgetType.getEfinBudgettype().equals("C") && !isAdjustementInvoice) {
              fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();
              EfinBudgetManencum fundsEncumbrance = PurchaseInvoiceSubmitUtils
                  .getNewFundsEnumbrance(fundsBudgetType, invoice.getEfinManualencumbrance(),
                      invoice, conversionrate);

              if (fundsEncumbrance != null) {
                PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, fundsEncumbrance,
                    Boolean.TRUE, strInvoiceType, conversionrate);
              }
            }
          }
          if ((invoice.getEfinEncumtype().equals("M")
              && (PPA_DOCUMENT.equals(strInvoiceType) | API_DOCUMENT.equals(strInvoiceType)))
              || (RDV_DOCUMENT.equals(strInvoiceType) && encumId != null)) {
            List<EfinRdvHoldAction> holdRelease = new ArrayList<EfinRdvHoldAction>();
            boolean skipTempEncumModify = false;
            if (RDV_DOCUMENT.equals(strInvoiceType)) {

              // validation
              invoice.getEfinRdvtxn().getEfinRDVTxnlineList().stream()
                  .filter(y -> !y.isSummaryLevel()).collect(Collectors.toList()).forEach(ln -> {

                    holdRelease.addAll(ln.getEfinRdvHoldActionList().stream()
                        .filter(x -> x.getEfinBudgetTransfertrxline() != null)
                        .collect(Collectors.toList()));

                  });
              if (holdRelease.size() > 0) {
                HashMap<AccountingCombination, BigDecimal> accountValueMap = new HashMap<AccountingCombination, BigDecimal>();
                for (EfinRdvHoldAction release : holdRelease) {
                  AccountingCombination account = release.getEfinRdvtxnline()
                      .getAccountingCombination();
                  BigDecimal relamount = release.getRDVHoldAmount().negate();

                  if (accountValueMap.containsKey(account)) {
                    BigDecimal amount = accountValueMap.get(account).add(relamount);
                    accountValueMap.put(account, amount);
                  } else {
                    accountValueMap.put(account, relamount);
                  }
                }

                for (EfinRdvHoldAction release : holdRelease) {
                  EfinBudgetManencum encum = release.getEfinBudgetTransfertrxline()
                      .getEfinBudgetTransfertrx().getManualEncumbrance();
                  List<EfinBudgetManencumlines> encumInvalidLine = encum
                      .getEfinBudgetManencumlinesList().stream()
                      .filter(x -> accountValueMap.containsKey(x.getAccountingCombination())
                          && x.getRevamount()
                              .compareTo(accountValueMap.get(x.getAccountingCombination())) < 0)
                      .collect(Collectors.toList());
                  if (encumInvalidLine.size() > 0) {
                    skipTempEncumModify = true;
                    break;
                  }
                }

                // modify temp encum for budget revision - hold relase
                if (!skipTempEncumModify) {
                  for (EfinRdvHoldAction release : holdRelease) {

                    EfinBudgetManencum encum = release.getEfinBudgetTransfertrxline()
                        .getEfinBudgetTransfertrx().getManualEncumbrance();
                    AccountingCombination fundsCombination = null;
                    AccountingCombination releaseCombination = release.getEfinRdvtxnline()
                        .getAccountingCombination();
                    if (releaseCombination.getSalesCampaign().getEfinBudgettype().equals("C")) {
                      fundsCombination = releaseCombination.getEfinFundscombination();
                    } else {
                      fundsCombination = releaseCombination;
                    }

                    AccountingCombination acctCom999 = BudgetHoldPlanReleaseDAOImpl
                        .get999AccountCombination(fundsCombination,
                            fundsCombination.getTrxOrganization().getId(),
                            release.getClient().getId());

                    encum.getEfinBudgetManencumlinesList().stream().filter(x -> x
                        .getAccountingCombination().getId().equals(acctCom999.getId())
                        && (x.getRevamount().compareTo(release.getRDVHoldAmount().negate()) >= 0))
                        .collect(Collectors.toList()).forEach(encumln -> {
                          // apply modification.
                          PurchaseInvoiceSubmitUtils.insertModification(encumln,
                              release.getRDVHoldAmount());
                          // 999 update increase the encumbrance
                          EncumbranceProcessDAO.updateBudgetInquiry(encumln,
                              encumln.getManualEncumbrance(), release.getRDVHoldAmount(), false,
                              false);
                          // 990 update
                          EncumbranceProcessDAO.updateBudgetInquiryfor990(
                              encumln.getAccountingCombination(), encumln.getClient().getId(),
                              encum, release.getRDVHoldAmount());

                        });
                  }
                  OBDal.getInstance().flush();
                }
              }

            }

            /* Apply modification */

            Boolean isValidateTax = false;
            Order latestOrder = PurchaseInvoiceSubmitUtils
                .getLatestOrderComplete(invoice.getSalesOrder());
            EFIN_TaxMethod invoiceTaxmethod = invoice.getEfinTaxMethod();
            EFIN_TaxMethod orderTaxmethod = null;
            if (latestOrder != null)
              orderTaxmethod = latestOrder.getEscmTaxMethod();
            long invoiceTaxpercent = invoiceTaxmethod != null ? invoiceTaxmethod.getTaxpercent()
                : 0;
            long orderTaxpercent = orderTaxmethod != null ? orderTaxmethod.getTaxpercent() : 0;

            if (RDV_DOCUMENT.equals(strInvoiceType) && latestOrder != null) {
              isValidateTax = invoiceTaxpercent > orderTaxpercent ? true : false;
            }

            isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
            Boolean hasAmountForTax = Boolean.TRUE;
            if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0
                && (invoice.getEfinRDVTxnList().get(0).getLineTaxamt()
                    .compareTo(BigDecimal.ZERO) == 0 || isValidateTax)) {
              Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();

              if (RDV_DOCUMENT.equals(strInvoiceType) && isValidateTax) {
                taxLinesMap = taxHandler.getTaxLineCodesAndAmountForRDV(invoice);
              } else {
                taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);
              }

              if (!taxLinesMap.isEmpty()) {
                hasAmountForTax = PurchaseInvoiceSubmitUtils.checkRemainingAmountForTaxEncumbrance(
                    invoice, taxLinesMap, Boolean.TRUE, Boolean.TRUE);

                if (hasAmountForTax) {
                  EfinBudgetManencum poEncumbrance = PurchaseInvoiceSubmitUtils
                      .getPoEncumbranceFromInvoice(invoice);
                  if (poEncumbrance.getEncumMethod().equals("A") && isExclusiveTaxInvoice) {
                    PurchaseInvoiceSubmitUtils.insertModificationForAutoInRdvInvoiceTax(invoice);
                  }
                  PurchaseInvoiceSubmitUtils.addEncumbranceModification(taxLinesMap, poEncumbrance,
                      invoice);
                } else {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@EFIN_NoRemainingAmt@");
                  bundle.setResult(result);
                  return;
                }
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().refresh(invoice);
            }

            isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(invoice,
                conversionrate);
            String strValidCombinationId = "";
            HashMap<String, BigDecimal> lineMap = PurchaseInvoiceSubmitUtils
                .getExpenseLines(invoice, conversionrate, strInvoiceType, isCostEncumbrance);
            HashMap<String, BigDecimal> rdvCostLineMap = PurchaseInvoiceSubmitUtils
                .getExpenseLines(invoice, conversionrate, strInvoiceType, false);
            EfinBudgetManencumlines encumbranceLine = null;
            EfinBudgetManencum fundsEncumbrance = null;
            if (API_DOCUMENT.equals(strInvoiceType) || RDV_DOCUMENT.equals(strInvoiceType)) {
              isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(invoice,
                  conversionrate);
              if (!isValidCombination) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Reservation_Nofunds@");
                bundle.setResult(result);
                return;
              }

              if (isCostEncumbrance && !isAdjustementInvoice) {
                Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();

                fundsEncumbrance = PurchaseInvoiceSubmitUtils.getNewFundsEnumbrance(fundsBudgetType,
                    invoice.getEfinManualencumbrance(), invoice, conversionrate);

                if (fundsEncumbrance != null) {
                  PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, fundsEncumbrance,
                      Boolean.TRUE, strInvoiceType, conversionrate);
                }
              }
            } else if (PPA_DOCUMENT.equals(strInvoiceType)) {
              PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionrate,
                  false);
            }

            if (!isCostEncumbrance) {
              if (!lineMap.isEmpty()) {
                for (Map.Entry<String, BigDecimal> entry : lineMap.entrySet()) {
                  strValidCombinationId = entry.getKey();
                  lineNetAmt = entry.getValue();
                  if (lineNetAmt.compareTo(BigDecimal.ZERO) > 0) {
                    encumbranceLine = PurchaseInvoiceSubmitUtils.getEncumbranceLine(
                        fundsEncumbrance == null ? invoice.getEfinManualencumbrance().getId()
                            : fundsEncumbrance.getId(),
                        strValidCombinationId);
                    PostUtilsDAO.InsertInvoicesInEncumbrance(invoice, encumbranceLine, lineNetAmt,
                        Boolean.FALSE);
                  }
                }
                OBDal.getInstance().flush();
              }
            }

            if (isCostEncumbrance) {
              if (!rdvCostLineMap.isEmpty()) {
                for (Map.Entry<String, BigDecimal> entry : rdvCostLineMap.entrySet()) {
                  strValidCombinationId = entry.getKey();
                  lineNetAmt = entry.getValue();
                  if (lineNetAmt.compareTo(BigDecimal.ZERO) > 0) {
                    encumbranceLine = PurchaseInvoiceSubmitUtils.getEncumbranceLine(
                        invoice.getEfinManualencumbrance().getId(), strValidCombinationId);
                    if (encumbranceLine != null) {
                      final AccountingCombination accCombination = encumbranceLine
                          .getAccountingCombination();
                      List<InvoiceLine> line = invoice.getInvoiceLineList().stream()
                          .filter(a -> a.getEfinCValidcombination() == accCombination)
                          .collect(Collectors.toList());
                      if (line.size() > 1) {
                        PurchaseInvoiceSubmitUtils.InsertInvoicesInEncumbrancePPI(invoice,
                            encumbranceLine, lineNetAmt, conversionrate);
                      } else if (line.size() > 0) {
                        PurchaseInvoiceSubmitUtils.InsertInvoicesInEncumbrance(line.get(0),
                            encumbranceLine, strInvoiceType, conversionrate);
                      }
                    }
                  }
                }
                OBDal.getInstance().flush();
              }
            }

            /*
             * for (InvoiceLine ln : invline.list()) { if
             * (ln.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) { PendingAmt =
             * BigDecimal.ZERO; efinbudgetencum efinencum = null; lineNetAmt =
             * ln.getLineNetAmount();
             * 
             * if (ln.getEfinBudgmanuencumln() != null && (API_DOCUMENT.equals(strInvoiceType) ||
             * RDV_DOCUMENT.equals(strInvoiceType))) { // do the entry in manual encumbrance line
             * invoice tab PurchaseInvoiceSubmitUtils.InsertInvoicesInEncumbrance(ln,
             * ln.getEfinBudgmanuencumln(), strInvoiceType, conversionrate);
             * OBDal.getInstance().flush();
             * 
             * if (isCostEncumbrance) { Campaign fundsBudgetType =
             * PurchaseInvoiceSubmitUtils.getFundsBudgetType();
             * 
             * EfinBudgetManencum fundsEncumbrance = PurchaseInvoiceSubmitUtils
             * .getNewFundsEnumbrance(fundsBudgetType, invoice.getEfinManualencumbrance(), invoice,
             * conversionrate);
             * 
             * if (fundsEncumbrance != null) {
             * PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, fundsEncumbrance,
             * Boolean.TRUE, strInvoiceType, conversionrate); } } } else if
             * (PPA_DOCUMENT.equals(strInvoiceType)) {
             * PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionrate);
             * } if (API_DOCUMENT.equals(strInvoiceType) || RDV_DOCUMENT.equals(strInvoiceType)) {
             * encumdoctype = "AEE";
             * 
             * } else if (PPA_DOCUMENT.equals(strInvoiceType)) encumdoctype = "AAE";
             * 
             * log.debug("encumdoctype:" + encumdoctype); } }
             */
            // if from rdv then stage move
            if (RDV_DOCUMENT.equals(strInvoiceType)) {
              String encum = invoice.getEfinManualencumbrance().getManualEncumbrance();
              EfinBudgetManencum manEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  encum);
              manEncum.setEncumStage("AEE");
              OBDal.getInstance().save(manEncum);
            }
            PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(), conversionrate,
                false);
          } else if ("A".equals(invoice.getEfinEncumtype())
              && PPA_DOCUMENT.equals(strInvoiceType)) {
            PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionrate,
                false);
            encumdoctype = "AAE";
            PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(), conversionrate,
                false);
          }

          if (invoice.getEfinDistribution() != null && PPI_DOCUMENT.equals(strInvoiceType)) {

            grandTotal = FinanceUtils.getConvertedAmount(invoice.getGrandTotalAmount(),
                conversionrate);

            if (invoice.getEfinEncumtype().equals("M")
                && invoice.getEfinManualencumbrance() != null) {
              if (PurchaseInvoiceSubmitUtils.checkRemainingAmount(invoice, grandTotal) == -1) {
                OBError result = OBErrorBuilder.buildMessage(null, "Error", "@Efin_Remamt_lesser@");
                bundle.setResult(result);
                return;
              }

              PurchaseInvoiceSubmitUtils.updateAdvanceEncumbrance(invoice, grandTotal);
            } else {
              Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();
              EfinBudgetManencum encumbrance = PurchaseInvoiceSubmitUtils
                  .getNewFundsEnumbrance(fundsBudgetType, null, invoice, conversionrate);
              EfinBudgetInquiry budgetinq = null;
              List<InvoiceLine> invoiceLineList = invoice.getInvoiceLineList();
              for (InvoiceLine invLine : invoiceLineList) {
                budgetinq = PurchaseInvoiceSubmitUtils
                    .getBudgetInquiry(invLine.getEfinExpenseAccount(), invoice.getEfinBudgetint());
                if (budgetinq == null) {
                  OBError result = OBErrorBuilder.buildMessage(null, "Error",
                      "@Efin_Reservation_Nofunds@");
                  bundle.setResult(result);
                  return;
                } else {
                  BigDecimal totalLineAmt = invoiceLineList.stream()
                      .filter(a -> a.getEfinExpenseAccount() == invLine.getEfinExpenseAccount())
                      .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                  if (budgetinq.getFundsAvailable().compareTo(totalLineAmt) < 0) {
                    OBError result = OBErrorBuilder.buildMessage(null, "Error",
                        "@Efin_Reservation_Nofunds@");
                    bundle.setResult(result);
                    return;
                  }

                }
              }
              PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, encumbrance,
                  Boolean.FALSE, strInvoiceType, conversionrate);
              EfinBudgetManencumv manenc = OBDal.getInstance().get(EfinBudgetManencumv.class,
                  encumbrance.getId());
              invoice.setEfinManualencumbrance(manenc);

              OBDal.getInstance().save(invoice);
            }

            // update manual encumbrance amt
            OBDal.getInstance().flush();
          }

          invoice.setEfinIsreserved(Boolean.TRUE);
          OBDal.getInstance().save(invoice);
          OBDal.getInstance().flush();
        }

        if (PO_DOCUMENT.equals(strInvoiceType) && currentDocStatus.equals("DR")) {
          Order order = null;
          for (InvoiceLine invLine : invoice.getInvoiceLineList()) {

            if (invLine.isEfinIspom()
                && invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")) {

              if (invoice.getEfinCOrder() != null)
                order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());

              OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                  invLine.getSalesOrderLine().getId());
              List<OrderLine> ordLineList = PurchaseInvoiceSubmitUtils
                  .getGreaterRevisionOrdLineList(order, ordLine);
              for (OrderLine ln : ordLineList) {
                OBInterceptor.setPreventUpdateInfoChange(true);
                if (!invLine.isEFINIsTaxLine() || (invLine.isEFINIsTaxLine() && order != null
                    && ((order.isEscmIstax() && invoice.isEfinIstax())
                        || (!order.isEscmIstax() && invoice.isEfinIstax()
                            && invoice.getEfinTaxMethod().isPriceIncludesTax())))) {
                  // ps = conn.getPreparedStatement(
                  // "update c_orderline set em_efin_amtinvoiced=? where c_orderline_id=? ");
                  // ps.setBigDecimal(1, ln.getEfinAmtinvoiced().add(invLine.getEfinAmtinvoiced()));
                  // ps.setString(2, ln.getId());
                  // ps.executeUpdate();
                  ln.setEfinAmtinvoiced(ln.getEfinAmtinvoiced().add(invLine.getEfinAmtinvoiced()));
                  OBDal.getInstance().save(ln);
                }
              }

            } else {
              if ((!invLine.isEFINIsTaxLine())
                  && (invLine.getEfinCElementvalue().getAccountType().equals("E"))) {
                OBInterceptor.setPreventUpdateInfoChange(true);
                OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                    invLine.getSalesOrderLine().getId());
                // ps = conn.getPreparedStatement(
                // "update c_orderline set qtyinvoiced=? where c_orderline_id=? ");
                // ps.setBigDecimal(1,
                // ordLine.getInvoicedQuantity().add(invLine.getInvoicedQuantity()));
                // ps.setString(2, ordLine.getId());
                // ps.executeUpdate();
                ordLine.setInvoicedQuantity(
                    ordLine.getInvoicedQuantity().add(invLine.getInvoicedQuantity()));
                OBDal.getInstance().save(ordLine);
              }
            }
          }
        }

        // Check Encumbrance Amount is Negative
        Map<EfinBudgetManencumlines, Double> encumMap = null;
        encumMap = invoice.getInvoiceLineList().stream()
            .filter(b -> b.getEfinBudgmanuencumln() != null)
            .collect(Collectors.groupingBy(InvoiceLine::getEfinBudgmanuencumln,
                Collectors.summingDouble(a -> a.getLineNo().doubleValue())));
        if (invoice.getEfinFundsEncumbrance() != null)
          fundsEncumList = invoice.getEfinFundsEncumbrance().getEfinBudgetManencumlinesList();

        if (encumMap != null)
          encumLinelist = new ArrayList<EfinBudgetManencumlines>(encumMap.keySet());
        if (encumLinelist.size() > 0) {
          checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);
          checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(fundsEncumList);
        }
        if (checkEncumbranceAmountZero) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
          bundle.setResult(result);
          return;
        }

        OBDal.getInstance().flush();

        // call invoice completion hook
        JSONObject parameters = new JSONObject();
        parameters.put("status", status);
        parameters.put("doctype", doctype);
        parameters.put("userId", userId);
        parameters.put("tabId", tabId);
        parameters.put("isPrepaymentApp", isPrepaymentApp);

        WeldUtils.getInstanceFromStaticBeanManager(PurchaseinvoiceCompletionHookCaller.class)
            .executeHook(strInvoiceType, invoice, parameters, vars, conn);

        if (header.getDocumentAction().equals("CO")) {
          if (status.equals("Submit")) {
            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PurInv_Submit@");
            bundle.setResult(result);
            return;
          } else if (status.equals("Approve")) {
            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PurInv_Approve@");
            bundle.setResult(result);
            return;
          }
        }
      }
    } catch (GenericJDBCException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in POContractSummaryAction :", e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      String errorMessage = OBMessageUtils.translateError(t.getMessage()).getMessage();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD(errorMessage));
      bundle.setResult(error);
      return;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      OBInterceptor.setPreventUpdateInfoChange(false);
    }

  }

  /**
   * This method is used to insert Encumbrance invoice reference
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param userId
   * @param manencumlineId
   * @param encuminvoiceId
   * @param encumManExpId
   * @param encumId
   * @param actId
   * @param invamt
   * @param expamt
   * @param actamt
   * @return
   */
  @SuppressWarnings("unused")
  public static int insertEncumInvoiceRef(Connection con, String clientId, String orgId,
      String userId, String manencumlineId, String encuminvoiceId, String encumManExpId,
      String encumId, String actId, BigDecimal invamt, BigDecimal expamt, BigDecimal actamt) {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    String invrefId = null;
    try {

      EfinEncumInvoiceRef invref = OBProvider.getInstance().get(EfinEncumInvoiceRef.class);
      invref.setClient(dao.getObject(Client.class, clientId));
      invref.setOrganization(dao.getObject(Organization.class, orgId));
      invref.setActive(true);
      invref.setCreatedBy(dao.getObject(User.class, userId));
      invref.setCreationDate(new java.util.Date());
      invref.setCreatedBy(dao.getObject(User.class, userId));
      invref.setUpdated(new java.util.Date());
      if (manencumlineId != null)
        invref.setManualEncumbranceLines(
            OBDal.getInstance().get(EfinBudgetManencumlines.class, manencumlineId));
      if (encuminvoiceId != null)
        invref.setEfinBudgetEncuminvoice(
            OBDal.getInstance().get(efinbudgetencum.class, encuminvoiceId));
      if (encumManExpId != null)
        invref.setEfinBudgetEncummanexpe(
            OBDal.getInstance().get(efinbudgetencum.class, encumManExpId));
      if (encumId != null)
        invref.setEncumbranceTransaction(OBDal.getInstance().get(efinbudgetencum.class, encumId));
      if (actId != null)
        invref.setEfinBudgetActual(OBDal.getInstance().get(EfinBudgetActual.class, actId));
      invref.setInvamount(invamt);
      invref.setManexpamount(expamt);
      invref.setActualAmount(actamt);
      OBDal.getInstance().save(invref);
      log.debug("invref:" + invref.toString());

      invrefId = invref.getId();
      if (invrefId != null)
        return 1;
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  /**
   * This method is used to insert invoice approver
   * 
   * @param invoice
   * @param comments
   * @param appstatus
   * @param pendingapproval
   * @return
   */
  public static int insertInvoiceApprover(Invoice invoice, String comments, String appstatus,
      String pendingapproval) {
    String histId = null;
    String strSeqNo = "";
    try {

      strSeqNo = String.valueOf(UtilityDAO.getHistorySequence(InvoiceApprovalTable.INVOICE_HISTORY,
          InvoiceApprovalTable.INVOICE_HEADER_COLUMN, invoice.getId()));

      efinpurchaseinapphist appHist = OBProvider.getInstance().get(efinpurchaseinapphist.class);
      appHist.setClient(OBDal.getInstance().get(Client.class, invoice.getClient().getId()));
      appHist.setOrganization(
          OBDal.getInstance().get(Organization.class, invoice.getOrganization().getId()));
      appHist.setActive(true);
      appHist.setUpdatedBy(OBDal.getInstance().get(User.class, invoice.getUpdatedBy().getId()));
      appHist.setCreationDate(new java.util.Date());
      appHist.setCreatedBy(OBDal.getInstance().get(User.class, invoice.getUpdatedBy().getId()));
      appHist.setUpdated(new java.util.Date());
      appHist.setInvoice(OBDal.getInstance().get(Invoice.class, invoice.getId()));
      appHist.setPurchaseaction(appstatus);
      appHist.setApprovedDate(new java.util.Date());
      appHist.setPendingOn(pendingapproval);
      appHist.setComments(comments);
      appHist.setSequenceNumber(Long.parseLong(strSeqNo));
      appHist.setRole(OBContext.getOBContext().getRole());

      OBDal.getInstance().save(appHist);
      log.debug("efinpurchaseinapphist:" + appHist.getId());

      histId = appHist.getId();
      if (histId != null)
        return 1;
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception in insertBudgetApprover: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  /**
   * Method to return whether current role is direct approver or not
   * 
   * @param InvoiceId
   * @param roleId
   * @param userId
   * @return true or false
   */
  public boolean isDirectApproval(String InvoiceId, String roleId, String userId) {
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    Boolean isDirectApproval = Boolean.FALSE;

    try {
      query = "select count(*) from c_invoice inv "
          + "join eut_next_role rl on inv.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "where inv.c_invoice_id = ? and li.ad_role_id = ?";

      ps = conn.prepareStatement(query);
      ps.setString(1, InvoiceId);
      ps.setString(2, roleId);

      rs = ps.executeQuery();

      if (rs.next()) {
        if (rs.getInt("count") > 0) {
          query = "select coalesce(li.ad_user_id,'') as userId from c_invoice inv "
              + "join eut_next_role rl on inv.em_eut_next_role_id = rl.eut_next_role_id "
              + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
              + "where inv.c_invoice_id = ? and li.ad_role_id = ?";
          ps = conn.prepareStatement(query);
          ps.setString(1, InvoiceId);
          ps.setString(2, roleId);
          rs = ps.executeQuery();
          while (rs.next()) {
            if (rs.getString("userId").equals("") || rs.getString("userId").equals(userId)) {
              isDirectApproval = Boolean.TRUE;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return isDirectApproval;
    }
    return isDirectApproval;
  }

}