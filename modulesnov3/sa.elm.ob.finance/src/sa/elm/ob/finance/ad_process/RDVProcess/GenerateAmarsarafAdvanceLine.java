package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.GenerateAmarsarafDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Divya J
 *
 */

public class GenerateAmarsarafAdvanceLine extends DalBaseProcess {

  /**
   * Generate amarsaraf from rdv for each version.
   */
  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(GenerateAmarsarafAdvanceLine.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      AccountingCombination uniqueCode = null;

      String txnLineId = (String) bundle.getParams().get("Efin_Rdvtxnline_ID");
      EfinRDVTxnline transactionLine = OBDal.getInstance().get(EfinRDVTxnline.class, txnLineId);
      FIN_PaymentMethod payMethod = null;
      PaymentTerm payTerm = null;
      PriceList prcList = null;
      DocumentType docType = null;
      boolean isExpense = false;
      EfinRDVTransaction version = transactionLine.getEfinRdvtxn();
      String txnVersion = version.getId();
      EfinRDV rdv = OBDal.getInstance().get(EfinRDV.class, version.getEfinRdv().getId());
      String ClientId = rdv.getClient().getId();
      String CalendarId = "";
      String AccountDate = "";

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
      if (transactionLine.getEfinRdv().getSalesOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class,
            transactionLine.getEfinRdv().getSalesOrder().getId());
        if ("ESCM_WD".equals(order.getEscmAppstatus())
            || "ESCM_OHLD".equals(order.getEscmAppstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_HoldWithdrawnPO@");
          bundle.setResult(result);
          return;
        }
      }

      List<Invoice> invoiceList = new ArrayList<Invoice>();

      // have document type
      OBQuery<DocumentType> transactionDoc = OBDal.getInstance().createQuery(DocumentType.class,
          "as e where e.efinIsrdvinv = 'Y'");
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
      // is sum of line amt have > 0
      if (transactionLine.getNetmatchAmt().compareTo(BigDecimal.ZERO) <= 0
          && transactionLine.getMatchAmt().compareTo(BigDecimal.ZERO) <= 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RdvVer_Netamt@");
        bundle.setResult(result);
        return;
      }

      // check already invoice is generated or not -- need to add line id also
      // OBQuery<Invoice> invoice = OBDal.getInstance().createQuery(Invoice.class,
      // "as e where e.efinRdvtxn.id=:rdvId ");
      // invoice.setNamedParameter("rdvId", version.getId());
      // invoiceList = invoice.list();
      //
      // if (invoiceList.size() > 0) {
      // Invoice invoiceObj = invoiceList.get(0);
      // if (invoiceObj != null && invoiceObj.getDocumentStatus() != null
      // && !invoiceObj.getDocumentStatus().equals("EFIN_CA")) {
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // "@Efin_AlreadyInvoiced_Amarsaraf@");
      // bundle.setResult(result);
      // return;
      // }
      // }

      // get smaple uniqcode to get dept and budgettype for invoice.
      if (!rdv.getTXNType().equals("POD")) {
        OBQuery<OrderLine> acct = OBDal.getInstance().createQuery(OrderLine.class,
            " escmIssummarylevel ='N' and eFINUniqueCode is not null and salesOrder.id = '"
                + rdv.getSalesOrder().getId() + "'");
        uniqueCode = acct.list().get(0).getEFINUniqueCode();

      }

      // if (!rdv.getTXNType().equals("POD")) {
      // Boolean hasAppliedAmount = GenerateAmarsarafDAO.checkEncumbranceAppliedAmount(version);
      //
      // if (!hasAppliedAmount) {
      // OBError result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_RDV_NoAppliedAmt@");
      // bundle.setResult(result);
      // return;
      // }
      // }

      /* Assign the Bundle Parameters */
      final String orgId = transactionLine.getOrganization().getId();
      BigDecimal invTotal = BigDecimal.ZERO;
      Invoice newinvoice = null;
      Organization org = null;
      org = OBDal.getInstance().get(Organization.class, transactionLine.getOrganization().getId());
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

      if (transactionLine.getOrganization().getCalendar() != null) {
        CalendarId = transactionLine.getOrganization().getCalendar().getId();

      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession()
            .createSQLQuery("select eut_parent_org ('" + transactionLine.getOrganization().getId()
                + "','" + transactionLine.getClient().getId() + "')");
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
      newinvoice = GenerateAmarsarafDAO.insertInvoiceLineLevel(uniqueCode, transactionLine, rdv,
          orgId, rdv.getTXNType(), payMethod, payTerm, prcList, docType);

      for (InvoiceLine invLine : newinvoice.getInvoiceLineList()) {
        invTotal = invTotal.add(invLine.getLineNetAmount());
        if (invLine.getEfinCValidcombination().getEfinDimensiontype().equals("E")) {
          // chk at least one expense code is present or not.
          isExpense = true;
        }
      }
      // generate encumbrance for rdv if not direct recipt.
      if (!rdv.getTXNType().equals("POD") && rdv.getManualEncumbrance() != null && isExpense) {
        if (invTotal.compareTo(rdv.getSalesOrder().getGrandTotalAmount()) != 0) {
          // split
          GenerateAmarsarafDAO.insertEncum(newinvoice, uniqueCode, version, rdv, orgId);
        } else {
          // stage move
          GenerateAmarsarafDAO.encumStageMove(newinvoice, rdv);
        }
      }
      transactionLine.setAmarsaraf(true);
      transactionLine.setTxnverStatus("INV");
      transactionLine.setInvoice(newinvoice);
      // version.set
      OBDal.getInstance().save(transactionLine);

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Success@");
      bundle.setResult(result);
      // Creating alert for rdv manager
      GenerateAmarsarafDAO.insertAlertforRDVManager(orgId, ClientId, vars, version.getInvoice(),
          version);
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
}
