package sa.elm.ob.scm.event;

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
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import sa.elm.ob.scm.Escm_custody_transaction;

public class ReceiptLineEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ShipmentInOutLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      ShipmentInOutLine poReceiptLine = (ShipmentInOutLine) event.getTargetInstance();
      ShipmentInOut poReceipt = poReceiptLine.getShipmentReceipt();

      if (poReceipt.getEscmReceivingtype().equals("IRT")
          || poReceipt.getEscmReceivingtype().equals("INR")
          || poReceipt.getEscmReceivingtype().equals("LD")) {
        OBQuery<Escm_custody_transaction> custtran = OBDal.getInstance().createQuery(
            Escm_custody_transaction.class, " as e where e.goodsShipmentLine.id=:receiptLnID ");
        custtran.setNamedParameter("receiptLnID", poReceiptLine.getId());
        if (custtran.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_InoutLineCantDel"));
        }
      }
      if (poReceipt.isEscmIscustodyTransfer() != null && poReceipt.isEscmIscustodyTransfer()
          && poReceipt.getEscmDocstatus().equals("DR")) {
        for (Escm_custody_transaction objTrans : poReceiptLine.getEscmCustodyTransactionList()) {
          OBDal.getInstance().remove(objTrans);
        }
      }
      if (!poReceipt.getEscmReceivingtype().equals("INR")) {
        if (poReceipt != null && !poReceipt.getEscmDocstatus().equals("DR")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
        }
      } else {
        if (poReceipt != null && poReceipt.getEscmDocstatus().equals("CO")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating PO Receipt:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating PO Receipt:", e);
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
      ShipmentInOutLine poReceiptLine = (ShipmentInOutLine) event.getTargetInstance();
      ShipmentInOut poReceipt = poReceiptLine.getShipmentReceipt();
      final Property objUpdatedBy = entities[0].getProperty(Product.PROPERTY_UPDATEDBY);
      if (poReceipt.getEscmReceivingtype().equals("INR")) {
        if (event.getCurrentState(objUpdatedBy) != poReceipt.getCreatedBy()) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Approver_NotUpdate"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while creating shipmentInoutLine : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating shipmentInoutLine : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
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
      ShipmentInOutLine poReceiptLine = (ShipmentInOutLine) event.getTargetInstance();
      ShipmentInOut poReceipt = poReceiptLine.getShipmentReceipt();
      final Property objUpdatedBy = entities[0].getProperty(Product.PROPERTY_UPDATEDBY);
      if (poReceipt.getEscmReceivingtype().equals("INR")) {
        if (event.getCurrentState(objUpdatedBy) != poReceipt.getCreatedBy()) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Approver_NotUpdate"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while creating shipmentInoutLine : " , e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating shipmentInoutLine : " , e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
