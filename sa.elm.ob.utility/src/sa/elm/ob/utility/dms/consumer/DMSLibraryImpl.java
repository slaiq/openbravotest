package sa.elm.ob.utility.dms.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ServiceAccount;
import sa.elm.ob.utility.dms.org.tempuri.AddAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.AddVersionAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.AttachmentsBulkResponse;
import sa.elm.ob.utility.dms.org.tempuri.BuildTreeRequest;
import sa.elm.ob.utility.dms.org.tempuri.CreateRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.CreateRecordWithAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DMSLibrary;
import sa.elm.ob.utility.dms.org.tempuri.DeleteAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DeleteRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetAttachmentsBulkRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.IDMSLibrary;
import sa.elm.ob.utility.dms.org.tempuri.ModifyRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.Response;

public class DMSLibraryImpl implements IDMSLibrary {

  private static final Logger log = LoggerFactory.getLogger(DMSLibraryImpl.class);

  @Override
  public Response buildTree(BuildTreeRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for buildTree started");
    Response response = getDMSService().buildTree(parameters, dmsClientID, serviceAccount);
    return response;
  }

  @Override
  public Response createRecordWithAttachment(CreateRecordWithAttachmentRequest parameters,
      String dmsClientID, ServiceAccount serviceAccount) {
    log.debug("Request for createRecordWithAttachment started");
    Response response = getDMSService().createRecordWithAttachment(parameters, dmsClientID,
        serviceAccount);
    return response;
  }

  @Override
  public Response getRecord(GetRecordRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for getRecord started");
    Response response = getDMSService().getRecord(parameters, dmsClientID, serviceAccount);
    return response;
  }

  @Override
  public Response createRecord(CreateRecordRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for createRecord started");
    Response response = getDMSService().createRecord(parameters, dmsClientID, serviceAccount);
    return response;
  }

  @Override
  public Response modifyRecord(ModifyRecordRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for modifyRecord started");
    Response response = getDMSService().modifyRecord(parameters, dmsClientID, serviceAccount);
    return response;
  }

  @Override
  public Response deleteRecord(DeleteRecordRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for deleteRecord started");
    Response response = getDMSService().deleteRecord(parameters, dmsClientID, serviceAccount);
    return response;
  }

  @Override
  public Response getAttachment(GetAttachmentRequest parameters, String dmsClientID,
      String ipAddress, ServiceAccount serviceAccount, String token) {
    log.debug("Request for getAttachment started");
    Response response = getDMSService().getAttachment(parameters, dmsClientID, ipAddress,
        serviceAccount, token);
    return response;
  }

  @Override
  public AttachmentsBulkResponse getAttachmentsBulk(GetAttachmentsBulkRequest parameters,
      String dmsClientID, ServiceAccount serviceAccount) {
    log.debug("Request for getAttachmentsBulk started");
    AttachmentsBulkResponse response = getDMSService().getAttachmentsBulk(parameters, dmsClientID,
        serviceAccount);
    return response;
  }

  @Override
  public Response addAttachment(AddAttachmentRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for addAttachment started");
    Response response = getDMSService().addAttachment(parameters, dmsClientID, serviceAccount);
    return response;
  }

  @Override
  public Response addVersionAttachment(AddVersionAttachmentRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for addVersionAttachment started");
    Response response = getDMSService().addVersionAttachment(parameters, dmsClientID,
        serviceAccount);
    return response;
  }

  @Override
  public Response deleteAttachment(DeleteAttachmentRequest parameters, String dmsClientID,
      ServiceAccount serviceAccount) {
    log.debug("Request for deleteAttachment started");
    Response response = getDMSService().deleteAttachment(parameters, dmsClientID, serviceAccount);
    return response;
  }

  /**
   * Get the Library Interface
   * 
   * @return
   */
  private IDMSLibrary getDMSService() {

    DMSLibrary dmsLibrary = new DMSLibrary();
    IDMSLibrary iDmsLibrary = dmsLibrary.getBasicHttpBindingIDMSLibrary();
    return iDmsLibrary;
  }

}
