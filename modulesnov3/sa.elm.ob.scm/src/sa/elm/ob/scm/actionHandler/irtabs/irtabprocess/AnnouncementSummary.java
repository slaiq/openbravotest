package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.Escmannoucements;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class AnnouncementSummary extends IRTabIconVariables {
  Logger log = Logger.getLogger(AnnouncementSummary.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");

      /* Announcements Summary-Announcements Summary, Media, Bids */
      if (!recordId.equals("")) {
        Escmannoucements annoucment = OBDal.getInstance().get(Escmannoucements.class, recordId);
        if (annoucment != null) {
          if (annoucment.getAlertStatus().equals("CO"))
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
