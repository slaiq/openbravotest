package sa.elm.ob.scm.event;

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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;

import sa.elm.ob.scm.MaterialIssueRequestCustody;

public class CustodyDetailsHandler extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in escm_mrequest_custody Table
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(MaterialIssueRequestCustody.ENTITY_NAME) };
  private static Logger log = Logger.getLogger(CustodyDetailsHandler.class);

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {

      OBContext.setAdminMode();
      // if record processed then should not allow to update/delete.

      MaterialIssueRequestCustody objRequestcustody = (MaterialIssueRequestCustody) event
          .getTargetInstance();
      if (!objRequestcustody.getAlertStatus().equals("N")) {

        if (objRequestcustody.getEscmMaterialReqln().getEscmMaterialRequest().getAlertStatus()
            .equals("ESCM_TR")) {
          throw new OBException(OBMessageUtils.messageBD("Escm_custody_update/delete"));
        }
        String role = OBContext.getOBContext().getRole().getId();

        // check role is warehouse Keeper
        OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
            "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' "
                + " and e.visibleAtRole.id=:roleID");
        preQuery.setNamedParameter("roleID", role);
        if (preQuery.list().size() == 0) {
          log.debug("Enter delete");
          throw new OBException(OBMessageUtils.messageBD("Escm_custody_update/delete"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while Deleting IssueRequest custody: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting IssueRequest custody: " + e);
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
      // String strQuery = "";
      // Query query = null;
      // BigDecimal qtyonhand = BigDecimal.ZERO;
      Property status = entities[0].getProperty(MaterialIssueRequestCustody.PROPERTY_ALERTSTATUS);

      Property bname = entities[0]
          .getProperty(MaterialIssueRequestCustody.PROPERTY_BENEFICIARYIDNAME);

      // if record processed then should not allow to update/delete.
      MaterialIssueRequestCustody objRequestcustody = (MaterialIssueRequestCustody) event
          .getTargetInstance();
      // if record processed then should not allow to update/delete.
      if (event.getCurrentState(bname) == event.getPreviousState(bname)) {
        /*
         * if (event.getCurrentState(status).equals(event.getPreviousState(status))) { if
         * (objRequestcustody.getEscmMaterialReqln().getEscmMaterialRequest().getAlertStatus()
         * .equals("ESCM_TR")) { throw new
         * OBException(OBMessageUtils.messageBD("Escm_custody_update/delete")); } }
         */
        String role = OBContext.getOBContext().getRole().getId();

        // get role
        // check role is warehouse Keeper
        if (objRequestcustody.getAlertStatus().equals(event.getPreviousState(status))) {
          OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
              "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' "
                  + " and e.visibleAtRole.id=:roleID");
          preQuery.setNamedParameter("roleID", role);

          if (preQuery.list().size() == 0) {
            log.debug("Enter update");
            throw new OBException(OBMessageUtils.messageBD("Escm_custody_update/delete"));
          }
        }
      }
      /*
       * if ((event.getCurrentState(custatt) != null && (!event.getCurrentState(custatt).equals(
       * event.getPreviousState(custatt)))) || (event.getCurrentState(custatt) == null &&
       * event.getPreviousState(custatt) != null)) { strQuery =
       * "    SELECT      COALESCE(sum(QtyOnHand),0) as   QtyOnHand  FROM (SELECT QtyOnHand " +
       * "    FROM M_Storage_Detail s    WHERE s.M_Product_ID= ?  " +
       * "    UNION    SELECT 0 AS QtyOnHand  FROM M_Storage_Pending s " +
       * "   WHERE s.M_Product_ID= ?) a"; query =
       * OBDal.getInstance().getSession().createSQLQuery(strQuery); query.setParameter(0,
       * objRequestcustody.getProduct().getId()); query.setParameter(1,
       * objRequestcustody.getProduct().getId()); log.debug("strQuery:" + query); if (query != null
       * && query.list().size() > 0) { qtyonhand = (BigDecimal) query.list().get(0); if
       * (qtyonhand.compareTo(BigDecimal.ZERO) < 0 || qtyonhand.compareTo(BigDecimal.ZERO) > 0) {
       * throw new OBException(OBMessageUtils.messageBD("ESCM_PrdDontchgCustAtt")); } } }
       */

    } catch (OBException e) {
      log.error(" Exception while updating IssueRequest custody:  ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating IssueRequest custody:  ", e);
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
      /*
       * // if record processed then should not allow to update/delete. MaterialIssueRequestCustody
       * objRequestcustody = (MaterialIssueRequestCustody) event .getTargetInstance(); if
       * (objRequestcustody.getEscmMaterialReqln().getEscmMaterialRequest().getAlertStatus()
       * .equals("ESCM_TR")) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_custody_update/delete")); }
       */String role = OBContext.getOBContext().getRole().getId();
      // get role
      // check role is warehouse Keeper
      OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
          "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' "
              + " and e.visibleAtRole.id=:roleID ");
      preQuery.setNamedParameter("roleID", role);

      if (preQuery.list().size() == 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_custody_update/delete"));
      }
    } catch (OBException e) {
      log.error(" Exception while saving IssueRequest custody:  ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while saving IssueRequest custody:  ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
