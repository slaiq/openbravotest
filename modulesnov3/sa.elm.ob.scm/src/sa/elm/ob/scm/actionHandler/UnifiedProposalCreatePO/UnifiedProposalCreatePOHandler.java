
package sa.elm.ob.scm.actionHandler.UnifiedProposalCreatePO;

import java.math.BigDecimal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * 
 * This Class is used to create PO from selected proposals
 * 
 * @author Kiruthika
 *
 */

public class UnifiedProposalCreatePOHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(UnifiedProposalCreatePOHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      // variable declaration
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject inspectLines = jsonparams.getJSONObject("Create PO");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");
      // JSONArray allLines = inspectLines.getJSONArray("_allRows");

      JSONObject successMessage, selectedRow;
      JSONObject json = new JSONObject();

      final String clientId = vars.getClient();
      final String userId = vars.getUser();
      String Lang = vars.getLanguage();

      String proposalId;
      BigDecimal awardedAmt;

      // check selected line should be greater than zero
      if (selectedlines.length() == 0) {
        successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

      for (int i = 0; i < selectedlines.length(); i++) {
        selectedRow = selectedlines.getJSONObject(i);
        proposalId = selectedRow.getString("id");
        awardedAmt = new BigDecimal(selectedRow.getString("awardedAmount"));
        UnifiedProposalCreatePO dao = new UnifiedProposalCreatePOImpl();
        OBError result = dao.createPO(proposalId, clientId, userId, Lang, awardedAmt);

        if (result.getType().equals("error")) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMessage = new JSONObject();
          String message = OBMessageUtils.parseTranslation(result.getMessage());
          errorMessage.put("severity", "error");
          errorMessage.put("text", message);
          json.put("message", errorMessage);
          json.put("retryExecution", true);
          return json;

        }
      }

      successMessage = new JSONObject();
      successMessage.put("severity", "success");
      successMessage.put("text", OBMessageUtils.messageBD("Escm_CreatePOSuccess"));
      json.put("message", successMessage);
      return json;

    } catch (Exception e) {
      log.error("Exception in POContractInspectionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    }

  }
}
