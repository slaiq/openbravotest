package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;

//ProposalEvaluation Event DAO file
public class TechnicalEvaluationEventDAO {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementActionMethod.class);

  /**
   * Integrate the Proposal attribute (created under Open Envelop committee) with Proposal
   * Evaluation Event .update the Technical Event id in Proposal attribute
   * 
   * @param TechEvlEvent
   *          object
   */
  public static void integProsalAtttoTechEvent(EscmTechnicalevlEvent techEvlEvent,
      EntityUpdateEvent upevent, EntityNewEvent newevent, Entity[] entities) {
    List<EscmProposalAttribute> proAttList = new ArrayList<EscmProposalAttribute>();
    BigDecimal difference = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();

      if (techEvlEvent.getBidNo() != null) {
        OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class,
            " as e where e.escmOpenenvcommitee.id is not null "
                + " and e.escmOpenenvcommitee.id=( select ope.id from escm_openenvcommitee ope where ope.bidNo='"
                + techEvlEvent.getBidNo().getId() + "' and ope.alertStatus='CO') ");
        proAttList = proposalatt.list();
        if (proAttList.size() > 0) {
          for (EscmProposalAttribute proatt : proAttList) {

            proatt.setTechNegotiatedPrice(proatt.getNetPrice());
            //
            // proatt.setTechnicalDiscount(proatt.getDiscount());
            //
            if (techEvlEvent.getEstimatedPrice() != null
                && techEvlEvent.getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0) {
              difference = techEvlEvent.getEstimatedPrice()
                  .subtract(proatt.getTechNegotiatedPrice())
                  .divide(techEvlEvent.getEstimatedPrice(), 2, RoundingMode.FLOOR);
              proatt.setTechVariation(difference);
            } else {
              proatt.setTechVariation(BigDecimal.ZERO);
            }
            if (proatt.getEscmProposalmgmt() != null) {
              EscmProposalMgmt objProposal = proatt.getEscmProposalmgmt();

              if (objProposal.getEscmProposalmgmtLineList().size() > 0) {
                for (EscmProposalmgmtLine objProposalLine : objProposal
                    .getEscmProposalmgmtLineList()) {
                  objProposalLine.setBaselineQuantity(objProposalLine.getMovementQuantity());
                  if ((objProposalLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                      && !objProposalLine.getEscmProposalmgmt().getEfinTaxMethod()
                          .isPriceIncludesTax())
                      || objProposalLine.getEscmProposalmgmt().getEfinTaxMethod() == null) {
                    // objProposalLine.setNegotUnitPrice(objProposalLine.getNetprice());
                    objProposalLine.setTechLineTotal(objProposalLine.getLineTotal());
                    // objProposalLine.setTechUnitPrice(objProposalLine.getUnitpricedis());
                    objProposalLine.setTEENetUnitprice(
                        objProposalLine.getNetUnitprice().setScale(2, RoundingMode.HALF_UP));
                  } else {
                    objProposalLine.setTEEUnitpricedis(objProposalLine.getNegotUnitPrice());
                    objProposalLine.setTEENetUnitprice(objProposalLine.getNegotUnitPrice());
                  }

                  objProposalLine.setTechUnitPrice(objProposalLine.getNegotUnitPrice());

                  objProposalLine.setTechLineQty(objProposalLine.getMovementQuantity());

                  if (objProposalLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                      && objProposalLine.getEscmProposalmgmt().getEfinTaxMethod()
                          .isPriceIncludesTax()) {
                    BigDecimal TotalTechLine = (objProposalLine.getMovementQuantity()
                        .multiply(objProposalLine.getNegotUnitPrice()))
                            .add(objProposalLine.getTaxAmount());
                    objProposalLine.setTechLineTotal(TotalTechLine);
                  }

                  objProposalLine
                      .setTEELineTaxamt(objProposalLine.getTaxAmount() == null ? BigDecimal.ZERO
                          : objProposalLine.getTaxAmount());
                  if (!objProposalLine.isSummary()) {
                    if (objProposalLine.getEscmProposalmgmt().getEfinTaxMethod() != null
                        && objProposalLine.getEscmProposalmgmt().getEfinTaxMethod()
                            .isPriceIncludesTax()
                        && objProposalLine.getEscmProposalmgmt().isTaxLine()) {
                      objProposalLine.setTEEInitUnitprice(objProposalLine.getNegotUnitPrice()
                          .add(objProposalLine.getUnittax()).setScale(2, RoundingMode.HALF_UP));
                    } else {
                      objProposalLine.setTEEInitUnitprice(objProposalLine.getNegotUnitPrice());
                    }

                  }

                }
              }
              proatt.setOrganization(objProposal.getOrganization());
              proatt.setTEEIstax(objProposal.isTaxLine());
              proatt.setTEEEfinTaxMethod(objProposal.getEfinTaxMethod());
              proatt.setTEETotalTaxamt(objProposal.getTotalTaxAmount() == null ? BigDecimal.ZERO
                  : objProposal.getTotalTaxAmount());

            }
            proatt.setNegotiatedPrice(proatt.getNetPrice());
            proatt.setEscmTechnicalevlEvent(techEvlEvent);
            OBDal.getInstance().save(proatt);
          }
        }
      }

    } catch (OBException e) {
      log.error("Exception while integProsalAtttoProsalEvent:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static boolean checkProposals(EscmTechnicalevlEvent techEvlEvent, String bidNo) {
    boolean proposalMatches = false;
    try {
      OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance()
          .createQuery(EscmProposalAttribute.class, " as e where e.escmTechnicalevlEvent.id='"
              + techEvlEvent.getId() + "' and e.escmOpenenvcommitee.id is not null "
              + " and e.escmOpenenvcommitee.id=( select ope.id from escm_openenvcommitee ope where ope.bidNo='"
              + bidNo + "' and ope.alertStatus='CO') ");
      if (proposalatt.list().size() > 0) {
        proposalMatches = true;
      } else
        proposalMatches = false;
    } catch (OBException e) {
      log.error("Exception while checkProposals:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return proposalMatches;
  }
}
