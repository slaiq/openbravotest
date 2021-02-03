package sa.elm.ob.finance.event;

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
import org.openbravo.model.common.plm.ProductCategory;

/**
 * 
 * @author Gowtham V
 *
 */
public class ProductCategoryEvent extends EntityPersistenceEventObserver {
  /**
   * should not allow more than one advance in category.
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ProductCategory.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ProductCategory proCategory = (ProductCategory) event.getTargetInstance();
      final Property isAdv = entities[0].getProperty(ProductCategory.PROPERTY_EFINISADVANCE);
      final Property isSummary = entities[0].getProperty(ProductCategory.PROPERTY_SUMMARYLEVEL);

      // Search key cannot be updated if account dimension exists
      if (!event.getCurrentState(isAdv).equals(event.getPreviousState(isAdv))) {
        if (proCategory.isEfinIsadvance()) {
          OBQuery<ProductCategory> categoryList = OBDal.getInstance()
              .createQuery(ProductCategory.class, "efinIsadvance='Y'");
          if (categoryList.list() != null && categoryList.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_PCategory_Adv"));

          }
        }
      }
      // if issummary yes for exst adv category then diable it.
      if (!event.getCurrentState(isSummary).equals(event.getPreviousState(isSummary))) {
        if (proCategory.isSummaryLevel() && proCategory.isEfinIsadvance()) {
          event.setCurrentState(isAdv, false);
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating Product category:  " + e);
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
      ProductCategory proCategory = (ProductCategory) event.getTargetInstance();
      if (proCategory.isEfinIsadvance()) {
        OBQuery<ProductCategory> categoryList = OBDal.getInstance()
            .createQuery(ProductCategory.class, "efinIsadvance='Y'");
        if (categoryList.list() != null && categoryList.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_PCategory_Adv"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating  Product category:   " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
