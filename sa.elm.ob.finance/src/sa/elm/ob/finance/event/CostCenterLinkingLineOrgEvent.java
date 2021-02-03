/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINCostorgnization;
import sa.elm.ob.finance.event.dao.CostCenterLinkingLineOrgEventDAO;

/**
 * @author Priyanka Ranjan on 06/09/2017
 * 
 */

// Handle the events in Organization tab in Cost Center Linking window

public class CostCenterLinkingLineOrgEvent extends EntityPersistenceEventObserver {
  // get entities of Organization tab
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINCostorgnization.ENTITY_NAME) };

  private static final Logger LOG = LoggerFactory.getLogger(CostCenterLinkingLineOrgEvent.class);

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
      EFINCostorgnization costcentorg = (EFINCostorgnization) event.getTargetInstance();
      final Property enable = entities[0].getProperty(EFINCostorgnization.PROPERTY_ACTIVE);
      final Property enabledisable = entities[0]
          .getProperty(EFINCostorgnization.PROPERTY_ENABLEDISABLE);

      Object currentEnable = event.getCurrentState(enable);
      Object previousEnable = event.getPreviousState(enable);
      String client = costcentorg.getClient().getId(), elementid = costcentorg.getAccount().getId(),
          orgid = costcentorg.getOrg().getId();
      boolean isactive = costcentorg.isActive();
      ConnectionProvider conn = new DalConnectionProvider(false);

      if ((!currentEnable.equals(previousEnable))
          && (costcentorg.isActive() || !costcentorg.isActive())) {

        // update enabledisable field with same value of enable(active) flag
        event.setCurrentState(enabledisable, isactive);

        // while updating enable flag in org tab , update all Cost Center tab records with same
        // enable value
        if (isactive) {
          CostCenterLinkingLineOrgEventDAO.updatecostcenterenablevalue(costcentorg, isactive);
        }

        // while updating enable flag in org tab , update enable flag with same value in all child
        // account's org and cost center
        CostCenterLinkingLineOrgEventDAO.updatecostorganizationenablevalue(elementid, client, orgid,
            conn, isactive);

      }
    } catch (OBException e) {
      LOG.error(" Exception while updating Cost Center Linking line(Organization): " + e, e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EFINCostorgnization costcentorg = (EFINCostorgnization) event.getTargetInstance();
      final Property enabledisable = entities[0]
          .getProperty(EFINCostorgnization.PROPERTY_ENABLEDISABLE);
      boolean isactive = costcentorg.isActive();

      if (costcentorg.isActive() || !costcentorg.isActive()) {

        // save enabledisable field with same value of enable(active) flag
        event.setCurrentState(enabledisable, isactive);
      }
    } catch (OBException e) {
      LOG.error(" Exception while creating record in Cost Center Linking  line(Organization): " + e,
          e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
