package sa.elm.ob.finance.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

/**
 * 
 * @author poongodi 05-12-2019
 *
 */

public class RevisionLineDisableProcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(RevisionLineDisableProcess.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      if (tabId.equals("E68453B4E62548C6B5E79FEDE3C36586")) {

        EfinBudgetTransfertrx header = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
            recordId);
        if (header.isRdvhold()) {
          enable = 1;
        } else {
          enable = 0;
        }
      }
    } catch (Exception e) {
      log.error("Exception in RevisionLineDisableProcess: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
