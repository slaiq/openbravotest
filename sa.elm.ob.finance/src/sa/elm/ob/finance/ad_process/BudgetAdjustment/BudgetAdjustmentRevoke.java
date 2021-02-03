package sa.elm.ob.finance.ad_process.BudgetAdjustment;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.process.Budget.BudgetRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopalakrishnan on 10/10/2017
 * 
 */

public class BudgetAdjustmentRevoke implements Process {
  /**
   * This process allow the user to revoke the submitted budget Adjustment.
   */
  private static final Logger log = Logger.getLogger(BudgetRevoke.class);
  private final OBError obError = new OBError();

  @SuppressWarnings("rawtypes")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    final String BudgetAdjustementid = (String) bundle.getParams().get("Efin_Budgetadj_ID")
        .toString();
    BudgetAdjustment budgetAddjustment = OBDal.getInstance().get(BudgetAdjustment.class,
        BudgetAdjustementid);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = budgetAddjustment.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    BudgetAdjustment headerId = null;
    int j = 0;
    boolean isfundserrorFlag = true;
    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    EfinBudgetManencum manualId = null;
    // After Approve or Rework by approver if submiter is try to Revoke the same record then throw
    // error
    if ((budgetAddjustment.getDocumentStatus().equals("EFIN_AP"))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approved@");
      bundle.setResult(result);
      return;
    }

    int count = 0;
    boolean errorFlag = true;
    try {
      OBContext.setAdminMode(true);
      if (errorFlag) {
        BudgetAdjustment header = OBDal.getInstance().get(BudgetAdjustment.class,
            BudgetAdjustementid);

        // revert the changes in budget enquiry
        for (EFINFundsReq fundreqmgmt : header.getEFINFundsReqList()) {
          isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
              OBDal.getInstance().getConnection(), fundreqmgmt.getId(), false, true);
        }
        if (!isfundserrorFlag) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_FundsReq_Rev_Error@");
          bundle.setResult(result);
          return;
        }
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
            + "    and al.efin_budgetadj_id =:budgetAdjId and al.fundsreserved='Y'";
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
                /*
                 * objInquiryLine
                 * .setEncumbrance(objInquiryLine.getEncumbrance().subtract((BigDecimal) row[4]));
                 * objInquiryLine .setObincAmt(objInquiryLine.getObincAmt().subtract((BigDecimal)
                 * row[3]));
                 */
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
            if ((j % 100) == 0) {
              OBDal.getInstance().flush();
              // OBDal.getInstance().getSession().clear();
            }
            j++;
            // row[1]
          }
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
            OBDal.getInstance().remove(reqln);
          }
          OBDal.getInstance().remove(manualId);
          header.setManualEncumbrance(null);
        }
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setDocumentStatus("DR");
        header.setAction("CO");
        header.setProcessed(false);
        header.setNextRole(null);
        OBDal.getInstance().save(header);
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.BUDGET_ADJUSTMENT_RULE);
        headerId = header;
        if (!StringUtils.isEmpty(headerId.getId())) {
          JSONObject historyData = new JSONObject();
          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", headerId.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", "REV");
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.Budget_Adjustment_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.Budget_Adjustment_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.Budget_Adjustment_DOCACTION_COLUMN);
          count = Utility.InsertApprovalHistory(historyData);

        }
        log.debug("headerId:" + headerId.getId());
        log.debug("count:" + count);
        if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {

          // Removing forwardRMI id
          if (headerId.getEUTForward() != null) {
            // Removing the Role Access given to the forwarded user
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(headerId.getEUTForward());
            // Removing Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(headerId.getId(),
                Constants.BUDGETADJUSTMENT);

          }
          if (headerId.getEUTReqmoreinfo() != null) {
            // access remove
            // update status as "DR"
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(headerId.getEUTReqmoreinfo());

            // Remove Forward_Rmi id from transaction screens
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(headerId.getId(),
                Constants.BUDGETADJUSTMENT);

          }
          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("EFin_BudgetAdjustment_Revoke"));
        }

        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
      } else if (!errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("");
      }
      bundle.setResult(obError);
    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While Revoke budget Preparation :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
