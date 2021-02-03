package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.Escm_Prsuppliers;

public class PrSuppliersEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escm_Prsuppliers.ENTITY_NAME) };

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
      Escm_Prsuppliers prSupplier = (Escm_Prsuppliers) event.getTargetInstance();

      // Throw error if Location / Address is empty for a supplier
      if (prSupplier.getLocationAddress() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_SupplierLocationEmpty"));
      }

      // Escm_Prsuppliers prSupplier = (Escm_Prsuppliers) event.getTargetInstance();
      // final Property supno = entities[0].getProperty(Escm_Prsuppliers.PROPERTY_SUPPLIERNUMBER);
      // final Property supname = entities[0].getProperty(Escm_Prsuppliers.PROPERTY_SUPPLIER);
      // final Property phone = entities[0].getProperty(Escm_Prsuppliers.PROPERTY_PHONE);
      // final Property fax = entities[0].getProperty(Escm_Prsuppliers.PROPERTY_FAX);
      // boolean isallow = false;

      // if (prSupplier.getPhone() != null && !prSupplier.getPhone().equals("")) {
      // if (!event.getCurrentState(phone).equals(event.getPreviousState(phone))) {
      // isallow = Utility.chkPhonenoisSaudiFormat(prSupplier.getPhone());
      // if (!isallow) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
      // }
      // }
      // }
      // fax number format (Numaric values,+,-,(,))
      // if (prSupplier.getFax() != null && !prSupplier.getFax().equals("")) {
      // if (!event.getCurrentState(fax).equals(event.getPreviousState(fax))) {
      // isallow = Utils.faxNumberFormat(prSupplier.getFax());
      // if (!isallow) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
      // }
      // }
      // }

    } catch (OBException e) {
      log.error("exception while updating prSupplierEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating prSupplierEvent", e);
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
      Escm_Prsuppliers prSupplier = (Escm_Prsuppliers) event.getTargetInstance();
      // boolean isallow = false;
      // if (prSupplier.getRequisition().getDocumentStatus().equals("ESCM_IP")
      // || prSupplier.getRequisition().getDocumentStatus().equals("CO")
      // && !(prSupplier.getRequisition().getEscmProcesstype().equals("DP"))) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_Suppliers_NotAdd"));
      // }

      // Throw error if Location / Address is empty for a supplier
      if (prSupplier.getLocationAddress() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_SupplierLocationEmpty"));
      }

      // phone number format (Numeric values,+,-)
      // if (prSupplier.getPhone() != null && !prSupplier.getPhone().equals("")) {
      // isallow = Utility.chkPhonenoisSaudiFormat(prSupplier.getPhone());
      // if (!isallow) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
      // }
      // }
      //
      // // fax number format (Numeric values,+,-,(,))
      // if (prSupplier.getFax() != null && !prSupplier.getFax().equals("")) {
      // isallow = Utils.faxNumberFormat(prSupplier.getFax());
      // if (!isallow) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
      // }
      // }
    }

    catch (OBException e) {
      log.error("exception while creating prSupplierEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating prSupplierEvent", e);
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
      Escm_Prsuppliers prSupplier = (Escm_Prsuppliers) event.getTargetInstance();
      String reqId = prSupplier.getRequisition().getId();
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, reqId);

      // after submit can not delete the records
      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_InProgress"));
      }
      // after Approved can not delete the records
      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Approved"));
      }
      // after Cancel can not delete the records
      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_CA")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Requisition_Cancel"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting prSupplierEvent  : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting prSupplierEvent  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
