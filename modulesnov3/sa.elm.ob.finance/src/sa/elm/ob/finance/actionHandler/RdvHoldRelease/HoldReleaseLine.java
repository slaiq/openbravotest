package sa.elm.ob.finance.actionHandler.RdvHoldRelease;

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

import sa.elm.ob.finance.EfinRdvHoldAction;

/**
 * @author Rashika.V.S on 02-03-2019
 *
 */

public class HoldReleaseLine extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(HoldReleaseLine.class);
  private static final String LINE = "LN";

  /**
   * This process is used to release the hold amount from lines
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

      JSONObject holdLines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = holdLines.getJSONArray("_selection");
      HoldReleaseLineHandlerDAO dao = new HoldReleaseLineHandlerDAOImpl();

      // get line number
      OBQuery<EfinRdvHoldAction> newHold = OBDal.getInstance().createQuery(EfinRdvHoldAction.class,
          "efinRdvtxnline.id =:txnLineId ");
      newHold.setNamedParameter("txnLineId", newTxnLineId);
      if (newHold.list() != null && newHold.list().size() > 0) {
        lineNo = newHold.list().size() * 10;
      } else {
        lineNo = 10;

      }

      // getting selected records in Hold Pop up
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        String HoldLine = selectedRow.getString("id");
        BigDecimal enteredHoldAmt = new BigDecimal(selectedRow.getString("enteredamt"));
        if (enteredHoldAmt.compareTo(BigDecimal.ZERO) == 0) {
          status = -1;
        } else {
          status = dao.insertHoldLines(enteredHoldAmt, HoldLine, newTxnLineId, lineNo, LINE, null);
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
      log.error(" Exception in HoldReleaseLine: " + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
  }

}
