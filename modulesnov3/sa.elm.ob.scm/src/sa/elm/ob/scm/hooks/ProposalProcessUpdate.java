package sa.elm.ob.scm.hooks;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.json.DefaultJsonDataService.DataSourceAction;
import org.openbravo.service.json.JsonDataServiceExtraActions;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;

/**
 * 
 * @author Divya
 * 
 * 
 *
 */

public class ProposalProcessUpdate implements JsonDataServiceExtraActions {

  private static final Logger log = Logger.getLogger(ProposalProcessUpdate.class);
  private static final String TAB_ID = "tabId";
  private static final String PROPOSAL_TAB_ID = "D6115C9AF1DD4C4C9811D2A69E42878B";
  private static final String Operation_Type = "_operationType";

  @Override
  public void doPreAction(Map<String, String> parameters, JSONArray data, DataSourceAction action) {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPostAction(Map<String, String> parameters, JSONObject content,
      DataSourceAction action, String originalObject) {
    // TODO Auto-generated method stub
    try {
      OBContext.setAdminMode();
      // We can identify the save and update using _doingAdd flag in parameter map
      // do the business logic what ever you want with the parameters
      if (parameters.containsKey(Operation_Type) && parameters.get(Operation_Type).equals("update")
          && PROPOSAL_TAB_ID.equals(parameters.get(TAB_ID))) {
        JSONObject responseJson = content.getJSONObject("response");
        JSONArray dataJson = responseJson.getJSONArray("data");
        String proposalId = dataJson.getJSONObject(0).getString("id");
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
        if (proposal != null) {
          for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
            line.setProcess(false);
            OBDal.getInstance().save(line);
          }
        }

      }
    } catch (Exception e) {
      log.error("Error while handling post save actions" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}