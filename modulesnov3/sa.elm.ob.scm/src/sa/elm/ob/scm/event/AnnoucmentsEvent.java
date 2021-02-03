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

import sa.elm.ob.scm.Escmannoucements;
import sa.elm.ob.utility.util.Utility;

public class AnnoucmentsEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmannoucements.ENTITY_NAME) };

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
      Escmannoucements annoucments = (Escmannoucements) event.getTargetInstance();
      final Property annoucementno = entities[0]
          .getProperty(Escmannoucements.PROPERTY_ANNOUCEMENTNO);

      String sequence = "";
      Boolean sequenceexists = false;
      // set new Spec No
      sequence = Utility.getTransactionSequencewithclient("0", annoucments.getClient().getId(),
          "ANN");
      sequenceexists = Utility.chkTransactionSequencewithclient(
          annoucments.getOrganization().getId(), annoucments.getClient().getId(), "ANN", sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(annoucementno, sequence);
      }

      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // Past date is not allowed in Announcement Date(H)
      if (annoucments.getAnnoucedate() != null) {
        if (dateFormat.parse(dateFormat.format(annoucments.getAnnoucedate()))
            .compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Ann_PastAnndate"));
        }
      }

      // Past date is not allowed in Announcement to date
      if (annoucments.getAnndateto() != null) {
        if (dateFormat.parse(dateFormat.format(annoucments.getAnndatefrom()))
            .compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Ann_PastAnnFromdate"));
        }
      }
      // 'Announcement Date to' should be not less than the 'Announcement Date from'
      if ((annoucments.getAnndateto() != null) && (annoucments.getAnndatefrom() != null)) {
        if (annoucments.getAnndateto().compareTo(annoucments.getAnndatefrom()) < 0) {
          throw new OBException(
              OBMessageUtils.messageBD("ESCM_AnnocToDateGreaterthanAnnocFromDate"));
        }
      }
    } catch (OBException e) {
      log.error("exception while saving announcement", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error("exception while saving announcement", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while saving announcement", e);
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
      Escmannoucements annoucments = (Escmannoucements) event.getTargetInstance();

      if (annoucments != null && !annoucments.getAlertStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
      }
    } catch (OBException e) {
      log.error(" Exception while Deleting RequisitionLine  : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting RequisitionLine  : ", e);
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
      Escmannoucements annoucments = (Escmannoucements) event.getTargetInstance();
      final Property annoucementdatefrom = entities[0]
          .getProperty(Escmannoucements.PROPERTY_ANNDATEFROM);
      final Property annoucementdateto = entities[0]
          .getProperty(Escmannoucements.PROPERTY_ANNDATETO);
      final Property annoucementdateH = entities[0]
          .getProperty(Escmannoucements.PROPERTY_ANNOUCEDATE);

      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // Past date is not allowed in Announcement Date(H)
      if (!event.getCurrentState(annoucementdateH)
          .equals(event.getPreviousState(annoucementdateH))) {
        if (annoucments.getAnnoucedate() != null) {
          if (dateFormat.parse(dateFormat.format(annoucments.getAnnoucedate()))
              .compareTo(todaydate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Ann_PastAnndate"));
          }
        }
      }

      // Past date is not allowed in Announcement to date
      if (annoucments.getAnndateto() != null) {
        if (dateFormat.parse(dateFormat.format(annoucments.getAnndatefrom()))
            .compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Ann_PastAnndate"));
        }
      }
      // 'Announcement Date to' should be not less than the 'Announcement Date from'
      if ((annoucments.getAnndateto() != null) && (annoucments.getAnndatefrom() != null)) {
        if (!event.getCurrentState(annoucementdatefrom)
            .equals(event.getPreviousState(annoucementdatefrom))
            || !event.getCurrentState(annoucementdateto)
                .equals(event.getPreviousState(annoucementdateto))) {
          if (annoucments.getAnndateto().compareTo(annoucments.getAnndatefrom()) < 0) {
            throw new OBException(
                OBMessageUtils.messageBD("ESCM_AnnocToDateGreaterthanAnnocFromDate"));
          }

        }
      }
    } catch (OBException e) {
      log.error(" Exception while Updating announcement  : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Updating announcement  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
