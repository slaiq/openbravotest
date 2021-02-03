/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.FundsRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author J.Divya
 */

public class FundsRequestAction implements Process {
  /**
   * This process allow the user to submit the Funds Request and impact in Budget
   */
  private static final Logger LOG = LoggerFactory.getLogger(FundsRequestAction.class);
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();

    final String fundsreqId = bundle.getParams().get("Efin_Fundsreq_ID").toString(),
        clientId = bundle.getContext().getClient();
    final String userId = bundle.getContext().getUser();
    String roleId = bundle.getContext().getRole();
    Boolean errorFlag = false, isValid = true;
    int count = 0;
    Date currentDate = new Date();
    Boolean allowDelegation = false, chkRoleIsInDocRul = false, allowUpdate = false;
    String docType = null, docValue = null, currentRoleId = null;
    String appstatus = "";
    String comments = bundle.getParams().get("comments").toString();
    Boolean isDist = false;
    boolean isWarn = false;
    boolean isDummyRole = false;
    boolean isPeriodOpen = false;
    Boolean allowApprove = false;
    try {
      OBContext.setAdminMode();
      final EFINFundsReq req = OBDal.getInstance().get(EFINFundsReq.class, fundsreqId);
      final String orgId = req.getOrganization().getId();
      List<EFINFundsReqLine> reqline = req.getEFINFundsReqLineList();
      LOG.debug("getDocumentStatus:" + req.getDocumentStatus());
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      // Task #8198
      // check submitted role have the branch details or not
      if (req.getDocumentStatus().equals("DR")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // find the submitted role org/branch details

      if (req.getNextRole() != null) {
        if (req.getEfinSubmittedRole() != null && req.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = req.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (req.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // Budget Definition Closed Validation
      if (req.getEfinBudgetint() != null && req.getEfinBudgetint().getStatus().equals("CL")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      // Pre Year Close Validation
      if (req.getEfinBudgetint() != null && req.getEfinBudgetint().isPreclose()) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      // based on transaction type set the document rule BCUR means BCU Funds Request Approval flow
      // or else ORG Funds Request Approval flow
      if (req.getTransactionType().equals("BCUR")) {
        docType = Resource.BCU_BUDGET_DISTRIBUTION;
        docValue = DocumentTypeE.BCU_FUNDS_REQUEST_MANAGEMENT.getDocumentTypeCode();
      } else {
        docType = Resource.ORG_BUDGET_DISTRIBUTION;
        docValue = DocumentTypeE.ORG_FUNDS_REQUEST_MANAGEMENT.getDocumentTypeCode();
      }

      // chk lines is distribute or release and set the isDist flag
      if (reqline.size() > 0) {
        for (EFINFundsReqLine ln : reqline) {
          // JSONObject json = FundsReqeManagementDAO.checkFundsAval(ln);
          // LOG.debug(
          // json.get("is990Acct") + ", " + json.get("isFundGreater") + "," + json.get("isWarn"));
          // if (!req.getDocumentStatus().equals("CO")) {
          // if (json.get("is990Acct").equals("true")) {
          // if (json.get("isFundGreater").equals("true")) {
          //
          // if (json.get("isWarn").equals("true")) {
          // ln.setFailureReason(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
          // ln.setAlertStatus("WAR");
          // OBDal.getInstance().save(ln);
          // isWarn = true;
          // } else {
          // ln.setFailureReason(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
          // ln.setAlertStatus("FL");
          // OBDal.getInstance().save(ln);
          // errorFlag = true;
          // }
          //
          // }
          // }
          // }
          if (ln.getREQType().equals("DIST")) {
            isDist = true;
            break;
          }
        }
        // if (errorFlag == true) {
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // OBMessageUtils.messageBD("Efin_FRMLines_Failed"));
        // bundle.setResult(result);
        // return;
        // }
      }

      final List<Object> parameters = new ArrayList<Object>();
      parameters.add(req.getClient().getId());
      parameters.add(req.getSalesCampaign().getId());
      parameters.add(req.getSalesCampaign().getEfinBudgettype());
      parameters.add(req.getEfinBudgetint() != null ? req.getEfinBudgetint().getId() : null);
      parameters.add(req.getId());
      parameters.add(OBMessageUtils.messageBD("Efin_Budget_Revision_Rules"));
      if ("C".equals(req.getSalesCampaign().getEfinBudgettype())) {
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_CostAmount").replace("@", ""));
        parameters.add("");
      } else {
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount").replace("@", ""));
        parameters.add("");
      }
      parameters.add(req.isReserve());
      BigDecimal resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
          .call("efin_budgetdistr_commonvalid", parameters, null);

      int resultCount = resultCount1.intValue();
      if (resultCount == 0) {
        errorFlag = true;
        //OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_FRMLines_Failed"));
        bundle.setResult(result);
        return;
      }

      // if request submit or Approving then do the further process
      if (req.getDocumentStatus().equals("DR") || req.getDocumentStatus().equals("WFA")
          || req.getDocumentStatus().equals("RW")) {

        // check no lines
        if ((req.getDocumentStatus().equals("DR") || req.getDocumentStatus().equals("RW"))
            && req.getEFINFundsReqLineList().size() == 0) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_FundsReq_OneLine@");
          bundle.setResult(result);
          return;
        }

        // check role is present in document rule or not
        if (req.getDocumentStatus().equals("DR") || req.getDocumentStatus().equals("RW")) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, userId, roleId, docType, BigDecimal.ZERO);
          LOG.debug("chkRoleIsInDocRul:" + chkRoleIsInDocRul);
          if (!chkRoleIsInDocRul) {
            errorFlag = true;

            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_RoleIsNotIncInDocRule@");// ESCM_RoleIsNotIncInDocRule
            bundle.setResult(result);
            return;
          }
        }
        if (req.getDocumentStatus().equals("DR") || req.getDocumentStatus().equals("RW")) {
          int submitAllowed = CommonValidations.checkUserRoleForSubmit("Efin_Fundsreq",
              vars.getUser(), vars.getRole(), fundsreqId, "Efin_Fundsreq_ID");
          if (submitAllowed == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Role_NotFundsReserve_submit@");
            bundle.setResult(result);
            return;
          }
        }

        // Check transaction period is opened or not
        if (req.getAction().equals("CO")) {
          isPeriodOpen = Utility.checkOpenPeriod(req.getTrxdate(), orgId, clientId);
          if (!isPeriodOpen) {
            if (req.getDocumentStatus().equals("DR")) {
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
                  .equals(UtilityDAO.getYearId(req.getTrxdate(), clientId))) {
                isPeriodOpen = Utility.checkOpenPeriod(currentDate,
                    orgId.equals("0") ? vars.getOrg() : orgId, req.getClient().getId());

                if (!isPeriodOpen) {
                  errorFlag = true;
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@PeriodNotAvailable@");
                  bundle.setResult(result);
                  return;
                } else {
                  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                  Date now = new Date();
                  Date todaydate = dateFormat.parse(dateFormat.format(now));
                  req.setAccountingDate(todaydate);
                  req.setTrxdate(todaydate);
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
        // If the record is Forwarded or given RMI then throw error when any other user tries to
        // approve the record without refreshing the page
        if (req.getEUTForward() != null) {
          allowApprove = forwardDao.allowApproveReject(req.getEUTForward(), userId, roleId,
              docType);
        }
        if (req.getEUTReqmoreinfo() != null || ((req.getEUTForward() != null) && (!allowApprove))) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        // check current role associated with document rule for approval flow
        if (!req.getDocumentStatus().equals("DR") && !req.getDocumentStatus().equals("RW")) {
          if (req.getNextRole() != null) {
            java.util.List<EutNextRoleLine> li = req.getNextRole().getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
          }
          if (req.getNextRole() != null) {
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId, docValue);

            /*
             * sql = ""; Connection con = OBDal.getInstance().getConnection(); sql =
             * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on"
             * + " dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '" +
             * currentDate + "' and to_date >='" + currentDate + "' and  document_type='" + docValue
             * + "'"; ps = con.prepareStatement(sql); LOG.debug("sql:" + sql); rs =
             * ps.executeQuery(); while (rs.next()) { String roleid = rs.getString("ad_role_id"); if
             * (roleid.equals(roleId)) { allowDelegation = true; break; } }
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

        // throw the error message while 2nd user try to approve while 1st user already reworked
        // that
        // record with same role
        if ((!vars.getRole().equals(req.getRole().getId()))
            && (req.getDocumentStatus().equals("RW") || req.getDocumentStatus().equals("DR"))) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        // checking for 'Release to HQ' type, whether increase amount is greater than Funds
        // available #Task no. 7834
        // if (reqline.size() > 0) {
        // if (!req.isReserve()) {
        // for (EFINFundsReqLine ln : reqline) {
        // if (ln.getREQType().equals("REL")) {
        // JSONObject json = FundsReqeManagementDAO.checkFundsAval(ln);
        // if (json.get("is990Acct").equals("false")) {
        // if (ln.getFundsAvailable().compareTo(ln.getIncrease()) == -1) {
        // ln.setFailureReason(
        // OBMessageUtils.messageBD("Efin_FundReq_ReqGreThanFundsAvail"));
        // ln.setAlertStatus("FL");
        // OBDal.getInstance().save(ln);
        // errorFlag = true;
        // }
        // }
        // }
        // }
        // }
        // if (errorFlag == true) {
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // OBMessageUtils.messageBD("Efin_FRMLines_Failed"));
        // bundle.setResult(result);
        // return;
        // }
        // }

        // check common validation before approving or submitting the record
        // isValid = CommonValidations.checkValidations(fundsreqId, "BudgetDistribution",
        // OBContext.getOBContext().getCurrentClient().getId(), "CO", isWarn);
        // LOG.debug("isValid:" + isValid);

        // if common validation return false throwing error
        // if (!isValid) {
        // OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revision_error@");
        // bundle.setResult(result);
        // return;
        // }
        // budget Revision Rule Validation
        // int ruleCount = 0;
        // if (!isWarn) {
        // ruleCount = BudgetRevisionRuleValidation.checkRuleValidation(fundsreqId, "BD", clientId,
        // req.getEfinBudgetint().getId());
        // LOG.debug("ruleCount:" + ruleCount);
        // LOG.debug("errorFlag:" + errorFlag);
        // }
        // // if revision rule not satisfied then throwing error
        // if (ruleCount == 2) {
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // OBMessageUtils.messageBD("Efin_budget_AdjustmentLines_Failed"));
        // bundle.setResult(result);
        // return;
        // }
        if (!errorFlag) {

          errorFlag = FundsRequestActionDAO.chkIncAndDecAmtEqulorNot(req, conn);
          LOG.debug("errorFlag123:" + errorFlag);

          // if not equal then throwing error
          if (errorFlag) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_FundReq_ChkFailRes@");
            bundle.setResult(result);
            return;
          }
          // else update the approver details
          else {

            // approval flow
            LOG.debug("errorFlag123:" + errorFlag);

            // set the approval history status
            if ((req.getDocumentStatus().equals("DR") || req.getDocumentStatus().equals("RW"))
                && req.getAction().equals("CO")) {
              appstatus = "SUB";
            } else if (req.getDocumentStatus().equals("WFA") && req.getAction().equals("AP")) {
              appstatus = "AP";
            }
            // set roleid value if next role line have dummy role id or else current role id is
            // roleid
            currentRoleId = roleId;
            if (req.getNextRole() != null) {
              roleId = req.getNextRole().getEutNextRoleLineList().get(0).getDummyRole() == null
                  ? roleId
                  : req.getNextRole().getEutNextRoleLineList().get(0).getDummyRole();
              if (req.getNextRole().getEutNextRoleLineList().get(0).getDummyRole() != null) {
                isDummyRole = true;
              }
            }

            // update the next role who is next approver and update the header status of funds
            // request based on approval flow and sending the alert
            JSONObject resultjson = FundsRequestActionDAO.updateNextRole(conn, clientId,
                req.getTransactionOrg().getId(), roleId, userId, req, appstatus, comments,
                currentDate, vars, docType, isDist, currentRoleId, isDummyRole);
            // if resultjson return 1 or 2 throw success message
            LOG.debug("resultjson:" + resultjson);

            // if (isWarn) {
            // OBError result = OBErrorBuilder.buildMessage(null, "warning",
            // OBMessageUtils.messageBD("Efin_FundsReqMgmt_Warning"));
            // bundle.setResult(result);
            // return;
            // }
            if (resultjson.getString("count").equals("1")
                || resultjson.getString("count").equals("2")) {
              // FundsRequestActionDAO.updateFundsReq(req);
              if (resultCount == 2) {
                OBError result = OBErrorBuilder.buildMessage(null, "warning",
                    OBMessageUtils.messageBD("Efin_budget_distribution_Warning"));
                bundle.setResult(result);
                return;
              } else {
                if (resultjson.getString("count").equals("1")) {
                  OBError result = OBErrorBuilder.buildMessage(null, "success",
                      "@EFIN_FundsReqApp_Success@");
                  bundle.setResult(result);
                  return;
                } else {
                  OBError result = OBErrorBuilder.buildMessage(null, "success",
                      "@EFIN_FundsReq_SubmitSuccess@");
                  bundle.setResult(result);
                  return;
                }
              }
            }
            // else resultjson return -1 or -2 throw error message
            else if (resultjson.get("count").equals("-1") || resultjson.get("count").equals("-2")
                || resultjson.get("count").equals("-3")) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  resultjson.getString("errorMsgs"));
              bundle.setResult(result);
              return;
            }
          }
        }
      }
      // reactivate process
      else if (req.getDocumentStatus().equals("CO")) {

        // reactivate the budget inquiry changes while reactivate the funds request
        errorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(conn, fundsreqId, false, true);
        LOG.debug("count:" + count);
        if (!errorFlag) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              OBMessageUtils.messageBD("Efin_budget_adj_Linesreact_Failed"));
          bundle.setResult(result);
          return;
        } else {

          // After reactivate if all the column (Current budget, fundsavailble, disincrease,
          // disdecrease, depincrease, depdecrease, revisionamount, enumcumbrance, actual) are zero
          // then delete the entry from budget inquiry

          for (EFINFundsReqLine line : req.getEFINFundsReqLineList()) {
            if (line.getToaccount() != null) {
              OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
                  EfinBudgetInquiry.class,
                  "accountingCombination.id='" + line.getToaccount().getId()
                      + "' and efinBudgetint.id = '" + req.getEfinBudgetint().getId()
                      + "' and salesCampaign.id = '" + req.getSalesCampaign().getId() + "' ");
              List<EfinBudgetInquiry> inquiryList = budgetinquiry.list();
              if (inquiryList != null && inquiryList.size() > 0) {

                EfinBudgetInquiry inquiry = inquiryList.get(0);
                if ((inquiry.getCurrentBudget().add(inquiry.getFundsAvailable())
                    .add(inquiry.getREVAmount()).add(inquiry.getSpentAmt())
                    .add(inquiry.getDisdecAmt()).add(inquiry.getDisincAmt())
                    .add(inquiry.getDeptIncrease()).add(inquiry.getDeptDecrease())
                    .add(inquiry.getEncumbrance()).add(inquiry.getObdecAmt())
                    .add(inquiry.getObincAmt())).compareTo(BigDecimal.ZERO) == 0) {
                  OBDal.getInstance().remove(inquiry);
                }

              }
              OBDal.getInstance().flush();
            }

          }

          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), docType);
          if (!StringUtils.isEmpty(req.getId())) {
            appstatus = "REA";

            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", req.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", appstatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Budget_Distribution_History);
            historyData.put("HeaderColumn", ApprovalTables.Budget_Distribution_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Budget_Distribution_DOCACTION_COLUMN);
            Utility.InsertApprovalHistory(historyData);
          }

          OBDal.getInstance().flush();
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@EFIN_FundsReq_ReactSuccess@");
          bundle.setResult(result);
          return;
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.debug("Exeception in FundsRequestAction:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
