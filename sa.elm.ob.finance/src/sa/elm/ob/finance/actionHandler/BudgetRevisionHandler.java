package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;

public class BudgetRevisionHandler extends BaseProcessActionHandler {

  private static final Logger log = Logger.getLogger(BudgetRevisionHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    JSONObject jsonResponse = new JSONObject();
    try {

      jsonResponse.put("retryExecution", true);
      return jsonResponse;

    } catch (final Exception e) {
      log.error("exception in BudgetREvisionHandler");
    }
    return jsonResponse;
  }

}
