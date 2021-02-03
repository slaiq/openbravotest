package sa.elm.ob.finance.util.budget;

import java.util.List;

import org.openbravo.model.ad.utility.TreeNode;

/**
 * 
 * @author Gopinagh.R
 *
 */

public interface BudgetUtilsDAO {

  /**
   * Get the parent of the element from {@link TreeNode}
   * 
   * @param strElementValueID
   * @param strClientID
   * @return parentID
   */
  public String getParentAccount(String strElementValueID, String strClientID);

  /**
   * returns the list of available children for the element.
   * 
   * @param strElementValueID
   * @return
   */

  public List<String> getChildren(String strElementValueID, String strClientID);

  /**
   * 
   * Gets the intersection of accounts from cost and funds budget type accounts. essentially returns
   * a list of 31 and 41 accounts.
   * 
   * @param strClientID
   * @return
   */
  public List<String> getProjectSummaryAccounts(String strClientID);

  /**
   * Gets the tree ID for accounts for the specified client.
   * 
   * @param strClientID
   * @return
   */
  public String getTreeID(String strClientID);
}
