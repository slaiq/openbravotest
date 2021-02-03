package sa.elm.ob.finance.ad_process.RDVProcess.DAO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
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
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Poongodi on 18/01/2018
 *
 */
public class RDVSubmitProcessDAO {

  private final static Logger log = LoggerFactory.getLogger(RDVSubmitProcessDAO.class);

  /**
   * ?
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param rdvtransaction
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramnextApproval
   * @param Lang
   * @param bundle
   * @return
   */
  @SuppressWarnings("unused")
  public static JSONObject updateHeaderStatus(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinRDVTransaction rdvtransaction, String appstatus,
      String comments, Date currentDate, VariablesSecureApp vars,
      NextRoleByRuleVO paramnextApproval, String Lang, ProcessBundle bundle) {
    String strRdvTrxId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    Boolean IsAdvance = false;
    String alertRuleId = "", alertWindow = AlertWindow.RDVTransaction;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = rdvtransaction.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramnextApproval;
    String errorMsgs = null;
    JSONObject result = new JSONObject();
    String fromUser = userId;
    String fromRole = roleId;
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    boolean isDummyRole = false;
    String description = "";
    String windowId = Constants.PO_Hold_Plan_Details_W;
    String docType = null;
    BigDecimal amount = BigDecimal.ZERO;
    String agencyHqOrg = null;
    boolean presentBranchOrg = false;
    List<EfinBudgetControlParam> controlList = new ArrayList<EfinBudgetControlParam>();

    try {
      OBContext.setAdminMode();

      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      BigDecimal qtyCount1 = BigDecimal.ZERO;
      BigDecimal qtyCount2 = BigDecimal.ZERO;
      isDirectApproval = isDirectApproval(rdvtransaction.getId(), roleId);
      String submittedRoleId = null;
      Order latestOrderVer = null;
      if (rdvtransaction.getEfinRdv().getSalesOrder() != null) {
        latestOrderVer = PurchaseInvoiceSubmitUtils
            .getLatestOrderComplete(rdvtransaction.getEfinRdv().getSalesOrder());

      }

      boolean isContractCategoryRole = false;

      // get alert window based on advance
      if (rdvtransaction.getEfinRDVTxnlineList().size() == 1) {
        EfinRDVTxnline txnLine = rdvtransaction.getEfinRDVTxnlineList().get(0);
        IsAdvance = txnLine.isAdvance();
        if (IsAdvance) {
          alertWindow = AlertWindow.RDVAdvance;
        } else {
          alertWindow = AlertWindow.RDVTransaction;
        }
      }
      if (rdvtransaction.isLastversion()) {
        docType = Resource.RDV_LAST_VERSION;
        amount = latestOrderVer != null ? latestOrderVer.getGrandTotalAmount() : BigDecimal.ZERO;
        if (rdvtransaction.isContractcategoryRolePassed()) {
          isContractCategoryRole = true;
        } else {
          isContractCategoryRole = UtilityDAO.getContractCategoryRole(Resource.RDV_LAST_VERSION,
              fromRole, orgId, rdvtransaction.getId(), amount);
        }
      } else if (!rdvtransaction.isLastversion()) {
        docType = Resource.RDV_Transaction;
        amount = BigDecimal.ZERO;
      }

      // setting json object value initially
      result.put("count", 0);
      result.put("errormsg", "null");

      // set the submitter user roleid
      if (rdvtransaction.getNextRole() == null) {
        submittedRoleId = OBContext.getOBContext().getRole().getId();
        rdvtransaction.setSubmitroleid(submittedRoleId);
      }
      // check the hqorg
      // OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance()
      // .createQuery(EfinBudgetControlParam.class, "as e where e.client.id =:clientId");
      // controlParam.setNamedParameter("clientId", clientId);
      // controlList = controlParam.list();
      // if (controlParam != null && controlList.size() > 0) {
      // agencyHqOrg = controlList.get(0).getAgencyHqOrg().getId();
      // }
      // if (!agencyHqOrg.equals(orgId)) {
      // if (submittedRoleId != null) {
      // Role role_access = OBDal.getInstance().get(Role.class, submittedRoleId);
      // if (role_access != null && role_access.getEutReg() != null) {
      // String branchOrg = role_access.getEutReg().getId();
      // if (branchOrg != null) {
      // orgId = branchOrg;
      // }
      //
      // }
      // }
      // }

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");

      /**
       * fetching from roleId and userId based on delegated user / forwarder/ direct approver
       **/
      if (rdvtransaction.getNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            rdvtransaction.getNextRole(), userId, roleId, clientId, orgId, docType, isDummyRole,
            isDirectApproval);
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

      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // first time submit the record
      if ((rdvtransaction.getNextRole() == null)) {
        if (rdvtransaction.isLastversion()) {
          nextApproval = NextRoleByRule.getLineManagerBasedNextRoleRDVLastVersion(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser, docType,
              amount, fromUser, false, rdvtransaction.getAppstatus(), isContractCategoryRole,
              latestOrderVer.getEscmContactType().getId());
        } else {
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser, docType,
              amount, fromUser, false, rdvtransaction.getAppstatus());
        }

      }
      // after submit- Next level approval
      else {
        // checking direct approver or delegated approver
        // if isDirectApproval flag is "true" then approver is direct approver
        if (isDirectApproval) {
          // getting next level approver

          if (rdvtransaction.isLastversion()) {
            nextApproval = NextRoleByRule.getLineManagerBasedNextRoleRDVLastVersion(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser, docType,
                amount, fromUser, false, rdvtransaction.getAppstatus(), isContractCategoryRole,
                latestOrderVer.getEscmContactType().getId());
          } else {
            nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser, docType,
                amount, fromUser, false, rdvtransaction.getAppstatus());
          }

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");

                // checking next level approver is delegated with backward role in Document Rule.
                // Ex- ((document rule is a,b,c,d) b is approving the record. c is next level
                // approver, but c is delegated with b then if b is approving it will wait for d
                // approve only. it will skip the c)
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(), docType, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                // check backward delegation
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, docType, amount);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, docType, amount);
          }
        }
        // if approver is delegated user
        else {

          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, docType, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, delegatedToRole, fromUser, docType, amount);
        }
      }

      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && (nextApproval.getErrorMsg().contains("NoManagerAssociatedWithRole")
              || nextApproval.getErrorMsg().equals("Managernotdefined"))) {
        errorMsgs = OBMessageUtils.messageBD("NoManagerAssociatedWithRole");
        count = -2;

        result.put("count", count);
        result.put("errormsg", errorMsgs);

      }
      // if Role doesnt has any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -3;

        result.put("count", count);
        result.put("errormsg", errorMsgs);

      }

      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_Nocontractcategoryaccess")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg()).replace("@",
            latestOrderVer.getEscmContactType().getCommercialName());
        count = -5;

        result.put("count", count);
        result.put("errormsg", errorMsgs);

      } // if no error and having next level approver then update the status as inprogress
      else if (nextApproval != null && nextApproval.hasApproval()) {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(rdvtransaction.getNextRole(), Resource.RDV_Transaction);

        // update the rdv transaction update status
        rdvtransaction.setUpdated(new java.util.Date());
        rdvtransaction.setUpdatedBy(OBContext.getOBContext().getUser());

        if ((rdvtransaction.getAppstatus().equals("REJ")
            || rdvtransaction.getAppstatus().equals("DR"))
            && rdvtransaction.getAction().equals("CO")) {
          rdvtransaction.setRevoke(true);
        } else
          rdvtransaction.setRevoke(false);
        rdvtransaction.setAction("AP");
        rdvtransaction.setAppstatus("WFA");
        rdvtransaction.setNextRole(nextRole);

        if (isContractCategoryRole && !rdvtransaction.isContractcategoryRolePassed()) {
          rdvtransaction.setContractcategoryRolePassed(true);
        }

        if (rdvtransaction.isAdvancetransaction()
            && rdvtransaction.getEfinRDVTxnlineList().size() > 0) {
          EfinRDVTxnline txnLineObj = rdvtransaction.getEfinRDVTxnlineList().get(0);
          txnLineObj.setAction("AP");
          txnLineObj.setApprovalStatus("WFA");
          OBDal.getInstance().save(txnLineObj);
        }

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRukeID");
        receipientQuery.setNamedParameter("alertRukeID", alertRuleId);
        List<AlertRecipient> receipientQueryList = receipientQuery.list();
        forwardDao.getAlertForForwardedUser(rdvtransaction.getId(), alertWindow, alertRuleId,
            objUser, clientId, Constants.APPROVE,
            rdvtransaction.getTXNVersion()
                + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
                    : "-" + rdvtransaction.getCertificateNo()),
            Lang, vars.getRole(), rdvtransaction.getEUTForwardReqmoreinfo(),
            Resource.RDV_Transaction, alertReceiversMap);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // // delete alert for approval alerts
          // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          // "as e where e.referenceSearchKey='" + rdvtransaction.getId()
          // + "' and e.alertStatus='NEW'");
          // if (alertQuery.list().size() > 0) {
          // for (Alert objAlert : alertQuery.list()) {
          // objAlert.setAlertStatus("SOLVED");
          // }
          // }
          // define waiting for approval description
          String Description = sa.elm.ob.finance.properties.Resource.getProperty("finance.rdv.wfa",
              Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(rdvtransaction.getId(),
                rdvtransaction.getTXNVersion()
                    + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
                        : "-" + rdvtransaction.getCertificateNo()),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                rdvtransaction.getClient().getId(), Description, "NEW", alertWindow,
                "finance.rdv.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance()
                .createQuery(EutDocappDelegateln.class, " as e left join e.eUTDocappDelegate as hd "
                    + " where hd.role.id = :roleID and hd.fromDate <= :fromDate and hd.date >= :currentDate "
                    + " and e.documentType='" + docType + "'");
            log.debug("delegationln:" + delegationln.getWhereAndOrderBy());
            delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("fromDate", currentDate);
            delegationln.setNamedParameter("currentDate", currentDate);
            List<EutDocappDelegateln> delegationlnList = delegationln.list();
            if (delegationln != null && delegationlnList.size() > 0) {
              AlertUtility.alertInsertionRole(rdvtransaction.getId(),
                  rdvtransaction.getTXNVersion()
                      + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
                          : "-" + rdvtransaction.getCertificateNo()),
                  delegationlnList.get(0).getRole().getId(),
                  delegationlnList.get(0).getUserContact().getId(),
                  rdvtransaction.getClient().getId(), Description, "NEW", alertWindow,
                  "finance.rdv.wfa", Constants.GENERIC_TEMPLATE);
              includeRecipient.add(delegationln.list().get(0).getRole().getId());
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationln.list().get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (receipientQueryList.size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQueryList) {
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
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
        result.put("count", count);
        result.put("errormsg", "null");
      }
      // final approver
      else {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        // update the header status
        rdvtransaction.setUpdated(new java.util.Date());
        rdvtransaction.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (rdvtransaction.getRole() != null) {
          objCreatedRole = rdvtransaction.getRole();
        }

        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(rdvtransaction.getNextRole(), Resource.RDV_Transaction);

        forwardDao.getAlertForForwardedUser(rdvtransaction.getId(), alertWindow, alertRuleId,
            objUser, clientId, Constants.APPROVE,
            rdvtransaction.getTXNVersion()
                + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
                    : "-" + rdvtransaction.getCertificateNo()),
            Lang, vars.getRole(), rdvtransaction.getEUTForwardReqmoreinfo(),
            Resource.RDV_Transaction, alertReceiversMap);

        // // delete alert for approval alerts
        // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
        // "as e where e.referenceSearchKey='" + rdvtransaction.getId()
        // + "' and e.alertStatus='NEW'");
        // if (alertQuery.list().size() > 0) {
        // for (Alert objAlert : alertQuery.list()) {
        // objAlert.setAlertStatus("SOLVED");
        // }
        // }
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.rdv.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(rdvtransaction.getId(),
            rdvtransaction.getTXNVersion()
                + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
                    : "-" + rdvtransaction.getCertificateNo()),
            "", rdvtransaction.getCreatedBy().getId(), rdvtransaction.getClient().getId(),
            Description, "NEW", alertWindow, "finance.rdv.approved", Constants.GENERIC_TEMPLATE);
        rdvtransaction.setAction("RE");
        rdvtransaction.setAppstatus("APP");
        rdvtransaction.setNextRole(null);

        if (isContractCategoryRole && !rdvtransaction.isContractcategoryRolePassed()) {
          rdvtransaction.setContractcategoryRolePassed(true);
        }

        count = 1;
        result.put("count", count);
        result.put("errormsg", "null");

        if (rdvtransaction.isAdvancetransaction()
            && rdvtransaction.getEfinRDVTxnlineList().size() > 0) {
          EfinRDVTxnline txnLineObj = rdvtransaction.getEfinRDVTxnlineList().get(0);
          txnLineObj.setAction("RE");
          txnLineObj.setApprovalStatus("APP");
          OBDal.getInstance().save(txnLineObj);
        }

        // set alert for budget user taskno- 7543
        // if (!rdvtransaction.getEfinRdv().getTXNType().equals("POD")
        // && rdvtransaction.getHoldamount().compareTo(BigDecimal.ZERO) > 0) {
        // description = OBMessageUtils.messageBD("EFIN_RDV_HOLD_ALERT")
        // .replace("%", rdvtransaction.getEfinRdv().getDocumentNo())
        // .replace("#", rdvtransaction.getHoldamount().toString())
        // .replace("$", rdvtransaction.getEfinRdv().getSalesOrder().getDocumentNo());
        // AlertUtility.alertInsertionPreferenceBudUser(rdvtransaction.getId(),
        // rdvtransaction.getEfinRdv().getDocumentNo(), "EFIN_BUDGET_USER",
        // rdvtransaction.getClient().getId(), description, "NEW", AlertWindow.RDVHOLDALERT,
        // "finance.hold.amount", Constants.GENERIC_TEMPLATE, windowId, null);
        // }

        // Check Alert for Budget User and change Status 'Solved' when txn version hold amount is
        // completely released
        // List<String> txnIdList = new ArrayList<String>();
        // List<Integer> released = new ArrayList<Integer>();
        // List<EfinRDVTransaction> rdvTransactionList = rdvtransaction.getEfinRdv()
        // .getEfinRDVTxnList().stream()
        // .filter(x -> (x.getHoldamount().compareTo(BigDecimal.ZERO) != 0
        // && !x.getId().equals(rdvtransaction.getId())))
        // .collect(Collectors.toList());
        // if (rdvTransactionList.size() > 0) {
        // rdvTransactionList.forEach(x -> {
        // x.getEfinRDVTxnlineList().forEach(y -> {
        //
        // List<EfinRdvHoldAction> holdRelease = y.getEfinRdvHoldActionList().stream()
        // .filter(z -> (z.getRDVHoldRel() == null)).collect(Collectors.toList());
        //
        // List<EfinRdvHoldAction> isRelease = holdRelease.stream()
        // .filter(z -> (z.isReleased() && z.getRDVHoldRel() == null))
        // .collect(Collectors.toList());
        //
        // if (isRelease.size() == holdRelease.size()) {
        // txnIdList.add(x.getId());
        // }
        // holdRelease.clear();
        // isRelease.clear();
        //
        // });
        // });
        // }
        // if (txnIdList.size() > 0) {
        // txnIdList.forEach(x -> {
        // OBQuery<org.openbravo.model.ad.alert.Alert> alertObj = OBDal.getInstance().createQuery(
        // org.openbravo.model.ad.alert.Alert.class,
        // " as e where e.referenceSearchKey =:referenceSearchKey and e.alertStatus = 'NEW' and
        // e.eutAlertKey = 'finance.hold.amount' ");
        // alertObj.setNamedParameter("referenceSearchKey", x);
        // List<org.openbravo.model.ad.alert.Alert> alertList = alertObj.list();
        // if (alertList.size() > 0) {
        // alertList.forEach(a -> {
        // org.openbravo.model.ad.alert.Alert alert = OBDal.getInstance()
        // .get(org.openbravo.model.ad.alert.Alert.class, a.getId());
        // alert.setAlertStatus("SOLVED");
        // });
        // }
        //
        // });
        // }

        // if netmatch amt is zero ,check totaldeduction same as match amt and check any external
        // contract penalty added , if not then dont allow to generate invoice,if there then allow
        // to
        // generate invoice only for transaction version need to check this case
        if (!rdvtransaction.isAdvancetransaction()) {
          if (rdvtransaction.getNetmatchAmt().compareTo(BigDecimal.ZERO) == 0
              && rdvtransaction.getMatchAmt().compareTo(rdvtransaction.getTOTDeduct()) == 0) {
            Boolean checkExternalPenaltyExists = GenerateAmarsarafDAO
                .checkExternalPenaltyExists(rdvtransaction);
            if (!checkExternalPenaltyExists) {
              rdvtransaction.setTxnverStatus("INV");
              rdvtransaction.setInvoice(null);
              rdvtransaction.setAmarsaraf(false);
            }
          }
        }
      }
      OBDal.getInstance().save(rdvtransaction);
      strRdvTrxId = rdvtransaction.getId();

      // insert approval history
      if (!StringUtils.isEmpty(strRdvTrxId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", strRdvTrxId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.RDV_Txn_History);
        historyData.put("HeaderColumn", ApprovalTables.RDV_Txn_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.RDV_Txn_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.RDV_Transaction);

      // after approved by forwarded user removing the forward and rmi id
      if (rdvtransaction.getEUTForwardReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(rdvtransaction.getEUTForwardReqmoreinfo());
        rdvtransaction.setEUTForwardReqmoreinfo(null);
      }
      if (rdvtransaction.getEUTReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(rdvtransaction.getEUTReqmoreinfo());
        rdvtransaction.setEUTReqmoreinfo(null);
        rdvtransaction.setRequestMoreInformation("N");
      }
      OBDal.getInstance().save(rdvtransaction);

    } /*
       * catch (Exception e) { log.error("Exception in updateHeaderStatus in Purchase Order: ", e);
       * OBDal.getInstance().rollbackAndClose(); }
       */
    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.debug("Exception in updateHeaderStatus in Purchase Order:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  public static JSONObject updateLineLevelStatus(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinRDVTxnline rdvTxnLine, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle) {
    String strRdvTrxLineId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    Boolean IsAdvance = false;
    String alertRuleId = "", alertWindow = AlertWindow.RDVAdvanceLine;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = rdvTxnLine.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramnextApproval;
    String errorMsgs = null;
    JSONObject result = new JSONObject();
    String fromUser = userId;
    String fromRole = roleId;
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    boolean isDummyRole = false;
    String description = "";

    try {
      OBContext.setAdminMode();

      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      isDirectApproval = isDirectApprovalLineLevel(rdvTxnLine.getId(), roleId);

      // setting json object value initially
      result.put("count", 0);
      result.put("errormsg", "null");

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          " as e where e.client.id=:clientId and e.efinProcesstype=:alertWindow");
      queryAlertRule.setNamedParameter("clientId", clientId);
      queryAlertRule.setNamedParameter("alertWindow", alertWindow);
      /**
       * fetching from roleId and userId based on delegated user / forwarder/ direct approver
       **/
      if (rdvTxnLine.getNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            rdvTxnLine.getNextRole(), userId, roleId, clientId, orgId, Resource.RDV_Transaction,
            isDummyRole, isDirectApproval);
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

      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // first time submit the record
      if ((rdvTxnLine.getNextRole() == null)) {
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
            Resource.RDV_Transaction, BigDecimal.ZERO, fromUser, false,
            rdvTxnLine.getApprovalStatus());

      }
      // after submit- Next level approval
      else {
        // checking direct approver or delegated approver
        // if isDirectApproval flag is "true" then approver is direct approver
        if (isDirectApproval) {
          // getting next level approver
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
              Resource.RDV_Transaction, BigDecimal.ZERO, fromUser, false,
              rdvTxnLine.getApprovalStatus());

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");

                // checking next level approver is delegated with backward role in Document Rule.
                // Ex- ((document rule is a,b,c,d) b is approving the record. c is next level
                // approver, but c is delegated with b then if b is approving it will wait for d
                // approve only. it will skip the c)
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(), Resource.RDV_Transaction, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                // check backward delegation
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, Resource.RDV_Transaction, BigDecimal.ZERO);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, Resource.RDV_Transaction, BigDecimal.ZERO);
          }
        }
        // if approver is delegated user
        else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, Resource.RDV_Transaction, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.RDV_Transaction, BigDecimal.ZERO);
        }
      }

      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("NoManagerAssociatedWithRole")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        count = -2;

        result.put("count", count);
        result.put("errormsg", errorMsgs);

      }
      // if Role doesnt has any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -3;

        result.put("count", count);
        result.put("errormsg", errorMsgs);

      }

      // if no error and having next level approver then update the status as inprogress
      else if (nextApproval != null && nextApproval.hasApproval()) {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(rdvTxnLine.getNextRole(), Resource.RDV_Transaction);

        // update the rdv transaction update status
        rdvTxnLine.setUpdated(new java.util.Date());
        rdvTxnLine.setUpdatedBy(OBContext.getOBContext().getUser());

        if ((rdvTxnLine.getApprovalStatus().equals("REJ")
            || rdvTxnLine.getApprovalStatus().equals("DR"))
            && rdvTxnLine.getAction().equals("CO")) {
          rdvTxnLine.setRevoke(true);
        } else
          rdvTxnLine.setRevoke(false);
        rdvTxnLine.setAction("AP");
        rdvTxnLine.setApprovalStatus("WFA");
        rdvTxnLine.setNextRole(nextRole);

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRukeID");
        receipientQuery.setNamedParameter("alertRukeID", alertRuleId);
        List<AlertRecipient> receipientQueryList = receipientQuery.list();
        forwardDao.getAlertForForwardedUser(rdvTxnLine.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, rdvTxnLine.getTrxlnNo().toString(), Lang, vars.getRole(),
            null, Resource.RDV_Transaction, alertReceiversMap);// rdvtransaction.getEUTForwardReqmoreinfo()

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // define waiting for approval description
          String Description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.rdvLine.wfa", Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(rdvTxnLine.getId(), rdvTxnLine.getTrxlnNo().toString(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                rdvTxnLine.getClient().getId(), Description, "NEW", alertWindow,
                "finance.rdvLine.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance()
                .createQuery(EutDocappDelegateln.class, " as e left join e.eUTDocappDelegate as hd "
                    + " where hd.role.id = :roleID and hd.fromDate <= :fromDate and hd.date >= :currentDate "
                    + " and e.documentType='EUT_124'");
            log.debug("delegationln:" + delegationln.getWhereAndOrderBy());
            delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("fromDate", currentDate);
            delegationln.setNamedParameter("currentDate", currentDate);
            List<EutDocappDelegateln> delegationlnList = delegationln.list();
            if (delegationln != null && delegationlnList.size() > 0) {
              AlertUtility.alertInsertionRole(rdvTxnLine.getId(),
                  rdvTxnLine.getTrxlnNo().toString(), delegationlnList.get(0).getRole().getId(),
                  delegationlnList.get(0).getUserContact().getId(), rdvTxnLine.getClient().getId(),
                  Description, "NEW", alertWindow, "finance.rdvLine.wfa",
                  Constants.GENERIC_TEMPLATE);
              includeRecipient.add(delegationln.list().get(0).getRole().getId());
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationln.list().get(0).getUserContact().getName());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (receipientQueryList.size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQueryList) {
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
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
        result.put("count", count);
        result.put("errormsg", "null");
      }
      // final approver
      else {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        // update the header status
        rdvTxnLine.setUpdated(new java.util.Date());
        rdvTxnLine.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (rdvTxnLine.getRole() != null) {
          objCreatedRole = rdvTxnLine.getRole();
        }

        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(rdvTxnLine.getNextRole(), Resource.RDV_Transaction);

        forwardDao.getAlertForForwardedUser(rdvTxnLine.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, rdvTxnLine.getTrxlnNo().toString(), Lang, vars.getRole(),
            null, Resource.RDV_Transaction, alertReceiversMap);// rdvtransaction.getEUTForwardReqmoreinfo()

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.rdvLine.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(rdvTxnLine.getId(), rdvTxnLine.getTrxlnNo().toString(), "",
            rdvTxnLine.getCreatedBy().getId(), rdvTxnLine.getClient().getId(), Description, "NEW",
            alertWindow, "finance.rdvLine.approved", Constants.GENERIC_TEMPLATE);
        rdvTxnLine.setAction("RE");
        rdvTxnLine.setApprovalStatus("APP");
        rdvTxnLine.setNextRole(null);
        count = 1;
        result.put("count", count);
        result.put("errormsg", "null");

      }
      OBDal.getInstance().save(rdvTxnLine);
      strRdvTrxLineId = rdvTxnLine.getId();

      // insert approval history
      if (!StringUtils.isEmpty(strRdvTrxLineId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", strRdvTrxLineId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.RDV_TxnLine_History);
        historyData.put("HeaderColumn", ApprovalTables.RDV_TxnLine_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.RDV_TxnLine_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.RDV_Transaction);
      //
      // // after approved by forwarded user removing the forward and rmi id
      // if (rdvtransaction.getEUTForwardReqmoreinfo() != null) {
      // forwardDao.setForwardStatusAsDraft(rdvtransaction.getEUTForwardReqmoreinfo());
      // rdvtransaction.setEUTForwardReqmoreinfo(null);
      // }
      // if (rdvtransaction.getEUTReqmoreinfo() != null) {
      // forwardDao.setForwardStatusAsDraft(rdvtransaction.getEUTReqmoreinfo());
      // rdvtransaction.setEUTReqmoreinfo(null);
      // rdvtransaction.setRequestMoreInformation("N");
      // }
      // OBDal.getInstance().save(rdvtransaction);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.debug("Exception in updateLineLevelStatus in Purchase Order:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * check approver is direct approver or not
   * 
   * @param rdvtrxid
   * @param roleId
   * @return
   */
  public static boolean isDirectApproval(String rdvtrxid, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(rdv.efin_rdvtxn_id) from efin_rdvtxn rdv join eut_next_role rl on "
          + "rdv.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and rdv.efin_rdvtxn_id = ? and li.ad_role_id =?";
      log.debug("query" + query.toString());
      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, rdvtrxid);
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
      log.error("Exception in RDV submit -isDirectApproval " + e.getMessage());
      return false;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static boolean isDirectApprovalLineLevel(String rdvTxnLineId, String roleId) {

    String query = null;
    List<Object> directAppList = new ArrayList<Object>();
    try {
      query = "select count(rdvline.efin_rdvtxnline_id) from efin_rdvtxnline rdvline join eut_next_role rl on "
          + "rdvline.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and rdvline.efin_rdvtxnline_id = ? and li.ad_role_id =?";
      log.debug("query" + query.toString());
      Query qry = OBDal.getInstance().getSession().createSQLQuery(query);
      qry.setParameter(0, rdvTxnLineId);
      qry.setParameter(1, roleId);
      directAppList = qry.list();
      if (directAppList.size() > 0) {
        BigInteger count = (BigInteger) directAppList.get(0);
        if (count.compareTo(BigInteger.ZERO) > 0)
          return true;
        else
          return false;
      } else
        return false;
    } catch (Exception e) {
      log.error("Exception in RDV submit -isDirectApprovalLineLevel " + e.getMessage());
      return false;
    } finally {
    }
  }

  public static JSONObject isAmtValidation(EfinRDVTransaction rdvTxn) {
    JSONObject result = new JSONObject();
    try {
      result.put("value", "1");
      BigDecimal matchAmt = rdvTxn.getEfinRDVTxnlineList().stream().map(a -> a.getMatchAmt())
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal penaltyAmt = rdvTxn.getEfinRDVTxnlineList().stream().map(a -> a.getPenaltyAmt())
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal holdAmt = rdvTxn.getEfinRDVTxnlineList().stream().map(a -> a.getHoldamt())
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal advanceDeduction = rdvTxn.getEfinRDVTxnlineList().stream()
          .map(a -> a.getADVDeduct()).reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal netMatchAmt = rdvTxn.getEfinRDVTxnlineList().stream().map(a -> a.getNetmatchAmt())
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (matchAmt.compareTo(rdvTxn.getMatchAmt()) != 0) {
        result.put("value", "0");
        result.put("msg", "@EFIN_RDVTxnMatchNotEqual@");
        return result;
      }

      if (netMatchAmt.compareTo(rdvTxn.getNetmatchAmt()) != 0) {
        result.put("value", "0");
        result.put("msg", "@EFIN_RDVTxnNetMatchNotEqual@");
        return result;
      }
      if (penaltyAmt.compareTo(rdvTxn.getPenaltyAmt()) != 0) {
        result.put("value", "0");
        result.put("msg", "@EFIN_RDVTxnPenaltyNotEqual@");
        return result;
      }
      if (holdAmt.compareTo(rdvTxn.getHoldamount()) != 0) {
        result.put("value", "0");
        result.put("msg", "@EFIN_RDVTxnHoldNotEqual@");
        return result;
      }
      if (advanceDeduction.compareTo(rdvTxn.getADVDeduct()) != 0) {
        result.put("value", "0");
        result.put("msg", "@EFIN_RDVTxnAdvDedNotEqual@");
        return result;
      }

    } catch (Exception e) {
      log.error("Exception in RDV submit -isAmtValidation " + e.getMessage());
      return result;
    } finally {
    }
    return result;
  }

  /**
   * Check qty/amt is fully matched or not
   * 
   * @param order
   * @param rdvTxn
   * @return
   */
  public static Boolean chkFullyMatchedOrNot(Order order, EfinRDVTransaction rdvTxn) {

    BigDecimal totalOrderQty = BigDecimal.ZERO;
    BigDecimal totalOrderAmt = BigDecimal.ZERO;
    BigDecimal totalMatchQty = BigDecimal.ZERO;
    BigDecimal totalMatchAmt = BigDecimal.ZERO;
    Boolean isfullyMatched = false;

    try {

      totalOrderQty = order.getOrderLineList().stream().filter(a -> !a.isEscmIssummarylevel())
          .map(a -> a.getOrderedQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add);

      totalOrderAmt = order.getGrandTotalAmount();

      totalMatchQty = rdvTxn.getEfinRdv().getEfinRDVTxnlineList().stream().filter(
          a -> a.isMatch() && !a.isAdvance() && !a.getEfinRdvtxn().getTxnverStatus().equals("DR"))
          .map(a -> a.getMatchQty()).reduce(BigDecimal.ZERO, BigDecimal::add);

      totalMatchAmt = rdvTxn.getEfinRdv().getEfinRDVTxnlineList().stream().filter(
          a -> a.isMatch() && !a.isAdvance() && !a.getEfinRdvtxn().getTxnverStatus().equals("DR"))
          .map(a -> a.getMatchAmt()).reduce(BigDecimal.ZERO, BigDecimal::add);

      if (order.getEscmReceivetype().equals(Constants.QTY_BASED)) {
        if (totalOrderQty.compareTo(totalMatchQty) == 0) {
          isfullyMatched = true;
        }
      } else if (order.getEscmReceivetype().equals(Constants.AMOUNT_BASED)) {
        if (totalOrderAmt.compareTo(totalMatchAmt) == 0) {
          isfullyMatched = true;
        }
      }
      return isfullyMatched;
    } catch (Exception e) {
      log.error("Exception in RDV submit -chkFullyMatchedOrNot " + e.getMessage());
      return isfullyMatched;
    }
  }

  /**
   * Check current version match qty/amt is greater than zero
   * 
   * @param order
   * @param rdvTxn
   * @return
   */
  public static Boolean isMatchQtyAmtZero(Order order, EfinRDVTransaction rdvTxn) {
    Boolean isMatchQtyAmtZero = false;
    BigDecimal totalMatchQty = BigDecimal.ZERO;
    BigDecimal totalMatchAmt = BigDecimal.ZERO;

    try {

      totalMatchQty = rdvTxn.getEfinRDVTxnlineList().stream()
          .filter(a -> !a.isSummaryLevel() && !a.isAdvance()).map(a -> a.getMatchQty())
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      totalMatchAmt = rdvTxn.getEfinRDVTxnlineList().stream()
          .filter(a -> !a.isSummaryLevel() && !a.isAdvance()).map(a -> a.getMatchAmt())
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (order.getEscmReceivetype().equals(Constants.QTY_BASED)) {
        if (totalMatchQty.compareTo(BigDecimal.ZERO) == 0) {
          isMatchQtyAmtZero = true;
        }
      } else if (order.getEscmReceivetype().equals(Constants.AMOUNT_BASED)) {
        if (totalMatchAmt.compareTo(BigDecimal.ZERO) == 0) {
          isMatchQtyAmtZero = true;
        }
      }
      return isMatchQtyAmtZero;
    } catch (Exception e) {
      log.error("Exception in RDV submit -isMatchQtyAmtZero " + e.getMessage());
      return isMatchQtyAmtZero;
    }
  }

  public static boolean branchOrgPresentInDocRule(String docType, String orgId, String clientId,
      String roleId, BigDecimal value) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(qdrh.eut_documentrule_header_id) "
          + "              from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on "
          + "              qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
          + "             where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and "
          + "             qdrh.document_type = ?  and qdrh.rulevalue <= '" + value
          + "' and qdrl.ad_role_id=? ";
      log.debug("query" + query.toString());
      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, clientId);
        ps.setString(2, orgId);
        ps.setString(3, docType);
        ps.setString(4, roleId);
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
      log.error("Exception in branchOrgPresentInDocRule " + e.getMessage());
      return false;
    } finally {
      // close db connection
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
