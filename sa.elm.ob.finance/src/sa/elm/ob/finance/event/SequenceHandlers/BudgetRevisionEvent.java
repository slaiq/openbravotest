package sa.elm.ob.finance.event.SequenceHandlers;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.calendar.Calendar;

import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 12/08/2016
 * 
 */
public class BudgetRevisionEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetTransfertrx.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
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
      EfinBudgetTransfertrx BudgetRevision = (EfinBudgetTransfertrx) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(EfinBudgetTransfertrx.PROPERTY_DOCUMENTNO);
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(BudgetRevision.getAccountingDate());
      OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
          "as e where e.organization.id ='0'");
      List<Calendar> calendarQueryList = calendarQuery.list();
      if (calendarQueryList.size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
      }
      Calendar calendar = calendarQueryList.get(0);
      String SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_Budget_Transfertrx",
          calendar.getId(), "0", true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
      }
      event.setCurrentState(documentNo, SequenceNo);
    } catch (Exception e) {
      log.error(" Exception while Save BudgetRevision" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
