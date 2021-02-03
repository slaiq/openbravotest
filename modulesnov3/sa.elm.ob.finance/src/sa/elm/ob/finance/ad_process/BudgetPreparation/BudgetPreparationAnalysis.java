package sa.elm.ob.finance.ad_process.BudgetPreparation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgPrepLines;
import sa.elm.ob.finance.EfinBudgetPreparation;

/**
 * @author Gopalakrishnan on 13/06/2016
 */

public class BudgetPreparationAnalysis extends DalBaseProcess {

  /**
   * BudgetRevision Transaction submit Tracking on Budget Transfer Table(efin_budget_transfer)
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetPreparationAnalysis.class);

  @SuppressWarnings("resource")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps1 = null;
    ResultSet rs = null;
    String query = "", query1 = "", lineQuery = "";
    // Create carry Forward
    log.debug("entering into BudgetRevision");
    try {
      OBContext.setAdminMode();
      String BudPreId = (String) bundle.getParams().get("Efin_Budget_Preparation_ID");
      EfinBudgetPreparation ObjBudPrep = OBDal.getInstance().get(EfinBudgetPreparation.class,
          BudPreId);
      String strAcctId = ObjBudPrep.getAccountElement().getId();
      String strBudType = ObjBudPrep.getSalesCampaign().getId();
      String strYear = ObjBudPrep.getYear().getFiscalYear();
      final String clientId = (String) bundle.getContext().getClient();
      long lineNo = 0;
      Boolean alreadyexists = false;
      Boolean budgetExists = false;

      /*
       * Check if the budget present for the same year,budget type, accounting element
       */

