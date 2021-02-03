package sa.elm.ob.finance.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;

/**
 * @author Gopalakrishnan on 28/09/2017
 * 
 */
public class BudgetAdjustmentLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(BudgetAdjustmentLine.ENTITY_NAME) };

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
      OBContext.setAdminMode(true);
      BudgetAdjustmentLine obj_adjustmentLine = (BudgetAdjustmentLine) event.getTargetInstance();
      BudgetAdjustment objAdjustment = obj_adjustmentLine.getEfinBudgetadj();
      if (objAdjustment.getDocumentStatus().equals("CO")
          || objAdjustment.getDocumentStatus().equals("EFIN_IP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Adjustment_processed"));
      }
      if (objAdjustment.getDocumentStatus().equals("EFIN_AP")) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_FCAAppLinesCantbeDeleted"));
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error(" Exception while Delete Account in Budget Adjustment Line: " + e);
      }
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * public void onSave(@Observes EntityNewEvent event) { if (!isValidEvent(event)) { return; } try
   * { BudgetAdjustmentLine adjustmentLine = (BudgetAdjustmentLine) event.getTargetInstance();
   * JSONObject json = BudgetAdjustmentLineEventDAO.checkFundsAval(adjustmentLine); if
   * (json.get("is990Acct").equals("true")) { if (json.get("isFundGreater").equals("true")) { if
   * (json.get("isWarn").equals("false")) { throw new
   * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } else { throw new
   * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } } } } catch
   * (OBException e) { OBDal.getInstance().rollbackAndClose();
   * log.error(" Exception while creating Budgte Adj Line: " + e); throw new
   * OBException(e.getMessage()); } catch (JSONException e) { e.printStackTrace(); } finally {
   * OBContext.restorePreviousMode(); } }
   */

  /*
   * public void onUpdate(@Observes EntityUpdateEvent event) { if (!isValidEvent(event)) { return; }
   * try { BudgetAdjustmentLine adjustmentLine = (BudgetAdjustmentLine) event.getTargetInstance();
   * JSONObject json = BudgetAdjustmentLineEventDAO.checkFundsAval(adjustmentLine); if
   * (json.get("is990Acct").equals("true")) { if (json.get("isFundGreater").equals("true")) { if
   * (json.get("isWarn").equals("false")) { throw new
   * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } else { throw new
   * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } } } } catch
   * (OBException e) { OBDal.getInstance().rollbackAndClose();
   * log.error(" Exception while updating FundsReqLine: " + e); throw new
   * OBException(e.getMessage()); } catch (JSONException e) { e.printStackTrace(); } finally {
   * OBContext.restorePreviousMode(); } }
   */
}
