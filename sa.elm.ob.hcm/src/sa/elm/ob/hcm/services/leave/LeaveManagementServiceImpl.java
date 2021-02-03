package sa.elm.ob.hcm.services.leave;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Service;

import sa.elm.ob.hcm.EHCMAbsenceAttendance;
import sa.elm.ob.hcm.EHCMAbsenceType;
import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.ad_process.AbsenceAccrual.AbsenceAccrualDAOImpl;
import sa.elm.ob.hcm.dao.leave.ViewLeaveDAO;
import sa.elm.ob.hcm.dto.leave.LeaveRequestDTO;
import sa.elm.ob.hcm.dto.leave.RejoinLeaveRequestDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveAccrualDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveDTO;
import sa.elm.ob.hcm.selfservice.exceptions.BusinessException;
import sa.elm.ob.hcm.selfservice.exceptions.SystemException;
import sa.elm.ob.utility.util.DateUtils;

/**
 * Leave Management Service Implementation
 * 
 * @author Gopalakrishnan
 *
 */
@Service
public class LeaveManagementServiceImpl implements LeaveManagementService {
  private ViewLeaveDAO viewLeaveDAO;
  private static final String OPEN_BRAVO_DATE_FORMAT = "dd-MM-yyyy";

  @Override
  public List<ViewLeaveDTO> viewLeaves(String username) {
    // TODO Auto-generated method stub
    EhcmEmpPerInfo objEmployeeeInfo = viewLeaveDAO.getEmployeeProfileByUser(username);
    List<ViewLeaveDTO> viewLeaveDetails = getLeaveDetails(objEmployeeeInfo);
    return viewLeaveDetails;
  }

  /**
   * Get the list of leave Details
   * 
   * @param Employee
   *          Info
   * @return Employee leave Details List
   */
  private List<ViewLeaveDTO> getLeaveDetails(EhcmEmpPerInfo objEmployeeeInfo) {
    List<ViewLeaveDTO> leaveDetailList = mapLeaveDetails(
        objEmployeeeInfo.getEHCMAbsenceAttendanceList());
    return leaveDetailList;
    // TODO Auto-generated method stub

  }

  /**
   * Convert leave details domain to DTO's
   * 
   * @param Leave
   *          List
   * @return
   */
  private List<ViewLeaveDTO> mapLeaveDetails(
      List<EHCMAbsenceAttendance> ehcmAbsenceAttendanceList) {
    List<ViewLeaveDTO> viewLeaveList = new ArrayList<ViewLeaveDTO>();
    ViewLeaveDTO viewLeaveDTO = null;

    for (EHCMAbsenceAttendance absenceAttendance : ehcmAbsenceAttendanceList) {
      viewLeaveDTO = new ViewLeaveDTO();
      viewLeaveDTO.setAbsenceType(absenceAttendance.getEhcmAbsenceType().getJobGroupName());
      if (null != absenceAttendance.getStartDate()) {
        viewLeaveDTO.setStartDate(DateUtils.convertDateToString(OPEN_BRAVO_DATE_FORMAT,
            absenceAttendance.getStartDate()));
      }
      if (null != absenceAttendance.getEndDate()) {
        viewLeaveDTO.setEndDate(
            DateUtils.convertDateToString(OPEN_BRAVO_DATE_FORMAT, absenceAttendance.getEndDate()));
      }
      viewLeaveDTO.setPendingUser(absenceAttendance.getAuthorizedPerson());
      viewLeaveDTO.setStatus(absenceAttendance.getDecisionStatus());
      viewLeaveDTO.setPeriod(absenceAttendance.getAbsenceDays());
      if (null != absenceAttendance.getDecisionDate()) {
        viewLeaveDTO.setRequestDate(DateUtils.convertDateToString(OPEN_BRAVO_DATE_FORMAT,
            absenceAttendance.getDecisionDate()));
      }
      viewLeaveList.add(viewLeaveDTO);

    }
    // TODO Auto-generated method stub
    return viewLeaveList;
  }

  @Override
  public List<ViewLeaveAccrualDTO> viewLeavesAccrual(String username, String asOfDate)
      throws BusinessException, SystemException {
    // TODO Auto-generated method stub
    EhcmEmpPerInfo objEmployeeeInfo = viewLeaveDAO.getEmployeeProfileByUser(username);
    List<EHCMAbsenceType> absenceTypeList = viewLeaveDAO
        .getAbsenceType(objEmployeeeInfo.getClient().getId());
    List<ViewLeaveAccrualDTO> accuralLeaveDTO = mapAccuralLeave(objEmployeeeInfo, absenceTypeList,
        asOfDate); // Date format yyyy-MM-dd

    return accuralLeaveDTO;
  }

  /**
   * Convert Accural Details to DTO
   * 
   * @param objEmployeeeInfo
   * @param absenceTypeList
   * @return
   */
  private List<ViewLeaveAccrualDTO> mapAccuralLeave(EhcmEmpPerInfo objEmployeeeInfo,
      List<EHCMAbsenceType> absenceTypeList, String asOfDate) {
    List<ViewLeaveAccrualDTO> viewAccuralList = new ArrayList<ViewLeaveAccrualDTO>();
    ViewLeaveAccrualDTO viewLeaveAccuralDTO = null;
    AbsenceAccrualDAOImpl absenceAccrualDAOImpl = new AbsenceAccrualDAOImpl();
    for (EHCMAbsenceType absenceType : absenceTypeList) {
      JSONObject jsonObject = viewLeaveDAO.getAvailedAndAvailableDays(absenceType, objEmployeeeInfo,
          asOfDate);
      JSONObject jsonObjectDates = absenceAccrualDAOImpl.getStartDateAndEndDate(asOfDate,
          absenceType, objEmployeeeInfo.getId());
      viewLeaveAccuralDTO.setAbsenceType(absenceType.getJobGroupName());
      try {
        viewLeaveAccuralDTO.setBalance(Integer.valueOf(jsonObject.getString("availabledays")));
        viewLeaveAccuralDTO.setLeaves(Integer.valueOf(jsonObject.getString("availeddays")));
        viewLeaveAccuralDTO.setEmpName(objEmployeeeInfo.getName());
        viewLeaveAccuralDTO.setEmpNo(objEmployeeeInfo.getSearchKey());
        viewLeaveAccuralDTO.setStartDate(jsonObjectDates.getString("startdate"));
      } catch (NumberFormatException | JSONException e) {
        // TODO Auto-generated catch block
      }
      viewAccuralList.add(viewLeaveAccuralDTO);
    }
    // TODO Auto-generated method stub
    return viewAccuralList;
  }

  @Override
  public LeaveRequestDTO submitLeaveRequest(String username, LeaveRequestDTO leaveRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LeaveRequestDTO saveForLaterLeaveRequest(String username,
      LeaveRequestDTO leaveRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getAllOriginalDecisionNo(String username) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LeaveRequestDTO getLeaveRequestByOriginalDecisionNo(String orginalDecNo) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LeaveRequestDTO submitCutoffLeaveRequest(String username, String orginalDecNo,
      LeaveRequestDTO leaveRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LeaveRequestDTO saveForLaterCutoffLeaveRequest(String username, String orginalDecNo,
      LeaveRequestDTO leaveRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LeaveRequestDTO submitCancelLeaveRequest(String username, String orginalDecNo,
      LeaveRequestDTO leaveRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RejoinLeaveRequestDTO submitRejoinLeaveRequest(String username, String orginalDecNo,
      RejoinLeaveRequestDTO leaveRequestDTO) {
    // TODO Auto-generated method stub
    return null;
  }

}
