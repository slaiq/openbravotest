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

import sa.elm.ob.scm.ESCMBGConfiscation;
import sa.elm.ob.utility.util.UtilityDAO;

public class BGConfiscationEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMBGConfiscation.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  // saving validation while save the BG Extension
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMBGConfiscation bgConfiscation = (ESCMBGConfiscation) event.getTargetInstance();
      // final Property internalno = entities[0].getProperty(ESCMBGConfiscation.PROPERTY_LETTERNO);
      // String sequence = "";
      // Boolean sequenceexists;
      Boolean errorflag = false;
      // set new Spec No
      /*
       * sequence = Utility.getTransactionSequencewithclient("0",
       * bgConfiscation.getClient().getId(), "BGCON"); sequenceexists =
       * Utility.chkTransactionSequencewithclient(bgConfiscation.getOrganization() .getId(),
       * bgConfiscation.getClient().getId(), "BGCON", sequence); if (!sequenceexists) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence")); } // set new Spec No if
       * (sequence.equals("false") || StringUtils.isEmpty(sequence)) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_NoSequence")); } else {
       * event.setCurrentState(internalno, sequence); }
       */

      // chk special character is present in bank letter ref or not
      errorflag = UtilityDAO.chkSpecialCharpresentornot(bgConfiscation.getBankLetterReference());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      // chk special character is present in letter no or not
      errorflag = UtilityDAO.chkSpecialCharpresentornot(bgConfiscation.getLetterNo());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      // chk special character is present in decree no or not
      errorflag = UtilityDAO.chkSpecialCharpresentornot(bgConfiscation.getDecreeNo());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      // chk Letter Reference date is greater than or equal to BG Confiscation request date
      if (bgConfiscation.getLetterReferenceDateH() != null && bgConfiscation.getRequestDateH()
          .compareTo(bgConfiscation.getLetterReferenceDateH()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetDatGrtthanReqDat"));
      }

      // chk Letter Date From is greater than or equal to BG Confiscation request date
      if (bgConfiscation.getLetterDatefrom() != null
          && bgConfiscation.getRequestDateH().compareTo(bgConfiscation.getLetterDatefrom()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetFromDatGrtthanReqDat"));
      }

      // chk Letter Date To is greater than or equal to BG Confiscation request date
      if (bgConfiscation.getLetterDateto() != null
          && bgConfiscation.getRequestDateH().compareTo(bgConfiscation.getLetterDateto()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetToDatGrtthanReqDat"));
      }

      // chk Letter Date To is greater than or equal to BG Confiscation Letter from date
      if (bgConfiscation.getLetterDatefrom() != null && bgConfiscation.getLetterDateto() != null
          && (bgConfiscation.getLetterDatefrom().compareTo(bgConfiscation.getLetterDateto()) > 0)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetToDatGrtthanLetFrmDate"));
      }

      // chk Decree Date is greater than or equal to BG Confiscation request date
      if (bgConfiscation.getDecreeDate() != null
          && (bgConfiscation.getRequestDateH().compareTo(bgConfiscation.getDecreeDate()) > 0)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGDecDatGrtthanReqDat"));
      }

    } catch (OBException e) {
      log.error("Exception while savingBG Extension:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while savingBG Extension:", e);
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
      ESCMBGConfiscation bgConfiscation = (ESCMBGConfiscation) event.getTargetInstance();
      Boolean errorflag = false;
      final Property bankletterref = entities[0]
          .getProperty(ESCMBGConfiscation.PROPERTY_BANKLETTERREFERENCE);
      final Property letterrefdate = entities[0]
          .getProperty(ESCMBGConfiscation.PROPERTY_LETTERREFERENCEDATEH);
      final Property letterno = entities[0].getProperty(ESCMBGConfiscation.PROPERTY_LETTERNO);
      final Property decreeno = entities[0].getProperty(ESCMBGConfiscation.PROPERTY_DECREENO);
      final Property decreedate = entities[0].getProperty(ESCMBGConfiscation.PROPERTY_DECREEDATE);
      final Property letterfrom = entities[0]
          .getProperty(ESCMBGConfiscation.PROPERTY_LETTERDATEFROM);
      final Property letterto = entities[0].getProperty(ESCMBGConfiscation.PROPERTY_LETTERDATETO);

      // chk special character is present in bank letter ref or not
      if (event.getCurrentState(bankletterref) != null && (!event.getCurrentState(bankletterref)
          .equals(event.getPreviousState(bankletterref)))) {
        errorflag = UtilityDAO.chkSpecialCharpresentornot(bgConfiscation.getBankLetterReference());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      // chk special character is present in letter no or not
      if (event.getCurrentState(letterno) != null
          && (!event.getCurrentState(letterno).equals(event.getPreviousState(letterno)))) {
        errorflag = UtilityDAO.chkSpecialCharpresentornot(bgConfiscation.getLetterNo());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      // chk special character is present in decree no or not
      if (event.getCurrentState(decreeno) != null
          && (!event.getCurrentState(decreeno).equals(event.getPreviousState(decreeno)))) {
        errorflag = UtilityDAO.chkSpecialCharpresentornot(bgConfiscation.getDecreeNo());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      // chk Letter Reference date is greater than or equal to BG Confiscation request date
      if (event.getCurrentState(letterrefdate) != null && (!event.getCurrentState(letterrefdate)
          .equals(event.getPreviousState(letterrefdate)))) {
        if (bgConfiscation.getLetterReferenceDateH() != null && bgConfiscation.getRequestDateH()
            .compareTo(bgConfiscation.getLetterReferenceDateH()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetDatGrtthanReqDat"));
        }
      }

      // chk Letter Date From is greater than or equal to BG Confiscation request date
      if (event.getCurrentState(letterfrom) != null
          && (!event.getCurrentState(letterfrom).equals(event.getPreviousState(letterfrom)))) {
        if (bgConfiscation.getLetterDatefrom() != null
            && bgConfiscation.getRequestDateH().compareTo(bgConfiscation.getLetterDatefrom()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetFromDatGrtthanReqDat"));
        }
      }

      // chk Letter Date To is greater than or equal to BG Confiscation request date
      if (event.getCurrentState(letterto) != null
          && (!event.getCurrentState(letterto).equals(event.getPreviousState(letterto)))) {
        if (bgConfiscation.getLetterDateto() != null
            && bgConfiscation.getRequestDateH().compareTo(bgConfiscation.getLetterDateto()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetToDatGrtthanReqDat"));
        }
      }
      // chk Letter Date To is greater than or equal to BG Confiscation Letter from date
      if ((event.getCurrentState(letterto) != null
          && (!event.getCurrentState(letterto).equals(event.getPreviousState(letterto))))
          || (event.getCurrentState(letterfrom) != null
              && (!event.getCurrentState(letterfrom).equals(event.getPreviousState(letterfrom))))) {
        if (bgConfiscation.getLetterDatefrom() != null && bgConfiscation.getLetterDateto() != null
            && (bgConfiscation.getLetterDatefrom()
                .compareTo(bgConfiscation.getLetterDateto()) > 0)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetToDatGrtthanLetFrmDate"));
        }
      }

      // chk Decree Date is greater than or equal to BG Confiscation request date
      if (event.getCurrentState(decreedate) != null
          && (!event.getCurrentState(decreedate).equals(event.getPreviousState(decreedate)))) {
        if (bgConfiscation.getDecreeDate() != null
            && (bgConfiscation.getRequestDateH().compareTo(bgConfiscation.getDecreeDate()) > 0)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGDecDatGrtthanReqDat"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating bg release:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating bg release:", e);
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
      ESCMBGConfiscation bgConfiscation = (ESCMBGConfiscation) event.getTargetInstance();
      if (bgConfiscation.isConfiscate() != null && bgConfiscation.isConfiscate()) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantDeleteLines"));
      }

    } catch (OBException e) {
      log.error("Exception while updating BG Extension:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating BG Extension:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
