package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmbidmgmtline;

/**
 * @author Kiruthika on 12/08/2020
 */

// Revert awarded quantity in Proposal
public class UnifiedProposalRevertAward implements Process {

  private static Logger log = Logger.getLogger(UnifiedProposalRevertAward.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();

      String proposalId = null;
      String proposalattrId = null;
      String proposalEvlEventId = null;
      List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();

      if (bundle.getParams().get("Escm_Proposalmgmt_ID") != null) {
        proposalId = bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
      }

      if (bundle.getParams().get("Escm_Proposal_Attr_ID") != null) {
        proposalattrId = bundle.getParams().get("Escm_Proposal_Attr_ID").toString();
        EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
            proposalattrId);
        proposalId = proposalAttr.getEscmProposalmgmt().getId();
      }

      if (bundle.getParams().get("Escm_Proposalevl_Event_ID") != null) {
        proposalEvlEventId = bundle.getParams().get("Escm_Proposalevl_Event_ID").toString();
      }

      if (proposalId != null) {
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        proposalList.add(proposal);
      }

      // Get proposal ids from bid
      if (proposalEvlEventId != null) {
        ESCMProposalEvlEvent event = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
            proposalEvlEventId);
        proposalList = event.getBidNo().getEscmProposalManagementList();
      }

      if (proposalList.size() > 0) {

        OBError message = UnifiedProposalActionMethod.revertAwardedQtyProposal(proposalList);
        if (message.getType().equals("error")) {
          OBDal.getInstance().rollbackAndClose();
          bundle.setResult(message);
          return;
        } else {

          // update bid qty
          EscmBidMgmt bid = proposalList.get(0).getEscmBidmgmt();
          for (Escmbidmgmtline bidLine : bid.getEscmBidmgmtLineList()) {
            if (bidLine != null) {
              bidLine.setAwardedqty(bidLine.getEscmProposalmgmtLineList().stream()
                  .map(a -> a.getAwardedqty()).reduce(BigDecimal.ZERO, BigDecimal::add));
              OBDal.getInstance().save(bidLine);
            }
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {
      log.error("Exeception in CreatePOFromProposal Process:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
