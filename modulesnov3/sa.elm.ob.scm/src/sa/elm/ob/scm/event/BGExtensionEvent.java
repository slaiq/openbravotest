package sa.elm.ob.scm.event;

import java.math.BigDecimal;

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

import sa.elm.ob.scm.ESCMBGExtension;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class BGExtensionEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMBGExtension.ENTITY_NAME) };

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
      ESCMBGExtension bgExtension = (ESCMBGExtension) event.getTargetInstance();

      Boolean errorflag = UtilityDAO.chkSpecialCharpresentornot(bgExtension.getBankLetterRef());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      if (bgExtension.getEscmBankguaranteeDetail() != null
          && bgExtension.getReqexpiryDate() != null) {
        /*
         * Task no:5336 check header expiry date is less than the req exppiry date if
         * (bgExtension.getEscmBankguaranteeDetail().getExpirydateh()
         * .compareTo(bgExtension.getReqexpiryDate()) >= 0) { throw new
         * OBException(OBMessageUtils.messageBD("ESCM_BGReqExpDateGrtThanExpDate")); }
         */
        if (BankGuaranteeDetailEventDAO.chkexpirydateconflics(
            bgExtension.getEscmBankguaranteeDetail(), bgExtension.getId(),
            bgExtension.getReqexpiryDate()))
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGEXtesnionSamePeriod"));
      }

      // fixing task #7636
      /*
       * if (bgExtension.getBankLetterRef() != null && bgExtension.getLetterRefDate() != null) { if
       * (bgExtension.getEscmBankguaranteeDetail() != null) {
       * 
       * BankGuaranteeProcessDAO.updateBGStatus(bgExtension.getEscmBankguaranteeDetail(),
       * bgExtension.getBankLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
       * bgExtension.getId(), bgExtension.getReqexpiryDate());
       * 
       * } }
       */

      /*
       * if (bgExtension.getEscmBankguaranteeDetail() != null) { if (BankGuaranteeDetailEventDAO
       * .chkBgExtProcessornot(bgExtension.getEscmBankguaranteeDetail())) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_BgExtNotYetProcess")); } }
       */

      if (bgExtension.getLetterRefDate() != null
          && bgExtension.getRequestDate().compareTo(bgExtension.getLetterRefDate()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetDatGrtthanReqDat"));
      }

      if (bgExtension.getEscmBankguaranteeDetail().getBgamount() != null && (bgExtension
          .getEscmBankguaranteeDetail().getBgamount().compareTo(BigDecimal.ZERO) == 0)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGExtBGAmtIsZero"));
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

  // update validation while update the BG Extension
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMBGExtension bgExtension = (ESCMBGExtension) event.getTargetInstance();

      final Property reqExpiryDate = entities[0]
          .getProperty(ESCMBGExtension.PROPERTY_REQEXPIRYDATE);
      // final Property ExtPeriodday =
      // entities[0].getProperty(ESCMBGExtension.PROPERTY_EXTPERIODDAYS);

      final Property bankletterref = entities[0]
          .getProperty(ESCMBGExtension.PROPERTY_BANKLETTERREF);
      final Property letterrefdate = entities[0]
          .getProperty(ESCMBGExtension.PROPERTY_LETTERREFDATE);

      if ((event.getCurrentState(bankletterref) != null
          && !event.getCurrentState(bankletterref).equals(event.getPreviousState(bankletterref)))) {
        Boolean errorflag = UtilityDAO.chkSpecialCharpresentornot(bgExtension.getBankLetterRef());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      if (event.getCurrentState(reqExpiryDate) != null
          && !event.getCurrentState(reqExpiryDate).equals(event.getPreviousState(reqExpiryDate))) {
        if (bgExtension.getEscmBankguaranteeDetail() != null
            && bgExtension.getReqexpiryDate() != null) {
          /*
           * Task no:5336 check header expiry date is less than the req exppiry date if
           * (bgExtension.getEscmBankguaranteeDetail().getExpirydateh()
           * .compareTo(bgExtension.getReqexpiryDate()) >= 0) { throw new
           * OBException(OBMessageUtils.messageBD("ESCM_BGReqExpDateGrtThanExpDate")); }
           */
          if (BankGuaranteeDetailEventDAO.chkexpirydateconflics(
              bgExtension.getEscmBankguaranteeDetail(), bgExtension.getId(),
              bgExtension.getReqexpiryDate()))
            throw new OBException(OBMessageUtils.messageBD("ESCM_BGEXtesnionSamePeriod"));
        }
      }
      // fixing task #7636
      /*
       * if ((event.getCurrentState(ExtPeriodday) != null &&
       * !event.getCurrentState(ExtPeriodday).equals(event.getPreviousState(ExtPeriodday))) ||
       * (event.getCurrentState(reqExpiryDate) != null && !event.getCurrentState(reqExpiryDate)
       * .equals(event.getPreviousState(reqExpiryDate))) || (event.getCurrentState(letterrefdate) !=
       * null && !event.getCurrentState(letterrefdate)
       * .equals(event.getPreviousState(letterrefdate))) || (event.getCurrentState(bankletterref) !=
       * null && !event.getCurrentState(bankletterref)
       * .equals(event.getPreviousState(bankletterref)))) {
       * 
       * if (bgExtension.getBankLetterRef() != null && bgExtension.getLetterRefDate() != null) { if
       * (bgExtension.getEscmBankguaranteeDetail() != null) {
       * BankGuaranteeProcessDAO.updateBGStatus(bgExtension.getEscmBankguaranteeDetail(),
       * bgExtension.getBankLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
       * bgExtension.getId(), bgExtension.getReqexpiryDate());
       * 
       * } } } if ((event.getPreviousState(bankletterref) != null &&
       * event.getCurrentState(bankletterref) == null) || (event.getPreviousState(letterrefdate) !=
       * null && event.getCurrentState(letterrefdate) == null)) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_BGCantUpdateLines")); }
       */

      if ((event.getCurrentState(letterrefdate) != null
          && !event.getCurrentState(letterrefdate).equals(event.getPreviousState(letterrefdate)))) {
        if (bgExtension.getLetterRefDate() != null
            && bgExtension.getRequestDate().compareTo(bgExtension.getLetterRefDate()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetDatGrtthanReqDat"));
        }
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

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      boolean processed = false;

      ESCMBGExtension bgExtension = (ESCMBGExtension) event.getTargetInstance();
      processed = BankGuaranteeDetailEventDAO
          .chkBgExtProcessornot(bgExtension.getEscmBankguaranteeDetail(), bgExtension.getLineNo());

      if (processed) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantDeleteLines"));
      }
    } catch (OBException e) {
      log.error("Exception while deleting BG Extension:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting BG Extension:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
