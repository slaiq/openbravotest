package sa.elm.ob.scm.event;

import java.util.List;

import javax.enterprise.event.Observes;

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

import sa.elm.ob.scm.EscmPoRcptRdvDefault;

/**
 * 
 * @author DivyaPrakash JS
 *
 */

public class EscmPoRcptRdvDefaultEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmPoRcptRdvDefault.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EscmPoRcptRdvDefault poRcptRdvDefault = (EscmPoRcptRdvDefault) event.getTargetInstance();
      List<EscmPoRcptRdvDefault> poRcptRdvDefaultList = null;

      // Should allow to save more than one record
      OBQuery<EscmPoRcptRdvDefault> poRcptRdvDefaultObj = OBDal.getInstance().createQuery(
          EscmPoRcptRdvDefault.class, "as e where e.client.id=:clientId and e.id <>:currentId");
      poRcptRdvDefaultObj.setNamedParameter("clientId", poRcptRdvDefault.getClient().getId());
      poRcptRdvDefaultObj.setNamedParameter("currentId", poRcptRdvDefault.getId());
      poRcptRdvDefaultList = poRcptRdvDefaultObj.list();
      if (poRcptRdvDefaultList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PoRcptRdvDefault_One_Record"));

      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
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
      EscmPoRcptRdvDefault poRcptRdvDefault = (EscmPoRcptRdvDefault) event.getTargetInstance();
      List<EscmPoRcptRdvDefault> poRcptRdvDefaultList = null;

      // Should allow to save more than one record
      OBQuery<EscmPoRcptRdvDefault> poRcptRdvDefaultObj = OBDal.getInstance()
          .createQuery(EscmPoRcptRdvDefault.class, "as e where e.client.id=:clientId");
      poRcptRdvDefaultObj.setNamedParameter("clientId", poRcptRdvDefault.getClient().getId());
      poRcptRdvDefaultList = poRcptRdvDefaultObj.list();
      if (poRcptRdvDefaultList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PoRcptRdvDefault_One_Record"));
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
