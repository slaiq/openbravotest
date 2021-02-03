package sa.elm.ob.scm.webservice.approvedpo;

import java.util.List;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.scm.webservice.dao.ResponseDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLineDTO;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;

/**
 * This is service class to create po receipt
 * 
 * @author Sathishkumar.P
 *
 */

public interface CreatePoReceiptService {

  /**
   * This method is used to intialize OBCONTEXT
   * 
   */

  void intializeOBContext(String clientId);

  /**
   * This method is used to intialize Vars
   * 
   */

  VariablesSecureApp intializeVars(String clientId);

  /**
   * This method is used to create PO receipt header
   * 
   * @param order
   * @param orderId
   * @return ShipmentInOut
   */

  ShipmentInOut createPOHeader(PoReceiptHeaderDTO order) throws Exception;

  /**
   * This method is used to create PO receipt lines
   * 
   * @param order
   * @param header
   * @return {@link ShipmentInOut}
   */

  void createPOHeaderLines(PoReceiptHeaderDTO order, ShipmentInOut header) throws Exception;

  /**
   * This method is used to check complete process of PO receipt
   * 
   * @param poreceipt
   * @param vars
   * @param connection(ConnectionProvider)
   * @return true if it is completed else there is some error
   * @throws Exception
   */

  OBError checkIRCompleteProcess(ShipmentInOut poreceipt, VariablesSecureApp vars,
      ConnectionProvider conn) throws CreateReceiptException, Exception;

  /**
   * This method is used to create RDV if it is not already present for PO
   * 
   * if it is present then it will return already created RDV
   * 
   * @param receipt
   * @return {@link EfinRDV}
   */

  EfinRDV createRDVForPOReceipt(ShipmentInOut receipt) throws CreateReceiptException, Exception;

  /**
   * This method is used to create RDV Transaction
   * 
   * @param inout
   * @param rdv
   * @return {@link EfinRDVTransaction}
   * @throws Exception
   */

  EfinRDVTransaction createRDVTxn(ShipmentInOut inout, EfinRDV rdv)
      throws CreateReceiptException, Exception;

  /**
   * This process is used to do match all in rdv txn
   * 
   * @param rdv
   * @param rdvTxn
   * @param connection(ConnectionProvider)
   * @return
   * @throws Exception
   */

  OBError matchAllProcessRDV(EfinRDV rdv, EfinRDVTransaction rdvTxn, VariablesSecureApp vars,
      ConnectionProvider conn) throws CreateReceiptException, Exception;

  /**
   * This method is used to reactivate the po receipt created
   * 
   * @param poreceipt
   * @param vars
   * @return
   * @throws Exception
   */

  void checkReactivateProcess(ShipmentInOut poreceipt, VariablesSecureApp vars)
      throws CreateReceiptException, Exception;

  /**
   * This method is used to delete the lines created for new Poreceipt
   * 
   * @param order
   * @param header
   * @throws Exception
   */

  void deletePOHeaderLines(ShipmentInOut header, VariablesSecureApp vars)
      throws CreateReceiptException, Exception;

  /**
   * This method is used to delete Po receipt created
   * 
   * @param order
   * @param header
   * @throws Exception
   */

  void deletePOHeader(ShipmentInOut header) throws CreateReceiptException, Exception;

  /*
   * Check whether RDV for PO with PO Receipt is already created and Transaction Version has draft
   * records
   * 
   * @param orderId
   * 
   * @return OBError
   */
  public void rdvInitialCheck(String orderId) throws CreateReceiptException, Exception;

  /**
   * This method is used to set the response
   * 
   * @param type
   * @param receiptNo
   * @param rdvNo
   * @param versionNo
   * @param msg
   * @return ResponseDTO
   */

  ResponseDTO setResponse(String responseNo, String type, String receiptNo, String rdvNo,
      String versionNo, String msg);

  /*
   * Check whether PO is already approved and it is a latest version
   * 
   * @param orderId
   * 
   * @return OBError
   */
  public void checkApprovedPOandVersion(String orderId) throws CreateReceiptException, Exception;

  /**
   * This method is used to insert hold in rdv transaction version line
   * 
   * @param rdv
   * @param rdvVersion
   * @param line
   * @param holdDTO
   */

  public void insertHold(EfinRDV rdv, EfinRDVTransaction rdvVersion,
      List<PoReceiptLineDTO> linesDTO) throws CreateReceiptException, Exception;

  /**
   * This process is used to do penalty process in RDV
   * 
   * @param rdv
   * @param rdvTxn
   * @param orderId
   * @return
   * @throws Exception
   */
  public void penaltyProcess(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws CreateReceiptException, Exception;

  /**
   * This process is used to do bulk penalty process in RDV
   * 
   * @param rdv
   * @param rdvTxn
   * @param orderId
   * @return
   * @throws Exception
   */
  public void bulkPenaltyProcess(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws CreateReceiptException, Exception;

  public void addBulkHold(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws CreateReceiptException, Exception;

  /**
   * 
   * @param order
   * @throws Exception
   */
  public void setPOLineId(PoReceiptHeaderDTO order) throws CreateReceiptException, Exception;

  /**
   * This method is used to get order id using the legacy contract number
   * 
   * @param legacyContractNo
   * @return legacyOrderId
   * @throws Exception
   */
  public String getLegacyOrderId(String legacyContractNo) throws CreateReceiptException, Exception;

  /**
   * This method is used to get order id using the contract number
   * 
   * @param legacyContractNo
   * @return legacyOrderId
   * @throws Exception
   */
  public List getOrderIdbyContractNo(String contractNo) throws CreateReceiptException, Exception;

  /**
   * This method is used to validate all the data in input request send by user
   * 
   * @param request
   * @throws Exception
   */
  public void validateInputRequest(PoReceiptHeaderDTO request)
      throws CreateReceiptException, Exception;

  /**
   * This method is used to store request and response in backend
   * 
   * @param request
   * 
   */

  public void storeRequestAndResponse(PoReceiptHeaderDTO request, ResponseDTO response)
      throws CreateReceiptException, Exception;

}