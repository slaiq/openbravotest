package sa.elm.ob.finance.event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Sathish Kumar on 26/09/2017
 * 
 */

// Handle the events in Initial budget creation window

public class InitialBudgetCreationEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetIntialization.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger LOG = Logger.getLogger(this.getClass());

  PreparedStatement ps1 = null;
  ResultSet rs1 = null;

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      final Property status = entities[0].getProperty(EfinBudgetIntialization.PROPERTY_STATUS);

      ConnectionProvider conn = new DalConnectionProvider(false);
      OBContext.setAdminMode();
      EfinBudgetIntialization budInt = (EfinBudgetIntialization) event.getTargetInstance();

      // check From period is greater than
      if (budInt.getToPeriod().getStartingDate()
          .compareTo(budInt.getFromperiod().getStartingDate()) < 0) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_ToprdLessFrmprd"));
      }

      String fromDate = Utility.formatDate(budInt.getFromperiod().getStartingDate());
      String toDate = Utility.formatDate(budInt.getToPeriod().getEndingDate());

      // check whether any other record is present on same period.
      ps1 = conn.getPreparedStatement("select budint.efin_budgetint_id from efin_budgetint  budint "
          + " join c_period frmperiod on frmperiod.c_period_id = budint.fromperiod "
          + " join c_period toperiod on toperiod.c_period_id = budint.toperiod "
          + " where  budint.ad_client_id='" + budInt.getClient().getId()
          + "' and ((to_date(to_char(frmperiod.startdate,'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
          + fromDate
          + "','dd-MM-yyyy') and to_date(to_char(toperiod.enddate,'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
          + toDate
          + "','dd-MM-yyyy')) or (to_date(to_char(toperiod.enddate, 'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
          + fromDate
          + "','dd-MM-yyyy') and to_date(to_char(frmperiod.startdate,'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
          + toDate + "','dd-MM-yyyy'))) ");
      rs1 = ps1.executeQuery();
      if (rs1.next()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_period_conflicts_budint"));
      }

      event.setCurrentState(status, "DR");

    } catch (OBException e) {
      LOG.error(" Exception while saving budget Intialization " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      LOG.error(" Exception while saving budget Intialization " + e);
      throw new OBException(e.getMessage());
    } finally {
      try {
        if (rs1 != null) {
          rs1.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      ConnectionProvider conn = new DalConnectionProvider(false);
      OBContext.setAdminMode();
      EfinBudgetIntialization budInt = (EfinBudgetIntialization) event.getTargetInstance();
      final Property year = entities[0].getProperty(EfinBudgetIntialization.PROPERTY_YEAR);
      final Property fromPrd = entities[0].getProperty(EfinBudgetIntialization.PROPERTY_FROMPERIOD);
      final Property toPrd = entities[0].getProperty(EfinBudgetIntialization.PROPERTY_TOPERIOD);

      if (!event.getCurrentState(year).equals(event.getPreviousState(year))
          || !event.getCurrentState(fromPrd).equals(event.getPreviousState(fromPrd))
          || !event.getCurrentState(toPrd).equals(event.getPreviousState(toPrd))) {

        // check From period is greater than
        if (budInt.getToPeriod().getStartingDate()
            .compareTo(budInt.getFromperiod().getStartingDate()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_ToprdLessFrmprd"));
        }

        String fromDate = Utility.formatDate(budInt.getFromperiod().getStartingDate());
        String toDate = Utility.formatDate(budInt.getToPeriod().getEndingDate());

        // check whether record is present on same period.
        ps1 = conn
            .getPreparedStatement("select budint.efin_budgetint_id from efin_budgetint  budint "
                + " join c_period frmperiod on frmperiod.c_period_id = budint.fromperiod "
                + " join c_period toperiod on toperiod.c_period_id = budint.toperiod "
                + " where  budint.ad_client_id='" + budInt.getClient().getId()
                + "' and ((to_date(to_char(frmperiod.startdate,'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
                + fromDate
                + "','dd-MM-yyyy') and to_date(to_char(toperiod.enddate,'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
                + toDate
                + "','dd-MM-yyyy')) or (to_date(to_char(toperiod.enddate, 'dd-MM-yyyy'),'dd-MM-yyyy') >= to_date('"
                + fromDate
                + "','dd-MM-yyyy') and to_date(to_char(frmperiod.startdate,'dd-MM-yyyy'),'dd-MM-yyyy') <= to_date('"
                + toDate + "','dd-MM-yyyy'))) and  budint.efin_budgetint_id != '" + budInt.getId()
                + "'");
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_period_conflicts_budint"));
        }
      }

    } catch (OBException e) {
      LOG.error(" Exception while saving budget Intialization: " + e, e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      LOG.error(" Exception while saving budget Intialization " + e, e);
      throw new OBException(e.getMessage());
    } finally {
      try {
        if (rs1 != null) {
          rs1.close();
        }
      } catch (Exception e) {
        LOG.error(" Exception while saving budget Intialization " + e, e);
      }
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinBudgetIntialization budgetInt = (EfinBudgetIntialization) event.getTargetInstance();
      if (budgetInt.getStatus().equals("OP") || budgetInt.getStatus().equals("CL")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_cannotdel_completed"));
      }

    } catch (OBException e) {
      LOG.error(" Exception while saving budget Intialization " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
