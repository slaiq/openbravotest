package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class TechnicalEvalEvents extends IRTabIconVariables {
  Logger log = Logger.getLogger(ProcurementCommitte.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /*
       * Technical Evaluation Events - Committee Recommendation, Technical Evaluation Events,
       * Proposals
       */
      if (tabId.equals("7185D00B421A4F62B403E085F00176D6")
          || tabId.equals("F8DBF5C0C51E4212A331FBA07BCDAC53")
          || tabId.equals("2F500D79A67F4CF5927467A48680B829")) {
        if (!recordId.equals("")) {
          EscmTechnicalevlEvent techevvlevent = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
              recordId);
          if (tabId.equals("2F500D79A67F4CF5927467A48680B829")) {
              enable = 1;
          }
          else if (tabId.equals("F8DBF5C0C51E4212A331FBA07BCDAC53")) {
            if (techevvlevent.getStatus().equals("CO") || techevvlevent.getStatus().equals("ESCM_IP"))
              enable = 1;
          }
          else {
            if (techevvlevent.getStatus().equals("CO") )
              enable = 1;
          }
        }
      }
      /* Technical Evaluation Events - Committee Comments */
      else if (tabId.equals("4937EA14A9E44775B176F79052F13BFF")) {
        if (!recordId.equals("")) {
          EscmProposalAttribute propattr = OBDal.getInstance().get(EscmProposalAttribute.class,
              recordId);
          if (propattr.getEscmTechnicalevlEvent() != null) {
            EscmTechnicalevlEvent techevent = propattr.getEscmTechnicalevlEvent();
            if (techevent.getStatus().equals("CO") || techevent.getStatus().equals("ESCM_IP"))
              enable = 1;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
