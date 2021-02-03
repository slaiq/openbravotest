package sa.elm.ob.finance.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.event.dao.DistibuteMethodDAO;

public class BudgetRevisionEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EfinBudgetTransfertrx.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();

      EfinBudgetTransfertrx Revision = (EfinBudgetTransfertrx) event.getTargetInstance();
      ConnectionProvider conn = new DalConnectionProvider(false);
      final Property distributeOrg = entities[0]
          .getProperty(EfinBudgetTransfertrx.PROPERTY_DISTRIBUTEORG);
      final Property transactiondate = entities[0]
          .getProperty(EfinBudgetTransfertrx.PROPERTY_TRXDATE);
      final Property accountingdate = entities[0]
          .getProperty(EfinBudgetTransfertrx.PROPERTY_ACCOUNTINGDATE);
      DistibuteMethodDAO dao = new DistibuteMethodDAO(conn);
      Object currentdistributeOrg = event.getCurrentState(distributeOrg);
      Object previousdistributeOrg = event.getPreviousState(distributeOrg);
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (currentdistributeOrg != previousdistributeOrg) {
        if (Revision.getDistributeOrg() != null) {
          dao.checkDistUniquecodePresntOrNot(Revision, null, conn, Revision.getId(), null,
              Revision.getDistributeOrg().getId());
        }

      }

      // Future Date not allowed for transaction date
      if (Revision.getTrxdate() != null & (!event.getCurrentState(transactiondate)
          .equals(event.getPreviousState(transactiondate)))) {
        if (Revision.getTrxdate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (Revision.getAccountingDate() != null & (!event.getCurrentState(accountingdate)
          .equals(event.getPreviousState(accountingdate)))) {
        if (Revision.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }
      // Budget should have combination of (Budgettype, account group, year) for funds budget and
      // cost budget
      // Removed for task no #5283
      /*
       * if(Revision.getDocType().equals("TRS") && Revision.getTransferSource().equals("EXT")) {
       * if(Revision.getReportdate() == null || Revision.getReportnumber() == null ||
       * Revision.getDecisiondate() == null || Revision.getDecisionnumber() == null) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_BudgetRev_Error")); } }
       */

    } catch (OBException e) {
      log.error(" Exception while creating BudgetRevisionEvent: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
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
      EfinBudgetTransfertrx Revision = (EfinBudgetTransfertrx) event.getTargetInstance();
      DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      Date now = new Date();
      Date todaydate = dateFormat.parse(dateFormat.format(now));

      if (!Revision.getEfinBudgetint().getStatus().equals("OP")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_budgetint_notOpen"));
      }
      // Budget should have combination of (Budgettype, account group, year) for funds budget and
      // cost budget
      // Removed for task no #5283
      /*
       * if (Revision.getDocType().equals("TRS") && Revision.getTransferSource().equals("EXT")) { if
       * (Revision.getReportdate() == null || Revision.getReportnumber() == null ||
       * Revision.getDecisiondate() == null || Revision.getDecisionnumber() == null) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_BudgetRev_Error")); } }
       */

      // Future Date not allowed for transaction date
      if (Revision.getTrxdate() != null) {
        if (Revision.getTrxdate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Trxdate"));
        }
      }
      // Future Date not allowed for accounting date
      if (Revision.getAccountingDate() != null) {
        if (Revision.getAccountingDate().compareTo(todaydate) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FutureDate_Not_Allowed_Acctdate"));
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating BudgetRevisionEvent: " + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
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
      EfinBudgetTransfertrx Revision = (EfinBudgetTransfertrx) event.getTargetInstance();
      if (Revision.getDocStatus().equals("CO") || Revision.getDocStatus().equals("WFA")
      /*
       * || (Revision.getEfinBudTransTrxAppHistList().size() > 0 &&
       * !Revision.getDocStatus().equals("DR"))
       */
      ) {
        throw new OBException(OBMessageUtils.messageBD("Efin_BudgetRev_CantDelCom"));
      }

      /*
       * if (Revision.getEfinBudgetRevVoid() != null) { EfinBudgetTransfertrx VoidReference =
       * OBDal.getInstance().get(EfinBudgetTransfertrx.class,
       * Revision.getEfinBudgetRevVoid().getId()); VoidReference.setVoidProcess(false);
       * OBDal.getInstance().save(VoidReference); } SQLQuery budgetRevisionline =
       * OBDal.getInstance().getSession().createSQLQuery(
       * "select Efin_Budget_Transfertrxline_id from Efin_Budget_Transfertrxline line" +
       * " join Efin_Budget_Transfertrx head on head.Efin_Budget_Transfertrx_ID=line.Efin_Budget_Transfertrx_ID"
       * + " where head.Efin_Budget_Transfertrx_ID='" + Revision.getId() + "'"); List result1 =
       * budgetRevisionline.list(); if (result1.size() > 0) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_LinesPresentHeaderCannot_Delete")); }
       */
    } catch (Exception e) {
      log.error(" Exception while creating BudgetRevisionEvent: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
