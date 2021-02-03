package sa.elm.ob.hcm.services.manager;

import java.util.List;

import sa.elm.ob.hcm.dto.employment.ViewEmplInfoDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveAccrualDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveDTO;
import sa.elm.ob.hcm.dto.manager.EmpBusinessMissionDTO;
import sa.elm.ob.hcm.dto.manager.EmpBusinessMissionManagerDTO;
import sa.elm.ob.hcm.dto.manager.EmpInfoManagerDTO;
import sa.elm.ob.hcm.dto.manager.NotificationsDTO;

/**
 * Manager Self Service Interface
 * 
 * @author oalbader
 *
 */
public interface ManagerSelfServiceService {

  /**
   * @param username
   * @return
   */
  List<EmpInfoManagerDTO> getAllEmployeesByManager(String username);

  /**
   * @param empNo
   * @return
   */
  List<ViewEmplInfoDTO> getEmployeeInformationByNumber(String empNo);

  /**
   * @param username
   * @return
   */
  List<ViewLeaveAccrualDTO> getAllEmployeesLeaveAccuralByManagerUsername(String username);

  /**
   * @param mngUsername
   * @param asOfDate
   * @param empNo
   * @return
   */
  List<ViewLeaveAccrualDTO> getEmployeeLeavesAccuralByDateAndEmpNo(String mngUsername,
      String asOfDate, String empNo);

  /**
   * @param originalDecNo
   * @return
   */
  ViewLeaveDTO getEmployeeLeaveByOriginalDecNo(String originalDecNo);

  /**
   * @param username
   * @return
   */
  List<EmpBusinessMissionManagerDTO> getAllEmployeesBusinessMissionByManagerUsername(
      String username);

  /**
   * @param mngUsername
   * @param asOfDate
   * @param empNo
   * @return
   */
  List<EmpBusinessMissionManagerDTO> getEmployeeBusinessMissionsByDateAndEmpNo(String mngUsername,
      String asOfDate, String empNo);

  /**
   * @param originalDecNo
   * @return
   */
  EmpBusinessMissionDTO getEmployeeBusinessMissionByOriginalDecNo(String originalDecNo);

  /**
   * @param username
   * @param status
   * @return
   */
  List<NotificationsDTO> getNotificationsByStatus(String username, String status);

  /**
   * @param username
   * @param user
   * @return
   */
  List<NotificationsDTO> getNotificationsByUser(String username, String user);

  /**
   * @param username
   * @param serviceType
   * @return
   */
  List<NotificationsDTO> getNotificationsByServiceType(String username, String serviceType);

  /**
   * @param username
   * @param letterNo
   */
  void approveRequest(String username, String letterNo);

  /**
   * @param username
   * @param letterNo
   */
  void rejectRequest(String username, String letterNo);

  /**
   * @param username
   * @param letterNo
   */
  void forwardRequest(String username, String letterNo);

  /**
   * @param username
   * @param letterNo
   */
  void returnForCorrection(String username, String letterNo);

}
