package sa.elm.ob.finance.process.gl_journal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.DocAmortization;
import org.openbravo.erpCommon.ad_forms.DocBank;
import org.openbravo.erpCommon.ad_forms.DocCash;
import org.openbravo.erpCommon.ad_forms.DocDPManagement;
import org.openbravo.erpCommon.ad_forms.DocFINFinAccTransaction;
import org.openbravo.erpCommon.ad_forms.DocFINPayment;
import org.openbravo.erpCommon.ad_forms.DocFINReconciliation;
import org.openbravo.erpCommon.ad_forms.DocGLJournal;
import org.openbravo.erpCommon.ad_forms.DocInOut;
import org.openbravo.erpCommon.ad_forms.DocInternalConsumption;
import org.openbravo.erpCommon.ad_forms.DocInventory;
import org.openbravo.erpCommon.ad_forms.DocInvoice;
import org.openbravo.erpCommon.ad_forms.DocMatchInv;
import org.openbravo.erpCommon.ad_forms.DocMovement;
import org.openbravo.erpCommon.ad_forms.DocOrder;
import org.openbravo.erpCommon.ad_forms.DocPayment;
import org.openbravo.erpCommon.ad_forms.DocProduction;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.ActualTransaction;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.ad_process.simpleGlJournal.SimpleGlJournalDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class GLJournalPostRequest implements Process {
  private static final Logger log = Logger.getLogger(GLJournalPostRequest.class);
  private final OBError obError = new OBError();
  public int errors = 0;
  public int success = 0;
  boolean errorFlag = false;

  // ConnectionProvider conn = bundle.getConnection();
  PreparedStatement ps = null;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("rework the budget");
    Connection con = null;
    try {
      ConnectionProvider connectionProvider = new DalConnectionProvider(true);
      con = connectionProvider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    final String journalId = (String) bundle.getParams().get("GL_Journal_ID").toString();
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String postDate = bundle.getParams().get("postdate").toString();

    GLJournal journal = OBDal.getInstance().get(GLJournal.class, journalId);
    final OBError msg = new OBError();
    String sql = "";
    // String comments = (String) bundle.getParams().get("comments").toString();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    ResultSet rs = null;

    log.debug("budgetId:" + journalId);
    try {
      OBContext.setAdminMode();
      ConnectionProvider connectionProvider = new DalConnectionProvider(true);
      con = connectionProvider.getConnection();
      // Check Posting Sequence
      String AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(journal.getAccountingDate());
      String postGregDate = UtilityDAO.convertToGregorian_tochar(postDate);
      String CalendarId = "";
      Organization org = journal.getOrganization();
      Period period = null;
      String canInsertActualUniqueCode = "";

      // check selected posting date should not be lesser then Accounting Date
      // if (dateFormat.parse(postGregDate).compareTo(journal.getDocumentDate()) < 0) {
      // OBDal.getInstance().rollbackAndClose();
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // "@Efin_ActDateGrtThanDocDate@");
      // bundle.setResult(result);
      // return;
      // }

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

      // String todayAccountDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
      String period_id = sa.elm.ob.utility.util.Utility.getPeriod(postGregDate, org.getId());
      if (!StringUtils.isEmpty(period_id)) {
        period = OBDal.getInstance().get(Period.class, period_id);
        if (!period.getStatus().equals("M") && !period.getStatus().equals("O")) {
          // period not opened
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      } else {
        // period not defined.
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
        bundle.setResult(result);
        return;
      }

      if (!journal.getPosted().equals("Y")) {
        String SequenceNo = UtilityDAO.getGeneralSequence(postGregDate, "NPS", CalendarId,
            org.getId(), false);
        if (SequenceNo.equals("0")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_NoPaymentSequence@");
          bundle.setResult(result);
          return;
        }

        if (journal.getEFINBudgetManencum() == null) {
          /* check sum of same unique code debit value is more than funds available */

          for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
            AccountingCombination uniqueCode = line.getAccountingCombination();
            if (uniqueCode.getEfinDimensiontype().equals("E")) {
              EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(uniqueCode,
                  journal.getEFINBudgetDefinition());
              BigDecimal fundsAvailable = BigDecimal.ZERO;
              if (budgetInq != null) {
                fundsAvailable = budgetInq.getFundsAvailable();
              }
              sql = "select line.c_validcombination_id,  sum(amtacctdr) as sum "
                  + " from gl_journalline line "
                  + " join gl_journal header on line.gl_journal_id = header.gl_journal_id "
                  + " where header.gl_journal_id  ='" + journal.getId()
                  + "'  and  amtacctdr > 0 and line.c_validcombination_id ='" + uniqueCode.getId()
                  + "'" + " group by line.c_validcombination_id  ";
              ps = connectionProvider.getPreparedStatement(sql);
              rs = ps.executeQuery();
              while (rs.next()) {
                log.debug("sum:" + rs.getBigDecimal("sum"));
                if (fundsAvailable.compareTo(rs.getBigDecimal("sum")) < 0) {
                  errorFlag = true;
                  log.debug("sumofdebit" + errorFlag);
                  line.setEfinCheckingStaus("ERR");
                  String status = OBMessageUtils.messageBD("Efin_GLJourlLn_ExFunds");
                  line.setEfinCheckingStausFailure(
                      status.replace("@", fundsAvailable.toPlainString()));
                  OBDal.getInstance().save(line);
                }
              }
            }
          }
        }
        if (errorFlag == false) {
          journal.setAccountingDate(dateFormat.parse(postGregDate));
          journal.setPeriod(period);
          OBDal.getInstance().save(journal);

          AcctServer tempServer = get("224", vars.getClient(), vars.getOrg(), journalId, "",
              connectionProvider);
          boolean postSuccess = false;
          postSuccess = tempServer.post(journalId, false, vars, connectionProvider, con);
          errors = errors + tempServer.errors;
          // success =+ success + tempServer.success;
          GLJournal header = OBDal.getInstance().get(GLJournal.class, journalId);
          OBDal.getInstance().refresh(header);
          String post = header.getPosted();
          header.setEfinPostreq(header.getPosted());
          // header.setAccountingDate(new Date());
          // header.setPeriod(period);
          // OBDal.getInstance().save(journal);
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();

          // set accounting date and period in accounting tab in Simple G/L journal window.
          // OBQuery<AccountingFact> factAcctObj = OBDal.getInstance()
          // .createQuery(AccountingFact.class, "as e where e.recordID =:simpleGLjournalId");
          // factAcctObj.setNamedParameter("simpleGLjournalId", header.getId());
          // if (factAcctObj != null && factAcctObj.list().size() > 0)
          // for (AccountingFact fact : factAcctObj.list()) {
          // AccountingFact factAcct = OBDal.getInstance().get(AccountingFact.class, fact.getId());
          // factAcct.setAccountingDate(new Date());
          // factAcct.setPeriod(period);
          // OBDal.getInstance().save(factAcct);
          // }

          if (!postSuccess || !header.getPosted().equals("Y")) {
            OBDal.getInstance().rollbackAndClose();
            header.setEfinPostreq(post);
            OBDal.getInstance().save(header);
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Posted_Gl_Fail@");
            bundle.setResult(result);
            return;
          } else {

            BigDecimal encumbrance_decrease_amount = BigDecimal.ZERO;
            Invoice journal_invoice = journal.getEfinCInvoice();
            // Remove temporary encumbrance created
            if (journal.getEFINBudgetManencum() != null) {
              SimpleGlJournalDAO.removeTemporaryEncumbrance(journal);

              // check the Funds available, once you remove the temporary encumbrance
              for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
                AccountingCombination uniqueCode = line.getAccountingCombination();
                if (uniqueCode.getEfinDimensiontype().equals("E")) {
                  EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils
                      .getBudgetInquiry(uniqueCode, journal.getEFINBudgetDefinition());
                  BigDecimal fundsAvailable = BigDecimal.ZERO;
                  if (budgetInq != null) {
                    fundsAvailable = budgetInq.getFundsAvailable();
                  }
                  sql = "select line.c_validcombination_id,  sum(amtacctdr) as sum "
                      + " from gl_journalline line "
                      + " join gl_journal header on line.gl_journal_id = header.gl_journal_id "
                      + " where header.gl_journal_id  ='" + journal.getId()
                      + "'  and  amtacctdr > 0 and line.c_validcombination_id ='"
                      + uniqueCode.getId() + "'" + " group by line.c_validcombination_id  ";
                  ps = connectionProvider.getPreparedStatement(sql);
                  rs = ps.executeQuery();
                  while (rs.next()) {
                    log.debug("sum:" + rs.getBigDecimal("sum"));
                    if (fundsAvailable.compareTo(rs.getBigDecimal("sum")) < 0) {
                      errorFlag = true;
                      log.debug("sumofdebit" + errorFlag);
                      line.setEfinCheckingStaus("ERR");
                      String status = OBMessageUtils.messageBD("Efin_GLJourlLn_ExFunds");
                      line.setEfinCheckingStausFailure(
                          status.replace("@", fundsAvailable.toPlainString()));
                      OBDal.getInstance().save(line);
                    }
                  }
                }
              }

              if (errorFlag) {
                OBDal.getInstance().rollbackAndClose();
                header.setEfinPostreq(post);
                OBDal.getInstance().save(header);
                msg.setType("Error");
                msg.setTitle("@Error@");
                msg.setMessage("@Efin_GLJourLine_Failed@");
                bundle.setResult(msg);
                return;
              }
            }

            // when the gljournal is against prepayemnt invoice
            // make modification entry in invoice expense account
            // find out invoice expense account

            if (journal.isEfinAdjInvoice()) {

              if (journal_invoice != null && journal_invoice.getEfinManualencumbrance() != null) {

                // find out expense uniquecode size from invoiceline
                List<String> invoiceUniquecodeList = SimpleGlJournalDAO
                    .findOutExpenseUniqueCodeFromInvoice(journal_invoice);

                if (invoiceUniquecodeList.size() == 1) {
                  for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
                    // AccountingCombination uniqueCode = line.getAccountingCombination();
                    if (line.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                      // if (!uniqueCode.getId().equals(invoiceUniquecodeList.get(0))) {
                      encumbrance_decrease_amount = line.getDebit();
                      SimpleGlJournalDAO.insertEncumbranceModification(
                          encumbrance_decrease_amount.negate(),
                          journal_invoice.getEfinManualencumbrance(), invoiceUniquecodeList.get(0),
                          journal.getDocumentNo());
                      // }
                      // else {
                      // // update invoice encumbrance as paid column values as "YES"
                      // if (journal_invoice.getGrandTotalAmount()
                      // .compareTo(journal_invoice.getEfinPreUsedamount()) == 0) {
                      // SimpleGlJournalDAO.moveReservedToActual(
                      // journal_invoice.getEfinManualencumbrance(),
                      // invoiceUniquecodeList.get(0), encumbrance_decrease_amount,
                      // journal_invoice.getId(), true);
                      // canInsertActualUniqueCode = uniqueCode.getId();
                      // }
                      //
                      // }
                    }
                  }

                } else {
                  // we need to check functional consultant for further flow ,as of now we have only
                  // one expense unique code associated per purchase invoice
                }

              }
            }

            // Update the credit and debit amount from journal line in budget enquiry actual amount
            for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
              AccountingCombination uniqueCode = line.getAccountingCombination();
              if (uniqueCode.getEfinDimensiontype().equals("E")) {

                if (!uniqueCode.isEFINDepartmentFund()) {
                  EfinBudgetInquiry budgetInquiry = ManualEncumbaranceSubmitDAO.getBudgetInquiry(
                      uniqueCode.getId(), journal.getEFINBudgetDefinition().getId());
                  if (budgetInquiry != null) {
                    if (line.getCredit().compareTo(new BigDecimal("0")) > 0) {
                      budgetInquiry
                          .setSpentAmt(budgetInquiry.getSpentAmt().subtract(line.getCredit()));
                      OBDal.getInstance().save(budgetInquiry);
                    } else {
                      budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(line.getDebit()));
                      OBDal.getInstance().save(budgetInquiry);
                    }
                    OBDal.getInstance().flush();

                  } else {
                    // Get Parent Id for new budget Inquiry
                    EfinBudgetInquiry parentInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                        line.getAccountingCombination(),
                        line.getJournalEntry().getEFINBudgetDefinition());

                    EfinBudgetInquiry budInq = OBProvider.getInstance()
                        .get(EfinBudgetInquiry.class);
                    budInq.setEfinBudgetint(line.getJournalEntry().getEFINBudgetDefinition());
                    budInq.setAccountingCombination(line.getAccountingCombination());
                    budInq.setUniqueCodeName(line.getEfinUniqueCode());
                    budInq.setUniqueCode(line.getAccountingCombination().getEfinUniqueCode());
                    budInq.setOrganization(line.getOrganization());
                    budInq.setDepartment(line.getSalesRegion());
                    budInq.setAccount(line.getEfinAccount());
                    budInq.setSalesCampaign(line.getSalesCampaign());
                    budInq.setProject(line.getProject());
                    budInq.setBusinessPartner(line.getBusinessPartner());
                    budInq.setFunctionalClassfication(line.getActivity());
                    budInq.setFuture1(line.getStDimension());
                    budInq.setNdDimension(line.getNdDimension());
                    budInq.setORGAmt(BigDecimal.ZERO);
                    budInq.setREVAmount(BigDecimal.ZERO);
                    budInq.setFundsAvailable(BigDecimal.ZERO);
                    budInq.setCurrentBudget(BigDecimal.ZERO);
                    budInq.setEncumbrance(BigDecimal.ZERO);
                    if (line.getCredit().compareTo(new BigDecimal("0")) > 0) {
                      budInq.setSpentAmt(line.getCredit().negate());
                    } else {
                      budInq.setSpentAmt(line.getDebit());
                    }

                    budInq.setParent(parentInq);
                    budInq.setVirtual(true);
                    OBDal.getInstance().save(budInq);

                  }

                }

                EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils
                    .getBudgetInquiry(uniqueCode, journal.getEFINBudgetDefinition());
                if (budgetInq != null) {
                  if (line.getCredit().compareTo(new BigDecimal("0")) > 0) {
                    budgetInq.setSpentAmt(budgetInq.getSpentAmt().subtract(line.getCredit()));
                    OBDal.getInstance().save(budgetInq);
                  } else {
                    budgetInq.setSpentAmt(budgetInq.getSpentAmt().add(line.getDebit()));
                    OBDal.getInstance().save(budgetInq);
                  }
                  OBDal.getInstance().flush();

                }

              }
            }

            // insert record in actual transaction table
            for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
              AccountingCombination uniqueCode = line.getAccountingCombination();
              GLJournal glheader = line.getJournalEntry();
              if (uniqueCode != null && uniqueCode.getEfinDimensiontype() != null
                  && uniqueCode.getEfinDimensiontype().equals("E")) {
                ActualTransaction actualTran = OBProvider.getInstance()
                    .get(ActualTransaction.class);
                actualTran.setOrganization(journal.getOrganization());
                actualTran.setAccountingDate(glheader.getAccountingDate());
                actualTran.setInvoiceDate(glheader.getDocumentDate());
                actualTran.setDescription(line.getDescription());
                actualTran.setJournalLine(line);
                actualTran.setJournalEntry(glheader);
                actualTran.setDocumentNo(glheader.getDocumentNo());
                actualTran.setAccountingCombination(uniqueCode);
                actualTran.setBudgetInitialization(glheader.getEFINBudgetDefinition());
                actualTran.setPost(true);
                if (line.getCredit().compareTo(new BigDecimal("0")) > 0) {
                  actualTran.setAmount(line.getCredit().multiply(new BigDecimal("-1")));
                } else {
                  actualTran.setAmount(line.getDebit());
                }
                OBDal.getInstance().save(actualTran);
              }

            }

            OBDal.getInstance().flush();
            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_posted_gl@");
            bundle.setResult(result);
            return;
          }
        } else {
          msg.setType("Error");
          msg.setTitle("@Error@");
          msg.setMessage("@Efin_GLJourLine_Failed@");
        }
        bundle.setResult(msg);
      }
    } catch (

    Exception e) {
      bundle.setResult(OBErrorBuilder.buildMessage(null, "success", "@HB_INTERNAL_ERROR@"));
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public AcctServer get(String AD_Table_ID, String AD_Client_ID, String AD_Org_ID, String Record_ID,
      String strDocTypeId, ConnectionProvider connectionProvider) throws ServletException {
    AcctServer acct = null;
    if (log.isDebugEnabled())
      log.debug("get - table: " + AD_Table_ID);
    if (AD_Table_ID.equals("D1A97202E832470285C9B1EB026D54E2")) {
      AD_Table_ID = "100";
    }
    if (AD_Table_ID.equals("4D8C3B3C31D1410DA046140C9F024D17")) {
      AD_Table_ID = "101";
    }
    if (AD_Table_ID.equals("B1B7075C46934F0A9FD4C4D0F1457B42")) {
      AD_Table_ID = "102";
    }
    if (AD_Table_ID.equals("318") || AD_Table_ID.equals("800060") || AD_Table_ID.equals("800176")
        || AD_Table_ID.equals("407") || AD_Table_ID.equals("392") || AD_Table_ID.equals("259")
        || AD_Table_ID.equals("800019") || AD_Table_ID.equals("319") || AD_Table_ID.equals("321")
        || AD_Table_ID.equals("323") || AD_Table_ID.equals("325") || AD_Table_ID.equals("224")
        || AD_Table_ID.equals("472") || AD_Table_ID.equals("800168") || AD_Table_ID.equals("100")
        || AD_Table_ID.equals("101") || AD_Table_ID.equals("102")) {
      switch (Integer.parseInt(AD_Table_ID)) {
      case 100:
        acct = new DocFINPayment(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "FIN_Payment";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "D1A97202E832470285C9B1EB026D54E2";
        acct.Record_ID = Record_ID;
        // acct.C_DocType_ID=strDocTypeId;
        acct.reloadAcctSchemaArray();
        break;
      case 101:
        acct = new DocFINFinAccTransaction(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "FIN_Finacc_Transaction";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "4D8C3B3C31D1410DA046140C9F024D17";
        acct.Record_ID = Record_ID;
        // acct.C_DocType_ID=strDocTypeId;
        acct.reloadAcctSchemaArray();
        break;
      case 102:
        acct = new DocFINReconciliation(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "FIN_Reconciliation";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "B1B7075C46934F0A9FD4C4D0F1457B42";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 318:
        acct = new DocInvoice(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Invoice";
        acct.AD_Table_ID = "318";
        acct.strDateColumn = "DateAcct";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        acct.groupLines = getGroupLines(AD_Client_ID);
        break;
      /*
       * case 390: acct = new DocAllocation (AD_Client_ID); acct.strDateColumn = "";
       * acct.AD_Table_ID = "390"; acct.reloadAcctSchemaArray(); acct.break;
       */
      case 800060:
        acct = new DocAmortization(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "A_Amortization";
        acct.AD_Table_ID = "800060";
        acct.strDateColumn = "DateAcct";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;

      case 800176:
        if (log.isDebugEnabled())
          log.debug("AcctServer - Get DPM");
        acct = new DocDPManagement(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_DP_Management";
        acct.AD_Table_ID = "800176";
        acct.strDateColumn = "DateAcct";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 407:
        acct = new DocCash(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Cash";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "407";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 392:
        acct = new DocBank(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Bankstatement";
        acct.strDateColumn = "StatementDate";
        acct.AD_Table_ID = "392";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 259:
        acct = new DocOrder(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Order";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "259";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 800019:
        acct = new DocPayment(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Settlement";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "800019";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 319:
        acct = new DocInOut(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_InOut";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "319";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 321:
        acct = new DocInventory(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Inventory";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "321";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 323:
        acct = new DocMovement(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Movement";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "323";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 325:
        acct = new DocProduction(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Production";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "325";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 224:
        if (log.isDebugEnabled())
          log.debug("AcctServer - Before OBJECT CREATION");
        acct = new DocGLJournal(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "GL_Journal";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "224";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 472:
        acct = new DocMatchInv(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_MatchInv";
        acct.strDateColumn = "DateTrx";
        acct.AD_Table_ID = "472";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 800168:
        acct = new DocInternalConsumption(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Internal_Consumption";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "800168";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      // case 473: acct = new
      // DocMatchPO (AD_Client_ID); acct.strDateColumn = "MovementDate";
      // acct.reloadAcctSchemaArray(); break; case DocProjectIssue.AD_TABLE_ID: acct = new
      // DocProjectIssue (AD_Client_ID); acct.strDateColumn = "MovementDate";
      // acct.reloadAcctSchemaArray(); break;

      }
    }

    if (acct == null)
      log.warn("AcctServer - get - Unknown AD_Table_ID=" + AD_Table_ID);
    else if (log.isDebugEnabled())
      log.debug("AcctServer - get - AcctSchemaArray length=" + (acct.m_as).length);
    if (log.isDebugEnabled())
      log.debug("AcctServer - get - AD_Table_ID=" + AD_Table_ID);
    return acct;
  }

  public String getGroupLines(String AD_Client_ID) {
    String GroupLines = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn
          .prepareStatement(" SELECT GROUPACCTINVLINES FROM AD_CLIENTINFO WHERE AD_CLIENT_ID = '"
              + AD_Client_ID + "'");
      rs = ps.executeQuery();
      if (rs.next()) {
        GroupLines = rs.getString("groupacctinvlines");
      }
    } catch (Exception e) {
      log.debug("error in GetGroupLines of EfinReportNotPosted", e);
      // TODO: handle exception
    }
    return GroupLines;
  }

}
