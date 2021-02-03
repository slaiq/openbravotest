package sa.elm.ob.hcm.selfservice.controller;

import java.io.IOException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import sa.elm.ob.hcm.EhcmDependents;
import sa.elm.ob.hcm.dto.profile.AddressInformationDTO;
import sa.elm.ob.hcm.dto.profile.DependentInformationDTO;
import sa.elm.ob.hcm.dto.profile.EmployeeAdditionalInformationDTO;
import sa.elm.ob.hcm.dto.profile.EmployeeProfileDTO;
import sa.elm.ob.hcm.dto.profile.PersonalInformationDTO;
import sa.elm.ob.hcm.selfservice.exceptions.BusinessException;
import sa.elm.ob.hcm.selfservice.exceptions.SystemException;
import sa.elm.ob.hcm.services.profile.EmployeeProfileService;
import sa.elm.ob.hcm.services.profile.EmployeeProfileUpdateService;

/**
 * Web Controller for Employee Profile
 * 
 * @author mrahim
 *
 */
@RestController
@RequestMapping("openerp/hr")
public class EmployeeProfileController {

  @Autowired
  private EmployeeProfileService employeeProfileService;

  @Autowired
  private EmployeeProfileUpdateService employeeProfileUpdateService;

  @RequestMapping(value = "/profile/{username}", method = RequestMethod.GET)
  public ResponseEntity<EmployeeProfileDTO> getEmployeeProfileByUser(
      @PathVariable("username") String username) {
    EmployeeProfileDTO employeeProfile = employeeProfileService.getEmployeeProfileByUser(username);

    return new ResponseEntity<EmployeeProfileDTO>(employeeProfile, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/dependents/{username}", method = RequestMethod.GET)
  public ResponseEntity<List<DependentInformationDTO>> getEmployeeDependents(
      @PathVariable("username") String username) {

    List<DependentInformationDTO> employeeDependents = employeeProfileService
        .getEmployeeDependents(username);

    return new ResponseEntity<List<DependentInformationDTO>>(employeeDependents, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/address/{username}", method = RequestMethod.GET)
  public ResponseEntity<AddressInformationDTO> getEmployeeAddress(
      @PathVariable("username") String username) {

    AddressInformationDTO employeeAddress = employeeProfileService.getEmployeeAddress(username);

    return new ResponseEntity<AddressInformationDTO>(employeeAddress, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/{username}", method = RequestMethod.POST)
  public ResponseEntity<Boolean> updateEmployeeProfile(@PathVariable String username,
      @RequestBody PersonalInformationDTO pesonalInformationDTO,
      @RequestBody AddressInformationDTO addressInformationDTO,
      @RequestBody List<DependentInformationDTO> dependentInformationDTO)
      throws BusinessException, SystemException {

    Boolean updateEmployeeProfile = employeeProfileUpdateService.updateEmployeeProfile(username,
        pesonalInformationDTO, addressInformationDTO, dependentInformationDTO);

    return new ResponseEntity<Boolean>(updateEmployeeProfile, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/dependent/{username}", method = RequestMethod.PUT)
  public ResponseEntity<Boolean> updateEmployeeDependent(@PathVariable String username,
      @RequestBody DependentInformationDTO dependentInformationDTO)
      throws BusinessException, SystemException {
    Boolean updateEmpDep = employeeProfileUpdateService.updateEmployeeDependent(username,
        dependentInformationDTO);
    return new ResponseEntity<Boolean>(updateEmpDep, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/address/{username}", method = RequestMethod.PUT)
  public ResponseEntity<Boolean> updateEmployeeAddress(@PathVariable String username,
      @RequestBody AddressInformationDTO addressInformationDTO)
      throws BusinessException, SystemException {
    Boolean updateEmpAddress = employeeProfileUpdateService.updateEmployeeAddress(username,
        addressInformationDTO);
    return new ResponseEntity<Boolean>(updateEmpAddress, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/personal/{username}", method = RequestMethod.PUT)
  public ResponseEntity<Boolean> updatePersonalInformation(@PathVariable String username,
      @RequestBody PersonalInformationDTO personalInformationDTO)
      throws BusinessException, SystemException {
    Boolean updateEmpPersonalInfo = employeeProfileUpdateService.updatePersonalInformation(username,
        personalInformationDTO);
    return new ResponseEntity<Boolean>(updateEmpPersonalInfo, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/dependent/{username}", method = RequestMethod.POST)
  public ResponseEntity<Boolean> addDependent(@PathVariable String username,
      @RequestBody DependentInformationDTO dependentInformationDTO)
      throws BusinessException, SystemException {
    Boolean addDependent = employeeProfileUpdateService.addDependent(username,
        dependentInformationDTO);
    return new ResponseEntity<Boolean>(addDependent, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/{username}/dependent/{dependentId}", method = RequestMethod.DELETE)
  public ResponseEntity<Boolean> removeDependent(@PathVariable("username") String username,
      @PathVariable("dependentId") String dependentId) throws BusinessException, SystemException {
    Boolean removeDependent = employeeProfileUpdateService.removeDependent(username, dependentId);
    return new ResponseEntity<Boolean>(removeDependent, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/contactInfo/{username}", method = RequestMethod.PUT)
  public ResponseEntity<Boolean> updateContactInformation(@PathVariable String username,
      @RequestBody EmployeeAdditionalInformationDTO empContactInfo)
      throws BusinessException, SystemException {
    Boolean empContact = employeeProfileUpdateService.updateContactInformation(username,
        empContactInfo);
    return new ResponseEntity<Boolean>(empContact, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/{username}/dependent/{dependentId}", method = RequestMethod.GET)
  public ResponseEntity<DependentInformationDTO> getDependentByNationalId(
      @PathVariable("username") String username, @PathVariable("dependentId") String dependentId)
      throws SystemException, BusinessException {

    EhcmDependents dependent = employeeProfileUpdateService.getDependentByNationalId(username,
        dependentId);

    DependentInformationDTO empDependent = new DependentInformationDTO();
    empDependent.setFirstNameEn(dependent.getFirstName());
    empDependent.setFatherNameEn(dependent.getFathername());
    empDependent.setGrandFatherNameEn(dependent.getGrandfather());
    empDependent.setFamilyNameEn(dependent.getFamily());
    empDependent.setDob(String.valueOf(dependent.getDob()));
    empDependent.setGender(dependent.getGender());
    empDependent.setNationalId(dependent.getNationalidentifier());

    return new ResponseEntity<DependentInformationDTO>(empDependent, HttpStatus.OK);
  }

  @RequestMapping(value = "/profile/photo/{username}", method = RequestMethod.PUT)
  public ResponseEntity<Boolean> updateProfilePhoto(@PathVariable String username,
      @RequestParam("avatar") MultipartFile photo) throws SystemException, BusinessException {

    Boolean updatePhoto = null;
    try {
      updatePhoto = employeeProfileUpdateService.updateProfilePhoto(username,
          Base64.encodeBase64(photo.getBytes()).toString());
    } catch (IOException e) {
    }

    return new ResponseEntity<Boolean>(updatePhoto, HttpStatus.OK);
  }

}
