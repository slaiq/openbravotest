package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.actionHandler.dao.BidMgmtActionHandlerDAO;
import sa.elm.ob.scm.actionHandler.dao.BidMgmtActionHandlerDAOImpl;
import sa.elm.ob.utility.util.Utility;

public class OEEProposalCreationHandler extends BaseActionHandler {
  private static final Logger log = Logger.getLogger(OEEProposalCreationHandler.class);
  private static String RECORD_ID = "Escm_Openenvcommitee_ID";
  private static String PROPOSAL_ACTION_PARAM = "Action";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    try {
      JSONObject result = new JSONObject();

      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");
      final String openEnvelopId = request.getString(RECORD_ID);
      String action = (String) params.get(PROPOSAL_ACTION_PARAM);
      String Supplier = null;
      String contractCategoryID = null;
      // if (params.has("Contract_Category_ID") && params.get("Contract_Category_ID") != null)
      // contractCategoryID = (String) params.get("Contract_Category_ID");

      if (params.has("Supplier") && action.equals("PC") && params.get("Supplier") != null)
        Supplier = (String) params.get("Supplier");
      BidMgmtActionHandlerDAO dao = new BidMgmtActionHandlerDAOImpl();

      // Execute process and prepare an array with actions to be executed after execution
      JSONArray actions = new JSONArray();

      // open the oee tab when click option of oee creation
      JSONObject openTabAction = new JSONObject();
      EscmBidMgmt bidmgmt = null;
      OBContext.setAdminMode();
      if (action != null) {
        if (openEnvelopId != null) {
          Escmopenenvcommitee openEnvelop = OBDal.getInstance().get(Escmopenenvcommitee.class,
              openEnvelopId);
          bidmgmt = openEnvelop.getBidNo();
          if (openEnvelop.getContractType() != null)
            contractCategoryID = openEnvelop.getContractType().getId();
          else {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("severity", "error");
            errorMsg.put("text", OBMessageUtils.messageBD("ESCM_ContractCatgCantBeEmpty"));
            result.put("message", errorMsg);
            return result;
          }
        }
        boolean contCatgMatch = true;

        if (contractCategoryID != null && bidmgmt != null) {
          String contCatgId = contractCategoryID;
          for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
            if (line.getProduct() != null) {
              if (line.getProduct().getESCMPRODCONTCATGList() != null
                  && line.getProduct().getESCMPRODCONTCATGList().size() != 0) {
                for (ESCMProductContCatg pContCatg : line.getProduct().getESCMPRODCONTCATGList()) {
                  if (pContCatg.getContractCategory().getId().equals(contCatgId)) {
                    contCatgMatch = true;
                    break;
                  } else {
                    if (contCatgId.equals(Utility.getConCatTypeOther())) {
                      contCatgMatch = true;
                      break;
                    } else {
                      contCatgMatch = false;
                    }
                  }
                }
                if (!contCatgMatch) {
                  OBDal.getInstance().rollbackAndClose();
                  JSONObject errorMsg = new JSONObject();
                  errorMsg.put("severity", "error");
                  errorMsg.put("text", OBMessageUtils.messageBD("ESCM_ItemMismatchWithContCatg"));
                  result.put("message", errorMsg);
                  return result;
                }
              }
            }
          }
        }
        if ((contractCategoryID != null && contCatgMatch) || (contractCategoryID == null)) {
          JSONObject resultProposalJson = dao.createProposalFromOEE(openEnvelopId, action, Supplier,
              contractCategoryID);
          if (resultProposalJson.has("result")) {
            if (resultProposalJson.getString("result").equals("1")) {

              if (resultProposalJson.has("proposalList")) {
                openTabAction.put("openDirectTabWithMultiRecord",
                    resultProposalJson.get("proposalList"));
                actions.put(openTabAction);
              }
              JSONObject msgInBPTab = new JSONObject();
              msgInBPTab.put("msgType", "success");
              msgInBPTab.put("msgTitle", "Success");
              msgInBPTab.put("msgText",
                  OBMessageUtils.messageBD("ESCM_BidActionProcess_ComSucess"));

              JSONObject msgInBPTabAction = new JSONObject();
              msgInBPTabAction.put("showMsgInView", msgInBPTab);

              actions.put(msgInBPTabAction);

              result.put("responseActions", actions);
            } else if (resultProposalJson.getString("result").equals("0")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMsg = new JSONObject();
              errorMsg.put("severity", "error");
              errorMsg.put("text", resultProposalJson.getString("errorMsg"));
              result.put("message", errorMsg);
              return result;
            } else if (resultProposalJson.getString("result").equals("2")) {
              if (resultProposalJson.has("proposalList")) {
                openTabAction.put("openDirectTabWithMultiRecord",
                    resultProposalJson.get("proposalList"));
                actions.put(openTabAction);
              }
              JSONObject msgInBPTab = new JSONObject();
              msgInBPTab.put("msgType", "success");
              msgInBPTab.put("msgTitle", "Process execution");
              msgInBPTab.put("msgText", OBMessageUtils.messageBD("ESCM_PropCreatedAllSuply"));

              JSONObject msgInBPTabAction = new JSONObject();
              msgInBPTabAction.put("showMsgInView", msgInBPTab);

              actions.put(msgInBPTabAction);
              result.put("responseActions", actions);
            } else if (resultProposalJson.getString("result").equals("3")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMsg = new JSONObject();
              errorMsg.put("severity", "error");
              errorMsg.put("text", OBMessageUtils.messageBD("ESCM_NoSupplyToProp"));
              result.put("message", errorMsg);
              return result;
            }
          }
        }
      }
      return result;
    } catch (JSONException e) {
      log.error("Error in process", e);
      return new JSONObject();
    }

  }

}
