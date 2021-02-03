package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.event.Observes;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudTransTrxAppHist;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.EfinRdvBudgTransfer;
import sa.elm.ob.finance.event.dao.BudgetLinesDAO;

public class BudgetRevisionLineEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetTransfertrxline.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private static final Logger log = LoggerFactory.getLogger(BudgetRevisionLineEvent.class);

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EfinBudgetTransfertrxline budgrevline = (EfinBudgetTransfertrxline) event.getTargetInstance();
      final Property increaseAmt = entities[0]
          .getProperty(EfinBudgetTransfertrxline.PROPERTY_INCREASE);
      BigDecimal transfer_Amount = BigDecimal.ZERO;
      BigDecimal IncreaseAmount = BigDecimal.ZERO;
      /*
       * JSONObject json = BudgetRevisionLineEventDAO.checkFundsAval(budgrevline); if
       * (json.get("is990Acct").equals("true")) { if (json.get("isFundGreater").equals("true")) { if
       * (json.get("isWarn").equals("false")) { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } else { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } } }
       */
      if (budgrevline != null) {
        if (budgrevline.getDecrease().signum() == -1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRevision_ln_dec"));
        }
        if (budgrevline.getIncrease().signum() == -1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRevision_ln_Inc"));
        }
      }
      final Property accountingcombination = entities[0]
          .getProperty(EfinBudgetTransfertrxline.PROPERTY_ACCOUNTINGCOMBINATION);// getting current
                                                                                 // entered
      // value
      Object currentaccountingcom = event.getCurrentState(accountingcombination);
      Object previousaccountingcom = event.getPreviousState(accountingcombination);
      if (currentaccountingcom != previousaccountingcom) {
        OBQuery<EfinBudgetTransfertrxline> budgrevchk = OBDal.getInstance().createQuery(
            EfinBudgetTransfertrxline.class,
            " efinBudgetTransfertrx.id ='" + budgrevline.getEfinBudgetTransfertrx().getId()
                + "' and client.id = '" + budgrevline.getClient().getId()
                + "' and accountingCombination.id='"
                + budgrevline.getAccountingCombination().getId() + "'"); // getting already saved
                                                                         // values

        if (budgrevchk.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRev_Error_Msg"));
        }
      }
      // isRDV hold ='Y' then should not reduce the increase amount
      if (budgrevline.isRdvhold()) {
        if (!event.getPreviousState(increaseAmt).equals(event.getCurrentState(increaseAmt))) {
          transfer_Amount = BudgetLinesDAO.getTransferamount(budgrevline);
          IncreaseAmount = new BigDecimal(event.getCurrentState(increaseAmt).toString());
          if (IncreaseAmount.compareTo(transfer_Amount) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_IncreaseAmt_Lssthan_TransferAmt"));
          }

        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating line in Budget Revision: " + e);
      throw new OBException(e.getMessage());
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
      EfinBudgetTransfertrxline budgrevline = (EfinBudgetTransfertrxline) event.getTargetInstance();

      /*
       * JSONObject json = BudgetRevisionLineEventDAO.checkFundsAval(budgrevline); if
       * (json.get("is990Acct").equals("true")) { if (json.get("isFundGreater").equals("true")) { if
       * (json.get("isWarn").equals("false")) { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } else { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_FCAAmtCantBeGreatThanBCU")); } } }
       */

      if (budgrevline != null) {
        if (budgrevline.getDecrease().signum() == -1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRevision_ln_dec"));
        }
        if (budgrevline.getIncrease().signum() == -1) {
          throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRevision_ln_Inc"));
        }
      }
      OBQuery<EfinBudgetTransfertrxline> budgrevchk = OBDal.getInstance().createQuery(
          EfinBudgetTransfertrxline.class,
          " efinBudgetTransfertrx.id ='" + budgrevline.getEfinBudgetTransfertrx().getId()
              + "' and client.id = '" + budgrevline.getClient().getId()
              + "' and accountingCombination.id='" + budgrevline.getAccountingCombination().getId()
              + "'"); // getting already saved values

      if (budgrevchk.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRev_Error_Msg"));

      }
    } catch (OBException e) {
      log.error(" Exception while creating line in Budget Revision:  " + e);
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
      OBContext.setAdminMode();
      EfinBudgetTransfertrxline budgrevline = (EfinBudgetTransfertrxline) event.getTargetInstance();
      OBQuery<EfinBudTransTrxAppHist> fundsreqhist = OBDal.getInstance()
          .createQuery(EfinBudTransTrxAppHist.class, "as e where e.efinBudgetTransfertrx.id='"
              + budgrevline.getEfinBudgetTransfertrx().getId() + "' order by creationDate desc");
      fundsreqhist.setMaxResult(1);
      if (fundsreqhist.list().size() > 0) {
        EfinBudTransTrxAppHist objLastLine = fundsreqhist.list().get(0);
        if (budgrevline.getCreatedBy().getId().equals(OBContext.getOBContext().getUser().getId())) {
          if (objLastLine.getREVAction() != null && (!objLastLine.getREVAction().equals("REW")
              && !objLastLine.getREVAction().equals("REV")
              && !objLastLine.getREVAction().equals("REA"))) {
            throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRev_CantDelCom"));
          }
        }
      }
      List<EfinRdvBudgTransfer> holdTransferList = budgrevline.getEfinRdvBudgtransferList();

      if (holdTransferList.size() > 0) {
        for (EfinRdvBudgTransfer list : holdTransferList) {
          OBDal.getInstance().remove(list);
        }

      }

      /*
       * EfinBudgetTransfertrx header = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
       * budgrevline.getEfinBudgetTransfertrx().getId()); if (header != null) { if
       * ((!header.getDocStatus().equals("DR")) || (header.getDocStatus().equals("DR") &&
       * header.isVoidProcess() && header.getEfinBudgetRevVoid() != null)) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_BudgetRevision_Error")); } }
       */
    } catch (Exception e) {
      log.error(" Exception while deleting line in Budget Revision:  " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
