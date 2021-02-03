/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinEncControl;

/**
 * @author Priyanka Ranjan on 10/10/2017
 */
// Event For Maintain Encumbrance Control window
public class MaintainEncumbranceControlEvent extends EntityPersistenceEventObserver {
  private static final Logger LOG = LoggerFactory.getLogger(MaintainEncumbranceControlEvent.class);
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinEncControl.ENTITY_NAME) };

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
      EfinEncControl enccontr = (EfinEncControl) event.getTargetInstance();
      final Property prType = entities[0].getProperty(EfinEncControl.PROPERTY_TYPELIST);

      // CHECK PR-TYPE IS ASSOCIATED WHEN ENCUMBRANCE TYPE IS "PRE"[PURCHASE REQUISITION]
      if ("PRE".equals(enccontr.getEncumbranceType())
          && StringUtils.isEmpty(enccontr.getTypeList())) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_PRTYPE_MANDATORY"));
      }

      // check unique constraint based on Encumbrance type , pr type and clientid
      if ("PRE".equals(enccontr.getEncumbranceType())) {
        OBQuery<EfinEncControl> efincontrolList = OBDal.getInstance().createQuery(
            EfinEncControl.class,
            "as e where e.encumbranceType=:type and e.typeList=:prtype and e.id!=:id");
        efincontrolList.setFilterOnActive(false);
        efincontrolList.setNamedParameter("type", enccontr.getEncumbranceType());
        efincontrolList.setNamedParameter("prtype", enccontr.getTypeList());
        efincontrolList.setNamedParameter("id", enccontr.getId());
        List<EfinEncControl> list = efincontrolList.list();
        if (list != null && list.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_enccontr_uni"));
        }
      } else {
        OBQuery<EfinEncControl> efincontrolList = OBDal.getInstance()
            .createQuery(EfinEncControl.class, "as e where e.encumbranceType=:type and e.id!=:id");
        efincontrolList.setFilterOnActive(false);
        efincontrolList.setNamedParameter("type", enccontr.getEncumbranceType());
        efincontrolList.setNamedParameter("id", enccontr.getId());
        List<EfinEncControl> list = efincontrolList.list();
        if (list != null && list.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_enccontr_uni"));
        }
      }

      /* if (!event.getCurrentState(isenable).equals(event.getPreviousState(isenable))) { */
      if (enccontr.isActive()) {
        if (!enccontr.isEncumbranceMethodAuto() && !enccontr.isEncumbranceMethodManual()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Auto_Manual_check"));
        }
      }

      if ((!"PRE".equals(enccontr.getEncumbranceType())) && enccontr.getTypeList() != null) {
        event.setCurrentState(prType, null);
      }

    } catch (OBException e) {
      LOG.error(" Exception while updating Maintain Encumbrance Control: " + e, e);
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
      EfinEncControl enccontr = (EfinEncControl) event.getTargetInstance();
      final Property prType = entities[0].getProperty(EfinEncControl.PROPERTY_TYPELIST);

      if (enccontr.isActive()) {
        if (!enccontr.isEncumbranceMethodAuto() && !enccontr.isEncumbranceMethodManual()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Auto_Manual_check"));
        }
      }

      // CHECK PR-TYPE IS ASSOCIATED WHEN ENCUMBRANCE TYPE IS "PRE"[PURCHASE REQUISITION]
      if ("PRE".equals(enccontr.getEncumbranceType())
          && StringUtils.isEmpty(enccontr.getTypeList())) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_PRTYPE_MANDATORY"));
      }

      // check unique constraint based on Encumbrance type , pr type and clientid
      if ("PRE".equals(enccontr.getEncumbranceType())) {
        OBQuery<EfinEncControl> efincontrolList = OBDal.getInstance().createQuery(
            EfinEncControl.class, "as e where  e.encumbranceType=:type and e.typeList=:prtype");
        efincontrolList.setFilterOnActive(false);
        efincontrolList.setNamedParameter("type", enccontr.getEncumbranceType());
        efincontrolList.setNamedParameter("prtype", enccontr.getTypeList());
        List<EfinEncControl> list = efincontrolList.list();
        if (list != null && list.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_enccontr_uni"));
        }
      } else {
        OBQuery<EfinEncControl> efincontrolList = OBDal.getInstance()
            .createQuery(EfinEncControl.class, "as e where e.encumbranceType=:type");
        efincontrolList.setFilterOnActive(false);
        efincontrolList.setNamedParameter("type", enccontr.getEncumbranceType());
        List<EfinEncControl> list = efincontrolList.list();
        if (list != null && list.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_enccontr_uni"));
        }
      }

      if ((!"PRE".equals(enccontr.getEncumbranceType())) && enccontr.getTypeList() != null) {
        event.setCurrentState(prType, null);
      }

    } catch (OBException e) {
      LOG.error(" Exception while creating Maintain Encumbrance Control: " + e, e);
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
      EfinEncControl enccontr = (EfinEncControl) event.getTargetInstance();
      // Should not allow to delete the Encumbrance Type which is used in Encumbrance.
      OBQuery<EfinBudgetManencum> encum = OBDal.getInstance().createQuery(EfinBudgetManencum.class,
          "as e where e.encumType = '" + enccontr.getEncumbranceType() + "' and e.client.id='"
              + enccontr.getClient().getId() + "'");
      if (encum.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_cannot_delete_encumtype"));
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while Delete Maintain Encumbrance Control: " + e, e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
