package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmBidTermCondition;
import sa.elm.ob.utility.util.Utility;

public class BidTermEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in Bid Management - Bid Terms and Conditions tab
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmBidTermCondition.ENTITY_NAME) };

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
      EscmBidTermCondition bidtermcdn = (EscmBidTermCondition) event.getTargetInstance();
      if (bidtermcdn.getEscmBidmgmt().getBidappstatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Inventory_Completed"));
      }
      // validation based on data type given for reference lookup line
      ESCMDefLookupsTypeLn lookupLine = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
          bidtermcdn.getAttributename().getId());

      Utility.validateInitialBGValue(bidtermcdn.getAttrvalue(),
          bidtermcdn.getAttributename().getCommercialName(), "Attribute Value", lookupLine);

    } catch (OBException e) {
      log.debug("exception while creating BidTermandCondition" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating BidTermandCondition" + e);
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
      EscmBidTermCondition bidtermcdn = (EscmBidTermCondition) event.getTargetInstance();
      if (bidtermcdn.getEscmBidmgmt().getBidappstatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Inventory_Completed"));
      }
      ESCMDefLookupsTypeLn lookupLine = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
          bidtermcdn.getAttributename().getId());

      Utility.validateInitialBGValue(bidtermcdn.getAttrvalue(),
          bidtermcdn.getAttributename().getCommercialName(), "Attribute Value", lookupLine);

    } catch (OBException e) {
      log.debug("exception while creating BidTermandCondition" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating BidTermandCondition" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
