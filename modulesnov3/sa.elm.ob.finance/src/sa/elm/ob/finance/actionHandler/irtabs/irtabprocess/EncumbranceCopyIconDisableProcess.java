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
 * @author Poongodi 04-01-2018
 *
 */

public class EncumbranceCopyIconDisableProcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(EncumbranceCopyIconDisableProcess.class);

  /**
   * This class is used to disable copy button & delete button for encumbrance method as auto
   * 
   */

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");
      final String btnName = jsonData.optString("button") == "" ? null
          : jsonData.getString("button");

      if (tabId.equals("9CBD55F879EA4DCAA4E944C0B7DC03D4")) {
        EfinBudgetManencum encRule = OBDal.getInstance().get(EfinBudgetManencum.class, recordId);

        if (btnName != null && btnName.equals("delete")) {
          if (encRule != null && (!encRule.getDocumentStatus().equals("DR")
              && !encRule.getDocumentStatus().equals("RW")) && btnName.equals("delete")) {
            enable = 1;
          } else {
            enable = 0;
          }
        } else {
          if (encRule != null && encRule.getEncumMethod().equals("A")) {
            enable = 1;
          } else {
            enable = 0;
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in EncumbranceCopyIconDisableProcess: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
