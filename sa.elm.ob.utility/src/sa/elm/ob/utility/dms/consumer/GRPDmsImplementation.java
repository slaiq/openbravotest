package sa.elm.ob.utility.dms.consumer;

import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.dms.consumer.dto.AddAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.DeleteAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.DeleteRecordGRPResponse;
import sa.elm.ob.utility.dms.consumer.dto.GetAttachmentGRPResponse;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ServiceAccount;
import sa.elm.ob.utility.dms.org.tempuri.AddAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.CreateRecordWithAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DeleteAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DeleteRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.IDMSLibrary;
import sa.elm.ob.utility.dms.org.tempuri.Response;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;
import sa.elm.ob.utility.dms.util.GetServiceAccount;

public class GRPDmsImplementation implements GRPDmsInterface {

  private static final Logger log = LoggerFactory.getLogger(GRPDmsImplementation.class);

  @Override
  public CreateAttachmentResponseGRP sendReportToDMS(String profileURI, String attachmentBase64,
      String nodeID, String documentName, String description, DMSXmlAttributes attributes) {
    Response response = null;
    CreateAttachmentResponseGRP createResponseGRP = null;
    CreateRecordWithAttachmentRequest createReq = null;
    try {
      log.debug("Starting sendReportToDMS method");
      OBContext.setAdminMode();
      IDMSLibrary dmsImpl = new DMSLibraryImpl();
      createReq = DMSUtility.createRequestAttachment(profileURI, attachmentBase64, nodeID,
          documentName, description, attributes);
      ServiceAccount serviceAccount = GetServiceAccount.getServiceCredentials();
      response = dmsImpl.createRecordWithAttachment(createReq,
          GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);
      createResponseGRP = DMSUtility.createResponse(response, createReq, null);
    } catch (Exception e) {
      log.error("Errow while sending report to DMS" + e.getMessage());
      createResponseGRP = DMSUtility.createResponse(response, createReq, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("End sendReportToDMS method");

    return createResponseGRP;
  }

  @Override
  public GetAttachmentGRPResponse getReportFromDMS(String profileURI) {
    Response response = null;
    GetAttachmentGRPResponse grpResponse = null;
    GetAttachmentRequest req = null;
    try {
      log.debug("Starting getReportFromDMS method");
      OBContext.setAdminMode();
      IDMSLibrary dmsImpl = new DMSLibraryImpl();
      req = DMSUtility.getattachment(profileURI);
      ServiceAccount serviceAccount = GetServiceAccount.getServiceCredentials();
      response = dmsImpl.getAttachment(req,
          GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), null, serviceAccount, null);
      grpResponse = DMSUtility.createGRPGetResponse(response, req, null);
    } catch (Exception e) {
      log.error("Errow while sending report to DMS" + e.getMessage());
      grpResponse = DMSUtility.createGRPGetResponse(response, req, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("End getReportFromDMS method");
    return grpResponse;
  }

  @Override
  public DeleteRecordGRPResponse deleteRecordinDMS(Invoice invoice) {
    Response response = null;
    DeleteRecordGRPResponse grpResponse = null;
    DeleteRecordRequest req = null;
    DeleteAttachmentRequest attachreq = null;

    try {
      log.debug("Starting deleteRecordinDMS method");

      OBContext.setAdminMode();
      IDMSLibrary dmsImpl = new DMSLibraryImpl();
      ServiceAccount serviceAccount = GetServiceAccount.getServiceCredentials();
      String profileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);

      // Delete Attachment
      attachreq = DMSUtility.deleteAttachmentRequest(invoice.getEutAttachPath());
      response = dmsImpl.deleteAttachment(attachreq,
          GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);
      if (response != null && !response.isHasError()) {
        // Delete Record
        req = DMSUtility.deleteRecordRequest(profileURI.concat(invoice.getEutDmsrecordpath()));
        response = dmsImpl.deleteRecord(req,
            GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);
        grpResponse = DMSUtility.createGRPDeleteRecordResponse(response, req, null);
      } else {
        grpResponse = DMSUtility.createGRPDeleteRecordResponse(response, req, null);
      }
    } catch (Exception e) {
      log.error("Errow while Deleting record in DMS" + e.getMessage());
      grpResponse = DMSUtility.createGRPDeleteRecordResponse(response, req, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("End deleteRecordinDMS method");
    return grpResponse;
  }

  @Override
  public AddAttachmentResponseGRP addAttachmentinDMS(String profileURI, String attachmentBase64,
      String documentName, String description) {
    Response response = null;
    AddAttachmentResponseGRP grpResponse = null;
    AddAttachmentRequest req = null;
    try {
      log.debug("Starting addAttachmentinDMS method");
      OBContext.setAdminMode();
      IDMSLibrary dmsImpl = new DMSLibraryImpl();
      req = DMSUtility.addAttachmentRequest(profileURI, attachmentBase64, documentName,
          description);
      ServiceAccount serviceAccount = GetServiceAccount.getServiceCredentials();
      response = dmsImpl.addAttachment(req,
          GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);
      grpResponse = DMSUtility.createAddAttachmentResponse(response, req, null);
    } catch (Exception e) {
      log.error("Errow while addAttachment in DMS" + e.getMessage());
      grpResponse = DMSUtility.createAddAttachmentResponse(response, req, e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("End addAttachmentinDMS method");
    return grpResponse;
  }

  @Override
  public DeleteAttachmentResponseGRP deleteAttachmentinDMS(String profileURI,
      String attachmentPath) {
    Response response = null;
    DeleteAttachmentResponseGRP grpResponse = null;
    DeleteAttachmentRequest attachreq = null;

    try {
      log.debug("Starting deleteAttachmentinDMS method");

      OBContext.setAdminMode();
      IDMSLibrary dmsImpl = new DMSLibraryImpl();
      ServiceAccount serviceAccount = GetServiceAccount.getServiceCredentials();

      // Delete Attachment
      attachreq = DMSUtility.deleteAttachmentRequest(profileURI);
      response = dmsImpl.deleteAttachment(attachreq,
          GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);
      if (response != null && !response.isHasError()) {
        grpResponse = DMSUtility.createGRPDeleteAttachmentResponse(response, attachreq, null);
      } else {
        grpResponse = DMSUtility.createGRPDeleteAttachmentResponse(response, attachreq, null);
      }
    } catch (Exception e) {
      log.error("Errow while Deleting attachment  in DMS" + e.getMessage());
      grpResponse = DMSUtility.createGRPDeleteAttachmentResponse(response, attachreq,
          e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    log.debug("End deleteAttachmentinDMS method");
    return grpResponse;
  }

}
