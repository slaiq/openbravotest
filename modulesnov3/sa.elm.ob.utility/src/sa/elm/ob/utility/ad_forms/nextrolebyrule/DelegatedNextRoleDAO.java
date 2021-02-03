package sa.elm.ob.utility.ad_forms.nextrolebyrule;

import java.util.Date;

/**
 * @author Kousalya
 */
public interface DelegatedNextRoleDAO {

  /**
   * Checks if there exists a processed delegation document for a given document type on the
   * specified date
   * 
   * @param currentDate
   * @param currentRoleId
   * @param docType
   */
  public Boolean checkDelegation(Date delegationDate, String strRoleId, String documentType)
      throws Exception;

  /**
   * Returns the delegated from role based on the logged in role and user for the document type for
   * the current date.
   * 
   * @param strRoleId
   * @param strDocumentType
   * @param strUserId
   * 
   */
  public String getDelegatedFromRole(String strRoleId, String strDocumentType, String strUserId)
      throws Exception;
}
