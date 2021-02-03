/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.budget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.marketing.Campaign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
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
 * @author Gowtham.V
 */
public class BudgetDAO {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetDAO.class);

  /**
   * This method is used to insert budget lines with valid combination while doing addlines button.
   * 
   * @param clientId
   * @param budget
   * @param Department
   * @return 0 -->No lines added 1 -->Success 2 -->Error.
   */
  public static int insertBudegtLines(String clientId, EFINBudget budget, String department,
      String organization, Connection connection) {
    final BigDecimal amount = BigDecimal.ZERO;
    long lineNo = 10, maxLine = 0;
    int returnValue = 0;
    boolean isActive = true, havingLines = false;
    String query = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      // get maximum line number if already have lines.
      if (budget.getEFINBudgetLinesList() != null && budget.getEFINBudgetLinesList().size() > 0) {
        List<EFINBudgetLines> lines = new ArrayList<EFINBudgetLines>();
        final List<EFINBudgetLines> inActiveCode = new ArrayList<EFINBudgetLines>();
        lines = budget.getEFINBudgetLinesList();

        // delete inactive validcombination lines.
        for (EFINBudgetLines delLines : lines) {
          isActive = delLines.getAccountingCombination().isActive();
          if (!isActive) {
            inActiveCode.add(delLines);
          }
        }
        budget.getEFINBudgetLinesList().removeAll(inActiveCode);
        for (EFINBudgetLines romoveLines : inActiveCode) {
          OBDal.getInstance().remove(romoveLines);
        }

        // get max line no
        if (budget.getEFINBudgetLinesList() != null && budget.getEFINBudgetLinesList().size() > 0) {
          final EFINBudgetLines lastLine = lines.get(budget.getEFINBudgetLinesList().size() - 1);
          maxLine = lastLine.getLineNo();
          lineNo = maxLine + 10;
        }
      }

      // get valid combinations from account combination.
      query = "select com.c_validcombination_id from c_validcombination com "
          + "join c_elementvalue acc on acc.c_elementvalue_id = com.account_id  "
          + "where com.c_campaign_id ='" + budget.getSalesCampaign().getId()
          + "' and com.c_salesregion_id ='" + department + "' and com.ad_org_id ='" + organization
          + "' and em_efin_uniquecode <> '' and "
          + "account_id in( select replace(unnest(string_to_array (eut_getchildacct('"
          + budget.getAccountElement().getId() + "'),',')::character varying []),'''','') ) "
          + "and acc.accounttype = 'E' and com.ad_client_id ='" + clientId
          + "' and com.isactive='Y' and "
          + "com.c_validcombination_id not in (select ln.c_validcombination_id from efin_budgetlines ln "
          + "join efin_budget hd on hd.efin_budget_id = ln.efin_budget_id "
          + " where hd.c_elementvalue_id = '" + budget.getAccountElement().getId()
          + "' and hd.c_campaign_id = '" + budget.getSalesCampaign().getId()
          + "' and hd.efin_budgetint_id = '" + budget.getEfinBudgetint().getId()
          + "' and hd.ad_client_id = '" + clientId + "'" + " )"
          + "and com.c_validcombination_id not in(select c_validcombination_id from efin_budgetinquiry where efin_budgetint_id = '"
          + budget.getEfinBudgetint().getId() + "' and ad_client_id = '" + clientId + "') ";
      ps = connection.prepareStatement(query);
      rs = ps.executeQuery();
      while (rs.next()) {
        havingLines = true;
        AccountingCombination uniquecode = OBDal.getInstance().get(AccountingCombination.class,
            rs.getString("c_validcombination_id"));
        final EFINBudgetLines budgetLine = OBProvider.getInstance().get(EFINBudgetLines.class);
        budgetLine.setOrganization(uniquecode.getOrganization());
        budgetLine.setAccountElement(uniquecode.getAccount());
        budgetLine.setEfinBudget(budget);
        budgetLine.setActivity(uniquecode.getActivity());
        budgetLine.setStDimension(uniquecode.getStDimension());
        budgetLine.setNdDimension(uniquecode.getNdDimension());
        budgetLine.setSalesRegion(uniquecode.getSalesRegion());
        budgetLine.setSalesCampaign(uniquecode.getSalesCampaign());
        budgetLine.setBusinessPartner(uniquecode.getBusinessPartner());
        budgetLine.setProject(uniquecode.getProject());
        budgetLine.setUniquecode(uniquecode.getEfinUniqueCode());
        budgetLine.setAccountingCombination(uniquecode);
        budgetLine.setUniqueCodeName(uniquecode.getEfinUniquecodename());
        budgetLine.setLineNo(lineNo);
        budgetLine.setAmount(amount);
        lineNo += 10;
        OBDal.getInstance().save(budgetLine);
        returnValue = 1; // Success.
      }
      if (!havingLines) {
        returnValue = 0; // No lines.
      }
      return returnValue;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in insertBudegtLines() " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 2; // process failed.
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        LOG.error("Exception in closing coonection() " + e);
      }
    }
  }

  /**
   * This mehod is used to insert lines in inquiry.
   * 
   * @param header
   * @return 0,1,2.
   */
  public static int insertInquiryLines(EFINBudget header) {
    List<EFINBudgetLines> lines = new ArrayList<EFINBudgetLines>();
    try {
      String query = "select efin_budgetlines_id from efin_budgetlines where efin_budget_id =:budId and c_validcombination_id  in (select c_validcombination_id from efin_budgetinquiry where efin_budgetint_id =:budIntId and ad_client_id =:client)";
      SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery.setParameter("budId", header.getId());
      sqlQuery.setParameter("budIntId", header.getEfinBudgetint().getId());
      sqlQuery.setParameter("client", header.getClient().getId());
      if (sqlQuery.list() != null && sqlQuery.list().size() > 0) {
        return 2;// already having lines in inquiry.
      } else {
        lines = header.getEFINBudgetLinesList();
        for (EFINBudgetLines line : lines) {
          EfinBudgetInquiry inquiry = OBProvider.getInstance().get(EfinBudgetInquiry.class);
          inquiry.setEfinBudgetint(header.getEfinBudgetint());
          inquiry.setBudgetLines(line);
          inquiry.setAccountingCombination(line.getAccountingCombination());
          inquiry.setUniqueCodeName(line.getUniqueCodeName());
          inquiry.setUniqueCode(line.getUniquecode());
          inquiry.setOrganization(line.getOrganization());
          inquiry.setDepartment(line.getSalesRegion());
          inquiry.setAccount(line.getAccountElement());
          inquiry.setSalesCampaign(line.getSalesCampaign());
          inquiry.setProject(line.getProject());
          inquiry.setBusinessPartner(line.getBusinessPartner());
          inquiry.setFunctionalClassfication(line.getActivity());
          inquiry.setFuture1(line.getStDimension());
          inquiry.setNdDimension(line.getNdDimension());
          inquiry.setORGAmt(line.getAmount());
          inquiry.setREVAmount(line.getAmount());
          inquiry.setFundsAvailable(line.getAmount());
          inquiry.setCurrentBudget(line.getAmount());
          inquiry.setBudget(true);
          OBDal.getInstance().save(inquiry);
          OBDal.getInstance().flush();
        }
      }
      return 1; // success
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in insertinquiry lines " + e, e);
        OBDal.getInstance().rollbackAndClose();
        return 0; // failed
      } else {
        return 0;
      }
    }
  }

  /**
   * check budget is having inactive account combination.
   * 
   * @param budget
   * @return
   */
  public static boolean isHavingInactiveAccountCombination(EFINBudget budget) {
    try {
      List<EFINBudgetLines> lineList = new ArrayList<EFINBudgetLines>();
      OBQuery<EFINBudgetLines> lines = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          "efinBudget.id = '" + budget.getId() + "'");
      if (lines.list() != null && lines.list().size() > 0) {
        lineList = lines.list();
      }
      for (EFINBudgetLines line : lineList) {
        if (!line.getAccountingCombination().isActive()) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in isHavingInactiveLine " + e, e);
      }
      return true;
    }
  }

  /**
   * get cost current budget value for funds.
   * 
   * @param line
   * @return
   */
  public static BigDecimal getCostCurrentBudget(EFINBudgetLines line) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      OBQuery<Campaign> budgettype = OBDal.getInstance().createQuery(Campaign.class,
          "efinBudgettype='C'");
      if (budgettype.list() != null && budgettype.list().size() > 0) {
        OBQuery<EfinBudgetInquiry> inq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            "organization.id = ? and department.id=? and account.id=? and businessPartner.id=? and salesCampaign.id=? and project.id=? and functionalClassfication.id=? and future1.id=? and ndDimension.id=? and efinBudgetint.id=?");
        parameters.add(line.getOrganization().getId());
        parameters.add(line.getSalesRegion().getId());
        parameters.add(line.getAccountElement().getId());
        parameters.add(line.getBusinessPartner().getId());
        parameters.add(budgettype.list().get(0));
        parameters.add(line.getProject().getId());
        parameters.add(line.getActivity().getId());
        parameters.add(line.getStDimension().getId());
        parameters.add(line.getNdDimension().getId());
        parameters.add(line.getEfinBudget().getEfinBudgetint().getId());
        inq.setParameters(parameters);
        if (inq.list() != null && inq.list().size() > 0) {
          return inq.list().get(0).getCurrentBudget();
        } else {
          return BigDecimal.ZERO;
        }
      } else {
        return BigDecimal.ZERO;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in getting costcurrent budget " + e, e);
      }
    }
    return BigDecimal.ZERO;
  }

  /**
   * get fundsbudget combiantion for cost.
   * 
   * @param line
   * @return
   */
  public static String getCostsFundsBudget(EFINBudgetLines line) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      OBQuery<Campaign> budgettype = OBDal.getInstance().createQuery(Campaign.class,
          "efinBudgettype='F'");
      if (budgettype.list() != null && budgettype.list().size() > 0) {
        OBQuery<AccountingCombination> inq = OBDal.getInstance().createQuery(
            AccountingCombination.class,
            "organization.id = ? and salesRegion.id=? and account.id=? and businessPartner.id=? and salesCampaign.id=? and project.id=? and activity.id=? and stDimension.id=? and ndDimension.id=? ");
        parameters.add(line.getOrganization().getId());
        parameters.add(line.getSalesRegion().getId());
        parameters.add(line.getAccountElement().getId());
        parameters.add(line.getBusinessPartner().getId());
        parameters.add(budgettype.list().get(0));
        parameters.add(line.getProject().getId());
        parameters.add(line.getActivity().getId());
        parameters.add(line.getStDimension().getId());
        parameters.add(line.getNdDimension().getId());
        inq.setParameters(parameters);
        if (inq.list() != null && inq.list().size() > 0) {
          return inq.list().get(0).getId();
        } else {
          return null;
        }
      } else {
        return null;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in get funds combination for cost. " + e, e);
      }
    }
    return null;
  }

  /**
   * This method is used to check funds request management sequence
   * 
   * @param headerId
   * @return
   */
  public static boolean checkFundsReqMgmtSeq(String headerId) {
    boolean hasSeq = true;
    OBQuery<EFINBudgetLines> budLineQry = null;
    EFINBudget budget = null;
    String AccountDate = "";
    try {
      OBContext.setAdminMode();
      // get Budget Line details with distribute is yes
      budLineQry = OBDal.getInstance().createQuery(EFINBudgetLines.class,
          " as e where e.efinBudget.id='" + headerId
              + "'  and e.amount > 0  and e.distribute='Y' and e.distributionLinkOrg is not null ");
      if (budLineQry != null && budLineQry.list().size() > 0) {
        budget = OBDal.getInstance().get(EFINBudget.class, headerId);
        if (budget != null)
          AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(budget.getTransactionDate());
        OBQuery<Calendar> calendarQuery = OBDal.getInstance().createQuery(Calendar.class,
            "as e where e.organization.id ='0'");
        Calendar calendar = calendarQuery.list().get(0);
        String SequenceNo = Utility.getDocumentSequence(AccountDate, "efin_fundsreq",
            calendar.getId(), "0", true);
        if (SequenceNo.equals("0")) {
          hasSeq = false;
        }
      }
    } catch (Exception e) {
      LOG.error("Exception in checkFundsReqMgmtSeq: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return hasSeq;
  }

  /**
   * Budget Submit, approval process hander.
   * 
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param header
   * @param appstatus
   * @param comments
   * @param vars
   * @param Lang
   * @return
   */
  public static int updateHeaderStatus(String clientId, String orgId, String roleId, String userId,
      final EFINBudget header, String appstatus, final String comments, VariablesSecureApp vars,
      final String Lang) {
    int count = 0;
    boolean isBackwardDelegation = false, isDirectApproval = false;
    String qu_next_role_id = "", delegatedFromRole = null, delegatedToRole = null,
        pendingapproval = "", alertRuleId = "";
    final String alertWindow = AlertWindow.Budget;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    Date currentDate = new Date();
    NextRoleByRuleVO nextApproval = null;
    EutNextRole nextRole = null;
    HashMap<String, String> role = null;
    String fromUser = userId;
    String fromRole = roleId;
    JSONObject fromUserandRoleJson = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    boolean isDummyRole = false;
    Role submittedRoleObj = null;
    String submittedRoleOrgId = null;

    Connection conn = OBDal.getInstance().getConnection();
    try {
      OBContext.setAdminMode();
      // Task #no 8198
      // find the submitted role org/branch details

      if (header.getEUTNextRole() != null) {
        if (header.getEfinSubmittedRole() != null
            && header.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = header.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (header.getEUTNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }
      isDirectApproval = isDirectApproval(header.getId(), roleId);
      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (LOG.isDebugEnabled())
        LOG.debug("queryAlertRule" + queryAlertRule.getWhereAndOrderBy());
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      /**
       * fetching from roleId and userId based on delegated user / forwarder/ direct approver
       **/
      if (header.getEUTNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(header.getEUTNextRole(),
            userId, roleId, clientId, submittedRoleOrgId, Resource.BUDGET_ENTRY_RULE, isDummyRole,
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

      if (header.getEUTNextRole() == null) {
        nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
            submittedRoleOrgId, fromRole, fromUser, Resource.BUDGET_ENTRY_RULE, 0.00);
      } else {

        if (isDirectApproval) {
          nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
              submittedRoleOrgId, fromRole, fromUser, Resource.BUDGET_ENTRY_RULE, 0.00);
          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    userRole.list().get(0).getUserContact().getId(), Resource.BUDGET_ENTRY_RULE,
                    "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    delegatedFromRole, delegatedToRole, fromUser, Resource.BUDGET_ENTRY_RULE, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                submittedRoleOrgId, delegatedFromRole, fromUser, Resource.BUDGET_ENTRY_RULE, 0.00);
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromUser, Resource.BUDGET_ENTRY_RULE, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.BUDGET_ENTRY_RULE, 0.00);
        }
      }
      if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(header.getEUTNextRole(), Resource.BUDGET_ENTRY_RULE);

        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        if (header.getAlertStatus().equals("REW") || header.getAlertStatus().equals("OP")
            || header.getAlertStatus().equals("Draft")) {
          header.setRevoke(true);
        } else
          header.setRevoke(false);
        header.setAlertStatus("INAPP");
        header.setEUTNextRole(nextRole);
        header.setSubmit(true);
        pendingapproval = nextApproval.getStatus();
        OBDal.getInstance().save(header);

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        forwardDao.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, header.getDocumentNo(), Lang, vars.getRole(),
            header.getEUTForwardReqmoreinfo(), Resource.BUDGET_ENTRY_RULE, alertReceiversMap);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          /*
           * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
           * "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'"); if
           * (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
           * objAlert.setAlertStatus("SOLVED"); } }
           */

          String description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.Budget.wfa", Lang) + " " + header.getCreatedBy().getName();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            try {
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                  objNextRoleLine.getRole().getId(),
                  (objNextRoleLine.getUserContact() == null ? ""
                      : objNextRoleLine.getUserContact().getId()),
                  header.getClient().getId(), description, "NEW", alertWindow, "finance.Budget.wfa",
                  Constants.GENERIC_TEMPLATE);
            } catch (Exception e) {

            }
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_103'");
            if (delegationln != null && delegationln.list().size() > 0) {
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                  delegationln.list().get(0).getRole().getId(),
                  delegationln.list().get(0).getUserContact().getId(), header.getClient().getId(),
                  description, "NEW", alertWindow, "finance.Budget.wfa",
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
        if (pendingapproval == null) {
          pendingapproval = nextApproval.getStatus();
        }
        count = 1;
      } else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        Role objCreatedRole = null;
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(header.getEUTNextRole(), Resource.BUDGET_ENTRY_RULE);

        if (header.getCreatedBy().getADUserRolesList().size() > 0) {
          objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        forwardDao.getAlertForForwardedUser(header.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, header.getDocumentNo(), Lang, vars.getRole(),
            header.getEUTForwardReqmoreinfo(), Resource.BUDGET_ENTRY_RULE, alertReceiversMap);

        // delete alert for approval alerts
        /*
         * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
         * "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'"); if
         * (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
         * objAlert.setAlertStatus("SOLVED"); } }
         */
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
        String description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.Budget.approved", Lang) + " " + objUser.getName();
        try {
          AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
              header.getRole().getId(), header.getCreatedBy().getId(), header.getClient().getId(),
              description, "NEW", alertWindow, "finance.Budget.approved",
              Constants.GENERIC_TEMPLATE);
        } catch (Exception e) {

        }
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setAlertStatus("APP");
        header.setRevoke(false);
        header.setEUTNextRole(nextRole);
        header.setSubmit(true);
        OBDal.getInstance().save(header);
        // insert lines in budget inquiry in final approval
        count = BudgetDAO.insertInquiryLines(header);
        if (count == 1) {
          FundsRequestActionDAO.directDistribute(conn, "BUD", header.getId(), vars, clientId,
              roleId);
        }
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("headerId:" + header.getId());
      }
      if (!StringUtils.isEmpty(header.getId())) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", header.getId());
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.Budget_History);
        historyData.put("HeaderColumn", ApprovalTables.Budget_History_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Budget_History_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);
      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.BUDGET_ENTRY_RULE);

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

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Budget revoke process handler.
   * 
   * @param header
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param comments
   * @param appstatus
   * @param vars
   * @param Lang
   * @return
   */
  public static int updateRevokeStatus(EFINBudget header, String clientId, String orgId,
      String roleId, String userId, String comments, String appstatus, VariablesSecureApp vars,
      final String Lang) {
    int count = 0;
    EutNextRole nextRole = null;
    String alertRuleId = "", alertWindow = AlertWindow.Budget, status = appstatus;
    Date currentDate = new Date();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

    try {
      OBContext.setAdminMode();
      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (LOG.isDebugEnabled()) {
        LOG.debug("queryAlertRule" + queryAlertRule.getWhereAndOrderBy());
      }
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }
      ArrayList<String> includeRecipient = new ArrayList<String>();

      nextRole = header.getEUTNextRole();

      // get alert recipient
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

      // set alerts for next roles
      if (nextRole.getEutNextRoleLineList().size() > 0) {
        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.referenceSearchKey='" + header.getId() + "' and e.alertStatus='NEW'");
        if (alertQuery.list().size() > 0) {
          for (Alert objAlert : alertQuery.list()) {
            objAlert.setAlertStatus("SOLVED");
          }
        }

        String description = sa.elm.ob.finance.properties.Resource
            .getProperty("finance.Budget.revoked", Lang) + " " + header.getCreatedBy().getName();

        for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
          try {
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                header.getClient().getId(), description, "NEW", alertWindow,
                "finance.Budget.revoked", Constants.GENERIC_TEMPLATE);
          } catch (Exception e) {

          }
          // get user name for delegated user to insert on approval history.
          OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
              EutDocappDelegateln.class,
              " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                  + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                  + "' and hd.date >='" + currentDate + "' and e.documentType='EUT_103'");
          if (delegationln != null && delegationln.list().size() > 0) {
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

      header.setUpdated(new java.util.Date());
      header.setUpdatedBy(OBContext.getOBContext().getUser());
      header.setAlertStatus("OP");
      header.setSubmit(false);
      header.setEUTNextRole(null);
      if (LOG.isDebugEnabled()) {
        LOG.debug("header:" + header.toString());
      }
      OBDal.getInstance().save(header);
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.BUDGET_ENTRY_RULE);

      if (!StringUtils.isEmpty(header.getId())) {
        status = "REV";

        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", header.getId());
        historyData.put("Comments", comments);
        historyData.put("Status", status);
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.Budget_History);
        historyData.put("HeaderColumn", ApprovalTables.Budget_History_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Budget_History_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);
        count = 1;
      }
      // Removing Forward and RMI Id
      if (header.getEUTForwardReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
        forwardDao.revokeRemoveForwardRmiFromWindows(header.getId(), Constants.BUDGET);
      }
      if (header.getEUTReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
        forwardDao.revokeRemoveRmiFromWindows(header.getId(), Constants.BUDGET);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in updateRevokeStatus in Budget: ", e);
      }
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Check role is next approver in document rule.
   * 
   * @param BudegtId
   * @param roleId
   * @return
   */
  private static boolean isDirectApproval(final String BudegtId, final String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(bu.efin_budget_id) from efin_budget bu join eut_next_role rl on "
          + "bu.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and bu.efin_budget_id = ? and li.ad_role_id =?";

      ps = con.prepareStatement(query);
      ps.setString(1, BudegtId);
      ps.setString(2, roleId);

      rs = ps.executeQuery();

      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      } else
        return false;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in isDirectApproval " + e.getMessage());
      }
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

  /**
   * Check is current roleis delagated to someone or not.
   * 
   * @param roleId
   * @return true, if delegated.
   */
  public static boolean isDelegatedRole(final String roleId) {
    Connection con = OBDal.getInstance().getConnection();
    String sql = "";
    ResultSet rs = null;
    Date currentDate = new Date();

    try {
      PreparedStatement st = null;
      sql = "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
          + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_103'";
      st = con.prepareStatement(sql);
      rs = st.executeQuery();
      while (rs.next()) {
        String roleid = rs.getString("ad_role_id");
        if (roleid.equals(roleId)) {
          return true;
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in isDelegatedRole " + e.getMessage());
      }
      return false;
    }
    return false;
  }

}