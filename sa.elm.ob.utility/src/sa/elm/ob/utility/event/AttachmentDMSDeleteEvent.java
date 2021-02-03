
package sa.elm.ob.utility.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.EutDmsintegrationDeletion;

public class AttachmentDMSDeleteEvent extends EntityPersistenceEventObserver {
  private static final Logger LOG = LoggerFactory.getLogger(AttachmentDMSDeleteEvent.class);

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Attachment.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Attachment attach = (Attachment) event.getTargetInstance();

      if (attach.getTable() != null
          && "B4146A5918884533B13F57A574EFF9D5".equals(attach.getTable().getId())
          && attach.getEutDmsAttachpath() != null) {
        EfinRDVTransaction rdvtrx = OBDal.getInstance().get(EfinRDVTransaction.class,
            attach.getRecord());
        if (rdvtrx != null) {
          if ("APP".equals(rdvtrx.getAppstatus())) {
            throw new OBException(OBMessageUtils.messageBD("EUT_Recordsenttodms_delete"));
          } else {
            EutDmsintegrationDeletion deletionLog = OBProvider.getInstance()
                .get(EutDmsintegrationDeletion.class);
            deletionLog.setAttachmentpath(attach.getEutDmsAttachpath());
            OBDal.getInstance().save(deletionLog);
          }
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while deleting signature configuration: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
