
package sa.elm.ob.finance.event.SequenceHandlers;

import java.util.List;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.Sequence;

/**
 * 
 * @author sathishkumar.p
 * 
 *         this class is to handle the unique name in Sequence name based on name, client)
 *
 */
public class DocumentSequenceUnique extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Sequence.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      Sequence sequence = (Sequence) event.getTargetInstance();

      OBQuery<Sequence> sequenceQry = OBDal.getInstance().createQuery(Sequence.class,
          " e where upper(e.name) = :name");

      sequenceQry.setNamedParameter("name", sequence.getName().trim().toUpperCase());

      List<Sequence> sequenceList = sequenceQry.list();
      if (sequenceList != null && sequenceList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("efin_documentno_uniq"));
      }

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
      Sequence sequence = (Sequence) event.getTargetInstance();

      Property documentNo = entities[0].getProperty(Sequence.PROPERTY_NAME);

      if (event.getPreviousState(documentNo) != event.getCurrentState(documentNo)) {
        OBQuery<Sequence> sequenceQry = OBDal.getInstance().createQuery(Sequence.class,
            " e where upper(e.name) = :name1");

        sequenceQry.setNamedParameter("name1", sequence.getName().trim().toUpperCase());

        List<Sequence> sequenceList = sequenceQry.list();
        if (sequenceList != null && sequenceList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("efin_documentno_uniq"));
        }
      }

    } finally {
      OBContext.restorePreviousMode();
    }
  }

}