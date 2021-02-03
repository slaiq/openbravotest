package sa.elm.ob.scm.ad_process.AddAllContracts;

public interface AddAllContracts {

  /**
   * Add all contracts in Role - Lookup Access
   * 
   * @param roleId
   * @param userId
   * @param orgId
   * @param clientId
   * @return
   */
  public boolean addAllContracts(String roleId, String userId, String orgId, String clientId);

}
