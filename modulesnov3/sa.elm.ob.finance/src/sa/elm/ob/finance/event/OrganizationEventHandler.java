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
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

public class OrganizationEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Organization.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    insertAccDept(event);

    // Search key cannot be updated if account dimension exists
    final Property value = entities[0].getProperty(Organization.PROPERTY_SEARCHKEY);
    if (!event.getCurrentState(value).equals(event.getPreviousState(value))) {
      verifyAccountDimension(event);
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    insertAccDept(event);
  }

  private void verifyAccountDimension(EntityPersistenceEvent event) {
    try {
      OBContext.setAdminMode();
      Organization org = (Organization) event.getTargetInstance();

      final OBCriteria<AccountingCombination> ac = OBDal.getInstance()
          .createCriteria(AccountingCombination.class);

      ac.add(Restrictions.eq(AccountingCombination.PROPERTY_ORGANIZATION, org));
      if (ac.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_AccountDimension_Exists"));
      }
    } catch (OBException e) {
      logger.error(" Exception : " + e);
      throw new OBException(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void insertAccDept(EntityPersistenceEvent event) {
    /*
     * if(costCenter){ //create record under acc dept tab Client client = null; Organization
     * organization = null; User user = null; SalesRegion salesRegion = null; try {
     * OBContext.setAdminMode(true);
     * 
     * client = OBDal.getInstance().get(Client.class, org.getClient().getId()); organization =
     * OBDal.getInstance().get(Organization.class, org.getId()); user =
     * OBDal.getInstance().get(User.class, org.getCreatedBy().getId());
     * 
     * salesRegion = OBProvider.getInstance().get(SalesRegion.class);
     * 
     * salesRegion.setClient(client); salesRegion.setOrganization(organization);
     * salesRegion.setCreatedBy(user); salesRegion.setUpdatedBy(user);
     * salesRegion.setSearchKey(org.getSearchKey()); salesRegion.setName(org.getName());
     * salesRegion.setDescription(org.getDescription()); OBDal.getInstance().save(salesRegion);
     * OBDal.getInstance().commitAndClose(); OBDal.getInstance().flush(); } catch (Exception e) {
     * logger.error(" Exception while inserting in Accounts Department ", e); } finally {
     * OBContext.restorePreviousMode(); } }
     */
  }
}