package sa.elm.ob.scm.actionHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;

import com.fasterxml.jackson.databind.ObjectMapper;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCM_Certificates;
import sa.elm.ob.utility.tabadul.Branch;
import sa.elm.ob.utility.tabadul.CountriesVO;
import sa.elm.ob.utility.tabadul.CountryLookupResponse;
import sa.elm.ob.utility.tabadul.CountryVO;
import sa.elm.ob.utility.tabadul.SupplierVO;
import sa.elm.ob.utility.tabadul.TabadulAuthenticationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulConstants;
import sa.elm.ob.utility.tabadul.TabadulIntegrationServiceImpl;
import sa.elm.ob.utility.tabadul.TabadulReason;
import sa.elm.ob.utility.tabadul.TabadulResponse;
import sa.elm.ob.utility.util.UtilityDAO;

public class SupplierController {

  private static final String DEFAULT_COUNTRY_ISO_CODE = "SA";

  private static final Logger log = Logger.getLogger(SupplierController.class);

  private static final Object DEFAULT_CURRENCY_ISO_CODE = "SAR";

  public BusinessPartner importSupplierByCRN(String supplierCRN)
      throws TabadulIntegrationException, Exception {

    BusinessPartner businessPartner = null;
    log.info("importSupplierByCRN Started");
    try {

      OBContext.setAdminMode();
      ObjectMapper mapper = new ObjectMapper();
      SupplierVO supplierVO = getSupplier(supplierCRN);
      log.info("Supplier imported from Tabadul");
      businessPartner = createSupplier(supplierCRN, supplierVO);

      OBContext.restorePreviousMode();

    } catch (Exception e) {
      OBContext.restorePreviousMode();
      log.error("Error While import Supplier", e);
      throw e;
    }

    return businessPartner;
  }

  // import supplier information only, without do any action on the system
  public SupplierVO importSupplier(String supplierCRN)
      throws TabadulIntegrationException, Exception {

    SupplierVO supplierVO = null;

    log.info("importSupplierByCRN Started");
    try {
      OBContext.setAdminMode();
      supplierVO = getSupplier(supplierCRN);
      log.info("Supplier imported from Tabadul");

      OBContext.restorePreviousMode();

    } catch (Exception e) {
      OBContext.restorePreviousMode();
      log.error("Error While import Supplier", e);
      throw e;
    }
    return supplierVO;
  }

  public BusinessPartner createSupplier(String supplierCRN, SupplierVO supplierVO)
      throws TabadulIntegrationException {
    if (supplierVO != null) {
      BusinessPartner businessPartner = new BusinessPartner();
      businessPartner.setEscmImported(true);
      businessPartner.setEfinDocumentno(supplierCRN);
      businessPartner.setVendor(true);
      businessPartner.setSearchKey(supplierCRN);
      String vendorTypeCode = supplierVO.getVendor_type_code();
      setBusinessPartnerCategory(businessPartner, vendorTypeCode);
      businessPartner.setEscmTabadulid(String.valueOf(supplierVO.getUid()));
      businessPartner.setActive(true);
      businessPartner.setCustomer(false);
      businessPartner.setEfinNationality(getDefaultCountry());
      businessPartner.setName(supplierVO.getC_name());
      businessPartner.setEscmBoardofmgmt(supplierVO.getBoard_of_management());
      // Map Owner details
      businessPartner.setESCMOwnerName(supplierVO.getOwner_name());
      businessPartner.setEscmOwnerid(supplierVO.getNat());
      businessPartner.setEscmActivaspercoc(supplierVO.getCr_activity());
      businessPartner.setCurrency(getDefaultCurrency());
      // Map Contact Details
      createContact(supplierVO, businessPartner);
      createCertificates(supplierVO, businessPartner);
      createAddresses(supplierVO, businessPartner);
      OBDal.getInstance().save(businessPartner);
      OBDal.getInstance().commitAndClose();
      return businessPartner;
    } else {
      throw new TabadulIntegrationException("EUT_TABADUL.ERROR.INTERNAL_ERROR");
    }

  }

