package sa.elm.ob.scm.actionHandler;

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
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

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

public class ReturnTransactionReject implements Process {
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(ReturnTransactionReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();
    final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();

    ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = inout.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    ShipmentInOut headerId = null;
    String appstatus = "", alertWindow = AlertWindow.ReturnTransaction;
    ArrayList<String> includeRecipient = new ArrayList<String>();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date currentDate = new Date();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = inout.getEutForward();
    try {
      OBContext.setAdminMode(true);

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      Boolean allowReject = false;

      if (errorFlag) {
        try {

          OBQuery<ShipmentInOutLine> lines = OBDal.getInstance()
              .createQuery(ShipmentInOutLine.class, "shipmentReceipt.id =:receiptID ");
          lines.setNamedParameter("receiptID", receiptId);
          count = lines.list().size();

          if (count > 0) {
            ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
            // -- forward/rmi
            if (header.getEutForward() != null) {
              allowReject = forwardReqMoreInfoDAO.allowApproveReject(header.getEutForward(), userId,
                  roleId, Resource.Return_Transaction);
            }
            if (header.getEutReqmoreinfo() != null
                || ((header.getEutForward() != null) && (!allowReject))) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Escm_AlreadyPreocessed_Approved@");
              bundle.setResult(result);
              return;
            }
            // -- forward/rmi
            if (header.getEutNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getEutNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (header.getEutNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                  DocumentTypeE.RETURN_TRANSACTION.getDocumentTypeCode());
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
                  .getNextRoleLineList(header.getEutNextRole(), Resource.Return_Transaction);

              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setEscmDocaction("CO");
              header.setEscmDocstatus("DR");
              header.setEutNextRole(null);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.Return_Transaction);

              // Removing forwardRMI id
              if (inout.getEutForward() != null) {
                // Removing the Role Access given to the forwarded user
                // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
                // set status as DR in forward Record
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(inout.getEutForward());
                // Removing Forward_Rmi id from transaction screens
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(inout.getId(),
                    Constants.RETURN_TRANSACTION);

              }
              if (inout.getEutReqmoreinfo() != null) {
                // access remove
                // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, rmiId, conn);
                // set status as DR in forward Record
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(inout.getEutReqmoreinfo());
                // Remove Forward_Rmi id from transaction screens
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inout.getId(),
                    Constants.RETURN_TRANSACTION);

              }

              // -------------

              headerId = header;
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
                historyData.put("HistoryTable", ApprovalTables.Return_Transaction_History);
                historyData.put("HeaderColumn", ApprovalTables.Return_Transaction_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.Return_Transaction_DOCACTION_COLUMN);
                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(header.getId())) {

                Role objCreatedRole = null;
                if (header.getCreatedBy().getADUserRolesList().size() > 0) {
                  if (header.getEscmAdRole() != null)
                    objCreatedRole = header.getEscmAdRole();
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

                String Description = sa.elm.ob.scm.properties.Resource
                    .getProperty("scm.Returntrans.rejected", Lang) + " " + objUser.getName();

                forwardReqMoreInfoDAO.getAlertForForwardedUser(inout.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT, inout.getDocumentNo(), Lang,
                    vars.getRole(), forwardObj, Resource.Return_Transaction, alertReceiversMap);

                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "",
                    header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                    alertWindow, "scm.Returntrans.rejected", Constants.GENERIC_TEMPLATE);
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_ReturnrejectSuccess@");
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
    }
  }
}
