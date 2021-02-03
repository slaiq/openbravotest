package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
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
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.ad_process.simpleGlJournal.SimpleGlJournalDAO;
import sa.elm.ob.finance.event.dao.GlJournalLineEventDAO;
import sa.elm.ob.utility.util.Utility;

public class GLJournalLineEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(GLJournalLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  static final BigDecimal ZERO = new BigDecimal(0.0);
  private Logger log = Logger.getLogger(this.getClass());

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      GLJournalLine journalLine = (GLJournalLine) event.getTargetInstance();
      final Property budgetline = entities[0].getProperty(GLJournalLine.PROPERTY_EFINBUDGETLINES);

      final Property uniquecode = entities[0].getProperty(GLJournalLine.PROPERTY_EFINUNIQUECODE);
      final Property debit = entities[0].getProperty(GLJournalLine.PROPERTY_DEBIT);
      final Property credit = entities[0].getProperty(GLJournalLine.PROPERTY_CREDIT);
      String BudgetLineId = "";
      List<AccountingCombination> accCombinationList = null;
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
      query.setParameter(0, journalLine.getOrganization().getId());
      query.setParameter(1, journalLine.getAccountingCombination().getAccount().getId());
      query.setParameter(2, journalLine.getProject().getId());
      query.setParameter(3, journalLine.getSalesRegion().getId());
      query.setParameter(4, journalLine.getSalesCampaign().getId());
      query.setParameter(5, journalLine.getActivity().getId());
      query.setParameter(6, journalLine.getStDimension().getId());
      query.setParameter(7, journalLine.getNdDimension().getId());
      query.setParameter(8, Utility.formatDate(journalLine.getJournalEntry().getAccountingDate()));
      query.setParameter(9, OBContext.getOBContext().getCurrentClient().getId());
      List<Object> list = query.list();
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          if (row[2] != null)
            BudgetLineId = row[2].toString();
          else {
            BudgetLineId = null;
          }
        }

      } else {
        BudgetLineId = null;
      }
      if (!StringUtils.isEmpty(BudgetLineId)) {
        event.setCurrentState(budgetline,
            OBDal.getInstance().get(EFINBudgetLines.class, BudgetLineId));
      } else {
        event.setCurrentState(budgetline, null);
      }
      // Restrict to allow negative value in debit
      if (journalLine.getDebit().compareTo(ZERO) == -1) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Negative_Value_Debit"));
      }
      // Restrict to allow negative value in credit
      if (journalLine.getCredit().compareTo(ZERO) == -1) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Negative_Value_Credit"));
      }
      // same unique code not added twice for one document
      OBQuery<GLJournalLine> duplicate = OBDal.getInstance().createQuery(GLJournalLine.class,
          " efinUniqueCode='" + journalLine.getEfinUniqueCode() + "' and  journalEntry.id='"
              + journalLine.getJournalEntry().getId() + "'");
      if (event.getPreviousState(uniquecode) != (event.getCurrentState(uniquecode))) {
        if (duplicate.list() != null && duplicate.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode"));
        }
      }
      // Default Department not allowed for expense accounts
      if (BudgetLineId != null && journalLine.getSalesRegion().isDefault()
          && journalLine.getAccountingCombination().getAccount().getAccountType().equals("E")) {

        throw new OBException(OBMessageUtils.messageBD("Efin_Gljournalline_default").replace("@",
            journalLine.getSalesRegion().getSearchKey()));

      }

      if (BudgetLineId != null && BudgetLineId != "") {
        if (journalLine.getEfinFundsAvailable().compareTo(new BigDecimal(0)) == 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_GLJouLn_FAZero"));
        }
      }
      // if(!event.getPreviousState(account).equals(event.getCurrentState(account))) {

      // if((!event.getPreviousState(account).equals(event.getCurrentState(account))) ||
      // (!event.getPreviousState(beneficiary).equals(event.getCurrentState(beneficiary)))) {
      // if((!event.getPreviousState(account).equals(event.getCurrentState(account))) ||
      // (!event.getPreviousState(beneficiary).equals(event.getCurrentState(beneficiary)))) {
      if ((journalLine.getAccountingCombination().getAccount().getAccountType().equals("A")
          || journalLine.getAccountingCombination().getAccount().getAccountType().equals("L"))
          && (journalLine.getBusinessPartner() == null
              || journalLine.getBusinessPartner().equals(""))) {
        // if((journalLine.getAccountingCombination().getAccount().getAccountType().equals("A") ||
        // journalLine.getAccountingCombination().getAccount().getAccountType().equals("L")) &&
        // (journalLine.getEfinBeneficiary() == null ||
        // journalLine.getEfinBeneficiary().equals(""))) {
        // throw new OBException(OBMessageUtils.messageBD("Efin_GLJournalLine_Beneficiary"));
      }
      // }
      // debit should select only expense account
      /*
       * if (event.getPreviousState(uniquecode) != (event.getCurrentState(uniquecode)) ||
       * event.getPreviousState(debit) != (event.getCurrentState(debit))) { if
       * (journalLine.getJournalEntry().isEfinAdjInvoice() &&
       * journalLine.getJournalEntry().getEfinCInvoice() != null) { AccountingCombination acc =
       * journalLine.getAccountingCombination(); if
       * (journalLine.getDebit().compareTo(BigDecimal.ZERO) > 0) { if
       * (!acc.getAccount().getAccountType().equals("E")) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Debit_Expense")); }
       * 
       * } }
       * 
       * }
       */
      // sum of line total amt should be equal to total debit amt
      if (event.getPreviousState(uniquecode) != (event.getCurrentState(uniquecode))
          || event.getPreviousState(debit) != (event.getCurrentState(debit))) {
        if (journalLine.getJournalEntry().isEfinAdjInvoice()
            && journalLine.getJournalEntry().getEfinCInvoice() != null) {
          if (journalLine.getDebit().compareTo(BigDecimal.ZERO) > 0) {
            boolean chkTotalDebitAmt = SimpleGlJournalDAO.checkAppliedAmt(
                journalLine.getJournalEntry().getEfinCInvoice(),
                journalLine.getAccountingCombination().getId(), journalLine.getDebit());
            if (chkTotalDebitAmt) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Appamt_Greater"));
            }

          }
        }
      }
      // uniquecode not matched then throw the error(credit)
      if (journalLine.getJournalEntry().isEfinAdjInvoice()
          && journalLine.getJournalEntry().getEfinCInvoice() != null) {
        if (journalLine.getCredit().compareTo(BigDecimal.ZERO) > 0) {
          accCombinationList = GlJournalLineEventDAO
              .getUniqueCodeListUsingInv(journalLine.getJournalEntry().getEfinCInvoice());
          if (accCombinationList != null && accCombinationList.size() > 0) {

            List<AccountingCombination> invList = accCombinationList.stream()
                .filter(a -> a.getId().equals(journalLine.getAccountingCombination().getId()))
                .collect(Collectors.toList());

            if (invList.size() == 0) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Uniquecode_Notmatch"));
            }

          }
        }
      }
      // sum of line total amt should be equal to total credit amt
      if (event.getPreviousState(uniquecode) != (event.getCurrentState(uniquecode))
          || event.getPreviousState(credit) != (event.getCurrentState(credit))) {
        if (journalLine.getJournalEntry().isEfinAdjInvoice()
            && journalLine.getJournalEntry().getEfinCInvoice() != null) {
          if (journalLine.getCredit().compareTo(BigDecimal.ZERO) > 0) {
            boolean chkTotalCreditAmt = SimpleGlJournalDAO.checkAppliedAmtUsingCredit(
                journalLine.getJournalEntry().getEfinCInvoice(),
                journalLine.getAccountingCombination().getId(), journalLine.getCredit());
            if (chkTotalCreditAmt) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Appamt_Greater"));
            }
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating record in GL Jouranl Line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String BudgetLineId = "";
      List<AccountingCombination> accCombinationList = null;
      GLJournalLine journalLine = (GLJournalLine) event.getTargetInstance();
      final Property budgetline = entities[0].getProperty(GLJournalLine.PROPERTY_EFINBUDGETLINES);

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
      query.setParameter(0, journalLine.getOrganization().getId());
      query.setParameter(1, journalLine.getAccountingCombination().getAccount().getId());
      query.setParameter(2, journalLine.getProject().getId());
      query.setParameter(3, journalLine.getSalesRegion().getId());
      query.setParameter(4, journalLine.getSalesCampaign().getId());
      query.setParameter(5, journalLine.getActivity().getId());
      query.setParameter(6, journalLine.getStDimension().getId());
      query.setParameter(7, journalLine.getNdDimension().getId());
      query.setParameter(8, Utility.formatDate(journalLine.getJournalEntry().getAccountingDate()));
      query.setParameter(9, OBContext.getOBContext().getCurrentClient().getId());
      List<Object> list = query.list();
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          if (row[2] != null)
            BudgetLineId = row[2].toString();

          else {
            BudgetLineId = null;
          }
        }

      } else {
        BudgetLineId = null;
      }

      // Restrict to allow negative value in debit
      if (journalLine.getDebit().compareTo(ZERO) == -1) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Negative_Value_Debit"));
      }
      // Restrict to allow negative value in credit
      if (journalLine.getCredit().compareTo(ZERO) == -1) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Negative_Value_Credit"));
      }
      // String BudgetLineId = getBudgetLine(event, null);
      if (!StringUtils.isEmpty(BudgetLineId)) {
        event.setCurrentState(budgetline,
            OBDal.getInstance().get(EFINBudgetLines.class, BudgetLineId));
      } else {
        event.setCurrentState(budgetline, null);
      }
      // same unique code not added twice for one document
      OBQuery<GLJournalLine> duplicate = OBDal.getInstance().createQuery(GLJournalLine.class,
          " efinUniqueCode='" + journalLine.getEfinUniqueCode() + "' and  journalEntry.id='"
              + journalLine.getJournalEntry().getId() + "'");
      if (duplicate.list() != null && duplicate.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode"));
      }
      // Default department not allowed for expense accounts
      if (BudgetLineId != null && journalLine.getSalesRegion().isDefault()
          && journalLine.getAccountingCombination().getAccount().getAccountType().equals("E")) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Gljournalline_default").replace("@",
            journalLine.getSalesRegion().getSearchKey()));

      }

      if (BudgetLineId != null && BudgetLineId != "") {
        if ((journalLine.getCredit().compareTo(new BigDecimal("0")) <= 0)
            && journalLine.getEfinFundsAvailable().compareTo(new BigDecimal(0)) == 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_GLJouLn_FAZero"));
        }
      }
      // if debit then should select only expense account
      /*
       * if (journalLine.getJournalEntry().isEfinAdjInvoice() &&
       * journalLine.getJournalEntry().getEfinCInvoice() != null) { AccountingCombination acc =
       * journalLine.getAccountingCombination(); if
       * (journalLine.getDebit().compareTo(BigDecimal.ZERO) > 0) { if
       * (!acc.getAccount().getAccountType().equals("E")) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Debit_Expense")); }
       * 
       * } }
       */

      // sum of line total amt should be equal to total debit amt
      if (journalLine.getJournalEntry().isEfinAdjInvoice()
          && journalLine.getJournalEntry().getEfinCInvoice() != null) {
        if (journalLine.getDebit().compareTo(BigDecimal.ZERO) > 0) {
          boolean chkTotalDebitAmt = SimpleGlJournalDAO.checkAppliedAmt(
              journalLine.getJournalEntry().getEfinCInvoice(),
              journalLine.getAccountingCombination().getId(), journalLine.getDebit());
          if (chkTotalDebitAmt) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Appamt_Greater"));
          }

        }
      }
      // uniquecode not matched then throw the error(credit)
      if (journalLine.getJournalEntry().isEfinAdjInvoice()
          && journalLine.getJournalEntry().getEfinCInvoice() != null) {
        if (journalLine.getCredit().compareTo(BigDecimal.ZERO) > 0) {
          accCombinationList = GlJournalLineEventDAO
              .getUniqueCodeListUsingInv(journalLine.getJournalEntry().getEfinCInvoice());
          if (accCombinationList != null && accCombinationList.size() > 0) {

            List<AccountingCombination> invList = accCombinationList.stream()
                .filter(a -> a.getId().equals(journalLine.getAccountingCombination().getId()))
                .collect(Collectors.toList());

            if (invList.size() == 0) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Uniquecode_Notmatch"));
            }

          }
        }
      }
      // sum of line total amt should be equal to total credit amt
      if (journalLine.getJournalEntry().isEfinAdjInvoice()
          && journalLine.getJournalEntry().getEfinCInvoice() != null) {
        if (journalLine.getCredit().compareTo(BigDecimal.ZERO) > 0) {
          boolean chkTotalCreditAmt = SimpleGlJournalDAO.checkAppliedAmtUsingCredit(
              journalLine.getJournalEntry().getEfinCInvoice(),
              journalLine.getAccountingCombination().getId(), journalLine.getCredit());
          if (chkTotalCreditAmt) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Appamt_Greater"));
          }

        }
      }

      /*
       * if ((journalLine.getAccountingCombination().getAccount().getAccountType().equals("A") ||
       * journalLine.getAccountingCombination().getAccount().getAccountType().equals("L")) &&
       * (journalLine.getBusinessPartner() == null || journalLine.getBusinessPartner().equals("")))
       * { // if((journalLine.getAccountingCombination().getAccount().getAccountType().equals("A")
       * || // journalLine.getAccountingCombination().getAccount().getAccountType().equals("L")) &&
       * // (journalLine.getEfinBeneficiary() == null || //
       * journalLine.getEfinBeneficiary().equals(""))) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_GLJournalLine_Beneficiary")); }
       */

    }

    catch (OBException e) {
      log.error(" Exception while creating record in GL Journal Line: " + e);
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
      GLJournalLine line = (GLJournalLine) event.getTargetInstance();
      if (line.getJournalEntry().getDocumentStatus().equals("EFIN_WFA"))
        throw new OBException(OBMessageUtils.messageBD("Efin_Document_Processed"));
    } catch (Exception e) {
      log.error(" Exception while Delete record in GL Jouranl Line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
  /*
   * public static String getBudgetLine(EntityNewEvent event, EntityUpdateEvent updateevent) {
   * String LineId = null; PreparedStatement ps = null; ResultSet rs = null; GLJournalLine
   * journalLine = null; try { if(event != null) journalLine = (GLJournalLine)
   * event.getTargetInstance(); else journalLine = (GLJournalLine) updateevent.getTargetInstance();
   * SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
   * "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
   * 
   * query.setParameter(0, journalLine.getOrganization().getId()); query.setParameter(1,
   * journalLine.getAccountingCombination().getAccount().getId()); query.setParameter(2,
   * journalLine.getProject().getId()); query.setParameter(3, journalLine.getSalesRegion().getId());
   * query.setParameter(4, journalLine.getSalesCampaign().getId()); query.setParameter(5,
   * journalLine.getActivity().getId()); query.setParameter(6,
   * journalLine.getStDimension().getId()); query.setParameter(7,
   * journalLine.getNdDimension().getId()); query.setParameter(8,
   * Utility.formatDate(journalLine.getAccountingDate())); query.setParameter(9,
   * OBContext.getOBContext().getCurrentClient().getId()); System.out.println("result:" +
   * query.getNamedParameters()); List<Object> list = query.list(); System.out.println("result:" +
   * query.list().size()); if(query != null && query.list().size() > 0) { for (Iterator iterator =
   * list.iterator(); iterator.hasNext();) { Object[] row = (Object[]) iterator.next();
   * if(!StringUtils.isEmpty(row[2].toString())) LineId = row[2].toString(); } } else { LineId =
   * null; } } catch (Exception e) { throw new OBException(e.getMessage()); } return LineId;
   * 
   * }
   */
}
