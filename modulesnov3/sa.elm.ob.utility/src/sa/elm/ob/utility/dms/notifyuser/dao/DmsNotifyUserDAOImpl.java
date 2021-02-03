package sa.elm.ob.utility.dms.notifyuser.dao;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.dms.consumer.dto.DMSNotifyRequestDTO;
import sa.elm.ob.utility.dms.notifyuser.exceptions.DmsNotifyUserException;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.util.Constants;

@Repository
public class DmsNotifyUserDAOImpl implements DmsNotifyUserDAO {
  private static final Logger log = LoggerFactory.getLogger(DmsNotifyUserDAOImpl.class);

  @Override
  public Boolean insertAlertNotificationForUser(String dmsIntegrationLogId)
      throws DmsNotifyUserException {
    Boolean isInserted = Boolean.FALSE;
    String requested_user = "";
    String invoice_record_id = "";
    String invoice_no = "";
    String response_message = "";
    String details_not_found = sa.elm.ob.utility.properties.Resource
        .getProperty("utility.dms.record.notfound", "ar_SA");

    try {
      OBContext.setAdminMode();

      // validate Client id configuration
      String clientId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty(WebserviceConstants.CLIENT_KEY);

      if (clientId == null) {
        throw new DmsNotifyUserException("Please configure client id in openbravo properties");

      }
      // find out the DMS integration details
      DMSIntegrationLog dmsintegrationObject = OBDal.getInstance().get(DMSIntegrationLog.class,
          dmsIntegrationLogId);
      if (dmsintegrationObject != null && dmsintegrationObject.getInvoice() != null) {
        requested_user = dmsintegrationObject.getCreatedBy().getId();
        invoice_record_id = dmsintegrationObject.getInvoice().getId();
        invoice_no = dmsintegrationObject.getInvoice().getDocumentNo();
        response_message = sa.elm.ob.utility.properties.Resource
            .getProperty("utility.dms.digitalsign.completed", "ar_sa");
        isInserted = alertInsertionRole(invoice_record_id, invoice_no, "", requested_user, clientId,
            response_message, "NEW", AlertWindow.PurchaseInvoice, response_message,
            Constants.GENERIC_TEMPLATE);
      } else if (dmsintegrationObject != null && dmsintegrationObject.getEfinRdvtxn() != null) {
        requested_user = dmsintegrationObject.getCreatedBy().getId();
        invoice_record_id = dmsintegrationObject.getEfinRdvtxn().getId();
        invoice_no = dmsintegrationObject.getEfinRdvtxn().getEfinRdv().getDocumentNo() + "-"
            + dmsintegrationObject.getEfinRdvtxn().getTXNVersion();
        response_message = sa.elm.ob.utility.properties.Resource
            .getProperty("utility.dms.digitalsign.completed", "ar_sa");
        isInserted = alertInsertionRole(invoice_record_id, invoice_no, "", requested_user, clientId,
            response_message, "NEW", AlertWindow.RDVTransaction, response_message,
            Constants.GENERIC_TEMPLATE);
      } else {
        Attachment attach = OBDal.getInstance().get(Attachment.class, dmsIntegrationLogId);
        if (attach == null) {
          isInserted = Boolean.FALSE;
          throw new DmsNotifyUserException(details_not_found);
        } else {
          isInserted = Boolean.TRUE;
        }
      }

    } catch (Exception e) {
      isInserted = Boolean.FALSE;
      OBDal.getInstance().rollbackAndClose();
      throw new DmsNotifyUserException(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }

    return isInserted;
  }

  @Override
  public Boolean alertInsertionRole(String DocumentId, String DocumentNo, String roleId,
      String userId, String clientId, String description, String status, String Window,
      String alertKey, String mailTemplate) throws DmsNotifyUserException {

    Boolean isSuccess = Boolean.TRUE;
    String alertRuleId = "";
    try {
      OBContext.setAdminMode();
      // get Requisition Alert
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + Window
              + "'  order by e.creationDate desc");
      queryAlertRule.setFilterOnReadableClients(false);
      queryAlertRule.setFilterOnReadableOrganization(false);
      queryAlertRule.setMaxResult(1);
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      Alert objAlert = OBProvider.getInstance().get(Alert.class);
      objAlert.setClient(OBDal.getInstance().get(Client.class, clientId));
      objAlert.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      objAlert.setAlertRule(OBDal.getInstance().get(AlertRule.class, alertRuleId));
      objAlert.setEutMailTmplt(mailTemplate);
      objAlert.setEutAlertKey(alertKey);
      // imported via data set
      objAlert.setDescription(description);
      if (!roleId.isEmpty() && !roleId.equals("")) {
        objAlert.setRole(OBDal.getInstance().get(Role.class, roleId));
      }
      if (!userId.isEmpty() && !userId.equals("")) {
        objAlert.setUserContact(OBDal.getInstance().get(User.class, userId));
      }
      objAlert.setRecordID(DocumentNo);
      objAlert.setReferenceSearchKey(DocumentId);
      objAlert.setAlertStatus(status);
      OBDal.getInstance().save(objAlert);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      isSuccess = Boolean.FALSE;
      throw new DmsNotifyUserException(Constants.ESCMERROR);
    } finally {
      OBContext.restorePreviousMode();
    }
    return isSuccess;
  }

  @Override
  public void updateDMSIntegrationLog(DMSNotifyRequestDTO notifyDTO) {
    try {
      OBContext.setAdminMode();
      @SuppressWarnings("hiding")
      DMSIntegrationLog log = OBDal.getInstance().get(DMSIntegrationLog.class,
          notifyDTO.getGrpProcessId());
      if (log != null) {
        if (notifyDTO.isError) {
          log.setAlertStatus(DMSConstants.DMS_FAILED);
          log.setPkiresponse(notifyDTO.getErrorMessage());
          OBDal.getInstance().save(log);
        } else {
          log.setAlertStatus(DMSConstants.DMS_SUCCESS);
          log.setAttachmentpath(notifyDTO.getAttachmentPath());

          Invoice invoice = log.getInvoice();
          if (invoice != null && notifyDTO.getAttachmentPath() != null) {
            invoice.setEutAttachPath(notifyDTO.getAttachmentPath());
            OBDal.getInstance().save(invoice);
          }

          EfinRDVTransaction rdvtrx = log.getEfinRdvtxn();
          if (rdvtrx != null && notifyDTO.getAttachmentPath() != null) {
            rdvtrx.setEutAttachPath(notifyDTO.getAttachmentPath());
            OBDal.getInstance().save(rdvtrx);
          }

          log.setPkiresponse(notifyDTO.toString());
          OBDal.getInstance().save(log);
        }
        OBDal.getInstance().flush();
      } else {

        Attachment attach = OBDal.getInstance().get(Attachment.class, notifyDTO.getGrpProcessId());
        if (attach != null) {

          if (notifyDTO.isError) {
            attach.setEutPkierrormessage(notifyDTO.getErrorMessage());
            OBDal.getInstance().save(attach);
          } else {
            if (notifyDTO.getAttachmentPath() != null) {
              attach.setEutDmsAttachpath(notifyDTO.getAttachmentPath());
              attach.setEutIspkisuccess(true);
              OBDal.getInstance().save(attach);
            }
          }
          OBDal.getInstance().flush();
        }

      }
    } catch (Exception e) {
      log.debug("Error while updating log" + e.getMessage());
    }

  }

}
