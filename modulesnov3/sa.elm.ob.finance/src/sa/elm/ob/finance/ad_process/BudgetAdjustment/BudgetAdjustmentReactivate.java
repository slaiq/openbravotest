package sa.elm.ob.finance.ad_process.BudgetAdjustment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopalakrishnan on 16/09/2017
 * 
 */

public class BudgetAdjustmentReactivate extends DalBaseProcess {

  /**
   * Budget Adjustment reactivate process
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetAdjustmentReactivate.class);

  @SuppressWarnings("rawtypes")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Boolean isdistpresent = false, isfundserrorFlag = true;
    String fundsreqId = "";
    Connection conn = OBDal.getInstance().getConnection();
    long distributionCount = 0;
    Boolean isError = false;
    BigDecimal resultCount1 = BigDecimal.ONE;

    log.debug("entering into BudgetAdditionProcess");
    try {
      OBContext.setAdminMode();
      String BudgetAdjustementid = (String) bundle.getParams().get("Efin_Budgetadj_ID");
      int i = 0;

      BudgetAdjustment budgetAddjustment = OBDal.getInstance().get(BudgetAdjustment.class,
          BudgetAdjustementid);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = budgetAddjustment.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      EfinBudgetManencum manualId = null;
      // check line is distribute.
      isdistpresent = FundsRequestActionDAO.chkdistisdoneornot(budgetAddjustment, null, null);
      log.debug("isdistpresent" + isdistpresent);

      // Check all the validation before doing the changes
      // 1. Get distributed lines and check the validation in FRM
      // 2. Check budget adjsutment common validations
      // 3. Get Release to HQ lines and check the validation in FRM
      List<Object> parameters = new ArrayList<Object>();

      for (EFINFundsReq fundreqmgmt : budgetAddjustment.getEFINFundsReqList()) {
        distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
            .filter(a -> a.getREQType().equals("DIST")).count();
        if (distributionCount > 0) {
          parameters = new ArrayList<Object>();
          parameters.add(budgetAddjustment.getClient().getId());
          parameters.add(budgetAddjustment.getBudgetType().getId());
          parameters.add(budgetAddjustment.getBudgetType().getEfinBudgettype());
          parameters.add(budgetAddjustment.getEfinBudgetint() != null
              ? budgetAddjustment.getEfinBudgetint().getId()
              : null);
          parameters.add(fundreqmgmt.getId());
          if ("C".equals(budgetAddjustment.getBudgetType().getEfinBudgettype())) {
            parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
                .replace("@", ""));
          } else {
            parameters.add(OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount")
                .replace("@", ""));

          }
          parameters.add("DIST");
          resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
              .call("efin_fundsreq_commonadj_rea", parameters, null);

          if (resultCount1.intValue() == 0) {
            isError = true;
          }
        }
      }

      parameters = new ArrayList<Object>();
      parameters.add(budgetAddjustment.getClient().getId());
      parameters.add(budgetAddjustment.getBudgetType().getId());
      parameters.add(budgetAddjustment.getBudgetType().getEfinBudgettype());
      parameters.add(budgetAddjustment.getEfinBudgetint() != null
          ? budgetAddjustment.getEfinBudgetint().getId()
          : null);
      parameters.add(budgetAddjustment.getId());
      if ("C".equals(budgetAddjustment.getBudgetType().getEfinBudgettype())) {
        parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
            .replace("@", ""));
        parameters
            .add(OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds").replace("@", ""));
      } else {
        parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
            .replace("@", ""));
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount").replace("@", ""));
      }
      resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
          .call("efin_budgetadj_commonvalidrea", parameters, null);

      if (resultCount1.intValue() == 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("EFIN_FundsReq_Rev_Error"));
        bundle.setResult(result);
        return;
      }

      for (EFINFundsReq fundreqmgmt : budgetAddjustment.getEFINFundsReqList()) {
        distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
            .filter(a -> !a.getREQType().equals("DIST")).count();
        if (distributionCount > 0) {
          parameters = new ArrayList<Object>();
          parameters.add(budgetAddjustment.getClient().getId());
          parameters.add(budgetAddjustment.getBudgetType().getId());
          parameters.add(budgetAddjustment.getBudgetType().getEfinBudgettype());
          parameters.add(budgetAddjustment.getEfinBudgetint() != null
              ? budgetAddjustment.getEfinBudgetint().getId()
              : null);
          parameters.add(fundreqmgmt.getId());
          if ("C".equals(budgetAddjustment.getBudgetType().getEfinBudgettype())) {
            parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
                .replace("@", ""));
          } else {
            parameters.add(OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount")
                .replace("@", ""));

          }
          parameters.add("REQ");
          resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
              .call("efin_fundsreq_commonadj_rea", parameters, null);

          if (resultCount1.intValue() == 0) {
            isError = true;
          }
        }
      }

      if (isError) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("EFIN_FundsReq_Rev_Error"));
        bundle.setResult(result);
        return;
      }

      // get funds reqId

      for (EFINFundsReq fundreqmgmt : budgetAddjustment.getEFINFundsReqList()) {
        distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
            .filter(a -> a.getREQType().equals("DIST")).count();
        if (distributionCount > 0) {
          isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
              OBDal.getInstance().getConnection(), fundreqmgmt.getId(), false, true);
        }
      }
      if (!isfundserrorFlag) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_FundsReq_Rev_Error@");
        bundle.setResult(result);
        return;
      }

      if (!isdistpresent || isfundserrorFlag) {
        Boolean validProcess = CommonValidations.checkValidations(BudgetAdjustementid,
            "BudgetAdjustment", OBContext.getOBContext().getCurrentClient().getId(), "RE", false);
        if (validProcess) {
          String str_budget_reference = budgetAddjustment.getEfinBudgetint() == null ? ""
              : budgetAddjustment.getEfinBudgetint().getId();

          String query = " select bt.efin_budgetint_id,bl.efin_budgetinquiry_id ,"
              + " cv.account_id,al.increase,al.decrease,  "
              + " cv.c_validcombination_id,al.efin_budgetadjline_id  "
              + " from efin_budgetadjline al "
              + "  left join c_validcombination cv on cv.c_validcombination_id = al.c_validcombination_id "
              + "    left join efin_budgetint bt on bt.efin_budgetint_id=:reference1  "
              + "    left join efin_budgetinquiry bl on  cv.c_validcombination_id =bl.c_validcombination_id and "
              + "    bl.efin_budgetint_id=:reference2 " + "    where al.ad_client_id =:clientId "
              + "    and al.efin_budgetadj_id =:budgetAdjId";
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
                  EfinBudgetInquiry objInquiryLine = OBDal.getInstance()
                      .get(EfinBudgetInquiry.class, row[1].toString());
                  objInquiryLine
                      .setObdecAmt(objInquiryLine.getObdecAmt().subtract((BigDecimal) row[4]));
                  objInquiryLine
                      .setObincAmt(objInquiryLine.getObincAmt().subtract((BigDecimal) row[3]));
                  OBDal.getInstance().save(objInquiryLine);
                  objAdjLine.setBudgetInquiryLine(objInquiryLine);
                  objAdjLine.setFundsreserved(false);
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
                OBDal.getInstance().getSession().clear();
              }
              i++;
              // row[1]
            }
          }

          for (EFINFundsReq fundreqmgmt : budgetAddjustment.getEFINFundsReqList()) {
            distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
                .filter(a -> !a.getREQType().equals("DIST")).count();
            if (distributionCount > 0) {
              isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
                  OBDal.getInstance().getConnection(), fundreqmgmt.getId(), false, true);
            }
          }
          if (!isfundserrorFlag) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EFIN_FundsReq_Rev_Error@");
            bundle.setResult(result);
            return;
          }

          OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance().createQuery(
              EfinBudgetManencum.class, " as e where e.sourceref = :BudgetAdjustementID ");
          chkLinePresent.setNamedParameter("BudgetAdjustementID", BudgetAdjustementid);
          List<EfinBudgetManencum> chkLinePresentList = chkLinePresent.list();
          if (chkLinePresent != null && chkLinePresentList.size() > 0) {
            manualId = chkLinePresentList.get(0);
            EfinBudgetManencum manual = manualId;
            manual.setDocumentStatus("DR");
            OBDal.getInstance().save(manual);
            for (EfinBudgetManencumlines reqln : manualId.getEfinBudgetManencumlinesList()) {
              List<EfinBudManencumRev> revlist = reqln.getEfinBudManencumRevList();
              for (EfinBudManencumRev revision : revlist) {
                // reqln.getEfinBudManencumRevList().remove(revision);
                OBDal.getInstance().remove(revision);
              }
              OBDal.getInstance().remove(reqln);
            }

            budgetAddjustment.setManualEncumbrance(null);
            OBDal.getInstance().save(budgetAddjustment);
            OBDal.getInstance().remove(manualId);

          }
          log.debug("chkLinePresent" + budgetAddjustment.getManualEncumbrance());
          budgetAddjustment.setProcessed(false);
          budgetAddjustment.setAction("CO");
          budgetAddjustment.setDocumentStatus("DR");

          // insert history
          if (budgetAddjustment != null) {

            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", budgetAddjustment.getId());
            historyData.put("Comments", "");
            historyData.put("Status", "RE");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Budget_Adjustment_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.Budget_Adjustment_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Budget_Adjustment_DOCACTION_COLUMN);
            Utility.InsertApprovalHistory(historyData);
          }
          OBDal.getInstance().save(budgetAddjustment);
          OBDal.getInstance().flush();
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Efin_Budget_Adjustment_Reactivated@");
          bundle.setResult(result);
          return;

        } else {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("Efin_budget_adj_Linesreact_Failed"));
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {

      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().commitAndClose();
    }
  }
}
