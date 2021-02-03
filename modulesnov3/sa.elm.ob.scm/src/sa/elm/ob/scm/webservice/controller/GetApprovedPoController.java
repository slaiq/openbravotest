package sa.elm.ob.scm.webservice.controller;

import java.util.List;
import java.util.Map;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sa.elm.ob.scm.webservice.approvedpo.CreatePoReceiptService;
import sa.elm.ob.scm.webservice.approvedpo.GetApprovedPoService;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.scm.webservice.dto.POHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoResponseNumberDTO;

/**
 * RestController class for getting all approved POs
 * 
 * @author Sathishkumar.P
 *
 */

@RestController
@RequestMapping("openerp/po")
public class GetApprovedPoController {

  @Autowired
  private GetApprovedPoService getApprovedPoService;

  @Autowired
  private CreatePoReceiptService createPoReceiptService;

  @RequestMapping(value = "/approvedpo", method = RequestMethod.GET)
  public ResponseEntity<PoResponseNumberDTO> getApprovedPO(
      @RequestParam Map<String, String> customQuery) throws Exception {

    PoResponseNumberDTO responseDTO = new PoResponseNumberDTO();

    String clientId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty(WebserviceConstants.CLIENT_KEY);

    if (clientId == null) {
      throw new Exception("Please configure client id in openbravo properties");
    }

    // Intialize OBcontext and vars
    createPoReceiptService.intializeOBContext(clientId);

    int startrow = 0, endrow = 0;
    try {
      if (!customQuery.isEmpty()) {
        if (customQuery.containsKey(WebserviceConstants.START_ROW)) {
          startrow = Integer.parseInt(customQuery.get(WebserviceConstants.START_ROW));
          if (startrow < 0) {
            startrow = 0;
          }
        }

        if (customQuery.containsKey(WebserviceConstants.END_ROW)) {
          endrow = Integer.parseInt(customQuery.get(WebserviceConstants.END_ROW));
          if (endrow < 0) {
            endrow = 0;
          }
          if (endrow >= startrow) {
            endrow = (Integer.parseInt(customQuery.get(WebserviceConstants.END_ROW)) - startrow)
                + 1;
          } else {
            endrow = 0;
          }

        }
      }
    } catch (Exception e) {
      throw new Exception(OBMessageUtils.messageBD("ESCM_Invalidurl"));
    }

    // Handling the request number
    if (!customQuery.isEmpty()) {
      if (customQuery.containsKey(WebserviceConstants.REQUEST_NO)) {
        responseDTO.setResponseNumber(customQuery.get(WebserviceConstants.REQUEST_NO));
      } else {
        responseDTO.setResponseNumber(null);
      }
    } else {
      responseDTO.setResponseNumber(null);
    }

    List<POHeaderDTO> approvedPOs = getApprovedPoService.getApprovedPO(startrow, endrow);
    responseDTO.setOrderDTO(approvedPOs);

    return new ResponseEntity<PoResponseNumberDTO>(responseDTO, HttpStatus.OK);
  }

  @RequestMapping(value = "/approvedpo/contractno/{no}", method = RequestMethod.GET)
  public ResponseEntity<PoResponseNumberDTO> getApprovedPOByContractNo(@PathVariable String no,
      @RequestParam Map<String, String> customQuery) {
    PoResponseNumberDTO responseDTO = new PoResponseNumberDTO();

    // Handling the request number
    if (!customQuery.isEmpty()) {
      if (customQuery.containsKey(WebserviceConstants.REQUEST_NO)) {
        responseDTO.setResponseNumber(customQuery.get(WebserviceConstants.REQUEST_NO));
      } else {
        responseDTO.setResponseNumber(null);
      }
    } else {
      responseDTO.setResponseNumber(null);
    }

    List<POHeaderDTO> approvedPOs = getApprovedPoService.getApprovedPOByContractNo(no);
    responseDTO.setOrderDTO(approvedPOs);

    return new ResponseEntity<PoResponseNumberDTO>(responseDTO, HttpStatus.OK);
  }

  @RequestMapping(value = "/approvedpo/contractdate/{date}", method = RequestMethod.GET)
  public ResponseEntity<List<POHeaderDTO>> getApprovedPOByContractDate(@PathVariable String date) {

    List<POHeaderDTO> approvedPOs = getApprovedPoService.getApprovedPOByContractDate(date);

    return new ResponseEntity<List<POHeaderDTO>>(approvedPOs, HttpStatus.OK);
  }

  @RequestMapping(value = "/approvedpo/contracttype/{type}", method = RequestMethod.GET)
  public ResponseEntity<List<POHeaderDTO>> getApprovedPOByContractType(@PathVariable String type) {

    List<POHeaderDTO> approvedPOs = getApprovedPoService.getApprovedPOByContractType(type);

    return new ResponseEntity<List<POHeaderDTO>>(approvedPOs, HttpStatus.OK);
  }

}
