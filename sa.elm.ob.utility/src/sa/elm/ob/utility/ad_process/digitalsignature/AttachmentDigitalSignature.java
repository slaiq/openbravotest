
package sa.elm.ob.utility.ad_process.digitalsignature;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class is used to send the attachment attached to DMS server and log the request and response 
 * 
 * @author sathish kumar
 * 
 */

public class AttachmentDigitalSignature extends BaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(DigitalSignature.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {

      OBContext.setAdminMode();
      AttachmentDMSInterface dmsAttachment = null;

      final JSONObject jsonData = new JSONObject(data);

      String recordId = jsonData.getString("recordId");
      String tabId = jsonData.getString("tabId");
      String attachmentId = jsonData.getString("attachmentId");

      if ("A0F3A7D17A834A93B3BD4D2C40E77AFE".equals(tabId)) {
        dmsAttachment = new RDVAttachmentDMSImpl();
      }

      dmsAttachment.addAttachment(tabId, recordId, attachmentId);

    } catch (Exception e) {
      log.error("Exception in Digital signature:", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
