package sa.elm.ob.utility.ad_actionHandler.deleteMessageActionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;

/**
 * 
 * @author DivyaPrakash JS
 * @since 26-07-2019
 *
 */

public class DeleteMessageActionHandler extends BaseActionHandler {
  private static final Logger log4j = Logger.getLogger(DeleteMessageActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();
    String tabId = "", message = "", lang = "", numberOfRecords = "";
    try {
      OBContext.setAdminMode();
      lang = OBContext.getOBContext().getLanguage().getLanguage();
      DeleteMessageActionHandlerDAO dao = new DeleteMessageActionHandlerImpl();
      tabId = new JSONObject(content).getString("tabId");
      numberOfRecords = new JSONObject(content).getString("numberOfRecords");
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      if (tab != null) {
        Long tabLevel = tab.getTabLevel();
        if (tabLevel == 0) {
          // Delete Message for tab level Zero
          message = dao.messageForTabLevelZero(tab, lang, numberOfRecords);
        } else {
          // Delete message for tab level greater than zero
          message = dao.messageForTabLevelGreaterThanZero(tab, lang, numberOfRecords);
        }
        if (!StringUtils.isEmpty(message)) {
          result.put("deleteMessage", message.toString());
        }
      }
    } catch (Exception e) {
      log4j.error("Excepiton in DeleteMessageActionHandler : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
