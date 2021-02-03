package sa.elm.ob.scm.ad_process.proposalevaluationevent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMCommRecommendation;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbankguaranteedetail;

/**
 * @author Divya on 07/06/2017
 */

public class ProposalEvaluationDeleteLines extends DalBaseProcess {

  /**
   * This servlet class was responsible for Proposal Evaluation Event Delete Line Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(ProposalEvaluationDeleteLines.class);

  // private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      // BigDecimal lineTotal = BigDecimal.ZERO;
      // declaring variables
      final String proposalEvaleventId = (String) bundle.getParams()
          .get("Escm_Proposalevl_Event_ID").toString();

      // getting Proposal event object by using Proposal Event Id
      ESCMProposalEvlEvent event = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
          proposalEvaleventId);

      List<EscmProposalAttribute> attrToDeleteList = new ArrayList<EscmProposalAttribute>();

      // getting proposal attribute list from proposal evaluation event.
      if (event.getEscmProposalAttrList().size() > 0) {
        event.setDeletelines(false);
        OBDal.getInstance().save(event);
        OBDal.getInstance().flush();

        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
          attr.setRank(null);
          attr.setDiscardedReason(null);
          attr.setProposalstatus(null);
          attr.setPEETechDiscount(BigDecimal.ZERO);
          attr.setPEETechDiscountamt(BigDecimal.ZERO);
          if (proposal.isTaxLine()) {
            attr.setPEEIstax(false);
            attr.setPEEEfinTaxMethod(null);
            attr.setPEETotalTaxamt(BigDecimal.ZERO);
          }

          // line updation
          for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
            // lineTotal = line.getNetprice().multiply((line.getMovementQuantity()));
            if ((line.getPEENegotUnitPrice().compareTo(line.getNetprice()) != 0)) {

              if (!line.getEscmProposalmgmt().getProposalType().equals("DR")
                  && line.getTechDiscountamt() != null) {
                line.setPEENegotUnitPrice(line.getTechUnitPrice());
              } else {
                line.setPEENegotUnitPrice(line.getGrossUnitPrice());
              }
              // line.setLineTotal(lineTotal);
              // line.setPEETechDiscount(new BigDecimal(0));
              // line.setPEETechDiscountamt(new BigDecimal(0));
              OBDal.getInstance().save(line);
              // }
            }

            line.setPEETechDiscount(BigDecimal.ZERO);
            line.setPEETechDiscountamt(BigDecimal.ZERO);
            line.setDiscount(line.getTechDiscount());
            line.setDiscountmount(line.getTechDiscountamt());
            line.setPEELineTaxamt(BigDecimal.ZERO);
            line.setPeestatus(null);
            line.setPEEInitUnitprice(BigDecimal.ZERO);
            line.setPEEUnitpricedis(BigDecimal.ZERO);
            line.setPEEUnittax(BigDecimal.ZERO);
            line.setPEENetUnitprice(BigDecimal.ZERO);
            if (line.getEscmProposalmgmt().getProposalType().equals("DR")) {
              // line.setLineTotal(line.getMovementQuantity().multiply(line.getGrossUnitPrice()));
              line.setPEEQty(BigDecimal.ZERO);
              line.setPEELineTotal(line.getMovementQuantity().multiply(line.getGrossUnitPrice()));
            } else {
              line.setPEELineTotal(line.getTechLineTotal());
              // after delete proposal in PEE revert qty also in proposal management line
              // line.setMovementQuantity(line.getTechLineQty());
              line.setPEEQty(line.getTechLineQty());
            }

          }

          // proposal and proposal attribute updation
          if (event.getBidNo() == null
              || (event.getBidNo() != null && event.getBidNo().getBidtype().equals("DR"))) {
            if (!proposal.getProposalstatus().equals("CL")) {
              proposal.setRank(null);
              proposal.setProposalstatus("SUB");
              OBDal.getInstance().save(proposal);
            }

            attrToDeleteList.add(attr);
            OBQuery<ESCMBGWorkbench> bgworkbench = OBDal.getInstance()
                .createQuery(ESCMBGWorkbench.class, " as e where e.escmProposalAttr.id=:attrId");
            bgworkbench.setNamedParameter("attrId", attr.getId());
            log.debug("listsize:" + bgworkbench.list().size());
            if (bgworkbench.list().size() > 0) {
              for (ESCMBGWorkbench bg : bgworkbench.list()) {
                for (Escmbankguaranteedetail bgdet : bg.getEscmBankguaranteeDetailList()) {
                  bgdet.setEscmProposalAttr(null);
                  OBDal.getInstance().save(bgdet);
                }
                bg.setEscmProposalAttr(null);
                OBDal.getInstance().save(bg);
              }
              OBDal.getInstance().flush();
            }
          } else {
            if (!proposal.getProposalstatus().equals("CL")) {
              proposal.setRank(null);
              proposal.setProposalstatus("TER");
              OBDal.getInstance().save(proposal);
            }

            attr.setEscmProposalevlEvent(null);
            OBDal.getInstance().save(attr);
          }
        }
        // remove the proposal attribute when proposal bid id direct or without bid
        event.getEscmProposalAttrList().removeAll(attrToDeleteList);
        for (EscmProposalAttribute attr : attrToDeleteList) {
          OBDal.getInstance().remove(attr);
        }
        OBDal.getInstance().flush();
      }

      if (event.getBidNo() != null) {
        OBQuery<ESCMCommRecommendation> comrecom = OBDal.getInstance()
            .createQuery(ESCMCommRecommendation.class, " as e where e.escmTechnicalevlEvent.id=( "
                + "select e.id from escm_technicalevl_event e where e.bidNo.id=:bidId)");
        comrecom.setNamedParameter("bidId", event.getBidNo().getId());
        if (comrecom.list().size() > 0) {
          for (ESCMCommRecommendation com : comrecom.list()) {
            com.setEscmProposalevlEvent(null);
            OBDal.getInstance().save(com);
          }
          OBDal.getInstance().flush();
        }
      }

      // remove the header bid value while delete the proposals in proposal evaluation event Task
      // no 5009; note no.14830 point no.2
      event.setBidNo(null);
      event.setBidName(null);
      event.setApprovedBudgetSAR(null);
      event.setEscmAnnoucements(null);
      event.setEscmOpenenvcommitee(null);
      event.setEscmCommittee(null);
      event.setEnvelopeDate(null);
      event.setEscmTechnicalevlEvent(null);
      event.setTECDateHijri(null);
      event.setProposalCounts(null);
      event.setDeletelines(false);
      event.setProposalCounts((long) 0);
      // spec no as null
      event.setSpecNo(null);
      OBDal.getInstance().save(event);
      OBDal.getInstance().flush();

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_ProEvlEvntLine_Del@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in ProposalEvaluation Delete lines Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}