package sa.elm.ob.utility.ad_process.Forward;

import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.Window;

import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;

public interface ForwardRequestMoreInfoDAO {

  /**
   * get all user list
   * 
   * @param clientId
   * @param searchTerm
   * @param pagelimit
   * @param page
   * @param loggedUserId
   * @param ForwardedUserId
   * @param inpRecordId
   * @param windowReference
   * @param docType
   * @param roleId
   * @param forwardRMI
   * @return User list
   * @throws Exception
   */
  JSONObject getUserList(String clientId, String searchTerm, int pagelimit, int page,
      String loggedUserId, String getUserList, String inpRecordId, String windowReference,
      String docType, String roleId, String forwardRMI) throws Exception;

  /**
   * get roles list based on given user
   * 
   * @param userId
   * @return User's Roles
   * @throws Exception
   */
  JSONObject getUserRole(String clientId, String searchTerm, int pagelimit, int page,
      String loggedUserId, String windowReference, String inpRecordId) throws Exception;

  /**
   * Insert or update record in EutForwardReqMoreInfo table
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param windowReference
   * @param docType
   * @return EutForwardReqMoreInfo Id
   * @throws Exception
   */
  public EutForwardReqMoreInfo insertOrUpdateRecord(HttpServletRequest request,
      VariablesSecureApp vars, String inpRecordId, String windowReference, String docType)
      throws Exception;

  /**
   * Insert or update record in EutReqMoreInfo table
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param recordType
   * @param windowReference
   * @return
   * @throws Exception
   */

  public EutForwardReqMoreInfo insertOrUpdateRecordRMI(HttpServletRequest request,
      VariablesSecureApp vars, String inpRecordId, String recordType, String windowReference,
      String docType) throws Exception;

  /**
   * Insert record in eut_nextrole_line with 'from user' and 'forward_rmi' id
   *
   * @param request
   * @param vars
   * @param inpRecordId
   * @param forwardrmi
   * @return EutNextRoleLine Id
   * @throws Exception
   */
  public EutNextRoleLine insertEutNextRoleLine(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, EutForwardReqMoreInfo forwardrmi, String documenttype) throws Exception;

  /**
   * set old records next role - Finance.
   *
   * @param inpRecordId
   * @param forwardrmi
   * @param windowReference
   * @return
   * @throws Exception
   */
  public HashMap<String, String> insertFinanceEutNextRoleLine(String inpRecordId,
      EutForwardReqMoreInfo forwardrmi, String windowReference) throws Exception;

  /**
   * Insert record in eut_nextrole_line with 'from user' and 'forward_rmi' id for RMI
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param forwardrmi
   * @param documenttype
   * @return
   * @throws Exception
   */
  public EutNextRoleLine insertEutNextRoleLineRMI(HttpServletRequest request,
      VariablesSecureApp vars, String inpRecordId, EutForwardReqMoreInfo forwardrmi,
      String documenttype) throws Exception;

  /**
   * Insert Action History for Forward
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param documenttype
   * @param forwardRevoketype
   * @param forwardrmi
   * @param revoke
   * @throws Exception
   */
  public void insertActionHistory(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, String documenttype, String forwardRevoketype,
      EutForwardReqMoreInfo forwardrmi, boolean revoke) throws Exception;

  /**
   * Insert action history for RMI Revoke
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param documenttype
   * @param forwardRevoketype
   * @param forwardrmi
   * @throws Exception
   */

  public void insertActionHistoryRMIRevoke(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, String documenttype, String forwardRevoketype,
      EutForwardReqMoreInfo forwardrmi) throws Exception;

  /**
   * Insert Action History for Request More Information
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param forwardrmi
   * @param documenttype
   * @param forwardRevoketype
   * @param isOriginalUser
   * @throws Exception
   */

  public void insertActionHistoryRMI(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, EutForwardReqMoreInfo forwardrmi, String documenttype,
      String forwardRevoketype, Boolean isOriginalUser) throws Exception;

  /**
   * Forward Revoke- Update status 'DR' in forward_rmi record - SCM
   * 
   * @param docType
   * @param vars
   * @param inpRecordId
   * @param comments
   * @return
   * @throws Exception
   */
  public EutForwardReqMoreInfo revokeForwardUpdateRecord(String docType, VariablesSecureApp vars,
      String inpRecordId, String comments, boolean revoke) throws Exception;

