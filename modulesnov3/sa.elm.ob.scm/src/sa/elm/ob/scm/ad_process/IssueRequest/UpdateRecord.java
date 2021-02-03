package sa.elm.ob.scm.ad_process.IssueRequest;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class UpdateRecord extends BaseActionHandler {
  Logger log4j = Logger.getLogger(UpdateRecord.class);
  JSONObject json;

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      json = new JSONObject();
      final JSONObject jsonData = new JSONObject(data);
      final String type = jsonData.getString("type") == null ? "" : jsonData.getString("type");
      if (type.equals("updateqty")) {
        boolean update = false;
        String recordId = jsonData.getString("recordId") == null ? ""
            : jsonData.getString("recordId");
        ;
        String warehouseId = jsonData.getString("warehouseId") == null ? ""
            : jsonData.getString("warehouseId");
        ;
        update = sa.elm.ob.scm.ad_process.IssueRequest.UpdateOnHandQty.updateOnHandQty(warehouseId,
            recordId);
        json.put("update", update);
      }
      if (type.equals("checkaccess")) {
        boolean hasAccess = false;
        String roleId = jsonData.getString("roleId") == null ? "" : jsonData.getString("roleId");
        ;
        String clientId = jsonData.getString("clientId") == null ? ""
            : jsonData.getString("clientId");
        ;
        String orgId = jsonData.getString("orgId") == null ? "" : jsonData.getString("orgId");
        ;
        String userId = jsonData.getString("userId") == null ? "" : jsonData.getString("userId");
        ;
        String mirId = jsonData.getString("mirId") == null ? "" : jsonData.getString("mirId");
        ;
        hasAccess = sa.elm.ob.scm.ad_process.IssueRequest.UpdateOnHandQty
            .checkMIROnHandQtyUpdateAccess(roleId, clientId, orgId, userId, mirId);
        json.put("hasAccess", hasAccess);
      }

    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return json;
  }
}