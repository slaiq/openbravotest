package sa.elm.ob.scm.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.scm.MaterialIssueRequest;

public class SalesRegionEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(SalesRegion.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      SalesRegion dept = (SalesRegion) event.getTargetInstance();
      OBQuery<MaterialIssueRequest> matiss = OBDal.getInstance()
          .createQuery(MaterialIssueRequest.class, " as e where e.beneficiaryIDName=:deptID ");
      matiss.setNamedParameter("deptID", dept.getId());
      if (matiss.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ForeignKeyViolation"));
      }
      OBQuery<ShipmentInOut> receipt = OBDal.getInstance().createQuery(ShipmentInOut.class,
          " as e where ( e.escmBname=:deptID or e.escmTobenefiName=:deptID )");
      receipt.setNamedParameter("deptID", dept.getId());
      if (receipt.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ForeignKeyViolation"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting department  : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting department  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
