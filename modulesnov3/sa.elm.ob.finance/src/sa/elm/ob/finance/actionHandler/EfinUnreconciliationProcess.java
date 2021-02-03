package sa.elm.ob.finance.actionHandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ReconciliationLine;
import sa.elm.ob.finance.ad_actionbutton.Reconcile.TransactionsDao;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author gopalakrishnan on 29-09-2016
 * 
 */

public class EfinUnreconciliationProcess extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(EfinUnreconciliationProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      String message = "";
      log.debug("params:" + jsonparams);
      SimpleDateFormat dteFormat = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      final String reconcileId = jsonRequest.getString("FIN_Reconciliation_ID");
      JSONObject encumlines = jsonparams.getJSONObject("UnReconciliation");
      String statementDate = jsonparams.getString("statement_date");
      String Description = jsonparams.getString("Description");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      FIN_Reconciliation objReconcile = OBDal.getInstance().get(FIN_Reconciliation.class,
          reconcileId);
      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          objReconcile.getAccount().getId());
      FIN_Reconciliation lastProcessedReconciliation = TransactionsDao
          .getLastReconciliation(account, "Y");

      // converting to form date to gregorian Date
      statementDate = dteFormat.format(yearFormat.parse(statementDate));
      statementDate = UtilityDAO.convertToGregorian(statementDate);
      statementDate = dteFormat.format(dateTimeFormat.parse(statementDate));

      Calendar calCurrent = Calendar.getInstance();
      calCurrent.setTime(FIN_Utility.getDateTime(statementDate));

      // Restrict the unreconcile process if data not selected in popup
      if (selectedlines.length() == 0) {
        JSONObject errorMessage = new JSONObject();
        message = OBMessageUtils.parseTranslation("@NoDataSelected@");
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
        return jsonResponse;
      }

      if (lastProcessedReconciliation != null) {

        Calendar calLast = Calendar.getInstance();
        calLast.setTime(
            FIN_Utility.getDateTime(dteFormat.format(lastProcessedReconciliation.getEndingDate())));
        if (calCurrent.before(calLast)) {
          JSONObject errorMessage = new JSONObject();
          message = OBMessageUtils.parseTranslation("@APRM_ReconcileInFutureOrPast@");
          errorMessage.put("severity", "error");
          errorMessage.put("text", message);
          jsonResponse.put("message", errorMessage);
          return jsonResponse;
        }
      }
      Calendar tomorrow = Calendar.getInstance();
      tomorrow.add(Calendar.DATE, 1);
      tomorrow.setTime(DateUtils.truncate(tomorrow.getTime(), Calendar.DATE));
      if (calCurrent.after(tomorrow)) {
        JSONObject errorMessage = new JSONObject();
        message = OBMessageUtils.parseTranslation("@APRM_ReconcileInFutureOrPast@");
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
        return jsonResponse;
      }

      // create unreconciled Record
      FIN_Reconciliation unreconcile = (FIN_Reconciliation) DalUtil.copy(objReconcile, false);
      unreconcile.setDocumentStatus("EFIN_UREC");
      unreconcile.setPosted("N");
      unreconcile.setProcessed(false);

      unreconcile.setTransactionDate(FIN_Utility.getDateTime(statementDate));
      unreconcile.setEndingDate(FIN_Utility.getDateTime(statementDate));
      unreconcile.setEfinDescription(Description);
      OBDal.getInstance().save(unreconcile);

      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        String recociliationLineId = selectedRow.getString("id");
        // update the selected line as unreconciled
        ReconciliationLine line = OBDal.getInstance().get(ReconciliationLine.class,
            recociliationLineId);
        line.setUnreconciled(true);
        OBDal.getInstance().save(line);
        // clone lines
        ReconciliationLine objCloneline = (ReconciliationLine) DalUtil.copy(line, false);
        objCloneline.setReconciliation(unreconcile);
        objCloneline.setUnreconciled(true);
        objCloneline.setWithdrawalAmount(line.getWithdrawalAmount().negate());
        OBDal.getInstance().save(objCloneline);

        // remove the reconciliation id from transactions to avail the same again reconcile.
        FIN_FinaccTransaction acctrx = line.getFinancialAccountTransaction();
        acctrx.setReconciliation(null);
        acctrx.setStatus("PWNC");
        OBDal.getInstance().save(acctrx);
        log.debug("reconcileId:" + reconcileId);
      }
      unreconcile.setProcessed(true);

      OBDal.getInstance().save(unreconcile);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      JSONObject errormsg = new JSONObject();

      message = OBMessageUtils.parseTranslation("@Success@");
      errormsg.put("severity", "success");
      errormsg.put("text", message);
      jsonResponse.put("message", errormsg);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception handling the UnReconciliation Process", e);

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug(" Returned Response " + jsonResponse);
    return jsonResponse;
  }

}
