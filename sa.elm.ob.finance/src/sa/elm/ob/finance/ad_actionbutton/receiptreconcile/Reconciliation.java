/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package sa.elm.ob.finance.ad_actionbutton.receiptreconcile;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.APRM_FinaccTransactionV;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.financialmgmt.payment.FIN_ReconciliationLine_v;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.finance.ReconciliationLine;
import sa.elm.ob.utility.util.UtilityDAO;

public class Reconciliation extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private AdvPaymentMngtDao dao;
  Set<FIN_FinaccTransaction> transactionsToBePosted = new HashSet<FIN_FinaccTransaction>();

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    SimpleDateFormat dteFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    if (vars.commandIn("DEFAULT")) {
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Reconciliation|Org");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Reconciliation|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Reconciliation|tabId");
      String strFinancialAccountId = vars.getStringParameter("inpfinFinancialAccountId");
      final int accesslevel = 3;

      if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(),
          strTabId))
          || !(Utility.isElementInList(
              Utility.getContext(this, vars, "#User_Client", strWindowId, accesslevel),
              vars.getClient())
              && Utility.isElementInList(
                  Utility.getContext(this, vars, "#User_Org", strWindowId, accesslevel),
                  strOrgId))) {
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(),
            Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
        vars.setMessage(strTabId, myError);
        printPageClosePopUp(response, vars);
      } else {
        printPage(response, vars, strOrgId, strWindowId, strTabId, strFinancialAccountId, null,
            null);
      }

    } else if (vars.commandIn("GRID")) {
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strStatementDate = vars.getStringParameter("inpStatementDate");
      try {
        strStatementDate = dteFormat.format(dteFormat.parse(strStatementDate));
        strStatementDate = UtilityDAO.convertToGregorian(strStatementDate);
        strStatementDate = dteFormat.format(dateTimeFormat.parse(strStatementDate));
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        throw new OBException(e);
      }

      String strSortColumn = vars.getStringParameter("inpSortColumn");
      String strSortType = vars.getStringParameter("inpSortType");
      // String strValidMoF = vars.getStringParameter("inpValidMoF");
      boolean strAfterDate = "Y".equals(vars.getStringParameter("inpAfterDate"));
      String selectedTransactionsIds = vars.getStringParameter("inpSelectedTransactionId");
      String strCurrentlyCleared = vars.getNumericParameter("inpCalcCurrentlyCleared");
      String strTotalPayment = vars.getNumericParameter("inpCalcTotalPayment");
      String strTotalDeposit = vars.getNumericParameter("inpCalcTotalDeposit");

      printGrid(response, strFinancialAccountId, strStatementDate, strAfterDate,
          selectedTransactionsIds, strCurrentlyCleared, strTotalPayment, strTotalDeposit,
          strSortColumn, strSortType, "N");

    } else if (vars.commandIn("SAVE") || vars.commandIn("PROCESS")) {
      String strTabId = vars.getGlobalVariable("inpTabId", "Reconciliation|tabId");
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strDifference = vars.getNumericParameter("inpCalcDifference");
      String strStatementDate = vars.getStringParameter("inpStatementDate");
      String strDescription = vars.getStringParameter("inpComment");
      try {
        strStatementDate = dteFormat.format(dteFormat.parse(strStatementDate));
        strStatementDate = UtilityDAO.convertToGregorian(strStatementDate);
        strStatementDate = dteFormat.format(dateTimeFormat.parse(strStatementDate));
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        throw new OBException(e);
      }
      String strBeginBalance = vars.getNumericParameter("inpBeginBalance");
      String strEndBalance = vars.getNumericParameter("inpEndBalance");
      boolean process = vars.commandIn("PROCESS");
      processReconciliation(response, vars, strTabId, strFinancialAccountId, strDifference,
          strStatementDate, strBeginBalance, strEndBalance, process, strDescription);

    } else if (vars.commandIn("UPDATESTATUS")) {
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strSelectedTransId = vars.getStringParameter("inpCurrentTransIdSelected", "");
      boolean isChecked = "true".equals(vars.getStringParameter("inpIsCurrentTransSelected"));

      updateTransactionStatus(response, strFinancialAccountId, strSelectedTransId, isChecked);
    }

  }

  private void updateTransactionStatus(HttpServletResponse response, String strFinancialAccountId,
      String strSelectedTransId, boolean isChecked) {

    OBContext.setAdminMode();
    try {
      if (strSelectedTransId != "") {
        FIN_FinaccTransaction trans = OBDal.getInstance().get(FIN_FinaccTransaction.class,
            strSelectedTransId);
        String newStatus = "RPPC";
        if (!isChecked) {
          newStatus = (trans.getPaymentAmount().compareTo(trans.getDepositAmount()) >= 0) ? "PWNC"
              : "RDNC";
          trans.setReconciliation(null);
          if (trans.getFinPayment() != null) {
            trans.getFinPayment().setStatus((trans.getFinPayment().isReceipt()) ? "RDNC" : "PWNC");
          }
        } else {
          FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
              strFinancialAccountId);
          FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account, "N");
          trans.setReconciliation(reconciliation);
          if (trans.getFinPayment() != null) {
            trans.getFinPayment().setStatus("RPPC");
          }
        }
        trans.setStatus(newStatus);
        OBDal.getInstance().save(trans);
        OBDal.getInstance().flush();
      }
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println("");
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void processReconciliation(HttpServletResponse response, VariablesSecureApp vars,
      String strTabId, String strFinancialAccountId, String strDifference, String strStatementDate,
      String strBeginBalance, String strEndBalance, boolean process, String inpDescription)
      throws IOException, ServletException {

    log4j.debug(
        "Output: Process or Save button pressed on Financial Account || Transaction || Reconciliation manual window");

    dao = new AdvPaymentMngtDao();
    OBError msg = new OBError();
    String lineQuery = "";
    ResultSet rs = null;
    PreparedStatement ps = null;
    SimpleDateFormat dteFormat = new SimpleDateFormat("dd-MM-yyyy");
    Connection conn = OBDal.getInstance().getConnection();
    OBContext.setAdminMode();

    try {

      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strFinancialAccountId);

      FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account, "N");
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

      FIN_Reconciliation lastProcessedReconciliation = TransactionsDao
          .getLastReconciliation(account, "Y");
      log4j.debug("strStatementDate: on save>>" + strStatementDate);
      log4j.debug("afterConversion:strStatementDate>>" + FIN_Utility.getDateTime(strStatementDate));
      reconciliation.setEndingBalance(BigDecimal.ZERO);
      reconciliation.setTransactionDate(FIN_Utility.getDateTime(strStatementDate));
      reconciliation.setEndingDate(FIN_Utility.getDateTime(strStatementDate));
      reconciliation.setDocumentStatus("DR");
      reconciliation.setProcessed(false);
      reconciliation.setAPRMProcessReconciliation("P");
      reconciliation.setAprmProcessRec("P");
      reconciliation.setEfinDescription(inpDescription);
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();

      String strNpsNo = sa.elm.ob.utility.util.Utility.getGeneralSequence(
          dateFormat.format(reconciliation.getCreationDate()), "NPS",
          reconciliation.getOrganization().getCalendar().getId(),
          reconciliation.getOrganization().getId(), Boolean.TRUE);

      if ("0".equals(strNpsNo)) {
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
        msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(),
            OBMessageUtils.messageBD("Efin_NoPaymentSequence")));
        vars.setMessage(strTabId, msg);
        msg = null;
        printPageClosePopUpAndRefreshParent(response, vars);
        return;
      }

      if (process) { // Validations
        String strMessage = "";
        boolean raiseException = false;
        // Hided cleared Difference "Zero" validation
        /*
         * if (new BigDecimal(strDifference).compareTo(BigDecimal.ZERO) != 0) { strMessage =
         * "@APRM_ReconciliationDiscrepancy@" + " " + strDifference; raiseException = true; }
         */

        Calendar calCurrent = Calendar.getInstance();
        calCurrent.setTime(FIN_Utility.getDateTime(strStatementDate));
        log4j.debug("strStatementDate >>" + strStatementDate);
        if (lastProcessedReconciliation != null) {
          Calendar calLast = Calendar.getInstance();
          calLast.setTime(FIN_Utility
              .getDateTime(dteFormat.format(lastProcessedReconciliation.getEndingDate())));
          if (calCurrent.before(calLast)) {
            strMessage = "@APRM_ReconcileInFutureOrPast@";
            raiseException = true;
          }
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        tomorrow.setTime(DateUtils.truncate(tomorrow.getTime(), Calendar.DATE));
        if (calCurrent.after(tomorrow)) {
          strMessage = "@APRM_ReconcileInFutureOrPast@";
          raiseException = true;
        }
        if (raiseException) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
          msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
          vars.setMessage(strTabId, msg);
          msg = null;
          printPageClosePopUpAndRefreshParent(response, vars);
          return;
        }

        boolean orgLegalWithAccounting = FIN_Utility.periodControlOpened(
            FIN_Reconciliation.TABLE_NAME, reconciliation.getId(),
            FIN_Reconciliation.TABLE_NAME + "_ID", "LE");
        boolean documentEnabled = getDocumentConfirmation(this, reconciliation.getId());
        if (documentEnabled && !FIN_Utility.isPeriodOpen(reconciliation.getClient().getId(),
            AcctServer.DOCTYPE_Reconciliation, reconciliation.getOrganization().getId(),
            strStatementDate) && orgLegalWithAccounting) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
          msg.setMessage(
              Utility.parseTranslation(this, vars, vars.getLanguage(), "@PeriodNotAvailable@"));
          vars.setMessage(strTabId, msg);
          msg = null;
          printPageClosePopUpAndRefreshParent(response, vars);
          return;
        }

        if (documentEnabled && orgLegalWithAccounting) {
          String identifier = linesInNotAvailablePeriod(reconciliation.getId());
          if (!identifier.equalsIgnoreCase("")) {
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            msg.setMessage(String.format(
                Utility.messageBD(this, "APRM_PeriodNotAvailableClearedItem", vars.getLanguage()),
                identifier));
            vars.setMessage(strTabId, msg);
            msg = null;
            printPageClosePopUpAndRefreshParent(response, vars);
            return;
          }
        }

        for (APRM_FinaccTransactionV finacctrxv : reconciliation.getAPRMFinaccTransactionVList()) {
          if (reconciliation.getEndingDate()
              .compareTo(finacctrxv.getFinancialAccountTransaction().getTransactionDate()) < 0) {
            FIN_FinaccTransaction trans = finacctrxv.getFinancialAccountTransaction();
            // We set processed to false before changing dates to avoid trigger exception
            boolean posted = "Y".equals(trans.getPosted());
            if (posted) {
              trans.setPosted("N");
              OBDal.getInstance().save(trans);
              OBDal.getInstance().flush();
            }
            trans.setProcessed(false);
            OBDal.getInstance().save(trans);
            OBDal.getInstance().flush();
            trans.setTransactionDate(reconciliation.getEndingDate());
            trans.setDateAcct(reconciliation.getEndingDate());
            OBDal.getInstance().save(trans);
            OBDal.getInstance().flush();
            // We set processed to true afterwards
            trans.setProcessed(true);
            OBDal.getInstance().save(trans);
            OBDal.getInstance().flush();
            if (posted) {
              trans.setPosted("Y");
              OBDal.getInstance().save(trans);
              OBDal.getInstance().flush();
            }
            // Changing dates for accounting entries as well
            TransactionsDao.updateAccountingDate(trans);
          }
          Boolean invoicePaidold = false;
          if (finacctrxv.getPayment() != null) {
            for (FIN_PaymentDetail pd : finacctrxv.getPayment().getFINPaymentDetailList()) {
              for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
                invoicePaidold = psd.isInvoicePaid();
                if (!invoicePaidold) {
                  if ((FIN_Utility
                      .invoicePaymentStatus(finacctrxv.getPayment().getPaymentMethod(),
                          reconciliation.getAccount(), finacctrxv.getPayment().isReceipt())
                      .equals(finacctrxv.getPayment().getStatus()))) {
                    psd.setInvoicePaid(true);
                  }
                  if (psd.isInvoicePaid()) {
                    FIN_Utility.updatePaymentAmounts(psd);
                  }
                }
              }
              FIN_Utility.updateBusinessPartnerCredit(finacctrxv.getPayment());
            }
          }
        }
        // Delete alreay existing li
        OBQuery<ReconciliationLine> ReconcileLine = OBDal.getInstance().createQuery(
            ReconciliationLine.class, "as e where e.reconciliation.id= :reconciliationID ");
        ReconcileLine.setNamedParameter("reconciliationID", reconciliation.getId());
        if (ReconcileLine.list().size() > 0) {
          ps = conn.prepareStatement(
              "delete from EFIN_Fin_Reconciliationline where fin_reconciliation_id ='"
                  + reconciliation.getId() + "' ");
          ps.executeUpdate();
        }
        // make entry in Reconciliation lines
        lineQuery = "SELECT fin_finacc_transaction.fin_finacc_transaction_id, fin_finacc_transaction.ad_client_id, "
            + " fin_finacc_transaction.ad_org_id, fin_finacc_transaction.created, fin_finacc_transaction.createdby, "
            + " fin_finacc_transaction.updated, fin_finacc_transaction.updatedby, fin_finacc_transaction.isactive,  "
            + " fin_finacc_transaction.fin_payment_id, fin_finacc_transaction.c_currency_id, fin_finacc_transaction.fin_financial_account_id, "
            + " fin_finacc_transaction.dateacct, fin_finacc_transaction.c_glitem_id, fin_finacc_transaction.paymentamt, "
            + " fin_finacc_transaction.depositamt, fin_finacc_transaction.c_project_id, fin_finacc_transaction.c_campaign_id, "
            + " fin_finacc_transaction.c_activity_id, fin_finacc_transaction.user1_id, fin_finacc_transaction.user2_id,  "
            + " fin_finacc_transaction.trxtype, fin_finacc_transaction.statementdate, fin_finacc_transaction.description, "
            + " fin_finacc_transaction.fin_reconciliation_id, fin_bankstatementline.fin_bankstatementline_id "
            + " FROM fin_finacc_transaction "
            + " LEFT JOIN fin_bankstatementline ON fin_finacc_transaction.fin_finacc_transaction_id = fin_bankstatementline.fin_finacc_transaction_id"
            + " where fin_finacc_transaction.fin_reconciliation_id='" + reconciliation.getId()
            + "'";
        ps = conn.prepareStatement(lineQuery);
        rs = ps.executeQuery();
        while (rs.next()) {
          ReconciliationLine line = OBProvider.getInstance().get(ReconciliationLine.class);
          line.setFinancialAccountTransaction(OBDal.getInstance().get(FIN_FinaccTransaction.class,
              rs.getString("fin_finacc_transaction_id")));
          line.setClient(OBDal.getInstance().get(Client.class, rs.getString("ad_client_id")));
          line.setOrganization(
              OBDal.getInstance().get(Organization.class, rs.getString("ad_org_id")));
          line.setPayment(
              OBDal.getInstance().get(FIN_Payment.class, rs.getString("fin_payment_id")));
          line.setCurrency(OBDal.getInstance().get(Currency.class, rs.getString("c_currency_id")));
          line.setDepositTo(OBDal.getInstance().get(FIN_FinancialAccount.class,
              rs.getString("fin_financial_account_id")));
          line.setAccountingDate(rs.getDate("dateacct"));
          if (rs.getString("c_glitem_id") != null && !rs.getString("c_glitem_id").equals("")) {
            line.setGLItem(OBDal.getInstance().get(GLItem.class, rs.getString("c_glitem_id")));
          }
          line.setWithdrawalAmount(rs.getBigDecimal("paymentamt"));
          line.setDepositAmount(rs.getBigDecimal("depositamt"));
          if (rs.getString("c_project_id") != null && !rs.getString("c_project_id").equals("")) {
            line.setProject(OBDal.getInstance().get(Project.class, rs.getString("c_project_id")));
          }
          if (rs.getString("c_campaign_id") != null && !rs.getString("c_campaign_id").equals("")) {
            line.setSalesCampaign(
                OBDal.getInstance().get(Campaign.class, rs.getString("c_campaign_id")));
          }
          if (rs.getString("fin_bankstatementline_id") != null
              && !rs.getString("fin_bankstatementline_id").equals("")) {
            line.setBankStatementLine(OBDal.getInstance().get(FIN_BankStatementLine.class,
                rs.getString("fin_bankstatementline_id")));
          }
          if (rs.getString("c_activity_id") != null && !rs.getString("c_activity_id").equals("")) {
            line.setFunctionalClassfication(
                OBDal.getInstance().get(ABCActivity.class, rs.getString("c_activity_id")));
          }
          if (rs.getString("user1_id") != null && !rs.getString("user1_id").equals("")) {
            line.setFuture1(
                OBDal.getInstance().get(UserDimension1.class, rs.getString("user1_id")));
          }
          if (rs.getString("user2_id") != null && !rs.getString("user2_id").equals("")) {
            line.setFuture2(
                OBDal.getInstance().get(UserDimension2.class, rs.getString("user2_id")));
          }
          line.setDocument(rs.getString("trxtype"));
          line.setTransactionDate(rs.getDate("statementdate"));
          line.setDescription(rs.getString("description"));
          line.setReconciliation(reconciliation);
          OBDal.getInstance().save(line);
          OBDal.getInstance().flush();
        }
        reconciliation.setEfinDescription(inpDescription);
        reconciliation.setDocumentStatus("CO");
        reconciliation.setProcessed(true);
        reconciliation.setAPRMProcessReconciliation("R");
        reconciliation.setAprmProcessRec("R");
        OBDal.getInstance().save(reconciliation);
        OBDal.getInstance().flush();

      }

      String strMessage = "@APRM_ReconciliationNo@" + ": " + reconciliation.getDocumentNo();
      msg.setType("Success");
      msg.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
      vars.setMessage(strTabId, msg);
      msg = null;
      printPageClosePopUpAndRefreshParent(response, vars);

    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strOrgId,
      String strWindowId, String strTabId, String strFinancialAccountId, String strStatementDate,
      String strEndBalance) throws IOException, ServletException {

    log4j.debug("Output: Receipt Reconcile button pressed on Financial Account || Transaction tab");

    dao = new AdvPaymentMngtDao();
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
    SimpleDateFormat dteFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String description = "";
    FIN_Reconciliation currentReconciliation = null;
    OBContext.setAdminMode();
    try {
      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strFinancialAccountId);

      FIN_Reconciliation lastProcessedReconciliation = TransactionsDao
          .getLastReconciliation(account, "Y");
      currentReconciliation = TransactionsDao.getLastReconciliation(account, "N");
      if (isAutomaticReconciliation(currentReconciliation)) {
        OBDal.getInstance().rollbackAndClose();
        OBError message = Utility.translateError(this, vars, vars.getLanguage(),
            Utility.parseTranslation(this, vars, vars.getLanguage(), "@APRM_ReconciliationMixed@"));
        vars.setMessage(strTabId, message);
        printPageClosePopUp(response, vars, Utility.getTabURL(strTabId, "R", true));
        return;
      }

      XmlDocument xmlDocument = xmlEngine
          .readXmlTemplate("sa/elm/ob/finance/ad_actionbutton/receiptreconcile/Reconciliation")
          .createXmlDocument();

      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("theme", vars.getTheme());

      xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("mainDate", DateTimeData.today(this));
      xmlDocument.setParameter("windowId", strWindowId);
      xmlDocument.setParameter("tabId", strTabId);
      xmlDocument.setParameter("orgId", strOrgId);
      xmlDocument.setParameter("finFinancialAccountId", strFinancialAccountId);

      BigDecimal currentEndBalance = BigDecimal.ZERO;
      if (vars.commandIn("PROCESS")) {
        xmlDocument.setParameter("statementDate", strStatementDate);
        xmlDocument.setParameter("endBalance", strEndBalance);
        xmlDocument.setParameter("calcEndingBalance", strEndBalance);

      } else {
        String currentStatementDate = DateTimeData.today(this);
        if (currentReconciliation != null) {
          currentStatementDate = dateFormater.format(currentReconciliation.getTransactionDate());
          currentEndBalance = currentReconciliation.getEndingBalance();
          description = currentReconciliation.getEfinDescription();
        }
        currentStatementDate = dateYearFormat.format(dteFormat.parse(currentStatementDate));
        currentStatementDate = UtilityDAO.convertTohijriDate(currentStatementDate);
        xmlDocument.setParameter("statementDate", currentStatementDate);
        xmlDocument.setParameter("endBalance", currentEndBalance.toString());
        xmlDocument.setParameter("calcEndingBalance", currentEndBalance.toString());
        xmlDocument.setParameter("inpDescription", description);
      }

      BigDecimal beginBalance = (lastProcessedReconciliation == null) ? account.getInitialBalance()
          : lastProcessedReconciliation.getEndingBalance();

      xmlDocument.setParameter("account", account.getName());
      xmlDocument.setParameter("beginBalance", beginBalance.toString());

      // Hidden inputs
      xmlDocument.setParameter("calcBeginningBalance", beginBalance.toString());
      xmlDocument.setParameter("calcTotalPayment", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcTotalDeposit", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcDifferenceToClear",
          currentEndBalance.subtract(beginBalance).toString());
      xmlDocument.setParameter("calcCurrentlyCleared",
          TransactionsDao.getCurrentlyClearedAmt(account.getId()).toString());
      xmlDocument.setParameter("calcDifference",
          currentEndBalance
              .subtract(beginBalance.add(TransactionsDao.getCurrentlyClearedAmt(account.getId())))
              .toString());

      OBContext.setAdminMode();
      try {
        xmlDocument.setParameter("precision",
            account.getCurrency().getStandardPrecision().toString());

        if (currentReconciliation == null) {
          DocumentType docType = FIN_Utility.getDocumentType(account.getOrganization(), "REC");
          if (docType == null) {
            OBError msg = new OBError();
            String strMessage = "@APRM_DocumentTypeNotFound@";
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
            vars.setMessage(strTabId, msg);
            msg = null;
            printPageClosePopUpAndRefreshParent(response, vars);
            return;
          }
          String docNumber = FIN_Utility.getDocumentNo(account.getOrganization(), "REC",
              "DocumentNo_FIN_Reconciliation");

          String strNpsNo = sa.elm.ob.utility.util.Utility.getGeneralSequence(
              DateTimeData.today(this), "GS", account.getOrganization().getCalendar().getId(),
              account.getOrganization().getId(), Boolean.TRUE);

          if ("0".equals(strNpsNo)) {
            OBError msg = new OBError();
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(),
                OBMessageUtils.messageBD("Efin_NoGeneralSequence")));
            vars.setMessage(strTabId, msg);
            msg = null;
            printPageClosePopUpAndRefreshParent(response, vars);
            return;
          }

          dao.getNewReconciliation(account.getOrganization(), account, docNumber, docType,
              dteFormat.parse(DateTimeData.today(this)), dteFormat.parse(DateTimeData.today(this)),
              beginBalance, BigDecimal.ZERO, "DR");
        }
      } finally {
        OBContext.restorePreviousMode();
      }

      OBError myMessage = vars.getMessage(strWindowId);
      vars.removeMessage(strWindowId);
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }

      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println(xmlDocument.print());
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printGrid(HttpServletResponse response, String strFinancialAccountId,
      String strStatmentDate, boolean afterDate, String selectedTransactionsIds,
      String strCurrentlyCleared, String strTotalPayment, String strTotalDeposit, String sortColumn,
      String sortType, String strValidMoF) throws IOException, ServletException {

    log4j.debug("Output: Grid on Financial Account || Transaction tab || Reconciliation window");

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("sa/elm/ob/finance/ad_actionbutton/receiptreconcile/ReconciliationGrid")
        .createXmlDocument();

    FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
        strFinancialAccountId);

    Map<String, String> map = FIN_Utility.getMapFromStringList(selectedTransactionsIds);

    FieldProvider[] data = TransactionsDao.getTransactionsFiltered(account,
        FIN_Utility.getDate(DateTimeData.nDaysAfter(this, strStatmentDate, "1")), afterDate,
        sortColumn, sortType, strValidMoF);

    BigDecimal currentlyCleared = new BigDecimal(strCurrentlyCleared);
    BigDecimal totalPayment = new BigDecimal(strTotalPayment);
    BigDecimal totalDeposit = new BigDecimal(strTotalDeposit);

    for (FieldProvider fp : data) {

      if (!map.containsKey(fp.getField("transactionId"))
          && !fp.getField("markSelectedId").isEmpty()) {
        BigDecimal payAmt = new BigDecimal(fp.getField("paymentAmount"));
        BigDecimal depAmt = new BigDecimal(fp.getField("depositAmount"));
        currentlyCleared = currentlyCleared.add(payAmt).subtract(depAmt);
        totalPayment = totalPayment.add(payAmt);
        totalDeposit = totalDeposit.add(depAmt);
      }
    }

    xmlDocument.setParameter("calcTotalPayment", totalPayment.toString());
    xmlDocument.setParameter("caclTotalDeposit", totalDeposit.toString());
    xmlDocument.setParameter("calcCurrentlyCleared", currentlyCleared.toString());
    xmlDocument.setData("structure", data);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private boolean isAutomaticReconciliation(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode();
    try {
      final OBCriteria<FIN_ReconciliationLine_v> obc = OBDal.getInstance()
          .createCriteria(FIN_ReconciliationLine_v.class);
      obc.add(Restrictions.eq(FIN_ReconciliationLine_v.PROPERTY_RECONCILIATION, reconciliation));
      obc.add(Restrictions.isNotNull(FIN_ReconciliationLine_v.PROPERTY_BANKSTATEMENTLINE));
      obc.setMaxResults(1);
      final List<FIN_ReconciliationLine_v> rec = obc.list();
      return (rec.size() != 0);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String getServletInfo() {
    return "This servlet manages manual transactions reconciliations.";
  }

  private String linesInNotAvailablePeriod(String reconciliationId) {
    final StringBuilder hql = new StringBuilder();

    hql.append(" as rl ");
    hql.append(" where rl.reconciliation.id = '").append(reconciliationId).append("' ");
    hql.append("   and c_chk_open_period(rl.organization, rl.transactionDate, 'REC', null) = 0 ");
    hql.append(" order by rl.transactionDate");

    final OBQuery<FIN_ReconciliationLine_v> obqRL = OBDal.getInstance()
        .createQuery(FIN_ReconciliationLine_v.class, hql.toString());
    obqRL.setMaxResult(1);

    List<FIN_ReconciliationLine_v> obqRLlist = obqRL.list();

    if (obqRLlist.size() == 0) {
      return "";
    } else {
      return obqRLlist.get(0).getIdentifier();
    }

  }

  public List<FIN_FinaccTransaction> getTransactionList(FIN_Reconciliation reconciliation) {
    OBContext.setAdminMode();
    List<FIN_FinaccTransaction> transactions = null;
    try {
      OBCriteria<FIN_FinaccTransaction> trans = OBDal.getInstance()
          .createCriteria(FIN_FinaccTransaction.class);
      trans.add(Restrictions.eq(FIN_FinaccTransaction.PROPERTY_RECONCILIATION, reconciliation));
      trans.setFilterOnReadableClients(false);
      trans.setFilterOnReadableOrganization(false);
      transactions = trans.list();
    } finally {
      OBContext.restorePreviousMode();
    }
    return transactions;
  }

  /*
   * Checks if this step (Reconciliation) is configured to generate accounting for the selected
   * financial account
   */
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    OBContext.setAdminMode();
    try {
      FIN_Reconciliation reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class,
          strRecordId);
      List<FIN_FinaccTransaction> transactions = getTransactionList(reconciliation);
      List<FIN_FinancialAccountAccounting> accounts = reconciliation.getAccount()
          .getFINFinancialAccountAcctList();
      for (FIN_FinaccTransaction transaction : transactions) {
        FIN_Payment payment = transaction.getFinPayment();
        // If payment exists, check Payment Method + financial Account Configuration
        if (payment != null) {
          OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
              .createCriteria(FinAccPaymentMethod.class);
          obCriteria.add(
              Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, reconciliation.getAccount()));
          obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
              payment.getPaymentMethod()));
          obCriteria.setFilterOnReadableClients(false);
          obCriteria.setFilterOnReadableOrganization(false);
          List<FinAccPaymentMethod> lines = obCriteria.list();
          for (FIN_FinancialAccountAccounting account : accounts) {
            if (payment.isReceipt()) {
              if (("INT").equals(lines.get(0).getINUponClearingUse())
                  && account.getInTransitPaymentAccountIN() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("DEP").equals(lines.get(0).getINUponClearingUse())
                  && account.getDepositAccount() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("CLE").equals(lines.get(0).getINUponClearingUse())
                  && account.getClearedPaymentAccount() != null) {
                transactionsToBePosted.add(transaction);
              }
            } else {
              if (("INT").equals(lines.get(0).getOUTUponClearingUse())
                  && account.getFINOutIntransitAcct() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("WIT").equals(lines.get(0).getOUTUponClearingUse())
                  && account.getWithdrawalAccount() != null) {
                transactionsToBePosted.add(transaction);
              } else if (("CLE").equals(lines.get(0).getOUTUponClearingUse())
                  && account.getClearedPaymentAccountOUT() != null) {
                transactionsToBePosted.add(transaction);
              }
            }
          }
        } else if (transaction.getGLItem() != null) {
          for (FIN_FinancialAccountAccounting account : accounts) {
            if ("BPD".equals(transaction.getTransactionType())
                && account.getClearedPaymentAccount() != null) {
              transactionsToBePosted.add(transaction);
            } else if ("BPW".equals(transaction.getTransactionType())
                && account.getClearedPaymentAccountOUT() != null) {
              transactionsToBePosted.add(transaction);
            }
          }
        } else {
          for (FIN_FinancialAccountAccounting account : accounts) {
            if ("BF".equals(transaction.getTransactionType())
                && account.getClearedPaymentAccountOUT() != null) {
              transactionsToBePosted.add(transaction);
            }
          }
        }
      }
    } catch (Exception e) {
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
    if (transactionsToBePosted.size() == 0) {
      return false;
    }
    return true;
  }

}
