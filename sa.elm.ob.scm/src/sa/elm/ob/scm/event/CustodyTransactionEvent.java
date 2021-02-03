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
import org.openbravo.model.ad.domain.Preference;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.utility.EutNextRoleLine;

public class CustodyTransactionEvent extends EntityPersistenceEventObserver {

  private static Logger log = Logger.getLogger(CustodyTransactionEvent.class);

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Escm_custody_transaction.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String roleid = OBContext.getOBContext().getRole().getId();
      Boolean allowUpdate = true;

      Escm_custody_transaction transaction = (Escm_custody_transaction) event.getTargetInstance();

      if (transaction.getGoodsShipmentLine() != null) {
        if ((transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmReceivingtype()
            .equals("INR")
            || transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmReceivingtype()
                .equals("LD"))
            && !transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmDocstatus()
                .equals("DR")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
        }

        if (!transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmReceivingtype()
            .equals("INR")) {
          if (!transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmDocstatus()
              .equals("DR")) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
          }
        } else {
          if (transaction.getGoodsShipmentLine().getShipmentReceipt().getEutNextRole() != null) {
            for (EutNextRoleLine line : transaction.getGoodsShipmentLine().getShipmentReceipt()
                .getEutNextRole().getEutNextRoleLineList()) {

              if (!roleid.equals(line.getRole().getId())) {
                allowUpdate = false;
                break;
              }

            }
            if (!allowUpdate) {
              if (!transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmDocstatus()
                  .equals("DR")) {
                throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
              }
            } else {
              if (transaction.getGoodsShipmentLine().getShipmentReceipt().getEscmDocstatus()
                  .equals("CO")) {
                throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
              }
            }
          }
        }
      }

      // UserRoles objUserRole = transaction.getUpdatedBy().getADUserRolesList().get(0);
      if (roleid != null) {
        // Role objRole = roleid.getRole();
        // check role is warehouse Keeper
        OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
            "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' "
                + " and e.visibleAtRole.id=:roleID ");
        preQuery.setNamedParameter("roleID", roleid);

        if ((preQuery.list().size()) == 0) {
          if (transaction.getEscmMaterialReqln() != null) {
            if (!transaction.getEscmMaterialReqln().getEscmMaterialRequest().getAlertStatus()
                .equals("DR")) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_POReceipt_CantDel"));
            }
          }
        }
      }

    } catch (OBException e) {
      log.error("Exception while delete the Custody Transaction :", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting the Custody Transaction :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
