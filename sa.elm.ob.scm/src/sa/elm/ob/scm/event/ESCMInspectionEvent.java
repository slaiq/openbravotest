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
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMInspection;

/**
 * @author Priyanka Ranjan on 22/02/2017
 */
public class ESCMInspectionEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMInspection.ENTITY_NAME) };

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
      ESCMInspection Check = (ESCMInspection) event.getTargetInstance();
      // Inspection Date should not allow future date
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      if (Check.getInspectionDate() != null) {
        Date inspectionDate = dateFormat.parse(dateFormat.format(Check.getInspectionDate()));
        if (inspectionDate != null) {
          if (inspectionDate.compareTo(todaydate) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_InspectionDate"));
          }
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating Inspection tab in Goods Receipt: ", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error(" Exception while updating Inspection tab in Goods Receipt: ", e);
    } catch (Exception e) {
      log.error(" Exception while updating Inspection tab in Goods Receipt: ", e);
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
      ESCMInspection Check = (ESCMInspection) event.getTargetInstance();
      // Inspection Date should not allow future date
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));
      if (Check.getInspectionDate() != null) {
        Date inspectionDate = dateFormat.parse(dateFormat.format(Check.getInspectionDate()));
        if (inspectionDate != null) {
          if (inspectionDate.compareTo(todaydate) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_InspectionDate"));
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating Inspection tab in Goods Receipt: ", e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      log.error(" Exception while creating Inspection tab in Goods Receipt: ", e);
    } catch (Exception e) {
      log.error(" Exception while creating Inspection tab in Goods Receipt: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
