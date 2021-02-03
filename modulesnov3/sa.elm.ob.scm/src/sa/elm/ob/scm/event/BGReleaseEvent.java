package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMBGRelease;
import sa.elm.ob.utility.util.UtilityDAO;

public class BGReleaseEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMBGRelease.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMBGRelease bgRelease = (ESCMBGRelease) event.getTargetInstance();
      if (bgRelease.isRelease() != null && bgRelease.isRelease()) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantDeleteLines"));
      }

    } catch (OBException e) {
      log.error("Exception while deleting BG Release:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting BG Release:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      log.debug("entered");
      ESCMBGRelease bgRelease = (ESCMBGRelease) event.getTargetInstance();

      // chk special character is present in bank letter ref or not
      Boolean errorflag = UtilityDAO.chkSpecialCharpresentornot(bgRelease.getBankLetterReference());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      // chk Letter Reference date is greater than or equal to BG Relase Req Date
      if (bgRelease.getLetterReferenceDateH() != null
          && bgRelease.getRequestDateH().compareTo(bgRelease.getLetterReferenceDateH()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetDatGrtthanReqDat"));
      }

    } catch (OBException e) {
      log.error("Exception while savingBG release:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while savingBG release:" + e);
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
      ESCMBGRelease bgRelease = (ESCMBGRelease) event.getTargetInstance();

      final Property bankletterref = entities[0]
          .getProperty(ESCMBGRelease.PROPERTY_BANKLETTERREFERENCE);
      final Property letterrefdate = entities[0]
          .getProperty(ESCMBGRelease.PROPERTY_LETTERREFERENCEDATEH);

      // chk special character is present in bank letter ref or not
      if ((event.getCurrentState(bankletterref) != null
          && !event.getCurrentState(bankletterref).equals(event.getPreviousState(bankletterref)))) {
        Boolean errorflag = UtilityDAO
            .chkSpecialCharpresentornot(bgRelease.getBankLetterReference());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      // chk Letter Reference date is greater than or equal to BG Relase Req Date
      if ((event.getCurrentState(letterrefdate) != null
          && !event.getCurrentState(letterrefdate).equals(event.getPreviousState(letterrefdate)))) {
        if (bgRelease.getLetterReferenceDateH() != null
            && bgRelease.getRequestDateH().compareTo(bgRelease.getLetterReferenceDateH()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetDatGrtthanReqDat"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating bg release:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating bg release:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}