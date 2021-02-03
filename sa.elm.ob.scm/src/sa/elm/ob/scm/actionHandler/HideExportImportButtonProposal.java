package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmProposalMgmt;

/**
 * 
 * @author Kiruthika on 28/05/2020
 *
 */
public class HideExportImportButtonProposal extends BaseActionHandler {
  private static Logger log4j = Logger.getLogger(HideExportImportButtonProposal.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {
      result.put("result", 0);
      OBContext.setAdminMode();
      EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
          (String) parameters.get("recordId"));
      if (proposal != null && proposal.getEscmProposalmgmtLineList().size() > 0) {
        result.put("result", 1);
      }
    } catch (Exception e) {
      log4j.error("Exception in HideExportImportButtonProposal :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
