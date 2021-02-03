package sa.elm.ob.finance.process.Budget;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.event.dao.BudgetLinesDAO;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

public class BudgetReactive implements Process {
  private static final Logger log = Logger.getLogger(BudgetRework.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("reactive the budget");
    final String budgetId = (String) bundle.getParams().get("Efin_Budget_ID").toString();
    EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, budgetId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budget.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EFINBudget headerId = null;
    String appstatus = "";
    List<EFINBudgetLines> lineList = new ArrayList<EFINBudgetLines>();
    lineList = budget.getEFINBudgetLinesList();
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = false, isdistpresent = false;
    String errorMsg = "", fundsreqId = null;
    int count = 0;

    log.debug("budgetId:" + budgetId);
    try {
      if (!errorFlag) {
        try {
          OBContext.setAdminMode(true);

          EFINBudget header = OBDal.getInstance().get(EFINBudget.class, budgetId);
          count = lineList.size();
          obError.setType("Error");
          obError.setTitle("Error");
          log.debug("count:" + count);
          if (count > 0) {
            // should not allow to reactivte if definition is not open.
            if (!header.getEfinBudgetint().getStatus().equals("OP")) {
              errorMsg = OBMessageUtils.messageBD("Efin_BudgetDef_NotOpen");
              obError.setMessage(errorMsg);
              bundle.setResult(obError);
              return;
            }
            // should not allow to reactivate cost budget once funds budget created.
            if (budget.getSalesCampaign().getEfinBudgettype().equals("C")) {
              errorFlag = BudgetLinesDAO.checkFundsBudgetCreated(budget);
              if (errorFlag) {
                errorMsg = OBMessageUtils.messageBD("Efin_FundsCreated_NoReactivateCost");
                obError.setMessage(errorMsg);
                bundle.setResult(obError);
                return;
              }
              // check corresponding funds unique code is present in budget enquiry if yes then we
              // should not allow to reactivate
              errorFlag = BudgetLinesDAO.checkFundsIsCreated(budget);
              if (errorFlag) {
                errorMsg = OBMessageUtils.messageBD("Efin_FundsCreated_NoReactivateCost");
                obError.setMessage(errorMsg);
                bundle.setResult(obError);
                return;
              }

            }
            // Distribute reactivate
            isdistpresent = FundsRequestActionDAO.chkdistisdoneornot(null, null, header);
            if (isdistpresent) {
              // get funds reqId
              fundsreqId = FundsRequestActionDAO.getFundsReqId(null, null, header);
              // check distributed budget is used or not
              if (fundsreqId != null) {
                errorFlag = BudgetLinesDAO.checkBudgetDisUsedInquiry(header, fundsreqId);
                if (errorFlag) {
                  errorMsg = OBMessageUtils.messageBD("Efin_BudgetCantReactive");
                  obError.setMessage(errorMsg);
                  bundle.setResult(obError);
                  return;
                } else {
                  if (fundsreqId != null) {
                    // delete budget enquiry
                    BudgetLinesDAO.deleteBudgetEnquiry(header, fundsreqId);
                    // delete Funds Request
                    EFINFundsReq fundreq = OBDal.getInstance().get(EFINFundsReq.class, fundsreqId);
                    FundsRequestActionDAO.deleteFundsReq(fundreq);
                  }

                }
              }
            } else {
              // check budget is used in inquiry
              errorFlag = BudgetLinesDAO.checkBudgetUsedInquiry(header);
              if (errorFlag) {
                errorMsg = OBMessageUtils.messageBD("Efin_BudgetCantReactive");
                obError.setMessage(errorMsg);
                bundle.setResult(obError);
                return;
              }
              // check budget is used in any transaction.
              errorFlag = BudgetLinesDAO.checkBudgetUsedorNot(lineList);
              if (errorFlag) {
                errorMsg = OBMessageUtils.messageBD("Efin_BudgetCantReactive");
                obError.setMessage(errorMsg);
                bundle.setResult(obError);
                return;
              }
            }

            log.debug("errorFlag:" + errorFlag);
            if (!errorFlag) {
              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setAlertStatus("OP");
              header.setSubmit(false);
              OBDal.getInstance().save(header);
              headerId = header;
              if (!StringUtils.isEmpty(headerId.getId())) {
                appstatus = "REACT";
                JSONObject historyData = new JSONObject();
                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", header.getId());
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("HistoryTable", ApprovalTables.Budget_History);
                historyData.put("HeaderColumn", ApprovalTables.Budget_History_HEADER_COLUMN);
                historyData.put("ActionColumn", ApprovalTables.Budget_History_DOCACTION_COLUMN);

                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
                obError.setType("Success");
                obError.setTitle("Success");
                obError.setMessage(OBMessageUtils.messageBD("Efin_Budget_ReactiveSuccess"));
                bundle.setResult(obError);
                return;
              }
            } else if (errorFlag) {
              obError.setType("Error");
              obError.setTitle("Error");
              obError.setMessage(errorMsg);
              bundle.setResult(obError);
              return;
            }
          }
        } catch (Exception e) {
          log.error("exception in budget reactive:", e);
          OBDal.getInstance().rollbackAndClose();
        } finally {
          OBContext.restorePreviousMode();
        }
      } else if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (

    Exception e) {
      bundle.setResult(obError);
      log.error("exception in budget reactive:", e);
      OBDal.getInstance().rollbackAndClose();
    }

  }
}
