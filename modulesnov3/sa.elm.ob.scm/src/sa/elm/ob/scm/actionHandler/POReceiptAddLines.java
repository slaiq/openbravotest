package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

public class POReceiptAddLines extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptAddLines.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject inspectLines = jsonparams.getJSONObject("Receive");
      final String inoutId = jsonRequest.getString("inpmInoutId");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");
      String exeStartDateH = null;
      String exeEndDateH = null;
      POReceiptAddLinesDAO dao = new POReceiptAddLinesDAO();

      // check selected line should be greater than zero
      if (selectedlines.length() == 0) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
      for (int i = 0; i < selectedlines.length(); i++) {

        JSONObject selectedRow = selectedlines.getJSONObject(i);
        if (selectedRow.has("exestartdateh") && selectedRow.getString("exestartdateh") != null) {
          exeStartDateH = selectedRow.getString("exestartdateh");
        }
        if (selectedRow.has("exeenddateh") && selectedRow.getString("exeenddateh") != null) {
          exeEndDateH = selectedRow.getString("exeenddateh");
        }
      }
      if (exeStartDateH.matches("\\d{2}-\\d{2}-\\d{4}")) {
        exeStartDateH = exeStartDateH.split("-")[2] + "-" + exeStartDateH.split("-")[1] + "-"
            + exeStartDateH.split("-")[0];
      }
      if (exeEndDateH.matches("\\d{2}-\\d{2}-\\d{4}")) {
        exeEndDateH = exeEndDateH.split("-")[2] + "-" + exeEndDateH.split("-")[1]
            + exeEndDateH.split("-")[0];
      }

      if ((exeEndDateH != null && !exeEndDateH.equals("null"))
          && (exeStartDateH != null && !exeStartDateH.equals("null"))) {
        if (exeEndDateH.compareTo(exeStartDateH) < 0) {
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("ESCM_StartDate_lessThanEndDate"));
          json.put("message", errorMessage);
          return json;
        }
      }

      int count = dao.insertInitialReceipt(inoutId, selectedlines);

      if (count == 1) {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
        json.put("message", errorMessage);
        return json;
      } else if (count == 2) {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_AmtZero"));
        json.put("message", errorMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in POContractInspectionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    }

  }
}
