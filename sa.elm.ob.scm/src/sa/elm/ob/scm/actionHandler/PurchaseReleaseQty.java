package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.springframework.util.StringUtils;

import sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines.PoReleaseAddLinesDAO;
import sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines.PoReleaseAddLinesDAOImpl;
import sa.elm.ob.utility.util.Constants;

public class PurchaseReleaseQty extends BaseActionHandler {
  private static Logger log = Logger.getLogger(PurchaseReleaseQty.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();

    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject encumlines = jsonparams.getJSONObject("Release");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      Order agreement = null;
      String ReleaseBpartnerID = null;

      BigDecimal lineNetAmt = BigDecimal.ZERO;
      BigDecimal unitPrice = BigDecimal.ZERO;
      BigDecimal totalAmt = BigDecimal.ZERO;

      List<String> selectedLinesList = new ArrayList<>();

      String lineNumber = null, lineNos = null;
      Boolean exceeds = false;
      BigDecimal minimumRelease = BigDecimal.ZERO;

      final UIDefinitionController.FormatDefinition formatDef = UIDefinitionController.getInstance()
          .getFormatDefinition("euro", "Relation");
      DecimalFormat decimal = new DecimalFormat(formatDef.getFormat());
      Integer roundoffConst = decimal.getMaximumFractionDigits();
      PurchaseReleaseDAO dao = new PurchaseReleaseDAO();
      PoReleaseAddLinesDAO addLinesdao = new PoReleaseAddLinesDAOImpl();

      String sequence = "", seqName = null, calendarId = "";
      Organization org = null;
      // Get Purchase Agreement Header Details.
      final String agreementId = jsonRequest.getString("inpcOrderId");
      log.debug("agreementId:" + agreementId);
      agreement = OBDal.getInstance().get(Order.class, agreementId);

      // Check whether release quantity is less than or equal to zero.
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          if (selectedRow.getString("summary").equals("false")) {

            if (selectedRow.has("releaseQty")
                && new BigDecimal(selectedRow.getString("releaseQty"))
                    .compareTo(BigDecimal.ZERO) <= 0
                && selectedRow.getString("summary").equals("false")) {
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurAgrmt_NoReleaseQty"));
              json.put("retryExecution", true);
              json.put("message", errorMessage);
              return json;
            }
          }
          selectedLinesList.add(selectedRow.getString("salesOrderLine"));
        }
      }
      // Check whether user selects different suppliers
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          if (selectedRow.getString("summary").equals("false")) {

            log.debug("selectedRow:" + selectedRow);
            String agreementLineId = selectedRow.getString("salesOrderLine");
            unitPrice = BigDecimal.ZERO;
            lineNetAmt = BigDecimal.ZERO;

            String releaseQty = selectedRow.getString("releaseQty");
            String bPartnerId = selectedRow.getString("businessPartner");
            log.debug("Agreement Line Id:" + agreementLineId);

            OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class,
                agreementLineId);

            if (ReleaseBpartnerID != null) {
              // Validations - Selected lines should have same suppliers.
              if (!bPartnerId.equals(ReleaseBpartnerID)) {
                JSONObject errorMessage = new JSONObject();
                errorMessage.put("severity", "error");
                errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurAgrmt_DiffSupplier"));
                json.put("retryExecution", true);
                json.put("message", errorMessage);
                return json;

              }
            } else {
              ReleaseBpartnerID = bPartnerId;
            }

            if (!StringUtils.isEmpty(releaseQty)) {
              // Get Purchase Agreement Line Details
              BigDecimal inProgressReleaseQty = dao.getInProgressReleaseQty(agreementLineId, null);
              BigDecimal remainingQty = selectedAgreementLine.getOrderedQuantity()
                  .subtract(selectedAgreementLine.getEscmReleaseqty())
                  .subtract(inProgressReleaseQty);
              BigDecimal selectedLineReleasedQty = new BigDecimal(releaseQty);

              // get unitprice for min/max release validation
              unitPrice = selectedAgreementLine.getLineNetAmount()
                  .divide(selectedAgreementLine.getOrderedQuantity());
              lineNetAmt = unitPrice.multiply(selectedLineReleasedQty);
              totalAmt = totalAmt.add(lineNetAmt);
              lineNumber = selectedAgreementLine.getLineNo().toString();

              // Validation - Release quantity should not exceed remaining quantity to release.
              if (selectedLineReleasedQty.compareTo(remainingQty) > 0) {
                exceeds = true;
                if (lineNos == null) {
                  lineNos = lineNumber;
                } else {
                  lineNos = lineNos + "," + lineNumber;
                }
              }

            } else {
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurAgrmt_NoReleaseQty"));
              json.put("retryExecution", true);
              json.put("message", errorMessage);
              return json;
            }

            if (exceeds) {
              String message = String
                  .format(OBMessageUtils.messageBD("ESCM_PurAgrmtGreaterReleaseQty"), lineNos);
              JSONObject errorMessage = new JSONObject();
              errorMessage.put("severity", "error");
              errorMessage.put("text", message);
              json.put("retryExecution", true);
              json.put("message", errorMessage);
              return json;
            }

          }
        }
        // check min release validation
        if ((agreement.getEscmMinRelease() != null
            && agreement.getEscmMinRelease().compareTo(BigDecimal.ZERO) > 0
            && agreement.getEscmMinRelease().compareTo(totalAmt) > 0)) {

          minimumRelease = agreement.getEscmMinRelease();
          String message = String.format(OBMessageUtils.messageBD("Escm_Min/Max_Release"),
              minimumRelease);

          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", message);
          json.put("retryExecution", true);
          json.put("message", errorMessage);
          return json;
        }
      } else {
        // Throws error when user selects no line.
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurAgrmt_NoLines"));
        json.put("retryExecution", true);
        json.put("message", errorMessage);
        return json;
      }

      // Check whether document sequence is created for Purchase Release
      String accountDate = new SimpleDateFormat("dd-MM-yyyy").format(agreement.getAccountingDate());
      org = OBDal.getInstance().get(Organization.class, agreement.getOrganization().getId());

      if (org.getCalendar() != null) {
        calendarId = org.getCalendar().getId();
      } else {
        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery("select eut_parent_org ('"
            + agreement.getOrganization().getId() + "','" + agreement.getClient().getId() + "')");
        Object list = query.list().get(0);
        orgIds = ((String) list).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            calendarId = org.getCalendar().getId();
            break;
          }
        }
      }

      seqName = Constants.PURCHASE_RELEASE_DOC_SEQ;

      sequence = dao.checkDocumentSequence(accountDate, seqName, calendarId,
          agreement.getOrganization().getId());
      if (sequence.equals("0")) {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("ESCM_NoDocSeqPurRelease"));
        json.put("message", errorMessage);
        return json;
      }

      BusinessPartner supplier = OBDal.getInstance().get(BusinessPartner.class, ReleaseBpartnerID);
      // Create Purchase Release Header
      // Type, Purchase Agreement FK
      Order releaseHeader = (Order) DalUtil.copy(agreement, false);
      releaseHeader.setProcessed(false);
      releaseHeader.setEscmIspurchaseagreement(true); // Is Purchase Agreement
      releaseHeader.setEscmOrdertype("PUR_REL"); // Type : Purchase Release
      releaseHeader.setEscmPurchaseagreement(agreement); // Purchase Agreement
      releaseHeader.setEscmAppstatus("DR");
      releaseHeader.setDocumentStatus("DR");
      releaseHeader.setEscmDocaction("CO");
      releaseHeader.setDocumentAction("CO");
      releaseHeader.setGrandTotalAmount(BigDecimal.ZERO);
      releaseHeader.setSummedLineAmount(BigDecimal.ZERO);
      releaseHeader.setCreationDate(new Date());
      releaseHeader.setUpdated(new Date());
      releaseHeader.setEfinEncumbered(false);
      releaseHeader.setEscmTotPoChangeType(null);
      releaseHeader.setEscmTotPoChangeFactor(null);
      releaseHeader.setEscmTotPoChangeValue(BigDecimal.ZERO);
      releaseHeader.setEscmCalculateTaxlines(false);
      releaseHeader.setBusinessPartner(supplier);
      releaseHeader.setEscmMinRelease(BigDecimal.ZERO);
      releaseHeader.setEscmMaxRelease(BigDecimal.ZERO);
      releaseHeader.setEscmOldOrder(null);
      releaseHeader.setEscmBaseOrder(null);
      releaseHeader.setEscmRevision(new Long(0));
      OBDal.getInstance().save(releaseHeader);

      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        String agreementLineId = selectedRow.getString("id");
        BigDecimal releaseQuantity = new BigDecimal(selectedRow.getString("releaseQty"));

        OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class, agreementLineId);

        // Tax Calculation
        JSONObject taxObject = addLinesdao.calculateTaxAmount(releaseHeader, selectedAgreementLine,
            roundoffConst, releaseQuantity);

        // inserting the selected lines
        addLinesdao.insertChildLines(agreementLineId, releaseHeader, taxObject, true,
            selectedLinesList);
      }

      JSONObject successMessage = new JSONObject();
      successMessage.put("severity", "success");
      successMessage.put("text", OBMessageUtils.messageBD("Escm_PurAgrmtReleaseSuccess")
          .replace("%", releaseHeader.getDocumentNo()));
      json.put("message", successMessage);
      return json;

    } catch (OBException e) {
      log.error("Exception in PurchaseReleaseQty :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    }

    catch (Exception e) {
      log.error("Exception in PurchaseReleaseQty :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
