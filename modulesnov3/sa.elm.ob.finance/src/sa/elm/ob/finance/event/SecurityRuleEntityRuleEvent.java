package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.finance.EfinSecurityRulesbpartner;
import sa.elm.ob.finance.EfinSecurityRulesbudg;

/**
 * @author Mouli.K
 */

public class SecurityRuleEntityRuleEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinSecurityRulesbpartner.ENTITY_NAME) };
  private Logger log = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      EfinSecurityRulesbpartner accounts = (EfinSecurityRulesbpartner) event.getTargetInstance();

      // log.debug(accounts.getTobudget().getSearchKey() + "=============================");
      String fromBp = accounts.getFrombpartner().getSearchKey();
      String toBp = accounts.getTobpartner().getSearchKey();
      int fromtoBp = fromBp.compareTo(toBp);
      if (fromtoBp > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FromTo_Validation"));
      }

    } catch (OBException e) {
      log.error(" Exception while creating record in securityrule Project: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("Exception on save Event in SecurityRuleEntityRuleEvent ", e);
    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      EfinSecurityRulesbpartner accounts = (EfinSecurityRulesbpartner) event.getTargetInstance();

      // log.debug(accounts.getTobudget().getSearchKey() + "=============================");
      String fromBp = accounts.getFrombpartner().getSearchKey();
      String toBp = accounts.getTobpartner().getSearchKey();
      int fromtoBp = fromBp.compareTo(toBp);
      if (fromtoBp > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FromTo_Validation"));
      }
    } catch (OBException e) {
      log.error(" Exception while updating record in securityrule Project: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("Exception on update Event in SecurityRuleBudgetTypeRuleTabEvent ", e);
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      EfinSecurityRulesbudg accounts = (EfinSecurityRulesbudg) event.getTargetInstance();
      Object securityRule = accounts.getEfinSecurityRules().getId();
      EfinSecurityRules ObjSecurity = OBDal.getInstance().get(EfinSecurityRules.class,
          securityRule.toString());
      if (ObjSecurity != null) {
        ObjSecurity.setCreateact(false);
      }
    } catch (Exception e) {
      log.debug("Exception in Delete Event in SecurityRuleEntityRuleEvent ", e);
    }

  }
}
