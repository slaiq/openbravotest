package sa.elm.ob.utility.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.Country;

import sa.elm.ob.hcm.EHCMOrgClassfication;
import sa.elm.ob.hcm.EHCMorgtype;
import sa.elm.ob.hcm.EhcmActiontype;
import sa.elm.ob.hcm.EhcmAddNationality;
import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.EhcmHrorgClassfication;
import sa.elm.ob.hcm.EhcmOrgView;
import sa.elm.ob.hcm.EhcmTitletype;

public class DataImportDAO {

  private static final String HR_ACTIVE_VALUE = "0";
  private static String DEFAULT_ORGANIZATION_ID = "HQ";
  private static String DEFAULT_TITLE = "M";
  private static String DEFAULT_COUNTRY = "29";
  private static String DEFAULT_LANG = "11";
  private static String DEFAULT_MARITAL_STATUS = "S";
  private static String NATIONAL_IDENTITY_ID = "NID";
  private static String IQAMA_IDENTITY_ID = "IQN";
  private static String STATUS_ISSUED = "I";
  public static String ROLE_ID = "B23B537E1AA5483C9410080B20FA8AE3";
  public static String DEFAULT_NATIONALITY_ID = "PQH_SA";
  private static Logger log = Logger.getLogger(DataImportDAO.class);
  private static String MINISTER_ORG_COED = "0100001-0200100";

  static {
    log.info("Load Import HR data properties.");
    Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    DEFAULT_ORGANIZATION_ID = openbravoProperties.getProperty("organization.default");
    DEFAULT_TITLE = openbravoProperties.getProperty("title.default");
    DEFAULT_COUNTRY = openbravoProperties.getProperty("country.default");
    DEFAULT_LANG = openbravoProperties.getProperty("lang.default");
    DEFAULT_MARITAL_STATUS = openbravoProperties.getProperty("maritalStatus.default");
    STATUS_ISSUED = openbravoProperties.getProperty("status.default");
    ROLE_ID = openbravoProperties.getProperty("roleId.default");
    DEFAULT_NATIONALITY_ID = openbravoProperties.getProperty("nationalityId.default");
    log.info("Import HR data properties" + DEFAULT_ORGANIZATION_ID + '|' + DEFAULT_TITLE + '|'
        + DEFAULT_COUNTRY + '|' + DEFAULT_LANG + '|' + DEFAULT_MARITAL_STATUS + '|'
        + NATIONAL_IDENTITY_ID + '|' + STATUS_ISSUED + '|' + ROLE_ID + '|'
        + DEFAULT_NATIONALITY_ID);
  }

  public static void updateEmployee(EhcmEmpPerInfo empInfo, DataImportVO vo) throws Exception {
    try {
      mapEmployeeInfo(empInfo, vo);
      OBDal.getInstance().save(empInfo);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }
  }

  public static EhcmEmpPerInfo createEmployee(DataImportVO vo) throws Exception {
    EhcmEmpPerInfo empInfo = OBProvider.getInstance().get(EhcmEmpPerInfo.class);
    try {
      empInfo.setClient(OBContext.getOBContext().getCurrentClient());
      empInfo.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      empInfo.setCreatedBy(OBContext.getOBContext().getUser());
      mapEmployeeInfo(empInfo, vo);
      OBDal.getInstance().save(empInfo);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }
    return empInfo;
  }

