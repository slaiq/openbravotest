package sa.elm.ob.finance.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public class ValidCombinationEvent extends EntityPersistenceEventObserver {
  /**
   * This class is used for handling the event in c_validcombination table
   */

  private final static String PROJECT = "PJ";
  private final static String USERDIMENSION1 = "U1";
  private final static String USERDIMENSION2 = "U2";
  private final static String ACTIVITY = "AY";
  private final static String BPARTNER = "BP";

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(AccountingCombination.ENTITY_NAME) };

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
      OBContext.setAdminMode();
      AccountingCombination dimension = (AccountingCombination) event.getTargetInstance();
      final Property uniqueCodeField = entities[0]
          .getProperty(AccountingCombination.PROPERTY_EFINUNIQUECODE);
      final Property uniqueCodeNameField = entities[0]
          .getProperty(AccountingCombination.PROPERTY_EFINUNIQUECODENAME);
      final Property project = entities[0].getProperty(AccountingCombination.PROPERTY_PROJECT);
      final Property bp = entities[0].getProperty(AccountingCombination.PROPERTY_BUSINESSPARTNER);
      final Property fc = entities[0].getProperty(AccountingCombination.PROPERTY_ACTIVITY);
      final Property future1 = entities[0].getProperty(AccountingCombination.PROPERTY_STDIMENSION);
      final Property future2 = entities[0].getProperty(AccountingCombination.PROPERTY_NDDIMENSION);
      final Property organization = entities[0]
          .getProperty(AccountingCombination.PROPERTY_TRXORGANIZATION);

      StringBuilder uniqueCodeName = new StringBuilder();
      StringBuilder uniqueCode = new StringBuilder();

      Map<String, Object> searchkey = new ConcurrentHashMap<String, Object>();
      Map<String, Object> name = new ConcurrentHashMap<String, Object>();
      Map<String, Object> idMap = new ConcurrentHashMap<String, Object>();

      final OBQuery<AcctSchemaElement> dimensions = OBDal.getInstance().createQuery(
          AcctSchemaElement.class,
          "client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

      // getting default dimension- searchkey, name and object and stored in map
      if (dimensions.list().size() > 0) {
        for (AcctSchemaElement dim : dimensions.list()) {
          if (dim.getType().equals(PROJECT)) {
            searchkey.put(PROJECT, dim.getProject().getSearchKey());
            name.put(PROJECT, dim.getProject().getName());
            idMap.put(PROJECT, dim.getProject());
          }
          if (dim.getType().equals(USERDIMENSION1)) {
            searchkey.put(USERDIMENSION1, dim.getEfinUser1().getSearchKey());
            name.put(USERDIMENSION1, dim.getEfinUser1().getName());
            idMap.put(USERDIMENSION1, dim.getEfinUser1());
          }
          if (dim.getType().equals(USERDIMENSION2)) {
            searchkey.put(USERDIMENSION2, dim.getEfinUser2().getSearchKey());
            name.put(USERDIMENSION2, dim.getEfinUser2().getName());
            idMap.put(USERDIMENSION2, dim.getEfinUser2());
          }
          if (dim.getType().equals(ACTIVITY)) {
            searchkey.put(ACTIVITY, dim.getActivity().getSearchKey());
            name.put(ACTIVITY, dim.getActivity().getName());
            idMap.put(ACTIVITY, dim.getActivity());
          }
          if (dim.getType().equals(BPARTNER)) {
            searchkey.put(BPARTNER, dim.getBusinessPartner().getSearchKey());
            name.put(BPARTNER, dim.getBusinessPartner().getName());
            idMap.put(BPARTNER, dim.getBusinessPartner());
          }
        }
      }

      // uniquecode and uniquecodename formation

      if (dimension.getOrganization() != null && dimension.getSalesRegion() != null
          && dimension.getAccount() != null) {

        uniqueCode.append(dimension.getOrganization().getSearchKey() + "-"
            + dimension.getSalesRegion().getSearchKey() + "-"
            + dimension.getAccount().getSearchKey());

        uniqueCodeName.append(dimension.getOrganization().getName() + "-"
            + dimension.getSalesRegion().getName() + "-" + dimension.getAccount().getName());

      }

      if (dimension.getProject() == null) {
        uniqueCode.append("-" + searchkey.get(PROJECT));
        uniqueCodeName.append("-" + name.get(PROJECT));
        event.setCurrentState(project, idMap.get(PROJECT));
      } else {
        uniqueCode.append("-" + dimension.getProject().getSearchKey());
        uniqueCodeName.append("-" + dimension.getProject().getName());
      }

      if (dimension.getSalesCampaign() != null) {
        uniqueCode.append("-" + dimension.getSalesCampaign().getSearchKey());
        uniqueCodeName.append("-" + dimension.getSalesCampaign().getName());
      }

      if (dimension.getBusinessPartner() == null) {
        uniqueCode.append("-" + searchkey.get(BPARTNER));
        uniqueCodeName.append("-" + name.get(BPARTNER));
        event.setCurrentState(bp, idMap.get(BPARTNER));
      } else {
        uniqueCode.append("-" + dimension.getBusinessPartner().getSearchKey());
        uniqueCodeName.append("-" + dimension.getBusinessPartner().getName());
      }

      if (dimension.getActivity() == null) {
        uniqueCode.append("-" + searchkey.get(ACTIVITY));
        uniqueCodeName.append("-" + name.get(ACTIVITY));
        event.setCurrentState(fc, idMap.get(ACTIVITY));
      } else {
        uniqueCode.append("-" + dimension.getActivity().getSearchKey());
        uniqueCodeName.append("-" + dimension.getActivity().getName());
      }

      if (dimension.getStDimension() == null) {
        uniqueCode.append("-" + searchkey.get(USERDIMENSION1));
        uniqueCodeName.append("-" + name.get(USERDIMENSION1));
        event.setCurrentState(future1, idMap.get(USERDIMENSION1));
      } else {
        uniqueCode.append("-" + dimension.getStDimension().getSearchKey());
        uniqueCodeName.append("-" + dimension.getStDimension().getName());
      }

      if (dimension.getNdDimension() == null) {
        uniqueCode.append("-" + searchkey.get(USERDIMENSION2));
        uniqueCodeName.append("-" + name.get(USERDIMENSION2));
        event.setCurrentState(future2, idMap.get(USERDIMENSION2));
      } else {
        uniqueCode.append("-" + dimension.getNdDimension().getSearchKey());
        uniqueCodeName.append("-" + dimension.getNdDimension().getName());
      }

      OBCriteria<AccountingCombination> accountCombination = OBDal.getInstance()
          .createCriteria(AccountingCombination.class);
      accountCombination.add(
          Restrictions.eq(AccountingCombination.PROPERTY_EFINUNIQUECODE, uniqueCode.toString()));
      accountCombination.setFilterOnActive(false);
      if (!(accountCombination.list() != null && accountCombination.list().size() > 0)) {
        event.setCurrentState(uniqueCodeField, uniqueCode.toString());
        event.setCurrentState(uniqueCodeNameField, uniqueCodeName.toString());
      } else {
        throw new OBException(OBMessageUtils.messageBD("Efin_CodeAlready_Exists"));
      }
      event.setCurrentState(organization, dimension.getOrganization());
    } catch (OBException e) {
      if (log.isDebugEnabled()) {
        log.debug(" Exception while saving dimension in validcombination: " + e);
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
      AccountingCombination dimension = (AccountingCombination) event.getTargetInstance();
      Property isActive = entities[0].getProperty(AccountingCombination.PROPERTY_ACTIVE);
      if (!dimension.isActive().equals(event.getPreviousState(isActive))) {
        if (dimension.isActive()) {
          if (!dimension.getAccount().isActive()) {
            throw new OBException(OBMessageUtils.messageBD("EFIN_CannotActivatetheDimension"));
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating validcombination: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
