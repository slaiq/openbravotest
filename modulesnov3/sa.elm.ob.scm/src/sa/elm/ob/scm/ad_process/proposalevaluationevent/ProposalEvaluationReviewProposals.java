package sa.elm.ob.scm.ad_process.proposalevaluationevent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.actionHandler.irtabs.PEEOnSaveHandler;

/**
 * 
 * @author qualian-Divya
 */
public class ProposalEvaluationReviewProposals implements Process {
  private static final Logger log = Logger.getLogger(ProposalEvaluationReviewProposals.class);
  private final OBError obError = new OBError();

  /**
   * This process will be delete the invalid BG Proposals and will add the proposals with valid BG.
   */
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Proposal Evaluation Review Proposals");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    final String propoevleventId = (String) bundle.getParams().get("Escm_Proposalevl_Event_ID")
        .toString();
    ESCMProposalEvlEvent prosalevalEvent = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
        propoevleventId);
    Connection connection = null;
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:", e);
      throw new RuntimeException(e);
    }

    // List<EscmProposalMgmt> proposallist = new ArrayList<EscmProposalMgmt>();
    // List<EscmProposalAttribute> attlist = new ArrayList<EscmProposalAttribute>();
    String proposalListId = null;
    String BGDetailList = null;
    boolean infoMsg = false;

    try {

      OBContext.setAdminMode(true);
      if (prosalevalEvent.getBidNo() != null
          && !prosalevalEvent.getBidNo().getBidtype().equals("DR")) {
        // get valid bg proposal
        proposalListId = ProposalEvaluationDAO.getValidBG(prosalevalEvent.getBidNo().getId(),
            connection);

        // delete invalid bg proposal
        ProposalEvaluationDAO.deleteunproperBG(proposalListId, prosalevalEvent, connection);
        if (proposalListId != null) {
          // insert valid bg proposal
          ProposalEvaluationDAO.insertPropoerBG(proposalListId, prosalevalEvent, connection);
          BGDetailList = PEEOnSaveHandler.integProsalAtttoProsalEvent(prosalevalEvent.getBidNo());
          if (BGDetailList != null)
            infoMsg = true;
        }
        OBDal.getInstance().flush();

        /*
         * // update the proposal count proposalcount =
         * prosalevalEvent.getEscmProposalAttrList().size();
         * prosalevalEvent.setProposalCounts((long) proposalcount);
         * OBDal.getInstance().save(prosalevalEvent);
         */
      }
      if (prosalevalEvent.getStatus().equals("DR") && prosalevalEvent.getBidNo() != null
          && prosalevalEvent.getBidNo().getBidtype().equals("DR")) {
        // Delete cancelled proposal and add valid proposal for the bid
        List<EscmProposalAttribute> attrToDeleteList = new ArrayList<EscmProposalAttribute>();
        // getting proposal attribute list from proposal evaluation event.
        if (prosalevalEvent.getEscmProposalAttrList().size() > 0) {
          prosalevalEvent.setDeletelines(false);
          OBDal.getInstance().save(prosalevalEvent);
          OBDal.getInstance().flush();

          for (EscmProposalAttribute attr : prosalevalEvent.getEscmProposalAttrList()) {
            EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
            attr.setRank(null);
            attr.setDiscardedReason(null);
            attr.setProposalstatus(null);
            attr.setPEETechDiscount(new BigDecimal("0"));
            attr.setPEETechDiscountamt(new BigDecimal("0"));

            // Set Tax field In attribute
            if (proposal.isTaxLine()) {
              attr.setPEEIstax(proposal.isTaxLine());
              attr.setPEEEfinTaxMethod(proposal.getEfinTaxMethod());
              attr.setPEETotalTaxamt(proposal.getTotalTaxAmount());
            } else {
              attr.setPEETotalTaxamt(BigDecimal.ZERO);
            }

            // line updation
            for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
              if ((line.getPEENegotUnitPrice().compareTo(line.getNetprice()) != 0)) {

                if (!line.getEscmProposalmgmt().getProposalType().equals("DR")
                    && line.getTechDiscountamt() != null) {
                  line.setPEENegotUnitPrice(line.getTechUnitPrice());
                } else {
                  line.setPEENegotUnitPrice(line.getGrossUnitPrice());
                }
                OBDal.getInstance().save(line);
              }

              line.setPEETechDiscount(new BigDecimal(0));
              line.setPEETechDiscountamt(new BigDecimal(0));
              line.setDiscount(line.getTechDiscount());
              line.setDiscountmount(line.getTechDiscountamt());
              line.setPeestatus(null);
              line.setPEELineTaxamt(line.getTaxAmount());
              if (line.getEscmProposalmgmt().getProposalType().equals("DR")) {
                line.setPEEQty(line.getMovementQuantity());
                line.setPEELineTotal(line.getMovementQuantity().multiply(line.getGrossUnitPrice()));
              } else {
                line.setPEELineTotal(line.getTechLineTotal());
                // after delete proposal in PEE revert qty also in proposal management line
                line.setPEEQty(line.getTechLineQty());
              }
              if (!line.isSummary()) {
                line.setPEEInitUnitprice(line.getNegotUnitPrice().add(line.getUnittax()).setScale(2,
                    RoundingMode.HALF_UP));
              }
            }

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

          }
          // remove the proposal attribute when proposal bid id direct or without bid
          prosalevalEvent.getEscmProposalAttrList().removeAll(attrToDeleteList);
          for (EscmProposalAttribute attr : attrToDeleteList) {
            OBDal.getInstance().remove(attr);
          }
          OBDal.getInstance().flush();
        }
        EscmBidMgmt bid = prosalevalEvent.getBidNo();
        prosalevalEvent.setBidNo(null);
        prosalevalEvent.setBidName(null);
        prosalevalEvent.setDeletelines(true);
        OBDal.getInstance().save(prosalevalEvent);
        OBDal.getInstance().flush();
        // Add Valid Proposal
        prosalevalEvent.setBidNo(bid);
        prosalevalEvent.setDeletelines(false);
        OBDal.getInstance().save(prosalevalEvent);
        OBDal.getInstance().flush();
        if (prosalevalEvent.getEscmProposalAttrList().size() == 0) {
          prosalevalEvent.setDeletelines(false);
        }
      }
      if (!infoMsg) {
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("ESCM_PEE_ReviewSucess"));
        bundle.setResult(obError);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      } else {
        obError.setType("Info");
        obError.setTitle("Info");
        obError.setMessage(BGDetailList);
        bundle.setResult(obError);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      }

    } catch (Exception e) {
      // bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
