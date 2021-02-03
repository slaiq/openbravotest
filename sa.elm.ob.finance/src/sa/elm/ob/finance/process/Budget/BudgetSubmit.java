package sa.elm.ob.finance.process.Budget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_process.budget.BudgetDAO;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class BudgetSubmit implements Process {

  private static final Logger LOG = LoggerFactory.getLogger(BudgetSubmit.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    final String budgetId = (String) bundle.getParams().get("Efin_Budget_ID").toString();
    EFINBudget budgheader = OBDal.getInstance().get(EFINBudget.class, budgetId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgheader.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String appstatus = "";
    LOG.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true, chkRoleIsInDocRul = false;
    String errorMsg = "";
    int count = 0, checkCount = 0;
    BigDecimal Zero = new BigDecimal(0.00);
    BigDecimal costCurrentBudget = new BigDecimal(0.00);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = "";
    boolean isPeriodOpen = true;
    Role submittedRoleObj = null;
    String submittedRoleOrgId = null;

    try {
      OBContext.setAdminMode(true);
      // Budget Definition Closed Validation
      if (budgheader.getEfinBudgetint().getStatus().equals("CL")) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      // Pre Year Close or closed year Validation
      if (budgheader.getEfinBudgetint() != null && budgheader.getEfinBudgetint().isPreclose()) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      // Task #8198
      // check submitted role have the branch details or not
      if (budgheader.getAlertStatus().equals("OP")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // find the submitted role org/branch details

      if (budgheader.getEUTNextRole() != null) {
        if (budgheader.getEfinSubmittedRole() != null
            && budgheader.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = budgheader.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (budgheader.getEUTNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      OBQuery<EFINBudgetLines> lines = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          " as e where e.efinBudget.id ='" + budgetId + "' order by e.accountElement.id desc");
      if (lines.list() != null && lines.list().size() == 0) {
        errorFlag = false;
        errorMsg = OBMessageUtils.messageBD("Efin_AddLines_Submit");
      } else {
        EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);

        // should not allow to submit if definition is not open.
        if (!budgheader.getEfinBudgetint().getStatus().equals("OP")) {
          errorMsg = OBMessageUtils.messageBD("Efin_BudgetDef_NotOpen");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);
          return;
        }
        // budget value is zero.
        if (budget.getTotalbudgetvalue() != null
            && budget.getTotalbudgetvalue().compareTo(Zero) == 0) {
          errorFlag = false;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Budgvalue_Zero@");
          bundle.setResult(result);
          return;
        }

        // Check transaction period is opened or not
        isPeriodOpen = Utility.checkOpenPeriod(budgheader.getTransactionDate(),
            orgId.equals("0") ? vars.getOrg() : orgId, budgheader.getClient().getId());
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
        if (budgheader.getAlertStatus().equals("OP") || budgheader.getAlertStatus().equals("REW")) {
          int submitAllowed = CommonValidations.checkUserRoleForSubmit("Efin_Budget",
              vars.getUser(), vars.getRole(), budgetId, "Efin_Budget_ID");
          if (submitAllowed == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Role_NotFundsReserve_submit@");
            bundle.setResult(result);
            return;
          }
        }

        // delete lines having amount 0.
        final List<EFINBudgetLines> budLines = lines.list().stream()
            .filter(a -> a.getAmount().equals(Zero)).collect(Collectors.toList());
        for (EFINBudgetLines delLines : budLines) {
          OBDal.getInstance().remove(delLines);
          lines.list().remove(delLines);
        }
        OBDal.getInstance().flush();

        if (lines.list().size() > 0) {
          for (EFINBudgetLines upline : lines.list()) {
            if (upline.getSalesCampaign().getEfinBudgettype().equals("F")) {
              costCurrentBudget = BudgetDAO.getCostCurrentBudget(upline);
              upline.setCostCurrentBudget(costCurrentBudget);
              OBDal.getInstance().save(upline);
            }
          }
        }

        // check all the lines are active or not
        errorFlag = BudgetDAO.isHavingInactiveAccountCombination(budgheader);
        if (!errorFlag) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Inactive_Uniquecode_Lines@");
          bundle.setResult(result);
          return;
        }

        // check account element is active or not.
        if (budget.getAccountElement() != null) {
          for (int i = 0; i < lines.list().size(); i++) {
            // Check whether inactive accounts added in budgetlines
            final ElementValue activeElement = OBDal.getInstance().get(ElementValue.class,
                lines.list().get(i).getAccountElement().getId());
            if (!activeElement.isActive()) {
              errorFlag = false;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_Budgetline_Element_inactive@");
              bundle.setResult(result);
              return;
            }
          }
        }

        for (EFINBudgetLines line : budget.getEFINBudgetLinesList()) {
          line.setCheckingStaus("SCS");
          line.setCheckingStausFailure(null);
          OBDal.getInstance().save(line);
        }

        // check unique code selected in line is already present in budget inquiry, if yes set error
        // in failure reason and throw error
        for (EFINBudgetLines line : budget.getEFINBudgetLinesList()) {
          OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class,
              "accountingCombination.id='" + line.getAccountingCombination().getId()
                  + "' and efinBudgetint.id = '" + budget.getEfinBudgetint().getId() + "' ");
          if (budgetinquiry.list() != null && budgetinquiry.list().size() > 0) {
            checkCount = 1;
            line.setCheckingStaus("FL");
            line.setCheckingStausFailure(OBMessageUtils.messageBD("Efin_AccountAlreadyinInq"));
            OBDal.getInstance().save(line);
          }
        }

        if (checkCount == 1) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_BudgetSubmit_ChkStatus@");
          bundle.setResult(result);
          return;
        }

        // calling common method to validate.
        count = sa.elm.ob.finance.util.CommonValidations.checkBudgetValidations(budgetId, clientId);
        if (count == 0) {
          for (EFINBudgetLines line : budget.getEFINBudgetLinesList()) {
            line.setCheckingStaus("SCS");
            line.setCheckingStausFailure(null);
            OBDal.getInstance().save(line);
          }
        } else if (count == 1) {
          errorFlag = false;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Lines_Inactive@");
          bundle.setResult(result);
          return;
        } else {
          errorFlag = false;
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_BudgetSubmit_ChkStatus@");
          bundle.setResult(result);
          return;
        }

        // new approval flow.
        appstatus = budgheader.getAlertStatus();

        // check role is present in document rule or not
        if (appstatus.equals("OP") || appstatus.equals("REW")) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, userId, roleId, Resource.BUDGET_ENTRY_RULE,
              BigDecimal.ZERO);
          if (!chkRoleIsInDocRul) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_RoleIsNotIncInDocRule@");
            bundle.setResult(result);
            return;
          }
        }

        if ((!vars.getRole().equals(budgheader.getRole().getId()))
            && (appstatus.equals("OP") || appstatus.equals("REW"))) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        if (errorFlag) {
          if (appstatus.equals("OP") || appstatus.equals("REW")) {
            ps = conn.prepareStatement(
                " select count(efin_budgetlines.dislinkorg) as count from efin_budgetlines where dislinkorg  not in (\n"
                    + "select dislinkorg from efin_budget where efin_budget_id =  '" + budgetId
                    + "')\n"
                    + "and efin_budgetlines.isdistribute = 'Y' and efin_budgetlines.dislinkorg is not null and \n"
                    + "efin_budgetlines.efin_budget_id = '" + budgetId + "'");
            rs = ps.executeQuery();
            if (rs.next()) {
              count = rs.getInt("count");
              if (count > 0) {
                query = "  update efin_budget set dislinkorg=null where efin_budget_id=?";
                ps = conn.prepareStatement(query);
                ps.setString(1, budgetId);
                ps.executeUpdate();
              }
            }
          }
          if (BudgetDAO.checkFundsReqMgmtSeq(budgheader.getId())) {
            count = BudgetDAO.updateHeaderStatus(clientId, orgId, roleId, userId, budgheader, "SUB",
                comments, vars, Lang);

            if (count == 1) {
              errorMsg = OBMessageUtils.messageBD("EFIN_BudgetSubmSuccess");
            } else {
              errorMsg = OBMessageUtils.messageBD("Efin_BugdetAddError");
            }
          } else {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_NoFRMDocumentSequence@");
            bundle.setResult(result);
            return;
          }
        }
      }
      if (count == 1 && errorFlag == true && !StringUtils.isEmpty(budgheader.getId())) {
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(errorMsg);
      } else if (errorFlag == false) {
        OBDal.getInstance().rollbackAndClose();
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isErrorEnabled()) {
        LOG.error("exception :", e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
