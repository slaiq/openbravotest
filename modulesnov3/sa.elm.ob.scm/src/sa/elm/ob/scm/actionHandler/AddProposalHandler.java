package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalRegulation;
import sa.elm.ob.scm.EscmProposalmgmtLine;

/**
 * This class is used to add proposals in proposal evaluation event
 * 
 * @author qualian
 *
 */
public class AddProposalHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(AddProposalHandler.class);
  PreparedStatement ps = null;
  ResultSet rs = null;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();

      // declaring JSONObject & variables
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject purreqline = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = purreqline.getJSONArray("_selection");
      BigDecimal approvedBudget = BigDecimal.ZERO;
      long lineno = 10;
      final String evaleventID = jsonRequest.getString("Escm_Proposalevl_Event_ID");
      BigDecimal grossPrice = new BigDecimal("0");
      BigDecimal netPrice = new BigDecimal("0");
      String strQuery = null;
      Query query = null;
      BigDecimal PRToal = BigDecimal.ZERO;
      StringBuilder proposalNo = new StringBuilder();

      // get the connection
      Connection conn = OBDal.getInstance().getConnection();
      List<EscmProposalAttribute> evleventlist = new ArrayList<EscmProposalAttribute>();
      // getting Proposal Evaluation Event object using proposal evaluation id
      ESCMProposalEvlEvent evlevent = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
          evaleventID);
      // Check if proposal amount is Zero and throw error
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          if (selectedRow.getString("totalamount").equals("0")) {

            proposalNo.append(selectedRow.getString("proposalno"));
            proposalNo.append(",");
          }
        }
        if (proposalNo != null && proposalNo.length() != 0) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text", OBMessageUtils.messageBD("ESCM_Proposal_AmtZero").replace("%",
              proposalNo.substring(0, proposalNo.length() - 1)));
          json.put("message", successMessage);
          return json;
        }
      }

      List<ESCMBGWorkbench> bglist = new ArrayList<ESCMBGWorkbench>();
      // insert the Proposal Attribute lines based on selection
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          grossPrice = BigDecimal.ZERO;
          netPrice = BigDecimal.ZERO;
          JSONObject selectedRow = selectedlines.getJSONObject(a);

          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
              selectedRow.getString("id"));

          // take max line no in Proposals
          ps = conn.prepareStatement(
              " select coalesce(max(line),0)+10   as lineno from escm_proposal_attr where escm_proposalevl_event_id=?");
          ps.setString(1, evlevent.getId());
          rs = ps.executeQuery();
          if (rs.next()) {
            lineno = rs.getLong("lineno");
          }
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
          att.setEscmProposalevlEvent(evlevent);
          // att,setTAX
          if (proposal.isTaxLine()) {
            att.setPEEIstax(proposal.isTaxLine());
            att.setPEEEfinTaxMethod(proposal.getEfinTaxMethod());
            att.setPEETotalTaxamt(proposal.getTotalTaxAmount() == null ? BigDecimal.ZERO
                : proposal.getTotalTaxAmount());
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
              line.setPEEInitUnitprice(line.getNegotUnitPrice().add(line.getUnittax()).setScale(2,
                  RoundingMode.HALF_UP));
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
          // update proposal attribute Id in Bank Guarentee
          // OBQuery<Escmbankguaranteedetail> bankdetail = OBDal.getInstance().createQuery(
          // Escmbankguaranteedetail.class,
          // " as e where e.escmProposalmgmt.id='" + proposal.getId() + "'");
          // if (bankdetail.list().size() > 0) {
          // for (Escmbankguaranteedetail bank : bankdetail.list()) {
          // bank.setEscmProposalAttr(att);
          // }
          // }

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

        }
        // after add the without bid proposal calculate the Approved Budget Average.
        evleventlist = evlevent.getEscmProposalAttrList();
        if (evleventlist.size() > 0) {
          for (EscmProposalAttribute attr : evleventlist) {
            EscmProposalMgmt prposal = attr.getEscmProposalmgmt();

            if (prposal.getApprovedBudgetSAR() != null)
              approvedBudget = approvedBudget.add(prposal.getApprovedBudgetSAR());
          }
          approvedBudget = approvedBudget.divide(
              new BigDecimal(evlevent.getEscmProposalAttrList().size()), 2, RoundingMode.HALF_UP);
          evlevent.setApprovedBudgetSAR(approvedBudget);
          evlevent.setDeletelines(true);
          OBDal.getInstance().save(evlevent);
        }
        // evlevent.setProposalCounts((long) evleventlist.size());
        // setting Success Message
        OBDal.getInstance().flush();
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
      // setting Error Message
      else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
    } /*
       * catch (Exception e) { log.error("Exception in AddProposalHandler :", e);
       * OBDal.getInstance().rollbackAndClose(); e.printStackTrace(); throw new OBException(e); }
       */

    catch (OBException e) {
      log.error("Exception in AddProposalHandler", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", e.getMessage());
        json.put("message", errorMessage);
        return json;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in AddProposalHandler ", e1);
        throw new OBException(e1);
      }
    } catch (Exception e) {
      log.error("Exception in AddProposalHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        json.put("message", errorMessage);
        return json;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in AddProposalHandler ", e1);
        throw new OBException(e1);

      }
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        log.error("Exception while closing the statement in AddProposalHandler ", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}
