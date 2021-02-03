package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.util.ArrayList;
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
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ESCMCustodytransferHist;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author gopalakrishnan on 09/06/2017
 * 
 */

public class CustodyTransferRevoke implements Process {
  private static final Logger log = Logger.getLogger(CustodyTransferRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
    ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = inout.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();

    ShipmentInOut headerId = null;
    String appstatus = "", alertWindow = AlertWindow.CustodyTransfer;
    String alertRuleId = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    String Description = "", NextUserId = null, strNextRoldId = null;
    User usr = OBDal.getInstance().get(User.class, vars.getUser());
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    List<ESCMCustodytransferHist> ctHistoryList = new ArrayList<ESCMCustodytransferHist>();
    List<EutNextRoleLine> nxtRoleLnList = new ArrayList<EutNextRoleLine>();

    try {
      OBContext.setAdminMode();

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      OBQuery<ESCMCustodytransferHist> history = OBDal.getInstance().createQuery(
          ESCMCustodytransferHist.class,
          " as e where e.goodsShipment.id=:receiptID order by e.creationDate desc ");
      history.setNamedParameter("receiptID", receiptId);
      history.setMaxResult(1);
      ctHistoryList = history.list();
      if (ctHistoryList.size() > 0) {
        ESCMCustodytransferHist apphistory = ctHistoryList.get(0);
        if (apphistory.getRequestreqaction().equals("REV")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }
      ShipmentInOut headerCheck = OBDal.getInstance().get(ShipmentInOut.class, receiptId);

      if (headerCheck.getEscmDocstatus().equals("ESCM_TR")
          || headerCheck.getEscmDocstatus().equals("DR")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      int count = 0;
      boolean errorFlag = true;
      if (errorFlag) {
        ShipmentInOut header = OBDal.getInstance().get(ShipmentInOut.class, receiptId);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setEscmDocstatus("DR");
        header.setEscmCtdocaction("CO");
        header.setEscmCtapplevel(Long.valueOf(1));
        OBDal.getInstance().save(header);
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
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);
        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);

        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          alertWindow = AlertWindow.CustodyTransfer;

          // solve approval alerts - Task No:7618
          AlertUtility.solveAlerts(receiptId);

          // get alert recipients - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

          // check and insert alert recipient
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

          // get Current ApproverUser Id for a record &&
          // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
          if (inout.getEutNextRole() != null && inout.getEscmCtapplevel() != 4) { // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
            OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
                " as line where line.eUTNextRole.id=:roleID ");
            line.setNamedParameter("roleID", inout.getEutNextRole().getId());
            nxtRoleLnList = line.list();
            if (nxtRoleLnList.size() > 0) {
              NextUserId = nxtRoleLnList.get(0).getUserContact().getId();
              strNextRoldId = nxtRoleLnList.get(0).getRole().getId();
            }
          }
          // NextRoleByRuleVO nextApproval = NextRoleByRule.getCustTranNextRole(
          // OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
          // Resource.CUSTODY_TRANSFER, inout, NextUserId, "1", inout.getEscmCtapplevel());
          // EutNextRole nextRole = null;
          //
          // if (nextApproval.getNextRoleId() != null) {
          // nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
          // }

          // set alert for next approver
          Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.revoked", Lang) + " "
              + usr.getName();

          AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), strNextRoldId,
              NextUserId, header.getClient().getId(), Description, "NEW", alertWindow,
              "scm.ct.revoked", Constants.GENERIC_TEMPLATE);

          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("Escm_MIR_Revoke"));

          header.setEutNextRole(null);
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.CUSTODY_TRANSFER);
          // Removing forwardRMI id
          if (header.getEutForward() != null) {
            // Removing the Role Access given to the forwarded user
            // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutForward());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                Constants.Custody_Transfer);

          }
          if (header.getEutReqmoreinfo() != null) {
            // access remove
            // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
            // requistion.getEutReqmoreinfo().getId(), conn);
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutReqmoreinfo());

            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                Constants.Custody_Transfer);

          }

          OBDal.getInstance().commitAndClose();
        }
        bundle.setResult(obError);

      }
    } /*
       * catch (Exception e) { bundle.setResult(obError); OBDal.getInstance().rollbackAndClose();
       * log.error("Exception While Revoke Custody Transfer :", e); }
       */
    catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke Custody Transfer :", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);

    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
