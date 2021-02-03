package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

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
import sa.elm.ob.finance.EFINServiceItemConfiguration;

public class ServiceItemMappingEvent extends EntityPersistenceEventObserver {
  /**
   * This event is used to check whether the record combination already exists in Service Item
   * Mapping Window
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINServiceItemConfiguration.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      final EFINServiceItemConfiguration servConfig = (EFINServiceItemConfiguration) event
          .getTargetInstance();

      final Property account = entities[0]
          .getProperty(EFINServiceItemConfiguration.PROPERTY_ACCOUNT);

      final Property serviceitem = entities[0]
          .getProperty(EFINServiceItemConfiguration.PROPERTY_SERVICEITEM);

      final Property applntype = entities[0]
          .getProperty(EFINServiceItemConfiguration.PROPERTY_APPLICATIONTYPE);

      final Property deptauthcode = entities[0]
          .getProperty(EFINServiceItemConfiguration.PROPERTY_DEPTAUTHCODE);

      final Property deptbenefitcode = entities[0]
          .getProperty(EFINServiceItemConfiguration.PROPERTY_DEPTBENEFITCODE);

      if ((event.getCurrentState(account) != event.getPreviousState(account))
          || (event.getCurrentState(serviceitem) != event.getPreviousState(serviceitem))
          || (event.getCurrentState(applntype) != event.getPreviousState(applntype))
          || (event.getCurrentState(deptauthcode) != event.getPreviousState(deptauthcode))
          || (event.getCurrentState(deptbenefitcode) != event.getPreviousState(deptbenefitcode))) {
        StringBuilder whereclause = new StringBuilder();
        whereclause.append(" account.id = :accountID " + "  and serviceItem.id = :serviceItem"
            + "  and applicationType.id = :applnType");

        if ((servConfig.getDeptAuthCode() == null) && (servConfig.getDeptBenefitCode() == null)) {

          whereclause.append(" and deptBenefitCode is null" + " and deptAuthCode is null ");
        } else if (servConfig.getDeptAuthCode() == null) {

          whereclause
              .append(" and deptBenefitCode.id = :deptBenefitCode" + " and deptAuthCode is null ");
        } else if (servConfig.getDeptBenefitCode() == null) {

          whereclause
              .append(" and deptAuthCode.id = :deptAuthCode" + "  and deptBenefitCode is null ");
        } else {

          whereclause.append(" and deptAuthCode.id = :deptAuthCode"
              + " and deptBenefitCode.id = :deptBenefitCode");
        }
        OBQuery<EFINServiceItemConfiguration> servItemConfig = OBDal.getInstance()
            .createQuery(EFINServiceItemConfiguration.class, whereclause.toString());
        servItemConfig.setNamedParameter("accountID", servConfig.getAccount().getId());
        servItemConfig.setNamedParameter("serviceItem", servConfig.getServiceItem().getId());
        servItemConfig.setNamedParameter("applnType", servConfig.getApplicationType().getId());
        if (servConfig.getDeptAuthCode() != null) {
          servItemConfig.setNamedParameter("deptAuthCode", servConfig.getDeptAuthCode().getId());
        }
        if (servConfig.getDeptBenefitCode() != null) {
          servItemConfig.setNamedParameter("deptBenefitCode",
              servConfig.getDeptBenefitCode().getId());
        }
        List<EFINServiceItemConfiguration> servItemConfigList = servItemConfig.list();
        if (servItemConfigList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_ServiceItemMapping"));
        }
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
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
      final EFINServiceItemConfiguration servConfig = (EFINServiceItemConfiguration) event
          .getTargetInstance();

      StringBuilder whereclause = new StringBuilder();
      whereclause.append(" account.id = :accountID " + "  and serviceItem.id = :serviceItem"
          + "  and applicationType.id = :applnType");

      if ((servConfig.getDeptAuthCode() == null) && (servConfig.getDeptBenefitCode() == null)) {

        whereclause.append(" and deptBenefitCode is null" + " and deptAuthCode is null ");
      } else if (servConfig.getDeptAuthCode() == null) {

        whereclause
            .append(" and deptBenefitCode.id = :deptBenefitCode" + " and deptAuthCode is null ");
      } else if (servConfig.getDeptBenefitCode() == null) {

        whereclause
            .append(" and deptAuthCode.id = :deptAuthCode" + "  and deptBenefitCode is null ");
      } else {

        whereclause.append(
            " and deptAuthCode.id = :deptAuthCode" + " and deptBenefitCode.id = :deptBenefitCode");
      }
      OBQuery<EFINServiceItemConfiguration> servItemConfig = OBDal.getInstance()
          .createQuery(EFINServiceItemConfiguration.class, whereclause.toString());
      servItemConfig.setNamedParameter("accountID", servConfig.getAccount().getId());
      servItemConfig.setNamedParameter("serviceItem", servConfig.getServiceItem().getId());
      servItemConfig.setNamedParameter("applnType", servConfig.getApplicationType().getId());
      if (servConfig.getDeptAuthCode() != null) {
        servItemConfig.setNamedParameter("deptAuthCode", servConfig.getDeptAuthCode().getId());
      }
      if (servConfig.getDeptBenefitCode() != null) {
        servItemConfig.setNamedParameter("deptBenefitCode",
            servConfig.getDeptBenefitCode().getId());
      }
      List<EFINServiceItemConfiguration> servItemConfigList = servItemConfig.list();
      if (servItemConfigList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_ServiceItemMapping"));
      }

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

  }
}