  public BusinessPartner updateSupplier(BusinessPartner businessPartner) throws Exception {

    SupplierVO supplierVO = null;

    if (businessPartner != null) {
      supplierVO = importSupplier(businessPartner.getEfinDocumentno());
      if (supplierVO != null) {
        businessPartner.setEscmImported(true);
        String vendorTypeCode = supplierVO.getVendor_type_code();
        setBusinessPartnerCategory(businessPartner, vendorTypeCode);
        businessPartner.setEscmTabadulid(String.valueOf(supplierVO.getUid()));
        businessPartner.setName(supplierVO.getC_name());
        businessPartner.setEscmBoardofmgmt(supplierVO.getBoard_of_management());
        // Map Owner details
        businessPartner.setESCMOwnerName(supplierVO.getOwner_name());
        businessPartner.setEscmOwnerid(supplierVO.getNat());
        businessPartner.setEscmActivaspercoc(supplierVO.getCr_activity());
        // Map Contact Details
        createContact(supplierVO, businessPartner);
        createCertificates(supplierVO, businessPartner);
        createAddresses(supplierVO, businessPartner);
        OBDal.getInstance().save(businessPartner);
        OBDal.getInstance().commitAndClose();
      } else {
        throw new TabadulIntegrationException("EUT_TABADUL.ERROR.INTERNAL_ERROR");
      }
    }
    return businessPartner;
  }

  private Currency getDefaultCurrency() {
    OBQuery<Currency> query = OBDal.getInstance().createQuery(Currency.class, "iSOCode= :iSOCode");
    query.setNamedParameter("iSOCode", DEFAULT_CURRENCY_ISO_CODE);
    return query.uniqueResult();
  }

  @SuppressWarnings("unused")
  private void setNationality(BusinessPartner businessPartner, String supplierNationalityId)
      throws Exception {
    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    log.info("Start Tabadul Auth.");
    String sessionToken = tabadulAuthenticationService.authenticate();
    log.info("invok IntegrationService.getCountries.");
    CountryLookupResponse countryLookupResponse = tabadulIntegrationService
        .getCountries(sessionToken);
    if (countryLookupResponse.getStatus() == null) {
      CountriesVO countries = countryLookupResponse.getCountries();
      String nationalityName = getNationality(countries, supplierNationalityId);
      businessPartner.setEfinNationalityTxt(nationalityName);
    } else {
      throw new TabadulIntegrationException("EUT_TABADUL.ERROR.INTERNAL_ERROR");
    }
  }

  private String getNationality(CountriesVO countries, String supplierNationalityId) {
    String nationality = null;
    if (countries != null && StringUtils.isNotBlank(supplierNationalityId)) {
      if (countries.getCountries() != null) {
        List<CountryVO> list = countries.getCountries().getCountry();
        for (CountryVO countryVO : list) {
          if (supplierNationalityId.equals(countryVO.getId())) {
            nationality = countryVO.getNameAr();
            break;
          }
        }
      }
    }
    return nationality;
  }

  private void setBusinessPartnerCategory(BusinessPartner businessPartner, String vendorTypeCode) {
    if (StringUtils.isNotBlank(vendorTypeCode)) {
      Category category = getBusinessPartnerCategory(vendorTypeCode);
      if (category != null) {
        log.info("BusinessPartnerCategory found for name:" + vendorTypeCode);
        businessPartner.setBusinessPartnerCategory(category);
      }
    }
  }

  private Category getBusinessPartnerCategory(String vendorTypeCode) {

    OBQuery<Category> query = OBDal.getInstance().createQuery(Category.class,
        "searchKey = :vendorTypeCode");
    query.setNamedParameter("vendorTypeCode", vendorTypeCode);
    return query.uniqueResult();
  }

  public BusinessPartner getBusinessPartnerByCRN(String supplierCRN) {

    OBQuery<BusinessPartner> query = OBDal.getInstance().createQuery(BusinessPartner.class,
        "searchKey = :supplierCRN");
    query.setFilterOnActive(false);
    query.setNamedParameter("supplierCRN", supplierCRN);
    return query.uniqueResult();
  }