  private static void mapEmployeeInfo(EhcmEmpPerInfo empInfo, DataImportVO vo) {
    Country country = getCountry(vo.getCountryOfBirth());
    BusinessPartner bPartner = null;
    if (empInfo.getBusinessPartner() == null) {
      bPartner = createBusinessPartner(vo, country);
      empInfo.setBusinessPartner(bPartner);
    } else {
      bPartner = updateBusinessPartner(vo, empInfo.getBusinessPartner().getId());
      empInfo.setBusinessPartner(bPartner);
    }
    empInfo.setEhcmTitletype(getTitle(vo.getTitle()));
    empInfo.setUpdatedBy(OBContext.getOBContext().getUser());
    empInfo.setArabicname(vo.getFullName());
    empInfo.setArabicfatname(vo.getFathername_ar());
    empInfo.setArbgrafaname(vo.getGrandname_ar());
    empInfo.setArabicfamilyname(vo.getFamilyname_ar());
    empInfo.setArbfouname(vo.getFirstname_ar());
    empInfo.setName(vo.getFullName());
    empInfo.setFathername(vo.getFathername_en());
    empInfo.setFamilyname(vo.getFamilyname_en());
    empInfo.setGrandfathername(vo.getGrandname_en());
    empInfo.setTownbirth(vo.getPlaceOfBirth());
    empInfo.setGender(vo.getSex());
    empInfo.setSearchKey(vo.getEmployeeno());
    setNationalityIdentifier(empInfo, vo);
    empInfo.setBloodtype(vo.getBlood_type());
    if (StringUtils.isEmpty(vo.getMaritalStatus())) {
      empInfo.setMarialstatus(DEFAULT_MARITAL_STATUS);
    } else {
      empInfo.setMarialstatus(vo.getMaritalStatus());
    }
    empInfo.setDecisionno(vo.getHire_decreeno());
    empInfo.setMcsletterno(vo.getHire_decreeno());
    empInfo.setEmail(vo.getEmail());
    empInfo.setMobno(vo.getMobile());
    empInfo.setWorkno(vo.getExtno());
    empInfo.setOfficename(vo.getOfficeno());
    empInfo.setStatus(STATUS_ISSUED);

    EhcmActiontype actionType = getActionType();
    empInfo.setPersonType(actionType);
    empInfo.setEhcmActiontype(actionType);

    empInfo.setManager(vo.getStrManagerId());
    empInfo.setDeptCode(vo.getOrgCode().equals("-") ? "" : vo.getOrgCode());

    EhcmAddNationality nationality = getNationality(vo.getNationality_name());
    if (nationality != null) {
      empInfo.setEhcmAddnationality(nationality);
    }
  }

  private static void setNationalityIdentifier(EhcmEmpPerInfo empInfo, DataImportVO vo) {
    String nationalityId = vo.getNationalityId();

    if (nationalityId == null || DEFAULT_NATIONALITY_ID.equalsIgnoreCase(nationalityId)) {
      empInfo.setNationalityIdentifier(vo.getNational_no());
    } else {
      empInfo.setNationalityIdentifier(vo.getIqamaNo());
    }
  }

  private static EhcmActiontype getActionType() {
    EhcmActiontype ehcmActiontype = null;
    try {
      OBQuery<EhcmActiontype> query = OBDal.getInstance().createQuery(EhcmActiontype.class,
          " where code='HE' ");
      query.setMaxResult(1);
      if (query != null && query.list().size() > 0) {
        ehcmActiontype = query.list().get(0);
      }
    } catch (Exception e) {
      log.error("Exception while getActionType(): ", e);
    }
    return ehcmActiontype;
  }

