package sa.elm.ob.scm.ad_process.TechnicalEvaluationEvent;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
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
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
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
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Divya on 05/01/2018
 *
 */
public class TechnicalEvaluationEventReject implements Process {
  /**
   * This Servlet Class responsible to reject records
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(TechnicalEvaluationEventReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub
    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    final String techEvaleventId = (String) bundle.getParams().get("Escm_Technicalevl_Event_ID")
        .toString();

    // getting Technical event object by using Technical Evaluation Event Id
    EscmTechnicalevlEvent event = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
        techEvaleventId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = event.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EscmTechnicalevlEvent headerId = null;
    String appstatus = "", alertWindow = AlertWindow.TechnicalEvalEvent;
    ArrayList<String> includeRecipient = new ArrayList<String>();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false, allowReject = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = event.getEUTForward();
    try {
      OBContext.setAdminMode(true);

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (event.getEUTForward() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(event.getEUTForward(), userId,
            roleId, Resource.TECHNICAL_EVALUATION_EVENT);
      }
      if (event.getEUTReqmoreinfo() != null
          || ((event.getEUTForward() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      if (errorFlag) {
        try {
          OBQuery<EscmProposalAttribute> lines = OBDal.getInstance()
              .createQuery(EscmProposalAttribute.class, "escmTechnicalevlEvent.id =:techeventID");
          lines.setNamedParameter("techeventID", techEvaleventId);
          count = lines.list().size();

          if (count > 0) {
            if (event.getEUTNextRole() != null) {
              java.util.List<EutNextRoleLine> li = event.getEUTNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (event.getEUTNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId,
                  DocumentTypeE.TEE.getDocumentTypeCode());
              /*
               * String sql = ""; sql =
               * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
               * + CurrentDate + "' and to_date >='" + CurrentDate +
               * "' and document_type='EUT_123'"; st = conn.prepareStatement(sql); rs =
               * st.executeQuery(); if (rs.next()) { String roleid = rs.getString("ad_role_id"); if
               * (roleid.equals(roleId)) { allowDelegation = true; } }
               */
            }
            if (allowUpdate || allowDelegation) {
              // get old nextrole line user and role list
              HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                  .getNextRoleLineList(event.getEUTNextRole(), Resource.TECHNICAL_EVALUATION_EVENT);

              event.setUpdated(new java.util.Date());
              event.setUpdatedBy(OBContext.getOBContext().getUser());
              event.setAction("CO");
              event.setStatus("ESCM_REJ");
              event.setEUTNextRole(null);
              OBDal.getInstance().save(event);
              // OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.TECHNICAL_EVALUATION_EVENT);
              headerId = event;
              if (!StringUtils.isEmpty(headerId.getId())) {
                appstatus = "REJ";

                JSONObject historyData = new JSONObject();

                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId.getId());
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.TECHNICAL_EVL_EVENT_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.TECHNICAL_EVL_EVENT_HEADER_COLUMN);
                historyData.put("ActionColumn",
                    ApprovalTables.TECHNICAL_EVL_EVENT_DOCACTION_COLUMN);
                count = Utility.InsertApprovalHistory(historyData);
              }

              // delete the unused nextroles in eut_next_role table.
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.TECHNICAL_EVALUATION_EVENT);

              // Removing forwardRMI id
              if (event.getEUTForward() != null) {
                // Removing the Role Access given to the forwarded user
                // Update statuses draft the forward Record
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(event.getEUTForward());
                // Removing Forward_Rmi id from transaction screens
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(event.getId(),
                    Constants.TECHNICAL_EVALUATION_EVENT);

              }
              if (event.getEUTReqmoreinfo() != null) {
                // Remove Forward_Rmi id from transaction screens
                // Update statuses draft the RMI Record
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(event.getEUTReqmoreinfo());
                // access remove
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(event.getId(),
                    Constants.TECHNICAL_EVALUATION_EVENT);

              }

              if (!OBContext.getOBContext().isInAdministratorMode())
                OBContext.setAdminMode(true);
              if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
                String Description = sa.elm.ob.scm.properties.Resource.getProperty(
                    "scm.techevaluation.event.rejected", Lang) + " " + objUser.getName();

                Role objCreatedRole = null;
                if (event.getCreatedBy().getADUserRolesList().size() > 0) {
                  if (event.getRole() != null)
                    objCreatedRole = event.getRole();
                  else
                    objCreatedRole = event.getCreatedBy().getADUserRolesList().get(0).getRole();
                }
                // get alert recipient - Task No:7618
                List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

                // check and insert alert recipient
                if (alrtRecList.size() > 0) {
                  for (AlertRecipient objAlertReceipient : alrtRecList) {
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

                // solving approval alerts - Task No:7618
                AlertUtility.solveAlerts(event.getId());

                forwardReqMoreInfoDAO.getAlertForForwardedUser(event.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT, event.getEventNo(), Lang,
                    vars.getRole(), forwardObj, Resource.TECHNICAL_EVALUATION_EVENT,
                    alertReceiversMap);
                // delete the unused nextroles in eut_next_role table.
                DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                    Resource.TECHNICAL_EVALUATION_EVENT);
                AlertUtility.alertInsertionRole(event.getId(), event.getEventNo(), "",
                    event.getCreatedBy().getId(), event.getClient().getId(), Description, "NEW",
                    alertWindow, "scm.techevaluation.event.rejected", Constants.GENERIC_TEMPLATE);
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_TEE_Rejected@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
              throw new OBException(errorMsg);
            }
          }
        } catch (Exception e) {
          log.error("exception :", e);
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);
          OBDal.getInstance().rollbackAndClose();
        }
      } else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } /*
       * catch (Exception e) { bundle.setResult(obError); log.error("exception :", e);
       * OBDal.getInstance().rollbackAndClose(); }
       */
    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in TechnicalEvaluationEventReject:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }
}
