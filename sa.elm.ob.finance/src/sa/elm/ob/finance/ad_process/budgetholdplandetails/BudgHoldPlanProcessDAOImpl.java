package sa.elm.ob.finance.ad_process.budgetholdplandetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess.RdvHoldActionDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess.RdvHoldActionDAOimpl;
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

/**
 * 
 * @author Kousalya on 27/11/2019
 *
 */
public class BudgHoldPlanProcessDAOImpl implements BudgHoldPlanProcessDAO {

  private final static Logger log = LoggerFactory.getLogger(BudgHoldPlanProcessDAOImpl.class);

  /**
   * Update Budget Hold Plan Details Status Based on next role and if final approver apply hold amt
   * on weightage concept and update the same amt in rdv trx- reduce net match amt and send alert to
   * budget user
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
   * @param budgHold
   * @return
   */
  @SuppressWarnings("unused")
  public JSONObject updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinRDVTransaction rdvtransaction, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle, EFINRdvBudgHold budgHold) {
    final String ADD = "AD";

    String strRdvBudgHoldId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    Boolean IsAdvance = false;
    String alertRuleId = "", alertWindow = AlertWindow.RDVBudgetHold;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = budgHold.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramnextApproval;
    String errorMsgs = null;
    JSONObject result = new JSONObject();
    String fromUser = userId;
    String fromRole = roleId;
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    boolean isDummyRole = false;
    String description = "";
    String windowId = Constants.Budget_HoldPlan_Details_W;
    List<EfinRDVTxnline> rdvLineList = null;
    BigDecimal totalNetMatch = BigDecimal.ZERO;
    BigDecimal weigtage = BigDecimal.ZERO;
    EfinRDVTransaction rdvTxn = budgHold.getEfinRdvtxn().getEfinRdvtxn();
    String ref = SequenceIdData.getUUID();
    Long number = 0L;
    RdvHoldActionDAO holdDao = new RdvHoldActionDAOimpl(con);

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
      isDirectApproval = isDirectApproval(budgHold.getId(), roleId);
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;

      // setting json object value initially
      result.put("count", 0);
      result.put("errormsg", "null");
      // find the submitted role org/branch details

      if (budgHold.getNextRole() != null) {
        if (budgHold.getEfinSubmittedRole() != null
            && budgHold.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = budgHold.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (budgHold.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");

      /**
       * fetching from roleId and userId based on delegated user / forwarder/ direct approver
       **/
      // if (budgHold.getNextRole() != null) {
      // fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(budgHold.getNextRole(),
      // userId, roleId, clientId, orgId, Resource.RDV_BudgHoldDtl, isDummyRole,
      // isDirectApproval);
      // if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
      // if (fromUserandRoleJson.has("fromUser"))
      // fromUser = fromUserandRoleJson.getString("fromUser");
      // if (fromUserandRoleJson.has("fromRole"))
      // fromRole = fromUserandRoleJson.getString("fromRole");
      // if (fromUserandRoleJson.has("isDirectApproval"))
      // isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
      // }
      // } else {
      // fromUser = userId;
      // fromRole = roleId;
      // }

      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      // first time submit the record
      if ((budgHold.getNextRole() == null)) {
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
            Resource.RDV_BudgHoldDtl, BigDecimal.ZERO, fromUser, false, budgHold.getStatus());
      }
      // after submit- Next level approval
      else {
        // checking direct approver or delegated approver
        // if isDirectApproval flag is "true" then approver is direct approver
        if (isDirectApproval) {
          // getting next level approver
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
              Resource.RDV_BudgHoldDtl, BigDecimal.ZERO, fromUser, false, budgHold.getStatus());

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
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    userRole.list().get(0).getUserContact().getId(), Resource.RDV_BudgHoldDtl, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                // check backward delegation
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    delegatedFromRole, delegatedToRole, fromUser, Resource.RDV_BudgHoldDtl,
                    BigDecimal.ZERO);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                submittedRoleOrgId, delegatedFromRole, fromUser, Resource.RDV_BudgHoldDtl,
                BigDecimal.ZERO);
          }
        }
        // if approver is delegated user
        else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromUser, Resource.RDV_BudgHoldDtl, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.RDV_BudgHoldDtl, BigDecimal.ZERO);
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
            .getNextRoleLineList(budgHold.getNextRole(), Resource.RDV_BudgHoldDtl);

        // update the rdv transaction update status
        budgHold.setUpdated(new java.util.Date());
        budgHold.setUpdatedBy(OBContext.getOBContext().getUser());

        // if ((budgHold.getStatus().equals("REJ")
        // || budgHold.getStatus().equals("DR"))
        // && budgHold.getAction().equals("CO")) {
        // rdvtransaction.setRevoke(true);
        // } else
        // rdvtransaction.setRevoke(false);
        budgHold.setAction("AP");
        budgHold.setStatus("WFA");
        budgHold.setNextRole(nextRole);

        // if (rdvtransaction.isAdvancetransaction()
        // && rdvtransaction.getEfinRDVTxnlineList().size() > 0) {
        // EfinRDVTxnline txnLineObj = rdvtransaction.getEfinRDVTxnlineList().get(0);
        // txnLineObj.setAction("AP");
        // txnLineObj.setApprovalStatus("WFA");
        // OBDal.getInstance().save(txnLineObj);
        // }

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id= :alertRuleID");
        receipientQuery.setNamedParameter("alertRuleID", alertRuleId);
        List<AlertRecipient> receipientQueryList = receipientQuery.list();
        // forwardDao.getAlertForForwardedUser(rdvtransaction.getId(), alertWindow, alertRuleId,
        // objUser, clientId, Constants.APPROVE,
        // rdvtransaction.getTXNVersion()
        // + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
        // : "-" + rdvtransaction.getCertificateNo()),
        // Lang, vars.getRole(), rdvtransaction.getEUTForwardReqmoreinfo(),
        // Resource.RDV_Transaction, alertReceiversMap);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {

          // solve approval alerts
          AlertUtility.solveAlerts(budgHold.getId());

          // define waiting for approval description
          String Description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.rdvbudghold.wfa", Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(budgHold.getId(),
                budgHold.getSalesOrder().getDocumentNo()
                    + "-" + budgHold.getEfinRdvtxn().getDocumentNo(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                budgHold.getClient().getId(), Description, "NEW", alertWindow,
                "finance.rdvbudghold.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance()
                .createQuery(EutDocappDelegateln.class, " as e left join e.eUTDocappDelegate as hd "
                    + " where hd.role.id = :roleID and hd.fromDate <= :fromDate and hd.date >= :currentDate "
                    + " and e.documentType='EUT_126'");
            log.debug("delegationln:" + delegationln.getWhereAndOrderBy());
            delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("fromDate", currentDate);
            delegationln.setNamedParameter("currentDate", currentDate);
            List<EutDocappDelegateln> delegationlnList = delegationln.list();
            if (delegationln != null && delegationlnList.size() > 0) {
              AlertUtility.alertInsertionRole(budgHold.getId(),
                  budgHold.getSalesOrder().getDocumentNo() + "-"
                      + budgHold.getEfinRdvtxn().getDocumentNo(),
                  delegationlnList.get(0).getRole().getId(),
                  delegationlnList.get(0).getUserContact().getId(), budgHold.getClient().getId(),
                  Description, "NEW", alertWindow, "finance.rdvbudghold.wfa",
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
      // final approver - apply budget hold amt on weightage concept and update hold details in rdv
      // trx version
      else {
        // holdaction insertion start
        for (EFINRdvBudgHoldLine line : budgHold.getEFINRdvBudgHoldLineList()) {
          OBQuery<EfinRDVTxnline> rdvTxnLine = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
              "accountingCombination.id=:acctId and efinRdvtxn.id=:rdvId and netmatchAmt > 0 ");
          rdvTxnLine.setNamedParameter("acctId", line.getAccountingCombination().getId());
          rdvTxnLine.setNamedParameter("rdvId", rdvTxn.getId());
          if (rdvTxnLine.list().size() > 0) {
            rdvLineList = rdvTxnLine.list();
            // iterate each line in rdv for this uniquecode
            for (EfinRDVTxnline txnLine : rdvLineList) {
              number = number + 10;
              totalNetMatch = rdvLineList.stream().map(a -> a.getNetmatchAmt())
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

              weigtage = (((txnLine.getNetmatchAmt()).divide(totalNetMatch, 6,
                  BigDecimal.ROUND_HALF_EVEN)).multiply(line.getHoldAmount())).setScale(2,
                      RoundingMode.HALF_UP);

              EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
              action.setTxngroupref(ref);
              action.setClient(OBDal.getInstance().get(Client.class, clientId));
              action.setSequenceNumber(number);
              action.setTxnApplicationNo(rdvTxn.getTXNVersion().toString());
              action.setAction(ADD);
              action.setActionDate(new java.util.Date());
              action.setAmount(txnLine.getMatchAmt());
              action.setEfinRdvHoldTypes(line.getEfinRdvHoldTypes());
              action.setRDVHoldPercentage(new BigDecimal(0));
              action.setRDVHoldAmount(weigtage);
              action.setActionReason(null);
              action.setActionJustification(null);
              action.setBusinessPartner(null);
              action.setName(null);
              action.setEfinRdvtxnline(txnLine);
              action.setFreezeRdvHold(false);
              action.setInvoice(null);
              action.setAmrasarfAmount(BigDecimal.ZERO);
              action.setEfinRdvBudgholdline(line);
              action.setTxn(true);
              OBDal.getInstance().save(action);
              OBDal.getInstance().flush();

              holdDao.insertHoldHeader(action, action.getEfinRdvtxnline(),
                  action.getRDVHoldAmount(), null, null);
            }
          }
        }
        // holdaction insertion end

        // solve approval alerts - TaskNo:7618
        AlertUtility.solveAlerts(budgHold.getId());

        ArrayList<String> includeRecipient = new ArrayList<String>();

        // update the header status
        budgHold.setUpdated(new java.util.Date());
        budgHold.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (budgHold.getRole() != null) {
          objCreatedRole = budgHold.getRole();
        }

        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(budgHold.getNextRole(), Resource.RDV_BudgHoldDtl);

        // forwardDao.getAlertForForwardedUser(rdvtransaction.getId(), alertWindow, alertRuleId,
        // objUser, clientId, Constants.APPROVE,
        // rdvtransaction.getTXNVersion()
        // + (StringUtils.isEmpty(rdvtransaction.getCertificateNo()) ? ""
        // : "-" + rdvtransaction.getCertificateNo()),
        // Lang, vars.getRole(), rdvtransaction.getEUTForwardReqmoreinfo(),
        // Resource.RDV_Transaction, alertReceiversMap);

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
            .getProperty("finance.rdvbudghold.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(budgHold.getId(),
            budgHold.getSalesOrder().getDocumentNo() + "-"
                + budgHold.getEfinRdvtxn().getDocumentNo(),
            "", budgHold.getCreatedBy().getId(), budgHold.getClient().getId(), Description, "NEW",
            alertWindow, "finance.rdvbudghold.approved", Constants.GENERIC_TEMPLATE);
        budgHold.setAction("RE");
        budgHold.setStatus("APP");
        budgHold.setNextRole(null);
        count = 1;
        result.put("count", count);
        result.put("errormsg", "null");

        // if (rdvtransaction.isAdvancetransaction()
        // && rdvtransaction.getEfinRDVTxnlineList().size() > 0) {
        // EfinRDVTxnline txnLineObj = rdvtransaction.getEfinRDVTxnlineList().get(0);
        // txnLineObj.setAction("RE");
        // txnLineObj.setApprovalStatus("APP");
        // OBDal.getInstance().save(txnLineObj);
        // }

        // Budget User Alert starts
        // set alert
        StringBuffer query = new StringBuffer();
        Query rdvBudgQuery = null;
        BigDecimal amtHold = BigDecimal.ZERO;

        query.append(
            "SELECT coalesce(SUM(rdvbudg.holdAmount), 0) as holdamt FROM EFIN_Rdv_BudgHoldLine rdvbudg where efinRdvBudghold.id=:rdvBudgId ");
        rdvBudgQuery = OBDal.getInstance().getSession().createQuery(query.toString());
        rdvBudgQuery.setParameter("rdvBudgId", budgHold.getId());
        log.debug(" Query : " + query.toString());
        if (rdvBudgQuery != null) {
          if (rdvBudgQuery.list().size() > 0) {
            if (rdvBudgQuery.iterate().hasNext()) {
              String holdAmt = rdvBudgQuery.iterate().next().toString();
              amtHold = new BigDecimal(holdAmt);
            }
          }
        }
        String acctnos = "";
        String uniqueCodeId = "";
        String uniqueCode = "";
        OBQuery<EFINRdvBudgHoldLine> budgHldLn = OBDal.getInstance().createQuery(
            EFINRdvBudgHoldLine.class,
            " as e where e.efinRdvBudghold.id=:rdvBudgHold order by line");
        budgHldLn.setNamedParameter("rdvBudgHold", budgHold.getId());
        if (budgHldLn.list().size() > 0) {
          for (EFINRdvBudgHoldLine budgHld : budgHldLn.list()) {
            if (StringUtils.isEmpty(acctnos)) {
              acctnos = budgHld.getAccountingCombination().getAccount().getSearchKey();
              uniqueCodeId = budgHld.getAccountingCombination().getId();
              uniqueCode = budgHld.getAccountingCombination().getEfinUniqueCode();
            } else
              acctnos += "," + budgHld.getAccountingCombination().getAccount().getSearchKey();
          }
        }

        /*
         * OBQuery<EFINRdvBudgholdV> budgHldAcct = OBDal.getInstance().createQuery(
         * EFINRdvBudgholdV.class,
         * " as e where e.accountingCombination.id=:uniqueCodeId order by created asc");
         * budgHldAcct.setNamedParameter("uniqueCodeId", uniqueCodeId); budgHldAcct.setMaxResult(1);
         * if (budgHldAcct.list().size() > 0) { budgAcct = budgHldAcct.list().get(0).getId();
         * uniqueCode = budgHldAcct.list().get(0).getAccountingCombination().getEfinUniqueCode(); }
         */
        log.debug("acctLineId>" + uniqueCode);
        if (amtHold.compareTo(BigDecimal.ZERO) > 0) {
          description = OBMessageUtils.messageBD("EFIN_RDVBudgetHoldAlert")
              .replace("$", rdvtransaction.getEfinRdv().getSalesOrder().getDocumentNo())
              .replace("%", acctnos);
          AlertUtility.alertInsertionPreferenceBudUser(uniqueCodeId, uniqueCode, "EFIN_BUDGET_USER",
              rdvtransaction.getClient().getId(), description, "NEW",
              AlertWindow.RDVBudgetHOLDALERT, "finance.rdvbudghold.amount",
              Constants.GENERIC_TEMPLATE, windowId, null);
        }

        // Check Alert for Budget User and change Status 'Solved' when txn version hold amount is
        // completely released
        List<String> txnIdList = new ArrayList<String>();
        List<Integer> released = new ArrayList<Integer>();
        List<EfinRDVTransaction> rdvTransactionList = rdvtransaction.getEfinRdv()
            .getEfinRDVTxnList().stream()
            .filter(x -> (x.getHoldamount().compareTo(BigDecimal.ZERO) != 0
                && !x.getId().equals(rdvtransaction.getId())))
            .collect(Collectors.toList());
        if (rdvTransactionList.size() > 0) {
          rdvTransactionList.forEach(x -> {
            x.getEfinRDVTxnlineList().forEach(y -> {

              List<EfinRdvHoldAction> holdRelease = y.getEfinRdvHoldActionList().stream()
                  .filter(z -> (z.getRDVHoldRel() == null)).collect(Collectors.toList());

              List<EfinRdvHoldAction> isRelease = holdRelease.stream()
                  .filter(z -> (z.isReleased() && z.getRDVHoldRel() == null))
                  .collect(Collectors.toList());

              if (isRelease.size() == holdRelease.size()) {
                txnIdList.add(x.getId());
              }
              holdRelease.clear();
              isRelease.clear();
            });
          });
        }
        if (txnIdList.size() > 0) {
          txnIdList.forEach(x -> {
            OBQuery<Alert> alertObj = OBDal.getInstance().createQuery(Alert.class,
                " as e where e.referenceSearchKey =:referenceSearchKey and e.alertStatus = 'NEW' and e.eutAlertKey = 'finance.rdvbudghold.amount' ");
            alertObj.setNamedParameter("referenceSearchKey", x);
            List<Alert> alertList = alertObj.list();
            if (alertList.size() > 0) {
              alertList.forEach(a -> {
                Alert alert = OBDal.getInstance().get(Alert.class, a.getId());
                alert.setAlertStatus("SOLVED");
              });
            }
          });
        }
        // Budget User Alert ends
      }
      OBDal.getInstance().save(budgHold);
      strRdvBudgHoldId = budgHold.getId();

      // insert approval history
      if (!StringUtils.isEmpty(strRdvBudgHoldId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", strRdvBudgHoldId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.RDV_BudgHold_History);
        historyData.put("HeaderColumn", ApprovalTables.RDV_BudgHold_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.RDV_BudgHold_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);
      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.RDV_BudgHoldDtl);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.debug("Exception in updateHeaderStatus Budget Hold Plan Details:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
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
   * @param rdvbudgholdid
   * @param roleId
   * @return
   */
  @SuppressWarnings("unused")
  public boolean isDirectApproval(String rdvbudgholdid, String roleId) {
    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(rdvbh.efin_rdv_budghold_id) from efin_rdv_budghold rdvbh join eut_next_role rl on "
          + "rdvbh.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and rdvbh.efin_rdv_budghold_id = ? and li.ad_role_id =?";
      log.debug("query" + query.toString());
      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, rdvbudgholdid);
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
      log.error("Exception in RDV Budget Hold submit -isDirectApproval " + e.getMessage());
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

  @Override
  public JSONObject reactivateHeader(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinRDVTransaction rdvtransaction, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle, EFINRdvBudgHold budgHold) {
    // TODO Auto-generated method stub
    String strRdvBudgHoldId = null;
    JSONObject result = new JSONObject();
    RdvHoldActionDAO dao = null;
    try {
      dao = new RdvHoldActionDAOimpl(OBDal.getInstance().getConnection());
      // setting json object value initially
      result.put("count", 0);
      result.put("errormsg", "null");

      // delete hold action.
      for (EFINRdvBudgHoldLine budgetLn : budgHold.getEFINRdvBudgHoldLineList()) {
        for (EfinRdvHoldAction holdAction : budgetLn.getEfinRdvHoldActionList()) {
          dao.deleteHoldHed(holdAction);
        }
      }

      // change status
      budgHold.setUpdated(new java.util.Date());
      budgHold.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
      budgHold.setStatus("DR");
      budgHold.setAction("CO");
      budgHold.setNextRole(null);
      OBDal.getInstance().save(budgHold);

      // insert approval history
      strRdvBudgHoldId = budgHold.getId();
      if (!StringUtils.isEmpty(strRdvBudgHoldId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", strRdvBudgHoldId);
        historyData.put("Comments", comments);
        historyData.put("Status", "REA");
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.RDV_BudgHold_History);
        historyData.put("HeaderColumn", ApprovalTables.RDV_BudgHold_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.RDV_BudgHold_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);
      }
      result.put("count", 1);
      result.put("errormsg", "null");
      OBDal.getInstance().flush();

      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.RDV_BudgHoldDtl);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.debug("Exception in reactivateHeader Budget Hold Plan Details:" + e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
