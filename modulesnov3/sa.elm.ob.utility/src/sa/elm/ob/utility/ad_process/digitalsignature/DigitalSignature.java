package sa.elm.ob.utility.ad_process.digitalsignature;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.dms.consumer.GRPDmsImplementation;
import sa.elm.ob.utility.dms.consumer.GRPDmsInterface;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.hook.DMSRDVCompletionHook;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;
import sa.elm.ob.utility.dms.util.GetServiceAccount;

public class DigitalSignature extends BaseActionHandler {
  private static final Logger log = LoggerFactory.getLogger(DigitalSignature.class);

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {

      OBContext.setAdminMode();
      VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId());
      ConnectionProvider conn = new DalConnectionProvider(false);
      GRPDmsInterface dmsGRP = new GRPDmsImplementation();

      final JSONObject jsonData = new JSONObject(data);
      JSONObject statusMessage = new JSONObject();
      String recordId = jsonData.getString("recordId");
      String userId = OBContext.getOBContext().getUser().getId();
      String profileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);
      String nodeID = GetServiceAccount.getProperty(DMSConstants.DMS_NODE_ID);

      DMSIntegrationLog dmslog = OBDal.getInstance().get(DMSIntegrationLog.class, recordId);

      if (dmslog != null) {

        if (!userId.equals(dmslog.getCreatedBy().getId())) {
          statusMessage.put("isError", true);
          statusMessage.put("Message", OBMessageUtils.messageBD("Efin_differentuser_dms"));
          result.put("message", statusMessage);
          return result;
        }

        if (!dmslog.getCreatedBy().isEutIssignrequired()) {
          statusMessage.put("isError", true);
          statusMessage.put("Message", OBMessageUtils.messageBD("Eut_signrequired_dms"));
          result.put("message", statusMessage);
          return result;
        }

        // TO-DO for now two if block is fine in future if we are implementing in more screen dont
        // create more if blocks. Construct a method and just pass the necessary parameeter or make
        // use of weld and hook to extend this functionality
        if (dmslog.getInvoice() != null) {
          Invoice invoice = dmslog.getInvoice();
          if ("Create".equals(dmslog.getRequestname()) && invoice.getEutDmsrecordpath() != null
              && invoice.getEutAttachPath() != null) {
            dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
            OBDal.getInstance().save(dmslog);
            OBDal.getInstance().flush();
            statusMessage.put("isError", true);
            statusMessage.put("Message", OBMessageUtils.messageBD("Efin_AlreadyPreocessed"));
            result.put("message", statusMessage);
            return result;
          }
          if (invoice.getEutDmsrecordpath() != null && invoice.getEutAttachPath() != null) {
            statusMessage.put("isError", false);
            statusMessage.put("Message", OBMessageUtils.messageBD("EUT_PKISUCCESS_SENT"));
            statusMessage.put("ProfileURI", invoice.getEutAttachPath());
            statusMessage.put("UserID", dmslog.getCreatedBy().getId());
            statusMessage.put("GrpRequestID", dmslog.getId());
            statusMessage.put("DocumentName", "Invoice_" + invoice.getDocumentNo() + ".pdf");
            statusMessage.put("position", DMSUtility.getPKIPosition(invoice.getEUTDocumentType(),
                "L" + dmslog.getApprovalposition()));
            statusMessage.put("level", "L" + invoice.getEutApprovalPosition());
            statusMessage.put("documentType", invoice.getEUTDocumentType());
            dmslog.setPkirequest(statusMessage.toString());
            OBDal.getInstance().save(dmslog);
          } else {
            // Create report and sent to DMS
            DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(invoice.getEUTDocumentType(),
                invoice.getId(), invoice.getDocumentNo(), dmslog.getId(),
                dmslog.getCreatedBy().getId());
            CreateAttachmentResponseGRP createGRPResponse = PurchaseInvoiceSubmitUtils
                .createReportAndSendToDMS(vars, invoice, conn, dmslog, dmsAttributes,
                    invoice.getDocumentNo());
            if (createGRPResponse != null) {
              if (!createGRPResponse.isHasError()) {
                statusMessage.put("isError", false);
                statusMessage.put("Message", OBMessageUtils.messageBD("Eut_createdinDMS_sentpki"));
                statusMessage.put("ProfileURI", createGRPResponse.getAttachmentPath());
                statusMessage.put("UserID", dmslog.getCreatedBy().getId());
                statusMessage.put("GrpRequestID", dmslog.getId());
                statusMessage.put("DocumentName", "Invoice_" + invoice.getDocumentNo());
                statusMessage.put("position", DMSUtility.getPKIPosition(
                    invoice.getEUTDocumentType(), "L" + dmslog.getApprovalposition()));
                statusMessage.put("level", "L" + invoice.getEutApprovalPosition());
                statusMessage.put("documentType", invoice.getEUTDocumentType());
                dmslog.setRequest(createGRPResponse.getRequest());
                dmslog.setResponsemessage(createGRPResponse.getResponse());
                dmslog.setPkirequest(statusMessage.toString());
                dmslog.setAlertStatus(DMSConstants.DMS_WAIT);
                OBDal.getInstance().save(dmslog);
              } else {
                dmslog.setRequest(createGRPResponse.getRequest());
                dmslog.setResponsemessage(createGRPResponse.getResponse());
                dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                OBDal.getInstance().save(dmslog);
                statusMessage.put("isError", true);
                statusMessage.put("Message", OBMessageUtils.messageBD("EUT_CreateDMS_failed"));
              }
            } else {
              statusMessage.put("isError", true);
              statusMessage.put("Message", OBMessageUtils.messageBD("EUT_CreateDMS_failed"));

            }
          }
        } else {

          EfinRDVTransaction rdv = dmslog.getEfinRdvtxn();
          EfinRDV rdvheader = rdv.getEfinRdv();
          CreateAttachmentResponseGRP createGRPResponse = null;

          if ("Create".equals(dmslog.getRequestname()) && rdv.getEutDmsrecordpath() != null
              && rdv.getEutAttachPath() != null) {
            dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
            OBDal.getInstance().save(dmslog);
            OBDal.getInstance().flush();
            statusMessage.put("isError", true);
            statusMessage.put("Message", OBMessageUtils.messageBD("Efin_AlreadyPreocessed"));
            result.put("message", statusMessage);
            return result;
          }
          if (rdv.getEutDmsrecordpath() != null && rdv.getEutAttachPath() != null) {
            statusMessage.put("isError", false);
            statusMessage.put("Message", OBMessageUtils.messageBD("EUT_PKISUCCESS_SENT"));
            statusMessage.put("ProfileURI", dmslog.getProfileuri());
            statusMessage.put("UserID", dmslog.getCreatedBy().getId());
            statusMessage.put("GrpRequestID", dmslog.getGrprequestid());
            statusMessage.put("page", dmslog.getPagecount());
            statusMessage.put("DocumentName",
                "RDV_" + rdvheader.getDocumentNo() + "_" + rdv.getTXNVersion() + ".pdf");
            statusMessage.put("position", DMSUtility.getPKIPosition(rdv.getEUTDocumentType(),
                "L" + dmslog.getApprovalposition()));
            statusMessage.put("level", "L" + rdv.getEutApprovalPosition());
            statusMessage.put("documentType", rdv.getEUTDocumentType());
            dmslog.setPkirequest(statusMessage.toString());
            OBDal.getInstance().save(dmslog);
          } else {
            // Create report and sent to DMS
            File file = DMSRDVCompletionHook.generateReport(rdv);
            DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(rdv.getEUTDocumentType(),
                rdv.getId(), rdvheader.getDocumentNo(), dmslog.getId(), userId);

            try (InputStream inp = new FileInputStream(file)) {
              // convert to base64 encode
              byte[] bytes = IOUtils.toByteArray(inp);
              String responseData = Base64.getEncoder().encodeToString(bytes);

              // delete file in Local Grp location
              file.delete();

              // send to DMS
              createGRPResponse = dmsGRP.sendReportToDMS(profileURI, responseData, nodeID,
                  rdvheader.getDocumentNo() + "-" + rdv.getTXNVersion(),
                  rdvheader.getDocumentNo() + "-" + rdv.getTXNVersion(), dmsAttributes);

            } catch (Exception e) {
              log.error("Exception in Generating report: ", e);
              e.printStackTrace();
              OBDal.getInstance().rollbackAndClose();
            }

            if (createGRPResponse != null) {
              if (!createGRPResponse.isHasError()) {
                statusMessage.put("isError", false);
                statusMessage.put("Message", OBMessageUtils.messageBD("Eut_createdinDMS_sentpki"));
                if (dmslog.getProfileuri() != null) {
                  statusMessage.put("ProfileURI",
                      createGRPResponse.getAttachmentPath() + "," + dmslog.getProfileuri());
                  statusMessage.put("UserID", dmslog.getCreatedBy().getId());
                  statusMessage.put("GrpRequestID",
                      dmslog.getId() + "," + dmslog.getGrprequestid());
                  statusMessage.put("DocumentName", "RDV_" + rdvheader.getDocumentNo() + "_"
                      + rdv.getTXNVersion() + ".pdf" + "," + dmslog.getDocumentname());
                  statusMessage.put("page", dmslog.getPagecount());

                } else {
                  statusMessage.put("ProfileURI", createGRPResponse.getAttachmentPath());
                  statusMessage.put("UserID", dmslog.getCreatedBy().getId());
                  statusMessage.put("GrpRequestID", dmslog.getId());
                  statusMessage.put("DocumentName",
                      "RDV_" + rdvheader.getDocumentNo() + "_" + rdv.getTXNVersion() + ".pdf");
                  statusMessage.put("page", dmslog.getPagecount());
                }

                statusMessage.put("position", DMSUtility.getPKIPosition(rdv.getEUTDocumentType(),
                    "L" + dmslog.getApprovalposition()));
                statusMessage.put("level", "L" + rdv.getEutApprovalPosition());
                statusMessage.put("documentType", rdv.getEUTDocumentType());

                dmslog.setRequest(createGRPResponse.getRequest());
                dmslog.setResponsemessage(createGRPResponse.getResponse());
                dmslog.setPkirequest(statusMessage.toString());
                dmslog.setAlertStatus(DMSConstants.DMS_WAIT);
                OBDal.getInstance().save(dmslog);
              } else {
                dmslog.setRequest(createGRPResponse.getRequest());
                dmslog.setResponsemessage(createGRPResponse.getResponse());
                dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                OBDal.getInstance().save(dmslog);
                statusMessage.put("isError", true);
                statusMessage.put("Message", OBMessageUtils.messageBD("EUT_CreateDMS_failed"));
              }
            } else {
              statusMessage.put("isError", true);
              statusMessage.put("Message", OBMessageUtils.messageBD("EUT_CreateDMS_failed"));
            }
          }

        }
      }

      OBDal.getInstance().flush();
      result.put("message", statusMessage);
    } catch (Exception e) {
      log.error("Exception in Digital signature:", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
