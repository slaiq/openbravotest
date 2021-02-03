package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;

import sa.elm.ob.scm.ESCMCharactTempltLine;

/**
 * @author Priyanka Ranjan on 03/03/2017
 */

public class ESCMCharacteristicsTemplateLineEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMCharactTempltLine.ENTITY_NAME) };

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
      ESCMCharactTempltLine Check = (ESCMCharactTempltLine) event.getTargetInstance();
      // update product window - characteristic tab if template is associated with some product
      final Property characteristic = entities[0]
          .getProperty(ESCMCharactTempltLine.PROPERTY_CHARACTERISTIC);
      if (!event.getCurrentState(characteristic).equals(event.getPreviousState(characteristic))) {
        OBQuery<Product> template = OBDal.getInstance().createQuery(Product.class,
            "as e where e.escmCharacTemplate.id=:tempId and e.client.id=:clientID");
        template.setNamedParameter("tempId", Check.getEscmCharactTemplt().getId());
        template.setNamedParameter("clientID", Check.getClient().getId());

        if (template.list().size() > 0) {
          for (Product prod : template.list()) {
            for (ProductCharacteristic character : prod.getProductCharacteristicList()) {
              Object oldcharact = event.getPreviousState(
                  entities[0].getProperty(ESCMCharactTempltLine.PROPERTY_CHARACTERISTIC));
              if (oldcharact.equals(character.getCharacteristic())) {
                character.setCharacteristic(Check.getCharacteristic());
                OBDal.getInstance().save(character);
                break;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error(" Exception while updating Characteristic Template Line  : ", e);
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
      ESCMCharactTempltLine Check = (ESCMCharactTempltLine) event.getTargetInstance();
      // Insert records in product window - characteristic tab if template is associated with some
      // product
      OBQuery<Product> template = OBDal.getInstance().createQuery(Product.class,
          "as e where e.escmCharacTemplate.id=:tempId and e.client.id=:clientID ");
      template.setNamedParameter("tempId", Check.getEscmCharactTemplt().getId());
      template.setNamedParameter("clientID", Check.getClient().getId());
      if (template.list().size() > 0) {
        for (Product prod : template.list()) {
          ProductCharacteristic line = OBProvider.getInstance().get(ProductCharacteristic.class);
          line.setSequenceNumber(Check.getLineNo());
          line.setCharacteristic(Check.getCharacteristic());
          line.setProduct(prod);
          OBDal.getInstance().save(line);
        }
      }

    } catch (Exception e) {
      log.error(" Exception while Saving Characteristic Template Line  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ESCMCharactTempltLine Check = (ESCMCharactTempltLine) event.getTargetInstance();
      // restric to delete the line if its associated with some product
      OBQuery<Product> template = OBDal.getInstance().createQuery(Product.class,
          "as e where e.escmCharacTemplate.id=:tempId and e.client.id=:clientID ");
      template.setNamedParameter("tempId", Check.getEscmCharactTemplt().getId());
      template.setNamedParameter("clientID", Check.getClient().getId());
      if (template.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Charateristic_delete"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting Characteristic Template Line  : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting Characteristic Template Line  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
