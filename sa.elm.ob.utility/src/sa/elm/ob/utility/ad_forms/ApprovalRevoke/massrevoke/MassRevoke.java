package sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke;

import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * 
 * @author Sathishkumar 13-11-2017
 *
 */

public abstract class MassRevoke {

  protected Logger log = Logger.getLogger(MassRevoke.class);

  /**
   * This method is used to get the total no of records waiting for approval
   * 
   * @param vars
   * @param clientId
   * @param windowId
   * @param searchFlag
   * @param vo
   * @return count of records waiting for approval
   */

  public abstract int getRevokeRecordsCount(VariablesSecureApp vars, String clientId,
      String windowId, String searchFlag, ApprovalRevokeVO vo);

  /**
   * This method is used to get the list of records waiting for approval
   * 
   * @param clientId
   * @param windowId
   * @param vo
   * @param limit
   * @param offset
   * @param sortColName
   * @param sortColType
   * @param searchFlag
   * @return list of records waiting for approval records
   */
  public abstract List<ApprovalRevokeVO> getRevokeRecordsList(VariablesSecureApp vars,
      String clientId, String windowId, ApprovalRevokeVO vo, int limit, int offset,
      String sortColName, String sortColType, String searchFlag, String lang);

  /**
   * This method is used to update the record for selected records after revoke
   * 
   * @param var
   * @param selectIds
   * @param inpWindowId
   * @return success or error in string
   */
  public abstract String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId);

  /**
   * This method is used to validate the records before doing the revoke
   * 
   * @param selectIds
   * @param inpWindowId
   * @return
   */
  public abstract String validateRecord(String selectIds, String inpWindowId);

}
