package sa.elm.ob.utility.dms.notifyuser.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import sa.elm.ob.utility.dms.consumer.dto.DMSNotifyRequestDTO;
import sa.elm.ob.utility.dms.consumer.dto.DMSNotifyResponseDTO;
import sa.elm.ob.utility.dms.notifyuser.exceptions.DmsNotifyUserException;
import sa.elm.ob.utility.dms.notifyuser.service.DmsNotifyUserService;

/**
 * 
 * @author Gopalakrishnan
 * 
 *         Web Controller for DMS notify User
 *
 */
@RestController
@RequestMapping("openerp/dms")
public class DmsNotifyUserController {
  @Autowired
  private DmsNotifyUserService dmsNotfiyUserService;

  @RequestMapping(value = "/notifyuser", method = RequestMethod.POST)
  public ResponseEntity<DMSNotifyResponseDTO> notifyUserService(
      @RequestBody DMSNotifyRequestDTO notifyDTO) throws DmsNotifyUserException {
    DMSNotifyResponseDTO response = new DMSNotifyResponseDTO();
    dmsNotfiyUserService.updateDMSIntegrationLog(notifyDTO);
    Boolean isNotified = dmsNotfiyUserService
        .insertAlertNotificationForUser(notifyDTO.getGrpProcessId());
    response.setSuccess(isNotified);
    return new ResponseEntity<DMSNotifyResponseDTO>(response, HttpStatus.OK);
  }

}
