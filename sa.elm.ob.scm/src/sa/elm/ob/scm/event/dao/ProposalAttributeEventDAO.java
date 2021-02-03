package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

//ProposalAttribute Event DAO file
public class ProposalAttributeEventDAO {
  private static final Logger log = LoggerFactory.getLogger(ProposalAttributeEventDAO.class);

  /**
   * before delete the Proposal Attribute , need to update the Proposal lines
   * 
   * @param proposalattribute
   * @param proposal
   * @param event
   */
  public static void updateProposalLineDetails(EscmProposalAttribute proposalattribute,
      EscmProposalMgmt proposal, ESCMProposalEvlEvent event) {
    BigDecimal lineTotal = BigDecimal.ZERO;
    EscmProposalAttribute attr = proposalattribute;
    int count = 0;
    try {
      OBContext.setAdminMode();

      // set the evaluation event delete line flag as false
      event.setDeletelines(false);
      OBDal.getInstance().save(event);
      OBDal.getInstance().flush();

      count = event.getEscmProposalAttrList().size();

      // line updation
      for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
        lineTotal = line.getNetprice().multiply((line.getMovementQuantity()));
        if ((line.getNegotUnitPrice().compareTo(line.getNetprice()) != 0)) {
          if (!proposal.isTaxLine()) {
            line.setNegotUnitPrice(line.getNetprice());
            line.setLineTotal(lineTotal);
          }

          OBDal.getInstance().save(line);
        }
      }

      // if proposal attribute count is greater than one, again update the delete line flag as true
      if (count > 1) {
        event.setDeletelines(true);
        OBDal.getInstance().save(event);
      }

      // update the corresponding proposal status as Submitted
      proposal.setProposalstatus("SUB");
      OBDal.getInstance().save(proposal);

      // remove the attribute obj from the Evaluation event Attribute List
      event.getEscmProposalAttrList().remove(attr);
      /*
       * event.setProposalCounts((long) (event.getEscmProposalAttrList().size()));
       * OBDal.getInstance().save(event);
       */
    } catch (OBException e) {
      log.error("Exception while updateProposalLineDetails:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}