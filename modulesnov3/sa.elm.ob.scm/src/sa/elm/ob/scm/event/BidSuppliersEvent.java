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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.utility.Utils;

public class BidSuppliersEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in Bid Management - Suppliers tab
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmbidsuppliers.ENTITY_NAME) };

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
      Escmbidsuppliers bidsuppliers = (Escmbidsuppliers) event.getTargetInstance();
      // final Property supno = entities[0].getProperty(Escmbidsuppliers.PROPERTY_SUPPLIERNUMBER);
      // final Property supname = entities[0].getProperty(Escmbidsuppliers.PROPERTY_SUPPLIER);
      final Property phone = entities[0].getProperty(Escmbidsuppliers.PROPERTY_SUPPLIERPHONE);
      final Property fax = entities[0].getProperty(Escmbidsuppliers.PROPERTY_SUPPLIERFAX);
      boolean isallow = false;

      /*
       * if (bidsuppliers.getEscmBidmgmt().getBidappstatus().equals("CO")) { if
       * (event.getCurrentState(supno) != null &&
       * !event.getCurrentState(supno).equals(event.getPreviousState(supno))) { if
       * (bidsuppliers.getSuppliernumber() != null && bidsuppliers.getSupplier() != null) {
       * BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class,
       * bidsuppliers.getSuppliernumber().getId()); if
       * (!partner.getName().equals(event.getPreviousState(supname))) { event.setCurrentState(supno,
       * null); event.setCurrentState(supname, event.getPreviousState(supname)); } } } }
       */
      // phone number format (Numaric values,+,-)
      if (bidsuppliers.getSupplierphone() != null && !bidsuppliers.getSupplierphone().equals("")) {
        if (!event.getCurrentState(phone).equals(event.getPreviousState(phone))) {
          isallow = Utils.phoneNumberFormat(bidsuppliers.getSupplierphone());
          if (!isallow) {
            // throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
          }
        }
      }
      // fax number format (Numaric values,+,-,(,))
      if (bidsuppliers.getSupplierfax() != null && !bidsuppliers.getSupplierfax().equals("")) {
        if (!event.getCurrentState(fax).equals(event.getPreviousState(fax))) {
          isallow = Utils.faxNumberFormat(bidsuppliers.getSupplierfax());
          if (!isallow) {
            // throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
          }
        }
      }

    } catch (OBException e) {
      log.debug("exception while updating BidSuppliersEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while updating BidSuppliersEvent" + e);
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
      Escmbidsuppliers bidsuppliers = (Escmbidsuppliers) event.getTargetInstance();
      boolean isallow = false;
      if (bidsuppliers.getEscmBidmgmt().getBidappstatus().equals("ESCM_IP")
          || bidsuppliers.getEscmBidmgmt().getBidappstatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Suppliers_NotAdd"));
      }
      // phone number format (Numaric values,+,-)
      if (bidsuppliers.getSupplierphone() != null && !bidsuppliers.getSupplierphone().equals("")) {
        isallow = Utils.phoneNumberFormat(bidsuppliers.getSupplierphone());
        if (!isallow) {
          // throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
        }
      }

      // fax number format (Numaric values,+,-,(,))
      if (bidsuppliers.getSupplierfax() != null && !bidsuppliers.getSupplierfax().equals("")) {
        isallow = Utils.faxNumberFormat(bidsuppliers.getSupplierfax());
        if (!isallow) {
          // throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
        }
      }
    }

    catch (OBException e) {
      log.debug("exception while creating BidSuppliersEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating BidSuppliersEvent" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
