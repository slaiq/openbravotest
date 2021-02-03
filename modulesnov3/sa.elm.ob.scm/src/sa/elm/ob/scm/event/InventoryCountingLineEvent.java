package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
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
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;

public class InventoryCountingLineEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InventoryCountLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());
  final Property sotrageBin = entities[0].getProperty(InventoryCountLine.PROPERTY_STORAGEBIN);
  final Property product = entities[0].getProperty(InventoryCountLine.PROPERTY_PRODUCT);
  final Property uom = entities[0].getProperty(InventoryCountLine.PROPERTY_UOM);
  final Property attributeset = entities[0]
      .getProperty(InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE);

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      InventoryCountLine line = (InventoryCountLine) event.getTargetInstance();

      if (line.getPhysInventory().getEscmStatus() != null
          && line.getPhysInventory().getEscmStatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Inventorycounting_delete"));
      }
      //
      // if (line.getPhysInventory().getEscmCounttype() != null
      // && line.getPhysInventory().getEscmCounttype().equals("TC")
      // && line.getBookQuantity().compareTo(BigDecimal.ZERO) > 0) {
      // // throw new OBException(OBMessageUtils.messageBD("Escm_NoDelete_Stock"));
      // }
    } catch (OBException e) {
      log.error(" Exception while deleting Inventoryline counting: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while deleting Inventoryline counting: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      InventoryCountLine currentLine = (InventoryCountLine) event.getTargetInstance();
      if (currentLine.getQuantityCount().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_negative_countedQty"));
      }
      /*
       * if (currentLine.getPhysInventory().getEscmCounttype().equals("TC")) { throw new
       * OBException(OBMessageUtils.messageBD("")); }
       */
      if (currentLine.getPhysInventory().getEscmCounttype() != null
          && currentLine.getPhysInventory().getEscmCounttype().equals("PC")) {
        if (!event.getCurrentState(sotrageBin).equals(event.getPreviousState(sotrageBin))
            || !event.getCurrentState(product).equals(event.getPreviousState(product))
            || !event.getCurrentState(uom).equals(event.getPreviousState(uom))
            || (event.getPreviousState(attributeset) != null && !event.getCurrentState(attributeset)
                .equals(event.getPreviousState(attributeset)))) {

          OBQuery<InventoryCountLine> line = OBDal.getInstance().createQuery(
              InventoryCountLine.class, "product.id =:productID and storageBin.id=:binID "
                  + "and uOM.id=:uomID and physInventory.id=:invID");
          line.setNamedParameter("productID", currentLine.getProduct().getId());
          line.setNamedParameter("binID", currentLine.getStorageBin().getId());
          line.setNamedParameter("uomID", currentLine.getUOM().getId());
          line.setNamedParameter("invID", currentLine.getPhysInventory().getId());

          if (line.list() != null && line.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Inventoyline_DuplicateProduct"));
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while updating Inventoryline counting:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating Inventoryline counting:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      // String sql = "";
      OBContext.setAdminMode();
      InventoryCountLine currentLine = (InventoryCountLine) event.getTargetInstance();
      if (currentLine.getPhysInventory().getEscmStatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Inventory_Completed"));
      }
      if (currentLine.getQuantityCount().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_negative_countedQty"));
      }
      /*
       * if (currentLine.getAttributeSetValue() == null) { sql =
       * " and attributeSetValue.id is null"; } else { sql = "and attributeSetValue.id='" +
       * currentLine.getAttributeSetValue().getId() + "'"; }
       */
      OBQuery<InventoryCountLine> line = OBDal.getInstance().createQuery(InventoryCountLine.class,
          "product.id =:productID and storageBin.id=:binID "
              + " and uOM.id=:uomID and physInventory.id=:invID");
      line.setNamedParameter("productID", currentLine.getProduct().getId());
      line.setNamedParameter("binID", currentLine.getStorageBin().getId());
      line.setNamedParameter("uomID", currentLine.getUOM().getId());
      line.setNamedParameter("invID", currentLine.getPhysInventory().getId());
      if (line.list() != null && line.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Inventoyline_DuplicateProduct"));
      }
    } catch (OBException e) {
      log.error("Exception while saving Inventoryline counting:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while saving Inventoryline counting:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
