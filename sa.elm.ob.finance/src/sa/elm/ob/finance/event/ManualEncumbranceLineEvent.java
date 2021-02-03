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

import sa.elm.ob.finance.EfinBudgetManencumlines;

public class ManualEncumbranceLineEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetManencumlines.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(ManualEncumbranceLineEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EfinBudgetManencumlines line = (EfinBudgetManencumlines) event.getTargetInstance();
      if (line != null) {
        if (line.getAmount().signum() == -1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Manencumline_amt"));
        }
      }

    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while updating  in Manual  Encumbrance Lines: " + e);
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
      EfinBudgetManencumlines line = (EfinBudgetManencumlines) event.getTargetInstance();

      if (line != null) {
        if (line.getAmount().signum() == -1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Manencumline_amt"));
        }
      }
    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while saving  in Manual Encumbrance lines: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // should not allow to delete lines if the encum status as inprogress and approved and cancelled
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinBudgetManencumlines encumLine = (EfinBudgetManencumlines) event.getTargetInstance();
      if (!encumLine.getManualEncumbrance().getDocumentStatus().equals("DR")
          && !encumLine.getManualEncumbrance().getDocumentStatus().equals("RW")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Encum_HdDelete"));
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
}
