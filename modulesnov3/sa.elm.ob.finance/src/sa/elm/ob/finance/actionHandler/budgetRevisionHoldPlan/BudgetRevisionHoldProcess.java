package sa.elm.ob.finance.actionHandler.budgetRevisionHoldPlan;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinBudgetTransfertrx;

/**
 * 
 * @author poongodi on 28/11/2019
 *
 */

public class BudgetRevisionHoldProcess extends BaseProcessActionHandler {

  final private static Logger log = Logger.getLogger(BudgetRevisionHoldProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    boolean insertLine = true;
    try {
      BudgetRevisionHoldProcessDAOImpl revisionProcessDAO = new BudgetRevisionHoldProcessDAOImpl();

      // Get the Params value
      log.debug("entering into BudgetRevisionHoldProcess");
      JSONObject jsonRequest = new JSONObject(content);

      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject inspectLines = jsonparams.getJSONObject("Revision");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");

      String revDocType = jsonparams.getString("Doctype");
      String uniqueCode = null;
      String funds_uniqueCode = null;
      EfinBudgetTransfertrx header = null;

      if (selectedlines.length() == 0) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_Line_Not_Selected"));
        json.put("message", successMessage);
        return json;
      } else {
        // check the funds accounts created
        for (int i = 0; i < selectedlines.length(); i++) {
          JSONObject selectedRow = selectedlines.getJSONObject(i);
          if (selectedRow.getString("accountingCombination") != null) {
            AccountingCombination accCombination = OBDal.getInstance()
                .get(AccountingCombination.class, selectedRow.getString("accountingCombination"));
            if (accCombination != null) {
              uniqueCode = accCombination.getEfinUniqueCode();
              if (accCombination.getSalesCampaign().getEfinBudgettype().equals("C")) {
                if (accCombination.getEfinFundscombination() != null)
                  funds_uniqueCode = accCombination.getEfinFundscombination().getEfinUniqueCode();
                else
                  funds_uniqueCode = null;
              } else {
                funds_uniqueCode = uniqueCode;
              }

            }
            if (funds_uniqueCode == null) {
              JSONObject successMessage = new JSONObject();
              successMessage.put("severity", "error");
              successMessage.put("text",
                  OBMessageUtils.messageBD("Efin_FundsAcc_Error").replace("%", uniqueCode));
              json.put("message", successMessage);
              return json;
            }

          }
          BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("amount"));
          EFINRdvBudgHoldLine budgetholdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
              selectedRow.getString("efinRdvBudgholdline"));
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

        header = revisionProcessDAO.insertBudgetRevisionHeader(selectedlines, revDocType);

        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("Efin_BudgetRev_Completed").replace("%",
            header.getDocumentNo()));
        json.put("message", successMessage);
        return json;
      }

    } catch (Exception e) {
      log.error("Exception in BudgetRevisionHoldProcess :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    }

  }
}
