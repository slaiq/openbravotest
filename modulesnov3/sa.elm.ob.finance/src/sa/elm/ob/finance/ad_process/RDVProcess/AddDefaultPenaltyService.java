package sa.elm.ob.finance.ad_process.RDVProcess;

import org.codehaus.jettison.json.JSONObject;

/**
 * 
 * @author Gopinagh.R
 *
 */
public interface AddDefaultPenaltyService {

  /**
   * Checks if the current line is applicable to add penalty
   * 
   * @param strRDVTrxLineID
   * @param strMatchQty
   * @return
   */
  public JSONObject applicableForPenalty(String strRDVTrxLineID, String strMatchQty,
      String strMatchAmt);

  /**
   * Checks if the penalty is already applied.
   * 
   * @param strRDVTrxLineID
   * @return
   */
  public Boolean hasPenaltyApplied(String strRDVTrxLineID);

  /**
   * Add a default penalty if the receipt date is greater than the need by date.
   * 
   * @param strRDVTrxLineID
   * @param strMatchQty
   * @param actionDate
   * @param strAdvanceDeductionAmount
   * @return
   */
  public JSONObject addPenalty(String strRDVTrxLineID, String strMatchQty, String actionDate,
      String strAdvanceDeductionAmount, String strMatchAmt) throws RDVException;

}
