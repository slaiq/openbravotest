package sa.elm.ob.scm.event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.scm.ESCMCommitteeMembers;
import sa.elm.ob.utility.util.Utility;

public class CommitteeMembersEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMCommitteeMembers.ENTITY_NAME) };

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
      ESCMCommitteeMembers committeemem = (ESCMCommitteeMembers) event.getTargetInstance();
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      PreparedStatement ps = null;
      ResultSet rs = null;
      // chk past date not allowed

      try {
        Date now = new Date();
        Date todaydate = dateFormat.parse(dateFormat.format(now));
        /*
         * if (committeemem.getEffectiveFrom() != null) { if
         * (dateFormat.parse(dateFormat.format(committeemem.getEffectiveFrom())).compareTo(
         * todaydate) < 0) { throw new OBException(OBMessageUtils.messageBD("ESCM_Effective_From"));
         * } }
         */
        if (committeemem.getEffectiveTo() != null) {
          if (dateFormat.parse(dateFormat.format(committeemem.getEffectiveTo()))
              .compareTo(todaydate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Effective_To"));
          }
          if (committeemem.getEffectiveTo().compareTo(committeemem.getEffectiveFrom()) <= 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_FromNotGreatTo"));
          }
        }
        if (committeemem.getBusinessPartner() != null) {
          OBQuery<ESCMCommitteeMembers> members = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.businessPartner.id=:bpartnerID and e.escmCommittee.id =:committeeID");
          members.setNamedParameter("bpartnerID", committeemem.getBusinessPartner().getId());
          members.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());

          if (members.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Comm_SameEmp"));
          }
          ConnectionProvider conn = new DalConnectionProvider(false);
          String fdate = Utility.formatDate(committeemem.getEffectiveFrom());
          String tdate = Utility.formatDate(committeemem.getEffectiveTo());
          try {
            ps = conn.getPreparedStatement(
                "select mem.effective_from from escm_committee_members mem  left join  escm_committee hd on hd.escm_committee_id=mem.escm_committee_id"
                    + "   where hd.ad_org_id ='" + committeemem.getOrganization().getId()
                    + "' and hd.ad_client_id='" + committeemem.getClient().getId()
                    + "'  and mem.c_bpartner_id='" + committeemem.getBusinessPartner().getId()
                    + "' and hd.type!='" + committeemem.getEscmCommittee().getType()
                    + "' and ((to_date(to_char(mem.effective_from,'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
                    + fdate
                    + "') and to_date(to_char(coalesce (mem.effective_to,to_date('21-06-2058','dd-MM-yyyy')),'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
                    + tdate
                    + "','dd-MM-yyyy')) or (to_date(to_char( coalesce (mem.effective_to,to_date('21-06-2058','dd-MM-yyyy')) ,'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
                    + fdate
                    + "') and to_date(to_char(mem.effective_from,'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
                    + tdate + "','dd-MM-yyyy'))) ");
            rs = ps.executeQuery();
            if (rs.next()) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_EmpCom_Conflict"));
            }
          } catch (OBException e) {
            log.error("Exception while creating Committee members", e);
            throw new OBException(e.getMessage());
          } catch (Exception e) {
            log.error("Exception while creating Committee members", e);
            throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          }

        }
      } catch (OBException e) {
        log.error("Exception while creating Committee members", e);
        throw new OBException(e.getMessage());
      } catch (ParseException e) {
        log.error("Exception while creating Committee members", e);
        throw new OBException(e.getMessage());
      } catch (Exception e) {
        log.error("Exception while creating Committee members", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } finally {
        // close connection
        try {
          if (rs != null)
            rs.close();
          if (ps != null)
            ps.close();
        } catch (Exception e) {
          log.error("Exception while closing the statement in CommitteeMembersEvent", e);
        }
      }

      if (committeemem.getMemberType() != null) {
        // Only one President is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("P")) {
          OBQuery<ESCMCommitteeMembers> president = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='P' and e.escmCommittee.id =:committeeID");
          president.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (president.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_President"));
          }
        }
        // Only one President Repl. is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("PR")) {
          OBQuery<ESCMCommitteeMembers> presidentreplace = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='PR' and e.escmCommittee.id =:committeeID");
          presidentreplace.setNamedParameter("committeeID",
              committeemem.getEscmCommittee().getId());

          if (presidentreplace.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_PresRepl"));
          }
        }
        // Only one Financial Ctrl is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("FC")) {
          OBQuery<ESCMCommitteeMembers> financialctrl = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='FC' and e.escmCommittee.id =:committeeID");
          financialctrl.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (financialctrl.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_Financial_Ctrl"));
          }
        }
        // Only one Secretary is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("SEC")) {
          OBQuery<ESCMCommitteeMembers> Secretary = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='SEC' and e.escmCommittee.id =:committeeID ");
          Secretary.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (Secretary.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_Secretary"));
          }
        }
        // Only one Member repl. is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("MR")) {
          OBQuery<ESCMCommitteeMembers> memberreplace = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='MR' and e.escmCommittee.id =:committeeID");
          memberreplace.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());

          if (memberreplace.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_Memb_Repl"));
          }
        }
      }
      // Effective From and Effective To should be present in between of Committee Period.
      if (committeemem.getEffectiveFrom() != null) {
        if (committeemem.getEffectiveFrom()
            .compareTo(committeemem.getEscmCommittee().getStartingDate()) < 0
            || committeemem.getEffectiveFrom()
                .compareTo(committeemem.getEscmCommittee().getEndDate()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Members_Date_Validation"));
        }
      }
      if (committeemem.getEffectiveTo() != null) {
        if (committeemem.getEffectiveTo()
            .compareTo(committeemem.getEscmCommittee().getStartingDate()) < 0
            || committeemem.getEffectiveTo()
                .compareTo(committeemem.getEscmCommittee().getEndDate()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Members_Date_Validation"));

        }
      }

    } catch (OBException e) {
      log.error("Exception while creating Committee members", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      ESCMCommitteeMembers committeemem = (ESCMCommitteeMembers) event.getTargetInstance();
      final Property startdate = entities[0]
          .getProperty(ESCMCommitteeMembers.PROPERTY_EFFECTIVEFROM);
      final Property enddate = entities[0].getProperty(ESCMCommitteeMembers.PROPERTY_EFFECTIVETO);
      final Property empployee = entities[0]
          .getProperty(ESCMCommitteeMembers.PROPERTY_BUSINESSPARTNER);

      final Property memberType = entities[0].getProperty(ESCMCommitteeMembers.PROPERTY_MEMBERTYPE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      /*
       * if (!event.getCurrentState(startdate).equals(event.getPreviousState(startdate))) { if
       * (dateFormat.parse(dateFormat.format(committeemem.getEffectiveFrom())).compareTo( todaydate)
       * < 0) { throw new OBException(OBMessageUtils.messageBD("ESCM_Effective_From")); } }
       */
      if (event.getCurrentState(enddate) != null
          && !event.getCurrentState(enddate).equals(event.getPreviousState(enddate))) {
        if (dateFormat.parse(dateFormat.format(committeemem.getEffectiveTo()))
            .compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Effective_To"));
        }
      }
      if ((!event.getCurrentState(startdate).equals(event.getPreviousState(startdate)))
          || (event.getCurrentState(enddate) != null
              && !event.getCurrentState(enddate).equals(event.getPreviousState(enddate)))) {
        if (committeemem.getEffectiveTo().compareTo(committeemem.getEffectiveFrom()) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_FromNotGreatTo"));
        }
      }
      if (!event.getCurrentState(empployee).equals(event.getPreviousState(empployee))) {
        if (committeemem.getBusinessPartner() != null) {
          OBQuery<ESCMCommitteeMembers> members = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.businessPartner.id=:bpartnerID and e.escmCommittee.id =:committeeID");
          members.setNamedParameter("bpartnerID", committeemem.getBusinessPartner().getId());
          members.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());

          if (members.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Comm_SameEmp"));
          }
        }
      }
      if (!event.getCurrentState(empployee).equals(event.getPreviousState(empployee))
          || (!event.getCurrentState(startdate).equals(event.getPreviousState(startdate)))
          || (event.getCurrentState(enddate) != null
              && !event.getCurrentState(enddate).equals(event.getPreviousState(enddate)))) {
        ConnectionProvider conn = new DalConnectionProvider(false);
        String fdate = Utility.formatDate(committeemem.getEffectiveFrom());
        String tdate = Utility.formatDate(committeemem.getEffectiveTo());
        ps = conn.getPreparedStatement(
            "select mem.effective_from from escm_committee_members mem  left join  escm_committee hd on hd.escm_committee_id=mem.escm_committee_id"
                + "   where hd.ad_org_id ='" + committeemem.getOrganization().getId()
                + "' and hd.ad_client_id='" + committeemem.getClient().getId()
                + "'  and mem.c_bpartner_id='" + committeemem.getBusinessPartner().getId()
                + "' and hd.type!='" + committeemem.getEscmCommittee().getType()
                + "' and ((to_date(to_char(mem.effective_from,'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
                + fdate
                + "') and to_date(to_char(coalesce (mem.effective_to,to_date('21-06-2058','dd-MM-yyyy')),'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
                + tdate
                + "','dd-MM-yyyy')) or (to_date(to_char( coalesce (mem.effective_to,to_date('21-06-2058','dd-MM-yyyy')) ,'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
                + fdate
                + "') and to_date(to_char(mem.effective_from,'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
                + tdate + "','dd-MM-yyyy'))) ");
        rs = ps.executeQuery();
        if (rs.next()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_EmpCom_Conflict"));
        }

      }
      if (event.getCurrentState(memberType) != null
          && !event.getCurrentState(memberType).equals(event.getPreviousState(memberType))) {
        // Only one President is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("P")) {
          OBQuery<ESCMCommitteeMembers> president = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='P' and e.escmCommittee.id =:committeeID");
          president.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (president.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_President"));
          }
        }
        // Only one President Repl. is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("PR")) {
          OBQuery<ESCMCommitteeMembers> presidentreplace = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='PR' and e.escmCommittee.id =:committeeID");
          presidentreplace.setNamedParameter("committeeID",
              committeemem.getEscmCommittee().getId());
          if (presidentreplace.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_PresRepl"));
          }
        }
        // Only one Financial Ctrl is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("FC")) {
          OBQuery<ESCMCommitteeMembers> financialctrl = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='FC' and e.escmCommittee.id =:committeeID");
          financialctrl.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (financialctrl.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_Financial_Ctrl"));
          }
        }
        // Only one Secretary is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("SEC")) {
          OBQuery<ESCMCommitteeMembers> Secretary = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='SEC' and e.escmCommittee.id =:committeeID");
          Secretary.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (Secretary.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_Secretary"));
          }
        }
        // Only one Member repl. is allow for one Committee.
        if (committeemem.getMemberType().getSearchKey().equals("MR")) {
          OBQuery<ESCMCommitteeMembers> memberreplace = OBDal.getInstance().createQuery(
              ESCMCommitteeMembers.class,
              " as e where e.memberType.searchKey='MR' and e.escmCommittee.id =:committeeID");
          memberreplace.setNamedParameter("committeeID", committeemem.getEscmCommittee().getId());
          if (memberreplace.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Com_One_Memb_Repl"));
          }
        }
      }
      // Effective From and Effective To should be present in between of Committee Period.
      if (!event.getCurrentState(startdate).equals(event.getPreviousState(startdate))) {
        if (committeemem.getEffectiveFrom() != null) {
          if (committeemem.getEffectiveFrom()
              .compareTo(committeemem.getEscmCommittee().getStartingDate()) < 0
              || committeemem.getEffectiveFrom()
                  .compareTo(committeemem.getEscmCommittee().getEndDate()) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Members_Date_Validation"));
          }
        }
      }
      if (!event.getCurrentState(enddate).equals(event.getPreviousState(enddate))) {
        if (committeemem.getEffectiveTo() != null) {
          if (committeemem.getEffectiveTo()
              .compareTo(committeemem.getEscmCommittee().getStartingDate()) < 0
              || committeemem.getEffectiveTo()
                  .compareTo(committeemem.getEscmCommittee().getEndDate()) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Members_Date_Validation"));

          }
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating Committee ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in CommitteeMembersEvent ", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}
