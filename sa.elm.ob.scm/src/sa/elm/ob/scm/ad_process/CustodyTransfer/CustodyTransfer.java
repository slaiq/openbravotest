package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.domain.Preference;
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
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 20/03/2017
 */

public class CustodyTransfer extends DalBaseProcess {

  /**
   * This servlet class was responsible for Custody Transfer Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(CustodyTransfer.class);
  private final OBError obError = new OBError();
  private static String errorMsg = null;

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    ResultSet rs = null;
    PreparedStatement st = null;
    boolean errorFlag = false;
    String lastproductid = "";
    Boolean isDirectApproval = false, isDelegated = false;
    Boolean allowApprove = false;
    log.debug("entering into CustodyTransfer Submit");
    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus(), errorMsge = "", NextUserId = null;
      @SuppressWarnings("unused")
      int count = 0;
      NextRoleByRuleVO nextApproval = null;
      String query = null;
      Connection con = OBDal.getInstance().getConnection();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String pendingapproval = "", appstatus = "";
      String DocAction = inout.getEscmCtdocaction();
      String alertWindowType = AlertWindow.CustodyTransfer, alertRuleId = "";
      String Lang = vars.getLanguage();
      String status = null, product = null;
      String InvCtrl_Role = "";
      String custodyId = null;
      User objUser = Utility.getObject(User.class, vars.getUser());
      JSONObject forwardJsonObj = new JSONObject();
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      User usr = OBDal.getInstance().get(User.class, vars.getUser());
      Date currentDate = new Date();
      List<EutNextRoleLine> nxtRoleList = new ArrayList<EutNextRoleLine>();
      List<EutDocappDelegateln> delegationLnList = new ArrayList<EutDocappDelegateln>();

      if (inout.getEscmCtapplevel() != 1 && inout.getEscmDocstatus().equals("DR")) {
        if (!inout.getCreatedBy().getId().equals(userId)) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

      }
      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approve the record without refreshing the page
      if (inout.getEutForward() != null) {
        allowApprove = forwardDao.allowApproveReject(inout.getEutForward(), userId, roleId,
            Resource.CUSTODY_TRANSFER);
      }
      if (inout.getEutReqmoreinfo() != null
          || ((inout.getEutForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }

      // check all custody line transaction belongs to current beneficiary
      if (inout.getMaterialMgmtShipmentInOutLineList().size() > 0) {
        for (ShipmentInOutLine objLine : inout.getMaterialMgmtShipmentInOutLineList()) {
          if (objLine.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            errorMsge = OBMessageUtils.messageBD("ESCM_ZERO_CUSTODY");
          }
          if (objLine.getEscmCustodyTransactionList().size() == 0) {
            errorFlag = true;
            errorMsge = OBMessageUtils.messageBD("ESCM_ZERO_CUSTODY");
          }
          for (Escm_custody_transaction objTransaction : objLine.getEscmCustodyTransactionList()) {
            if (objTransaction.getEscmMrequestCustody().getBeneficiaryIDName() != inout
                .getEscmBname()) {
              errorFlag = true;
              objTransaction
                  .setErrorreason("Not Belongs to " + inout.getEscmBname().getCommercialName());
              OBDal.getInstance().save(objTransaction);
              OBDal.getInstance().flush();
              errorMsge = OBMessageUtils.messageBD("ESCM_Not_Beneficiary").replace("@",
                  inout.getEscmBname().getCommercialName());
            }
          }
        }
      }
      if (inout.getEutNextRole() != null) {
        forwardJsonObj = forwardDao.getForwardFromUserFromRole(inout.getEutNextRole(), userId,
            roleId, clientId);
      }

      // check whether the current user is present among the signatures or a delegated user
      isDirectApproval = CustodyTransferDAO.isCtDirectApproval(inout, userId);
      isDelegated = CustodyTransferDAO.isCtDelegated(inout, userId, roleId, currentDate,
          DocumentTypeE.CUSTODY_TRANSFER.getDocumentTypeCode());
      if (!isDirectApproval && !isDelegated
          && (forwardJsonObj == null || (forwardJsonObj != null && forwardJsonObj.length() == 0))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      if (!errorFlag) {
        for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
          for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
            status = null;
            OBQuery<Escm_custody_transaction> transaction = OBDal.getInstance().createQuery(
                Escm_custody_transaction.class,
                " as e where e.goodsShipmentLine.id in "
                    + " ( select line.id  from MaterialMgmtShipmentInOutLine line  left join line.shipmentReceipt hd "
                    + " where (hd.escmReceivingtype='INR'  or hd.escmIscustodyTransfer='Y') "
                    + "and hd.escmDocstatus='ESCM_IP' and hd.id <>:inoutID) and e.escmMrequestCustody.id=:custodyID ");
            transaction.setNamedParameter("inoutID", inout.getId());
            transaction.setNamedParameter("custodyID", tran.getEscmMrequestCustody().getId());

            log.debug("wherecl:" + transaction.getWhereAndOrderBy());
            log.debug("size:" + transaction.list().size());
            if (transaction.list().size() > 0) {
              errorFlag = true;
              for (Escm_custody_transaction trans : transaction.list()) {
                if (status == null) {
                  status = OBMessageUtils.messageBD("ESCM_CT_TagAlreadyUsed");
                  status = status.replace("%",
                      trans.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo()
                          + " Used by user" + trans.getCreatedBy().getName());
                  if (product == null) {
                    lastproductid = trans.getGoodsShipmentLine().getProduct().getId();
                    product = trans.getGoodsShipmentLine().getProduct().getName();
                  } else {
                    if (!lastproductid.equals(trans.getGoodsShipmentLine().getProduct().getId())) {
                      lastproductid = trans.getGoodsShipmentLine().getProduct().getId();
                      product = product + "," + trans.getGoodsShipmentLine().getProduct().getName();
                    }
                  }
                } else {
                  status += trans.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo();
                }
              }

            }
            tran.setErrorreason(status);
            OBDal.getInstance().save(line);
          }
        }
        if (errorFlag) {
          status = OBMessageUtils.messageBD("ESCM_ProcessFailed(Tag Status)");
          status = status.replace("%", product);
          // status += product;
          errorMsge = status;
        }
      }

      // check preferences
      String isAdmin = "", isCustodyController = "", approvalFlow = "1";

      try {
        isAdmin = Preferences.getPreferenceValue("ESCM_CTIsAdmin", true, vars.getClient(),
            inout.getId(), vars.getUser(), vars.getRole(), "184");

      } catch (PropertyException e) {
        isAdmin = "N";
      }

      if (isAdmin == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
        bundle.setResult(result);
        return;
      }
      try {
        isCustodyController = Preferences.getPreferenceValue("ESCM_Custody_Control", true,
            vars.getClient(), inout.getId(), vars.getUser(), vars.getRole(), "184");

      } catch (PropertyException e) {
      }
      if (isCustodyController == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
        bundle.setResult(result);
        return;
      }
      if ((isAdmin != null && isAdmin.equals("Y"))
          || (isCustodyController != null && isCustodyController.equals("Y"))
              && inout.getEscmDocstatus().equals("DR")) {
        if (inout.getCreatedBy().getId().equals(inout.getEscmCtsender().getId())) {
          approvalFlow = "2";
        } else {
          approvalFlow = "1";
        }

      } else {
        approvalFlow = "2";
      }
      if (!inout.getEscmDocstatus().equals("DR")) {
        approvalFlow = "1";
      }
      // approvalFlow = 1 start from sender
      // approvalFlow = 2 start from sender line manager

      // get Current ApproverUser Id for a record &&
      // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
      if (inout.getEutNextRole() != null && inout.getEscmCtapplevel() != 4) { // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
        OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
            " as line where line.eUTNextRole.id=:roleID "
                + " and line.eUTForwardReqmoreinfo is null and line.eUTReqmoreinfo is null ");
        line.setNamedParameter("roleID", inout.getEutNextRole().getId());
        nxtRoleList = line.list();
        if (nxtRoleList.size() > 0) {
          NextUserId = nxtRoleList.get(0).getUserContact().getId();
        }
      }
      if (inout.getEscmCtapplevel() != 4) { // !vars.getUser().equals(inout.getEscmCtreclinemng().getId())
        // checking next user for approve the record
        nextApproval = NextRoleByRule.getCustTranNextRole(con, clientId, orgId, roleId, userId,
            Resource.CUSTODY_TRANSFER, inout, NextUserId, approvalFlow, inout.getEscmCtapplevel());
      }

      // if Role doesnot have any user associated then this condition will execute and return error
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NORole_ForUser")) {
        errorMsg = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsg = errorMsg.replace("@", nextApproval.getUserName());
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
        bundle.setResult(result);
        return;
      }
      if (!errorFlag) {
        boolean sequenceexists = false;
        // final approval process
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(inout.getEutNextRole(), Resource.CUSTODY_TRANSFER);

        if (DocAction.equals("AP") && inout.getEscmCtapplevel() == 4) { // vars.getUser().equals(inout.getEscmCtreclinemng().getId())
          if (inout.getEscmSpecno() == null) {
            String sequence = Utility.getSpecificationSequence(inout.getOrganization().getId(),
                "CT");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              sequenceexists = Utility.chkSpecificationSequence(inout.getOrganization().getId(),
                  "CT", sequence);
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
            // update custody trnasaction and custody detail based on inoutline

            for (Escm_custody_transaction objCustodytran : inoutline
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              objCustody.setAlertStatus("IU");
              objCustody.setBeneficiaryType(inout.getEscmTobeneficiary());
              objCustody.setBeneficiaryIDName(inout.getEscmTobenefiName());
              OBDal.getInstance().save(objCustody);
              objCustodytran.setTransactiontype("TR");
              objCustodytran.setTransactionDate(inout.getMovementDate());
              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + " escm_custody_transaction_id not in ( ? ) and escm_mrequest_custody_id = ? "
                  + " and isprocessed='Y' order by created desc limit 1";
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
              objCustodytran.setBname(inout.getEscmTobenefiName());
              objCustodytran.setBtype(inout.getEscmTobeneficiary());
              objCustody.setBeneficiaryIDName(inout.getEscmTobenefiName());
              objCustody.setBeneficiaryType(inout.getEscmTobeneficiary());
              objCustodytran.setDocumentNo(inout.getEscmSpecno());
              OBDal.getInstance().save(objCustodytran);
            }
          }
          inout.setUpdated(new java.util.Date());
          inout.setUpdatedBy(OBContext.getOBContext().getUser());
          inout.setEscmDocstatus("CO");
          inout.setDocumentStatus("CO");
          inout.setDocumentAction("--");
          inout.setEscmCtdocaction("PD");
          inout.setEutNextRole(null);
          OBDal.getInstance().save(inout);
          count = 1;
          OBDal.getInstance().flush();
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.CUSTODY_TRANSFER);
          appstatus = "AP";

          // alert process
          ArrayList<CustTransferAlertVO> includereceipient = new ArrayList<CustTransferAlertVO>();
          CustTransferAlertVO vo = null;

          // solve approval alerts - Task No:7618
          AlertUtility.solveAlerts(inout.getId());

          // get alert rule id - Task No:7618
          alertRuleId = AlertUtility.getAlertRule(clientId, alertWindowType);

          // getting alert if forward user reject or approve
          forwardDao.getAlertForForwardedUser(inout.getId(), alertWindowType, alertRuleId, objUser,
              clientId, Constants.APPROVE, inout.getDocumentNo(), Lang, vars.getRole(),
              inout.getEutForward(), Resource.CUSTODY_TRANSFER, alertReceiversMap);

          // get alert recipients - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

          if (alrtRecList.size() > 0) {
            for (AlertRecipient rec : alrtRecList) {
              log.debug("Inside alert");
              if (rec.getUserContact() != null) {
                OBCriteria<UserRoles> userRolesCriteria = OBDal.getInstance()
                    .createCriteria(UserRoles.class);
                userRolesCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, rec.getRole()));
                userRolesCriteria.add(
                    Restrictions.eq(AlertRecipient.PROPERTY_USERCONTACT, rec.getUserContact()));

                if (userRolesCriteria.list() != null && userRolesCriteria.list().size() > 0) {
                  vo = new CustTransferAlertVO(rec.getRole().getId(), rec.getUserContact().getId());
                } else {
                  vo = new CustTransferAlertVO(rec.getRole().getId(), "0");
                }

              } else {
                vo = new CustTransferAlertVO(rec.getRole().getId(), "0");
              }
              includereceipient.add(vo);
              OBDal.getInstance().remove(rec);
            }
          }
          Role objCreatedRole = null;
          if (inout.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = inout.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          if (objCreatedRole != null) {
            vo = new CustTransferAlertVO(objCreatedRole.getId(), inout.getCreatedBy().getId());
            includereceipient.add(vo);
          }

          // final approved need to send alert to inventory control
          OBQuery<Preference> invRole = OBDal.getInstance().createQuery(Preference.class,
              "as e where e.property='ESCM_Inventory_Control' and e.searchKey='Y' and e.client.id=:clientID ");
          invRole.setNamedParameter("clientID", clientId);
          if (invRole.list().size() > 0) {
            InvCtrl_Role = invRole.list().get(0).getVisibleAtRole().getId();
            vo = new CustTransferAlertVO(invRole.list().get(0).getVisibleAtRole().getId(), "0");
            includereceipient.add(vo);
          }

          // avoid duplicate recipient
          Set<CustTransferAlertVO> s = new HashSet<CustTransferAlertVO>();
          s.addAll(includereceipient);
          includereceipient = new ArrayList<CustTransferAlertVO>();
          includereceipient.addAll(s);

          // insert alert receipients
          for (CustTransferAlertVO vo1 : includereceipient) {
            if (vo1.getUserId().equals("0")) {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, clientId, alertWindowType);
            }

            else {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                  alertWindowType);
            }
          }
          // set alert for requester
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.approved",
              Lang) + " " + usr.getName();
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(),
              inout.getEscmAdRole().getId(), inout.getCreatedBy().getId(),
              inout.getClient().getId(), Description, "NEW", alertWindowType, "scm.ct.approved",
              Constants.GENERIC_TEMPLATE);
          // set alert for inventory control
          if (StringUtils.isNotEmpty(InvCtrl_Role)) {
            AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), InvCtrl_Role, "",
                inout.getClient().getId(), Description, "NEW", alertWindowType, "scm.ct.approved",
                Constants.GENERIC_TEMPLATE);
          }
        }

        // other than approval process
        else {

          // update header status as inprogress and assign next approval
          if (inout.getEscmDocstatus().equals("DR")
              && (!inout.getCreatedBy().getId().equals(inout.getEscmCtsender().getId()))) {
            inout.setEscmCtapplevel(inout.getEscmCtapplevel());
          } else {
            inout.setEscmCtapplevel(inout.getEscmCtapplevel() + (long) 1);
          }
          inout.setUpdated(new java.util.Date());
          inout.setUpdatedBy(OBContext.getOBContext().getUser());
          inout.setEutNextRole(
              OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId()));
          inout.setEscmDocstatus("ESCM_IP");
          inout.setEscmCtdocaction("AP");

          OBDal.getInstance().save(inout);

          if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RJD")) && DocAction.equals("CO")) {
            appstatus = "SUB";
          } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
            appstatus = "AP";
          }
          pendingapproval = nextApproval.getStatus();

          // alert process
          // getting alertruleID
          ArrayList<CustTransferAlertVO> includereceipient = new ArrayList<CustTransferAlertVO>();
          CustTransferAlertVO vo = null;
          // ArrayList<String> includereceipientsuser = new ArrayList<String>();

          // get alert rule id - Task No:7618
          alertRuleId = AlertUtility.getAlertRule(clientId, alertWindowType);

          // set the alert for next approvals
          EutNextRole nextrole = OBDal.getInstance().get(EutNextRole.class,
              nextApproval.getNextRoleId());

          if (nextrole.getEutNextRoleLineList().size() > 0) {
            // solve approval alerts - Task No:7618
            AlertUtility.solveAlerts(inout.getId());
          }
          // getting alert if forward user reject or approve
          forwardDao.getAlertForForwardedUser(inout.getId(), alertWindowType, alertRuleId, objUser,
              clientId, Constants.APPROVE, inout.getDocumentNo(), Lang, vars.getRole(),
              inout.getEutForward(), Resource.CUSTODY_TRANSFER, alertReceiversMap);

          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.wfa", Lang)
              + " " + inout.getCreatedBy().getName();

          // insert alert for next user
          for (EutNextRoleLine line : nextrole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(),
                line.getRole().getId(), line.getUserContact().getId(), clientId, Description, "NEW",
                alertWindowType, "scm.ct.wfa", Constants.GENERIC_TEMPLATE);

            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.userContact.id =:userID "
                    + " and hd.role.id=:roleID and hd.fromDate <=:currentdate and hd.date >=:currentdate "
                    + " and e.documentType=:docType ");
            delegationln.setNamedParameter("userID", line.getUserContact().getId());
            delegationln.setNamedParameter("roleID", line.getRole().getId());
            delegationln.setNamedParameter("currentdate", currentDate);
            delegationln.setNamedParameter("docType", Resource.CUSTODY_TRANSFER);
            delegationLnList = delegationln.list();
            if (delegationLnList.size() > 0) {
              vo = new CustTransferAlertVO(delegationLnList.get(0).getRole().getId(),
                  delegationLnList.get(0).getUserContact().getId());
              AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(),
                  delegationLnList.get(0).getRole().getId(),
                  delegationLnList.get(0).getUserContact().getId(), clientId, Description, "NEW",
                  alertWindowType, "scm.ct.wfa", Constants.GENERIC_TEMPLATE);
              includereceipient.add(vo);

              if (pendingapproval != null)
                pendingapproval += "/" + delegationLnList.get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationLnList.get(0).getUserContact().getName());
            }

            vo = new CustTransferAlertVO(line.getRole().getId(), line.getUserContact().getId());
            includereceipient.add(vo);
          }

          // get alert recipients - Task No:7618
          List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);
          if (alrtRecList.size() > 0) {
            for (AlertRecipient rec : alrtRecList) {
              log.debug("Inside alert");

              if (rec.getUserContact() != null) {
                OBCriteria<UserRoles> userRolesCriteria = OBDal.getInstance()
                    .createCriteria(UserRoles.class);
                userRolesCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, rec.getRole()));
                userRolesCriteria.add(
                    Restrictions.eq(AlertRecipient.PROPERTY_USERCONTACT, rec.getUserContact()));

                if (userRolesCriteria.list() != null && userRolesCriteria.list().size() > 0) {
                  vo = new CustTransferAlertVO(rec.getRole().getId(), rec.getUserContact().getId());
                } else {
                  vo = new CustTransferAlertVO(rec.getRole().getId(), "0");
                }

              } else {
                vo = new CustTransferAlertVO(rec.getRole().getId(), "0");
              }
              includereceipient.add(vo);
              OBDal.getInstance().remove(rec);
            }
          }
          // avoid duplicate receipient
          Set<CustTransferAlertVO> s = new HashSet<CustTransferAlertVO>();
          s.addAll(includereceipient);
          includereceipient = new ArrayList<CustTransferAlertVO>();
          includereceipient.addAll(s);

          log.debug("list duplicates  after:" + includereceipient.size());

          for (CustTransferAlertVO vo1 : includereceipient) {
            if (vo1.getUserId().equals("0")) {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, clientId, alertWindowType);
            }

            else {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                  alertWindowType);
            }
          }

        }

        // after approved by forwarded user
        if (inout.getEutForward() != null) {
          // Remove Role Access to Receiver
          // forwardDao.removeRoleAccess(clientId, objRequisition.getEutForward().getId(), con);
          // set status as "Draft" for forward record
          forwardDao.setForwardStatusAsDraft(inout.getEutForward());
          // set forward_rmi id as null in record
          inout.setEutForward(null);
        }

        // removing rmi
        if (inout.getEutReqmoreinfo() != null) {

          // Remove Role Access to Receiver
          // forwardDao.removeReqMoreInfoRoleAccess(clientId,
          // objRequisition.getEutReqmoreinfo().getId(),
          // con);
          // set status as "Draft" for forward record
          forwardDao.setForwardStatusAsDraft(inout.getEutReqmoreinfo());
          // set forward_rmi id as null in record
          inout.setEutReqmoreinfo(null);
          inout.setEscmReqMoreInfo("N");
        }

        OBDal.getInstance().save(inout);
        custodyId = inout.getId();
        if (!StringUtils.isEmpty(custodyId)) {

          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", custodyId);
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", pendingapproval);
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);

          Utility.InsertApprovalHistory(historyData);

        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed:" + errorMsge);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
      bundle.setResult(obError);
      OBDal.getInstance().save(inout);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in Custody Transfer Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      // Close DB Connection
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }

  }
}

class CustTransferAlertVO {

  private String roleId;
  private String userId;

  public CustTransferAlertVO(String roleId, String userId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  public boolean equals(Object obj) {
    // TODO Auto-generated method stub
    if (obj instanceof CustTransferAlertVO) {
      CustTransferAlertVO temp = (CustTransferAlertVO) obj;
      if (this.userId.equals(temp.userId) && this.roleId.equals(temp.roleId))
        return true;
    }
    return false;

  }

  public int hashCode() {
    // TODO Auto-generated method stub

    return (this.userId.hashCode() + this.roleId.hashCode());
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String toString() {
    return "userId: " + userId + "  roleId: " + roleId;
  }
}