package sa.elm.ob.scm.actionHandler;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidmgmthistory;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is responsible for Bid Management Revoke Process
 * 
 * @author qualian
 * 
 */

public class BidManagementRevoke implements Process {
  private static final Logger log = Logger.getLogger(BidManagementRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    final String bidId = (String) bundle.getParams().get("Escm_Bidmgmt_ID").toString();
    EscmBidMgmt bid = Utility.getObject(EscmBidMgmt.class, bidId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = bid.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EfinBudgetManencum encumbrance = null;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EscmBidMgmt headerId = null;
    JSONObject resultEncum = null;
    String appstatus = "";// , alertWindow = AlertWindow.BidManagement;
    // String alertRuleId = "";
    // ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    // String Lang = vars.getLanguage();
    // String Description = "", lastWaitingRoleId = "";
    try {
      OBContext.setAdminMode();
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
      enccontrollist = BidManagementDAO.getPREncumTypeList(clientId);

      /*
       * OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
       * "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow + "'");
       * if (queryAlertRule.list().size() > 0) { AlertRule objRule = queryAlertRule.list().get(0);
       * alertRuleId = objRule.getId(); }
       */

      Escmbidmgmthistory apphistory = BidManagementDAO.getBidHistory(bidId);
      if (apphistory.getRequestreqaction().equals("REV")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      EscmBidMgmt headerCheck = Utility.getObject(EscmBidMgmt.class, bidId);

      if (headerCheck.getBidstatus().equals("CD") || headerCheck.getBidstatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      int count = 0;
      boolean errorFlag = true;
      if (errorFlag) {
        EscmBidMgmt header = Utility.getObject(EscmBidMgmt.class, bidId);
        // Task No.5925
        if (enccontrollist.size() > 0 && header.getEncumbrance() != null) {
          encumbrance = header.getEncumbrance();

          // reject the merge and splitencumbrance
          resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(header);
          // if full qty only used then remove the encumbrance reference and change the
          // encumencumbrance stage as PR Stage
          if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
              && resultEncum.getBoolean("isAssociatePREncumbrance")
              && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
            errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(header, null, null);
            log.debug("errorFlag12:" + errorFlag);
            if (errorFlag) {
              OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProcessFailed(Reason)@");
              bundle.setResult(result1);
              return;
            } else {
              // Check Encumbrance Amount is Zero Or Negative
              if (header.getEncumbrance() != null)
                encumLinelist = header.getEncumbrance().getEfinBudgetManencumlinesList();
              if (encumLinelist.size() > 0)
                checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

              if (checkEncumbranceAmountZero) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
                bundle.setResult(result);
                return;
              }
              BidManagementDAO.reactivateStageUniqueCodeChg(resultEncum, bid, null);
              encumbrance.setEncumStage("PRE");
              header.setEfinIsbudgetcntlapp(false);
              header.setEncumbrance(null);
            }

          } else {

            errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(header, null, null);
            if (errorFlag) {
              OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_ProcessFailed(Reason)@");
              bundle.setResult(result1);
              return;
            } else {
              if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                BidManagementDAO.reactivateSplitPR(resultEncum, header, null);
              }
              if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                BidManagementDAO.reactivateSplitPR(resultEncum, header, null);
              }
            }
          }
        }
        // End Task No.5925

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setBidstatus("IA");
        header.setBidappstatus("DR");
        header.setEscmDocaction("CO");
        header.setEUTNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.Bid_Management);

        headerId = header;
        if (!StringUtils.isEmpty(headerId.getId())) {
          appstatus = "REV";
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", headerId.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.Bid_Management_History);
          historyData.put("HeaderColumn", ApprovalTables.Bid_Management_History_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.Bid_Management_History_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {

          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.Bid_Management);

          // Removing the forwardRMI id
          if (header.getEUTForwardReqmoreinfo() != null) {
            // forwardId = header.getEUTForwardReqmoreinfo().getId();
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);

            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());

            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                Constants.BID_MANAGEMENT);
            // Deleting the forward Record

          }
          if (header.getEUTReqmoreinfo() != null) {
            // rmiId = header.getEUTReqmoreinfo().getId();
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,header.getEUTReqmoreinfo().getId(),
            // conn);
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());

            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                Constants.BID_MANAGEMENT);

          }

          /*
           * Role objCreatedRole = null; if (header.getCreatedBy().getADUserRolesList().size() > 0)
           * { objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole(); }
           * alertWindow = AlertWindow.BidManagement;
           */
          // remove approval alert
          BidManagementDAO.getAlerts(bidId);

          // Check Encumbrance Amount is Zero Or Negative
          if (header.getEncumbrance() != null)
            encumLinelist = header.getEncumbrance().getEfinBudgetManencumlinesList();
          if (encumLinelist.size() > 0)
            checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

          if (checkEncumbranceAmountZero) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
            bundle.setResult(result);
            return;
          }

          // check and insert alert recipient
          /*
           * OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
           * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'"); if
           * (receipientQuery.list().size() > 0) { for (AlertRecipient objAlertReceipient :
           * receipientQuery.list()) { includeRecipient.add(objAlertReceipient.getRole().getId());
           * OBDal.getInstance().remove(objAlertReceipient); } } if (objCreatedRole != null)
           * includeRecipient.add(objCreatedRole.getId());
           */
          // avoid duplicate recipient
          /*
           * HashSet<String> incluedSet = new HashSet<String>(includeRecipient); Iterator<String>
           * iterator = incluedSet.iterator(); while (iterator.hasNext()) {
           * AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow); }
           * NextRoleByRuleVO nextApproval = NextRoleByRule.getMIRRevokeRequesterNextRole(OBDal
           * .getInstance().getConnection(), clientId, orgId, roleId, userId,
           * Resource.Bid_Management, header.getRole().getId()); EutNextRole nextRole = null;
           * nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
           * 
           * // set alert for next approver Description =
           * sa.elm.ob.scm.properties.Resource.getProperty("scm.BidMgmt.revoked", Lang) + " " +
           * header.getCreatedBy().getName();
           * 
           * // set revoke alert to last waiting role
           * AlertUtility.alertInsertionRole(header.getId(), header.getBidno(), lastWaitingRoleId,
           * "", header.getClient().getId(), Description, "NEW", alertWindow); for (EutNextRoleLine
           * objNextRoleLine : nextRole.getEutNextRoleLineList()) { AlertUtility
           * .alertInsertionRole(header.getId(), header.getBidno(), objNextRoleLine.getRole()
           * .getId(), "", header.getClient().getId(), Description, "NEW", alertWindow);
           * 
           * obError.setType("Success"); obError.setTitle("Success");
           * obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke")); }
           */
          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke"));
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();

        }
        bundle.setResult(obError);
      }
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke Bid Management Revoke :", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
