package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMPaymentSchedule;

public class PaymentScheduleEvent extends EntityPersistenceEventObserver {

  /**
   * @author Mouli K
   */

  private Logger log = Logger.getLogger(this.getClass());
  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMPaymentSchedule.ENTITY_NAME) };
  Order order = null;

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    BigDecimal paymentScheduleAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      ESCMPaymentSchedule paymentSchedule = (ESCMPaymentSchedule) event.getTargetInstance();
      order = paymentSchedule.getDocumentNo();
      if (paymentSchedule.getAmount() != null) {
        if ((paymentSchedule.getAmount().compareTo(paymentSchedule.getInvoicedAmt()) < 0)
            && paymentSchedule.getDocumentNo().getEscmAppstatus().equals("ESCM_AP")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PaymentScheduleAmt_lt_invAmt"));
        }
        paymentScheduleAmt = order.getEscmPaymentscheduleAmt().add(paymentSchedule.getAmount());
        if (order.getGrandTotalAmount().compareTo(paymentScheduleAmt) >= 0) {
          order.setEscmPaymentscheduleAmt(paymentScheduleAmt);
          OBDal.getInstance().save(order);
        } else {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PaymentScheduleAmt_Exceed"));
        }
      }
      if (paymentSchedule.getNeedbydate() != null) {
        Date payementScheduleDate = dateFormat
            .parse(dateFormat.format(paymentSchedule.getNeedbydate()));
        Date orderDate = dateFormat.parse(dateFormat.format(order.getOrderDate()));
        if (payementScheduleDate.compareTo(orderDate) < 0)
          throw new OBException(OBMessageUtils.messageBD("ESCM_PaySchedule_Needbydate_gt_podate"));
      }
    } catch (OBException e) {
      log.error("Exception while saving PaymentScheduleEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while saving PaymentScheduleEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Property amount = entities[0].getProperty(ESCMPaymentSchedule.PROPERTY_AMOUNT);
    ESCMPaymentSchedule paymentSchedule = (ESCMPaymentSchedule) event.getTargetInstance();
    order = paymentSchedule.getDocumentNo();

    BigDecimal curAmount = paymentSchedule.getAmount() != null ? paymentSchedule.getAmount()
        : BigDecimal.ZERO;
    BigDecimal preAmount = event.getPreviousState(amount) != null
        ? (BigDecimal) event.getPreviousState(amount)
        : BigDecimal.ZERO;
    BigDecimal paymentScheduleAmt = BigDecimal.ZERO;

    try {
      OBContext.setAdminMode();
      if (curAmount.compareTo(preAmount) != 0) {
        if (paymentSchedule.getAmount().compareTo(paymentSchedule.getInvoicedAmt()) < 0
            && paymentSchedule.getDocumentNo().getEscmAppstatus().equals("ESCM_AP")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PaymentScheduleAmt_lt_invAmt"));
        }
        paymentScheduleAmt = order.getEscmPaymentscheduleAmt().subtract(preAmount).add(curAmount);
        // check payment schdule amount update via pop process or line level edit
        if (!order.isEscmUpdatePaymentschedule()) {
          if (order.getGrandTotalAmount().compareTo(paymentScheduleAmt) >= 0) {
            order.setEscmPaymentscheduleAmt(paymentScheduleAmt);
            OBDal.getInstance().save(order);
          } else {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PaymentScheduleAmt_Exceed"));
          }
        } else {
          order.setEscmPaymentscheduleAmt(paymentScheduleAmt);
          OBDal.getInstance().save(order);
        }

      }
      if (paymentSchedule.getNeedbydate() != null) {
        Date payementScheduleDate = dateFormat
            .parse(dateFormat.format(paymentSchedule.getNeedbydate()));
        Date orderDate = dateFormat.parse(dateFormat.format(order.getOrderDate()));
        if (payementScheduleDate.compareTo(orderDate) < 0)
          throw new OBException(OBMessageUtils.messageBD("ESCM_PaySchedule_Needbydate_gt_podate"));
      }

    } catch (OBException e) {
      e.printStackTrace();
      log.error("Exception while updating PaymentScheduleEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Updating PaymentScheduleEvent  : ", e);
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
      ESCMPaymentSchedule paymentSchedule = (ESCMPaymentSchedule) event.getTargetInstance();
      if (paymentSchedule.getInvoicedAmt().compareTo(BigDecimal.ZERO) > 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_PO_PaymentSchedule_Delete"));
      }
      if (paymentSchedule.getAmount() != null) {
        order = paymentSchedule.getDocumentNo();
        order.setEscmPaymentscheduleAmt(
            order.getEscmPaymentscheduleAmt().subtract(paymentSchedule.getAmount()));
        OBDal.getInstance().save(order);
      }
    } catch (Exception e) {
      log.error(" Exception while Deleting PaymentScheduleEvent  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