  private static BusinessPartner updateBusinessPartner(DataImportVO vo, String strBPartnerId) {
    BusinessPartner bp = null;
    try {
      OBContext.setAdminMode();

      bp = Utility.getObject(BusinessPartner.class, strBPartnerId);
      bp.setEscmImported(true);
      bp.setUpdatedBy(OBContext.getOBContext().getUser());
      bp.setName(vo.getFullName());
      setNationalityInfo(vo, bp);
      bp.setEscmIdentityexpdate(new Date());
      bp.setEmployee(Boolean.TRUE);
      bp.setVendor(Boolean.FALSE);
      bp.setCustomer(Boolean.FALSE);
      bp.setEhcmManager(vo.getStrManagerId());
      bp.setEhcmDepartmentCode(vo.getOrgCode().equals("-") ? "" : vo.getOrgCode());

      if (vo.getIsActive().equals("1"))
        bp.setActive(Boolean.FALSE);
      else
        bp.setActive(Boolean.TRUE);

      bp.setEhcmPosition(vo.getPosition());
      bp.setEhcmGrade(vo.getGradeCode());

      bp.setEhcmParentOrg(getParentOrganization(vo));
      bp.setEfinNationalityTxt(vo.getNationality_name());

      OBDal.getInstance().save(bp);
    } catch (Exception e) {
      log.error("Exception while updateBusinessPartner.", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return bp;
  }

  private static String getParentOrganization(DataImportVO vo) {
    String parentOrg = OrgUtils.getParentOrg(vo.getOrgCode());
    log.info("Get Parent Organization for: " + vo.getOrgCode() + ", Parent code is:" + parentOrg);
    return parentOrg;
  }

  private static EhcmTitletype getTitle(String titleName) {
    EhcmTitletype title = null;
    try {
      if (StringUtils.isEmpty(titleName)) {
        titleName = DEFAULT_TITLE;
      }
      OBQuery<EhcmTitletype> query = OBDal.getInstance().createQuery(EhcmTitletype.class,
          " where name ='" + titleName + "'");
      if (query != null && query.list().size() > 0) {
        title = query.list().get(0);
      }
    } catch (Exception e) {
      log.error("Exception while getBusinessPartner.", e);
    }
    return title;
  }

  private static BusinessPartner createBusinessPartner(DataImportVO vo, Country country) {
    BusinessPartner bp = null;

    try {
      bp = OBProvider.getInstance().get(BusinessPartner.class);

      bp.setEscmImported(true);
      bp.setClient(OBContext.getOBContext().getCurrentClient());
      bp.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      bp.setCreatedBy(OBContext.getOBContext().getUser());
      bp.setUpdatedBy(OBContext.getOBContext().getUser());

      bp.setSearchKey(vo.getEmployeeno());
      bp.setEfinDocumentno(vo.getEmployeeno());
      bp.setName(vo.getFullName());
      bp.setEfinNationality(country);
      setNationalityInfo(vo, bp);
      bp.setEscmIdentityexpdate(new Date());
      bp.setEmployee(Boolean.TRUE);
      bp.setVendor(Boolean.FALSE);
      bp.setCustomer(Boolean.FALSE);
      bp.setBusinessPartnerCategory(getBusinessPartnerCategory());
      bp.setEhcmManager(vo.getStrManagerId());
      bp.setEhcmDepartmentCode(vo.getOrgCode().equals("-") ? "" : vo.getOrgCode());
      bp.setEhcmPosition(vo.getPosition());
      bp.setEhcmGrade(vo.getGradeCode());
      bp.setEfinNationalityTxt(vo.getNationality_name());

      bp.setEhcmParentOrg(getParentOrganization(vo));

      if (vo.getIsActive().equals("1"))
        bp.setActive(Boolean.FALSE);

      OBDal.getInstance().save(bp);
    } catch (Exception e) {
      log.error("Exception while getBusinessPartner.", e);
    }
    return bp;
  }

  private static void setNationalityInfo(DataImportVO vo, BusinessPartner bp) {
    String nationalityId = vo.getNationalityId();
    if (nationalityId == null || DEFAULT_NATIONALITY_ID.equalsIgnoreCase(nationalityId)) {
      bp.setEfinIdentityname(NATIONAL_IDENTITY_ID);
      bp.setEfinNationalidnumber(vo.getNational_no());
    } else {
      bp.setEfinIdentityname(IQAMA_IDENTITY_ID);
      bp.setEfinNationalidnumber(vo.getIqamaNo());
    }
  }

  private static Category getBusinessPartnerCategory() {
    Category category = null;
    try {
      OBQuery<Category> query = OBDal.getInstance().createQuery(Category.class,
          " order by default desc ");
      if (query != null && query.list().size() > 0) {
        category = query.list().get(0);
      }
    } catch (Exception e) {
      log.error("Exception while getBusinessPartnerCategory.", e);
    }
    return category;
  }

  private static Country getCountry(String countryOfBirth) {
    Country country = null;
    try {
      OBQuery<Country> query = OBDal.getInstance().createQuery(Country.class,
          " where iSOCountryCode ='" + countryOfBirth + "'");
      if (query != null && query.list().size() > 0)
        country = query.list().get(0);
      else {
        country = getDefaultCountry(country);
      }
    } catch (Exception e) {
      log.error("Exception while get Country.", e);
    }
    return country;
  }

  private static Country getDefaultCountry(Country country) {
    OBQuery<Country> defaultQuery = OBDal.getInstance().createQuery(Country.class,
        " where iSOCountryCode ='" + DEFAULT_COUNTRY + "'");
    if (defaultQuery != null && defaultQuery.list().size() > 0) {
      country = defaultQuery.list().get(0);
    } else {
      log.error("Can't find default country.");
    }
    return country;
  }

  private static EhcmAddNationality getNationality(String nationalityName) {
    EhcmAddNationality ehcmAddNationality = null;
    try {
      OBQuery<EhcmAddNationality> nationality = OBDal.getInstance()
          .createQuery(EhcmAddNationality.class, " where name = '" + nationalityName + "'");
      if (nationality != null && nationality.list().size() > 0) {
        ehcmAddNationality = nationality.list().get(0);
      }
    } catch (Exception e) {
      log.error("Exception while get Nationality.", e);
    }
    return ehcmAddNationality;
  }

  public static EhcmEmpPerInfo getEmployee(String employeeno) {

    EhcmEmpPerInfo ehcmEmpPerInfo = null;
    try {
      OBQuery<EhcmEmpPerInfo> query = OBDal.getInstance().createQuery(EhcmEmpPerInfo.class,
          " where searchKey='" + employeeno + "'");
      if (query != null && query.list().size() > 0) {
        ehcmEmpPerInfo = query.list().get(0);
      }
    } catch (Exception e) {
      log.error("Exception while check Avaiable employee", e);
    }
    return ehcmEmpPerInfo;
  }

  public static List<DataImportVO> getEmployees(Connection connection) throws SQLException {
    List<DataImportVO> employees = new ArrayList<DataImportVO>();
    String sql = "SELECT emp.EMPLOYEE_ID, emp.ISACTIVE, emp.NATIONAL_NO, emp.EMPLOYEE_FIRST_NAME_AR, emp.EMPLOYEE_FAMILY_NAME_AR, emp.EMAIL, org.LOCATION_ID FROM XXX_HR_INT_EMPLOYEE emp "
        + "LEFT JOIN XXX_HR_INT_ORGANIZATION org ON emp.ACTUAL_ORG_ID =  org.ORG_ID";
    PreparedStatement ps = connection.prepareStatement(sql.toString());
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      employees.add(mapEmployee(rs));
    }
    return employees;
  }

  public static List<DataImportVO> getContractedEmployees(Connection connection)
      throws SQLException {
    List<DataImportVO> employees = new ArrayList<DataImportVO>();
    String sql = "SELECT CT.EMPL_CODE, DECODE (CT.EMPL_STATUS, 'Active','0','1'), CT.ID_NO, CT.ARABIC_FIRST_NAME, CT.ARABIC_FAMILY_NAME, CT.MOT_EMAIL, org.LOCATION_ID FROM HR_EMPLOYEES_OF_CONTRATORS CT "
        + " JOIN XXX_HR_INT_ORGANIZATION org ON concat(concat(CT.BRANCH_CODE, '-'), CT.DEPARTMENT_CODE) =  org.ORG_CODE ";
    PreparedStatement ps = connection.prepareStatement(sql.toString());
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      employees.add(mapEmployee(rs));
    }
    return employees;
  }

