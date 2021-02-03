package sa.elm.ob.scm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * @author Priyanka Ranjan on 18/02/2017
 */

public class ESCMBusinessPartnerEvent extends EntityPersistenceEventObserver {

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
      BusinessPartner Check = (BusinessPartner) event.getTargetInstance();
      final Property defaultBP = entities[0]
          .getProperty(BusinessPartner.PROPERTY_ESCMDEFAULTPARTNER);
      final Property identityexpdate = entities[0]
          .getProperty(BusinessPartner.PROPERTY_ESCMIDENTITYEXPDATE);

      // CR Expiry Date should allow current or future date.
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      if (Check.getEscmCrexpirydate() != null) {
        if (Check.getEscmCrexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CR_ExpiryDate"));
        }
      }
      // check default business partner exists
      if (event.getCurrentState(defaultBP) != null && event.getPreviousState(defaultBP) != null) {
        if (!event.getCurrentState(defaultBP).equals(event.getPreviousState(defaultBP))) {
          if (Check.isEscmDefaultpartner()) {
            OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(
                BusinessPartner.class,
                "as e where e.client.id=:clientId and e.escmDefaultpartner='Y'");
            bpQuery.setNamedParameter("clientId", Check.getClient().getId());
            if (bpQuery.list().size() > 0) {
              throw new OBException(OBMessageUtils.messageBD("em_escm_defaultpartner"));
            }
          }
        }
      }

      if (Check.getEfinZakatexpirydate() != null) {
        if (Check.getEfinZakatexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Zakat_Expiry_Date"));
        }
      }
      if (Check.getEfinIqamaexpirydate() != null) {
        if (Check.getEfinIqamaexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Iqama_Expiry_Date"));
        }
      }
      if (Check.getEfinSagiaexpirydate() != null) {
        if (Check.getEfinSagiaexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Sagia_Expiry_Date"));
        }
      }

