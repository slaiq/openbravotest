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
 * All portions are Copyright (C) 2012-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package sa.elm.ob.finance.process.gl_journal;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetActual;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_forms.journalapproval.dao.GLJournalApprovalDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.EutJournalApproval;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.UtilityDAO;

public class FIN_AddPaymentFromJournal extends DalBaseProcess {
  final private static Logger log = LoggerFactory.getLogger(FIN_AddPaymentFromJournal.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    int cont = 0;

    // Recover context and variables
    ConnectionProvider conn = bundle.getConnection();
    VariablesSecureApp varsAux = bundle.getContext().toVars();
    HttpServletRequest request = RequestContext.get().getRequest();
    PreparedStatement ps = null, ps1 = null, ps2 = null;
    ResultSet rs = null, rs1 = null, rs2 = null;
    boolean errorFlag = false;
    final OBError msg = new OBError();
    String sql = "", sql1 = "", sql2 = "";

    OBContext.setOBContext(varsAux.getUser(), varsAux.getRole(), varsAux.getClient(),
        varsAux.getOrg());
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      OBContext.setAdminMode();

      // retrieve the parameters from the bundle±
      final String journalId = (String) bundle.getParams().get("GL_Journal_ID");
      String docAction = vars.getStringParameter("inpdocaction");
      GLJournal journal = OBDal.getInstance().get(GLJournal.class, journalId);
      // Check Posting Sequence
      String AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(journal.getAccountingDate());
      String CalendarId = "";
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
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            CalendarId = org.getCalendar().getId();
            break;
          }
        }

      }

      if ("".equals(docAction) || "CO".equals(docAction)) {
        docAction = "EUT_SU";
      }
      log.debug("docAction:" + docAction);

      // Set the docAction of the Journal (Complete, Reactivate, Close...)
      journal = OBDal.getInstance().get(GLJournal.class, journalId);
      journal.setDocumentAction(docAction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(journal);

      // Check while submit or Approve the Journal Whether debit amount is less than Funds available
      // or not.

      if ("EUT_SU".equals(docAction)) {

        String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "NPS", CalendarId,
            org.getId(), false);
        if (SequenceNo.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoPaymentSequence"));
        }

        /* Debit and credit amounts do not match */
        sql = " select totaldr, totalcr,controlamt from gl_journal where gl_journal_id= '"
            + journalId + "'";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("totaldr") != rs.getInt("totalcr")) {
            throw new OBException("@DistinctAmtError@ ");
          }
        }
        /* Period not valid */
        sql = " select count(*) as count from c_period p where p.c_period_id= '"
            + journal.getPeriod().getId() + "' and  '" + journal.getAccountingDate()
            + "' between p.startdate and p.enddate ";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("count") < 1)
            throw new OBException("@PeriodNotValid@ ");
        }
        /*
         * Check the header belongs to a organization where transactions are posible and ready to
         * use
         */
        sql = " select ad_org.isready as ready , ad_orgtype.istransactionsallowed as transactionallowed from gl_journal, ad_org,ad_orgtype where ad_org.ad_org_id=gl_journal.ad_org_id and ad_org.ad_orgtype_id=ad_orgtype.ad_orgtype_id"
            + " and gl_journal.gl_journal_id='" + journalId + "'";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getString("ready").equals("N"))
            throw new OBException("@OrgHeaderNotReady@ ");
          if (rs.getString("transactionallowed").equals("N"))
            throw new OBException("@OrgHeaderNotTransAllowed@ ");
        }
        /* Check if the gl journal has lines. */
        sql = " select max(documentno) as documentno  from gl_journal where exists (select 1 from gl_journalline where gl_journalline.gl_journal_id= gl_journal.gl_journal_id and gl_journalline.gl_journal_id='"
            + journalId + "')";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getString("documentno") == null) {
            sql = " select documentNo  from gl_journal where  GL_Journal.gl_journal_id='"
                + journalId + "'";
            ps = conn.getPreparedStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
              String status = OBMessageUtils.messageBD("GLJournalHasNoLines");
              status = status + rs.getString("documentNo");
              throw new OBException(status);
            }
          }

        }
        /* lines and header in different organization */
        sql = " select ad_org_chk_documents('GL_JOURNAL', 'GL_JOURNALLINE', '" + journalId
            + "','GL_JOURNAL_ID', 'GL_JOURNAL_ID')  as isincluded from dual ";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getInt("isincluded") == -1)
            throw new OBException("@LinesAndHeaderDifferentLEorBU@ ");
        }
        /**/
        sql = " SELECT gll.ad_org_id, gll.ad_client_id   FROM gl_journalline gll  WHERE gll.gl_journal_id = '"
            + journalId + "'";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        while (rs.next()) {
          sql1 = " select AD_ISORGINCLUDED('" + rs.getString("ad_org_id") + "','"
              + journal.getOrganization().getId() + "','" + rs.getString("ad_client_id")
              + "') as isincluded from dual ";
          ps1 = conn.getPreparedStatement(sql1);
          rs1 = ps1.executeQuery();
          if (rs1.next()) {
            if (rs1.getInt("isincluded") == -1)
              throw new OBException("@ForcedOrgNotMatchDocument@ ");
          } else
            continue;
        }
        /* check batch org and header org is different or not */
        sql = " SELECT COALESCE(gl_journalbatch.ad_org_id, gl_journal.ad_org_id) as batchorgId from  gl_journal LEFT JOIN gl_journalbatch  ON gl_journal.gl_journalbatch_id = gl_journalbatch.gl_journalbatch_id   WHERE gl_journal.gl_journal_id =  '"
            + journalId + "'";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (!rs.getString("batchorgId").equals(journal.getOrganization().getId()))
            throw new OBException("@BatchAndHeaderDifferentOrg@ ");
        }
        /* check period is available or not */
        sql = "  SELECT AD_GET_DOC_LE_BU('GL_JOURNAL', '" + journalId
            + "', 'GL_JOURNAL_ID', 'LE') as orgbuleId from dual";
        ps = conn.getPreparedStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          sql1 = "  SELECT AD_OrgType.IsAcctLegalEntity as isacctle  FROM AD_OrgType, AD_Org  WHERE AD_Org.AD_OrgType_ID = AD_OrgType.AD_OrgType_ID  AND AD_Org.AD_Org_ID='"
              + rs.getString("orgbuleId") + "'";
          ps1 = conn.getPreparedStatement(sql1);
          rs1 = ps1.executeQuery();
          if (rs1.next()) {
            if (rs1.getString("isacctle").equals("Y")) {
              sql2 = "	 SELECT C_CHK_OPEN_PERIOD_GLJ('" + journal.getOrganization().getId()
                  + "', '" + journal.getPeriod().getId() + "') as availableperiod from dual";
              ps2 = conn.getPreparedStatement(sql2);
              rs2 = ps2.executeQuery();
              if (rs2.next()) {
                if (rs2.getInt("availableperiod") != 1) {
                  throw new OBException("@PeriodNotAvailable@ ");
                }
              }
            }
          }
        }

        /* check sum of same unique code debit value is more than funds available */

        for (GLJournalLine line : journal.getFinancialMgmtGLJournalLineList()) {
          AccountingCombination uniqueCode = line.getAccountingCombination();
          if (uniqueCode.getEfinDimensiontype().equals("E")) {
            EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(uniqueCode,
                journal.getEFINBudgetDefinition());
            sql = "select line.c_validcombination_id,  sum(amtacctdr) as sum "
                + " from gl_journalline line "
                + " join gl_journal header on line.gl_journal_id = header.gl_journal_id "
                + " where header.gl_journal_id  ='" + journal.getId()
                + "'  and  amtacctdr > 0 and line.c_validcombination_id ='" + uniqueCode.getId()
                + "'" + " group by line.c_validcombination_id  ";
            ps = conn.getPreparedStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
              log.debug("sum:" + rs.getBigDecimal("sum"));
              if (budgetInq.getFundsAvailable().compareTo(rs.getBigDecimal("sum")) < 0) {
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

        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(journal);
      }
      // If Reactive the Journal Then Remove the Record into Actual Table.
      else if ("RE".equals(docAction) || "CL".equals(docAction)) {
        for (GLJournalLine jline : journal.getFinancialMgmtGLJournalLineList()) {
          OBQuery<EfinBudgetActual> actual = OBDal.getInstance().createQuery(EfinBudgetActual.class,
              "journalLine.id='" + jline.getId() + "'");
          if (actual.list().size() > 0) {
            EfinBudgetActual actualdetail = actual.list().get(0);
            OBDal.getInstance().remove(actualdetail);
          } else
            continue;
        }

        try {
          // Call GL_Journal_Post method from the database.
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(null);
          parameters.add(journalId);
          final String procedureName = "gl_journal_post";
          CallStoredProcedure mm = CallStoredProcedure.getInstance();
          mm.call(procedureName, parameters, null, false, false);
        } catch (Exception e) {
          OBDal.getInstance().rollbackAndClose();
          OBError error = OBMessageUtils.translateError(conn, vars, vars.getLanguage(),
              e.getCause().getMessage());
          throw new OBException(error.getMessage());
        }

        OBDal.getInstance().refresh(journal);
        if ("RE".equals(docAction))
          GLJournalApprovalDAO.insertApprovalHistory(vars, "RE", journal, null,
              journal.getOrganization().getId(), null);
        for (GLJournalLine jline : journal.getFinancialMgmtGLJournalLineList()) {
          GLJournalLine journalLine = OBDal.getInstance().get(GLJournalLine.class, jline.getId());
          journalLine.setEfinCheckingStaus(null);
          journalLine.setEfinCheckingStausFailure("");
          OBDal.getInstance().save(journalLine);
        }

      }

      // If all the validation satisfied and errorFlag is false then do the further process.
      log.debug("errorFlag:" + errorFlag);
      if (!errorFlag && "EUT_SU".equals(docAction)) {

        // Set the docAction of the Journal (Complete, Reactivate, Close...)
        journal.setDocumentAction(docAction);
        // Check if the Lines of the Journal have related Payments. In that case
        // the Payments must be deleted before Closing or Reactivating the line.
        String relatedPayments = "";
        if (!"EUT_SU".equals(docAction)) {
          for (GLJournalLine journalLine : journal.getFinancialMgmtGLJournalLineList()) {
            if (journalLine.getRelatedPayment() != null) {
              relatedPayments = relatedPayments + journalLine.getLineNo() + ", ";
            }
          }
        }
        if (!"".equals(relatedPayments)) {
          relatedPayments = relatedPayments.substring(0, relatedPayments.length() - 2);
          throw new OBException("@FIN_JournalLineRelatedPayments@: " + relatedPayments);
        }

        // Set Next Role
        /*
         * NextRoleByRuleVO nextRoleByRuleVO = NextRoleByRule.getNextRole(
         * OBDal.getInstance().getConnection(), vars.getClient(), journal.getOrganization().getId(),
         * vars.getRole(), vars.getUser(), Resource.GLJOURNAL_RULE, actualAmount);
         */

        NextRoleByRuleVO nextRoleByRuleVO = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), vars.getClient(),
            journal.getOrganization().getId(), vars.getRole(), vars.getUser(),
            Resource.GLJOURNAL_RULE, BigDecimal.ZERO, journal.getCreatedBy().getId(), false,
            journal.getDocumentStatus());
        EutNextRole nextRole = null;
        if (!journal.getDocumentStatus().equals("EFIN_WFA"))
          // insert into Approval
          insertApproval(journal, vars, nextRoleByRuleVO.getStatus());
        if (nextRoleByRuleVO != null && nextRoleByRuleVO.getErrorMsg() != null) {
          if (nextRoleByRuleVO.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
            journal.setEutNextRole(null);
            throw new OBException("@Escm_No_LineManager@");
          } else if (nextRoleByRuleVO.getErrorMsg().equals("EUT_NOUser_ForRoles")) {
            journal.setEutNextRole(null);
            throw new OBException(OBMessageUtils.messageBD("EUT_NOUser_ForRoles").replace("@",
                nextRoleByRuleVO.getRoleName()));
          }
        } else if (nextRoleByRuleVO != null && nextRoleByRuleVO.hasApproval()) {
          nextRole = OBDal.getInstance().get(EutNextRole.class, nextRoleByRuleVO.getNextRoleId());
          journal.setEutNextRole(nextRole);
          journal.setDocumentStatus("EFIN_WFA");
        } else {
          journal.setEutNextRole(null);
        }

        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(journal);
        if (journal.getEutNextRole() == null) {
          try {
            // Call GL_Journal_Post method from the database.
            final List<Object> parameters = new ArrayList<Object>();
            parameters.add(null);
            parameters.add(journalId);
            final String procedureName = "gl_journal_post";
            CallStoredProcedure mm = CallStoredProcedure.getInstance();
            mm.call(procedureName, parameters, null, false, false);
          } catch (Exception e) {
            OBDal.getInstance().rollbackAndClose();
            OBError error = OBMessageUtils.translateError(conn, vars, vars.getLanguage(),
                e.getCause().getMessage());
            throw new OBException(error.getMessage());
          }
          OBDal.getInstance().refresh(journal);

        }

        for (GLJournalLine jline : journal.getFinancialMgmtGLJournalLineList()) {
          // Recover again the object to avoid problems with Dal
          GLJournalLine journalLine = OBDal.getInstance().get(GLJournalLine.class, jline.getId());
          if (journalLine.isOpenItems() && journalLine.getRelatedPayment() == null) {
            // Create bundle
            vars = new VariablesSecureApp(varsAux.getUser(), varsAux.getClient(), varsAux.getOrg(),
                varsAux.getRole(), varsAux.getLanguage());
            ProcessBundle pb = new ProcessBundle("91527EBD804949E6AAA72CBB9C889269", vars)
                .init(conn);
            HashMap<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("GL_JournalLine_ID", journalLine.getId());
            pb.setParams(parameters);
            OBError myMessage = null;
            // Create a Payment for the Journal line
            FIN_AddPaymentFromJournalLine myProcess = new FIN_AddPaymentFromJournalLine();
            myProcess.setDoCommit(false);
            myProcess.execute(pb);
            myMessage = (OBError) pb.getResult();

            if (myMessage.getType().equals("Error")) {
              throw new OBException("@FIN_PaymentFromJournalError@ " + journalLine.getLineNo()
                  + " - " + myMessage.getMessage());
            }
            cont++;
          }
          // If processing journal is not check with Open Items then set the status and insert the
          // record into Actual Table.
          else {
            journalLine.setEfinCheckingStaus("SCS");
            journalLine.setEfinCheckingStausFailure("");
            OBDal.getInstance().save(journalLine);

          }

        }

      }
      // OBError is also used for successful results
      if (!errorFlag) {
        msg.setType("Success");
        msg.setTitle("@Success@");
        if (cont > 0) {
          msg.setMessage(" @FIN_NumberOfPayments@: " + cont);
        }
      } else {
        msg.setType("Error");
        msg.setTitle("@Error@");
        msg.setMessage("@Efin_GLJourLine_Failed@");
      }

      bundle.setResult(msg);
      OBDal.getInstance().commitAndClose();
    } catch (final OBException e) {
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("@Error@");
      OBDal.getInstance().rollbackAndClose();
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void insertApproval(GLJournal journal, VariablesSecureApp vars, String pendingApp) {
    EutJournalApproval approval = null;
    Client client = null;
    Organization organization = null;
    User user = null;
    Role role = null;

    try {
      OBContext.setAdminMode();

      client = OBDal.getInstance().get(Client.class, vars.getClient());
      organization = OBDal.getInstance().get(Organization.class, vars.getOrg());
      user = OBDal.getInstance().get(User.class, vars.getUser());
      role = OBDal.getInstance().get(Role.class, vars.getRole());

      approval = OBProvider.getInstance().get(EutJournalApproval.class);

      approval.setClient(client);
      approval.setOrganization(organization);
      approval.setCreatedBy(user);
      approval.setUpdatedBy(user);
      approval.setUserContact(user);
      approval.setRole(role);
      approval.setApproveddate(new Date());
      approval.setJournalEntry(journal);
      approval.setAlertStatus("SUB");
      approval.setPendingapproval(pendingApp);

      OBDal.getInstance().save(approval);
      OBDal.getInstance().flush();
      // OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception while insertApproval ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
