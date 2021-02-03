package sa.elm.ob.finance.event;

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
import org.openbravo.model.common.invoice.Invoice;

import sa.elm.ob.finance.ad_process.Sadad.ReceiveBillErrorConstant;

/**
 * This class is used to handle [save, update] event of invoice table. Mainly for saddad related
 * fields
 * 
 * @author sathishkumar.P
 *
 */

public class InvoiceSadadEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

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
      OBContext.setAdminMode(true);
      Invoice invoice = (Invoice) event.getTargetInstance();

      // This check only for order to receive
      if (invoice.isSalesTransaction() && invoice.getTransactionDocument().isEfinIssaddad()) {

        if (invoice.getEfinApplicationtype() == null) {
          throw new OBException(
              OBMessageUtils.messageBD(ReceiveBillErrorConstant.EFIN_APPLICATION_TYPE)
                  + ReceiveBillErrorConstant.WORD_SEPEARATOR
                  + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
        }

        if (invoice.getEfinCustomeridtype() == null) {
          throw new OBException(OBMessageUtils.messageBD(ReceiveBillErrorConstant.CUSTOMERIDTYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
        }

        if (invoice.getEfinCustomertype() == null) {
          throw new OBException(OBMessageUtils.messageBD(ReceiveBillErrorConstant.EFIN_CUSTOMERTYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
        }

        if (invoice.getEfinElementvalue() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_mainaccnotempty"));
        }

        if (invoice.getEfinDeptauthcode() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_deptauthnotempty"));
        }

        if (invoice.getEfinDeptbenefitcode() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_deptbennotempty"));
        }

      }

    } catch (Exception e) {
      log.error(" Exception while saving invoice : " + e);
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
      OBContext.setAdminMode(true);
      Invoice invoice = (Invoice) event.getTargetInstance();

      // This check only for order to receive
      if (invoice.isSalesTransaction() && invoice.getTransactionDocument().isEfinIssaddad()) {

        if (invoice.getEfinApplicationtype() == null) {
          throw new OBException(
              OBMessageUtils.messageBD(ReceiveBillErrorConstant.EFIN_APPLICATION_TYPE)
                  + ReceiveBillErrorConstant.WORD_SEPEARATOR
                  + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
        }

        if (invoice.getEfinCustomeridtype() == null) {
          throw new OBException(OBMessageUtils.messageBD(ReceiveBillErrorConstant.CUSTOMERIDTYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
        }

        if (invoice.getEfinCustomertype() == null) {
          throw new OBException(OBMessageUtils.messageBD(ReceiveBillErrorConstant.EFIN_CUSTOMERTYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
        }

        if (invoice.getEfinElementvalue() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_mainaccnotempty"));
        }

        if (invoice.getEfinDeptauthcode() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_deptauthnotempty"));
        }

        if (invoice.getEfinDeptbenefitcode() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_deptbennotempty"));
        }

      }

    } catch (OBException e) {
      log.error(" Exception while updating invoice : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
