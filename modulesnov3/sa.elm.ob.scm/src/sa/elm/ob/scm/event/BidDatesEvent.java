package sa.elm.ob.scm.event;

import java.sql.SQLException;
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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbiddates;
import sa.elm.ob.scm.event.dao.BidEventDAO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class BidDatesEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmbiddates.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) throws SQLException {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      Escmbiddates dates = (Escmbiddates) event.getTargetInstance();
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      // DateFormat timeFormat = new SimpleDateFormat("HH:mm");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      String allowPastDate = "";

      final Property openEnvelopDayTime = entities[0]
          .getProperty(Escmbiddates.PROPERTY_OPENENVDAYTIME);
      final Property proposalLastDayTime = entities[0]
          .getProperty(Escmbiddates.PROPERTY_PROPOSALLASTDAYTIME);

      // Skip Past Date validation if SCM_AllowPastDate preference is present
      try {
        allowPastDate = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
            dates.getClient().getId(), dates.getOrganization().getId(),
            OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
            Constants.BID_MANAGEMENT_W);
      } catch (PropertyException e) {
        allowPastDate = "N";
      }
      if (allowPastDate.equals("N")) {
        if (dateFormat.parse(dateFormat.format(dates.getQuelastdate())).compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidDates_PastQus"));
        }
        if (dates.getOpenenvday() != null) {
          if (dateFormat.parse(dateFormat.format(dates.getOpenenvday())).compareTo(todaydate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_BidDates_PastOpen"));
          }
        }
      }

      /*
       * if (dates.getProposallastday() != null) { if
       * (dateFormat.parse(dateFormat.format(dates.getProposallastday())) .compareTo(todaydate) < 0)
       * { throw new OBException(OBMessageUtils.messageBD("ESCM_BidDates_PastPropsals")); } }
       */
      if (dates.getOpenenvday() != null && dates.getQuelastdate() != null) {
        if (dateFormat.parse(dateFormat.format(dates.getOpenenvday()))
            .compareTo(dateFormat.parse(dateFormat.format(dates.getQuelastdate()))) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Proposal_Questions_Day(Compare)"));
        }
      }
      /*
       * if (dates.getProposallastday() != null && dates.getOpenenvday() != null) { if
       * (dateFormat.parse(dateFormat.format(dates.getOpenenvday())).compareTo(
       * dateFormat.parse(dateFormat.format(dates.getProposallastday()))) <= 0) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Proposal_Envelope_Day(Compare)")); } } if
       * (dates.getProposallastday() != null && dates.getOpenenvday() != null) { if
       * (dateFormat.parse(dateFormat.format(dates.getOpenenvday())).compareTo(
       * dateFormat.parse(dateFormat.format(dates.getProposallastday()))) == 0) {
       * 
       * if (timeFormat.parse(dates.getOpenenvdaytime()).compareTo(
       * timeFormat.parse(dates.getProposallastdaytime())) <= 0) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Proposal_Envelope_Day(Compare)")); }
       * 
       * } }
       */
      /*
       * // Check time entered in biddate is valid String proposalTime =
       * event.getCurrentState(proposalLastDayTime).toString(); String propTime[] =
       * proposalTime.split(":");
       * 
       * if (Integer.parseInt(propTime[0]) > 23) { if (Integer.parseInt(propTime[0]) == 24) { if
       * (Integer.parseInt(propTime[1]) > 0) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_NotValidPropTime")); } } else { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_NotValidPropTime")); } }
       */
      String proposalTime = event.getCurrentState(openEnvelopDayTime).toString();
      String envTime[] = proposalTime.split(":");

      if (Integer.parseInt(envTime[0]) > 23) {
        if (Integer.parseInt(envTime[0]) == 24) {
          if (Integer.parseInt(envTime[1]) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidEnvTime"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidEnvTime"));
        }
      }
      String proposallastdayTime = event.getCurrentState(proposalLastDayTime).toString();
      String proposallastTime[] = proposallastdayTime.split(":");

      if (Integer.parseInt(proposallastTime[0]) > 23) {
        if (Integer.parseInt(proposallastTime[0]) == 24) {
          if (Integer.parseInt(proposallastTime[1]) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidPropsalTime"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidPropsalTime"));
        }
      }
      EscmBidMgmt bidmgmt = Utility.getObject(EscmBidMgmt.class, dates.getEscmBidmgmt().getId());
      if (bidmgmt.getBidstatus().equals("ACT") && bidmgmt.getBidappstatus().equals("ESCM_AP")
          && bidmgmt.getTabadulTenderID() != null) {
        bidmgmt.setBidstatus("EXT");
        bidmgmt.setBidappstatus("ESCM_RA");
        bidmgmt.setEscmDocaction("CO");
        bidmgmt.setTabadulStatus("EXT");
      }
      if (bidmgmt.getBidappstatus().equals("ESCM_AP") || bidmgmt.getBidappstatus().equals("DR")
          || bidmgmt.getBidstatus().equals("EXT")) {
        BidEventDAO.setBidDate(bidmgmt.getId(), dates.getId());
      }
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error("exception while creating BidDatesEvent", e);
    }

    catch (OBException e) {
      log.error("exception while creating BidDatesEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating BidDatesEvent", e);
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
      Escmbiddates dates = (Escmbiddates) event.getTargetInstance();
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      final Property quslastday = entities[0].getProperty(Escmbiddates.PROPERTY_QUELASTDATE);
      // final Property propsallastday = entities[0]
      // .getProperty(Escmbiddates.PROPERTY_PROPOSALLASTDAY);
      final Property openenvelopday = entities[0].getProperty(Escmbiddates.PROPERTY_OPENENVDAY);
      final Property proposalLastDayTime = entities[0]
          .getProperty(Escmbiddates.PROPERTY_PROPOSALLASTDAYTIME);
      final Property openEnvelopDayTime = entities[0]
          .getProperty(Escmbiddates.PROPERTY_OPENENVDAYTIME);
      String allowPastDate = "";

      // Skip Past Date validation if SCM_AllowPastDate preference is present
      if (!event.getCurrentState(quslastday).equals(event.getPreviousState(quslastday))
          || (event.getCurrentState(openenvelopday) != null && !event
              .getCurrentState(openenvelopday).equals(event.getPreviousState(openenvelopday)))) {
        try {
          allowPastDate = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
              dates.getClient().getId(), dates.getOrganization().getId(),
              OBContext.getOBContext().getUser().getId(),
              OBContext.getOBContext().getRole().getId(), Constants.BID_MANAGEMENT_W);
        } catch (PropertyException e) {
          allowPastDate = "N";
        }
        if (allowPastDate.equals("N")) {
          if (!event.getCurrentState(quslastday).equals(event.getPreviousState(quslastday))) {
            if (dateFormat.parse(dateFormat.format(dates.getQuelastdate()))
                .compareTo(todaydate) < 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_BidDates_PastQus"));
            }
          }
          if (event.getCurrentState(openenvelopday) != null && !event
              .getCurrentState(openenvelopday).equals(event.getPreviousState(openenvelopday))) {
            if (dateFormat.parse(dateFormat.format(dates.getOpenenvday()))
                .compareTo(todaydate) < 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_BidDates_PastOpen"));
            }
          }
        }
      }

      /*
       * if (event.getCurrentState(propsallastday) != null &&
       * !event.getCurrentState(propsallastday).equals(event.getPreviousState(propsallastday))) { if
       * (dateFormat.parse(dateFormat.format(dates.getProposallastday())).compareTo(todaydate) < 0)
       * { throw new OBException(OBMessageUtils.messageBD("ESCM_BidDates_PastPropsals")); } }
       */

      if (dates.getOpenenvday() != null && dates.getQuelastdate() != null) {
        if (dateFormat.parse(dateFormat.format(dates.getOpenenvday()))
            .compareTo(dateFormat.parse(dateFormat.format(dates.getQuelastdate()))) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Proposal_Questions_Day(Compare)"));
        }
      }
      /*
       * if (dates.getProposallastday() != null && dates.getOpenenvday() != null) { if
       * (dateFormat.parse(dateFormat.format(dates.getOpenenvday())).compareTo(
       * dateFormat.parse(dateFormat.format(dates.getProposallastday()))) <= 0) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Proposal_Envelope_Day(Compare)")); } } if
       * (dates.getProposallastday() != null && dates.getOpenenvday() != null) { if
       * (dateFormat.parse(dateFormat.format(dates.getOpenenvday())).compareTo(
       * dateFormat.parse(dateFormat.format(dates.getProposallastday()))) == 0) {
       * 
       * if (timeFormat.parse(dates.getOpenenvdaytime()).compareTo(
       * timeFormat.parse(dates.getProposallastdaytime())) <= 0) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Proposal_Envelope_Day(Compare)")); }
       * 
       * } }
       */
      /*
       * // Check time entered in biddate is valid String proposalTime =
       * event.getCurrentState(proposalLastDayTime).toString(); String propTime[] =
       * proposalTime.split(":");
       * 
       * if (Integer.parseInt(propTime[0]) > 23) { if (Integer.parseInt(propTime[0]) == 24) { if
       * (Integer.parseInt(propTime[1]) > 0) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_NotValidPropTime")); } } else { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_NotValidPropTime")); } }
       */

      String proposalTime = event.getCurrentState(openEnvelopDayTime).toString();
      String envTime[] = proposalTime.split(":");

      if (Integer.parseInt(envTime[0]) > 23) {
        if (Integer.parseInt(envTime[0]) == 24) {
          if (Integer.parseInt(envTime[1]) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidEnvTime"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidEnvTime"));
        }
      }

      String proposallastdayTime = event.getCurrentState(proposalLastDayTime).toString();
      String proposallastTime[] = proposallastdayTime.split(":");

      if (Integer.parseInt(proposallastTime[0]) > 23) {
        if (Integer.parseInt(proposallastTime[0]) == 24) {
          if (Integer.parseInt(proposallastTime[1]) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidPropsalTime"));
          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NotValidPropsalTime"));
        }
      }

      /*
       * EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class,
       * dates.getEscmBidmgmt().getId()); if (bidmgmt.getBidstatus().equals("ACT") &&
       * bidmgmt.getBidappstatus().equals("ESCM_AP")) { if (dates.isApproved()) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_AppDateCantModify")); } }
       */

    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error("exception while updating BidDatesEvent", e);
    } catch (OBException e) {
      log.error("exception while updating BidDatesEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating BidDatesEvent", e);
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
      Escmbiddates dates = (Escmbiddates) event.getTargetInstance();
      // Completed record can not be delete
      if (dates.isApproved() && dates.getEscmBidmgmt().getBidstatus().equals("ACT")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Biddate_NotAllow(Delete)"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting BidDates : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting BidDates : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
