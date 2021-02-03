package sa.elm.ob.finance.actionHandler.budgetRevisionHoldPlan;

import org.codehaus.jettison.json.JSONArray;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetTransfertrx;

public interface BudgetRevisionHoldProcessDao {

  /**
   * Method to insert budget revision
   * 
   * @param selectedlines
   * 
   */
  public EfinBudgetTransfertrx insertBudgetRevisionHeader(JSONArray selectedlines,
      String revDocType);

  /**
   * 
   * @param accCombination
   * @param budgetInt
   * @return
   */
  public AccountingCombination getParentFunds990acct(AccountingCombination accCombination,
      EfinBudgetIntialization budgetInt);

}
