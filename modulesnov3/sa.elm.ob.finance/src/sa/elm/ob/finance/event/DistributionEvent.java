package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.efinDistributionLines;

/**
 * 
 * @author poongodi on 15/12/2017
 *
 */
public class DistributionEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(efinDistributionLines.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  private static final Logger log = LoggerFactory.getLogger(efinDistributionLines.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      efinDistributionLines distribution = (efinDistributionLines) event.getTargetInstance();
      final Property accountingcombination = entities[0]
          .getProperty(efinDistributionLines.PROPERTY_ACCOUNTINGCOMBINATION);
      // adjuniquecode should be unique
      OBQuery<efinDistributionLines> duplicate1 = OBDal.getInstance()
          .createQuery(efinDistributionLines.class,
              " accountingCombination.id='" + distribution.getAccountingCombination().getId()
                  + "' and  efinDistribution.id='" + distribution.getEfinDistribution().getId()
                  + "'");
      if (!event.getPreviousState(accountingcombination)
          .equals(event.getCurrentState(accountingcombination))) {
        if (duplicate1.list() != null && duplicate1.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_adj_uniquecode"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating line in advance type: " + e);
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
      efinDistributionLines distribution = (efinDistributionLines) event.getTargetInstance();

      // adjuniquecode should be unique
      OBQuery<efinDistributionLines> duplicate1 = OBDal.getInstance()
          .createQuery(efinDistributionLines.class,
              " accountingCombination.id='" + distribution.getAccountingCombination().getId()
                  + "' and  efinDistribution.id='" + distribution.getEfinDistribution().getId()
                  + "'");

      if (duplicate1.list() != null && duplicate1.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_adj_uniquecode"));
      }

    } catch (OBException e) {
      log.error(" Exception while creating line in advance type:  " + e);
      throw new OBException(e.getMessage());
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }
}
