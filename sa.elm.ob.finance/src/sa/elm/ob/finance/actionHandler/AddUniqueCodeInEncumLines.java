package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.actionHandler.dao.AddUniqueCodeInEncumLinesDAO;
import sa.elm.ob.finance.util.CommonValidations;

/**
 * 
 * @author Divya -03/04/2019
 *
 */
public class AddUniqueCodeInEncumLines extends BaseProcessActionHandler {

  final private static Logger log = Logger.getLogger(UniqueCodeFilterProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode();
      log.debug("entering into AddUniqueCodeInEncumLines process");

      // Get the Params value
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");

      String amount = jsonparams.getString("Amount");
      String combinationId = jsonparams.getString("C_Validcombination_ID");
      String encumbranceId = jsonRequest.getString("inpefinBudgetManencumId");
      String fundsAvailablePopupVal = jsonparams.getString("Funds_Available");

      BigDecimal Amount = new BigDecimal(amount);
      BigDecimal fAInPopup = new BigDecimal(fundsAvailablePopupVal);

      AccountingCombination combination = null;
      EfinBudgetManencum encum = null;
      String DIMESION_TYPE_EXPENSE = "E";
      long lineno = 10;
      Boolean errorFlag = false;
      BigDecimal fundsAvailable = BigDecimal.ZERO;
      JSONObject fundsCheckingObject = null;
      List<EfinBudgetManencumlines> encumLineList = null;
      if (!StringUtils.isEmpty("combinationId")) {
        combination = OBDal.getInstance().get(AccountingCombination.class, combinationId);
      }
      if (!StringUtils.isEmpty("encumbranceId")) {
        encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumbranceId);
      }

      if (encum != null && encum.getEfinBudgetManencumlinesList().size() > 0) {
        lineno = (encum.getEfinBudgetManencumlinesList().size() * 10) + 10;

        OBQuery<EfinBudgetManencumlines> chklineExists = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            " as e where e.manualEncumbrance.id=:encumbId and e.accountingCombination.id=:acctCombId  ");
        chklineExists.setNamedParameter("encumbId", encumbranceId);
        chklineExists.setNamedParameter("acctCombId", combination.getId());
        encumLineList = chklineExists.list();
        if (encumLineList.size() > 0) {
          errorFlag = true;
        }
      }
      try {
        if (DIMESION_TYPE_EXPENSE.equals(combination.getEfinDimensiontype())) {
          fundsCheckingObject = CommonValidations.getFundsAvailable(encum.getBudgetInitialization(),
              combination);
          fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
        }
      } catch (Exception e) {
        fundsAvailable = BigDecimal.ZERO;
      }

      if (Amount.compareTo(BigDecimal.ZERO) <= 0) {
        errorFlag = true;
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@EFIN_PenaltyRelAmtGrtZero@"));
        jsonResponse.put("message", errormsg);
        return jsonResponse;
      }

      if (fundsAvailable.compareTo(Amount) < 0) {
        errorFlag = true;
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_Encum_Amt_Error@"));
        jsonResponse.put("message", errormsg);
        return jsonResponse;
      }

      if (!errorFlag) {
        int count = AddUniqueCodeInEncumLinesDAO.insertEncumbranceLines(encum, Amount, combination,
            lineno, fAInPopup);
        if (count > 0) {
          OBDal.getInstance().commitAndClose();
          JSONObject successmsg = new JSONObject();
          successmsg.put("severity", "success");
          successmsg.put("text", OBMessageUtils.parseTranslation("@EFIN_AddUniqueCode_Success@"));
          jsonResponse.put("message", successmsg);
          return jsonResponse;
        } else {
          OBDal.getInstance().rollbackAndClose();
          JSONObject successmsg = new JSONObject();
          successmsg.put("severity", "error");
          successmsg.put("text", OBMessageUtils.parseTranslation("@EFIN_NotAddUniqueCode@"));
          jsonResponse.put("message", successmsg);
          return jsonResponse;
        }
      } else {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@efin_manenlne_uniqucode@"));
        jsonResponse.put("message", errormsg);
        return jsonResponse;
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
