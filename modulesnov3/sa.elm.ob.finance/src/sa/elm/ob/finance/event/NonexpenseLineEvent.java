package sa.elm.ob.finance.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinNonexpenseLines;

public class NonexpenseLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinNonexpenseLines.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  private static final Logger log = LoggerFactory.getLogger(NonexpenseLineEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EfinNonexpenseLines nonexpline = (EfinNonexpenseLines) event.getTargetInstance();
      final Property uniquecode = entities[0].getProperty(EfinNonexpenseLines.PROPERTY_UNIQUECODE);
      OBQuery<EfinNonexpenseLines> duplicate = OBDal.getInstance().createQuery(
          EfinNonexpenseLines.class, " uniqueCode='" + nonexpline.getUniqueCode() + "'");
      if (!event.getPreviousState(uniquecode).equals(event.getCurrentState(uniquecode))) {
        OBQuery<EFINBudgetLines> budget = OBDal.getInstance().createQuery(EFINBudgetLines.class,
            "accountElement.id='" + nonexpline.getAccountElement().getId() + "'");
        if (budget.list().size() > 0) {
          ElementValue element = OBDal.getInstance().get(ElementValue.class,
              nonexpline.getAccountElement().getId());
          throw new OBException(
              OBMessageUtils.messageBD("Efin_Acct_in_Budget").replace("%", element.getSearchKey()));
        }
        if (duplicate.list() != null && duplicate.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode"));
        }

      }

    } catch (OBException e) {
      log.error(" Exception while updating line in Nonexpense accounts: " + e);
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
      EfinNonexpenseLines nonexpline = (EfinNonexpenseLines) event.getTargetInstance();
      OBQuery<EFINBudgetLines> budget = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          "accountElement.id='" + nonexpline.getAccountElement().getId() + "'");
      if (budget.list().size() > 0) {
        ElementValue element = OBDal.getInstance().get(ElementValue.class,
            nonexpline.getAccountElement().getId());
        throw new OBException(
            OBMessageUtils.messageBD("Efin_Acct_in_Budget").replace("%", element.getSearchKey()));
      }
      OBQuery<EfinNonexpenseLines> duplicate = OBDal.getInstance().createQuery(
          EfinNonexpenseLines.class, " uniqueCode='" + nonexpline.getUniqueCode() + "'");
      if (duplicate.list() != null && duplicate.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode"));
      }
    } catch (OBException e) {
      log.error(" Exception while creating line in Nonexpense Accounts:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) throws SQLException {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinNonexpenseLines nonexpline = (EfinNonexpenseLines) event.getTargetInstance();
      String Uniquecode = nonexpline.getUniqueCode();
      PreparedStatement ps = null;
      ResultSet rs = null;
      Connection conn = OBDal.getInstance().getConnection();
      ps = conn.prepareStatement(
          "select * from gl_journal gl left join gl_journalline li on gl.gl_journal_id = li.gl_journal_id "
              + "where em_efin_uniquecode=? and docstatus in('CO','WFA')");
      ps.setString(1, Uniquecode);
      rs = ps.executeQuery();

      if (rs.next()) {

        throw new OBException(OBMessageUtils.messageBD("Efin_Nonexpense_entry"));

      }
    } catch (OBException e) {
      log.error(" Exception while creating line in Nonexpense Accounts:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
