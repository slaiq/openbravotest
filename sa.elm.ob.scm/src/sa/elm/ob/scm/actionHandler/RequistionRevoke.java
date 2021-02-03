package sa.elm.ob.scm.actionHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.ESCMPurchaseReqAppHist;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Sathish Kumar on 10-11-2017
 * 
 */

public class RequistionRevoke implements Process {
  private static final Logger log = Logger.getLogger(RequistionRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    final String requistion_ID = (String) bundle.getParams().get("M_Requisition_ID");
    Requisition requistion = Utility.getObject(Requisition.class, requistion_ID);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = requistion.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    // EutNextRole nextRole = null;
    VariablesSecureApp vars = new VariablesSecureApp(request);

    Requisition headerId = null;
    String appstatus = "", alertWindow = AlertWindow.PurchaseRequisition;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    boolean enccontrolFlag = true;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    try {
      OBContext.setAdminMode();
      Requisition headerCheck = OBDal.getInstance().get(Requisition.class, requistion_ID);
      // check pr encumbrance type is enable or not .. Task No.5925
      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id=:clientID and e.active='Y' ");
      encumcontrol.setNamedParameter("clientID", clientId);
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      enccontrollist = encumcontrol.list();

      // Task No.5925
      if (enccontrollist.size() > 0) {
        if (headerCheck.isEfinEncumbered()) {
          // get encum line list
          List<EfinBudgetManencumlines> encumLinesList = null;
          OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
              .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID ");
          encumLines.setNamedParameter("encumID", headerCheck.getEfinBudgetManencum().getId());
          if (encumLines.list() != null && encumLines.list().size() > 0) {
            encumLinesList = encumLines.list();
          }
          // validation
          enccontrolFlag = RequisitionDao.checkFundsForReject(headerCheck, encumLinesList);
          if (enccontrolFlag) {
            // manual encum
            if (headerCheck.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              // update amount
              OBInterceptor.setPreventUpdateInfoChange(true);
              RequisitionDao.updateManualEncumAmountRej(headerCheck, encumLinesList, false, "");
              headerCheck.setEfinEncumbered(false);
              OBDal.getInstance().save(headerCheck);
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);
            }
            // auto encumbrance
            else {
              RequisitionDao.updateAmtInEnquiryRej(headerCheck.getId(), encumLinesList, false, "");
              // remove encum
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  headerCheck.getEfinBudgetManencum().getId());
              encum.setDocumentStatus("DR");
              headerCheck.setEfinBudgetManencum(null);
              headerCheck.setEscmManualEncumNo("");
              headerCheck.setEfinEncumbered(false);
              OBDal.getInstance().save(headerCheck);
              // remove encum reference in lines.
              List<RequisitionLine> reqLine = headerCheck.getProcurementRequisitionLineList();
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

      OBQuery<ESCMPurchaseReqAppHist> history = OBDal.getInstance().createQuery(
          ESCMPurchaseReqAppHist.class,
          " as e where e.requisition.id=:reqID order by e.creationDate desc ");
      history.setNamedParameter("reqID", requistion_ID);
      history.setMaxResult(1);
      if (history.list().size() > 0) {
        ESCMPurchaseReqAppHist apphistory = history.list().get(0);
        if (apphistory.getPurchasereqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }

      if (headerCheck.getEscmDocStatus().equals("ESCM_TR")
          || headerCheck.getEscmDocStatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      int count = 0;
      boolean errorFlag = true;
      if (errorFlag) {
        Requisition header = OBDal.getInstance().get(Requisition.class, requistion_ID);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setEscmDocStatus("DR");
        header.setEscmDocaction("CO");
        // nextRole = header.getEutNextRole();
        header.setEutNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION);
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION_LIMITED);
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
          historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }

          // solve approval alerts - Task No:7618
          AlertUtility.solveAlerts(requistion_ID);

          // check and insert alert recipient - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

          if (alrtRecList.size() > 0) {
            for (AlertRecipient objAlertReceipient : alrtRecList) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          if (objCreatedRole != null)
            includeRecipient.add(objCreatedRole.getId());
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
          }
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_REQUISITION);
          // delete the unused nextroles in eut_next_role table.
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.PURCHASE_REQUISITION_LIMITED);

          // Removing forwardRMI id
          if (requistion.getEutForward() != null) {
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(requistion.getEutForward());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(requistion.getId(),
                Constants.PURCHASE_REQUISITION);

          }
          if (requistion.getEutReqmoreinfo() != null) {
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
            // requistion.getEutReqmoreinfo().getId(), conn);
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(requistion.getEutReqmoreinfo());

            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(requistion.getId(),
                Constants.PURCHASE_REQUISITION);

          }

          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();

        }
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("ESCM_PurReq_RevokeSuccess"));
      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("ESCM_PurReq_RevokeNotSuccess"));
      }
      bundle.setResult(obError);

    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke Purchase requistion Revoke :", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
