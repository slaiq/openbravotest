package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class OpenEnvEvent extends IRTabIconVariables {
  Logger log = Logger.getLogger(OpenEnvEvent.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /* Open Envelope Event-Header */
      if (tabId.equals("8095B818800446D795B8ADFEDE104733")) {
        if (!recordId.equals("")) {
          Escmopenenvcommitee opnEnvCmt = OBDal.getInstance().get(Escmopenenvcommitee.class,
              recordId);
          if (opnEnvCmt != null) {
            if (opnEnvCmt.getAlertStatus().equals("CO") || opnEnvCmt.getAlertStatus().equals("DR"))
              enable = 1;
          }
        }
      }
      /* Open Envelope Event- Bank Guarantee Detail */
      else if (tabId.equals("BC7489A521854DA1B92D40ED7C7A7098")) {
        if (!recordId.equals("")) {
          EscmProposalAttribute att = OBDal.getInstance().get(EscmProposalAttribute.class,
              recordId);
          Escmopenenvcommitee opnEnvCmt = att.getEscmOpenenvcommitee();
          if (opnEnvCmt != null) {
            if (opnEnvCmt.getAlertStatus().equals("CO"))
              enable = 1;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
