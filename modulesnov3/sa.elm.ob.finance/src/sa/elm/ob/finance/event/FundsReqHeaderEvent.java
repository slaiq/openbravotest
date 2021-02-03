package sa.elm.ob.finance.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
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

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.utility.util.Utility;

public class FundsReqHeaderEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINFundsReq.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    String AccountDate = "";
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EFINFundsReq fundsreq = (EFINFundsReq) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(EFINFundsReq.PROPERTY_DOCUMENTNO);
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(fundsreq.getAccountingDate());
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
          "as e where e.organization.id ='0'");
      if (calendarQuery.list().size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
      }
      Calendar calendar = calendarQuery.list().get(0);
      String SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_fundsreq",
          calendar.getId(), "0", true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
      }
      event.setCurrentState(documentNo, SequenceNo);
      if (!fundsreq.getEfinBudgetint().getStatus().equals("OP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_budgetint_notOpen"));
      }

      // Future Date not allowed for transaction date
      if (fundsreq.getTrxdate() != null) {
        if (fundsreq.getTrxdate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (fundsreq.getAccountingDate() != null) {
        if (fundsreq.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while creating the FundsReq: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EFINFundsReq fundsreq = (EFINFundsReq) event.getTargetInstance();
      log.debug("status:" + fundsreq.getDocumentStatus());
      if (fundsreq.getDocumentStatus().equals("CO") || fundsreq.getDocumentStatus().equals("WFA")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_CantDelFRMProc"));
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while delete the FundsReq: " + e);
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
      EFINFundsReq fundsreq = (EFINFundsReq) event.getTargetInstance();
      final Property transactiondate = entities[0].getProperty(EFINFundsReq.PROPERTY_TRXDATE);
      final Property accountingdate = entities[0].getProperty(EFINFundsReq.PROPERTY_ACCOUNTINGDATE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // Future Date not allowed for transaction date
      if (fundsreq.getTrxdate() != null & (!event.getCurrentState(transactiondate)
          .equals(event.getPreviousState(transactiondate)))) {
        if (fundsreq.getTrxdate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (fundsreq.getAccountingDate() != null & (!event.getCurrentState(accountingdate)
          .equals(event.getPreviousState(accountingdate)))) {
        if (fundsreq.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating  FundsReq: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
