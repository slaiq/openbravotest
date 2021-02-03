package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceRevokeDAO;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Constants;

public class ManualEncumbranceRevoke implements Process {

  private static final Logger log = Logger.getLogger(ManualEncumbranceRevoke.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("rework the budget");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = (String) bundle.getContext().getOrganization();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    final String REVOKE = "REV";
    String comments = (String) bundle.getParams().get("comments").toString();
    String headerId = null, alertWindow = AlertWindow.Encumbrance, alertRuleId = null,
        Description = "";
    EutNextRole nextRole = null;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    boolean errorFlag = true;
    int count = 0;
    final String ManEncumId = (String) bundle.getParams().get("Efin_Budget_Manencum_ID");
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    log.debug("budgetId:" + ManEncumId);

    try {
      OBContext.setAdminMode(true);
      EfinBudgetManencum headerCheck = OBDal.getInstance().get(EfinBudgetManencum.class,
          ManEncumId);
      nextRole = OBDal.getInstance().get(EutNextRole.class, headerCheck.getNextRole().getId());

      if (!headerCheck.isRevoke()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(result);
        return;
      }
      if (errorFlag) {
        try {
          OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class, "manualEncumbrance.id ='" + ManEncumId + "'");
          count = lines.list().size();

          if (count > 0) {
            EfinBudgetManencum header = OBDal.getInstance().get(EfinBudgetManencum.class,
                ManEncumId);

            // update header status
            header.setUpdated(new java.util.Date());
            header.setUpdatedBy(OBContext.getOBContext().getUser());
            header.setDocumentStatus("DR");
            header.setAction("CO");
            header.setNextRole(null);
            log.debug("header:" + header.toString());
            OBDal.getInstance().save(header);

            headerId = header.getId();
            if (!StringUtils.isEmpty(header.getId())) {

              ManualEncumbaranceSubmitDAO.insertManEncumHistory(OBDal.getInstance().getConnection(),
                  clientId, orgId, roleId, userId, headerId, comments, REVOKE, null);

              // if reserve funds crossed then revert the impacts of budget enquiry.
              if (header.isReservedfund())
                ManualEncumbaranceRevokeDAO.revokeBudgetEnquiryImpact(header);

              // RemoveEncumRecord(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
              // userId, headerId);
            }

            // Alert Process Start
            if (!StringUtils.isEmpty(header.getId())) {

              Role objCreatedRole = null;
              if (header.getCreatedBy().getADUserRolesList().size() > 0) {
                objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
              }

              // get alert rule id
              OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
                  "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                      + "'");
              if (queryAlertRule.list().size() > 0) {
                AlertRule objRule = queryAlertRule.list().get(0);
                alertRuleId = objRule.getId();
              }

              // delete alert for approval alerts
              OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                  "as e where e.referenceSearchKey='" + header.getId()
                      + "' and e.alertStatus='NEW'");
              if (alertQuery.list().size() > 0) {
                for (Alert objAlert : alertQuery.list()) {
                  objAlert.setAlertStatus("SOLVED");
                  OBDal.getInstance().save(objAlert);
                }
              }

              // check and insert alert recipient
              OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                  AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

              if (receipientQuery.list().size() > 0) {
                for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
                  includeRecipient.add(objAlertReceipient.getRole().getId());
                  OBDal.getInstance().remove(objAlertReceipient);
                }
              }
              includeRecipient.add(objCreatedRole.getId());

              // avoid duplicate recipient
              HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
              Iterator<String> iterator = incluedSet.iterator();
              while (iterator.hasNext()) {
                AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
              }

              Description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.encumbrance.revoked", vars.getLanguage()) + " " + objUser.getName();

              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                    objNextRoleLine.getRole().getId(),
                    (objNextRoleLine.getUserContact() == null ? ""
                        : objNextRoleLine.getUserContact().getId()),
                    header.getClient().getId(), Description, "NEW", alertWindow,
                    "finance.encumbrance.revoked", Constants.GENERIC_TEMPLATE);
              }
            }
            // Alert Process end

            // delete records from next role table
            DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.MANUAL_ENCUMBRANCE_RULE);

            // Removing the Forward and RMI id
            if (header.getEUTForwardReqmoreinfo() != null) {
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
              forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                  Constants.ENCUMBRANCE);
            }
            if (header.getEUTReqmoreinfo() != null) {
              forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
              forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                  Constants.ENCUMBRANCE);
            }

            if (count > 0 && !StringUtils.isEmpty(header.getId())) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_ManEncum_Revoke@");
              bundle.setResult(result);
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
              return;
            }
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

  public static int RemoveEncumRecord(Connection con, String clientId, String orgId, String roleId,
      String userId, String headerId) {
    try {
      OBContext.setAdminMode(true);
      OBQuery<EfinBudgetManencumlines> encumlines = OBDal.getInstance()
          .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id ='" + headerId + "'");
      log.debug("encumlines:" + encumlines.getWhereAndOrderBy());
      if (encumlines.list().size() > 0) {
        for (int i = 0; i < encumlines.list().size(); i++) {
          EfinBudgetManencumlines list = encumlines.list().get(i);

          OBQuery<efinbudgetencum> qry = OBDal.getInstance().createQuery(efinbudgetencum.class,
              " manualEncumbranceLines.id='" + list.getId() + "'");
          log.debug("removewhere:" + qry.getWhereAndOrderBy());
          if (qry.list().size() > 0) {
            for (int j = 0; j < qry.list().size(); j++) {
              efinbudgetencum encumheader = qry.list().get(j);
              OBDal.getInstance().remove(encumheader);
            }
          }
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }
}
