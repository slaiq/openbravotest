package sa.elm.ob.scm.ad_process.SiteIssueRequest;

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
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 10/04/2017
 */
public class SiteMaterialIssueRequestReject implements Process {
  /**
   * This servlet class was responsible for Site Issue Request Reject Process with Approval
   * 
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(SiteMaterialIssueRequestReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Site Material issue request reject");
    OBContext.setAdminMode(true);
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
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
    String appstatus = "", alertWindow = AlertWindow.SiteIssueRequest;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false, allowReject = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    EutForwardReqMoreInfo forwardObj = Mrequest.getEUTForward();

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
          roleId, Resource.MATERIAL_ISSUE_REQUEST);
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
      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);

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
              String sql = "";
              sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
                  + CurrentDate + "' and to_date >='" + CurrentDate
                  + "' and document_type='EUT_112'";
              st = conn.prepareStatement(sql);
              rs = st.executeQuery();
              while (rs.next()) {
                String roleid = rs.getString("ad_role_id");
                if (roleid.equals(roleId)) {
                  allowDelegation = true;
                }
              }
            }
            if (allowUpdate || allowDelegation) {
              // get old nextrole line user and role list
              HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                  .getNextRoleLineList(header.getEUTNextRole(), Resource.MATERIAL_ISSUE_REQUEST);

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
              header.setEscmSmirAction("CO");
              // header.setSubmit(false);
              header.setEUTNextRole(null);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.MATERIAL_ISSUE_REQUEST);
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
                    Lang, vars.getRole(), forwardObj, Resource.MATERIAL_ISSUE_REQUEST,
                    alertReceiversMap);
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
                String Description = sa.elm.ob.scm.properties.Resource
                    .getProperty("scm.smir.rejected", Lang) + " " + objUser.getName();
                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "",
                    header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                    alertWindow, "scm.smir.rejected", Constants.GENERIC_TEMPLATE);

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_SMIRRejectSuccess@");
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
      log.error("Exception While SiteMaterialIssueRequestReject :", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
  }
}
