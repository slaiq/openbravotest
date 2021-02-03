package sa.elm.ob.finance.actionHandler.RdvHoldRelease;

import java.math.BigDecimal;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRdvHoldAction;

/**
 * @author Rashika.V.S on 02-03-2019
 *
 */

public class HoldReleaseVersion extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(HoldReleaseVersion.class);
  private static final String VERSION = "VER";

  /**
   * This process is used to release the hold amount from lines
   */

  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      HoldReleaseLineHandlerDAO dao = new HoldReleaseLineHandlerDAOImpl();

      String newTxnId = jsonRequest.getString("inpefinRdvtxnId");

      long lineNo = 10;
      int status = 0;
      String message = "";
      String appStatus = null;

      JSONObject penaltyLines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = penaltyLines.getJSONArray("_selection");

      // Hold release is not allowed, if actual version is not approved.
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        String holdLine = selectedRow.getString("id");
        if (holdLine != null) {
          EfinRdvHoldAction holdAction = OBDal.getInstance().get(EfinRdvHoldAction.class, holdLine);
          appStatus = holdAction.getEfinRdvtxnline().getEfinRdvtxn().getAppstatus();
          if (appStatus != null && !appStatus.equals("APP")) {
            JSONObject errormsg = new JSONObject();
            errormsg.put("severity", "error");
            errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_HoldReleaseNotAllowed@"));
            jsonResponse.put("message", errormsg);
            OBDal.getInstance().rollbackAndClose();
            return jsonResponse;
          }
        }
      }

      // getting selected records in Hold Pop up
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        String holdLine = selectedRow.getString("id");
        BigDecimal enteredHoldAmt = new BigDecimal(selectedRow.getString("enteredamt"));
        if (enteredHoldAmt.compareTo(BigDecimal.ZERO) == 0) {
          status = -1;
        } else {
          status = dao.insertHoldLines(enteredHoldAmt, holdLine, null, lineNo, VERSION, newTxnId);
          lineNo = lineNo + 10;
        }
      }
      if (selectedlines.length() == 0) {
        status = 2;
      }

      if (status == 2) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_Line_Not_Selected@"));
        jsonResponse.put("message", errormsg);
        OBDal.getInstance().rollbackAndClose();
        return jsonResponse;
      } else if (status == 0) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_PenaltyRelease_Error@"));
        jsonResponse.put("message", errormsg);
        OBDal.getInstance().rollbackAndClose();
        return jsonResponse;
      } else if (status == -1) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_PenaltyRelease_Zero@"));
        jsonResponse.put("message", errormsg);
        OBDal.getInstance().rollbackAndClose();
        return jsonResponse;
      } else {
        JSONObject errormsg = new JSONObject();
        message = OBMessageUtils.parseTranslation("@Efin_RdvHold_Release_success@");
        errormsg.put("severity", "success");
        errormsg.put("text", message);
        jsonResponse.put("message", errormsg);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
        return jsonResponse;
      }

    } catch (Exception e) {
      log.error(" Exception in HoldReleaseVersion() " + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
  }

}
