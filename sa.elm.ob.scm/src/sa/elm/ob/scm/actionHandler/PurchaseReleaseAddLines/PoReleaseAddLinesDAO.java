package sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

import sa.elm.ob.scm.EscmOrderlineV;

public interface PoReleaseAddLinesDAO {

  /**
   * Method to Insert the selected Agreement Lines
   * 
   * @param agreementLineId
   * @param releaseQty
   * @param releaseHdr
   * @param selectedRow
   * @param roundoffConst
   * @return
   */
  public int insertAgreementLines(String agreementLineId, BigDecimal releaseQty, Order releaseHdr,
      JSONObject selectedRow, Integer roundoffConst);

  /**
   * Method to delete the Line selected if the Line is already inserted and the quantity is zero
   * 
   * @param agreementLineId
   * @param releaseHdr
   * @return
   */
  public int deleteAgreementLine(String agreementLineId, Order releaseHdr);

  /**
   * Method to Insert/update the selected Agreement Lines - Amount based
   * 
   * @param agreementLineId
   * @param releaseQty
   * @param releaseHdr
   * @param selectedRow
   * @return
   */
  public int insertAgreementLinesAmt(String agreementLineId, BigDecimal releaseAmt,
      Order releaseHdr, JSONObject selectedRow);

  /**
   * Method to delete the Line selected if the Line is already inserted and the quantity is zero
   * 
   * @param agreementLineId
   * @param releaseHdr
   * @return
   */
  public int deleteAgreementLineAmt(String agreementLineId, Order releaseHdr);

  /**
   * Method to calculate tax - Quantity based
   * 
   * @param releaseHdr
   * @param selectedAgreementLine
   * @param roundoffConst
   * @param selectedLineReleasedQty
   * @return
   */
  public JSONObject calculateTaxAmount(Order releaseHdr, OrderLine selectedAgreementLine,
      int roundoffConst, BigDecimal selectedLineReleasedQty);

  /**
   * Method to calculate tax - Amount based
   * 
   * @param releaseHdr
   * @param selectedAgreementLine
   * @param selectedLineReleasedAmt
   * @return
   */
  public JSONObject calculateTaxAmtBased(Order releaseHdr, OrderLine selectedAgreementLine,
      BigDecimal selectedLineReleasedAmt);

  /**
   * Method to get next Line number
   * 
   * @param releaseHdr
   * @return
   */
  public long getLineNumber(Order releaseHdr);

  /**
   * Method to insert the selected agreement records
   * 
   * @param agreementLineId
   * @param releaseHdr
   * @param taxObject
   * @param isSelectedLine
   * @param selectedLinesList
   * @return
   */
  public int insertChildLines(String agreementLineId, Order releaseHdr, JSONObject taxObject,
      boolean isSelectedLine, List<String> selectedLinesList);

  /**
   * Method to insert the Parent and child Records
   * 
   * @param agreementLine
   * @param releaseHdr
   * @param taxObject
   * @param isSelectedLine
   * @return
   */
  public OrderLine insertParentAndChild(OrderLine agreementLine, Order releaseHdr,
      JSONObject taxObject, boolean isSelectedLine);

  /**
   * Method to get the parentLine Id of Release record based on the agreement Parent Line
   * 
   * @param agreementLine
   * @param releaseHdr
   * @return
   */
  public EscmOrderlineV getPoReleaseParentLine(OrderLine agreementLine, Order releaseHdr);

}
