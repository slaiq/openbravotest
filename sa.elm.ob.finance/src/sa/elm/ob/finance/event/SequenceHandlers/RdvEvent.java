package sa.elm.ob.finance.event.SequenceHandlers;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.session.OBPropertiesProvider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gowtham V
 * 
 */
public class RdvEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(EfinRDV.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(RdvEvent.class);

  public void onSave(@Observes EntityNewEvent event) {
    String AccountDate = "", orgId = "";
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinRDV header = (EfinRDV) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(EfinRDV.PROPERTY_DOCUMENTNO);
      Property totAdvance = entities[0].getProperty(EfinRDV.PROPERTY_TOTADV);
      Property totContRem = entities[0].getProperty(EfinRDV.PROPERTY_CONTRACTAMTREM);
      Property isAdvance = entities[0].getProperty(EfinRDV.PROPERTY_NOADVANCE);

      orgId = header.getOrganization().getId();
      String SequenceNo = "0";

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
      if (header.getSalesOrder() != null) {
        Order objOrder = OBDal.getInstance().get(Order.class, header.getSalesOrder().getId());
        if ("ESCM_WD".equals(objOrder.getEscmAppstatus())
            || "ESCM_OHLD".equals(objOrder.getEscmAppstatus())) {
          throw new OBException(OBMessageUtils.messageBD("EUT_HoldWithdrawnPO"));
        }
      }

      // set document NO.
      OBQuery<Calendar> calendarQuery = null;

      if (header.isWebservice()) {
        calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
            "as e where e.organization.id ='0' and e.client.id = :clientId");
        calendarQuery.setNamedParameter("clientId", OBPropertiesProvider.getInstance()
            .getOpenbravoProperties().getProperty(WebserviceConstants.CLIENT_KEY));
        calendarQuery.setFilterOnReadableClients(false);
        calendarQuery.setFilterOnReadableOrganization(false);

      } else {
        calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
            "as e where e.organization.id ='0'");
      }

      if (calendarQuery.list().size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
      }
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(header.getTXNDate());
      Calendar calendar = calendarQuery.list().get(0);
      if (header.getTXNType().equals("PO")) {
        SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_rdv_mus", calendar.getId(),
            orgId, true);
      } else if (header.getTXNType().equals("POS")) {
        SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_rdv_po", calendar.getId(),
            orgId, true);
      } else if (header.getTXNType().equals("POD")) {
        SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_rdv_dr", calendar.getId(),
            orgId, true);
      }
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
      }
      event.setCurrentState(documentNo, SequenceNo);

      // po is mandatory
      if (header.getTXNType().equals("PO")) {
        if (header.getSalesOrder() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_RDV_Po_Mandatory"));
        } else {
          if (header.getSalesOrder().getEscmOrdertype().equals("PUR")) {
            event.setCurrentState(isAdvance, true);
          }
          // set adv amt
          // event.setCurrentState(totAdvance, totAdv);
          // 'Total Advance' should be same as initial version(0) 'advance payment amount'
          event.setCurrentState(totAdvance, header.getSalesOrder().getEscmAdvpaymntAmt());
          event.setCurrentState(totContRem, header.getSalesOrder().getGrandTotalAmount());
        }
      }
      // receipt is mandatory
      if (!header.getTXNType().equals("PO")) {
        if (header.getGoodsShipment() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_RDV_PoR_Mandatory"));
        }
      }
      // To check Txn date(greg) is valid date format or not
      if (!header.getTxndategre().matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_invalidtxndategreg"));
      }
      // Legacy adv balance should not be greater than legancy total adv paid
      if (header.getLegacyTotaladvPaid() != null && header.getLegacyAdvanceBalance() != null
          && header.getLegacyTotaladvPaid().compareTo(header.getLegacyAdvanceBalance()) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_openadvbal_greater"));

      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Save RDV Header: " + e);
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
      EfinRDV header = (EfinRDV) event.getTargetInstance();

      final Property isAdv = entities[0].getProperty(EfinRDV.PROPERTY_NOADVANCE);
      final Property advAmt = entities[0].getProperty(EfinRDV.PROPERTY_TOTADV);

      if (event.getPreviousState(isAdv) != event.getCurrentState(isAdv)) {
        if (header.isNoadvance()) {
          event.setCurrentState(advAmt, BigDecimal.ZERO);
        }
        if (!header.isNoadvance()) {
          // event.setCurrentState(advAmt, totAdv);
          // 'Total Advance' should be same as initial version(0) 'advance payment amount'
          event.setCurrentState(advAmt, header.getSalesOrder().getEscmAdvpaymntAmt());
        }
      }
      // To check Txn date(greg) is valid date format or not
      if (!header.getTxndategre().matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_invalidtxndategreg"));
      }
      // Legacy adv balance should not be greater than legancy total adv paid
      if (header.getLegacyTotaladvPaid() != null && header.getLegacyAdvanceBalance() != null
          && header.getLegacyTotaladvPaid().compareTo(header.getLegacyAdvanceBalance()) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_openadvbal_greater"));

      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while update RDV Header: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    String AccountDate = "";
    String seqName = null;
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinRDV header = (EfinRDV) event.getTargetInstance();
      if (header.getEfinRDVTxnList() != null && header.getEfinRDVTxnList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Rdv_Header_Del"));
      }

      // update 'next assigned no' with previous no. in document seq while delete the recent
      // record for reusing the document sequence task no - 7409
      if (header.getTXNType().equals("PO")) {
        seqName = Constants.RDV_MUS_DOC_SEQ_NAME;
      } else if (header.getTXNType().equals("POS")) {
        seqName = Constants.RDV_PO_DOC_SEQ_NAME;
      } else if (header.getTXNType().equals("POD")) {
        seqName = Constants.RDV_DR_DOC_SEQ_NAME;
      }
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(header.getTXNDate());
      Utility.setDocumentSequenceAfterDeleteRecord(AccountDate, seqName,
          header.getOrganization().getId(), Long.parseLong(header.getDocumentNo()), null, true);

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while delete RDVheader: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
