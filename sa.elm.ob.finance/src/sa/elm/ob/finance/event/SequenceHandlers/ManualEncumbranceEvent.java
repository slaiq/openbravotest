package sa.elm.ob.finance.event.SequenceHandlers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
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
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 12/08/2016
 * 
 */
public class ManualEncumbranceEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetManencum.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(ManualEncumbranceEvent.class);

  public void onSave(@Observes EntityNewEvent event) {
    String AccountDate = "";

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EfinBudgetManencum Encumbrance = (EfinBudgetManencum) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(EfinBudgetManencum.PROPERTY_DOCUMENTNO);
      LOG.info("date>>" + Encumbrance.getAccountingDate());
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      boolean supplierMandatory = false;
      String SequenceNo = "";

      // set document NO.
      OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
          "as e where e.organization.id ='0'");
      if (calendarQuery.list().size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
      }
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(Encumbrance.getAccountingDate());
      Calendar calendar = calendarQuery.list().get(0);
      // User created sequence no
      if (Encumbrance.getEncumMethod().equals("M")) {
        SequenceNo = Utility.getDocumentSequence(AccountDate, "Efin_Budget_Manencum_User",
            calendar.getId(), Encumbrance.getOrganization().getId(), true);
        if (SequenceNo.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
        }

        event.setCurrentState(documentNo, SequenceNo);
      }
      // System created sequence no
      else {
        SequenceNo = Utility.getDocumentSequence(AccountDate, "Efin_Budget_Manencum",
            calendar.getId(), Encumbrance.getOrganization().getId(), true);
        if (SequenceNo.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
        }

        event.setCurrentState(documentNo, SequenceNo);
      }

      // Future Date not allowed for transaction date
      if (Encumbrance.getTransactionDate() != null) {
        if (Encumbrance.getTransactionDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (Encumbrance.getAccountingDate() != null) {
        if (Encumbrance.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }
      // Supplier is mandatory if encumbrance type - Supplier Mandatory is 'yes'
      if (Encumbrance.getEncumType() != null) {
        OBQuery<EfinEncControl> enccontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            "as e where e.encumbranceType='" + Encumbrance.getEncumType() + "' and e.client.id='"
                + OBContext.getOBContext().getCurrentClient().getId() + "'");
        if (enccontrol.list().size() > 0
            && enccontrol.list().get(0).isSupplierMandatory() != null) {
          supplierMandatory = enccontrol.list().get(0).isSupplierMandatory();
          if (supplierMandatory) {
            if (Encumbrance.getBusinessPartner() == null) {
              throw new OBException(
                  OBMessageUtils.messageBD("Efin_Supplier_Mandatory_For_EncumType"));
            }
          }
        }
      }
      // To check whether next assigned document number already exists in encumbrance
      if (StringUtils.isNotEmpty(SequenceNo)) {
        List<EfinBudgetManencum> encumbranceList = null;
        OBQuery<EfinBudgetManencum> encumbrance = OBDal.getInstance()
            .createQuery(EfinBudgetManencum.class, " as e where e.documentNo =:documentNo");
        encumbrance.setNamedParameter("documentNo", SequenceNo);
        encumbranceList = encumbrance.list();
        if (encumbranceList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_ENCUM_DOCNO_EXISTS"));
        }
      }

    } catch (OBException e) {
      LOG.error(" Exception while updating Encumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Save ManualEncumbrance: " + e);
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
      OBContext.setAdminMode();
      EfinBudgetManencum Encumbrance = (EfinBudgetManencum) event.getTargetInstance();
      final Property transactiondate = entities[0]
          .getProperty(EfinBudgetManencum.PROPERTY_TRANSACTIONDATE);
      final Property accountingdate = entities[0]
          .getProperty(EfinBudgetManencum.PROPERTY_ACCOUNTINGDATE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      boolean supplierMandatory = false;

      // Future Date not allowed for transaction date
      if (Encumbrance.getTransactionDate() != null & (!event.getCurrentState(transactiondate)
          .equals(event.getPreviousState(transactiondate)))) {
        if (Encumbrance.getTransactionDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (Encumbrance.getAccountingDate() != null & (!event.getCurrentState(accountingdate)
          .equals(event.getPreviousState(accountingdate)))) {
        if (Encumbrance.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }

      // Supplier is mandatory if encumbrance type - Supplier Mandatory is 'yes'
      if (Encumbrance.getEncumType() != null) {
        OBQuery<EfinEncControl> enccontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            "as e where e.encumbranceType='" + Encumbrance.getEncumType() + "' and e.client.id='"
                + OBContext.getOBContext().getCurrentClient().getId() + "'");
        if (enccontrol.list().size() > 0
            && enccontrol.list().get(0).isSupplierMandatory() != null) {
          supplierMandatory = enccontrol.list().get(0).isSupplierMandatory();
          if (supplierMandatory) {
            if (Encumbrance.getBusinessPartner() == null) {
              throw new OBException(
                  OBMessageUtils.messageBD("Efin_Supplier_Mandatory_For_EncumType"));
            }
          }
        }
      }

    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
      OBContext.setAdminMode();
      EfinBudgetManencum Encumbrance = (EfinBudgetManencum) event.getTargetInstance();
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(Encumbrance.getAccountingDate());
      // update 'next assigned no' with previous no. in document seq while delete the recent record
      // for reusing the document sequence- task no - 7409
      Utility.setDocumentSequenceAfterDeleteRecord(AccountDate,
          Constants.ENCUMBRANCE_MANUAL_DOC_SEQ_NAME, Encumbrance.getOrganization().getId(),
          Long.parseLong(Encumbrance.getDocumentNo()), null, true);

    } catch (Exception e) {
      LOG.error(" Exception while deleting Encumbrance: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
