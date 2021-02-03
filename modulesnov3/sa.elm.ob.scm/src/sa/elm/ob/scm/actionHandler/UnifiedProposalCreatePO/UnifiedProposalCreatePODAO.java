package sa.elm.ob.scm.actionHandler.UnifiedProposalCreatePO;

import java.math.BigDecimal;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.ad_process.ProposalManagement.CreatePOFromProposal;

public class UnifiedProposalCreatePODAO {

  private static Logger log = Logger.getLogger(UnifiedProposalCreatePODAO.class);

  public static OBError createPO(String proposalId, String clientId, String userId, String Lang,
      BigDecimal awardedAmt) {

    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

    try {
      OBContext.setAdminMode();

      OBError error = null;
      EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

      String processId = "D9E65F8444854EAF8B4F347924CBEBE3";
      ProcessBundle pb = new ProcessBundle(processId, vars);
      pb.setCloseConnection(false);
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("Escm_Proposalmgmt_ID", proposal.getId());
      pb.setParams(parameters);
      CreatePOFromProposal createPO = new CreatePOFromProposal();
      createPO.execute(pb);
      error = (OBError) pb.getResult();
      if (error.getType().equals("error")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", error.getMessage());
        return result;
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_CreatePOSuccess@");
        return result;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while createPO" + e.getMessage());
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
