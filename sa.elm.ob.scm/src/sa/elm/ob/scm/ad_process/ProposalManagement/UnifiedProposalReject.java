package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class UnifiedProposalReject extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalReject.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    User objUser = Utility.getObject(User.class, vars.getUser());
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

      User user = OBDal.getInstance().get(User.class, userId);
      EutForwardReqMoreInfo forwardObj = unifiedProposal.getEUTForwardReqmoreinfo();
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      String alertWindow = AlertWindow.UnifiedProposal, alertRuleId = "",
          Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.unifiedProposal.reject",
              Lang) + " " + user.getName(),
          histStatus = "", comments = (String) bundle.getParams().get("comments").toString();
      boolean errorFlag = false, headerUpdate = false;

      boolean allowUpdate = false;
      boolean allowDelegation = false;

      int count = 0;
      Date CurrentDate = new Date();

      ArrayList<String> includeRecipient = null;
      Role objCreatedRole = null;
      String documentType = unifiedProposal.getProposalType().equals("DR") ? "EUT_122" : "EUT_117";

      if (unifiedProposal.getEUTNextRole() != null) {
        java.util.List<EutNextRoleLine> li = unifiedProposal.getEUTNextRole()
            .getEutNextRoleLineList();
        for (int i = 0; i < li.size(); i++) {
          String role = li.get(i).getRole().getId();
          if (roleId.equals(role)) {
            allowUpdate = true;
          }
        }
      }
      // check current role is a delegated role or not
      if (unifiedProposal.getEUTNextRole() != null) {
        DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
        allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId, documentType);
      }

      if (!allowUpdate && !allowDelegation) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      OBError error = UnifiedProposalRejectMethods.proposalRejectValidation(unifiedProposalId,
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

        // Reject validation
        // for (String proposalId : proposalList) {
        // if (!proposalId.equals(unifiedProposalId)) {
        // EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        // OBError error1 = UnifiedProposalRejectMethods.proposalRejectValidation(proposalId,
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
        log.error("Exception in UnifiedProposalReject " + e.getMessage());
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
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
            .getNextRoleLineList(unifiedProposal.getEUTNextRole(), documentType);

        headerUpdate = UnifiedProposalRejectMethods.updateUnifiedProposalReject(unifiedProposal);

        if (headerUpdate) {
          OBDal.getInstance().save(unifiedProposal);

          // insert the Action history
          if (!StringUtils.isEmpty(unifiedProposalId)) {
            JSONObject historyData = new JSONObject();
            histStatus = "REJ";
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

          // Removing forwardRMI id
          if (unifiedProposal.getEUTForwardReqmoreinfo() != null) {
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // set status as DR in forward Record
            forwardReqMoreInfoDAO
                .setForwardStatusAsDraft(unifiedProposal.getEUTForwardReqmoreinfo());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(unifiedProposal.getId(),
                Constants.PROPOSAL_MANAGEMENT);

          }
          if (unifiedProposal.getEUTReqmoreinfo() != null) {
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmiId, conn);
            // set status as DR in forward Record
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(unifiedProposal.getEUTReqmoreinfo());
            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(unifiedProposal.getId(),
                Constants.PROPOSAL_MANAGEMENT);

          }

          // -------------

          // alert Process

          String proposalEvlNo = "", proposalEvlId = "";
          OBQuery<EscmProposalAttribute> proposalAttrQry = OBDal.getInstance().createQuery(
              EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id = :proposalId");
          proposalAttrQry.setNamedParameter("proposalId", unifiedProposal.getId());
          if (proposalAttrQry != null) {
            List<EscmProposalAttribute> proposalAttrList = proposalAttrQry.list();
            if (proposalAttrList.size() > 0) {
              EscmProposalAttribute proposalAttr = proposalAttrList.get(0);
              proposalEvlNo = proposalAttr.getEscmProposalevlEvent().getEventNo();
              proposalEvlId = proposalAttr.getEscmProposalevlEvent().getId();
            }
          }

          // get alert rule id - Task No:7618
          alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

          if (count > 0 && !StringUtils.isEmpty(proposalEvlId)) {
            includeRecipient = new ArrayList<String>();

            // get creater role
            if (unifiedProposal.getCreatedBy().getADUserRolesList().size() > 0) {
              if (unifiedProposal.getRole() != null)
                objCreatedRole = unifiedProposal.getRole();
              else
                objCreatedRole = unifiedProposal.getCreatedBy().getADUserRolesList().get(0)
                    .getRole();
            }
            // get alert recipient - Task No:7618
            List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

            // check and insert alert recipient
            if (alrtRecList.size() > 0) {
              for (AlertRecipient objAlertReceipient : alrtRecList) {
                if (objAlertReceipient.getUserContact() != null)
                  includeRecipient.add(objAlertReceipient.getRole().getId());
                OBDal.getInstance().remove(objAlertReceipient);
              }
            }
            if (includeRecipient != null)
              includeRecipient.add(objCreatedRole.getId());

            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
            }

          }

          else {
            includeRecipient = new ArrayList<String>();
            objCreatedRole = unifiedProposal.getRole();

            // solve approval alerts - Task No:7618
            AlertUtility.solveAlerts(proposalEvlId);

            forwardReqMoreInfoDAO.getAlertForForwardedUser(proposalEvlId, alertWindow, alertRuleId,
                objUser, clientId, Constants.REJECT, proposalEvlNo, Lang, vars.getRole(),
                forwardObj, documentType, alertReceiversMap);

            // set alert for requester
            AlertUtility.alertInsertionRole(proposalEvlId, proposalEvlNo,
                unifiedProposal.getRole().getId(), unifiedProposal.getCreatedBy().getId(),
                unifiedProposal.getClient().getId(), Description, "NEW", alertWindow,
                "scm.pm.rejected", Constants.GENERIC_TEMPLATE);
            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.PROPOSAL_MANAGEMENT);
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.PROPOSAL_MANAGEMENT_DIRECT);
          }
        }
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in UnifiedProposalReject:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
