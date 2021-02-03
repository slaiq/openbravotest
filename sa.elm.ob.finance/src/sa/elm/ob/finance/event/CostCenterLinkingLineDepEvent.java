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
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINCostcenters;
import sa.elm.ob.finance.EFINCostorgnization;
import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * @author Sathish Kumar on 14/09/2017
 * 
 */

// Handle the events in Cost Center tab in Cost Center Linking window

public class CostCenterLinkingLineDepEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINCostcenters.ENTITY_NAME) };

  private static final Logger LOG = LoggerFactory.getLogger(CostCenterLinkingLineDepEvent.class);

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
      EFINCostcenters costcentorg = (EFINCostcenters) event.getTargetInstance();
      final Property enable = entities[0].getProperty(EFINCostorgnization.PROPERTY_ACTIVE);

      Object currentEnable = event.getCurrentState(enable);
      Object previousEnable = event.getPreviousState(enable);

      if ((!currentEnable.equals(previousEnable))) {
        if (!costcentorg.isActive()) {
          final OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
              EfinBudgetControlParam.class,
              "client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

          if (controlParam.list().size() > 0) {
            EfinBudgetControlParam config = controlParam.list().get(0);
            if (config.getBudgetcontrolCostcenter().getId()
                .equals(costcentorg.getDepartment().getId())
                || config.getBudgetcontrolunit().getId()
                    .equals(costcentorg.getDepartment().getId())) {
              throw new OBException(OBMessageUtils.messageBD("EFIN_Cannot_disableDep"));
            }
          }
        }

      }
    } catch (OBException e) {
      LOG.error(" Exception while updating Cost center department " + e, e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * public void onDelete(@Observes EntityDeleteEvent event) { if (!isValidEvent(event)) { return; }
   * try { OBContext.setAdminMode(); EFINCostcenters costcentorg = (EFINCostcenters)
   * event.getTargetInstance();
   * 
   * final OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
   * EfinBudgetControlParam.class, "client.id='" +
   * OBContext.getOBContext().getCurrentClient().getId() + "'");
   * 
   * if (controlParam.list().size() > 0) { EfinBudgetControlParam config =
   * controlParam.list().get(0); if
   * (config.getBudgetcontrolCostcenter().getId().equals(costcentorg.getDepartment().getId()) ||
   * config.getBudgetcontrolunit().getId().equals(costcentorg.getDepartment().getId())) { throw new
   * OBException(OBMessageUtils.messageBD("EFIN_Cannot_disableDep")); } } } catch (OBException e) {
   * LOG.error(" Exception while deleting Cost center department" + e, e); throw new
   * OBException(e.getMessage()); } finally { OBContext.restorePreviousMode(); } }
   */

}
