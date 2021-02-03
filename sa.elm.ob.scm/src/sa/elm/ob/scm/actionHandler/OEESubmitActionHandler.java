
package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.actionHandler.irtabs.PEEOnSaveHandler;
import sa.elm.ob.scm.ad_process.OpenEnvlopCommitee.OpenEnvlopCommiteeAction;

public class OEESubmitActionHandler extends BaseActionHandler {
  private static final Logger log = Logger.getLogger(OEESubmitActionHandler.class);
  private static final String processId = "4753925FCB4A4831BB3EFC9CCDA75FDE";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    try {
      {
        JSONObject result = new JSONObject();
        VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        OBContext.setAdminMode(true);
        JSONObject jsonRequest = new JSONObject(content);
        JSONObject additionalParmeter = jsonRequest.optJSONObject("_params");
        if (jsonRequest.optString("action").equals("getBGDetails")) {
          JSONObject validations = jsonRequest.has("validation")
              ? jsonRequest.getJSONObject("validation")
              : null;
          if (jsonRequest.has("oeeId")) {
            Escmopenenvcommitee openenvcommitee = OBDal.getInstance().get(Escmopenenvcommitee.class,
                jsonRequest.getString("oeeId"));
            if (openenvcommitee != null) {
              Long count = openenvcommitee.getEscmProposalAttrList().stream()
                  .filter(a -> a.getGrossPrice().compareTo(BigDecimal.ONE) > 0).count();
              if (count == 0 && (validations == null
                  || (validations != null && !validations.has("Proposalpricevalidation")))) {
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("severity", "error");
                errorMsg.put("text", OBMessageUtils.messageBD("Escm_allproposal_zerogross"));
                errorMsg.put("Proposalpricevalidation", true);
                result.put("message", errorMsg);
                return result;
              } else {
                if ((validations == null
                    || (validations != null && !validations.has("BGvalidation")))) {
                  String message = PEEOnSaveHandler
                      .integProsalAtttoProsalEventWithCustomErrorMsg(openenvcommitee);
                  if (message != null) {
                    JSONObject errorMsg = new JSONObject();
                    errorMsg.put("severity", "error");
                    errorMsg.put("text", OBMessageUtils.messageBD("Escm_OEEQuestion") + message);
                    errorMsg.put("BGvalidation", true);
                    errorMsg.put("Proposalpricevalidation", true);
                    result.put("message", errorMsg);
                    return result;
                  }
                }
              }
            }

          }
        } else {
          if (jsonRequest.has("oeeId") || jsonRequest.has("inpescmOpenenvcommiteeId")) {
            String oeeId = jsonRequest.has("oeeId") ? jsonRequest.getString("oeeId")
                : jsonRequest.getString("inpescmOpenenvcommiteeId");
            JSONObject errorMsg = new JSONObject();
            ProcessBundle pb = new ProcessBundle(processId, vars);
            pb.setCloseConnection(false);
            HashMap<String, Object> processParamters = new HashMap<String, Object>();
            processParamters.put("Escm_Openenvcommitee_ID", oeeId);
            pb.setParams(processParamters);
            new OpenEnvlopCommiteeAction().doExecute(pb);
            OBError error = (OBError) pb.getResult();
            if (error.getType().equals("success")) {
              errorMsg.put("severity", "success");
              errorMsg.put("msgType", "success");
              errorMsg.put("msgTitle", "Success");
              errorMsg.put("text", OBMessageUtils.messageBD(error.getMessage().replace("@", "")));
              result.put("message", errorMsg);
            } else {
              errorMsg.put("severity", "error");
              errorMsg.put("text", OBMessageUtils.messageBD(error.getMessage().replace("@", "")));
              result.put("message", errorMsg);
            }
          }

        }
        return result;
      }
    } catch (JSONException e) {
      log.error("Error in process", e);
      return new JSONObject();
    } catch (Exception e) {
      log.error("Error in process", e);
      return new JSONObject();
    }

  }
}
