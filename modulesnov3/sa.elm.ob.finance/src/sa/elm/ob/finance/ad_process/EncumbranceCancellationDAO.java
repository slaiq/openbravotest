/**
 * 
 */
package sa.elm.ob.finance.ad_process;

import sa.elm.ob.finance.EfinBudgetManencum;

/**
 * @author Gopinagh. R
 *
 */
public interface EncumbranceCancellationDAO {

  /**
   * checks if the encumbrance is transacted.
   * 
   * @param encumbrance
   *          {@link EfinBudgetManencum} object
   * @return {@link Boolean} true if the record is transacted
   */

  public Boolean isTransactedEncumbrance(EfinBudgetManencum encumbrance);

}
