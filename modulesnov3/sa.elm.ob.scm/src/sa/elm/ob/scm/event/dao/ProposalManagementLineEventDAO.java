package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;

//ProposalManagement Line Event DAO file
public class ProposalManagementLineEventDAO {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementActionMethod.class);

  /**
   * update Proposal Attribute Negotiated Price (Gross Price,Net Price,Discount,Discount Amt)
   * 
   * @param proposalmgmtLine
   * @param event
   * @param negotiatePrice
   */
  public static void updateProsalAttNegPrice(EscmProposalmgmtLine proposalmgmtLine,
      EntityUpdateEvent event, Property negotiatePrice, Property lineTotal) {
    BigDecimal totalNegPrice = BigDecimal.ZERO, oldNegPrice = BigDecimal.ZERO,
        newNegPrice = BigDecimal.ZERO;
    BigDecimal grossPrice = BigDecimal.ZERO;
    BigDecimal netPrice = BigDecimal.ZERO, negPrice = BigDecimal.ZERO;
    List<EscmProposalAttribute> attlist = new ArrayList<EscmProposalAttribute>();
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
          EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalID ");
      proposalatt.setNamedParameter("proposalID", proposalmgmtLine.getEscmProposalmgmt().getId());
      proposalatt.setMaxResult(1);
      attlist = proposalatt.list();
      if (attlist.size() > 0) {
        EscmProposalAttribute att = attlist.get(0);

        if (!event.getCurrentState(negotiatePrice).equals(event.getPreviousState(negotiatePrice))
            && (!event.getCurrentState(negotiatePrice).equals(proposalmgmtLine.getNetprice()))) {
          oldNegPrice = (BigDecimal) event.getPreviousState(negotiatePrice);
          /*
           * totalNegPrice = proposalmgmtLine.getNegotUnitPrice().subtract(oldNegPrice); newNegPrice
           * = att.getNegotiatedPrice()
           * .add(totalNegPrice.multiply((proposalmgmtLine.getMovementQuantity())));
           */
          totalNegPrice = proposalmgmtLine.getPEENegotUnitPrice().subtract(oldNegPrice);
          newNegPrice = att.getNegotiatedPrice()
              .add(totalNegPrice.multiply((proposalmgmtLine.getPEEQty())));
          att.setNegotiatedPrice(newNegPrice);
        }

        if (!event.getCurrentState(lineTotal).equals(event.getPreviousState(lineTotal))) { // &&
                                                                                           // !event.getCurrentState(lineTotal).equals(att.getNetPrice())
          for (EscmProposalmgmtLine line : att.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {
            if ((line.getPeestatus() == null) || (!line.getPeestatus().equals("CL"))) {
              if (!line.isSummary()) {
                /*
                 * grossPrice = grossPrice
                 * .add((line.getMovementQuantity()).multiply(line.getGrossUnitPrice()));
                 */

                // grossPrice =
                // grossPrice.add((line.getPEEQty()).multiply(line.getGrossUnitPrice()));
                if (line.getPEENegotUnitPrice() != null) {
                  grossPrice = grossPrice
                      .add((line.getPEEQty()).multiply(line.getPEENegotUnitPrice()));
                }
                /*
                 * netPrice =
                 * netPrice.add((line.getMovementQuantity()).multiply(line.getNetprice())); negPrice
                 * = negPrice .add((line.getMovementQuantity()).multiply(line.getNegotUnitPrice()));
                 */
                // negPrice = negPrice.add(line.getLineTotal());
                // netPrice = netPrice.add(line.getLineTotal());

                negPrice = negPrice.add(line.getPEELineTotal());
                netPrice = netPrice.add(line.getPEELineTotal());

              }
            }
          }

          att.setProsalGrossprice(grossPrice);
          // att.setProsalNetprice(netPrice);
          att.setProsalDiscountamt(grossPrice.subtract(netPrice));
          if (att.getProsalDiscountamt().compareTo(BigDecimal.ZERO) > 0)
            att.setProsalDiscount((att.getProsalDiscountamt().multiply(new BigDecimal("100")))
                .divide(att.getProsalGrossprice(), 2, RoundingMode.HALF_UP));
          if (att.getNegotiatedPrice() != null && negPrice.compareTo(att.getNegotiatedPrice()) != 0)
            att.setNegotiatedPrice(negPrice);

          if ((att.getEstimatedPrice().compareTo(BigDecimal.ZERO) > 0)
              && att.getNetPrice() != null) {
            att.setVariation(((att.getEstimatedPrice().subtract(att.getProsalNetprice()))
                .divide(att.getEstimatedPrice(), 4, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(100)));
          }
        }
        OBDal.getInstance().save(att);

      }
    } catch (OBException e) {
      log.error("Exception while updateProsalAttPrices:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public static boolean chkProsalAttlinkWithProornot(EscmProposalMgmt proposalmgmt) {
    List<ESCMProposalEvlEvent> eventlist = new ArrayList<ESCMProposalEvlEvent>();

    try {
      OBContext.setAdminMode();
      OBQuery<ESCMProposalEvlEvent> proposalevent = OBDal.getInstance().createQuery(
          ESCMProposalEvlEvent.class,
          " as e where e.id in ( select attr.escmProposalevlEvent.id "
              + " from escm_proposal_attr attr where attr.escmProposalmgmt.id=:proposalID "
              + " and attr.escmProposalevlEvent is not null ) and   e.deletelines='N'  ");
      proposalevent.setNamedParameter("proposalID", proposalmgmt.getId());
      proposalevent.setMaxResult(1);
      eventlist = proposalevent.list();
      if (eventlist.size() > 0) {
        return true;
      } else
        return false;
    } catch (OBException e) {
      log.error("Exception while chkProsalAttlinkWithProornot:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Method to check whether the open envelope event is completed or not
   * 
   * @param bidId
   * @return
   */
  public static boolean chkopenvelopcomornot(String bidId) {
    List<Escmopenenvcommitee> oplist = new ArrayList<Escmopenenvcommitee>();
    try {
      OBContext.setAdminMode();
      OBQuery<Escmopenenvcommitee> openenvelop = OBDal.getInstance().createQuery(
          Escmopenenvcommitee.class, " as e where e.bidNo.id=:bidID and e.alertStatus='CO'");
      openenvelop.setNamedParameter("bidID", bidId);
      oplist = openenvelop.list();
      if (oplist.size() > 0) {
        return true;
      } else
        return false;
    } catch (OBException e) {
      log.error("Exception while chkProsalAttlinkWithProornot:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * To update technical evaluation event header negotiated price
   * 
   * @param proposalmgmtLine
   * @param event
   * @param techLineTotal
   */
  public static void updateTechnicalNegPrice(EscmProposalmgmtLine proposalmgmtLine,
      EntityUpdateEvent event, Property techLineTotal) {
    List<EscmProposalAttribute> attlist = new ArrayList<EscmProposalAttribute>();
    BigDecimal attrNegotiatePrice = BigDecimal.ZERO;
    BigDecimal discountAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
          EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalID ");
      proposalatt.setNamedParameter("proposalID", proposalmgmtLine.getEscmProposalmgmt().getId());
      proposalatt.setMaxResult(1);
      attlist = proposalatt.list();
      if (attlist.size() > 0) {
        EscmProposalAttribute att = attlist.get(0);
        EscmProposalMgmt proposal = att.getEscmProposalmgmt();
        if (proposal != null && proposal.getEscmProposalmgmtLineList().size() > 0) {
          for (EscmProposalmgmtLine objTechLine : proposal.getEscmProposalmgmtLineList()) {
            if (!objTechLine.isSummary()) {
              attrNegotiatePrice = attrNegotiatePrice.add(objTechLine.getTechLineTotal());
              if (objTechLine.getTechDiscountamt() != null)
                discountAmt = discountAmt.add(objTechLine.getTechDiscountamt());
            }
          }
        }
        att.setTechNegotiatedPrice(attrNegotiatePrice);
        att.setTechnicalDiscountamt(discountAmt);
        OBDal.getInstance().save(att);
      }

    } catch (OBException e) {
      log.error("Exception while updateTechnicalNegPrice:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}