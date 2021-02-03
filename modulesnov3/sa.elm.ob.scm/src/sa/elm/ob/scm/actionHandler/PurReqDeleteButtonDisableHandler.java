package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.procurement.Requisition;

public class PurReqDeleteButtonDisableHandler extends BaseActionHandler {
  Logger log4j = Logger.getLogger(PurReqDeleteButtonDisableHandler.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      OBContext.setAdminMode();
      // get the data as json
      final JSONObject jsonData = new JSONObject(data);
      final String recordId = jsonData.getString("recordId");
      final String userId = OBContext.getOBContext().getUser().getId();
      final String roleId = OBContext.getOBContext().getRole().getId();
      final String clientId = OBContext.getOBContext().getCurrentClient().getId();
      log4j.debug("recordid:" + recordId);
      JSONObject json = new JSONObject();
      if (recordId != null && !recordId.equals("") && !recordId.equals("-1")) {
        Requisition objRequisition = OBDal.getInstance().get(Requisition.class, recordId);
        OBContext.setAdminMode(true);
        String preferenceValue = "", preferenceValueOfIC = "";
        try {
          preferenceValue = Preferences.getPreferenceValue("ESCM_LineManager", true, clientId,
              objRequisition.getOrganization().getId(), userId, roleId, "800092");

        } catch (PropertyException e) {
        }

        try {
          preferenceValueOfIC = Preferences.getPreferenceValue("ESCM_Inventory_Control", true,
              clientId, objRequisition.getOrganization().getId(), userId, roleId, "800092");

        } catch (PropertyException e) {
        }

        if (((preferenceValue != null && preferenceValue.equals("Y")
            || (preferenceValueOfIC != null && preferenceValueOfIC.equals("Y")))
            && objRequisition.getEscmDocStatus().equals("ESCM_IP"))
            || objRequisition.getCreatedBy().getId().equals(userId)) {
          json.put("reqCount", 1);
        } else {
          json.put("reqCount", 0);
        }

      }
      log4j.debug("json:" + json);
      // and return it
      return json;

    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
