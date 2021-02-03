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

import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;

public class BidMgmtSourceRefEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in Bid Management - source reference tab
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmbidsourceref.ENTITY_NAME) };

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
      Escmbidsourceref bidmgmtsrc = (Escmbidsourceref) event.getTargetInstance();
      BigDecimal srcrefQty = bidmgmtsrc.getReservedQuantity();

      if (bidmgmtsrc.getRequisitionLine() != null && srcrefQty.compareTo(BigDecimal.ZERO) > 0) {
        bidmgmtsrc.setReservedQuantity(BigDecimal.ZERO);

        // / update the requisition line
        RequisitionLine line = bidmgmtsrc.getRequisitionLine();
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        line.setEscmBidmgmtQty(line.getEscmBidmgmtQty().subtract(srcrefQty));
        OBDal.getInstance().save(line);

        // update the bidmanagement line
        Escmbidmgmtline bidline = bidmgmtsrc.getEscmBidmgmtLine();
        bidline.setUpdated(new java.util.Date());
        bidline.setUpdatedBy(OBContext.getOBContext().getUser());
        bidline.setMovementQuantity(bidline.getMovementQuantity().subtract(srcrefQty));
        OBDal.getInstance().save(bidline);
        log.debug("getQuantity:" + bidline.getMovementQuantity());
      } /*
         * else if (bidmgmtsrc.getRequisitionLine() == null && srcrefQty.compareTo(BigDecimal.ZERO)
         * > 0) { bidmgmtsrc.setReservedQuantity(BigDecimal.ZERO); Escmbidmgmtline bidline =
         * bidmgmtsrc.getEscmBidmgmtLine(); bidline.setUpdated(new java.util.Date());
         * bidline.setUpdatedBy(OBContext.getOBContext().getUser());
         * bidline.setMovementQuantity(bidline.getMovementQuantity().subtract(srcrefQty));
         * OBDal.getInstance().save(bidline); }
         */
    } catch (OBException e) {
      log.debug("exception while deleting BidMgmtSourceRefEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while deleting BidMgmtSourceRefEvent" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
