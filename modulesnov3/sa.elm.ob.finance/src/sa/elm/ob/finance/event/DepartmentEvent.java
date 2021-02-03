package sa.elm.ob.finance.event;

import java.util.List;

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
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.sales.SalesRegion;

public class DepartmentEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(SalesRegion.ENTITY_NAME) };

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
      SalesRegion dept = (SalesRegion) event.getTargetInstance();
      Property searchkey = entities[0].getProperty(SalesRegion.PROPERTY_SEARCHKEY);
      Property istransdep = entities[0].getProperty(SalesRegion.PROPERTY_EFINTRANSACTIONDEP);
      Property budgetdep = entities[0].getProperty(SalesRegion.PROPERTY_DEFAULT);
      if (dept.getSearchKey() != null && dept.getName() != null) {
        if (dept.getSearchKey() != event.getPreviousState(searchkey)) {
          OBQuery<SalesRegion> salesregion = OBDal.getInstance().createQuery(SalesRegion.class,
              "searchKey='" + dept.getSearchKey() + "' and client.id = '" + dept.getClient().getId()
                  + "'");
          List<SalesRegion> saleslist = salesregion.list();
          if (saleslist.size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_DepEvent_Error"));
          }
        }
      }
      if (dept.isEfinTransactiondep()) {
        if (!dept.isDefault()) {
          if (dept.isEfinTransactiondep() != event.getPreviousState(istransdep)) {
            OBQuery<SalesRegion> type = OBDal.getInstance().createQuery(SalesRegion.class,
                "efinTransactiondep='Y' and client.id = '" + dept.getClient().getId()
                    + "' and organization.id = '" + dept.getOrganization().getId() + "'");
            if (type.list().size() > 0) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Tran_Dep"));
            }
          }
        }
      }
      if (dept.isDefault() && dept.isEfinTransactiondep()) {
        throw new OBException(OBMessageUtils.messageBD("EM_Efin_TranDep_def_avail"));

      }
      if (dept.isDefault()) {
        if (!dept.isEfinTransactiondep()) {
          if (dept.isDefault() != event.getPreviousState(budgetdep)) {
            OBQuery<SalesRegion> type = OBDal.getInstance().createQuery(SalesRegion.class,
                "default='Y' and client.id = '" + dept.getClient().getId() + "'");
            if (type.list().size() > 0) {
              throw new OBException(OBMessageUtils.messageBD("EM_Efin_Dept_def_avail"));
            }
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Department in Organization: " + e);
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
      SalesRegion dept = (SalesRegion) event.getTargetInstance();
      log.debug("Dep" + dept.isDefault());
      log.debug("Dep" + dept.isEfinTransactiondep());
      if (dept.getSearchKey() != null && dept.getName() != null) {
        OBQuery<SalesRegion> salesregion = OBDal.getInstance().createQuery(SalesRegion.class,
            "searchKey='" + dept.getSearchKey() + "' and client.id = '" + dept.getClient().getId()
                + "'");
        List<SalesRegion> saleslist = salesregion.list();
        if (saleslist.size() > 0) {
          // throw new OBException(Utility.messageBD(conn,
          // "There is already a Department with the same (Client, Search Key). (Client, Search Key)
          // must be unique. You must change the value entered.",
          // language));
          throw new OBException(OBMessageUtils.messageBD("Efin_DepEvent_Error"));
        }

      }
      if (dept.isEfinTransactiondep()) {
        if (!dept.isDefault()) {
          OBQuery<SalesRegion> type = OBDal.getInstance().createQuery(SalesRegion.class,
              "efinTransactiondep='Y' and client.id = '" + dept.getClient().getId()
                  + "' and organization.id = '" + dept.getOrganization().getId() + "'");
          if (type.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Tran_Dep"));
          }
        }
      }
      if (dept.isDefault() && dept.isEfinTransactiondep()) {
        throw new OBException(OBMessageUtils.messageBD("EM_Efin_TranDep_def_avail"));

      }
      if (dept.isDefault()) {
        if (!dept.isEfinTransactiondep()) {
          OBQuery<SalesRegion> type = OBDal.getInstance().createQuery(SalesRegion.class,
              "default='Y' and client.id = '" + dept.getClient().getId() + "'");
          if (type.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("EM_Efin_Dept_def_avail"));
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating Department in Organization: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
