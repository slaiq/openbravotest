
package sa.elm.ob.finance.dms.serviceimplementation;

import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Attachment;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.dms.service.DMSRDVService;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.EutDmsintegrationDeletion;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;

public class DMSRDVServiceImpl implements DMSRDVService {
  private static final Logger log = Logger.getLogger(DMSRDVServiceImpl.class);

  @Override
  public void rejectAndReactivateOperations(EfinRDVTransaction rdv) throws Exception {
    try {
      OBContext.setAdminMode();
      if (rdv.getEutDmsrecordpath() != null) {
        // DMS integration to delete the record created already
        DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null, DMSConstants.DMS_DELETE,
            rdv);

        EutDmsintegrationDeletion deletionLog = OBProvider.getInstance()
            .get(EutDmsintegrationDeletion.class);

        if (!isAttachmentSentToDMS(rdv.getId())) {
          deletionLog.setRecordpath(rdv.getEutDmsrecordpath());
          rdv.setEutDmsrecordpath(null);
        }

        deletionLog.setAttachmentpath(rdv.getEutAttachPath());
        OBDal.getInstance().save(deletionLog);

        dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);

        rdv.setEutAttachPath(null);
        OBDal.getInstance().save(dmslog);
        OBDal.getInstance().save(rdv);
        OBDal.getInstance().flush();

      }
    } catch (Exception e) {
      log.error("Error while rejecting thre rdv" + e.getMessage());
      // throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static boolean isAttachmentSentToDMS(String rdvTransactionId) {
    Boolean isSent = false;
    try {

      OBQuery<Attachment> attachQry = OBDal.getInstance().createQuery(Attachment.class,
          "as e where  e.record = :recordId and e.eutDmsAttachpath is not null and e.table.id ='B4146A5918884533B13F57A574EFF9D5' ");
      attachQry.setNamedParameter("recordId", rdvTransactionId);
      List<Attachment> fileList = attachQry.list();
      if (fileList.size() > 0) {
        isSent = true;
      }

    } catch (Exception e) {
      log.error("Error while getting attachment" + e.getMessage());
    }
    return isSent;
  }

}
