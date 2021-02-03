package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.util.Utility;

public class GetFundsAvailable extends BaseActionHandler {

  final private static Logger log = Logger.getLogger(UniqueCodeFilterProcess.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);

      // Get the Params value

      log.debug("entering into AddUniqueCodeInEncumLines process");
      String action = null;
      String combinationId = null;
      String encumbranceId = jsonRequest.getString("targetRecordId");
      EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
          encumbranceId);
      String budgetInitialId = null;
      String DIMESION_TYPE_EXPENSE = "E";
      if (jsonRequest.has("action")) {
        action = jsonRequest.getString("action");
      }
      if (jsonRequest.has("combinationId")) {
        combinationId = jsonRequest.getString("combinationId");
      }
      if (encumbrance != null && StringUtils.isNotEmpty(encumbranceId)
          && encumbrance.getBudgetInitialization() != null) {
        budgetInitialId = encumbrance.getBudgetInitialization().getId();
      }

      if (action.equals("getfundsAvailable")) {
        AccountingCombination combination = OBDal.getInstance().get(AccountingCombination.class,
            combinationId);
        BigDecimal fundsAvailable = BigDecimal.ZERO;
        JSONObject fundsCheckingObject = null;

        if (combination != null && budgetInitialId != null) {
          EfinBudgetIntialization budgetIntialization = Utility
              .getObject(EfinBudgetIntialization.class, budgetInitialId);

          try {
            if (DIMESION_TYPE_EXPENSE.equals(combination.getEfinDimensiontype())) {
              fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                  combination);
              fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
            }
          } catch (Exception e) {
            fundsAvailable = BigDecimal.ZERO;
          }
        }
        jsonResponse.put("funds_available", fundsAvailable);
      }

    } catch (final Exception e) {
      e.printStackTrace();
      JSONObject errormsg = new JSONObject();
      try {
        errormsg.put("severity", "error");
        errormsg.put("text", errormsg.put("text", "Error while filtering unique code"));
        jsonResponse.put("message", errormsg);
      } catch (JSONException e1) {
        log.error("exception :", e1);
      }
    }
    return jsonResponse;
  }
}