  public static Boolean hasRole(String strRoleId) {
    try {
      Role role = Utility.getObject(Role.class, strRoleId);
      if (role != null)
        return Boolean.TRUE;
      else
        return Boolean.FALSE;
    } catch (Exception e) {
      log.error("Exception while check the Role: ", e);
      return Boolean.FALSE;
    }
  }

  public static User createUser(DataImportVO employee, Role role, BusinessPartner bp)
      throws Exception {
    User user = null;
    try {
      user = OBProvider.getInstance().get(User.class);

      user.setClient(OBContext.getOBContext().getCurrentClient());
      user.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      user.setCreatedBy(OBContext.getOBContext().getUser());
      user.setUpdatedBy(OBContext.getOBContext().getUser());

      user.setName(bp.getName());
      user.setUsername(bp.getEfinDocumentno());
      user.setBusinessPartner(bp);
      user.setDefaultLanguage(Utility.getObject(Language.class, DEFAULT_LANG));
      user.setFirstName(employee.getFirstname_ar());
      user.setLastName(employee.getFamilyname_ar());
      user.setEmail(employee.getEmail());
      if (calculateActiveValue(employee.getIsActive())) {
        user.setActive(true);
      } else {
        user.setActive(false);
      }
      OBDal.getInstance().save(user);

      UserRoles userRoles = OBProvider.getInstance().get(UserRoles.class);
      userRoles.setClient(OBContext.getOBContext().getCurrentClient());
      userRoles.setOrganization(Utility.getObject(Organization.class, HR_ACTIVE_VALUE));
      userRoles.setCreatedBy(OBContext.getOBContext().getUser());
      userRoles.setUpdatedBy(OBContext.getOBContext().getUser());

      userRoles.setUserContact(user);
      userRoles.setRole(role);

      OBDal.getInstance().save(userRoles);

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }

    return user;
  }

  private static boolean calculateActiveValue(String value) {
    return HR_ACTIVE_VALUE.equals(value);
  }

