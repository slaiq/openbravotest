package sa.elm.ob.finance.ad_process.RDVProcess.hook;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.application.window.FICExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;

/**
 * THis class is used to add the new attribute called attachment path in attachment objects
 * 
 * This class is only for rdv transaction tab
 * 
 * @author sathishkumar
 *
 */

public class RDVAttachmentFICHook implements FICExtension {

  public static String TAB_ID = "A0F3A7D17A834A93B3BD4D2C40E77AFE";
  private static final Logger log = Logger.getLogger(RDVAttachmentFICHook.class);

  @Override
  public void execute(String mode, Tab tab, Map<String, JSONObject> columnValues, BaseOBObject row,
      List<String> changeEventCols, List<JSONObject> calloutMessages, List<JSONObject> attachments,
      List<String> jsExcuteCode, Map<String, Object> hiddenInputs, int noteCount,
      List<String> overwrittenAuxiliaryInputs) {

    try {

      if (attachments != null && attachments.size() > 0) {
        for (JSONObject attachment : attachments) {
          if (attachment.optString("id", null) != null) {
            Attachment attachmentObj = OBDal.getInstance().get(Attachment.class,
                attachment.getString("id"));
            if (attachmentObj != null && attachmentObj.getEutDmsAttachpath() != null) {
              attachment.put("attachmentPath", attachmentObj.getEutDmsAttachpath());
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Errow in RDVAttachmentFICHook " + e.getMessage());
    }

  }

}
