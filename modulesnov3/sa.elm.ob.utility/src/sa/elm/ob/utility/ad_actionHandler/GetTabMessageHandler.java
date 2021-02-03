package sa.elm.ob.utility.ad_actionHandler;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.window.GetTabMessageActionHandler;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;

import sa.elm.ob.utility.dms.consumer.dto.PKIRequestVO;
import sa.elm.ob.utility.dms.util.DMSUtility;

/**
 * Is used to get the message set in session for a tab
 * 
 */
@ApplicationScoped
public class GetTabMessageHandler extends BaseActionHandler {
  private static final Logger log = Logger.getLogger(GetTabMessageActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();

    String tabId = "";
    RequestContext rc = RequestContext.get();
    try {
      tabId = new JSONObject(content).getString("tabId");
      final String attr = tabId + "|MESSAGE";
      final String pkiData = tabId + "|PKI";
      final OBError msg = (OBError) rc.getSessionAttribute(attr);
      final PKIRequestVO pki = (PKIRequestVO) rc.getSessionAttribute(pkiData);

      if (msg != null) {
        result.put("type", "TYPE_" + msg.getType().toUpperCase());
        result.put("title", msg.getTitle());
        result.put("text", msg.getMessage());
        rc.removeSessionAttribute(attr);
      }

      if (pki != null) {
        result.put("isPKI", true);
        result.put("attachmentPath", pki.getProfileURI());
        result.put("userId", pki.getUserId());
        result.put("grpRequestID", pki.getGrpRequestID());
        result.put("documentName", pki.getDocumentName());
        result.put("page", pki.getPageCount());
        result.put("position",
            DMSUtility.getPKIPosition(pki.getDocumentType(), "L" + pki.getApprovalPosition()));
        result.put("level", "L" + pki.getApprovalPosition());
        result.put("documentType", pki.getDocumentType());

        rc.removeSessionAttribute(pkiData);
      } else {
        result.put("isPKI", false);
      }
    } catch (Exception e) {
      log.error("Error getting message for tab " + tabId, e);
    }
    return result;
  }
}
