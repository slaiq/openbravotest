package sa.elm.ob.scm.actionHandler;

import java.sql.ResultSet;
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

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class MaterialIssueRequestReject implements Process {
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(MaterialIssueRequestReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Material issue request reject");
    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();
    /*
     * try { ConnectionProvider provider = bundle.getConnection(); connection =
     * provider.getConnection(); } catch (NoConnectionAvailableException e) {
     * log.error("No Database Connection Available.Exception:" + e); throw new RuntimeException(e);
     * }
     */
    final String Material_ID = (String) bundle.getParams().get("Escm_Material_Request_ID")
        .toString();
    MaterialIssueRequest Mrequest = OBDal.getInstance().get(MaterialIssueRequest.class,
        Material_ID);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = Mrequest.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    MaterialIssueRequest headerId = null;
    String appstatus = "", alertWindow = AlertWindow.IssueRequest;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    EutForwardReqMoreInfo forwardObj = Mrequest.getEUTForward();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    ResultSet rs = null;
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false, allowReject = false;
    String errorMsg = "", alertRuleId = "";
    Date currentDate = new Date();
    int count = 0;
    String documentType = Mrequest.getEscmDocumenttype();

    log.debug("Material_ID:" + Material_ID);

    if (Mrequest.getAlertStatus().equals("ESCM_AP")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Escm_AlreadyPreocessed_Approved@");
      bundle.setResult(result);
      return;
    }
    // If the record is Forwarded or given RMI then throw error when any other user tries to
    // reject the record without refreshing the page
    if (Mrequest.getEUTForward() != null) {
      allowReject = forwardReqMoreInfoDAO.allowApproveReject(Mrequest.getEUTForward(), userId,
          roleId, documentType);
    }
    if (Mrequest.getEUTReqmoreinfo() != null
        || ((Mrequest.getEUTForward() != null) && (!allowReject))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Escm_AlreadyPreocessed_Approved@");
      bundle.setResult(result);
      return;
    }
    try {
      OBContext.setAdminMode(true);

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (errorFlag) {
        try {
          OBQuery<MaterialIssueRequestLine> lines = OBDal.getInstance()
              .createQuery(MaterialIssueRequestLine.class, "escmMaterialRequest.id =:mirID");
          lines.setNamedParameter("mirID", Material_ID);
          count = lines.list().size();

          if (count > 0) {
            MaterialIssueRequest header = OBDal.getInstance().get(MaterialIssueRequest.class,
                Material_ID);
            if (header.getEUTNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getEUTNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (header.getEUTNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(currentDate, roleId, documentType);
              /*
               * String sql = ""; sql =
               * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
               * + CurrentDate + "' and to_date >='" + CurrentDate +
               * "' and document_type='EUT_112' and dll.ad_role_id='" + roleId + "'"; st =
               * conn.prepareStatement(sql); rs = st.executeQuery(); if (rs.next()) {
               * allowDelegation = true; }
               */
            }
            if (allowUpdate || allowDelegation) {
              // get old nextrole line user and role list
              HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                  .getNextRoleLineList(header.getEUTNextRole(), documentType);

              // Removing Forward and RMI Id
              if (header.getEUTForward() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForward());
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                    Constants.MATERIAL_ISSUE_REQUEST);
              }
              if (header.getEUTReqmoreinfo() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                    Constants.MATERIAL_ISSUE_REQUEST);
              }

              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setAlertStatus("DR");
              header.setEscmAction("CO");
              // header.setSubmit(false);
              header.setEUTNextRole(null);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Mrequest.getEscmDocumenttype());
              headerId = header;
              log.debug("headerId:" + headerId.getId());
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
                historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);
                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(header.getId())) {
                Role objCreatedRole = null;
                if (header.getCreatedBy().getADUserRolesList().size() > 0) {
                  if (header.getRole() != null)
                    objCreatedRole = header.getRole();
                  else
                    objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
                }
                // get alert recipients - Task No:7618
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

                // solve approval alerts - Task No:7618
                AlertUtility.solveAlerts(header.getId());

                forwardReqMoreInfoDAO.getAlertForForwardedUser(Mrequest.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT, Mrequest.getDocumentNo(),
                    Lang, vars.getRole(), forwardObj, documentType, alertReceiversMap);
                // check current role exists in document rule ,if it is not there then delete Delete
                // it
                // why ??? current user only already approved
                /*
                 * String checkQuery =
                 * "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '" +
                 * vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";
                 * 
                 * OBQuery<Requisition> checkRecipientQry = OBDal.getInstance().createQuery(
                 * Requisition.class, checkQuery); if (checkRecipientQry.list().size() == 0) {
                 * OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance().createQuery(
                 * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId +
                 * "' and e.role.id='" + vars.getRole() + "'"); if (currentRoleQuery.list().size() >
                 * 0) { for (AlertRecipient delObject : currentRoleQuery.list()) {
                 * OBDal.getInstance().remove(delObject); } } }
                 */
                // set alert for requester
                String Description = sa.elm.ob.scm.properties.Resource.getProperty(
                    "scm.materialissuerequest.rejected", Lang) + " " + objUser.getName();
                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "",
                    header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                    alertWindow, "scm.materialissuerequest.rejected", Constants.GENERIC_TEMPLATE);
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_materialrejectSuccess@");
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
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
      // close connection
      try {
        if (rs != null)
          rs.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
  }
}
