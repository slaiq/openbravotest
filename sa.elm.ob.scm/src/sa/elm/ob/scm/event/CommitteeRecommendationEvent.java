package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMCommRecommendation;
import sa.elm.ob.scm.ESCMProposalEvlEvent;

/**
 * 
 * @author Divya J - 04-01-2018
 *
 */

public class CommitteeRecommendationEvent extends EntityPersistenceEventObserver {
  private Logger log = Logger.getLogger(this.getClass());

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMCommRecommendation.ENTITY_NAME) };

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
      ESCMCommRecommendation commrecomm = (ESCMCommRecommendation) event.getTargetInstance();
      final Property proposalEvalevent = entities[0]
          .getProperty(ESCMCommRecommendation.PROPERTY_ESCMPROPOSALEVLEVENT);
      if (commrecomm.getEscmTechnicalevlEvent() != null) {
        OBQuery<ESCMProposalEvlEvent> proevleventQry = OBDal.getInstance()
            .createQuery(ESCMProposalEvlEvent.class, " as e where  e.bidNo.id=:bidID )");
        proevleventQry.setNamedParameter("bidID",
            commrecomm.getEscmTechnicalevlEvent().getBidNo().getId());
        proevleventQry.setMaxResult(1);
        if (proevleventQry.list().size() > 0) {
          event.setCurrentState(proposalEvalevent, proevleventQry.list().get(0));
        }
      }

    } catch (OBException e) {
      log.error("exception while creating CommitteeRecommendation in Proposal Evaluation event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating CommitteeRecommendation in Proposal Evaluation event", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
