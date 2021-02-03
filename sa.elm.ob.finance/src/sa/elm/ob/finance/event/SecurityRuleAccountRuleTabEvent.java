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
import sa.elm.ob.finance.EfinSecurityRuleslines;

/**
 * @author Priyanka Ranjan on 29/08/2016
 */
/**
 * In security rule- After initiate rule if we update,insert,delete any existing value in any of the
 * tabs, it need to display initiate rule button
 */

public class SecurityRuleAccountRuleTabEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinSecurityRuleslines.ENTITY_NAME) };
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
      EfinSecurityRuleslines accounts = (EfinSecurityRuleslines) event.getTargetInstance();

      // log.debug(accounts.getFromact().getSearchKey());
      String fromAccount = accounts.getFromact().getSearchKey();
      String toAccount = accounts.getToact().getSearchKey();
      int fromtoAccount = fromAccount.compareTo(toAccount);
      if (fromtoAccount > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FromTo_Validation"));
      }

      if (accounts.getFromact().getOrganization().getId() != accounts.getToact().getOrganization()
          .getId()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Acct_Same_Org"));
      }
      Object securityRule = accounts.getEfinSecurityRules().getId();
      EfinSecurityRules ObjSecurity = OBDal.getInstance().get(EfinSecurityRules.class,
          securityRule.toString());
      if (ObjSecurity != null) {
        ObjSecurity.setCreateact(false);
      }
    } catch (OBException e) {
      log.error(" Exception while creating record in securityrule account: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("Exception on save Event in SecurityRuleAccountRuleTabEvent ", e);
    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      EfinSecurityRuleslines accounts = (EfinSecurityRuleslines) event.getTargetInstance();
      String fromAccount = accounts.getFromact().getSearchKey();
      String toAccount = accounts.getToact().getSearchKey();
      int fromtoAccount = fromAccount.compareTo(toAccount);
      if (fromtoAccount > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FromTo_Validation"));
      }
      if (accounts.getFromact().getOrganization().getId() != accounts.getToact().getOrganization()
          .getId()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Acct_Same_Org"));
      }
      Object securityRule = accounts.getEfinSecurityRules().getId();
      EfinSecurityRules ObjSecurity = OBDal.getInstance().get(EfinSecurityRules.class,
          securityRule.toString());
      if (ObjSecurity != null) {
        ObjSecurity.setCreateact(false);
      }
    } catch (OBException e) {
      log.error(" Exception while updating record in securityrule account: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("Exception on update Event in SecurityRuleAccountRuleTabEvent ", e);
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      EfinSecurityRuleslines accounts = (EfinSecurityRuleslines) event.getTargetInstance();
      Object securityRule = accounts.getEfinSecurityRules().getId();
      EfinSecurityRules ObjSecurity = OBDal.getInstance().get(EfinSecurityRules.class,
          securityRule.toString());
      if (ObjSecurity != null) {
        ObjSecurity.setCreateact(false);
      }
    } catch (Exception e) {
      log.debug("Exception in Delete Event in SecurityRuleAccountRuleTabEvent ", e);
    }

  }
}
