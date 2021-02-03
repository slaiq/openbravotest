package sa.elm.ob.scm.event;

import java.text.DateFormat;
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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.ESCMAnnouSummaryMedia;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.event.dao.AnnouncementMediaEventDAO;
import sa.elm.ob.utility.util.Constants;

public class AnnouncementMedia extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMAnnouSummaryMedia.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMAnnouSummaryMedia media = (ESCMAnnouSummaryMedia) event.getTargetInstance();
      ESCMDefLookupsTypeLn mediaType = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
          media.getMediaType().getId());
      final Property medianame = entities[0].getProperty(ESCMAnnouSummaryMedia.PROPERTY_MEDIANAME);
      final Property onlineMediaName = entities[0]
          .getProperty(ESCMAnnouSummaryMedia.PROPERTY_ONLINEMEDIA);
      final Property editiondate = entities[0]
          .getProperty(ESCMAnnouSummaryMedia.PROPERTY_EDITIONDATEH);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      boolean Result = false;
      String allowPastDate = "";

      if (mediaType.getSearchKey().equals("OLA")) {
        // check online media is having value
        if (media.getOnlinemedia() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_onlinemedia_mandatory"));
        }
      } else {
        if (media.getMediaName() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_medianame_mandatory"));
        }
      }
      // Past date is not allowed in Edition Date (H).
      if (media.getEditionDateH() != null) {
        if (!event.getCurrentState(editiondate).equals(event.getPreviousState(editiondate))) {
          try {
            allowPastDate = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
                media.getClient().getId(), media.getOrganization().getId(),
                OBContext.getOBContext().getUser().getId(),
                OBContext.getOBContext().getRole().getId(), Constants.ANNOUNCEMENT_SUMMARY_W);
          } catch (PropertyException e) {
            allowPastDate = "N";
          }
          if (allowPastDate.equals("N")) {
            if (dateFormat.parse(dateFormat.format(media.getEditionDateH()))
                .compareTo(todaydate) < 0) {
              throw new OBException(
                  OBMessageUtils.messageBD("ESCM_PastDateNotAllowedIN_EditionDate"));
            }
          }
        }
      }
      // Media name should not be same.
      if (mediaType.getSearchKey().equals("OLA")) {
        // check online media is having value
        if (!event.getCurrentState(onlineMediaName)
            .equals(event.getPreviousState(onlineMediaName))) {
          Result = AnnouncementMediaEventDAO.checkMediaExists(media);
        }
      } else {
        if (!event.getCurrentState(medianame).equals(event.getPreviousState(medianame))) {
          Result = AnnouncementMediaEventDAO.checkMediaExists(media);
        }
      }
      if (Result)
        throw new OBException(OBMessageUtils.messageBD("Escm_Media_Exists"));

    } catch (OBException e) {
      log.error("Exception while updating media:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating media:", e);
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
      ESCMAnnouSummaryMedia media = (ESCMAnnouSummaryMedia) event.getTargetInstance();
      ESCMDefLookupsTypeLn mediaType = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
          media.getMediaType().getId());
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      boolean Result = false;
      String allowPastDate = "";

      if (mediaType.getSearchKey().equals("OLA")) {
        // check online media is having value
        if (media.getOnlinemedia() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_onlinemedia_mandatory"));
        }
      } else {
        if (media.getMediaName() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_medianame_mandatory"));
        }
      }

      // Skip Past Date validation Edition Date (H) if SCM_AllowPastDate preference is present

      if (media.getEditionDateH() != null) {
        try {
          allowPastDate = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
              media.getClient().getId(), media.getOrganization().getId(),
              OBContext.getOBContext().getUser().getId(),
              OBContext.getOBContext().getRole().getId(), Constants.ANNOUNCEMENT_SUMMARY_W);
        } catch (PropertyException e) {
          allowPastDate = "N";
        }
        if (allowPastDate.equals("N")) {
          // Past date is not allowed in Edition Date (H).
          if (dateFormat.parse(dateFormat.format(media.getEditionDateH()))
              .compareTo(todaydate) < 0) {
            throw new OBException(
                OBMessageUtils.messageBD("ESCM_PastDateNotAllowedIN_EditionDate"));
          }
        }

      }
      // Media name should not be same.
      Result = AnnouncementMediaEventDAO.checkMediaExists(media);
      if (Result)
        throw new OBException(OBMessageUtils.messageBD("Escm_Media_Exists"));

    } catch (OBException e) {
      log.error("Exception while saving media", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while saving media", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
