package sa.elm.ob.finance.event;

import java.util.HashMap;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.Efin_Parent_Sequence;
import sa.elm.ob.finance.util.TableIdConstant;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is used to handle [save, update, delete] event of Efin_parent_sequenece table.
 * 
 * @author sathishkumar.P
 *
 */

public class ParentSequenceEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Efin_Parent_Sequence.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      Efin_Parent_Sequence parentSeq = (Efin_Parent_Sequence) event.getTargetInstance();
      Property value = entities[0].getProperty(Efin_Parent_Sequence.PROPERTY_SEARCHKEY);

      // check decimal point is there
      if (parentSeq.getFINFrom().doubleValue() % 1 != 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_seq_decimalnotallowed"));
      }

      // check from is lesser than to value if so throw error

      if (parentSeq.getFINFrom().compareTo(parentSeq.getFINTo()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_seq_fromnotgreater"));
      }

      HashMap<String, String> columnValueMap = new HashMap<String, String>();
      columnValueMap.put(TableIdConstant.PARENT_PAYMENTSEQUENCE_HQLNAME,
          parentSeq.getEfinPaymentSequences().getId());

      boolean checkOverlap = UtilityDAO.checkOverlapExists(TableIdConstant.PARENTSEQUENCE_HQLNAME,
          TableIdConstant.PARENTSEQUENCE_FROMHQLNAME, TableIdConstant.PARENTSEQUENCE_TOHQLNAME,
          parentSeq.getFINFrom(), parentSeq.getFINTo(), columnValueMap, null);

      if (checkOverlap) {
        throw new OBException(OBMessageUtils.messageBD("Efin_sequence_Overlap"));
      }

      String searchkey = parentSeq.getFINFrom().toString() + " - "
          + parentSeq.getFINTo().toString();

      event.setCurrentState(value, searchkey);

    } catch (Exception e) {
      log.error(" Exception while saving Parent sequeence : " + e);
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
      Efin_Parent_Sequence parentSeq = (Efin_Parent_Sequence) event.getTargetInstance();
      Property fromSeq = entities[0].getProperty(Efin_Parent_Sequence.PROPERTY_FINFROM);
      Property toSeq = entities[0].getProperty(Efin_Parent_Sequence.PROPERTY_FINTO);
      Property enable = entities[0].getProperty(Efin_Parent_Sequence.PROPERTY_ENABLE);

      Property value = entities[0].getProperty(Efin_Parent_Sequence.PROPERTY_SEARCHKEY);

      if ((!parentSeq.isEnable())
          && (event.getCurrentState(enable) != event.getPreviousState(enable))) {

        long usedCount = parentSeq.getEfinChildSequenceList().parallelStream()
            .filter(a -> a.isUsed()).count();
        if (usedCount > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_seq_cannotupdate"));
        }
      }

      if (event.getCurrentState(toSeq) != event.getPreviousState(toSeq)
          || event.getCurrentState(fromSeq) != event.getPreviousState(fromSeq)) {

        // check decimal point is there in from and to
        if (parentSeq.getFINFrom().doubleValue() % 1 != 0
            || parentSeq.getFINTo().doubleValue() % 1 != 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_seq_decimalnotallowed"));
        }

        if (parentSeq.getFINFrom().compareTo(parentSeq.getFINTo()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_seq_fromnotgreater"));
        }

        HashMap<String, String> columnValueMap = new HashMap<String, String>();
        columnValueMap.put(TableIdConstant.PARENT_PAYMENTSEQUENCE_HQLNAME,
            parentSeq.getEfinPaymentSequences().getId());

        String whereClause = "e.id !='" + parentSeq.getId() + "'";

        boolean checkOverlap = UtilityDAO.checkOverlapExists(TableIdConstant.PARENTSEQUENCE_HQLNAME,
            TableIdConstant.PARENTSEQUENCE_FROMHQLNAME, TableIdConstant.PARENTSEQUENCE_TOHQLNAME,
            parentSeq.getFINFrom(), parentSeq.getFINTo(), columnValueMap, whereClause);

        if (checkOverlap) {
          throw new OBException(OBMessageUtils.messageBD("Efin_sequence_Overlap"));
        }

        long parentFromCount = parentSeq.getEfinChildSequenceList().parallelStream()
            .filter(a -> a.getFINFrom().compareTo(parentSeq.getFINFrom()) < 0).count();

        if (parentFromCount > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_childfromvaluelesser"));
        }

        long parentToCount = parentSeq.getEfinChildSequenceList().parallelStream()
            .filter(a -> a.getFINTo().compareTo(parentSeq.getFINTo()) > 0).count();

        if (parentToCount > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_childtovaluegreater"));

        }

      }

      String searchkey = parentSeq.getFINFrom().toString() + " - "
          + parentSeq.getFINTo().toString();

      event.setCurrentState(value, searchkey);

    } catch (OBException e) {
      log.error(" Exception while updating parent sequence : " + e);
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

      OBContext.setAdminMode(true);
      Efin_Parent_Sequence seq = (Efin_Parent_Sequence) event.getTargetInstance();

      long usedCount = seq.getEfinChildSequenceList().parallelStream().filter(a -> a.isUsed())
          .count();

      if (usedCount > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_cantdeletesequence"));
      }

    } catch (OBException e) {
      log.error(" Exception while deleting parent sequence  : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
