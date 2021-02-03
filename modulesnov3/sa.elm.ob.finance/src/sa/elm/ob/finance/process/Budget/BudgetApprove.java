/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */

package sa.elm.ob.finance.process.Budget;

import java.sql.Connection;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.ad_process.budget.BudgetDAO;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.DocumentTypeE;

/**
 * 
 * @author Gowtham.V
 *
 */
public class BudgetApprove implements Process {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetApprove.class);
  private final OBError obError = new OBError();
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    LOG.debug("approve the budget");
    HttpServletRequest request = RequestContext.get().getRequest();
    final String budgetId = (String) bundle.getParams().get("Efin_Budget_ID").toString();
    EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budget.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString(), appstatus = "",
        errorMsg = "", roles = "";
    boolean allowUpdate = false, allowDelegation = false, errorFlag = true,
        checkDistUniquecodePresntOrNot = false;
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Date currentDate = new Date();
    Connection conn = OBDal.getInstance().getConnection();
    Boolean allowApprove = false;
    int checkCount = 0;

    if (LOG.isDebugEnabled()) {
      LOG.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    }
    int count = 0;
    if (LOG.isDebugEnabled()) {
      LOG.debug("budgetId:" + budgetId);
    }
    // checkDistUniquecodePresntOrNot
    checkDistUniquecodePresntOrNot = FundsRequestActionDAO.checkDistUniquecodePresntOrNot(null,
        null, conn, budgetId, budget);
    if (!checkDistUniquecodePresntOrNot) {
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          OBMessageUtils.messageBD("Efin_Revision_error"));
      bundle.setResult(result);
      return;
    }
    // If one user Reworked the record and second user try to Approve same record with same role
    // then throw error
    if (budget.getAlertStatus().equals("REW")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }
    // After Revoked by submiter if approver is try to Approve the same record then throw error
    if (budget.getAlertStatus().equals("OP")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }

    // If the record is Forwarded or given RMI then throw error when any other user tries to
    // approve the record without refreshing the page
    if (budget.getEUTForwardReqmoreinfo() != null) {
      allowApprove = forwardDao.allowApproveReject(budget.getEUTForwardReqmoreinfo(), userId,
          roleId, Resource.BUDGET_ENTRY_RULE);
    }
    if (budget.getEUTReqmoreinfo() != null
        || ((budget.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyProcessed_Approved@");
      bundle.setResult(result);
      return;
    }

    if (budget.getEFINBudgetLinesList().size() > 0) {
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
    }

    if (checkCount == 1) {
      OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_BudgetSubmit_ChkStatus@");
      bundle.setResult(result);
      return;
    }

    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);

          OBQuery<EFINBudgetLines> lines = OBDal.getInstance().createQuery(EFINBudgetLines.class,
              "efinBudget.id ='" + budgetId + "'");
          count = lines.list().size();

          if (count > 0) {

            // check current role associated with document rule for approval flow
            if (!appstatus.equals("OP") && !appstatus.equals("REW")) {
              if (budget.getEUTNextRole() != null) {
                java.util.List<EutNextRoleLine> li = budget.getEUTNextRole()
                    .getEutNextRoleLineList();
                for (int i = 0; i < li.size(); i++) {
                  roles = li.get(i).getRole().getId();
                  if (roleId.equals(roles)) {
                    allowUpdate = true;
                  }
                }
              }
              if (budget.getEUTNextRole() != null) {
                DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
                allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                    DocumentTypeE.BUDGET_ENTRY.getDocumentTypeCode());
                // = BudgetDAO.isDelegatedRole(roleId);
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

            if ((allowUpdate || allowDelegation) && errorFlag) {
              count = BudgetDAO.updateHeaderStatus(clientId, orgId, roleId, userId, budget, "APP",
                  comments, vars, Lang);
              if (count == 2) {
                errorMsg = OBMessageUtils.messageBD("Efin_Duplicate_Inquiry");
              } else {
                errorMsg = OBMessageUtils.messageBD("Efin_BugdetAddError");
              }
              if (count == 1 && !StringUtils.isEmpty(budget.getId())) {
                obError.setType("Success");
                obError.setTitle("Success");
                obError.setMessage(OBMessageUtils.messageBD("EFIN_BudgetSuccess"));
              } else {
                OBDal.getInstance().rollbackAndClose();
                obError.setType("Error");
                obError.setTitle("Error");
                obError.setMessage(errorMsg);
              }
            } else {
              errorFlag = false;
              OBDal.getInstance().rollbackAndClose();
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
              throw new OBException(errorMsg);
            }
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled())
            LOG.error("exception :", e);
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
        }
      } else if (errorFlag == false) {
        OBDal.getInstance().rollbackAndClose();
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      if (LOG.isErrorEnabled())
        LOG.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
