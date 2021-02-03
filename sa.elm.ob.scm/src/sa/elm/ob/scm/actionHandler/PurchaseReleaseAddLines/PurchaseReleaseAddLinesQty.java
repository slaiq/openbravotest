package sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import sa.elm.ob.scm.actionHandler.PurchaseReleaseDAO;

public class PurchaseReleaseAddLinesQty extends BaseActionHandler {

  private static final Logger log = LoggerFactory.getLogger(PurchaseReleaseAddLinesQty.class);

  /**
   * This process is used to add purchase agreement lines in Purchase Release
   */

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    JSONObject jsonResponse = new JSONObject();
    BigDecimal lineNetAmt = BigDecimal.ZERO;
    BigDecimal unitPrice = BigDecimal.ZERO;
    BigDecimal totalAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject agreementLines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = agreementLines.getJSONArray("_selection");
      String orderId = jsonRequest.getString("inpcOrderId");
      String lineNumber = null, lineNos = null;
      Boolean exceeds = false;
      PoReleaseAddLinesDAO dao = new PoReleaseAddLinesDAOImpl();
      PurchaseReleaseDAO purchaseReleaseDao = new PurchaseReleaseDAO();
      Order releaseHdr = OBDal.getInstance().get(Order.class, orderId);
      int status = 1;
      String message = "";
      Order agreement = null;
      // getting decimal format from format.xml
      final UIDefinitionController.FormatDefinition formatDef = UIDefinitionController.getInstance()
          .getFormatDefinition("euro", "Relation");
      DecimalFormat decimal = new DecimalFormat(formatDef.getFormat());
      Integer roundoffConst = decimal.getMaximumFractionDigits();
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
            BigDecimal releaseQty = new BigDecimal(selectedRow.getString("releaseQty"));
            if (releaseQty.compareTo(BigDecimal.ZERO) < 0) {
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", OBMessageUtils.messageBD("ESCM_NEGATIVE_QTY"));
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
            String agreementLineId = selectedRow.getString("salesOrderLine");
            String releaseQty = selectedRow.getString("releaseQty");

            OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class,
                agreementLineId);
            agreement = selectedAgreementLine.getSalesOrder();
            if (!StringUtils.isEmpty(releaseQty)) {

              // Get Purchase Agreement Line Details
              BigDecimal inProgressReleaseQty = purchaseReleaseDao
                  .getInProgressReleaseQty(agreementLineId, null);
              BigDecimal remainingQty = selectedAgreementLine.getOrderedQuantity()
                  .subtract(selectedAgreementLine.getEscmReleaseqty())
                  .subtract(inProgressReleaseQty);
              BigDecimal selectedLineReleasedQty = new BigDecimal(releaseQty);
              lineNumber = selectedAgreementLine.getLineNo().toString();
              // get unitprice for min/max release validation
              unitPrice = selectedAgreementLine.getLineNetAmount()
                  .divide(selectedAgreementLine.getOrderedQuantity());
              lineNetAmt = unitPrice.multiply(selectedLineReleasedQty);
              totalAmt = totalAmt.add(lineNetAmt);

              if (selectedLineReleasedQty.compareTo(remainingQty) > 0) {
                // message =
                // String.format(OBMessageUtils.messageBD("ESCM_PurAgrmtGreaterReleaseQty"),
                // remainingQty, releaseQty, lineNumber);
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
              message = String.format(OBMessageUtils.messageBD("ESCM_PurAgrmtGreaterReleaseQty"),
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
            && agreement.getEscmMinRelease().compareTo(totalAmt) > 0)) {
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
        BigDecimal releaseQty = new BigDecimal(selectedRow.getString("releaseQty"));

        OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class, agreementLineId);

        // Tax Calculation
        JSONObject taxObject = dao.calculateTaxAmount(releaseHdr, selectedAgreementLine,
            roundoffConst, releaseQty);

        if ((!selectedAgreementLine.isEscmIssummarylevel())
            && releaseQty.compareTo(BigDecimal.ZERO) == 0) {
          status = dao.deleteAgreementLine(agreementLineId, releaseHdr);
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
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
