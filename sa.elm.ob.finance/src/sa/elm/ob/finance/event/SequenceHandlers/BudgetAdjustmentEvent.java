package sa.elm.ob.finance.event.SequenceHandlers;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.calendar.Calendar;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 15/09/2017
 * 
 */
public class BudgetAdjustmentEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(BudgetAdjustment.ENTITY_NAME) };

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
      OBContext.setAdminMode(true);
      BudgetAdjustment obj_adjustment = (BudgetAdjustment) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(BudgetAdjustment.PROPERTY_DOCNO);
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(obj_adjustment.getAccountingDate());
      OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
          "as e where e.organization.id ='0'");
      List<Calendar> calendarQueryList = calendarQuery.list();
      if (calendarQueryList.size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
      }
      Calendar calendar = calendarQueryList.get(0);
      String SequenceNo = Utility.getDocumentSequence(AccountDate, "Efin_Budgetadj",
          calendar.getId(), "0", true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
      }
      if (!obj_adjustment.getEfinBudgetint().getStatus().equals("OP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_budgetint_notOpen"));
      }
      event.setCurrentState(documentNo, SequenceNo);
    } catch (Exception e) {
      log.error(" Exception while Save BudgetRevision" + e, e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      BudgetAdjustment obj_adjustment = (BudgetAdjustment) event.getTargetInstance();
      // List<BudgetadjustmentHistory> histList = obj_adjustment.getEfinBudgetadjHistList();
      if (obj_adjustment.getDocumentStatus().equals("CO")
          || obj_adjustment.getDocumentStatus().equals("EFIN_IP")
          || obj_adjustment.getDocumentStatus().equals("EFIN_AP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Adjustment_processed"));
      } /*
         * else if (histList.size() > 0) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_Budget_Adjustment_History(Error)")); }
         */
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(" Exception while Delete Account in Budget Adjustment: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
