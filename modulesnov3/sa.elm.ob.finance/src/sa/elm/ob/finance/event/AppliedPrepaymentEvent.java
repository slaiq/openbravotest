package sa.elm.ob.finance.event;

import java.math.BigDecimal;

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

import sa.elm.ob.finance.AppliedPrepaymentInvoice;

/**
 * 
 * @author Gopalakrishnan on 26/08/2016
 * 
 */
public class AppliedPrepaymentEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(AppliedPrepaymentInvoice.ENTITY_NAME) };

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
      AppliedPrepaymentInvoice appPay = (AppliedPrepaymentInvoice) event.getTargetInstance();
      if (BigDecimal.ZERO.compareTo(appPay.getAppliedAmount()) == 1) {
        throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmountNegative"));
      }
      if (appPay.getEfinAppliedInvoice() != null) {
        if (appPay.getAppliedAmount()
            .compareTo(appPay.getEfinAppliedInvoice().getEfinPreRemainingamount()) == 1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_isHigh"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception in AppliedPrepaymentEvent Update " + e);
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
      AppliedPrepaymentInvoice appPay = (AppliedPrepaymentInvoice) event.getTargetInstance();
      if (BigDecimal.ZERO.compareTo(appPay.getAppliedAmount()) == 1) {
        throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmountNegative"));
      }
      if (appPay.getEfinAppliedInvoice() != null) {
        if (appPay.getAppliedAmount()
            .compareTo(appPay.getEfinAppliedInvoice().getEfinPreRemainingamount()) == 1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_AppliedAmt_isHigh"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception in AppliedPrepaymentEvent Save " + e);
      throw new OBException(e.getMessage());
    } finally {

    }
  }

}