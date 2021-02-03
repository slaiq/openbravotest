package sa.elm.ob.finance.event;

import java.util.List;
import javax.enterprise.event.Observes;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EFINSadadbilConfig;

public class SadadBillConfigClientEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(EFINSadadbilConfig.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String curClient = OBContext.getOBContext().getCurrentClient().getId();
      OBQuery<EFINSadadbilConfig> billConfig = OBDal.getInstance().createQuery(EFINSadadbilConfig.class,"client.id = :clientID ");
      billConfig.setNamedParameter("clientID", curClient);
      
      List<EFINSadadbilConfig> billConfigList = billConfig.list();
      if(billConfigList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("efin_sadadbill_config_client"));
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
