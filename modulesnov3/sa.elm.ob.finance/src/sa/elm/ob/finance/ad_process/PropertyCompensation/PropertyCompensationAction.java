package sa.elm.ob.finance.ad_process.PropertyCompensation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
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
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinPropertyCompensation;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gopalakrishnan
 *
 */
public class PropertyCompensationAction extends DalBaseProcess {
  /**
   * This servlet class file is responsible to process the approval flow property compensation
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(PropertyCompensationAction.class);
  ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;

    try {
      OBContext.setAdminMode();
      // declaring variables
      final String str_property_compensation_id = (String) bundle.getParams()
          .get("Efin_Property_Compensation_ID").toString();
      EfinPropertyCompensation objPropertyComp = OBDal.getInstance()
          .get(EfinPropertyCompensation.class, str_property_compensation_id);

      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objPropertyComp.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String alertWindow = AlertWindow.PropertyCompensation;
      String documentType = Resource.PROPERTY_COMPENSATION;
      NextRoleByRuleVO nextApproval = null;
      String Lang = vars.getLanguage();

      Connection conn = OBDal.getInstance().getConnection();
      Boolean chkRoleIsInDocRul = false;
      boolean allowUpdate = false;
      boolean allowDelegation = false, allowApprove = false;
      String appstatus = "";
      Date currentDate = new Date();

      // submit process start

      // Approval flow start

      if (!objPropertyComp.getCreatedBy().getId().equals(vars.getUser())
          && objPropertyComp.getEfinDocaction().equals("CO")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Role_NotFundsReserve_submit@");
        bundle.setResult(result);
        return;
      }

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approves the record without refreshing the page
      if (objPropertyComp.getEUTForward() != null) {
        allowApprove = forwardReqMoreInfoDAO.allowApproveReject(objPropertyComp.getEUTForward(),
            userId, roleId, documentType);
      }
      if (objPropertyComp.getEUTReqmoreinfo() != null
          || ((objPropertyComp.getEUTForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      // Task #8198
      // check submitted role have the branch details or not
      if (objPropertyComp.getDocumentStatus().equals("DR")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // find the submitted role org/branch details

      if (objPropertyComp.getNextRole() != null) {
        if (objPropertyComp.getSubmittedrole() != null
            && objPropertyComp.getSubmittedrole().getEutReg() != null) {
          submittedRoleOrgId = objPropertyComp.getSubmittedrole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (objPropertyComp.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // check role is present in document rule or not
      if (objPropertyComp.getDocumentStatus().equals("DR")
          || objPropertyComp.getDocumentStatus().equals("EFIN_REJ")
          || objPropertyComp.getDocumentStatus().equals("EFIN_WFA")) {

        if (objPropertyComp.getDocumentStatus().equals("DR")
            || objPropertyComp.getDocumentStatus().equals("EFIN_REJ")) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, userId, roleId, documentType, BigDecimal.ZERO);
          if (!chkRoleIsInDocRul) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_RoleIsNotIncInDocRule@");
            bundle.setResult(result);
            return;
          }

        }

        // check current role associated with document rule for approval flow
        if (!objPropertyComp.getDocumentStatus().equals("DR")
            && !objPropertyComp.getDocumentStatus().equals("EFIN_REJ")) {
          if (objPropertyComp.getNextRole() != null) {
            java.util.List<EutNextRoleLine> li = objPropertyComp.getNextRole()
                .getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
          }
          if (objPropertyComp.getNextRole() != null) {
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                DocumentTypeE.PROPERTY_COMP.getDocumentTypeCode());

          }
          if (!allowUpdate && !allowDelegation) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_AlreadyPreocessed_Approve@");
            bundle.setResult(result);
            return;
          }
        }

        // check already approved or not
        if ((!vars.getUser().equals(objPropertyComp.getCreatedBy().getId()))
            && (objPropertyComp.getDocumentStatus().equals("DR"))) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }

        if (!errorFlag) {
          // set value for approval history status
          if ((objPropertyComp.getDocumentStatus().equals("DR")
              || objPropertyComp.getDocumentStatus().equals("EFIN_REJ"))
              && objPropertyComp.getEfinDocaction().equals("CO")) {
            appstatus = "SUB";
          } else if (objPropertyComp.getDocumentStatus().equals("EFIN_WFA")
              && objPropertyComp.getEfinDocaction().equals("AP")) {
            appstatus = "AP";
          }

          // update next role
          JSONObject upresult = updateHeaderStatus(conn, clientId, orgId, roleId, userId,
              objPropertyComp, appstatus, comments, currentDate, vars, nextApproval, Lang,
              documentType);
          if (upresult != null) {
            // if role does not associate with any user then dont allow to process for next approve
            if (upresult.has("count")
                && (upresult.getInt("count") == -2 || upresult.getInt("count") == 3)) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  upresult.getString("errormsg"));
              bundle.setResult(result);
              return;
            }
            // approve success message
            else if (upresult.has("count") && upresult.getInt("count") == 2) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_Property_Comp_App@");
              bundle.setResult(result);
              return;
            }
            // submit sucess message
            else if (upresult.has("count") && upresult.getInt("count") == 1) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@EFIN_Property_Comp_Success@");
              bundle.setResult(result);
              return;
            }
          }
        }
      }
      // reactive process start
      else if (objPropertyComp.getEfinDocaction().equals("RE")) {
        if (objPropertyComp.getInvoice() != null) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Prop_Comp_Reactivate_NotAllow@");
          bundle.setResult(result);
          return;
        }

        // chk already reactivated or not
        if (objPropertyComp.getEfinDocaction().equals("DR")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }

        if (!errorFlag) {
          // update Property Compensation if we reactivate
          objPropertyComp.setUpdated(new java.util.Date());
          objPropertyComp.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          objPropertyComp.setDocumentStatus("DR");
          objPropertyComp.setEfinDocaction("CO");
          OBDal.getInstance().save(objPropertyComp);

          // insert approval history
          if (!StringUtils.isEmpty(objPropertyComp.getId())) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", objPropertyComp.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Property_Comp_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.Property_Comp_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Property_Comp_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }
          // move the alert to solvesection
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "' order by e.creationDate desc");
          queryAlertRule.setMaxResult(1);
          if (queryAlertRule.list().size() > 0) {
            String alertRuleId = queryAlertRule.list().get(0).getId();
            sa.elm.ob.scm.util.AlertUtilityDAO.deleteAlertPreference(objPropertyComp.getId(),
                alertRuleId);
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }

      } // reactive process end

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exception in PropertyCompensationAction Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      if (OBContext.getOBContext().isInAdministratorMode())
        OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to update header status
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param objPropertyComp
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramNextApproval
   * @param Lang
   * @param documentType
   * @return
   */
  private JSONObject updateHeaderStatus(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinPropertyCompensation objPropertyComp, String appstatus,
      String comments, Date currentDate, VariablesSecureApp vars,
      NextRoleByRuleVO paramNextApproval, String Lang, String documentType) {
    String objPropertyCompId = null, pendingapproval = null;
    JSONObject result = new JSONObject();
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.PropertyCompensation;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objPropertyComp.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramNextApproval;
    try {
      OBContext.setAdminMode();
      int count = 0;
      String propertyCompId = objPropertyComp.getId();
      JSONObject tableData = new JSONObject();
      tableData.put("headerColumn", ApprovalTables.Property_Comp_HEADER_COLUMN);
      tableData.put("tableName", ApprovalTables.Property_Comp_Table);
      tableData.put("headerId", propertyCompId);
      tableData.put("roleId", roleId);
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;
      boolean isDummyRole = false;
      isDirectApproval = Utility.isDirectApproval(tableData);
      String errorMsgs = null;

      // setting json object value initially
      result.put("count", 0);
      result.put("errormsg", "null");

      // find the submitted role org/branch details
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;

      if (objPropertyComp.getNextRole() != null) {
        if (objPropertyComp.getSubmittedrole() != null
            && objPropertyComp.getSubmittedrole().getEutReg() != null) {
          submittedRoleOrgId = objPropertyComp.getSubmittedrole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (objPropertyComp.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "'  and e.efinProcesstype='" + alertWindow
              + "' ");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      /**
       * fetching from roleId and userId based on delegated user / forwarder/ direct approver
       **/
      if (objPropertyComp.getNextRole() != null) {
        fromUserandRoleJson = forwardReqMoreInfoDAO.getFromuserAndFromRoleWhileApprove(
            objPropertyComp.getNextRole(), userId, roleId, clientId, submittedRoleOrgId,
            Resource.PROPERTY_COMPENSATION, isDummyRole, isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }

      } else {
        fromUser = userId;
        fromRole = roleId;
      }

      // first time submit the record
      if (objPropertyComp.getNextRole() == null) {
        // getting next level approver
        nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
            submittedRoleOrgId, fromRole, fromUser, Resource.PROPERTY_COMPENSATION,
            BigDecimal.ZERO);
        objPropertyComp.setSubmittedrole(submittedRoleObj);
      }

      else {
        if (isDirectApproval) {
          // getting next level approver
          nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
              submittedRoleOrgId, fromRole, fromUser, Resource.PROPERTY_COMPENSATION,
              BigDecimal.ZERO);
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id=:roleId");
                userRole.setNamedParameter("roleId", objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    userRole.list().get(0).getUserContact().getId(), Resource.PROPERTY_COMPENSATION,
                    "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                // check backward delegation
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    delegatedFromRole, delegatedToRole, fromUser, Resource.PROPERTY_COMPENSATION,
                    BigDecimal.ZERO);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                submittedRoleOrgId, delegatedFromRole, fromUser, Resource.PROPERTY_COMPENSATION,
                BigDecimal.ZERO);
          }

        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromUser, Resource.PROPERTY_COMPENSATION,
              qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.PROPERTY_COMPENSATION, BigDecimal.ZERO);
        }
      }
      // if Role doesnt has any user associated then this condition will execute and return error
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -2;
        result.put("count", count);
        result.put("errormsg", errorMsgs);

      } // if no error and having next level approver then update the status as inprogress
      else if (nextApproval != null && nextApproval.hasApproval()) {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
            .getNextRoleLineList(objPropertyComp.getNextRole(), Resource.PROPERTY_COMPENSATION);

        // update the property Compensation status
        objPropertyComp.setUpdated(new java.util.Date());
        objPropertyComp.setUpdatedBy(OBContext.getOBContext().getUser());
        objPropertyComp.setEfinDocaction("AP");
        objPropertyComp.setDocumentStatus("EFIN_WFA");
        objPropertyComp.setNextRole(nextRole);

        List<AlertRecipient> alrtRecList = sa.elm.ob.scm.util.AlertUtility
            .getAlertReceipient(alertRuleId);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {

          // solving approval alerts - Task No:7618
          AlertUtility.solveAlerts(objPropertyComp.getId());

          forwardReqMoreInfoDAO.getAlertForForwardedUser(objPropertyComp.getId(), alertWindow,
              alertRuleId, objUser, clientId, Constants.APPROVE, objPropertyComp.getDocumentNo(),
              Lang, vars.getRole(), objPropertyComp.getEUTForward(), Resource.PROPERTY_COMPENSATION,
              alertReceiversMap);
          // define waiting for approval description
          String Description = sa.elm.ob.finance.properties.Resource.getProperty("finance.pc.wfa",
              Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objPropertyComp.getId(),
                objPropertyComp.getDocumentNo(), objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                objPropertyComp.getClient().getId(), Description, "NEW", alertWindow,
                "finance.pc.wfa", Constants.GENERIC_TEMPLATE);

            List<EutDocappDelegateln> delegationlnList = UtilityDAO
                .getDelegation(objNextRoleLine.getRole().getId(), currentDate, "EUT_123");

            if (delegationlnList != null && delegationlnList.size() > 0) {
              for (EutDocappDelegateln obDocAppDelegation : delegationlnList) {
                AlertUtility.alertInsertionRole(objPropertyComp.getId(),
                    objPropertyComp.getDocumentNo(), obDocAppDelegation.getRole().getId(),
                    obDocAppDelegation.getUserContact().getId(),
                    objPropertyComp.getClient().getId(), Description, "NEW", alertWindow,
                    "finance.pc.wfa", Constants.GENERIC_TEMPLATE);

                includeRecipient.add(obDocAppDelegation.getRole().getId());
              }
              if (nextRole.getEutNextRoleLineList().size() == 1 && delegationlnList.size() == 1
                  && Utility.getAssignedUserForRoles(
                      nextRole.getEutNextRoleLineList().get(0).getRole().getId()).size() == 1) {
                if (pendingapproval != null)
                  pendingapproval += objNextRoleLine.getRole().getName() + " ("
                      + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + "/"
                      + delegationlnList.get(0).getRole().getName() + " - "
                      + delegationlnList.get(0).getUserContact().getName();
                else
                  pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                      objNextRoleLine.getRole().getName() + " ("
                          + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                          + delegationlnList.get(0).getRole().getName() + "-"
                          + delegationlnList.get(0).getUserContact().getName());
              }

            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 1;
        result.put("count", count);
        result.put("errormsg", "null");
      } else {
        OBContext.setAdminMode(true);

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
            .getNextRoleLineList(objPropertyComp.getNextRole(), Resource.PROPERTY_COMPENSATION);

        ArrayList<String> includeRecipient = new ArrayList<String>();
        // nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        objPropertyComp.setUpdated(new java.util.Date());
        objPropertyComp.setUpdatedBy(OBContext.getOBContext().getUser());
        objPropertyComp.setDocumentStatus("EFIN_AP");
        Role objCreatedRole = null;
        User objCreatedUser = OBDal.getInstance().get(User.class,
            objPropertyComp.getCreatedBy().getId());
        if (objCreatedUser.getADUserRolesList().size() > 0) {
          objCreatedRole = objCreatedUser.getADUserRolesList().get(0).getRole();
        }

        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        forwardReqMoreInfoDAO.getAlertForForwardedUser(objPropertyComp.getId(), alertWindow,
            alertRuleId, objUser, clientId, Constants.APPROVE, objPropertyComp.getDocumentNo(),
            Lang, vars.getRole(), objPropertyComp.getEUTForward(), Resource.PROPERTY_COMPENSATION,
            alertReceiversMap);

        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.pc.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objPropertyComp.getId(), objPropertyComp.getDocumentNo(),
            objPropertyComp.getRole().getId(), objPropertyComp.getCreatedBy().getId(),
            objPropertyComp.getClient().getId(), Description, "NEW", alertWindow,
            "finance.ba.approved", Constants.GENERIC_TEMPLATE);
        objPropertyComp.setNextRole(null);
        objPropertyComp.setEfinDocaction("RE");
        count = 2;
        result.put("count", count);
        result.put("errormsg", "null");
        // Final Approval
        // count=2
      }

      OBDal.getInstance().save(objPropertyComp);
      objPropertyCompId = objPropertyComp.getId();
      if (!StringUtils.isEmpty(objPropertyCompId)) {

        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", objPropertyCompId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.Property_Comp_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.Property_Comp_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Property_Comp_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);
      }

      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), documentType);

      // after approved by forwarded user removing the forward and rmi id
      if (objPropertyComp.getEUTForward() != null) {
        forwardReqMoreInfoDAO.setForwardStatusAsDraft(objPropertyComp.getEUTForward());
        objPropertyComp.setEUTForward(null);
      }
      if (objPropertyComp.getEUTReqmoreinfo() != null) {
        forwardReqMoreInfoDAO.setForwardStatusAsDraft(objPropertyComp.getEUTReqmoreinfo());
        objPropertyComp.setEUTReqmoreinfo(null);
        // objPropertyComp.setEfinReqMoreInfo("N");
      }
      OBDal.getInstance().save(objPropertyComp);
    } catch (

    Exception e) {
      e.printStackTrace();
      log.error("Exception in updateHeaderStatus in Property Compensation : ", e);
      OBDal.getInstance().rollbackAndClose();
      try {

        result.put("count", "3");
        result.put("errormsg", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {

      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

}
