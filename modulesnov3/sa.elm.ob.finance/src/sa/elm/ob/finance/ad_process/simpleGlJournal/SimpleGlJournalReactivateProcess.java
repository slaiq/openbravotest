package sa.elm.ob.finance.ad_process.simpleGlJournal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;

import sa.elm.ob.finance.EfinBudgetActual;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.util.ApprovalTables;

/**
 * @author Poongodi on 12/04/2019
 */

public class SimpleGlJournalReactivateProcess implements Process {

  private static final Logger log = Logger.getLogger(SimpleGlJournalReactivateProcess.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      ConnectionProvider conn = bundle.getConnection();
      String glJournalId = (String) bundle.getParams().get("GL_Journal_ID");
      GLJournal objJournal = OBDal.getInstance().get(GLJournal.class, glJournalId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objJournal.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      boolean errorFlag = false;
      Currency currency = null;
      BigDecimal conversionrate = BigDecimal.ZERO;
      // After posting should not allow to do reactivate
      if (objJournal.getEfinAction().equals("PD")) {
        if (objJournal.getPosted().equals("Y")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@GLJournalDocumentPosted@");
          bundle.setResult(result);
          return;
        }
      }

      // change the status
      if (objJournal.getDocumentStatus().equals("CO") && objJournal.getEfinAction().equals("PD")) {

        if (objJournal.isEfinAdjInvoice() && objJournal.getEfinCInvoice() != null) {
          currency = FinanceUtils.getCurrency(orgId, objJournal.getEfinCInvoice());
          // get conversion rate
          conversionrate = FinanceUtils.getConversionRate(OBDal.getInstance().getConnection(),
              orgId, objJournal.getEfinCInvoice(), currency);
          SimpleGlJournalDAO.updateAppliedAmountToUsedAmount(objJournal, conversionrate, true);
          SimpleGlJournalDAO.updatePrepaymentUsedAmount(objJournal.getId(), conversionrate, true);
        }
        // If Reactive the Journal Then Remove the Record into Actual Table.
        for (GLJournalLine jline : objJournal.getFinancialMgmtGLJournalLineList()) {
          OBQuery<EfinBudgetActual> actual = OBDal.getInstance().createQuery(EfinBudgetActual.class,
              "journalLine.id='" + jline.getId() + "'");
          List<EfinBudgetActual> actualList = actual.list();
          if (actualList.size() > 0) {
            EfinBudgetActual actualdetail = actualList.get(0);
            OBDal.getInstance().remove(actualdetail);
          } else
            continue;
        }

        try {
          // Call GL_Journal_Post method from the database.
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(null);
          parameters.add(glJournalId);
          final String procedureName = "gl_journal_post";
          CallStoredProcedure mm = CallStoredProcedure.getInstance();
          mm.call(procedureName, parameters, null, false, false);
        } catch (Exception e) {
          OBDal.getInstance().rollbackAndClose();
          OBError error = OBMessageUtils.translateError(conn, vars, vars.getLanguage(),
              e.getCause().getMessage());
          throw new OBException(error.getMessage());
        }

        OBDal.getInstance().refresh(objJournal);

        for (GLJournalLine jline : objJournal.getFinancialMgmtGLJournalLineList()) {
          GLJournalLine journalLine = OBDal.getInstance().get(GLJournalLine.class, jline.getId());
          journalLine.setEfinCheckingStaus(null);
          journalLine.setEfinCheckingStausFailure("");
          OBDal.getInstance().save(journalLine);
        }

        if (!errorFlag) {
          objJournal.setUpdated(new java.util.Date());
          objJournal.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          objJournal.setDocumentStatus("DR");
          objJournal.setEfinAction("CO");
          objJournal.setEutNextRole(null);
          OBDal.getInstance().save(objJournal);

          // insert approval history
          if (!StringUtils.isEmpty(objJournal.getId())) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", objJournal.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.GL_JOURNAL_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.GL_JOURNAL_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.GL_JOURNAL_DOCACTION_COLUMN);

            SimpleGlJournalDAO.glJournalHistory(historyData);

          }

          // Remove temporary encumbrance created
          if (objJournal.getEFINBudgetManencum() != null) {
            SimpleGlJournalDAO.removeTemporaryEncumbrance(objJournal);
          }

          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in gljournalReactivate:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
