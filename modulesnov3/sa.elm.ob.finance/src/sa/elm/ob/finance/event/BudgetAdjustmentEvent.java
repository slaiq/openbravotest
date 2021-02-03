package sa.elm.ob.finance.event;

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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.event.dao.DistibuteMethodDAO;

/**
 * @author Gopalakrishnan on 09/10/2017
 * 
 */
public class BudgetAdjustmentEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(BudgetAdjustment.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      BudgetAdjustment Adjustment = (BudgetAdjustment) event.getTargetInstance();
      final Property requesterRole = entities[0].getProperty(BudgetAdjustment.PROPERTY_ROLE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      // get role
      event.setCurrentState(requesterRole, OBContext.getOBContext().getRole());

      // Future Date not allowed for transaction date
      if (Adjustment.getTRXDate() != null) {
        if (Adjustment.getTRXDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (Adjustment.getAccountingDate() != null) {
        if (Adjustment.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }

    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(" Exception while Saving  in Budget Adjustment: " + e, e);
      }
      throw new OBException(e.getMessage());
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

      BudgetAdjustment Adjustment = (BudgetAdjustment) event.getTargetInstance();
      ConnectionProvider conn = new DalConnectionProvider(false);
      final Property distributeOrg = entities[0]
          .getProperty(BudgetAdjustment.PROPERTY_DISTRIBUTIONLINKORG);
      final Property transactiondate = entities[0].getProperty(BudgetAdjustment.PROPERTY_TRXDATE);
      final Property accountingdate = entities[0]
          .getProperty(BudgetAdjustment.PROPERTY_ACCOUNTINGDATE);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      DistibuteMethodDAO dao = new DistibuteMethodDAO(conn);
      Object currentdistributeOrg = event.getCurrentState(distributeOrg);
      Object previousdistributeOrg = event.getPreviousState(distributeOrg);

      if (currentdistributeOrg != previousdistributeOrg) {
        if (Adjustment.getDistributionLinkOrg() != null) {
          dao.checkDistUniquecodePresntOrNot(null, Adjustment, conn, Adjustment.getId(), null,
              Adjustment.getDistributionLinkOrg().getId());
        }

      }
      // Future Date not allowed for transaction date
      if (Adjustment.getTRXDate() != null & (!event.getCurrentState(transactiondate)
          .equals(event.getPreviousState(transactiondate)))) {
        if (Adjustment.getTRXDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (Adjustment.getAccountingDate() != null & (!event.getCurrentState(accountingdate)
          .equals(event.getPreviousState(accountingdate)))) {
        if (Adjustment.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while creating BudgetRevisionEvent: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
