package sa.elm.ob.scm.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
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

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.utility.util.Utility;

public class OpenEnvelopCommiteeEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmopenenvcommitee.ENTITY_NAME) };

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
      Escmopenenvcommitee openenvcommitee = (Escmopenenvcommitee) event.getTargetInstance();
      final Property eventno = entities[0].getProperty(Escmopenenvcommitee.PROPERTY_EVENTNO);
      final Property proposalcount = entities[0]
          .getProperty(Escmopenenvcommitee.PROPERTY_PROPOSALCOUNT);
      final Property returnaddlines = entities[0]
          .getProperty(Escmopenenvcommitee.PROPERTY_REFBUTTON);
      final Property appbudget = entities[0].getProperty(Escmopenenvcommitee.PROPERTY_APPROVEDBUD);
      final Property contCategory = entities[0]
          .getProperty(Escmopenenvcommitee.PROPERTY_CONTRACTTYPE);

      String sequence = "";
      Boolean sequenceexists = false;
      Query query = null;
      String strQuery = "";
      List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
      EscmProposalMgmt proposalObj = null;

      // set new Spec No
      sequence = Utility.getTransactionSequencewithclient("0", openenvcommitee.getClient().getId(),
          "OEC");
      sequenceexists = Utility.chkTransactionSequencewithclient(
          openenvcommitee.getOrganization().getId(), openenvcommitee.getClient().getId(), "OEC",
          sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(eventno, sequence);

      }

      OBQuery<EscmProposalMgmt> proposalmgmt = OBDal.getInstance().createQuery(
          EscmProposalMgmt.class,
          " client.id=:clientID and escmBidmgmt.id =:bidID and proposalstatus='SUB'");
      proposalmgmt.setNamedParameter("clientID", openenvcommitee.getClient().getId());
      proposalmgmt.setNamedParameter("bidID", openenvcommitee.getBidNo().getId());
      event.setCurrentState(proposalcount, new Long(proposalmgmt.list().size()));

      if (proposalmgmt.list().size() > 0) {
        event.setCurrentState(returnaddlines, true);
      }
      EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class,
          openenvcommitee.getBidNo().getId());
      event.setCurrentState(appbudget, bidmgmt.getApprovedbudget().toString());

      strQuery = " select max(escm_biddates.openenvday) from escm_biddates "
          + " left join escm_bidmgmt on escm_biddates.escm_bidmgmt_id = escm_bidmgmt.escm_bidmgmt_id "
          + "where escm_bidmgmt.escm_bidmgmt_id =:bidID ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter("bidID", openenvcommitee.getBidNo().getId());
      if (query != null && query.list().size() > 0) {
        Object row = query.list().get(0);
        if (row != null) {
          if (openenvcommitee.getTodaydate().compareTo((Date) row) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Escm_openEnv_Date"));
          }
        }
      }

      if (bidmgmt != null) {
        OBQuery<EscmProposalMgmt> proposalQry = OBDal.getInstance()
            .createQuery(EscmProposalMgmt.class, " as e where e.escmBidmgmt.id=:bidId and"
                + " e.contractType is not null  order by e.creationDate desc  ");
        proposalQry.setNamedParameter("bidId", bidmgmt.getId());
        proposalList = proposalQry.list();
        if (proposalList.size() > 0) {
          proposalObj = proposalList.get(0);
          if (proposalObj.getContractType() != null && !proposalObj.getContractType().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Contract_Inactive_Proposal"));
          } else {
            event.setCurrentState(contCategory, proposalObj.getContractType());
          }
        } else {
          if (bidmgmt.getContractType() != null && !bidmgmt.getContractType().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Contract_Inactive_Bid"));
          } else {
            event.setCurrentState(contCategory, bidmgmt.getContractType());
          }
        }
      }

    } catch (OBException e) {
      log.error("exception while creating OpenEnvelopCommitee", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating OpenEnvelopCommitee", e);
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
      Query query = null;
      String strQuery = "";
      Escmopenenvcommitee opencommitee = (Escmopenenvcommitee) event.getTargetInstance();
      final Property tdydate = entities[0].getProperty(Escmopenenvcommitee.PROPERTY_TODAYDATE);
      EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class,
          opencommitee.getBidNo().getId());
      List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
      EscmProposalMgmt proposalObj = null;
      final Property contCategory = entities[0]
          .getProperty(Escmopenenvcommitee.PROPERTY_CONTRACTTYPE);

      if (opencommitee.getBidNo() != null) {
        strQuery = " select max(escm_biddates.openenvday) from escm_biddates "
            + " left join escm_bidmgmt on escm_biddates.escm_bidmgmt_id = escm_bidmgmt.escm_bidmgmt_id "
            + "where escm_bidmgmt.escm_bidmgmt_id =:bidID ";
        query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        query.setParameter("bidID", opencommitee.getBidNo().getId());
        if (query != null && query.list().size() > 0) {
          Object row = query.list().get(0);
          if (row != null) {
            if (!event.getCurrentState(tdydate).equals(event.getPreviousState(tdydate))) {
              if (opencommitee.getTodaydate().compareTo((Date) row) < 0) {
                throw new OBException(OBMessageUtils.messageBD("Escm_openEnv_Date"));

              }
            }
          }
        }
      }

      if (bidmgmt != null) {
        OBQuery<EscmProposalMgmt> proposalQry = OBDal.getInstance()
            .createQuery(EscmProposalMgmt.class, " as e where e.escmBidmgmt.id=:bidId and"
                + " e.contractType is not null  order by e.creationDate desc  ");
        proposalQry.setNamedParameter("bidId", bidmgmt.getId());
        proposalList = proposalQry.list();
        if (proposalList.size() > 0) {
          proposalObj = proposalList.get(0);
          if (proposalObj.getContractType() != null && !proposalObj.getContractType().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Contract_Inactive_Proposal"));
          } else {
            event.setCurrentState(contCategory, proposalObj.getContractType());
          }
        } else {
          if (bidmgmt.getContractType() != null && !bidmgmt.getContractType().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Contract_Inactive_Bid"));
          } else {
            event.setCurrentState(contCategory, bidmgmt.getContractType());
          }
        }
      }
    } catch (OBException e) {
      log.error("exception while updating OpenEnvelopCommitee", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating OpenEnvelopCommitee", e);
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
      Escmopenenvcommitee opencommitee = (Escmopenenvcommitee) event.getTargetInstance();
      if (opencommitee.getAlertStatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_openenvelop_delete"));
      }
      for (EscmProposalAttribute attr : opencommitee.getEscmProposalAttrList()) {
        if ((attr.getEscmBankguaranteeDetailList() != null
            && attr.getEscmBankguaranteeDetailList().size() > 0)
            || (attr.getESCMBGWorkbenchList() != null
                && attr.getESCMBGWorkbenchList().size() > 0)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_cantdelete_openenv"));
        }
      }
    } catch (OBException e) {
      log.error("exception while deleting openenvelopevent: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting openenvelopevent: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
