package sa.elm.ob.finance.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

/**
 * 
 * @author Poongodi 16-09-2019
 *
 */

public class EnclinesDeleteDisableProcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(EnclinesDeleteDisableProcess.class);

  /**
   * This class is used to disable delete button in encumbrance
   * 
   */

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");
      final String btnName = jsonData.optString("button") == "" ? null
          : jsonData.getString("button");
      if (tabId.equals("A2E25351FBFF41CB949EDF35DE875B73") && btnName.equals("delete")) {

        EfinBudgetManencum encRule = OBDal.getInstance().get(EfinBudgetManencum.class, recordId);

        if (encRule != null && (!encRule.getDocumentStatus().equals("DR")
            && !encRule.getDocumentStatus().equals("RW"))) {
          enable = 1;
        } else {
          enable = 0;
        }
      }
    } catch (Exception e) {
      log.error("Exception in EnclinesDeleteDisableProcess: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
