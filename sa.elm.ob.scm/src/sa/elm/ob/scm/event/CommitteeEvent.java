package sa.elm.ob.scm.event;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.utility.util.Utility;

public class CommitteeEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMCommittee.ENTITY_NAME) };

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
      ESCMCommittee committee = (ESCMCommittee) event.getTargetInstance();
      final Property committeeno = entities[0].getProperty(ESCMCommittee.PROPERTY_COMMITTEE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

      String sequence = "";
      Boolean sequenceexists = false;
      // set new Spec No
      sequence = Utility.getTransactionSequence(committee.getOrganization().getId(), "COM");
      sequenceexists = Utility.chkTransactionSequence(committee.getOrganization().getId(), "COM",
          sequence);
      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(committeeno, sequence);
      }

      // chk past date not allowed

      try {
        DateFormat dateyearFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        Date todaydate = dateFormat.parse(dateFormat.format(now));
        if (committee.getCreationDate() != null) {
          if (dateFormat.parse(dateFormat.format(committee.getCreationDate()))
              .compareTo(todaydate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_CreationDate"));
          }
        }
        if (committee.getStartingDate() != null) {
          if (dateFormat.parse(dateFormat.format(committee.getStartingDate()))
              .compareTo(todaydate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_StartDate"));
          }
        }
        if (committee.getEndDate() != null) {
          if (dateFormat.parse(dateFormat.format(committee.getEndDate()))
              .compareTo(todaydate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_EndDate"));
          }
          if (committee.getEndDate().compareTo(committee.getStartingDate()) <= 0) {
            throw new OBException(OBMessageUtils.messageBD("EHCM_LevStartDateGreaterEndDate"));
          }
        }
        if (committee.getType() != null) {
          String startdate1 = convertTohijriDate(
              dateyearFormat.format(committee.getStartingDate()));
          String startyear = startdate1.split("-")[2];
          if (committee.getType().equals("PC")) {
            int endyear = Integer.valueOf(startyear) + Integer.valueOf(1);
            String endingdate = endyear + startdate1.split("-")[1] + startdate1.split("-")[0];
            endingdate = getOneDayMinusHijiriDate(endingdate,
                OBContext.getOBContext().getCurrentClient().getId());
            endingdate = convertToGregorianDate(
                (endingdate.split("-")[2] + endingdate.split("-")[1] + endingdate.split("-")[0]));
            if (committee.getEndDate().compareTo(dateyearFormat.parse(endingdate)) > 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_ProCom_YearVal"));
            }

          } else if (committee.getType().equals("OEC")) {
            int endyear = Integer.valueOf(startyear) + Integer.valueOf(3);
            String endingdate = endyear + startdate1.split("-")[1]
                + startdate1.toString().split("-")[0];
            endingdate = getOneDayMinusHijiriDate(endingdate,
                OBContext.getOBContext().getCurrentClient().getId());
            endingdate = convertToGregorianDate(
                (endingdate.split("-")[2] + endingdate.split("-")[1] + endingdate.split("-")[0]));
            if (committee.getEndDate().compareTo(dateyearFormat.parse(endingdate)) > 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_OecCom_YearVal"));
            }
          }
        }
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        log.error("exception while creating Committee", e);
      } catch (Exception e) {
        log.error("exception while creating Committee", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
      // Decree No should be unique across all records of Organization.
      OBQuery<ESCMCommittee> Decreeno = OBDal.getInstance().createQuery(ESCMCommittee.class,
          " as e where e.decreeNo=:decreeno and e.organization.id =:orgID");
      Decreeno.setNamedParameter("decreeno", committee.getDecreeNo());
      Decreeno.setNamedParameter("orgID", committee.getOrganization().getId());

      if (Decreeno.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_DecreeNo_Unique_Org"));
      }

    } catch (OBException e) {
      log.error("exception while creating Committee", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating Committee", e);
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
      ESCMCommittee committee = (ESCMCommittee) event.getTargetInstance();
      final Property creationdate = entities[0].getProperty(ESCMCommittee.PROPERTY_CREATIONDATE);
      final Property startdate = entities[0].getProperty(ESCMCommittee.PROPERTY_STARTINGDATE);
      final Property type = entities[0].getProperty(ESCMCommittee.PROPERTY_TYPE);

      final Property enddate = entities[0].getProperty(ESCMCommittee.PROPERTY_ENDDATE);
      final Property decreeNo = entities[0].getProperty(ESCMCommittee.PROPERTY_DECREENO);
      DateFormat dateyearFormat = new SimpleDateFormat("yyyy-MM-dd");
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (!event.getCurrentState(creationdate).equals(event.getPreviousState(creationdate))) {
        if (dateFormat.parse(dateFormat.format(committee.getCreationDate()))
            .compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Com_CreationDate"));
        }
      }
      if (!event.getCurrentState(startdate).equals(event.getPreviousState(startdate))) {
        if (dateFormat.parse(dateFormat.format(committee.getStartingDate()))
            .compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Com_StartDate"));
        }
      }
      if (event.getCurrentState(enddate) != null
          && !event.getCurrentState(enddate).equals(event.getPreviousState(enddate))) {
        if (dateFormat.parse(dateFormat.format(committee.getEndDate())).compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Com_EndDate"));
        }
      }
      if ((!event.getCurrentState(startdate).equals(event.getPreviousState(startdate)))
          || (event.getCurrentState(enddate) != null
              && !event.getCurrentState(enddate).equals(event.getPreviousState(enddate)))) {
        if (committee.getEndDate().compareTo(committee.getStartingDate()) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("EHCM_LevStartDateGreaterEndDate"));
        }
      }
      if ((!event.getCurrentState(startdate).equals(event.getPreviousState(startdate)))
          || (event.getCurrentState(enddate) != null
              && !event.getCurrentState(enddate).equals(event.getPreviousState(enddate)))
          || (!event.getCurrentState(type).equals(event.getPreviousState(type)))) {
        String startdate1 = convertTohijriDate(dateyearFormat.format(committee.getStartingDate()));
        String startyear = startdate1.split("-")[2];
        if (committee.getType().equals("PC")) {
          int endyear = Integer.valueOf(startyear) + Integer.valueOf(1);
          String endingdate = endyear + startdate1.split("-")[1] + startdate1.split("-")[0];
          endingdate = getOneDayMinusHijiriDate(endingdate,
              OBContext.getOBContext().getCurrentClient().getId());
          endingdate = convertToGregorianDate(
              (endingdate.split("-")[2] + endingdate.split("-")[1] + endingdate.split("-")[0]));
          if (committee.getEndDate().compareTo(dateyearFormat.parse(endingdate)) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_ProCom_YearVal"));
          }

        } else if (committee.getType().equals("OEC")) {
          int endyear = Integer.valueOf(startyear) + Integer.valueOf(3);
          String endingdate = endyear + startdate1.split("-")[1] + startdate1.split("-")[0];
          endingdate = getOneDayMinusHijiriDate(endingdate,
              OBContext.getOBContext().getCurrentClient().getId());
          endingdate = convertToGregorianDate(
              (endingdate.split("-")[2] + endingdate.split("-")[1] + endingdate.split("-")[0]));
          if (committee.getEndDate().compareTo(dateyearFormat.parse(endingdate)) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_OecCom_YearVal"));
          }
        }
      }
      // Decree No should be unique across all records of Organization.
      if (!event.getCurrentState(decreeNo).equals(event.getPreviousState(decreeNo))) {
        OBQuery<ESCMCommittee> Decreeno = OBDal.getInstance().createQuery(ESCMCommittee.class,
            " as e where e.decreeNo=:decreeno and e.organization.id =:orgID");
        Decreeno.setNamedParameter("decreeno", committee.getDecreeNo());
        Decreeno.setNamedParameter("orgID", committee.getOrganization().getId());
        if (Decreeno.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_DecreeNo_Unique_Org"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Committee ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating Committee ", e);
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
      ESCMCommittee committee = (ESCMCommittee) event.getTargetInstance();
      // Completed record can not be delete
      if (committee.getAlertStatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Committee_Completed"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting Committee : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting Committee : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unused")
  public String getOneDayMinusHijiriDate(String gregoriandate, String clientId) {
    BigInteger days = BigInteger.ZERO;
    Query query = null;
    String strQuery = "", startdate = "";
    try {

      strQuery = " select  hijri_date from eut_hijri_dates  where hijri_date < '" + gregoriandate
          + "'  order by hijri_date desc limit 1 ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null && query.list().size() > 0) {
        Object row = query.list().get(0);
        startdate = (String) row;
        startdate = startdate.substring(6, 8) + "-" + startdate.substring(4, 6) + "-"
            + startdate.substring(0, 4);
      }
    } catch (Exception e) {
      log.error("Exception in getOneDayMinusHijiriDate", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return startdate;
  }

  public String convertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery gradeQuery = OBDal.getInstance().getSession()
          .createSQLQuery("select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
              + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (gradeQuery.list().size() > 0) {
        Object row = (Object) gradeQuery.list().get(0);
        hijriDate = (String) row;
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

  public String convertToGregorianDate(String hijriDate) {
    String gregDate = "";
    try {
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(
          "select to_char(gregorian_date,'YYYY-MM-DD')  from eut_hijri_dates where hijri_date ='"
              + hijriDate + "'");
      if (Query.list().size() > 0) {
        Object row = (Object) Query.list().get(0);
        gregDate = (String) row;
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertToGregorianDate() Method : ", e);
      return "0";
    }
    return gregDate;
  }
}
