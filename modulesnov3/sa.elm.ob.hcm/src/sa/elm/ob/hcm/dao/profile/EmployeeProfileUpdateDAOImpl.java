package sa.elm.ob.hcm.dao.profile;

import java.text.ParseException;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.City;
import org.openbravo.model.common.geography.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import sa.elm.ob.hcm.EHCMEmpAddress;
import sa.elm.ob.hcm.EhcmDependents;
import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.EhcmTitletype;
import sa.elm.ob.hcm.dto.profile.AddressInformationDTO;
import sa.elm.ob.hcm.dto.profile.DependentInformationDTO;
import sa.elm.ob.hcm.dto.profile.EmployeeAdditionalInformationDTO;
import sa.elm.ob.hcm.dto.profile.PersonalInformationDTO;
import sa.elm.ob.hcm.selfservice.exceptions.BusinessException;
import sa.elm.ob.hcm.selfservice.exceptions.SystemException;
import sa.elm.ob.hcm.util.MessageKeys;
import sa.elm.ob.utility.util.DateUtils;

/**
 * 
 * 
 * @author gopalakrishnan
 *
 */
@Repository
public class EmployeeProfileUpdateDAOImpl implements EmployeeProfileUpdateDAO {
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(EmployeeProfileDAOImpl.class);
  private static final String OPEN_BRAVO_DATE_FORMAT = "dd-MM-yyyy";
  private EmployeeProfileDAO employeeProfileDAO;

