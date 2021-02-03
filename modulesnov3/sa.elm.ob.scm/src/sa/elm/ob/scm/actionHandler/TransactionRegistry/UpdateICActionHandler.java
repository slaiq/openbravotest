/**
 * 
 */
package sa.elm.ob.scm.actionHandler.TransactionRegistry;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

/**
 * @author qualian
 *
 */
public class UpdateICActionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(UpdateICActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject responseObject = new JSONObject();
    UpdateICHandlerDAO dao = new UpdateICHandlerDAO(OBDal.getInstance().getConnection());

    try {
      OBContext.setAdminMode();
      final JSONObject jsonData = new JSONObject(content);
      final JSONArray transactionIds = jsonData.getJSONArray("transactions");
      responseObject = dao.ValidateTransactions(transactionIds);

    } catch (Exception e) {
      log.error("Exception in UpdateICActionHandler: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return responseObject;
  }

}
