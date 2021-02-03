package sa.elm.ob.finance.ad_process.simpleGlJournal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;

/**
 * 
 * @author poongodi on 12/04/2019
 *
 */
public class SimpleGlJournalReject implements Process {
  private static final Logger log = Logger.getLogger(SimpleGlJournalReject.class);
  private final OBError obError = new OBError();

  @SuppressWarnings("unused")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      /* throw new RuntimeException(e); */
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    }
    String glJournalId = (String) bundle.getParams().get("GL_Journal_ID");
    GLJournal objJournal = OBDal.getInstance().get(GLJournal.class, glJournalId);

    String alertRuleId = "", alertWindow = AlertWindow.GlJournal;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objJournal.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String headerId = null;
    String appstatus = "";
    String Lang = vars.getLanguage();

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    Connection con = OBDal.getInstance().getConnection();
    EfinBudgetManencum manualId = null;
    String errorMsg = "";
    Date currentDate = new Date();
    String sql = "";
    String rolesequenceno = null;
    boolean isfundserrorFlag = true;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String forwardId = null, requestMoreInfoId = null;
    Currency currency = null;
    BigDecimal conversionrate = BigDecimal.ZERO;
    if (objJournal.getDocumentStatus().equals("CO")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);

          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          if (objJournal.getEutNextRole() != null) {
            java.util.List<EutNextRoleLine> li = objJournal.getEutNextRole()
                .getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
            // check current role is a delegated role or not
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                DocumentTypeE.JOURNAL_ENTRIES.getDocumentTypeCode());

            if (allowUpdate || allowDelegation) {

              if (objJournal.isEfinAdjInvoice() && objJournal.getEfinCInvoice() != null) {
                currency = FinanceUtils.getCurrency(orgId, objJournal.getEfinCInvoice());
                // get conversion rate
                conversionrate = FinanceUtils.getConversionRate(OBDal.getInstance().getConnection(),
                    orgId, objJournal.getEfinCInvoice(), currency);
                SimpleGlJournalDAO.updateAppliedAmountToUsedAmount(objJournal, conversionrate,
                    true);
                SimpleGlJournalDAO.updatePrepaymentUsedAmount(objJournal.getId(), conversionrate,
                    true);
              }

              objJournal.setUpdated(new java.util.Date());
              objJournal.setUpdatedBy(OBContext.getOBContext().getUser());
              objJournal.setDocumentStatus("DR");
              objJournal.setEfinAction("CO");
              objJournal.setEutNextRole(null);
              objJournal.setEFINRevoke(false);
              log.debug("header:" + objJournal.toString());
              OBDal.getInstance().save(objJournal);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.GLJOURNAL_RULE);
              headerId = objJournal.getId();
              if (!StringUtils.isEmpty(objJournal.getId())) {
                appstatus = "REW";
                JSONObject historyData = new JSONObject();

                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId);
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.GL_JOURNAL_HISTORY);
                historyData.put("HeaderColumn", ApprovalTables.GL_JOURNAL_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.GL_JOURNAL_DOCACTION_COLUMN);
                SimpleGlJournalDAO.glJournalHistory(historyData);
              }

              Role objCreatedRole = null;
              if (objJournal.getCreatedBy().getADUserRolesList().size() > 0) {
                if (objJournal.getEFINRole() != null)
                  objCreatedRole = objJournal.getEFINRole();
                else
                  objCreatedRole = objJournal.getCreatedBy().getADUserRolesList().get(0).getRole();
              }
              log.debug("objCreatedRole:" + objCreatedRole);

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
              // delete alert for approval alerts
              OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
                  "as e where e.referenceSearchKey='" + objJournal.getId()
                      + "' and e.alertStatus='NEW'");

              if (alertQuery.list().size() > 0) {
                for (Alert objAlert : alertQuery.list()) {
                  objAlert.setAlertStatus("SOLVED");
                }
              }

              String Description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.gljournal.rejected", vars.getLanguage()) + " " + objUser.getName();
              log.debug("Description:" + Description);
              // delete the unused nextroles in eut_next_role table.
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.GLJOURNAL_RULE);
              AlertUtility.alertInsertionRole(objJournal.getId(), objJournal.getDocumentNo(),
                  objCreatedRole.getId(), objJournal.getCreatedBy().getId(),
                  objJournal.getClient().getId(), Description, "NEW", alertWindow,
                  "finance.gljournal.rejected", Constants.GENERIC_TEMPLATE);

              // Remove temporary encumbrance created
              if (objJournal.getEFINBudgetManencum() != null) {
                SimpleGlJournalDAO.removeTemporaryEncumbrance(objJournal);
              }

              if (!StringUtils.isEmpty(objJournal.getId())) {

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Efin_GlJournal_Reject@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().commitAndClose();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
              throw new OBException(errorMsg);
            }
          }

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);

          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);

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
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
