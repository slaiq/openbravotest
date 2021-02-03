package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementActionMethod;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian
 * 
 */

public class ProposalManagementRevertAward implements Process {
  /**
   * This class is used to revert the Awarded action in Proposal Management
   */
  private static final Logger log = Logger.getLogger(ProposalManagementRevertAward.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    String proposalId = null;
    String proposalattrId = null;
    EscmProposalMgmt proposal = null;
    if (bundle.getParams().get("Escm_Proposalmgmt_ID") != null) {
      proposalId = bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
    }

    if (bundle.getParams().get("Escm_Proposal_Attr_ID") != null) {
      proposalattrId = bundle.getParams().get("Escm_Proposal_Attr_ID").toString();
      EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
          proposalattrId);
      proposalId = proposalAttr.getEscmProposalmgmt().getId();
    }
    proposal = Utility.getObject(EscmProposalMgmt.class, proposalId);
    final String clientId = (String) bundle.getContext().getClient();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String proposalAttrId = (String) bundle.getParams().get("Escm_Proposal_Attr_ID");
    boolean status = false;
    BigDecimal soucrRef_Qty = BigDecimal.ZERO;
    BigDecimal total_award_qty = BigDecimal.ZERO;
    // HttpServletRequest request = RequestContext.get().getRequest();
    try {
      OBContext.setAdminMode();

      if (proposal.getProposalstatus().equals("AWD")
          && (proposal.getProposalappstatus().equals("INC")
              || proposal.getProposalappstatus().equals("REJ")
              || proposal.getProposalappstatus().equals("REA"))) {
        status = ProposalManagementActionMethod.revertAwardedProposal(proposalId, proposalAttrId,
            clientId, roleId, userId);
        // reduce already awarded quantity from requisition line
        EscmProposalMgmt obj_proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        for (EscmProposalmgmtLine obj_proposal_line : obj_proposal.getEscmProposalmgmtLineList()) {
          for (EscmProposalsourceRef obj_sourceRef_line : obj_proposal_line
              .getEscmProposalsourceRefList()) {
            if (obj_sourceRef_line.getRequisitionLine() != null) {
              RequisitionLine objRequisition = obj_sourceRef_line.getRequisitionLine();
              soucrRef_Qty = obj_sourceRef_line.getReservedQuantity();
              total_award_qty = objRequisition.getEscmAwardedQty().subtract(soucrRef_Qty);
              objRequisition.setEscmAwardedQty(total_award_qty);
              OBDal.getInstance().save(objRequisition);
            }
          }
        }
        if (status) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Already_Processed@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revert Proposal Management Revert Awarded :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}