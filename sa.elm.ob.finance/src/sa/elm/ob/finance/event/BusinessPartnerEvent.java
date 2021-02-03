package sa.elm.ob.finance.event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BankAccount;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.geography.Country;

import sa.elm.ob.utility.Utils;

/**
 * @author Priyanka Ranjan on 29/07/2016
 */

public class BusinessPartnerEvent extends EntityPersistenceEventObserver {

  private static final String DEFAULT_COUNTRY_ISO_CODE = "SA";

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(BusinessPartner.ENTITY_NAME) };

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
      final Property isHrSupplier = entities[0]
          .getProperty(BusinessPartner.PROPERTY_EFINISHRSUPPLIER);
      BusinessPartner Check = (BusinessPartner) event.getTargetInstance();
      String NationalIdNumber = Check.getEfinNationalidnumber() == null ? ""
          : Check.getEfinNationalidnumber();
      String IqamaNumber = Check.getEfinIqamano() == null ? "" : Check.getEfinIqamano();
      if ((Check.getEfinIdentityname() != null && Check.getEfinIdentityname().equals("NID"))
          && (NationalIdNumber != null && !NationalIdNumber.equals(""))) {
      }

      if (!IqamaNumber.equals("")) {
        boolean checkIQAMA = Utils.isIqamaNumber(IqamaNumber);
        if (checkIQAMA == false) {

          throw new OBException(
              OBMessageUtils.messageBD("Efin_BusinessPartner_IQAMANum_Validation"));
        }
      }

      if (Check.isVendor() && Check.isEfinBlacklist()) {
        if ("".equals(Check.getEfinReason()) || Check.getEfinReason() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BusinessPartner_VendorTab_Error"));
        }
      }
      final Property efinNationalidnumber = entities[0]
          .getProperty(BusinessPartner.PROPERTY_EFINNATIONALIDNUMBER);// getting current entered
                                                                      // value
      log.debug("prev:" + event.getPreviousState(efinNationalidnumber));
      log.debug("curr:" + event.getCurrentState(efinNationalidnumber));
      if (event.getCurrentState(efinNationalidnumber) != null
          && (!event.getCurrentState(efinNationalidnumber)
              .equals(event.getPreviousState(efinNationalidnumber)))) {
        OBQuery<BusinessPartner> businesspartnernational = OBDal.getInstance().createQuery(
            BusinessPartner.class,
            "as e where e.efinNationalidnumber='" + Check.getEfinNationalidnumber()
                + "' and e.client.id='" + Check.getClient().getId() + "' ");
        if (businesspartnernational.list().size() > 0) {
          throw new OBException(
              OBMessageUtils.messageBD("Efin_BusinessPartner_civilReg_nationalid_Exists")
                  .replace("@", Check.getEfinNationalidnumber() + ","));
        }
      }

      // handle iban duplication incase they change hrsupplier flag
      if (event.getCurrentState(isHrSupplier) != event.getPreviousState(isHrSupplier)
          && !Check.isEfinIshrsupplier()) {

        List<String> ibanNoList = Check.getBusinessPartnerBankAccountList().stream()
            .filter(a -> a.getBankFormat().equals("IBAN")).map(a -> a.getIBAN())
            .collect(Collectors.toList());

        if (ibanNoList.size() > 0) {
          StringBuilder whereClauseBuilder = new StringBuilder();
          whereClauseBuilder
              .append(" as e where e.iBAN in (:ibanList) and e.businessPartner.id !=:id");

          OBQuery<BankAccount> bankAccount = OBDal.getInstance().createQuery(BankAccount.class,
              whereClauseBuilder.toString());
          bankAccount.setFilterOnActive(false);
          bankAccount.setFilterOnReadableClients(true);
          bankAccount.setFilterOnReadableOrganization(true);

          bankAccount.setNamedParameter("ibanList", ibanNoList);
          bankAccount.setNamedParameter("id", Check.getId());

          if (bankAccount.list() != null && bankAccount.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_ibanduplicateexist"));
          }
        }

      }
    } catch (OBException e) {
      log.error(" Exception while updating BusinessPartner: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("null")
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      BusinessPartner businessPartner = (BusinessPartner) event.getTargetInstance();
      OBQuery<Sequence> seqlist = OBDal.getInstance().createQuery(Sequence.class,
          " as e where e.name='DocumentNo_C_BPartner'");
      Sequence gensequence = seqlist.list().get(0);
      gensequence.setNextAssignedNumber(gensequence.getNextAssignedNumber() + 1);
      OBDal.getInstance().save(gensequence);

      String IqamaNumber = businessPartner.getEfinIqamano() == null ? ""
          : businessPartner.getEfinIqamano();

      if (!IqamaNumber.equals("")) {
        boolean checkIQAMA = Utils.isIqamaNumber(IqamaNumber);
        if (checkIQAMA == false) {
          throw new OBException(
              OBMessageUtils.messageBD("Efin_BusinessPartner_IQAMANum_Validation"));
        }
      }

      if (businessPartner.isVendor() && businessPartner.isEfinBlacklist()) {
        if ("".equals(businessPartner.getEfinReason()) || businessPartner.getEfinReason() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BusinessPartner_VendorTab_Error"));
        }
      }
      OBQuery<BusinessPartner> businesspartnernational = OBDal.getInstance().createQuery(
          BusinessPartner.class,
          "as e where e.efinNationalidnumber='" + businessPartner.getEfinNationalidnumber()
              + "' and e.client.id='" + businessPartner.getClient().getId() + "'");
      if (businesspartnernational.list().size() > 0) {
        /*
         * throw new OBException(OBMessageUtils.messageBD(
         * "Efin_BusinessPartner_civilReg_nationalid_Exists").replace("@",
         * Check.getEfinNationalidnumber() + ","));
         */
      }
      if (!businessPartner.isEscmImported()) {
        addDefaultAddress(event);
      }
    } catch (OBException e) {
      log.error(" Exception while creating BusinessPartner: " + e);
      throw new OBException(e.getMessage());
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
      BusinessPartner businessPartner = (BusinessPartner) event.getTargetInstance();
      if (businessPartner.getInvoiceList() != null && businessPartner.getInvoiceList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Bp_Used"));
      }

    } catch (OBException e) {
      log.debug("exception while deleting businesspartner" + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  private void addDefaultAddress(EntityNewEvent event) {
    BusinessPartner businessPartner = (BusinessPartner) event.getTargetInstance();
    createAddresses(businessPartner);
  }

  private void createAddresses(BusinessPartner businessPartner) {
    Location location = new Location();
    location.setShipToAddress(true);
    location.setInvoiceToAddress(true);
    location.setActive(true);
    location.setName("default Address");
    createLocationAddress(location);
    businessPartner.getBusinessPartnerLocationList().add(location);
    location.setBusinessPartner(businessPartner);
  }

  private void createLocationAddress(Location location) {
    org.openbravo.model.common.geography.Location locationAddress = new org.openbravo.model.common.geography.Location();
    locationAddress.setAddressLine1("Default Address");
    locationAddress.setCountry(getDefaultCountry());
    List<Location> businessPartnerLocationList = new ArrayList<>();
    businessPartnerLocationList.add(location);
    locationAddress.setBusinessPartnerLocationList(businessPartnerLocationList);
    location.setLocationAddress(locationAddress);
    OBDal.getInstance().save(locationAddress);
  }

  private Country getDefaultCountry() {

    OBQuery<Country> query = OBDal.getInstance().createQuery(Country.class,
        "iSOCountryCode= :iSOCountryCode");
    query.setNamedParameter("iSOCountryCode", DEFAULT_COUNTRY_ISO_CODE);
    return query.uniqueResult();
  }
}
