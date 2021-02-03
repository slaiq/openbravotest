package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.actionHandler.dao.PenaltyReleaseLineHandlerDao;

/**
 * 
 * @author Gowtham V
 *
 */
public class PenaltyReleaseLine extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(PenaltyReleaseLine.class);

  /**
   * This class is used to release penalty process.
   */
  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      String newTxnLineId = jsonRequest.getString("inpefinRdvtxnlineId");
      long lineNo = 0;
      int status = 0;
      String message = "";

      JSONObject penaltyLines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = penaltyLines.getJSONArray("_selection");

      // get line no
      OBQuery<EfinPenaltyAction> newPenalty = OBDal.getInstance()
          .createQuery(EfinPenaltyAction.class, "efinRdvtxnline.id = '" + newTxnLineId + "'");
      if (newPenalty.list() != null && newPenalty.list().size() > 0) {
        lineNo = newPenalty.list().size() * 10;
      } else {
        lineNo = 10;
      }

      // getting selected records in Release Pop up.
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        String penaltyLine = selectedRow.getString("id");
        BigDecimal enteredpenaltyAmt = new BigDecimal(selectedRow.getString("enteredamt"));
        if (enteredpenaltyAmt.compareTo(BigDecimal.ZERO) == 0) {
          status = -1;
        } else {
          status = PenaltyReleaseLineHandlerDao.insertPenaltyLines(enteredpenaltyAmt, penaltyLine,
              newTxnLineId, lineNo, "LN", null);
          lineNo = lineNo + 10;
        }
      }

      if (status == 0) {
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
        message = OBMessageUtils.parseTranslation("@Efin_Penalty_Release_sucs@");
        errormsg.put("severity", "success");
        errormsg.put("text", message);
        jsonResponse.put("message", errormsg);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
        return jsonResponse;
      }

    } catch (Exception e) {
      log.error(" Exception while penalty release line: " + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
  }
}
