package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetTypeAcct;

public class AccountBudTypEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINBudgetTypeAcct.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EFINBudgetTypeAcct account = (EFINBudgetTypeAcct) event.getTargetInstance();

      final OBCriteria<EFINBudget> budget = OBDal.getInstance().createCriteria(EFINBudget.class);

      budget.add(Restrictions.eq(EFINBudget.PROPERTY_ACCOUNTELEMENT, account.getAccountElement()));
      budget.add(Restrictions.eq(EFINBudget.PROPERTY_SALESCAMPAIGN, account.getSalesCampaign()));

      if (budget.list().size() > 0) {

        throw new OBException(OBMessageUtils.messageBD("Efin_AccBud_Event"));
      } else {
        OBContext.restorePreviousMode();
      }

    } catch (OBException e) {
      log.error(" Exception while deleting Account: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
