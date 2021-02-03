package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.actionHandler.dao.BidMgmtActionHandlerDAO;
import sa.elm.ob.scm.actionHandler.dao.BidMgmtActionHandlerDAOImpl;
import sa.elm.ob.utility.util.Utility;

public class BidMgmtActionHandler extends BaseActionHandler {
  private static final Logger log = Logger.getLogger(BidMgmtActionHandler.class);
  private static String RECORD_ID = "Escm_Bidmgmt_ID";
  private static String BIDMGMT_ACTION_PARAM = "Action";
  private static String OPENENVELOP_TABID = "8095B818800446D795B8ADFEDE104733";
  private static String TEE_TABID = "7185D00B421A4F62B403E085F00176D6";
  private static String PEE_TABID = "61D6CF3612134CAF942B811EC74B1F0B";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    try {
      JSONObject result = new JSONObject();

      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");
      final String bidManagementId = request.getString(RECORD_ID);
      String action = (String) params.get(BIDMGMT_ACTION_PARAM);
      HttpServletRequest requestVar = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(requestVar);
      String proposal_action = null;
      String supplier = null;
      String removeTransaction = null;
      String contractCategoryID = null;

      if (params.has("Proposal_Action") && action.equals("PRO")
          && params.get("Proposal_Action") != null)
        proposal_action = (String) params.get("Proposal_Action");

      if (params.has("Supplier") && proposal_action != null && proposal_action.equals("PC")
          && params.get("Supplier") != null)
        supplier = (String) params.get("Supplier");

      if (params.has("RemoveTransaction") && action != null && action.equals("DELTRNS")
          && params.get("RemoveTransaction") != null)
        removeTransaction = (String) params.get("RemoveTransaction");

      if (action != null && action.equals("PRO") && proposal_action != null
          && !proposal_action.equals("PD") && params.has("Contract_Category_ID")
          && params.get("Contract_Category_ID") != null) {
        contractCategoryID = (String) params.get("Contract_Category_ID");
      }

      BidMgmtActionHandlerDAO dao = new BidMgmtActionHandlerDAOImpl();

      // Execute process and prepare an array with actions to be executed after execution
      JSONArray actions = new JSONArray();
      JSONObject recordInfo = new JSONObject();

      // open the oee tab when click option of oee creation
      JSONObject openTabAction = new JSONObject();

      // open the pee tab when click option of pee creation
      JSONObject peeTabAction = new JSONObject();
      String msgText = null;
      OBContext.setAdminMode();

      if (action.equals("OEECD")) {

        JSONObject resultOEEJson = dao.checkorcreateOEE(bidManagementId);
        if (resultOEEJson.has("result")) {
          if (resultOEEJson.getString("result").equals("1")) {
            recordInfo.put("tabId", OPENENVELOP_TABID);
            recordInfo.put("recordId", resultOEEJson.getString("openId"));
            recordInfo.put("wait", true);
            openTabAction.put("openDirectTab", recordInfo);
            actions.put(openTabAction);
            result.put("responseActions", actions);
            return result;
          } else if (resultOEEJson.getString("result").equals("0")) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("severity", "error");
            if (resultOEEJson.getString("errorMsg").contains("@")) {
              msgText = resultOEEJson.getString("errorMsg").replace("@", "");
              msgText = OBMessageUtils.messageBD(msgText);
            } else {
              msgText = resultOEEJson.getString("errorMsg");
            }
            errorMsg.put("text", msgText);
            result.put("message", errorMsg);
            return result;
          }
        }

      }
      // TEE creation
      if (action.equals("TEECD")) {

        JSONObject resultTEEJson = dao.checkorcreateTEE(bidManagementId);
        if (resultTEEJson.has("result")) {
          if (resultTEEJson.getString("result").equals("1")) {
            recordInfo.put("tabId", TEE_TABID);
            recordInfo.put("recordId", resultTEEJson.getString("teeId"));
            recordInfo.put("wait", true);
            peeTabAction.put("openDirectTab", recordInfo);
            actions.put(peeTabAction);
            result.put("responseActions", actions);
            return result;
          } else if (resultTEEJson.getString("result").equals("0")) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("severity", "error");
            if (resultTEEJson.getString("errorMsg").contains("@")) {
              msgText = resultTEEJson.getString("errorMsg").replace("@", "");
              msgText = OBMessageUtils.messageBD(msgText);
            } else {
              msgText = resultTEEJson.getString("errorMsg");
            }
            errorMsg.put("text", msgText);
            result.put("message", errorMsg);
            return result;
          }
        }

      }
      if (action.equals("PRO")) {
        OBContext.setAdminMode();
        boolean contCatgMatch = true;
        EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class, bidManagementId);
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
          JSONObject resultProposalJson = dao.createProposal(bidManagementId, proposal_action,
              supplier, contractCategoryID);
          if (resultProposalJson.has("result")) {
            if (resultProposalJson.getString("result").equals("1")
                && resultProposalJson.has("proposalList")) {
              openTabAction.put("openDirectTabWithMultiRecord",
                  resultProposalJson.get("proposalList"));
              actions.put(openTabAction);

              if (!proposal_action.equals("PD")) {
                JSONObject msgInTab = new JSONObject();
                msgInTab.put("msgType", "success");
                msgInTab.put("msgTitle", "Success");
                msgInTab.put("msgText",
                    OBMessageUtils.messageBD("ESCM_BidActionProcess_ComSucess"));

                JSONObject msgInTabAction = new JSONObject();
                msgInTabAction.put("showMsgInView", msgInTab);

                actions.put(msgInTabAction);
              }

              result.put("responseActions", actions);
              return result;
            } else if (resultProposalJson.getString("result").equals("0")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMsg = new JSONObject();
              errorMsg.put("severity", "error");
              if (resultProposalJson.getString("errorMsg").contains("@")) {
                msgText = resultProposalJson.getString("errorMsg").replace("@", "");
                msgText = OBMessageUtils.messageBD(msgText);
              } else {
                msgText = resultProposalJson.getString("errorMsg");
              }
              errorMsg.put("text", msgText);
              result.put("message", errorMsg);
              return result;
            } else if (resultProposalJson.getString("result").equals("2")
                && resultProposalJson.has("proposalList")) {

              openTabAction.put("openDirectTabWithMultiRecord",
                  resultProposalJson.get("proposalList"));
              actions.put(openTabAction);

              JSONObject msgInBPTab = new JSONObject();
              msgInBPTab.put("msgType", "success");
              msgInBPTab.put("msgTitle", "Process execution");
              msgInBPTab.put("msgText", OBMessageUtils.messageBD("ESCM_PropCreatedAllSuply"));

              JSONObject msgInBPTabAction = new JSONObject();
              msgInBPTabAction.put("showMsgInView", msgInBPTab);

              actions.put(msgInBPTabAction);
              result.put("responseActions", actions);
              return result;
            } else if (resultProposalJson.getString("result").equals("3")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMsg = new JSONObject();
              errorMsg.put("severity", "error");
              errorMsg.put("text", OBMessageUtils.messageBD("ESCM_NoSupplyToProp"));
              result.put("message", errorMsg);
              return result;
            } else if (resultProposalJson.getString("result").equals("4")) {
              OBDal.getInstance().rollbackAndClose();
              JSONObject errorMsg = new JSONObject();
              errorMsg.put("severity", "error");
              errorMsg.put("text", OBMessageUtils.messageBD("ESCM_NoProposals_Available"));
              result.put("message", errorMsg);
              return result;
            }
          }
        }
      }

      // pee creation
      if (action.equals("PEECD")) {
        JSONObject resultPEEJson = dao.createPee(bidManagementId);
        if (resultPEEJson.has("result")) {
          if (resultPEEJson.getString("result").equals("1")) {
            recordInfo.put("tabId", PEE_TABID);
            recordInfo.put("recordId", resultPEEJson.getString("peeId"));
            recordInfo.put("wait", true);
            peeTabAction.put("openDirectTab", recordInfo);
            actions.put(peeTabAction);
            if (resultPEEJson.has("BGDetail") && resultPEEJson.getString("BGDetail") != null) {
              JSONObject msgInTab = new JSONObject();
              msgInTab.put("msgType", "info");
              msgInTab.put("msgTitle", "info");
              msgInTab.put("msgText", resultPEEJson.getString("BGDetail"));

              JSONObject msgInTabAction = new JSONObject();
              msgInTabAction.put("showMsgInView", msgInTab);

              actions.put(msgInTabAction);
            }

            result.put("responseActions", actions);
            return result;
          } else if (resultPEEJson.getString("result").equals("0")) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("severity", "error");
            if (resultPEEJson.getString("errorMsg").contains("@")) {
              msgText = resultPEEJson.getString("errorMsg").replace("@", "");
              msgText = OBMessageUtils.messageBD(msgText);
            }
            if (resultPEEJson.getString("errorMsg").contains("@")
                && resultPEEJson.getString("errorMsg").contains("@ESCM_Proposal_Zero@")) {
              String[] res = resultPEEJson.getString("errorMsg").split("@", 0);
              String proposalNo = res[2];
              msgText = res[1].replace("@", "");
              msgText = OBMessageUtils.messageBD(msgText).concat(proposalNo);
            } else {
              msgText = resultPEEJson.getString("errorMsg");
            }
            errorMsg.put("text", msgText);
            result.put("message", errorMsg);
            return result;
          }
        }
      }

      if (action.equals("DELTRNS")) {
        JSONObject validJson = dao.checkValidationBeforeRemTran(bidManagementId, removeTransaction);
        if (validJson.has("result")) {
          if (validJson.getString("result").equals("0")) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("severity", "error");
            if (validJson.getString("errorMsg").contains("@")) {
              msgText = validJson.getString("errorMsg").replace("@", "");
              msgText = OBMessageUtils.messageBD(msgText);
            } else {
              msgText = validJson.getString("errorMsg");
            }
            errorMsg.put("text", msgText);
            result.put("message", errorMsg);
            return result;
          } else if (validJson.getString("result").equals("1")) {
            JSONObject removeTransJSON = dao.removeTransactions(bidManagementId, removeTransaction,
                vars);
            if (removeTransJSON.has("result")) {
              if (removeTransJSON.getString("result").equals("0")) {
                OBDal.getInstance().rollbackAndClose();
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("severity", "error");
                if (removeTransJSON.getString("errorMsg").contains("@")) {
                  msgText = removeTransJSON.getString("errorMsg").replace("@", "");
                  msgText = OBMessageUtils.messageBD(msgText);
                } else {
                  msgText = removeTransJSON.getString("errorMsg");
                }
                errorMsg.put("text", msgText);
                result.put("message", errorMsg);
                return result;
              }
              if (removeTransJSON.getString("result").equals("2")) {
                OBDal.getInstance().rollbackAndClose();
                JSONObject errorMsg = new JSONObject();
                errorMsg.put("severity", "error");
                if (removeTransJSON.getString("successMsg").contains("@")) {
                  msgText = removeTransJSON.getString("successMsg").replace("@", "");
                  msgText = OBMessageUtils.messageBD(msgText);
                } else {
                  msgText = removeTransJSON.getString("successMsg");
                }
                errorMsg.put("text", msgText);
                result.put("message", errorMsg);
                return result;
              } else {
                JSONObject successMsg = new JSONObject();
                successMsg.put("severity", "success");
                successMsg.put("text", OBMessageUtils.messageBD("ESCM_BidActionProcess_ComSucess"));
                result.put("message", successMsg);
                return result;
              }
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
