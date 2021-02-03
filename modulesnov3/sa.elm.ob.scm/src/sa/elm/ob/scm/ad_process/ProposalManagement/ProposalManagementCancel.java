/*
 * @author Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.scm.ad_process.ProposalManagement;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * This class is used to cancel the proposal management.
 */

public class ProposalManagementCancel extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementCancel.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean Status = false;
    try {
      OBContext.setAdminMode();
      final String proposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID").toString();
      EscmProposalMgmt proposalmgmt = Utility.getObject(EscmProposalMgmt.class, proposalId);
      Status = ProposalManagementActionMethod.cancelProposal("CL", proposalmgmt);

      if (Status) {
        // inserting action history
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", vars.getClient());
        historyData.put("OrgId", proposalmgmt.getOrganization().getId());
        historyData.put("RoleId", vars.getRole());
        historyData.put("UserId", vars.getUser());
        historyData.put("HeaderId", proposalId);
        historyData.put("Comments", "");
        historyData.put("Status", "CA");
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
        historyData.put("HeaderColumn", ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
        historyData.put("ActionColumn",
            ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
        Utility.InsertApprovalHistory(historyData);

        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
        bundle.setResult(result);
        return;
      }

    } catch (Exception e) {
      log.error("Exeception in Proposal header Cancel:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);

    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
