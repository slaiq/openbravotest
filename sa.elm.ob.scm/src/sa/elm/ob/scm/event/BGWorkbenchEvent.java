package sa.elm.ob.scm.event;

import java.text.ParseException;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.utility.util.Utility;

public class BGWorkbenchEvent extends EntityPersistenceEventObserver {

  private static Logger log = Logger.getLogger(BGWorkbenchEvent.class);
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMBGWorkbench.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws ParseException {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      ESCMBGWorkbench bgworkbench = (ESCMBGWorkbench) event.getTargetInstance();
      final Property internalno = entities[0].getProperty(ESCMBGWorkbench.PROPERTY_INTERNALNO);
      if (event.getCurrentState(internalno) == null) {
        String sequence = Utility.getTransactionSequencewithclient("0",
            bgworkbench.getClient().getId(), "BGD");
        Boolean sequenceexists = Utility.chkTransactionSequencewithclient(
            bgworkbench.getOrganization().getId(), bgworkbench.getClient().getId(), "BGD",
            sequence);

        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }

        // ** thorw the error if same sequence exists *//*
        if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
        } else {
          event.setCurrentState(internalno, Long.parseLong(sequence));
        }
      }

    } catch (OBException e) {
      log.debug("exception while creating BGWorkbenchEvent " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating BGWorkbenchEvent " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Order ord = null;
      ESCMBGWorkbench bgworkbench = (ESCMBGWorkbench) event.getTargetInstance();

      if (bgworkbench.getBghdstatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantDeleteLines"));
      }
      if (bgworkbench.getBghdstatus().equals("DR") && bgworkbench.getDocumentType().equals("POC")
          && bgworkbench.getDocumentNo() != null) {
        ord = OBDal.getInstance().get(Order.class, bgworkbench.getDocumentNo().getId());
        if (ord.getEscmAppstatus().equals("ESCM_AP")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLinked_PO"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while deleting BG workbench:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting BG workbench:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMBGWorkbench bgworkbench = (ESCMBGWorkbench) event.getTargetInstance();
      final Property multibank = entities[0].getProperty(ESCMBGWorkbench.PROPERTY_MULTIBANKS);
      final Property proposalAttr = entities[0]
          .getProperty(ESCMBGWorkbench.PROPERTY_ESCMPROPOSALATTR);
      final Property proposalMgmt = entities[0]
          .getProperty(ESCMBGWorkbench.PROPERTY_ESCMPROPOSALMGMT);
      final Property order = entities[0].getProperty(ESCMBGWorkbench.PROPERTY_SALESORDER);

      if (event.getCurrentState(multibank) != null
          && !event.getCurrentState(multibank).equals(event.getPreviousState(multibank))) {
        Boolean multibanks = (Boolean) event.getCurrentState(multibank);
        if (!multibanks && bgworkbench.getEscmBankguaranteeDetailList().size() > 1) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGMultiCantUpdate"));
        }
      }

      if (bgworkbench.getDocumentNo() == null) {
        event.setCurrentState(proposalAttr, null);
        event.setCurrentState(proposalMgmt, null);
        event.setCurrentState(order, null);
      }
    } catch (OBException e) {
      log.error("Exception while updating BG workbench:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating BG workbench:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
