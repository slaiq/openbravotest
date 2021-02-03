package sa.elm.ob.finance.ad_process.RDVProcess;

import java.util.ArrayList;
import java.util.Date;
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
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.dms.service.DMSRDVService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSRDVServiceImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Poongodi on 19/01/2018
 *
 */
public class RDVRevokeProcess implements Process {

  private static final Logger log = Logger.getLogger(RDVRevokeProcess.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    EutNextRole nextRole = null;
    Date currentDate = new Date();
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = (String) bundle.getContext().getOrganization();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    String headerId = null;
    String appstatus = "";
    String alertRuleId = "", alertWindow = AlertWindow.RDVTransaction;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    boolean errorFlag = true;
    Boolean IsAdvance = false;
    String constant = null;

    final String rdvTxnId = (String) bundle.getParams().get("Efin_Rdvtxn_ID");

    try {
      OBContext.setAdminMode(true);
      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class, rdvTxnId);

      // get alert window based on advance
      if (transaction.getEfinRDVTxnlineList().size() == 1) {
        EfinRDVTxnline txnLine = transaction.getEfinRDVTxnlineList().get(0);
        IsAdvance = txnLine.isAdvance();
        if (IsAdvance) {
          alertWindow = AlertWindow.RDVAdvance;
          constant = Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE;
        } else {
          alertWindow = AlertWindow.RDVTransaction;
          constant = Constants.RECEIPT_DELIVERY_VERIFICATION;
        }
      }

      if (!transaction.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }
      if (errorFlag) {
        try {

          EfinRDVTransaction version = OBDal.getInstance().get(EfinRDVTransaction.class, rdvTxnId);

          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          nextRole = version.getNextRole();

          // get alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

          // set alerts for next roles
          if (nextRole.getEutNextRoleLineList().size() > 0) {
            // delete alert for approval alerts
            OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                "as e where e.referenceSearchKey='" + version.getId()
                    + "' and e.alertStatus='NEW'");
            if (alertQuery.list().size() > 0) {
              for (Alert objAlert : alertQuery.list()) {
                objAlert.setAlertStatus("SOLVED");
              }
            }

            String description = sa.elm.ob.finance.properties.Resource.getProperty(
                "finance.rdv.revoked", vars.getLanguage()) + " " + version.getCreatedBy().getName();

            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              try {
                AlertUtility.alertInsertionRole(version.getId(),
                    version.getTXNVersion() + (StringUtils.isEmpty(version.getCertificateNo()) ? ""
                        : "-" + version.getCertificateNo()),
                    objNextRoleLine.getRole().getId(),
                    (objNextRoleLine.getUserContact() == null ? ""
                        : objNextRoleLine.getUserContact().getId()),
                    version.getClient().getId(), description, "NEW", alertWindow,
                    "finance.rdv.revoked", Constants.GENERIC_TEMPLATE);
              } catch (Exception e) {

              }
              // get user name for delegated user to insert on approval history.
              OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                  EutDocappDelegateln.class,
                  " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                      + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                      + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_124'");
              if (delegationln != null && delegationln.list().size() > 0) {
                includeRecipient.add(delegationln.list().get(0).getRole().getId());
              }
              // add next role recipient
              includeRecipient.add(objNextRoleLine.getRole().getId());

            }
          }
          // existing Recipient
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
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

          version.setUpdated(new java.util.Date());
          version.setUpdatedBy(OBContext.getOBContext().getUser());
          version.setAppstatus("DR");
          version.setAction("CO");
          version.setNextRole(null);
          version.setSubmitroleid(null);
          if (version.isContractcategoryRolePassed()) {
            version.setContractcategoryRolePassed(false);
          }

          if (version.isAdvancetransaction() && version.getEfinRDVTxnlineList().size() > 0) {
            EfinRDVTxnline txnLineObj = version.getEfinRDVTxnlineList().get(0);
            txnLineObj.setAction("CO");
            txnLineObj.setApprovalStatus("DR");
            OBDal.getInstance().save(txnLineObj);
          }

          OBDal.getInstance().save(version);
          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.RDV_Transaction);
          headerId = version.getId();
          log.debug("headerId:" + version.getId());
          if (!StringUtils.isEmpty(version.getId())) {
            appstatus = "REV";
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", headerId);
            historyData.put("Comments", comments);
            historyData.put("Status", appstatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.RDV_Txn_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_Txn_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_Txn_DOCACTION_COLUMN);
            Utility.InsertApprovalHistory(historyData);

          }
          if (!StringUtils.isEmpty(version.getId())) {
            // Removing forwardRMI id
            if (version.getEUTForwardReqmoreinfo() != null) {
              // Removing the Role Access given to the forwarded user
              // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(version.getEUTForwardReqmoreinfo());
              // Removing Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(version.getId(), constant);

            }
            if (version.getEUTReqmoreinfo() != null) {
              // access remove
              // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
              // requistion.getEutReqmoreinfo().getId(), conn);
              // update status as "DR"
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(version.getEUTReqmoreinfo());

              // Remove Forward_Rmi id from transaction screens
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(version.getId(), constant);

            }

            // Check Encumbrance Amount is Zero Or Negative
            if (transaction.getEfinRdv() != null) {
              if (transaction.getEfinRdv().getManualEncumbrance() != null) {
                encumLinelist = transaction.getEfinRdv().getManualEncumbrance()
                    .getEfinBudgetManencumlinesList();
              }
            }
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (checkEncumbranceAmountZero) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
              bundle.setResult(result);
              return;
            }

            try {
              // DMS integration
              DMSRDVService dmsService = new DMSRDVServiceImpl();
              dmsService.rejectAndReactivateOperations(transaction);
            } catch (Exception e) {
              log.error("Error while deleting the record in dms revoke" + e.getMessage());
            }

            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Revoke@");
            bundle.setResult(result);
            return;
          }

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);
          OBDal.getInstance().rollbackAndClose();

        }
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
