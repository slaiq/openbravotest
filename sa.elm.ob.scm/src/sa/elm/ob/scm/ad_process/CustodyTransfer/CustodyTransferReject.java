package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ad_reports.CustodyCardReport.CustodyCardReportVO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 10/05/2017
 */
public class CustodyTransferReject implements Process {

  private static final Logger log = Logger.getLogger(CustodyTransferReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus();
      // Connection con = OBDal.getInstance().getConnection();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String appstatus = "";
      // String DocAction = inout.getEscmCtdocaction();
      String alertWindow = AlertWindow.CustodyTransfer, alertRuleId = "";
      String Lang = vars.getLanguage();
      boolean errorFlag = false;
      User usr = OBDal.getInstance().get(User.class, userId);
      int count = 0;
      Boolean isDirectApproval = false, isDelegated = false;
      Boolean allowReject = false;
      User objUser = OBDal.getInstance().get(User.class, vars.getUser());
      Date currentDate = new Date();
      JSONObject forwardJsonObj = new JSONObject();
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      EutForwardReqMoreInfo forwardObj = inout.getEutForward();

      if (DocStatus.equals("DR")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approve the record without refreshing the page
      if (inout.getEutForward() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(inout.getEutForward(), userId,
            roleId, Resource.CUSTODY_TRANSFER);
      }
      if (inout.getEutReqmoreinfo() != null
          || ((inout.getEutForward() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      forwardJsonObj = forwardReqMoreInfoDAO.getForwardFromUserFromRole(inout.getEutNextRole(),
          userId, roleId, clientId);
      // check whether the current user is present among the signatures or a delegated user
      isDirectApproval = CustodyTransferDAO.isCtDirectApproval(inout, userId);
      isDelegated = CustodyTransferDAO.isCtDelegated(inout, userId, roleId, currentDate,
          DocumentTypeE.CUSTODY_TRANSFER.getDocumentTypeCode());
      if (!isDirectApproval && !isDelegated
          && (forwardJsonObj == null || (forwardJsonObj != null && forwardJsonObj.length() == 0))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // update header status
      if (!errorFlag) {
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
            .getNextRoleLineList(inout.getEutNextRole(), Resource.CUSTODY_TRANSFER);

        if (inout.getEutForward() != null) {

          // Removing the Role Access given to the forwarded user
          // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
          // update status as "DR"
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(inout.getEutForward());

          // Removing Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(inout.getId(),
              Constants.Custody_Transfer);

        }
        if (inout.getEutReqmoreinfo() != null) {
          // access remove
          // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
          // objRequisition.getEutReqmoreinfo().getId(), conn);

          // update status as "DR"
          forwardReqMoreInfoDAO.setForwardStatusAsDraft(inout.getEutReqmoreinfo());
          // Remove Forward_Rmi id from transaction screens
          forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inout.getId(),
              Constants.Custody_Transfer);

        }
        inout.setUpdated(new java.util.Date());
        inout.setUpdatedBy(OBContext.getOBContext().getUser());
        inout.setEscmCtdocaction("CO");
        inout.setEscmDocstatus("DR");
        inout.setEutNextRole(null);
        inout.setEscmCtapplevel((long) 1);
        log.debug("header:" + inout.toString());
        OBDal.getInstance().save(inout);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.CUSTODY_TRANSFER);
        // Insert into Approval History
        if (!StringUtils.isEmpty(inout.getId())) {
          appstatus = "REJ";

          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", inout.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);

        }
        // alert Process
        if (count > 0 && !StringUtils.isEmpty(inout.getId())) {
          ArrayList<CustodyCardReportVO> includereceipient = new ArrayList<CustodyCardReportVO>();
          CustodyCardReportVO vo = null;

          // get alert rule id - Task No:7618
          alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

          // get creater role
          Role objCreatedRole = null;
          if (inout.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = inout.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          // get alert recipients - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

          // check and insert alert recipient
          if (alrtRecList.size() > 0) {
            for (AlertRecipient objAlertReceipient : alrtRecList) {
              vo = new CustodyCardReportVO();
              vo.setRoleId(objAlertReceipient.getRole().getId());
              if (objAlertReceipient.getUserContact() != null)
                vo.setUserId(objAlertReceipient.getUserContact().getId());
              includereceipient.add(vo);
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          // added created user also in alert receipient
          if (objCreatedRole != null) {
            vo = new CustodyCardReportVO();
            vo.setRoleId(objCreatedRole.getId());
            vo.setUserId(inout.getCreatedBy().getId());
            includereceipient.add(vo);
          }
          // avoid duplicate recipient
          HashSet<CustodyCardReportVO> incluedSet = new HashSet<CustodyCardReportVO>(
              includereceipient);
          for (CustodyCardReportVO vo1 : incluedSet) {
            AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                alertWindow);
          }

          // solve approval alerts - Task No:7618
          AlertUtility.solveAlerts(inout.getId());

          forwardReqMoreInfoDAO.getAlertForForwardedUser(inout.getId(), alertWindow, alertRuleId,
              objUser, clientId, Constants.REJECT, inout.getDocumentNo(), Lang, vars.getRole(),
              forwardObj, Resource.CUSTODY_TRANSFER, alertReceiversMap);
          // set alert for requester
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.rejected",
              Lang) + " " + usr.getName();
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), "",
              inout.getCreatedBy().getId(), inout.getClient().getId(), Description, "NEW",
              alertWindow, "scm.ct.rejected", Constants.GENERIC_TEMPLATE);
        }
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;

      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exception in custody transfer Reject :" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);

    } finally {
      OBContext.restorePreviousMode();
    }

  }
}