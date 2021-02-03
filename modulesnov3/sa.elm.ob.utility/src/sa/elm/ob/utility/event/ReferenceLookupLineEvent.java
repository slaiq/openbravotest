package sa.elm.ob.utility.event;

import javax.enterprise.event.Observes;

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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.utility.EUTDeflookupsTypeLn;

public class ReferenceLookupLineEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EUTDeflookupsTypeLn.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EUTDeflookupsTypeLn lookupLineObj = (EUTDeflookupsTypeLn) event.getTargetInstance();

      if (lookupLineObj.getEUTDeflookupsType() != null
          && lookupLineObj.getEUTDeflookupsType().getSearchKey().equals("PENALTY_TYPE")) {
        if (lookupLineObj.getPenaltyLogic() == null) {
          throw new OBException(OBMessageUtils.messageBD("EUT_PenaltyLogicNotNull"));
        }
      }

    } catch (OBException e) {
      log.error("exception while creating ReferenceLookupLineEvent", e);
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
      EUTDeflookupsTypeLn lookupLineObj = (EUTDeflookupsTypeLn) event.getTargetInstance();
      final Property code = entities[0].getProperty(EUTDeflookupsTypeLn.PROPERTY_CODE);
      final Property penaltyLogic = entities[0]
          .getProperty(EUTDeflookupsTypeLn.PROPERTY_PENALTYLOGIC);
      if (lookupLineObj.getEUTDeflookupsType() != null
          && lookupLineObj.getEUTDeflookupsType().getSearchKey().equals("PENALTY_TYPE")) {
        if (lookupLineObj.getPenaltyLogic() == null) {
          throw new OBException(OBMessageUtils.messageBD("EUT_PenaltyLogicNotNull"));
        }
        OBQuery<EfinPenaltyTypes> penaltyTypeQry = OBDal.getInstance().createQuery(
            EfinPenaltyTypes.class,
            " as e where e.deductiontype.code=:deductionType and e.client.id=:clientId");
        penaltyTypeQry.setNamedParameter("deductionType", event.getPreviousState(code));
        penaltyTypeQry.setNamedParameter("clientId", lookupLineObj.getClient().getId());
        if (penaltyTypeQry.list().size() > 0) {
          if (event.getCurrentState(code) != event.getPreviousState(code)
              || event.getCurrentState(penaltyLogic) != event.getPreviousState(penaltyLogic)) {
            throw new OBException(OBMessageUtils.messageBD("EUT_CodeAlreadyUsedInPenalty"));
          }
        }
      }
    } catch (OBException e) {
      log.error("exception while updating ReferenceLookupLineEvent", e);
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
      OBContext.setAdminMode();
      EUTDeflookupsTypeLn lookupLineObj = (EUTDeflookupsTypeLn) event.getTargetInstance();

      OBQuery<EfinPenaltyTypes> penaltyTypeQry = OBDal.getInstance()
          .createQuery(EfinPenaltyTypes.class, " as e where e.deductiontype.code=:deductionType ");
      penaltyTypeQry.setNamedParameter("deductionType", lookupLineObj.getCode());

      if (penaltyTypeQry.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("EUT_CodeAlreadyUsedInPenalty"));
      }
    } catch (OBException e) {
      log.error(" Exception while Delete ReferenceLookupLineEventf: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
