package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.RDVSubmitProcessDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.hook.RDVSubmitCompletionHookCaller;
import sa.elm.ob.finance.dms.service.DMSRDVService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSRDVServiceImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
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
 * @author Poongodi on 18/01/2018
 */

public class RDVSubmitProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for submit action in RDV.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(RDVSubmitProcess.class);
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;

    try {
      OBContext.setAdminMode();
      // declaring variables
      final String rdvTxnId = (String) bundle.getParams().get("Efin_Rdvtxn_ID").toString();
      String tabId = (String) bundle.getParams().get("tabId");
      // getting EfinRDVTransaction event object by using rdvTxnId
      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class, rdvTxnId);
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();

      final String clientId = (String) bundle.getContext().getClient();
      String orgId = transaction.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      NextRoleByRuleVO nextApproval = null;
      String Lang = vars.getLanguage();
      String intialStatus = transaction.getAppstatus();
      String rdvWindowId = "A9E4930BC3A8499C82E358FADA3CDEC8";

      Connection conn = OBDal.getInstance().getConnection();
      ConnectionProvider connection = bundle.getConnection();
      Boolean chkRoleIsInDocRul = false;
      boolean allowUpdate = false;
      boolean allowDelegation = false;
      Boolean allowApprove = false;
      boolean chkacccombination = false;
      String appstatus = "";
      Date currentDate = new Date();
      boolean isPeriodOpen = true;
      boolean isRDVCreatedFromBudgRev = false;
      Boolean isFullyMatched = false;
      Boolean isMatchQtyAmtZero = false;
      DocumentTypeE docType = null;
      String transaction_DocType = null;
      BigDecimal amount = BigDecimal.ZERO;
      BigDecimal PototalScheduleAmtValue = BigDecimal.ZERO;
      BigDecimal scheduleAmt = BigDecimal.ZERO;
      Order latestOrderVer = null;
      if (transaction.getEfinRdv().getSalesOrder() != null) {
        latestOrderVer = PurchaseInvoiceSubmitUtils
            .getLatestOrderComplete(transaction.getEfinRdv().getSalesOrder());

      }

      String preferenceValue = "N";

      // submit process start

      // Check transaction period is opened or not before submitting record

      // if ("CO".equals(transaction.getAction())) {
      // isPeriodOpen = Utility.checkOpenPeriod(transaction.getTxnverDate(),
      // orgId.equals("0") ? vars.getOrg() : orgId, transaction.getClient().getId());
      // if (!isPeriodOpen) {
      // errorFlag = true;
      // OBDal.getInstance().rollbackAndClose();
      // OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
      // bundle.setResult(result);
      // return;
      // }
      // }
      // Task#8180 : Not allow to complete the process when PO/Contract closed

      // DMS Validation check

      try {
        preferenceValue = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
            "Eut_AllowDMSIntegration", true, vars.getClient(), orgId, null, null, null);
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;
      } catch (PropertyException e) {
        preferenceValue = "N";
      }

      if ("Y".equals(preferenceValue)) {
        OBQuery<Attachment> attachQry = OBDal.getInstance().createQuery(Attachment.class,
            "as e where e.record = :recordId and e.eutDmsAttachpath is null and e.table.id ='B4146A5918884533B13F57A574EFF9D5' ");
        attachQry.setNamedParameter("recordId", rdvTxnId);
        List<Attachment> fileList = attachQry.list();
        for (Attachment attach : fileList) {
          String extension = FilenameUtils.getExtension(attach.getName());
          if ("pdf".equals(extension)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EUT_NOT_ALLFILES_SENTTODMS@");
            bundle.setResult(result);
            return;
          }

        }

      }

      if (transaction.getEfinRdv().getSalesOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class,
            transaction.getEfinRdv().getSalesOrder().getId());
        if ("ESCM_CL".equals(order.getEscmAppstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PO_Closed@");
          bundle.setResult(result);
          return;
        }
      }

      // approval flow based on last version checkbox
      if (transaction.isLastversion()) {
        transaction_DocType = Resource.RDV_LAST_VERSION;
        amount = latestOrderVer != null ? latestOrderVer.getGrandTotalAmount() : BigDecimal.ZERO;
      } else if (!transaction.isLastversion()) {
        transaction_DocType = Resource.RDV_Transaction;
        amount = BigDecimal.ZERO;
      }

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
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

      JSONObject amtValidation = RDVSubmitProcessDAO.isAmtValidation(transaction);
      if (amtValidation != null && amtValidation.has("value")) {
        if (amtValidation.getString("value").equals("0") && amtValidation.has("msg")) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              amtValidation.getString("msg"));
          bundle.setResult(result);
          return;
        }
      }

      // chk rdv is created from Budget Revision
      if (transaction.getEfinRDVTxnlineList().size() > 0) {
        for (EfinRDVTxnline txnLineObj : transaction.getEfinRDVTxnlineList()) {
          List<EfinRdvHoldAction> rdvHoldActList = txnLineObj.getEfinRdvHoldActionList().stream()
              .filter(a -> a.getRDVHoldRel() != null
                  && a.getRDVHoldRel().getEfinRdvBudgholdline() != null)
              .collect(Collectors.toList());
          if (rdvHoldActList.size() > 0) {
            isRDVCreatedFromBudgRev = true;
            break;
          }
        }
      }

      // if (!transaction.isWebservice() && (transaction.getAppstatus().equals("DR")
      // || transaction.getAppstatus().equals("REJ"))) {
      // int submitAllowed = CommonValidations.checkUserRoleForSubmit("efin_rdvtxn", vars.getUser(),
      // vars.getRole(), rdvTxnId, "Efin_Rdvtxn_ID");
      //
      // if (submitAllowed == 0 && !isRDVCreatedFromBudgRev) {
      // errorFlag = true;
      // OBDal.getInstance().rollbackAndClose();
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // "@Efin_Role_NotFundsReserve_submit@");
      // bundle.setResult(result);
      // return;
      // }
      // }

      // Approval flow start
      // check role is present in document rule or not
      if (transaction.getAppstatus().equals("DR") || transaction.getAppstatus().equals("REJ")
          || transaction.getAppstatus().equals("WFA")) {

        List<EfinBudgetControlParam> controlList = new ArrayList<EfinBudgetControlParam>();
        String agencyHqOrg = null,
            submittedRoleId = transaction.getSubmitroleid() != null ? transaction.getSubmitroleid()
                : OBContext.getOBContext().getRole().getId();
        // check the hqorg
        OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance()
            .createQuery(EfinBudgetControlParam.class, "as e where e.client.id =:clientId");
        controlParam.setNamedParameter("clientId", clientId);
        controlList = controlParam.list();
        if (controlParam != null && controlList.size() > 0) {
          agencyHqOrg = controlList.get(0).getAgencyHqOrg().getId();
        }
        // if (!agencyHqOrg.equals(orgId)) {
        if (submittedRoleId != null) {
          Role role_access = OBDal.getInstance().get(Role.class, submittedRoleId);
          if (role_access != null && role_access.getEutReg() != null) {
            String branchOrg = role_access.getEutReg().getId();
            if (branchOrg != null) {
              orgId = branchOrg;
            }

          }
        }
        // }
        if (transaction.getAppstatus().equals("DR") || transaction.getAppstatus().equals("REJ")) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, roleId, transaction_DocType, amount);
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
        if (transaction.getAppstatus().equals("DR") || transaction.getAppstatus().equals("REJ")) {
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

        // If the record is Forwarded or given RMI then throw error when any other user tries to
        // approve the record without refreshing the page
        if (transaction.getEUTForwardReqmoreinfo() != null) {
          allowApprove = forwardDao.allowApproveReject(transaction.getEUTForwardReqmoreinfo(),
              userId, roleId, transaction_DocType);
        }
        if (transaction.getEUTReqmoreinfo() != null
            || ((transaction.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }

        // check current role associated with document rule for approval flow
        if (!transaction.getAppstatus().equals("DR") && !transaction.getAppstatus().equals("REJ")) {
          if (transaction.getNextRole() != null) {
            java.util.List<EutNextRoleLine> li = transaction.getNextRole().getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
          }
          if (transaction.getNextRole() != null) {
            if (transaction.isLastversion()) {
              docType = DocumentTypeE.RDV_LV;
            } else if (!transaction.isLastversion()) {
              docType = DocumentTypeE.RDV;
            }
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                docType.getDocumentTypeCode());
            /*
             * sql = ""; Connection con = OBDal.getInstance().getConnection(); PreparedStatement st
             * = null; ResultSet rs1 = null; sql =
             * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
             * + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_124'";
             * st = con.prepareStatement(sql); rs1 = st.executeQuery(); while (rs1.next()) { String
             * roleid = rs1.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation =
             * true; break; } }
             */
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
        //
        // // check already approved or not
        // if (!transaction.isWebservice() && !isRDVCreatedFromBudgRev
        // && (!vars.getRole()
        // .equals(transaction.getRole() != null ? transaction.getRole().getId() : null))
        // && (transaction.getAppstatus().equals("DR"))) {
        // errorFlag = true;
        // OBDal.getInstance().rollbackAndClose();
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // "@Efin_AlreadyPreocessed_Approve@");
        // bundle.setResult(result);
        // return;
        // }

        // if line has without accounting combination then should throw the error if advance flag as
        // no
        if ((transaction.getAppstatus().equals("DR") || transaction.getAppstatus().equals("REJ"))
            && transaction.getAction().equals("CO")) {
          for (EfinRDVTxnline line : transaction.getEfinRDVTxnlineList()) {
            if (!line.isAdvance() && !line.isSummaryLevel()
                && line.getAccountingCombination() == null) {
              chkacccombination = true;
              break;
            }
          }
          if (chkacccombination) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@efin_Rdvline_uniquecode@");
            bundle.setResult(result);
            return;
          }

        }

        // Check Encumbrance Amount is Zero Or Negative
        if (transaction.getEfinRdv().getManualEncumbrance() != null) {
          encumLinelist = transaction.getEfinRdv().getManualEncumbrance()
              .getEfinBudgetManencumlinesList();
        }
        if (encumLinelist.size() > 0)
          checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

        if (checkEncumbranceAmountZero) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
          bundle.setResult(result);
          return;
        }

        if (!errorFlag) {
          // set value for approval history status
          if ((transaction.getAppstatus().equals("DR") || transaction.getAppstatus().equals("REJ"))
              && transaction.getAction().equals("CO")) {
            appstatus = "SUB";
          } else if (transaction.getAppstatus().equals("WFA")
              && transaction.getAction().equals("AP")) {
            appstatus = "AP";
          }

          // update hold amount if match amount changed.
          /*
           * Query query = null; List holdList = null; RdvHoldActionDAO dao = null; Connection con =
           * null; String oldAction = ""; BigDecimal holdamtTxnOnly = BigDecimal.ZERO,
           * holdamtLineOnly = BigDecimal.ZERO, totalHoldAmt = BigDecimal.ZERO; BigDecimal lineMatch
           * = BigDecimal.ZERO, totalMatch = BigDecimal.ZERO; EfinRdvHoldTypes oldholdTypeId = null;
           * BigDecimal diffpenltyAmt = BigDecimal.ZERO, weigtage = BigDecimal.ZERO; dao = new
           * RdvHoldActionDAOimpl(con); String sqlString =
           * "select coalesce(sum(rdv_hold_amount),0) as amt,act.txngroupref,act.action,act.efin_rdv_hold_types_id from efin_rdv_hold_action act "
           * + "    join efin_rdvtxnline ln on ln.efin_rdvtxnline_id = act.efin_rdvtxnline_id " +
           * "    where ln.efin_rdvtxn_id =:txnId  and act.istxn='Y' " +
           * "    group by act.action,act.efin_rdv_hold_types_id,act.txngroupref "; query =
           * OBDal.getInstance().getSession().createSQLQuery(sqlString); query.setParameter("txnId",
           * transaction.getId()); holdList = query.list(); totalMatch =
           * transaction.getMatchAmt().subtract(transaction.getADVDeduct())
           * .subtract(transaction.getPenaltyAmt()); if (holdList != null && holdList.size() > 0) {
           * for (Object holdObj : holdList) { Object[] holdRec = (Object[]) holdObj; if (holdRec[1]
           * != null) { List<EfinRdvHoldAction> holdActionList = null; OBQuery<EfinRdvHoldAction>
           * holdActList = OBDal.getInstance() .createQuery(EfinRdvHoldAction.class,
           * "e where e.txngroupref=:refId"); holdActList.setNamedParameter("refId", holdRec[1]);
           * holdActionList = holdActList.list();
           * 
           * for (EfinRdvHoldAction holdAction : holdActionList) { if
           * (holdAction.getEfinRdvtxnline().isMatch()) { // lineMatch =
           * holdAction.getEfinRdvtxnline().getMatchAmt() //
           * .subtract(holdAction.getEfinRdvtxnline().getADVDeduct()) //
           * .subtract(holdAction.getEfinRdvtxnline().getPenaltyAmt()); lineMatch =
           * holdAction.getEfinRdvtxnline().getMatchAmt(); weigtage = ((lineMatch.divide(totalMatch,
           * 6, BigDecimal.ROUND_HALF_EVEN)) .multiply(new
           * BigDecimal(holdRec[0].toString()))).setScale(2, RoundingMode.HALF_UP); //
           * holdAction.setRDVHoldAmount(weigtage); holdamtTxnOnly = holdamtTxnOnly.add(weigtage);
           * oldAction = holdAction.getAction(); oldholdTypeId = holdAction.getEfinRdvHoldTypes();
           * diffpenltyAmt = weigtage.subtract(holdAction.getRDVHoldAmount());
           * OBDal.getInstance().save(holdAction); OBDal.getInstance().flush();
           * dao.insertHoldHeader(holdAction, holdAction.getEfinRdvtxnline(), diffpenltyAmt,
           * oldholdTypeId, oldAction); } else { dao.deleteHoldHed(holdAction); } } } } }
           * 
           * for (EfinRDVTxnline txnLine : transaction.getEfinRDVTxnlineList()) { for
           * (EfinRdvHoldAction holdAction : txnLine.getEfinRdvHoldActionList()) { if
           * (!holdAction.isTxn()) { holdamtLineOnly =
           * holdamtLineOnly.add(holdAction.getRDVHoldAmount()); } } }
           * 
           * totalHoldAmt = holdamtTxnOnly.add(holdamtLineOnly); // error if netmatch amount went
           * -ve. if (holdList.size() > 0 &&
           * transaction.getMatchAmt().subtract(transaction.getPenaltyAmt())
           * .subtract(transaction.getADVDeduct()).subtract(totalHoldAmt)
           * .compareTo(BigDecimal.ZERO) <= 0) { errorFlag = true;
           * OBDal.getInstance().rollbackAndClose(); OBError result =
           * OBErrorBuilder.buildMessage(null, "error", "@Efin_Rdv_Netmatch_Negative@");
           * bundle.setResult(result); return; }
           */

          // update next role
          JSONObject upresult = RDVSubmitProcessDAO.updateHeaderStatus(conn, clientId, orgId,
              roleId, userId, transaction, appstatus, comments, currentDate, vars, nextApproval,
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

            if (upresult.has("count") && upresult.getInt("count") == -5) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  upresult.getString("errormsg"));
              bundle.setResult(result);
              return;
            }

            // submit success message
            else if (upresult.has("count") && upresult.getInt("count") == 2) {

              // // Is Last version checkbox validation
              /*
               * if (!transaction.isAdvancetransaction()) {
               * 
               * Order latestOrder = PurchaseInvoiceSubmitUtils
               * .getLatestOrderComplete(transaction.getEfinRdv().getSalesOrder());
               * 
               * if (latestOrder != null) { if
               * (latestOrder.getEscmReceivetype().equals(Constants.QTY_BASED)) { isFullyMatched =
               * RDVSubmitProcessDAO.chkFullyMatchedOrNot(latestOrder, transaction); } else if
               * (latestOrder.getEscmReceivetype().equals(Constants.AMOUNT_BASED)) { isFullyMatched
               * = RDVSubmitProcessDAO.chkFullyMatchedOrNot(latestOrder, transaction); }
               * 
               * // check current version match qty/amt is equal to zero // ( doing only
               * penalty/hold release in a version ) -> Don't update isMatchQtyAmtZero =
               * RDVSubmitProcessDAO.isMatchQtyAmtZero(latestOrder, transaction);
               * 
               * if (isFullyMatched && !transaction.isLastversion() && !isMatchQtyAmtZero) {
               * transaction.setLastversion(true); OBDal.getInstance().save(transaction); } } }
               */

              // DMS integration starts

              JSONObject parameters = new JSONObject();
              if ("WFA".equals(intialStatus)) {
                parameters.put("status", "approve");
              } else {
                parameters.put("status", "submit");
              }
              parameters.put("documentType", transaction_DocType);
              parameters.put("isFinalLevelApprove", false);
              parameters.put("userId", userId);
              parameters.put("tabId", tabId);

              WeldUtils.getInstanceFromStaticBeanManager(RDVSubmitCompletionHookCaller.class)
                  .executeHook(transaction, parameters, vars, connection);

              OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Submit@");
              bundle.setResult(result);
              return;
            }
            // approve sucess message
            else if (upresult.has("count") && upresult.getInt("count") == 1) {

              // // Is Last version checkbox validation
              /*
               * if (!transaction.isAdvancetransaction()) {
               * 
               * Order latestOrder = PurchaseInvoiceSubmitUtils
               * .getLatestOrderComplete(transaction.getEfinRdv().getSalesOrder());
               * 
               * if (latestOrder != null) { if
               * (latestOrder.getEscmReceivetype().equals(Constants.QTY_BASED)) { isFullyMatched =
               * RDVSubmitProcessDAO.chkFullyMatchedOrNot(latestOrder, transaction); } else if
               * (latestOrder.getEscmReceivetype().equals(Constants.AMOUNT_BASED)) { isFullyMatched
               * = RDVSubmitProcessDAO.chkFullyMatchedOrNot(latestOrder, transaction); }
               * 
               * // check current version match qty/amt is equal to zero // ( doing only
               * penalty/hold release in a version ) -> Don't update isMatchQtyAmtZero =
               * RDVSubmitProcessDAO.isMatchQtyAmtZero(latestOrder, transaction);
               * 
               * if (isFullyMatched && !transaction.isLastversion() && !isMatchQtyAmtZero) {
               * transaction.setLastversion(true); OBDal.getInstance().save(transaction); } } }
               */

              // When RDV transaction is approved from branch send alert to HQ RDV user
              EfinBudgetControlParam budgContrparam = FundsReqMangementDAO
                  .getControlParam(clientId);
              if (budgContrparam != null && budgContrparam.getAgencyHqOrg() != null) {
                String hqOrg = budgContrparam.getAgencyHqOrg().getId();
                String submitterRoleStr = transaction.getSubmitroleid();
                if (submitterRoleStr != null) {
                  Role submitterRole = OBDal.getInstance().get(Role.class, submitterRoleStr);
                  if (submitterRole != null && submitterRole.getEutReg() != null) {
                    String branchOrg = submitterRole.getEutReg().getId();
                    if (!hqOrg.equals(branchOrg)) { // Branch org
                      // Send alert to HQ RDV User
                      User user = OBDal.getInstance().get(User.class, userId);
                      String Description = sa.elm.ob.finance.properties.Resource
                          .getProperty("finance.rdv.approved", Lang) + " " + user.getName();

                      AlertUtility.alertInsertionPreferenceBudUser(transaction.getId(),
                          transaction.getTXNVersion()
                              + (StringUtils.isEmpty(transaction.getCertificateNo()) ? ""
                                  : "-" + transaction.getCertificateNo()),
                          "EFIN_HQ_RDV_User", transaction.getClient().getId(), Description, "NEW",
                          AlertWindow.RDVTransaction, "finance.rdv.approved",
                          Constants.GENERIC_TEMPLATE, rdvWindowId, null);
                    }
                  }
                }
              }

              JSONObject parameters = new JSONObject();
              parameters.put("status", "approve");
              parameters.put("isFinalLevelApprove", true);
              parameters.put("documentType", transaction_DocType);
              parameters.put("userId", userId);
              parameters.put("tabId", tabId);

              WeldUtils.getInstanceFromStaticBeanManager(RDVSubmitCompletionHookCaller.class)
                  .executeHook(transaction, parameters, vars, connection);

              OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Approve@");
              bundle.setResult(result);
              return;
            }
          }
        }
      }
      // reactive process start
      else if (transaction.getAppstatus().equals("APP")) {

        // chk already reactivated or not
        if (transaction.getAppstatus().equals("DR")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }

        if (!errorFlag) {
          // update Proposal event status if we reactivate
          transaction.setUpdated(new java.util.Date());
          transaction.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          transaction.setAppstatus("DR");
          transaction.setAction("CO");
          transaction.setTxnverStatus("DR");
          OBDal.getInstance().save(transaction);

          if (transaction.isAdvancetransaction()
              && transaction.getEfinRDVTxnlineList().size() > 0) {
            EfinRDVTxnline txnLineObj = transaction.getEfinRDVTxnlineList().get(0);
            txnLineObj.setAction("CO");
            txnLineObj.setApprovalStatus("DR");
            txnLineObj.setTxnverStatus("DR");
            OBDal.getInstance().save(txnLineObj);
          }

          // insert approval history
          if (!StringUtils.isEmpty(transaction.getId())) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", transaction.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.RDV_Txn_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_Txn_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_Txn_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }

          try {
            // DMS integration
            DMSRDVService dmsService = new DMSRDVServiceImpl();
            dmsService.rejectAndReactivateOperations(transaction);
          } catch (Exception e) {
            log.error("Error while deleting the record in dms reactivate" + e.getMessage());
          }

          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }

      } // reactive process end

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in RDVSubmitProcess:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}