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
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.scm.event.dao.RFPSalesVoucherEventDAO;
import sa.elm.ob.utility.Utils;
import sa.elm.ob.utility.util.Utility;

public class RFPSalesVoucherEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmsalesvoucher.ENTITY_NAME) };

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
      Escmsalesvoucher salesVoucher = (Escmsalesvoucher) event.getTargetInstance();

      final Entity voucher = ModelProvider.getInstance().getEntity(Escmsalesvoucher.ENTITY_NAME);
      final Property docStatus = voucher.getProperty(Escmsalesvoucher.PROPERTY_DOCUMENTSTATUS);
      boolean isallow = false;

      event.setCurrentState(docStatus, "DR");

      // phone number format (Numaric values,+,-)
      if (salesVoucher.getSupplierPhone() != null && !salesVoucher.getSupplierPhone().equals("")) {
        isallow = Utils.phoneNumberFormat(salesVoucher.getSupplierPhone());
        if (!isallow) {
          // throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
        }
      }
      // fax number format (Numaric values,+,-,(,))
      if (salesVoucher.getFax() != null && !salesVoucher.getFax().equals("")) {
        isallow = Utils.faxNumberFormat(salesVoucher.getFax());
        if (!isallow) {
          // throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
        }
      }

      // If RFP price is zero do not show the Payment Information Tab
      if (salesVoucher.getRFPPriceSAR() != null
          && salesVoucher.getRFPPriceSAR().doubleValue() > 0) {
        if (null == salesVoucher.getPaymenttype()
            || salesVoucher.getPaymenttype().trim().length() == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_Type_Mandatory"));
        }

        if (null == salesVoucher.getPaymentDocno()
            || salesVoucher.getPaymenttype().trim().length() == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_DocNo_Mandatory"));
        }

        if (null == salesVoucher.getDocumentdateh()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_DocDateH_Mandatory"));
        }

        if (null == salesVoucher.getAmountsar()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_Amount_Mandatory"));
        }

      }

      // Amount should not greater than RFP Price and should not accept the zero value
      if (salesVoucher.getRFPPriceSAR() != null) {
        if (null != salesVoucher.getAmountsar()) {
          if (salesVoucher.getAmountsar().compareTo(salesVoucher.getRFPPriceSAR()) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Amount_CannotlessThan_RFPPrice"));
          } else if (salesVoucher.getAmountsar().compareTo(salesVoucher.getRFPPriceSAR()) > 0) {
            throw new OBException(
                OBMessageUtils.messageBD("ESCM_Amount_CannotGreaterThan_RFPPrice"));
          }

        }
      }
      if (salesVoucher.getMobile() != null && !salesVoucher.getMobile().equals("")) {
        if (!Utility.chkPhonenoisSaudiFormat(salesVoucher.getMobile())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_MobileNumberFormat"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating RFP sales Voucher: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating RFP sales Voucher: ", e);
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
      Escmsalesvoucher salesVoucher = (Escmsalesvoucher) event.getTargetInstance();
      final Property phone = entities[0].getProperty(Escmsalesvoucher.PROPERTY_SUPPLIERPHONE);
      final Property fax = entities[0].getProperty(Escmsalesvoucher.PROPERTY_FAX);
      // final Property supno = entities[0].getProperty(Escmsalesvoucher.PROPERTY_SUPPLIERNUMBER);
      // final Property supname = entities[0].getProperty(Escmsalesvoucher.PROPERTY_SUPPLIER);
      final Property supnumber = entities[0].getProperty(Escmsalesvoucher.PROPERTY_SUPPLIERNUMBER);
      final Property mobile = entities[0].getProperty(Escmsalesvoucher.PROPERTY_MOBILE);

      boolean isallow = false;

      /*
       * if (salesVoucher.getDocumentStatus().equals("CO")) { if (event.getCurrentState(supno) !=
       * null && !event.getCurrentState(supno).equals(event.getPreviousState(supno))) { if
       * (salesVoucher.getSupplierNumber() != null && salesVoucher.getSupplier() != null) {
       * BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class,
       * salesVoucher.getSupplierNumber().getId()); if
       * (!partner.getName().equals(event.getPreviousState(supname))) { event.setCurrentState(supno,
       * null); event.setCurrentState(supname, event.getPreviousState(supname)); } } } }
       */

      // phone number format (Numaric values,+,-)
      if (salesVoucher.getSupplierPhone() != null && !salesVoucher.getSupplierPhone().equals("")) {
        if (!event.getCurrentState(phone).equals(event.getPreviousState(phone))) {
          isallow = Utils.phoneNumberFormat(salesVoucher.getSupplierPhone());
          if (!isallow) {
            // throw new OBException(OBMessageUtils.messageBD("ESCM_PhoneNumberFormat"));
          }
        }
      }
      // fax number format (Numaric values,+,-,(,))
      if (salesVoucher.getFax() != null && !salesVoucher.getFax().equals("")) {
        if (!event.getCurrentState(fax).equals(event.getPreviousState(fax))) {
          isallow = Utils.faxNumberFormat(salesVoucher.getFax());
          if (!isallow) {
            // throw new OBException(OBMessageUtils.messageBD("ESCM_FaxNumberFormat"));
          }
        }
      }
      // if bid is used in proposal, should not allow to change.
      if (event.getPreviousState(supnumber) != null) {
        BusinessPartner bpartner = (BusinessPartner) event.getPreviousState(supnumber);
        if (!event.getCurrentState(supnumber).equals(event.getPreviousState(supnumber))) {
          isallow = RFPSalesVoucherEventDAO.checkRFPExists(salesVoucher, bpartner.getId());
          if (isallow) {
            throw new OBException(OBMessageUtils.messageBD("Escm_RFP_Used_Proposal"));
          }
        }
      }

      // If RFP price is zero do not show the Payment Information Tab
      if (salesVoucher.getRFPPriceSAR() != null
          && salesVoucher.getRFPPriceSAR().doubleValue() > 0) {
        if (null == salesVoucher.getPaymenttype()
            || salesVoucher.getPaymenttype().trim().length() == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_Type_Mandatory"));
        }

        if (null == salesVoucher.getPaymentDocno()
            || salesVoucher.getPaymenttype().trim().length() == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_DocNo_Mandatory"));
        }

        if (null == salesVoucher.getDocumentdateh()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_DocDateH_Mandatory"));
        }

        if (null == salesVoucher.getAmountsar()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Payment_Amount_Mandatory"));
        }

      }
      // Amount should not greater than RFP Price and should not accept the zero value
      if (salesVoucher.getRFPPriceSAR() != null && salesVoucher.getAmountsar() != null
          && salesVoucher.getRFPPriceSAR() != null) {
        if (salesVoucher.getAmountsar().compareTo(salesVoucher.getRFPPriceSAR()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Amount_CannotlessThan_RFPPrice"));
        } else if (salesVoucher.getAmountsar().compareTo(salesVoucher.getRFPPriceSAR()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Amount_CannotGreaterThan_RFPPrice"));
        }
      }
      if (event.getCurrentState(mobile) != null
          && !event.getCurrentState(mobile).equals(event.getPreviousState(mobile))) {

        if (!Utility.chkPhonenoisSaudiFormat(salesVoucher.getMobile())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_MobileNumberFormat"));
        }

      }

    } catch (OBException e) {
      log.error("exception while updating RFP sales Voucher", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating RFP sales Voucher", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
