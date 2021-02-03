
package sa.elm.ob.finance.util.autoreleasefunds;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;

/**
 * @author Sathishkumar.p
 *
 */
public interface AutoReleaseFundsService {

  /**
   * This method will check in case of decrease then we should check whether 990 account has enough
   * fundsavailable for decrease, if not check 999 account
   * 
   * @param revision
   * @param adjustment
   * @return true if it has enough bcu funds or else false
   */
  public Boolean checkBCUFundsAvailable(EfinBudgetTransfertrx revision, BudgetAdjustment adjustment,
      HashMap<EfinBudgetTransfertrxline, BigDecimal> revLineList,
      HashMap<BudgetAdjustmentLine, BigDecimal> adjustmentLineList);

  /**
   * This method is used to insert release entry in funds request management
   * 
   * @param revision(EfinBudgetTransfertrx
   *          object)
   * 
   * @param distribution(EFINFundsReq
   *          Object)
   * @return true if it has created successfully or else false
   */
  public Boolean insertReleaseInBudgetDistribution(EfinBudgetTransfertrx revision,
      BudgetAdjustment distribution, HashMap<EfinBudgetTransfertrxline, BigDecimal> revLineMap,
      HashMap<BudgetAdjustmentLine, BigDecimal> adjustmentLineMap, boolean isDistribution,
      List<EFINFundsReqLine> lineList);

}
