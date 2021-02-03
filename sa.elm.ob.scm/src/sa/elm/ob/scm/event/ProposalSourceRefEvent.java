package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;

/**
 * 
 * @author Gopalakrishnan on 28/08/2017
 * 
 */

public class ProposalSourceRefEvent extends EntityPersistenceEventObserver {
  /**
   * Business event in Table EscmProposalsourceRef
   */
  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmProposalsourceRef.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmProposalsourceRef proposalSource = (EscmProposalsourceRef) event.getTargetInstance();
      BigDecimal srcrefQty = proposalSource.getReservedQuantity();
      if (proposalSource.getRequisitionLine() != null && srcrefQty.compareTo(BigDecimal.ZERO) > 0) {
        proposalSource.setReservedQuantity(BigDecimal.ZERO);

        // / update the requisition line
        RequisitionLine line = proposalSource.getRequisitionLine();
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        if (line.getEscmProposalsourceRefList().size() == 1) {
          line.setEscmIsproposal(false);
        }
        // line.setEscmProposalqty(line.getEscmProposalqty().subtract(srcrefQty));
        OBDal.getInstance().save(line);

        // update the Order line
        EscmProposalmgmtLine objProposalLine = proposalSource.getEscmProposalmgmtLine();
        objProposalLine.setUpdated(new java.util.Date());
        objProposalLine.setUpdatedBy(OBContext.getOBContext().getUser());
        objProposalLine
            .setMovementQuantity(objProposalLine.getMovementQuantity().subtract(srcrefQty));
        objProposalLine.setLineTotal(objProposalLine.getNegotUnitPrice()
            .multiply((objProposalLine.getMovementQuantity().subtract(srcrefQty))));
        OBDal.getInstance().save(objProposalLine);
      }

    } catch (OBException e) {
      log.error("exception while deleting Proposal Source Source Reference Event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting Proposal Source Source Reference Event", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
