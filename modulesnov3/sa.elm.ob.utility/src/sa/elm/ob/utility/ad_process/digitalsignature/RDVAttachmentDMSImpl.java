package sa.elm.ob.utility.ad_process.digitalsignature;

import org.apache.commons.io.FilenameUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.dms.consumer.GRPDmsImplementation;
import sa.elm.ob.utility.dms.consumer.GRPDmsInterface;
import sa.elm.ob.utility.dms.consumer.dto.AddAttachmentResponseGRP;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;
import sa.elm.ob.utility.dms.util.GetServiceAccount;
import sa.elm.ob.utility.properties.Resource;

public class RDVAttachmentDMSImpl extends AttachmentDMSInterface {
  private static final Logger log = LoggerFactory.getLogger(RDVAttachmentDMSImpl.class);

  @Override
  public void addAttachment(String tabId, String recordId, String... attachmentId) {
    try {

      EfinRDVTransaction rdvtrnsaction = OBDal.getInstance().get(EfinRDVTransaction.class,
          recordId);
      String userId = OBContext.getOBContext().getUser().getId();
      String profileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);
      String nodeID = GetServiceAccount.getProperty(DMSConstants.DMS_NODE_ID);

      GRPDmsInterface dmsGRP = new GRPDmsImplementation();
      CreateAttachmentResponseGRP response = null;
      AddAttachmentResponseGRP addAttachmentResponse = null;

      if (rdvtrnsaction != null) {
        EfinRDV rdv = rdvtrnsaction.getEfinRdv();
        String document_type = rdvtrnsaction.isLastversion() ? Resource.RDV_LAST_VERSION
            : Resource.RDV_Transaction;

        for (String id : attachmentId) {
          Attachment attachment = OBDal.getInstance().get(Attachment.class, id);
          String fileBase64 = getBase64String(attachment);
          String extension = FilenameUtils.getExtension(attachment.getName());
          if (fileBase64 != null && attachment.getEutDmsAttachpath() == null
              && "pdf".equals(extension)) {
            if (rdvtrnsaction.getEutDmsrecordpath() != null) {
              log.debug("Inside the add attachment block");
              DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null,
                  DMSConstants.DMS_ADD, rdvtrnsaction);
              String recProfileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);

              addAttachmentResponse = dmsGRP.addAttachmentinDMS(
                  recProfileURI.concat(rdvtrnsaction.getEutDmsrecordpath()), fileBase64,
                  attachment.getName(), attachment.getName());
              log.debug("Response from server" + addAttachmentResponse);

              if (addAttachmentResponse != null) {
                if (!addAttachmentResponse.isHasError()) {
                  dmslog.setResponsemessage(addAttachmentResponse.getResponse());
                  dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
                  dmslog.setAttachmentpath(addAttachmentResponse.getAttachmentPath());
                  dmslog.setRequest(addAttachmentResponse.getRequest());
                  dmslog.setResponsemessage(addAttachmentResponse.getResponse());
                  attachment.setEutDmsAttachpath(addAttachmentResponse.getAttachmentPath());
                  OBDal.getInstance().save(attachment);
                  OBDal.getInstance().save(dmslog);
                } else {
                  dmslog.setApprovalposition(rdvtrnsaction.getEutApprovalPosition());
                  dmslog.setResponsemessage(addAttachmentResponse.getErrorMsg());
                  dmslog.setRequest(addAttachmentResponse.getRequest());
                  dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                  OBDal.getInstance().save(dmslog);
                }
              }
              OBDal.getInstance().flush();

            } else {
              rdvtrnsaction.setEUTDocumentType(document_type);
              rdvtrnsaction.setEutApprovalPosition(DMSConstants.DEFAULT_APPROVAL_POSITION);
              OBDal.getInstance().save(rdvtrnsaction);
              OBDal.getInstance().flush();
              log.debug("Inside the create record and attachment block");

              DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null,
                  DMSConstants.DMS_CREATE, rdvtrnsaction);
              DMSXmlAttributes dmsAttributes = new DMSXmlAttributes(document_type,
                  rdvtrnsaction.getId(), rdv.getDocumentNo(), dmslog.getId(), userId);

              response = dmsGRP.sendReportToDMS(profileURI, fileBase64, nodeID,
                  rdv.getDocumentNo() + "-" + rdvtrnsaction.getTXNVersion(),
                  rdv.getDocumentNo() + "-" + rdvtrnsaction.getTXNVersion(), dmsAttributes);
              log.info("dmsreonseee:" + response);
              if (response != null) {
                if (!response.isHasError()) {
                  log.info("dmsreonseee1:" + response);
                  dmslog.setPkirequest(null);
                  dmslog.setApprovalposition(null);
                  dmslog.setResponsemessage(response.getResponse());
                  dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
                  dmslog.setAttachmentpath(response.getAttachmentPath());
                  log.info("dmsreonseee1 getRequest:" + response.getRequest().toString());
                  dmslog.setRequest(response.getRequest().toString());
                  log.info("dmsreonseee1 getResponse():" + response.getResponse());
                  dmslog.setResponsemessage(response.getResponse());
                  rdvtrnsaction.setEutDmsrecordpath(response.getRecordPath());
                  attachment.setEutDmsAttachpath(response.getAttachmentPath());
                  OBDal.getInstance().save(attachment);
                  OBDal.getInstance().save(rdvtrnsaction);
                  OBDal.getInstance().save(dmslog);
                } else {
                  dmslog.setApprovalposition(rdvtrnsaction.getEutApprovalPosition());
                  dmslog.setResponsemessage(response.getErrorMsg());
                  dmslog.setRequest(response.getRequest());
                  dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                  OBDal.getInstance().save(dmslog);
                }
              }
              OBDal.getInstance().flush();
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Error while adding attachment " + e.getMessage());
    }

  }

}
