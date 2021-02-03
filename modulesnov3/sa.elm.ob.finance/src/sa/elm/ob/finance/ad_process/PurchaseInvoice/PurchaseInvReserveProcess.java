package sa.elm.ob.finance.ad_process.PurchaseInvoice;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.PostUtilsDAO;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetActual;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinEncumInvoiceRef;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.finance.efinpurchaseinapphist;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAO;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAOImpl;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.InvoiceApprovalTable;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Divya on 17/08/2016
 */

public class PurchaseInvReserveProcess extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(PurchaseInvReserveProcess.class);
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

    try {
      OBContext.setAdminMode();

      // Declare the Variable

      String sql = "", sql1 = "", sql2 = "", negamtqry = "", errorMessage = "", status = "",
          appstatus = "", encumdoctype = "", pendingapproval = "", linesumqry = "", invlineqry = "";
      PreparedStatement ps = null, ps1 = null, ps2 = null, negamtps = null, deptvalps = null,
          invlinesps = null, linesumps = null, invlineps = null;
      BigDecimal PendingAmt = BigDecimal.ZERO, ReaduceExpAmt = BigDecimal.ZERO,
          grandTotal = BigDecimal.ZERO, conversionrate = BigDecimal.ZERO,
          lineNetAmt = BigDecimal.ZERO, sumLineNetAmt = BigDecimal.ZERO;
      SQLQuery invLineQuery = null;
      ResultSet rs = null, rs1 = null, rs2 = null, negamtrs = null, deptvalrs = null,
          invlinesrs = null, linesumrs = null, invliners = null;
      ConnectionProvider conn = bundle.getConnection();
      final String clientId = bundle.getContext().getClient();
      final String userId = bundle.getContext().getUser();
      String roleId = bundle.getContext().getRole();
      boolean checkException = false, chkUserIsDeptHead = false, reserve = false,
          allowApprove = false, isValid = Boolean.TRUE;
      String uniquecode = null, p_instance_id = null, DeptHeadRoleId = null;
      int count = 0, errorcount = 0;
      String headerId = null, doctype = null, encumtype = null, errorMsg = "", currencyId = null,
          alertRuleId = "", alertWindow = "", Description = "", alertKey = "";
      SimpleDateFormat timeYearFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Organization organization = null;
      Currency currency = null;
      Boolean isCostEncumbrance = Boolean.FALSE;
      Date currentDate = new Date();
      Boolean isAdjustementInvoice = Boolean.FALSE, isReserveRoleCrossed = Boolean.FALSE;
      String DocType = "";
      Boolean isExclusiveTaxInvoice = Boolean.FALSE;
      String createdUserId = null;
      String createdRoleId = null;
      String orgId = null;
      String costCenterId = null;
      //
      String unReserveSql = null;
      List<String> invoiceList = new ArrayList<String>();
      String invoiceId = null;
      EfinBudgetManencumv encumId = null;
      String message = "";

      unReserveSql = "  select  distinct inv.c_invoice_id from c_invoice inv "
          + " join c_doctype doc on doc.c_doctype_id = inv.c_doctypetarget_id "
          + " join c_invoiceline invln on invln.c_invoice_id = inv.c_invoice_id "
          + " join c_validcombination com on com.c_validcombination_id= invln.em_efin_c_validcombination_id "
          + " where docstatus in ('CO','EFIN_WFA')  and em_efin_isreserved='N' "
          + " and com.em_efin_dimensiontype='E'   " + "  order by inv.c_invoice_id ";

      SQLQuery unReserveSqlQry = OBDal.getInstance().getSession().createSQLQuery(unReserveSql);
      if (unReserveSqlQry != null) {
        invoiceList = unReserveSqlQry.list();

        if (invoiceList.size() > 0) {
          for (String inv : invoiceList) {
            Object invId = (Object) inv;
            invoiceId = (String) inv;
            invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
            orgId = invoice.getOrganization().getId();
            costCenterId = invoice.getEfinCSalesregion().getId();

            String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
            isCostEncumbrance = PurchaseInvoiceSubmitUtils.isCostEncumbrance(invoice);
            isAdjustementInvoice = PurchaseInvoiceSubmitUtils.isAdjustmentOnlyInvoice(invoice);
            TaxLineHandlerDAO taxHandler = new TaxLineHandlerImpl();
            boolean allowUpdate = false, allowDelegation = false;

            JSONObject invoiceCheck = new JSONObject();
            MultipleInvoiceLineAgainstPOLineDAO dao = new MultipleInvoiceLineAgainstPOLineDAOImpl();
            Boolean isError = false;

            // if reserve funds role crossed then should not allow to submit.
            if (invoice.getEfinInvoicetypeTxt().equals("PPA")) {
              DocType = DocumentTypeE.PREPAYMENT_APPLICATION.getDocumentTypeCode();
            } else if (invoice.getEfinInvoicetypeTxt().equals("PPI")) {
              DocType = DocumentTypeE.PREPAYMENT.getDocumentTypeCode();
            } else {
              DocType = DocumentTypeE.AP_INVOICE.getDocumentTypeCode();
            }

            // Restrict to submit a purchase invoice from a role, which is not present in the
            // document
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

            if (invoice.getDocumentStatus().equals("DR")) {
              isError = dao.checkAmountValidationForSplit(clientId, invoice);
              if (isError) {

                message += invoice.getDocumentNo() + ",";
                continue;
                // throw new OBException(OBMessageUtils.messageBD("Efin_Amount_Greater"));
              }
            }
            // check if any line with amount zero - TaskNo - 7493
            if (invoice.getDocumentStatus().equals("DR")) {
              isError = dao.checkLineHavingZeroAmt(invoice);
              if (isError) {
                message += invoice.getDocumentNo() + ",";
                continue;
                // throw new OBException(OBMessageUtils.messageBD("EFIN_Line_Amount_Zero"));
              }
            }

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
                  message += invoice.getDocumentNo() + ",";
                  continue;
                  // throw new
                  // OBException(OBMessageUtils.messageBD("Efin_PrepayInvoiceAmount_High"));
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
                  OBQuery<EfinBudgetManencumlines> manualline = OBDal.getInstance().createQuery(
                      EfinBudgetManencumlines.class,
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
                message += invoice.getDocumentNo() + ",";
                continue;
                // throw new OBException(OBMessageUtils.messageBD("Efin_LineNotExistsIn_EncumLine")
                // .replace("@", uniqueCode.replaceFirst(",", "")));
              }
            }

            /* check already reserve is done for particular invoice */
            OBQuery<efinbudgetencum> encumlist = OBDal.getInstance()
                .createQuery(efinbudgetencum.class, " invoice.id='" + invoice.getId() + "'");
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
                message += invoice.getDocumentNo() + ",";
                continue;
                // throw new OBException(status);
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
                    if (rs.getBigDecimal("manEncumamount").compareTo(sumval) < 0) {
                      message += invoice.getDocumentNo() + ",";
                      continue;
                      // throw new OBException("@Efin_PI_GrsAmtExMEAmt@");
                    }

                  }
                }
              }
            }

            OBQuery<InvoiceLine> invlines = OBDal.getInstance().createQuery(InvoiceLine.class,
                " invoice.id='" + invoice.getId() + "'");

            // If lines other than selected advance type
            if (PPI_DOCUMENT.equals(strInvoiceType)) {
              if (invoice.getEfinEncumtype().equals("M")) {
                if (invlines.list().size() > 0) {
                  for (InvoiceLine lines : invlines.list()) {
                    if (lines.getEfinDistributionLines() == null) {
                      message += invoice.getDocumentNo() + ",";
                      continue;
                      // throw new OBException("@Efin_PurInv_APP_ManLine@");
                    }
                  }
                }
              }
            }

            if (!invoice.isEfinIsreserved() && API_DOCUMENT.equals(strInvoiceType)) {
              // check used amount is negative or not
              if (invoice.getEfinEncumtype().equals("M")) {
                if (invlines.list().size() > 0) {
                  for (InvoiceLine lines : invlines.list()) {
                    if (lines.getEfinBudgmanuencumln() != null) {
                      EfinBudgetManencumlines encumlines = OBDal.getInstance().get(
                          EfinBudgetManencumlines.class, lines.getEfinBudgmanuencumln().getId());
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
                          message += invoice.getDocumentNo() + ",";
                          continue;
                          // throw new OBException("@Efin_Manencum_UseamtNeg@");
                        }

                        // check encumbrance remaining amount should not be greater than encumbrance
                        // original amount

                        if ((encumlines.getRemainingAmount().subtract(sumLineNetAmt))
                            .compareTo(encumlines.getRevamount()) > 0) {
                          message += invoice.getDocumentNo() + ",";
                          continue;
                          // throw new OBException("@Efin_ManEnc_RemAmt@");
                        }

                        // check invoice linenetamt is greater than the encumbrance remaining
                        // amount, if
                        // yes throw error
                        if (encumlines.getRemainingAmount().compareTo(sumLineNetAmt) < 0) {
                          String erMsg = OBMessageUtils.messageBD("EFIN_PurInv_ManEncumRemAmt");
                          message += invoice.getDocumentNo() + ",";
                          continue;
                          // throw new OBException(erMsg.replace("%",
                          // encumlines.getAccountingCombination().getEfinUniqueCode()));
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
                message += invoice.getDocumentNo() + ",";
                continue;
                // throw new OBException("@Efin_appliedamount<lineamt@" + strUniqueCode);
              }
            }
            /*
             * if (PPA_DOCUMENT.equals(strInvoiceType)) { BigDecimal grandtotal = BigDecimal.ZERO;
             * if (invoice.getEfinManualencumbrance() != null) { EfinBudgetManencum manecnum =
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
                      message += invoice.getDocumentNo() + ",";
                      continue;
                      // throw new OBException("@Efin_Amount>FA@");
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
                            .map(a -> a.getLineNetAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        sumLineNetAmt = FinanceUtils.getConvertedAmount(sumLineNetAmt,
                            conversionrate);
                        if ((budgline.getEncumbrance().add(sumLineNetAmt))
                            .compareTo(BigDecimal.ZERO) < 0) {
                          message += invoice.getDocumentNo() + ",";
                          continue;
                          // throw new OBException("@Efin_BudgLine_EncumNeg@");
                        }
                      }
                    }
                  }
                }
              }
            }

            if (PO_DOCUMENT.equals(strInvoiceType)) {

              // check new version created in PO
              int versionCount = 0;
              Order order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
              if (order.getEscmBaseOrder() != null) {
                versionCount = order.getEscmBaseOrder().getOrderEMEscmBaseOrderList().size();
              } else {
                versionCount = order.getOrderEMEscmBaseOrderList().size();
              }
              if (order.getEscmRevision() < versionCount) {
                message += invoice.getDocumentNo() + ",";
                continue;
                // throw new OBException("@Efin_New_Po_Created@");
              }

            }

            // task no 7551
            if (invoice.getEfinManualencumbrance() != null) {
              encumId = invoice.getEfinManualencumbrance();
            }
            if (!invoice.isEfinIsreserved()) {
              if ((invoice.getEfinEncumtype().equals("M")
                  && (PPA_DOCUMENT.equals(strInvoiceType) | API_DOCUMENT.equals(strInvoiceType)))
                  || (RDV_DOCUMENT.equals(strInvoiceType) && encumId != null)) {

                /* Apply modification */

                isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
                Boolean hasAmountForTax = Boolean.TRUE;
                if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0 && invoice
                    .getEfinRDVTxnList().get(0).getLineTaxamt().compareTo(BigDecimal.ZERO) == 0) {
                  Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
                  taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);

                  if (!taxLinesMap.isEmpty()) {
                    hasAmountForTax = PurchaseInvoiceSubmitUtils
                        .checkRemainingAmountForTaxEncumbrance(invoice, taxLinesMap, Boolean.TRUE,
                            Boolean.FALSE);

                    if (!hasAmountForTax) {
                      // OBDal.getInstance().rollbackAndClose();
                      // OBError result = OBErrorBuilder.buildMessage(null, "error",
                      // "@EFIN_NoRemainingAmt@");
                      // bundle.setResult(result);
                      // return;
                      message += invoice.getDocumentNo() + ",";
                      continue;
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
                      // OBDal.getInstance().rollbackAndClose();
                      // OBError result = OBErrorBuilder.buildMessage(null, "error",
                      // "@EFIN_NoRemainingAmt@");
                      // bundle.setResult(result);
                      // return;
                      message += invoice.getDocumentNo() + ",";
                      continue;
                    }
                  }
                } else {
                  Boolean hasAmountForTax = PurchaseInvoiceSubmitUtils
                      .checkRemainingAmountForPOMEncumbrance(invoice, Boolean.FALSE);

                  if (!hasAmountForTax) {
                    // OBDal.getInstance().rollbackAndClose();
                    // OBError result = OBErrorBuilder.buildMessage(null, "error",
                    // "@EFIN_NoRemainingAmt@");
                    // bundle.setResult(result);
                    // return;
                    message += invoice.getDocumentNo() + ",";
                    continue;
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

            if (invoice.getGrandTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
              grandTotal = invoice.getGrandTotalAmount().multiply(new BigDecimal(-1));
            } else
              grandTotal = invoice.getGrandTotalAmount();

            // Check whether Funds budget is defined and Funds available is greater than the line
            // net
            // amount for the lines
            PurchaseInvoiceSubmitUtils.preValidation(OBContext.getOBContext().getRole().getId(),
                delegatedFromRole, hasDelegation, doctype, invoice, strInvoiceType, conversionrate,
                null);

            log.debug("reserve:" + reserve);
            reserve = true;
            EfinBudgetManencumlines manualline = null;
            Boolean isValidCombination = Boolean.TRUE;
            Campaign budgetType = PurchaseInvoiceSubmitUtils
                .getBudgetType(invoice.getEfinBudgetType());

            encumId = invoice.getEfinManualencumbrance();
            if (reserve && !invoice.isEfinIsreserved()) {

              if (PPA_DOCUMENT.equals(strInvoiceType)) {
                // check sum of invoice Lines Amount greater than encumbrance remain amount
                if (!invoice.isEfinIsreserved() && PPA_DOCUMENT.equals(strInvoiceType)) {
                  String strUniqueCode = PurchaseInvoiceSubmitUtils.isValidPPA(invoice,
                      conversionrate);
                  if (strUniqueCode.length() > 0) {
                    message += invoice.getDocumentNo() + ",";
                    continue;
                    // throw new OBException("@Efin_appliedamount<lineamt@" + strUniqueCode);
                  }

                  // check sum of applied amount > applied prepayment invoice amount
                  String appliedPrepayment = UtilityDAO.checkAppliedPrepayment(invoiceId,
                      conversionrate);
                  if (!appliedPrepayment.equals("")) {
                    message += invoice.getDocumentNo() + ",";
                    continue;
                    // throw new OBException(OBMessageUtils.messageBD("Efin_Applied_PrepaymentLess")
                    // .replace("@invoice@", appliedPrepayment));
                  }
                  // check sum of applied amount =application invoice total amount
                  String applicationInvAmtcheck = UtilityDAO
                      .checkApplicationInvoiceAmount(invoiceId);
                  if (!applicationInvAmtcheck.equals("1")) {
                    message += invoice.getDocumentNo() + ",";
                    continue;
                    // throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_Equal"));
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
                  isValidCombination = PurchaseInvoiceSubmitUtils
                      .preReservationFundsChecking(invoice, conversionrate);
                  isCost = false;
                  if (!isValidCombination) {
                    // OBDal.getInstance().rollbackAndClose();
                    // OBError result = OBErrorBuilder.buildMessage(null, "error",
                    // "@Efin_Reservation_Nofunds@");
                    // bundle.setResult(result);
                    // return;
                    message += invoice.getDocumentNo() + ",";
                    continue;
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
                      // OBDal.getInstance().rollbackAndClose();
                      // OBError result = OBErrorBuilder.buildMessage(null, "error",
                      // "@EFIN_NoRemainingAmt@");
                      // bundle.setResult(result);
                      // return;
                      message += invoice.getDocumentNo() + ",";
                      continue;
                    }
                  }
                } else {
                  Boolean hasAmountForTax = PurchaseInvoiceSubmitUtils
                      .checkRemainingAmountForPOMEncumbrance(invoice, Boolean.TRUE);

                  if (!hasAmountForTax) {
                    // OBDal.getInstance().rollbackAndClose();
                    // OBError result = OBErrorBuilder.buildMessage(null, "error",
                    // "@EFIN_NoRemainingAmt@");
                    // bundle.setResult(result);
                    // return;
                    message += invoice.getDocumentNo() + ",";
                    continue;
                  }
                }

                if (invLineAmt.compareTo(po.getGrandTotalAmount()) == 0) {
                  // stage move
                  String encumID = invoice.getEfinManualencumbrance().getId();
                  EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      encumID);
                  encum.setEncumStage("AEE");
                  OBDal.getInstance().save(encum);

                  // insert invoice reference in new encum lines.
                  PurchaseInvoiceSubmitUtils.insertEncumbranceInvoiceReference(invoice, encum,
                      isCost, strInvoiceType, conversionrate);

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
                  if (invoice.getEfinManualencumbrance().getEncumbranceMethod().equals("A")
                      && isExclusiveTaxInvoice) {

                    PurchaseInvoiceSubmitUtils.insertModificationForAutoInPoMatch(invoice);

                  }

                  String newPOMencum = PurchaseInvoiceSubmitUtils.splitPoencum(header);
                  EfinBudgetManencum POMencum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      newPOMencum);

                  // insert invoice reference in new encum lines.
                  PurchaseInvoiceSubmitUtils.insertEncumbranceInvoiceReference(invoice, POMencum,
                      isCost, strInvoiceType, conversionrate);

                  // insert invoice reference in new encum lines.
                  // for (EfinBudgetManencumlines encln : POMencum.getEfinBudgetManencumlinesList())
                  // {
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
                    || (RDV_DOCUMENT.equals(strInvoiceType))
                    || (API_DOCUMENT.equals(strInvoiceType))) {
                  isValidCombination = PurchaseInvoiceSubmitUtils
                      .preReservationFundsChecking(invoice, conversionrate);

                  if (!isValidCombination) {
                    // OBDal.getInstance().rollbackAndClose();
                    //
                    // OBError result = OBErrorBuilder.buildMessage(null, "error",
                    // "@Efin_Reservation_Nofunds@");
                    // bundle.setResult(result);
                    // return;
                    message += invoice.getDocumentNo() + ",";
                    continue;
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

                  PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, manual,
                      Boolean.FALSE, strInvoiceType, conversionrate);
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

                /* Apply modification */

                isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);
                Boolean hasAmountForTax = Boolean.TRUE;
                if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0 && invoice
                    .getEfinRDVTxnList().get(0).getLineTaxamt().compareTo(BigDecimal.ZERO) == 0) {
                  Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
                  taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);

                  if (!taxLinesMap.isEmpty()) {
                    hasAmountForTax = PurchaseInvoiceSubmitUtils
                        .checkRemainingAmountForTaxEncumbrance(invoice, taxLinesMap, Boolean.TRUE,
                            Boolean.TRUE);

                    if (hasAmountForTax) {
                      EfinBudgetManencum poEncumbrance = PurchaseInvoiceSubmitUtils
                          .getPoEncumbranceFromInvoice(invoice);
                      PurchaseInvoiceSubmitUtils.addEncumbranceModification(taxLinesMap,
                          poEncumbrance, invoice);
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
                  isValidCombination = PurchaseInvoiceSubmitUtils
                      .preReservationFundsChecking(invoice, conversionrate);
                  if (!isValidCombination) {
                    // OBDal.getInstance().rollbackAndClose();
                    // OBError result = OBErrorBuilder.buildMessage(null, "error",
                    // "@Efin_Reservation_Nofunds@");
                    // bundle.setResult(result);
                    // return;
                    message += invoice.getDocumentNo() + ",";
                    continue;
                  }

                  if (isCostEncumbrance && !isAdjustementInvoice) {
                    Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();

                    fundsEncumbrance = PurchaseInvoiceSubmitUtils.getNewFundsEnumbrance(
                        fundsBudgetType, invoice.getEfinManualencumbrance(), invoice,
                        conversionrate);

                    if (fundsEncumbrance != null) {
                      PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice,
                          fundsEncumbrance, Boolean.TRUE, strInvoiceType, conversionrate);
                    }

                  }
                } else if (PPA_DOCUMENT.equals(strInvoiceType)) {
                  PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice,
                      conversionrate, false);
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
                        PostUtilsDAO.InsertInvoicesInEncumbrance(invoice, encumbranceLine,
                            lineNetAmt, Boolean.FALSE);
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
                 * if (ln.getEfinBudgmanuencumln() != null && (API_DOCUMENT.equals(strInvoiceType)
                 * || RDV_DOCUMENT.equals(strInvoiceType))) { // do the entry in manual encumbrance
                 * line invoice tab PurchaseInvoiceSubmitUtils.InsertInvoicesInEncumbrance(ln,
                 * ln.getEfinBudgmanuencumln(), strInvoiceType, conversionrate);
                 * OBDal.getInstance().flush();
                 * 
                 * if (isCostEncumbrance) { Campaign fundsBudgetType =
                 * PurchaseInvoiceSubmitUtils.getFundsBudgetType();
                 * 
                 * EfinBudgetManencum fundsEncumbrance = PurchaseInvoiceSubmitUtils
                 * .getNewFundsEnumbrance(fundsBudgetType, invoice.getEfinManualencumbrance(),
                 * invoice, conversionrate);
                 * 
                 * if (fundsEncumbrance != null) {
                 * PurchaseInvoiceSubmitUtils.insertAutoEncumbranceLines(invoice, fundsEncumbrance,
                 * Boolean.TRUE, strInvoiceType, conversionrate); } } } else if
                 * (PPA_DOCUMENT.equals(strInvoiceType)) {
                 * PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice,
                 * conversionrate); } if (API_DOCUMENT.equals(strInvoiceType) ||
                 * RDV_DOCUMENT.equals(strInvoiceType)) { encumdoctype = "AEE";
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
                PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(),
                    conversionrate, false);
              } else if ("A".equals(invoice.getEfinEncumtype())
                  && PPA_DOCUMENT.equals(strInvoiceType)) {
                PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionrate,
                    false);
                encumdoctype = "AAE";
                PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(),
                    conversionrate, false);
              }

              if (invoice.getEfinDistribution() != null && PPI_DOCUMENT.equals(strInvoiceType)) {

                grandTotal = FinanceUtils.getConvertedAmount(invoice.getGrandTotalAmount(),
                    conversionrate);

                if (invoice.getEfinEncumtype().equals("M")
                    && invoice.getEfinManualencumbrance() != null) {
                  if (PurchaseInvoiceSubmitUtils.checkRemainingAmount(invoice, grandTotal) == -1) {
                    // OBError result = OBErrorBuilder.buildMessage(null, "Error",
                    // "@Efin_Remamt_lesser@");
                    // bundle.setResult(result);
                    // return;
                    message += invoice.getDocumentNo() + ",";
                    continue;
                  }

                  PurchaseInvoiceSubmitUtils.updateAdvanceEncumbrance(invoice, grandTotal);
                } else {
                  Campaign fundsBudgetType = PurchaseInvoiceSubmitUtils.getFundsBudgetType();
                  EfinBudgetManencum encumbrance = PurchaseInvoiceSubmitUtils
                      .getNewFundsEnumbrance(fundsBudgetType, null, invoice, conversionrate);
                  EfinBudgetInquiry budgetinq = null;
                  List<InvoiceLine> invoiceLineList = invoice.getInvoiceLineList();
                  for (InvoiceLine invLine : invoiceLineList) {
                    budgetinq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                        invLine.getEfinExpenseAccount(), invoice.getEfinBudgetint());
                    if (budgetinq == null) {
                      // OBError result = OBErrorBuilder.buildMessage(null, "Error",
                      // "@Efin_Reservation_Nofunds@");
                      // bundle.setResult(result);
                      // return;
                      message += invoice.getDocumentNo() + ",";
                      continue;
                    } else {
                      BigDecimal totalLineAmt = invoiceLineList.stream()
                          .filter(a -> a.getEfinExpenseAccount() == invLine.getEfinExpenseAccount())
                          .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                      if (budgetinq.getFundsAvailable().compareTo(totalLineAmt) < 0) {
                        // OBError result = OBErrorBuilder.buildMessage(null, "Error",
                        // "@Efin_Reservation_Nofunds@");
                        // bundle.setResult(result);
                        // return;
                        message += invoice.getDocumentNo() + ",";
                        continue;
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
          }
        }
      }
      if (message != null) {
        message = "Process completed Successfully.some of the invoice cant able to process,Invoice No :"
            + message;
      } else {
        message = " Process completed Successfully";
      }
      OBError result = OBErrorBuilder.buildMessage(null, "success", message);
      bundle.setResult(result);
      return;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

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
      e.printStackTrace();
      return isDirectApproval;
    }
    return isDirectApproval;
  }
}