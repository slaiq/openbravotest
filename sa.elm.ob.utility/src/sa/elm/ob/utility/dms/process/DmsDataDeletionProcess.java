package sa.elm.ob.utility.dms.process;

import java.util.List;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.utility.EutDmsintegrationDeletion;
import sa.elm.ob.utility.dms.consumer.DMSLibraryImpl;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.Document;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ServiceAccount;
import sa.elm.ob.utility.dms.org.tempuri.DeleteAttachmentRequest;
import sa.elm.ob.utility.dms.org.tempuri.DeleteRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.GetRecordRequest;
import sa.elm.ob.utility.dms.org.tempuri.IDMSLibrary;
import sa.elm.ob.utility.dms.org.tempuri.Response;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.dms.util.GetServiceAccount;

public class DmsDataDeletionProcess extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(DmsDataDeletionProcess.class);
  private ProcessLogger logger;

  @SuppressWarnings("unused")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = null;
    logger = bundle.getLogger();
    Response response = null;
    DeleteAttachmentRequest attachreq = null;
    DeleteRecordRequest req = null;
    boolean deleteAttachStatus = false;
    try {
      OBContext.setAdminMode();
      logger.logln("DMS Data deletion Schedule Started.");
      String clientId = GetServiceAccount.getProperty(WebserviceConstants.CLIENT_KEY);
      vars = new VariablesSecureApp(WebserviceConstants.DEFAULT_USER_ID, clientId,
          WebserviceConstants.DEFAULT_ORG_ID, WebserviceConstants.DEFAULT_ROLE_ID);
      String profileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);

      IDMSLibrary dmsImpl = new DMSLibraryImpl();
      ServiceAccount serviceAccount = GetServiceAccount.getServiceCredentials();
      OBQuery<EutDmsintegrationDeletion> deletionList = OBDal.getInstance()
          .createQuery(EutDmsintegrationDeletion.class, " as e order by e.recordpath desc");
      List<EutDmsintegrationDeletion> deleteList = deletionList.list();

      for (EutDmsintegrationDeletion deleteRecord : deleteList) {
        deleteAttachStatus = true;

        // initially delete all the attachments
        if (deleteRecord.getRecordpath() == null) {
          logger.logln("Attachments only deletion");
          logger.logln("Attachment path:" + deleteRecord.getAttachmentpath());

          attachreq = DMSUtility.deleteAttachmentRequest(deleteRecord.getAttachmentpath());
          response = dmsImpl.deleteAttachment(attachreq, clientId, serviceAccount);
          if (response != null && !response.isHasError()) {
            // delete record from deletion history table
            // deleteList.remove(deleteRecord);
            OBDal.getInstance().remove(deleteRecord);
            OBDal.getInstance().flush();
          }
        } else {
          // delete the records if attachments linked delete attachment first.
          logger.logln("Record path:" + deleteRecord.getRecordpath());

          GetRecordRequest recordReq = DMSUtility
              .getRecordRequest(profileURI.concat(deleteRecord.getRecordpath()));
          response = dmsImpl.getRecord(recordReq,
              GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);

          // get Attachment List
          List<Document> docList = response.getResponseRoot().getValue().getDocuments().getValue()
              .getDocument();
          logger.logln("docList size:" + docList.size());

          for (Document dmsDoc : docList) {
            logger.logln("attachment path:" + dmsDoc);

            logger.logln("attachment path1:" + dmsDoc.getURI());

            logger.logln("attachment path2:" + dmsDoc.getURI().toString());
            logger.logln("attachment path:" + dmsDoc.getURI().toString());

            // call delete attachment
            attachreq = DMSUtility.deleteAttachmentRequest(dmsDoc.getURI().getValue());
            response = dmsImpl.deleteAttachment(attachreq, clientId, serviceAccount);
            if (response != null && !response.isHasError()) {
              continue;
            } else {
              deleteAttachStatus = false;
              break;
            }
          }

          // call delete record after deletion of attachment
          if (deleteAttachStatus) {
            logger.logln("deleteing reord:" + deleteRecord.getRecordpath());

            req = DMSUtility.deleteRecordRequest(profileURI.concat(deleteRecord.getRecordpath()));
            response = dmsImpl.deleteRecord(req,
                GetServiceAccount.getProperty(DMSConstants.DMS_CLIENT_ID), serviceAccount);
            if (response != null && !response.isHasError()) {
              // delete record from deletion history table
              // deleteList.remove(deleteRecord);
              OBDal.getInstance().remove(deleteRecord);
              OBDal.getInstance().flush();
            } else {
              logger.logln("deleteing reord msg:" + response.getErrorMessage().getValue());
            }
          }
        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in DMS Data deletion process:", e);
      logger.logln("Exeception in DMS Data deletion process:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
