package sa.elm.ob.finance.event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINBudgetTypeAcct;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.event.dao.AccountTreeElementValueEventDAO;

public class AccountTreeEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ElementValue.ENTITY_NAME) };

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
      ElementValue elementValue = (ElementValue) event.getTargetInstance();
      final Property activeProperty = entities[0].getProperty(ElementValue.PROPERTY_ACTIVE);
      Property validToDate = entities[0].getProperty(ElementValue.PROPERTY_VALIDTODATE);
      Property validFromDate = entities[0].getProperty(ElementValue.PROPERTY_VALIDFROMDATE);
      Property allowBudget = entities[0].getProperty(ElementValue.PROPERTY_EFINALLOWBUDGETING);
      Property isSummary = entities[0].getProperty(ElementValue.PROPERTY_SUMMARYLEVEL);
      Property acctStatus = entities[0].getProperty(ElementValue.PROPERTY_EFINSTATUS);
      Property isActive = entities[0].getProperty(ElementValue.PROPERTY_ACTIVE);
      final Property value = entities[0].getProperty(ElementValue.PROPERTY_SEARCHKEY);
      final Property isdeptfund = entities[0].getProperty(ElementValue.PROPERTY_EFINISDEPTFUND);
      final Property isfundsOnly = entities[0].getProperty(ElementValue.PROPERTY_EFINFUNDSONLY);

      String clientid = elementValue.getClient().getId();
      ConnectionProvider conn = new DalConnectionProvider(false);
      boolean deptfund = elementValue.isEfinIsdeptfund() == null ? false
          : elementValue.isEfinIsdeptfund();

      if (log.isDebugEnabled()) {
        log.debug(" Active From:" + elementValue.getValidFromDate());
        log.debug("Active To:" + elementValue.getValidToDate());
        log.debug("new: " + elementValue.isActive());
        log.debug("old: " + event.getPreviousState(activeProperty));
      }

      // Summary level should not be checked if element level is Subaccount
      if ("S".equals(elementValue.getElementLevel()) && elementValue.isSummaryLevel()) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_CheckSubaccount"));
      }

      if (elementValue.getValidFromDate() != null && elementValue.getValidToDate() != null) {
        if (((elementValue.getValidFromDate()).compareTo((elementValue.getValidToDate())) > 0)) {
          log.error("Invalid From and To Date");
          throw new OBException(OBMessageUtils.messageBD("EFIN_Invalid_ToDate"));
        }
      }
      if (elementValue.isActive()
          && !(elementValue.isActive().equals(event.getPreviousState(activeProperty)))
          && (elementValue.getValidToDate() == null || elementValue.getValidFromDate() == null)) {
        // event.setCurrentState(validToDate, new
        // SimpleDateFormat("dd-MM-yyyy").parse("31-12-9999"));
        if ((elementValue.getValidToDate() == null) && (elementValue.getValidFromDate() == null)) {
          event.setCurrentState(validFromDate, new Date());
          event.setCurrentState(validToDate,
              new SimpleDateFormat("dd-MM-yyyy").parse("30-12-1480"));
        } else if (elementValue.getValidToDate() == null) {
          event.setCurrentState(validToDate,
              new SimpleDateFormat("dd-MM-yyyy").parse("31-12-1480"));
        } else if (elementValue.getValidFromDate() == null) {
          event.setCurrentState(validFromDate, new Date());
        }
        if (elementValue.getValidToDate() == null && elementValue.getValidFromDate() != null) {
          if (elementValue.getValidFromDate().after(new Date())) {
            event.setCurrentState(validFromDate, new Date());
          }
        }
      } else if (elementValue.isActive()
          && !(elementValue.isActive().equals(event.getPreviousState(activeProperty)))
          && (elementValue.getValidToDate() != null || elementValue.getValidFromDate() != null)) {
        if (elementValue.getValidFromDate().after(new Date())) {
          event.setCurrentState(validFromDate, new Date());
        }
      } else if (!elementValue.isActive()
          && !(elementValue.isActive().equals(event.getPreviousState(activeProperty)))
          && elementValue.getValidFromDate() == null) {
        if ((elementValue.getValidToDate() == null || elementValue.getValidToDate().equals(""))
            && (elementValue.getValidFromDate() == null
                || elementValue.getValidFromDate().equals(""))) {
          event.setCurrentState(validToDate, new Date());
        } else if (elementValue.getValidToDate() == null) {
          event.setCurrentState(validToDate, new Date());
        }
      } else if (!elementValue.isActive()
          && !(elementValue.isActive().equals(event.getPreviousState(activeProperty)))
          && elementValue.getValidFromDate() != null) {
        if (elementValue.getValidFromDate().before(new Date())
            && elementValue.getValidToDate() == null) {
          event.setCurrentState(validToDate, new Date());
        }
      }
      if (elementValue.isSummaryLevel() != null && elementValue.isEfinAllowBudgeting() != null
          && event.getPreviousState(isSummary) != null
          && event.getPreviousState(allowBudget) != null) {
        if ((!(elementValue.isSummaryLevel().equals(event.getPreviousState(isSummary))))
            || (!(elementValue.isEfinAllowBudgeting()
                .equals(event.getPreviousState(allowBudget))))) {
          OBQuery<EFINBudgetTypeAcct> budgetType = OBDal.getInstance().createQuery(
              EFINBudgetTypeAcct.class, "accountElement.id = '" + elementValue.getId() + "'");
          if (budgetType != null && budgetType.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Account_Element"));
          } else {
            if (!elementValue.isSummaryLevel()) {
              event.setCurrentState(allowBudget, false);
            }
          }
        }
      }

      // Search key cannot be updated if account dimension exists
      if (!event.getCurrentState(value).equals(event.getPreviousState(value))) {
        final OBCriteria<AccountingCombination> ac = OBDal.getInstance()
            .createCriteria(AccountingCombination.class);
        ac.add(Restrictions.eq(AccountingCombination.PROPERTY_ACCOUNT, elementValue));
        List<AccountingCombination> acList = ac.list();
        if (acList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AccountDimension_Exists"));
        }
      }

      // Restrict to edit deptfund flag for child account for which already we have uniquecode
      if (event.getPreviousState(isdeptfund) != null
          && !event.getCurrentState(isdeptfund).equals(event.getPreviousState(isdeptfund))
          && elementValue.isSummaryLevel().equals(false)) {
        OBQuery<AccountingCombination> acccomb = OBDal.getInstance().createQuery(
            AccountingCombination.class,
            "account.id = '" + elementValue.getId() + "' and efinUniqueCode is not null");
        if (acccomb != null && acccomb.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Uniquecode_already_Created"));
        }
      }

      // while updating 'department fund' flag in element value tab , update 'department fund' flag
      // with same value in all child accounts
      if (event.getPreviousState(isdeptfund) != null
          && !event.getCurrentState(isdeptfund).equals(event.getPreviousState(isdeptfund))) {
        AccountTreeElementValueEventDAO.updateDeptFund(elementValue.getId(), clientid, conn,
            deptfund);
      }

      OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
          " as e where e.account.id='" + elementValue.getId() + "'");
      if (!event.getPreviousState(isfundsOnly).equals(event.getCurrentState(isfundsOnly))) {
        OBQuery<EFINBudgetLines> budgLns = OBDal.getInstance().createQuery(EFINBudgetLines.class,
            " as e left join e.efinBudget bud where e.accountElement.id='" + elementValue.getId()
                + "' and bud.alertStatus = 'INAPP'");
        if (budInq.list().size() > 0 || budgLns.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_ElementCantBeChanged"));
        } /*
           * else { // Set Child elements funds only to Yes/No BudgetUtilsDAO utilDAO = new
           * BudgetUtilsDAOImpl(); if (elementValue.isSummaryLevel()) { List<String> childLs =
           * utilDAO.getChildren(elementValue.getId(), clientid); if (childLs.size() > 0) { for
           * (String elemId : childLs) { ElementValue elemVal =
           * Utility.getObject(ElementValue.class, elemId);
           * elemVal.setEfinFundsonly(elementValue.isEfinFundsonly()); } } } }
           */
      }

      if (event.getPreviousState(acctStatus) != null
          && !event.getPreviousState(acctStatus).equals(event.getCurrentState(acctStatus))) {
        OBQuery<AccountingCombination> acctComb = OBDal.getInstance().createQuery(
            AccountingCombination.class, " as e where e.account.id='" + elementValue.getId() + "'");
        acctComb.setFilterOnActive(false);
        // Hold to Withdrawn
        if (event.getPreviousState(acctStatus).equals("HO")
            && (event.getCurrentState(acctStatus) != null
                && event.getCurrentState(acctStatus).equals("WD"))) {
          event.setCurrentState(validToDate, new Date());
          event.setCurrentState(isActive, false);

          if (acctComb.list().size() > 0) {
            for (AccountingCombination acctcb : acctComb.list())
              acctcb.setActive(false);
          }
        }
        // Withdrawn to (Hold or null) - Status cannot be changed from withdrawn to hold
        else if (event.getPreviousState(acctStatus).equals("WD")
            && (event.getCurrentState(acctStatus) == null
                || event.getCurrentState(acctStatus).equals("")
                || event.getCurrentState(acctStatus).equals("HO"))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_NoStatusChangeFromWithdrawntoHold"));
        }
        // (Withdrawn or Hold) to Cancelled - Check transactions done if yes don't allow to cancel
        // or
        // else
        // allow to cancel
        else if ((event.getPreviousState(acctStatus).equals("WD")
            || event.getPreviousState(acctStatus).equals("HO"))
            && (event.getCurrentState(acctStatus) != null
                && event.getCurrentState(acctStatus).equals("CA"))) {
          if (budInq.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("EFIN_ElementStatusCantBeChanged"));
          } else {
            if (elementValue.getValidToDate() == null)
              event.setCurrentState(validToDate, new Date());
            event.setCurrentState(isActive, false);
            if (acctComb.list().size() > 0) {
              for (AccountingCombination acctcb : acctComb.list())
                acctcb.setActive(false);
            }
          }
        }
        // Cancelled to (Hold or Withdrawn or null)
        else if (event.getPreviousState(acctStatus).equals("CA")
            && (event.getCurrentState(acctStatus) == null
                || event.getCurrentState(acctStatus).equals("")
                || event.getCurrentState(acctStatus).equals("HO")
                || event.getCurrentState(acctStatus).equals("WD"))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_StatusChangeNotAllowed"));
        }
        // Hold to null
        else if (event.getPreviousState(acctStatus).equals("HO")
            && (event.getCurrentState(acctStatus) == null
                || event.getCurrentState(acctStatus).equals(""))) {
          event.setCurrentState(validToDate, null);
          event.setCurrentState(isActive, true);
          if (acctComb.list().size() > 0) {
            for (AccountingCombination acctcb : acctComb.list()) {
              acctcb.setActive(true);
            }
          }
        }
      } else if (event.getPreviousState(acctStatus) == null) {
        OBQuery<AccountingCombination> acctComb = OBDal.getInstance().createQuery(
            AccountingCombination.class, " as e where e.account.id='" + elementValue.getId() + "'");
        acctComb.setFilterOnActive(false);
        if (event.getCurrentState(acctStatus) != null
            && event.getCurrentState(acctStatus).equals("CA")) {
          if (budInq.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("EFIN_ElementStatusCantBeChanged"));
          } else {
            if (elementValue.getValidToDate() == null)
              event.setCurrentState(validToDate, new Date());
            event.setCurrentState(isActive, false);
            if (acctComb.list().size() > 0) {
              for (AccountingCombination acctcb : acctComb.list())
                acctcb.setActive(false);
            }
          }
        } else if (event.getCurrentState(acctStatus) != null
            && event.getCurrentState(acctStatus).equals("HO")) {
          event.setCurrentState(validToDate, null);
          event.setCurrentState(isActive, false);
          if (acctComb.list().size() > 0) {
            for (AccountingCombination acctcb : acctComb.list())
              acctcb.setActive(false);
          }
        } else if (event.getCurrentState(acctStatus) != null
            && event.getCurrentState(acctStatus).equals("WD")) {
          event.setCurrentState(validToDate, new Date());
          event.setCurrentState(isActive, false);
          if (acctComb.list().size() > 0) {
            for (AccountingCombination acctcb : acctComb.list())
              acctcb.setActive(false);
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Element Tree: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log.error(" Exception while updating Element Tree: " + e);
      throw new OBException(e.getMessage());
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

      ElementValue elementValue = (ElementValue) event.getTargetInstance();
      Property allowBudget = entities[0].getProperty(ElementValue.PROPERTY_EFINALLOWBUDGETING);

      if (log.isDebugEnabled()) {
        log.debug(" Active From:" + elementValue.getValidFromDate());
        log.debug("Active To:" + elementValue.getValidToDate());
      }

      // Summary level should not be checked if element level is Subaccount
      if ("S".equals(elementValue.getElementLevel()) && elementValue.isSummaryLevel()) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_CheckSubaccount"));
      }

      if (elementValue.getValidFromDate() != null && elementValue.getValidToDate() != null) {
        if (elementValue.getValidFromDate().after(elementValue.getValidToDate())) {
          log.error("Invalid From and To Date");
          throw new OBException(OBMessageUtils.messageBD("EFIN_Invalid_ToDate"));
        }
      }
      if (!elementValue.isSummaryLevel()) {
        event.setCurrentState(allowBudget, false);
      }
      /*
       * // while updating 'department fund' flag in element value tab , update 'department fund'
       * flag // with same value in all child accounts
       * AccountTreeElementValueEventDAO.updateDeptFund(elementValue.getId(), clientid, conn,
       * deptfund);
       */

    } catch (OBException e) {
      log.error(" Exception while creating Element Tree: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}