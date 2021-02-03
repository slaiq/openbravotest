package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.materialmgmt.cost.ABCActivity;

public class ActivityEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ABCActivity.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    // Search key cannot be updated if account dimension exists
    final Property value = entities[0].getProperty(ABCActivity.PROPERTY_SEARCHKEY);
    if (!event.getCurrentState(value).equals(event.getPreviousState(value))) {
      verifyAccountDimension(event);
    }
  }

  private void verifyAccountDimension(EntityPersistenceEvent event) {
    try {
      OBContext.setAdminMode();
      ABCActivity act = (ABCActivity) event.getTargetInstance();

      final OBCriteria<AccountingCombination> ac = OBDal.getInstance()
          .createCriteria(AccountingCombination.class);
      ac.add(Restrictions.eq(AccountingCombination.PROPERTY_ACTIVITY, act));

      if (ac.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_AccountDimension_Exists"));
      }
    } catch (OBException e) {
      log.error(" Exception : " + e);
      throw new OBException(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
