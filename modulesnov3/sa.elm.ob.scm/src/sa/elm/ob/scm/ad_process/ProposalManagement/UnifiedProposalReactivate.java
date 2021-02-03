package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
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
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;

public class UnifiedProposalReactivate extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalReactivate.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      final String unifiedProposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID")
          .toString();
      EscmProposalMgmt unifiedProposal = OBDal.getInstance().get(EscmProposalMgmt.class,
          unifiedProposalId);

      final String clientId = (String) bundle.getContext().getClient();
      String userId = (String) bundle.getContext().getUser();
      String tabId = (String) bundle.getParams().get("tabId");
      String roleId = (String) bundle.getContext().getRole();
      String orgId = unifiedProposal.getOrganization().getId();

      String histStatus = "", comments = (String) bundle.getParams().get("comments").toString();
      boolean errorFlag = false, headerUpdate = false;
      String DocStatus = unifiedProposal.getProposalappstatus();

      if (DocStatus.equals("INC")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // OBError error = UnifiedProposalReactivateMethods
      // .proposalReactivateValidation(unifiedProposalId, clientId, orgId, userId, roleId, tabId);
      // if (error.getType().equals("error")) {
      // OBDal.getInstance().rollbackAndClose();
      // bundle.setResult(error);
      // return;
      // }

      Connection con = OBDal.getInstance().getConnection();
      PreparedStatement ps = null;
      ResultSet rs = null;
      String query = null;
      List<String> proposalList = new ArrayList<String>();

      try {

        query = " select escm_proposalmgmt_id from escm_proposalmgmt_line "
            + " where escm_proposalmgmt_line_id in (select escm_unifiedproposalines_v_id  "
            + " from escm_unifiedproposalines_v where escm_proposalmgmt_id = ?) "
            + " group by escm_proposalmgmt_id ";

        ps = con.prepareStatement(query);
        ps.setString(1, unifiedProposalId);

        rs = ps.executeQuery();

        // Get proposal id list
        while (rs.next()) {
          proposalList.add(rs.getString("escm_proposalmgmt_id"));
        }

        // Reactivate validation
        // Check PO is created for awarded proposals
        boolean hasError = false;
        String proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
          OBError error1 = UnifiedProposalReactivateMethods.proposalReactivateValidation(proposalId,
              clientId, proposal.getOrganization().getId(), userId, roleId, tabId);
          if (error1.getType().equals("error")) {
            OBDal.getInstance().rollbackAndClose();
            hasError = true;
            proposalMessage = proposalMessage + proposal.getProposalno() + ", ";
          }
        }
        if (hasError) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@Escm_Proposalhasorder@");
          bundle.setResult(result);
          return;
        }

        // Update line info if any error occurs
        hasError = false;
        proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
          OBError error1 = UnifiedProposalRejectMethods.encumbranceValidationReject(proposal, vars);
          if (error1.getType().equals("error")) {
            // OBDal.getInstance().rollbackAndClose();
            hasError = true;
            proposalMessage = proposalMessage + proposal.getProposalno() + ", ";
          }
        }
        if (hasError) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
              "@Efin_Encum_Used_Cannot_Rej@");
          bundle.setResult(result);
          return;
        }

        // Skip new version (todo)
        // Check bid encumbrance for unified proposal
        if (unifiedProposal.getEscmBidmgmt() != null
            && unifiedProposal.getEscmBidmgmt().getEncumbrance() != null) {
          OBError error1 = UnifiedProposalRejectMethods
              .getUnifiedProposaltoBidDetailsRej(unifiedProposal, vars, true, proposalList);
          if (error1.getType().equals("error")) {
            bundle.setResult(error1);
            return;
          }
        }

        // If no error occurs, do encumbrance changes
        for (String proposalId : proposalList) {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
          OBError error1 = UnifiedProposalRejectMethods.updateProposalEncumbranceReject(proposal,
              vars);
          if (error1.getType().equals("error")) {
            OBDal.getInstance().rollbackAndClose();
            bundle.setResult(error1);
            return;
          }
        }
        // update bid encumbrance for unified proposal
        if (unifiedProposal.getEscmBidmgmt() != null
            && unifiedProposal.getEscmBidmgmt().getEncumbrance() != null) {

          boolean isFullyAwarded = UnifiedProposalActionMethod.isProposalFullyAwarded(proposalList);
          if (isFullyAwarded) {

            OBError error1 = UnifiedProposalRejectMethods.changeEncumStageRej(unifiedProposal,
                vars);
            if (error1.getType().equals("error")) {
              OBDal.getInstance().rollbackAndClose();
              bundle.setResult(error1);
              return;
            }

          } else {
            // reactivate split encumbrance
            for (String proposalId : proposalList) {
              EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
                  proposalId);
              UnifiedProposalRejectMethods.reactivateSplitBid(proposalMgmt, false, proposalList);
            }

            OBError error1 = UnifiedProposalRejectMethods
                .getUnifiedProposaltoBidDetailsRej(unifiedProposal, vars, false, proposalList);
            if (error1.getType().equals("error")) {
              OBDal.getInstance().rollbackAndClose();
              bundle.setResult(error1);
              return;
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        // log.error("Exception in isDirectApproval " + e.getMessage());
      } finally {
        // close db connection
        try {
          if (rs != null)
            rs.close();
          if (ps != null)
            ps.close();
        } catch (Exception e) {
        }
      }

      // update proposal Management header status based on reject
      if (!errorFlag) {

        headerUpdate = UnifiedProposalReactivateMethods
            .updateUnifiedProposalReactivate(unifiedProposal);

        if (headerUpdate) {
          OBDal.getInstance().save(unifiedProposal);

          // insert the Action history
          if (!StringUtils.isEmpty(unifiedProposalId)) {
            JSONObject historyData = new JSONObject();
            histStatus = "REA";// Reactivate
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", unifiedProposalId);
            historyData.put("Comments", comments);
            historyData.put("Status", histStatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
            historyData.put("HeaderColumn",
                ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
            historyData.put("ActionColumn",
                ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);

            UnifiedProposalActionMethod.InsertApprovalHistory(historyData);
          }

          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT);
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PROPOSAL_MANAGEMENT_DIRECT);

        }
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in UnifiedProposalReactivate:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
