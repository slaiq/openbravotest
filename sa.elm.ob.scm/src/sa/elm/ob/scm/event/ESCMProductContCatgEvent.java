package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.event.dao.ESCMProductContCatgEventDAO;

/**
 * 
 * @author DivyaPrakash 11/03/2019
 *
 */

public class ESCMProductContCatgEvent extends EntityPersistenceEventObserver {
  private Logger log = Logger.getLogger(this.getClass());

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMProductContCatg.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  // update event
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMProductContCatg cntrctCtgry = (ESCMProductContCatg) event.getTargetInstance();
      final Property contractCategory = entities[0]
          .getProperty(ESCMProductContCatg.PROPERTY_CONTRACTCATEGORY);
      Boolean isCntrctCtgryUsed = false;
      ESCMProductContCatgEventDAO dao = null;
      dao = (ESCMProductContCatgEventDAO) new ESCMProductContCatgEventDAOImpl();

      ESCMDefLookupsTypeLn cntrctCtgryObj = (ESCMDefLookupsTypeLn) event
          .getPreviousState(contractCategory);
      String cntrctCtgryId = cntrctCtgryObj.getId();

      // If Contract Category is used in PO/PR/BID/PropMgmt then we should not allow to update.
      if (!event.getPreviousState(contractCategory)
          .equals(event.getCurrentState(contractCategory))) {
        isCntrctCtgryUsed = dao.isCntrctCtgryUsed(cntrctCtgryId, cntrctCtgry.getProduct().getId());
        if (isCntrctCtgryUsed) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CANTUPDATE_DEL"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating the ContractCategories:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating the ContractCategories:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // delete event
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMProductContCatg cntrctCtgry = (ESCMProductContCatg) event.getTargetInstance();
      Boolean isCntrctCtgryUsed = false;
      ESCMProductContCatgEventDAO dao = null;
      dao = (ESCMProductContCatgEventDAO) new ESCMProductContCatgEventDAOImpl();
      String cntrctCtgryId = cntrctCtgry.getContractCategory().getId();

      // If Contract Category is used in PO/PR/BID/PropMgmt then we should not allow to update.
      isCntrctCtgryUsed = dao.isCntrctCtgryUsed(cntrctCtgryId, cntrctCtgry.getProduct().getId());
      if (isCntrctCtgryUsed) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_CANTUPDATE_DEL"));
      }

    } catch (OBException e) {
      log.error("Exception while deleting the ContractCategories:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating Inspection tab in Goods Receipt: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
