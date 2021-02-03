package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.HashMap;

public interface MergedProposal {

  /**
   * This method is used to update the awarding qty in proposal lines
   * 
   * @param lineMap
   * 
   */
  public void updateAwardQtyinProposal(HashMap<String, BigDecimal> qtyLineMap,
      HashMap<String, BigDecimal> qtyMap);

  /**
   * This method is used to validate the qty entered by the user
   * 
   * @param qtyMap
   * @param qtyLineMap
   * @return
   */
  public boolean validateQty(HashMap<String, BigDecimal> qtyMap,
      HashMap<String, BigDecimal> qtyLineMap);

  /**
   * This method is used to update the status and ispartial flag in proposal
   * 
   * @param qtyMap
   */
  public void updateStatusInProposal(HashMap<String, BigDecimal> qtyMap, boolean awardFullqty);

}
