package sa.elm.ob.finance.event.SequenceHandlers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLJournal;

import sa.elm.ob.finance.EfinYearSequence;
import sa.elm.ob.utility.util.UtilityDAO;

public class GLJournalEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(GLJournal.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    String AccountDate = "";
    String CalendarId = "";
    String OrgId = "";
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      GLJournal journal = (GLJournal) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(GLJournal.PROPERTY_DOCUMENTNO);
      OrgId = journal.getOrganization().getId();
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(journal.getAccountingDate());
      Organization org = null;
      org = OBDal.getInstance().get(Organization.class, journal.getOrganization().getId());

      if (org.getCalendar() != null) {
        CalendarId = org.getCalendar().getId();

      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery("select eut_parent_org ('"
            + journal.getOrganization().getId() + "','" + journal.getClient().getId() + "')");
        @SuppressWarnings("unchecked")
        List<String> list = query.list();

        orgIds = list.get(0).split(",");

        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));

          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();

            break;
          }
        }

      }
      String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "GS", CalendarId, OrgId, true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
      }
      event.setCurrentState(documentNo, SequenceNo);

      if (journal.getEFINBudgetDefinition() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_budgetintman_gl"));
      }

      // Adjust prepayment invoice validation
      if (journal.isEfinAdjInvoice() && journal.getEfinCInvoice() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_G/L_Prepayment_Inv_Man"));
      }

    } catch (Exception e) {
      log.error(" Exception while Delete Account in Budget Type: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    String OrgId = "", preOrgId = "";
    String AccountDate = "", preAccountDate = "";
    String CalendarId = "", preCalendarId = "";

    try {
      OBContext.setAdminMode(true);
      GLJournal journal = (GLJournal) event.getTargetInstance();
      Property isAdjinvoice = entities[0].getProperty(GLJournal.PROPERTY_EFINADJINVOICE);
      Property invoice = entities[0].getProperty(GLJournal.PROPERTY_EFINCINVOICE);
      Property order = entities[0].getProperty(GLJournal.PROPERTY_EFINPURCHASEORDER);
      Property documentNo = entities[0].getProperty(GLJournal.PROPERTY_DOCUMENTNO);
      Property orgPro = entities[0].getProperty(GLJournal.PROPERTY_ORGANIZATION);
      Property docDate = entities[0].getProperty(GLJournal.PROPERTY_DOCUMENTDATE);

      // Updating Document No. only when the Document date is updated if year is changed
      if (!event.getCurrentState(docDate).equals(event.getPreviousState(docDate))) {
        Date newAcctDate = (Date) event.getCurrentState(docDate);
        Date oldAcctDate = (Date) event.getPreviousState(docDate);

        String newAcctDateYearId = UtilityDAO.eventgetYearId(newAcctDate,
            journal.getClient().getId());
        String oldAcctDateYearId = UtilityDAO.eventgetYearId(oldAcctDate,
            journal.getClient().getId());

        if (newAcctDateYearId != null && newAcctDateYearId.length() > 0 && oldAcctDateYearId != null
            && oldAcctDateYearId.length() > 0) {
          // Current Values
          OrgId = journal.getOrganization().getId();
          AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(journal.getAccountingDate());
          Organization org = null;
          org = OBDal.getInstance().get(Organization.class, journal.getOrganization().getId());
          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();
          } else {
            // get parent organization list
            String[] orgIds = null;
            SQLQuery query = OBDal.getInstance().getSession()
                .createSQLQuery("select eut_parent_org ('" + journal.getOrganization().getId()
                    + "','" + journal.getClient().getId() + "')");
            @SuppressWarnings("unchecked")
            List<String> list = query.list();

            orgIds = list.get(0).split(",");

            for (int i = 0; i < orgIds.length; i++) {
              org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));

              if (org.getCalendar() != null) {
                CalendarId = org.getCalendar().getId();
                break;
              }
            }
          }
          // Updating the Document No based on the Accounting Date
          String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "GS", CalendarId, OrgId,
              true);
          if (SequenceNo.equals("0")) {
            throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
          }
          event.setCurrentState(documentNo, SequenceNo);

          // Previous Values
          Organization preOrg = (Organization) event.getPreviousState(orgPro);
          String preDocumentno = (String) event.getPreviousState(documentNo);
          preOrgId = journal.getOrganization().getId();
          preAccountDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(event.getPreviousState(docDate));

          if (preOrg.getCalendar() != null) {
            preCalendarId = preOrg.getCalendar().getId();
          } else {
            // get parent organization list
            String[] orgIds = null;
            SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
                "select eut_parent_org ('" + preOrgId + "','" + journal.getClient().getId() + "')");
            @SuppressWarnings("unchecked")
            List<String> list = query.list();
            orgIds = list.get(0).split(",");
            for (int i = 0; i < orgIds.length; i++) {
              preOrg = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
              if (preOrg.getCalendar() != null) {
                preCalendarId = preOrg.getCalendar().getId();
                break;
              }
            }
          }
          updateGeneralSequence(preAccountDate, "GS", preCalendarId, preOrgId, preDocumentno);
        }
      }

      if (journal.getEFINBudgetDefinition() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_budgetintman_gl"));
      }

      // Adjust prepayment invoice validation
      if (journal.isEfinAdjInvoice() && journal.getEfinCInvoice() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_G/L_Prepayment_Inv_Man"));
      }
      if (!journal.isEfinAdjInvoice()) {
        event.setCurrentState(invoice, null);
        event.setCurrentState(order, null);
      }

    } catch (Exception e) {
      log.error(" Exception while updating simple gljournal: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unlikely-arg-type")
  public void updateGeneralSequence(String AccountDate, String Type, String CalendarId,
      String OrgId, String preDocNo) {
    String yearquery = "", ParentQury = "";
    String[] orgIds = null;
    String periodQuery = "";
    String sequence = "0";
    String sequenceId = "";
    String yearId = "";
    String periodId = "";
    String gsQuery = "";
    try {
      OBContext.setAdminMode();
      yearquery = "     select yr.c_year_id from c_period pr"
          + " join c_year yr on pr.c_year_id=yr.c_year_id" + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";
      periodQuery = "select pr.c_period_id from c_period pr "
          + " join c_year yr on pr.c_year_id=yr.c_year_id " + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";
      // get general sequence
      if (Type.equals("GS")) {
        Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
        Object yearID = resultset.list().get(0);
        yearId = (String) yearID;
        // get GeneralSequence number from year
        gsQuery = "select yrseq.efin_year_sequence_id from ad_sequence seq "
            + "join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
            + " where yrseq.c_year_id= :year_id and seq.em_efin_isgeneralseq='Y' and seq.ad_org_id= :org_id ";
        Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
        genSequencelist.setParameter("year_id", yearId);
        genSequencelist.setParameter("org_id", OrgId);
        if (genSequencelist.list().size() == 0) {
          ParentQury = " select eut_parent_org('" + OrgId + "','"
              + OBContext.getOBContext().getCurrentClient().getId() + "')";
          Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
          Object parentOrg = parentresult.list().get(0);
          orgIds = ((String) parentOrg).split(",");
          for (int i = 0; i < orgIds.length; i++) {
            Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
            Sequencelist.setParameter("year_id", yearId);
            Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
            if (Sequencelist.list().size() > 0) {
              Object sequenceID = Sequencelist.list().get(0);
              sequenceId = (String) sequenceID;
              break;
            }
          }
        } else {
          Object sequenceID = genSequencelist.list().get(0);
          sequenceId = (String) sequenceID;
        }
        EfinYearSequence yearSequence = OBDal.getInstance().get(EfinYearSequence.class, sequenceId);
        if (yearSequence != null) {
          sequence = yearSequence.getNextAssignedNumber() == null ? ""
              : yearSequence.getNextAssignedNumber().toString();
          String preYearSequenceno = Long
              .toString(Math.subtractExact(yearSequence.getNextAssignedNumber(), new Long(1)));

          if ((preYearSequenceno).equals(preDocNo)) {
            yearSequence.setNextAssignedNumber(yearSequence.getNextAssignedNumber() - 1);
          }
          OBDal.getInstance().save(yearSequence);
        }
      }

    } catch (Exception e) {
      // TODO: handle exception
      log.error("Exception in UpdateGeneralSequence() Method : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
