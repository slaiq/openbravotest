package sa.elm.ob.scm.ad_process.Requisition;

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
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
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
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 15/02/2017
 */
public class RequisitionRework implements Process {
  /**
   * This servlet class is responsible for Requisition Reject Process.
   * 
   */
  private static final Logger log = Logger.getLogger(RequisitionRework.class);
  private final OBError obError = new OBError();
  private static String documentType = "";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("rework the Requisition");
    @SuppressWarnings("unused")
    Connection connection = null;
    OBContext.setAdminMode(true);
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      // throw new RuntimeException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    }

    String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
    Requisition objRequisition = Utility.getObject(Requisition.class, strRequisitionId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objRequisition.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("notes").toString();
    String headerId = null;
    String appstatus = "", alertWindow = AlertWindow.PurchaseRequisition;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    String Lang = vars.getLanguage();
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    EutForwardReqMoreInfo forwardObj = null;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    // Task No.5925
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    log.debug("Requisition Id:" + strRequisitionId);
    OBContext.setAdminMode();
    if (objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Escm_AlreadyPreocessed_Approved@");
      bundle.setResult(result);
      return;
    }

    try {
      OBContext.setAdminMode();
      forwardObj = objRequisition.getEutForward();
      if (objRequisition.getEscmProcesstype() != null
          && objRequisition.getEscmProcesstype().equals("DP")) {
        documentType = Resource.PURCHASE_REQUISITION;
      } else {
        documentType = Resource.PURCHASE_REQUISITION_LIMITED;
      }

      Boolean allowReject = false;

      if (objRequisition.getEutForward() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(objRequisition.getEutForward(),
            userId, roleId, documentType);
      }
      if (objRequisition.getEutReqmoreinfo() != null
          || ((objRequisition.getEutForward() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // check pr encumbrance type is enable or not .. Task No.5925
      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id=:clientID and e.active='Y' ");
      encumcontrol.setNamedParameter("clientID", clientId);
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      enccontrollist = encumcontrol.list();

      // Task No.5925
      if (enccontrollist.size() > 0) {
        if (objRequisition.isEfinEncumbered()) {
          // get encum line list
          List<EfinBudgetManencumlines> encumLinesList = null;
          OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
              .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID");
          encumLines.setNamedParameter("encumID", objRequisition.getEfinBudgetManencum().getId());
          if (encumLines.list() != null && encumLines.list().size() > 0) {
            encumLinesList = encumLines.list();
          }
          // validation
          errorFlag = RequisitionDao.checkFundsForReject(objRequisition, encumLinesList);
          if (errorFlag) {
            // manual encum
            if (objRequisition.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              // update amount
              OBInterceptor.setPreventUpdateInfoChange(true);
              RequisitionDao.updateManualEncumAmountRej(objRequisition, encumLinesList, false, "");
              objRequisition.setEfinEncumbered(false);
              OBDal.getInstance().save(objRequisition);
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);
            }
            // auto encumbrance
            else {
              RequisitionDao.updateAmtInEnquiryRej(objRequisition.getId(), encumLinesList, false,
                  "");

              // Check Encumbrance Amount is Zero Or Negative
              if (objRequisition.getEfinBudgetManencum() != null)
                encumLinelist = objRequisition.getEfinBudgetManencum()
                    .getEfinBudgetManencumlinesList();
              if (encumLinelist.size() > 0)
                checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

              if (checkEncumbranceAmountZero) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
                bundle.setResult(result);
                return;
              }

              // remove encum
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  objRequisition.getEfinBudgetManencum().getId());
              encum.setDocumentStatus("DR");
              objRequisition.setEfinBudgetManencum(null);
              objRequisition.setEscmManualEncumNo("");
              objRequisition.setEfinEncumbered(false);
              OBDal.getInstance().save(objRequisition);
              // remove encum reference in lines.
              List<RequisitionLine> reqLine = objRequisition.getProcurementRequisitionLineList();
              for (RequisitionLine reqLineList : reqLine) {
                reqLineList.setEfinBudEncumlines(null);
                OBDal.getInstance().save(reqLineList);
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().remove(encum);
            }
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Encum_Used_Cannot_Rej@");
            bundle.setResult(result);
            return;
          }
        }
      }
      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (errorFlag) {
        try {

          // get old nextrole line user and role list
          HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
              .getNextRoleLineList(objRequisition.getEutNextRole(), documentType);

          if (objRequisition.getEscmMaterialRequest() == null) {
            if (objRequisition.getEutNextRole() != null) {
              java.util.List<EutNextRoleLine> li = objRequisition.getEutNextRole()
                  .getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (objRequisition.getEutNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId, documentType);

              /*
               * String sql = ""; sql =
               * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
               * + CurrentDate + "' and to_date >='" + CurrentDate +
               * "' and document_type='EUT_111'"; st = conn.prepareStatement(sql); rs =
               * st.executeQuery(); if (rs.next()) { String roleid = rs.getString("ad_role_id"); if
               * (roleid.equals(roleId)) { allowDelegation = true; } }
               */
            }
            if (allowUpdate || allowDelegation) {
              // Removing forwardRMI id
              if (objRequisition.getEutForward() != null) {

                // Removing the Role Access given to the forwarded user
                // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
                // update status as "DR"
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(objRequisition.getEutForward());

                // Removing Forward_Rmi id from transaction screens
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(objRequisition.getId(),
                    Constants.PURCHASE_REQUISITION);

              }
              if (objRequisition.getEutReqmoreinfo() != null) {
                // access remove
                // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
                // objRequisition.getEutReqmoreinfo().getId(), conn);

                // update status as "DR"
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(objRequisition.getEutReqmoreinfo());
                // Remove Forward_Rmi id from transaction screens
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(objRequisition.getId(),
                    Constants.PURCHASE_REQUISITION);

              }
              objRequisition.setUpdated(new java.util.Date());
              objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
              objRequisition.setEscmDocStatus("DR");
              objRequisition.setEscmDocaction("CO");
              objRequisition.setEutNextRole(null);
              OBDal.getInstance().save(objRequisition);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.PURCHASE_REQUISITION);
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.PURCHASE_REQUISITION_LIMITED);

              headerId = objRequisition.getId();
              if (!StringUtils.isEmpty(objRequisition.getId())) {
                appstatus = "REJ";

                JSONObject historyData = new JSONObject();

                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId);
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(objRequisition.getId())) {
                Role objCreatedRole = null;
                if (objRequisition.getEscmAdRole() != null)
                  objCreatedRole = objRequisition.getEscmAdRole();
                else if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {

                  objCreatedRole = objRequisition.getCreatedBy().getADUserRolesList().get(0)
                      .getRole();
                }
                // check and insert alert recipient - Task No:7618
                List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

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

                // delete alert for approval alerts - Task No:7618
                AlertUtility.deleteAlerts(objRequisition.getId());

                forwardReqMoreInfoDAO.getAlertForForwardedUser(objRequisition.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT,
                    objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(), Lang,
                    vars.getRole(), forwardObj, documentType, alertReceiversMap);
                // check current role exists in document rule ,if it is not there then delete Delete
                // it
                // why ??? current user only already approved
                // Bug #6969-Note 18793(Delegated recipient is getting deleted)
                /*
                 * String checkQuery =
                 * "as a join a.eutNextRole r join r.eutNextRoleLineList l where l.role.id = '" +
                 * vars.getRole() + "' and a.escmDocStatus ='ESCM_IP'";
                 * 
                 * OBQuery<Requisition> checkRecipientQry = OBDal.getInstance()
                 * .createQuery(Requisition.class, checkQuery); if (checkRecipientQry.list().size()
                 * == 0) { OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance()
                 * .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId +
                 * "' and e.role.id='" + vars.getRole() + "'"); if (currentRoleQuery.list().size() >
                 * 0) { for (AlertRecipient delObject : currentRoleQuery.list()) {
                 * OBDal.getInstance().remove(delObject); } } }
                 */

                // set alert for requester
                String Description = sa.elm.ob.scm.properties.Resource
                    .getProperty("scm.pr.rejected", vars.getLanguage()) + " " + objUser.getName();
                AlertUtility.alertInsertionRole(objRequisition.getId(),
                    objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                    objCreatedRole.getId(), objRequisition.getCreatedBy().getId(),
                    objRequisition.getClient().getId(), Description, "NEW", alertWindow,
                    "scm.pr.rejected", Constants.GENERIC_TEMPLATE);

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@ESCM_Requisition_Rework@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
              throw new OBException(errorMsg);
            }
          } else {

            // get alert rule id - Task:7618
            alertRuleId = AlertUtility.getAlertRule(clientId, "PRFMIR");

            // upddate the header status as rejected
            objRequisition.setUpdated(new java.util.Date());
            objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
            objRequisition.setEscmDocStatus("ESCM_REJ");
            objRequisition.setEscmDocaction("PD");
            objRequisition.setEutNextRole(null);
            OBDal.getInstance().save(objRequisition);
            OBDal.getInstance().flush();

            Role objCreatedRole = null;

            if (objRequisition.getEscmMaterialRequest() != null) {
              if (objRequisition.getEscmMaterialRequest().getRole() != null)
                objCreatedRole = objRequisition.getEscmMaterialRequest().getRole();
              else if (objRequisition.getEscmMaterialRequest().getCreatedBy().getADUserRolesList()
                  .size() > 0) {

                objCreatedRole = objRequisition.getEscmMaterialRequest().getCreatedBy()
                    .getADUserRolesList().get(0).getRole();
              }
            } else {
              if (objRequisition.getEscmAdRole() != null)
                objCreatedRole = objRequisition.getEscmAdRole();
              else if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {

                objCreatedRole = objRequisition.getCreatedBy().getADUserRolesList().get(0)
                    .getRole();
              }
            }

            if (includeRecipient != null)
              includeRecipient.add(objCreatedRole.getId());

            // delete alert for approval alerts - Task:7618
            AlertUtility.deleteAlerts(objRequisition.getId());

            forwardReqMoreInfoDAO.getAlertForForwardedUser(objRequisition.getId(), alertWindow,
                alertRuleId, objUser, clientId, Constants.REJECT,
                objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(), Lang,
                vars.getRole(), forwardObj, documentType, alertReceiversMap);
            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId,
                  AlertWindow.PurchaseReqForMIR);
            }
            String Description = sa.elm.ob.scm.properties.Resource
                .getProperty("scm.createPR.for.MIR.rej", Lang) + " "
                + OBContext.getOBContext().getUser().getName();

            AlertUtility.alertInsertionRole(objRequisition.getId(),
                objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                objCreatedRole.getId(), "", objRequisition.getClient().getId(), Description, "NEW",
                AlertWindow.PurchaseReqForMIR, "scm.createPR.for.MIR.rej",
                Constants.GENERIC_TEMPLATE);

            HashSet<String> incluedSet1 = new HashSet<String>(includeRecipient);
            Iterator<String> iterator1 = incluedSet1.iterator();
            while (iterator1.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator1.next(), null, clientId,
                  AlertWindow.PurchaseReqForMIR);
            }

            headerId = objRequisition.getId();
            if (!StringUtils.isEmpty(objRequisition.getId())) {
              appstatus = "REJ";

              JSONObject historyData = new JSONObject();

              historyData.put("ClientId", clientId);
              historyData.put("OrgId", orgId);
              historyData.put("RoleId", roleId);
              historyData.put("UserId", userId);
              historyData.put("HeaderId", headerId);
              historyData.put("Comments", comments);
              historyData.put("Status", appstatus);
              historyData.put("NextApprover", "");
              historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
              historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
              historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

              count = Utility.InsertApprovalHistory(historyData);
            }

            // Check Encumbrance Amount is Zero Or Negative
            if (objRequisition.getEfinBudgetManencum() != null)
              encumLinelist = objRequisition.getEfinBudgetManencum()
                  .getEfinBudgetManencumlinesList();
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (checkEncumbranceAmountZero) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
              bundle.setResult(result);
              return;
            }

            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@ESCM_Requisition_Rework@");
            bundle.setResult(result);
            return;

          }
        } catch (Exception e) {
          // Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          bundle.setResult(error);
          /*
           * obError.setType("Error"); obError.setTitle("Error"); obError.setMessage(errorMsg);
           * bundle.setResult(obError);
           */
          OBDal.getInstance().rollbackAndClose();

        }
      }

      else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }

      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception in Requisition rework:", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
