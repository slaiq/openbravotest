package sa.elm.ob.scm.event;

import java.math.BigDecimal;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMBGAmtRevision;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class BGAmtRevisionEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMBGAmtRevision.ENTITY_NAME) };

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
      ESCMBGAmtRevision bgAmtRevision = (ESCMBGAmtRevision) event.getTargetInstance();
      final Property letterno = entities[0].getProperty(ESCMBGAmtRevision.PROPERTY_LETTERNO);
      final Property revision = entities[0].getProperty(ESCMBGAmtRevision.PROPERTY_REVISION);

      String sequence = "";
      Boolean sequenceexists;
      Boolean errorflag = false;
      // set new Spec No
      sequence = Utility.getTransactionSequencewithclient("0", bgAmtRevision.getClient().getId(),
          "BGAMT");
      sequenceexists = Utility.chkTransactionSequencewithclient(
          bgAmtRevision.getOrganization().getId(), bgAmtRevision.getClient().getId(), "BGAMT",
          sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(letterno, sequence);
      }
      if (bgAmtRevision.getBankLetterReference() != null
          && bgAmtRevision.getLetterReferenceDateH() != null) {
        event.setCurrentState(revision, true);
      }
      if (bgAmtRevision.getReductionPercentage() != null
          && bgAmtRevision.getReductionPercentage().compareTo(new BigDecimal(100)) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGAmtRevRed_Percent"));
      }

      // Fixed Task #7722: Bank Guarantee unable to create new Revision
      // if (bgAmtRevision.getEscmBankguaranteeDetail() != null) {
      // if (BankGuaranteeDetailEventDAO
      // .chkBgAmtProcessornot(bgAmtRevision.getEscmBankguaranteeDetail())) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_BgAmtNotYetProcess"));
      // }
      // }

      // chk already it revisied 100 % or not
      if (bgAmtRevision.getEscmBankguaranteeDetail() != null
          && bgAmtRevision.getEscmBankguaranteeDetail().getESCMBGAmtRevisionList().size() > 0) {
        if (BGWorkbenchDAO
            .chkhundreadperrevornot(bgAmtRevision.getEscmBankguaranteeDetail().getId())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGAmtRevHundPercet"));
        }
      }
      if (bgAmtRevision.getEscmBankguaranteeDetail().getBgamount() != null && (bgAmtRevision
          .getEscmBankguaranteeDetail().getBgamount().compareTo(BigDecimal.ZERO) == 0)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGAmtIsZero"));
      }

      // chk special character is present in bank letter ref or not
      errorflag = UtilityDAO.chkSpecialCharpresentornot(bgAmtRevision.getBankLetterReference());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      // chk special character is present in invoice verfication no or not
      errorflag = UtilityDAO.chkSpecialCharpresentornot(bgAmtRevision.getInvoiceVerficiationno());
      if (errorflag)
        throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));

      // chk Letter Reference date is greater than or equal to BG Amt Revision Letter date
      if (bgAmtRevision.getLetterReferenceDateH() != null
          && bgAmtRevision.getLetterDate().compareTo(bgAmtRevision.getLetterReferenceDateH()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetRefDatGrtthanLetDat"));
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
      // BigDecimal Diff = BigDecimal.ZERO, revAmount = BigDecimal.ZERO;
      Boolean errorflag = false;
      ESCMBGAmtRevision bgAmtRevision = (ESCMBGAmtRevision) event.getTargetInstance();
      final Property revision = entities[0].getProperty(ESCMBGAmtRevision.PROPERTY_REVISION);
      final Property redPercent = entities[0]
          .getProperty(ESCMBGAmtRevision.PROPERTY_REDUCTIONPERCENTAGE);
      // final Property redAmt = entities[0].getProperty(ESCMBGAmtRevision.PROPERTY_REDUCEDAMT);
      final Property invverno = entities[0]
          .getProperty(ESCMBGAmtRevision.PROPERTY_INVOICEVERFICIATIONNO);

      final Property bankletterref = entities[0]
          .getProperty(ESCMBGAmtRevision.PROPERTY_BANKLETTERREFERENCE);
      final Property letterrefdate = entities[0]
          .getProperty(ESCMBGAmtRevision.PROPERTY_LETTERREFERENCEDATEH);

      if (bgAmtRevision.getBankLetterReference() != null
          && bgAmtRevision.getLetterReferenceDateH() != null) {
        event.setCurrentState(revision, true);
      } else {
        event.setCurrentState(revision, false);
      }
      if (event.getCurrentState(redPercent) != null
          && !event.getCurrentState(redPercent).equals(event.getPreviousState(redPercent))) {
        if (bgAmtRevision.getReductionPercentage() != null
            && bgAmtRevision.getReductionPercentage().compareTo(new BigDecimal(100)) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGAmtRevRed_Percent"));
        }
      }

      /*
       * if ((event.getCurrentState(letterrefdate) != null && !event.getCurrentState(letterrefdate)
       * .equals(event.getPreviousState(letterrefdate))) || (event.getCurrentState(bankletterref) !=
       * null && !event.getCurrentState(bankletterref)
       * .equals(event.getPreviousState(bankletterref))) ||
       * (!event.getCurrentState(redPercent).equals(event.getPreviousState(redPercent))) ||
       * (!event.getCurrentState(redAmt).equals(event.getPreviousState(redAmt)))) {
       * 
       * Diff = ((BigDecimal) event.getCurrentState(redAmt)).subtract((BigDecimal) event
       * .getPreviousState(redAmt)); if
       * (bgAmtRevision.getEscmBankguaranteeDetail().getESCMBGAmtRevisionList().size() > 0) { for
       * (ESCMBGAmtRevision rev : bgAmtRevision.getEscmBankguaranteeDetail()
       * .getESCMBGAmtRevisionList()) { if (!rev.getId().equals(bgAmtRevision.getId())) { revAmount
       * = revAmount.add(rev.getNETBgamt()); } } revAmount.add(bgAmtRevision.getNETBgamt()); if
       * (revAmount.compareTo(bgAmtRevision.getEscmBankguaranteeDetail().getBgamount()) < 0) { throw
       * new OBException(OBMessageUtils.messageBD("ESCM_BGAmtRevRed_Percent")); } } }
       */

      // chk special character is present in bank letter ref or not
      if ((event.getCurrentState(bankletterref) != null
          && !event.getCurrentState(bankletterref).equals(event.getPreviousState(bankletterref)))) {
        errorflag = UtilityDAO.chkSpecialCharpresentornot(bgAmtRevision.getBankLetterReference());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      // chk special character is present in invoice verfication no or not
      if ((event.getCurrentState(invverno) != null
          && !event.getCurrentState(invverno).equals(event.getPreviousState(invverno)))) {
        errorflag = UtilityDAO.chkSpecialCharpresentornot(bgAmtRevision.getInvoiceVerficiationno());
        if (errorflag)
          throw new OBException(OBMessageUtils.messageBD("ESCM_SplCharNotAllowed"));
      }

      // chk Letter Reference date is greater than or equal to BG Amt Revision Letter date
      if ((event.getCurrentState(letterrefdate) != null
          && !event.getCurrentState(letterrefdate).equals(event.getPreviousState(letterrefdate)))) {
        if (bgAmtRevision.getLetterReferenceDateH() != null && bgAmtRevision.getLetterDate()
            .compareTo(bgAmtRevision.getLetterReferenceDateH()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BGLetRefDatGrtthanLetDat"));
        }
      }

      // Fixed Task #7722: Bank Guarantee unable to create new Revision
      // if ((event.getPreviousState(bankletterref) != null
      // && event.getCurrentState(bankletterref) == null)
      // || (event.getPreviousState(letterrefdate) != null
      // && event.getCurrentState(letterrefdate) == null)) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantUpdateLines"));
      // }

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
      ESCMBGAmtRevision bgAmtRevision = (ESCMBGAmtRevision) event.getTargetInstance();
      boolean processed = false;

      // Fixed Task #7722: Bank Guarantee unable to create new Revision
      // if (bgAmtRevision.getBankLetterReference() != null
      // && bgAmtRevision.getLetterReferenceDateH() != null) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_BGCantDeleteLines"));
      // }
      processed = BankGuaranteeDetailEventDAO.chkBgAmtRevProcessornot(
          bgAmtRevision.getEscmBankguaranteeDetail(), bgAmtRevision.getLineNo());

      if (processed) {
        throw new OBException(OBMessageUtils.messageBD("Escm_BGAmtRevCantDelete"));
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
