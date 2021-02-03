package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCM_Proposal_CommentAttr;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.event.dao.TechnicalEvaluationEventDAO;
import sa.elm.ob.utility.util.Utility;

public class TechnicalEvaluationEvent extends EntityPersistenceEventObserver {
  private Logger log = Logger.getLogger(this.getClass());

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmTechnicalevlEvent.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  // save event
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmTechnicalevlEvent proEvlEvent = (EscmTechnicalevlEvent) event.getTargetInstance();
      final Property eventno = entities[0].getProperty(EscmTechnicalevlEvent.PROPERTY_EVENTNO);
      List<EscmProposalMgmt> proposaList = new ArrayList<EscmProposalMgmt>();
      StringBuilder proposalNo = new StringBuilder();

      String sequence = "";
      Boolean sequenceexists = false;

      // set new Spec No
      sequence = Utility.getTransactionSequencewithclient("0", proEvlEvent.getClient().getId(),
          "TEE");

      // check already same spec no exists or not.
      sequenceexists = Utility.chkTransactionSequencewithclient(
          proEvlEvent.getOrganization().getId(), proEvlEvent.getClient().getId(), "TEE", sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }

      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(eventno, sequence);
      }

      // Check if proposals has error and throw error
      EscmTechnicalevlEvent techEvlEvent = (EscmTechnicalevlEvent) event.getTargetInstance();
      if (techEvlEvent.getBidNo() != null) {
        EscmBidMgmt bid = techEvlEvent.getBidNo();
        OBQuery<EscmProposalMgmt> proposals = OBDal.getInstance().createQuery(
            EscmProposalMgmt.class, "as e where  e.escmBidmgmt.id=:bidId and e.totalamount=0");
        proposals.setNamedParameter("bidId", bid.getId());
        proposaList = proposals.list();
        if (proposaList.size() > 0) {
          for (EscmProposalMgmt prop : proposaList) {
            proposalNo.append(prop.getProposalno());
            proposalNo.append(",");
          }
        }
        if (proposalNo != null && proposalNo.length() != 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Proposal_AmtZero").replace("%",
              proposalNo.substring(0, proposalNo.length() - 1)));
        }

        // Task no:8327 check if proposal have tax , if not then throw error
        for (EscmProposalMgmt proposalObj : bid.getEscmProposalManagementList()) {
          if (!proposalObj.isTaxLine() || (proposalObj.isTaxLine()
              && proposalObj.getTotalTaxAmount().compareTo(BigDecimal.ZERO) == 0)) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_TaxIsMandatoryProposal"));
          }
        }
      }

      // Integrate the Proposal attribute (created under Open Envelop committee) with Technical
      // Evaluation Event

      TechnicalEvaluationEventDAO.integProsalAtttoTechEvent(proEvlEvent, null, event, entities);

      // Announcement is mandatory if bid is tender.
      // Task No:7224
      // if (proEvlEvent.getBidNo() != null && proEvlEvent.getBidNo().getBidtype().equals("TR")
      // && proEvlEvent.getAnnouncementID() == null) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_ProposalEvlEvent_AnctMad"));
      // }
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

  // update event
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EscmTechnicalevlEvent techEvent = (EscmTechnicalevlEvent) event.getTargetInstance();

      // final Property bid = entities[0].getProperty(EscmTechnicalevlEvent.PROPERTY_BIDNO);
      // final Property announcement = entities[0]
      // .getProperty(EscmTechnicalevlEvent.PROPERTY_ANNOUNCEMENTID);
      // Announcement is mandatory if bid is tender.

      /*
       * Task 7224 log.debug("cu:" + event.getCurrentState(announcement)); if
       * ((!event.getCurrentState(bid).equals(event.getPreviousState(bid))) ||
       * event.getCurrentState(announcement) == null || (event.getCurrentState(announcement) != null
       * && !event.getCurrentState(announcement) .equals(event.getPreviousState(announcement)))) {
       * if (techEvent.getBidNo() != null && techEvent.getBidNo().getBidtype().equals("TR") &&
       * techEvent.getAnnouncementID() == null) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_ProposalEvlEvent_AnctMad")); } }
       */
      // Check Proposal matches with the selected bid
      boolean proposalMatches = TechnicalEvaluationEventDAO.checkProposals(techEvent,
          techEvent.getBidNo().getId());
      if (!proposalMatches) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_TechEvalPropNotMatches"));
      }
    } catch (OBException e) {
      log.error("exception while creating Proposal Evaluation event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating Proposal Evaluation event", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      /* OBContext.restorePreviousMode(); */
    }
  }

  // delete event
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      List<EscmProposalAttribute> proAttList = new ArrayList<EscmProposalAttribute>();
      List<ESCM_Proposal_CommentAttr> proCommAttList = new ArrayList<ESCM_Proposal_CommentAttr>();
      List<EscmProposalmgmtLine> proposallineList = new ArrayList<EscmProposalmgmtLine>();
      EscmTechnicalevlEvent techEvent = (EscmTechnicalevlEvent) event.getTargetInstance();

      // dont allwo to delete the inprogress record
      if (techEvent.getStatus().equals("CO") || techEvent.getStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_TEE_CantDelCom"));
      }

      // update the proposal attribute values before delete the TEE
      OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
          EscmProposalAttribute.class, " as e where e.escmTechnicalevlEvent.id=:techEventID");
      proposalatt.setNamedParameter("techEventID", techEvent.getId());
      proAttList = proposalatt.list();
      if (proAttList.size() > 0) {
        for (EscmProposalAttribute proatt : proAttList) {
          proatt.setEscmTechnicalevlEvent(null);
          proatt.setTechevalDecision(null);
          proatt.setTechVariation(BigDecimal.ZERO);
          proatt.setTechnicalDiscount(BigDecimal.ZERO);
          // proatt.setTechnicalDiscountamt(BigDecimal.ZERO);
          // proatt.setTechNegotiatedPrice(proatt.getGrossPrice());
          OBDal.getInstance().save(proatt);

          // update the proposal line values before delete the TEE
          OBQuery<EscmProposalmgmtLine> proposallineQry = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id=:proposalID ");
          proposallineQry.setNamedParameter("proposalID", proatt.getEscmProposalmgmt().getId());
          proposallineList = proposallineQry.list();
          if (proposallineList.size() > 0) {
            for (EscmProposalmgmtLine proposalline : proposallineList) {
              proposalline.setTechDiscount(BigDecimal.ZERO);
              proposalline.setTechDiscountamt(BigDecimal.ZERO);
              proposalline.setTechLineTotal(BigDecimal.ZERO);
              proposalline.setTechLineQty(BigDecimal.ZERO);
              proposalline.setTechUnitPrice(BigDecimal.ZERO);
              proposalline.setPEELineTotal(BigDecimal.ZERO);
              proposalline.setPEEQty(BigDecimal.ZERO);
              proposalline.setPEENegotUnitPrice(BigDecimal.ZERO);
              proposalline.setTEELineTaxamt(BigDecimal.ZERO);
              proposalline.setPEELineTaxamt(BigDecimal.ZERO);
              /*
               * proposalline.setTechLineTotal(
               * proposalline.getTechLineQty().multiply(proposalline.getTechUnitPrice()));
               */
              OBDal.getInstance().save(proposalline);
            }
          }

          // update the proposal comments attribute values before delete the TEE
          OBQuery<ESCM_Proposal_CommentAttr> proposalcommattQry = OBDal.getInstance().createQuery(
              ESCM_Proposal_CommentAttr.class,
              " as e where e.istechevent='Y' and  e.escmProposalAttr.id=:proattID");
          proposalcommattQry.setNamedParameter("proattID", proatt.getId());
          proCommAttList = proposalcommattQry.list();
          if (proCommAttList.size() > 0) {
            for (ESCM_Proposal_CommentAttr procomatt : proCommAttList) {
              OBDal.getInstance().remove(procomatt);
            }
          }

        }
        OBDal.getInstance().flush();
      }

    } catch (OBException e) {
      log.error("Exception while deleting the TEE:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting the TEE:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
