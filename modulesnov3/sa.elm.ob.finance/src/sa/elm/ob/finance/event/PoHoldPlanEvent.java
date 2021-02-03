package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINRdvBudgHold;

/**
 * @author Priyanka C
 * 
 */
public class PoHoldPlanEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINRdvBudgHold.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(EFINRdvBudgHold.class);

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EFINRdvBudgHold rdvbudgHoldHdr = (EFINRdvBudgHold) event.getTargetInstance();

      OBQuery<EFINRdvBudgHold> budgHoldHdr = OBDal.getInstance().createQuery(EFINRdvBudgHold.class,
          "salesOrder.id=:orderId and efinRdvtxn.id=:rdvTxnId");
      budgHoldHdr.setNamedParameter("orderId", rdvbudgHoldHdr.getSalesOrder().getId());
      budgHoldHdr.setNamedParameter("rdvTxnId", rdvbudgHoldHdr.getEfinRdvtxn().getId());
      if (budgHoldHdr.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_PoPlanAlreadyCreatedForPO"));
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while creating PO hold plan details for same po "
            + "and rdv combination : " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      EFINRdvBudgHold rdvBudgHoldHdr = (EFINRdvBudgHold) event.getTargetInstance();
      if (!rdvBudgHoldHdr.getStatus().equals("DR") && !rdvBudgHoldHdr.getStatus().equals("REJ")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_RDVDel_App"));
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while delete Po hold plan detail: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
