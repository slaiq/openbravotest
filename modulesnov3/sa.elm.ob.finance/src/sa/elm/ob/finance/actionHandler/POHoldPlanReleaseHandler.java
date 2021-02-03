package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.finance.EFINRdvBudgHoldLine;

public class POHoldPlanReleaseHandler extends BaseActionHandler {

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
        EFINRdvBudgHoldLine line = OBDal.getInstance().get(EFINRdvBudgHoldLine.class, recordId);
        if (line != null) {
          json.put("pendingQty", (line.getHoldAmount()
              .subtract(line.getReleaseAmount().add(line.getBudgTransferamt()))));
        } else {
          json.put("pendingQty", 0);
        }
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
