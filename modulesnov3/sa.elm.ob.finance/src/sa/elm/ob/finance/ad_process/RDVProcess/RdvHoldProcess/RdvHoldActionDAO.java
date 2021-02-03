package sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldTypes;

public interface RdvHoldActionDAO {
  /**
   * 
   * @param clientId
   * @param inpRDVTxnLineId
   * @param searchAttr
   * @return HoldDetails List
   */
  JSONObject getHoldList(String clientId, String inpRDVTxnLineId, JSONObject searchAttr);

  /**
   * 
   * @param clientId
   * @param inpRDVTxnLineId
   * @param searchAttr
   * @return HoldDetails List
   */
  JSONObject getHoldTxnList(String clientId, String inpRDVTxnLineId, JSONObject searchAttr);

  /**
   * 
   * @param clientId
   * @param contractId
   * @param searchFlag
   * @param vo
   * @return HoldListDetailsCount
   */
  int getHoldListCount(String clientId, String inpRDVTxnLineId);

  /**
   * 
   * @param clientId
   * @param employeeId
   * @param vo
   * @param limit
   * @param offset
   * @param sortColName
   * @param sortColType
   * @param searchFlag
   * @return ContractList
   */
  // List<PenaltyActionVO> getHoldList(String clientId, String inpRDVTxnLineId, PenaltyActionVO vo,
  // int limit, int offset, String sortColName, String sortColType, String searchFlag);

  /**
   * 
   * @param clientId
   * @param rdvtrxlineId
   * @return
   */
  List<RdvHoldActionVO> getbpartnername(String clientId, String rdvtrxlineId);

  /**
   * 
   * @param clientId
   * @param action
   * @param lang
   * @return
   */
  List<RdvHoldActionVO> getHoldType(String clientId, String action, String lang);

  /**
   * 
   * @param clientId
   * @param rdvtrxlineId
   * @return
   */
  List<RdvHoldActionVO> getBudgetAdjustmentUniquecode(String clientId, String rdvtrxlineId);

  /**
   * 
   * @param clientId
   * @return
   */
  List<RdvHoldActionVO> getinvoiceno(String clientId);

  /**
   * 
   * @param seqno
   * @param trxappno
   * @param clientId
   * @param actiontype
   * @param actionDate
   * @param amount
   * @param penalty_type
   * @param penalty_per
   * @param penalty_amt
   * @param actreason
   * @param actionjus
   * @param bpartnerId
   * @param bpname
   * @param freezepenalty
   * @param invoice
   * @param amarsarfamount
   * @param RDVTrxlineId
   * @param penalty_account
   * @param uniquecode
   * @param isRdvSaveAction
   * @return
   */
  String getHoldAction(String seqno, String trxappno, String clientId, String actiontype,
      String actionDate, String amount, String penalty_type, String penalty_per, String penalty_amt,
      String actreason, String actionjus, String bpartnerId, String bpname, String freezepenalty,
      String invoice, String amarsarfamount, String RDVTrxlineId, String penalty_account,
      String uniquecode, Boolean isRdvSaveAction);

  /**
   * 
   * @param seqno
   * @param trxappno
   * @param clientId
   * @param actiontype
   * @param actionDate
   * @param amount
   * @param penalty_type
   * @param penalty_per
   * @param penalty_amt
   * @param actreason
   * @param actionjus
   * @param bpartnerId
   * @param bpname
   * @param freezepenalty
   * @param invoice
   * @param amarsarfamount
   * @param RDVTrxId
   * @param penalty_account
   * @param uniquecode
   * @param isRdvSaveAction
   * @return
   */
  String getHoldTxnAction(String seqno, String trxappno, String clientId, String actiontype,
      String actionDate, String amount, String penalty_type, String penalty_per, String penalty_amt,
      String actreason, String actionjus, String bpartnerId, String bpname, String freezepenalty,
      String invoice, String amarsarfamount, String RDVTxnId, String penalty_account,
      String uniquecode, Boolean isRdvSaveAction);

  /**
   * 
   * @param pentlaction
   * @param oldpenaltyTypeId
   * @param penaltyamt
   */
  void updateHold(EfinRdvHoldAction holdaction, EfinRdvHoldTypes oldholdTypeId,
      BigDecimal penaltyamt);

  /**
   * 
   * @param pentlaction
   * @param oldpenaltyTypeId
   * @param penaltyamt
   */
  void updateoldPenalty(EfinRdvHoldAction pentlaction, EfinRdvHoldTypes oldholdTypeId,
      BigDecimal holdamt);

  /**
   * 
   * @param hijridate
   * @return
   */
  Date convertGregorian(String hijridate);

  /**
   * Method to insert hold if hold does not exists based on RDV header
   * 
   * @param holdaction
   * @param holdType
   * @param holdApplied
   */
  public void insertRdvHold(EfinRdvHoldAction holdaction, EfinRdvHoldTypes holdType,
      BigDecimal holdApplied);

  /**
   * Method to delete Hold if the hold type is not used in other lines
   * 
   * @param oldholdId
   * @param holdType
   * @param holdaction
   */
  public void deleteRdvHold(String oldholdId, EfinRdvHoldTypes holdType,
      EfinRdvHoldAction holdaction);

  /**
   * insert hold header if hold header does not exists (based on RDV transaction line ) insert a
   * hold header otherwise update hold amount and update penalty amount
   * 
   * @param objLine
   * @param efinRdvtxnline
   * @param diffpenltyAmt
   * @param oldholdTypeId
   * @param oldAction
   */
  void insertHoldHeader(EfinRdvHoldAction objLine, EfinRDVTxnline efinRdvtxnline,
      BigDecimal diffpenltyAmt, EfinRdvHoldTypes oldholdTypeId, String oldAction);

  /**
   * Method to delete the hold header
   * 
   * @param holdaction
   */
  void deleteHoldHed(EfinRdvHoldAction holdaction);
}
