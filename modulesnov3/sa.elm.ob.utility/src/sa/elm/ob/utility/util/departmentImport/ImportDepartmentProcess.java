package sa.elm.ob.utility.util.departmentImport;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import sa.elm.ob.utility.util.ConnectionUtility;
import sa.elm.ob.utility.util.DataImportDAO;
import sa.elm.ob.utility.util.DataImportVO;
import sa.elm.ob.utility.util.Utility;

public class ImportDepartmentProcess extends DalBaseProcess {
	private static final Logger log = Logger.getLogger(ImportDepartmentProcess.class);
	private static final String DEFAULT_ORGANIZATION_TYPE_ID = "2";
	private static final String DEFAULT_CURRENCY_ID = "317";

	private ProcessLogger logger;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {

		logger = bundle.getLogger();
		Connection connection = null;
		OBError obError = null;
		
		try {
			OBContext.setAdminMode();

			log.info("Import Department Process Started.");
			logger.logln("Import Department Process Started.");

			Client client = OBContext.getOBContext().getCurrentClient();

			String clientId = client.getId();
			log.info("Client Id :" + clientId);


			String connectionId = (String) bundle.getParams().get(ConnectionUtility.HR_CONNECTION_ID);
			log.info("Oracle Connection Id from process parameters :" + connectionId);
			try {
				connection = ConnectionUtility.getHrConnection(connectionId);
			} catch (Exception e) {
				throw new Exception("Can't connect to HR database, Process will be terminated", e);
			}

			if (connection == null) {
				throw new Exception("Can't connect to HR database, Process will be terminated");
			}

			OrganizationType organizationType = Utility.getObject(OrganizationType.class, DEFAULT_ORGANIZATION_TYPE_ID);
			if (organizationType == null) {
				throw new Exception(" Can't get default Organization Type with Id:" + DEFAULT_ORGANIZATION_TYPE_ID + ", Process will be terminated");
			}
			
			Currency currency = Utility.getObject(Currency.class, DEFAULT_CURRENCY_ID);
			if (currency == null) {
				throw new Exception("Can't get default Currency with Id:" + DEFAULT_CURRENCY_ID + ", Process will be terminated");
			}
			List<DataImportVO> hrDepartements;
			try {
				hrDepartements = DataImportDAO.getHRDepartements(connection);
			} catch (Exception e) {
				throw new Exception("Error while getting departements details from HR database, Process will be terminated.", e);
			}

			int addSuccess = 0;
			int addFailure = 0;
			int newRecords = 0;
			int updateSuccess = 0;
			int updateFailure = 0;
			int total = hrDepartements.size();

			for (DataImportVO department : hrDepartements) {
				log.info("processing department: " + department.getOrgCode() + ", Client Id :" + clientId );
				try {
					Organization organization = DataImportDAO.getOrganization(department.getOrgCode(), clientId);
					
					if("INT".equalsIgnoreCase(department.getOrgInExType())) {
						if (organization == null) {
							log.info("New departement found, Department Id: " + department.getOrgCode() + ", Client Id" + clientId);
							newRecords++;
							int noOfCreated = DataImportDAO.addOrganization(organizationType, currency, department, hrDepartements, client);
							log.info("Create new departement Completed Successfully with code =" + department.getOrgCode() + ", and parent code =" + department.getParentOrgCode());
							addSuccess = addSuccess + noOfCreated;
						} else {
							try {
								DataImportDAO.updateOrganization(organization, department, client);
								log.info("Organization updated Successfully, Organization Id:" + organization.getSearchKey());
								updateSuccess++;
							} catch (Exception e) {
								log.error("Error while updating parent departement, Department Id: " + department.getOrgCode(), e);
								logger.logln("Error while updating parent departement, Department Id: " + department.getOrgCode() + ", Exception Message:" + e.getMessage());
								updateFailure++;
							}
						}
					} else {
						if (organization != null) {
							deactiveteOrganization(organization);
						}
					}
					
				} catch (Exception e) {
					addFailure++;
					log.error("Error while adding new departement, Department Id: " + department.getOrgCode(), e);
					logger.logln("Error while adding new departement, Department Id: " + department.getOrgCode() + ", Exception Message:" + e.getMessage());
				}
			}

			StringBuilder result = new StringBuilder();
			result.append("Total Records: " + total);
			result.append(", New Records: " + newRecords);
			result.append(", Add Success: " + addSuccess);
			result.append(", Add Failure: " + addFailure);
			result.append(", Update Success: " + updateSuccess);
			result.append(", Update Failure: " + updateFailure);

			obError = buildResult("Success", "Departement Import Process Completed Successfully.", result.toString());

			log.info("Import Department Completed :" + result);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			logger.logln(e.getMessage());
			if (e.getCause() != null) {
				obError = buildResult("Error", e.getMessage(), e.getCause().getMessage());
				logger.logln("Cause:" + e.getMessage());
			} else {
				obError = buildResult("Error", e.getMessage(), null);
			}
			bundle.setResult(obError);
			if (connection != null) {
				connection.close();
			}
			logger.logln("Import Department Completed with errors.");
			throw e;
		} finally {
			bundle.setResult(obError);
			if (connection != null) {
				connection.close();
			}
			OBContext.restorePreviousMode();
		}
		
		logger.logln("Import Department Completed Successfully.");
	}

	private void deactiveteOrganization(Organization organization) throws Exception {
		organization.setActive(false);
		try {
			OBDal.getInstance().save(organization);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			throw e;
		}
	}

	private OBError buildResult(String type, String title, String message) {
		OBError obError = new OBError();
		obError.setType(type);
		obError.setTitle(title);
		obError.setMessage(message);
		return obError;
	}
}