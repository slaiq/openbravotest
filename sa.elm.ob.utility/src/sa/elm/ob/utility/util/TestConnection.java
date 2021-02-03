package sa.elm.ob.utility.util;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.EUT_OrclConnection;

public class TestConnection extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(TestConnection.class);
  private final OBError obError = new OBError();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      Boolean isValid = Boolean.FALSE;
      JSONObject resultObject = null;
      String strConnectionId = (String) bundle.getParams().get("EUT_Orcl_Connection_ID");
      log.debug("strConnectionId :" + strConnectionId);

      EUT_OrclConnection conn = OBDal.getInstance().get(EUT_OrclConnection.class, strConnectionId);
      resultObject = ConnectionUtility.testConnection(conn.getConnectionUrl(), conn.getUsername(),
          conn.getPassword(), Boolean.TRUE);
      isValid = resultObject.getBoolean("isValid");

      if (isValid) {
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage("Connection successful");
      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(resultObject.getString("message"));
      }
      bundle.setResult(obError);
      return;
    } catch (Exception e) {
      log.error("Exception while establishing connection:", e);
      obError.setType("Error");
      obError.setTitle("Error");
      bundle.setResult(obError);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
