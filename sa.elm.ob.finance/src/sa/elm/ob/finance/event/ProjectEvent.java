package sa.elm.ob.finance.event;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.project.Project;
import org.openbravo.model.project.ProjectAccounts;

public class ProjectEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Project.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    // Search key cannot be updated if account dimension exists
    final Property value = entities[0].getProperty(Project.PROPERTY_SEARCHKEY);
    if (!event.getCurrentState(value).equals(event.getPreviousState(value))) {
      verifyAccountDimension(event);
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    PreparedStatement ps = null;
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Project project = (Project) event.getTargetInstance();
      Connection con = OBDal.getInstance().getConnection();
      OBQuery<ProjectAccounts> prjacct = OBDal.getInstance().createQuery(ProjectAccounts.class,
          " project.id='" + project.getId() + "'");
      if (prjacct.list().size() > 0) {
        for (ProjectAccounts proj : prjacct.list()) {
          ps = con.prepareStatement(
              "delete from c_project_acct where c_project_acct_id='" + proj.getId() + "' ");
          ps.executeUpdate();
        }

      }
    } catch (Exception e) {
      log.error(" Exception while Delete Project: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void verifyAccountDimension(EntityPersistenceEvent event) {
    try {
      OBContext.setAdminMode();
      Project proj = (Project) event.getTargetInstance();

      final OBCriteria<AccountingCombination> ac = OBDal.getInstance()
          .createCriteria(AccountingCombination.class);
      ac.add(Restrictions.eq(AccountingCombination.PROPERTY_PROJECT, proj));

      if (ac.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_AccountDimension_Exists"));
      }
    } catch (OBException e) {
      log.error(" Exception : " + e);
      throw new OBException(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
