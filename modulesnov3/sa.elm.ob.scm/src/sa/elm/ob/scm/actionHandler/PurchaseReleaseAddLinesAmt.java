package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.springframework.util.StringUtils;

import sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines.PoReleaseAddLinesDAO;
import sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines.PoReleaseAddLinesDAOImpl;

/**
 * 
 * @author DivyaPrakash JS 26.03.2019
 *
 */

public class PurchaseReleaseAddLinesAmt extends BaseActionHandler {
  private static Logger log = Logger.getLogger(PurchaseReleaseAddLinesAmt.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      PoReleaseAddLinesDAO dao = new PoReleaseAddLinesDAOImpl();
      PurchaseReleaseDAO purchaseReleaseDao = new PurchaseReleaseDAO();
      BigDecimal totalAmount = BigDecimal.ZERO;
      int status = 1;
      String lineNumber = null, lineNos = null;
      Boolean exceeds = false;
      String message = "";
      Order agreement = null;
      JSONObject agreementLines = jsonparams.getJSONObject("PurchaseReleaseAddLineAmt");
      JSONArray selectedlines = agreementLines.getJSONArray("_selection");
      String orderId = jsonRequest.getString("inpcOrderId");
      Order releaseHdr = OBDal.getInstance().get(Order.class, orderId);
      List<String> selectedLinesList = new ArrayList<>();
      BigDecimal minimumRelease = BigDecimal.ZERO;

      if (selectedlines.length() == 0) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_Line_Not_Selected@"));
        jsonResponse.put("retryExecution", true);
        jsonResponse.put("message", errormsg);
        OBDal.getInstance().rollbackAndClose();
        return jsonResponse;
      } else {

        for (int i = 0; i < selectedlines.length(); i++) {
          JSONObject selectedRow = selectedlines.getJSONObject(i);
          if (selectedRow.getString("summary").equals("false")) {
            BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("releaseamt"));
            if (releaseAmt.compareTo(BigDecimal.ZERO) < 0) {
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", OBMessageUtils.messageBD("ESCM_NEGATIVE_AMT"));
              jsonResponse.put("retryExecution", true);
              jsonResponse.put("message", errorMessage);
              return jsonResponse;
            }
          }
          selectedLinesList.add(selectedRow.getString("salesOrderLine"));
        }

        // Validation - Release quantity should not exceed remaining quantity to release.
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          if (selectedRow.getString("summary").equals("false")) {
            log.debug("selectedRow:" + selectedRow);
            String agreementLineId = selectedRow.getString("salesOrderLine");

            String releaseAmt = selectedRow.getString("releaseamt");
            log.debug("Agreement Line Id:" + agreementLineId);

            OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class,
                agreementLineId);
            agreement = selectedAgreementLine.getSalesOrder();

            boolean agreementHasTax = false;
            if (agreement.isEscmIstax() && agreement.getEscmTaxMethod() != null) {
              agreementHasTax = true;
            }

            if (!StringUtils.isEmpty(releaseAmt)) {

              // Get Purchase Agreement Line Details
              BigDecimal inProgressReleaseAmt = purchaseReleaseDao
                  .getInProgressReleaseAmt(agreementLineId, null, agreementHasTax);
              BigDecimal remainingAmt = selectedAgreementLine.getLineNetAmount()
                  .subtract(selectedAgreementLine.getEscmReleaseamt())
                  .subtract(inProgressReleaseAmt);
              BigDecimal selectedLineReleasedAmt = new BigDecimal(releaseAmt);
              lineNumber = selectedAgreementLine.getLineNo().toString();
              totalAmount = totalAmount.add(selectedLineReleasedAmt);

              // Validation - Release amount should not exceed remaining amount to release.
              if (selectedLineReleasedAmt.compareTo(remainingAmt) > 0) {
                // message =
                // String.format(OBMessageUtils.messageBD("ESCM_PurAgrmtGreaterReleaseamt"),
                // releaseAmt, remainingAmt.toPlainString(), lineNumber);
                // JSONObject errorMessage = new JSONObject();
                // errorMessage.put("severity", "error");
                // errorMessage.put("text", message);
                // jsonResponse.put("message", errorMessage);
                // return jsonResponse;
                exceeds = true;
                if (lineNos == null) {
                  lineNos = lineNumber;
                } else {
                  lineNos = lineNos + "," + lineNumber;
                }
              }
            }

            if (exceeds) {
              message = String.format(OBMessageUtils.messageBD("ESCM_PurAgrmtGreaterReleaseamt"),
                  lineNos);
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", message);
              jsonResponse.put("retryExecution", true);
              jsonResponse.put("message", errorMessage);
              return jsonResponse;
            }

          }
        }

        // check min release validation
        if ((agreement.getEscmMinRelease() != null
            && agreement.getEscmMinRelease().compareTo(BigDecimal.ZERO) > 0
            && agreement.getEscmMinRelease().compareTo(totalAmount) > 0)) {
          minimumRelease = agreement.getEscmMinRelease();
          message = String.format(OBMessageUtils.messageBD("Escm_Min/Max_Release"), minimumRelease);

          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", message);
          jsonResponse.put("retryExecution", true);
          jsonResponse.put("message", errorMessage);
          return jsonResponse;
        }
      }

      // getting selected records from AddLines Pop up
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        String agreementLineId = selectedRow.getString("id");
        BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("releaseamt"));
        OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class, agreementLineId);

        if (releaseAmt.compareTo(BigDecimal.ZERO) < 0) {
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurAgrmt_DiffSupplier"));
          jsonResponse.put("retryExecution", true);
          jsonResponse.put("message", errorMessage);
          return jsonResponse;
        }

        // Tax Calculation
        JSONObject taxObject = dao.calculateTaxAmtBased(releaseHdr, selectedAgreementLine,
            releaseAmt);

        if ((!selectedAgreementLine.isEscmIssummarylevel())
            && releaseAmt.compareTo(BigDecimal.ZERO) == 0) {
          status = dao.deleteAgreementLineAmt(agreementLineId, releaseHdr);
        } else {
          status = dao.insertChildLines(agreementLineId, releaseHdr, taxObject, true,
              selectedLinesList);
        }
        if (status == 0) {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "error");
          errormsg.put("text", OBMessageUtils.parseTranslation("@Efin_PenaltyRelease_Error@"));
          jsonResponse.put("message", errormsg);
          OBDal.getInstance().rollbackAndClose();
          return jsonResponse;
        }
      }

      JSONObject errormsg = new JSONObject();
      message = OBMessageUtils.parseTranslation("@Escm_PurchaseRel_Addln_Success@");
      errormsg.put("severity", "success");
      errormsg.put("text", message);
      jsonResponse.put("message", errormsg);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      return jsonResponse;

    } catch (Exception e) {
      log.error(" Exception in PurchaseReleaseAddLinesQty: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();

    }
  }

}