  private void createAddresses(SupplierVO supplierVO, BusinessPartner businessPartner) {
    Branch branch = supplierVO.getBranches().getBranch();
    Location location;

    if (branch != null) {
      log.info("There is a branch address defined for this supplier.");
      int locationListSize = businessPartner.getBusinessPartnerLocationList().size();
      if (locationListSize > 0) {
        location = businessPartner.getBusinessPartnerLocationList().get(locationListSize - 1);
        if (location.getName().equals("Default Address")) {
          location = new Location();
        }
      } else {
        location = new Location();
      }
      location.setPhone(branch.getPhone() + " Ext.: " + branch.getExt());
      location.setFax(branch.getFax() + " Ext.: " + branch.getFaxExt());
      location.setName(branch.getcType());
      location.setShipToAddress(true);
      location.setInvoiceToAddress(true);
      location.setActive(true);
      location.setName("Official Address");
      createLocationAddress(location, branch);
      businessPartner.getBusinessPartnerLocationList().add(location);
      location.setBusinessPartner(businessPartner);
    } else {
      log.info("There is not branch address defined for this supplier.");
    }
  }

  private void createLocationAddress(Location location, Branch branch) {

    OBContext.setAdminMode(true);
    org.openbravo.model.common.geography.Location locationAddress = new org.openbravo.model.common.geography.Location();
    locationAddress.setAddressLine1(branch.getLocation());
    locationAddress.setCountry(getDefaultCountry());
    locationAddress.setPostalCode(branch.getZipCode());
    locationAddress.setAddressLine2(branch.getPoBox());
    City city = getCityByName(branch.getCity());
    if (city != null) {
      locationAddress.setCity(city);
    }
    List<Location> businessPartnerLocationList = new ArrayList<>();
    businessPartnerLocationList.add(location);
    locationAddress.setBusinessPartnerLocationList(businessPartnerLocationList);
    location.setLocationAddress(locationAddress);
    OBDal.getInstance().save(locationAddress);
  }

  private City getCityByName(String cityName) {

    OBQuery<City> query = OBDal.getInstance().createQuery(City.class, "name= :name");
    query.setNamedParameter("name", cityName);
    return query.uniqueResult();
  }

  private Country getDefaultCountry() {

    OBQuery<Country> query = OBDal.getInstance().createQuery(Country.class,
        "iSOCountryCode= :iSOCountryCode");
    query.setNamedParameter("iSOCountryCode", DEFAULT_COUNTRY_ISO_CODE);
    return query.uniqueResult();
  }

  private SupplierVO getSupplier(String crNumber) throws Exception {

    SupplierVO supplierVO = null;
    TabadulIntegrationServiceImpl tabadulIntegrationService = new TabadulIntegrationServiceImpl();
    TabadulAuthenticationServiceImpl tabadulAuthenticationService = new TabadulAuthenticationServiceImpl();
    tabadulIntegrationService.setTabadulAuthenticationService(tabadulAuthenticationService);
    log.info("Start Tabadul Auth.");
    String sessionToken = tabadulAuthenticationService.authenticate();

    log.info("invok IntegrationService.getSupplierByCR.");
    TabadulResponse tabadulResponse = tabadulIntegrationService.getSupplierByCR(crNumber, true,
        sessionToken);
    if (tabadulResponse.getStatus() == null) {
      supplierVO = tabadulResponse.getSupplierVO();
    } else {
      TabadulReason reason = tabadulResponse.getReason();
      if (reason != null) {
        if (TabadulConstants.ErrorCode.INVALID_USER.getErrorCode()
            .equalsIgnoreCase(reason.getId())) {
          throw new TabadulIntegrationException("EUT_TABADUL.ERROR.GET_VENDOR.INVALID_CR");
        } else {
          throw new TabadulIntegrationException("EUT_TABADUL.ERROR.INTERNAL_ERROR");
        }
      } else {
        throw new TabadulIntegrationException("EUT_TABADUL.ERROR.INTERNAL_ERROR");
      }
    }
    return supplierVO;
  }

  private void createCertificates(SupplierVO supplierVO, BusinessPartner businessPartner) {

    if (businessPartner.getESCMCertificatesList().size() > 0) {
      for (ESCM_Certificates cert : businessPartner.getESCMCertificatesList()) {
        if (cert.getCertificateName() == getCertificateType("GOIS")) {
          createGOISCertificate(supplierVO, businessPartner, cert);
        } else if (cert.getCertificateName() == getCertificateType("CRN")) {
          createCRCertificate(supplierVO, businessPartner, cert);
        } else if (cert.getCertificateName() == getCertificateType("LO")) {
          createLOCertificate(supplierVO, businessPartner, cert);
        } else if (cert.getCertificateName() == getCertificateType("SAGIA")) {
          createSAGIACertificate(supplierVO, businessPartner, cert);
        }
      }
    } else {
      createCRCertificate(supplierVO, businessPartner, null);
      createLOCertificate(supplierVO, businessPartner, null);
      createGOISCertificate(supplierVO, businessPartner, null);
      createSAGIACertificate(supplierVO, businessPartner, null);
    }

  }

