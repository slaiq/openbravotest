package sa.elm.ob.utility.ad_callouts;

import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;

import sa.elm.ob.utility.util.Utility;

public class UtilityCallout extends BaseActionHandler {
  private static Logger log4j = Logger.getLogger(UtilityCallout.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      JSONObject result = new JSONObject();
      final JSONObject jsonData = new JSONObject(data);
      if (jsonData.has("action")) {
        String action = jsonData.getString("action");
        // Get JS Date Format
        if ("getJSDateFormat".equals(action)) {
          if (Utility.dateFormatJS == null) {
            Utility.dateFormatJS = new SimpleDateFormat(OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().getProperty("dateFormat.java"));
            Utility.dateTimeFormatJS = new SimpleDateFormat(OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().getProperty("dateTimeFormat.java"));
            Utility.dateFormat = new SimpleDateFormat(OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().getProperty("dateFormat.java"));
            Utility.dateTimeFormat = new SimpleDateFormat(OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().getProperty("dateTimeFormat.java"));
            Utility.timeFormat = new SimpleDateFormat("HH:mm:ss");

            Utility.strDateFormatJS = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                .getProperty("dateFormat.java");
            Utility.strDateTimeFormatJS = OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().getProperty("dateTimeFormat.java");
            Utility.strDateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                .getProperty("dateFormat.java");
            Utility.strDateTimeFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                .getProperty("dateTimeFormat.java");
            Utility.strDateFormatSQL = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                .getProperty("dateFormat.sql");
            Utility.strDateTimeFormatSQL = OBPropertiesProvider.getInstance()
                .getOpenbravoProperties().getProperty("dateTimeFormat.sql");
            Utility.strTimeFormat = "HH:mm:ss";
          }
          result.put("defaultJSDateFormat", Utility.strDateFormatJS);
          result.put("defaultJSDateTimeFormat", Utility.strDateTimeFormatJS);
        }
      }
      return result;
    } catch (Exception e) {
      log4j.error("Exception in UtilityCallout :", e);
      throw new OBException(e);
    }
  }
}
