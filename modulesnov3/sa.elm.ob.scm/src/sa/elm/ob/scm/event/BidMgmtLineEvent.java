package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmBidmgmtLineV;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.event.dao.BidEventDAO;
import sa.elm.ob.utility.util.Utility;

public class BidMgmtLineEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in Bid Management Line
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmbidmgmtline.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      Escmbidmgmtline bidmgmtline = (Escmbidmgmtline) event.getTargetInstance();
      BigDecimal srcrefqty = BigDecimal.ZERO;
      String newParentId = null, oldParentId = null;
      final Property qty = entities[0].getProperty(Escmbidmgmtline.PROPERTY_MOVEMENTQUANTITY);
      final Property parent = entities[0].getProperty(Escmbidmgmtline.PROPERTY_PARENTLINE);

      BigDecimal newqty = (BigDecimal) event.getCurrentState(qty);
      BigDecimal oldqty = (BigDecimal) event.getPreviousState(qty);
      EscmBidmgmtLineV oldParent = (EscmBidmgmtLineV) event.getPreviousState(parent);
      EscmBidmgmtLineV newParent = (EscmBidmgmtLineV) event.getCurrentState(parent);

      newParentId = (newParent == null ? "" : newParent.getId());
      oldParentId = (oldParent == null ? "" : oldParent.getId());

      BigDecimal qtyDiff = newqty.subtract(oldqty);
      Long existinSourceLine = new Long(10);
      log.debug("newqty:" + newqty);
      log.debug("oldqty:" + oldqty);
      if (!event.getCurrentState(qty).equals(event.getPreviousState(qty))
          && newqty.compareTo(oldqty) < 0 && !bidmgmtline.isSummarylevel()) {
        List<Escmbidsourceref> ref = BidEventDAO.getSourRefList(bidmgmtline.getId());

        if (ref.size() > 0) {
          for (Escmbidsourceref reference : ref) {
            srcrefqty = srcrefqty.add(reference.getReservedQuantity());
            existinSourceLine = reference.getLineNo() + new Long(10);
          }
        }
        log.debug("srcrefqty:" + srcrefqty);
        log.debug("qty:" + event.getCurrentState(qty));
        // not allow to enter less qty compare to reserved qty
        if (srcrefqty.compareTo(new BigDecimal(event.getCurrentState(qty).toString())) > 0
            && bidmgmtline.getEscmBidsourcerefList().get(0).getRequisition() != null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidMgmLine_QtyGrtSrcQty"));
        }
        //
        else {
          if (BidEventDAO.getBidSrfCount(bidmgmtline.getId()) > 0) {
            Escmbidsourceref manualReference = ref.get(0);
            manualReference.setReservedQuantity(manualReference.getReservedQuantity().add(qtyDiff));
            OBDal.getInstance().save(manualReference);
          } else {
            final Entity bidlineentity = ModelProvider.getInstance()
                .getEntity(Escmbidmgmtline.ENTITY_NAME);
            Object srcref = BidEventDAO.setSourRef(bidmgmtline, existinSourceLine);
            /*
             * final Escmbidsourceref srcref = OBProvider.getInstance().get(Escmbidsourceref.class);
             * srcref.setEscmBidmgmtLine(bidmgmtline);
             * srcref.setReservedQuantity(bidmgmtline.getMovementQuantity());
             * srcref.setLineNo(existinSourceLine);
             */

            final Property refproperty = bidlineentity
                .getProperty(Escmbidmgmtline.PROPERTY_ESCMBIDSOURCEREFLIST);
            @SuppressWarnings("unchecked")
            final List<Object> srcls = (List<Object>) event.getCurrentState(refproperty);
            srcls.add(srcref);
          }
        }
      }

      if (!oldParentId.equals(newParentId)) {
        // if parent line id is not null, then set issummary of parent has true,
        // set movementqty has 1 for parent
        // and set uom as each for parent

        if (bidmgmtline.getParentline() != null) {

          if (bidmgmtline.getParentline().getId().equals(bidmgmtline.getId())) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_BidMgmLine_SameParent"));
          }

        }

        if (!StringUtils.isEmpty(oldParentId)) {
          // update parent bid line when it is deselected
          Escmbidmgmtline ParentLineId = Utility.getObject(Escmbidmgmtline.class, oldParentId);
          if (ParentLineId != null) {
            /*
             * final OBQuery<Escmbidmgmtline> EscmbidmgmtlineList = OBDal.getInstance().createQuery(
             * Escmbidmgmtline.class, "parentline.id='" + oldParentId + "'");
             */
            int lineCount = BidEventDAO.getBidMgmtLineCount(oldParentId);

            if ((!bidmgmtline.isManual()) && !(ParentLineId.isManual())) {
              // check when we change parent of deliverable from PR we have to check old parent has
              // atleast one child
              if (lineCount == 1) {
                throw new OBException(OBMessageUtils.messageBD("ESCM_BidMgmLine_CannotReorder"));
              }
            } else {
              if (lineCount == 1) {
                Escmbidmgmtline bidLine = Utility.getObject(Escmbidmgmtline.class, oldParentId);
                bidLine.setSummarylevel(false);
                OBDal.getInstance().save(bidLine);
              }
            }
          }

        }

        if (bidmgmtline.getParentline() != null) {
          BidEventDAO.setBidLineParent(bidmgmtline.getParentline().getId());
        }
      }
      /*
       * If line is created manually, update source reference quantity when line quantity is changed
       */
      if (oldqty != newqty && bidmgmtline.isManual()) {
        List<Escmbidsourceref> ref = BidEventDAO.getSourRefList(bidmgmtline.getId());
        if (ref.size() > 0) {
          Escmbidsourceref srcRef = ref.get(0);
          srcRef.setReservedQuantity(newqty);
        }
      }
    } catch (OBException e) {
      log.error("exception while deleting BidMgmtLineEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting BidMgmtLineEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      Escmbidmgmtline bidmgmtline = (Escmbidmgmtline) event.getTargetInstance();
      /*
       * if (bidmgmtline.getEscmBidsourcerefList().size() > 0) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_BidLineCantDel")); }
       */
      List<Escmbidsourceref> reflist = bidmgmtline.getEscmBidsourcerefList();
      log.debug("reflist:" + reflist.size());
      for (Escmbidsourceref ref : reflist) {
        if (ref.getRequisitionLine() != null) {
          RequisitionLine line = ref.getRequisitionLine();
          line.setUpdated(new java.util.Date());
          line.setUpdatedBy(OBContext.getOBContext().getUser());
          line.setEscmBidmgmtQty(line.getEscmBidmgmtQty().subtract(ref.getReservedQuantity()));
          OBDal.getInstance().save(line);
        }

        ref.setReservedQuantity(BigDecimal.ZERO);
        OBDal.getInstance().save(ref);

      }
      log.debug("reflist11:" + reflist.size());
      for (Escmbidsourceref ref1 : reflist) {
        OBDal.getInstance().remove(ref1);
      }

      if (bidmgmtline.getParentline() != null) {
        /*
         * OBQuery<Escmbidmgmtline> chkLineExists = OBDal.getInstance().createQuery(
         * Escmbidmgmtline.class, "as e where e.escmBidmgmt.id = '" +
         * bidmgmtline.getEscmBidmgmt().getId() + "' and e.parentline.id ='" +
         * bidmgmtline.getParentline().getId() + "'"); chkLineExists.setMaxResult(1);
         */
        int lineCount = BidEventDAO.getBidMgmtParentLineCount(bidmgmtline);

        if (lineCount == 1) {
          if (bidmgmtline.isManual()) {
            Escmbidmgmtline parentLine = Utility.getObject(Escmbidmgmtline.class,
                bidmgmtline.getParentline().getId());
            parentLine.setSummarylevel(false);
            OBDal.getInstance().save(parentLine);
          }
        }
      }

    } catch (OBException e) {
      log.debug("exception while deleting BidMgmtLineEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while deleting BidMgmtLineEvent" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unused")
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {

      OBContext.setAdminMode();
      Escmbidmgmtline bidmgmtline = (Escmbidmgmtline) event.getTargetInstance();
      final Property uniquecodename = entities[0]
          .getProperty(Escmbidmgmtline.PROPERTY_UNIQUECODENAME);

      if (bidmgmtline.isManual()) {
        final Entity bidlineentity = ModelProvider.getInstance()
            .getEntity(Escmbidmgmtline.ENTITY_NAME);
        final Escmbidsourceref srcref = Utility.getEntity(Escmbidsourceref.class);
        srcref.setEscmBidmgmtLine(bidmgmtline);
        srcref.setReservedQuantity(bidmgmtline.getMovementQuantity());
        if (bidmgmtline.getCreatedBy().getEscmDepartment() != null) {
          srcref.setRequestingDepartment(bidmgmtline.getCreatedBy().getEscmDepartment());
        }
        srcref.setLineNo(new Long(10));
        srcref.setDescription(bidmgmtline.getDescription());
        final Property qty = entities[0].getProperty(Escmbidmgmtline.PROPERTY_MOVEMENTQUANTITY);
        final Property refproperty = bidlineentity
            .getProperty(Escmbidmgmtline.PROPERTY_ESCMBIDSOURCEREFLIST);
        @SuppressWarnings("unchecked")
        final List<Object> srcls = (List<Object>) event.getCurrentState(refproperty);
        srcls.add(srcref);
      }
      if (bidmgmtline.getAccountingCombination() != null) {
        if (bidmgmtline.getAccountingCombination().getEfinUniquecodename() != null) {
          event.setCurrentState(uniquecodename,
              bidmgmtline.getAccountingCombination().getEfinUniquecodename());
        }
      }

      // if parent line id is not null, then set issummary of parent has true,
      // set movementqty has 1 for parent
      // and set uom as each for parent
      if (bidmgmtline.getParentline() != null) {
        BidEventDAO.setBidLineParent(bidmgmtline.getParentline().getId());
      }

    } catch (OBException e) {
      log.debug("exception while creating BidManagementLinesEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating BidManagementLinesEvent" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
