package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinLookupLine;
import sa.elm.ob.finance.event.dao.FinanceReferenceLookupLineEventDAO;

/**
 * 
 * @author Gokul 12/07/19
 *
 */
public class FinanceReferenceLookupLineEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinLookupLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  FinanceReferenceLookupLineEventDAO dao = null;
  Boolean checkDefaultAlreadyExist = false;
  String lookUpTypeId = null, clientId = null;
  VariablesSecureApp vars = null;

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinLookupLine lookupLineObj = (EfinLookupLine) event.getTargetInstance();
      dao = new FinanceReferenceLookupLineEventDAO();
      lookUpTypeId = lookupLineObj.getLookUp().getId();
      clientId = OBContext.getOBContext().getCurrentClient().getId();
      if (lookupLineObj.isEscmDefault())
        checkDefaultAlreadyExist = dao.checkDefaultAlreadyExist(lookUpTypeId, clientId);
      if (checkDefaultAlreadyExist) {
        throw new OBException(OBMessageUtils.messageBD("efin_lookupline_default"));
      }
    } catch (OBException e) {
      log.error("exception while saving FinanceReferenceLookupLineEvent", e);
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
      final Property isDefault = entities[0].getProperty(EfinLookupLine.PROPERTY_ESCMDEFAULT);
      EfinLookupLine lookupLineObj = (EfinLookupLine) event.getTargetInstance();
      dao = new FinanceReferenceLookupLineEventDAO();
      lookUpTypeId = lookupLineObj.getLookUp().getId();
      clientId = OBContext.getOBContext().getCurrentClient().getId();
      if (!(event.getPreviousState(isDefault).equals((event.getCurrentState(isDefault))))
          && lookupLineObj.isEscmDefault())
        checkDefaultAlreadyExist = dao.checkDefaultAlreadyExist(lookUpTypeId, clientId);
      if (checkDefaultAlreadyExist) {
        throw new OBException(OBMessageUtils.messageBD("efin_lookupline_default"));
      }
    } catch (OBException e) {
      log.error("exception while saving FinanceReferenceLookupLineEvent", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
