package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.List;

import sa.elm.ob.scm.DocumentSequence;

/**
 * 
 * @author gopalakrishnan on 08/06/2017
 * 
 *         This Servlet class was responsible for business events in Escm_Sequence Table
 */

public class TransactionSequenceEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(DocumentSequence.ENTITY_NAME) };

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
      DocumentSequence transaction = (DocumentSequence) event.getTargetInstance();
      String message = null, transactionname = "";
      OBQuery<List> ref = OBDal.getInstance().createQuery(List.class,
          " as e where  e.reference.id='001853451717483682B375C6C6C8EF2B' "
              + " and e.searchKey=:transType");
      ref.setNamedParameter("transType", transaction.getTransactionType());
      ref.setMaxResult(1);
      if (ref.list().size() > 0) {
        transactionname = ref.list().get(0).getName();
      }

      if ((transaction.getTransactionType().toString().equals("ANN")
          || transaction.getTransactionType().toString().equals("PEE")
          || transaction.getTransactionType().toString().equals("TEE")
          || transaction.getTransactionType().toString().equals("BGD")
          || transaction.getTransactionType().toString().equals("OEC")
          || transaction.getTransactionType().toString().equals("BGCON")
          || transaction.getTransactionType().toString().equals("BGAMT"))
          && !transaction.getOrganization().getId().toString().equals("0")) {
        message = OBMessageUtils.messageBD("Escm_Announcement_Org(*)");
        message = message.replace("%", transactionname);
        throw new OBException(message);
      }
      if ((!transaction.getTransactionType().toString().equals("ANN")
          && !transaction.getTransactionType().toString().equals("PEE")
          && !transaction.getTransactionType().toString().equals("TEE")
          && !transaction.getTransactionType().toString().equals("BGD")
          && !transaction.getTransactionType().toString().equals("OEC")
          && !transaction.getTransactionType().toString().equals("BGCON")
          && !transaction.getTransactionType().toString().equals("BGAMT"))
          && transaction.getOrganization().getId().toString().equals("0")) {
        message = OBMessageUtils.messageBD("Escm_Sequence_Org(Without *)");
        message = message.replace("%", transactionname);
        throw new OBException(message);
      }

    } catch (OBException e) {
      log.error("exception while creating transaction Sequence", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating transaction Sequence", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      String message = null, transactionname = "";
      DocumentSequence transaction = (DocumentSequence) event.getTargetInstance();
      OBQuery<List> ref = OBDal.getInstance().createQuery(List.class,
          " as e where e.reference.id='001853451717483682B375C6C6C8EF2B' "
              + " and e.searchKey=:transType");
      ref.setNamedParameter("transType", transaction.getTransactionType());
      ref.setMaxResult(1);
      if (ref.list().size() > 0) {
        transactionname = ref.list().get(0).getName();
      }
      if ((transaction.getTransactionType().toString().equals("ANN")
          || transaction.getTransactionType().toString().equals("PEE")
          || transaction.getTransactionType().toString().equals("TEE")
          || transaction.getTransactionType().toString().equals("BGD")
          || transaction.getTransactionType().toString().equals("OEC")
          || transaction.getTransactionType().toString().equals("BGCON")
          || transaction.getTransactionType().toString().equals("BGAMT"))
          && !transaction.getOrganization().getId().toString().equals("0")) {
        message = OBMessageUtils.messageBD("Escm_Announcement_Org(*)");
        message = message.replace("%", transactionname);
        throw new OBException(message);
      }
      if ((!transaction.getTransactionType().toString().equals("ANN")
          && !transaction.getTransactionType().toString().equals("PEE")
          && !transaction.getTransactionType().toString().equals("TEE")
          && !transaction.getTransactionType().toString().equals("BGD")
          && !transaction.getTransactionType().toString().equals("OEC")
          && !transaction.getTransactionType().toString().equals("BGCON")
          && !transaction.getTransactionType().toString().equals("BGAMT"))
          && transaction.getOrganization().getId().toString().equals("0")) {
        message = OBMessageUtils.messageBD("Escm_Sequence_Org(Without *)");
        message = message.replace("%", transactionname);
        throw new OBException(message);
      }

    } catch (OBException e) {
      log.error("exception while creating transaction Sequence", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating transaction Sequence", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
