package sa.elm.ob.finance.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.openbravo.model.marketing.Campaign;

import sa.elm.ob.finance.dao.GLDimensionEventDAO;

public class GeneralLedgerDimension extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(AcctSchemaElement.ENTITY_NAME) };

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
      AcctSchemaElement dimension = (AcctSchemaElement) event.getTargetInstance();

      if (dimension.getType().equals("PJ")) {
        if (dimension.getProject() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_proj_man"));
        }
      }
      if (dimension.getType().equals("U1")) {
        if (dimension.getEfinUser1() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_fut1_man"));
        }
      }
      if (dimension.getType().equals("U2")) {
        if (dimension.getEfinUser2() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_fut2_man"));
        }
      }
      if (dimension.getType().equals("AY")) {
        if (dimension.getActivity() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_funclas_man"));
        }
      }
      if (dimension.getType().equals("BP")) {
        if (dimension.getBusinessPartner() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_bp_man"));
        }
      }
      if (dimension.getType().equals("OO")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }
      if (dimension.getType().equals("SR")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }
      if (dimension.getType().equals("AC")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }
      if (dimension.getType().equals("MC")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while saving dimension in general ledger: " + e);
      throw new OBException(e.getMessage());
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
      AcctSchemaElement dimension = (AcctSchemaElement) event.getTargetInstance();
      Map<String, String> preferenceMap = new ConcurrentHashMap<String, String>();

      if (dimension.getType().equals("PJ")) {
        if (dimension.getProject() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_proj_man"));
        }
      }
      if (dimension.getType().equals("U1")) {
        if (dimension.getEfinUser1() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_fut1_man"));
        }
      }
      if (dimension.getType().equals("U2")) {
        if (dimension.getEfinUser2() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_fut2_man"));
        }
      }
      if (dimension.getType().equals("AY")) {
        if (dimension.getActivity() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_funclas_man"));
        }
      }
      if (dimension.getType().equals("BP")) {
        if (dimension.getBusinessPartner() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_def_bp_man"));
        }
      }
      if (dimension.getType().equals("OO")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }
      if (dimension.getType().equals("SR")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }
      if (dimension.getType().equals("AC")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }
      if (dimension.getType().equals("MC")) {
        if (!dimension.isEfinInvisible()) {
          throw new OBException(OBMessageUtils.messageBD("Efin_dimension_Visible"));
        }
      }

      // change preference value
      getPreferenceMap(preferenceMap);

      if (dimension.getAccountingSchema().isEfinIsready()) {
        GLDimensionEventDAO.updatePreference(preferenceMap, dimension);
      }

    } catch (OBException e) {
      log.error(" Exception while saving dimension in general ledger: " + e, e);
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
      AcctSchemaElement dimension = (AcctSchemaElement) event.getTargetInstance();
      OBQuery<Campaign> budgetType = OBDal.getInstance().createQuery(Campaign.class,
          "efinCAcctschema.id='" + dimension.getAccountingSchema().getId() + "'");
      if (budgetType != null && budgetType.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Generalledger_Dimension"));
      }
    } catch (OBException e) {
      log.error(" Exception while deleting dimension in general ledger: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param preferenceMap
   * @return preferenceMap
   */

  private static Map<String, String> getPreferenceMap(Map<String, String> preferenceMap) {
    // add property for preference
    preferenceMap.put("BP", "Efin_bpdimension_visible");
    preferenceMap.put("PJ", "Efin_Projectdimension_visible");
    preferenceMap.put("U1", "Efin_future1dimension_visible");
    preferenceMap.put("U2", "Efin_future2dimension_visible");
    preferenceMap.put("AY", "Efin_functionalClassdimension_visible");
    return preferenceMap;
  }
}