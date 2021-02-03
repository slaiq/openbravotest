package sa.elm.ob.scm.webservice.vehiclesystem.controller;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.webservice.vehiclesystem.constant.VehicleSystemConstants;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.CustodyTagDetailsIntResponse;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.GenericResponse;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.ProductDetailsIntRequest;
import sa.elm.ob.scm.webservice.vehiclesystem.service.VehicleService;
import sa.elm.ob.scm.webservice.vehiclesystem.util.StringValidationUtil;

/**
 * 
 * @author Kazim
 *
 */

@RestController
@RequestMapping("openerp/vehicle")
public class VehicleSystemIntController {
  private static final Logger log4j = LoggerFactory.getLogger(VehicleSystemIntController.class);

  @Autowired
  private VehicleService vehicleService;

  @RequestMapping(value = "/alltagdetails/{employeeCode}", method = RequestMethod.GET)
  public ResponseEntity<GenericResponse> getVehicleDetails(@PathVariable String employeeCode)
      throws Exception {

    GenericResponse<List<CustodyTagDetailsIntResponse>> response = new GenericResponse<List<CustodyTagDetailsIntResponse>>();

    try {
      OBContext.setAdminMode();
      String employeeId = StringValidationUtil.getEmployeeDetails(employeeCode);
      if (StringUtils.isNotBlank(employeeId)) {
        response.setStatus(VehicleSystemConstants.SUCCESS);
        response.setData(vehicleService.getDetailsByEmployeeCode(employeeId));
      } else {
        response.setErrorMessage("Employee not exists in the system");
      }

    } catch (Exception exception) {
      response.setErrorMessage("Internal Excpetion Occurred");
      log4j.error("getVehicleDetails Exception ", exception);
    } finally {
      OBContext.restorePreviousMode();
    }

    return new ResponseEntity<GenericResponse>(response, HttpStatus.OK);
  }

  @RequestMapping(value = "/tagdetails/{employeeCode}/{tagCode}", method = RequestMethod.GET)
  public ResponseEntity<GenericResponse> getVehicleDetailsByEmployeeAndTagCode(
      @PathVariable String employeeCode, @PathVariable String tagCode) throws Exception {

    GenericResponse<CustodyTagDetailsIntResponse> response = new GenericResponse<>();

    try {
      OBContext.setAdminMode();
      String employeeId = StringValidationUtil.getEmployeeDetails(employeeCode);

      // validate employee code
      if (StringUtils.isBlank(employeeId)) {
        response.setErrorMessage("Employee code validation failed");
      }
      // validate tag code
      if (!StringValidationUtil.notNullAndEmpty(tagCode)) {
        response.setErrorMessage("Tag code validation failed");
      }

      if (response.getErrorMessage() == null) {
        response.setStatus(VehicleSystemConstants.SUCCESS);
        response.setData(vehicleService.getDetailsByEmployeeCodeAndTagCode(employeeId, tagCode));
      }

    } catch (Exception exception) {
      response.setErrorAndExceptionMessage("Internal Excpetion Occurred", exception.getMessage());
      log4j.error("getVehicleDetailsByEmployeeAndTagCode Exception ", exception);
    } finally {
      OBContext.restorePreviousMode();
    }

    return new ResponseEntity<GenericResponse>(response, HttpStatus.OK);
  }

  @RequestMapping(value = "/custody", method = RequestMethod.POST)
  public ResponseEntity<GenericResponse> updateCustody(
      @RequestBody ProductDetailsIntRequest request) throws Exception {

    GenericResponse<CustodyTagDetailsIntResponse> response = new GenericResponse();
    try {

      OBContext.setAdminMode();
      // validate tag number

      if (request.getTagNo() == null) {
        response.setErrorMessage("Product Tag No should not be empty");
      } else {
        MaterialIssueRequestCustody objCustody = StringValidationUtil
            .findCustodyDetails(request.getTagNo(), request.getCurrentBeneficiaryIDName());
        if (objCustody == null) {
          response.setErrorMessage("Product-Tag are not under the employee");
        }
      }

      // validate current beneficiary id-name
      if (!StringValidationUtil.notNullAndEmpty(request.getCurrentBeneficiaryIDName())) {
        response.setErrorMessage("Current beneficiary id validation failed");
      }

      if (response.getErrorMessage() == null) {
        response.setStatus(VehicleSystemConstants.SUCCESS);
        vehicleService.updateCustodyData(request);

      }

    } catch (Exception exception) {
      response.setErrorAndExceptionMessage("Internal Excpetion Occurred", exception.getMessage());
      log4j.error("updateCustody Exception ", exception);

    } finally {
      OBContext.restorePreviousMode();
    }
    return new ResponseEntity<GenericResponse>(response, HttpStatus.OK);

  }

}
