package sa.elm.ob.utility.ad_process.ForwardRmiRoleAccess;

public interface ForwardRoleAccessDAO {

  /**
   * 
   * @param clientId
   * @param forwardrmiId
   * @return
   * @throws Exception
   */
  public int forwardRoleAccessRemove(String clientId, String forwardrmiId) throws Exception;

  /**
   * 
   * @param clientId
   * @param forwardrmiId
   * @param doctype
   * @return
   * @throws Exception
   */
  public int forwardAccessPreference(String clientId, String forwardrmiId, String windowId)
      throws Exception;

  /**
   * 
   * @param clientId
   * @param userId
   * @param forwardrmiId
   * @param doctype
   * @return
   * @throws Exception
   */
  public int forwardAccessWindow(String clientId, String userId, String forwardrmiId,
      String doctype) throws Exception;

  /**
   * 
   * @param clientId
   * @param forwardrmiId
   * @return
   * @throws Exception
   */
  public int forwardCheckBoxAccess(String clientId, String forwardrmiId) throws Exception;

  /**
   * 
   * @param clientId
   * @param userId
   * @param forwardrmiId
   * @param doctype
   * @return
   * @throws Exception
   */
  public int requestMoreInfoAccessWindow(String clientId, String userId, String forwardrmiId,
      String doctype) throws Exception;

  /**
   * RMI Role Access Remove
   * 
   * @param clientId
   * @param forwardrmiId
   * @return
   * @throws Exception
   */
  public int requestMoreInforRoleAccessRemove(String clientId, String forwardrmiId)
      throws Exception;

}
