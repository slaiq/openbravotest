package sa.elm.ob.finance.event;

import java.util.List;
import javax.enterprise.event.Observes;
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
import org.openbravo.model.sales.SalesRegion;

public class DefaultCostCenter extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(SalesRegion.ENTITY_NAME) };
  /**
   * This event is used to check whether the Default Cost Center is only one per client
   */
  @Override
  protected Entity[] getObservedEntities() {
    return entities;

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String curClient = OBContext.getOBContext().getCurrentClient().getId();
      Boolean isDefault = true;
      
      final Property efinDefault = entities[0].getProperty(SalesRegion.PROPERTY_EFINDEFAULT);

      if (event.getCurrentState(efinDefault) != null
          && event.getCurrentState(efinDefault).equals(true)
          && (!event.getCurrentState(efinDefault)
              .equals(event.getPreviousState(efinDefault)))) {
        OBQuery<SalesRegion> salesregion = OBDal.getInstance().createQuery(SalesRegion.class,"client.id = :clientID and efinDefault = :default");
        salesregion.setNamedParameter("clientID", curClient);
        salesregion.setNamedParameter("default", isDefault);

        List<SalesRegion> salesregionList = salesregion.list();
        if(salesregionList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_default_CostCenter"));
            }
      }
    }catch (OBException e) {
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
    finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String curClient = OBContext.getOBContext().getCurrentClient().getId();
      Boolean isDefault = true;
      
      final Property efinDefault = entities[0].getProperty(SalesRegion.PROPERTY_EFINDEFAULT);

      if ( event.getCurrentState(efinDefault) != null
          && event.getCurrentState(efinDefault).equals(true)) {
        OBQuery<SalesRegion> salesregion = OBDal.getInstance().createQuery(SalesRegion.class,"client.id = :clientID and efinDefault = :default");
        salesregion.setNamedParameter("clientID", curClient);
        salesregion.setNamedParameter("default", isDefault);
      
        List<SalesRegion> salesregionList = salesregion.list();
        if(salesregionList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_default_CostCenter"));
            }
      }
    }catch (OBException e) {
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
    finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }
}
