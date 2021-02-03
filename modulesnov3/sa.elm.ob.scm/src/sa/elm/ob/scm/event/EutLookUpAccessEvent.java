package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.utility.EutLookupAccess;

/**
 * 
 * @author Gokul 21/09/2020
 * 
 *
 */

public class EutLookUpAccessEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EutLookupAccess.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EutLookupAccess lookUpAccess = (EutLookupAccess) event.getTargetInstance();
      EutLookUpAccessEventDAO dao = new EutLookUpAccessEventDAO();
      Boolean checkListIsUnique = false;
      checkListIsUnique = dao.checkListIsUnique(lookUpAccess);
      if (checkListIsUnique) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_LookUpaccess_Unique"));
      }

    } catch (OBException e) {
      log.error("exception while Saving EutLookUpAccessEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while Saving EutLookUpAccessEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
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
      EutLookupAccess lookUpAccess = (EutLookupAccess) event.getTargetInstance();
      EutLookUpAccessEventDAO dao = new EutLookUpAccessEventDAO();
      final Property contractCategory = entities[0]
          .getProperty(EutLookupAccess.PROPERTY_ESCMDEFLOOKUPSTYPELN);

      Boolean checkListIsUnique = false;
      checkListIsUnique = dao.checkListIsUnique(lookUpAccess);
      if (!event.getPreviousState(contractCategory)
          .equals(event.getCurrentState(contractCategory))) {
        if (checkListIsUnique) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_LookUpaccess_Unique"));
        }
      }
    } catch (OBException e) {
      log.error("exception while updating EutLookUpAccessEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating EutLookUpAccessEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
