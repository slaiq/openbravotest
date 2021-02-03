package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class ProposalEvalEvents extends IRTabIconVariables {
  Logger log = Logger.getLogger(ProcurementCommitte.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /*
       * Proposals Evaluation Events - Committee Recommendation, Proposals Evaluation Events,
       * Proposals
       */
      if (tabId.equals("B95E00033F514207B2915772C2D6D282")
          || tabId.equals("61D6CF3612134CAF942B811EC74B1F0B")
          || tabId.equals("53A3B7C2D094483CBC66DEE4D9715A6E")) {
        if (!recordId.equals("")) {
          ESCMProposalEvlEvent propevvlevent = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
              recordId);
          if (tabId.equals("53A3B7C2D094483CBC66DEE4D9715A6E")) {
            if (propevvlevent.getBidNo() != null || propevvlevent.getStatus().equals("CO"))
              enable = 1;
          } else {
            if (propevvlevent.getStatus().equals("CO"))
              enable = 1;
          }
        }
      }
      /* Proposals Evaluation Events - Committee Comments */
      else if (tabId.equals("6E0596A123994C82BC8F80C0D2554578")) {
        if (!recordId.equals("")) {
          EscmProposalAttribute propattr = OBDal.getInstance().get(EscmProposalAttribute.class,
              recordId);
          if (propattr.getEscmProposalevlEvent() != null) {
            ESCMProposalEvlEvent propevvlevent = propattr.getEscmProposalevlEvent();
            if (propevvlevent.getStatus().equals("CO"))
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
