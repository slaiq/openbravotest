package sa.elm.ob.finance.ad_process.RDVProcess;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;

/**
 * 
 * @author Gopinagh.R
 *
 */

public class RDVOnSaveHandler extends BaseActionHandler {

  Logger log4j = Logger.getLogger(RDVOnSaveHandler.class);
  JSONObject json = null;
  private static final String CHECK_FOR_PENALTY = "checkForPenalty";
  private static final String ADD_PENALTY = "addPenalty";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);

      json = new JSONObject();

      final String recordId = jsonData.getString("recordId");
      final String action = jsonData.getString("inpAction");
      final String strMatchQty = jsonData.getString("matchQty");
      final String strMatchAmt = jsonData.getString("matchAmt");

      AddDefaultPenaltyService addPenaltyService = new AddDefaultPenaltyServiceImpl();

      if (CHECK_FOR_PENALTY.equals(action)) {
        json = addPenaltyService.applicableForPenalty(recordId, strMatchQty, strMatchAmt);

        // json.put("addPenalty", canApplyPenalty ? "true" : "false");

      } else if (ADD_PENALTY.equals(action)) {

        String strActionDate = jsonData.getString("actionDate");
        String strAdvanceDeductionAmount = jsonData.getString("advDeductionAmt");

        json = addPenaltyService.addPenalty(recordId, strMatchQty, strActionDate,
            strAdvanceDeductionAmount, strMatchAmt);
      }

    } catch (Exception e) {
      log4j.error(" Exception in RDVOnSaveHandler: " + e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

}
