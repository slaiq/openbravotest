package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.MaterialIssueRequestLine;

public class MaterialIssuePendQtyHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    // TODO Auto-generated method stub
    try {
      final JSONObject jsonData = new JSONObject(data);
      final String recordId = jsonData.getString("recordId");
      // HttpServletRequest request = RequestContext.get().getRequest();
      // VariablesSecureApp vars = new VariablesSecureApp(request);
      JSONObject json = new JSONObject();
      OBContext.setAdminMode(true);
      if (recordId != null) {
        MaterialIssueRequestLine line = OBDal.getInstance().get(MaterialIssueRequestLine.class,
            recordId);
        json.put("pendingQty", line.getPendingQty());
      } else {
        json.put("pendingQty", 0);
      }
      OBContext.restorePreviousMode();
      return json;
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      throw new OBException(e);
    }
  }
}
