package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EFINBudgetTypeAcct;

public class BudgetTypeAcctEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINBudgetTypeAcct.ENTITY_NAME) };

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
      EFINBudgetTypeAcct acct = (EFINBudgetTypeAcct) event.getTargetInstance();

      log.debug("acct:" + acct.getAccountElement().getSearchKey());
      if (acct.getAccountElement().getSearchKey() != null) {
        log.debug("acct:" + acct.getSalesCampaign().getId());
        OBQuery<EFINBudgetTypeAcct> budgetTypeAcct = OBDal.getInstance().createQuery(
            EFINBudgetTypeAcct.class,
            " salesCampaign.id='" + acct.getSalesCampaign() + "' and accountElement.id='"
                + acct.getAccountElement().getId() + "'  and client.id = '"
                + acct.getClient().getId() + "'");
        log.debug("budgetTypeAcct:" + budgetTypeAcct.getWhereAndOrderBy());
        List<EFINBudgetTypeAcct> budgetlist = budgetTypeAcct.list();
        if (budgetlist.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetTypeAcc_Event"));

        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Account in Budget Type: " + e);
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
      EFINBudgetTypeAcct acct = (EFINBudgetTypeAcct) event.getTargetInstance();
      if (acct.getAccountElement().getSearchKey() != null) {
        log.debug("budgetTypeAcct:" + acct.getSalesCampaign().getId());
        OBQuery<EFINBudgetTypeAcct> budgetTypeAcct = OBDal.getInstance().createQuery(
            EFINBudgetTypeAcct.class,
            " salesCampaign.id='" + acct.getSalesCampaign().getId() + "' and accountElement.id='"
                + acct.getAccountElement().getId() + "'  and client.id = '"
                + acct.getClient().getId() + "'");
        log.debug("budgetTypeAcct:" + budgetTypeAcct.getWhereAndOrderBy());
        List<EFINBudgetTypeAcct> budgetlist = budgetTypeAcct.list();
        if (budgetlist.size() > 0) {
          String language = OBContext.getOBContext().getLanguage().getLanguage();
          ConnectionProvider conn = new DalConnectionProvider(false);
          throw new OBException(Utility.messageBD(conn,
              "There is already a Budget Type Account with the same (Budget Type, Account Element). (Budget Type, Account Element) must be unique. You must change the values entered.",
              language));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating Account in Budget Type: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
