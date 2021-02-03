package sa.elm.ob.hcm.services.leave;

import java.util.List;

import sa.elm.ob.hcm.dto.leave.LeaveRequestDTO;
import sa.elm.ob.hcm.dto.leave.RejoinLeaveRequestDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveAccrualDTO;
import sa.elm.ob.hcm.dto.leave.ViewLeaveDTO;
import sa.elm.ob.hcm.selfservice.exceptions.BusinessException;
import sa.elm.ob.hcm.selfservice.exceptions.SystemException;

/**
 * Leave Management Service Interface
 * 
 * @author Gopalakrishnan
 * @author oalbader
 *
 */

public interface LeaveManagementService {

  /**
   * 
   * @param username
   * @return Leave Details
   */
  List<ViewLeaveDTO> viewLeaves(String username);

  /**
   * @param username
   * @return Leave accruals Details
   */
  List<ViewLeaveAccrualDTO> viewLeavesAccrual(String username, String asOfDate)
      throws BusinessException, SystemException;

  /**
   * @param username
   * @param leaveRequestDTO
   * @return
   */
  LeaveRequestDTO submitLeaveRequest(String username, LeaveRequestDTO leaveRequestDTO);

  /**
   * @param username
   * @param leaveRequestDTO
   * @return
   */
  LeaveRequestDTO saveForLaterLeaveRequest(String username, LeaveRequestDTO leaveRequestDTO);

  /**
   * Get All Original Decision Numbers belong to given username
   * 
   * @param username
   * @return
   */
  List<String> getAllOriginalDecisionNo(String username);

  /**
   * Retrieve Leave Request Info by original decision number
   * 
   * @param orginalDecNo
   * @return
   */
  LeaveRequestDTO getLeaveRequestByOriginalDecisionNo(String orginalDecNo);

  /**
   * 
   * @param username
   * @param leaveRequestDTO
   * @return
   */
  LeaveRequestDTO submitCutoffLeaveRequest(String username, String orginalDecNo,
      LeaveRequestDTO leaveRequestDTO);

  /**
   * @param username
   * @param leaveRequestDTO
   * @return
   */
  LeaveRequestDTO saveForLaterCutoffLeaveRequest(String username, String orginalDecNo,
      LeaveRequestDTO leaveRequestDTO);

  /**
   * @param username
   * @param leaveRequestDTO
   * @return
   */
  LeaveRequestDTO submitCancelLeaveRequest(String username, String orginalDecNo,
      LeaveRequestDTO leaveRequestDTO);

  /**
   * @param username
   * @param leaveRequestDTO
   * @return
   */
  RejoinLeaveRequestDTO submitRejoinLeaveRequest(String username, String orginalDecNo,
      RejoinLeaveRequestDTO leaveRequestDTO);

}
