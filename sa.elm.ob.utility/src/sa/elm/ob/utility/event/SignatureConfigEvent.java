package sa.elm.ob.utility.event;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.EUTSignatureConfig;
import sa.elm.ob.utility.event.dao.SignatureConfigEventDao;

public class SignatureConfigEvent extends EntityPersistenceEventObserver {
  private static final Logger LOG = LoggerFactory.getLogger(SignatureConfigEvent.class);

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EUTSignatureConfig.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EUTSignatureConfig signatureConfig = (EUTSignatureConfig) event.getTargetInstance();
      SignatureConfigEventDao dao = new SignatureConfigEventDao();
      List<EUTSignatureConfig> signatureList = new ArrayList<>();

      // check same doctype already exists or not.
      signatureList = dao.getListofRecords(signatureConfig.getDocumentType());
      if (signatureList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Eut_SignDocAlreadyDefined"));
      }
    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while creating signature configuration: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check is document already signed for this documenttype
   * 
   * @param event
   */
  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    boolean alreadyProcessed = false;
    try {
      OBContext.setAdminMode();
      EUTSignatureConfig signatureConfig = (EUTSignatureConfig) event.getTargetInstance();
      SignatureConfigEventDao dao = new SignatureConfigEventDao();

      // check already signature did on records.
      alreadyProcessed = dao.checkIsDocumentAlreadySigned(signatureConfig.getDocumentType());
      if (alreadyProcessed) {
        throw new OBException(OBMessageUtils.messageBD("Eut_SignAlreadyProcessed"));
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
