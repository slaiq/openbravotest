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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtHist;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.util.ApprovalTables;

public class UnifiedProposalRevoke extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalRevoke.class);

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
      String comments = (String) bundle.getParams().get("comments").toString();
      boolean errorFlag = false, headerUpdate = false;
      String appstatus = "";
      int count = 0;
      Boolean isAlreadyRevoked = true;
      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();

      List<EscmProposalmgmtHist> history = proposalDAO.getProposalHist(unifiedProposalId);
      if (history.size() > 0) {
        EscmProposalmgmtHist apphistory = history.get(0);
        if (apphistory.getRequestreqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }

      // To check whether record is already revoked
      isAlreadyRevoked = ProposalManagementRejectMethods.isAlreadyRevoked(unifiedProposalId, userId,
          roleId);
      if (isAlreadyRevoked) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      OBError error = UnifiedProposalRevokeMethods.proposalRevokeValidation(unifiedProposalId,
          clientId, orgId, userId, roleId, tabId);
      if (error.getType().equals("error")) {
        OBDal.getInstance().rollbackAndClose();
        bundle.setResult(error);
        return;
      }

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

        // Revoke validation
        // for (String proposalId : proposalList) {
        // if (!proposalId.equals(unifiedProposalId)) {
        // EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        // OBError error1 = UnifiedProposalRevokeMethods.proposalRevokeValidation(proposalId,
        // clientId, proposal.getOrganization().getId(), userId, roleId, tabId);
        // if (error1.getType().equals("error")) {
        // OBDal.getInstance().rollbackAndClose();
        // bundle.setResult(error1);
        // return;
        // }
        // }
        // }

        boolean hasError = false;
        String proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%", "");
        // Update line info if any error occurs
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
            // split bid encumbrance
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
        OBDal.getInstance().rollbackAndClose();
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

      if (!errorFlag) {
        headerUpdate = UnifiedProposalRevokeMethods.updateUnifiedProposalRevoke(unifiedProposal);
        if (headerUpdate) {
          if (!StringUtils.isEmpty(unifiedProposal.getId())) {
            appstatus = "REV";
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", unifiedProposal.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", appstatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Proposal_Management_History);
            historyData.put("HeaderColumn",
                ApprovalTables.Proposal_Management_History_HEADER_COLUMN);
            historyData.put("ActionColumn",
                ApprovalTables.Proposal_Management_History_DOCACTION_COLUMN);
            count = UnifiedProposalActionMethod.InsertApprovalHistory(historyData);
          }
          if (count > 0 && !StringUtils.isEmpty(unifiedProposal.getId())) {

            String proposalEvlId = "";
            OBQuery<EscmProposalAttribute> proposalAttrQry = OBDal.getInstance().createQuery(
                EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id = :proposalId");
            proposalAttrQry.setNamedParameter("proposalId", unifiedProposal.getId());
            if (proposalAttrQry != null) {
              List<EscmProposalAttribute> proposalAttrList = proposalAttrQry.list();
              if (proposalAttrList.size() > 0) {
                EscmProposalAttribute proposalAttr = proposalAttrList.get(0);
                proposalEvlId = proposalAttr.getEscmProposalevlEvent().getId();
              }
            }

            AlertUtility.solveAlerts(proposalEvlId);
          }
        }
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in UnifiedProposalReject:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
