package sa.elm.ob.scm.actionHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopalakrishnan on 16/06/2020
 *
 */

public class PurchaseRequisitionAction extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CreatePOSubmit.class);
  private static String RECORD_ID = "M_Requisition_ID";
  private static String BIDMGMT_TABID = "31960EC365D746A180594FFB7B403ABB";
  private static String PROP_TABID = "D6115C9AF1DD4C4C9811D2A69E42878B";
  private static String BIDMGMT_DIRECT = "BM-DR";
  private static String BIDMGMT_TENDERANDDIRECT = "BM-TR-LD";
  private static String PROPOSALMGMT = "PMG";

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    JSONObject jsonResponse = new JSONObject();
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    EscmProposalMgmt proposalMgmt = null;
    JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode();
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String purchase_requistion_id = jsonRequest.getString(RECORD_ID);
      JSONObject openTabAction = new JSONObject();
      JSONObject recordInfo = new JSONObject();
      JSONArray actions = new JSONArray();

      String pr_action = jsonparams.getString("praction");
      String is_pee_required = jsonparams.getString("peerequired");
      String comments = jsonparams.getString("comments");
      String supplier = jsonparams.getString("supplier");
      String contractCategory = null;
      if (jsonparams.has("Contract Category"))
        contractCategory = jsonparams.getString("Contract Category");

      String sequence = "";
      Boolean sequenceexists = false;

      PurchaseRequisitionActionDAO dao = new PurchaseRequisitionActionDAOImpl();
      Requisition objRequistion = OBDal.getInstance().get(Requisition.class,
          purchase_requistion_id);

      if ((pr_action.equals("ESCM_RE")) && (comments.equals(null) || comments.equals("null"))) {
        JSONObject erorMessage = new JSONObject();
        erorMessage.put("severity", "error");
        erorMessage.put("text", OBMessageUtils.messageBD("ESCM_Comments_Mandatory"));
        json.put("message", erorMessage);
        json.put("retryExecution", true);
        return json;
      }

      if (objRequistion.getEscmDocStatus().equals("ESCM_CA")
          || objRequistion.getEscmDocStatus().equals("ESCM_REJ")) {
        JSONObject erorMessage = new JSONObject();
        erorMessage.put("severity", "error");
        erorMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Action_Not_Possible"));
        json.put("message", erorMessage);
        return json;
      }
      // For Creating the Auto Bid
      if (pr_action.equals("ESCM_BID")) {

        if (objRequistion.getEscmProcesstype().equals("DP")) {
          sequence = Utility.getTransactionSequence(objRequistion.getOrganization().getId(),
              BIDMGMT_DIRECT);
          sequenceexists = Utility.chkTransactionSequence(objRequistion.getOrganization().getId(),
              BIDMGMT_DIRECT, sequence);
        }

        if (objRequistion.getEscmProcesstype().equals("PB")
            || objRequistion.getEscmProcesstype().equals("LB")) {
          sequence = Utility.getTransactionSequence(objRequistion.getOrganization().getId(),
              BIDMGMT_TENDERANDDIRECT);
          sequenceexists = Utility.chkTransactionSequence(objRequistion.getOrganization().getId(),
              BIDMGMT_TENDERANDDIRECT, sequence);
        }

        if (!sequenceexists) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
          json.put("message", erorMessage);
          return json;
        }
        // set new Spec No
        if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_NoSequence"));
          json.put("message", erorMessage);
          return json;
        } else {
          EscmBidMgmt obj_bid = dao.createAutoBidFromPR(objRequistion, vars, contractCategory);

          // Execute process and prepare an array with actions to be executed after execution

          if (obj_bid != null) {
            recordInfo.put("tabId", BIDMGMT_TABID);
            recordInfo.put("recordId", obj_bid.getId());
            recordInfo.put("wait", true);
            openTabAction.put("openDirectTab", recordInfo);
            actions.put(openTabAction);
            result.put("responseActions", actions);
            JSONObject successMessage = new JSONObject();
            String msg = OBMessageUtils.messageBD("Escm_PR_Action_Bid_Created");
            successMessage.put("severity", "success");
            successMessage.put("text", msg.replace("@", obj_bid.getBidno()));
            result.put("message", successMessage);
            return result;
          } else {
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("ESCM_ProcessFailed"));
            json.put("message", erorMessage);
            return json;
          }
        }
      }
      // For Creating the Proposal from PR
      else if (pr_action.equals("ESCM_PR")) {
        if (supplier == null || supplier.equals("") || supplier.equals("null")) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text",
              OBMessageUtils.messageBD("Escm_Supplier_IsMandatory_For_Proposal"));
          json.put("message", erorMessage);
          return json;
        }
        if (objRequistion.getEscmProcesstype().equals("DP")) {

          sequence = Utility.getTransactionSequence(objRequistion.getOrganization().getId(),
              PROPOSALMGMT);
          sequenceexists = Utility.chkTransactionSequence(objRequistion.getOrganization().getId(),
              PROPOSALMGMT, sequence);
          if (!sequenceexists) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
            json.put("message", erorMessage);
            return json;
          }
          // set new Spec No
          if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject erorMessage = new JSONObject();
            erorMessage.put("severity", "error");
            erorMessage.put("text", OBMessageUtils.messageBD("Escm_NoSequence"));
            json.put("message", erorMessage);
            return json;
          } else {
            EscmProposalMgmt obj_proposal = dao.createAutoProposalFromPR(objRequistion, vars,
                is_pee_required, supplier);
            if (obj_proposal != null) {
              recordInfo.put("tabId", PROP_TABID);
              recordInfo.put("recordId", obj_proposal.getId());
              recordInfo.put("wait", true);
              openTabAction.put("openDirectTab", recordInfo);
              actions.put(openTabAction);
              result.put("responseActions", actions);
              JSONObject successMessage = new JSONObject();
              String msg = OBMessageUtils.messageBD("Escm_Proposal_Created_From_PR");
              successMessage.put("severity", "success");
              successMessage.put("text", msg.replace("@", obj_proposal.getProposalno()));
              result.put("message", successMessage);
              return result;
            } else {
              JSONObject erorMessage = new JSONObject();
              erorMessage.put("severity", "error");
              erorMessage.put("text", OBMessageUtils.messageBD("ESCM_ProcessFailed"));
              json.put("message", erorMessage);
              return json;
            }
          }
        } else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_Not_Direct_PR_FOR_Proposal"));
          json.put("message", erorMessage);
          return json;
        }

      }

      // For Return the PR from Purchase manager
      else if (pr_action.equals("ESCM_RE")) {
        Boolean isPrReturned = dao.returnPR(objRequistion, vars, comments);
        if (isPrReturned) {

          objRequistion.setEscmSendnotification(true);
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          successMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Return_Successfully"));
          json.put("message", successMessage);
          return json;
        } else {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_ProcessFailed"));
          json.put("message", erorMessage);
          return json;
        }

      } else {
        JSONObject erorMessage = new JSONObject();
        erorMessage.put("severity", "error");
        erorMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Action_Not_Allowed"));
        json.put("message", erorMessage);
        return json;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in PurchaseRequisitionAction :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonResponse;
  }

}
