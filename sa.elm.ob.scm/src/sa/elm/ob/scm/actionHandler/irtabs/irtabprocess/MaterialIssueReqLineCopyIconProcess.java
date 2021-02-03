package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class MaterialIssueReqLineCopyIconProcess extends IRTabIconVariables {
  Logger log = Logger.getLogger(MaterialIssueReqLineCopyIconProcess.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      VariablesSecureApp vars = new VariablesSecureApp(request);
      /* Material Issue Request-Material Issue Request */
      if (!recordId.equals("")) {
        MaterialIssueRequestLine missreqLine = OBDal.getInstance()
            .get(MaterialIssueRequestLine.class, recordId);

        try {

          String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
              vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
              "D8BA0A87790B4B67A86A8DF714525736");

          if (preferenceValue != null && preferenceValue.equals("Y"))
            ispreference = true;
        } catch (PropertyException e) {
          ispreference = false;
        }
        if (ispreference) {
          if (!missreqLine.getProduct().isStocked() && !missreqLine.getProduct().isPurchase())
            enable = 0;
          else
            enable = 1;
        } else {
          enable = 1;
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
