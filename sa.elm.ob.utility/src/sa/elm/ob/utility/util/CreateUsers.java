package sa.elm.ob.utility.util;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * @author Gopinagh.R
 * 
 */

public class CreateUsers extends DalBaseProcess {
  // private static final String MAIN_BRANCH_ID = "178";

  private static final Logger log = Logger.getLogger(CreateUsers.class);

  private ProcessLogger logger;
  private CreateUserStatus createUserStatus = new CreateUserStatus();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();

    logger.logln("Create user process started.");
    log.info("Create user process started.");

    OBError obError = null;

    try {

      OBContext.setAdminMode();

      if (!DataImportDAO.hasRole(DataImportDAO.ROLE_ID)) {
        throw new Exception(
            "Default user role not found, Please check dataset from Enterprise Module Management or contact system administrator.");
      }

      List<DataImportVO> employees = getHrEmployees(bundle);

      Role role = Utility.getObject(Role.class, DataImportDAO.ROLE_ID);

      createUserStatus.setAllRecords(employees.size());
      for (DataImportVO employee : employees) {
        handleEmployee(role, employee);
      }

      StringBuilder recordSummary = buildSummary(createUserStatus);

      obError = buildResult("Success", "Create user process Completed.", recordSummary.toString());
      bundle.setResult(obError);

      logger.logln("Create user execution summary: " + recordSummary);
      log.info("Create user execution summary: " + recordSummary);

      logger.logln("Create user process Completed.");
      log.info("Create user process Completed.");

    } catch (Exception e) {
      logger.logln(e.getMessage());
      log.error(e.getMessage(), e);
      if (e.getCause() != null) {
        logger.logln(e.getCause().getMessage());
        obError = buildResult("Error", e.getMessage(), e.getCause().getMessage());
      } else {
        obError = buildResult("Error", e.getMessage(), null);
      }
      bundle.setResult(obError);
      OBContext.restorePreviousMode();
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void handleEmployee(Role role, DataImportVO employee) {
    if (StringUtils.isNotEmpty(employee.getNational_no())) {
      BusinessPartner businessPartner = DataImportDAO.getBusinessPartner(employee.getEmployeeno());
      if (businessPartner != null) {
        log.info(
            "Employee found in Business Partner with Employee Id  :" + employee.getEmployeeno());
        createUserStatus.incrementFoundInBP();
        User user = DataImportDAO.getUser(employee);
        if (user == null) {
          processNewUser(role, employee, businessPartner);
        } else {
          processExistingUser(employee, user);
        }
      } else {
        logger.logln("Employee Not found in Business Partner and will not be created, Employee Id:"
            + employee.getEmployeeno());
        log.info("Employee Not found in Business Partner and will not be created, Employee Id:"
            + employee.getEmployeeno());
        createUserStatus.incrementNotFoundInBP();
      }
    } else {
      logger.logln("Employee has empty National ID and will not be Processed, Employee Id:"
          + employee.getEmployeeno());
      log.info("Employee has empty National ID and will not be Processed, Employee Id:"
          + employee.getEmployeeno());
      createUserStatus.incrementEmptyNID();
    }
  }

  private List<DataImportVO> getHrEmployees(ProcessBundle bundle) throws Exception {

    String connectionId = (String) bundle.getParams().get(ConnectionUtility.HR_CONNECTION_ID);
    Connection connection = null;
    try {
      connection = ConnectionUtility.getHrConnection(connectionId);
    } catch (Exception e) {
      throw new Exception("Can't Connect to Oracle database, Process terminated.", e);
    }
    List<DataImportVO> employees = new ArrayList<DataImportVO>();
    List<DataImportVO> contractors = new ArrayList<DataImportVO>();
    try {
      logger.logln("Get Employees from HR database.");
      log.info("Get Employees from HR database.");
      employees = DataImportDAO.getEmployees(connection);
      contractors = DataImportDAO.getContractedEmployees(connection);
      for (Iterator<DataImportVO> conIterator = contractors.iterator(); conIterator.hasNext();) {
        DataImportVO contr = conIterator.next();
        for (Iterator<DataImportVO> empIterator = employees.iterator(); empIterator.hasNext();) {
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
      employees.addAll(contractors);
      connection.close();
    } catch (Exception e) {
      if (connection != null) {
        connection.close();
      }
      throw new Exception("Can't get Employees Data from HR database.", e);
    }
    return employees;
  }

  private StringBuilder buildSummary(CreateUserStatus createUserStatus) {
    StringBuilder recordSummary = new StringBuilder();
    recordSummary
        .append("Total Employees fetched from HR database = " + createUserStatus.getAllRecords());
    recordSummary.append(", Found in business partener = " + createUserStatus.getFoundInBP());
    recordSummary.append(", Not in main branch = " + createUserStatus.getNotMainBranch());
    recordSummary.append(", Moved from main branch = " + createUserStatus.getMovedFromMainBranch());
    recordSummary
        .append(", Not Found in business partener = " + createUserStatus.getNotFoundInBP());
    recordSummary.append(", Empty National Id = " + createUserStatus.getEmptyNID());
    recordSummary.append(", Successfully Created = " + createUserStatus.getCreateSuccess());
    recordSummary.append(", Failed Create = " + createUserStatus.getCreateFail());
    recordSummary.append(", Successfully Updated = " + createUserStatus.getUpdateSuccess());
    recordSummary.append(", Failed Update = " + createUserStatus.getUpdateFail());
    return recordSummary;
  }

  private void processExistingUser(DataImportVO employee, User user) {
    log.info("Employee found in User table and will be Updated, Employee Id  :"
        + employee.getEmployeeno());
    try {
      log.info("Employee updated successfully, Employee Id  :" + employee.getEmployeeno());
      // if (MAIN_BRANCH_ID.equalsIgnoreCase(employee.getLocationId())) {
      DataImportDAO.updateUser(user, employee);
      createUserStatus.incrementUpdateSuccess();
      /*
       * } else { user.setActive(false); OBDal.getInstance().save(user);
       * OBDal.getInstance().flush(); OBDal.getInstance().commitAndClose();
       * log.info("User Moved from main branch:" + user.getUsername());
       * createUserStatus.incrementMovedFromMainBranch(); }
       */
    } catch (Exception e) {
      createUserStatus.incrementUpdateFail();
      logger.logln("Error while updating user, casue:" + e.getMessage());
      log.error("Error while updating user, casue:" + e.getMessage(), e);
    }
  }

  private void processNewUser(Role role, DataImportVO employee, BusinessPartner businessPartner) {
    log.info("Employee Not found in User table and will be Created, Employee Id:"
        + employee.getEmployeeno());
    try {
      // if (MAIN_BRANCH_ID.equalsIgnoreCase(employee.getLocationId())) {
      DataImportDAO.createUser(employee, role, businessPartner);
      log.info("Employee Created successfully, Employee Id  :" + employee.getEmployeeno());
      createUserStatus.incrementCreateSuccess();
      // } else {
      // createUserStatus.incrementNotMainBranch();
      // }
    } catch (Exception e) {
      createUserStatus.incrementCreateFail();
      logger.logln("Error while creating new user, casue:" + e.getMessage());
      log.error("Error while creating new user, casue:" + e.getMessage(), e);
    }
  }

  private OBError buildResult(String type, String title, String message) {
    OBError obError = new OBError();
    obError.setType(type);
    obError.setTitle(title);
    obError.setMessage(message);
    return obError;
  }

  private class CreateUserStatus {

    int allRecords = 0;
    int foundInBP = 0;
    int notFoundInBP = 0;
    int createSuccess = 0;
    int updateSuccess = 0;
    int createFail = 0;
    int updateFail = 0;
    int emptyNID = 0;
    int notMainBranch = 0;
    int movedFromMainBranch = 0;

    public int getAllRecords() {
      return allRecords;
    }

    public void incrementNotMainBranch() {
      notMainBranch++;
    }

    public void incrementMovedFromMainBranch() {
      movedFromMainBranch++;
    }

    public void incrementEmptyNID() {
      emptyNID++;
    }

    public void incrementNotFoundInBP() {
      notFoundInBP++;
    }

    public void incrementUpdateFail() {
      updateFail++;
    }

    public void incrementUpdateSuccess() {
      updateSuccess++;
    }

    public void incrementCreateFail() {
      createFail++;
    }

    public void incrementCreateSuccess() {
      createSuccess++;
    }

    public void incrementFoundInBP() {
      foundInBP++;
    }

    public void setAllRecords(int allRecords) {
      this.allRecords = allRecords;
    }

    public int getFoundInBP() {
      return foundInBP;
    }

    public int getNotFoundInBP() {
      return notFoundInBP;
    }

    public int getCreateSuccess() {
      return createSuccess;
    }

    public int getUpdateSuccess() {
      return updateSuccess;
    }

    public int getCreateFail() {
      return createFail;
    }

    public int getUpdateFail() {
      return updateFail;
    }

    public int getEmptyNID() {
      return emptyNID;
    }

    public int getNotMainBranch() {
      return notMainBranch;
    }

    public int getMovedFromMainBranch() {
      return movedFromMainBranch;
    }
  }
}