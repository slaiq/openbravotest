package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class ProcurementCommitte extends IRTabIconVariables {
  Logger log = Logger.getLogger(ProcurementCommitte.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");

      /* Procurement Committees-Members */
      if (!recordId.equals("")) {
        ESCMCommittee committee = OBDal.getInstance().get(ESCMCommittee.class, recordId);
        if (committee != null) {
          if (committee.getAlertStatus().equals("CO"))
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
