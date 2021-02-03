package sa.elm.ob.scm.ad_process.ReturnTransaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
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
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
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
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 *
 */

public class ReturnTransactionSubmit extends DalBaseProcess {
  /**
   * This servlet is responsible for the Submit process in Return Transaction
   */
  private static final Logger log = LoggerFactory.getLogger(ReturnTransactionSubmit.class);
  private String currentRoleId = null;
  private static String errorMsgs = null;
  private static String failureMsg = null;

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String appstatus = "";
    boolean errorFlag = false;

    try {

      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus();
      String DocAction = inout.getEscmDocaction();
      NextRoleByRuleVO nextApproval = null;
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      currentRoleId = (String) bundle.getContext().getRole();
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

      if (inout.getEutNextRole() != null) {
        if (inout.getEutNextRole().getEutNextRoleLineList() != null) {
          roleId = (inout.getEutNextRole().getEutNextRoleLineList().get(0).getDummyRole() == null
              ? roleId
              : inout.getEutNextRole().getEutNextRoleLineList().get(0).getDummyRole());
        }

      }
      PreparedStatement st = null;
      Date currentDate = new Date();
      int count = 0;
      MaterialTransaction trans = null;
      String query = null;
      String status = null;
      String product = null;
      String errorMsg = "";
      Boolean allowApprove = false, allowDelegation = false, allowUpdate = false;

      Boolean chkRoleIsInDocRul, chkSubRolIsInFstRolofDR = false;
      String comments = (String) bundle.getParams().get("notes").toString();

      // -- forward/rmi
      if (inout.getEutForward() != null) {
        allowApprove = forwardDao.allowApproveReject(inout.getEutForward(), userId, roleId,
            Resource.Return_Transaction);
      }
      if (inout.getEutReqmoreinfo() != null
          || ((inout.getEutForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if ((!vars.getUser().equals(inout.getCreatedBy().getId()))
          && (DocStatus.equals("ESCM_REJ"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // -- forward/rmi

      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }

      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_RJD")) {
        if (inout.getEutNextRole() != null) {
          java.util.List<EutNextRoleLine> li = inout.getEutNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (inout.getEutNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();

          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              Resource.Return_Transaction);
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

      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        if (line.getEscmCustodyTransactionList().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTran_LineQtyZero@");
          bundle.setResult(result);
          return;
        }
      }
      // checking for already the custody tag present in CT or RT in inprogress status
      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
          status = null;
          OBQuery<Escm_custody_transaction> transaction = OBDal.getInstance()
              .createQuery(Escm_custody_transaction.class, " as e where e.goodsShipmentLine.id in "
                  + " ( select line.id  from MaterialMgmtShipmentInOutLine line  left join line.shipmentReceipt hd "
                  + " where (hd.escmReceivingtype='INR'  or hd.escmIscustodyTransfer='Y') and hd.escmDocstatus='ESCM_IP'"
                  + " and hd.id <>:inoutID) and e.escmMrequestCustody.id=:custodyID ");
          transaction.setNamedParameter("inoutID", inout.getId());
          transaction.setNamedParameter("custodyID", tran.getEscmMrequestCustody().getId());

          if (transaction.list().size() > 0) {
            errorFlag = true;
            status = null;
            for (Escm_custody_transaction tra : transaction.list()) {
              if (status == null) {
                status = OBMessageUtils.messageBD("ESCM_CT_TagAlreadyUsed");
                status = status.replace("%",
                    tra.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo()
                        + " Used by user - " + tra.getCreatedBy().getName());
              } else {
                status += tra.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo()
                    + " Used by user - " + tra.getCreatedBy().getName();
              }
              if (product == null) {
                product = tra.getGoodsShipmentLine().getProduct().getName();
              } else {
                if (!product.equals(tra.getGoodsShipmentLine().getProduct().getName())) {
                  product = product + " ," + tra.getGoodsShipmentLine().getProduct().getName();
                }
              }
            }

          }
          tran.setErrorreason(status);

        }
      }
      if (errorFlag) {
        errorMsg = OBMessageUtils.messageBD("ESCM_ProcessFailed(Tag Status)").replace("%", product);
        OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
        bundle.setResult(result);
        return;
      }
      // checking for beneficiary
      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
          if (tran.getEscmMrequestCustody().getBeneficiaryType() != null
              && !tran.getEscmMrequestCustody().getBeneficiaryType().equals("MA")) {
            if (tran.getEscmMrequestCustody().getBeneficiaryIDName() != null
                || tran.getEscmMrequestCustody().getBeneficiaryType() != null)
              if (!tran.getEscmMrequestCustody().getBeneficiaryIDName()
                  .equals(line.getShipmentReceipt().getEscmBname())
                  || !tran.getEscmMrequestCustody().getBeneficiaryType()
                      .equals(line.getShipmentReceipt().getEscmBtype())) {
                errorFlag = true;
                status = OBMessageUtils.messageBD("ESCM_Notbelong");
                status = status.replace("%", inout.getEscmBname().getCommercialName());
                tran.setErrorreason(status);
              }
          } else if (tran.getEscmMrequestCustody().getBeneficiaryType() == null
              && tran.getEscmMrequestCustody().getBeneficiaryIDName() == null) {
            errorFlag = true;
            status = OBMessageUtils.messageBD("ESCM_Notbelong");
            status = status.replace("%", inout.getEscmBname().getCommercialName());
            tran.setErrorreason(status);
          }
        }
      }
      if (errorFlag) {
        errorMsg = OBMessageUtils.messageBD("ESCM_Not_Beneficiary").replace("@",
            inout.getEscmBname().getCommercialName());
        OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
        bundle.setResult(result);
        return;

      }
      // check role is present in document rule or not
      if (DocStatus.equals("DR")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, Resource.Return_Transaction, BigDecimal.ZERO);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
      }
      // chk submitting role is in first role in document rule
      if (DocStatus.equals("DR")) {
        chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(
            OBDal.getInstance().getConnection(), clientId, orgId, userId, roleId,
            Resource.Return_Transaction, BigDecimal.ZERO);
        if (!chkSubRolIsInFstRolofDR) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotInFirstRole@");
          bundle.setResult(result);
          return;
        }
      }
      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }
      /*
       * // Try to approve already completed record if (inout.getEscmReceivingtype().equals("INR"))
       * { for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) { for
       * (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) { if
       * (tran.getEscmMrequestCustody().getAlertStatus().equals("RET")) { errorFlag = true; break; }
       * } } } if (errorFlag) { OBDal.getInstance().rollbackAndClose(); OBError result =
       * OBErrorBuilder .buildMessage(null, "error", "@ESCM_IssRet_TagAlrProcessed@");
       * bundle.setResult(result); return; }
       */
      if ((!vars.getUser().equals(inout.getCreatedBy().getId())) && (DocStatus.equals("DR"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }

      if (!errorFlag) {
        if (DocStatus.equals("DR") && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, inout, appstatus,
            comments, currentDate, vars, nextApproval, Lang);
        boolean sequenceexists = false;
        if (count == 2) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", failureMsg);
          bundle.setResult(result);
          return;
        } else if (count == -3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsgs);
          bundle.setResult(result);
          return;
        }

        else if (count == 1) {
          if (inout.getEscmSpecno() == null) {
            String sequence = Utility
                .getProcessSpecificationSequence(inout.getOrganization().getId(), "INR");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              sequenceexists = Utility.chkSpecificationSequence(inout.getOrganization().getId(),
                  "INR", sequence);
              if (!sequenceexists) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_Duplicate_SpecNo@");
                bundle.setResult(result);
                return;
              }
              inout.setEscmSpecno(sequence);
            }
          }
          for (ShipmentInOutLine inoutline : inout.getMaterialMgmtShipmentInOutLineList()) {
            if (inout.getEscmReceivingtype().equals("INR")) {
              trans = OBProvider.getInstance().get(MaterialTransaction.class);
              trans.setOrganization(inoutline.getOrganization());
              trans.setClient(inoutline.getClient());
              trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setCreationDate(new java.util.Date());
              trans.setUpdated(new java.util.Date());
              if (inout.getEscmReceivingtype().equals("INR"))
                trans.setMovementType("V+");
              else if (inout.getEscmReceivingtype().equals("IRT"))
                trans.setMovementType("V-");

              OBQuery<Locator> locator = OBDal.getInstance().createQuery(Locator.class,
                  " as e where e.warehouse.id=:warehouseID and e.default='Y' ");
              locator.setNamedParameter("warehouseID", inout.getWarehouse().getId());
              locator.setMaxResult(1);
              if (locator.list().size() > 0) {
                trans.setStorageBin(locator.list().get(0));
                // log4j.debug("getStorageBin:" + trans.getStorageBin());
              } else {
                errorFlag = true;
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_Locator(Empty)@");
                bundle.setResult(result);
                return;
              }

              trans.setProduct(inoutline.getProduct());
              trans.setMovementDate(inout.getMovementDate());
              if (inout.getEscmReceivingtype().equals("INR")) {
                trans.setMovementQuantity(inoutline.getMovementQuantity());
                trans.setEscmTransactiontype("INR");
              }
              trans.setGoodsShipmentLine(inoutline);
              trans.setUOM(OBDal.getInstance().get(UOM.class, inoutline.getUOM().getId()));

              OBDal.getInstance().save(trans);
            }
            // update custody trnasaction and custody detail based on inoutline

            for (Escm_custody_transaction objCustodytran : inoutline
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              if (inout.getEscmReceivingtype().equals("INR")) {
                objCustody.setAlertStatus("RET");
                objCustody.setBeneficiaryType(null);
                objCustody.setBeneficiaryIDName(null);
              } else {
                objCustody.setBeneficiaryType(inout.getEscmBtype());
                objCustody.setBeneficiaryIDName(inout.getEscmBname());
              }
              OBDal.getInstance().save(objCustody);
              // update custody transaction status
              if (inout.getEscmReceivingtype().equals("INR"))
                objCustodytran.setTransactiontype("RE");

              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + "  escm_custody_transaction_id not in ( ? ) and escm_mrequest_custody_id = ? "
                  + " and isprocessed = 'Y' order by created desc limit 1";
              st = conn.prepareStatement(query);
              st.setString(1, objCustodytran.getId());
              st.setString(2, objCustody.getId());
              rs = st.executeQuery();
              if (rs.next()) {
                Escm_custody_transaction updCustodytran = OBDal.getInstance().get(
                    Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
                updCustodytran.setReturnDate(inout.getMovementDate());
                OBDal.getInstance().save(updCustodytran);
              }
              objCustodytran.setProcessed(true);
              objCustodytran.setDocumentNo(inout.getEscmSpecno());
              OBDal.getInstance().save(objCustodytran);
            }
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else if (count == 3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {
      log.debug("Exception in Return Transaction Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }

  }

  private int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, ShipmentInOut objRequest, String appstatus, String comments, Date currentDate,
      VariablesSecureApp vars, NextRoleByRuleVO paramNextApproval, String Lang) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.ReturnTransaction;
    String strRoleId = "";
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objRequest.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramNextApproval;
    boolean isBackwardDelegation = false;
    HashMap<String, String> role = null;
    String qu_next_role_id = "";
    String delegatedFromRole = null;
    String delegatedToRole = null;
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    List<EutDocappDelegateln> delegationlnList = new ArrayList<EutDocappDelegateln>();
    boolean isDummyRole = false;
    String fromUser = userId;
    String fromRole = roleId;
    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      EutNextRole nextRole = null;
      isDirectApproval = isDirectApproval(objRequest.getId(), currentRoleId);

      strRoleId = objRequest.getEscmAdRole().getId();

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      // ------
      // Forward
      if ((objRequest.getEutNextRole() != null)) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objRequest.getEutNextRole(), fromUser, fromRole, clientId, orgId,
            Resource.Return_Transaction, isDummyRole, isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }
      } else {
        fromUser = userId;
        fromRole = roleId;
      }

      // ------

      if ((objRequest.getEutNextRole() == null)) {
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
            Resource.Return_Transaction, strRoleId, fromUser, true, objRequest.getEscmDocstatus());

      } else {
        if (isDirectApproval) {
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              Resource.Return_Transaction, strRoleId, fromUser, true,
              objRequest.getEscmDocstatus());

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id=:roleID");
                userRole.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(), Resource.Return_Transaction,
                    "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, Resource.Return_Transaction, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, Resource.Return_Transaction, 0.00);
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, Resource.Return_Transaction, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getRequesterDelegatedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                delegatedToRole, fromUser, Resource.Return_Transaction,
                objRequest.getEscmAdRole().getId());
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        count = 3;
      } else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOlineMngr_requester")) {
        failureMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        failureMsg = failureMsg.replaceAll("@", nextApproval.getUserName());
        count = -2;

      } else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -3;
      }

      else if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEutNextRole(), Resource.Return_Transaction);

        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setEscmDocaction("AP");
        objRequest.setEscmDocstatus("ESCM_IP");
        objRequest.setEutNextRole(nextRole);

        // get alert recipients - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          /*
           * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
           * "as e where e.referenceSearchKey='" + objRequest.getId() +
           * "' and e.alertStatus='NEW'"); if (alertQuery.list().size() > 0) { for (Alert objAlert :
           * alertQuery.list()) { objAlert.setAlertStatus("SOLVED"); } }
           */

          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.Returntrans.wfa",
              Lang) + " " + objCreater.getName();
          forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE, objRequest.getDocumentNo(), Lang, vars.getRole(),
              objRequest.getEutForward(), Resource.Return_Transaction, alertReceiversMap);
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                objRequest.getClient().getId(), Description, "NEW", alertWindow,
                "scm.Returntrans.wfa", Constants.GENERIC_TEMPLATE);

            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID "
                    + " and hd.fromDate <=:currentdate and hd.date >=:currentdate "
                    + " and e.documentType='EUT_113'");
            delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("currentdate", currentDate);
            delegationlnList = delegationln.list();
            if (delegationlnList.size() > 0) {
              AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
                  delegationlnList.get(0).getRole().getId(),
                  delegationlnList.get(0).getUserContact().getId(), objRequest.getClient().getId(),
                  Description, "NEW", alertWindow, "scm.Returntrans.wfa",
                  Constants.GENERIC_TEMPLATE);
              log.debug("del role>" + delegationlnList.get(0).getRole().getId());
              includeRecipient.add(delegationlnList.get(0).getRole().getId());
              if (pendingapproval != null)
                pendingapproval += "/" + delegationlnList.get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationlnList.get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
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
        objRequest.setEscmDocaction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
      } else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEutNextRole(), Resource.Return_Transaction);

        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (objRequest.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = objRequest.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        // delete alert for approval alerts
        /*
         * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
         * "as e where e.referenceSearchKey='" + objRequest.getId() + "' and e.alertStatus='NEW'");
         * if (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
         * objAlert.setAlertStatus("SOLVED"); } }
         */

        // get alert recipients - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);
        // check and insert recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, objRequest.getDocumentNo(), Lang, vars.getRole(),
            objRequest.getEutForward(), Resource.Return_Transaction, alertReceiversMap);
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource
            .getProperty("scm.Returntrans.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objRequest.getId(), objRequest.getDocumentNo(),
            objRequest.getEscmAdRole().getId(), objRequest.getCreatedBy().getId(),
            objRequest.getClient().getId(), Description, "NEW", alertWindow,
            "scm.Returntrans.approved", Constants.GENERIC_TEMPLATE);
        objRequest.setEscmDocaction("PD");
        objRequest.setEscmDocstatus("CO");
        objRequest.setEutNextRole(null);
        count = 1;

      }
      OBDal.getInstance().save(objRequest);
      requistionId = objRequest.getId();
      if (!StringUtils.isEmpty(requistionId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", requistionId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.Return_Transaction_History);
        historyData.put("HeaderColumn", ApprovalTables.Return_Transaction_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Return_Transaction_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.Return_Transaction);

      // Forward and RMI Changes
      // after approved by forwarded user
      if (objRequest.getEutForward() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeRoleAccess(clientId, objRequest.getEUTForwardReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEutForward());
        // set forward_rmi id as null in record
        objRequest.setEutForward(null);
      }

      // removing rmi
      if (objRequest.getEutReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeReqMoreInfoRoleAccess(clientId, objRequest.getEUTReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEutReqmoreinfo());
        // set forward_rmi id as null in record
        objRequest.setEutReqmoreinfo(null);
        objRequest.setEscmReqMoreInfo("N");
      }
      // Forward and RMI Changes end

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in RT: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  @SuppressWarnings("unused")
  private boolean isDirectApproval(String RequestId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(req.m_inout_id) from m_inout req join eut_next_role rl on "
          + "req.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.m_inout_id = ? and li.ad_role_id =?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, RequestId);
        ps.setString(2, roleId);

        rs = ps.executeQuery();

        if (rs.next()) {
          if (rs.getInt("count") > 0)
            return true;
          else
            return false;
        } else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {

      }
    }
  }
}