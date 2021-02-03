package sa.elm.ob.finance.ad_process;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
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
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAO;
import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAOImpl;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.vo.RDVHoldProcessVO;
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
 * @author poongodi on 13/09/2017
 */

public class BudgetRevisionSubmitProcess extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(BudgetRevisionSubmitProcess.class);
  private HashMap<EfinBudgetTransfertrxline, BigDecimal> releaseLineMap = new HashMap<EfinBudgetTransfertrxline, BigDecimal>();
  private List<EFINFundsReqLine> lineList = new ArrayList<EFINFundsReqLine>();
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    int count = 0;
    Boolean isValid = true;
    PreparedStatement ps = null;
    ResultSet rs = null;
    Date currentDate = new Date();
    boolean allowUpdate = false;
    boolean allowDelegation = false, checkDistUniquecodePresntOrNot = false;

    String sql = "", appstatus = "";

    try {
      OBContext.setAdminMode();
      String BudRevId = (String) bundle.getParams().get("Efin_Budget_Transfertrx_ID");
      EfinBudgetTransfertrx efinBudgetRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          BudRevId);
      Connection conn = OBDal.getInstance().getConnection();
      String DocStatus = efinBudgetRev.getDocStatus();
      String DocAction = efinBudgetRev.getAction();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = efinBudgetRev.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      Boolean chkRoleIsInDocRul;
      boolean errorFlag = false;
      JSONObject jsonObject = null;
      String strquery = "";
      BigDecimal decreaseAmt = BigDecimal.ZERO;
      String validCombinationId = "";
      boolean isWarn = false;
      boolean isPeriodOpen = true;

      Boolean allowApprove = false;
      // Budget Definition closed validation
      if (efinBudgetRev.getEfinBudgetint() != null
          && efinBudgetRev.getEfinBudgetint().getStatus().equals("CL")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      // Pre Close Year Validation
      if (efinBudgetRev.getEfinBudgetint() != null
          && efinBudgetRev.getEfinBudgetint().isPreclose()) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      // rdv hold release validation
      // Task no:7945 point no4
      /*
       * if (efinBudgetRev.isRdvhold()) { for (EfinBudgetTransfertrxline lineObj :
       * efinBudgetRev.getEfinBudgetTransfertrxlineList()) { if
       * (lineObj.getEfinRdvBudgtransferList().size() > 0) { for (EfinRdvBudgTransfer
       * rdvBudgTransfer : lineObj.getEfinRdvBudgtransferList()) { EFINRdvBudgHoldLine
       * budgetholdLine = rdvBudgTransfer.getEfinRdvBudgholdline(); if
       * (budgetholdLine.getEfinRdvBudghold().getEfinRdvtxn() != null) { EfinRDVTransaction rdvTxn =
       * budgetholdLine.getEfinRdvBudghold().getEfinRdvtxn() .getEfinRdvtxn(); if
       * (rdvTxn.getTxnverStatus().equals("DR")) { message =
       * OBMessageUtils.messageBD("Efin_Hold_RelNotInSameVers").replace("%",
       * rdvTxn.getEfinRdv().getDocumentNo() + "-" + rdvTxn.getTXNVersion());
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", message); bundle.setResult(result); return; } } } } } }
       */

      if (efinBudgetRev.getEUTForwardReqmoreinfo() != null) {
        allowApprove = forwardDao.allowApproveReject(efinBudgetRev.getEUTForwardReqmoreinfo(),
            userId, roleId, Resource.BUDGET_REVISION_RULE);
      }
      if (efinBudgetRev.getEUTReqmoreinfo() != null
          || ((efinBudgetRev.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      // OBQuery<EfinBudgetTransfertrxline> budgrevline = OBDal.getInstance().createQuery(
      // EfinBudgetTransfertrxline.class,
      // " as e where e.efinBudgetTransfertrx.id='" + BudRevId + "' ");
      // if (budgrevline.list().size() > 0) {
      // for (EfinBudgetTransfertrxline brl : budgrevline.list()) {
      // JSONObject json = BudgetRevisionDAO.checkFundsAval(brl);
      // if (json.get("is990Acct").equals("true")) {
      // if (json.get("isFundGreater").equals("true")) {
      // log.debug("is warn>" + json.get("isWarn"));
      // if (json.get("isWarn").equals("true")) {
      // brl.setStatus(
      // "Warning : " + OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
      // OBDal.getInstance().save(brl);
      // isWarn = true;
      // // errorFlag = true;
      // } else {
      // brl.setStatus("Failed : " + OBMessageUtils.messageBD("EFIN_BR_NFA"));
      // OBDal.getInstance().save(brl);
      // errorFlag = true;
      // }
      // }
      // }
      // }
      // if (errorFlag == true) {
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // OBMessageUtils.messageBD("Efin_budgetRev_Failed"));
      // bundle.setResult(result);
      // return;
      // }
      // }

      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(efinBudgetRev.getClient().getId());
      parameters.add(efinBudgetRev.getSalesCampaign().getId());
      parameters.add(efinBudgetRev.getSalesCampaign().getEfinBudgettype());
      parameters
          .add(efinBudgetRev.getEfinBudgetint() != null ? efinBudgetRev.getEfinBudgetint().getId()
              : null);
      parameters.add(efinBudgetRev.getId());
      parameters.add(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
      if ("C".equals(efinBudgetRev.getSalesCampaign().getEfinBudgettype())) {
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_CostAmount").replace("@", ""));
        parameters
            .add(OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds").replace("@", ""));
      } else {
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount").replace("@", ""));
        parameters
            .add(OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost").replace("@", ""));
      }
      parameters.add(OBMessageUtils.messageBD("Efin_noBudgetInquiry").replace("@", ""));
      parameters.add(OBMessageUtils.messageBD("Efin_NocostBudgetdefined").replace("@", ""));
      parameters.add(OBMessageUtils.messageBD("EFIN_DistUnqiueCodeNotPresent").replace("@", ""));
      parameters.add(efinBudgetRev.getDocStatus());
      BigDecimal resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
          .call("efin_budgetrev_commonvalid", parameters, null);

      int resultCount = resultCount1.intValue();
      if (resultCount == 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_Revision_error"));
        bundle.setResult(result);
        return;
      }
      if (resultCount == 2) {
        isWarn = true;
      }
      if ((DocStatus.equals("DR") || DocStatus.equals("RW")) && DocAction.equals("CO")) {
        int submitAllowed = CommonValidations.checkUserRoleForSubmit("Efin_Budget_Transfertrx",
            vars.getUser(), vars.getRole(), BudRevId, "Efin_Budget_Transfertrx_ID");
        if (submitAllowed == 0 && !efinBudgetRev.isRdvhold()) {
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

      if (efinBudgetRev.getNextRole() != null) {
        if (efinBudgetRev.getEfinSubmittedRole() != null
            && efinBudgetRev.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = efinBudgetRev.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (efinBudgetRev.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // check role is present in document rule or not
      if (DocStatus.equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, submittedRoleOrgId, userId, roleId, Resource.BUDGET_REVISION_RULE,
            BigDecimal.ZERO);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }

      // check validation
      if (!BudgetRevisionDAO.checkManualUniqueCodeEntry(efinBudgetRev)) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_budgetRev_Failed"));
        bundle.setResult(result);
        return;
      }

      // Check transaction period is opened or not
      isPeriodOpen = Utility.checkOpenPeriod(efinBudgetRev.getTrxdate(),
          orgId.equals("0") ? vars.getOrg() : orgId, efinBudgetRev.getClient().getId());
      if (!isPeriodOpen) {
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
              .equals(UtilityDAO.getYearId(efinBudgetRev.getTrxdate(), clientId))) {
            isPeriodOpen = Utility.checkOpenPeriod(currentDate,
                orgId.equals("0") ? vars.getOrg() : orgId, efinBudgetRev.getClient().getId());

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
              Format monthFormatter = new SimpleDateFormat("MMM-yyyy");
              String month = monthFormatter.format(todaydate);
              efinBudgetRev.setAccountingDate(todaydate);
              efinBudgetRev.setTrxdate(todaydate);
              efinBudgetRev.setTransactionperiod(month);
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

      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("RW")) {
        if (efinBudgetRev.getNextRole() != null) {
          java.util.List<EutNextRoleLine> li = efinBudgetRev.getNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        // check current role is delegated role or not
        if (efinBudgetRev.getNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              DocumentTypeE.BUDGET_REVISION.getDocumentTypeCode());

          /*
           * sql = ""; Connection con = OBDal.getInstance().getConnection(); PreparedStatement st =
           * null; ResultSet rs1 = null; sql =
           * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
           * + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_104'"; st
           * = con.prepareStatement(sql); rs1 = st.executeQuery(); while (rs1.next()) { String
           * roleid = rs1.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation =
           * true; break; } }
           */
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
      if ((!vars.getRole().equals(efinBudgetRev.getRole().getId())) && !efinBudgetRev.isRdvhold()
          && (DocStatus.equals("DR"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (efinBudgetRev.getDocStatus().equals("DR")) {
        OBQuery<EfinBudgetTransfertrxline> lines = OBDal.getInstance().createQuery(
            EfinBudgetTransfertrxline.class,
            " as e where e.efinBudgetTransfertrx.id ='" + BudRevId + "'");
        OBQuery<EfinBudgetTransfertrxline> linesZero = OBDal.getInstance().createQuery(
            EfinBudgetTransfertrxline.class, " as e where e.efinBudgetTransfertrx.id ='" + BudRevId
                + "' and e.decrease=0 and e.increase=0 ");
        if (lines.list().size() == 0) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "Please add lines to submit");
          bundle.setResult(result);
          return;
        }
        if (linesZero.list().size() != 0) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@EFin_Revision_Values_Zero@");
          bundle.setResult(result);
          return;
        }
        if (efinBudgetRev.getDocType().equals("TRS")) {
          ps = conn.prepareStatement(
              " select ln.efin_budget_transfertrx_id from efin_budget_transfertrxline ln "
                  + " where ln.efin_budget_transfertrx_id='" + BudRevId
                  + "' group by ln.efin_budget_transfertrx_id "
                  + " having  sum(ln.increase)<>sum(ln.decrease)");
          rs = ps.executeQuery();
          if (rs.next()) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_BudgetRevision_Transfer_Error@");
            bundle.setResult(result);
            return;
          }
        }
      }
      checkDistUniquecodePresntOrNot = FundsRequestActionDAO
          .checkDistUniquecodePresntOrNot(efinBudgetRev, null, conn, efinBudgetRev.getId(), null);

      if (!checkDistUniquecodePresntOrNot) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_Revision_error"));
        bundle.setResult(result);
        return;
      }

      // isValid = CommonValidations.checkValidations(BudRevId, "BudgetRevision",
      // OBContext.getOBContext().getCurrentClient().getId(), "CO", isWarn);

      if (resultCount == 1 || resultCount == 2) {
        // if (isValid) {
        if (efinBudgetRev.getDocStatus().equals("DR")
            || efinBudgetRev.getDocStatus().equals("RW")) {
          // Check Auto release funds validations and funds available validation
          AutoReleaseFundsService autoRelease = new AutoReleaseFundsImpl();
          if (!autoRelease.checkBCUFundsAvailable(efinBudgetRev, null, releaseLineMap, null)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                OBMessageUtils.messageBD("Efin_Revision_error"));
            bundle.setResult(result);
            return;
          }
          // release from 999 to 990 in case of decrease and 990 doesn't have enough funds available
          // in 990
          autoRelease.insertReleaseInBudgetDistribution(efinBudgetRev, null, releaseLineMap, null,
              false, lineList);

        }
        // Budget Revision Rule Validation
        // int ruleCount = 0;
        // if (!isWarn) {
        // ruleCount = BudgetRevisionRuleValidation.checkRuleValidation(BudRevId, "BR",
        // efinBudgetRev.getClient().getId(), efinBudgetRev.getEfinBudgetint().getId());
        //
        // if (ruleCount == 2) {
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // OBMessageUtils.messageBD("Efin_Revision_error"));
        // bundle.setResult(result);
        // return;
        // }
        // if (ruleCount == 1) {
        // OBError result = OBErrorBuilder.buildMessage(null, "warning",
        // OBMessageUtils.messageBD("Efin_Budget_Revision_Waring"));
        // bundle.setResult(result);
        // return;
        // }
        // }

        if ((DocStatus.equals("DR") || DocStatus.equals("RW")) && DocAction.equals("CO")) {
          ps = conn.prepareStatement(
              " select count(efin_budget_transfertrxline.distribute_line_org) as count from efin_budget_transfertrxline where distribute_line_org  not in (\n"
                  + "select distribute_org from efin_budget_transfertrx where efin_budget_transfertrx_id =  '"
                  + BudRevId + "')\n"
                  + "and efin_budget_transfertrxline.distribute = 'Y' and efin_budget_transfertrxline.distribute_line_org is not null and \n"
                  + "efin_budget_transfertrxline.efin_budget_transfertrx_id = '" + BudRevId + "'");

          rs = ps.executeQuery();
          if (rs.next()) {
            count = rs.getInt("count");
            if (count > 0) {
              sql = "  update efin_budget_transfertrx set distribute_org=null where efin_budget_transfertrx_id=?";
              ps = conn.prepareStatement(sql);
              ps.setString(1, BudRevId);
              ps.executeUpdate();
            }
          }
          appstatus = "SUB";
        } else if (DocStatus.equals("WFA") && DocAction.equals("AP")) {
          appstatus = "AP";
        }

        // previously encumbrance will be created during the funds reserve role , but hereafter it
        // will created during submission itself. Changed for issue auto release funds
        if (efinBudgetRev.getDocStatus().equals("DR")
            || efinBudgetRev.getDocStatus().equals("RW")) {
          strquery = " select efin_budget_transfertrxline.c_validcombination_id as validcombination,efin_budget_transfertrx.\n"
              + "efin_budgetint_id as budgetinitial,sum( efin_budget_transfertrxline.increase) as \n"
              + "increase,sum(efin_budget_transfertrxline.decrease) as decrease from efin_budget_transfertrx\n"
              + " left join efin_budget_transfertrxline\n"
              + "  on efin_budget_transfertrx.efin_budget_transfertrx_id = efin_budget_transfertrxline.efin_budget_transfertrx_id\n"
              + "    where efin_budget_transfertrx.efin_budgetint_id is not null \n"
              + "and efin_budget_transfertrxline.c_validcombination_id is not null \n"
              + "and efin_budget_transfertrx.efin_budget_transfertrx_id = ?  and    efin_budget_transfertrxline.decrease > 0 "
              + " group by efin_budget_transfertrxline.c_validcombination_id,efin_budget_transfertrx.\n"
              + "efin_budgetint_id ";
          ps = conn.prepareStatement(strquery);
          ps.setString(1, BudRevId);
          rs = ps.executeQuery();

          while (rs.next()) {
            decreaseAmt = rs.getBigDecimal("decrease");
            validCombinationId = rs.getString("validcombination");
            EncumbranceProcessDAO.insertEncumbranceLine(conn, efinBudgetRev, null, decreaseAmt,
                validCombinationId, false);
          }
        }
        jsonObject = updateHeaderStatus(conn, clientId, orgId, roleId, userId, efinBudgetRev,
            appstatus, comments, currentDate, vars, Lang);
        jsonObject.getBoolean("reserve");
        count = jsonObject.getInt("count");

        if (count == 2) {
          // If lines are added with unique code which doesnt exist in budget enquiry, then create
          // an
          // entry in budget enquiry

          List<EfinBudgetTransfertrxline> lines = efinBudgetRev.getEfinBudgetTransfertrxlineList()
              .stream().filter(a -> a.getCurrentBudget().compareTo(BigDecimal.ZERO) <= 0)
              .collect(Collectors.toList());

          for (EfinBudgetTransfertrxline line : lines) {

            AccountingCombination uniqucode = line.getAccountingCombination();
            OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "accountingCombination.id='" + line.getAccountingCombination().getId()
                    + "' and efinBudgetint.id = '" + efinBudgetRev.getEfinBudgetint().getId()
                    + "' and salesCampaign.id = '" + efinBudgetRev.getSalesCampaign().getId()
                    + "' ");
            if (budgetinquiry.list() == null || budgetinquiry.list().size() == 0) {
              EfinBudgetInquiry inquiry = OBProvider.getInstance().get(EfinBudgetInquiry.class);
              inquiry.setEfinBudgetint(efinBudgetRev.getEfinBudgetint());
              inquiry.setAccountingCombination(uniqucode);
              inquiry.setUniqueCodeName(uniqucode.getEfinUniquecodename());
              inquiry.setUniqueCode(uniqucode.getEfinUniqueCode());
              inquiry.setOrganization(uniqucode.getOrganization());
              inquiry.setDepartment(uniqucode.getSalesRegion());
              inquiry.setAccount(uniqucode.getAccount());
              inquiry.setSalesCampaign(uniqucode.getSalesCampaign());
              inquiry.setProject(uniqucode.getProject());
              inquiry.setBusinessPartner(uniqucode.getBusinessPartner());
              inquiry.setFunctionalClassfication(uniqucode.getActivity());
              inquiry.setFuture1(uniqucode.getStDimension());
              inquiry.setNdDimension(uniqucode.getNdDimension());
              inquiry.setORGAmt(BigDecimal.ZERO);
              inquiry.setREVAmount(BigDecimal.ZERO);
              inquiry.setFundsAvailable(BigDecimal.ZERO);
              inquiry.setCurrentBudget(BigDecimal.ZERO);
              inquiry.setBudget(true);
              OBDal.getInstance().save(inquiry);

            }
          }
          OBDal.getInstance().flush();

          String accountingcombination = "";
          sql = "select c_validcombination_id from efin_budget_transfertrxline where efin_budget_transfertrx_id = '"
              + efinBudgetRev.getId() + "'";
          ps = conn.prepareStatement(sql);
          rs = ps.executeQuery();

          while (rs.next()) {

            accountingcombination = rs.getString("c_validcombination_id");
            OBQuery<EfinBudgetManencum> chklineexists = OBDal.getInstance().createQuery(
                EfinBudgetManencum.class, "as e where e.sourceref = '" + BudRevId + "'");

            if (chklineexists.list().size() > 0) {
              EncumbranceProcessDAO.insertModificationEntry(conn, efinBudgetRev, null,
                  accountingcombination);

            }
          }

          count = updatebudgetinquiryIncrease(efinBudgetRev.getId());
          FundsRequestActionDAO.directDistribute(conn, "BR", efinBudgetRev.getId(), vars, clientId,
              roleId);

          if (efinBudgetRev.isRdvhold()) {
            for (EfinBudgetTransfertrxline transferTrxLineObj : efinBudgetRev
                .getEfinBudgetTransfertrxlineList()) {
              BigDecimal releaseAmt = transferTrxLineObj.getEfinRdvBudgtransferList().stream()
                  .map(a -> a.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

              if (releaseAmt.compareTo(BigDecimal.ZERO) > 0 && transferTrxLineObj.isDistribute()) {
                AccountingCombination acctComb999 = BudgetHoldPlanReleaseDAOImpl
                    .get999AccountCombination(transferTrxLineObj.getAccountingCombination(),
                        transferTrxLineObj.getDistributeLineOrg().getId(), clientId);
                EncumbranceProcessDAO.insertEncumbranceLine(conn, efinBudgetRev, null, releaseAmt,
                    acctComb999.getId(), true);
              }
            }
          }

        }
        if (count == 3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
          bundle.setResult(result);
          return;
        }
        if (isWarn) {
          OBError result = OBErrorBuilder.buildMessage(null, "warning",
              OBMessageUtils.messageBD("Efin_BudgetRev_Warning"));
          bundle.setResult(result);
          return;
        }

        if (count > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@EFIN_Process_Success@");
          bundle.setResult(result);
          return;
        }
      } else {

        // revert the changes in budget enquiry

        for (EFINFundsReqLine line : lineList) {
          BudgetRevisionDAO.revertBudgetInquiry(line.getEfinFundsreq(), line);
          OBDal.getInstance().remove(line);
        }
        for (EFINFundsReq fundsReq : efinBudgetRev.getEFINFundsReqList()) {
          fundsReq.setDocumentStatus("DR");
          fundsReq.setAction("CO");
          fundsReq.setReserve(false);
          OBDal.getInstance().save(fundsReq);
          OBDal.getInstance().remove(fundsReq);
        }

        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revision_error@");
        bundle.setResult(result);
        return;
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      log.debug("Exeception in BudgetRevision:" + e);
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
   * @param efinBudgetRev
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param Lang
   * @return
   */
  private JSONObject updateHeaderStatus(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinBudgetTransfertrx efinBudgetRev, String appstatus,
      String comments, Date currentDate, VariablesSecureApp vars, String Lang) {
    String transferId = null, pendingapproval = null, alertRuleId = "",
        alertWindow = AlertWindow.BudgetRevision, reserveRoleId = "";
    Boolean isDirectApproval = false;
    User objCreater = efinBudgetRev.getCreatedBy();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    JSONObject return_obj = new JSONObject();
    JSONObject fromUserandRoleJson = new JSONObject();
    // ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    String fromUser = userId;
    String fromRole = roleId;
    boolean isDummyRole = false;
    try {
      OBContext.setAdminMode(true);
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      boolean isBackwardDelegation = false;
      boolean reserve = false;

      JSONObject tableData = new JSONObject();
      tableData.put("headerColumn", ApprovalTables.BUDGET_REVISION_HEADER_COLUMN);
      tableData.put("tableName", ApprovalTables.BUDGET_REVISION_TABLE);
      tableData.put("headerId", efinBudgetRev.getId());
      tableData.put("roleId", roleId);
      isDirectApproval = Utility.isDirectApproval(tableData);

      EfinBudgetTransfertrx transfertrx = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          efinBudgetRev.getId());
      NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId, roleId,
          userId, Resource.BUDGET_REVISION_RULE, 0.00);

      EutNextRole nextRole = null;

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      // find the submitted role org/branch details

      if (efinBudgetRev.getNextRole() != null) {
        if (efinBudgetRev.getEfinSubmittedRole() != null
            && efinBudgetRev.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = efinBudgetRev.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (efinBudgetRev.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      if ((efinBudgetRev.getNextRole() != null)) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            efinBudgetRev.getNextRole(), userId, roleId, clientId, submittedRoleOrgId,
            Resource.BUDGET_REVISION_RULE, isDummyRole, isDirectApproval);
        if (fromUserandRoleJson != null && ((JSONObject) fromUserandRoleJson).length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = (fromUserandRoleJson).getString("fromUser");
          if ((fromUserandRoleJson).has("fromRole"))
            fromRole = (fromUserandRoleJson).getString("fromRole");
          if ((fromUserandRoleJson).has("isDirectApproval"))
            isDirectApproval = (fromUserandRoleJson).getBoolean("isDirectApproval");
        }
      } else {
        fromUser = userId;
        fromRole = roleId;
      }

      if ((efinBudgetRev.getNextRole() == null)) {
        // nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
        // orgId, roleId, userId, Resource.BUDGET_REVISION_RULE, 0);
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
            Resource.BUDGET_REVISION_RULE, BigDecimal.ZERO, fromUser, false,
            efinBudgetRev.getDocStatus());
        reserveRoleId = fromRole;
        // reserve = UtilityDAO.getReserveFundsRole(Resource.BUDGET_REVISION_RULE, roleId, orgId,
        // efinBudgetRev.getId());
      } else {
        if (isDirectApproval) {

          // nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(),
          // clientId,
          // orgId, roleId, userId, Resource.BUDGET_REVISION_RULE, 0);
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
              Resource.BUDGET_REVISION_RULE, BigDecimal.ZERO, fromUser, false,
              efinBudgetRev.getDocStatus());
          reserveRoleId = fromRole;
          // reserve = UtilityDAO.getReserveFundsRole(Resource.BUDGET_REVISION_RULE, roleId, orgId,
          // efinBudgetRev.getId());
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    userRole.list().get(0).getUserContact().getId(), Resource.BUDGET_REVISION_RULE,
                    "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    delegatedFromRole, delegatedToRole, fromUser, Resource.BUDGET_REVISION_RULE,
                    0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                submittedRoleOrgId, delegatedFromRole, fromUser, Resource.BUDGET_REVISION_RULE,
                0.00);
            reserveRoleId = delegatedFromRole;
            // reserve = UtilityDAO.getReserveFundsRole(Resource.BUDGET_REVISION_RULE,
            // delegatedFromRole, orgId, efinBudgetRev.getId());
          }
        } else {

          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, userId, Resource.BUDGET_REVISION_RULE, qu_next_role_id);
          delegatedFromRole = role.get("FromUserRoleId");

          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)

            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.BUDGET_REVISION_RULE, 0.00);
          reserveRoleId = delegatedFromRole;
          // reserve = UtilityDAO.getReserveFundsRole(Resource.BUDGET_REVISION_RULE,
          // delegatedFromRole,
          // orgId, efinBudgetRev.getId());
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        return_obj.put("count", 3);
      } else if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(transfertrx.getNextRole(), Resource.BUDGET_REVISION_RULE);

        transfertrx.setUpdated(new java.util.Date());
        transfertrx.setUpdatedBy(OBContext.getOBContext().getUser());
        if ((transfertrx.getDocStatus().equals("RW") || transfertrx.getDocStatus().equals("DR"))
            && transfertrx.getAction().equals("CO")) {
          transfertrx.setRevoke(true);
        } else
          transfertrx.setRevoke(false);
        transfertrx.setDocStatus("WFA");
        transfertrx.setNextRole(nextRole);
        transfertrx.setAction("AP");

        log.debug("doc sts:" + transfertrx.getDocStatus() + "action:" + transfertrx.getAction());
        return_obj.put("count", 1);

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        forwardDao.getAlertForForwardedUser(transfertrx.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, transfertrx.getDocumentNo(), Lang, vars.getRole(),
            transfertrx.getEUTForwardReqmoreinfo(), Resource.BUDGET_REVISION_RULE,
            alertReceiversMap);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          /*
           * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
           * "as e where e.referenceSearchKey='" + transfertrx.getId() +
           * "' and e.alertStatus='NEW'"); if (alertQuery.list().size() > 0) { for (Alert objAlert :
           * alertQuery.list()) { objAlert.setAlertStatus("SOLVED"); } }
           */
          String Description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.revision.wfa", Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(transfertrx.getId(), transfertrx.getDocumentNo(),
                objNextRoleLine.getRole().getId(), "", transfertrx.getClient().getId(), Description,
                "NEW", alertWindow, "finance.revision.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_104'");
            if (delegationln != null && delegationln.list().size() > 0) {
              AlertUtility.alertInsertionRole(transfertrx.getId(), transfertrx.getDocumentNo(),
                  delegationln.list().get(0).getRole().getId(),
                  delegationln.list().get(0).getUserContact().getId(),
                  transfertrx.getClient().getId(), Description, "NEW", alertWindow,
                  "finance.revision.wfa", Constants.GENERIC_TEMPLATE);
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

        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

      } else {

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(transfertrx.getNextRole(), Resource.BUDGET_REVISION_RULE);

        if (transfertrx.getEfinBudgetRevVoid() != null) {
          EfinBudgetTransfertrx VoidReference = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              transfertrx.getEfinBudgetRevVoid().getId());
          VoidReference.setDocStatus("VO");
          OBDal.getInstance().save(VoidReference);
        }
        transfertrx.setUpdated(new java.util.Date());
        transfertrx.setUpdatedBy(OBContext.getOBContext().getUser());
        if (transfertrx.getEfinBudgetRevVoid() != null) {
          transfertrx.setDocStatus("VO");
        } else {
          transfertrx.setDocStatus("CO");
        }
        transfertrx.setNextRole(null);
        transfertrx.setAction("PD");
        transfertrx.setRevoke(false);

        return_obj.put("count", 2);

        ArrayList<String> includeRecipient = new ArrayList<String>();
        Role objCreatedRole = null;

        if (transfertrx.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = transfertrx.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        /*
         * // delete alert for approval alerts OBQuery<Alert> alertQuery =
         * OBDal.getInstance().createQuery(Alert.class, "as e where e.referenceSearchKey='" +
         * transfertrx.getId() + "' and e.alertStatus='NEW'"); if (alertQuery.list().size() > 0) {
         * for (Alert objAlert : alertQuery.list()) { objAlert.setAlertStatus("SOLVED"); } }
         */

        forwardDao.getAlertForForwardedUser(transfertrx.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, transfertrx.getDocumentNo(), Lang, vars.getRole(),
            transfertrx.getEUTForwardReqmoreinfo(), Resource.BUDGET_REVISION_RULE,
            alertReceiversMap);

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
        includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.revision.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(transfertrx.getId(), transfertrx.getDocumentNo(),
            transfertrx.getRole().getId(), transfertrx.getCreatedBy().getId(),
            transfertrx.getClient().getId(), Description, "NEW", alertWindow,
            "finance.revision.approved", Constants.GENERIC_TEMPLATE);

        // budget hold plan release
        if (transfertrx.isRdvhold()) {
          BudgetHoldPlanReleaseDAO holdPlanReleaseDAO = new BudgetHoldPlanReleaseDAOImpl();
          holdPlanReleaseDAO.addBudgRevHoldReleaseInRDV(OBDal.getInstance().getConnection(),
              transfertrx);
          // send alert to po hold plan
          insertAlertforRDVHoldUser(orgId, transfertrx.getClient().getId(), vars, transfertrx);
          // send alert to Budgetuser when Budget revision is approved
          BudgetRevisionDAO.insertAlertforBudgetUser(vars, transfertrx);
        }
      }

      // after approved by forwarded user
      if (transfertrx.getEUTForwardReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeRoleAccess(clientId, objRequest.getEUTForwardReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(transfertrx.getEUTForwardReqmoreinfo());
        // set forward_rmi id as null in record
        transfertrx.setEUTForwardReqmoreinfo(null);
      }

      // removing rmi
      if (transfertrx.getEUTReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeReqMoreInfoRoleAccess(clientId, objRequest.getEUTReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(transfertrx.getEUTReqmoreinfo());
        // set forward_rmi id as null in record
        transfertrx.setEUTReqmoreinfo(null);
        transfertrx.setRequestMoreInformation("N");
      }

      OBDal.getInstance().save(transfertrx);
      transferId = transfertrx.getId();

      if (!StringUtils.isEmpty(transferId)) {
        JSONObject historyData = new JSONObject();
        log.debug("Appstatus" + appstatus);
        log.debug("pendingapproval" + pendingapproval);
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", transferId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.BUDGET_REVISION_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.BUDGET_REVISION_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.BUDGET_REVISION_DOCACTION_COLUMN);
        Utility.InsertApprovalHistory(historyData);
      }
      OBDal.getInstance().flush();
      reserve = UtilityDAO.getReserveFundsRole(Resource.BUDGET_REVISION_RULE, fromRole, orgId,
          efinBudgetRev.getId(), BigDecimal.ZERO);
      return_obj.put("reserve", reserve);
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.BUDGET_REVISION_RULE);
    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in IssueRequest: ", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        return_obj.put("count", 0);
        return_obj.put("reserve", false);
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      return return_obj;
    } finally {
      OBContext.restorePreviousMode();
    }
    return return_obj;
  }

  /**
   * This method is used to update budget inquiry increase
   * 
   * @param BudRevId
   * @return
   */
  public static int updatebudgetinquiryIncrease(String BudRevId) {

    int count = 0;
    String strquery, query = "";
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps1 = null, ps2 = null;
    ResultSet rs = null, rs1 = null;
    String validcombination = "";
    String budgetinitial = "";

    try {
      OBContext.setAdminMode(true);
      strquery = " select efin_budget_transfertrxline.c_validcombination_id as validcombination,efin_budget_transfertrx.\n"
          + "efin_budgetint_id as budgetinitial,sum( efin_budget_transfertrxline.increase) as \n"
          + "increase,sum(efin_budget_transfertrxline.decrease) as decrease from efin_budget_transfertrx\n"
          + " left join efin_budget_transfertrxline\n"
          + "  on efin_budget_transfertrx.efin_budget_transfertrx_id = efin_budget_transfertrxline.efin_budget_transfertrx_id\n"
          + "    where efin_budget_transfertrx.efin_budgetint_id is not null \n"
          + "and efin_budget_transfertrxline.c_validcombination_id is not null \n"
          + "and efin_budget_transfertrx.efin_budget_transfertrx_id = ? and    efin_budget_transfertrxline.increase > 0 "
          + " group by efin_budget_transfertrxline.c_validcombination_id,efin_budget_transfertrx.\n"
          + "efin_budgetint_id ";
      ps = conn.prepareStatement(strquery);
      ps.setString(1, BudRevId);
      rs = ps.executeQuery();
      log.debug("strquery" + strquery);
      while (rs.next()) {
        validcombination = rs.getString("validcombination");
        budgetinitial = rs.getString("budgetinitial");

        query = "select efin_budgetinquiry_id ,c_validcombination_id from efin_budgetinquiry "
            + "where efin_budgetint_id= '" + budgetinitial + "' and c_validcombination_id= '"
            + validcombination + "' ";
        ps1 = conn.prepareStatement(query);
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          if (rs.getBigDecimal("increase") != null) {
            query = "  update efin_budgetinquiry set revinc_amt=revinc_amt + ? where efin_budgetinquiry_id=? and c_validcombination_id = ?";
            ps2 = conn.prepareStatement(query);
            ps2.setBigDecimal(1, rs.getBigDecimal("increase"));
            ps2.setString(2, rs1.getString("efin_budgetinquiry_id"));
            ps2.setString(3, rs1.getString("c_validcombination_id"));
            ps2.executeUpdate();
          }
          if (rs.getBigDecimal("decrease") != null) {
            query = "  update efin_budgetinquiry set revdec_amt =revdec_amt+ ? where efin_budgetinquiry_id=? and c_validcombination_id = ?";
            ps2 = conn.prepareStatement(query);
            ps2.setBigDecimal(1, rs.getBigDecimal("decrease"));
            ps2.setString(2, rs1.getString("efin_budgetinquiry_id"));
            ps2.setString(3, rs1.getString("c_validcombination_id"));
            ps2.executeUpdate();
          }
        }

      }
      count = 1;
    } catch (Exception e) {
      log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * This method is used to update budget inquiry decrease
   * 
   * @param BudRevId
   * @return
   */
  public static int updatebudgetinquiryDecrease(String BudRevId) {

    int count = 0;
    String strquery, query = "";
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps1 = null, ps2 = null;
    ResultSet rs = null, rs1 = null;
    String validcombination = "";
    String budgetinitial = "";

    try {
      OBContext.setAdminMode(true);
      strquery = " select efin_budget_transfertrxline.c_validcombination_id as validcombination,efin_budget_transfertrx.\n"
          + "efin_budgetint_id as budgetinitial,sum( efin_budget_transfertrxline.increase) as \n"
          + "increase,sum(efin_budget_transfertrxline.decrease) as decrease from efin_budget_transfertrx\n"
          + " left join efin_budget_transfertrxline\n"
          + "  on efin_budget_transfertrx.efin_budget_transfertrx_id = efin_budget_transfertrxline.efin_budget_transfertrx_id\n"
          + "    where efin_budget_transfertrx.efin_budgetint_id is not null \n"
          + "and efin_budget_transfertrxline.c_validcombination_id is not null \n"
          + "and efin_budget_transfertrx.efin_budget_transfertrx_id = ?  and    efin_budget_transfertrxline.decrease > 0 "
          + " group by efin_budget_transfertrxline.c_validcombination_id,efin_budget_transfertrx.\n"
          + "efin_budgetint_id ";
      ps = conn.prepareStatement(strquery);
      ps.setString(1, BudRevId);
      rs = ps.executeQuery();
      log.debug("strquery" + strquery);
      while (rs.next()) {
        validcombination = rs.getString("validcombination");
        budgetinitial = rs.getString("budgetinitial");

        query = "select efin_budgetinquiry_id ,c_validcombination_id from efin_budgetinquiry "
            + "where efin_budgetint_id= '" + budgetinitial + "' and c_validcombination_id= '"
            + validcombination + "' ";
        ps1 = conn.prepareStatement(query);
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          if (rs.getBigDecimal("increase") != null) {
            query = "  update efin_budgetinquiry set revinc_amt=revinc_amt + ? where efin_budgetinquiry_id=? and c_validcombination_id = ?";
            ps2 = conn.prepareStatement(query);
            ps2.setBigDecimal(1, rs.getBigDecimal("increase"));
            ps2.setString(2, rs1.getString("efin_budgetinquiry_id"));
            ps2.setString(3, rs1.getString("c_validcombination_id"));
            ps2.executeUpdate();
          }
          if (rs.getBigDecimal("decrease") != null) {
            query = "  update efin_budgetinquiry set revdec_amt =revdec_amt+ ? where efin_budgetinquiry_id=? and c_validcombination_id = ?";
            ps2 = conn.prepareStatement(query);
            ps2.setBigDecimal(1, rs.getBigDecimal("decrease"));
            ps2.setString(2, rs1.getString("efin_budgetinquiry_id"));
            ps2.setString(3, rs1.getString("c_validcombination_id"));
            ps2.executeUpdate();
          }
        }
      }
      count = 1;
    } catch (Exception e) {
      log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public static int insertAlertforRDVHoldUser(String orgId, String ClientId,
      VariablesSecureApp vars, EfinBudgetTransfertrx header) {
    String alertWindow = AlertWindow.RDVRevisionHold;

    String alertRuleId = "";
    ArrayList<RDVHoldProcessVO> includereceipient = new ArrayList<RDVHoldProcessVO>();
    RDVHoldProcessVO vo = null;
    List<EFINRdvBudgHold> budgetHoldList = new ArrayList<EFINRdvBudgHold>();
    try {
      OBContext.setAdminMode();

      header.getEfinBudgetTransfertrxlineList().forEach(a -> {
        a.getEfinRdvBudgtransferList().forEach(b -> {
          if (!budgetHoldList.contains(b.getEfinRdvBudgholdline().getEfinRdvBudghold()))
            budgetHoldList.add(b.getEfinRdvBudgholdline().getEfinRdvBudghold());
        });

      });
      if (budgetHoldList.size() > 0) {
        // getting alert receipient
        OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(AlertRule.class,
            " as e where e.client.id='" + ClientId + "' and e.efinProcesstype='" + alertWindow
                + "'");
        if (alertrule.list().size() > 0) {
          alertRuleId = alertrule.list().get(0).getId();
        }
        // Make solve the previous alert.
        OBQuery<Alert> resolveQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.alertRule.id='" + alertRuleId
                + "' and e.alertStatus='NEW' and e.client.id = '" + ClientId + "'");
        if (resolveQuery.list().size() > 0) {
          for (Alert objAlert : resolveQuery.list()) {
            objAlert.setAlertStatus("SOLVED");
          }

        }
        OBQuery<AlertRecipient> alertrec = OBDal.getInstance().createQuery(AlertRecipient.class,
            " as e where e.alertRule.id='" + alertRuleId + "'");
        if (alertrec.list().size() > 0) {
          for (AlertRecipient rec : alertrec.list()) {
            OBDal.getInstance().remove(rec);
          }
        }
        for (EFINRdvBudgHold budgHoldObj : budgetHoldList) {

          // String alertQuery = "select efin_rdv_budgholdline_id from efin_rdv_budgtransfer where
          // efin_budget_transfertrxline_id ='"
          // + line.getId() + "'";
          // SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(alertQuery);
          // @SuppressWarnings("rawtypes")
          // List RdvList = Query.list();
          // if (RdvList != null && RdvList.size() > 0) {
          // for (int i = 0; i < RdvList.size(); i++) {
          // Object objRdvList = RdvList.get(i);
          // holdLineId = objRdvList.toString();
          // holdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class, holdLineId);
          if (budgHoldObj != null) {
            vo = new RDVHoldProcessVO(budgHoldObj.getRole().getId(),
                budgHoldObj.getCreatedBy().getId());
            includereceipient.add(vo);
          }

          // avoid duplicate recipient
          Set<RDVHoldProcessVO> s = new HashSet<RDVHoldProcessVO>();
          s.addAll(includereceipient);
          includereceipient = new ArrayList<RDVHoldProcessVO>();
          includereceipient.addAll(s);

          // insert alert receipients
          for (RDVHoldProcessVO vo1 : includereceipient) {

            if (vo1.getUserId().equals("0")) {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, ClientId, alertWindow);
            }

            else {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), ClientId,
                  alertWindow);
            }
          }

          String Description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.rdvbudgethold.success", vars.getLanguage()) + " "
              + budgHoldObj.getSalesOrder().getDocumentNo();
          // insert alert in alert window
          AlertUtility.alertInsertionRole(budgHoldObj.getId(),
              budgHoldObj.getSalesOrder().getDocumentNo(), "", "", budgHoldObj.getClient().getId(),
              Description, "NEW", alertWindow, "finance.rdvbudgethold.success",
              Constants.GENERIC_TEMPLATE);
          // }
          // }
        }
      }
    }

    catch (Exception e) {
      log.error(" Exception while insertAlertforRDVHoldUser for rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
    }
    return 0;
  }

}