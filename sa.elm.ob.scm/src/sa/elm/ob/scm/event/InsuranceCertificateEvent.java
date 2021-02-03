package sa.elm.ob.scm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import sa.elm.ob.scm.EscmInsuranceCertificate;
import sa.elm.ob.scm.event.dao.InsuranceCertificateEventDAO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 * 
 */

public class InsuranceCertificateEvent extends EntityPersistenceEventObserver {
  private static Logger log = Logger.getLogger(InsuranceCertificateEvent.class);
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmInsuranceCertificate.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmInsuranceCertificate InsuranceCerificate = (EscmInsuranceCertificate) event
          .getTargetInstance();
      final Property InternalNo = entities[0]
          .getProperty(EscmInsuranceCertificate.PROPERTY_INTERNALNO);
      final Property Status = entities[0].getProperty(EscmInsuranceCertificate.PROPERTY_STATUS);
      String sequence = "";
      Boolean sequenceexists = false, Result = false;
      // set new Spec No
      sequence = Utility.getTransactionSequence(InsuranceCerificate.getOrganization().getId(),
          "ICW");
      sequenceexists = Utility.chkTransactionSequence(InsuranceCerificate.getOrganization().getId(),
          "ICW", sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(InternalNo, sequence);
      }

      // set status as active.
      event.setCurrentState(Status, "ACT");

      // Check IC NO is duplicating or not
      Result = InsuranceCertificateEventDAO.checkICNOExists(InsuranceCerificate);
      if (Result) {
        throw new OBException(
            OBMessageUtils.messageBD("Escm_IC_NO_Duplicating").replace("@", "IC No"));
      }

