package sa.elm.ob.scm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import sa.elm.ob.scm.EscmICExtension;
import sa.elm.ob.scm.ad_process.InsuranceCertificate.InsuranceCertificateProcessDAO;
import sa.elm.ob.scm.event.dao.InsuranceCertificateEventDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class ICExtensionEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmICExtension.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  // saving validation while save the IC Extension
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EscmICExtension icExtension = (EscmICExtension) event.getTargetInstance();
      if (icExtension.getEscmInsuranceCertificate() != null
          && icExtension.getRequestedExpiryDateH() != null) {
        if (icExtension.getEscmInsuranceCertificate().getExpirydateh()
            .compareTo(icExtension.getRequestedExpiryDateH()) >= 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_ICReqExpDateGrtThanExpDate"));
        }
      }

      // Task No.7635 Note No:20040
      if (icExtension.getLetterRef() != null && icExtension.getLetterReferenceDateH() != null) {
        // InsuranceCertificateProcessDAO.updateICStatus(icExtension.getEscmInsuranceCertificate(),
        // icExtension.getLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
        // icExtension.getId(), icExtension.getRequestedExpiryDateH());
      }

      // Task No.7635 Note No:20040
      if (icExtension.getEscmInsuranceCertificate() != null) {
        if (InsuranceCertificateEventDAO
            .chkICExtProcessornot(icExtension.getEscmInsuranceCertificate())) {
          // throw new OBException(OBMessageUtils.messageBD("ESCM_ICExtNotYetProcess"));
        }
      }

      // special characters should not allowed in insurance c letter reference

      if (UtilityDAO.chkSpecialCharpresentornot(icExtension.getLetterRef())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@",
            "Insurance cletter reference"));
      }

      // past date should not be allowed in letter reference date
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (icExtension.getLetterReferenceDateH() != null
          && dateFormat.parse(dateFormat.format(icExtension.getLetterReferenceDateH()))
              .compareTo(todaydate) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_Pastletterdate"));
      }
      // Without Document number(PO/contract) can't do extension
      if (icExtension.getEscmInsuranceCertificate().getSalesOrder() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Without_DocNo_CantDo_Extension"));
      }

      // Task No.7635 Note No:20040
      if (icExtension.getEscmInsuranceCertificate() != null
          && icExtension.getRequestedExpiryDateH() != null) {
        if (icExtension.getRequestedExpiryDateH()
            .compareTo(icExtension.getEscmInsuranceCertificate().getExpirydateh()) > 0) {
          InsuranceCertificateProcessDAO.updateICStatus(icExtension.getEscmInsuranceCertificate(),
              icExtension.getLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
              icExtension.getId(), icExtension.getRequestedExpiryDateH());
        }
      }

    } catch (OBException e) {
      log.error("Exception while savingIC Extension:", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.debug("exception while parsing date in saving IC Extension", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while savingIC Extension:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // update validation while update the IC Extension
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EscmICExtension icExtension = (EscmICExtension) event.getTargetInstance();

      final Property reqExpiryDate = entities[0]
          .getProperty(EscmICExtension.PROPERTY_REQUESTEDEXPIRYDATEH);
      final Property ExtPeriodday = entities[0]
          .getProperty(EscmICExtension.PROPERTY_EXTPERIODMONTH);
      final Property cletter = entities[0].getProperty(EscmICExtension.PROPERTY_LETTERREF);

      final Property bankletterref = entities[0].getProperty(EscmICExtension.PROPERTY_LETTERREF);
      final Property letterrefdate = entities[0]
          .getProperty(EscmICExtension.PROPERTY_LETTERREFERENCEDATEH);

      if (event.getCurrentState(reqExpiryDate) != null
          && !event.getCurrentState(reqExpiryDate).equals(event.getPreviousState(reqExpiryDate))) {
        if (icExtension.getEscmInsuranceCertificate() != null
            && icExtension.getRequestedExpiryDateH() != null) {
          if (icExtension.getEscmInsuranceCertificate().getExpirydateh()
              .compareTo(icExtension.getRequestedExpiryDateH()) >= 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_ICReqExpDateGrtThanExpDate"));
          }
          // Task No.7635 Note No:20040
          if (icExtension.getRequestedExpiryDateH()
              .compareTo(icExtension.getEscmInsuranceCertificate().getExpirydateh()) > 0) {
            InsuranceCertificateProcessDAO.updateICStatus(icExtension.getEscmInsuranceCertificate(),
                icExtension.getLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
                icExtension.getId(), icExtension.getRequestedExpiryDateH());
          }
        }
      }

      if ((event.getCurrentState(ExtPeriodday) != null
          && !event.getCurrentState(ExtPeriodday).equals(event.getPreviousState(ExtPeriodday)))
          || (event.getCurrentState(reqExpiryDate) != null && !event.getCurrentState(reqExpiryDate)
              .equals(event.getPreviousState(reqExpiryDate)))
          || (event.getCurrentState(letterrefdate) != null && !event.getCurrentState(letterrefdate)
              .equals(event.getPreviousState(letterrefdate)))
          || (event.getCurrentState(bankletterref) != null && !event.getCurrentState(bankletterref)
              .equals(event.getPreviousState(bankletterref)))) {

        // Task No.7635 Note No:20040
        if (icExtension.getLetterRef() != null && icExtension.getLetterReferenceDateH() != null) {
          // InsuranceCertificateProcessDAO.updateICStatus(icExtension.getEscmInsuranceCertificate(),
          // icExtension.getLetterRef(), "EXT", OBContext.getOBContext().getUser().getId(),
          // icExtension.getId(), icExtension.getRequestedExpiryDateH());
        }
      }

      if ((event.getPreviousState(bankletterref) != null
          && event.getCurrentState(bankletterref) == null)
          || (event.getPreviousState(letterrefdate) != null
              && event.getCurrentState(letterrefdate) == null)) {
        // Task No.7635 Note No:20040
        // throw new OBException(OBMessageUtils.messageBD("ESCM_ICCantUpdateLines"));
      }

      // special characters should not allowed in insurance c letter reference

      if (event.getCurrentState(cletter) != null
          && !event.getCurrentState(cletter).equals(event.getPreviousState(cletter))
          && UtilityDAO.chkSpecialCharpresentornot(icExtension.getLetterRef())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@",
            "Insurance cletter reference"));
      }

      // past date should not be allowed in letter reference date
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (event.getCurrentState(letterrefdate) != null
          && !event.getCurrentState(letterrefdate).equals(event.getPreviousState(letterrefdate))
          && dateFormat.parse(dateFormat.format(icExtension.getLetterReferenceDateH()))
              .compareTo(todaydate) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_Pastletterdate"));
      }
      // Without Document number(PO/contract) can't do extension
      if (icExtension.getEscmInsuranceCertificate().getSalesOrder() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Without_DocNo_CantDo_Extension"));
      }

    } catch (OBException e) {
      log.error("Exception while updating IC Extension:", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("exception while parsing date in updating IC Extension:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating IC Extension:", e);
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
      EscmICExtension icExtension = (EscmICExtension) event.getTargetInstance();

      // Task No.7635 Note No:20040
      if (icExtension.getLetterRef() != null && icExtension.getLetterReferenceDateH() != null) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_ICCantDeleteLines"));
      }

      // Task No.7635 Note No:20040
      if (InsuranceCertificateProcessDAO.checkICExtensionProcessed(icExtension)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_ICCantDeleteLines"));
      } else {
        InsuranceCertificateProcessDAO.updateExtensionExpiryDate(icExtension);
      }
    } catch (OBException e) {
      log.error("Exception while deleting IC Extension:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating IC Extension:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
