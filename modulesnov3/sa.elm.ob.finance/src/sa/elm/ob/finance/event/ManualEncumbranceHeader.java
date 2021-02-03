/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */

package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;

/**
 * 
 * @author Gowtham.V
 *
 */
public class ManualEncumbranceHeader extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetManencum.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(ManualEncumbranceHeader.class);

  // should not allow to delete manuamencumbrance if its approved.
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EfinBudgetManencum encum = (EfinBudgetManencum) event.getTargetInstance();
      if (encum.getEncumMethod().equals("M") && !encum.getEncumType().equals("TE")) {
        if (!encum.getDocumentStatus().equals("DR") && !encum.getDocumentStatus().equals("RW")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Encum_HdDelete"));
        }
      } else {
        if (encum.getProcurementRequisitionEMEfinBudgetManencumIDList().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Encum_HdDelete"));
        }
      }
    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Deleting ManualEncubrance: " + e);
      }
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
      EfinBudgetManencum encum = (EfinBudgetManencum) event.getTargetInstance();
      if (encum.getBudgetInitialization() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Encum_BudInit_Mandatory"));
      }
      if (encum.getSalesCampaign() == null) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_BudgetTypeNotEmpty"));
      }

    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Saving ManualEncubrance: " + e);
      }
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
      EfinBudgetManencum encum = (EfinBudgetManencum) event.getTargetInstance();
      if (encum.getBudgetInitialization() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Encum_BudInit_Mandatory"));
      }
      if (encum.getSalesCampaign() == null) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_BudgetTypeNotEmpty"));
      }
    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Updating ManualEncubrance: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}