  /**
   * Forward Revoke- Update status 'DR' in forward_rmi record - Finance
   * 
   * @param docType
   * @param inpRecordId
   * @return
   * @throws Exception
   */
  public String revokeFinanceForwardUpdateRecord(String docType, String inpRecordId)
      throws Exception;

  /**
   * Forward Revoke- Update status 'DR' in Request More Information record
   * 
   * @param docType
   * @param vars
   * @param inpRecordId
   * @param comments
   * @return
   * @throws Exception
   */
  public EutForwardReqMoreInfo revokeForwardUpdateRecordRMI(String windowReference,
      VariablesSecureApp vars, String inpRecordId, String comments, boolean isrevoke,
      String docType, boolean revoke, String action) throws Exception;

  /**
   * Forward Revoke - delete line from eut_next_role_ln
   * 
   * @param request
   * @param vars
   * @param inpRecordId
   * @param documenttype
   * @return
   * @throws Exception
   */
  public void revokeForwardDeleteEutNextRoleLine(HttpServletRequest request,
      EutForwardReqMoreInfo forwardrmi, VariablesSecureApp vars, String inpRecordId,
      String documenttype) throws Exception;

  /**
   * RMI Revoke - delete line from eut_next_role_ln
   * 
   * @param request
   * @param forwardrmi
   * @param vars
   * @param inpRecordId
   * @param documenttype
   * @throws Exception
   */
  public void revokeForwardDeleteEutNextRoleLineRMI(HttpServletRequest request,
      EutForwardReqMoreInfo forwardrmi, VariablesSecureApp vars, String inpRecordId,
      String documenttype) throws Exception;

  /**
   * set null for forward_rmi id in all transaction screens - SCM.
   * 
   * @param inpRecordId
   * @param documenttype
   * @throws Exception
   */
  public void revokeRemoveForwardRmiFromWindows(String inpRecordId, String documenttype)
      throws Exception;

  /**
   * set null for forward_rmi id in all transaction screens - Finance.
   * 
   * @param inpRecordId
   * @param documenttype
   * @throws Exception
   */
  public void revokeFinanceRemoveForwardRmi(String inpRecordId, String documenttype)
      throws Exception;

  /**
   * give role access
   * 
   * @param clientId
   * @param userId
   * @param forwardrmiId
   * @param doctype
   * @param windowId
   * @param conn
   * @throws Exception
   */
  public void giveRoleAccess(String clientId, String userId, String forwardrmiId, String doctype,
      String windowId, Connection conn) throws Exception;

  /**
   * remove role access
   * 
   * @param clientId
   * @param forwardrmiId
   * @param conn
   * @throws Exception
   */
  public void removeRoleAccess(String clientId, String forwardrmiId, Connection conn)
      throws Exception;

  /**
   * 
   * @param inpRecordId
   * @param windowReference
   * @return
   * @throws Exception
   */