      if ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals(""))
          && ((Check.getEfinNationalidnumber() == null || Check.getEfinNationalidnumber()
              .equals("")) /* || (Check.getEscmIdentityexpdate() == null) */)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Bpartner_mandatory"));

      }
      /*
       * if ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals("")) &&
       * (Check.getEfinIdentityname().equals("NID") || Check.getEfinIdentityname() .equals("IQN")))
       * { event.setCurrentState(employee, true); event.setCurrentState(vendor, false);
       * event.setCurrentState(customer, false); } if ((Check.getEfinIdentityname() != null &&
       * !Check.getEfinIdentityname().equals("")) && (Check.getEfinIdentityname().equals("CRN"))) {
       * event.setCurrentState(employee, false); event.setCurrentState(vendor, true);
       * event.setCurrentState(customer, false); } if ((Check.getEfinIdentityname() == null ||
       * Check.getEfinIdentityname().equals(""))) { event.setCurrentState(employee, false);
       * event.setCurrentState(vendor, false); event.setCurrentState(customer, true); }
       */
      /*
       * // National ID is mandatory for Saudi Arabian. if
       * (Check.getEfinNationality().getISOCountryCode().equals("SA") &&
       * ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals("NID")) ||
       * (Check .getEfinNationalidnumber() == null || Check.getEfinNationalidnumber().equals(""))))
       * { throw new OBException(OBMessageUtils.messageBD("ESCM_National_ID_Mandatory")); }
       */
      /*
       * // If Civil Registration/NationalID number have value then Identity Name should not be
       * Blank if ((Check.getEfinNationalidnumber() != null &&
       * !Check.getEfinNationalidnumber().equals("")) && (Check.getEfinIdentityname() == null ||
       * Check.getEfinIdentityname().equals(""))) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_IdentityName")); }
       */

      // Identity Expiry Date should allow current or future date.
      if (event.getCurrentState(identityexpdate) != null
          && event.getPreviousState(identityexpdate) != null) {
        if (!event.getCurrentState(identityexpdate)
            .equals(event.getPreviousState(identityexpdate))) {
          if (Check.getEscmIdentityexpdate() != null) {
            if (Check.getEscmIdentityexpdate().compareTo(todaydate) < 0) {
              throw new OBException(
                  OBMessageUtils.messageBD("ESCM_PastDate_NotAllow_IN_IdentityExpDate"));
            }
          }
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating BusinessPartner: ", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error(" Exception while updating BusinessPartner: ", e);
    } catch (Exception e) {
      log.error(" Exception while updating BusinessPartner: ", e);
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
      BusinessPartner Check = (BusinessPartner) event.getTargetInstance();
      final Property vendor = entities[0].getProperty(BusinessPartner.PROPERTY_VENDOR);
      final Property customer = entities[0].getProperty(BusinessPartner.PROPERTY_CUSTOMER);
      final Property employee = entities[0].getProperty(BusinessPartner.PROPERTY_EMPLOYEE);
      // CR Expiry Date should allow current or future date.
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      if (Check.getEscmCrexpirydate() != null) {
        if (Check.getEscmCrexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CR_ExpiryDate"));
        }
      }
      // check default business partner exists
      if (Check.isEscmDefaultpartner()) {
        OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
            "as e where e.client.id=:clientID and e.escmDefaultpartner='Y'");
        bpQuery.setNamedParameter("clientID", Check.getClient().getId());
        if (bpQuery.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("em_escm_defaultpartner"));
        }
      }

      if (Check.getEfinZakatexpirydate() != null) {
        if (Check.getEfinZakatexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Zakat_Expiry_Date"));
        }
      }
      if (Check.getEfinIqamaexpirydate() != null) {
        if (Check.getEfinIqamaexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Iqama_Expiry_Date"));
        }
      }
      if (Check.getEfinSagiaexpirydate() != null) {
        if (Check.getEfinSagiaexpirydate().compareTo(todaydate) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Sagia_Expiry_Date"));
        }
      }

      if ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals(""))
          && ((Check.getEfinNationalidnumber() == null || Check.getEfinNationalidnumber()
              .equals("")) /* || (Check.getEscmIdentityexpdate() == null) */)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Bpartner_mandatory"));

      }
      if ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals(""))
          && (Check.getEfinIdentityname().equals("NID")
              || Check.getEfinIdentityname().equals("IQN"))) {
        event.setCurrentState(employee, true);
        event.setCurrentState(vendor, false);
        event.setCurrentState(customer, false);
      }
      if ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals(""))
          && (Check.getEfinIdentityname().equals("CRN"))) {
        event.setCurrentState(employee, false);
        event.setCurrentState(vendor, true);
        event.setCurrentState(customer, false);
      }
      /*
       * // National ID is mandatory for Saudi Arabian. if
       * (Check.getEfinNationality().getISOCountryCode().equals("SA") &&
       * ((Check.getEfinIdentityname() != null && !Check.getEfinIdentityname().equals("NID")) ||
       * (Check .getEfinNationalidnumber() == null || Check.getEfinNationalidnumber().equals(""))))
       * { throw new OBException(OBMessageUtils.messageBD("ESCM_National_ID_Mandatory")); }
       */
      /*
       * // If Civil Registration/NationalID number have value then Identity Name should not be
       * Blank if ((Check.getEfinNationalidnumber() != null &&
       * !Check.getEfinNationalidnumber().equals("")) && (Check.getEfinIdentityname() == null ||
       * Check.getEfinIdentityname().equals(""))) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_IdentityName")); }
       */

      // Identity Expiry Date should allow current or future date.
      if (Check.getEscmIdentityexpdate() != null) {
        if (Check.getEscmIdentityexpdate().compareTo(todaydate) < 0) {
          throw new OBException(
              OBMessageUtils.messageBD("ESCM_PastDate_NotAllow_IN_IdentityExpDate"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating BusinessPartner: ", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error(" Exception while creating BusinessPartner: ", e);
    } catch (Exception e) {
      log.error(" Exception while creating BusinessPartner: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
