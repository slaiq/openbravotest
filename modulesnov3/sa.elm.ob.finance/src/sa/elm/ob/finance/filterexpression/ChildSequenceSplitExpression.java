package sa.elm.ob.finance.filterexpression;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;

/**
 * This class is used to set default expression for From and To field in Split sequence process
 * definition
 * 
 * @author Sathishkumar.P
 *
 */

public class ChildSequenceSplitExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(ChildSequenceSplitExpression.class);

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    String clientid = "";
    String from = "";
    String to = "";
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");

      if (strCurrentParam.equals("From")) {
        from = context.getString("inpnextSequence");
        if (from.equals("null")) {
          return "";
        } else {
          return from;
        }
      }

      if (strCurrentParam.equals("To")) {
        to = context.getString("inpfinTo");
        if (to.equals("null")) {
          return "";
        } else {
          return to;
        }
      }
    } catch (JSONException e) {
      log4j.debug("Error while setting default expression " + e.getMessage());
      return null;
    }
    return clientid;
  }
}
