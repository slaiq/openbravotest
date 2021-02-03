package sa.elm.ob.scm.ad_process.mms;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.scm.webservice.approvedpo.CreatePoReceiptService;
import sa.elm.ob.scm.webservice.approvedpo.CreatePoReceiptServiceImpl;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dao.ResponseDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;

public class MmsCreatePoReceipt {
  private static final Logger log4j = LoggerFactory.getLogger(MmsCreatePoReceipt.class);

  public ResponseEntity<ResponseDTO> createPoReceipt(@RequestBody PoReceiptHeaderDTO poreceipt,
      VariablesSecureApp vars) throws Exception {

    CreatePoReceiptService createPoReceiptService = new CreatePoReceiptServiceImpl();

    ShipmentInOut poReceiptHeader = null;
    EfinRDV rdv = null;
    EfinRDVTransaction rdvTxn = null;
    String orderId = null;

    ObjectMapper mapper = new ObjectMapper();
    String inputRequest = mapper.writeValueAsString(poreceipt);
    log4j.debug("inputRequest:" + mapper.writeValueAsString(poreceipt));
    PoReceiptHeaderDTO originalRequest = mapper.readValue(inputRequest, PoReceiptHeaderDTO.class);

    ConnectionProvider conn = new DalConnectionProvider(false);

    ResponseDTO response = new ResponseDTO();

    try {

      // validate input request
      createPoReceiptService.validateInputRequest(poreceipt);

      // Set orderline id
      createPoReceiptService.setPOLineId(poreceipt);

      // Check whether PO is already approved and it is a latest version
      createPoReceiptService.checkApprovedPOandVersion(poreceipt.getOrderId());

      // Check whether RDV for PO with PO Receipt is already created and Transaction is draft
      createPoReceiptService.rdvInitialCheck(orderId);

      // Create PO receipt header
      poReceiptHeader = createPoReceiptService.createPOHeader(poreceipt);

      // Create PO receipt lines
      createPoReceiptService.createPOHeaderLines(poreceipt, poReceiptHeader);

      // Execute PO receipt complete process
      OBError completionResult = createPoReceiptService.checkIRCompleteProcess(poReceiptHeader,
          vars, conn);

      if (completionResult.getTitle().toLowerCase().equals(WebserviceConstants.SUCCESS)) {

        // Create RDV header
        rdv = createPoReceiptService.createRDVForPOReceipt(poReceiptHeader);

        // Create RDV transaction version
        rdvTxn = createPoReceiptService.createRDVTxn(poReceiptHeader, rdv);

        // Execute match all process
        completionResult = createPoReceiptService.matchAllProcessRDV(rdv, rdvTxn, vars, conn);
        if (completionResult.getType().toLowerCase().equals(WebserviceConstants.SUCCESS)) {

          // Adding bulk hold for RDV transaction
          createPoReceiptService.addBulkHold(poreceipt, rdv, rdvTxn);

          // Adding hold for RDV transaction line
          createPoReceiptService.insertHold(rdv, rdvTxn, poreceipt.getLineDTO());

          // penalty process
          createPoReceiptService.penaltyProcess(poreceipt, rdv, rdvTxn);

          // bulk penalty process
          createPoReceiptService.bulkPenaltyProcess(poreceipt, rdv, rdvTxn);

          // set Response
          response = createPoReceiptService.setResponse(poreceipt.getRequestNo(),
              WebserviceConstants.SUCCESS, poReceiptHeader.getDocumentNo(), rdv.getDocumentNo(),
              rdvTxn.getTXNVersion().toString(), null);

          return new ResponseEntity<ResponseDTO>(response, HttpStatus.OK);
        } else {
          OBDal.getInstance().rollbackAndClose();
          response = createPoReceiptService.setResponse(poreceipt.getRequestNo(),
              WebserviceConstants.ERROR, null, null, null, completionResult.getMessage());
          return new ResponseEntity<ResponseDTO>(response, HttpStatus.OK);
        }
      } else {
        OBDal.getInstance().rollbackAndClose();
        response = createPoReceiptService.setResponse(poreceipt.getRequestNo(),
            WebserviceConstants.ERROR, null, null, null, completionResult.getMessage());
        return new ResponseEntity<ResponseDTO>(response, HttpStatus.OK);
      }
    } catch (CreateReceiptException e) {
      OBDal.getInstance().rollbackAndClose();
      response = createPoReceiptService.setResponse(poreceipt.getRequestNo(),
          WebserviceConstants.ERROR, null, null, null, e.getMessage());
      return new ResponseEntity<ResponseDTO>(response, HttpStatus.OK);
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      response = createPoReceiptService.setResponse(poreceipt.getRequestNo(),
          WebserviceConstants.ERROR, null, null, null, e.getMessage());
      return new ResponseEntity<ResponseDTO>(response, HttpStatus.OK);
    } finally {
      createPoReceiptService.storeRequestAndResponse(originalRequest, response);
    }

  }

}
