package sa.elm.ob.finance.ad_process.dao;

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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetEncumAppHist;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.efinbudgetencum;
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
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Priyanka Ranjan on 18-12-2017
 */

// DAO file for Encumbrance Submit process
public class ManualEncumbaranceSubmitDAO {
  private static final Logger LOG = LoggerFactory.getLogger(ManualEncumbaranceSubmitDAO.class);

  /**
   * Checks if a budget lines has Insufficient Funds Available" is excluded from the submit process
   * 
   * @param manEncumId
   *          ,Accounting Date
   * @return "0" is the budget lines has Insufficient Funds
   */

  public static String preValidation(String ManEncumId, Date ActDate, String docStatus,
      boolean hasApproval, String docAction) {
    // Pre-validation on funds available
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps1 = null, ps2 = null;
    ResultSet rs = null, rs1 = null, rs2 = null;
    String Query = "";
    String amount = "";
    String validation = "0";
    BigDecimal wfaAmt = new BigDecimal(0);
    BigDecimal currAmt = new BigDecimal(0);
    int errorFlag = 0;
    try {
      Query = "  select efin_budget_manencumlines_id,uniquecode,amount ,efin_budget_manencum_id,now() as date,efin_budgetlines_id , "
          + " ad_org_id,c_project_id,c_salesregion_id,c_campaign_id,c_activity_id,user1_id,user2_id,c_elementvalue_id,funds_available  from efin_budget_manencumlines"
          + " where efin_budget_manencum_id='" + ManEncumId + "'";
      LOG.debug("PreValidation select Query:" + Query.toString());
      ps = conn.prepareStatement(Query);
      rs = ps.executeQuery();
      while (rs.next()) {
        String encumLinesId = rs.getString("efin_budget_manencumlines_id");
        currAmt = rs.getBigDecimal("amount");
        ps1 = conn.prepareStatement(
            "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
        ps1.setString(1, rs.getString("ad_org_id"));
        ps1.setString(2, rs.getString("c_elementvalue_id"));
        ps1.setString(3, rs.getString("c_project_id"));
        ps1.setString(4, rs.getString("c_salesregion_id"));
        ps1.setString(5, rs.getString("c_campaign_id"));
        ps1.setString(6, rs.getString("c_activity_id"));
        ps1.setString(7, rs.getString("user1_id"));
        ps1.setString(8, rs.getString("user2_id"));
        ps1.setString(9, Utility.formatDate(ActDate));
        ps1.setString(10, OBContext.getOBContext().getCurrentClient().getId());
        LOG.debug("budget_process query:" + ps1.toString());
        rs1 = ps1.executeQuery();
        if (rs1.next()) {
          amount = rs1.getString("p_amount");
        }
        LOG.debug("amount<linesFunds-->" + amount + "<" + currAmt);
        LOG.debug("wfaAmt>>" + (currAmt.add(wfaAmt.divide(new BigDecimal(2)))) + ">" + amount);
        // if(status && docAction)
        if ((docStatus.equals("DR") || docStatus.equals("RW")) && docAction.equals("CO")) {
          // if (hasApproval) {
          ps = conn.prepareStatement(
              "select sum(eln.amount) as wfaamt from efin_budget_manencumlines eln left join efin_budget_manencum e "
                  + "on e.efin_budget_manencum_id=eln.efin_budget_manencum_id "
                  + "where docstatus='WFA' and e.efin_budget_manencum_id<>? and efin_budgetlines_id=? group by c_elementvalue_id ");
          ps.setString(1, ManEncumId);
          ps.setString(2, rs.getString("efin_budgetlines_id"));
          LOG.debug("wfa qry>>" + ps.toString());
          rs1 = ps.executeQuery();
          if (rs1.next()) {
            wfaAmt = rs1.getBigDecimal("wfaamt");
          }

          // checking current amt+(WFA amt/2) greater than funds available
          if ((currAmt.add(wfaAmt.divide(new BigDecimal(2)))).compareTo(new BigDecimal(amount)) > 0
              || new BigDecimal(amount).compareTo(BigDecimal.ZERO) == 0)
            // || (currAmt.add(wfaAmt.divide(new BigDecimal(2))))
            // .compareTo(new BigDecimal(amount)) == 0)

            errorFlag = 1;
          else
            errorFlag = 0;

        } else {
          LOG.debug("no Approval>>" + hasApproval);
          if (new BigDecimal(amount).compareTo(currAmt) < 0
              || new BigDecimal(amount).compareTo(BigDecimal.ZERO) == 0)

            errorFlag = 1;
          else
            errorFlag = 0;
        }
        LOG.debug("errorFlag>>" + errorFlag);
        if (errorFlag == 1) {
          EfinBudgetManencumlines encumlines = OBDal.getInstance()
              .get(EfinBudgetManencumlines.class, encumLinesId);
          encumlines.setCheckingStatus("Error: In sufficient  Funds Available");
          encumlines.setFundsAvailable(new BigDecimal(amount));
          encumlines.setErrorflag("1");
          OBDal.getInstance().save(encumlines);
          OBDal.getInstance().flush();
        } else if (errorFlag == 0) {
          EfinBudgetManencumlines encumlines = OBDal.getInstance()
              .get(EfinBudgetManencumlines.class, encumLinesId);
          encumlines.setCheckingStatus("Success");
          encumlines.setFundsAvailable(new BigDecimal(amount));
          encumlines.setErrorflag("0");
          OBDal.getInstance().save(encumlines);
          OBDal.getInstance().flush();
        }
      }
      OBDal.getInstance().flush();

      ps2 = conn.prepareStatement(
          "select efin_budget_manencumlines_id from efin_budget_manencumlines where efin_budget_manencum_id='"
              + ManEncumId + "' and errorflag='1' ");
      rs2 = ps2.executeQuery();
      if (rs2.next()) {
        validation = "0";
      } else {
        validation = "1";
      }
      LOG.debug("validation>" + validation);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      validation = "2";
      LOG.error("Exception in PreValidation Of Encumbarance", e.getMessage());
      return validation;
    }
    return validation;

  }

  /**
   * Insert The Records in efinbudgetencum table to maintain history of encumbrance
   * 
   * @param manEncumId
   * @return "1" is the process success
   */

  public static String submitProcess(String ManEncumId) {
    // Inserts records in Manual Encumbarnce History("Efin_Budget_Encum")
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String Query = "";
    try {
      LOG.debug("entering Submit Process");
      Query = " select ln.efin_budget_manencumlines_id,ln.uniquecode,ln.c_salesregion_id as dept,ln.amount ,ln.efin_budget_manencum_id,now() as date,ln.efin_budgetlines_id,head.dateacct as actdate,head.trxdate as trxdate,ln.description ,head.encum_type as type from efin_budget_manencumlines ln"
          + " join efin_budget_manencum head on head.efin_budget_manencum_id = ln.efin_budget_manencum_id "
          + " where ln.efin_budget_manencum_id='" + ManEncumId + "'";
      LOG.debug("Sumbit select Query:" + Query.toString());
      ps = conn.prepareStatement(Query);
      rs = ps.executeQuery();
      while (rs.next()) {
        efinbudgetencum efinencum = OBProvider.getInstance().get(efinbudgetencum.class);
        efinencum.setClient(OBContext.getOBContext().getCurrentClient());
        efinencum.setOrganization(OBContext.getOBContext().getCurrentOrganization());
        efinencum.setActive(true);
        efinencum.setUpdatedBy(OBContext.getOBContext().getUser());
        efinencum.setCreationDate(new java.util.Date());
        efinencum.setCreatedBy(OBContext.getOBContext().getUser());
        efinencum.setUpdated(new java.util.Date());
        efinencum.setAmount(rs.getBigDecimal("amount"));
        efinencum.setTransactionDate(rs.getDate("trxdate"));
        efinencum.setAccountingDate(rs.getDate("actdate"));
        efinencum.setDescription(rs.getString("description"));
        efinencum.setManualEncumbranceLines(OBDal.getInstance().get(EfinBudgetManencumlines.class,
            rs.getString("efin_budget_manencumlines_id")));
        /*
         * efinencum.setBudgetLines( OBDal.getInstance().get(EFINBudgetLines.class,
         * rs.getString("efin_budgetlines_id")));
         */
        efinencum.setAppstatus("APP");
        if (rs.getString("type").equals("MEI"))
          efinencum.setDoctype("MEI");
        else if (rs.getString("type").equals("MAD"))
          efinencum.setDoctype("MAD");
        efinencum.setUniqueCode(rs.getString("uniquecode"));
        efinencum
            .setManualEncumbrance(OBDal.getInstance().get(EfinBudgetManencum.class, ManEncumId));

        // to add department
        SalesRegion Dept = OBDal.getInstance().get(SalesRegion.class, rs.getString("dept"));
        efinencum.setDept(Dept);
        LOG.debug("dept :" + Dept.getName());

        OBDal.getInstance().save(efinencum);
        OBDal.getInstance().flush();
        // All values in Encumabarance Inserted
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      // e.printStackTrace();
      // log.error("Exception in submitProcess Of Encumbarance", e.getMessage());
      throw new OBException(e);
      // return "0";
    }
    return "1";
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param encumbranceId
   * @param comments
   * @return EfinBudgetManencumID
   */
  public static String checkApprover(Connection con, String clientId, String orgId, String roleId,
      String userId, String encumbranceId, String comments, VariablesSecureApp vars) {
    String headerId = null;
    Boolean isDirectApproval = false, reserve = false;
    String appstatus = "", pendingapproval = "", reserveRoleId = "";// , manencumstatus = "";
    BigDecimal rem_amount = BigDecimal.ZERO;
    PreparedStatement st1 = null;
    String alertRuleId = "", alertWindow = AlertWindow.Encumbrance, Description = "";
    Date currentDate = new Date();
    String doctype = null;
    doctype = Resource.MANUAL_ENCUMBRANCE_RULE;
    User objUser = Utility.getObject(User.class, vars.getUser());
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    String fromUser = userId;
    String fromRole = roleId;
    boolean isDummyRole = false;
    StringBuffer delegationQuery = new StringBuffer();
    Boolean isReserveRoleCrossed = Boolean.FALSE;
    try {
      OBContext.setAdminMode(true);
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      EfinBudgetManencum header = OBDal.getInstance().get(EfinBudgetManencum.class, encumbranceId);
      // Task #8198
      // approval flow based on submitted user role
      if (header.getNextRole() != null) {
        if (header.getEfinSubmittedRole() != null
            && header.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = header.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (header.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }
      if (header.getDocumentStatus().equals("CO") || header.getDocumentStatus().equals("WFA"))
        appstatus = "APP";

      else if (header.getDocumentStatus().equals("DR") | header.getDocumentStatus().equals("RW"))
        appstatus = "SUB";
      NextRoleByRuleVO nextApproval = null;
      EutNextRole nextRole = null;
      isDirectApproval = isDirectApproval(encumbranceId, roleId);
      LOG.debug("chkDirectApproval" + isDirectApproval);

      if ((header.getNextRole() != null)) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(header.getNextRole(),
            userId, roleId, clientId, submittedRoleOrgId, Resource.MANUAL_ENCUMBRANCE_RULE,
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

      if ((header.getNextRole() == null)) {
        reserveRoleId = fromRole;
        // nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
        // orgId, roleId, userId, Resource.MANUAL_ENCUMBRANCE_RULE, encumamt);
        // line manager based rule
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
            Resource.MANUAL_ENCUMBRANCE_RULE, BigDecimal.ZERO, fromUser, false,
            header.getDocumentStatus());
      } else {
        if (isDirectApproval) {
          reserveRoleId = fromRole;
          // nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(),
          // clientId,
          // orgId, roleId, userId, Resource.MANUAL_ENCUMBRANCE_RULE, encumamt);
          // line manager based rule
          nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
              OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId, fromRole, fromUser,
              Resource.MANUAL_ENCUMBRANCE_RULE, BigDecimal.ZERO, fromUser, false,
              header.getDocumentStatus());
        } else {
          String delegatedFromRole = null;
          String delegatedToRole = null;
          HashMap<String, String> role = null;

          String qu_next_role_id = "";

          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromUser, Resource.MANUAL_ENCUMBRANCE_RULE,
              qu_next_role_id);
          delegatedFromRole = role.get("FromUserRoleId");

          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)

            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.MANUAL_ENCUMBRANCE_RULE, 0.00);
          reserveRoleId = delegatedFromRole;
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && ((nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole"))
              || (nextApproval.getErrorMsg().equals("Managernotdefined")))) {
        return "NoManagerAssociatedWithRole";
      } else if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        LOG.debug("next roleid>>" + nextApproval.getNextRoleId());
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(header.getNextRole(), Resource.MANUAL_ENCUMBRANCE_RULE);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        if ((header.getDocumentStatus().equals("RW") || header.getDocumentStatus().equals("DR"))
            && header.getAction().equals("CO")) {
          header.setRevoke(true);
        } else
          header.setRevoke(false);
        header.setDocumentStatus("WFA");
        header.setNextRole(nextRole);
        header.setAction("AP");
        pendingapproval = nextApproval.getStatus();
        // manencumstatus = "In Approval";

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        forwardDao.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, header.getDocumentNo(), vars.getLanguage(), vars.getRole(),
            header.getEUTForwardReqmoreinfo(), Resource.MANUAL_ENCUMBRANCE_RULE, alertReceiversMap);
        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          /*
           * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
           * "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'"); if
           * (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
           * objAlert.setAlertStatus("SOLVED"); } }
           */
          // set the description for alert
          Description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.encumbrance.waiting.for.approval", vars.getLanguage());

          if (pendingapproval == null)
            pendingapproval = nextApproval.getStatus();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                header.getClient().getId(), Description, "NEW", alertWindow,
                "finance.encumbrance.waiting.for.approval", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            delegationQuery.append("as e  left join e.eUTDocappDelegate as hd "
                + " where hd.role.id =:nxtRoleId and hd.fromDate <=:currentdate "
                + " and hd.date >=:currentdate and e.documentType=:docType ");
            if (objNextRoleLine.getUserContact() != null)
              delegationQuery.append(" and hd.userContact.id=:nxtUserId ");

            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance()
                .createQuery(EutDocappDelegateln.class, delegationQuery.toString());

            delegationln.setNamedParameter("nxtRoleId", objNextRoleLine.getRole().getId());
            delegationln.setNamedParameter("currentdate", currentDate);
            delegationln.setNamedParameter("docType", doctype);
            if (objNextRoleLine.getUserContact() != null)
              delegationln.setNamedParameter("nxtUserId", objNextRoleLine.getUserContact().getId());

            if (delegationln != null && delegationln.list().size() > 0) {
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                  delegationln.list().get(0).getRole().getId(),
                  delegationln.list().get(0).getUserContact().getId(), header.getClient().getId(),
                  Description, "NEW", alertWindow, "finance.encumbrance.waiting.for.approval",
                  Constants.GENERIC_TEMPLATE);
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationln.list().get(0).getUserContact().getName());
              includeRecipient.add(delegationln.list().get(0).getRole().getId());

            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());

          }
        }

