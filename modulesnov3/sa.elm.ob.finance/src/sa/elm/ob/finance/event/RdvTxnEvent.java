package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.management.Query;

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
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gowtham V
 * 
 */
public class RdvTxnEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinRDVTransaction.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(RdvTxnEvent.class);
  BigDecimal qty = BigDecimal.ZERO, verQty = BigDecimal.ZERO, delQty = BigDecimal.ZERO;
  BigDecimal newqty = BigDecimal.ZERO;
  BigDecimal diff = BigDecimal.ZERO;
  Query sqlQuery1 = null, sqlQuery2 = null;
  String query = null, query1 = null, product = "";
  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  Date now = new Date();
  boolean fullyUsed = true;

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinRDVTransaction Txn = (EfinRDVTransaction) event.getTargetInstance();
      final Property refbutton = entities[0].getProperty(EfinRDVTransaction.PROPERTY_REFBUTTON);
      final Property advDocNO = entities[0]
          .getProperty(EfinRDVTransaction.PROPERTY_ADVANCESEQUENCE);

      Date todaydate = dateFormat.parse(dateFormat.format(now));
      // if the rdv version is 1 ,then the version date should be after po receipt date
      /*
       * if (!Txn.isAdvancetransaction()) { if (Txn.getTXNVersion() == 1) { Order objOrder =
       * Txn.getEfinRdv().getSalesOrder(); if (objOrder != null) { // find receipts associated for
       * this order Boolean isNotValid = isNotValidVersionDate(objOrder, Txn); // check any receipt
       * that has after date against version date if (isNotValid) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Version_Date_Vs_Receipt_Date")); } } } else { //
       * find previous receipt Long long_previous_number = Txn.getTXNVersion() - 1;
       * EfinRDVTransaction obj_previous_version = getPrevoiousVersion(Txn.getEfinRdv(),
       * long_previous_number); if (obj_previous_version != null &&
       * Txn.getTxnverDate().compareTo(obj_previous_version.getTxnverDate()) < 0) { throw new
       * OBException( OBMessageUtils.messageBD("Efin_Version_Date_Vs_LastVersion_Date")); } } }
       */
      // should not allow future date.
      if (Txn.getCertificateDate().compareTo(todaydate) > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Certificatedate_Fut"));
      }
      event.setCurrentState(refbutton, true);

      // while saving advance check whether rdv already has normal transactions
      // only one advance is allowed
      if (Txn.isAdvancetransaction()) {

        if (!Txn.getEfinRdv().getTXNType().equals("PO")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_rdvadvforonlypotype"));
        }
        // dont allow to add advance transsaction if legacy advance deduction is there
        if (Txn.getEfinRdv().getLegacyAdvanceBalance() != null
            && Txn.getEfinRdv().getLegacyAdvanceBalance().compareTo(BigDecimal.ZERO) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_RDVAdvanceCantIfLegacyExists"));
        }

        List<EfinRDVTransaction> txnList = Txn.getEfinRdv().getEfinRDVTxnList().stream()
            .filter(a -> !a.isAdvancetransaction()).collect(Collectors.toList());
        if (txnList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_rdvalreadyhavetrx"));
        }

        List<EfinRDVTransaction> advancetxnList = Txn.getEfinRdv().getEfinRDVTxnList().stream()
            .filter(a -> a.isAdvancetransaction()).collect(Collectors.toList());
        if (advancetxnList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_rdvonlyoneadvisallowed"));
        }

        // set document NO.
        OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
            "as e where e.organization.id ='0'");
        if (calendarQuery.list().size() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
        }
        String accountDate = new SimpleDateFormat("dd-MM-yyyy").format(Txn.getCertificateDate());
        Calendar calendar = calendarQuery.list().get(0);
        String SequenceNo = Utility.getDocumentSequence(accountDate, "efin_rdv_advance",
            calendar.getId(), Txn.getOrganization().getId(), true);

        if (SequenceNo.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
        } else {
          event.setCurrentState(advDocNO, SequenceNo);
        }
      }

      // multiple draft version allowed so commented this validation.
      // should not allow to insert new version until
      /*
       * OBQuery<EfinRDVTransaction> transaction = OBDal.getInstance().createQuery(
       * EfinRDVTransaction.class, " efinRdv.id='" + Txn.getEfinRdv().getId() +
       * "' and txnverStatus not in ('INV','PD')"); if (transaction.list() != null &&
       * transaction.list().size() > 0) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Rdv_Ver")); }
       */

      // should not allow to insert new version until
      OBQuery<EfinRDVTxnline> rdvtxnLineQry = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
          " efinRdv.id='" + Txn.getEfinRdv().getId()
              + "' and isadvance='Y'   and txnverStatus not in ('INV','PD')");
      if (rdvtxnLineQry.list() != null && rdvtxnLineQry.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Rdv_Ver"));
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while insert RDVtxn: " + e);
      }
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
      OBContext.setAdminMode(true);
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      final Property holdAmt = entities[0].getProperty(EfinRDVTransaction.PROPERTY_HOLDAMOUNT);
      final Property versionDate = entities[0].getProperty(EfinRDVTransaction.PROPERTY_TXNVERDATE);

      Date taxEffectiveFrom = new SimpleDateFormat("yyyy-MM-dd").parse("2020-07-01");
      Date orderStartDate = new SimpleDateFormat("yyyy-MM-dd").parse("2020-05-11");
      Date orderEndDate = new SimpleDateFormat("yyyy-MM-dd").parse("2020-06-30");
      EfinRDVTransaction Txn = (EfinRDVTransaction) event.getTargetInstance();
      // RDV date Validation
      // if the rdv version is 1 ,then the version date should be after po receipt date
      /*
       * if (!event.getCurrentState(versionDate).equals(event.getPreviousState(versionDate))) { if
       * (!Txn.isAdvancetransaction()) { if (Txn.getTXNVersion() == 1) { Order objOrder =
       * Txn.getEfinRdv().getSalesOrder(); if (objOrder != null) { // find receipts associated for
       * this order Boolean isNotValid = isNotValidVersionDate(objOrder, Txn); // check any receipt
       * that has after date against version date if (isNotValid) { throw new OBException(
       * OBMessageUtils.messageBD("Efin_Version_Date_Vs_Receipt_Date")); } } } else { // find
       * previous receipt Long long_previous_number = Txn.getTXNVersion() - 1; EfinRDVTransaction
       * obj_previous_version = getPrevoiousVersion(Txn.getEfinRdv(), long_previous_number); if
       * (Txn.getTxnverDate().compareTo(obj_previous_version.getTxnverDate()) < 0) { throw new
       * OBException( OBMessageUtils.messageBD("Efin_Version_Date_Vs_LastVersion_Date")); } } } }
       */

      if (Txn.isAdvancetransaction()
          && !event.getCurrentState(versionDate).equals(event.getPreviousState(versionDate))
          && Txn.getNetmatchAmt().compareTo(BigDecimal.ZERO) > 0) {
        if (Txn.getEfinRdv() != null && Txn.getEfinRdv().getSalesOrder() != null) {
          Order ord = Txn.getEfinRdv().getSalesOrder();
          Order latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(ord);
          Date oldVersionDate = (Date) event.getPreviousState(versionDate);
          Date newVersionDate = (Date) event.getCurrentState(versionDate);
          if (latestOrder.getEscmSignaturedate() != null
              ? latestOrder.getEscmSignaturedate().compareTo(orderStartDate) >= 0
              : latestOrder.getOrderDate().compareTo(orderStartDate) >= 0
                  && latestOrder.getEscmSignaturedate() != null
                      ? latestOrder.getEscmSignaturedate().compareTo(orderEndDate) <= 0
                      : latestOrder.getOrderDate().compareTo(orderEndDate) <= 0) {
            // if old version date is after/equal to july 1 then new version date should not beofre
            // july 1
            // or old version date is before july 1 then new version date should not after/equal to
            // july 1
            if ((oldVersionDate.compareTo(taxEffectiveFrom) >= 0
                && newVersionDate.compareTo(taxEffectiveFrom) < 0)
                || (oldVersionDate.compareTo(taxEffectiveFrom) < 0
                    && newVersionDate.compareTo(taxEffectiveFrom) >= 0)) {
              throw new OBException(OBMessageUtils.messageBD("EFIN_VersionDateCantChange"));
            }
          }
        }
      }

      // should not allow future date.
      if (Txn.getCertificateDate().compareTo(todaydate) > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Certificatedate_Fut"));
      }

      // Validation for hold Amount
      if (event.getPreviousState(holdAmt) != event.getCurrentState(holdAmt)) {
        if ((Txn.getMatchAmt().subtract(Txn.getPenaltyAmt()).subtract(Txn.getADVDeduct()))
            .compareTo(Txn.getHoldamount()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_holdAmt_greater"));
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while update RDVtxn: " + e);
      }
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
      boolean isparent = false;
      EfinRDVTransaction Txn = (EfinRDVTransaction) event.getTargetInstance();

      if (!Txn.getTxnverStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Txn_Delete"));
      }

      for (EfinRDVTxnline txnLine : Txn.getEfinRDVTxnlineList()) {
        if (txnLine.getEfinParent() != null) {
          txnLine.setEfinParent(null);
          OBDal.getInstance().save(txnLine);
          isparent = true;
        }
      }
      if (isparent) {
        OBDal.getInstance().flush();
      }
      // Should not allow to delete inprogress and approved record.
      if (!(Txn.getAppstatus().equals("DR") || Txn.getAppstatus().equals("REJ"))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_RDVDel_App"));
      }

      // update 'next assigned no' with previous no. in document seq while delete the recent
      // record for reusing the document sequence task no - 7409
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(Txn.getCertificateDate());
      if (Txn.getAdvancesequence() != null) {
        Utility.setDocumentSequenceAfterDeleteRecord(AccountDate,
            Constants.RDV_ADVANCE_DOC_SEQ_NAME, Txn.getOrganization().getId(),
            Long.parseLong(Txn.getAdvancesequence()), null, true);
      }
      /*
       * if (Txn.isWebservice()) { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_WebService")); }
       */

      if (Txn.getEfinRDVTxnlineList().size() > 0) {
        for (EfinRDVTxnline lineObj : Txn.getEfinRDVTxnlineList()) {
          for (EfinRdvHoldAction holdActObj : lineObj.getEfinRdvHoldActionList()) {
            if (holdActObj.getEfinRdvBudgholdline() != null || (holdActObj.getRDVHoldRel() != null
                && holdActObj.getRDVHoldRel().getEfinRdvBudgholdline() != null)) {
              throw new OBException(OBMessageUtils.messageBD("Efin_RDVHoldPlanNotAllowToDel"));
            }
          }
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while delete RDVtxn: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to fetch receipts associated with order
   * 
   * @param objOrder
   * @return List of associated receipt details
   */
  private Boolean isNotValidVersionDate(Order objOrder, EfinRDVTransaction rdvTrxn) {

    Boolean isNotValid = false;
    try {

      OBContext.setAdminMode();
      OBQuery<ShipmentInOut> obQry = OBDal.getInstance().createQuery(ShipmentInOut.class,
          "as e where e.salesOrder.id = :salesOrderId");
      obQry.setNamedParameter("salesOrderId", objOrder.getId());
      if (obQry.list().size() > 0) {
        for (ShipmentInOut receipt : obQry.list()) {
          if (receipt.getMovementDate().compareTo(rdvTrxn.getTxnverDate()) > 0) {
            isNotValid = true;
          }
          if (isNotValid)
            break;
        }

      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while fetch the receipt list against order in the event: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isNotValid;
  }

  /**
   * Method to fetch previous version rdv for the passing rdv version
   * 
   * @param efinRdv
   * @param long_previous_number
   * @return previous version details
   */
  private EfinRDVTransaction getPrevoiousVersion(EfinRDV efinRdv, Long long_previous_number) {
    EfinRDVTransaction prev_version_object = null;
    try {
      OBContext.setAdminMode();

      OBQuery<EfinRDVTransaction> prev_version_object_qry = OBDal.getInstance().createQuery(
          EfinRDVTransaction.class,
          "as e where e.tXNVersion = :previousversion and e.efinRdv.id = :rdvid");
      prev_version_object_qry.setNamedParameter("previousversion", long_previous_number);
      prev_version_object_qry.setNamedParameter("rdvid", efinRdv.getId());
      if (prev_version_object_qry.list().size() > 0) {
        prev_version_object = prev_version_object_qry.list().get(0);
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while fetch  RDVtxn previos version in the event: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return prev_version_object;
  }

}
