/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.Efin_Child_Sequence;
import sa.elm.ob.finance.Efin_Return_Sequence;
import sa.elm.ob.finance.Efin_payment_sequences;
import sa.elm.ob.finance.dao.PaymentSequenceDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Priyanka Ranjan on 27/12/2017
 */

// Event For Payment Out Screen
public class EfinPaymentOutHeaderEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_Payment.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger LOG = LoggerFactory.getLogger(EfinPaymentOutHeaderEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      FIN_Payment paymentout = (FIN_Payment) event.getTargetInstance();
      final Property status = entities[0].getProperty(FIN_Payment.PROPERTY_STATUS);
      final Property paymentSequence = entities[0]
          .getProperty(FIN_Payment.PROPERTY_EFINPAYMENTSEQUENCE);
      final Property childSequenceProperty = entities[0]
          .getProperty(FIN_Payment.PROPERTY_EFINCHILDSEQUENCE);
      final Property parentSequenceProperty = entities[0]
          .getProperty(FIN_Payment.PROPERTY_EFINPARENTSEQUENCES);

      Map<Efin_Child_Sequence, String> sequenceMap = new HashMap<Efin_Child_Sequence, String>();

      // Supplier Bank is Mandatory if Payment Instruction is IBAN Transfer or Generic Account
      // Transfer
      if (paymentout.getEfinPayinst() != null) {
        if ((paymentout.getEfinPayinst().equals("IBAN")
            || paymentout.getEfinPayinst().equals("GENERIC"))
            && (paymentout.getEFINBank() == null || paymentout.getEfinSupbankacct() == null)
            && !paymentout.isReceipt()) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_SupplierBank_Mandatory"));
        }
      }

      // If payment is cancelled then we should reuse the sequence based on the payment sequence
      // definition
      if (!paymentout.isReceipt() && StringUtils.isNotEmpty(paymentout.getEfinPaymentsequence())) {
        if (event.getCurrentState(status) != event.getPreviousState(status)
            && "EFIN_CAN".equals(paymentout.getStatus())) {
          if (paymentout.getEfinPaymentSequences() != null
              && paymentout.getEfinPaymentSequences().isReturn()) {
            Efin_Return_Sequence returnSeq = OBProvider.getInstance()
                .get(Efin_Return_Sequence.class);
            returnSeq.setReturnsequence(new BigDecimal(paymentout.getEfinPaymentsequence()));
            returnSeq.setEfinChildSequence(paymentout.getEfinChildSequence());
            OBDal.getInstance().save(returnSeq);
            event.setCurrentState(paymentSequence, null);
          }
        }
      }

      if (!paymentout.isReceipt() && StringUtils.isNotEmpty(paymentout.getEfinPaymentsequence())
          && event.getCurrentState(parentSequenceProperty) != event
              .getPreviousState(parentSequenceProperty)) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_cantchange_parentseq"));
      }

      // generate payment sequence
      if (!paymentout.isReceipt()) {
        if (StringUtils.isEmpty(paymentout.getEfinPaymentsequence())) {
          Efin_payment_sequences paymentSeq = paymentout.getEfinPaymentSequences();
          if (paymentout.getEfinParentSequences() != null && paymentSeq != null) {
            if (paymentSeq.isSavePayment()) {
              sequenceMap = PaymentSequenceDAO.getPaymentSequenceWhileSave(paymentout);
            }
          }
          if (sequenceMap != null && sequenceMap.size() > 0) {
            Map.Entry<Efin_Child_Sequence, String> entry = sequenceMap.entrySet().iterator().next();
            Efin_Child_Sequence childSequence = entry.getKey();
            String value = entry.getValue();
            if (value != null && StringUtils.isNotEmpty(value)) {
              event.setCurrentState(paymentSequence, value);
              event.setCurrentState(childSequenceProperty, childSequence);
            }
          }
        }
      }

    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while updating Payment Out: " + e, e);
      }
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
      FIN_Payment paymentout = (FIN_Payment) event.getTargetInstance();
      final Property paymentSequence = entities[0]
          .getProperty(FIN_Payment.PROPERTY_EFINPAYMENTSEQUENCE);
      final Property childSequenceProperty = entities[0]
          .getProperty(FIN_Payment.PROPERTY_EFINCHILDSEQUENCE);

      Map<Efin_Child_Sequence, String> sequenceMap = new HashMap<Efin_Child_Sequence, String>();

      // Supplier Bank is Mandatory if Payment Instruction is IBAN Transfer or Generic Account
      // Transfer
      if (paymentout.getEfinPayinst() != null) {
        if ((paymentout.getEfinPayinst().equals("IBAN")
            || paymentout.getEfinPayinst().equals("GENERIC"))
            && (paymentout.getEFINBank() == null || paymentout.getEfinSupbankacct() == null)
            && !paymentout.isReceipt()) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_SupplierBank_Mandatory"));
        }
      }

      if (!paymentout.isReceipt()) {
        // generate payment sequence
        Efin_payment_sequences paymentSeq = paymentout.getEfinPaymentSequences();
        if (paymentout.getEfinParentSequences() != null && paymentSeq != null) {
          if (paymentSeq.isSavePayment()) {
            sequenceMap = PaymentSequenceDAO.getPaymentSequenceWhileSave(paymentout);
          }
        }
        if (sequenceMap != null && sequenceMap.size() > 0) {
          Map.Entry<Efin_Child_Sequence, String> entry = sequenceMap.entrySet().iterator().next();
          Efin_Child_Sequence childSequence = entry.getKey();
          String value = entry.getValue();
          if (value != null && StringUtils.isNotEmpty(value)) {
            event.setCurrentState(paymentSequence, value);
            event.setCurrentState(childSequenceProperty, childSequence);
          }
        }
      }
    } catch (OBException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(" Exception while creating Payment Out: " + e, e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    String AccountDate = "";
    String seqName = null;
    if (!isValidEvent(event)) {
      return;
    }
    try {

      OBContext.setAdminMode(true);
      FIN_Payment payment = (FIN_Payment) event.getTargetInstance();
      if (!payment.isReceipt() && payment.getEfinPaymentsequence() != null
          && StringUtils.isNotEmpty(payment.getEfinPaymentsequence())) {
        Efin_Return_Sequence returnSeq = OBProvider.getInstance().get(Efin_Return_Sequence.class);
        returnSeq.setReturnsequence(new BigDecimal(payment.getEfinPaymentsequence()));
        returnSeq.setEfinChildSequence(payment.getEfinChildSequence());
        OBDal.getInstance().save(returnSeq);
        OBDal.getInstance().flush();
      }
      // for both Payment Out and Payment In
      // update 'next assigned no' with previous no. in document seq while delete the recent
      // record for reusing the document sequence task no - 7409
      DocumentType docType = OBDal.getInstance().get(DocumentType.class,
          payment.getDocumentType().getId());
      if (docType.getDocumentSequence() != null) {
        // get document sequence
        Sequence docseq = OBDal.getInstance().get(Sequence.class,
            docType.getDocumentSequence().getId());
        seqName = docseq.getName();
        AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(payment.getPaymentDate());
        Utility.setDocumentSequenceAfterDeleteRecord(AccountDate, seqName,
            payment.getOrganization().getId(), Long.parseLong(payment.getDocumentNo()), null,
            false);
      }

    } catch (OBException e) {
      LOG.error(" Exception while deleting parent sequence  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
