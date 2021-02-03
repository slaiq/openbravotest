package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

import sa.elm.ob.finance.ReconciliationLine;
import sa.elm.ob.utility.util.Utility;

public class FINReconciliationTemplate extends DocFINReconciliationTemplate {

  Logger log4j = Logger.getLogger(FINReconciliationTemplate.class);

  @Override
  public Fact createFact(DocFINReconciliation docREC, AcctSchema as, ConnectionProvider conn,
      Connection con, VariablesSecureApp vars) throws ServletException {
    Fact fact = new Fact(docREC, as, Fact.POST_Actual);

    // Select specific definition
    OBContext.setAdminMode();
    try {

      String Fact_Acct_Group_ID = SequenceIdData.getUUID();
      BigDecimal totalAmt = BigDecimal.ZERO, totalDeposit = BigDecimal.ZERO;
      FIN_Reconciliation reconciliation = Utility.getObject(FIN_Reconciliation.class,
          docREC.Record_ID);
      Boolean hasSequenceDefined = Boolean.FALSE;
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      // String strDateFormat =
      // OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("dateFormat.java");

      String strSeqNo = Utility.getGeneralSequence(
          dateFormat.format(reconciliation.getCreationDate()), "NPS",
          reconciliation.getOrganization().getCalendar().getId(),
          reconciliation.getOrganization().getId(), Boolean.TRUE);

      if (!strSeqNo.equals("0")) {
        hasSequenceDefined = Boolean.TRUE;
      }

      if (hasSequenceDefined) {
        if (reconciliation != null) {
          List<ReconciliationLine> clearedItems = reconciliation.getEFINFinReconciliationlineList();
          if (clearedItems != null && clearedItems.size() > 0) {

            for (ReconciliationLine lines : clearedItems) {
              if (lines.getFinancialAccountTransaction().getTransactionType()
                  .equals(DocFINReconciliation.TRXTYPE_BPDeposit)) {
                totalDeposit = totalDeposit.add(lines.getDepositAmount());
              } else {
                totalAmt = totalAmt.add(lines.getWithdrawalAmount());
              }
            }
            /*
             * convertedAmount = docREC.convertAmount(totalAmt, Boolean.FALSE,
             * dateFormat.format(reconciliation.getTransactionDate()),
             * AcctServer.TABLEID_Transaction, reconciliation.getId(), docREC.C_Currency_ID,
             * as.m_C_Currency_ID, null, as, fact, Fact_Acct_Group_ID,
             * docREC.nextSeqNo(docREC.SeqNo), conn);
             */

            if (reconciliation.getDocumentStatus().equals("EFIN_UREC")) {

              if (totalAmt.compareTo(BigDecimal.ZERO) > 0) {
                fact.createLine(null,
                    docREC.getWithdrawalAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, "0", (totalAmt.negate()).toString(), Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);

                fact.createLine(null,
                    docREC.getClearOutAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, (totalAmt.negate()).toString(), "0", Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);
              }
              if (totalAmt.compareTo(BigDecimal.ZERO) < 0) {
                fact.createLine(null,
                    docREC.getClearOutAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, "0", (totalAmt).toString(), Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);

                fact.createLine(null,
                    docREC.getWithdrawalAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, (totalAmt).toString(), "0", Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);
              }
              if (totalDeposit.compareTo(BigDecimal.ZERO) > 0) {
                fact.createLine(null,
                    docREC.getDepositAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, "0", (totalDeposit.negate()).toString(),
                    Fact_Acct_Group_ID, docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType,
                    docREC.DateAcct, null, conn);
                fact.createLine(null,
                    docREC.getClearInAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, (totalAmt.negate()).toString(), "0", Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);
              }

            } else {
              if (totalAmt.compareTo(BigDecimal.ZERO) > 0) {
                fact.createLine(null,
                    docREC.getWithdrawalAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, totalAmt.toString(), "0", Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);

                fact.createLine(null,
                    docREC.getClearOutAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, "0", totalAmt.toString(), Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);
              }
              if (totalDeposit.compareTo(BigDecimal.ZERO) > 0) {
                fact.createLine(null,
                    docREC.getDepositAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, "0", totalDeposit.toString(), Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);

                fact.createLine(null,
                    docREC.getClearInAccount(as, reconciliation.getAccount(), conn),
                    docREC.C_Currency_ID, totalDeposit.toString(), "0", Fact_Acct_Group_ID,
                    docREC.nextSeqNo(docREC.SeqNo), docREC.DocumentType, docREC.DateAcct, null,
                    conn);
              }
            }
          }
        }
      } else {
        docREC.setStatus("EFIN_NoSeq");
        docREC.strMessage = OBMessageUtils.messageBD("Efin_NonPaymentSequndefined");
        docREC.setMessageResult(conn, "EFIN_NoSeq", "error", null);
      }
    } catch (Exception e) {
      log4j.error("Exception while posting reconciliation: ", e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return fact;
  }
}