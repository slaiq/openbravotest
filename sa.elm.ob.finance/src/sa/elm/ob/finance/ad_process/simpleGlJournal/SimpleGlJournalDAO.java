package sa.elm.ob.finance.ad_process.simpleGlJournal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.service.db.CallStoredProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.util.FinanceUtils;

public class SimpleGlJournalDAO {

  private final static Logger log = LoggerFactory.getLogger(SimpleGlJournalDAO.class);

  /**
   * method to insert approval history in the specified tables
   * 
   * @param data
   *          {@link JSONObject} containing the data like the approval action and the next performer
   *          etc.
   * @return The count of the inserted lines.
   */
  public static int glJournalHistory(JSONObject data) {
    int count = 0;

    try {
      StringBuilder queryBuilder = new StringBuilder();
      String historyId = SequenceIdData.getUUID();
      String strTableName = data.getString("HistoryTable");

      queryBuilder.append(" INSERT INTO  ").append(strTableName);
      queryBuilder.append(" ( ").append(strTableName.concat("_id"))
          .append(", ad_client_id, ad_org_id,");
      queryBuilder.append(" createdby, updatedby,   ").append(data.getString("HeaderColumn"))
          .append(" , approveddate, ");
      queryBuilder.append(data.getString("ActionColumn"))
          .append(" , pendingapproval, ad_user_id,ad_role_id,Status)");
      queryBuilder.append(" VALUES (?, ?, ?, ");
      queryBuilder.append(" ?, ?,?, ?,");
      queryBuilder.append(" ?, ?, ?, ?,?);");

      PreparedStatement query = OBDal.getInstance().getConnection()
          .prepareStatement(queryBuilder.toString());

      query.setString(1, historyId);
      query.setString(2, data.getString("ClientId"));
      query.setString(3, data.getString("OrgId"));
      query.setString(4, data.getString("UserId"));
      query.setString(5, data.getString("UserId"));
      query.setString(6, data.getString("HeaderId"));
      query.setDate(7, new java.sql.Date(System.currentTimeMillis()));
      query.setString(8, data.getString("Status"));
      query.setString(9, data.optString("NextApprover"));
      query.setString(10, data.getString("UserId"));
      query.setString(11, data.optString("RoleId"));
      query.setString(12, data.optString("Status"));
      log.debug("History Query: " + query.toString());

      count = query.executeUpdate();
    } catch (Exception e) {
      count = 0;
      log.error("Exception while glJournalHistory(): " + e);
    }
    return count;
  }

