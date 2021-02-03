package sa.elm.ob.finance.event.SequenceHandlers;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.calendar.Period;

import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class InvoiceEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());
  private static String DRAFT = "DR";

  public void onSave(@Observes EntityNewEvent event) {
    String AccountDate = "";
    String CalendarId = "";
    String OrgId = "";

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      Invoice invoice = (Invoice) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(Invoice.PROPERTY_DOCUMENTNO);
      OrgId = invoice.getOrganization().getId();

      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(invoice.getAccountingDate());

      Organization org = null;
      org = OBDal.getInstance().get(Organization.class, invoice.getOrganization().getId());

      if (org.getCalendar() != null) {
        CalendarId = org.getCalendar().getId();

      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery("select eut_parent_org ('"
            + invoice.getOrganization().getId() + "','" + invoice.getClient().getId() + "')");
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
      if (!invoice.isSalesTransaction()) {

        String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "GS", CalendarId, OrgId,
            true);
        if (SequenceNo.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
        }
        // check document number already exists in invoice
        Boolean isDocumentNoExists = PurchaseInvoiceSubmitUtils.checkInvoiceDocumentNo(SequenceNo);
        if (isDocumentNoExists) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Invoice_Document_Exists"));
        }
        event.setCurrentState(documentNo, SequenceNo);

      }

      if (invoice.getPriceList() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_No_PriceList"));
      }

      // FOR PO Match Transction order is mandatory
      if (invoice.isSalesTransaction() != null && !invoice.isSalesTransaction() && invoice.getEfinInvoicetypeTxt() != null && invoice.getEfinInvoicetypeTxt().equals("POM")) {
        if (invoice.getEfinCOrder() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Order_POM_Mandatory"));
        }
      }

      // check customer type is not null for sadad integration
      if (invoice.isSalesTransaction() && invoice.getEfinCustomertype() == null
          && invoice.getDocumentType().isEfinIssaddad()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_CustomerIdnotEmpty"));
      }

    } catch (Exception e) {
      log.error(" Exception while Delete Account in Budget Type: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      TaxLineHandlerDAO dao = new TaxLineHandlerImpl();

      Invoice invoice = (Invoice) event.getTargetInstance();
      Property accountingDate = entities[0].getProperty(Invoice.PROPERTY_ACCOUNTINGDATE);
      Property bPartner = entities[0].getProperty(Invoice.PROPERTY_BUSINESSPARTNER);
      Property order = entities[0].getProperty(Invoice.PROPERTY_EFINCORDER);
      Property statusProperty = entities[0].getProperty(Invoice.PROPERTY_DOCUMENTSTATUS);
      Property attachProperty = entities[0].getProperty(Invoice.PROPERTY_EUTATTACHPATH);

      if (invoice.isEfinCreatedfromsadad() != null && invoice.isEfinCreatedfromsadad()) {
        return;
      }

      if (event.getCurrentState(attachProperty) != event.getPreviousState(attachProperty)) {
        return;
      }
      List<Period> oldDatePeriod = null, newDatePeriod = null;

      String oldYear = "", currentYear = "";
      String calender = Utility.getCalendar(invoice.getOrganization().getId());
      Boolean hasTaxLines = dao.hasTaxLines(invoice);
      // should not allow to update Bpartner if document is PrePaymentInvoice.

      if (!invoice.isSalesTransaction()
          && (invoice.getEfinInvoicetypeTxt() != null
              && invoice.getEfinInvoicetypeTxt().equals("PPI"))
          && event.getPreviousState(bPartner) != event.getCurrentState(bPartner)) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Cant_Change_Bp_PPI"));
      }
      // should not allow to update accounting date above the boundary line of year.
      oldDatePeriod = Utility.getPeriodList(event.getPreviousState(accountingDate), calender);

      if (oldDatePeriod != null && oldDatePeriod.size() > 0) {
        oldYear = oldDatePeriod.get(0).getYear().getId();
      } else {
        throw new OBException(OBMessageUtils.messageBD("Efin_Period_NotDefined_Exst"));
      }

      newDatePeriod = Utility.getPeriodList(invoice.getAccountingDate(), calender);
      if (newDatePeriod != null && newDatePeriod.size() > 0) {
        currentYear = newDatePeriod.get(0).getYear().getId();
      } else {
        throw new OBException(OBMessageUtils.messageBD("Efin_Period_NotDefined_Current"));
      }

      if (!oldYear.equals(currentYear)) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Acctdate_CanntChange"));
      }

      // if line is present should not allow to change order.
      if (!invoice.isSalesTransaction() && (invoice.getEfinInvoicetypeTxt() != null
          && invoice.getEfinInvoicetypeTxt().equals("POM"))) {
        if (event.getPreviousState(order) != event.getCurrentState(order)
            && invoice.getInvoiceLineList().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Inv_OrdChange"));
        }
      }

      // FOR PO Match Transction order is mandatory
      if (!invoice.isSalesTransaction() && (invoice.getEfinInvoicetypeTxt() != null
          && invoice.getEfinInvoicetypeTxt().equals("POM"))) {
        if (invoice.getEfinCOrder() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Order_POM_Mandatory"));
        }

        if (invoice.isEfinIstax() && DRAFT.equals(invoice.getDocumentStatus())
            && event.getPreviousState(statusProperty) == null) {

          if (hasTaxLines) {
            throw new OBException("Efin_POLines");
          }
        }
      }

      // check customer type is not null for sadad integration
      if (invoice.isSalesTransaction() && invoice.getEfinCustomertype() == null
          && invoice.getDocumentType().isEfinIssaddad()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_CustomerIdnotEmpty"));
      }

    } catch (OBException e) {
      log.error(" Exception while updating record in GL Jouranl Line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    String AccountDate = "";
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      Invoice invoice = (Invoice) event.getTargetInstance();

      if (invoice.isEfinIsrdv()) {

        // processed or not.
        if (invoice.isProcessed()) {
          throw new OBException(OBMessageUtils.messageBD("20501"));
        }

        // if next version is created and current version have advance then should not allow to
        // delete.
        // List<EfinRDVTxnline> rdvTxnList = invoice.getEfinRdvtxn().getEfinRDVTxnlineList();
        // for (EfinRDVTxnline rdvTxnln : rdvTxnList) {
        // if (rdvTxnln.isAdvance()) {
        // long version = rdvTxnln.getEfinRdvtxn().getTXNVersion();
        // if (version == 0) {
        // version = version + 1;
        // }
        // if ((rdvTxnln.getEfinRdv().getEfinRDVTxnList().size() > version)) {
        // throw new OBException(OBMessageUtils.messageBD("Efin_Have_Adv_Deduction_Del"));
        // }
        // }
        // }

        // because of multiple draft version below condition is removed.

        // if next version is created and trying to delete invoice for previous version should not
        // allow to
        // delete.

        /*
         * long version = invoice.getEfinRdvtxn().getTXNVersion(); OBQuery<EfinRDVTransaction>
         * trxVerListQry = OBDal.getInstance().createQuery( EfinRDVTransaction.class,
         * " as e where e.efinRdv.id=:rdvId order by e.tXNVersion desc ");
         * 
         * trxVerListQry.setNamedParameter("rdvId", invoice.getEfinRdvtxn().getEfinRdv().getId());
         * trxVerListQry.setMaxResult(1); if (trxVerListQry.list().size() > 0) { long maxVersion =
         * trxVerListQry.list().get(0).getTXNVersion(); if (maxVersion > version) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_Cannot_Delete_Invoice"));
         * 
         * } }
         */

        if (invoice.getEfinRdvtxn() != null && invoice.getEfinRdvtxn().getEfinRdv() != null) {
          OBQuery<EfinRDVTransaction> line = OBDal.getInstance().createQuery(
              EfinRDVTransaction.class,
              " as e where e.efinRdv.id='" + invoice.getEfinRdvtxn().getEfinRdv().getId() + "' "
                  + " and e.tXNVersion > " + invoice.getEfinRdvtxn().getTXNVersion()
                  + " and e.isadvancetransaction='N' and e.txnverStatus<>'DR' ");
          if (line.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Cannot_Delete_Invoice"));
          }
        }

        // if current version have penalty and applied penalty release in further verison then
        // should not allow.
        EfinRDVTransaction rdvTxn = invoice.getEfinRdvtxn();
        OBQuery<EfinPenaltyAction> penalty = OBDal.getInstance()
            .createQuery(EfinPenaltyAction.class, "efinRdvtxnline.efinRdvtxn.id=:rdvTxnId");
        penalty.setNamedParameter("rdvTxnId", rdvTxn.getId());
        List<EfinPenaltyAction> penaltyList = penalty.list();
        for (EfinPenaltyAction penaltyRel : penaltyList) {
          if (penaltyRel.getEfinPenaltyActionPenaltyRelIDList().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Have_PenaltyRelease"));
          }
        }

      }
      // update 'next assigned no' with previous no. in document seq while delete the recent record
      // for reusing the document sequence- task no - 7409
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(invoice.getAccountingDate());
      Utility.setDocumentSequenceAfterDeleteRecord(AccountDate, null,
          invoice.getOrganization().getId(), Long.parseLong(invoice.getDocumentNo()), "GS", true);

    } catch (Exception e) {
      log.error(" Exception while delete invoice: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
