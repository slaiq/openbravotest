package sa.elm.ob.scm.actionHandler.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalRegulation;
import sa.elm.ob.scm.EscmProposalmgmtLine;

public class CreatePEEActionHandlerDAOImpl implements CreatePEEActionHandlerDAO {

  private static final Logger log = Logger.getLogger(CreatePEEActionHandlerDAOImpl.class);

  @Override

  public JSONObject createPEE(String proposalId) {
    JSONObject peeResultJson = new JSONObject();
    String commiteeId = null;
    EscmProposalMgmt proposal = null;

    try {
      OBContext.setAdminMode();
      if (proposalId != null)
        proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

      OBQuery<EscmProposalAttribute> attrQuery = OBDal.getInstance().createQuery(
          EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalId ");
      attrQuery.setNamedParameter("proposalId", proposalId);
      if (attrQuery.list().size() > 0) {
        EscmProposalAttribute proposalAttr = attrQuery.list().get(0);
        peeResultJson.put("result", "1");
        peeResultJson.put("peeId", proposalAttr.getEscmProposalevlEvent().getId());
      } else {

        BidMgmtActionHandlerDAOImpl bidDao = new BidMgmtActionHandlerDAOImpl();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        ESCMProposalEvlEvent proposalEval = OBProvider.getInstance()
            .get(ESCMProposalEvlEvent.class);
        proposalEval.setOrganization(proposal.getOrganization());
        proposalEval.setDateHijri(new java.util.Date());
        proposalEval.setDateGregorian(dateFormat.format(new java.util.Date()));
        proposalEval.setTimeEvaluation(timeFormat.format(new java.util.Date()));
        proposalEval.setPreparerIDName(OBContext.getOBContext().getUser());
        commiteeId = bidDao.getCommiteeIdName(proposal.getProposalType(),
            proposal.getClient().getId());
        proposalEval.setEscmCommittee(OBDal.getInstance().get(ESCMCommittee.class, commiteeId));

        OBDal.getInstance().save(proposalEval);
        OBDal.getInstance().flush();

        // Add proposal in proposal attributes
        JSONObject resultAttrJson = insertProposalAttr(proposalEval, proposal);
        if (resultAttrJson.has("result")) {
          if (resultAttrJson.getString("result").equals("1")) {

            peeResultJson.put("result", "1");
            peeResultJson.put("peeId", proposalEval.getId());
            peeResultJson.put("attrId", resultAttrJson.getString("attrId"));

          } else if (resultAttrJson.getString("result").equals("0")) {
            peeResultJson.put("result", "0");
            peeResultJson.put("errorMsg", resultAttrJson.getString("errorMsg"));
          }
        }

      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while createPEE:" + e);
      try {
        peeResultJson.put("result", "0");
        peeResultJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return peeResultJson;

    } catch (JSONException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while createPEE:" + e);
      try {
        peeResultJson.put("result", "0");
        peeResultJson.put("errorMsg", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return peeResultJson;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while createPEE:" + e);
      try {
        peeResultJson.put("result", "0");
        peeResultJson.put("errorMsg", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return peeResultJson;

    } finally {
      OBContext.restorePreviousMode();
    }
    return peeResultJson;

  }

  public JSONObject insertProposalAttr(ESCMProposalEvlEvent evlEvent, EscmProposalMgmt proposal) {

    JSONObject attrResultJson = new JSONObject();
    try {
      OBContext.setAdminMode();
      // Declaring variables
      long lineno = 10;
      BigDecimal grossPrice = new BigDecimal("0");
      BigDecimal netPrice = new BigDecimal("0");
      String strQuery = null;
      Query query = null;
      BigDecimal PRToal = BigDecimal.ZERO;

      List<ESCMBGWorkbench> bglist = new ArrayList<ESCMBGWorkbench>();
      grossPrice = BigDecimal.ZERO;
      netPrice = BigDecimal.ZERO;

      EscmProposalAttribute att = OBProvider.getInstance().get(EscmProposalAttribute.class);
      att.setLineNo(lineno);
      att.setEscmProposalmgmt(proposal);
      Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
      att.setCurrency(
          proposal.getCurrency() == null
              ? (proposal.getOrganization().getCurrency() == null ? objCurrency
                  : proposal.getOrganization().getCurrency())
              : proposal.getCurrency());
      // att.setCurrency(proposal.getCurrency());
      att.setOrganization(proposal.getOrganization());
      att.setSupplier(proposal.getSupplier());
      att.setBranchName(proposal.getBranchName());
      att.setEscmProposalevlEvent(evlEvent);
      if (proposal.isTaxLine()) {
        att.setPEEIstax(proposal.isTaxLine());
        att.setPEEEfinTaxMethod(proposal.getEfinTaxMethod());
        att.setPEETotalTaxamt(
            proposal.getTotalTaxAmount() == null ? BigDecimal.ZERO : proposal.getTotalTaxAmount());
      } else {
        att.setPEETotalTaxamt(BigDecimal.ZERO);
      }
      // calculate Gross and net price for proposal attr header
      for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
        if (!line.isSummary()) {
          grossPrice = grossPrice
              .add(line.getMovementQuantity().multiply(line.getGrossUnitPrice()));
          netPrice = netPrice.add(line.getLineTotal());
          line.setPEENegotUnitPrice(line.getNegotUnitPrice());
          line.setPEEQty(line.getMovementQuantity());
          line.setPEELineTotal(line.getLineTotal());
          line.setPEELineTaxamt(
              line.getTaxAmount() == null ? BigDecimal.ZERO : line.getTaxAmount());
          line.setPEEInitUnitprice(
              line.getNegotUnitPrice().add(line.getUnittax()).setScale(2, RoundingMode.HALF_UP));
        } else {
          line.setPEEQty(line.getMovementQuantity());
        }

        strQuery = " select case when coalesce(sum(reqln.qty),0) > 0 then "
            + " round((coalesce(sum(reqln.linenetamt),0)/coalesce(sum(reqln.qty),0))*coalesce(sum(srcref.quantity),0),2) else 0 end as estimprc "
            + " from escm_proposalmgmt   pro "
            + " left join  escm_proposalmgmt_line line on line.escm_proposalmgmt_id =pro.escm_proposalmgmt_id "
            + " left join  escm_proposalsource_ref srcref on srcref.escm_proposalmgmt_line_id= line.escm_proposalmgmt_line_id "
            + " left join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id  "
            + " where line.escm_proposalmgmt_line_id =? and reqln.em_escm_issummary  ='N' ";

        query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
        query.setParameter(0, line.getId());

        if (query.list().size() > 0) {
          PRToal = PRToal.add((BigDecimal) query.list().get(0));
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
      if (att.getEscmProposalmgmt().getProjectduration() != null)
        att.setProjectduration(att.getEscmProposalmgmt().getProjectduration());

      if (PRToal.compareTo(BigDecimal.ZERO) > 0) {
        att.setEstimatedPrice(PRToal);
      }
      OBDal.getInstance().save(att);
      // update proposal attribute Id in Proposal Regulation
      OBQuery<EscmProposalRegulation> proregl = OBDal.getInstance().createQuery(
          EscmProposalRegulation.class, " as e where e.escmProposalmgmt.id=:proposalId ");
      proregl.setNamedParameter("proposalId", proposal.getId());
      if (proregl.list().size() > 0) {
        for (EscmProposalRegulation req : proregl.list()) {
          req.setEscmProposalAttr(att);
        }
      }

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

      // update proposal status as analysis once proposal link with proposal evaluation event
      proposal.setProposalstatus("ANY");
      OBDal.getInstance().save(proposal);

      OBDal.getInstance().flush();

      evlEvent.setApprovedBudgetSAR(proposal.getApprovedBudgetSAR());
      evlEvent.setDeletelines(true);
      OBDal.getInstance().save(evlEvent);

      attrResultJson.put("result", "1");
      attrResultJson.put("attrId", att.getId());

      OBDal.getInstance().flush();

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while insertProposalAttr:" + e);
      try {
        attrResultJson.put("result", "0");
        attrResultJson.put("errorMsg", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return attrResultJson;

    } catch (JSONException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while insertProposalAttr:" + e);
      try {
        attrResultJson.put("result", "0");
        attrResultJson.put("errorMsg", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return attrResultJson;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while insertProposalAttr:" + e);
      try {
        attrResultJson.put("result", "0");
        attrResultJson.put("errorMsg", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return attrResultJson;

    } finally {
      OBContext.restorePreviousMode();
    }
    return attrResultJson;
  }

}
