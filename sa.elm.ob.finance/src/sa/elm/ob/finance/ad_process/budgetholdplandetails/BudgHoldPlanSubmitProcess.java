package sa.elm.ob.finance.ad_process.budgetholdplandetails;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvBudgTransfer;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Kousalya on 27/11/2019
 */

public class BudgHoldPlanSubmitProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for submit action in Budget Hold Plan Details.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BudgHoldPlanSubmitProcess.class);
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;

    try {
      OBContext.setAdminMode();
      // declaring variables
      final String rdvbudgholdId = (String) bundle.getParams().get("Efin_Rdv_Budghold_ID")
          .toString();
      EFINRdvBudgHold budgHold = Utility.getObject(EFINRdvBudgHold.class, rdvbudgholdId);
      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class,
          budgHold.getEfinRdvtxn().getId());
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = budgHold.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      NextRoleByRuleVO nextApproval = null;
      String Lang = vars.getLanguage();
      List<EFINRdvBudgHoldLine> budgHoldLineList = budgHold.getEFINRdvBudgHoldLineList();

      Connection conn = OBDal.getInstance().getConnection();
      BudgHoldPlanProcessDAO budgHldDAO = new BudgHoldPlanProcessDAOImpl();
      Boolean chkRoleIsInDocRul = false;
      boolean allowUpdate = false;
      boolean allowDelegation = false;
      String appstatus = "";
      Date currentDate = new Date();
      boolean isPeriodOpen = true;

      // submit process start
      if (!budgHold.getAction().equals("RE")) {
        // Check transaction period is opened or not before submitting record
        if ("CO".equals(budgHold.getAction())) {
          isPeriodOpen = Utility.checkOpenPeriod(transaction.getTxnverDate(),
              orgId.equals("0") ? vars.getOrg() : orgId, transaction.getClient().getId());
          if (!isPeriodOpen) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
            bundle.setResult(result);
            return;
          }
        }
        if (transaction.getTxnverStatus().equals("DR")
            && !transaction.getAppstatus().equals("DR")) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_POHoldPlan_SubmitError@");
          bundle.setResult(result);
          return;
        }
        if (budgHold.getEFINRdvBudgHoldLineList().size() > 0) {
          List<EFINRdvBudgHoldLine> budgHoldLessThanZeroLineQry = budgHold
              .getEFINRdvBudgHoldLineList().stream()
              .filter(a -> a.getHoldAmount().compareTo(BigDecimal.ZERO) < 0)
              .collect(Collectors.toList());
          List<EFINRdvBudgHoldLine> budgHoldLessAllLineZeroLnQry = budgHold
              .getEFINRdvBudgHoldLineList().stream()
              .filter(a -> a.getHoldAmount().compareTo(BigDecimal.ZERO) == 0)
              .collect(Collectors.toList());
          if (budgHoldLessThanZeroLineQry.size() > 0 || budgHold.getEFINRdvBudgHoldLineList()
              .size() == budgHoldLessAllLineZeroLnQry.size()) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EFIN_POHoldPlanHoldAmtZero@");
            bundle.setResult(result);
            return;
          }
        }

        // Throw error if PO is in status Withdrawn / Hold
        if (transaction.getEfinRdv().getSalesOrder() != null) {
          Order order = OBDal.getInstance().get(Order.class,
              transaction.getEfinRdv().getSalesOrder().getId());
          if ("ESCM_WD".equals(order.getEscmAppstatus())
              || "ESCM_OHLD".equals(order.getEscmAppstatus())) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_HoldWithdrawnPO@");
            bundle.setResult(result);
            return;
          }
        }

        // check hold amount is greater than netmatch in rdv.
        List<EfinRDVTxnline> rdvTxnLnList = transaction.getEfinRDVTxnlineList();
        for (EFINRdvBudgHoldLine ln : budgHoldLineList) {
          String accountId = ln.getAccountingCombination().getId();
          BigDecimal netMatchAmt = rdvTxnLnList.stream()
              .filter(a -> StringUtils.equals(
                  a.getAccountingCombination() != null ? a.getAccountingCombination().getId() : "",
                  accountId))
              .map(a -> a.getNetmatchAmt()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (ln.getHoldAmount().compareTo(netMatchAmt) > 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Rdv_Holdamt_Gt_NetMatch@");
            bundle.setResult(result);
            return;
          }
        }

        // Check UserRole are allowed to submit the record
        if (budgHold.getStatus().equals("DR") || budgHold.getStatus().equals("REJ")) {
          int submitAllowed = CommonValidations.checkUserRoleForSubmit("Efin_Rdv_Budghold",
              vars.getUser(), vars.getRole(), rdvbudgholdId, "Efin_Rdv_Budghold_ID");
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
        if (budgHold.getStatus().equals("DR")) {
          submittedRoleObj = OBContext.getOBContext().getRole();
          if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_RoleBranchNotDefine@");
            bundle.setResult(result);
            return;
          }
        }
        // find the submitted role org/branch details

        if (budgHold.getNextRole() != null) {
          if (budgHold.getEfinSubmittedRole() != null
              && budgHold.getEfinSubmittedRole().getEutReg() != null) {
            submittedRoleOrgId = budgHold.getEfinSubmittedRole().getEutReg().getId();
          } else {
            submittedRoleOrgId = orgId;
          }
        } else if (budgHold.getNextRole() == null) {
          submittedRoleObj = OBContext.getOBContext().getRole();
          if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
            submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
          } else {
            submittedRoleOrgId = orgId;
          }
        }
        // Approval flow start
        // check role is present in document rule or not
        if (budgHold.getStatus().equals("DR") || budgHold.getStatus().equals("REJ")
            || budgHold.getStatus().equals("WFA")) {
          if (budgHold.getStatus().equals("DR") || budgHold.getStatus().equals("REJ")) {
            chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, userId, roleId, Resource.RDV_BudgHoldDtl,
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
          // Line Net Match Amount should not be less than zero
          if (budgHold.getStatus().equals("DR") || budgHold.getStatus().equals("REJ")) {
            for (EfinRDVTxnline trxnLine : transaction.getEfinRDVTxnlineList()) {
              if (trxnLine.getNetmatchAmt().compareTo(new BigDecimal(0)) < 0) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_LineNetAmt_LessthanZero@");
                bundle.setResult(result);
                return;
              }
            }
          }

          // check current role associated with document rule for approval flow
          if (!budgHold.getStatus().equals("DR") && !budgHold.getStatus().equals("REJ")) {
            if (budgHold.getNextRole() != null) {
              java.util.List<EutNextRoleLine> li = budgHold.getNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (budgHold.getNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                  DocumentTypeE.RDVBH.getDocumentTypeCode());
            }
            if (!allowUpdate && !allowDelegation) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_AlreadyPreocessed_Approve@");
              bundle.setResult(result);
              return;
            }
          }

          // check netmatch amt should not be zero.
          if (transaction.getNetmatchAmt().compareTo(BigDecimal.ZERO) <= 0
              && transaction.getMatchAmt().compareTo(BigDecimal.ZERO) <= 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_RdvNetmatch_Negative@");
            bundle.setResult(result);
            return;
          }
          // check invoice generated for selected version
          if (transaction.getInvoice() != null && !transaction.getTxnverStatus().equals("DR")) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EFIN_RDVBudgHoldTrxVersionInvGen@");
            bundle.setResult(result);
            return;
          }

          // check already approved or not
          if ((!vars.getRole()
              .equals(budgHold.getRole() != null ? budgHold.getRole().getId() : null))
              && (budgHold.getStatus().equals("DR"))) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_AlreadyPreocessed_Approve@");
            bundle.setResult(result);
            return;
          }

          if (!errorFlag) {
            // set value for approval history status
            if ((budgHold.getStatus().equals("DR") || budgHold.getStatus().equals("REJ"))
                && budgHold.getAction().equals("CO")) {
              appstatus = "SUB";
            } else if (budgHold.getStatus().equals("WFA") && budgHold.getAction().equals("AP")) {
              appstatus = "AP";
            }

            // update next role
            JSONObject upresult = budgHldDAO.updateHeaderStatus(conn, clientId, orgId, roleId,
                userId, transaction, appstatus, comments, currentDate, vars, nextApproval, Lang,
                bundle, budgHold);
            if (upresult != null) {
              // if role does not associate with any user then dont allow to process for next
              // approve
              if (upresult.has("count") && upresult.getInt("count") == -2) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_No_LineManager@");
                bundle.setResult(result);
                return;
              }
              if (upresult.has("count") && upresult.getInt("count") == -3) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    upresult.getString("errormsg"));
                bundle.setResult(result);
                return;
              }
              // submit success message
              else if (upresult.has("count") && upresult.getInt("count") == 2) {
                OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Submit@");
                bundle.setResult(result);
                return;
              }
              // approve sucess message
              else if (upresult.has("count") && upresult.getInt("count") == 1) {
                OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Approve@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }
      // Reactivate Process
      if (budgHold.getAction().equals("RE")) {

        // chk already reactivated or not
        if (!budgHold.getStatus().equals("APP")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }

        // check RDV is in draft status while reactivate
        if (!transaction.getAppstatus().equals("DR") && !transaction.getAppstatus().equals("REJ")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@EFIN_CannotReactivateBudgHold@");
          bundle.setResult(result);
          return;
        }

        // Budget revision created so cannot able to reactivate.
        for (EFINRdvBudgHoldLine ln : budgHoldLineList) {
          OBQuery<EfinRdvBudgTransfer> budgetTransferObj = OBDal.getInstance().createQuery(
              EfinRdvBudgTransfer.class, "as e where e.efinRdvBudgholdline.id =:budgetHoldLineId ");
          budgetTransferObj.setNamedParameter("budgetHoldLineId", ln.getId());
          if (budgetTransferObj.list().size() > 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Rdv_Hold_Revision_Created@");
            bundle.setResult(result);
            return;
          }
        }

        // Budget revision created so cannot able to reactivate.
        for (EFINRdvBudgHoldLine ln : budgHoldLineList) {
          for (EfinRdvHoldAction actObj : ln.getEfinRdvHoldActionList()) {
            if (actObj.getEfinRdvHoldActionRDVHoldRelIDList().size() > 0) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_HoldCantReact_ReleaseDone@");
              bundle.setResult(result);
              return;
            }
          }
        }

        // Reactivate Process
        JSONObject upresult = budgHldDAO.reactivateHeader(conn, clientId, orgId, roleId, userId,
            transaction, appstatus, comments, currentDate, vars, nextApproval, Lang, bundle,
            budgHold);

        if (upresult.has("count") && upresult.getInt("count") == 1) {
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_PoHold_Reactivate@");
          bundle.setResult(result);
          return;
        } else {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
          bundle.setResult(result);
          return;
        }

      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in RDV Budget Hold SubmitProcess:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}