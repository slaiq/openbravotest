package sa.elm.ob.utility.util;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class UpdateParent extends DalBaseProcess {
  private final OBError obError = new OBError();
  private static final Logger log = Logger.getLogger(UpdateParent.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    JSONObject resultObject = null;
    try {
      OBContext.setAdminMode();
      resultObject = OrgUtils.updateParentDept();

      if (resultObject.getString("result").equals("S")) {
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(resultObject.getString("message"));
        bundle.setResult(obError);
      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(resultObject.getString("message"));
        bundle.setResult(obError);
      }
    } catch (Exception e) {
      obError.setType("Error");
      obError.setTitle("Error");
      obError.setMessage(e.getMessage());
      bundle.setResult(obError);

      log.error("Exception while Updating Parent :", e);

    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
