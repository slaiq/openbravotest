package sa.elm.ob.scm.ad_process.AddAllContracts;

public class AddAllContractsImpl implements AddAllContracts {

  /**
   * Add all contracts in Role - Lookup Access
   * 
   * @param roleId
   * @param userId
   * @param orgId
   * @param clientId
   * @return
   */
  @Override
  public boolean addAllContracts(String roleId, String userId, String orgId, String clientId) {
    return AddAllContractsDAO.addAllContracts(roleId, userId, orgId, clientId);
  }

}
