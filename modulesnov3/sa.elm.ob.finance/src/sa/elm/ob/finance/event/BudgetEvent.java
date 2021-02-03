/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */

package sa.elm.ob.finance.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EfinBudgetPreparation;
import sa.elm.ob.finance.event.dao.BudgetLinesDAO;
import sa.elm.ob.finance.event.dao.DistibuteMethodDAO;
import sa.elm.ob.utility.util.Utility;

public class BudgetEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINBudget.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(BudgetLinesDAO.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EFINBudget budget = (EFINBudget) event.getTargetInstance();
      final Property transactiondate = entities[0].getProperty(EFINBudget.PROPERTY_TRANSACTIONDATE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      List<Period> oldDatePeriod = null, newDatePeriod = null;

      String oldYear = "", currentYear = "";
      String calender = budget.getYear().getCalendar().getId();

      // From and To period should be mandatory
      if (budget.getFrmperiod() == null || budget.getToperiod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Periods_Mandatory"));
      }
      ConnectionProvider conn = new DalConnectionProvider(false);
      final Property distributeOrg = entities[0]
          .getProperty(EFINBudget.PROPERTY_DISTRIBUTIONLINKORG);
      DistibuteMethodDAO dao = new DistibuteMethodDAO(conn);
      Object currentdistributeOrg = event.getCurrentState(distributeOrg);
      Object previousdistributeOrg = event.getPreviousState(distributeOrg);

      if (currentdistributeOrg != previousdistributeOrg) {
        if (budget.getDistributionLinkOrg() != null) {
          dao.checkDistUniquecodePresntOrNot(null, null, conn, budget.getId(), budget,
              budget.getDistributionLinkOrg().getId());
        }

      }
      // Future Date not allowed for transaction date
      if (budget.getTransactionDate() != null & (!event.getCurrentState(transactiondate)
          .equals(event.getPreviousState(transactiondate)))) {
        if (budget.getTransactionDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }

      // should not allow to update accounting date above the boundary line of year.
      oldDatePeriod = Utility.getPeriodList(event.getPreviousState(transactiondate), calender);

      if (oldDatePeriod != null && oldDatePeriod.size() > 0) {
        oldYear = oldDatePeriod.get(0).getYear().getId();
      } else {
        throw new OBException(OBMessageUtils.messageBD("Efin_Period_NotDefined_Exst"));
      }

      newDatePeriod = Utility.getPeriodList(budget.getTransactionDate(), calender);
      if (newDatePeriod != null && newDatePeriod.size() > 0) {
        currentYear = newDatePeriod.get(0).getYear().getId();
      } else {
        throw new OBException(OBMessageUtils.messageBD("Efin_Period_NotDefined_Current"));
      }

      if (!oldYear.equals(currentYear)) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Acctdate_CanntChange"));
      }

      /*
       * OBQuery<EFINBudget> budgetchk = OBDal.getInstance().createQuery(EFINBudget.class, " id='" +
       * budget.getId() + "' and client.id = '" + budget.getClient().getId() + "'");
       */

      /*
       * if (budget.getSalesCampaign().getId() != null && budget.getYear().getId() != null) { if
       * (budgetchk.list().size() > 0) { EFINBudget dblist = budgetchk.list().get(0); if
       * (!dblist.getSalesCampaign().getId().equals(budget.getSalesCampaign().getId()) ||
       * !dblist.getYear().getId().equals(budget.getYear().getId()) ||
       * !dblist.getAccountElement().getId().equals(budget.getAccountElement().getId())) { // Budget
       * should have combination of (Budgettype, account group, year) for funds budget // and cost
       * budget Campaign budgetType = OBDal.getInstance().get(Campaign.class,
       * budget.getSalesCampaign().getId()); if (budgetType.isEfinIscarryforward()) { count =
       * BudgetLinesDAO.checkCostBudgetCombination(budget); if (count > 0) { throw new OBException(
       * OBMessageUtils.messageBD("Efin_Budget_Costbudget_Validation")); } } else { count =
       * BudgetLinesDAO.checkFundsBudgetCombination(budget); if (count > 0) { throw new OBException(
       * OBMessageUtils.messageBD("Efin_Budget_Fundsbudget_Validation")); } } } } }
       */
    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while updating Account in Budget Type: " + e);
      }
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    boolean result = false;
    String AccountDate = "";
    try {
      OBContext.setAdminMode();

      EFINBudget budget = (EFINBudget) event.getTargetInstance();
      Property documentNo = entities[0].getProperty(EFINBudget.PROPERTY_DOCUMENTNO);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // From and To period should be mandatory
      if (budget.getFrmperiod() == null || budget.getToperiod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Periods_Mandatory"));
      }

      if (!budget.getEfinBudgetint().getStatus().equals("OP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_budgetint_notOpen"));
      }

      // Budget should have combination of (Budgettype, account group, year) for funds budget and
      // cost budget unique
      /*
       * if (budget.getSalesCampaign().getId() != null && budget.getYear().getId() != null) {
       * Campaign budgetType = OBDal.getInstance().get(Campaign.class,
       * budget.getSalesCampaign().getId()); if (budgetType.isEfinIscarryforward()) { count =
       * BudgetLinesDAO.checkCostBudgetCombination(budget); if (count > 0) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Budget_Costbudget_Validation")); } } else {
       * count = BudgetLinesDAO.checkFundsBudgetCombination(budget); if (count > 0) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Budget_Fundsbudget_Validation")); } } }
       */

      // funds budget should created after cost budget if same account in both budget type.
      if (budget.getSalesCampaign().getEfinBudgettype().equals("F")) {
        result = BudgetLinesDAO.checkCostBudgetAlreadyCreated(budget);
        if (result) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoCostBudget"));
        }
      }

      // Future Date not allowed for transaction date
      if (budget.getTransactionDate() != null) {
        if (budget.getTransactionDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }

      // set document NO.
      OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
          "as e where e.organization.id ='0'");
      if (calendarQuery.list().size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
      }
      AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(budget.getTransactionDate());
      Calendar calendar = calendarQuery.list().get(0);
      String SequenceNo = Utility.getDocumentSequence(AccountDate, "Efin_Budget", calendar.getId(),
          "0", true);
      if (SequenceNo.equals("0")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
      }
      event.setCurrentState(documentNo, SequenceNo);

    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while creating Account in Budget Type: " + e);
      }
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
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
      EFINBudget budget = (EFINBudget) event.getTargetInstance();
      if (budget.getAlertStatus().equals("APP") || budget.getAlertStatus().equals("INAPP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Delete"));
      }
      if (budget.getEfinBudgetPreparation() != null) {
        EfinBudgetPreparation budgPrep = OBDal.getInstance().get(EfinBudgetPreparation.class,
            budget.getEfinBudgetPreparation().getId());
        budgPrep.setConvertbudget(true);
        OBDal.getInstance().save(budgPrep);
      }
      // check having lines or not.
      /*
       * count = BudgetLinesDAO.getHeaderhaveLine(budget); if (count > 0) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_LinesPresentHeaderCannot_Delete")); }
       */
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Delete Account in Budget Type: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
