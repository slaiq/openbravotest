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

import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.event.dao.BidEventDAO;

public class BidConfigurationEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmbidconfiguration.ENTITY_NAME) };

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
      Escmbidconfiguration bidconfiguration = (Escmbidconfiguration) event.getTargetInstance();
      if (bidconfiguration.getAppminvalue() == null && bidconfiguration.getAppmaxvalue() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_BidConfig"));
      }
      if (bidconfiguration.getAppminvalue() != null) {
        if (bidconfiguration.getAppminvalue() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Appmin"));
        }
      }
      if (bidconfiguration.getAppmaxvalue() != null) {
        if (bidconfiguration.getAppmaxvalue() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Appmax"));
        }
      }
      if (bidconfiguration.getAppmaxvalue() != null && bidconfiguration.getAppminvalue() != null) {
        if (bidconfiguration.getAppmaxvalue() < bidconfiguration.getAppminvalue()) {
          throw new OBException(OBMessageUtils.messageBD("Escm_MaxValuegreater"));
        }
      }
      if (BidEventDAO.getBidConfigCount(bidconfiguration) > 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_BidConfiguration"));
      }
    } catch (OBException e) {
      log.error("Exception while creating BidConfigurationEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while creating BidConfigurationEvent", e);
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
      Escmbidconfiguration bidconfiguration = (Escmbidconfiguration) event.getTargetInstance();
      final Property type = entities[0].getProperty(Escmbidconfiguration.PROPERTY_BIDTYPE);
      final Property org = entities[0].getProperty(Escmbidconfiguration.PROPERTY_ORGANIZATION);

      if (bidconfiguration.getAppminvalue() == null && bidconfiguration.getAppmaxvalue() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_BidConfig"));
      }

      if (bidconfiguration.getAppminvalue() != null) {
        if (bidconfiguration.getAppminvalue() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Appmin"));
        }
      }

      if (bidconfiguration.getAppmaxvalue() != null) {
        if (bidconfiguration.getAppmaxvalue() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Appmax"));
        }
      }
      if (bidconfiguration.getAppmaxvalue() != null && bidconfiguration.getAppminvalue() != null) {
        if (bidconfiguration.getAppmaxvalue() < bidconfiguration.getAppminvalue()) {
          throw new OBException(OBMessageUtils.messageBD("Escm_MaxValuegreater"));
        }
      }
      if (!event.getCurrentState(org).equals(event.getPreviousState(org))
          || !event.getCurrentState(type).equals(event.getPreviousState(type))) {

        if (BidEventDAO.getBidConfigCount(bidconfiguration) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_BidConfiguration"));
        }
      }
    } catch (OBException e) {
      log.error("Exception while updating bidconfigurationEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating bidconfigurationEvent" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
