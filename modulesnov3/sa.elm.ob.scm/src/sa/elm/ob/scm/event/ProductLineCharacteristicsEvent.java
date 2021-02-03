package sa.elm.ob.scm.event;

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
import org.openbravo.model.common.plm.ProductCharacteristic;

/**
 * 
 * @author Priyanka Ranjan on 28/03/2017
 * 
 */

public class ProductLineCharacteristicsEvent extends EntityPersistenceEventObserver {
  /**
   * This Class is responsible for business events in m_product_ch Table
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ProductCharacteristic.ENTITY_NAME) };

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
      ProductCharacteristic objproductch = (ProductCharacteristic) event.getTargetInstance();
      final Property characteristic = entities[0]
          .getProperty(ProductCharacteristic.PROPERTY_CHARACTERISTIC);
      if (!event.getCurrentState(characteristic).equals(event.getPreviousState(characteristic))) {
        // Characteristic should be unique
        OBQuery<ProductCharacteristic> objProductChQuery = OBDal.getInstance().createQuery(
            ProductCharacteristic.class,
            "as e where e.characteristic.id=:characID and e.product.id=:prdtID and e.client.id=:clientID");
        objProductChQuery.setNamedParameter("characID", objproductch.getCharacteristic().getId());
        objProductChQuery.setNamedParameter("prdtID", objproductch.getProduct().getId());
        objProductChQuery.setNamedParameter("clientID", objproductch.getClient().getId());

        if (objProductChQuery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Ch_Unique"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating  Product Characteristics:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating  Product Characteristics:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
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
      ProductCharacteristic objproductch = (ProductCharacteristic) event.getTargetInstance();
      // Characteristic should be unique
      OBQuery<ProductCharacteristic> objProductChQuery = OBDal.getInstance().createQuery(
          ProductCharacteristic.class,
          "as e where e.characteristic.id=:characID and e.product.id=:prdtID  and e.client.id=:clientID");
      objProductChQuery.setNamedParameter("characID", objproductch.getCharacteristic().getId());
      objProductChQuery.setNamedParameter("prdtID", objproductch.getProduct().getId());
      objProductChQuery.setNamedParameter("clientID", objproductch.getClient().getId());
      if (objProductChQuery.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Product_Ch_Unique"));
      }
    } catch (OBException e) {
      log.error(" Exception while creating Product Characteristics: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating Product Characteristics: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
