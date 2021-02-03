package sa.elm.ob.finance.ad_process.PurchaseInvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinInvoicePaymentSch;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.EfinPrepaymentInvoice;
import sa.elm.ob.finance.actionHandler.InvoiceRevokeDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.finance.dms.service.DMSInvoiceService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSInvoiceServiceImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class PurchaseInvoiceRework implements Process {

  private static final Logger log = Logger.getLogger(PurchaseInvoiceRework.class);
  private final OBError obError = new OBError();
  private static final String API_DOCUMENT = "API";
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";
  private static final String RDV_DOCUMENT = "RDV";
  private static final String PO_DOCUMENT = "POM";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("rework the budget");

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    final String invoiceId = (String) bundle.getParams().get("C_Invoice_ID");
    final String clientId = bundle.getContext().getClient();
    ConnectionProvider conn = bundle.getConnection();
    String comments = bundle.getParams().get("comments").toString();
    Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
    final String orgId = invoice.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    Currency currency = null;
    BigDecimal grandTotal = BigDecimal.ZERO, conversionRate = BigDecimal.ZERO;
    boolean errorFlag = true, allowReject = false;
    String doctype = "", strInvoiceType = "", alertRuleId = "", alertWindow = "", description = "",
        alertKey = "", encumid = "";
    int count = 0;
    boolean checkEncumbranceAmountZero = false;
    boolean checkEncumbranceAmountZerofunds = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = invoice.getEutForward();
    List<EfinManualEncumInvoice> reservedInvoices = new ArrayList<EfinManualEncumInvoice>();
    ArrayList<String> includeRecipient = new ArrayList<String>();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String createdUserId = null;
    String createdRoleId = null;
    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);
          String strEncumbranceId = "";

          if ("DR".equals(invoice.getDocumentStatus())
              || "EFIN_CA".equals(invoice.getDocumentStatus())) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AlreadyPreocessed@");
            bundle.setResult(result);
            return;
          }
          // revert payment schedule changes
          if (invoice.getEFINInvoicePaymentSchList().size() > 0) {
            for (EfinInvoicePaymentSch invoicePayObj : invoice.getEFINInvoicePaymentSchList()) {
              BigDecimal invoiceAmount = invoicePayObj.getInvoiceAmount();
              // get respective PO payment Schedule
              ESCMPaymentSchedule poPaySchObj = invoicePayObj.getEscmPaymentSchedule();
              poPaySchObj.setInvoicedAmt(poPaySchObj.getInvoicedAmt().subtract(invoiceAmount));
              OBDal.getInstance().save(poPaySchObj);
              invoicePayObj
                  .setInvoiceAmount(invoicePayObj.getInvoiceAmount().subtract(invoiceAmount));
              invoicePayObj.setUniquecodeamount(BigDecimal.ZERO);
              invoicePayObj.setDistributionamount(BigDecimal.ZERO);
            }
          }
          strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

          if (API_DOCUMENT.equals(strInvoiceType) || RDV_DOCUMENT.equals(strInvoiceType)
              || PO_DOCUMENT.equals(strInvoiceType)) {
            doctype = Resource.AP_INVOICE_RULE;
            alertWindow = AlertWindow.PurchaseInvoice;
          } else if (PPI_DOCUMENT.equals(strInvoiceType)) {
            doctype = Resource.AP_Prepayment_Inv_RULE;
            alertWindow = AlertWindow.PIAPPrepaymentInvoice;
          } else if (PPA_DOCUMENT.equals(strInvoiceType)) {
            doctype = Resource.AP_Prepayment_App_RULE;
            alertWindow = AlertWindow.PIAPPrepaymentApplication;
          }

          if (invoice.getEutForward() != null) {
            allowReject = forwardReqMoreInfoDAO.allowApproveReject(invoice.getEutForward(), userId,
                roleId, doctype);
          }
          if (invoice.getEutReqmoreinfo() != null
              || ((invoice.getEutForward() != null) && (!allowReject))) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }
          if (invoice.getEutForward() != null) {
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(invoice.getEutForward());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(invoice.getId(),
                Constants.AP_INVOICE);
          }
          if (invoice.getEutReqmoreinfo() != null) {
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(invoice.getEutReqmoreinfo());
            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(invoice.getId(), Constants.AP_INVOICE);
          }
          OBDal.getInstance().save(invoice);
          OBDal.getInstance().flush();

          if (invoice.getInvoiceLineList().size() > 0) {
            Invoice header = OBDal.getInstance().get(Invoice.class, invoice.getId());

            // get old nextrole line user and role list
            HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                .getNextRoleLineList(header.getEutNextRole(), doctype);

            header.setUpdated(new java.util.Date());
            header.setUpdatedBy(OBContext.getOBContext().getUser());
            header.setDocumentStatus("DR");
            header.setEutNextRole(null);
            header.setEfinNextapprovers(null);
            header.setEfinDocaction("CO");

            OBDal.getInstance().save(header);
            OBDal.getInstance().flush();

            // get alert rule id
            OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
                "as e where e.client.id= :clientID and e.efinProcesstype= :efinProcesstype ");
            queryAlertRule.setNamedParameter("clientID", clientId);
            queryAlertRule.setNamedParameter("efinProcesstype", alertWindow);
            List<AlertRule> queryAlertRuleList = queryAlertRule.list();
            if (queryAlertRuleList.size() > 0) {
              AlertRule objRule = queryAlertRuleList.get(0);
              alertRuleId = objRule.getId();
            }

            // Alert process Start
            Role objCreatedRole = null;
            if (header.getCreatedBy().getADUserRolesList().size() > 0) {
              if (header.getEfinAdRole() != null)
                objCreatedRole = header.getEfinAdRole();
              else
                objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            log.debug("objCreatedRole:" + objCreatedRole);
            // check and insert alert recipient
            OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
                .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRuleID");
            receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
            List<AlertRecipient> receipientQueryList = receipientQuery.list();
            if (receipientQueryList.size() > 0) {
              for (AlertRecipient objAlertReceipient : receipientQueryList) {
                includeRecipient.add(objAlertReceipient.getRole().getId());
                OBDal.getInstance().remove(objAlertReceipient);
              }
            }

            if (objCreatedRole != null)
              includeRecipient.add(objCreatedRole.getId());
            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
            }

            forwardReqMoreInfoDAO.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId,
                objUser, clientId, Constants.REJECT, header.getDocumentNo(), vars.getLanguage(),
                vars.getRole(), forwardObj, doctype, alertReceiversMap);

            // delete alert for approval alerts
            // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            // "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'");
            //
            // if (alertQuery.list().size() > 0) {
            // for (Alert objAlert : alertQuery.list()) {
            // objAlert.setAlertStatus("SOLVED");
            // }
            // }

            // set the description for alert based on Document type
            if (alertWindow.equals("API")) {
              description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.purchaseinvoice.rejected", vars.getLanguage()) + " " + objUser.getName();
              alertKey = "finance.purchaseinvoice.rejected";

            } else if (alertWindow.equals("APPI")) {
              description = sa.elm.ob.finance.properties.Resource
                  .getProperty("finance.apprepaymentinvoice.rejected", vars.getLanguage()) + " "
                  + objUser.getName();
              alertKey = "finance.purchaseinvoice.rejected";

            } else if (alertWindow.equals("APPA")) {
              description = sa.elm.ob.finance.properties.Resource
                  .getProperty("finance.apprepaymentapplication.rejected", vars.getLanguage()) + " "
                  + objUser.getName();
              alertKey = "finance.apprepaymentapplication.rejected";
            }

            log.debug("Description:" + description);
            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), doctype);

            JSONObject SubmitterDetailJson = Utility.getSubmitterDetail(alertWindow,
                header.getId());
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
                header.getClient().getId(), description, "NEW", alertWindow, alertKey,
                Constants.GENERIC_TEMPLATE);
            // Alert Process end

            // DocumentRuleDAO.deleteUnusedNextRoles(conn.getConnection(), doctype);

            // GET PARENT ORG CURRENCY
            currency = FinanceUtils.getCurrency(orgId, invoice);
            // get conversion rate
            conversionRate = FinanceUtils.getConversionRate(conn.getConnection(), orgId, invoice,
                currency);

            // reduce qty in order for POM.
            if (PO_DOCUMENT.equals(strInvoiceType)) {
              OBInterceptor.setPreventUpdateInfoChange(true);

              Order order = null;
              if (invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")) {
                for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
                  if (invLine.isEfinIspom()) {
                    if (invoice.getEfinCOrder() != null)
                      order = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
                    OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                        invLine.getSalesOrderLine().getId());
                    if (!invLine.isEFINIsTaxLine() || (invLine.isEFINIsTaxLine() && order != null
                        && ((order.isEscmIstax() && invoice.isEfinIstax())
                            || (!order.isEscmIstax() && invoice.isEfinIstax()
                                && invoice.getEfinTaxMethod().isPriceIncludesTax())))) {
                      ordLine.setEfinAmtinvoiced(
                          ordLine.getEfinAmtinvoiced().subtract(invLine.getEfinAmtinvoiced()));
                    }
                    OBDal.getInstance().save(ordLine);
                  }
                }
              } else {
                for (InvoiceLine invLine : invoice.getInvoiceLineList()) {
                  if (invLine.isEfinIspom() && !invLine.isEFINIsTaxLine()) {
                    OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                        invLine.getSalesOrderLine().getId());
                    ordLine.setInvoicedQuantity(
                        ordLine.getInvoicedQuantity().subtract(invLine.getInvoicedQuantity()));
                    OBDal.getInstance().save(ordLine);
                  }
                }
              }
            }

            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);

            // remove if reserve was done.
            if (PO_DOCUMENT.equals(strInvoiceType)) {
              invoice.setEfinEncumbranceType("POE");
              OBDal.getInstance().save(invoice);

              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);
              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {

                  OBDal.getInstance().remove(reservedInvoice);
                }
                OBDal.getInstance().flush();
                // check Budget type of invoice is cost, if so delete funds encumbrance

                if ("C".equals(invoice.getEfinBudgetType())) {

                  if (invoice.getEfinFundsEncumbrance() != null) {
                    EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();
                    fundsEncum.setDocumentStatus("DR");

                    // Check Encumbrance Amount is Zero Or Negative
                    if (invoice.getEfinFundsEncumbrance() != null)
                      encumLinelist = invoice.getEfinFundsEncumbrance()
                          .getEfinBudgetManencumlinesList();
                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZerofunds = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (invoice.getEfinManualencumbrance() != null) {
                      EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                          invoice.getEfinManualencumbrance().getId());
                      encumLinelist = encum.getEfinBudgetManencumlinesList();
                    }

                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZero = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (checkEncumbranceAmountZero) {
                      OBDal.getInstance().rollbackAndClose();
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@ESCM_Encumamt_Neg@");
                      bundle.setResult(result);
                      return;
                    }
                    // empty the reference in invoice for funds
                    invoice.setEfinFundsEncumbrance(null);
                    OBDal.getInstance().save(invoice);
                    OBDal.getInstance().remove(fundsEncum);

                  }

                }

                if (invoice.getEfinManualencumbrance() != null) {

                  BigDecimal invLineAmt = invoice.getInvoiceLineList().stream()
                      .filter(a -> a.isEfinIspom() == true).map(a -> a.getLineNetAmount())
                      .reduce(BigDecimal.ZERO, BigDecimal::add);

                  Order po = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());

                  // Task 7847: PO to PO Match Invoice not creating split encumbrance
                  // If Invoice encumbrance is different from Order encumbrance, then split case
                  boolean isSplitEncumbrance = false;
                  if (invoice.getEfinManualencumbrance() != null
                      && invoice.getEfinCOrder() != null) {
                    isSplitEncumbrance = PurchaseInvoiceSubmitUtils.isEncumbranceDifferent(
                        invoice.getEfinManualencumbrance().getId(),
                        invoice.getEfinCOrder().getId());
                  }

                  if (invLineAmt.compareTo(po.getGrandTotalAmount()) != 0 || isSplitEncumbrance) {

                    // splitted enum
                    encumid = invoice.getEfinManualencumbrance().getId();
                    EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
                        encumid);

                    if (encumbrance != null) {
                      OBQuery<InvoiceLine> invLine = OBDal.getInstance().createQuery(
                          InvoiceLine.class,
                          "invoice.id= :invoiceID and efinBudgmanuencumln.id is not null ");
                      invLine.setNamedParameter("invoiceID", invoice.getId());
                      List<InvoiceLine> invlineList = invLine.list();
                      if (invlineList != null && invlineList.size() > 0) {
                        for (InvoiceLine lineinv : invlineList) {
                          lineinv.setEfinBudgmanuencumln(null);
                          OBDal.getInstance().save(lineinv);
                        }
                      }
                      OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
                          .createQuery(EfinBudManencumRev.class, " as e where e.sRCManencumline.id "
                              + " in ( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id= :encumID)");
                      revQuery.setNamedParameter("encumID", encumbrance.getId());
                      List<EfinBudManencumRev> revQueryList = revQuery.list();
                      if (revQueryList.size() > 0) {
                        for (EfinBudManencumRev rev : revQueryList) {
                          EfinBudgetManencumlines srclines = rev.getSRCManencumline();
                          rev.setSRCManencumline(null);
                          OBDal.getInstance().save(rev);
                          EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
                          /**
                           * in case if we are taking amt from remaining amt when we dont have
                           * enought amt in app amt then while revoke have to give the amt back to
                           * remaining amt
                           **/
                          Order latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(po);
                          /** encumbrance lines updated amt **/
                          BigDecimal encuUpdateAmt = lines.getSystemUpdatedAmt();
                          /** order total line net amt based on encumbrance line uniquecode **/
                          BigDecimal grandTotalAmt = latestOrder.getOrderLineList().stream()
                              .filter(
                                  a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                                      && a.getEFINUniqueCode().getId()
                                          .equals(lines.getAccountingCombination().getId()))
                              .map(a -> a.getLineNetAmount())
                              .reduce(BigDecimal.ZERO, BigDecimal::add);

                          // Get grand total of other POs linked with the same encumbrance
                          BigDecimal otherPoTotal = InvoiceRevokeDAO.getGrandTotalOtherPO(
                              latestOrder, lines.getAccountingCombination().getId());

                          /**
                           * find the differenct amt which we manually added in modification or
                           * remaining amt
                           **/
                          BigDecimal incAmt = encuUpdateAmt
                              .subtract(grandTotalAmt.add(otherPoTotal));
                          /**
                           * diff amt(enc update amt - po amt) greater than zero and po encumbrance
                           * is manual and diff amt not presented in remaining amt place then have
                           * to give the amt back to in the place of remaining
                           **/
                          if (incAmt.compareTo(BigDecimal.ZERO) > 0
                              && incAmt.compareTo(lines.getRemainingAmount()) != 0
                              && po.getEfinBudgetManencum() != null
                              && po.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                            /** subtract remaining amt in the difference **/
                            incAmt = incAmt.subtract(lines.getRemainingAmount());
                            /**
                             * diff amt compare with reduce amt if greater than and equal add reduce
                             * amt itself in remaining amt not to update the app amt
                             **/
                            if (incAmt.compareTo(rev.getRevamount().negate()) >= 0) {
                              lines.setRemainingAmount(
                                  lines.getRemainingAmount().add(rev.getRevamount().negate()));
                            }
                            /**
                             * diff amt compare with reduce amt if lesser than add diff amt in
                             * remaining amt and update app amt also
                             **/
                            else if (incAmt.compareTo(rev.getRevamount().negate()) < 0) {
                              // BigDecimal updRemaAmt =
                              // incAmt.subtract(lines.getRemainingAmount());
                              lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                              lines.setAPPAmt(lines.getAPPAmt()
                                  .add(rev.getRevamount().negate().subtract(incAmt)));
                            }
                          }
                          /** other cases **/
                          else {
                            lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
                          }
                          // lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
                          lines.getEfinBudManencumRevList().remove(rev);
                          encumbrance.getEfinBudgetManencumlinesList().remove(srclines);
                        }
                        if (po.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                          PurchaseInvoiceSubmitUtils.removePoMatchExtraBudgetAmount(
                              po.getEfinBudgetManencum(), invoice, null);
                        }
                      }
                      // event.setCurrentState(propencum, null);
                      EfinBudgetManencumv POEncumbrance = Utility
                          .getObject(EfinBudgetManencumv.class, po.getEfinBudgetManencum().getId());
                      invoice.setEfinManualencumbrance(POEncumbrance);
                      invoice.setEfinEncumtype(POEncumbrance.getEncumbranceMethod());
                      invoice.setEfinEncumbranceType(POEncumbrance.getEncumbranceType());
                      OBDal.getInstance().save(invoice);
                      encumbrance.setDocumentStatus("DR");
                      OBDal.getInstance().remove(encumbrance);
                    }

                  } else {
                    // stage alone revert it.
                    encumid = invoice.getEfinManualencumbrance().getId();
                    EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                        encumid);
                    encum.setEncumStage("POE");
                    OBDal.getInstance().save(encum);

                    // update the encumbrance line id is null
                    for (InvoiceLine line : invoice.getInvoiceLineList()) {
                      line.setEfinBudgmanuencumln(null);
                      OBDal.getInstance().save(line);
                    }
                  }
                }
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().refresh(invoice);
            }

            if ((invoice.getEfinEncumtype().equals("M")
                && (PPA_DOCUMENT.equals(strInvoiceType) | API_DOCUMENT.equals(strInvoiceType)))
                || (RDV_DOCUMENT.equals(strInvoiceType)
                    && invoice.getEfinManualencumbrance() != null
                    && !invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {

              TaxLineHandlerDAO taxHandler = new TaxLineHandlerImpl();

              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);

              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {

                  OBDal.getInstance().remove(reservedInvoice);
                }

                // check Budget type of invoice is cost, if so delete funds encumbrance

                if ("C".equals(invoice.getEfinBudgetType())) {

                  if (invoice.getEfinFundsEncumbrance() != null) {
                    EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();
                    fundsEncum.setDocumentStatus("DR");
                    OBDal.getInstance().save(fundsEncum);
                    OBDal.getInstance().flush();

                    // Check Encumbrance Amount is Zero Or Negative
                    if (invoice.getEfinFundsEncumbrance() != null)
                      encumLinelist = invoice.getEfinFundsEncumbrance()
                          .getEfinBudgetManencumlinesList();
                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZerofunds = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (invoice.getEfinManualencumbrance() != null) {
                      EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                          invoice.getEfinManualencumbrance().getId());
                      encumLinelist = encum.getEfinBudgetManencumlinesList();
                    }

                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZero = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (checkEncumbranceAmountZero) {
                      OBDal.getInstance().rollbackAndClose();
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@ESCM_Encumamt_Neg@");
                      bundle.setResult(result);
                      return;
                    }
                    // empty the reference in invoice for funds
                    invoice.setEfinFundsEncumbrance(null);
                    OBDal.getInstance().save(invoice);
                    OBDal.getInstance().remove(fundsEncum);

                  }
                }

                if (RDV_DOCUMENT.equals(strInvoiceType)) {
                  String encum = invoice.getEfinManualencumbrance().getManualEncumbrance();
                  EfinBudgetManencum manEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      encum);
                  manEncum.setEncumStage("MUS");

                  // Add modification to nullify the already created modification in case of
                  // ministry tax invoice

                  Boolean isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);

                  if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0 && invoice
                      .getEfinRDVTxnList().get(0).getLineTaxamt().compareTo(BigDecimal.ZERO) == 0) {
                    Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
                    taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);

                    if (!taxLinesMap.isEmpty()) {

                      EfinBudgetManencum poEncumbrance = PurchaseInvoiceSubmitUtils
                          .getPoEncumbranceFromInvoice(invoice);
                      PurchaseInvoiceSubmitUtils.addEncumbranceModification(taxLinesMap,
                          poEncumbrance, invoice, Boolean.TRUE);

                      // revert the extra amount which is taken from budget enquiry for tax in po
                      // encum.
                      if (poEncumbrance.getEncumMethod().equals("A")) {
                        PurchaseInvoiceSubmitUtils.removePoExtraAmount(poEncumbrance, invoice,
                            false, null);
                      } else {
                        for (Entry<String, BigDecimal> taxEntries : taxLinesMap.entrySet()) {
                          String strUniqueCodeId = taxEntries.getKey();
                          BigDecimal taxAmount = taxEntries.getValue().setScale(2,
                              RoundingMode.HALF_UP);

                          EfinBudgetManencumlines lines = PurchaseInvoiceSubmitUtils
                              .getEncumbranceLine(poEncumbrance.getId(), strUniqueCodeId);

                          /**
                           * in case if we are taking amt from remaining amt when we dont have
                           * enought amt in app amt then while revoke have to give the amt back to
                           * remaining amt
                           **/
                          Order invoicePO = OBDal.getInstance().get(Order.class,
                              invoice.getSalesOrder().getId());
                          Order latestOrder = PurchaseInvoiceSubmitUtils
                              .getLatestOrderComplete(invoicePO);
                          /** encumbrance lines updated amt **/
                          BigDecimal encuUpdateAmt = lines.getSystemUpdatedAmt();

                          /** order total line net amt based on encumbrance line uniquecode **/
                          BigDecimal grandTotalAmt = latestOrder.getOrderLineList().stream()
                              .filter(
                                  a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                                      && a.getEFINUniqueCode().getId()
                                          .equals(lines.getAccountingCombination().getId()))
                              .map(a -> a.getLineNetAmount())
                              .reduce(BigDecimal.ZERO, BigDecimal::add);

                          // Get grand total of other POs linked with the same encumbrance
                          BigDecimal otherPoTotal = InvoiceRevokeDAO.getGrandTotalOtherPO(
                              latestOrder, lines.getAccountingCombination().getId());
                          /**
                           * find the differenct amt which we manually added in modification or
                           * remaining amt
                           **/
                          BigDecimal incAmt = encuUpdateAmt
                              .subtract(grandTotalAmt.add(otherPoTotal));
                          /**
                           * diff amt(enc update amt - po amt) greater than zero and po encumbrance
                           * is manual and diff amt not presented in remaining amt place then have
                           * to give the amt back to in the place of remaining
                           **/
                          if (incAmt.compareTo(BigDecimal.ZERO) > 0
                              && incAmt.compareTo(lines.getRemainingAmount()) != 0
                              && poEncumbrance != null
                              && poEncumbrance.getEncumMethod().equals("M")) {

                            /** subtract remaining amt in the difference **/
                            incAmt = incAmt.subtract(lines.getRemainingAmount());
                            /**
                             * diff amt compare with tax amt if greater than and equal add reduce
                             * amt itself in remaining amt not to update the app amt
                             **/
                            if (incAmt.compareTo(taxAmount) >= 0) {
                              lines.setRemainingAmount(lines.getRemainingAmount().add(taxAmount));
                              lines.setAPPAmt(lines.getAPPAmt().subtract(taxAmount));
                            }
                            /**
                             * diff amt compare with tax amt if lesser than add diff amt in
                             * remaining amt and update app amt also
                             **/
                            else if (incAmt.compareTo(taxAmount) < 0) {
                              // BigDecimal updRemaAmt =
                              // incAmt.subtract(lines.getRemainingAmount());
                              lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                              lines.setAPPAmt((lines.getAPPAmt().subtract(taxAmount))
                                  .add(taxAmount.subtract(incAmt)));
                            }
                          }
                        }
                      }
                      OBDal.getInstance().flush();
                      OBDal.getInstance().refresh(invoice);
                    }
                  }
                  OBDal.getInstance().save(manEncum);
                  OBDal.getInstance().flush();
                }
              }
            }
            if ((invoice.getEfinEncumtype().equals("A") && API_DOCUMENT.equals(strInvoiceType))
                || (RDV_DOCUMENT.equals(strInvoiceType)
                    && invoice.getEfinManualencumbrance() != null
                    && invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {
              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);
              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {
                  OBDal.getInstance().remove(reservedInvoice);
                }
              }

              if (invoice.isEfinIsreserved() && invoice.getEfinManualencumbrance() != null) {
                strEncumbranceId = invoice.getEfinManualencumbrance().getId();
                EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
                    strEncumbranceId);
                encumbrance.setDocumentStatus("DR");
                OBDal.getInstance().save(encumbrance);

                if (encumbrance != null) {
                  for (EfinBudgetManencumlines manencumlines : encumbrance
                      .getEfinBudgetManencumlinesList()) {
                    OBDal.getInstance().remove(manencumlines);
                  }

                }

                if (invoice.getEfinFundsEncumbrance() != null) {
                  String strfundEncumbranceId = invoice.getEfinFundsEncumbrance().getId();
                  EfinBudgetManencum fundEncumbrance = Utility.getObject(EfinBudgetManencum.class,
                      strfundEncumbranceId);
                  fundEncumbrance.setDocumentStatus("DR");
                  OBDal.getInstance().save(fundEncumbrance);

                  if (fundEncumbrance != null) {

                    for (EfinBudgetManencumlines manencumlines : fundEncumbrance
                        .getEfinBudgetManencumlinesList()) {
                      OBDal.getInstance().remove(manencumlines);
                    }
                  }
                  // Check Encumbrance Amount is Zero Or Negative
                  if (invoice.getEfinFundsEncumbrance() != null)
                    encumLinelist = invoice.getEfinFundsEncumbrance()
                        .getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZerofunds = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (invoice.getEfinManualencumbrance() != null) {
                    EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                        invoice.getEfinManualencumbrance().getId());
                    encumLinelist = encum.getEfinBudgetManencumlinesList();
                  }

                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  invoice.setEfinFundsEncumbrance(null);
                  OBDal.getInstance().save(invoice);
                  OBDal.getInstance().remove(fundEncumbrance);

                }
                invoice.setEfinManualencumbrance(null);
                OBDal.getInstance().save(invoice);
                OBDal.getInstance().remove(encumbrance);
                OBDal.getInstance().flush();
              }
            }

            // invoice created via the po hold plan
            if (RDV_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
              InvoiceRevokeDAO.releaseTempEncumbrance(invoice);
            }

            if (invoice.getEfinManualencumbrance() != null && invoice.getEfinDistribution() != null
                && PPI_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
              grandTotal = invoice.getGrandTotalAmount();
              grandTotal = FinanceUtils.getConvertedAmount(grandTotal, conversionRate);

              reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoiceId);

              OBQuery<EfinPrepaymentInvoice> prepayinv = OBDal.getInstance().createQuery(
                  EfinPrepaymentInvoice.class,
                  " manualEncumbrance.id in ( select inv.efinManualencumbrance.id from  Invoice inv  where inv.id= :invoiceID ) ");
              prepayinv.setNamedParameter("invoiceID", invoice.getId());
              List<EfinPrepaymentInvoice> prepayinvList = prepayinv.list();
              if (prepayinvList.size() > 0) {
                for (EfinPrepaymentInvoice pre : prepayinvList) {
                  OBDal.getInstance().remove(pre);
                }
              }

              if (reservedInvoices.size() > 0) {
                for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {
                  OBDal.getInstance().remove(reservedInvoice);
                }
              }

              if ("A".equals(invoice.getEfinEncumtype())) {
                strEncumbranceId = invoice.getEfinManualencumbrance().getId();
                EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
                    strEncumbranceId);
                encumbrance.setDocumentStatus("DR");

                if (encumbrance != null) {
                  for (EfinBudgetManencumlines manencumlines : encumbrance
                      .getEfinBudgetManencumlinesList()) {
                    OBDal.getInstance().remove(manencumlines);
                  }
                }
                // Check Encumbrance Amount is Zero Or Negative
                if (invoice.getEfinFundsEncumbrance() != null)
                  encumLinelist = invoice.getEfinFundsEncumbrance()
                      .getEfinBudgetManencumlinesList();
                if (encumLinelist.size() > 0)
                  checkEncumbranceAmountZerofunds = UtilityDAO
                      .checkEncumbranceAmountZero(encumLinelist);

                if (invoice.getEfinManualencumbrance() != null) {
                  EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      invoice.getEfinManualencumbrance().getId());
                  encumLinelist = encum.getEfinBudgetManencumlinesList();
                }

                if (encumLinelist.size() > 0)
                  checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

                if (checkEncumbranceAmountZero) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_Encumamt_Neg@");
                  bundle.setResult(result);
                  return;
                }
                invoice.setEfinManualencumbrance(null);
                OBDal.getInstance().save(invoice);
                OBDal.getInstance().remove(encumbrance);
              }

              OBDal.getInstance().flush();
            }

            if (PPA_DOCUMENT.equals(strInvoiceType) && invoice.isEfinIsreserved()) {
              if (invoice.getEfinEncumtype().equals("M")) {
                PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionRate,
                    true);
                PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(),
                    conversionRate, true);

              } else {
                PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionRate,
                    true);
                PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(),
                    conversionRate, true);
              }

            }
            if (invoice.isEfinIsreserved()) {
              invoice.setEfinIsreserved(false);
            }

            // insert a record in ApprovalHistory
            count = PurchaseInvoiceSubmit.insertInvoiceApprover(invoice, comments, "REW", null);
            // Check Encumbrance Amount is Zero Or Negative
            if (invoice.getEfinFundsEncumbrance() != null)
              encumLinelist = invoice.getEfinFundsEncumbrance().getEfinBudgetManencumlinesList();
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (invoice.getEfinManualencumbrance() != null) {
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  invoice.getEfinManualencumbrance().getId());
              encumLinelist = encum.getEfinBudgetManencumlinesList();
            }
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (checkEncumbranceAmountZero) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
              bundle.setResult(result);
              return;
            }
            if (count > 0 && !StringUtils.isEmpty(header.getId())
                && header.getDocumentStatus().equals("DR")) {

              try {
                // DMS integration
                DMSInvoiceService dmsService = new DMSInvoiceServiceImpl();
                dmsService.rejectAndReactivateOperations(invoice);
              } catch (Exception e) {

              }

              OBDal.getInstance().commitAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PurInvRework@");
              bundle.setResult(result);
              return;
            }
          }
        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);
          e.printStackTrace();
          OBDal.getInstance().rollbackAndClose();

        }
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