  /**
   * find Title by Title Name
   * 
   * @param titleName
   * @param employeeOB
   * @return
   */
  private EhcmTitletype findTitleByTitleName(String titleName, EhcmEmpPerInfo employeeOB) {

    try {
      OBContext.setAdminMode();
      final OBCriteria<EhcmTitletype> obtitle = OBDal.getInstance()
          .createCriteria(EhcmTitletype.class);
      obtitle.add(Restrictions.eq(EhcmTitletype.PROPERTY_NAME, titleName));
      obtitle.add(Restrictions.eq(EhcmTitletype.PROPERTY_CLIENT, employeeOB.getClient().getId()));
      obtitle.setFilterOnReadableClients(false);
      obtitle.setFilterOnReadableOrganization(false);
      final EhcmTitletype titleOB = (EhcmTitletype) obtitle.uniqueResult();
      return titleOB;
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Find Dependents by nationalId and employee id
   * 
   * @param dependentInformationDTO
   * @param employeeId
   * @return
   */
  private EhcmDependents findDependentByNationalId(String nationalID, EhcmEmpPerInfo employeeOB) {

    try {
      OBContext.setAdminMode();
      final OBCriteria<EhcmDependents> obDependent = OBDal.getInstance()
          .createCriteria(EhcmDependents.class);
      obDependent.add(Restrictions.eq(EhcmDependents.PROPERTY_NATIONALIDENTIFIER, nationalID));
      obDependent
          .add(Restrictions.eq(EhcmDependents.PROPERTY_CLIENT, employeeOB.getClient().getId()));
      obDependent.add(Restrictions.eq(EhcmDependents.PROPERTY_EHCMEMPPERINFO, employeeOB.getId()));
      obDependent.setFilterOnReadableClients(false);
      obDependent.setFilterOnReadableOrganization(false);
      final EhcmDependents dependentOB = (EhcmDependents) obDependent.uniqueResult();
      return dependentOB;
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void updateEmployeeProfileByUser(EhcmEmpPerInfo employeeOB,
      PersonalInformationDTO personalInformationDTO) throws SystemException {
    try {
      // TODO Auto-generated method stub
      employeeOB.setName(personalInformationDTO.getFirstNameEn());
      employeeOB.setArabicname(personalInformationDTO.getFirstNameAr());
      employeeOB.setArabicfatname(personalInformationDTO.getFatherNameAr());
      employeeOB.setFathername(personalInformationDTO.getFatherNameEn());
      employeeOB.setArbgrafaname(personalInformationDTO.getGrandFatherNameAr());
      employeeOB.setGrandfathername(personalInformationDTO.getGrandFatherNameEn());
      employeeOB.setArabicfamilyname(personalInformationDTO.getFamilyNameAr());
      employeeOB.setFamilyname(personalInformationDTO.getFamilyNameEn());
      if (null != personalInformationDTO.getDob()) {
        try {
          employeeOB.setDob(DateUtils.convertStringToDate(OPEN_BRAVO_DATE_FORMAT,
              personalInformationDTO.getDob()));
        } catch (ParseException e) {
          // TODO Auto-generated catch block
        }
      }
      // get title id based on title name
      if (personalInformationDTO.getTitle() != null) {

        employeeOB
            .setEhcmTitletype(findTitleByTitleName(personalInformationDTO.getTitle(), employeeOB));
      }

      employeeOB.setNationalityIdentifier(personalInformationDTO.getNationalId());
      employeeOB.setMarialstatus(personalInformationDTO.getMaritalStatus());
      employeeOB.setGender(personalInformationDTO.getGender());
      OBDal.getInstance().save(employeeOB);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public void updateEmployeeDependent(EhcmEmpPerInfo employeeOB,
      DependentInformationDTO ehcmDependent) throws SystemException {
    // TODO Auto-generated method stub
    try {
      // find dependent
      EhcmDependents ehcmDependents = findDependentByNationalId(ehcmDependent.getNationalId(),
          employeeOB);

      ehcmDependents.setRelationship(ehcmDependent.getRelationship());
      ehcmDependents.setFirstName(ehcmDependent.getFirstNameEn());
      ehcmDependents.setFathername(ehcmDependent.getFatherNameAr());
      ehcmDependents.setGrandfather(ehcmDependent.getGrandFatherNameAr());
      ehcmDependents.setFamily(ehcmDependent.getFatherNameAr());
      if (null != ehcmDependent.getDob()) {
        try {
          employeeOB.setDob(
              DateUtils.convertStringToDate(OPEN_BRAVO_DATE_FORMAT, ehcmDependent.getDob()));
        } catch (ParseException e) {
          // TODO Auto-generated catch block
        }
      }
      ehcmDependents.setGender(ehcmDependent.getGender());
      OBDal.getInstance().save(ehcmDependents);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public void updateEmployeeAddress(EhcmEmpPerInfo employeeOB,
      AddressInformationDTO addressInformationDTO) throws SystemException {
    // TODO Auto-generated method stub
    try {
      OBContext.setAdminMode();
      EHCMEmpAddress ehcmEmpAddress = employeeOB.getEHCMEmpAddressList().get(0);// Need to
      // check
      // if
      // there
      // can be
      // multiple
      // addresses
      // get country id based on country name
      final OBCriteria<Country> obc = OBDal.getInstance().createCriteria(Country.class);
      obc.add(Restrictions.eq(Country.PROPERTY_NAME, addressInformationDTO.getCountry()));
      obc.setFilterOnReadableClients(false);
      obc.setFilterOnReadableOrganization(false);
      final Country countryOB = (Country) obc.uniqueResult();
      ehcmEmpAddress.setCountry(countryOB);
      // get city id based on city name
      final OBCriteria<City> obcity = OBDal.getInstance().createCriteria(City.class);
      obcity.add(Restrictions.eq(City.PROPERTY_NAME, addressInformationDTO.getCity()));
      obcity.setFilterOnReadableClients(false);
      obcity.setFilterOnReadableOrganization(false);
      final City cityOB = (City) obcity.uniqueResult();
      ehcmEmpAddress.setCity(cityOB);
      // addressInformationDTO.setRegion(ehcmEmpAddress.get); Need to confirm from
      // where to get the
      // region
      ehcmEmpAddress.setDistrict(addressInformationDTO.getCity());
      ehcmEmpAddress.setStreet(addressInformationDTO.getStreet());
      ehcmEmpAddress.setPostBox(addressInformationDTO.getPostBox());
      ehcmEmpAddress.setPostalCode(addressInformationDTO.getPostalCode());
      ehcmEmpAddress.setAddressLine1(addressInformationDTO.getAddressLine1());
      ehcmEmpAddress.setAddressLine2(addressInformationDTO.getAddressLine2());
      OBDal.getInstance().save(ehcmEmpAddress);

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public void addDependent(String userName, DependentInformationDTO dependentInformationDTO)
      throws SystemException {
    // TODO Auto-generated method stub
    try {
      EhcmEmpPerInfo employeeOB = employeeProfileDAO.getEmployeeProfileByUser(userName);
      // find dependent
      EhcmDependents ehcmDependents = OBProvider.getInstance().get(EhcmDependents.class);

      ehcmDependents.setRelationship(dependentInformationDTO.getRelationship());
      ehcmDependents.setFirstName(dependentInformationDTO.getFirstNameEn());
      ehcmDependents.setFathername(dependentInformationDTO.getFatherNameAr());
      ehcmDependents.setGrandfather(dependentInformationDTO.getGrandFatherNameAr());
      ehcmDependents.setFamily(dependentInformationDTO.getFatherNameAr());
      if (null != dependentInformationDTO.getDob()) {
        try {
          employeeOB.setDob(DateUtils.convertStringToDate(OPEN_BRAVO_DATE_FORMAT,
              dependentInformationDTO.getDob()));
        } catch (ParseException e) {
          // TODO Auto-generated catch block
        }
      }
      ehcmDependents.setGender(dependentInformationDTO.getGender());
      ehcmDependents.setEhcmEmpPerinfo(employeeOB);
      ehcmDependents.setOrganization(employeeOB.getOrganization());
      ehcmDependents.setClient(employeeOB.getClient());
      ehcmDependents.setCreationDate(new java.util.Date());
      ehcmDependents.setCreatedBy(OBDal.getInstance().get(User.class, "100"));
      ehcmDependents.setUpdated(new java.util.Date());
      ehcmDependents.setUpdatedBy(OBDal.getInstance().get(User.class, "100"));
      OBDal.getInstance().save(ehcmDependents);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void removeDependent(String userName, String dependentId)
      throws SystemException, BusinessException {
    // TODO Auto-generated method stub
    try {
      // find dependent
      EhcmEmpPerInfo employeeOB = employeeProfileDAO.getEmployeeProfileByUser(userName);
      EhcmDependents ehcmDependents = findDependentByNationalId(dependentId, employeeOB);
      if (ehcmDependents == null)
        throw new BusinessException(MessageKeys.DEPENDENT_NOT_AVAILABLE);
      else
        OBDal.getInstance().remove(ehcmDependents);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void updateContactInformation(String userName,
      EmployeeAdditionalInformationDTO additionalInformationDTO)
      throws SystemException, BusinessException {
    // TODO Auto-generated method stub
    try {
      // find dependent
      EhcmEmpPerInfo employeeOB = employeeProfileDAO.getEmployeeProfileByUser(userName);
      employeeOB.setWorkno(additionalInformationDTO.getWorkno());
      employeeOB.setEmail(additionalInformationDTO.getEmail());
      employeeOB.setMobno(additionalInformationDTO.getMobno());
      employeeOB.setHomeno(additionalInformationDTO.getHomeno());
      OBDal.getInstance().save(employeeOB);

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public EhcmDependents findDependentByNationalId(String userName, String NationalID)
      throws SystemException, BusinessException {
    // TODO Auto-generated method stub
    EhcmEmpPerInfo employeeOB = employeeProfileDAO.getEmployeeProfileByUser(userName);
    EhcmDependents ehcmDependents = findDependentByNationalId(NationalID, employeeOB);
    return ehcmDependents;
  }

  @Override
  public void updateProfilePhoto(String userName, String PhotoBytes)
      throws SystemException, BusinessException {
    // TODO Auto-generated method stub
    try {
      // find dependent
      EhcmEmpPerInfo employeeOB = employeeProfileDAO.getEmployeeProfileByUser(userName);
      Image imgOB = OBProvider.getInstance().get(Image.class);
      imgOB.setClient(OBDal.getInstance().get(Client.class, employeeOB.getClient()));
      imgOB.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      imgOB.setName(employeeOB.getName());
      imgOB.setBindaryData(Base64.decodeBase64(PhotoBytes));
      imgOB.setWidth(new Long(200));
      imgOB.setHeight(new Long(200));
      // imgOB.setMimetype(mimetype);
      OBDal.getInstance().save(imgOB);
      OBDal.getInstance().flush();
      employeeOB.setCIVAdImage(imgOB);
      OBDal.getInstance().save(employeeOB);
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
