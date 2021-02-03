package sa.elm.ob.scm.actionHandler.dao;

import org.codehaus.jettison.json.JSONObject;

public interface CreatePEEActionHandlerDAO {

  /**
   * If action is Create PEE / Display, will check if PEE exists for this proposal then it will
   * redirect to PEE screen else create the PEE and proposal attribute automatically and then
   * redirect to PEE window
   * 
   * @param proposalId
   * @return
   */
  public JSONObject createPEE(String proposalId);

}
