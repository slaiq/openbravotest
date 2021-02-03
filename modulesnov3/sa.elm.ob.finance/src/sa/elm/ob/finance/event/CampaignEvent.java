package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;

public class CampaignEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Campaign.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Campaign budtype = (Campaign) event.getTargetInstance();
      final Property startdate = entities[0].getProperty(Campaign.PROPERTY_STARTINGDATE);
      final Property enddate = entities[0].getProperty(Campaign.PROPERTY_ENDINGDATE);
      final Property value = entities[0].getProperty(Campaign.PROPERTY_SEARCHKEY);
      // Search key cannot be updated if account dimension exists
      if (!event.getCurrentState(value).equals(event.getPreviousState(value))) {
        verifyAccountDimension(event);
      }
      // should not allowing to enter "Active from date" more than "Active to date"
      if (budtype.getStartingDate() != null && budtype.getEndingDate() != null
          && (!event.getCurrentState(startdate).equals(event.getPreviousState(startdate))
              || !event.getCurrentState(enddate).equals(event.getPreviousState(enddate)))) {
        if (budtype.getStartingDate().compareTo(budtype.getEndingDate()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Invalid_ToDate"));
        }

      }
    } catch (OBException e) {
      log.error(" Exception while updating Budget Type:  " + e);
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
      Campaign budtype = (Campaign) event.getTargetInstance();
      // should not allowing to enter "Active from date" more than "Active to date"
      if (budtype.getStartingDate() != null && budtype.getEndingDate() != null
          && budtype.getStartingDate().compareTo(budtype.getEndingDate()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Invalid_ToDate"));
      }
    } catch (OBException e) {
      log.error(" Exception while creating Budget Type:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void verifyAccountDimension(EntityPersistenceEvent event) {
    try {
      OBContext.setAdminMode();
      Campaign camp = (Campaign) event.getTargetInstance();

      final OBCriteria<AccountingCombination> ac = OBDal.getInstance()
          .createCriteria(AccountingCombination.class);
      ac.add(Restrictions.eq(AccountingCombination.PROPERTY_SALESCAMPAIGN, camp));

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
