package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

import sa.elm.ob.utility.util.Utility;

public class InventoryCountingEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InventoryCount.ENTITY_NAME) };

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
      InventoryCount header = (InventoryCount) event.getTargetInstance();

      // end should be greater than start date.
      if (header.getEscmEnddate() != null) {
        if ((header.getEscmEnddate().compareTo(header.getEscmStartdate()) < 0)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_date_validation"));
        }
      }
    } catch (OBException e) {
      log.error("Exception while updating Inventory counting:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating Inventory counting:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      // Long code = Long.valueOf("0001");
      String sequence = "";
      Boolean sequenceexists = false;
      InventoryCount header = (InventoryCount) event.getTargetInstance();
      final Property SpecNo = entities[0].getProperty(InventoryCount.PROPERTY_ESCMSPECNO);
      // final Property DocNo = entities[0].getProperty(InventoryCount.PROPERTY_ESCMDOCNO);
      // end should be greater than start date.
      if (header.getEscmEnddate() != null) {
        if ((header.getEscmEnddate().compareTo(header.getEscmStartdate()) < 0)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_date_validation"));
        }
      }
      /*
       * // generate seq.No DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy"); Date now =
       * new Date(); Date todaydate = dateFormat.parse(dateFormat.format(now)); SimpleDateFormat
       * dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
       * 
       * String hijiridate = convertTohijriDate(dateYearFormat.format(todaydate)); int year =
       * Integer.parseInt(hijiridate.split("-")[2]); log.debug("Year:" + year); // 1439R0001 // find
       * last transaction year. int lastyear = 0; OBQuery<InventoryCount> Inventory =
       * OBDal.getInstance().createQuery( InventoryCount.class, "as e where e.organization.id='" +
       * header.getOrganization().getId() + "' order by e.creationDate desc"); if (Inventory != null
       * && Inventory.list().size() > 0) { Inventory.setMaxResult(1); Date oldyear =
       * Inventory.list().get(0).getCreationDate(); String hijiridatelastdate =
       * convertTohijriDate(dateYearFormat.format(oldyear)); lastyear =
       * Integer.parseInt(hijiridatelastdate.split("-")[2]); } else { lastyear = year; } // compare
       * last year with curent year to generate spec_no again from 0001 for every year.
       * OBQuery<InventoryCount> InventoryNew = OBDal.getInstance().createQuery(
       * InventoryCount.class, "as e where e.organization.id='" + header.getOrganization().getId() +
       * "' and '" + lastyear + "'= '" + year + "' order by e.creationDate desc"); if (InventoryNew
       * != null && InventoryNew.list().size() > 0) { InventoryNew.setMaxResult(1); if
       * (InventoryNew.list().size() > 0) { code = InventoryNew.list().get(0).getEscmDocno(); if
       * (code != null) { newCode = String.valueOf(year).concat("P").concat(String.format("%04d",
       * code + 1)); event.setCurrentState(DocNo, code + 1); } else { newCode =
       * String.valueOf(year).concat("P") .concat(String.format("%04d", Long.valueOf("0001")));
       * event.setCurrentState(DocNo, Long.valueOf("0001")); } } } else { newCode =
       * String.valueOf(year).concat("P").concat(String.format("%04d", code));
       * event.setCurrentState(DocNo, code); } // set new Spec No event.setCurrentState(SpecNo,
       * newCode);
       */
      sequence = Utility.getSpecificationSequence(header.getOrganization().getId(), "IC");
      if (!header.isEscmIsphysicalinventory()) {
        if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
          throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
        } else {
          sequenceexists = Utility.chkTransactionSequence(header.getOrganization().getId(), "IC",
              sequence);
          if (!sequenceexists) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
          }
          event.setCurrentState(SpecNo, sequence);
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating Inventory counting: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating Inventory counting: ", e);
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
      InventoryCount header = (InventoryCount) event.getTargetInstance();

      if (header.getEscmStatus().equals("CO")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Inventorycounting_delete"));
      }
    } catch (OBException e) {
      log.error("Exception while deleting Inventory counting:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting Inventory counting:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  public String convertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery gradeQuery = OBDal.getInstance().getSession()
          .createSQLQuery("select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
              + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (gradeQuery.list().size() > 0) {
        Object row = (Object) gradeQuery.list().get(0);
        hijriDate = (String) row;
        log.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }

}
