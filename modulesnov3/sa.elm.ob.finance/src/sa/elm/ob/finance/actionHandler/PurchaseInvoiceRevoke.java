package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinInvoicePaymentSch;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.dms.service.DMSInvoiceService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSInvoiceServiceImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class PurchaseInvoiceRevoke implements Process {

  private static final Logger log = Logger.getLogger(PurchaseInvoiceRevoke.class);
  private OBError result = new OBError();
  private static final String API_DOCUMENT = "API";
  private static final String PIAPPrepaymentInvoice = "APPI";
  private static final String PIAPPrepaymentApplication = "APPA";
  private static final String PO_DOCUMENT = "POM";
  private static final String RDV_DOCUMENT = "RDV";

  public void execute(ProcessBundle bundle) throws Exception {

    final String invoiceId = (String) bundle.getParams().get("C_Invoice_ID");
    final String comments = (String) bundle.getParams().get("comments");

    Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

    int count = 0;
    try {
      Boolean isValidInvoice = Boolean.TRUE;
      String strInvoiceType = "";
      JSONObject historyData = new JSONObject();
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();

      OBContext.setAdminMode();
      if (!StringUtils.isEmpty(invoice.getId())) {

        isValidInvoice = InvoiceRevokeDAO.checkIfValidInvoice(invoice);
        strInvoiceType = InvoiceRevokeDAO.getInvoiceType(invoice);
        String invType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

        if (!isValidInvoice) {
          result = OBErrorBuilder.buildMessage(null, "error", "@Efin_AlreadyPreocessed@");
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
        // reduce qty in order for POM.
        if (PO_DOCUMENT.equals(invType)) {
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
                  OBInterceptor.setPreventUpdateInfoChange(true);
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

        if (invoice.isEfinIsreserved()) {

          InvoiceRevokeDAO.revertReservedInvoice(invoice);
          invoice.setEfinIsreserved(false);

          // invoice created via the po hold plan
          if (RDV_DOCUMENT.equals(invType) && invoice.isEfinIsreserved()) {
            InvoiceRevokeDAO.releaseTempEncumbrance(invoice);
          }
        }

        InvoiceRevokeDAO.updateInvoiceStatus(invoice);
        historyData = InvoiceRevokeDAO.getHistoryData(invoice, comments);

        if (historyData != null) {
          Utility.InsertApprovalHistory(historyData);
          count++;
        }

        try {
          // DMS integration
          DMSInvoiceService dmsService = new DMSInvoiceServiceImpl();
          dmsService.rejectAndReactivateOperations(invoice);
        } catch (Exception e) {
          log.error("Error while removing record from dms" + e.getMessage());
        }

        // Removing Forward and RMI Id
        if (invoice.getEutForward() != null) {
          forwardDao.setForwardStatusAsDraft(invoice.getEutForward());
          forwardDao.revokeRemoveForwardRmiFromWindows(invoice.getId(), Constants.AP_INVOICE);
        }
        if (invoice.getEutReqmoreinfo() != null) {
          forwardDao.setForwardStatusAsDraft(invoice.getEutReqmoreinfo());
          forwardDao.revokeRemoveRmiFromWindows(invoice.getId(), Constants.AP_INVOICE);
        }

        if (count > 0 && !StringUtils.isEmpty(invoice.getId())) {
          insertAlert(invoice, bundle);
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              strInvoiceType);
        }

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

        result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PurchaseInvoice_Revoke@");
        bundle.setResult(result);
        return;
      }

      bundle.setResult(result);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (

    Exception e) {
      result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * After Revoke, insert alert to last waiting role
   * 
   * @return Success or error
   */

  public static String insertAlert(Invoice invoice, ProcessBundle bundle) {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    final String clientId = (String) bundle.getContext().getClient();

    String alertWindow = sa.elm.ob.finance.util.AlertWindow.PurchaseInvoice;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();

    String Description = "", alertKey = "";

    String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

    Role objCreatedRole = null;

    if (invoice.getCreatedBy().getADUserRolesList().size() > 0) {
      objCreatedRole = invoice.getCreatedBy().getADUserRolesList().get(0).getRole();
    }

    // get alert rule for invoice
    OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
        "as e where e.client.id='" + invoice.getClient().getId() + "' and e.efinProcesstype='"
            + alertWindow + "'");
    if (queryAlertRule.list().size() > 0) {
      AlertRule objRule = queryAlertRule.list().get(0);
      alertRuleId = objRule.getId();
    }

    // remove approval alert
    OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
        "as e where e.referenceSearchKey='" + invoice.getId() + "' and e.alertStatus='NEW'");
    if (alertQuery.list().size() > 0) {
      for (Alert objAlert : alertQuery.list()) {
        objAlert.setAlertStatus("SOLVED");
        OBDal.getInstance().save(objAlert);
      }
    }
    // check and insert alert recipient
    OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(AlertRecipient.class,
        "as e where e.alertRule.id='" + alertRuleId + "'");
    if (receipientQuery.list().size() > 0) {
      for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
        includeRecipient.add(objAlertReceipient.getRole().getId());
        OBDal.getInstance().remove(objAlertReceipient);
      }
    }
    includeRecipient.add(objCreatedRole.getId());
    // avoid duplicate recipient
    HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
    Iterator<String> iterator = incluedSet.iterator();
    while (iterator.hasNext()) {
      AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
    }

    // set the description for alert based on Document type
    if (alertWindow.equals(API_DOCUMENT)) {
      Description = sa.elm.ob.finance.properties.Resource
          .getProperty("finance.purchaseinvoice.revoked", vars.getLanguage()) + " "
          + invoice.getCreatedBy().getName();
      alertKey = "finance.purchaseinvoice.revoked";

    } else if (alertWindow.equals(PIAPPrepaymentInvoice)) {
      Description = sa.elm.ob.finance.properties.Resource
          .getProperty("finance.apprepaymentinvoice.revoked", vars.getLanguage()) + " "
          + invoice.getCreatedBy().getName();
      alertKey = "finance.apprepaymentinvoice.revoked";

    } else if (alertWindow.equals(PIAPPrepaymentApplication)) {
      Description = sa.elm.ob.finance.properties.Resource
          .getProperty("finance.apprepaymentapplication.revoked", vars.getLanguage()) + " "
          + invoice.getCreatedBy().getName();
      alertKey = "finance.apprepaymentapplication.revoked";
    }

    if (invoice.getEutNextRole() != null) {

      // Filtering 'nextRoleList' to avoid duplicate alerts for same role and user.
      ArrayList<EutNextRoleLine> nextRoleList = new ArrayList<EutNextRoleLine>();
      HashSet<String> nextRoleSet = new HashSet<String>();
      Integer initial = 0;
      for (EutNextRoleLine ln : invoice.getEutNextRole().getEutNextRoleLineList()) {
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

      // set revoke alert to last waiting role
      for (EutNextRoleLine objNextRoleLine : nextRoleList) {
        AlertUtility.alertInsertionRole(invoice.getId(), invoice.getDocumentNo(),
            objNextRoleLine.getRole().getId(),
            objNextRoleLine.getUserContact() == null ? ""
                : objNextRoleLine.getUserContact().getId(),
            invoice.getClient().getId(), Description, "NEW", alertWindow, alertKey,
            Constants.GENERIC_TEMPLATE);
      }
    }
    invoice.setEutNextRole(null);
    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    return strInvoiceType;

  }
}
