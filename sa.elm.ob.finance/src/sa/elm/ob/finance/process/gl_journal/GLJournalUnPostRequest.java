package sa.elm.ob.finance.process.gl_journal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.ActualTransaction;
import sa.elm.ob.finance.EfinBudgetActual;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinJournalUnpostEntry;
import sa.elm.ob.finance.EfinUnPostJustification;
import sa.elm.ob.finance.ad_process.GLJournal.ResetAccounting;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.ad_process.simpleGlJournal.SimpleGlJournalDAO;

public class GLJournalUnPostRequest implements Process {
  private static final Logger log = Logger.getLogger(GLJournalUnPostRequest.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    Connection connection = null;
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    final String journalId = (String) bundle.getParams().get("GL_Journal_ID").toString();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String sql = "";
    boolean errorFlag = false, isDocDatePeriodOpen = false;

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    PreparedStatement st = null, ps = null;
    ResultSet rs = null, rs1 = null;
    String CalendarId = "";
    Period period = null;
    log.debug("budgetId:" + journalId);
    try {
      OBContext.setAdminMode(true);
      GLJournal journal = OBDal.getInstance().get(GLJournal.class, journalId);
      Organization org = journal.getOrganization();

      if (org.getCalendar() != null) {
        CalendarId = org.getCalendar().getId();
      } else {
        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
            "select eut_parent_org ('" + org.getId() + "','" + org.getClient().getId() + "')");
        Object parentOrg = query.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int k = 0; k < orgIds.length; k++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[k].replace("'", ""));
          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();
            break;
          }
        }
      }

      String acctDate = new SimpleDateFormat("dd-MM-yyyy").format(journal.getAccountingDate());
      String period_id = sa.elm.ob.utility.util.Utility.getPeriod(acctDate, org.getId());
      if (!StringUtils.isEmpty(period_id)) {
        period = OBDal.getInstance().get(Period.class, period_id);
        if (!period.getStatus().equals("M") && !period.getStatus().equals("O")) {
          // period not opened
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        } else {
          String docDate = new SimpleDateFormat("dd-MM-yyyy").format(journal.getDocumentDate());
          period_id = sa.elm.ob.utility.util.Utility.getPeriod(docDate, org.getId());
          period = OBDal.getInstance().get(Period.class, period_id);
          if ((period != null && period.getStatus().equals("M")
              || period.getStatus().equals("O"))) {
            // period opened
            isDocDatePeriodOpen = true;
          }
        }
      } else {
        // period not defined.
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
        bundle.setResult(result);
        return;
      }

      if (journal.getPosted().equals("Y")) {
        //
        // journal.setAccountingDate(journal.getDocumentDate());
        // journal.setPeriod(period);
        // OBDal.getInstance().save(journal);

        // insert into unpost justification
        EfinUnPostJustification header = OBProvider.getInstance()
            .get(EfinUnPostJustification.class);
        header.setClient(OBDal.getInstance().get(Client.class, journal.getClient().getId()));
        header.setOrganization(
            OBDal.getInstance().get(Organization.class, journal.getOrganization().getId()));
        header.setActive(true);
        header.setCreatedBy(OBDal.getInstance().get(User.class, journal.getOrganization().getId()));
        header.setCreationDate(new java.util.Date());
        header.setCreatedBy(OBDal.getInstance().get(User.class, journal.getOrganization().getId()));
        header.setUpdated(new java.util.Date());
        header.setJournalEntry(OBDal.getInstance().get(GLJournal.class, journal.getId()));
        header.setUnposteddate(new java.util.Date());
        header.setComments(comments);
        OBDal.getInstance().save(header);

        OBQuery<AccountingFact> acct = OBDal.getInstance().createQuery(AccountingFact.class,
            "recordID='" + journalId + "'");

        if (acct.list().size() > 0) {
          for (int i = 0; i < acct.list().size(); i++) {
            AccountingFact factheader = acct.list().get(i);
            EfinJournalUnpostEntry unpost = OBProvider.getInstance()
                .get(EfinJournalUnpostEntry.class);
            unpost.setClient(OBDal.getInstance().get(Client.class, factheader.getClient().getId()));
            unpost.setOrganization(
                OBDal.getInstance().get(Organization.class, factheader.getOrganization().getId()));
            unpost.setActive(true);
            unpost.setCreatedBy(
                OBDal.getInstance().get(User.class, factheader.getOrganization().getId()));
            unpost.setCreationDate(new java.util.Date());
            unpost.setCreatedBy(
                OBDal.getInstance().get(User.class, factheader.getOrganization().getId()));
            unpost.setUpdated(new java.util.Date());
            unpost.setEfinUnpostJustification(
                OBDal.getInstance().get(EfinUnPostJustification.class, header.getId()));
            unpost.setRecordID(factheader.getRecordID());
            unpost.setLineID(factheader.getLineID());
            unpost.setAmtacctCr(factheader.getCredit());
            unpost.setAmtacctDr(factheader.getDebit());
            unpost.setForeignCurrencyDebit(factheader.getForeignCurrencyDebit());
            unpost.setCredit(factheader.getForeignCurrencyCredit());
            unpost.setUniqueCode(factheader.getEFINUniqueCode());
            unpost.setAccount(factheader.getAccount());
            unpost.setAccountingDate(factheader.getAccountingDate());
            unpost.setTransactionDate(factheader.getTransactionDate());
            OBDal.getInstance().save(unpost);

          }
        }

        long start = System.currentTimeMillis();
        OBError myMessage = new OBError();

        String strTableId = "224";
        st = connection.prepareStatement(
            "SELECT MAX(FACT_ACCT_GROUP_ID) AS ID, MAX(AD_ORG_ID) AS ORG, MAX(AD_CLIENT_ID) AS CLIENT, '' AS SCHEMA_ID , '' AS SCHEMA_NAME  FROM FACT_ACCT  WHERE RECORD_ID = ?"
                + " AND AD_TABLE_ID = ?");
        st.setString(1, journalId);
        st.setString(2, strTableId);
        rs = st.executeQuery();
        if (rs.next()) {

          try {
            /* check sum of same unique code credit value is more than funds available */

            for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
              AccountingCombination uniqueCode = line.getAccountingCombination();
              if (uniqueCode.getEfinDimensiontype().equals("E")) {
                EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils
                    .getBudgetInquiry(uniqueCode, journal.getEFINBudgetDefinition());
                sql = "select line.c_validcombination_id, sum(amtacctcr) as sum "
                    + " from gl_journalline line "
                    + " join gl_journal header on line.gl_journal_id = header.gl_journal_id "
                    + " where header.gl_journal_id  ='" + journal.getId()
                    + "'  and  amtacctcr > 0 and line.c_validcombination_id ='" + uniqueCode.getId()
                    + "'" + " group by line.c_validcombination_id";
                ps = connection.prepareStatement(sql);
                rs1 = ps.executeQuery();
                while (rs1.next()) {
                  log.debug("sum:" + rs1.getBigDecimal("sum"));
                  if (budgetInq.getFundsAvailable().compareTo(rs1.getBigDecimal("sum")) < 0) {
                    errorFlag = true;
                    log.debug("sumofdebit" + errorFlag);
                    line.setEfinCheckingStaus("ERR");
                    String status = OBMessageUtils.messageBD("Efin_GLJourlLn_ExFunds");
                    line.setEfinCheckingStausFailure(
                        status.replace("@", budgetInq.getFundsAvailable().toPlainString()));
                    OBDal.getInstance().save(line);
                  }
                }
              }
            }

            if (errorFlag == false) {
              HashMap<String, Integer> hm = ResetAccounting.delete(rs.getString("CLIENT"),
                  rs.getString("ORG"), strTableId, journalId, "", "");
              OBQuery<GLJournalLine> line = OBDal.getInstance().createQuery(GLJournalLine.class,
                  " as e where e.journalEntry='" + journalId + "'");
              if (line.list() != null && line.list().size() > 0) {
                for (GLJournalLine journalLine : line.list()) {
                  log.debug("removing failure reason while unposting");
                  journalLine.setEfinCheckingStaus("SCS");
                  journalLine.setEfinCheckingStausFailure("");
                  OBQuery<EfinBudgetActual> actual = OBDal.getInstance().createQuery(
                      EfinBudgetActual.class, "as e where e.documentType='GLJ' and e.journalLine='"
                          + journalLine.getId() + "'");
                  if (actual.list() != null && actual.list().size() > 0) {
                    for (EfinBudgetActual actualList : actual.list()) {
                      actualList.setPosted(false);
                    }
                  }

                }
              }
              GLJournal header1 = OBDal.getInstance().get(GLJournal.class, journalId);
              OBDal.getInstance().refresh(header1);
              header1.setEfinPostreq(header1.getPosted());
              if (isDocDatePeriodOpen) {
                header1.setAccountingDate(journal.getDocumentDate());
                header1.setPeriod(period);
              }
              OBDal.getInstance().save(header1);
              myMessage.setType("Success");
              myMessage.setTitle(
                  Utility.messageBD(bundle.getConnection(), "Success", vars.getLanguage()));
              myMessage.setMessage(Utility.parseTranslation(bundle.getConnection(), vars,
                  vars.getLanguage(), "@UnpostedDocuments@ = " + hm.get("updated")
                      + ", @DeletedEntries@ = " + hm.get("deleted")));

              // make increase modification entry for the invoice encumbrance
              // when g/l journal adjusted against the prepayment invoice
              BigDecimal encumbrance_decrease_amount = BigDecimal.ZERO;
              Invoice journal_invoice = header1.getEfinCInvoice();
              if (header1.isEfinAdjInvoice()) {

                if (journal_invoice != null && journal_invoice.getEfinManualencumbrance() != null) {

                  // find out expense uniquecode size from invoiceline
                  List<String> invoiceUniquecodeList = SimpleGlJournalDAO
                      .findOutExpenseUniqueCodeFromInvoice(journal_invoice);

                  if (invoiceUniquecodeList.size() == 1) {
                    for (GLJournalLine journal_line : journal.getFinancialMgmtGLJournalLineList()) {
                      // AccountingCombination uniqueCode = journal_line.getAccountingCombination();
                      if (journal_line.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                        // if (!uniqueCode.getId().equals(invoiceUniquecodeList.get(0))) {
                        encumbrance_decrease_amount = journal_line.getDebit();
                        SimpleGlJournalDAO.insertEncumbranceModification(
                            encumbrance_decrease_amount, journal_invoice.getEfinManualencumbrance(),
                            invoiceUniquecodeList.get(0), journal.getDocumentNo());
                        // }
                        // else {
                        // // update invoice encumbrance as paid column values as "YES"
                        // if (journal_invoice.getGrandTotalAmount()
                        // .compareTo(journal_invoice.getEfinPreUsedamount()) == 0) {
                        // SimpleGlJournalDAO.moveReservedToActual(
                        // journal_invoice.getEfinManualencumbrance(),
                        // invoiceUniquecodeList.get(0), encumbrance_decrease_amount,
                        // journal_invoice.getId(), false);
                        // }
                        //
                        // }
                      }
                    }

                  } else {
                    // we need to check functional consultant for further flow ,as of now we have
                    // only
                    // one expense unique code associated per purchase invoice
                  }

                }
              }

              // Revert Amount from Budget Inquiry Table

              for (GLJournalLine lines : journal.getFinancialMgmtGLJournalLineList()) {
                AccountingCombination uniqueCode = lines.getAccountingCombination();
                if (uniqueCode.getEfinDimensiontype().equals("E")) {
                  // Revert from NON 990, 990 dept funds

                  if (!uniqueCode.isEFINDepartmentFund()) {
                    EfinBudgetInquiry budgetInquiry = ManualEncumbaranceSubmitDAO.getBudgetInquiry(
                        uniqueCode.getId(), journal.getEFINBudgetDefinition().getId());

                    if (budgetInquiry != null) {
                      if (lines.getCredit().compareTo(new BigDecimal("0")) > 0) {
                        budgetInquiry
                            .setSpentAmt(budgetInquiry.getSpentAmt().add(lines.getCredit()));
                        OBDal.getInstance().save(budgetInquiry);
                      } else {
                        budgetInquiry
                            .setSpentAmt(budgetInquiry.getSpentAmt().subtract(lines.getDebit()));
                        OBDal.getInstance().save(budgetInquiry);
                      }
                    }
                  }

                  EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils
                      .getBudgetInquiry(uniqueCode, journal.getEFINBudgetDefinition());
                  if (budgetInq != null) {
                    if (lines.getCredit().compareTo(new BigDecimal("0")) > 0) {
                      budgetInq.setSpentAmt(budgetInq.getSpentAmt().add(lines.getCredit()));
                      OBDal.getInstance().save(budgetInq);
                    } else {
                      budgetInq.setSpentAmt(budgetInq.getSpentAmt().subtract(lines.getDebit()));
                      OBDal.getInstance().save(budgetInq);
                    }
                  }
                }
              }

              // Remove entry of Actual Transaction
              OBQuery<ActualTransaction> actualTran = OBDal.getInstance().createQuery(
                  ActualTransaction.class, " journalEntry.id='" + header1.getId() + "'");
              for (ActualTransaction actualLine : actualTran.list()) {
                OBDal.getInstance().remove(actualLine);
              }
              OBDal.getInstance().flush();

              // Create temporary encumbrance during unpost
              if (header1.getEFINBudgetManencum() == null && !header1.isEfinAdjInvoice()
                  && header1.getEfinCInvoice() == null) {
                List<GLJournalLine> glLineList = header1.getFinancialMgmtGLJournalLineList()
                    .stream()
                    .filter(a -> a.getDebit().compareTo(BigDecimal.ZERO) > 0
                        && a.getAccountingCombination().getEfinDimensiontype().equals("E"))
                    .collect(Collectors.toList());
                if (glLineList.size() > 0) {
                  SimpleGlJournalDAO.insertTemporaryEncumbrance(header1, glLineList);
                }
              }
            } else {
              myMessage.setType("Error");
              myMessage.setTitle("@Error@");
              myMessage.setMessage("@Efin_GLJourLine_Failed@");
            }
          } catch (OBException e) {
            myMessage.setType("Error");
            myMessage.setMessage(Utility.parseTranslation(bundle.getConnection(), vars,
                vars.getLanguage(), e.getMessage()));
          }
          log.debug("Total deleting /milis: " + (System.currentTimeMillis() - start));
        }

        bundle.setResult(myMessage);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      }
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