  private void createSAGIACertificate(SupplierVO supplierVO, BusinessPartner businessPartner,
      ESCM_Certificates cert) {
    if (StringUtils.isNotBlank(supplierVO.getLicenseID())) {
      ESCMDefLookupsTypeLn certificateType = getCertificateType("SAGIA");
      if (certificateType != null) {
        ESCM_Certificates sagiaCertificate;

        if (cert != null) {
          sagiaCertificate = cert;
        } else {
          sagiaCertificate = new ESCM_Certificates();
        }

        sagiaCertificate.setCertificateName(certificateType);
        sagiaCertificate.setSequence(10L);
        sagiaCertificate.setCertificateNumber(supplierVO.getLicenseID());
        sagiaCertificate.setCreatedfromtabadul(true);
        // TODO there is no registry date for SAGIA certificate
        sagiaCertificate.setRegistryDate(new Date());
        sagiaCertificate.setRegistryExpiryDate(new Date());

        if (cert == null) {
          sagiaCertificate.setBusinessPartner(businessPartner);
          businessPartner.getESCMCertificatesList().add(sagiaCertificate);
        }
      } else {
        log.error("Can't Find Certificate type for :" + "SAGIA");
      }
    } else {
      log.error("Can't Find Certificate Number for :" + "SAGIA");
    }
  }

  private void createGOISCertificate(SupplierVO supplierVO, BusinessPartner businessPartner,
      ESCM_Certificates cert) {
    if (StringUtils.isNotBlank(supplierVO.getGosiRegistrationID())) {
      ESCMDefLookupsTypeLn certificateType = getCertificateType("GOIS");
      if (certificateType != null) {
        ESCM_Certificates goisCertificate;

        if (cert != null) {
          goisCertificate = cert;
        } else {
          goisCertificate = new ESCM_Certificates();
        }

        goisCertificate.setSequence(20L);
        goisCertificate.setCertificateName(certificateType);
        goisCertificate.setCertificateNumber(supplierVO.getGosiRegistrationID());
        goisCertificate.setCreatedfromtabadul(true);
        // TODO there is no registry date for GOIS certificate
        goisCertificate.setRegistryDate(new Date());
        goisCertificate.setRegistryExpiryDate(new Date());

        if (cert == null) {
          goisCertificate.setBusinessPartner(businessPartner);
          businessPartner.getESCMCertificatesList().add(goisCertificate);
        }
      } else {
        log.error("Can't Find Certificate type for :" + "GOIS");
      }
    } else {
      log.error("Can't Find Certificate Number for :" + "GOIS");
    }
  }

  private void createLOCertificate(SupplierVO supplierVO, BusinessPartner businessPartner,
      ESCM_Certificates cert) {
    if (StringUtils.isNotBlank(supplierVO.getEstLaborOfficeId())) {
      ESCMDefLookupsTypeLn certificateType = getCertificateType("LO");
      if (certificateType != null) {
        ESCM_Certificates loCertificate;

        if (cert != null) {
          loCertificate = cert;
        } else {
          loCertificate = new ESCM_Certificates();
        }

        loCertificate.setSequence(30L);
        loCertificate.setCertificateName(certificateType);
        loCertificate.setCertificateNumber(
            supplierVO.getEstLaborOfficeId() + "/" + supplierVO.getEstSequenceNumber());
        // TODO there is no registry date for LO certificate
        loCertificate.setRegistryDate(new Date());
        loCertificate.setRegistryExpiryDate(new Date());
        loCertificate.setCreatedfromtabadul(true);
        if (cert == null) {
          loCertificate.setBusinessPartner(businessPartner);
          businessPartner.getESCMCertificatesList().add(loCertificate);
        }
      } else {
        log.error("Can't Find Certificate type for :" + "LO");
      }
    } else {
      log.error("Can't Find Certificate Number for :" + "LO");
    }
  }