      // check special characters is present in the following fields - IC No, IC Archive Ref
      if (UtilityDAO.chkSpecialCharpresentornot(InsuranceCerificate.getNo())) {
        throw new OBException(
            OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@", "IC NO"));
      }

      if (InsuranceCerificate.getArchiveRef() != null
          && UtilityDAO.chkSpecialCharpresentornot(InsuranceCerificate.getArchiveRef())) {
        throw new OBException(
            OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@", "IC Archive Ref"));
      }

      // check phone number and fax number is in valid format
      if (InsuranceCerificate.getFax() != null
          && !sa.elm.ob.utility.Utils.faxNumberFormat(InsuranceCerificate.getFax())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_NotvalidFax"));
      }
      if (InsuranceCerificate.getTel() != null
          && !sa.elm.ob.utility.Utils.phoneNumberFormat(InsuranceCerificate.getTel())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_NotvalidPhone"));
      }

      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // Past date is not allowed in StartDate and EndDate
      if (InsuranceCerificate.getStartdateh() != null && dateFormat
          .parse(dateFormat.format(InsuranceCerificate.getStartdateh())).compareTo(todaydate) < 0) {
        // Task No.7624
        // throw new OBException(OBMessageUtils.messageBD("Escm_IC_Paststartdate"));
      }
      if (InsuranceCerificate.getExpirydateh() != null
          && dateFormat.parse(dateFormat.format(InsuranceCerificate.getExpirydateh()))
              .compareTo(todaydate) < 0) {
        // Task No.7624
        // throw new OBException(OBMessageUtils.messageBD("Escm_IC_Pastenddate"));
      }
      // Check Valid Email
      if (InsuranceCerificate.getEmail() != null
          && !InsuranceCerificate.getEmail().matches(Utility.emailFormat)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_notvalidemail"));
      }
      // check DocNo And InsCom Combination already Exists
      if (InsuranceCerificate.getSalesOrder() != null) {
        Result = InsuranceCertificateEventDAO.checkDocNoAndInsComCombinationExists(
            InsuranceCerificate.getInsuranceCompany().getId(),
            InsuranceCerificate.getSalesOrder().getId());
        if (Result) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_InsCom_DocNo_Combination_Exist"));
        }
      }
    } catch (OBException e) {
      log.error("exception while creating Insurance Certificate save event", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("exception while parsing date in Insurance Certificate save event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating Insurance Certificate save event", e);
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
      EscmInsuranceCertificate InsuranceCerificate = (EscmInsuranceCertificate) event
          .getTargetInstance();
      final Property IcNo = entities[0].getProperty(EscmInsuranceCertificate.PROPERTY_NO);
      final Property icArchiveNo = entities[0]
          .getProperty(EscmInsuranceCertificate.PROPERTY_ARCHIVEREF);
      final Property phone = entities[0].getProperty(EscmInsuranceCertificate.PROPERTY_TEL);
      final Property fax = entities[0].getProperty(EscmInsuranceCertificate.PROPERTY_FAX);
      final Property email = entities[0].getProperty(EscmInsuranceCertificate.PROPERTY_EMAIL);
      final Property startdate = entities[0]
          .getProperty(EscmInsuranceCertificate.PROPERTY_STARTDATEH);
      final Property Expirydate = entities[0]
          .getProperty(EscmInsuranceCertificate.PROPERTY_EXPIRYDATEH);
      final Property InsuranceCompany = entities[0]
          .getProperty(EscmInsuranceCertificate.PROPERTY_INSURANCECOMPANY);
      final Property DocNo = entities[0].getProperty(EscmInsuranceCertificate.PROPERTY_SALESORDER);
      Boolean Result = false;

      // Check IC NO is duplicating or not
      if (!event.getPreviousState(IcNo).equals(event.getCurrentState(IcNo))) {
        Result = InsuranceCertificateEventDAO.checkICNOExists(InsuranceCerificate);
        if (Result) {
          throw new OBException(OBMessageUtils.messageBD("Escm_IC_NO_Duplicating"));
        }
      }

      // check special characters is present in the following fields - IC No, IC Archive Ref
      if (event.getCurrentState(IcNo) != null
          && !event.getPreviousState(IcNo).equals(event.getCurrentState(IcNo))
          && UtilityDAO.chkSpecialCharpresentornot(InsuranceCerificate.getNo())) {
        throw new OBException(
            OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@", "IC NO"));
      }

      if (event.getCurrentState(icArchiveNo) != null
          && !event.getCurrentState(icArchiveNo).equals(event.getPreviousState(icArchiveNo))
          && UtilityDAO.chkSpecialCharpresentornot(InsuranceCerificate.getArchiveRef())) {
        throw new OBException(
            OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@", "IC Archive Ref"));
      }

      // check phone number and fax number is in valid format
      if (event.getCurrentState(fax) != null
          && !event.getCurrentState(fax).equals(event.getPreviousState(fax))
          && !sa.elm.ob.utility.Utils.faxNumberFormat(InsuranceCerificate.getFax())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_NotvalidFax"));
      }
      if (event.getCurrentState(phone) != null
          && !event.getCurrentState(phone).equals(event.getPreviousState(phone))
          && !sa.elm.ob.utility.Utils.phoneNumberFormat(InsuranceCerificate.getTel())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_NotvalidPhone"));
      }

      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // Past date is not allowed in StartDate and EndDate
      if (event.getCurrentState(startdate) != null
          && !event.getCurrentState(startdate).equals(event.getPreviousState(startdate))
          && dateFormat.parse(dateFormat.format(InsuranceCerificate.getStartdateh()))
              .compareTo(todaydate) < 0) {
        // Task No.7624
        // throw new OBException(OBMessageUtils.messageBD("Escm_IC_Paststartdate"));
      }
      if (event.getCurrentState(Expirydate) != null
          && !event.getCurrentState(Expirydate).equals(event.getPreviousState(Expirydate))
          && dateFormat.parse(dateFormat.format(InsuranceCerificate.getExpirydateh()))
              .compareTo(todaydate) < 0) {
        // Task No.7624
        // throw new OBException(OBMessageUtils.messageBD("Escm_IC_Pastenddate"));
      }
      // Check Valid Email
      if (event.getCurrentState(email) != null
          && !event.getCurrentState(email).equals(event.getPreviousState(email))
          && !InsuranceCerificate.getEmail().matches(Utility.emailFormat)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_notvalidemail"));
      }
      // check DocNo And InsCom Combination already Exists
      if (InsuranceCerificate.getSalesOrder() != null) {
        if ((!event.getCurrentState(InsuranceCompany)
            .equals(event.getPreviousState(InsuranceCompany)))
            || (!event.getCurrentState(DocNo).equals(event.getPreviousState(DocNo)))) {
          Result = InsuranceCertificateEventDAO.checkDocNoAndInsComCombinationExists(
              InsuranceCerificate.getInsuranceCompany().getId(),
              InsuranceCerificate.getSalesOrder().getId());
          if (Result) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_InsCom_DocNo_Combination_Exist"));
          }
        }
      }
    } catch (OBException e) {
      log.error("exception while creating Insurance Certificate update event", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("exception while parsing date in Insurance Certificate update event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating Insurance Certificate update event", e);
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
      EscmInsuranceCertificate InsuranceCerificate = (EscmInsuranceCertificate) event
          .getTargetInstance();
      if (!InsuranceCerificate.getStatus().equals("ACT")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_ICCantDeleteLines"));
      }
    } catch (OBException e) {
      log.error("Exception while deleting insurance certificate:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting insurance certificate:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
