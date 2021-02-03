package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetTransfertrx;

public class BudgetRevHeaderVoidedEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetTransfertrx.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger log = LoggerFactory.getLogger(BudgetRevHeaderVoidedEvent.class);

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      EfinBudgetTransfertrx budgrevhead = (EfinBudgetTransfertrx) event.getTargetInstance();
      OBQuery<EfinBudgetTransfertrx> Revision = OBDal.getInstance()
          .createQuery(EfinBudgetTransfertrx.class, " id ='" + budgrevhead.getId()
              + "' and efinBudgetRevVoid.id is not null" + " and  docStatus ='WFA'");
      if (Revision.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRevHead_Void_Error_Msg"));
      }
    } catch (OBException e) {
      log.error(
          " Exception while deleting header in Budget Revision which was created while voiding a budget revision and status is in In approval: "
              + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
