package sa.elm.ob.scm.event;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.event.dao.ProposalEvaluationEventDAO;
import sa.elm.ob.utility.util.Utility;

public class ProposalEvaluationEvent extends EntityPersistenceEventObserver {
  private Logger log = Logger.getLogger(this.getClass());

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMProposalEvlEvent.ENTITY_NAME) };

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
      ESCMProposalEvlEvent proEvlEvent = (ESCMProposalEvlEvent) event.getTargetInstance();
      final Property eventno = entities[0].getProperty(ESCMProposalEvlEvent.PROPERTY_EVENTNO);
      final Property ispartialaward = entities[0]
          .getProperty(ESCMProposalEvlEvent.PROPERTY_ISPARTIALAWARD);

      String sequence = "";
      Boolean sequenceexists = false;

      // set new Spec No
      sequence = Utility.getTransactionSequencewithclient("0", proEvlEvent.getClient().getId(),
          "PEE");

      // check already same spec no exists or not.
      sequenceexists = Utility.chkTransactionSequencewithclient(
          proEvlEvent.getOrganization().getId(), proEvlEvent.getClient().getId(), "PEE", sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }

      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(eventno, sequence);
      }

      // Announcement is mandatory if bid is tender.
      // Task No:7224
      // if (proEvlEvent.getBidNo() != null && proEvlEvent.getBidNo().getBidtype().equals("TR")
      // && proEvlEvent.getEscmAnnoucements() == null) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_ProposalEvlEvent_AnctMad"));
      // }
      // Integrate the Proposal attribute (created under Open Envelop committee) with Proposal
      // Evaluation Event
      ProposalEvaluationEventDAO.integProsalAtttoProsalEvent(proEvlEvent, null, event, entities);

      // update the rank

      String proposalEvalTime = proEvlEvent.getTimeEvaluation().toString();
      String propEvalHour[] = proposalEvalTime.split(":");

      if (Integer.parseInt(propEvalHour[0]) > 23) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PropEvlEventTimeVal"));
      }

      // ProposalEvaluationEventDAO.updatebank(proEvlEvent);

      if (proEvlEvent.getBidNo() != null && proEvlEvent.getBidNo().isPartialaward()) {
        event.setCurrentState(ispartialaward, true);
      }

    } catch (OBException e) {
      log.error("exception while creating Proposal Evaluation event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating Proposal Evaluation event", e);
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
      ESCMProposalEvlEvent proEvlEvent = (ESCMProposalEvlEvent) event.getTargetInstance();

      if (proEvlEvent.getStatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_ProEvalEvntCo"));
      }
      // cant able to delete the Proposal evaluation event if event have proposal attribute
      if (proEvlEvent.getEscmProposalAttrList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_ProEVlEvntDel"));
      }
      if (proEvlEvent.getSpecNo() != null && StringUtils.isNotEmpty(proEvlEvent.getSpecNo())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Spec_Generated(Error)"));
      }

    } catch (OBException e) {
      log.error("Exception while deleting PO Receipt:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting PO Receipt:", e);
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
      ESCMProposalEvlEvent proEvlEvent = (ESCMProposalEvlEvent) event.getTargetInstance();
      final Property proposalEvalTime = entities[0]
          .getProperty(ESCMProposalEvlEvent.PROPERTY_TIMEEVALUATION);
      final Property bid = entities[0].getProperty(ESCMProposalEvlEvent.PROPERTY_BIDNO);
      final Property ispartialaward = entities[0]
          .getProperty(ESCMProposalEvlEvent.PROPERTY_ISPARTIALAWARD);
      final Property awardAllQty = entities[0]
          .getProperty(ESCMProposalEvlEvent.PROPERTY_ISAWARDFULLQTY);

      if (!event.getPreviousState(proposalEvalTime)
          .equals(event.getCurrentState(proposalEvalTime))) {
        String currentproposalEvalTime = proEvlEvent.getTimeEvaluation().toString();
        String propEvalHour[] = currentproposalEvalTime.split(":");

        if (Integer.parseInt(propEvalHour[0]) > 23) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PropEvlEventTimeVal"));
        }
      }

      // Integrate the Proposal attribute (created under Open Envelop committee) with Proposal
      // Evaluation Event
      if (proEvlEvent.getEscmProposalAttrList().size() == 0) {
        ProposalEvaluationEventDAO.integProsalAtttoProsalEvent(proEvlEvent, event, null, entities);
      }

      // dont allow to change the bid if proposal evl event having the lines
      if (proEvlEvent.isDeletelines() != null && proEvlEvent.isDeletelines()
          && ((event.getCurrentState(bid) == null && event.getPreviousState(bid) != null)
              || (event.getCurrentState(bid) != null
                  && !event.getCurrentState(bid).equals(event.getPreviousState(bid))))
          && proEvlEvent.getEscmProposalAttrList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_DontAllowToChangeBid"));
      }

      /*
       * Task 7224 // Announcement is mandatory if bid is tender. if (proEvlEvent.getBidNo() != null
       * && proEvlEvent.getBidNo().getBidtype().equals("TR") && proEvlEvent.getEscmAnnoucements() ==
       * null) { throw new OBException(OBMessageUtils.messageBD("ESCM_ProposalEvlEvent_AnctMad")); }
       */
      // ProposalEvaluationEventDAO.updatebank(proEvlEvent);

      if (proEvlEvent.getBidNo() != null && proEvlEvent.getBidNo().isPartialaward()) {
        event.setCurrentState(ispartialaward, true);
      } else {
        event.setCurrentState(ispartialaward, false);
        event.setCurrentState(awardAllQty, false);
      }

    } catch (OBException e) {
      log.error("Exception while updating PO Receipt:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating PO Receipt:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
