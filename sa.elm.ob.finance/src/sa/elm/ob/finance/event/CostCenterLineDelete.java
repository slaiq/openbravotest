package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinCostCenterDescriptor;

public class CostCenterLineDelete extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinCostCenterDescriptor.ENTITY_NAME) };
  private static final Logger LOG = LoggerFactory.getLogger(CostCenterLineDelete.class);

  /**
   * This event is used to delete the lines in cost center window only when the enabled flag is
   * disabled
   */
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
      EfinCostCenterDescriptor costCenter = (EfinCostCenterDescriptor) event.getTargetInstance();
      boolean enable = costCenter.isActive();

      if (enable) {
        throw new OBException(OBMessageUtils.messageBD("Efin_CostCenterLine_Delete"));
      }
    } catch (OBException e) {
      LOG.error(" Exception while deleting Cost Center Line " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
