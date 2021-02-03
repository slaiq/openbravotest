package sa.elm.ob.utility.ad_actionHandler;

import java.sql.Connection;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.ad_actionHandler.dao.ConcurrentExecutionOfProcessDefinitionDAO;

/**
 * 
 * @author divya 20-06-2019
 *
 */
public class ConcurrentExecutionOfProcessDefinition extends BaseActionHandler {

  private static final Logger LOG = LoggerFactory
      .getLogger(ConcurrentExecutionOfProcessDefinition.class);
  public static final String SCHEDULED = "SCH";
  public static final String ERROR = "ERR";

  public static final String COMPLETE = "COM";

  /**
   * This class is used to ConcurrentExecutionOfProcessDefinition
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();

    Connection conn = OBDal.getInstance().getConnection();

    try {
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = null;
      if (jsonRequest.has("params")) {
        jsonparams = jsonRequest.getJSONObject("params");
      }
      String action = "";
      String id = null;
      String processId = null;
      String messagetxt = null;
      String status = null;
      long startTime = System.currentTimeMillis();
      String processDefMonitorId = null;
      long endTime = 0;
      boolean concurrentExecute = false;
      if (jsonRequest.has("action")) {
        action = jsonRequest.getString("action");
      }
      if (jsonRequest.has("id")) {
        id = jsonRequest.getString("id");
      }
      if (jsonRequest.has("processId")) {
        processId = jsonRequest.getString("processId");

      }
      if (action.equals("insertProcessMonitor")) {
        status = SCHEDULED;
        processDefMonitorId = ConcurrentExecutionOfProcessDefinitionDAO.insertProcessMonitor(conn,
            vars, parameters, status, (jsonparams == null ? null : jsonparams.toString()),
            startTime, null, id, processId);
        // json.put("processDefMonitorId", processDefMonitorId);
        concurrentExecute = ConcurrentExecutionOfProcessDefinitionDAO.checkConcurrentExecution(conn,
            id, processId);
        JSONObject errorMessage = new JSONObject();
        endTime = System.currentTimeMillis();
        String duration = ConcurrentExecutionOfProcessDefinitionDAO
            .getDuration(endTime - startTime);
        status = COMPLETE;
        LOG.debug("concurrentExecute:" + concurrentExecute);
        if (concurrentExecute) {

          ConcurrentExecutionOfProcessDefinitionDAO.updateProcessMonitor(conn, vars, status,
              endTime, duration, processDefMonitorId, messagetxt);

          errorMessage.put("severity", "error");
          errorMessage.put("title", "Error");
          errorMessage.put("text", OBMessageUtils.messageBD("EFIN_InvAlreadyProcessed"));
          json.put("message", errorMessage);
          return json;
        }

        else {

          ConcurrentExecutionOfProcessDefinitionDAO.updateProcessMonitor(conn, vars, status,
              endTime, duration, processDefMonitorId, messagetxt);
          errorMessage.put("severity", "success");
          errorMessage.put("title", "Success");
          errorMessage.put("text", "Success");
          json.put("message", errorMessage);
          return json;
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in ConcurrentExecutionOfProcessDefinition :", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }
}