  private void createCRCertificate(SupplierVO supplierVO, BusinessPartner businessPartner,
      ESCM_Certificates cert) {
    if (StringUtils.isNotBlank(supplierVO.getCr_number())) {
      ESCMDefLookupsTypeLn certificateType = getCertificateType("CRN");
      if (certificateType != null) {
        ESCM_Certificates crnCertificate;

        if (cert != null) {
          crnCertificate = cert;
        } else {
          crnCertificate = new ESCM_Certificates();
        }

        crnCertificate.setSequence(40L);
        crnCertificate.setCertificateName(certificateType);
        crnCertificate.setCertificateNumber(supplierVO.getCr_number());
        crnCertificate.setCreatedfromtabadul(true);
        Date registryDate = UtilityDAO.convertToGregorianDate(supplierVO.getC_create_hijri());
        if (registryDate != null) {
          crnCertificate.setRegistryDate(registryDate);
        }
        Date expiryDate = UtilityDAO.convertToGregorianDate(supplierVO.getC_expire_hijri());
        if (expiryDate != null) {
          crnCertificate.setRegistryExpiryDate(expiryDate);
          OBErrorBuilder.buildMessage(null, "success", "@Escm_Supplier_Imported_Success@");
        } else {
          crnCertificate.setRegistryExpiryDateMCI(supplierVO.getC_expire_hijri());
          OBErrorBuilder.buildMessage(null, "warning", "@Escm_Expire_Date_Incorrect_Warn@");
        }

        if (cert == null) {
          crnCertificate.setBusinessPartner(businessPartner);
          businessPartner.getESCMCertificatesList().add(crnCertificate);
        }
      } else {
        log.error("Can't Find Certificate type for :" + "CRN");
      }
    } else {
      log.error("Can't Find Certificate Number for :" + "CRN");
    }
  }

  private ESCMDefLookupsTypeLn getCertificateType(String searchKey) {

    OBQuery<ESCMDefLookupsTypeLn> query = OBDal.getInstance().createQuery(
        ESCMDefLookupsTypeLn.class,
        "searchKey= :key and escmDeflookupsType.reference= :lookupType");
    query.setNamedParameter("key", searchKey);
    query.setNamedParameter("lookupType", "CN");

    return query.uniqueResult();
  }

  private void createContact(SupplierVO supplierVO, BusinessPartner businessPartner) {

    User contact;
    boolean isNew = true;

    if (businessPartner.getADUserList().size() > 0) {
      contact = businessPartner.getADUserList().get(0);
      isNew = false;
    } else {
      contact = new User();
    }

    contact.setActive(false);
    contact.setFirstName(supplierVO.getFirst_name());
    contact.setLastName(supplierVO.getFamily_name());
    contact.setName(supplierVO.getFirst_name() + " " + supplierVO.getSecond_name() + " "
        + supplierVO.getFamily_name());
    contact.setPosition(supplierVO.getJob());
    contact.setAlternativePhone(supplierVO.getMobile());
    contact.setPhone(supplierVO.getPhone());
    contact.setEscmAuthnat(supplierVO.getUser_NAT());
    contact.setEscmAuthnationality(supplierVO.getNationality());

    if (isNew) {
      contact.setBusinessPartner(businessPartner);
      businessPartner.getADUserList().add(contact);
    }
  }

  /**
   * 
   * @param certificateNumberCR
   * @param id
   * @return map with businesspartner number and certificate number
   */

  public Map<String, String> isCertificateNumberExists(List<String> certificateNumberCR,
      String id) {

    Map<String, String> duplicateCerficateBpList = new HashMap<String, String>();

    if (certificateNumberCR != null) {
      OBQuery<ESCM_Certificates> certificate = OBDal.getInstance().createQuery(
          ESCM_Certificates.class,
          " as e where e.certificateNumber in (:certificateNumberCR)  and e.businessPartner.id != :id ");
      certificate.setNamedParameter("certificateNumberCR", certificateNumberCR);
      certificate.setNamedParameter("id", id);

      for (ESCM_Certificates cr : certificate.list()) {
        duplicateCerficateBpList.put(cr.getCertificateNumber(),
            cr.getBusinessPartner().getSearchKey());
      }
    }

    return duplicateCerficateBpList;
  }
}
