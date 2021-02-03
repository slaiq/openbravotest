package sa.elm.ob.hcm.services.manager;

import java.util.List;

import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.dto.employment.ViewEmplInfoDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveAccrualDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveDTO;
import sa.elm.ob.hcm.dto.manager.EmpBusinessMissionDTO;
import sa.elm.ob.hcm.dto.manager.EmpBusinessMissionManagerDTO;
import sa.elm.ob.hcm.dto.manager.EmpInfoManagerDTO;
import sa.elm.ob.hcm.dto.manager.NotificationsDTO;

/**
 * @author oalbader
 *
 */
@Service
public class ManagerSelfServiceServiceImpl implements ManagerSelfServiceService {

  @Override
  public List<EmpInfoManagerDTO> getAllEmployeesByManager(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ViewEmplInfoDTO> getEmployeeInformationByNumber(String empNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ViewLeaveAccrualDTO> getAllEmployeesLeaveAccuralByManagerUsername(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ViewLeaveAccrualDTO> getEmployeeLeavesAccuralByDateAndEmpNo(String mngUsername,
      String asOfDate, String empNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ViewLeaveDTO getEmployeeLeaveByOriginalDecNo(String originalDecNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<EmpBusinessMissionManagerDTO> getAllEmployeesBusinessMissionByManagerUsername(
      String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<EmpBusinessMissionManagerDTO> getEmployeeBusinessMissionsByDateAndEmpNo(
      String mngUsername, String asOfDate, String empNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EmpBusinessMissionDTO getEmployeeBusinessMissionByOriginalDecNo(String originalDecNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<NotificationsDTO> getNotificationsByStatus(String username, String status) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<NotificationsDTO> getNotificationsByUser(String username, String user) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<NotificationsDTO> getNotificationsByServiceType(String username, String serviceType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void approveRequest(String username, String letterNo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void rejectRequest(String username, String letterNo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void forwardRequest(String username, String letterNo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void returnForCorrection(String username, String letterNo) {
    // TODO Auto-generated method stub

  }

}
