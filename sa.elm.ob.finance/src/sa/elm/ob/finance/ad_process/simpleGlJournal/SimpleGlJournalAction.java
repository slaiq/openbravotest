package sa.elm.ob.finance.ad_process.simpleGlJournal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.event.dao.GlJournalLineEventDAO;
import sa.elm.ob.finance.process.gl_journal.FIN_AddPaymentFromJournalLine;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author poongodi on 11/04/19
 */

public class SimpleGlJournalAction extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(SimpleGlJournalAction.class);
  private static String errorMsgs = null;

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false;
    boolean allowDelegation = false;

    try {
      OBContext.setAdminMode();
      final String strJournalId = (String) bundle.getParams().get("GL_Journal_ID");
      GLJournal objJournal = OBDal.getInstance().get(GLJournal.class, strJournalId);
      String DocStatus = objJournal.getDocumentStatus();
      String DocAction = objJournal.getEfinAction();
      NextRoleByRuleVO nextApproval = null;
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objJournal.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();

      Date currentDate = new Date();
      int count = 0;
      String sql = "";
      Boolean chkRoleIsInDocRul;
      String comments = (String) bundle.getParams().get("notes");
      String documentRule = Resource.GLJOURNAL_RULE;
      // Check Posting Sequence
      String AccountDate = new SimpleDateFormat("dd-MM-yyyy")
          .format(objJournal.getAccountingDate());
      String CalendarId = "";
      Organization org = objJournal.getOrganization();
      PreparedStatement ps = null;
      ResultSet rs = null;
      Connection conn = OBDal.getInstance().getConnection();
      CalendarId = SimpleGlJournalDAO.getCalenderId(org);
      boolean chkDebitCreditAmt = false;
      boolean PeriodValidation = false;
      boolean chkOrgHeaderNotReady = false;
      boolean chkOrgHeaderNotTransAllowed = false;
      boolean chkHeaderLineOrg = false;
      boolean chkForcedOrganization = false;
      boolean chkBatchOrg = false;
      boolean chkPeriodAvailable = false;
      boolean chkTotalDebitAmt = false;
      boolean chkTotalCreditAmt = false;
      List<AccountingCombination> accCombinationList = null;

      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      // Task #8198
      // check submitted role have the branch details or not
      if (DocStatus.equals("DR")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // find the submitted role org/branch details

      if (objJournal.getEutNextRole() != null) {
        if (objJournal.getEfinSubmittedRole() != null
            && objJournal.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = objJournal.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (objJournal.getEutNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // check role is present in document rule or not
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, submittedRoleOrgId, userId, roleId, documentRule,
            objJournal.getTotalDebitAmount());
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("EFIN_RDR")) {
        int submitAllowed = CommonValidations.checkUserRoleForSubmit("gl_journal", vars.getUser(),
            vars.getRole(), strJournalId, "gl_journal_id");
        if (submitAllowed == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      // while submit and approve then need to chk the following cdn
      if (!errorFlag) {
        String SequenceNo = UtilityDAO.getGeneralSequence(AccountDate, "NPS", CalendarId,
            org.getId(), false);
        if (SequenceNo.equals("0")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_NoPaymentSequence@");
          bundle.setResult(result);
          return;
        }
        // debit and credit amt
        chkDebitCreditAmt = SimpleGlJournalDAO.chkDebitCreditAmt(strJournalId);
        if (chkDebitCreditAmt) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@DistinctAmtError@");
          bundle.setResult(result);
          return;
        }
        // chk period
        PeriodValidation = SimpleGlJournalDAO.PeriodValidation(objJournal);
        if (PeriodValidation) {
          if (DocStatus.equals("DR")) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotValid@");
            bundle.setResult(result);
            return;
          }

          else {
            // check the status of record ,status is other than draft check current date and
            // accounting date year is
            // same then check period is open for the current date then allow by updating
            // the accounting date as current date,then do not
            // allow to submit

            if (UtilityDAO.getYearId(currentDate, clientId)
                .equals(UtilityDAO.getYearId(objJournal.getAccountingDate(), clientId))) {
              Boolean isPeriodOpen = Utility.checkOpenPeriod(currentDate,
                  orgId.equals("0") ? vars.getOrg() : orgId, objJournal.getClient().getId());

              if (!isPeriodOpen) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
                bundle.setResult(result);
                return;
              } else {
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                Date now = new Date();
                Date todaydate = dateFormat.parse(dateFormat.format(now));
                objJournal.setAccountingDate(todaydate);
                String strcPeriodId = UtilityDAO.getPeriod(dateFormat.format(now), orgId);
                objJournal.setPeriod(OBDal.getInstance()
                    .get(org.openbravo.model.financialmgmt.calendar.Period.class, strcPeriodId));
              }

            } else {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
              bundle.setResult(result);
              return;
            }
          }
        }
        /*
         * Check the header belongs to a organization where transactions are posible and ready to
         * use
         */
        chkOrgHeaderNotReady = SimpleGlJournalDAO.chkOrgHeaderNotReady(strJournalId);
        if (chkOrgHeaderNotReady) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@OrgHeaderNotReady@");
          bundle.setResult(result);
          return;
        }
        /*
         * Check the header belongs to a organization where transactions are posible and ready to
         * use
         */
        chkOrgHeaderNotTransAllowed = SimpleGlJournalDAO.chkOrgHeaderNotTransAllowed(strJournalId);
        if (chkOrgHeaderNotTransAllowed) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@OrgHeaderNotTransAllowed@");
          bundle.setResult(result);
          return;
        }

        /* Check if the gl journal has lines. */
        sql = " select max(documentno) as documentno  from gl_journal where exists (select 1 from gl_journalline where gl_journalline.gl_journal_id= gl_journal.gl_journal_id and gl_journalline.gl_journal_id='"
            + strJournalId + "')";
        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();
        if (rs.next()) {
          if (rs.getString("documentno") == null) {
            sql = " select documentNo  from gl_journal where  GL_Journal.gl_journal_id='"
                + strJournalId + "'";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
              String status = OBMessageUtils.messageBD("GLJournalHasNoLines");
              status = status + rs.getString("documentNo");
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", status);
              bundle.setResult(result);
              return;
            }
          }

        }
        /* lines and header in different organization */
        chkHeaderLineOrg = SimpleGlJournalDAO.chkOrganization(strJournalId);
        if (chkHeaderLineOrg) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@LinesAndHeaderDifferentLEorBU@");
          bundle.setResult(result);
          return;
        }
        chkForcedOrganization = SimpleGlJournalDAO.chkForcedOrganization(objJournal);
        if (chkForcedOrganization) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ForcedOrgNotMatchDocument@");
          bundle.setResult(result);
          return;
        }
        /* check batch org and header org is different or not */
        chkBatchOrg = SimpleGlJournalDAO.chkBatchOrg(objJournal);
        if (chkBatchOrg) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@BatchAndHeaderDifferentOrg@");
          bundle.setResult(result);
          return;
        }
        /* check period is available or not */
        chkPeriodAvailable = SimpleGlJournalDAO.chkPeriodAvailable(objJournal);
        if (chkPeriodAvailable) {
          if (DocStatus.equals("DR")) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
            bundle.setResult(result);
            return;
          }

          else {
            // check the status of record ,status is other than draft check current date and
            // accounting date year is
            // same then check period is open for the current date then allow by updating
            // the accounting date as current date,then do not
            // allow to submit

            if (UtilityDAO.getYearId(currentDate, clientId)
                .equals(UtilityDAO.getYearId(objJournal.getAccountingDate(), clientId))) {
              Boolean isPeriodOpen = Utility.checkOpenPeriod(currentDate,
                  orgId.equals("0") ? vars.getOrg() : orgId, objJournal.getClient().getId());

              if (!isPeriodOpen) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
                bundle.setResult(result);
                return;
              } else {
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                Date now = new Date();
                Date todaydate = dateFormat.parse(dateFormat.format(now));
                objJournal.setAccountingDate(todaydate);
                String strcPeriodId = UtilityDAO.getPeriod(dateFormat.format(now), orgId);
                objJournal.setPeriod(OBDal.getInstance()
                    .get(org.openbravo.model.financialmgmt.calendar.Period.class, strcPeriodId));
              }

            } else {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
              bundle.setResult(result);
              return;
            }
          }
        }

        /* check sum of same unique code debit value is more than funds available */
        if (objJournal.getEFINBudgetManencum() == null) {
          for (GLJournalLine line : objJournal.getFinancialMgmtGLJournalLineList()) {
            AccountingCombination uniqueCode = line.getAccountingCombination();
            if (uniqueCode.getEfinDimensiontype().equals("E")) {
              EfinBudgetInquiry budgetInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(uniqueCode,
                  objJournal.getEFINBudgetDefinition());
              sql = "select line.c_validcombination_id,  sum(amtacctdr) as sum "
                  + " from gl_journalline line "
                  + " join gl_journal header on line.gl_journal_id = header.gl_journal_id "
                  + " where header.gl_journal_id  ='" + objJournal.getId()
                  + "'  and  amtacctdr > 0 and line.c_validcombination_id ='" + uniqueCode.getId()
                  + "'" + " group by line.c_validcombination_id  ";
              ps = conn.prepareStatement(sql);
              rs = ps.executeQuery();
              while (rs.next()) {
                log.debug("sum:" + rs.getBigDecimal("sum"));
                if (budgetInq != null
                    && budgetInq.getFundsAvailable().compareTo(rs.getBigDecimal("sum")) < 0) {
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
        }

        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(objJournal);

        // check the debit amt equal to remaining amount in prepayment inv
        if (objJournal.getDocumentStatus().equals("DR")) {
          if (objJournal.isEfinAdjInvoice() && objJournal.getEfinCInvoice() != null) {
            for (GLJournalLine line : objJournal.getFinancialMgmtGLJournalLineList()) {
              if (line.getDebit().compareTo(new BigDecimal(0)) > 0)
                chkTotalDebitAmt = SimpleGlJournalDAO.checkAppliedAmt(objJournal.getEfinCInvoice(),
                    line.getAccountingCombination().getId(), line.getDebit());
              if (chkTotalDebitAmt) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Appamt_Greater@");
                bundle.setResult(result);
                return;
              }

            }
          }
        }
        // if uniquecode not matched with prepayment invoice uniquecode then throw the error
        if (objJournal.isEfinAdjInvoice() && objJournal.getEfinCInvoice() != null) {
          accCombinationList = GlJournalLineEventDAO
              .getUniqueCodeListUsingInv(objJournal.getEfinCInvoice());
          for (GLJournalLine line : objJournal.getFinancialMgmtGLJournalLineList()) {
            if (line.getCredit().compareTo(new BigDecimal(0)) > 0) {
              if (accCombinationList != null && accCombinationList.size() > 0) {
                List<AccountingCombination> invList = accCombinationList.stream()
                    .filter(a -> a.getId().equals(line.getAccountingCombination().getId()))
                    .collect(Collectors.toList());

                if (invList.size() == 0) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Efin_Uniquecode_Notmatch@");
                  bundle.setResult(result);
                  return;
                }

              }

            }
          }
        }
        // check the credit amt equal to remaining amount in prepayment inv
        if (objJournal.getDocumentStatus().equals("DR")) {
          if (objJournal.isEfinAdjInvoice() && objJournal.getEfinCInvoice() != null) {
            for (GLJournalLine line : objJournal.getFinancialMgmtGLJournalLineList()) {
              if (line.getCredit().compareTo(new BigDecimal(0)) > 0)
                chkTotalCreditAmt = SimpleGlJournalDAO.checkAppliedAmtUsingCredit(
                    objJournal.getEfinCInvoice(), line.getAccountingCombination().getId(),
                    line.getCredit());
              if (chkTotalCreditAmt) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Appamt_Greater@");
                bundle.setResult(result);
                return;
              }

            }
          }
        }
      }

      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_REJ")
          && !DocStatus.equals("ESCM_RA")) {
        if (objJournal.getEutNextRole() != null) {
          java.util.List<EutNextRoleLine> li = objJournal.getEutNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (objJournal.getEutNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              DocumentTypeE.JOURNAL_ENTRIES.getDocumentTypeCode());

        }
        if (!allowUpdate && !allowDelegation) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }

      if ((!vars.getRole().equals(objJournal.getEFINRole().getId())) && (DocStatus.equals("DR"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (!errorFlag) {
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("EFIN_WFA") && DocAction.equals("AP")) {
          appstatus = "AP";
        }

        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, objJournal, appstatus,
            comments, currentDate, vars, nextApproval, Lang, bundle);
        if (count == 3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
          bundle.setResult(result);
          return;
        } else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsgs);
          bundle.setResult(result);
          return;
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_ErroFlag_Encumbarance_lines@");
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      log.debug("Exeception in GLJournalSubmit Process:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to update header status
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param objJournal
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramnextApproval
   * @param Lang
   * @param bundle
   * @return
   */
  private int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, GLJournal objJournal, String appstatus, String comments, Date currentDate,
      VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle) {
    String JournalId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = sa.elm.ob.finance.util.AlertWindow.GlJournal;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater;
    VariablesSecureApp varsAux = bundle.getContext().toVars();
    Currency currency = null;
    BigDecimal conversionrate = BigDecimal.ZERO;
    Connection conn = OBDal.getInstance().getConnection();
    boolean reserve = false;
    String doctype = Resource.GLJOURNAL_RULE;
    boolean hasDelegation = Boolean.FALSE;
    String delegatedFromRole_res = "";
    Role submittedRoleObj = null;
    String submittedRoleOrgId = null;
    // find the submitted role org/branch details

    if (objJournal.getEutNextRole() != null) {
      if (objJournal.getEfinSubmittedRole() != null
          && objJournal.getEfinSubmittedRole().getEutReg() != null) {
        submittedRoleOrgId = objJournal.getEfinSubmittedRole().getEutReg().getId();
      } else {
        submittedRoleOrgId = orgId;
      }
    } else if (objJournal.getEutNextRole() == null) {
      submittedRoleObj = OBContext.getOBContext().getRole();
      if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
        submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
      } else {
        submittedRoleOrgId = orgId;
      }
    }

    if (objJournal.getDocumentStatus().equals("DR")) {
      objCreater = objUser;
      objJournal.setEFINRole(OBDal.getInstance().get(Role.class, vars.getRole()));
      objJournal.setCreatedBy(objCreater);

      if (objJournal.isEfinAdjInvoice() && objJournal.getEfinCInvoice() != null) {
        currency = FinanceUtils.getCurrency(orgId, objJournal.getEfinCInvoice());
        // get conversion rate
        conversionrate = FinanceUtils.getConversionRate(conn, orgId, objJournal.getEfinCInvoice(),
            currency);
        SimpleGlJournalDAO.updateAppliedAmountToUsedAmount(objJournal, conversionrate, false);
        SimpleGlJournalDAO.updatePrepaymentUsedAmount(objJournal.getId(), conversionrate, false);

      }

    } else {
      objCreater = objJournal.getCreatedBy();
    }

    NextRoleByRuleVO nextApproval = paramnextApproval;

    try {
      OBContext.setAdminMode();

      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      String fromUser = userId;
      String fromRole = roleId;
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      isDirectApproval = isDirectApproval(objJournal.getId(), roleId);
      String documentRule = Resource.GLJOURNAL_RULE;
      @SuppressWarnings("unused")
      int cont = 0;

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      log.debug("queryAlertRule" + queryAlertRule.getWhereAndOrderBy());
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      if ((objJournal.getEutNextRole() != null)) {

        fromUser = userId;
        fromRole = roleId;
      }

      if ((objJournal.getEutNextRole() == null)) {
        nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
            submittedRoleOrgId, fromRole, fromUser, documentRule, objJournal.getTotalDebitAmount());
      } else {
        if (isDirectApproval) {

          nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
              submittedRoleOrgId, fromRole, fromUser, documentRule,
              objJournal.getTotalDebitAmount());

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    userRole.list().get(0).getUserContact().getId(), documentRule, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    delegatedFromRole, delegatedToRole, fromUser, documentRule,
                    objJournal.getTotalDebitAmount());
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                submittedRoleOrgId, delegatedFromRole, fromUser, documentRule,
                objJournal.getTotalDebitAmount());

          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromUser, documentRule, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                documentRule, objJournal.getTotalDebitAmount());
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        count = 3;
      }
      // if Role doesnt has any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -2;
      } else if (nextApproval != null && nextApproval.hasApproval()) {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        String appResource = null;

        appResource = "finance.gl.journal";

        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        // get old nextrole line user and role list
        forwardDao.getNextRoleLineList(objJournal.getEutNextRole(), documentRule);

        objJournal.setUpdated(new java.util.Date());
        objJournal.setUpdatedBy(OBContext.getOBContext().getUser());
        if ((objJournal.getDocumentStatus().equals("REJ")
            || objJournal.getDocumentStatus().equals("DR"))
            && objJournal.getEfinAction().equals("CO")) {
          objJournal.setEFINRevoke(true);
        } else
          objJournal.setEFINRevoke(false);
        objJournal.setEfinAction("AP");
        objJournal.setDocumentStatus("EFIN_WFA");
        objJournal.setEutNextRole(nextRole);
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + objJournal.getId()
                  + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
            }
          }

          String Description = sa.elm.ob.finance.properties.Resource.getProperty(appResource, Lang)
              + " " + objCreater.getName();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {

            sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(objJournal.getId(),
                objJournal.getDocumentNo(), objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                objJournal.getClient().getId(), Description, "NEW", alertWindow, appResource,
                Constants.GENERIC_TEMPLATE);

            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_106'");
            if (delegationln != null && delegationln.list().size() > 0) {

              sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(objJournal.getId(),
                  objJournal.getDocumentNo(), delegationln.list().get(0).getRole().getId(),
                  delegationln.list().get(0).getUserContact().getId(),
                  objJournal.getClient().getId(), Description, "NEW", alertWindow, appResource,
                  Constants.GENERIC_TEMPLATE);
              log.debug("del role>" + delegationln.list().get(0).getRole().getId());

              includeRecipient.add(delegationln.list().get(0).getRole().getId());
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationln.list().get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(iterator.next(), null, clientId,
              alertWindow);
        }
        objJournal.setEfinAction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
      } else {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        objJournal.setUpdated(new java.util.Date());
        objJournal.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;

        if (objJournal.getCreatedBy().getADUserRolesList().size() > 0
            && objJournal.getEFINRole() != null) {
          objCreatedRole = objJournal.getEFINRole();
        }
        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.referenceSearchKey='" + objJournal.getId() + "' and e.alertStatus='NEW'");
        if (alertQuery.list().size() > 0) {
          for (Alert objAlert : alertQuery.list()) {
            objAlert.setAlertStatus("SOLVED");
          }
        }
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }

        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(iterator.next(), null, clientId,
              alertWindow);
        } // set alert for requester
        String appResource = null;

        appResource = "finance.gl.journalapp";

        String Description = sa.elm.ob.finance.properties.Resource.getProperty(appResource, Lang)
            + " " + objUser.getName();
        sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(objJournal.getId(),
            objJournal.getDocumentNo(), objJournal.getEFINRole().getId(),
            objJournal.getCreatedBy().getId(), objJournal.getClient().getId(), Description, "NEW",
            alertWindow, appResource, Constants.GENERIC_TEMPLATE);
        objJournal.setEfinAction("PD");
        objJournal.setDocumentStatus("CO");
        objJournal.setEutNextRole(null);

        count = 1;

      }

      JournalId = objJournal.getId();

      if (objJournal.getEutNextRole() == null) {
        try {
          // Call GL_Journal_Post method from the database.
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(null);
          parameters.add(JournalId);
          final String procedureName = "gl_journal_post";
          CallStoredProcedure mm = CallStoredProcedure.getInstance();
          mm.call(procedureName, parameters, null, false, false);
        }

        catch (Exception e) {
          OBDal.getInstance().rollbackAndClose();
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          bundle.setResult(error.getMessage());
        }
        // OBDal.getInstance().refresh(objJournal);

      }
      for (GLJournalLine jline : objJournal.getFinancialMgmtGLJournalLineList()) {
        // Recover again the object to avoid problems with Dal
        GLJournalLine journalLine = OBDal.getInstance().get(GLJournalLine.class, jline.getId());
        if (journalLine.isOpenItems() && journalLine.getRelatedPayment() == null) {
          // Create bundle
          vars = new VariablesSecureApp(varsAux.getUser(), varsAux.getClient(), varsAux.getOrg(),
              varsAux.getRole(), varsAux.getLanguage());
          ProcessBundle pb = new ProcessBundle("91527EBD804949E6AAA72CBB9C889269", vars)
              .init((ConnectionProvider) conn);
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
            throw new OBException("@FIN_PaymentFromJournalError@ " + journalLine.getLineNo() + " - "
                + myMessage.getMessage());
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

      if (!StringUtils.isEmpty(JournalId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", JournalId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.GL_JOURNAL_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.GL_JOURNAL_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.GL_JOURNAL_DOCACTION_COLUMN);

        SimpleGlJournalDAO.glJournalHistory(historyData);

      }

      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), documentRule);

      // Create temporary encumbrance during reserve role approval
      if (count != 3 && count != -2) {

        DelegatedNextRoleDAO delegationDao = new DelegatedNextRoleDAOImpl();
        hasDelegation = delegationDao.checkDelegation(currentDate, roleId, doctype);

        if (hasDelegation) {
          delegatedFromRole_res = delegationDao.getDelegatedFromRole(roleId, doctype, userId);
        }

        reserve = UtilityDAO.getReserveFundsRole(doctype, fromRole,
            objJournal.getOrganization().getId(), objJournal.getId(), BigDecimal.ZERO);

        // Check delegated from role is reserve role
        if (hasDelegation && !reserve) {
          reserve = UtilityDAO.getReserveFundsRole(doctype, delegatedFromRole_res,
              objJournal.getOrganization().getId(), objJournal.getId(), BigDecimal.ZERO);
        }

        // If reserving role and encumbrance not created
        // OR at final level approval and encumbrance not created
        if ((reserve && objJournal.getEFINBudgetManencum() == null)
            || ((!reserve && nextApproval != null && nextApproval.getNextRoleId() == null
                && nextApproval.getErrorMsg() == null)
                && objJournal.getEFINBudgetManencum() == null)) {
          // do not insert temporary encumbrance for pre-payment invoice adjust gl-journal
          if (!objJournal.isEfinAdjInvoice()) {
            List<GLJournalLine> glLineList = objJournal.getFinancialMgmtGLJournalLineList().stream()
                .filter(a -> a.getDebit().compareTo(BigDecimal.ZERO) > 0
                    && a.getAccountingCombination().getEfinDimensiontype().equals("E"))
                .collect(Collectors.toList());
            if (glLineList.size() > 0) {
              SimpleGlJournalDAO.insertTemporaryEncumbrance(objJournal, glLineList);
              OBDal.getInstance().flush();
            }
          }

        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exception in updateHeaderStatus in gl journal:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      /* OBContext.restorePreviousMode(); */
    }
    return count;
  }

  /**
   * This method is to check is direct approval
   * 
   * @param RequestId
   * @param roleId
   * @return
   */
  @SuppressWarnings("unused")
  private boolean isDirectApproval(String RequestId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(ord.gl_journal_id) from gl_journal ord join eut_next_role rl on "
          + "ord.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and ord.gl_journal_id = ? and li.ad_role_id =?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, RequestId);
        ps.setString(2, roleId);

        rs = ps.executeQuery();

        if (rs.next()) {
          if (rs.getInt("count") > 0)
            return true;
          else
            return false;
        } else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return false;
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
  }

}
