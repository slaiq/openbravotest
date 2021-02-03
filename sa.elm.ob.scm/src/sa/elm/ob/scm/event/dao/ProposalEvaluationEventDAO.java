package sa.elm.ob.scm.event.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMCommRecommendation;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

//ProposalEvaluation Event DAO file
public class ProposalEvaluationEventDAO {
  private static final Logger log = LoggerFactory.getLogger(ProposalEvaluationEventDAO.class);

  /**
   * Integrate the Proposal attribute (created under Open Envelop committee) with Proposal
   * Evaluation Event .update the Proposal Event id in Proposal attribute
   * 
   * @param proEvlEvent
   *          object
   */
  public static void integProsalAtttoProsalEvent(ESCMProposalEvlEvent proEvlEvent,
      EntityUpdateEvent upevent, EntityNewEvent newevent, Entity[] entities) {
    String strQuery = null;
    Query query = null;
    BigDecimal PRToal = BigDecimal.ZERO;
    BigDecimal grossPrice = BigDecimal.ZERO;
    BigDecimal netPrice = BigDecimal.ZERO;
    List<ESCMBGWorkbench> bglist = new ArrayList<ESCMBGWorkbench>();
    Integer count = 0;
    // int proposalcount = 0;
    try {
      OBContext.setAdminMode();
      final Property deletelines = entities[0]
          .getProperty(ESCMProposalEvlEvent.PROPERTY_DELETELINES);
      // final Property proposalcounts = entities[0]
      // .getProperty(ESCMProposalEvlEvent.PROPERTY_PROPOSALCOUNTS);
      // if bid is tender/ limited then once bid is associated with Proposal Evaluation event change
      // the Proposal status as Analysis and update the event id,estimated price,variation and net
      // price
      if (proEvlEvent.getBidNo() != null && (proEvlEvent.getBidNo().getBidtype().equals("TR")
          || proEvlEvent.getBidNo().getBidtype().equals("LD"))) {
        OBQuery<EscmProposalMgmt> proposal = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
            " as e where e.escmBidmgmt.id=:bidId and e.proposalstatus='TER'");
        proposal.setNamedParameter("bidId", proEvlEvent.getBidNo().getId());
        if (proposal.list().size() > 0) {
          for (EscmProposalMgmt pro : proposal.list()) {
            count = 0;
            // take the count of bg to conside that proposal while
            strQuery = " select case when bgdetcount= actcount then 1 else 0 end  from escm_bgworkbench bg "
                + " join ( select count(escm_bankguarantee_detail_id)  as bgdetcount,escm_bgworkbench_id  from escm_bankguarantee_detail group by escm_bgworkbench_id ) "
                + " bgdet on bgdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
                + " join ( select count(escm_bankguarantee_detail_id) as actcount,escm_bgworkbench_id  from escm_bankguarantee_detail where  bgstatus not in ('REL','CON','EXP') "
                + "  group by  escm_bgworkbench_id )  actdet on actdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
                + " where bg.document_no  ='" + pro.getId() + "'"
                + " and bg.document_type='P' and bg.bghdstatus='CO' ";
            query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
            log.debug("st:" + query.toString());

            if (query != null && query.list().size() > 0) {
              count = (Integer) query.list().get(0);
            }
            log.debug("count:" + count);
            if (count == 0)
              continue;
            pro.setProposalstatus("ANY");

            OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
                EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalId"
                    + " and e.escmOpenenvcommitee.id is not null  ");
            proposalatt.setNamedParameter("proposalId", pro.getId());

            if (proposalatt.list().size() > 0) {
              for (EscmProposalAttribute att : proposalatt.list()) {
                // proposalcount++;
                grossPrice = BigDecimal.ZERO;
                netPrice = BigDecimal.ZERO;
                PRToal = BigDecimal.ZERO;

                // update the proposal attribute net price and proposal event header id
                // calculate Gross and net price for proposal attr header
                for (EscmProposalmgmtLine line : att.getEscmProposalmgmt()
                    .getEscmProposalmgmtLineList()) {
                  line.setPEELineTaxamt(
                      line.getTaxAmount() == null ? BigDecimal.ZERO : line.getTaxAmount());
                  if (!line.isSummary()) {
                    grossPrice = grossPrice
                        .add((line.getMovementQuantity()).multiply(line.getGrossUnitPrice()));
                    netPrice = netPrice.add(line.getLineTotal());

                    // netPrice = netPrice.add(line.getNegotUnitPrice());

                  }
                  // start -calculate the estimate price based on bid - requisition line
                  strQuery = " select case when coalesce(sum(reqln.qty),0) > 0 then "
                      + " round((coalesce(sum(reqln.linenetamt),0)/coalesce(sum(reqln.qty),0))*coalesce(sum(srcref.quantity),0),2) else 0 end   "
                      + " from escm_proposalmgmt   pro join  escm_proposalmgmt_line proline on proline.escm_proposalmgmt_id =pro.escm_proposalmgmt_id join  escm_bidmgmt_line line on line.escm_bidmgmt_id =pro.escm_bidmgmt_id and proline.description = line.description "
                      + " join  escm_bidsourceref srcref on srcref.escm_bidmgmt_line_id= line.escm_bidmgmt_line_id "
                      + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id "
                      + " where proline.escm_proposalmgmt_line_id = ? and reqln.em_escm_issummary  ='N' and proline.issummarylevel  ='N'";

                  query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
                  query.setParameter(0, line.getId());
                  log.debug("strQuery:" + query);
                  if (query != null && query.list().size() > 0) {
                    PRToal = PRToal.add((BigDecimal) query.list().get(0));
                  }
                  // end -calculate the estimate price
                  if (!line.isSummary()) {
                    line.setPEEInitUnitprice(line.getNegotUnitPrice().add(line.getUnittax())
                        .setScale(2, RoundingMode.HALF_UP));
                  }
                  OBDal.getInstance().save(line);
                }
                att.setOrganization(pro.getOrganization());
                att.setProsalGrossprice(grossPrice);
                att.setProsalNetprice(netPrice);
                att.setProsalDiscountamt(grossPrice.subtract(netPrice));
                att.setPEEIstax(pro.isTaxLine());
                att.setPEEEfinTaxMethod(pro.getEfinTaxMethod());
                att.setPEETotalTaxamt(
                    pro.getTotalTaxAmount() == null ? BigDecimal.ZERO : pro.getTotalTaxAmount());

                if (att.getProsalDiscountamt().compareTo(BigDecimal.ZERO) > 0)
                  att.setProsalDiscount((att.getProsalDiscountamt().multiply(new BigDecimal("100")))
                      .divide(att.getProsalGrossprice(), 2, RoundingMode.HALF_UP));
                if (att.getProsalNetprice() != null)
                  att.setNegotiatedPrice(att.getProsalNetprice());
                att.setEscmProposalevlEvent(proEvlEvent);
                if (att.getEscmProposalmgmt().getProjectduration() != null)
                  att.setProjectduration(att.getEscmProposalmgmt().getProjectduration());

                if (PRToal.compareTo(BigDecimal.ZERO) > 0) {
                  att.setEstimatedPrice(PRToal);
                }

                // calculate the variation between estimated price and proposal net price
                if ((PRToal.compareTo(BigDecimal.ZERO) > 0) && att.getProsalNetprice() != null) {
                  att.setVariation(((att.getEstimatedPrice().subtract(att.getProsalNetprice()))
                      .divide(att.getEstimatedPrice(), 4, RoundingMode.HALF_UP))
                          .multiply(new BigDecimal(100)));
                }
                att.setEfinBudgetinitial(pro.getEfinBudgetinitial());
                OBDal.getInstance().save(att);

                // update the bank guarantee details
                OBQuery<ESCMBGWorkbench> bgdetail = OBDal.getInstance().createQuery(
                    ESCMBGWorkbench.class, " as e where e.escmProposalmgmt.id=:proposalId");
                bgdetail.setNamedParameter("proposalId", att.getEscmProposalmgmt().getId());
                bglist = bgdetail.list();
                if (bglist.size() > 0) {
                  for (ESCMBGWorkbench bg : bglist) {
                    bg.setEscmProposalAttr(att);
                    OBDal.getInstance().save(bg);
                  }
                }
              }
            }
          }
        }

        if (upevent != null) {
          upevent.setCurrentState(deletelines, true);
          // upevent.setCurrentState(proposalcounts, (long) proposalcount);

        } else {
          newevent.setCurrentState(deletelines, true);
          // newevent.setCurrentState(proposalcounts, (long) proposalcount);
        }

        OBQuery<ESCMCommRecommendation> comrecom = OBDal.getInstance()
            .createQuery(ESCMCommRecommendation.class, " as e where e.escmTechnicalevlEvent.id=("
                + " select e.id from escm_technicalevl_event e where e.bidNo.id=:bidId)");
        comrecom.setNamedParameter("bidId", proEvlEvent.getBidNo().getId());
        if (comrecom.list().size() > 0) {
          for (ESCMCommRecommendation com : comrecom.list()) {
            com.setEscmProposalevlEvent(proEvlEvent);
            OBDal.getInstance().save(com);
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while integProsalAtttoProsalEvent:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}

class BankGuaranteeComp implements Comparator<BankGuaranteeVO> {

  @Override
  public int compare(BankGuaranteeVO e1, BankGuaranteeVO e2) {
    if (e1.getAmount().compareTo(e2.getAmount()) <= 0) {
      return 1;
    } else {
      return -1;
    }
  }
}

class BankGuaranteeVO {

  private String proposaId;
  private String proposalAttId;

  private BigDecimal proposalAmt;

  public BankGuaranteeVO(String n, BigDecimal s, String b) {
    this.proposaId = n;
    this.proposalAmt = s;
    this.proposalAttId = b;
  }

  public String getProposalId() {
    return proposaId;
  }

  public String getProposalAttId() {
    return proposalAttId;
  }

  public void setProposalAttId(String proposalAttId) {
    this.proposalAttId = proposalAttId;
  }

  public void setProposalId(String proposaId) {
    this.proposaId = proposaId;
  }

  public BigDecimal getAmount() {
    return proposalAmt;
  }

  public void setSalary(BigDecimal proposalAmt) {
    this.proposalAmt = proposalAmt;
  }

  public String toString() {
    return "proposaId: " + this.proposaId + "-- proposalAmt: " + this.proposalAmt;
  }
}
