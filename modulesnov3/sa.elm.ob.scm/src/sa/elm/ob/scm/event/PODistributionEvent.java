package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmproposalDistribution;

public class PODistributionEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmproposalDistribution.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmproposalDistribution proposaldistribution = (EscmproposalDistribution) event
          .getTargetInstance();
      final Property proposalmgmt = entities[0]
          .getProperty(EscmproposalDistribution.PROPERTY_ESCMPROPOSALMGMT);
      if (proposaldistribution.getDocumentNo() != null) {
        if (proposaldistribution.getDocumentNo().getEscmProposalmgmt() == null) {
          event.setCurrentState(proposalmgmt, null);
        }
      }

    } catch (OBException e) {
      log.debug("exception while creating PODistributionEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating PODistributionEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

}
