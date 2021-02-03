package sa.elm.ob.utility.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Table;

public class TableHeaderEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Table.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Table tableObj = (Table) event.getTargetInstance();
      if (tableObj.isView() && tableObj.isEutEnablenavigationlink() != null
          && tableObj.isEutEnablenavigationlink()
          && (tableObj.getWindow() == null || tableObj.getEutTab() == null)) {
        throw new OBException(OBMessageUtils.messageBD("Eut_WindowTabMandatory"));
      }

    } catch (OBException e) {
      log.error("exception while creating TableHeaderEvent", e);
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
      Table tableObj = (Table) event.getTargetInstance();

      if (tableObj.isView() && tableObj.isEutEnablenavigationlink() != null
          && tableObj.isEutEnablenavigationlink()
          && (tableObj.getWindow() == null || tableObj.getEutTab() == null)) {
        throw new OBException(OBMessageUtils.messageBD("Eut_WindowTabMandatory"));
      }

    } catch (OBException e) {
      log.error("exception while updating TableHeaderEvent", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
