package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.util.Date;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.finance.Efin_RDv_Types;

/**
 * 
 * @author Gopinagh.R
 *
 */

public interface AddDefaultPenaltyDAO {
  /**
   * Checks whether the current RDV Transaction line is applicable for adding penalty.
   * 
   * @param strRDVTrxLineID
   * @return
   */
  public Boolean isPenaltyApplicable(String strRDVTrxLineID);

  /**
   * Checks if the penalty is enabled for the particular document type in the {@link Efin_RDv_Types}
   * window.
   * 
   * @param strRDVTrxLineID
   * @return
   */
  public Boolean isPenaltyEnabled(String strRDVTrxLineID);

  /**
   * Returns the maximum received date for the RDV Line, if the RDV doesn't have PO Receipt
   * reference.
   * 
   * @param strRDVTrxLineID
   * @return
   */
  public Date getMaximumReceivedDate(String strRDVTrxLineID);

  /**
   * Get the delayed penalty object.
   * 
   * @return
   */
  public EfinPenaltyTypes getDelayedPenaltyType(String strClientID);

  /**
   * Adds a default penalty.
   * 
   * @param strRDVTrxLineID
   * @param penaltyTypes
   * @param matchQty
   * @return
   */
  public Boolean addPenalty(String strRDVTrxLineID, EfinPenaltyTypes penaltyTypes,
      BigDecimal matchQty, String actionDate, String strAdvanceDeductionAmount, String strMatchAmt)
      throws RDVException;

  /**
   * Get the sequence number for the penalty to be inserted.
   * 
   * @param strRDVTrxLineID
   * @return
   */
  public Long getSequenceNumber(String strRDVTrxLineID);

  /**
   * perform default Validations before saving the record.
   * 
   * @param strRDVTrxLineID
   * @param strMatchQty
   * @return
   */
  public JSONObject defaultValidations(String strRDVTrxLineID, String strMatchQty,
      String strMatchAmt);

  /**
   * 
   * Return default uniqueCode and related objects
   * 
   * @param strRDVTrxLineID
   * @param strClientId
   * @return
   */

  public JSONObject getDefaultUniqueCode(String strRDVTrxLineID, String strClientId);

  /**
   * Checks if the net match amount becomes negative after applying penalty.
   * 
   * @param strRDVTrxLineID
   * @return
   */
  public Boolean isTotalDeductionGreaterThanMatchAmount(String strRDVTrxLineID,
      String strPenaltyType);

  /**
   * get sum of match amount,sum of netmatch amount,transaction type , transaction version status
   * from rdv
   * 
   * @param strSelectedRecordsId
   * @return
   */
  public JSONObject getSelectedRecordsInformation(JSONArray strSelectedRecordsId);
}
