package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class MaterialIssueReqIconProcess extends IRTabIconVariables {
  Logger log = Logger.getLogger(MaterialIssueReqIconProcess.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      VariablesSecureApp vars = new VariablesSecureApp(request);

      /* Material Issue Request-Material Issue Request */
      if (!recordId.equals("")) {
        MaterialIssueRequest missreq = OBDal.getInstance().get(MaterialIssueRequest.class,
            recordId);
        if (missreq != null && (!(vars.getUser().equals(missreq.getCreatedBy().getId())))
            && missreq.getAlertStatus().equals("ESCM_IP")) {
          enable = 1;
        }
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
          if (missreq != null && (missreq.getWarehouse() != null)) {
            if (missreq.getAlertStatus().equals("ESCM_TR")
                || missreq.getAlertStatus().equals("ESCM_IP"))
              enable = 1;
          }
        }
        if (missreq != null && missreq.getAlertStatus().equals("ESCM_TR")) {
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
