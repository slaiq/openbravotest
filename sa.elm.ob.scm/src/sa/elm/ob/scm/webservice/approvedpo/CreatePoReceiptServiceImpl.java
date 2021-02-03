package sa.elm.ob.scm.webservice.approvedpo;

import java.util.List;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.springframework.stereotype.Service;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dao.CreatePoReceiptDAO;
import sa.elm.ob.scm.webservice.dao.CreateRDVHoldDAO;
import sa.elm.ob.scm.webservice.dao.CreateRdvDAO;
import sa.elm.ob.scm.webservice.dao.PenaltyProcessDAO;
import sa.elm.ob.scm.webservice.dao.ResponseDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLineDTO;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;

/**
 * This class is implementation class for CreatePoReceiptService
 * 
 * @author Sathishkumar.P
 *
 */

@Service
public class CreatePoReceiptServiceImpl implements CreatePoReceiptService {

  @Override
  public void intializeOBContext(String clientId) {
    OBContext.setOBContext(WebserviceConstants.DEFAULT_USER_ID, WebserviceConstants.DEFAULT_ROLE_ID,
        clientId, WebserviceConstants.DEFAULT_ORG_ID);
  }

  @Override
  public VariablesSecureApp intializeVars(String clientId) {
    return new VariablesSecureApp(WebserviceConstants.DEFAULT_USER_ID, clientId,
        WebserviceConstants.DEFAULT_ORG_ID, WebserviceConstants.DEFAULT_ROLE_ID);
  }

  public void setPOLineId(PoReceiptHeaderDTO order) throws Exception {
    SetPOLineIdDAO.setPOLineId(order);
  }

  @Override
  public ShipmentInOut createPOHeader(PoReceiptHeaderDTO order) throws Exception {
    return CreatePoReceiptDAO.insertPOReceiptHeader(order);
  }

  @Override
  public void createPOHeaderLines(PoReceiptHeaderDTO order, ShipmentInOut header) throws Exception {
    CreatePoReceiptDAO.createPoReceiptLines(order, header);
  }

  @Override
  public OBError checkIRCompleteProcess(ShipmentInOut poreceipt, VariablesSecureApp vars,
      ConnectionProvider conn) throws Exception {
    return CreatePoReceiptDAO.checkIRCompleteProcess(poreceipt, vars, conn);
  }

  @Override
  public EfinRDV createRDVForPOReceipt(ShipmentInOut receipt) throws Exception {
    return CreateRdvDAO.createRDVForPOReceipt(receipt);
  }

  @Override
  public EfinRDVTransaction createRDVTxn(ShipmentInOut inout, EfinRDV rdv) throws Exception {
    return CreateRdvDAO.createRDVTxn(inout, rdv);
  }

  @Override
  public void checkReactivateProcess(ShipmentInOut poreceipt, VariablesSecureApp vars)
      throws Exception {
    CreatePoReceiptDAO.reactivatePoReceipt(poreceipt, vars);
    OBDal.getInstance().flush();

  }

  @Override
  public void deletePOHeaderLines(ShipmentInOut header, VariablesSecureApp vars) throws Exception {
    CreatePoReceiptDAO.deleteLines(header, vars);
  }

  @Override
  public void deletePOHeader(ShipmentInOut header) throws Exception {
    CreatePoReceiptDAO.deleteHeader(header);
  }

  @Override
  public OBError matchAllProcessRDV(EfinRDV rdv, EfinRDVTransaction rdvTxn, VariablesSecureApp vars,
      ConnectionProvider conn) throws Exception {
    return CreatePoReceiptDAO.matchAll(rdv, rdvTxn, vars, conn);
  }

  @Override
  public ResponseDTO setResponse(String responseNo, String type, String receiptNo, String rdvNo,
      String versionNo, String msg) {
    ResponseDTO response = new ResponseDTO();
    response.setResponseNo(responseNo);
    response.setStatus(type);
    response.setPoReceiptNo(receiptNo);
    response.setRdvNo(rdvNo);
    response.setRdvTrnNo(versionNo);
    response.setErrorMsg(msg);
    return response;
  }

  @Override
  public void rdvInitialCheck(String orderId) throws Exception {
    CreateRdvDAO.rdvInitialCheck(orderId);
  }

  @Override
  public void checkApprovedPOandVersion(String orderId) throws Exception {
    CreateRdvDAO.checkApprovedPOandVersion(orderId);
  }

  @Override
  public void insertHold(EfinRDV rdv, EfinRDVTransaction rdvVersion, List<PoReceiptLineDTO> lineDTO)
      throws Exception {
    CreateRDVHoldDAO.insertHold(rdv, rdvVersion, lineDTO);
  }

  @Override
  public void penaltyProcess(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws Exception {
    PenaltyProcessDAO.penaltyProcess(order, rdv, rdvTxn);
  }

  @Override
  public void bulkPenaltyProcess(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws Exception {
    PenaltyProcessDAO.bulkPenaltyProcess(order, rdv, rdvTxn);
  }

  @Override
  public void addBulkHold(PoReceiptHeaderDTO order, EfinRDV rdv, EfinRDVTransaction rdvTxn)
      throws Exception {
    CreateRDVHoldDAO.addBulkHold(order, rdv, rdvTxn);
  }

  @Override
  public String getLegacyOrderId(String legacyContractNo) throws Exception {
    return CreatePoReceiptDAO.getLegacyOrderId(legacyContractNo);
  }

  @Override
  public List getOrderIdbyContractNo(String legacyContractNo) throws Exception {
    return CreatePoReceiptDAO.getOrderIdbyContractNo(legacyContractNo);
  }

  @Override
  public void validateInputRequest(PoReceiptHeaderDTO request) throws Exception {
    CreatePoReceiptDAO.validateInputRequest(request);
  }

  @Override
  public void storeRequestAndResponse(PoReceiptHeaderDTO request, ResponseDTO response)
      throws CreateReceiptException, Exception {
    CreatePoReceiptDAO.storeRequestAndResponse(request, response);
  }

}
