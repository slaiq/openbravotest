package sa.elm.ob.finance.ad_process.BudgetPreparation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EfinBudgPrepLines;
import sa.elm.ob.finance.EfinBudgetPreparation;
import sa.elm.ob.finance.EfinBudgetPreparationHistory;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;

/**
 * @author Gopalakrishnan on 14/06/2016
 */

public class BudgetPreparationSubmit extends DalBaseProcess {

  /**
   * BudgetPreparationSubmit Transaction submit Tracking on Budget preparation History
   * Table(Efin_Budget_Preparation)
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetPreparationSubmit.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    Connection connection = null;
    PreparedStatement ps = null;
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    ResultSet rs = null, rs1 = null, rs2 = null, rs3 = null;
    String query = "";
    String errorMsg = "";
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String appstatus = "", pendingapproval = "";
    final String strPreparationId = (String) bundle.getParams().get("Efin_Budget_Preparation_ID")
        .toString();
    String comments = (String) bundle.getParams().get("comments").toString();
    EfinBudgetPreparation objBudgetPrep = OBDal.getInstance().get(EfinBudgetPreparation.class,
        strPreparationId);
    String strAcctId = objBudgetPrep.getAccountElement().getId();
    String strBudType = objBudgetPrep.getSalesCampaign().getId();
    String strYearId = objBudgetPrep.getYear().getId();
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objBudgetPrep.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    int count = 0;
    Boolean errorFlag = false;
    boolean allowUpdate = false;
    // Create carry Forward
    log.debug("entering into BudgetPreparationSubmit");
    try {
      OBContext.setAdminMode(true);

      OBQuery<EFINBudget> budgetlist = OBDal.getInstance().createQuery(EFINBudget.class,
          " as e where e.accountElement.id= :accountElementID and e.year.id= :yearID "
              + " and e.salesCampaign.id= :salesCampaignID ");
      budgetlist.setNamedParameter("accountElementID", strAcctId);
      budgetlist.setNamedParameter("yearID", strYearId);
      budgetlist.setNamedParameter("salesCampaignID", strBudType);
      if (budgetlist.list().size() > 0) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_BudgetPrep_Exists@");
        bundle.setResult(result);
        return;
      }

      // check whether All the subaccount(s) under selected Account Group in budget preparation are
      // added in lines.
      OBQuery<EfinBudgPrepLines> lines = OBDal.getInstance().createQuery(EfinBudgPrepLines.class,
          " as e where e.efinBudgetPreparation.id = :efinBudgetPreparationID order by e.accountElement.id desc");
      lines.setNamedParameter("efinBudgetPreparationID", strPreparationId);
      List<EfinBudgPrepLines> linesList = lines.list();
      LinkedHashSet<String> elementset = new LinkedHashSet<String>();
      LinkedHashSet<String> Lineset = new LinkedHashSet<String>();

      EfinBudgetPreparation budgetPrep = OBDal.getInstance().get(EfinBudgetPreparation.class,
          strPreparationId);
      if (linesList.size() > 0) {
        for (int i = 0; i < linesList.size(); i++) {
          // Check whether inactive accounts added in budgetpreplines
          ElementValue activeElement = OBDal.getInstance().get(ElementValue.class,
              linesList.get(i).getAccountElement().getId());
          if (activeElement.isActive().equals(true)) {
            Lineset.add(linesList.get(i).getAccountElement().getId());
          } else {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_BudgetPrepline_Element_inactive@");
            bundle.setResult(result);
            return;
          }
        }
      }
      SQLQuery accountelement = OBDal.getInstance().getSession()
          .createSQLQuery(" select c_elementvalue_id from ad_treenode "
              + " join c_elementvalue el on el.c_elementvalue_id = ad_treenode.node_id "
              + " where parent_id = :parentID and el.isactive='Y' order by c_elementvalue_id desc ");
      accountelement.setParameter("parentID", budgetPrep.getAccountElement().getId());
      for (int i = 0; i < accountelement.list().size(); i++) {
        elementset.add(accountelement.list().get(i).toString());
      }
      Boolean Check = elementset.equals(Lineset);
      if (Check == false) {
        errorFlag = false;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Prep_AccountGroup_Valid@");
        bundle.setResult(result);
        return;
      }

      // Checking Cost Budget Available for Year With Same accounting Element
      if (!objBudgetPrep.getSalesCampaign().isEfinIscarryforward()) {
        query = "select bud.efin_budget_id from efin_budget bud "
            + "join c_campaign typ on typ.c_campaign_id=bud.c_campaign_id"
            + " where  typ.em_efin_iscarryforward ='Y' and bud.c_elementvalue_id='"
            + objBudgetPrep.getAccountElement().getId() + "' and bud.c_year_id='"
            + objBudgetPrep.getYear().getId() + "' ";
        log.debug("CostBudgetQuery:" + query.toString());
        ps = connection.prepareStatement(query);
        rs = ps.executeQuery();
        if (rs.next()) {

          // Process the correct FundsBudget lines
          query = "select funds.efin_budgprep_lines_id,funds.amount as fundsamount ,cos.amount as costamount"
              + " from efin_budgprep_lines funds join (select efin_budgetlines_id,ad_org_id,c_salesregion_id,user2_id,user1_id,c_activity_id,c_elementvalue_id,c_campaign_id,c_project_id,amount from efin_budgetlines where efin_budget_id ='"
              + rs.getString("efin_budget_id")
              + "') cos on (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id = funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.c_elementvalue_id||funds.c_project_id ) "
              + " where  1=1  and funds.amount <= cos.amount "
              + " and (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id = funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.c_elementvalue_id||funds.c_project_id ) "
              + " and funds.efin_budget_preparation_id='" + objBudgetPrep.getId() + "'";
          log.debug("CorrectLineQuery:" + query.toString());
          ps = connection.prepareStatement(query);
          rs3 = ps.executeQuery();
          while (rs3.next()) {
            EfinBudgPrepLines FundsBudgetLines = OBDal.getInstance().get(EfinBudgPrepLines.class,
                rs3.getString("efin_budgprep_lines_id"));
            FundsBudgetLines.setCheckingStaus("SCS");
            FundsBudgetLines.setFailureReason(null);
            // all Correct Combination Successfully Updated
          }

          // Checking Funds Budget Extra Account Combination from Cost Budget
          query = " select funds.efin_budgprep_lines_id from efin_budgprep_lines funds "
              + " where (funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.c_elementvalue_id||funds.c_project_id)  not in ( select (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id ) from efin_budgetlines cos where cos.efin_budget_id ='"
              + rs.getString("efin_budget_id") + "' " + " ) and funds.efin_budget_preparation_id ='"
              + objBudgetPrep.getId() + "' ";
          log.debug("extraLinesQuery:" + query.toString());
          ps = connection.prepareStatement(query);
          rs1 = ps.executeQuery();
          while (rs1.next()) {
            EfinBudgPrepLines FundsBudgetLines = OBDal.getInstance().get(EfinBudgPrepLines.class,
                rs1.getString("efin_budgprep_lines_id"));
            FundsBudgetLines.setCheckingStaus("FL");
            FundsBudgetLines.setFailureReason("Combination doesn't exists in '"
                + objBudgetPrep.getYear().getFiscalYear() + "'' cost budget");
            errorFlag = true;
            errorMsg = OBMessageUtils.messageBD("Efin_BudgetPre_ChkStatus");
          }

          // Checking Funds Budget lines amount exceed from Cost Budget lines amount
          query = "select funds.efin_budgprep_lines_id,funds.amount as fundsamount ,cos.amount as costamount"
              + " from efin_budgprep_lines funds join (select efin_budgetlines_id,ad_org_id,c_salesregion_id,user2_id,user1_id,c_activity_id,c_elementvalue_id,c_campaign_id,c_project_id,amount from efin_budgetlines where efin_budget_id ='"
              + rs.getString("efin_budget_id")
              + "') cos on (cos.ad_org_id||cos.c_salesregion_id||cos.user2_id||cos.user1_id||cos.c_activity_id||cos.c_elementvalue_id||cos.c_project_id = funds.ad_org_id||funds.c_salesregion_id||funds.user2_id||funds.user1_id||funds.c_activity_id||funds.c_elementvalue_id||funds.c_project_id ) "
              + " where  1=1  and funds.amount > cos.amount "
              + " and funds.efin_budget_preparation_id='" + objBudgetPrep.getId() + "'";
          log.debug("extraAmountQury:" + query.toString());
          ps = connection.prepareStatement(query);
          rs2 = ps.executeQuery();
          while (rs2.next()) {
            EfinBudgPrepLines FundsBudgetLines = OBDal.getInstance().get(EfinBudgPrepLines.class,
                rs2.getString("efin_budgprep_lines_id"));
            FundsBudgetLines.setCheckingStaus("FL");
            FundsBudgetLines.setFailureReason(
                "Amount is greater than cost budget amount " + rs2.getString("costamount") + " ");
            errorFlag = true;

            errorMsg = OBMessageUtils.messageBD("Efin_BudgetPre_ChkStatus");
          }

        }

        OBDal.getInstance().flush();
      }

      // check current role is present in document rule or not
      if (budgetPrep.getNextRole() != null) {
        java.util.List<EutNextRoleLine> li = budgetPrep.getNextRole().getEutNextRoleLineList();
        for (int i = 0; i < li.size(); i++) {
          String role = li.get(i).getRole().getId();
          if (roleId.equals(role)) {
            allowUpdate = true;
          }
        }

      }

      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(budgetPrep.getCreatedBy().getId()))
          && budgetPrep.getAlertStatus().equals("RW")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      // After Revoked by submiter if approver is try to Approve the same record then throw error
      if ((!vars.getUser().equals(budgetPrep.getCreatedBy().getId()))
          && budgetPrep.getAlertStatus().equals("O")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      if (!errorFlag) {
        NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(
            OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
            Resource.BUDGET_PREPARATION_RULE, 0.00);
        EutNextRole nextRole = null;
        if (objBudgetPrep.getAlertStatus().equals("APP")
            || objBudgetPrep.getAlertStatus().equals("IA")) {
          appstatus = "APP";
        } else if (objBudgetPrep.getAlertStatus().equals("O")
            | objBudgetPrep.getAlertStatus().equals("RW")) {
          appstatus = "SUB";
          allowUpdate = true;
        }

        if (allowUpdate) {

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            objBudgetPrep.setUpdated(new java.util.Date());
            objBudgetPrep.setUpdatedBy(OBContext.getOBContext().getUser());
            if (objBudgetPrep.getAlertStatus().equals("RW")
                || objBudgetPrep.getAlertStatus().equals("O")) {
              objBudgetPrep.setBudgetprepareRevoke(true);
            } else
              objBudgetPrep.setBudgetprepareRevoke(false);
            objBudgetPrep.setAlertStatus("IA");
            objBudgetPrep.setNextRole(nextRole);
            objBudgetPrep.setAction("AP");
            pendingapproval = nextApproval.getStatus();
          } else {
            objBudgetPrep.setUpdated(new java.util.Date());
            objBudgetPrep.setUpdatedBy(OBContext.getOBContext().getUser());
            objBudgetPrep.setAlertStatus("APP");
            objBudgetPrep.setAction("PD");
            // objBudgetPrep.setRevoke(false);
            objBudgetPrep.setNextRole(nextRole);
          }
          OBDal.getInstance().save(objBudgetPrep);

          if (!StringUtils.isEmpty(objBudgetPrep.getId())) {
            count = insertBudgetPreparationHistory(OBDal.getInstance().getConnection(), clientId,
                orgId, roleId, userId, objBudgetPrep, comments, appstatus, pendingapproval);
            if (count == 0) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
              bundle.setResult(result);
              return;
            }
          }
        } else {
          errorFlag = true;
          errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
          throw new OBException(errorMsg);
        }

      }
      if (errorFlag == false && !StringUtils.isEmpty(objBudgetPrep.getId())) {
        obError.setType("Success");
        obError.setTitle("Success");
        obError.setMessage(OBMessageUtils.messageBD("Efin_BudgetPre_Submit"));
      } else if (errorFlag == true) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.BUDGET_PREPARATION_RULE);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to insert budget preparation history
   * 
   * @param con
   *          Establish the server connection
   * @param clientId
   *          Client of current record
   * @param orgId
   *          Organization of current record
   * @param roleId
   *          Current session Role
   * @param userId
   *          current session user
   * @param objBudgetPrep
   *          Budget Preparation Object
   * @param comments
   * @param appstatus
   * @return 1 if the process has been successfully insert the record in history table.
   * 
   */
  public static int insertBudgetPreparationHistory(Connection con, String clientId, String orgId,
      String roleId, String userId, EfinBudgetPreparation objBudgetPrep, String comments,
      String appstatus, String pendingapproval) {
    String histId = null;
    try {
      OBContext.setAdminMode(true);
      EfinBudgetPreparationHistory hist = OBProvider.getInstance()
          .get(EfinBudgetPreparationHistory.class);
      hist.setClient(OBDal.getInstance().get(Client.class, clientId));
      hist.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
      hist.setActive(true);
      hist.setCreatedBy(OBDal.getInstance().get(User.class, userId));
      hist.setCreationDate(new java.util.Date());
      hist.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
      hist.setUpdated(new java.util.Date());
      hist.setEfinBudgetPreparation(objBudgetPrep);
      hist.setBudgetprepAction(appstatus);
      hist.setApprovedDate(new java.util.Date());
      hist.setComments(comments);
      hist.setPendingapproval(pendingapproval);
      OBDal.getInstance().save(hist);
      histId = hist.getId();
      if (histId != null)
        return 1;
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in insertBudgetApprover: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

}
