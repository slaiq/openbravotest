package sa.elm.ob.finance.ad_process.BudgetAdjustment;

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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.actionHandler.dao.AdjustmentAddLineHandlerDao;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.DAO.EncumbranceProcessDAO;
import sa.elm.ob.finance.util.autoreleasefunds.AutoReleaseFundsImpl;
import sa.elm.ob.finance.util.autoreleasefunds.AutoReleaseFundsService;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gopalakrishnan on 16/09/2017
 * 
 */

public class BudgetAdjustmentProcess extends DalBaseProcess {

  /**
   * Budget Adjustment submit process
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetAdjustmentProcess.class);
  private final OBError obError = new OBError();
  private HashMap<BudgetAdjustmentLine, BigDecimal> releaseLineMap = new HashMap<BudgetAdjustmentLine, BigDecimal>();
  private List<EFINFundsReqLine> lineList = new ArrayList<EFINFundsReqLine>();
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @SuppressWarnings("rawtypes")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    Boolean allowApprove = false;

    log.debug("entering into BudgetAdditionProcess");
    try {
      OBContext.setAdminMode();
      final String BudgetAdjustementid = (String) bundle.getParams().get("Efin_Budgetadj_ID");
      String comments = (String) bundle.getParams().get("comments").toString();
      int i = 0, count = 0;
      JSONObject jsonObject = null;

      BudgetAdjustment budgetAddjustment = OBDal.getInstance().get(BudgetAdjustment.class,
          BudgetAdjustementid);
      final String orgId = budgetAddjustment.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      final String docAction = budgetAddjustment.getAction();
      final String docStatus = budgetAddjustment.getDocumentStatus();
      final String clientId = (String) bundle.getContext().getClient();
      String str_budget_reference = budgetAddjustment.getEfinBudgetint() == null ? ""
          : budgetAddjustment.getEfinBudgetint().getId();
      boolean errorFlag = false, checkDistUniquecodePresntOrNot = false;
      String appstatus = "";
      Date currentDate = new Date();
      String documentType = Resource.BUDGET_ADJUSTMENT_RULE;
      String isDistribute = "", disOrg = "";
      boolean isWarn = false;
      boolean isPeriodOpen = false;

      // Budget Definition closed validation
      if (budgetAddjustment.getEfinBudgetint() != null
          && budgetAddjustment.getEfinBudgetint().getStatus().equals("CL")) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_Budget_Definition_Closed"));
        bundle.setResult(result);
        return;
      }
      // Pre Close Year Validation
      if (budgetAddjustment.getEfinBudgetint() != null
          && budgetAddjustment.getEfinBudgetint().isPreclose()) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_PreClose_Year_Validation"));
        bundle.setResult(result);
        return;
      }

      // Check lines are present in budget adjustment before submitting
      if (docStatus.equals("DR") && budgetAddjustment.getAction().equals("CO")) {
        if (budgetAddjustment.getEfinBudgetAdjlineList().size() == 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("Efin_budget_AdjustmentLines_noline"));
          bundle.setResult(result);
          return;
        }
      }

      /*
       * int ruleCount = BudgetRevisionRuleValidation.checkRuleValidation(BudgetAdjustementid, "BA",
       * clientId, str_budget_reference);
       */

      // OBQuery<BudgetAdjustmentLine> adjustmentLine = OBDal.getInstance().createQuery(
      // BudgetAdjustmentLine.class, " as e where e.efinBudgetadj.id= :efinBudgetadjID ");
      // adjustmentLine.setNamedParameter("efinBudgetadjID", BudgetAdjustementid);
      // List<BudgetAdjustmentLine> adjustmentLineList = adjustmentLine.list();
      // if (adjustmentLineList.size() > 0) {
      // for (BudgetAdjustmentLine bal : adjustmentLineList) {
      // JSONObject json = BudgetAdjustmenDAO.checkFundsAval(bal);
      // if (json.get("is990Acct").equals("true")) {
      // if (json.get("isFundGreater").equals("true")) {
      // if (json.get("isWarn").equals("true")) {
      // bal.setFailureReason(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
      // bal.setAlertStatus("Warning");
      // OBDal.getInstance().save(bal);
      // isWarn = true; //
      // errorFlag = true;
      // } else {
      // bal.setFailureReason(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
      // bal.setAlertStatus("Failed");
      // OBDal.getInstance().save(bal);
      // errorFlag = true;
      // }
      // }
      // }
      // }
      //
      // if (errorFlag == true) {
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // OBMessageUtils.messageBD("Efin_budget_AdjustmentLines_Failed"));
      // bundle.setResult(result);
      // return;
      // }
      // }

