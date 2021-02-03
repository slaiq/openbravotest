package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.utility.util.Utility;

public class SalesVoucherEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escmsalesvoucher.ENTITY_NAME) };

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
      Escmsalesvoucher salesvoucher = (Escmsalesvoucher) event.getTargetInstance();
      final Property BidNo = entities[0].getProperty(Escmsalesvoucher.PROPERTY_VOUCHERNO);
      String sequence = "";
      Boolean sequenceexists = false;
      // set new Spec No

      sequence = Utility.getTransactionSequence(salesvoucher.getOrganization().getId(), "SV");
      sequenceexists = Utility.chkTransactionSequence(salesvoucher.getOrganization().getId(), "SV",
          sequence);

      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(BidNo, sequence);
      }

    } catch (OBException e) {
      log.error("exception while creating SalesVoucherEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating SalesVoucherEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
