package sa.elm.ob.hcm.ad_process.empBusinessMission;

import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.hcm.EHCMBusMissionSummary;
import sa.elm.ob.hcm.EHCMEmpBusinessMission;
import sa.elm.ob.hcm.EHCMMiscatEmployee;

/**
 * Interface for all Business Mission related DB Operations
 * 
 * @author divya -28-02-2018
 *
 */
public interface EmpBusinessMissionDAO {

  /**
   * update business mission status as "Issued"
   * 
   * @param empBusinessMission
   * @throws Exception
   */
  void updateEmpBusMissionStatus(EHCMEmpBusinessMission empBusinessMission) throws Exception;

  /**
   * insert business mission info
   * 
   * @param empBusinessMission
   * @param busMissionSummary
   * @param vars
   * @param decisionType
   * @throws Exception
   */
  void insertBusMissionSummary(EHCMEmpBusinessMission empBusinessMission,
      EHCMBusMissionSummary busMissionSummary, VariablesSecureApp vars, String decisionType)
      throws Exception;

  /**
   * update business mission info
   * 
   * @param empBusinessMission
   * @param busMissSummary
   * @param vars
   * @param decisiontype
   * @throws Exception
   */
  void updateBusMissionSummary(EHCMEmpBusinessMission empBusinessMission,
      EHCMBusMissionSummary busMissSummary, VariablesSecureApp vars, String decisiontype)
      throws Exception;

  /**
   * update old business mission as Inactive
   * 
   * @param empBusinessMission
   */
  void updateOldEmpBusinessMissionInAct(EHCMEmpBusinessMission empBusinessMission);

  /**
   * 
   * @param empBusinessMission
   */
  void removebusinessMissionActRecord(EHCMEmpBusinessMission empBusinessMission);

  /**
   * 
   * @param employeeId
   * @param originaldecId
   * @return
   */

  EHCMBusMissionSummary updatePaymentFlag(String employeeId, String originaldecId,
      boolean reactive);

  /**
   * get active business mission summary
   * 
   * @param empBusinessMission
   * @return
   * @throws Exception
   */
  EHCMBusMissionSummary getActEmpBusinessMissSummary(EHCMEmpBusinessMission empBusinessMission)
      throws Exception;

  /**
   * update mission balance after issued
   * 
   * @param misCatEmp
   * @param decisionType
   * @param empBusinessMission
   * @param vars
   * @throws Exception
   */

  void updateMissionBalance(EHCMMiscatEmployee misCatEmp, String decisionType,
      EHCMEmpBusinessMission empBusinessMission, VariablesSecureApp vars) throws Exception;

  /**
   * 
   * @param businessmissionId
   * @return
   * @throws Exception
   */

  boolean checkBusinessmissionAlreadyUsed(String businessmissionId) throws Exception;

  /**
   * 
   * @param employeeId
   * @param originaldecId
   * @throws Exception
   */
  void removeBusinessmissionSummary(String employeeId, String originaldecId) throws Exception;

  /**
   * 
   * @param employeeId
   * @param BusinessMissionObj
   * @return
   * @throws Exception
   */

  EHCMBusMissionSummary updateBusinessMissionSummary(String employeeId,
      EHCMEmpBusinessMission BusinessMissionObj) throws Exception;

}
