package sa.elm.ob.finance.util.budget;

/**
 * 
 * @author Gopinagh.R
 *
 */

public interface BudgetingUtilsService {

  /**
   * Checks whether the element belongs to either of 31 or 41 account group.
   * 
   * @param strElementValueId
   * 
   */
  public Boolean isProjectAccount(String strElementValueId, String strClientId);

  /**
   * Checks whether the account is a funds only account.
   * 
   * returns true, only if the account is a parent account (31/41) account and funds only flag is
   * set.
   * 
   * @param strElementValueID
   * @param strClientId
   * @return
   */
  public Boolean isFundsOnlyAccount(String strElementValueID, String strClientId);

}