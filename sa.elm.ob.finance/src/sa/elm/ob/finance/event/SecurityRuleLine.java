package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import sa.elm.ob.finance.EfinSecurityRuleslines;

/**
 * @author Gopalakrishnan on 01/07/2016
 */
public class SecurityRuleLine extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinSecurityRuleslines.ENTITY_NAME) };

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
      EfinSecurityRuleslines securityLines = (EfinSecurityRuleslines) event.getTargetInstance();
      // rules process don't allow to save new records
      if (securityLines.getEfinSecurityRules().isEfinProcessbutton()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_securityRuleLine_SaveFailed"));
      }
    }

    catch (OBException e) {
      log.error(" Exception while creating Lines in Security Rule: " + e);
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
      EfinSecurityRuleslines securityLines = (EfinSecurityRuleslines) event.getTargetInstance();
      // rules process don't allow to save new records
      if (securityLines.getEfinSecurityRules().isEfinProcessbutton()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_SecurityRuleDeleteFailed"));
      }
    }

    catch (OBException e) {
      log.error(" Exception while deleteing Lines in Security Rule: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
