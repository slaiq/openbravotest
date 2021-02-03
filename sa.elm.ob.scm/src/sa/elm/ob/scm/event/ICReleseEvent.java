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
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmICRelease;
import sa.elm.ob.utility.util.UtilityDAO;

public class ICReleseEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmICRelease.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  // saving validation while save the IC Relese
  // more than one relese should not be allowed.
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EscmICRelease icRelease = (EscmICRelease) event.getTargetInstance();
      if (icRelease.getEscmInsuranceCertificate() != null) {
        OBQuery<EscmICRelease> insCertificate = OBDal.getInstance().createQuery(EscmICRelease.class,
            "escmInsuranceCertificate.id='" + icRelease.getEscmInsuranceCertificate().getId()
                + "'");
        if (insCertificate.list() != null && insCertificate.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_IC_Relese_One"));
        }
      }
      // special characters should not allowed in insurance c letter reference

      if (UtilityDAO.chkSpecialCharpresentornot(icRelease.getInsuranceCLetterReference())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@",
            "Insurance cletter reference"));
      }

      // past date should not be allowed in requested date
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (icRelease.getLetterReferenceDateH() != null && dateFormat
          .parse(dateFormat.format(icRelease.getLetterReferenceDateH())).compareTo(todaydate) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_Pastletterdate"));
      }
      // Without Document number(PO/contract) can't do release
      if (icRelease.getEscmInsuranceCertificate().getSalesOrder() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Without_DocNo_CantDo_Release"));
      }
    } catch (OBException e) {
      log.error("Exception while savingIC release:", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("exception while parsing date in saving IC release", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while savingIC release:", e);
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
      EscmICRelease icRelease = (EscmICRelease) event.getTargetInstance();
      final Property letterDate = entities[0]
          .getProperty(EscmICRelease.PROPERTY_LETTERREFERENCEDATEH);
      final Property cletter = entities[0]
          .getProperty(EscmICRelease.PROPERTY_INSURANCECLETTERREFERENCE);

      // special characters should not allowed in insurance c letter reference

      if (event.getCurrentState(cletter) != null
          && !event.getCurrentState(cletter).equals(event.getPreviousState(cletter))
          && UtilityDAO.chkSpecialCharpresentornot(icRelease.getInsuranceCLetterReference())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_SpecialCharacter").replace("@",
            "Insurance cletter reference"));
      }

      // past date should not be allowed in requested date

      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (event.getCurrentState(letterDate) != null
          && !event.getCurrentState(letterDate).equals(event.getPreviousState(letterDate))
          && dateFormat.parse(dateFormat.format(icRelease.getLetterReferenceDateH()))
              .compareTo(todaydate) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_IC_Pastletterdate"));
      }
      // Without Document number(PO/contract) can't do release
      if (icRelease.getEscmInsuranceCertificate().getSalesOrder() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Without_DocNo_CantDo_Release"));
      }
    } catch (OBException e) {
      log.error("Exception while updating IC release:", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("exception while parsing date in updating IC release", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating IC release:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