        // existing Recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
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

      } else {
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(header.getNextRole(), Resource.MANUAL_ENCUMBRANCE_RULE);
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setDocumentStatus("CO");
        header.setNextRole(null);
        header.setAction("PD");
        // manencumstatus = "Approved";
        header.setRevoke(false);

        Role objCreatedRole = null;
        ArrayList<String> includeRecipient = new ArrayList<String>();
        if (header.getCreatedBy().getADUserRolesList().size() > 0) {
          if (header.getRole() != null)
            objCreatedRole = header.getRole();
          else
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        forwardDao.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, header.getDocumentNo(), vars.getLanguage(), vars.getRole(),
            header.getEUTForwardReqmoreinfo(), Resource.MANUAL_ENCUMBRANCE_RULE, alertReceiversMap);
        // delete alert for approval alerts
        /*
         * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
         * "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'"); if
         * (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
         * objAlert.setAlertStatus("SOLVED"); } }
         */
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery1 = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
        if (receipientQuery1.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery1.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        includeRecipient.add(objCreatedRole.getId());

        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        // set alert for requester
        Description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.encumbrance.approved", vars.getLanguage());

        if (header != null && header.getEfinBudgetEncumAppHistList().size() > 0) {
          OBQuery<EfinBudgetEncumAppHist> approvalHistoryQuery = OBDal.getInstance().createQuery(
              EfinBudgetEncumAppHist.class,
              "as e where e.encumAction=:action and e.manualEncumbrance.id=:id order by e.creationDate desc");
          approvalHistoryQuery.setNamedParameter("action", "SUB");
          approvalHistoryQuery.setNamedParameter("id", header.getId());
          List<EfinBudgetEncumAppHist> approvalHistoryList = approvalHistoryQuery.list();
          if (approvalHistoryList.size() > 0) {
            EfinBudgetEncumAppHist submitterApproval = approvalHistoryList.get(0);
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                header.getRole().getId(), submitterApproval.getCreatedBy().getId(),
                header.getClient().getId(), Description, "NEW", alertWindow,
                "finance.encumbrance.approved", Constants.GENERIC_TEMPLATE);
          }

        }

        // EfinBudgetManencum header = OBDal.getInstance().get(EfinBudgetManencum.class,
        // encumbranceId);
        OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class, "manualEncumbrance.id='" + encumbranceId + "'");
        if (lines.list() != null && lines.list().size() > 0) {
          for (int i = 0; i < lines.list().size(); i++) {
            EfinBudgetManencumlines encumline = lines.list().get(i);
            rem_amount = rem_amount.add(encumline.getOriginalamount());

            BigDecimal remaining_amount = (encumline.getOriginalamount())
                .subtract(encumline.getUsedAmount());
            encumline.setRemainingAmount(remaining_amount);
            OBDal.getInstance().save(encumline);
          }
        }
        header.setRemainingamt(rem_amount);
        LOG.debug("remaining amt:" + header.getRemainingamt());
        // String success = submitProcess(encumbranceId);
      }
      LOG.debug("approve:" + header.toString());
      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();

      // checking role is reserver , if reserver then doing funds allocation
      reserve = UtilityDAO.getReserveFundsRole(Resource.MANUAL_ENCUMBRANCE_RULE, reserveRoleId,
          header.getOrganization().getId(), header.getId(), BigDecimal.ZERO);
      LOG.debug("reserve:" + reserve);

      isReserveRoleCrossed = UtilityDAO.chkReserveIsDoneorNot(clientId, orgId, reserveRoleId,
          userId, Resource.MANUAL_ENCUMBRANCE_RULE, BigDecimal.ZERO);

      if (reserve || (isReserveRoleCrossed && !header.isReservedfund())) {
        // insert record in budget enquiry
        for (EfinBudgetManencumlines encumLine : header.getEfinBudgetManencumlinesList()) {
          EfinBudgetInquiry budInq = null;
          String sql = "select efin_updateBudgetInq(?,?,?) from dual ;";
          st1 = con.prepareStatement(sql);
          st1.setString(1, encumLine.getAccountingCombination().getId());
          st1.setBigDecimal(2, encumLine.getRevamount());
          st1.setString(3, header.getBudgetInitialization().getId());
          st1.executeQuery();

          if (!encumLine.getAccountingCombination().isEFINDepartmentFund()) {
            // update or insert in budget enquiry for unique code with department funds ='N'
            String budgetIntId = encumLine.getManualEncumbrance().getBudgetInitialization().getId();

            budInq = getBudgetInquiry(encumLine.getAccountingCombination().getId(), budgetIntId);

            if (budInq != null) {
              budInq.setEncumbrance(budInq.getEncumbrance().add(encumLine.getAmount()));
              OBDal.getInstance().save(budInq);
            } else {
              EfinBudgetInquiry parentInq = null;
              // Get Parent Id for new budget Inquiry
              parentInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                  encumLine.getAccountingCombination(),
                  encumLine.getManualEncumbrance().getBudgetInitialization());

              budInq = OBProvider.getInstance().get(EfinBudgetInquiry.class);
              budInq.setEfinBudgetint(encumLine.getManualEncumbrance().getBudgetInitialization());
              budInq.setAccountingCombination(encumLine.getAccountingCombination());
              budInq.setUniqueCodeName(encumLine.getUniqueCodeName());
              budInq.setUniqueCode(encumLine.getAccountingCombination().getEfinUniqueCode());
              budInq.setOrganization(encumLine.getOrganization());
              budInq.setDepartment(encumLine.getSalesRegion());
              budInq.setAccount(encumLine.getAccountElement());
              budInq.setSalesCampaign(encumLine.getSalesCampaign());
              budInq.setProject(encumLine.getProject());
              budInq.setBusinessPartner(encumLine.getBusinessPartner());
              budInq.setFunctionalClassfication(encumLine.getActivity());
              budInq.setFuture1(encumLine.getStDimension());
              budInq.setNdDimension(encumLine.getNdDimension());
              budInq.setORGAmt(BigDecimal.ZERO);
              budInq.setREVAmount(BigDecimal.ZERO);
              budInq.setFundsAvailable(BigDecimal.ZERO);
              budInq.setCurrentBudget(BigDecimal.ZERO);
              budInq.setEncumbrance(encumLine.getAmount());
              budInq.setSpentAmt(BigDecimal.ZERO);
              budInq.setParent(parentInq);
              budInq.setVirtual(true);
              OBDal.getInstance().save(budInq);
            }
          }

        }
        OBDal.getInstance().flush();
        header.setReservedfund(true);
      }

      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.MANUAL_ENCUMBRANCE_RULE);
      // after approved by forwarded user removing the forward and rmi id
      if (header.getEUTForwardReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
        header.setEUTForwardReqmoreinfo(null);
      }
      if (header.getEUTReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
        header.setEUTReqmoreinfo(null);
        header.setRequestMoreInformation("N");
      }

      headerId = header.getId();
      if (!StringUtils.isEmpty(headerId)) {
        insertManEncumHistory(OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
            headerId, comments, appstatus, pendingapproval);
        /*
         * changeEncumStatus(OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
         * headerId, manencumstatus);
         */
      }

    } catch (

    Exception e) {
      LOG.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return headerId;
    } finally {
      OBContext.restorePreviousMode();
    }
    return headerId;
  }

  /**
   * getBudgetInquiry is used to get the budget inquiry with given validcombination and budget
   * definition Id
   * 
   * @param encumLine
   * @return list of EfinBudgetInquiry
   */
  public static EfinBudgetInquiry getBudgetInquiry(String combinationId, String budIntId) {

    StringBuffer whereClause = new StringBuffer();
    whereClause.append(" where accountingCombination.id = :accId");
    whereClause.append(" and efinBudgetint.id = :budIntId ");

    OBQuery<EfinBudgetInquiry> query = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
        whereClause.toString());
    query.setNamedParameter("accId", combinationId);
    query.setNamedParameter("budIntId", budIntId);

    List<EfinBudgetInquiry> budInquiryList = query.list();
    if (budInquiryList != null && budInquiryList.size() > 0) {
      return budInquiryList.get(0);
    } else {
      return null;
    }

  }

  /**
   * 
   * @param ManEncumId
   * @param roleId
   * @return boolean
   */
  @SuppressWarnings("unused")
  public static boolean isDirectApproval(String ManEncumId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(*) from EFIN_BUDGET_MANENCUM man join eut_next_role rl on "
          + "man.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + " and man.EFIN_BUDGET_MANENCUM_id = ? and li.ad_role_id =? ";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, ManEncumId);
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
      LOG.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    }
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param headerId
   * @param comments
   * @param appstatus
   * @param pendingapproval
   * @return
   */
  public static int insertManEncumHistory(Connection con, String clientId, String orgId,
      String roleId, String userId, String headerId, String comments, String appstatus,
      String pendingapproval) {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    String histId = null;
    try {
      OBContext.setAdminMode(true);

      EfinBudgetEncumAppHist hist = OBProvider.getInstance().get(EfinBudgetEncumAppHist.class);
      hist.setClient(dao.getObject(Client.class, clientId));
      hist.setOrganization(dao.getObject(Organization.class, orgId));
      hist.setActive(true);
      hist.setCreatedBy(dao.getObject(User.class, userId));
      hist.setCreationDate(new java.util.Date());
      hist.setCreatedBy(dao.getObject(User.class, userId));
      hist.setUpdated(new java.util.Date());
      hist.setManualEncumbrance(OBDal.getInstance().get(EfinBudgetManencum.class, headerId));
      hist.setEncumAction(appstatus);
      hist.setApprovedDate(new java.util.Date());
      hist.setComments(comments);
      hist.setPendingApproval(pendingapproval);
      LOG.debug("hist:" + hist.toString());
      OBDal.getInstance().save(hist);
      histId = hist.getId();
      if (histId != null)
        return 1;
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      LOG.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

}
