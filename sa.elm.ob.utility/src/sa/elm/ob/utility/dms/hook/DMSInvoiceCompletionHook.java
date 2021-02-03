package sa.elm.ob.utility.dms.hook;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.hook.PurchaseInvoiceCompletionHook;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.PKIRequestVO;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;

public class DMSInvoiceCompletionHook implements PurchaseInvoiceCompletionHook {

  private static final Logger log = LoggerFactory.getLogger(DMSInvoiceCompletionHook.class);

  @Override
  public JSONObject exec(String strInvoiceType, Invoice header, JSONObject parameters,
      VariablesSecureApp vars, org.openbravo.database.ConnectionProvider conn) throws Exception {
    JSONObject result = new JSONObject();
    String preferenceValue = "";

    Invoice invoice = header;

    try {
      preferenceValue = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
          "Eut_AllowDMSIntegration", true, vars.getClient(), invoice.getOrganization().getId(),
          null, null, null);

      preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;
    } catch (PropertyException e) {
      preferenceValue = "N";
    }

    try {
      log.debug("Entering into dms hook");
      String documentName = "Invoice_" + invoice.getDocumentNo() + ".pdf";
      String status = parameters.getString("status");
      String docType = parameters.getString("doctype");
      String userId = parameters.getString("userId");
      String tabId = parameters.getString("tabId");

      Boolean isPrepaymentApp = parameters.getBoolean("isPrepaymentApp");
      if ("Y".equals(preferenceValue)
          && !OBContext.getOBContext().getUser().isEutIssignrequired()) {
        if (status.equals("Submit")) {
          invoice.setEUTDocumentType(docType);
          invoice.setEutApprovalPosition("1");
          OBDal.getInstance().save(invoice);
        } else {
          if (invoice.getEutApprovalPosition() != null) {
            invoice.setEutApprovalPosition(
                String.valueOf(Integer.parseInt(invoice.getEutApprovalPosition()) + 1));
            OBDal.getInstance().save(invoice);
          }
        }
      }

      if ("Y".equals(preferenceValue) && OBContext.getOBContext().getUser().isEutIssignrequired()) {

        if (status.equals("Submit")) {
          invoice.setEUTDocumentType(docType);
          invoice.setEutApprovalPosition("1");

          if (!isPrepaymentApp) {
            DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(invoice,
                DMSConstants.DMS_CREATE, null);
            DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(docType, invoice.getId(),
                invoice.getDocumentNo(), dmslog.getId(), userId);
            CreateAttachmentResponseGRP response = PurchaseInvoiceSubmitUtils
                .createReportAndSendToDMS(vars, invoice, conn, dmslog, dmsAttributes, documentName);
            if (response != null) {
              if (!response.isHasError()) {
                PKIRequestVO pkiRequest = new PKIRequestVO(response.getAttachmentPath(),
                    dmslog.getId(), userId, documentName, invoice.getEutApprovalPosition(), docType,
                    1);
                vars.setAdditionalData(tabId, pkiRequest);
                dmslog.setPkirequest(pkiRequest.toString());
                dmslog.setApprovalposition(invoice.getEutApprovalPosition());
                dmslog.setResponsemessage(response.getResponse());
                dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
                dmslog.setAttachmentpath(response.getAttachmentPath());
                dmslog.setRequest(response.getRequest());
                dmslog.setResponsemessage(response.getResponse());
                invoice.setEutAttachPath(response.getAttachmentPath());
                invoice.setEutDmsrecordpath(response.getRecordPath());
                OBDal.getInstance().save(invoice);
                OBDal.getInstance().save(dmslog);
              } else {
                dmslog.setApprovalposition(invoice.getEutApprovalPosition());
                dmslog.setResponsemessage(response.getErrorMsg());
                dmslog.setRequest(response.getRequest());
                dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                OBDal.getInstance().save(dmslog);
              }
            }
          }
          OBDal.getInstance().flush();

        } else if (status.equals("Approve")) {

          if (invoice.getEutApprovalPosition() != null) {
            invoice.setEutApprovalPosition(
                String.valueOf(Integer.parseInt(invoice.getEutApprovalPosition()) + 1));
            OBDal.getInstance().save(invoice);
          }

          if (!isPrepaymentApp) {

            if (invoice.getEutAttachPath() == null) {
              DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(invoice,
                  DMSConstants.DMS_CREATE, null);

              DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(docType, invoice.getId(),
                  invoice.getDocumentNo(), dmslog.getId(), userId);
              CreateAttachmentResponseGRP response = PurchaseInvoiceSubmitUtils
                  .createReportAndSendToDMS(vars, invoice, conn, dmslog, dmsAttributes,
                      documentName);
              if (response != null) {
                if (!response.isHasError()) {
                  PKIRequestVO pkiRequest = new PKIRequestVO(response.getAttachmentPath(),
                      dmslog.getId(), userId, documentName, invoice.getEutApprovalPosition(),
                      docType, 1);
                  vars.setAdditionalData(tabId, pkiRequest);
                  dmslog.setPkirequest(pkiRequest.toString());
                  dmslog.setApprovalposition(invoice.getEutApprovalPosition());
                  dmslog.setResponsemessage(response.getResponse());
                  dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
                  dmslog.setAttachmentpath(response.getAttachmentPath());
                  dmslog.setRequest(response.getRequest());
                  dmslog.setResponsemessage(response.getResponse());
                  invoice.setEutAttachPath(response.getAttachmentPath());
                  invoice.setEutDmsrecordpath(response.getRecordPath());
                  OBDal.getInstance().save(invoice);
                  OBDal.getInstance().save(dmslog);
                } else {
                  dmslog.setApprovalposition(invoice.getEutApprovalPosition());
                  dmslog.setResponsemessage(response.getErrorMsg());
                  dmslog.setRequest(response.getRequest());
                  dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                  OBDal.getInstance().save(dmslog);
                }
              }
              OBDal.getInstance().flush();

            } else {
              DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(invoice,
                  DMSConstants.DMS_UPDATE, null);

              PKIRequestVO pkiRequest = new PKIRequestVO(invoice.getEutAttachPath(), dmslog.getId(),
                  userId, documentName, invoice.getEutApprovalPosition(), docType, 1);

              vars.setAdditionalData(tabId, pkiRequest);
              dmslog.setPkirequest(pkiRequest.toString());
              dmslog.setApprovalposition(invoice.getEutApprovalPosition());

              OBDal.getInstance().save(dmslog);
              OBDal.getInstance().flush();
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Error while dms integration hook" + e.getMessage());
    }
    return result;
  }

}
