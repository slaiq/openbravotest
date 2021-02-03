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

import sa.elm.ob.finance.Efin_Child_Sequence;
import sa.elm.ob.finance.util.TableIdConstant;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is used to handle [save, update, delete] event of Efin_parent_sequenece table.
 * 
 * @author sathishkumar.P
 *
 */

public class ChildSequenceEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Efin_Child_Sequence.ENTITY_NAME) };

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
      Efin_Child_Sequence childSeq = (Efin_Child_Sequence) event.getTargetInstance();
      Property nextseq = entities[0].getProperty(Efin_Child_Sequence.PROPERTY_NEXTSEQUENCE);

      // set next seq
      event.setCurrentState(nextseq, childSeq.getFINFrom());

      // check decimal point is there in from and to
      if (childSeq.getFINFrom().doubleValue() % 1 != 0
          || childSeq.getFINTo().doubleValue() % 1 != 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_seq_decimalnotallowed"));
      }

      if (childSeq.getFINFrom().compareTo(childSeq.getFINTo()) > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_seq_fromnotgreater"));
      }

      // check child from value should not be lesser than parent value
      if (childSeq.getFINFrom().compareTo(childSeq.getEfinParentSequence().getFINFrom()) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_childfromvaluelesser"));
      }

      // child to value should not greater than parent to value
      if (childSeq.getEfinParentSequence().getFINTo().compareTo(childSeq.getFINTo()) < 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_childtovaluegreater"));
      }

      HashMap<String, String> columnValueMap = new HashMap<String, String>();
      columnValueMap.put(TableIdConstant.CHILD_PARENTSEQUENCE_HQLNAME,
          childSeq.getEfinParentSequence().getId());

      boolean checkOverlap = UtilityDAO.checkOverlapExists(TableIdConstant.CHILDSEQUENCE_HQLNAME,
          TableIdConstant.CHILDSEQUENCE_FROMHQLNAME, TableIdConstant.CHILDSEQUENCE_TOHQLNAME,
          childSeq.getFINFrom(), childSeq.getFINTo(), columnValueMap, null);

      if (checkOverlap) {
        throw new OBException(OBMessageUtils.messageBD("Efin_sequence_Overlap"));
      }

    } catch (Exception e) {
      log.error(" Exception while saving Child sequeence : " + e);
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
      Efin_Child_Sequence childSeq = (Efin_Child_Sequence) event.getTargetInstance();
      Property fromSeq = entities[0].getProperty(Efin_Child_Sequence.PROPERTY_FINFROM);
      Property toSeq = entities[0].getProperty(Efin_Child_Sequence.PROPERTY_FINTO);
      Property enable = entities[0].getProperty(Efin_Child_Sequence.PROPERTY_ENABLE);
      Property user = entities[0].getProperty(Efin_Child_Sequence.PROPERTY_USERCONTACT);

      if (childSeq.isUsed() && (event.getCurrentState(enable) != event.getPreviousState(enable)
          || event.getCurrentState(fromSeq) != event.getPreviousState(fromSeq)
          || event.getCurrentState(user) != event.getPreviousState(user))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_seq_cannotupdate"));
      }

      if (childSeq.isUsed() && event.getCurrentState(fromSeq) != event.getPreviousState(fromSeq)) {
        if (childSeq.getFINTo().compareTo(childSeq.getNextSequence()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_tolessthannext"));
        }
      }

      if (event.getCurrentState(toSeq) != event.getPreviousState(toSeq)
          || event.getCurrentState(fromSeq) != event.getPreviousState(fromSeq)) {

        // check decimal point is there in from and to
        if (childSeq.getFINFrom().doubleValue() % 1 != 0
            || childSeq.getFINTo().doubleValue() % 1 != 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_seq_decimalnotallowed"));
        }

        if (childSeq.getFINFrom().compareTo(childSeq.getFINTo()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_seq_fromnotgreater"));
        }

        // check child from value should not be lesser than parent value
        if (childSeq.getFINFrom().compareTo(childSeq.getEfinParentSequence().getFINFrom()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_childfromvaluelesser"));
        }

        // child to value should not greater than parent to value
        if (childSeq.getEfinParentSequence().getFINTo().compareTo(childSeq.getFINTo()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_childtovaluegreater"));
        }

        HashMap<String, String> columnValueMap = new HashMap<String, String>();
        columnValueMap.put(TableIdConstant.CHILD_PARENTSEQUENCE_HQLNAME,
            childSeq.getEfinParentSequence().getId());

        String whereClause = " e.id !='" + childSeq.getId() + "'";

        boolean checkOverlap = UtilityDAO.checkOverlapExists(TableIdConstant.CHILDSEQUENCE_HQLNAME,
            TableIdConstant.CHILDSEQUENCE_FROMHQLNAME, TableIdConstant.CHILDSEQUENCE_TOHQLNAME,
            childSeq.getFINFrom(), childSeq.getFINTo(), columnValueMap, whereClause);

        if (checkOverlap) {
          throw new OBException(OBMessageUtils.messageBD("Efin_sequence_Overlap"));
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating child sequence : " + e);
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
      Efin_Child_Sequence childSeq = (Efin_Child_Sequence) event.getTargetInstance();

      if (childSeq.isUsed()) {
        throw new OBException(OBMessageUtils.messageBD("Efin_cantdeletesequence"));
      }

    } catch (OBException e) {
      log.error(" Exception deleting child sequence : " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
