/**
 * 
 */
package sa.elm.ob.utility.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import sa.elm.ob.hcm.EhcmEmpPerInfo;

/**
 * @author Gopinagh.R
 * 
 */
public class ConnectAndUpdate extends DalBaseProcess {

	private static final Logger log = Logger.getLogger(ConnectAndUpdate.class);
	private static String MALE_AR = "ذكر";
	private ProcessLogger logger;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		logger = bundle.getLogger();
		Connection connection = null;
		OBError obError = null;

		try {
			String connectionId = (String) bundle.getParams().get(ConnectionUtility.HR_CONNECTION_ID);

			OBContext.setAdminMode();

			logger.logln("Import Employee Schedule Started.");
			log.info("Import Employee Schedule Started.");

			try {
				connection = ConnectionUtility.getHrConnection(connectionId);
			} catch (Exception e) {
				throw new Exception("Can't Connect to Oracle database, Process terminated.", e);
			}

			List<DataImportVO> vos = null;
	                List<DataImportVO> contractor = null;

			int totalRecords = 0;
			
			try {
				vos = getEmployeesList(connection);
				contractor = getContractorsList(connection);
				//vos.addAll(getContingentEmployeesList(connection));
				//vos = getContractorsList(connection);
				for (Iterator<DataImportVO> conIterator = contractor.iterator(); conIterator.hasNext();) {
				        DataImportVO contr = conIterator.next();
				        for (Iterator<DataImportVO> empIterator = vos.iterator(); empIterator.hasNext();) {
				          DataImportVO emp = empIterator.next();
				          if (contr.getEmployeeno().equals(emp.getEmployeeno())) {
				            if (contr.getIsActive().equals("0")) {
				              empIterator.remove();
				            } else {
				              conIterator.remove();
				            }
				          }
				        }
				      }
				vos.addAll(contractor);

				totalRecords = vos.size();
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception("Can't get Employees Data.", e);
			}

			int createdSuccessfully = 0;
			int failedCreate = 0;
			
			int updatedSuccessfully = 0;
			int failedUpdate = 0;


			for (DataImportVO employeeVO : vos) {
				EhcmEmpPerInfo empInfo = DataImportDAO.getEmployee(employeeVO.getEmployeeno());
				if (empInfo == null) {
					try {
						empInfo = DataImportDAO.createEmployee(employeeVO);
						log.info("New Employee Add Successfully, Employee No. =" + employeeVO.getEmployeeno());
						createdSuccessfully++;
					} catch (Exception e) {
						failedCreate++;
						logger.logln("Failed to create New Employee , Employee No. =" + employeeVO.getEmployeeno() + " Error:" + e.getMessage());
						log.error("Failed to create New Employee , Employee No. =" + employeeVO.getEmployeeno(), e);
					}
				} else {
					try {
						DataImportDAO.updateEmployee(empInfo, employeeVO);
						log.info("Employee Updated Successfully, Employee No. =" + employeeVO.getEmployeeno());
						updatedSuccessfully++;
					} catch (Exception e) {
						failedUpdate++;
						logger.logln("Failed to update Employee , Employee No. =" + employeeVO.getEmployeeno() + " Error: =" + e.getMessage());
						log.error("Failed to update Employee , Employee No. =" + employeeVO.getEmployeeno(), e);
					}
				}
			}

			StringBuilder recordSummary = new StringBuilder();
			recordSummary.append("Total Records in HR Database = " + totalRecords);
			recordSummary.append(", Successfully Created = " + createdSuccessfully);
			recordSummary.append(", Failed to create = " + failedCreate);
			recordSummary.append(", Successfully Updated = " + updatedSuccessfully);
			recordSummary.append(", Failed to Update = " + failedUpdate);

			logger.logln(recordSummary.toString());
			log.info(recordSummary.toString());

			obError = buildResult("Success", "Employees Imported Successfully.", recordSummary.toString());
			
			bundle.setResult(obError);
			logger.logln("Import Employee Schedule Finished.");
			log.info("Import Employee Schedule Finished.");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			if (e.getCause() != null) {
				obError = buildResult("Error", e.getMessage(), e.getCause().getMessage());
			} else {
				obError = buildResult("Error", e.getMessage(), null);
			}
			bundle.setResult(obError);
			if (connection != null) {
				connection.close();
			}
			OBContext.restorePreviousMode();
			throw e;
		} finally {			
			if (connection != null) {
				connection.close();
			}
			OBContext.restorePreviousMode();
		}

	}

	private OBError buildResult(String type, String title, String message) {
		OBError obError = new OBError();
		obError.setType(type);
		obError.setTitle(title);
		obError.setMessage(message);
		return obError;
	}

	
	private List<DataImportVO> getContractorsList(Connection connection) throws SQLException {

		HashSet<String> managersList = getContractorsManagersList(connection);
		managersList.addAll(getContractorsManagersList1(connection));

		List<DataImportVO> vos = new ArrayList<DataImportVO>();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append(
				" select CT.EMPL_CODE, CT.ARABIC_FIRST_NAME, CT.ARABIC_FATHER_NAME, CT.ARABIC_GDFTHER_NAME, CT.ARABIC_FAMILY_NAME, ");
		sqlBuilder.append(
				" CT.ENGLISH_FIRST_NAME, CT.ENGLISH_FATHER_NAME, CT.ENGLISH_FAMILY_NAME, ");
		sqlBuilder.append(
				" CT.START_DATE, CT.ID_NO, ");
		sqlBuilder.append(
				" CT.NATIONALITY, CT.MOT_EMAIL, CT.MOBILE_NUMBER, org.LOCATION_ID,  ");
		sqlBuilder.append(
				" CT.MANAGER_NAME, CT.BRANCH_NAME, CT.ARABIC_FIRST_NAME || ' ' || CT.ARABIC_FATHER_NAME || ' ' || CT.ARABIC_FAMILY_NAME "
				+ " as full_name, CT.EMPL_STATUS, coalesce(org.ORG_CODE,'-') as orgcode , coalesce(CT.MANAGER_CODE,'-') as mgrID ");
		sqlBuilder.append(
				" ,"
				+ " COALESCE(CT.POSITION_TITLE, '') as position, CT.ID_NO as iqamaNo");
		sqlBuilder.append(" from HR_EMPLOYEES_OF_CONTRATORS CT  ");
		sqlBuilder.append(
				" join XXX_HR_INT_ORGANIZATION org on org.ORG_CODE = concat(concat(CT.BRANCH_CODE, '-'), CT.DEPARTMENT_CODE) where ( CT.ID_NO IS NOT NULL) ");

		PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			DataImportVO vo = mapContractorsData(rs, managersList);
			vos.add(vo);
		}
		return vos;
	}

	private List<DataImportVO> getEmployeesList(Connection connection) throws SQLException {

		HashSet<String> managersList = getManagersList(connection);
                managersList.addAll(getManagersList1(connection));

		List<DataImportVO> vos = new ArrayList<DataImportVO>();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append(
				" select emp.EMPLOYEE_ID, emp.TITLE, emp.SEX, emp.EMPLOYEE_FIRST_NAME_AR, emp.EMPLOYEE_FATHER_NAME_AR, emp.EMPLOYEE_GRAND_NAME_AR, emp.EMPLOYEE_FAMILY_NAME_AR, ");
		sqlBuilder.append(
				" emp.ENGLISH_FIRST_NAME_EN, emp.ENGLISH_FATHER_NAME_EN, emp.ENGLISH_GRAND_NAME_EN, emp.ENGLISH_FAMILY_NAME_EN, ");
		sqlBuilder.append(
				" emp.MINISTRY_HIRE_DATE, emp.GOV_HIRE_DATE, emp.NATIONAL_NO, emp.BIRTH_DATE, emp.COUNTRY_OF_BIRTH, emp.PLACE_OF_BIRTH, emp.BLOOD_TYPE, emp.MARITAL_STATUS, ");
		sqlBuilder.append(
				" emp.NATIONALITY_NAME, emp.RELIGION, emp.EMAIL, emp.MOBILE, emp.EXTENSION_NO, emp.OFFICE_NO, emp.LOCATION_ID, emp.HIRE_DECREE_NO, emp.HIRE_DECREE_DATE, ");
		sqlBuilder.append(
				" emp.MANAGER_NAME, emp.ACTUAL_ORG_NAME, coalesce(EMPLOYEE_FULL_NAME_AR, EMPLOYEE_FIRST_NAME_AR) as full_name, emp.ISACTIVE, coalesce(org.ORG_CODE,'-') as orgcode , coalesce(emp.MANAGER_ID,'-') as mgrID ");
		sqlBuilder.append(
				" ,coalesce(emp.RANK_ID,'') as rankId, coalesce(emp.RANK_NAME, '') as rank, COALESCE(emp.JOB_NAME, '') as position, NATIONALITY_ID as natId, IQAMA_NO as iqamaNo");
		sqlBuilder.append(" from XXX_HR_INT_EMPLOYEE emp  ");
		sqlBuilder.append(
				" left join XXX_HR_INT_ORGANIZATION org on org.ORG_ID = emp.ACTUAL_ORG_ID where (NATIONAL_NO IS NOT NULL OR IQAMA_NO IS NOT NULL) ");

		PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			DataImportVO vo = mapEmployeeData(rs, managersList);
			vos.add(vo);
		}
		return vos;
	}
	
	private DataImportVO mapContractorsData(ResultSet rs, HashSet<String> managersList) throws SQLException {

		DataImportVO vo = new DataImportVO();

		vo.setEmployeeno(rs.getString(1));
		vo.setFirstname_ar(rs.getString(2));
		vo.setFathername_ar(rs.getString(3));
		vo.setGrandname_ar(rs.getString(4));
		vo.setFamilyname_ar(rs.getString(5));
		vo.setFirstname_en(rs.getString(6));
		vo.setFathername_en(rs.getString(7));
		vo.setFamilyname_en(rs.getString(8));
		vo.setHiredate_ministry(rs.getString(9));
		vo.setHiredate_govt(rs.getString(9));
		vo.setNational_no(rs.getString(10));
		vo.setNationality_name(rs.getString(11));
		log.info("setNationality_name:" + vo.getNationality_name() );
		vo.setEmail(rs.getString(12));
		vo.setMobile(rs.getString(13));
		vo.setLocationId(rs.getString(14));
		vo.setManager_name(rs.getString(15));
		vo.setAct_org_name(rs.getString(16));
		vo.setFullName(rs.getString(17));
		if (null != rs.getString(18)) {
			String status = rs.getString(18);
			if (status.trim().equals("Active")) {
				vo.setIsActive("0");
			}else {
				vo.setIsActive("1");
			}
		}
		vo.setOrgCode(rs.getString(19));
		vo.setPosition(rs.getString(21));
		vo.setIqamaNo(rs.getString(22));

		String managerId = rs.getString(20);

		if (managersList.contains(managerId)) {
			vo.setStrManagerId(managerId);
		} else {
			vo.setStrManagerId("");
		}

		return vo;
	}


	private DataImportVO mapEmployeeData(ResultSet rs, HashSet<String> managersList) throws SQLException {

		DataImportVO vo = new DataImportVO();

		vo.setEmployeeno(rs.getString(1));
		vo.setTitle(rs.getString(2));
		if (rs.getString(3).equals(MALE_AR)) {
			vo.setSex("M");
		} else {
			vo.setSex("F");
		}
		vo.setFirstname_ar(rs.getString(4));
		vo.setFathername_ar(rs.getString(5));
		vo.setGrandname_ar(rs.getString(6));
		vo.setFamilyname_ar(rs.getString(7));
		vo.setFirstname_en(rs.getString(8));
		vo.setFathername_en(rs.getString(9));
		vo.setGrandname_en(rs.getString(10));
		vo.setFamilyname_en(rs.getString(11));
		vo.setHiredate_ministry(rs.getString(12));
		vo.setHiredate_govt(rs.getString(13));
		vo.setNational_no(rs.getString(14));
		vo.setBirthdate(rs.getString(15));
		vo.setCountryOfBirth(rs.getString(16));
		vo.setPlaceOfBirth(rs.getString(17));
		vo.setBlood_type(rs.getString(18));
		vo.setMaritalStatus(rs.getString(19));
		vo.setNationality_name(rs.getString(20));
		log.info("setNationality_name:" + vo.getNationality_name() );
		vo.setReligion(rs.getString(21));
		vo.setEmail(rs.getString(22));
		vo.setMobile(rs.getString(23));
		vo.setExtno(rs.getString(24));
		vo.setOfficeno(rs.getString(25));
		vo.setLocationId(rs.getString(26));
		vo.setHire_decreeno(rs.getString(27));
		vo.setHire_decreedate(rs.getString(28));
		vo.setManager_name(rs.getString(29));
		vo.setAct_org_name(rs.getString(30));
		vo.setFullName(rs.getString(31));
		vo.setIsActive(rs.getString(32));
		vo.setOrgCode(rs.getString(33));
		vo.setGradeCode(rs.getString("rankId"));
		vo.setGrade(rs.getString("rank"));
		vo.setPosition(rs.getString("position"));
		vo.setNationalityId(rs.getString("natId"));
		vo.setIqamaNo(rs.getString("iqamaNo"));

		String managerId = rs.getString(34);
		
		if (managersList.contains(managerId)) {
			vo.setStrManagerId(managerId);
		} else {
			vo.setStrManagerId("");
		}

		return vo;
	}
	
	private List<DataImportVO> getContingentEmployeesList(Connection connection) throws SQLException {

		HashSet<String> managersList = getManagersList(connection);
                managersList.addAll(getManagersList1(connection));

		List<DataImportVO> vos = new ArrayList<DataImportVO>();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append(
				" select cont.EMP_NO, cont.TITLE, cont.gender, cont.FIRST_AR_NAME, cont.FATHER_AR_NAME, cont.GRANDFATHER_AR_NAME, cont.FAMILY_AR_NAME, ");
		sqlBuilder.append(
				" cont.FIRST_EN_NAME, cont.FATHER_EN_NAME, cont.GRANDFATHER_EN_NAME, cont.FAMILY_EN_NAME, ");
		sqlBuilder.append(
				" cont.HIRING_DATE_MINISTRY, cont.NATIONAL_ID, cont.BIRTH_DATE, cont.BLOOD_TYPE, cont.MARTIAL_STATUS, ");
		sqlBuilder.append(
				" cont.NATIONALITY, cont.RELIGION, cont.E_MAIL, cont.MOBILE,  cont.LOCATION_ID, ");
		sqlBuilder.append(
				" coalesce(cont.EMPLOYEE_FULL_NAME_AR, cont.FIRST_AR_NAME) as full_name, cont.ISACTIVE, coalesce(org.ORG_CODE,'-') as orgcode , coalesce(cont.MANAGER_CODE,'-') as mgrID, ");
		sqlBuilder.append(
				" cont.IQAMA_ID as iqamaNo ");
		sqlBuilder.append(" from XXX_HR_INT_CONTINGENT_WORKER cont  ");
		sqlBuilder.append(
				" left join XXX_HR_INT_ORGANIZATION org on org.ORG_ID = cont.organization_id where (cont.NATIONAL_ID IS NOT NULL OR cont.IQAMA_ID IS NOT NULL) ");

		PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			DataImportVO vo = mapContingentEmployeeData(rs, managersList);
			vos.add(vo);
		}
		return vos;
	}
	
	private DataImportVO mapContingentEmployeeData(ResultSet rs, HashSet<String> managersList) throws SQLException {

		DataImportVO vo = new DataImportVO();

		vo.setEmployeeno(rs.getString(1));
        if (rs.getString(2) != null && !rs.getString(2).equals("")) {
		vo.setTitle(rs.getString(2));
        }
		log.info("contingent EMP:" + rs.getString(1) );
        if (rs.getString(3) != null && !rs.getString(3).equals("")) {
			if (rs.getString(3).equals(MALE_AR)) {
				vo.setSex("M");
			} else {
				vo.setSex("F");
			}
        }
        if (rs.getString(4) != null && !rs.getString(4).equals("")) {
		vo.setFirstname_ar(rs.getString(4));
        }
        if (rs.getString(5) != null && !rs.getString(5).equals("")) {
		vo.setFathername_ar(rs.getString(5));
        }
        if (rs.getString(6) != null && !rs.getString(6).equals("")) {
		vo.setGrandname_ar(rs.getString(6));
        }
        if (rs.getString(7) != null && !rs.getString(7).equals("")) {
		vo.setFamilyname_ar(rs.getString(7));
        }
        if (rs.getString(8) != null && !rs.getString(8).equals("")) {
		vo.setFirstname_en(rs.getString(8));
        }
        if (rs.getString(9) != null && !rs.getString(9).equals("")) {
		vo.setFathername_en(rs.getString(9));
        }
        if (rs.getString(10) != null && !rs.getString(10).equals("")) {
		vo.setGrandname_en(rs.getString(10));
        }
        if (rs.getString(11) != null && !rs.getString(11).equals("")) {
		vo.setFamilyname_en(rs.getString(11));
        }
        if (rs.getString(12) != null && !rs.getString(12).equals("")) {
		vo.setHiredate_ministry(rs.getString(12));
		vo.setHiredate_govt(rs.getString(12));
        }
        if (rs.getString(13) != null && !rs.getString(13).equals("")) {
    		vo.setNational_no(rs.getString(13));
    		vo.setNationalityId("PQH_SA");
        }
        if (rs.getString(14) != null && !rs.getString(14).equals("")) {
		vo.setBirthdate(rs.getString(14));
        }
        if (rs.getString(15) != null && !rs.getString(15).equals("")) {
		vo.setBlood_type(rs.getString(15));
        }
        if (rs.getString(16) != null && !rs.getString(16).equals("")) {
		vo.setMaritalStatus(rs.getString(16));
        }
        if (rs.getString(17) != null && !rs.getString(17).equals("")) {
		vo.setNationality_name(rs.getString(17));
        }
		log.info("setNationality_name:" + vo.getNationality_name() );
        if (rs.getString(18) != null && !rs.getString(18).equals("")) {
		vo.setReligion(rs.getString(18));
        }
        if (rs.getString(19) != null && !rs.getString(19).equals("")) {
		vo.setEmail(rs.getString(19));
        }
        if (rs.getString(20) != null && !rs.getString(20).equals("")) {
		vo.setMobile(rs.getString(20));
        }
        if (rs.getString(21) != null && !rs.getString(21).equals("")) {
		vo.setLocationId(rs.getString(21));
        }
        if (rs.getString(22) != null && !rs.getString(22).equals("")) {
		vo.setFullName(rs.getString(22));
        }
        if (rs.getString(23) != null && !rs.getString(23).equals("")) {
		vo.setIsActive(rs.getString(23));
        }
        if (rs.getString(24) != null && !rs.getString(24).equals("")) {
		vo.setOrgCode(rs.getString(24));
        }
        if (rs.getString("iqamaNo") != null && !rs.getString("iqamaNo").equals("")) {
		vo.setIqamaNo(rs.getString("iqamaNo"));
        }

        if (rs.getString(25) != null && !rs.getString(25).equals("")) {
		String managerId = rs.getString(25);
		if (managersList.contains(managerId)) {
			vo.setStrManagerId(managerId);
		} else {
			vo.setStrManagerId("");
		}
        }

		return vo;
	}

	private HashSet<String> getManagersList(Connection connection) throws SQLException {

		HashSet<String> managers = new HashSet<String>();
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append(
				"select EMPLOYEE_ID from XXX_HR_INT_EMPLOYEE where EMPLOYEE_ID in ( select distinct MANAGER_ID from XXX_HR_INT_EMPLOYEE where MANAGER_ID is not null and MANAGER_ID <> '0' )");
		PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			managers.add(rs.getString(1));
		}
		ps.clearBatch();
		rs.close();

		return managers;
	}
	
	/**
         * Check whether the manager itself from contractors for permanent employee.
         * @param connection
         * @return
         * @throws SQLException
         */
	private HashSet<String> getManagersList1(Connection connection) throws SQLException {

          HashSet<String> managers = new HashSet<String>();
          StringBuilder sqlBuilder = new StringBuilder();
          sqlBuilder.append(
                          "select EMPL_CODE from HR_EMPLOYEES_OF_CONTRATORS where EMPL_CODE in ( select distinct MANAGER_ID from XXX_HR_INT_EMPLOYEE where MANAGER_ID is not null and MANAGER_ID <> '0' )");
          PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
          ResultSet rs = ps.executeQuery();
          while (rs.next()) {
                  managers.add(rs.getString(1));
          }
          ps.clearBatch();
          rs.close();

          return managers;
        }
	
	/**
	 * Get the Managers from Permanent Employees who are managing the contractors
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	private HashSet<String> getContractorsManagersList(Connection connection) throws SQLException {

		HashSet<String> managers = new HashSet<String>();
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append(
				" select EMPLOYEE_ID from XXX_HR_INT_EMPLOYEE where EMPLOYEE_ID in(select distinct MANAGER_CODE from HR_EMPLOYEES_OF_CONTRATORS  where MANAGER_CODE is not null and MANAGER_CODE <> '0' )");
		PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			managers.add(rs.getString(1));
		}
		ps.clearBatch();
		rs.close();

		return managers;
	}

	/**
	 * Check whether the manager itself from contractors
	 * @param connection
	 * @return
	 * @throws SQLException
	 */
	private HashSet<String> getContractorsManagersList1(Connection connection) throws SQLException {

		HashSet<String> managers = new HashSet<String>();
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append(
				" select EMPL_CODE from HR_EMPLOYEES_OF_CONTRATORS where EMPL_CODE in(select distinct MANAGER_CODE from HR_EMPLOYEES_OF_CONTRATORS  where MANAGER_CODE is not null and MANAGER_CODE <> '0' )");
		PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString());
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			managers.add(rs.getString(1));
		}
		ps.clearBatch();
		rs.close();

		return managers;
	}

}