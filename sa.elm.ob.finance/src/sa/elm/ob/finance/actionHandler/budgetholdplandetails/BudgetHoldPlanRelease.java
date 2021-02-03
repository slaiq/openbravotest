package sa.elm.ob.finance.actionHandler.budgetholdplandetails;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinRDVTransaction;

/**
 * @author divya J on 29-11-2019
 */

public class BudgetHoldPlanRelease extends BaseProcessActionHandler {

  /**
   * This servlet class was responsible for submit action in RDV.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetHoldPlanRelease.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      BudgetHoldPlanReleaseDAO holdPlanReleaseDAO = new BudgetHoldPlanReleaseDAOImpl();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("Release");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      Connection conn = OBDal.getInstance().getConnection();
      Boolean IS_MANUAL = true;
      // declaring variables
      final String rdvBudgHoldHeadId = jsonRequest.getString("inpefinRdvBudgholdId");
      String message = null;
      // getting Budget Hold Object
      EFINRdvBudgHold rdvBudgHold = OBDal.getInstance().get(EFINRdvBudgHold.class,
          rdvBudgHoldHeadId);

      EfinRDVTransaction rdvTxnObj = null;
      // get RDV Transaction Object
      if (rdvBudgHold.getEfinRdvtxn() != null)
        rdvTxnObj = OBDal.getInstance().get(EfinRDVTransaction.class,
            rdvBudgHold.getEfinRdvtxn().getId());

      // validation

      // hold and release should not be in same rdv version
      // Task no. 7945 point no 4
      Comparator<EfinRDVTransaction> comparator = Comparator
          .comparing(EfinRDVTransaction::getCreationDate);
      EfinRDVTransaction recentrdvTxn = rdvTxnObj.getEfinRdv().getEfinRDVTxnList().stream()
          .max(comparator).get();

      // if (recentrdvTxn.getTxnverStatus().equals("DR")
      // && !recentrdvTxn.getAppstatus().equals("DR")) {
      // message = OBMessageUtils.messageBD("Efin_Hold_RelNotInSameVers").replace("%",
      // rdvTxnObj.getEfinRdv().getDocumentNo() + "-" + rdvTxnObj.getTXNVersion());
      // JSONObject successMessage = new JSONObject();
      // successMessage.put("severity", "error");
      // successMessage.put("text", message);
      // json.put("message", successMessage);
      // return json;
      // }

      if (selectedlines.length() > 0) {
        for (int line = 0; line < selectedlines.length(); line++) {
          JSONObject selectedRow = selectedlines.getJSONObject(line);
          BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("enteredAmount"));
          String budgetHoldPlanLineId = selectedRow.getString("id");
          EFINRdvBudgHoldLine budgetholdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
              budgetHoldPlanLineId);
          BigDecimal remainingAmount = budgetholdLine.getHoldAmount()
              .subtract(budgetholdLine.getReleaseAmount().add(budgetholdLine.getBudgTransferamt()));
          if (releaseAmt.compareTo(remainingAmount) > 0) {
            JSONObject successMessage = new JSONObject();
            successMessage.put("severity", "error");
            successMessage.put("text", OBMessageUtils.messageBD("EFIN_HoldEntAmtNotGrtThanRemAmt"));
            json.put("message", successMessage);
            return json;
          }
        }
      }
      // // check budget revision created for this budget hold and fully release done- add budget
      // // transfer
      // if (rdvBudgHold.getEFINRdvBudgHoldLineList().size() > 0) {
      // List<EFINRdvBudgHoldLine> lineList = rdvBudgHold.getEFINRdvBudgHoldLineList().stream()
      // .filter(a -> a.getHoldAmount().compareTo(BigDecimal.ZERO) > 0
      // && ((a.getHoldAmount().subtract(a.getReleaseAmount()))
      // .compareTo(BigDecimal.ZERO) > 0))
      // .collect(Collectors.toList());
      // if (lineList.size() == 0) {
      // JSONObject successMessage = new JSONObject();
      // successMessage.put("severity", "error");
      // successMessage.put("text", "No lines to release the hold Amount");
      // json.put("message", successMessage);
      // return json;
      // }
      // }

      // do release
      JSONObject result = holdPlanReleaseDAO.addHoldReleaseInRDV(conn, rdvBudgHold, selectedlines,
          IS_MANUAL);
      if (result.has("result")) {
        if (result.getString("result").equals("1")) {
          OBDal.getInstance().flush();
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "success");
          errorMessage.put("text", OBMessageUtils.messageBD("Efin_HoldPlanRelSuccess"));
          json.put("message", errorMessage);
          return json;
        } else {
          OBDal.getInstance().rollbackAndClose();
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text", OBMessageUtils.messageBD("Efin_HoldPlanRelNotSuccess"));
          json.put("message", successMessage);
          return json;
        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (log.isErrorEnabled()) {
        log.error("Exeception in BudgetHoldPlanReleaseProcess:", e);
      }
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;

  }

}