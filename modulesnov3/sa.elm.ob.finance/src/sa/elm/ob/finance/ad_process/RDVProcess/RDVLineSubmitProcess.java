package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.RDVSubmitProcessDAO;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Divya on 09/05/2019
 */

public class RDVLineSubmitProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for submit action in RDV.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(RDVLineSubmitProcess.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;

    try {
      OBContext.setAdminMode();

      final String rdvTxnLineId = (String) bundle.getParams().get("Efin_Rdvtxnline_ID").toString();
      // getting EfinRDVTransactionLine event object by using rdvTxnId
      EfinRDVTxnline transactionLine = OBDal.getInstance().get(EfinRDVTxnline.class, rdvTxnLineId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = transactionLine.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();

      Connection conn = OBDal.getInstance().getConnection();
      boolean isPeriodOpen = true;
      Boolean chkRoleIsInDocRul = false;
      boolean allowUpdate = false;
      boolean allowDelegation = false;
      Boolean allowApprove = false;
      Date currentDate = new Date();
      String appstatus = "";
      String Lang = vars.getLanguage();
      String comments = (String) bundle.getParams().get("comments").toString();
      NextRoleByRuleVO nextApproval = null;

      // Check transaction period is opened or not before submitting record
      if ("CO".equals(transactionLine.getAction())) {
        isPeriodOpen = Utility.checkOpenPeriod(transactionLine.getMatchDate(),
            orgId.equals("0") ? vars.getOrg() : orgId, transactionLine.getClient().getId());
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
      if (transactionLine.getEfinRdv().getSalesOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class,
            transactionLine.getEfinRdv().getSalesOrder().getId());
        if ("ESCM_WD".equals(order.getEscmAppstatus())
            || "ESCM_OHLD".equals(order.getEscmAppstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_HoldWithdrawnPO@");
          bundle.setResult(result);
          return;
        }
      }

      if (transactionLine.getApprovalStatus().equals("DR")
          || transactionLine.getApprovalStatus().equals("REJ")) {
        int submitAllowed = CommonValidations.checkUserRoleForSubmit("efin_rdvtxnline",
            vars.getUser(), vars.getRole(), rdvTxnLineId, "Efin_Rdvtxnline_ID");
        if (submitAllowed == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      // Approval flow start
      // check role is present in document rule or not
      if (transactionLine.getApprovalStatus().equals("DR")
          || transactionLine.getApprovalStatus().equals("REJ")
          || transactionLine.getApprovalStatus().equals("WFA")) {
        if (transactionLine.getApprovalStatus().equals("DR")
            || transactionLine.getApprovalStatus().equals("REJ")) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, roleId, Resource.RDV_Transaction, BigDecimal.ZERO);
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
        if (transactionLine.getApprovalStatus().equals("DR")
            || transactionLine.getApprovalStatus().equals("REJ")) {
          if (transactionLine.getNetmatchAmt().compareTo(new BigDecimal(0)) < 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_LineNetAmt_LessthanZero@");
            bundle.setResult(result);
            return;

          }
        }
        // check netmatch amt should not be zero.
        if (transactionLine.getNetmatchAmt().compareTo(BigDecimal.ZERO) <= 0
            && transactionLine.getMatchAmt().compareTo(BigDecimal.ZERO) <= 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_RdvNetmatch_Negative@");
          bundle.setResult(result);
          return;
        }

        // // If the record is Forwarded or given RMI then throw error when any other user tries to
        // // approve the record without refreshing the page
        // if (transactionLine.getEUTForwardReqmoreinfo() != null) {
        // allowApprove = forwardDao.allowApproveReject(transaction.getEUTForwardReqmoreinfo(),
        // userId, roleId, Resource.RDV_Transaction);
        // }
        // if (transaction.getEUTReqmoreinfo() != null
        // || ((transaction.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
        // OBDal.getInstance().rollbackAndClose();
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // "@Efin_AlreadyPreocessed_Approve@");
        // bundle.setResult(result);
        // return;
        // }

        // check current role associated with document rule for approval flow
        if (!transactionLine.getApprovalStatus().equals("DR")
            && !transactionLine.getApprovalStatus().equals("REJ")) {
          if (transactionLine.getNextRole() != null) {
            java.util.List<EutNextRoleLine> li = transactionLine.getNextRole()
                .getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
          }
          if (transactionLine.getNextRole() != null) {
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                DocumentTypeE.RDV.getDocumentTypeCode());
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
        // check already approved or not
        if ((!vars.getRole()
            .equals(transactionLine.getRole() != null ? transactionLine.getRole().getId() : null))
            && (transactionLine.getApprovalStatus().equals("DR"))) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }
        if (!errorFlag) {

          // set value for approval history status
          if ((transactionLine.getApprovalStatus().equals("DR")
              || transactionLine.getApprovalStatus().equals("REJ"))
              && transactionLine.getAction().equals("CO")) {
            appstatus = "SUB";
          } else if (transactionLine.getApprovalStatus().equals("WFA")
              && transactionLine.getAction().equals("AP")) {
            appstatus = "AP";
          }

          // update next role
          JSONObject upresult = RDVSubmitProcessDAO.updateLineLevelStatus(conn, clientId, orgId,
              roleId, userId, transactionLine, appstatus, comments, currentDate, vars, nextApproval,
              Lang, bundle);
          if (upresult != null) {
            // if role does not associate with any user then dont allow to process for next approve
            if (upresult.has("count") && upresult.getInt("count") == -2) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
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
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_RDVAdvLine_Submit@");
              bundle.setResult(result);
              return;
            }
            // approve sucess message
            else if (upresult.has("count") && upresult.getInt("count") == 1) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_RDVAdvLine_Approve@");
              bundle.setResult(result);
              return;
            }
          }
        }

      } else if (transactionLine.getApprovalStatus().equals("APP")) {

        // chk already reactivated or not
        if (transactionLine.getApprovalStatus().equals("DR")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }
        if (!errorFlag) {
          // update Proposal event status if we reactivate
          transactionLine.setUpdated(new java.util.Date());
          transactionLine.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          transactionLine.setApprovalStatus("DR");
          transactionLine.setAction("CO");
          transactionLine.setTxnverStatus("DR");
          OBDal.getInstance().save(transactionLine);

          // insert approval history
          if (!StringUtils.isEmpty(transactionLine.getId())) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", transactionLine.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.RDV_TxnLine_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_TxnLine_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_TxnLine_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in RDVLineSubmitProcess:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
