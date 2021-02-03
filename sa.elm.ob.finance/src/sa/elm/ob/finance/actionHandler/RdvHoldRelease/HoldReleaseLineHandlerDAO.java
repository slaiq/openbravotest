package sa.elm.ob.finance.actionHandler.RdvHoldRelease;

import java.math.BigDecimal;

import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldHeader;

public interface HoldReleaseLineHandlerDAO {

  /**
   * Method to insert the holdLines in rdvHoldAction table
   * 
   * @param holdAmt
   * @param holdLineId
   * @param newTxnLineId
   * @param lineNo
   * @param type
   * @param txnId
   * @return
   */
  public int insertHoldLines(BigDecimal holdAmt, String holdLineId, String newTxnLineId,
      long lineNo, String type, String txnId);

  /**
   * Method to insert holdLines in rdvHoldHeader
   * 
   * @param action
   * @param efinRdvtxnline
   * @param rdvHoldAmount
   */
  public void insertHoldReleaseHeader(EfinRdvHoldAction action, EfinRDVTxnline efinRdvtxnline,
      BigDecimal rdvHoldAmount);

  /**
   * Method to get hold amount on each hold types based on RDV header and hold type
   * 
   * @param trxline
   * @param actiontype
   * @param penaltyaction
   * @return
   */
  public JSONObject getHoldAmt(EfinRDVTxnline trxline, String actiontype,
      EfinRdvHoldAction holdaction);

  /**
   * Method to update the RDVHold Applied
   * 
   * @param result
   * @param holdaction
   */
  public void updateHold(JSONObject result, EfinRdvHoldAction holdaction);

  /**
   * Method to get previous RDV transaction based on current RDV transaction line not including
   * current version id
   * 
   * @param rdvtrxln
   * @return
   */
  public EfinRDVTransaction getPrevRdvTransaction(EfinRDVTxnline rdvtrxln);

  /**
   * Method to get previous RDV transaction line based on previous transaction version and current
   * RDV line product id
   * 
   * @param rdvtrxId
   * @param rdvtrxln
   * @return
   */
  public EfinRDVTxnline getPrevRdvTransactionLine(String rdvtrxId, EfinRDVTxnline rdvtrxln);

  /**
   * Method to get the hold header previous hold amount values based on previous RDV transaction and
   * RDV transaction line
   * 
   * @param previousrdvtrx
   * @param rdvtrxln
   * @return
   */
  public BigDecimal getHeaderPrevHoldAmount(EfinRDVTransaction previousrdvtrx,
      EfinRDVTxnline rdvtrxln);

  /**
   * Method to check if hold header exists or not based on current RDV transaction line and RDV
   * transaction id
   * 
   * @param rdvtrxln
   * @return
   */
  public EfinRdvHoldHeader getrdvHoldheader(EfinRDVTxnline rdvtrxln);
}
