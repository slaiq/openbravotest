package sa.elm.ob.finance.dms.serviceimplementation;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.finance.dms.service.DMSInvoiceService;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.EutDmsintegrationDeletion;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;

public class DMSInvoiceServiceImpl implements DMSInvoiceService {

  @Override
  public void rejectAndReactivateOperations(Invoice invoice) throws Exception {
    try {
      OBContext.setAdminMode();
      if (invoice.getEutDmsrecordpath() != null) {
        // DMS integration to delete the record created already
        DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(invoice,
            DMSConstants.DMS_DELETE, null);

        EutDmsintegrationDeletion deletionLog = OBProvider.getInstance()
            .get(EutDmsintegrationDeletion.class);
        deletionLog.setRecordpath(invoice.getEutDmsrecordpath());
        deletionLog.setAttachmentpath(invoice.getEutAttachPath());
        OBDal.getInstance().save(deletionLog);

        dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
        invoice.setEutDmsrecordpath(null);
        invoice.setEutAttachPath(null);
        OBDal.getInstance().save(dmslog);
        OBDal.getInstance().save(invoice);

        OBDal.getInstance().flush();

        // Commented this line because we will unlink the record of dms with invoice
        // Later we will make a scheduler to remove it from dms server itself
        /*
         * GRPDmsInterface dmsGRP = new GRPDmsImplementation();
         * 
         * DeleteRecordGRPResponse response =
         * dmsGRP.deleteRecordinDMS(invoice.getEutDmsrecordpath()); if (!response.isError() &&
         * response.isOperationSuccess()) { invoice.setEutDmsrecordpath(null);
         * invoice.setEutAttachPath(null); dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
         * DMSUtility.updateStatusinIntegrationLog(invoice, dmslog.getId()); } else {
         * dmslog.setAlertStatus(DMSConstants.DMS_FAILED); }
         * dmslog.setRequest(response.getRequest());
         * dmslog.setResponsemessage(response.getResponse()); OBDal.getInstance().save(dmslog);
         * OBDal.getInstance().save(invoice); OBDal.getInstance().flush();
         */

      }
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