  public static List<BusinessPartner> mapBusinessPartners(HashSet<String> employees) {
    List<BusinessPartner> bpList = null;
    try {
      String employeeIds = StringUtils.join(employees, "','");
      employeeIds = "'" + employeeIds + "'";
      OBQuery<BusinessPartner> bpQuery = null;
      log.info("Employee Ids: " + employeeIds);
      if (employeeIds.length() > 0) {
        bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
            " where  efinDocumentno in (" + employeeIds
                + ") and efinDocumentno not in (select username from ADUser where username is not null )");
      }
      bpList = bpQuery.list();
    } catch (Exception e) {
      log.error("Exception while get Business Partners.", e);
    }
    return bpList;
  }

  public static BusinessPartner getBusinessPartner(String employeeId) {
    BusinessPartner bp = null;
    try {
      OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
          " where efinDocumentno = :employeeId ");
      bpQuery.setNamedParameter("employeeId", employeeId);
      bpQuery.setFilterOnActive(false);
      bp = bpQuery.uniqueResult();
    } catch (Exception e) {
      log.error("Exception while get Business Partners.", e);
    }
    return bp;
  }

  public static List<DataImportVO> getHRDepartements(Connection connection) throws SQLException {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append(
        " select ORG_ID, ORG_CODE, ORG_NAME_AR,ORG_TYPE, PARENT_ORG_CODE, PARENT_ORG_ID as parentOrg, ORG_IN_EX  from XXX_HR_INT_ORGANIZATION where ORG_CODE is not null");

    List<DataImportVO> vos = new ArrayList<DataImportVO>();
    PreparedStatement ps = connection.prepareStatement(queryBuilder.toString());
    ResultSet rs = ps.executeQuery();

    while (rs.next()) {
      DataImportVO vo = new DataImportVO();
      vo.setOrgCode(rs.getString(2));
      vo.setOrgName(rs.getString(3));
      vo.setOrgType(rs.getString(4));
      vo.setParentOrgCode(rs.getString(5));
      vo.setParentOrgId(rs.getString(6));
      vo.setOrgInExType(rs.getString(7));
      vos.add(vo);
    }
    return vos;
  }

  private static Long getMaxOrgLevel(String strClientId) {
    Long orgLevel = 1L;
    try {
      StringBuilder queryBuilder = new StringBuilder();

      queryBuilder.append("select max(level) as level  from EHCM_org_type e where e.client.id ='")
          .append(strClientId).append("' ");
      log.info("Query : " + queryBuilder.toString());
      Query query = OBDal.getInstance().getSession().createQuery(queryBuilder.toString());

      if (query != null && query.list().size() > 0) {
        orgLevel = (Long) query.list().get(0);
        orgLevel = orgLevel + 1;
      }
    } catch (Exception e) {
      log.error("Exception while get Max Org Level.", e);
    }
    return orgLevel;
  }

  public static int addOrganization(OrganizationType organizationType, Currency currency,
      DataImportVO department, List<DataImportVO> hrDepartements, Client client) throws Exception {

    log.error("Start Create new departement with code:" + department.getOrgCode()
        + ", and parent code:" + department.getParentOrgCode());

    int created = 0;
    Organization parentOrg = getParentOrganization(department, client);

    if (parentOrg != null) {
      log.info("parent organization found in database for code:" + department.getOrgCode()
          + ", parent code code:" + department.getParentOrgCode());
      createOrganization(organizationType, currency, department, client, parentOrg);
      created++;
    } else {
      log.error(
          "Can't find parent organization in database with code:" + department.getParentOrgCode());
      DataImportVO parentDepartementVO = getDepartement(hrDepartements,
          department.getParentOrgCode());
      if (parentDepartementVO != null) {
        log.info("parent organization found in new departement list  with code :"
            + department.getOrgCode() + ", and parent code =" + department.getParentOrgCode()
            + ", parent will be created.");
        addOrganization(organizationType, currency, parentDepartementVO, hrDepartements, client);
        created++;
      } else {
        log.error("Can't find parent organization in new departement list  with code:"
            + department.getParentOrgCode());
        throw new Exception(
            "Can't Save Departement, Parent Departement not exist in database or the new departement list.");
      }
    }
    return created;
  }

  private static Organization createOrganization(OrganizationType organizationType,
      Currency currency, DataImportVO department, Client client, Organization parentOrg)
      throws Exception {
    Organization organization = null;
    try {
      organization = OBProvider.getInstance().get(Organization.class);
      organization.setClient(client);
      organization.setSearchKey(department.getOrgCode());
      organization.setName(department.getOrgName());
      organization.setCurrency(currency);
      organization.setOrganizationType(organizationType);
      organization.setCreatedBy(OBContext.getOBContext().getUser());
      organization.setUpdatedBy(OBContext.getOBContext().getUser());
      organization.setEhcmIshrorg("Y");
      organization.setEhcmOrgtyp(getOrAddOrgType(department.getOrgType(), client));
      organization.setEhcmAdOrg(parentOrg);
      if (parentOrg != null) {
        organization
            .setEhcmParentOrg(OBDal.getInstance().get(EhcmOrgView.class, parentOrg.getId()));
      }
      OBDal.getInstance().save(organization);
      // Create the organization classification
      saveClassifications(organization);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }

    return organization;
  }

  /**
   * Save all the classifications with HR as enabled
   * 
   * @param organization
   */
  private static void saveClassifications(Organization organization) {

    // Save the HR Classification
    saveClassification(organization, getEhcmClassificationByName("HR"), true);
    // Save the Finance Classification
    saveClassification(organization, getEhcmClassificationByName("FIN"), false);
    // Save SCM Classification
    saveClassification(organization, getEhcmClassificationByName("SCM"), false);
  }

  /**
   * Save the individual classification
   * 
   * @param classification
   * @param code
   * @param organization
   * @param orgClassName
   * @param enabled
   */
  private static void saveClassification(Organization organization,
      EHCMOrgClassfication ehcmOrgClassfication, boolean enabled) {

    EhcmHrorgClassfication ehcmHorgClassfication = new EhcmHrorgClassfication();

    ehcmHorgClassfication.setOrganization(organization);
    ehcmHorgClassfication.setOrganizationClassification(ehcmOrgClassfication);
    ehcmHorgClassfication.setEnabled(enabled);

    OBDal.getInstance().save(ehcmOrgClassfication);

  }

  private static EHCMOrgClassfication getEhcmClassificationByName(String classification) {
    OBQuery<EHCMOrgClassfication> bpQuery = OBDal.getInstance()
        .createQuery(EHCMOrgClassfication.class, " where classification = :classification ");
    bpQuery.setNamedParameter("classification", classification);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private static DataImportVO getDepartement(List<DataImportVO> hrDepartements,
      String parentOrgCode) {
    DataImportVO parentDepartementVO = null;
    for (DataImportVO departement : hrDepartements) {
      if (departement.getOrgCode().equals(parentOrgCode)) {
        return departement;
      }
    }
    return parentDepartementVO;
  }

  private static Organization getDefaultOrg(String clientId) {
    log.info("DEFAULT_ORGANIZATION_ID:" + DEFAULT_ORGANIZATION_ID);
    Organization org = getOrganization(DEFAULT_ORGANIZATION_ID, clientId);
    return org;
  }

  @SuppressWarnings("unused")
  private static Organization getRootOrganization() {
    Organization organization = null;
    try {
      organization = OBDal.getInstance().get(Organization.class, "0");
      if (organization != null) {
        log.info("Root Organzation fetched with name :" + organization.getName());
      }
    } catch (Exception e) {
      log.error("Exception while get root organization", e);
    }
    return organization;
  }

  private static EHCMorgtype getOrAddOrgType(String orgType, Client client) {

    EHCMorgtype eHCMorgtype = null;
    try {
      OBQuery<EHCMorgtype> obQuery = OBDal.getInstance().createQuery(EHCMorgtype.class,
          " where lower(searchKey) ='" + orgType.toLowerCase() + "'");
      if (obQuery != null && obQuery.list().size() > 0) {
        eHCMorgtype = obQuery.list().get(0);
      } else {
        Long orgLevel = getMaxOrgLevel(client.getId());

        eHCMorgtype = OBProvider.getInstance().get(EHCMorgtype.class);

        eHCMorgtype.setClient(client);
        eHCMorgtype.setOrganization(OBContext.getOBContext().getCurrentOrganization());
        eHCMorgtype.setLevel(orgLevel);
        eHCMorgtype.setSearchKey(orgType);
        eHCMorgtype.setOrgtypename(orgType);
        eHCMorgtype.setStartDate(new java.util.Date());

        OBDal.getInstance().save(eHCMorgtype);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Excpetion while check Org Types.", e);
    }
    return eHCMorgtype;
  }

  public static void updateOrganization(Organization organization, DataImportVO department,
      Client client) throws Exception {
    if (organization != null) {
      Organization parentOrg = getParentOrganization(department, client);
      try {
        organization.setActive(true);
        organization.setEhcmAdOrg(parentOrg);
        organization.setName(department.getOrgName());
        if (parentOrg != null) {
          organization
              .setEhcmParentOrg(OBDal.getInstance().get(EhcmOrgView.class, parentOrg.getId()));
        }
        OBDal.getInstance().save(organization);
        // check if the list of organization classifications is there or not
        if (null == organization.getEhcmHrorgClassficationList()
            || organization.getEhcmHrorgClassficationList().size() == 0) {
          saveClassifications(organization);
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      } catch (Exception e) {
        OBDal.getInstance().rollbackAndClose();
        throw e;
      }
    }
  }

  private static Organization getParentOrganization(DataImportVO department, Client client) {
    Organization parentOrg = null;

    if (isMinister(department)) {
      log.info("Minister Organization found, Parent organization will be HQ.");
      parentOrg = getHQOrganization();
    } else if (department.getParentOrgCode() == null) {
      log.info("parent organization code is empty for department with code:"
          + department.getOrgCode() + ", default organization will be used as a parent.");
      parentOrg = getDefaultOrg(client.getId());
    } else {
      parentOrg = getOrganization(department.getParentOrgCode(), client.getId());
    }
    return parentOrg;
  }

  private static boolean isMinister(DataImportVO department) {
    return MINISTER_ORG_COED.equalsIgnoreCase(department.getOrgCode());
  }

  private static Organization getHQOrganization() {
    Organization organization = null;
    try {
      OBQuery<Organization> orgQuery = OBDal.getInstance().createQuery(Organization.class,
          " where searchKey = :orgCode");
      orgQuery.setNamedParameter("orgCode", "HQ");
      orgQuery.setFilterOnActive(false);
      if (orgQuery != null) {
        List<Organization> orgs = orgQuery.list();
        if (orgs.size() > 0) {
          organization = orgs.get(0);
        }
      }
    } catch (Exception e) {
      log.error("Exception while get HQ Organization", e);
    }
    return organization;
  }

  public static Organization getOrganization(String orgCode, String clientId) {

    Organization organization = null;
    try {
      OBQuery<Organization> orgQuery = OBDal.getInstance().createQuery(Organization.class,
          " where searchKey = :orgCode and client.id = :clientId");
      orgQuery.setNamedParameter("orgCode", orgCode);
      orgQuery.setNamedParameter("clientId", clientId);
      orgQuery.setFilterOnActive(false);
      if (orgQuery != null) {
        List<Organization> orgs = orgQuery.list();
        if (orgs.size() > 0) {
          organization = orgs.get(0);
        }
      }
    } catch (Exception e) {
      log.error(
          "Exception while get Organization with code =" + orgCode + ", Client Id =" + clientId, e);
    }
    return organization;
  }

  public static User getUser(DataImportVO employee) {
    User user = null;
    try {
      OBQuery<User> bpQuery = OBDal.getInstance().createQuery(User.class,
          " where username = :username ");
      bpQuery.setFilterOnActive(false);
      bpQuery.setNamedParameter("username", employee.getEmployeeno());
      user = bpQuery.uniqueResult();
    } catch (Exception e) {
      log.error("Exception while get Business Partners.", e);
    }
    return user;
  }

  public static DataImportVO mapEmployee(ResultSet rs) throws SQLException {
    DataImportVO employeeVO = new DataImportVO();
    employeeVO.setEmployeeno(rs.getString(1));
    employeeVO.setIsActive(rs.getString(2));
    employeeVO.setNational_no(rs.getString(3));
    employeeVO.setFirstname_ar(rs.getString(4));
    employeeVO.setFamilyname_ar(rs.getString(5));
    employeeVO.setEmail(rs.getString(6));
    employeeVO.setLocationId(rs.getString(7));
    return employeeVO;
  }

  public static void updateUser(User user, DataImportVO employee) throws Exception {
    try {
      if (calculateActiveValue(employee.getIsActive())) {
        user.setActive(true);
      } else {
        user.setActive(false);
      }
      user.setFirstName(employee.getFirstname_ar());
      user.setLastName(employee.getFamilyname_ar());
      user.setEmail(employee.getEmail());
      OBDal.getInstance().save(user);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }
  }
}