      // check iswarn flag in budget revision rules

      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(budgetAddjustment.getClient().getId());
      parameters.add(budgetAddjustment.getBudgetType().getId());
      parameters.add(budgetAddjustment.getBudgetType().getEfinBudgettype());
      parameters.add(budgetAddjustment.getEfinBudgetint() != null
          ? budgetAddjustment.getEfinBudgetint().getId()
          : null);
      parameters.add(budgetAddjustment.getId());
      parameters.add(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
      if ("C".equals(budgetAddjustment.getBudgetType().getEfinBudgettype())) {
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_CostAmount").replace("@", ""));
        parameters
            .add(OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds").replace("@", ""));
      } else {
        parameters
            .add(OBMessageUtils.messageBD("Efin_BudgetAdj_DecreaseAmt_Greater").replace("@", ""));
        parameters
            .add(OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost").replace("@", ""));
      }
      parameters.add(budgetAddjustment.getDocumentStatus());
      BigDecimal resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
          .call("efin_budgetadj_commonvalid", parameters, null);

      int resultCount = resultCount1.intValue();
      if (resultCount == 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_budget_AdjustmentLines_Failed"));
        bundle.setResult(result);
        return;
      }

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approve the record without refreshing the page
      if (budgetAddjustment.getEUTForward() != null) {
        allowApprove = forwardDao.allowApproveReject(budgetAddjustment.getEUTForward(), userId,
            roleId, Resource.BUDGET_ADJUSTMENT_RULE);
      }
      if (budgetAddjustment.getEUTReqmoreinfo() != null
          || ((budgetAddjustment.getEUTForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      if (docStatus.equals("DR") && budgetAddjustment.getAction().equals("CO")) {
        int submitAllowed = CommonValidations.checkUserRoleForSubmit("Efin_Budgetadj",
            vars.getUser(), vars.getRole(), BudgetAdjustementid, "Efin_Budgetadj_ID");
        if (submitAllowed == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      // Task #8198
      // check submitted role have the branch details or not
      if (docStatus.equals("DR")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // find the submitted role org/branch details

      if (budgetAddjustment.getNextRole() != null) {
        if (budgetAddjustment.getEfinSubmittedRole() != null
            && budgetAddjustment.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = budgetAddjustment.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (budgetAddjustment.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }
      // check role is present in document rule or not
      if (docStatus.equals("DR")) {
        Boolean chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(
            OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, userId, roleId,
            Resource.BUDGET_ADJUSTMENT_RULE, BigDecimal.ZERO);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }

      if ((!vars.getRole().equals(budgetAddjustment.getRole().getId()))
          && (docStatus.equals("DR") || docStatus.equals("EFIN_RJD"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      checkDistUniquecodePresntOrNot = FundsRequestActionDAO.checkDistUniquecodePresntOrNot(null,
          budgetAddjustment, conn, BudgetAdjustementid, null);
      if (!checkDistUniquecodePresntOrNot) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_Revision_error"));
        bundle.setResult(result);
        return;
      }
      ps = conn.prepareStatement(
          " select isdistribute,dislinkorg from efin_budgetadjline where efin_budgetadj_id = '"
              + BudgetAdjustementid + "' ");
      rs = ps.executeQuery();
      if (rs.next()) {
        isDistribute = rs.getString("isdistribute");
        disOrg = rs.getString("dislinkorg");
      }
      if (isDistribute.equals("Y") && disOrg != null) {
        String AccountDate = new SimpleDateFormat("dd-MM-yyyy")
            .format(budgetAddjustment.getAccountingDate());
        OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
            "as e where e.organization.id ='0'");
        List<Calendar> calendarQueryList = calendarQuery.list();
        if (calendarQueryList.size() == 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoFiscalCalendar"));
        }
        Calendar calendar = calendarQueryList.get(0);
        String SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_fundsreq",
            calendar.getId(), "0", true);
        if (SequenceNo.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_Docseq"));
        }

      }
      // Check transaction period is opened or not
      if (docAction.equals("CO")) {
        isPeriodOpen = Utility.checkOpenPeriod(budgetAddjustment.getTRXDate(), orgId, clientId);
        if (!isPeriodOpen) {
          if (docStatus.equals("DR")) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
            bundle.setResult(result);
            return;
          } else {
            // check the status of record ,status is other than draft check current date and
            // accounting date year is
            // same then check period is open for the current date then allow by updating
            // the accounting date as current date,then do not
            // allow to submit

            if (UtilityDAO.getYearId(currentDate, clientId)
                .equals(UtilityDAO.getYearId(budgetAddjustment.getTRXDate(), clientId))) {
              isPeriodOpen = Utility.checkOpenPeriod(currentDate,
                  orgId.equals("0") ? vars.getOrg() : orgId, budgetAddjustment.getClient().getId());

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
                budgetAddjustment.setAccountingDate(todaydate);
                budgetAddjustment.setTRXDate(todaydate);
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
      }

      if (docStatus.equals("DR") || docStatus.equals("EFIN_RJD")) {
        // Check Auto release funds validations and funds available validation
        AutoReleaseFundsService autoRelease = new AutoReleaseFundsImpl();
        if (!autoRelease.checkBCUFundsAvailable(null, budgetAddjustment, null, releaseLineMap)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("Efin_Revision_error"));
          bundle.setResult(result);
          return;
        }

        // release from 999 to 990 in case of decrease and 990 doesn't have enough funds available
        // in 990

        autoRelease.insertReleaseInBudgetDistribution(null, budgetAddjustment, null, releaseLineMap,
            false, lineList);

      }

      // // pre validation to check funds available
      // Boolean validProcess = CommonValidations.checkValidations(BudgetAdjustementid,
      // "BudgetAdjustment", clientId, "CO", isWarn);
      //
      // if (!validProcess) {
      // // revert the changes in budget enquiry
      //
      // for (EFINFundsReqLine line : lineList) {
      // BudgetRevisionDAO.revertBudgetInquiry(line.getEfinFundsreq(), line);
      // EFINFundsReq line_FRM_obj = line.getEfinFundsreq();
      // line_FRM_obj.setDocumentStatus("DR");
      // line_FRM_obj.setAction("CO");
      // OBDal.getInstance().save(line_FRM_obj);
      // OBDal.getInstance().remove(line);
      // }
      // for (EFINFundsReq fundsReq : budgetAddjustment.getEFINFundsReqList()) {
      // fundsReq.setDocumentStatus("DR");
      // fundsReq.setAction("CO");
      // fundsReq.setReserve(false);
      // OBDal.getInstance().save(fundsReq);
      // OBDal.getInstance().remove(fundsReq);
      // }
      //
      // errorFlag = true;
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // OBMessageUtils.messageBD("Efin_budget_AdjustmentLines_Failed"));
      // bundle.setResult(result);
      // return;
      // }
      // budget Revision Rule Validation
      String val_query = "select string_agg(em_efin_uniquecode,',') from efin_budgetadjline ln "
          + " join c_validcombination cv on cv.c_validcombination_id =ln.c_validcombination_id "
          + " where decrease =0 and increase=0 and ln.efin_budgetadj_id= :bidgetAdjID ";
      SQLQuery sqlValQuery = OBDal.getInstance().getSession().createSQLQuery(val_query);
      sqlValQuery.setParameter("bidgetAdjID", BudgetAdjustementid);
      List queryvalList = sqlValQuery.list();
      if (sqlValQuery != null && queryvalList.size() > 0) {
        Object zeroCodes = queryvalList.get(0);
        if (zeroCodes != null) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("Efin_Adjustment_Zero").replace("@", zeroCodes.toString()));
          bundle.setResult(result);
          return;
        }
      }

      String query = " select bt.efin_budgetint_id,bl.efin_budgetinquiry_id ,"
          + " cv.account_id,al.increase,al.decrease,  "
          + " cv.c_validcombination_id,al.efin_budgetadjline_id  " + " from efin_budgetadjline al "
          + "  left join c_validcombination cv on cv.c_validcombination_id = al.c_validcombination_id "
          + "    left join efin_budgetint bt on bt.efin_budgetint_id=:reference1  "
          + "    left join efin_budgetinquiry bl on  cv.c_validcombination_id =bl.c_validcombination_id and "
          + "    bl.efin_budgetint_id=:reference2 " + "    where al.ad_client_id =:clientId "
          + "    and al.efin_budgetadj_id =:budgetAdjId and al.fundsreserved='N' ";
      /*
       * // update line status and failure reason else if (validProcess) { for (BudgetAdjustmentLine
       * objLine : budgetAddjustment.getEfinBudgetAdjlineList()) {
       * objLine.setAlertStatus("Success"); objLine.setFailureReason(null); } }
       */
      /* Assign the Bundle Parameters */
      // check adjustment amount zero
      NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(
          OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, roleId, userId,
          documentType, 0.00);
      if (!errorFlag) {
        if ((docStatus.equals("DR") || docStatus.equals("EFIN_RJD")) && docAction.equals("CO")) {
          ps = conn.prepareStatement(
              " select count(efin_budgetadjline.dislinkorg) as count from efin_budgetadjline where dislinkorg  not in (\n"
                  + "select dislinkorg from efin_budgetadj where efin_budgetadj_id =  '"
                  + BudgetAdjustementid + "')\n"
                  + "and efin_budgetadjline.isdistribute = 'Y' and efin_budgetadjline.dislinkorg is not null and \n"
                  + "efin_budgetadjline.efin_budgetadj_id = '" + BudgetAdjustementid + "'");
          log.debug("sql" + ps.toString());
          rs = ps.executeQuery();
          if (rs.next()) {
            count = rs.getInt("count");
            if (count > 0) {
              String updateQuery = "  update efin_budgetadj set dislinkorg=null where efin_budgetadj_id=?";
              ps = conn.prepareStatement(updateQuery);
              ps.setString(1, BudgetAdjustementid);
              ps.executeUpdate();
            }
          }
          appstatus = "SUB";
        } else if (docStatus.equals("EFIN_IP") && docAction.equals("AP")) {
          appstatus = "AP";
        }

        if (docStatus.equals("DR") || docStatus.equals("EFIN_RJD")) {

          SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
          sqlQuery.setParameter("reference1", str_budget_reference);
          sqlQuery.setParameter("reference2", str_budget_reference);
          sqlQuery.setParameter("clientId", clientId);
          sqlQuery.setParameter("budgetAdjId", BudgetAdjustementid);
          log.debug("sql" + str_budget_reference);
          log.debug("sql" + str_budget_reference);
          log.debug("sql" + clientId);
          log.debug("adj" + BudgetAdjustementid);
          log.debug("sql" + query.toString());
          List queryList = sqlQuery.list();
          if (sqlQuery != null && queryList.size() > 0) {
            for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
              Object[] row = (Object[]) iterator.next();
              if (row[0] != null) {
                if (row[1] != null) {
                  log.debug("row" + (BigDecimal) row[4]);
                  if (((BigDecimal) row[4]).compareTo(new BigDecimal(0)) > 0) {

                    EncumbranceProcessDAO.insertEncumbranceLine(conn, null, budgetAddjustment,
                        ((BigDecimal) row[4]), row[5].toString(), false);
                    BudgetAdjustmentLine objAdjLine = OBDal.getInstance()
                        .get(BudgetAdjustmentLine.class, row[6].toString());
                    objAdjLine.setFundsreserved(true);
                    OBDal.getInstance().save(objAdjLine);
                  }
                  /*
                   * if (((BigDecimal) row[4]).compareTo(BigDecimal.ZERO) == 1) {
                   * BudgetAdjustmentLine objAdjLine = OBDal.getInstance()
                   * .get(BudgetAdjustmentLine.class, row[6].toString()); EfinBudgetInquiry
                   * objInquiryLine = OBDal.getInstance() .get(EfinBudgetInquiry.class,
                   * row[1].toString()); // check funds should not less than zero after adjustment
                   * BigDecimal new_funds = objInquiryLine.getFundsAvailable()
                   * .subtract((BigDecimal) row[4]); if (BigDecimal.ZERO.compareTo(new_funds) == 1)
                   * { OBDal.getInstance().rollbackAndClose(); OBError result =
                   * OBErrorBuilder.buildMessage(null, "error",
                   * OBMessageUtils.messageBD("Efin_no_funds_available").replaceAll("@",
                   * objAdjLine.getAccountingCombination().getEfinUniqueCode()));
                   * bundle.setResult(result); return; }
                   * objInquiryLine.setObdecAmt(objInquiryLine.getObdecAmt().add((BigDecimal)
                   * row[4])); objAdjLine.setFundsreserved(true);
                   * OBDal.getInstance().save(objInquiryLine);
                   * objAdjLine.setBudgetInquiryLine(objInquiryLine);
                   * objAdjLine.setCostcurbudget(AdjustmentAddLineHandlerDao.getCostCurrentBudget(
                   * objAdjLine.getAccountingCombination(), str_budget_reference));
                   * OBDal.getInstance().save(objAdjLine); }
                   */
                }
              } else {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    OBMessageUtils.messageBD("Efin_Budget_Not_Defined"));
                bundle.setResult(result);
                return;
              }
              if ((i % 100) == 0) {
                OBDal.getInstance().flush();
                // OBDal.getInstance().getSession().clear();
              }
              i++;
              // row[1]
            }
          }
        }

        jsonObject = updateHeaderStatus(conn, clientId, orgId, roleId, userId, budgetAddjustment,
            appstatus, comments, currentDate, vars, nextApproval, Lang, documentType);
      }
      count = jsonObject.getInt("count");
      if (count == 2) {
        String accountingcombination = "";
        String sql = "";
        sql = "select c_validcombination_id from efin_budgetadjline where efin_budgetadj_id = '"
            + BudgetAdjustementid + "'";
        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();

        while (rs.next()) {

          accountingcombination = rs.getString("c_validcombination_id");
          OBQuery<EfinBudgetManencum> chklineexists = OBDal.getInstance().createQuery(
              EfinBudgetManencum.class, "as e where e.sourceref = '" + BudgetAdjustementid + "'");

          if (chklineexists.list().size() > 0) {
            EncumbranceProcessDAO.insertModificationEntry(conn, null, budgetAddjustment,
                accountingcombination);

          }
        }
        SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
        sqlQuery.setParameter("reference1", str_budget_reference);
        sqlQuery.setParameter("reference2", str_budget_reference);
        sqlQuery.setParameter("clientId", clientId);
        sqlQuery.setParameter("budgetAdjId", BudgetAdjustementid);
        List queryList = sqlQuery.list();
        if (sqlQuery != null && queryList.size() > 0) {
          for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            if (row[0] != null) {
              if (row[1] != null) {
                BudgetAdjustmentLine objAdjLine = OBDal.getInstance()
                    .get(BudgetAdjustmentLine.class, row[6].toString());
                EfinBudgetInquiry objInquiryLine = OBDal.getInstance().get(EfinBudgetInquiry.class,
                    row[1].toString());
                // check funds should not less than zero after adjustment
                BigDecimal new_funds = objInquiryLine.getFundsAvailable()
                    .subtract((BigDecimal) row[4]);
                if (BigDecimal.ZERO.compareTo(new_funds) == 1) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      OBMessageUtils.messageBD("Efin_no_funds_available").replaceAll("@",
                          objAdjLine.getAccountingCombination().getEfinUniqueCode()));
                  bundle.setResult(result);
                  return;
                }
                objInquiryLine.setObdecAmt(objInquiryLine.getObdecAmt().add((BigDecimal) row[4]));
                objInquiryLine.setObincAmt(objInquiryLine.getObincAmt().add((BigDecimal) row[3]));

                OBDal.getInstance().save(objInquiryLine);
                objAdjLine.setBudgetInquiryLine(objInquiryLine);
                objAdjLine.setCostcurbudget(AdjustmentAddLineHandlerDao.getCostCurrentBudget(
                    objAdjLine.getAccountingCombination(), str_budget_reference));
                objAdjLine.setFundsreserved(true);
                OBDal.getInstance().save(objAdjLine);

              }
            } else {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  OBMessageUtils.messageBD("Efin_Budget_Not_Defined"));
              bundle.setResult(result);
              return;
            }
            if ((i % 100) == 0) {
              OBDal.getInstance().flush();
              // OBDal.getInstance().getSession().clear();
            }
            i++;
            // row[1]
          }
        }
      } else if (count == 3) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
        bundle.setResult(result);
        return;
      }

      budgetAddjustment.setProcessed(true);
      OBDal.getInstance().save(budgetAddjustment);

      // direct distribute
      if (count == 2) {
        FundsRequestActionDAO.directDistribute(conn, "BADJ", budgetAddjustment.getId(), vars,
            clientId, roleId);
      }
      OBDal.getInstance().flush();
      if (isWarn) {
        OBError result = OBErrorBuilder.buildMessage(null, "warning",
            OBMessageUtils.messageBD("Efin_budget_Adjustment_Warning"));
        bundle.setResult(result);
        return;
      }
      if (count > 0) {
        if (resultCount == 2) {
          OBError result = OBErrorBuilder.buildMessage(null, "warning",
              OBMessageUtils.messageBD("Efin_budget_Adjustment_Warning"));
          bundle.setResult(result);
          return;
        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@EFIN_BudgetAdjustment_Success@");
          bundle.setResult(result);
          return;
        }
      } else {
        errorFlag = false;
      }

      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed");
      }
      bundle.setResult(obError);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {

      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBDal.getInstance().commitAndClose();
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
   * @param objAdjustment
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramNextApproval
   * @param Lang
   * @param documentType
   * @return
   */
  private JSONObject updateHeaderStatus(Connection con, String clientId, String orgId,
      String roleId, String userId, BudgetAdjustment objAdjustment, String appstatus,
      String comments, Date currentDate, VariablesSecureApp vars,
      NextRoleByRuleVO paramNextApproval, String Lang, String documentType) {
    String adjustMentId = null, pendingapproval = null, reserveRoleId = "";
    JSONObject return_obj = new JSONObject();
    Boolean isDirectApproval = false, reserve = false;
    String alertRuleId = "", alertWindow = AlertWindow.BudgetAdjustment;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objAdjustment.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramNextApproval;
    try {
      OBContext.setAdminMode();
      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);

      String BudgetAdjustementid = objAdjustment.getId();
      JSONObject tableData = new JSONObject();
      tableData.put("headerColumn", ApprovalTables.Budget_Adjustment_HEADER_COLUMN);
      tableData.put("tableName", ApprovalTables.Budget_Adjustment_Table);
      tableData.put("headerId", objAdjustment.getId());
      tableData.put("roleId", roleId);
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;
      boolean isDummyRole = false;
      isDirectApproval = Utility.isDirectApproval(tableData);

      // find the submitted role org/branch details
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;

      if (objAdjustment.getNextRole() != null) {
        if (objAdjustment.getEfinSubmittedRole() != null
            && objAdjustment.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = objAdjustment.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (objAdjustment.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      /**
       * fetching from roleId and userId based on delegated user / forwarder/ direct approver
       **/
      if (objAdjustment.getNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objAdjustment.getNextRole(), userId, roleId, clientId, submittedRoleOrgId,
            Resource.BUDGET_ENTRY_RULE, isDummyRole, isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }

      } else {
        fromUser = userId;
        fromRole = roleId;
      }

      if ((objAdjustment.getNextRole() == null)) {
        // nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId, roleId, userId,
        // documentType, 0.00);
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
            Resource.BUDGET_ADJUSTMENT_RULE, BigDecimal.ZERO, fromUser, false,
            objAdjustment.getDocumentStatus());
        // check reserve role
        reserveRoleId = roleId;
        // reserve = UtilityDAO.getReserveFundsRole(documentType, roleId, orgId,
        // BudgetAdjustementid);
      } else {
        if (isDirectApproval) {
          // nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId, roleId, userId,
          // documentType, 0.00);
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
              Resource.BUDGET_ADJUSTMENT_RULE, BigDecimal.ZERO, fromUser, false,
              objAdjustment.getDocumentStatus());
          reserveRoleId = roleId;
          /*
           * reserve = UtilityDAO.getReserveFundsRole(documentType, roleId, orgId,
           * BudgetAdjustementid);
           */
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(), documentType, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, documentType, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                submittedRoleOrgId, delegatedFromRole, userId, documentType, 0.00);
            reserveRoleId = delegatedFromRole;
            // reserve = UtilityDAO.getReserveFundsRole(documentType, delegatedFromRole, orgId,
            // BudgetAdjustementid);
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromUser, documentType, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                documentType, 0.00);
          reserveRoleId = delegatedFromRole;
          /*
           * reserve = UtilityDAO.getReserveFundsRole(documentType, delegatedFromRole, orgId,
           * BudgetAdjustementid);
           */
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        return_obj.put("count", 3);
      } else if (nextApproval != null && nextApproval.hasApproval()) {
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objAdjustment.getNextRole(), Resource.BUDGET_ADJUSTMENT_RULE);
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        objAdjustment.setUpdated(new java.util.Date());
        objAdjustment.setUpdatedBy(OBContext.getOBContext().getUser());
        objAdjustment.setDocumentStatus("EFIN_IP");
        objAdjustment.setNextRole(nextRole);
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        forwardDao.getAlertForForwardedUser(objAdjustment.getId(), alertWindow, alertRuleId,
            objUser, clientId, Constants.APPROVE, objAdjustment.getDocno(), Lang, vars.getRole(),
            objAdjustment.getEUTForward(), Resource.BUDGET_ADJUSTMENT_RULE, alertReceiversMap);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          // "as e where e.referenceSearchKey='" + objAdjustment.getId()
          // + "' and e.alertStatus='NEW'");
          // if (alertQuery.list().size() > 0) {
          // for (Alert objAlert : alertQuery.list()) {
          // objAlert.setAlertStatus("SOLVED");
          // }
          // }
          String Description = sa.elm.ob.finance.properties.Resource.getProperty("finance.ba.wfa",
              Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objAdjustment.getId(), objAdjustment.getDocno(),
                objNextRoleLine.getRole().getId(), "", objAdjustment.getClient().getId(),
                Description, "NEW", alertWindow, "finance.ba.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_119'");
            if (delegationln != null && delegationln.list().size() > 0) {

              AlertUtility.alertInsertionRole(objAdjustment.getId(), objAdjustment.getDocno(),
                  delegationln.list().get(0).getRole().getId(),
                  delegationln.list().get(0).getUserContact().getId(),
                  objAdjustment.getClient().getId(), Description, "NEW", alertWindow,
                  "finance.Budget.wfa", Constants.GENERIC_TEMPLATE);

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
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
        objAdjustment.setAction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        log.debug(
            "doc sts:" + objAdjustment.getDocumentStatus() + "action:" + objAdjustment.getAction());
        return_obj.put("count", 1);
        // count = 1;
        // Waiting For Approval flow

      } else {
        OBContext.setAdminMode(true);

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objAdjustment.getNextRole(), Resource.BUDGET_ADJUSTMENT_RULE);

        ArrayList<String> includeRecipient = new ArrayList<String>();
        // nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        objAdjustment.setUpdated(new java.util.Date());
        objAdjustment.setUpdatedBy(OBContext.getOBContext().getUser());
        objAdjustment.setDocumentStatus("EFIN_AP");
        Role objCreatedRole = null;
        User objCreatedUser = OBDal.getInstance().get(User.class,
            objAdjustment.getCreatedBy().getId());
        if (objCreatedUser.getADUserRolesList().size() > 0) {
          objCreatedRole = objCreatedUser.getADUserRolesList().get(0).getRole();
        }
        /*
         * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
         * "as e where e.referenceSearchKey='" + objAdjustment.getId() +
         * "' and e.alertStatus='NEW'"); if (alertQuery.list().size() > 0) { for (Alert objAlert :
         * alertQuery.list()) { objAlert.setAlertStatus("SOLVED"); } }
         */
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        forwardDao.getAlertForForwardedUser(objAdjustment.getId(), alertWindow, alertRuleId,
            objUser, clientId, Constants.APPROVE, objAdjustment.getDocno(), Lang, vars.getRole(),
            objAdjustment.getEUTForward(), Resource.BUDGET_ADJUSTMENT_RULE, alertReceiversMap);

        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.ba.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objAdjustment.getId(), objAdjustment.getDocno(),
            objAdjustment.getRole().getId(), objAdjustment.getCreatedBy().getId(),
            objAdjustment.getClient().getId(), Description, "NEW", alertWindow,
            "finance.ba.approved", Constants.GENERIC_TEMPLATE);
        objAdjustment.setNextRole(null);
        objAdjustment.setAction("PD");
        return_obj.put("count", 2);
        // count = 2;
        // Final Approval Flow
      }

      OBDal.getInstance().save(objAdjustment);
      adjustMentId = objAdjustment.getId();
      if (!StringUtils.isEmpty(adjustMentId)) {

        if (return_obj.getInt("count") != 3) {
          JSONObject historyData = new JSONObject();
          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", adjustMentId);
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", pendingapproval);
          historyData.put("HistoryTable", ApprovalTables.Budget_Adjustment_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.Budget_Adjustment_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.Budget_Adjustment_DOCACTION_COLUMN);

          Utility.InsertApprovalHistory(historyData);
        }

      }
      // OBDal.getInstance().flush();
      // check reserve role
      reserve = UtilityDAO.getReserveFundsRole(documentType, fromRole, orgId, BudgetAdjustementid,
          BigDecimal.ZERO);
      return_obj.put("reserve", reserve);
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), documentType);

      // after approved by forwarded user removing the forward and rmi id
      if (objAdjustment.getEUTForward() != null) {
        forwardDao.setForwardStatusAsDraft(objAdjustment.getEUTForward());
        objAdjustment.setEUTForward(null);
      }
      if (objAdjustment.getEUTReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(objAdjustment.getEUTReqmoreinfo());
        objAdjustment.setEUTReqmoreinfo(null);
        objAdjustment.setREQMoreInfo("N");
      }
      OBDal.getInstance().save(objAdjustment);
    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in IssueRequest: ", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        return_obj.put("count", 0);
        return_obj.put("reserve", false);
      } catch (JSONException e1) {

      }
      return return_obj;
    } finally {
      OBContext.restorePreviousMode();
    }
    return return_obj;
  }

}
