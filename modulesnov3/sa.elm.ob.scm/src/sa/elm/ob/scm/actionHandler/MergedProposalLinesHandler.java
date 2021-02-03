
package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMProposalEvlEvent;

/**
 * 
 * This Class is used to update the awarded qty in proposal lines and bid lines from selected lines
 * 
 * @author Sathish kumar.P
 *
 */

public class MergedProposalLinesHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptAddLines.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    try {
      // variable declaration
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject inspectLines = jsonparams.getJSONObject("Add Lines");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");
      JSONArray allLines = inspectLines.getJSONArray("_allRows");

      HashMap<String, BigDecimal> qtyBidMap = new HashMap<>();
      HashMap<String, BigDecimal> qtyProposalLineMap = new HashMap<>();
      HashMap<String, BigDecimal> qtyProposalMap = new HashMap<>();

      JSONObject successMessage, selectedRow, allRow, selectedRowProposal;
      JSONObject json = new JSONObject();

      String key, proposalLineId, proposalId;
      BigDecimal awardingQty, qty;
      String proposalEvlEvntId = null;

      if (jsonRequest.has("Escm_Proposalevl_Event_ID")) {
        proposalEvlEvntId = jsonRequest.getString("Escm_Proposalevl_Event_ID");
      }

      Boolean isValid = false, awardfullqty = false;

      // check selected line should be greater than zero
      if (selectedlines.length() == 0) {
        successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

      // get proposalevaluationevent object

      if (proposalEvlEvntId != null) {
        ESCMProposalEvlEvent event = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
            proposalEvlEvntId);
        if (event.isAwardfullqty()) {
          awardfullqty = true;
        }
      }

      // update the quantity in original proposal lines
      for (int i = 0; i < selectedlines.length(); i++) {
        selectedRow = selectedlines.getJSONObject(i);
        key = selectedRow.getString("escmBidmgmtLine");
        proposalLineId = selectedRow.getString("escmProposalmgmtLine");
        awardingQty = new BigDecimal(selectedRow.getString("awardedqty"));

        if (qtyBidMap.containsKey(key)) {
          qty = qtyBidMap.get(key);
          qtyBidMap.put(key, qty.add(awardingQty));
        } else {
          qtyBidMap.put(key, awardingQty);
        }
        qtyProposalLineMap.put(proposalLineId, awardingQty);
      }

      // forming map based on proposal and it's awarded qty
      for (int i = 0; i < selectedlines.length(); i++) {
        selectedRowProposal = selectedlines.getJSONObject(i);
        proposalId = selectedRowProposal.getString("escmProposalmgmt");
        awardingQty = new BigDecimal(selectedRowProposal.getString("awardedqty"));

        if (qtyProposalMap.containsKey(proposalId)) {
          qty = qtyProposalMap.get(proposalId);
          qtyProposalMap.put(proposalId, qty.add(awardingQty));
        } else {
          qtyProposalMap.put(proposalId, awardingQty);
        }
      }

      MergedProposal mergedProposal = new MergedProposalImpl();
      isValid = mergedProposal.validateQty(qtyBidMap, qtyProposalLineMap);

      if (isValid) {
        mergedProposal.updateAwardQtyinProposal(qtyProposalLineMap, qtyBidMap);
        mergedProposal.updateStatusInProposal(qtyProposalMap, awardfullqty);
      } else {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_Grtthanbidqty"));
        json.put("message", errorMessage);
        json.put("retryExecution", true);
        return json;
      }

      successMessage = new JSONObject();
      successMessage.put("severity", "success");
      successMessage.put("text", OBMessageUtils.messageBD("Escm_Mergedproposal_success"));
      json.put("message", successMessage);
      return json;

    } catch (Exception e) {
      log.error("Exception in POContractInspectionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    }

  }
}
