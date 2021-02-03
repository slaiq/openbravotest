package sa.elm.ob.finance.ad_process.RDVProcess;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

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

import sa.elm.ob.finance.EfinRDVTxnline;
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

/**
 * 
 * @author divya on 24/09/2019
 *
 */
public class RDVLineRevokeProcess implements Process {

  private static final Logger log = Logger.getLogger(RDVLineRevokeProcess.class);
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
    boolean errorFlag = true;
    Boolean IsAdvance = false;
    String constant = null;

    final String rdvTxnLineId = (String) bundle.getParams().get("Efin_Rdvtxnline_ID");

    try {
      OBContext.setAdminMode(true);
      EfinRDVTxnline transactionLine = OBDal.getInstance().get(EfinRDVTxnline.class, rdvTxnLineId);

      constant = Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE;

      if (!transactionLine.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }
      if (errorFlag) {
        try {

          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          nextRole = transactionLine.getNextRole();

          // get alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

          // set alerts for next roles
          if (nextRole.getEutNextRoleLineList().size() > 0) {
            // delete alert for approval alerts
            OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                "as e where e.referenceSearchKey='" + transactionLine.getId()
                    + "' and e.alertStatus='NEW'");
            if (alertQuery.list().size() > 0) {
              for (Alert objAlert : alertQuery.list()) {
                objAlert.setAlertStatus("SOLVED");
              }
            }

            String description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.rdvLine.revoked", vars.getLanguage()) + " "
                + transactionLine.getCreatedBy().getName();

            for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
              try {
                AlertUtility.alertInsertionRole(transactionLine.getId(),
                    transactionLine.getTrxlnNo().toString(), objNextRoleLine.getRole().getId(),
                    (objNextRoleLine.getUserContact() == null ? ""
                        : objNextRoleLine.getUserContact().getId()),
                    transactionLine.getClient().getId(), description, "NEW", alertWindow,
                    "finance.rdvLine.revoked", Constants.GENERIC_TEMPLATE);
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

          transactionLine.setUpdated(new java.util.Date());
          transactionLine.setUpdatedBy(OBContext.getOBContext().getUser());
          transactionLine.setApprovalStatus("DR");
          transactionLine.setAction("CO");
          transactionLine.setNextRole(null);

          OBDal.getInstance().save(transactionLine);
          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.RDV_Transaction);
          headerId = transactionLine.getId();
          log.debug("headerId:" + transactionLine.getId());
          if (!StringUtils.isEmpty(transactionLine.getId())) {
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
            historyData.put("HistoryTable", ApprovalTables.RDV_TxnLine_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_TxnLine_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_TxnLine_DOCACTION_COLUMN);
            Utility.InsertApprovalHistory(historyData);

          }
          if (!StringUtils.isEmpty(transactionLine.getId())) {
            // Removing forwardRMI id
            // if (transactionLine.getEUTForwardReqmoreinfo() != null) {
            // // Removing the Role Access given to the forwarded user
            // // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);
            // // update status as "DR"
            // forwardReqMoreInfoDAO.setForwardStatusAsDraft(version.getEUTForwardReqmoreinfo());
            // // Removing Forward_Rmi id from transaction screens
            // forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(version.getId(), constant);
            //
            // }
            // if (transactionLine.getEUTReqmoreinfo() != null) {
            // // access remove
            // // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
            // // requistion.getEutReqmoreinfo().getId(), conn);
            // // update status as "DR"
            // forwardReqMoreInfoDAO.setForwardStatusAsDraft(version.getEUTReqmoreinfo());
            //
            // // Remove Forward_Rmi id from transaction screens
            // forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(version.getId(), constant);
            //
            // }

            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDVLine_Revoke@");
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
