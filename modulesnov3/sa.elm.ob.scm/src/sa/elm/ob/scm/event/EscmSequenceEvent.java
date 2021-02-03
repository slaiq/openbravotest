package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.DocumentSequence;

/**
 * @author gopalakrishnan on 15/04/2017
 * 
 */

public class EscmSequenceEvent extends EntityPersistenceEventObserver {
  /**
   * Business event handler on table Escm_Sequence
   */

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(DocumentSequence.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    String code = "", prefixCode = "";
    try {
      OBContext.setAdminMode();
      DocumentSequence objSequence = (DocumentSequence) event.getTargetInstance();
      code = objSequence.getStartingNo().toString();
      if (objSequence.getPrefix() != null)
        prefixCode = objSequence.getPrefix().toString();

      if (objSequence.isTransaction()) {
        if (objSequence.getPrefix() == null || objSequence.getPrefixtwo() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_TraSeq_PrefMand"));
        }
      }
      // get number count
      boolean isallow = false;
      // Code should allow only numeric values
      isallow = code.matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values
      if (!isallow) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Sequence(Number Only)"));
      }
      boolean isPrefixallow = false;

      // Code should allow only numeric values
      if (!prefixCode.equals("") && objSequence.isTransaction())
        isPrefixallow = prefixCode.matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values
      if (!prefixCode.equals("") && !isPrefixallow && objSequence.isTransaction()) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Prefix(NumberOnly)"));
      }

    } catch (OBException e) {
      log.error("exception while creating sequence", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating sequence", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    String code = "", prefixCode = "";
    try {
      OBContext.setAdminMode();
      DocumentSequence objSequence = (DocumentSequence) event.getTargetInstance();
      code = objSequence.getStartingNo().toString();
      if (objSequence.getPrefix() != null)
        prefixCode = objSequence.getPrefix().toString();
      final Property istransaction = entities[0]
          .getProperty(DocumentSequence.PROPERTY_ISTRANSACTION);
      // get number count
      boolean isallow = false;
      // Code should allow only numeric values
      isallow = code.matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values
      if (!isallow) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Sequence(Number Only)"));
      }
      boolean isPrefixallow = false;

      // Code should allow only numeric values
      if (!prefixCode.equals(""))
        isPrefixallow = prefixCode.matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values

      if (!prefixCode.equals("") && !isPrefixallow && objSequence.isTransaction()) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Prefix(NumberOnly)"));
      }
      if (!event.getPreviousState(istransaction).equals(event.getCurrentState(istransaction))) {
        if (objSequence.isTransaction()) {
          if (objSequence.getPrefix() == null || objSequence.getPrefixtwo() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_TraSeq_PrefMand"));
          }
        }
      }

    } catch (OBException e) {
      log.error("exception while updating sequence", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating sequence", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
