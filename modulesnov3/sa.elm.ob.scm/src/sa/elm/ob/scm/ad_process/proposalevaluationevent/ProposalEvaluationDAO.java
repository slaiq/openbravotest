package sa.elm.ob.scm.ad_process.proposalevaluationevent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian-Divya
 * 
 */
// BGWorkbenchDAO callout file
public class ProposalEvaluationDAO {
  private static final Logger log = LoggerFactory.getLogger(ProposalEvaluationDAO.class);

  /**
   * get valid BG(only consider BG Status is Active,Extend)
   * 
   * @param bidId
   * @param con
   * @return
   */
  public static String getValidBG(String bidId, Connection con) {
    String strQuery = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    String proposallistId = null;
    try {
      OBContext.setAdminMode();
      strQuery = " select  array_to_string(array_agg(bg.document_no),',') as proposalid  from escm_bgworkbench bg "
          + " join ( select count(escm_bankguarantee_detail_id)  as bgdetcount,escm_bgworkbench_id  from escm_bankguarantee_detail group by escm_bgworkbench_id ) "
          + " bgdet on bgdet.escm_bgworkbench_id= bg.escm_bgworkbench_id "
          + " join ( select count(escm_bankguarantee_detail_id) as actcount,escm_bgworkbench_id  from escm_bankguarantee_detail where  bgstatus not in ('REL','CON','EXP') "
          + "  group by  escm_bgworkbench_id )  actdet on actdet.escm_bgworkbench_id= bg.escm_bgworkbench_id  left   join escm_proposal_attr attr on attr.escm_proposalmgmt_id= bg.document_no "
          + " left join escm_proposalmgmt prop on prop.escm_proposalmgmt_id=attr.escm_proposalmgmt_id "
          + " where bg.escm_bidmgmt_id    ='" + bidId + "'"
          + " and bg.document_type='P' and bg.bghdstatus='CO' and bgdetcount= actcount ";
      EscmBidMgmt bid = Utility.getObject(EscmBidMgmt.class, bidId);
      if (bid.getBidtype().equals("DR")) {
        strQuery += " and prop.proposalstatus<>'CL'";
      }
      ps = con.prepareStatement(strQuery);
      log.debug("ps" + ps.toString());
      rs = ps.executeQuery();
      if (rs.next()) {
        proposallistId = rs.getString("proposalid");
      }
      return proposallistId;
    } catch (OBException e) {
      log.error("Exception while getValidBG:" + e);
      return null;
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      log.error("Exception while getValidBG:" + e);
      return null;
    } finally {
      // close connection
      try {
        if (ps != null) {
          ps.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("An error has ocurred when trying to close the statement: " + e.getMessage(), e);
      }
      OBContext.restorePreviousMode();
    }

  }

  /**
   * delete invalid proposal , if bg for that proposal is Expried ,release ,Confiscated , then we
   * remove that proposal under evaluation event
   * 
   * @param proposalId
   * @param event
   * @param con
   */

  public static void deleteunproperBG(String proposalId, ESCMProposalEvlEvent event,
      Connection con) {
    String strQuery = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    List<EscmProposalAttribute> attrlist = new ArrayList<EscmProposalAttribute>();
    BigDecimal lineTotal = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      if (proposalId != null) {
        strQuery = " select escm_proposal_attr_id from escm_proposal_attr   where escm_proposalevl_event_id ='"
            + event.getId() + "' and  escm_proposalmgmt_id not in ( "
            + " select replace(unnest(string_to_array('" + proposalId
            + "',',')::character varying [] ) ,'''','') from dual)";
        ps = con.prepareStatement(strQuery);
        log.debug("ps" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {
          EscmProposalAttribute attr = OBDal.getInstance().get(EscmProposalAttribute.class,
              rs.getString("escm_proposal_attr_id"));
          EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
          // line updation
          for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
            lineTotal = line.getNetprice().multiply((line.getMovementQuantity()));
            if ((line.getNegotUnitPrice().compareTo(line.getNetprice()) != 0)) {
              line.setNegotUnitPrice(line.getNetprice());
              line.setLineTotal(lineTotal);
              OBDal.getInstance().save(line);
            }
          }

          proposal.setRank(null);
          proposal.setProposalstatus("OPE");
          OBDal.getInstance().save(proposal);
          removebgId(attr.getId());
          attr.setEscmProposalevlEvent(null);
          attr.setRank(null);
          OBDal.getInstance().save(attr);
        }
      } else {

        attrlist = event.getEscmProposalAttrList();
        if (attrlist.size() > 0) {
          for (EscmProposalAttribute attr : attrlist) {
            EscmProposalMgmt proposal = attr.getEscmProposalmgmt();

            // line updation
            for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
              lineTotal = line.getNetprice().multiply((line.getMovementQuantity()));
              if ((line.getNegotUnitPrice().compareTo(line.getNetprice()) != 0)) {
                line.setNegotUnitPrice(line.getNetprice());
                line.setLineTotal(lineTotal);
                OBDal.getInstance().save(line);
              }
            }

            proposal.setProposalstatus("OPE");
            proposal.setRank(null);
            OBDal.getInstance().save(proposal);
            removebgId(attr.getId());
            attr.setEscmProposalevlEvent(null);
            attr.setRank(null);
            OBDal.getInstance().save(attr);

          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while deleteunproperBG:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (SQLException e) {
      log.error("Exception while deleteunproperBG:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (ps != null) {
          ps.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("An error has ocurred when trying to close the statement: ", e.getMessage(), e);
      }
      OBContext.restorePreviousMode();
    }

  }

  /**
   * remove the attribute id from the proposal when we are deleting that proposal under evaluation
   * event.
   * 
   * @param attrId
   */
  private static void removebgId(String attrId) {
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMBGWorkbench> bgworkbench = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
          " as e where e.escmProposalAttr.id=:attrID ");
      bgworkbench.setNamedParameter("attrID", bgworkbench);
      log.debug("listsize:" + bgworkbench.list().size());
      if (bgworkbench.list().size() > 0) {
        for (ESCMBGWorkbench bg : bgworkbench.list()) {
          for (Escmbankguaranteedetail bgdet : bg.getEscmBankguaranteeDetailList()) {
            bgdet.setEscmProposalAttr(null);
            OBDal.getInstance().save(bgdet);
            OBDal.getInstance().flush();
          }
          bg.setEscmProposalAttr(null);
          OBDal.getInstance().save(bg);
          OBDal.getInstance().flush();
        }
      }
    } catch (OBException e) {
      log.error("Exception while removebgId:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * insert valid proposal , if bg for that proposal is other than Expried ,release ,Confiscated ,
   * then we include that proposal under evaluation event
   * 
   * @param proposalId
   * @param event
   * @param con
   */
  public static void insertPropoerBG(String proposalId, ESCMProposalEvlEvent event,
      Connection con) {
    String strQuery = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      strQuery = " select escm_proposalmgmt_id from escm_proposalmgmt where escm_proposalmgmt_id  in "
          + " (select replace(unnest(string_to_array('" + proposalId
          + "',',')::character varying [] ) ,'''','') from dual) and  escm_proposalmgmt_id not in (select escm_proposalmgmt_id from escm_proposal_attr "
          + "  where escm_proposalevl_event_id ='" + event.getId() + "' and  escm_proposalmgmt_id "
          + " in ( select replace(unnest(string_to_array('" + proposalId
          + "',',')::character varying [] ) ,'''','') from dual))";

      ps = con.prepareStatement(strQuery);
      log.debug("ps" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
            rs.getString("escm_proposalmgmt_id"));
        if (proposal != null && !proposal.getEscmBidmgmt().getBidtype().equals("DR")) {
          insertPropoerBG(proposal, event, con);
        }
      }
    } catch (OBException e) {
      log.error("Exception while insertPropoerBG:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (SQLException e) {
      log.error("Exception while insertPropoerBG:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (ps != null) {
          ps.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("An error has ocurred when trying to close the statement: ", e.getMessage(), e);
      }
      OBContext.restorePreviousMode();
    }

  }

  /**
   * integrate the proposal attribute and evaluation event by updating the event Id in Proposal
   * attribute.
   * 
   * @param pro
   * @param proEvlEvent
   * @param con
   */
  public static void insertPropoerBG(EscmProposalMgmt pro, ESCMProposalEvlEvent proEvlEvent,
      Connection con) {
    String strQuery = null;
    Query query = null;
    BigDecimal PRToal = BigDecimal.ZERO;
    BigDecimal grossPrice = BigDecimal.ZERO;
    BigDecimal netPrice = BigDecimal.ZERO;
    List<ESCMBGWorkbench> bglist = new ArrayList<ESCMBGWorkbench>();
    try {
      OBContext.setAdminMode();

      pro.setProposalstatus("ANY");
      log.debug("pro" + pro);

      OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance()
          .createQuery(EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalId "
              + " and e.escmOpenenvcommitee.id is not null  ");
      proposalatt.setNamedParameter("proposalId", pro.getId());
      if (proposalatt.list().size() > 0) {
        for (EscmProposalAttribute att : proposalatt.list()) {
          grossPrice = BigDecimal.ZERO;
          netPrice = BigDecimal.ZERO;
          // update the proposal attribute net price and proposal event header id
          // calculate Gross and net price for proposal attr header
          for (EscmProposalmgmtLine line : att.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {

            // set PEE tax amount based on proposal management line
            line.setPEELineTaxamt(line.getTaxAmount());

            if (!line.isSummary()) {
              grossPrice = grossPrice
                  .add((line.getMovementQuantity()).multiply(line.getGrossUnitPrice()));
              netPrice = netPrice.add(line.getLineTotal());
            }
            // start -calculate the estimate price based on bid - requisition line
            strQuery = " select case when coalesce(sum(reqln.qty),0) > 0 then "
                + " round((coalesce(sum(reqln.linenetamt),0)/coalesce(sum(reqln.qty),0))*coalesce(sum(line.movementqty),0),2) else 0 end   "
                + " from escm_proposalmgmt   pro  join  escm_proposalmgmt_line proline on proline.escm_proposalmgmt_id =pro.escm_proposalmgmt_id  "
                + " join  escm_bidmgmt_line line on line.escm_bidmgmt_id =pro.escm_bidmgmt_id "
                + " join  escm_bidsourceref srcref on srcref.escm_bidmgmt_line_id= line.escm_bidmgmt_line_id "
                + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id "
                + " where proline.escm_proposalmgmt_line_id = ? and reqln.em_escm_issummary  ='N' and line.issummarylevel  ='N'";

            query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
            query.setParameter(0, line.getId());
            log.debug("strQuery:" + query);
            if (query != null && query.list().size() > 0) {
              PRToal = PRToal.add((BigDecimal) query.list().get(0));
            }
            // end -calculate the estimate price
            if (!line.isSummary()) {
              line.setPEEInitUnitprice(line.getNegotUnitPrice().add(line.getUnittax()).setScale(2,
                  RoundingMode.HALF_UP));
            }
          }
          att.setProsalGrossprice(grossPrice);
          att.setProsalNetprice(netPrice);
          att.setProsalDiscountamt(grossPrice.subtract(netPrice));
          if (att.getProsalDiscountamt().compareTo(BigDecimal.ZERO) > 0)
            att.setProsalDiscount((att.getProsalDiscountamt().multiply(new BigDecimal("100")))
                .divide(att.getProsalGrossprice(), 2, RoundingMode.HALF_UP));
          if (att.getNetPrice() != null)
            att.setNegotiatedPrice(att.getProsalNetprice());
          att.setEscmProposalevlEvent(proEvlEvent);
          if (att.getEscmProposalmgmt().getProjectduration() != null)
            att.setProjectduration(att.getEscmProposalmgmt().getProjectduration());

          if (PRToal.compareTo(BigDecimal.ZERO) > 0) {
            att.setEstimatedPrice(PRToal);
          }

          // calculate the variation between estimated price and proposal net price
          if ((PRToal.compareTo(BigDecimal.ZERO) > 0) && att.getNetPrice() != null) {
            att.setVariation((att.getEstimatedPrice().subtract(att.getNetPrice()))
                .divide(att.getEstimatedPrice(), RoundingMode.HALF_UP));
          }

          // Set PEE Tax fields
          if (att.getEscmProposalmgmt().isTaxLine()) {
            att.setPEEIstax(att.getEscmProposalmgmt().isTaxLine());
            att.setPEEEfinTaxMethod(att.getEscmProposalmgmt().getEfinTaxMethod());
            att.setPEETotalTaxamt(att.getEscmProposalmgmt().getTotalTaxAmount());
          }

          OBDal.getInstance().save(att);

          // update the bank guarantee details
          OBQuery<ESCMBGWorkbench> bgdetail = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
              " as e where e.escmProposalmgmt.id=:proposalId");
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

    } catch (OBException e) {
      log.error("Exception while insertPropoerBGs:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}