  /**
   * 
   * @param org
   * @return
   */
  public static String getCalenderId(Organization org) {
    String CalendarId = null;
    try {
      if (org.getCalendar() != null) {
        CalendarId = org.getCalendar().getId();

      } else {

        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
            "select eut_parent_org ('" + org.getId() + "','" + org.getClient().getId() + "')");
        Object parentOrg = query.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();
            break;
          }
        }

      }
    } catch (Exception e) {
      log.error("Exception while getCalenderId(): " + e);
    }
    return CalendarId;
  }

  /**
   * 
   * @param strJournalId
   * @return
   */
  public static boolean chkDebitCreditAmt(String strJournalId) {
    boolean chkDebitCreditAmt = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      /* Debit and credit amounts do not match */
      sql = " select totaldr, totalcr,controlamt from gl_journal where gl_journal_id= '"
          + strJournalId + "'";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (rs.getBigDecimal("totaldr").compareTo(rs.getBigDecimal("totalcr")) > 0
            || rs.getBigDecimal("totaldr").compareTo(rs.getBigDecimal("totalcr")) < 0) {
          chkDebitCreditAmt = true;
        }
      }

    } catch (Exception e) {
      log.error("Exception chkDebitCreditAmt: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return chkDebitCreditAmt;
  }

  /**
   * 
   * @param objJournal
   * @return
   */
  public static boolean PeriodValidation(GLJournal objJournal) {
    boolean PeriodValidation = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = " select count(*) as count from c_period p where p.c_period_id= '"
          + objJournal.getPeriod().getId() + "' and  '" + objJournal.getAccountingDate()
          + "' between p.startdate and p.enddate ";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") < 1)
          PeriodValidation = true;
      }

    } catch (Exception e) {
      log.error("Exception PeriodValidation: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return PeriodValidation;
  }

  /**
   * 
   * @param strJournalId
   * @return
   */
  public static boolean chkOrgHeaderNotReady(String strJournalId) {
    boolean chkOrgHeaderNotReady = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = " select ad_org.isready as ready , ad_orgtype.istransactionsallowed as transactionallowed from gl_journal, ad_org,ad_orgtype where ad_org.ad_org_id=gl_journal.ad_org_id and ad_org.ad_orgtype_id=ad_orgtype.ad_orgtype_id"
          + " and gl_journal.gl_journal_id='" + strJournalId + "'";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (rs.getString("ready").equals("N"))
          chkOrgHeaderNotReady = true;

      }

    } catch (Exception e) {
      log.error("Exception chkOrgHeaderNotReady: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return chkOrgHeaderNotReady;
  }

  /**
   * 
   * @param strJournalId
   * @return
   */
  public static boolean chkOrgHeaderNotTransAllowed(String strJournalId) {
    boolean chkOrgHeaderNotTransAllowed = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = " select ad_org.isready as ready , ad_orgtype.istransactionsallowed as transactionallowed from gl_journal, ad_org,ad_orgtype where ad_org.ad_org_id=gl_journal.ad_org_id and ad_org.ad_orgtype_id=ad_orgtype.ad_orgtype_id"
          + " and gl_journal.gl_journal_id='" + strJournalId + "'";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {

        if (rs.getString("transactionallowed").equals("N"))
          chkOrgHeaderNotTransAllowed = true;
      }

    } catch (Exception e) {
      log.error("Exception chkOrgHeaderNotTransAllowed: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return chkOrgHeaderNotTransAllowed;
  }

  /**
   * 
   * @param strJournalId
   * @return
   */
  public static boolean chkOrganization(String strJournalId) {
    boolean chkOrganization = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = " select ad_org_chk_documents('GL_JOURNAL', 'GL_JOURNALLINE', '" + strJournalId
          + "','GL_JOURNAL_ID', 'GL_JOURNAL_ID')  as isincluded from dual ";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (rs.getInt("isincluded") == -1)
          chkOrganization = true;
      }

    } catch (Exception e) {
      log.error("Exception chkOrganization: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return chkOrganization;
  }

  /**
   * 
   * @param objJournal
   * @return
   */
  public static boolean chkForcedOrganization(GLJournal objJournal) {
    boolean chkForcedOrganization = false;
    PreparedStatement ps = null, ps1 = null;
    ResultSet rs = null, rs1 = null;
    String sql1 = "", sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = " SELECT gll.ad_org_id, gll.ad_client_id   FROM gl_journalline gll  WHERE gll.gl_journal_id = '"
          + objJournal.getId() + "'";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      while (rs.next()) {
        sql1 = " select AD_ISORGINCLUDED('" + rs.getString("ad_org_id") + "','"
            + objJournal.getOrganization().getId() + "','" + rs.getString("ad_client_id")
            + "') as isincluded from dual ";
        ps1 = conn.prepareStatement(sql1);
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          if (rs1.getInt("isincluded") == -1)
            chkForcedOrganization = true;
        } else
          continue;
      }

    } catch (Exception e) {
      log.error("Exception chkForcedOrganization: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
        if (rs1 != null)
          rs1.close();
        if (ps1 != null)
          ps1.close();
      } catch (Exception e) {
      }
    }
    return chkForcedOrganization;
  }

  /**
   * 
   * @param objJournal
   * @return
   */
  public static boolean chkBatchOrg(GLJournal objJournal) {
    boolean chkBatchOrg = false;
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = " SELECT COALESCE(gl_journalbatch.ad_org_id, gl_journal.ad_org_id) as batchorgId from  gl_journal LEFT JOIN gl_journalbatch  ON gl_journal.gl_journalbatch_id = gl_journalbatch.gl_journalbatch_id   WHERE gl_journal.gl_journal_id =  '"
          + objJournal.getId() + "'";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (!rs.getString("batchorgId").equals(objJournal.getOrganization().getId()))
          chkBatchOrg = true;
      }

    } catch (Exception e) {
      log.error("Exception chkBatchOrg: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
    return chkBatchOrg;
  }

  /**
   * 
   * @param objJournal
   * @return
   */
  public static boolean chkPeriodAvailable(GLJournal objJournal) {
    boolean chkPeriodAvailable = false;
    PreparedStatement ps = null, ps1 = null, ps2 = null;
    ResultSet rs = null, rs1 = null, rs2 = null;
    String sql1 = "", sql2 = "", sql = "";
    Connection conn = OBDal.getInstance().getConnection();
    try {

      sql = "  SELECT AD_GET_DOC_LE_BU('GL_JOURNAL', '" + objJournal.getId()
          + "', 'GL_JOURNAL_ID', 'LE') as orgbuleId from dual";
      ps = conn.prepareStatement(sql);
      rs = ps.executeQuery();
      if (rs.next()) {
        sql1 = "  SELECT AD_OrgType.IsAcctLegalEntity as isacctle  FROM AD_OrgType, AD_Org  WHERE AD_Org.AD_OrgType_ID = AD_OrgType.AD_OrgType_ID  AND AD_Org.AD_Org_ID='"
            + rs.getString("orgbuleId") + "'";
        ps1 = conn.prepareStatement(sql1);
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          if (rs1.getString("isacctle").equals("Y")) {
            sql2 = "   SELECT C_CHK_OPEN_PERIOD_GLJ('" + objJournal.getOrganization().getId()
                + "', '" + objJournal.getPeriod().getId() + "') as availableperiod from dual";
            ps2 = conn.prepareStatement(sql2);
            rs2 = ps2.executeQuery();
            if (rs2.next()) {
              if (rs2.getInt("availableperiod") != 1) {
                chkPeriodAvailable = true;
              }
            }
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception chkPeriodAvailable: " + e);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
        if (rs1 != null)
          rs1.close();
        if (ps1 != null)
          ps1.close();
        if (rs2 != null)
          rs2.close();
        if (ps2 != null)
          ps2.close();
      } catch (Exception e) {
      }
    }
    return chkPeriodAvailable;
  }

  /**
   * This method is used to update applied amount to used amount
   * 
   * @param journal
   * @param conversionrate
   * @param isRevoke
   */
  public static void updateAppliedAmountToUsedAmount(GLJournal journal, BigDecimal conversionrate,
      Boolean isRevoke) {
    try {
      BigDecimal covertedAmount = BigDecimal.ZERO;
      String validCombination = "";
      String expenseaccnt = "";
      InvoiceLine accComObj = null;
      if (journal.getFinancialMgmtGLJournalLineList().size() > 0) {
        EfinBudgetManencumlines encumbranceLine = null;
        for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
          if (line.getCredit().compareTo(BigDecimal.ZERO) > 0) {
            validCombination = line.getAccountingCombination().getId();
            covertedAmount = covertedAmount.add(line.getCredit());
          }
        }
        covertedAmount = FinanceUtils.getConvertedAmount(covertedAmount, conversionrate);
        List<InvoiceLine> accCom = new ArrayList<InvoiceLine>();

        OBQuery<InvoiceLine> query = OBDal.getInstance().createQuery(InvoiceLine.class,
            "as e where e.invoice.id=:invoiceId and e.efinCValidcombination.id=:expenseactId");
        query.setNamedParameter("invoiceId", journal.getEfinCInvoice().getId());
        query.setNamedParameter("expenseactId", validCombination);
        if (query != null) {
          accCom = query.list();
          if (accCom.size() > 0) {
            accComObj = accCom.get(0);
            expenseaccnt = accComObj.getEfinExpenseAccount().getId();
          }
        }

        encumbranceLine = getEncumbranceLine(
            journal.getEfinCInvoice().getEfinManualencumbrance().getId(), expenseaccnt);

        if (!isRevoke) {
          encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().subtract(covertedAmount));
          encumbranceLine.setUsedAmount(encumbranceLine.getUsedAmount().add(covertedAmount));
        } else {
          encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(covertedAmount));
          encumbranceLine.setUsedAmount(encumbranceLine.getUsedAmount().subtract(covertedAmount));
        }

        OBDal.getInstance().save(encumbranceLine);
      }

      OBDal.getInstance().flush();

    } catch (

    Exception e) {
      log.error("Exception in updateAppliedAmountToUsedAmount :", e);
    }
  }

  /**
   * This method is used to get encumbrace line
   * 
   * @param manualEncumbranceId
   * @param validCombination
   * @return
   */
  public static EfinBudgetManencumlines getEncumbranceLine(String manualEncumbranceId,
      String validCombination) {
    EfinBudgetManencumlines line = null;
    List<EfinBudgetManencumlines> lines = new ArrayList<EfinBudgetManencumlines>();
    try {
      OBQuery<EfinBudgetManencumlines> query = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " where manualEncumbrance.id = :manualEncumbrance and accountingCombination.id = :validCombination ");

      query.setNamedParameter("manualEncumbrance", manualEncumbranceId);
      query.setNamedParameter("validCombination", validCombination);

      if (query != null) {
        lines = query.list();
        if (lines.size() > 0) {
          line = lines.get(0);
        }
      }

    } catch (Exception e) {
      log.error("Exception in updateAppliedAmountToUsedAmount :", e);
    }
    return line;
  }

  /**
   * This method is sued to update prepayment used amount
   * 
   * @param strjournalId
   * @param conversionrate
   * @param isRevoke
   */
  @SuppressWarnings("unchecked")
  public static void updatePrepaymentUsedAmount(String strjournalId, BigDecimal conversionrate,
      Boolean isRevoke) {
    try {
      String sql = "";
      List<Object> prepayments = new ArrayList<Object>();
      sql = " select AmtSourceDr,apppay.em_efin_c_invoice_id  from gl_journal apppay "
          + "          join gl_journalline line on line.gl_journal_id = apppay.gl_journal_id "
          + "          join c_invoice appinv on apppay.em_efin_c_invoice_id=appinv.c_invoice_id "
          + "           where  apppay.gl_journal_id='" + strjournalId
          + "' and line.AmtSourceDr > 0";

      SQLQuery ps = OBDal.getInstance().getSession().createSQLQuery(sql);
      if (ps != null) {
        prepayments = ps.list();

        if (prepayments.size() > 0) {
          for (Object prepayment : prepayments) {
            Object[] details = (Object[]) prepayment;

            Invoice apprepayInvoice = OBDal.getInstance().get(Invoice.class, details[1].toString());

            BigDecimal appliedAmt = new BigDecimal(details[0].toString());
            BigDecimal EfinPreRemainAmt = apprepayInvoice.getEfinPreRemainingamount();

            if (!isRevoke) {
              apprepayInvoice
                  .setEfinPreUsedamount(apprepayInvoice.getEfinPreUsedamount().add(appliedAmt));
              apprepayInvoice.setEfinPreRemainingamount(EfinPreRemainAmt.subtract(appliedAmt));
            } else {
              apprepayInvoice.setEfinPreUsedamount(
                  apprepayInvoice.getEfinPreUsedamount().subtract(appliedAmt));
              apprepayInvoice.setEfinPreRemainingamount(EfinPreRemainAmt.add(appliedAmt));
            }

            OBDal.getInstance().save(apprepayInvoice);

          }
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      log.error("Exception in updatePrepaymentUsedAmount :", e);
    }
  }

  /**
   * This method is used to check applied amount
   * 
   * @param invoice
   * @param validcombinationId
   * @param lineDebitAmt
   * @return
   */
  public static boolean checkAppliedAmt(Invoice invoice, String validcombinationId,
      BigDecimal lineDebitAmt) {
    String Sql = "";
    BigDecimal debitAmt = new BigDecimal(0);
    BigDecimal appAmt = new BigDecimal(0);
    boolean chkAmt = false;
    try {
      Invoice prePayInv = OBDal.getInstance().get(Invoice.class, invoice.getId());
      Sql = "select sum(amtacctdr) as debitAmt,c_validcombination_id from gl_journalline join gl_journal on "
          + "gl_journalline.gl_journal_id = gl_journal.gl_journal_id "
          + "where gl_journal.em_efin_c_invoice_id = '" + invoice.getId() + "' "
          + "and gl_journal.docstatus!='DR'  and gl_journalline.amtacctdr > 0 "
          + "group by gl_journal.em_efin_c_invoice_id,gl_journalline.c_validcombination_id";
      SQLQuery st1 = OBDal.getInstance().getSession().createSQLQuery(Sql);
      log.debug("application  :" + st1.toString());
      if (st1 != null && st1.list().size() > 0) {
        Object[] row = (Object[]) st1.list().get(0);
        debitAmt = (BigDecimal) row[0];
      }
      debitAmt = debitAmt.add(lineDebitAmt);
      Sql = " select sum(linenetamt) as amt from c_invoiceline  where c_invoice_id ='"
          + invoice.getId() + "'" + "             group by c_invoice_id ";
      SQLQuery st2 = OBDal.getInstance().getSession().createSQLQuery(Sql);
      log.debug("application1  :" + st2.toString());
      if (st2 != null && st2.list().size() > 0) {
        appAmt = (BigDecimal) st2.list().get(0);
      }
      if (debitAmt.compareTo(appAmt) > 0) {
        chkAmt = true;
      }

      // OBQuery<org.openbravo.model.common.invoice.InvoiceLine> line = OBDal.getInstance()
      // .createQuery(org.openbravo.model.common.invoice.InvoiceLine.class,
      // "as e where e.invoice.id = :invoiceId and e.efinExpenseAccount.id = :validCombinationId");
      // line.setNamedParameter("invoiceId", prePayInv.getId());
      // line.setNamedParameter("validCombinationId", validcombinationId);
      // if (line != null && line.list().size() > 0) {
      // org.openbravo.model.common.invoice.InvoiceLine manualLine = line.list().get(0);
      // appAmt = manualLine.getLineNetAmount();
      // }

    }

    catch (final Exception e) {
      log.error("Exception in checkApplicationInvoiceAmount() Method : ", e);

    }
    return chkAmt;

  }

  /**
   * This method is used to check applied amount using credit
   * 
   * @param invoice
   * @param validcombinationId
   * @param lineCreditAmt
   * @return
   */
  public static boolean checkAppliedAmtUsingCredit(Invoice invoice, String validcombinationId,
      BigDecimal lineCreditAmt) {
    String Sql = "";
    BigDecimal creditAmt = new BigDecimal(0);
    BigDecimal appAmt = new BigDecimal(0);
    boolean chkAmt = false;
    try {
      Invoice prePayInv = OBDal.getInstance().get(Invoice.class, invoice.getId());
      Sql = "select sum(amtacctcr) as creditAmt,c_validcombination_id from gl_journalline join gl_journal on "
          + "gl_journalline.gl_journal_id = gl_journal.gl_journal_id "
          + "where gl_journal.em_efin_c_invoice_id = '" + invoice.getId() + "' "
          + "and gl_journal.docstatus!='DR'  and gl_journalline.amtacctcr > 0 "
          + "and gl_journalline.c_validcombination_id ='" + validcombinationId + "'"
          + "group by gl_journal.em_efin_c_invoice_id,gl_journalline.c_validcombination_id";
      SQLQuery st1 = OBDal.getInstance().getSession().createSQLQuery(Sql);
      log.debug("application  :" + st1.toString());
      if (st1 != null && st1.list().size() > 0) {
        Object[] row = (Object[]) st1.list().get(0);
        creditAmt = (BigDecimal) row[0];
      }
      creditAmt = creditAmt.add(lineCreditAmt);
      OBQuery<org.openbravo.model.common.invoice.InvoiceLine> line = OBDal.getInstance()
          .createQuery(org.openbravo.model.common.invoice.InvoiceLine.class,
              "as e where e.invoice.id = :invoiceId and e.efinCValidcombination.id = :validCombinationId");
      line.setNamedParameter("invoiceId", prePayInv.getId());
      line.setNamedParameter("validCombinationId", validcombinationId);
      if (line != null && line.list().size() > 0) {
        org.openbravo.model.common.invoice.InvoiceLine manualLine = line.list().get(0);
        appAmt = manualLine.getLineNetAmount();
      }
      if (creditAmt.compareTo(appAmt) > 0) {
        chkAmt = true;
      }
    }

    catch (final Exception e) {
      log.error("Exception in checkApplicationInvoiceAmount() Method : ", e);

    }
    return chkAmt;

  }

  /**
   * Create Temporary Encumbrance at reserving role approval in GL Journal
   * 
   * @param glJournal
   * @param glLineList
   * @return
   */
  public static void insertTemporaryEncumbrance(GLJournal glJournal,
      List<GLJournalLine> glLineList) {
    EfinBudgetManencum manual = null;
    EfinBudgetManencumlines lines = null;
    long lineno = 10;
    SalesRegion salesRegion = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, " as e where e.client.id = :clientID");
      controlParam.setNamedParameter("clientID", glJournal.getClient().getId());
      controlParam.setMaxResult(1);
      if (controlParam != null) {
        List<EfinBudgetControlParam> controlParamList = controlParam.list();
        if (controlParamList.size() > 0) {
          salesRegion = controlParamList.get(0).getBudgetcontrolunit();
        }
      }

      // Create encumbrance header
      manual = OBProvider.getInstance().get(EfinBudgetManencum.class);
      manual.setOrganization(glJournal.getOrganization());
      manual.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      manual.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));

      manual.setEncumType("TE");
      manual.setEncumMethod("A");

      // Get funds budget type id
      OBQuery<Campaign> campaignQry = OBDal.getInstance().createQuery(Campaign.class,
          " as e where e.client.id = :clientID and efinBudgettype = :fundsType ");
      campaignQry.setNamedParameter("clientID", glJournal.getClient().getId());
      campaignQry.setNamedParameter("fundsType", "F");
      campaignQry.setMaxResult(1);
      if (campaignQry != null) {
        List<Campaign> campaignList = campaignQry.list();
        if (campaignList.size() > 0) {
          manual.setSalesCampaign(campaignList.get(0));
        }
      }
      manual.setBudgetInitialization(glJournal.getEFINBudgetDefinition());
      manual.setSalesRegion(salesRegion);
      manual.setDocumentStatus("CO");
      manual.setAction("PD");
      manual.setDescription(glJournal.getDocumentNo());
      OBDal.getInstance().save(manual);
      OBDal.getInstance().flush();

      // Create encumbrance lines
      for (GLJournalLine glLine : glLineList) {

        AccountingCombination validCombination = OBDal.getInstance()
            .get(AccountingCombination.class, glLine.getAccountingCombination().getId());

        BigDecimal decreaseAmount = glLine.getDebit();

        lines = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
        lines.setOrganization(manual.getOrganization());
        lines.setLineNo(lineno);
        lines.setAccountingCombination(validCombination);
        lines.setManualEncumbrance(manual);
        lines.setSalesRegion(validCombination.getSalesRegion());
        lines.setAccountElement(validCombination.getAccount());
        lines.setSalesCampaign(validCombination.getSalesCampaign());
        lines.setBusinessPartner(validCombination.getBusinessPartner());
        lines.setProject(validCombination.getProject());
        lines.setStDimension(validCombination.getStDimension());
        lines.setNdDimension(validCombination.getNdDimension());
        lines.setActivity(validCombination.getActivity());
        lines.setAmount(decreaseAmount);
        lines.setRevamount(decreaseAmount);
        lines.setRemainingAmount(decreaseAmount);
        OBDal.getInstance().save(lines);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(lines);

        // Budget inquiry is not updating by trigger because of type temporary encumbrance
        // So calling efin_updateBudgetInq procedure
        try {
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(lines.getAccountingCombination().getId());
          parameters.add(lines.getRevamount());
          parameters.add(manual.getBudgetInitialization().getId());
          final String procedureName = "efin_updateBudgetInq";
          CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
        } catch (Exception e) {
          throw new OBException(e);
        }

        lineno = lineno + 10;
      }
      glJournal.setEFINBudgetManencum(manual);
      OBDal.getInstance().save(glJournal);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(glJournal);

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertTemporaryEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in insertTemporaryEncumbrance " + e.getMessage());
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Remove temporary encumbrance in GlJournal
   * 
   * @param glJournal
   */
  public static void removeTemporaryEncumbrance(GLJournal glJournal) {

    try {
      if (glJournal.getEFINBudgetManencum() != null) {

        EfinBudgetManencum manualEncum = glJournal.getEFINBudgetManencum();

        // Budget inquiry is not updating by trigger because of type temporary encumbrance
        // So calling efin_updateBudgetInq procedure
        for (EfinBudgetManencumlines encumLines : manualEncum.getEfinBudgetManencumlinesList()) {
          try {
            final List<Object> parameters = new ArrayList<Object>();
            parameters.add(encumLines.getAccountingCombination().getId());
            parameters.add(encumLines.getRevamount().negate());
            parameters.add(manualEncum.getBudgetInitialization().getId());
            final String procedureName = "efin_updateBudgetInq";
            CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
          } catch (Exception e) {
            throw new OBException(e);
          }
        }

        manualEncum.setDocumentStatus("DR");
        OBDal.getInstance().save(manualEncum);

        for (EfinBudgetManencumlines encumLines : manualEncum.getEfinBudgetManencumlinesList()) {
          OBDal.getInstance().remove(encumLines);
        }

        glJournal.setEFINBudgetManencum(null);
        OBDal.getInstance().save(glJournal);
        OBDal.getInstance().remove(manualEncum);
        OBDal.getInstance().flush();
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while removeTemporaryEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in removeTemporaryEncumbrance " + e.getMessage());
      throw new OBException(e.getMessage());
    }
  }

  /**
   * 
   * @param journal_invoice
   * @return return the list of expense unique code associated with the invoice
   * 
   */
  public static List<String> findOutExpenseUniqueCodeFromInvoice(Invoice journal_invoice) {
    List<String> uniquecodeList = new ArrayList<String>();
    try {
      OBQuery<InvoiceLine> invoiceLineListQuery = OBDal.getInstance().createQuery(InvoiceLine.class,
          " as e where e.invoice.id=:invoiceId ");
      invoiceLineListQuery.setNamedParameter("invoiceId", journal_invoice.getId());
      if (invoiceLineListQuery.list().size() > 0) {
        for (InvoiceLine invoice_line : invoiceLineListQuery.list()) {
          if (uniquecodeList.size() == 0) {
            uniquecodeList.add(invoice_line.getEfinExpenseAccount().getId());
          } else if (uniquecodeList.size() > 0
              && !uniquecodeList.contains(invoice_line.getEfinExpenseAccount().getId())) {
            uniquecodeList.add(invoice_line.getEfinExpenseAccount().getId());
          }
        }
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while findOutExpenseUniqueCodeFromInvoice: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in findOutExpenseUniqueCodeFromInvoice " + e.getMessage());
      throw new OBException(e.getMessage());
    }
    return uniquecodeList;
  }

  /**
   * Insert modification on encumbrance
   * 
   * @param encumbrance_decrease_amount
   * @param efinManualencumbrance
   */
  public static void insertEncumbranceModification(BigDecimal encumbrance_decrease_amount,
      EfinBudgetManencumv efinManualencumbrance, String uniqueCode, String gl_documentno) {

    try {
      EfinBudgetManencumlines objEncumLine = null;
      // get encumbrance details
      OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
      manline.setNamedParameter("encumID", efinManualencumbrance.getId());
      manline.setNamedParameter("accID", uniqueCode);

      manline.setMaxResult(1);
      if (manline.list().size() > 0) {
        objEncumLine = manline.list().get(0);
        objEncumLine.setUsedAmount(
            objEncumLine.getUsedAmount().subtract(encumbrance_decrease_amount.negate()));
        OBDal.getInstance().save(objEncumLine);
        OBDal.getInstance().flush();

        EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
        if (!StringUtils.isEmpty(objEncumLine.getId())) {
          // insert into Manual Encumbrance Revision Table
          manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
          manEncumRev.setOrganization(
              OBDal.getInstance().get(Organization.class, objEncumLine.getOrganization().getId()));
          manEncumRev.setActive(true);
          manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
          manEncumRev.setCreationDate(new java.util.Date());
          manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
          manEncumRev.setUpdated(new java.util.Date());
          manEncumRev.setUniqueCode(objEncumLine.getUniquecode());
          manEncumRev.setManualEncumbranceLines(objEncumLine);
          manEncumRev.setRevdate(new Date());
          manEncumRev.setStatus("APP");
          manEncumRev.setRevamount(encumbrance_decrease_amount);
          manEncumRev.setEncumbranceType("MO");
          manEncumRev.setDescription("GL Number :" + gl_documentno);
          manEncumRev.setAuto(true);
          manEncumRev.setAccountingCombination(objEncumLine.getAccountingCombination());
          OBDal.getInstance().save(manEncumRev);
          OBDal.getInstance().flush();

        }
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertEncumbranceModification: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in insertEncumbranceModification " + e.getMessage());
      throw new OBException(e.getMessage());
    }

  }

  /**
   * Update the payment details in encumbrance invoice referece table
   * 
   * @param efinManualencumbrance
   * @param uniqueCodeid
   * @param encumbrance_decrease_amount
   * @param invoiceId
   */
  public static void moveReservedToActual(EfinBudgetManencumv efinManualencumbrance,
      String uniqueCodeId, BigDecimal encumbrance_decrease_amount, String invoiceId,
      Boolean isPaid) {
    try {
      EfinBudgetManencumlines objEncumLine = null;
      // get encumbrance details
      OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:accID ");
      manline.setNamedParameter("encumID", efinManualencumbrance.getId());
      manline.setNamedParameter("accID", uniqueCodeId);

      manline.setMaxResult(1);
      if (manline.list().size() > 0) {
        objEncumLine = manline.list().get(0);
        for (EfinManualEncumInvoice reservedInvoice : objEncumLine
            .getEfinManualEncumInvoiceList()) {
          if (reservedInvoice.getInvoice().getId().equals(invoiceId)) {
            reservedInvoice.setPaymentComplete(isPaid);
            OBDal.getInstance().save(reservedInvoice);
          }
        }

      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while moveReservedToActual: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in moveReservedToActual " + e.getMessage());
      throw new OBException(e.getMessage());
    }

  }
}
