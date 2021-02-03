package sa.elm.ob.scm.ad_process.ProposalAttribute.Tax;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.erpCommon.utility.OBError;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

public interface TechnicalTaxCalculationDAO {

  /**
   * Method to calculate the tax percentage
   * 
   * @param proposalMgmt
   * 
   * @return tax percentage.
   */
  public BigDecimal getTaxPercent(EscmProposalMgmt proposalMgmt);

  /**
   * Checks if the proposal tax calculation is inclusive of tax
   * 
   * @param proposalMgmt
   * 
   * @return
   */
  public Boolean isPriceInclusiveOfTax(EscmProposalMgmt proposalMgmt);

  /**
   * Method to get the proposal lines
   * 
   * @param proposalMgmt
   * 
   * @return {@link List} of {@link ProposalLine} objects
   */
  public List<EscmProposalmgmtLine> getProposalLines(EscmProposalMgmt proposalMgmt);

  /**
   * Method to insert the calculated tax.
   * 
   * @param proposalMgmt
   * @param decimalFormat
   * 
   * @return {@link OBError}
   */
  public OBError insertTaxAmount(EscmProposalAttribute proposalAttr, EscmProposalMgmt proposalMgmt,
      Integer decimalFormat);

  /**
   * Method to calculated tax
   * 
   * @param proposalLine
   * 
   * @return
   */
  public JSONObject calculateTax(EscmProposalmgmtLine proposalLine);

  /**
   * 
   * @param proposalMgmt
   * @return
   */

  public BigDecimal getTaxPercentFromProposalAttr(EscmProposalMgmt proposalMgmt);

}