      lineNo = 10;
      query = " select  distinct ln.uniquecode,ln.ad_org_id,ln.c_salesregion_id,ln.c_elementvalue_id, "
          + " ln.c_campaign_id,ln.user1_id,ln.c_activity_id,ln.c_project_id,ln.user2_id from efin_budget bug "
          + "   join c_year yr on yr.c_year_id=bug.c_year_id "
          + "	join efin_budgetlines ln on ln.efin_budget_id= bug.efin_budget_id "
          + "	where bug.c_elementvalue_id ='" + strAcctId + "' and ln.c_campaign_id='"
          + strBudType + "' "
          + "	and bug.status='APP' and bug.isactive='Y'  and (to_number(year) < to_number('"
          + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-3))";
      log.debug("query for last three years:" + query.toString());
      ps = conn.prepareStatement(query);
      rs = ps.executeQuery();

      // check budget lines exists
      if (rs.next()) {
        budgetExists = true;
      }

      /*
       * OBQuery<EFINBudget> budgetlist = OBDal.getInstance().createQuery(EFINBudget.class,
       * "as e where e.accountElement.id='" + strAcctId + "' and e.year.id='" + strYearId +
       * "' and e.salesCampaign.id='" + strBudType + "'"); if(budgetlist.list().size() > 0) {
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", "@Efin_BudgetPrep_Exists@"); bundle.setResult(result); return; }
       */
      if (budgetExists) {
        /*
         * Bring last three year budget lines(Unique code Combinations)
         */
        lineQuery = "SELECT COALESCE(MAX(LINE),0)+10 AS lineno FROM EFIN_BUDGPREP_LINES WHERE EFIN_BUDGET_PREPARATION_ID='"
            + ObjBudPrep.getId() + "'";
        ps1 = conn.prepareStatement(lineQuery);
        rs = ps1.executeQuery();
        if (rs.next()) {
          lineNo = rs.getLong("lineno");
        }
        query = " select  distinct ln.uniquecode,ln.ad_org_id,ln.c_salesregion_id,ln.c_elementvalue_id, "
            + " ln.c_campaign_id,ln.user1_id,ln.c_activity_id,ln.c_project_id,ln.user2_id from efin_budget bug "
            + "   join c_year yr on yr.c_year_id=bug.c_year_id "
            + "	join efin_budgetlines ln on ln.efin_budget_id= bug.efin_budget_id "
            + "	where bug.c_elementvalue_id ='" + strAcctId + "' and ln.c_campaign_id='"
            + strBudType + "' "
            + "	and bug.status='APP' and bug.isactive='Y'  and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-3))"
            + " and ln.uniquecode not in (select uniquecode from efin_budgprep_lines where efin_budget_preparation_id ='"
            + ObjBudPrep.getId() + "' ) ";
        log.debug("query for last three years:" + query.toString());
        ps = conn.prepareStatement(query);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines prepLines = OBProvider.getInstance().get(EfinBudgPrepLines.class);
          prepLines.setLineNo(lineNo);
          prepLines.setEfinBudgetPreparation(ObjBudPrep);
          prepLines.setClient(OBDal.getInstance().get(Client.class, clientId));
          prepLines.setActive(true);
          prepLines.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          prepLines.setCreationDate(new java.util.Date());
          prepLines.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          prepLines.setUpdated(new java.util.Date());
          prepLines.setUniqueCode(rs.getString("uniquecode"));
          prepLines.setOrganization(
              OBDal.getInstance().get(Organization.class, rs.getString("ad_org_id")));
          prepLines.setSalesRegion(
              OBDal.getInstance().get(SalesRegion.class, rs.getString("c_salesregion_id")));
          prepLines.setAccountElement(
              OBDal.getInstance().get(ElementValue.class, rs.getString("c_elementvalue_id")));
          prepLines.setSalesCampaign(
              OBDal.getInstance().get(Campaign.class, rs.getString("c_campaign_id")));
          prepLines.setStDimension(
              OBDal.getInstance().get(UserDimension1.class, rs.getString("user1_id")));
          prepLines.setNdDimension(
              OBDal.getInstance().get(UserDimension2.class, rs.getString("user2_id")));
          prepLines.setActivity(
              OBDal.getInstance().get(ABCActivity.class, rs.getString("c_activity_id")));
          prepLines
              .setProject(OBDal.getInstance().get(Project.class, rs.getString("c_project_id")));
          lineNo += 10;
          alreadyexists = true;
          OBDal.getInstance().save(prepLines);
        }
        OBDal.getInstance().flush();

        if (!alreadyexists) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "warning",
              "@Efin_Budget_preparationlinesAdded@");
          bundle.setResult(result);
          return;
        }
        /*
         * Calculation Of Current Budget amount
         */

        /*
         * bring last three year budget avg
         */

        query1 = " select main.efin_budgprep_lines_id,sum(main.avg) as avg from (select prln.efin_budgprep_lines_id, round((sum(ln.current_budget)/3),2) as avg "
            + " from efin_budgprep_lines prln join efin_budgetlines ln on prln.uniquecode=ln.uniquecode  join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id  where bu.c_elementvalue_id ='"
            + strAcctId + "' and ln.c_campaign_id='" + strBudType + "' "
            + " and bu.status='APP' and bu.isactive='Y' and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-3))  "
            + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "' group by prln.efin_budgprep_lines_id,ln.uniquecode ,yr.c_year_id )main "
            + " group by main.efin_budgprep_lines_id ";
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        log.debug("last three year udpate query:" + query1);
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setThirdyearamt(new BigDecimal(rs.getFloat("avg")));
          OBDal.getInstance().save(lines);
        }

        /*
         * bring last two years budget avg
         */
        query1 = " select main.efin_budgprep_lines_id,sum(main.avg) as avg from (select prln.efin_budgprep_lines_id, round((sum(ln.current_budget)/2),2) as avg "
            + " from efin_budgprep_lines prln join efin_budgetlines ln on prln.uniquecode=ln.uniquecode  join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id  where bu.c_elementvalue_id ='"
            + strAcctId + "' and ln.c_campaign_id='" + strBudType + "' "
            + " and bu.status='APP' and bu.isactive='Y' and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-2))  "
            + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "' group by prln.efin_budgprep_lines_id,ln.uniquecode ,yr.c_year_id )main "
            + " group by main.efin_budgprep_lines_id ";
        log.debug("last two year avg:" + query1);
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setSecondyearamt(new BigDecimal(rs.getFloat("avg")));
          OBDal.getInstance().save(lines);
        }

        /*
         * bring last year budget amount
         */
        query1 = "select prln.efin_budgprep_lines_id,ln.current_budget as amount from efin_budgprep_lines prln "
            + " join efin_budgetlines ln on prln.uniquecode=ln.uniquecode join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id where bu.c_elementvalue_id ='"
            + strAcctId + "' " + " and ln.c_campaign_id='" + strBudType
            + "' and bu.status='APP' and bu.isactive='Y' " + " and to_number(year) = (to_number('"
            + strYear + "')-1) " + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "'	 ";
        log.debug("last year amot:" + query1);
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setLastyearamt(new BigDecimal(rs.getFloat("amount")));
          OBDal.getInstance().save(lines);
        }

        /*
         * Calculation Of funds available amount
         */

        /*
         * bring last three year funds available Avg
         */

        query1 = " select main.efin_budgprep_lines_id,sum(main.avg) as avg from (select prln.efin_budgprep_lines_id, round((sum(ln.funds_available)/3),2) as avg "
            + " from efin_budgprep_lines prln join efin_budgetlines ln on prln.uniquecode=ln.uniquecode  join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id  where bu.c_elementvalue_id ='"
            + strAcctId + "' and ln.c_campaign_id='" + strBudType + "' "
            + " and bu.status='APP' and bu.isactive='Y' and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-3))  "
            + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "' group by prln.efin_budgprep_lines_id,ln.uniquecode ,yr.c_year_id )main "
            + " group by main.efin_budgprep_lines_id ";
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        log.debug("last three year funds amt:" + query1);
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setThreeyearfa(new BigDecimal(rs.getFloat("avg")));
          OBDal.getInstance().save(lines);
        }

        /*
         * bring last two years funds available Avg
         */
        query1 = " select main.efin_budgprep_lines_id,sum(main.avg) as avg from (select prln.efin_budgprep_lines_id, round((sum(ln.funds_available)/2),2) as avg "
            + " from efin_budgprep_lines prln join efin_budgetlines ln on prln.uniquecode=ln.uniquecode  join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id  where bu.c_elementvalue_id ='"
            + strAcctId + "' and ln.c_campaign_id='" + strBudType + "' "
            + " and bu.status='APP' and bu.isactive='Y' and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-2))  "
            + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "' group by prln.efin_budgprep_lines_id,ln.uniquecode ,yr.c_year_id )main "
            + " group by main.efin_budgprep_lines_id ";
        log.debug("last two year funds amt:" + query1);
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setTwoyearfa(new BigDecimal(rs.getFloat("avg")));
          OBDal.getInstance().save(lines);
        }

        /*
         * bring last year funds available
         */
        query1 = "select prln.efin_budgprep_lines_id,ln.funds_available as amount from efin_budgprep_lines prln "
            + " join efin_budgetlines ln on prln.uniquecode=ln.uniquecode join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id where bu.c_elementvalue_id ='"
            + strAcctId + "' " + " and ln.c_campaign_id='" + strBudType
            + "' and bu.status='APP' and bu.isactive='Y' " + " and to_number(year) = (to_number('"
            + strYear + "')-1) " + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "'	 ";
        log.debug("last year funds amt:" + query1);
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setLastyearfa(new BigDecimal(rs.getFloat("amount")));
          OBDal.getInstance().save(lines);
        }

        /*
         * Calculation Of Actual amount
         */

        /*
         * bring last three year Actual amount Avg
         */

        query1 = " select main.efin_budgprep_lines_id,sum(main.avg) as avg from (select prln.efin_budgprep_lines_id, round((sum(ln.amount_spent)/3),2) as avg "
            + " from efin_budgprep_lines prln join efin_budgetlines ln on prln.uniquecode=ln.uniquecode  join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id  where bu.c_elementvalue_id ='"
            + strAcctId + "' and ln.c_campaign_id='" + strBudType + "' "
            + " and bu.status='APP' and bu.isactive='Y' and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-3))  "
            + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "' group by prln.efin_budgprep_lines_id,ln.uniquecode ,yr.c_year_id )main "
            + " group by main.efin_budgprep_lines_id ";
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        log.debug("last three year actual amt:" + query1);
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setThreeyearactual(new BigDecimal(rs.getFloat("avg")));
          OBDal.getInstance().save(lines);
        }

        /*
         * bring last two years Actual amount Avg
         */
        query1 = " select main.efin_budgprep_lines_id,sum(main.avg) as avg from (select prln.efin_budgprep_lines_id, round((sum(ln.amount_spent)/2),2) as avg "
            + " from efin_budgprep_lines prln join efin_budgetlines ln on prln.uniquecode=ln.uniquecode  join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id  where bu.c_elementvalue_id ='"
            + strAcctId + "' and ln.c_campaign_id='" + strBudType + "' "
            + " and bu.status='APP' and bu.isactive='Y' and (to_number(year) < to_number('"
            + strYear + "') and to_number(year) >= (to_number('" + strYear + "')-2))  "
            + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "' group by prln.efin_budgprep_lines_id,ln.uniquecode ,yr.c_year_id )main "
            + " group by main.efin_budgprep_lines_id ";
        log.debug("last two year Actual avg:" + query1);
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setTwoyearactual(new BigDecimal(rs.getFloat("avg")));
          OBDal.getInstance().save(lines);
        }

        /*
         * bring last year Actual amount
         */
        query1 = "select prln.efin_budgprep_lines_id,ln.amount_spent as amount from efin_budgprep_lines prln "
            + " join efin_budgetlines ln on prln.uniquecode=ln.uniquecode join efin_budget bu on bu.efin_budget_id=ln.efin_budget_id "
            + " join c_year yr on yr.c_year_id=bu.c_year_id where bu.c_elementvalue_id ='"
            + strAcctId + "' " + " and ln.c_campaign_id='" + strBudType
            + "' and bu.status='APP' and bu.isactive='Y' " + " and to_number(year) = (to_number('"
            + strYear + "')-1) " + " and prln.efin_budget_preparation_id='" + ObjBudPrep.getId()
            + "'	 ";
        log.debug("last year actual amt:" + query1);
        ps = conn.prepareStatement(query1);
        rs = ps.executeQuery();
        while (rs.next()) {
          EfinBudgPrepLines lines = OBDal.getInstance().get(EfinBudgPrepLines.class,
              rs.getString("efin_budgprep_lines_id"));
          lines.setLastyearactual(new BigDecimal(rs.getFloat("amount")));
          OBDal.getInstance().save(lines);
        }
        OBDal.getInstance().flush();
      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "warning",
            "@Efin_BudgetPreparation_NoBudget@");
        bundle.setResult(result);
        return;
      }
      OBDal.getInstance().commitAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "success",
          "Process Completed Successfully");
      bundle.setResult(result);
    } catch (Exception e) {
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
        if (ps1 != null) {
          ps1.close();
        }
      } catch (Exception e) {
        log.error("Exception in BudgetPreparationAnalysis : " + e);
      }

      OBContext.restorePreviousMode();
    }

  }

}
