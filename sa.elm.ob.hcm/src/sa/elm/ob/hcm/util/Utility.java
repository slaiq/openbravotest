package sa.elm.ob.hcm.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.model.common.geography.Country;

import sa.elm.ob.hcm.EHCMEmpScholarship;
import sa.elm.ob.hcm.EHCMEmpSupervisor;
import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.EmploymentInfo;

/**
 * @auther Qualian
 */
public class Utility {
  private static final Logger log4j = Logger.getLogger(Utility.class);

  /**
   * getActive Employment Info
   * 
   * @param employeeId
   * @return
   */
  public static EmploymentInfo getActiveEmployInfo(String employeeId) {
    return UtilityDAO.getActiveEmployInfo(employeeId);
  }

  /**
   * 
   * @param employeeId
   * @return
   */
  public static EmploymentInfo getRequestedEmployeeInfo(String employeeId, String requestDatepar) {
    return UtilityDAO.getRequestedEmployeeInfo(employeeId, requestDatepar);
  }

  /**
   * get saudi arabia country id using code
   * 
   * @param clientId
   * @return
   */

  public static Country getSaudiArabiaCountryId(String clientId) {
    return UtilityDAO.getSaudiArabiaCountryId(clientId);
  }

  /**
   * calculate the date using no of days with startdate
   * 
   * @param clientId
   * @param days
   * @param startDate
   *          - hijiri format(dd-MM-yyyy')
   * @return
   */
  public static Date calculateDateUsingDays(String clientId, String days, String startDate) {
    return UtilityDAO.calculateDateUsingDays(clientId, days, startDate);
  }

  /**
   * calculate the date using no of days with startdate
   * 
   * @param clientId
   * @param days
   * @param startDate-
   *          gregorian date format(yyyy-MM-dd)
   * @return
   */
  public static Date calDateUsingDaysWithGreDate(String clientId, int days, Date startDate) {
    return UtilityDAO.calDateUsingDaysWithGreDate(clientId, days, startDate);
  }

  /**
   * get Supervisor for Employee
   * 
   * @param employeeId
   * @param clientId
   * @return supervisor
   */
  public static EHCMEmpSupervisor getSupervisorforEmployee(String employeeId, String clientId) {
    return UtilityDAO.getSupervisorforEmployee(employeeId, clientId);
  }

  /**
   * calculate the days between start date and enddate
   * 
   * @param startDate
   *          hijiri format(dd-MM-yyyy')
   * @param endDate
   *          hijiri format(dd-MM-yyyy')
   * @return noofDays
   */
  public static int calculatetheDays(String startDate, String endDate) {
    return UtilityDAO.calculatetheDays(startDate, endDate);
  }

  /**
   * calculate the days between start date and enddate
   * 
   * @param startDate
   *          gregorian format(yyyy-MM-dd')
   * @param endDate
   *          gregorian format(yyyy-MM-dd')
   * @return noofDays
   */

  public static int caltheDaysUsingGreDate(Date startDate, Date endDate) {
    return UtilityDAO.caltheDaysUsingGreDate(startDate, endDate);
  }

  /**
   * get Employment Status for employee
   * 
   * @param emplinfo
   * @return
   */
  public static String getEmployeeEmploymentStatus(EmploymentInfo emplinfo) {
    return UtilityDAO.getEmployeeEmploymentStatus(emplinfo);
  }

  /**
   * 
   * @param employeeId
   * @param empScholarship
   * @return
   */
  public static JSONObject calculateEmpExtScholarship(String employeeId,
      EHCMEmpScholarship empScholarship) {
    return UtilityDAO.calculateEmpExtScholarship(employeeId, empScholarship);
  }

  /**
   * overlap with decision date
   * 
   * @param type
   * @param startDate
   * @param endDate
   * @param employeeId
   * @return error flag and error message
   */
  public static JSONObject overlapWithDecisionsDate(String type, String startDate, String endDate,
      String employeeId) {
    return UtilityDAO.overlapWithDecisionsDate(type, startDate, endDate, employeeId);
  }

  /**
   * 
   * @param employeeId
   * @return
   */
  public static EmploymentInfo getPromotionEmployee(String employeeId) {
    return UtilityDAO.getPromotionEmployeeInfo(employeeId);
  }

  /**
   * check decision overlapping
   * 
   * @param decisionType
   * @param startDate
   * @param endDate
   * @param employeeId
   * @param type
   * @param currentDecisionId
   * @return
   */
  public static JSONObject chkDecisionOverlap(String decisionType, String startDate, String endDate,
      String employeeId, String type, String currentDecisionId) {
    return UtilityDAO.chkDecisionOverlap(decisionType, startDate, endDate, employeeId, type,
        currentDecisionId);
  }

  public static String getBeforeDateInGreg(String hijiriDate) {
    return UtilityDAO.getBeforeDateInGreg(hijiriDate);
  }

  /**
   * Provides the work image logo as a BufferedImage object.
   * 
   * @param empId
   *          The employee id to display
   * 
   * @return The image requested
   * @throws IOException
   * @see #getImageLogo(String)
   */
  public static BufferedImage showEmployeeProfilePicture(String empId) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(UtilityDAO.getEmployeeImage(empId)));
  }

  /**
   * get no. of days for balance year
   * 
   * @param balance
   * @return
   * @throws IOException
   */
  public static BigDecimal balanceDaysInYear(String employeeId, String DecisionType)
      throws IOException {
    return UtilityDAO.getbalanceDaysInYear(employeeId, DecisionType);
  }

  /**
   * check decision date is overlapping with recent startdate in employmentinfo
   * 
   * @param employeeId
   * @param startDate
   * @param clientId
   * @return
   * @throws IOException
   */
  public static boolean chkOverlapDecisionStartdate(String employeeId, Date startDate,
      String clientId) throws IOException {
    return UtilityDAO.chkOverlapDecisionStartdate(employeeId, startDate, clientId);
  }

  /**
   * insert active employment info details while doing issue decision in all decision screens if
   * didnt change anything related to position department grade
   * 
   * @param empPerInfo
   *          - employee object
   * @param employInfo
   *          - insert employ info object
   * @param isExtraStep
   *          - extra step
   * @return
   * @throws Exception
   */
  public static EmploymentInfo insertActEmplymntInfoDetailsInIssueDecision(
      EhcmEmpPerInfo empPerInfo, EmploymentInfo employInfo, boolean isExtraStep, boolean status)
      throws Exception {
    return UtilityDAO.insertActEmplymntInfoDetailsInIssueDecision(empPerInfo, employInfo,
        isExtraStep, status);
  }

  /**
   * getting hiring employ info record
   * 
   * @param employeeId
   * @return
   * @throws Exception
   */
  public static EmploymentInfo getHiringEmployInfo(String employeeId) throws Exception {
    return UtilityDAO.getHiringEmployInfo(employeeId);
  }

}