  public EutForwardReqMoreInfo getRmiIdReqRes(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Returns the next role - SCM
   * 
   * @param inpRecordId
   * @param windowReference
   * @return
   * @throws Exception
   */
  public EutNextRole getNextRole(String inpRecordId, String windowReference) throws Exception;

  /**
   * Returns the next role - Finance
   * 
   * @param inpRecordId
   * @param windowReference
   * @return
   * @throws Exception
   */
  public EutNextRole getFinanceNextRole(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Returns the next role lines list
   * 
   * @param eutNextRoleHeader
   * @return
   * @throws Exception
   */

  public List<EutNextRoleLine> getNextRoleLine(String eutNextRoleHeader) throws Exception;

  /**
   * alert process
   * 
   * @param clientId
   * @param alertWindowType
   * @param forReqMoreInfo
   * @param Forward_or_Revoke
   * @param Lang
   * @param UserName
   * @param WindowName
   * @param isOriginalUser
   * @param request
   * @return
   * @throws Exception
   */
  public void alertprocess(String clientId, String alertWindowType,
      EutForwardReqMoreInfo forReqMoreInfo, String Forward_or_Revoke, String Lang, String UserName,
      String WindowName, String docType, Boolean isOriginalUser, HttpServletRequest request,
      String windowReference) throws Exception;

  /**
   * Returns the AlertRuleId of the AlertProcessType
   * 
   * @param clientId
   * @param alertWindowType
   * @param isFinance
   * @return
   * @throws Exception
   */
  public String getAlertRuleId(String clientId, String alertWindowType, Boolean isFinance)
      throws Exception;

  /**
   * Returns Document No. for alert process - Finance
   * 
   * @param forReqMoreInfo
   * @return
   * @throws Exception
   */
  public HashMap<String, String> alertFinanceprocess(EutForwardReqMoreInfo forReqMoreInfo)
      throws Exception;

  /**
   * get user and role from which record is forwarded
   * 
   * @param eutNextRole
   * @param currentUser
   * @param currentRole
   * @param clientId
   * @return
   * @throws Exception
   */
  JSONObject getForwardFromUserFromRole(EutNextRole eutNextRole, String currentUser,
      String currentRole, String clientId) throws Exception;

  /**
   * check whether eut_forward_reqmoreinfo_id is null in eut_next_role_line table
   * 
   * @param eutNextRole
   * @param currentRole
   * @param clientId
   * @return
   * @throws Exception
   */
  public boolean isForwardRMIIdIsNull(EutNextRole eutNextRole, String currentRole, String clientId)
      throws Exception;

  /**
   * give role access for RMI
   * 
   * @param clientId
   * @param userId
   * @param forwardrmiId
   * @param doctype
   * @param windowId
   * @param conn
   * @throws Exception
   */
  public void giveReqMoreInfoRoleAccess(String clientId, String userId, String forwardrmiId,
      String doctype, String windowId, Connection conn) throws Exception;

  /**
   * Remove role access for RMI
   * 
   * @param clientId
   * @param forwardrmiId
   * @param conn
   * @throws Exception
   */
  public void removeReqMoreInfoRoleAccess(String clientId, String forwardrmiId, Connection conn)
      throws Exception;

  /**
   * Get department from business partner
   * 
   * @param userId
   * @return
   * @throws Exception
   */
  public JSONObject getUserDepartment(String userId) throws Exception;

  /**
   * Get position from business partner
   * 
   * @param userId
   * @return
   * @throws Exception
   */
  public JSONObject getUserPosition(String userId) throws Exception;

  /**
   * set null for forward_rmi id in all transaction screens
   * 
   * @param inpRecordId
   * @param documenttype
   * @throws Exception
   */
  public void revokeRemoveRmiFromWindows(String inpRecordId, String documenttype) throws Exception;

  /**
   * get preference object
   * 
   * @param property
   * @param isListProperty
   * @param client
   * @param org
   * @param user
   * @param role
   * @param window
   * @param exactMatch
   * @param checkWindow
   * @param activeFilterEnabled
   * @return Preference Object
   * @throws Exception
   */
  public List<Preference> getPreferences(String property, boolean isListProperty, String client,
      String org, String user, String role, String window, boolean exactMatch, boolean checkWindow,
      boolean activeFilterEnabled) throws Exception;

  /**
   * Get Request More Information from transaction screen - SCM.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public EutForwardReqMoreInfo getReqMoreInfo(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Get Request More Information from transaction screen - Finance.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public EutForwardReqMoreInfo getFinanceReqMoreInfo(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Set Request More Information id in transaction screen - SCM.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public void setReqMoreInfoID(String inpRecordId, String windowReference,
      EutForwardReqMoreInfo rmi) throws Exception;

  /**
   * Set Request More Information id in transaction screen - Finance.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public void setFinanceReqMoreInfoID(String inpRecordId, String windowReference,
      EutForwardReqMoreInfo rmi) throws Exception;

  /**
   * Get client id and organization id from transaction screen - SCM.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public JSONObject getClientIdOrgId(String inpRecordId, String docType) throws Exception;

  /**
   * Get client id and organization id from transaction screen - Finance.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public HashMap<String, String> getClientIdOrgIdFinance(String inpRecordId, String docType)
      throws Exception;

  /**
   * Get Forward obj from transaction screen - SCM.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public EutForwardReqMoreInfo getForwardObj(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Get Forward obj from transaction screen - Finance.
   * 
   * @param inpRecordId
   * @param windowReference
   * @throws Exception
   */
  public EutForwardReqMoreInfo getFinanceForwardObj(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Get status as "Draft" for forward record
   * 
   * @param forwardRequestMoreInfo
   * @return
   * @throws Exception
   */
  public EutForwardReqMoreInfo setForwardStatusAsDraft(EutForwardReqMoreInfo forwardRequestMoreInfo)
      throws Exception;

  /**
   * Get default role from user.
   * 
   * @param userId
   * @param windowReference
   * @param inpRecordId
   * @throws Exception
   */
  public JSONObject getDefaultRole(String userId, String windowReference, String inpRecordId)
      throws Exception;

  /**
   * 
   * @param inpRecordId
   * @return
   * @throws Exception
   */
  public List<Preference> getPreferenceObj(String clientId, boolean isRMI) throws Exception;

  public JSONObject getFromuserAndFromRoleWhileApprove(EutNextRole nextRoleObj, String userId,
      String roleId, String clientId, String orgId, String documentType, boolean isDummyRole,
      boolean isDirectApproval) throws Exception;

  /**
   * getting translation window name
   * 
   * @param windowObj
   * @param lang
   * @return
   * @throws Exception
   */
  public String getWindowTranslationName(Window windowObj, String lang) throws Exception;

  /**
   * 
   * @param recordId
   * @param alertWindow
   * @param alertRuleId
   * @param objUser
   * @param clientId
   * @param approve_reject
   * @param documentNo
   * @param Lang
   * @throws Exception
   */

  public void getAlertForForwardedUser(String recordId, String alertWindow, String alertRuleId,
      User objUser, String clientId, String approve_reject, String documentNo, String Lang,
      String roleId, EutForwardReqMoreInfo forwardReqMoreInfo, String doctype,
      HashMap<String, String> alertReceiversMap) throws Exception;

  /**
   * 
   * @param alertWindow
   * @throws Exception
   */
  public String getAlertForForwardedUserFinance(String approve_reject, String alertWindow)
      throws Exception;

  /**
   * get forwarded/delegated from user and from role
   * 
   * @param recordId
   * @param clientId
   * @param doctype
   * @param orgId
   * @return forwarded/delegated from user and from role
   * @throws Exception
   */
  public JSONObject getFrowardedOrDelegatedFromUserFromRole(String recordId, String clientId,
      String doctype, String orgId) throws Exception;

  /**
   * Check whether the RMI is already revoked
   * 
   * @param inpRecordId
   * @param clientId
   * @param UserId
   * @param roleId
   * @param orgId
   * @param docType
   * @return
   * @throws Exception
   */

  public Boolean isRmiRevoked(String inpRecordId, String clientId, String UserId, String roleId,
      String orgId, String docType) throws Exception;

  /**
   * Check whether the response is valid or not, if the user tries to respond without refreshing the
   * page
   * 
   * @param inpRecordId
   * @param clientId
   * @param userId
   * @param roleId
   * @param orgId
   * @param docType
   * @param windowReference
   * @param isResponse
   * @return
   * @throws Exception
   */
  public Boolean responsePreValidation(String inpRecordId, String clientId, String userId,
      String roleId, String orgId, String docType, String windowReference, Boolean isResponse)
      throws Exception;

  /**
   * Check whether the current user is delegated by the next approver
   * 
   * @param currentDate
   * @param docType
   * @param nextRoleId
   * @param nextUserId
   * @param currentRoleId
   * @param currentUserId
   * @return
   * @throws Exception
   */
  public Boolean checkIsDelegatedUserOrRole(Date currentDate, String docType, String nextRoleId,
      String nextUserId, String currentRoleId, String currentUserId) throws Exception;

  /**
   * 
   * @param currentDate
   * @param docType
   * @param nextRoleId
   * @param nextUserId
   * @param currentRoleId
   * @param currentUserId
   * @return
   * @throws Exception
   */
  public Boolean checkFwdandRmiDelegatedUserOrRole(Date currentDate, String docType,
      String nextRoleId, String nextUserId, String currentRoleId, String currentUserId)
      throws Exception;

  /**
   * Returns true if forward is valid when user clicks without refreshing the page
   * 
   * @param inpRecordId
   * @param clientId
   * @param userId
   * @param roleId
   * @param orgId
   * @param docType
   * @param windowReference
   * @return
   * @throws Exception
   */
  public Boolean forwardPreValidation(String inpRecordId, String clientId, String userId,
      String roleId, String orgId, String docType, String windowReference) throws Exception;

  /**
   * Returns true if the user is a valid approver
   * 
   * @param forwardId
   * @param userId
   * @param roleId
   * @param docType
   * @return
   * @throws Exception
   */
  public Boolean allowApproveReject(EutForwardReqMoreInfo forwardId, String userId, String roleId,
      String docType) throws Exception;

  /**
   * Returns the previous forwarded/rmi user Id
   * 
   * @param recordId
   * @param forwardRmiId
   * @return
   * @throws Exception
   */
  public String checkIsNestedForwardRmi(String recordId, String forwardRmiId) throws Exception;

  /**
   * Returns true if the role selected have the organization access for the current record
   * 
   * @param inpRecordId
   * @param windowReference
   * @param roleId
   * @return
   * @throws Exception
   */
  public Boolean checkFrwdRmiToRoleHaveOrgAccess(String inpRecordId, String windowReference,
      String roleId) throws Exception;

  /**
   * Returns the Error Message for the validations in current transaction
   * 
   * @param windowReference
   * @param inpRecordId
   * @return
   * @throws Exception
   */
  public String checkTransactionValidation(String windowReference, String inpRecordId)
      throws Exception;

  /**
   * 
   * @param currentDate
   * @param docType
   * @param currentRoleId
   * @param currentUserId
   * @param delegatedFromRole
   * @param delegatedFromUser
   * @return
   * @throws Exception
   */
  public Boolean checkSelectedUserisDelegatedorNot(Date currentDate, String docType,
      String currentRoleId, String currentUserId, String delegatedFromRole,
      String delegatedFromUser) throws Exception;

  /**
   * 
   * @param UserId
   * @return
   * @throws Exception
   */
  public String getUserName(String UserId) throws Exception;

  /**
   * 
   * @param eutNextRole
   * @param clientId
   * @return
   * @throws Exception
   */
  public String getOriginalNextRoleLine(String eutNextRole, String clientId) throws Exception;

  /**
   * get previous forward obj
   * 
   * @param requestId
   * @param userId
   * @param roleId
   * @param docType
   * @return previous forward obj
   * @throws Exception
   */
  public EutForwardReqMoreInfo getPreviousForwardObj(String requestId, String userId, String roleId,
      String docType) throws Exception;

  /**
   * check Current role and user is same with recent forwarded fromUser and fromRole - for check
   * Forward Revoke can possible by current user role or not
   * 
   * @param currentForwardId
   * @param userId
   * @param roleId
   * @param docType
   * @return True or False
   * @throws Exception
   */
  public Boolean isCurrentRoleandUserCanRevoke(String currentForwardId, String userId,
      String roleId, String docType) throws Exception;

  /**
   * get delegater from user and role based on session user
   * 
   * @param userId
   * @param roleId
   * @param documentType
   * @return
   * @throws Exception
   */
  public JSONObject getDelegateFromUserandRole(String userId, String roleId, String documentType)
      throws Exception;

  /**
   * get previous rmi record if response is occuring in chain level
   * 
   * @param userId
   * @param roleId
   * @param responseObj
   * @return
   * @throws Exception
   */

  public EutForwardReqMoreInfo getPreviousRMIRecordBasedOnRequestId(String userId, String roleId,
      EutForwardReqMoreInfo responseObj, String documentType, boolean isrevoke) throws Exception;

  /**
   * get rmi request id if response is giving for the request
   * 
   * @param userId
   * @param roleId
   * @param requestId
   * @return
   * @throws Exception
   */
  public EutForwardReqMoreInfo getRMIRequestId(String userId, String roleId, String requestId,
      String doctype) throws Exception;

  /**
   * check user is rmi revoker or not
   * 
   * @param rmi
   * @param userId
   * @param roleId
   * @param documentType
   * @return
   * @throws Exception
   */
  public Boolean checkRMIRevokeUser(EutForwardReqMoreInfo rmi, String userId, String roleId,
      String documentType) throws Exception;

  public HashMap<String, String> getNextRoleLineList(EutNextRole nextrole, String docType)
      throws Exception;

  /**
   * get rmi request users and roles
   * 
   * @param rmi
   * @param isgetRole
   * @param inpRecord
   * @param userId
   * @return
   * @throws Exception
   */

  public JSONObject getResponseUserAndRoles(EutForwardReqMoreInfo rmi, boolean isgetRole,
      String inpRecord, String userId) throws Exception;

  /**
   * set rmi request status as "DR" and processed "Y"
   * 
   * @param rmi
   * @param userId
   * @param roleId
   * @return
   * @throws Exception
   */
  public EutForwardReqMoreInfo setRMIRecordStatusAsDraft(EutForwardReqMoreInfo rmi, String userId,
      String roleId) throws Exception;

  /**
   * Returns delegated from user, from role, to user and to role.
   * 
   * @param currentDate
   * @param docType
   * @param nextRoleId
   * @param nextUserId
   * @param currentRoleId
   * @param currentUserId
   * @return
   * @throws Exception
   */
  public JSONObject isDelegatedFromToUserRole(Date currentDate, String docType, String nextRoleId,
      String nextUserId, String currentRoleId, String currentUserId) throws Exception;

  /**
   * get rmi request detail message
   * 
   * @param rmi
   * @param inpRecord
   * @param userId
   * @param roleId
   * @return
   * @throws Exception
   */
  public String getRmiRequestDetail(EutForwardReqMoreInfo rmi, String inpRecord, String userId,
      String roleId) throws Exception;

  /**
   * insert approval history for ManualEncumbrance
   * 
   * @param data
   * @throws Exception
   */
  public void InsertEncumbranceApprovalHistory(JSONObject data) throws Exception;

  public HashMap<String, String> getFinanceCreatedUser(String inpRecordId, String windowReference)
      throws Exception;

  /**
   * Checks whether forward/rmi is done by delegated user using EutNextNole
   * 
   * @param inpRecordId
   * @param windowReference
   * @param docType
   * @param currentDate
   * @param userId
   * @param roleId
   * @return
   * @throws Exception
   */
  public JSONObject delegatedFromUserValidation(String inpRecordId, String windowReference,
      String docType, String clientId, Date currentDate, String userId, String roleId)
      throws Exception;

  /**
   * Checks whether forward/rmi is done by delegated user using Session User & Role
   * 
   * @param inpRecordId
   * @param windowReference
   * @param userId
   * @param roleId
   * @return
   * @throws Exception
   */
  public JSONObject isDelegatedUser(String inpRecordId, String windowReference, String userId,
      String roleId, String forwardOrRmi) throws Exception;

  /**
   * 
   * @param string
   * @param vars
   * @param id
   * @param poWindowId
   * @param requester_user_id
   * @param requester_role_id
   * @return
   */

  String checkAndReturnTemporaryPreference(String preferece_name, String roleId, String userId,
      String clientId, String id, String poWindowId, String requester_user_id,
      String requester_role_id);

  /**
   * 
   * @param record
   *          Id
   * @return latest forward record
   */
  EutForwardReqMoreInfo findForwardReferenceAgainstTheRecord(String recordId, String userId,
      String roleId);

  /**
   * This method is used to find the orginal alert which is there before any forward request starts
   * 
   * We will mark this as original alert and also add nextrole id as string
   * 
   * @param recordId
   */

  public void setOriginalAlert(String recordId, EutNextRole nextrole);

  /**
   * This method is used to find out the orginal alert coming from approval cycle (Exclude a forward
   * or RMI alert)
   * 
   * 
   * @param recordId
   * @param nextrole
   * @return alert list({@link Alert}
   */
  public List<Alert> findOriginalAlert(String recordId, EutNextRole nextrole, String reference,
      boolean fwd_Rmi);
}
