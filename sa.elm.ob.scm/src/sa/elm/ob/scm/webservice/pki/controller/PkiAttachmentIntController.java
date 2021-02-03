package sa.elm.ob.scm.webservice.pki.controller;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sa.elm.ob.scm.webservice.pki.dao.PkiDao;
import sa.elm.ob.scm.webservice.pki.dto.FileDataResponse;
import sa.elm.ob.scm.webservice.pki.dto.LicenseFileDto;
import sa.elm.ob.scm.webservice.pki.util.PkiFileUtility;

/**
 * 
 * @author Kazim PKI Integration
 */

@RestController
@RequestMapping("openerp/pki")
public class PkiAttachmentIntController {

  private static final Logger log4j = LoggerFactory.getLogger(PkiAttachmentIntController.class);

  @Autowired
  PkiFileUtility PkiFileUtility;

  @RequestMapping(value = "/file-data", method = RequestMethod.GET)
  public ResponseEntity<FileDataResponse> getFileEncodedBytes(@RequestParam String userId)
      throws Exception {

    log4j.info("PKI -> getFileEncodedBytes start");
    FileDataResponse response = new FileDataResponse();

    try {
      OBContext.setAdminMode();

      if (!validateData(userId, "PKI -> getFileEncodedBytes usedId is not found")) {
        return new ResponseEntity<FileDataResponse>(response, HttpStatus.BAD_REQUEST);
      }

      // get user signature
      User user = PkiDao.getUser(userId);
      if (user != null && user.getEutDigitalsignature() != null) {
        response.setSignatureBytes(
            PkiFileUtility.getUserEncodedBytes(user.getEutDigitalsignature().getId()));
      } else {
        log4j.error("PKI -> getFileEncodedBytes User record is not found");
        return new ResponseEntity<FileDataResponse>(response, HttpStatus.BAD_REQUEST);
      }

      // if license data bytes empty we fetch it the bytes and store
      // in variable to reuse
      if (StringUtils.isBlank(LicenseFileDto.getLicData())) {
        String licdata = PkiFileUtility.getLinceseEncodedByte();
        LicenseFileDto.setLicData(licdata);
        response.setLicenseBytes(licdata);
      } else {
        response.setLicenseBytes(LicenseFileDto.getLicData());
      }

    } catch (Exception exception) {
      log4j.error("PKI -> getFileEncodedBytes Exception :", exception);
      return new ResponseEntity<FileDataResponse>(response, HttpStatus.BAD_REQUEST);
    } finally {
      OBContext.restorePreviousMode();
    }
    log4j.info("PKI -> getFileEncodedBytes end");
    return new ResponseEntity<FileDataResponse>(response, HttpStatus.OK);

  }

  boolean validateData(String data, String logErrorMessage) {
    if (!StringUtils.isNotBlank(data)) {
      log4j.error(logErrorMessage);
      return false;
    }
    return true;
  }

}
