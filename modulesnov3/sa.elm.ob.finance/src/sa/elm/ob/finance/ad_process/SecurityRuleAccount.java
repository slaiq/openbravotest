package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.finance.EfinSecurityRulesAct;
import sa.elm.ob.finance.EfinSecurityRulesFutureOne;
import sa.elm.ob.finance.EfinSecurityRulesactiv;
import sa.elm.ob.finance.EfinSecurityRulesbpartner;
import sa.elm.ob.finance.EfinSecurityRulesbudg;
import sa.elm.ob.finance.EfinSecurityRulesdept;
import sa.elm.ob.finance.EfinSecurityRulesfutureTwo;
import sa.elm.ob.finance.EfinSecurityRuleslines;
import sa.elm.ob.finance.EfinSecurityRulesproj;
import sa.elm.ob.finance.EfinSecurityactpickV;
import sa.elm.ob.finance.EfinSecuritydeptpickV;
import sa.elm.ob.finance.EfinSecurityprojpickV;
import sa.elm.ob.finance.SecurityRuleBudgetType;
import sa.elm.ob.finance.SecurityRuleDepartment;
import sa.elm.ob.finance.SecurityRuleFutureOne;
import sa.elm.ob.finance.SecurityRuleFutureSecond;
import sa.elm.ob.finance.SecurityRuleProject;
import sa.elm.ob.finance.SecurityRulesActivity;
import sa.elm.ob.finance.SecurityRulesBpartner;

/**
 * @author Qualian on 29/06/2106
 */

public class SecurityRuleAccount extends DalBaseProcess {

  /**
   * Create account Entries in Security Rule Account Tab Table(efin_security_rules_act)
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(SecurityRuleAccount.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = "";
    ArrayList<String> includeList = new ArrayList<String>();
    ArrayList<String> includeCodeList = new ArrayList<String>();
    ArrayList<String> excludeList = new ArrayList<String>();
    HashMap<String, String> map = new HashMap<String, String>();
    // Create Accounts
    log.debug("entering into SecurityRuleAccount");
    try {
      OBContext.setAdminMode();
      String SecurityRuleId = (String) bundle.getParams().get("Efin_Security_Rules_ID");
      EfinSecurityRules Rules = OBDal.getInstance().get(EfinSecurityRules.class, SecurityRuleId);
      if (SecurityRuleId != null) {
        query = " select count(he.efin_security_rules_id) as count from efin_security_rules as he "
            + "  join (select efin_security_ruleslines_id,efin_security_rules_id from efin_security_ruleslines where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as acc "
            + "    on acc.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesdept_id,efin_security_rules_id from efin_security_rulesdept where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as dept "
            + "    on dept.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesproj_id,efin_security_rules_id from efin_security_rulesproj where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as pro "
            + "    on pro.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesbudg_id,efin_security_rules_id from efin_security_rulesbudg where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as bud "
            + "    on bud.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesactiv_id,efin_security_rules_id from efin_security_rulesactiv where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as fun "
            + "    on fun.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesfuture1_id,efin_security_rules_id from efin_security_rulesfuture1 where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as fut1 "
            + "    on fut1.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesfuture2_id,efin_security_rules_id from efin_security_rulesfuture2 where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as fut2 "
            + "    on fut2.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select efin_security_rulesbpartner_id,efin_security_rules_id from efin_security_rulesbpartner where "
            + "    efin_security_rules_id = '" + SecurityRuleId + "' limit 1) as bp "
            + "    on bp.efin_security_rules_id=he.efin_security_rules_id "
            + "  where he.efin_security_rules_id='" + SecurityRuleId + "'";
        ps = conn.prepareStatement(query);
        rs = ps.executeQuery();
        if (rs.next()) {
          int count = rs.getInt("count");
          if (count == 0) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_security_rule_process@");
            bundle.setResult(result);
            return;
          }

        }
      }

      /*
       * 
       * Creating Security Rule Account
       */
      // Delete lines from security account
      OBQuery<EfinSecurityRulesAct> securityact = OBDal.getInstance().createQuery(
          EfinSecurityRulesAct.class, "as e where e.efinSecurityRules.id='" + Rules.getId() + "'");
      if (securityact.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from efin_security_rules_act where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Account
      OBQuery<EfinSecurityRuleslines> includelines = OBDal.getInstance().createQuery(
          EfinSecurityRuleslines.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include size:" + includelines.list().size());
      if (includelines.list().size() > 0) {
        for (EfinSecurityRuleslines lineslist : includelines.list()) {
          query = "select act.c_elementvalue_id from c_elementvalue act where to_number(act.value) between (select to_number(ac.value) from efin_security_ruleslines line "
              + " join c_elementvalue ac on ac.c_elementvalue_id =line.fromact where efin_security_ruleslines_id='"
              + lineslist.getId()
              + "') and (select to_number(ac.value) from efin_security_ruleslines line "
              + " join c_elementvalue ac on ac.c_elementvalue_id =line.toact where efin_security_ruleslines_id='"
              + lineslist.getId() + "') and act.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("c_elementvalue_id"));
          }
        }
      }
      // Create Include Code for Account
      OBQuery<EfinSecurityRuleslines> includeCodeLines = OBDal.getInstance().createQuery(
          EfinSecurityRuleslines.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='INC' ");
      log.debug("include Code Account size:" + includeCodeLines.list().size());
      if (includelines.list().size() > 0) {
        for (EfinSecurityRuleslines lineslist : includeCodeLines.list()) {
          query = "select act.c_elementvalue_id from c_elementvalue act "
              + "where to_number(act.value) between ? and ? and act.ad_client_id=?";
          ps = conn.prepareStatement(query);
          ps.setInt(1, lineslist.getFromCode().intValue());
          ps.setInt(2, lineslist.getToCode().intValue());
          ps.setString(3, vars.getClient());
          rs = ps.executeQuery();
          while (rs.next()) {
            includeCodeList.add(rs.getString("c_elementvalue_id"));
          }
        }
      }
      // Create Exclude List for Account
      OBQuery<EfinSecurityRuleslines> Excludelines = OBDal.getInstance().createQuery(
          EfinSecurityRuleslines.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      if (Excludelines.list().size() > 0) {
        for (EfinSecurityRuleslines exlineslist : Excludelines.list()) {
          query = "select act.c_elementvalue_id from c_elementvalue act where to_number(act.value) between (select to_number(ac.value) from efin_security_ruleslines line "
              + " join c_elementvalue ac on ac.c_elementvalue_id =line.fromact where efin_security_ruleslines_id='"
              + exlineslist.getId()
              + "') and (select to_number(ac.value) from efin_security_ruleslines line "
              + " join c_elementvalue ac on ac.c_elementvalue_id =line.toact where efin_security_ruleslines_id='"
              + exlineslist.getId() + "') and act.ad_client_id='" + vars.getClient() + "'";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("c_elementvalue_id"));
          }
        }
      }
      log.debug("include list size:" + includeList.size());
      log.debug("include code list size:" + includeCodeList.size());
      log.debug("exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.addAll(includeCodeList);
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesActList().size() > 0) {
        for (EfinSecurityRulesAct act : Rules.getEfinSecurityRulesActList()) {
          includeList.add(act.getElementvalue().getId());
          if (act.isEfinManual())
            map.put(act.getElementvalue().getId(), "Y");
          else {
            map.put(act.getElementvalue().getId(), "N");
          }

        }
      }
      // Finally Delete the Exist Accounts
      ps = conn
          .prepareStatement("delete from efin_security_rules_act where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> incluedSet = new HashSet<String>(includeList);
      log.debug("include set size :" + incluedSet.size());
      Iterator<String> iterator = incluedSet.iterator();
      EfinSecurityRulesAct act = null;
      while (iterator.hasNext()) {
        act = OBProvider.getInstance().get(EfinSecurityRulesAct.class);
        act.setEfinManual(false);
        act.setElementvalue(OBDal.getInstance().get(EfinSecurityactpickV.class, iterator.next()));
        act.setEfinSecurityRules(Rules);
        OBDal.getInstance().save(act);
        // records inserted in security accounts
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Accounts tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_act set Efin_Manual='" + map.get(key)
            + "' where c_elementvalue_id='" + key + "' and efin_security_rules_id='" + Rules.getId()
            + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Department
       * 
       */
      includeList = new ArrayList<String>();
      includeCodeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Department
      OBQuery<SecurityRuleDepartment> securityDept = OBDal.getInstance().createQuery(
          SecurityRuleDepartment.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityDept.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from efin_security_rules_dept where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Department
      OBQuery<EfinSecurityRulesdept> includeDeptlines = OBDal.getInstance().createQuery(
          EfinSecurityRulesdept.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include Dept size:" + includeDeptlines.list().size());
      if (includeDeptlines.list().size() > 0) {
        for (EfinSecurityRulesdept Deptlineslist : includeDeptlines.list()) {
          query = "select dept.c_salesregion_id from c_salesregion dept where to_number(dept.value) between (select to_number(dep.value)"
              + " from efin_security_rulesdept line  join c_salesregion dep on dep.c_salesregion_id =line.fromdept where efin_security_rulesdept_id='"
              + Deptlineslist.getId() + "') "
              + "and (select to_number(dep.value) from efin_security_rulesdept line  "
              + "join c_salesregion dep on dep.c_salesregion_id =line.todept where efin_security_rulesdept_id='"
              + Deptlineslist.getId() + "') and dept.ad_client_id='" + vars.getClient()
              + "' and dept.ad_org_id ='" + Deptlineslist.getFromdept().getOrganization().getId()
              + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("c_salesregion_id"));
          }
        }
      }
      // Create Include Code List for Department
      OBQuery<EfinSecurityRulesdept> includeCodeDeptlines = OBDal.getInstance().createQuery(
          EfinSecurityRulesdept.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='INC' ");
      log.debug("include Code Dept size:" + includeCodeDeptlines.list().size());
      if (includeCodeDeptlines.list().size() > 0) {
        for (EfinSecurityRulesdept Deptlineslist : includeCodeDeptlines.list()) {
          query = "select dept.c_salesregion_id from c_salesregion dept "
              + "where to_number(dept.value) between ? and ? and dept.ad_client_id=?";
          ps = conn.prepareStatement(query);
          ps.setInt(1, Deptlineslist.getFromCode().intValue());
          ps.setInt(2, Deptlineslist.getToCode().intValue());
          ps.setString(3, vars.getClient());
          rs = ps.executeQuery();
          while (rs.next()) {
            includeCodeList.add(rs.getString("c_salesregion_id"));
          }
        }
      }
      // Create Exclude List for Department
      OBQuery<EfinSecurityRulesdept> ExcludeDeptlines = OBDal.getInstance().createQuery(
          EfinSecurityRulesdept.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      if (ExcludeDeptlines.list().size() > 0) {
        for (EfinSecurityRulesdept exDeptlineslist : ExcludeDeptlines.list()) {
          query = "select dept.c_salesregion_id from c_salesregion dept where to_number(dept.value) between (select to_number(dep.value)"
              + " from efin_security_rulesdept line  join c_salesregion dep on dep.c_salesregion_id =line.fromdept where efin_security_rulesdept_id='"
              + exDeptlineslist.getId() + "') "
              + "and (select to_number(dep.value) from efin_security_rulesdept line  "
              + "join c_salesregion dep on dep.c_salesregion_id =line.todept where efin_security_rulesdept_id='"
              + exDeptlineslist.getId() + "') and dept.ad_client_id='" + vars.getClient() + "'";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("c_salesregion_id"));
          }
        }
      }
      log.debug("Dept include list size:" + includeList.size());
      log.debug("Dept exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.addAll(includeCodeList);
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesDepartmentList().size() > 0) {
        for (SecurityRuleDepartment dept : Rules.getEfinSecurityRulesDepartmentList()) {
          includeList.add(dept.getDepartment().getId());
          if (dept.isManual())
            map.put(dept.getDepartment().getId(), "Y");
          else {
            map.put(dept.getDepartment().getId(), "N");
          }

        }
      }
      // Finally Delete the Exist Department
      ps = conn
          .prepareStatement("delete from efin_security_rules_dept where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> DeptincluedSet = new HashSet<String>(includeList);
      log.debug("include set size :" + DeptincluedSet.size());
      Iterator<String> Deptiterator = DeptincluedSet.iterator();
      SecurityRuleDepartment Dept = null;
      while (Deptiterator.hasNext()) {
        Dept = OBProvider.getInstance().get(SecurityRuleDepartment.class);
        Dept.setManual(false);
        Dept.setDepartment(
            OBDal.getInstance().get(EfinSecuritydeptpickV.class, Deptiterator.next()));
        Dept.setRule(Rules);
        OBDal.getInstance().save(Dept);
        // records inserted in security Departments
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Department tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_dept set Efin_Manual='"
            + map.get(key) + "' where C_Salesregion_ID='" + key + "' and efin_security_rules_id='"
            + Rules.getId() + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Project
       * 
       */
      includeList = new ArrayList<String>();
      includeCodeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Project
      OBQuery<SecurityRuleProject> securityProject = OBDal.getInstance()
          .createQuery(SecurityRuleProject.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityProject.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from efin_security_rules_proj where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Project
      OBQuery<EfinSecurityRulesproj> includeProjlines = OBDal.getInstance().createQuery(
          EfinSecurityRulesproj.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include Project size:" + includeProjlines.list().size());
      if (includeProjlines.list().size() > 0) {
        for (EfinSecurityRulesproj ProjLineslist : includeProjlines.list()) {
          query = "select proj.c_project_id from c_project proj where to_number(proj.value) between (select to_number(pro.value)"
              + " from efin_security_rulesproj line  join c_project pro on pro.c_project_id =line.fromproject where efin_security_rulesproj_id='"
              + ProjLineslist.getId() + "') " + "and (select to_number(pro.value)"
              + " from efin_security_rulesproj line  join c_project pro on pro.c_project_id =line.toproject where efin_security_rulesproj_id='"
              + ProjLineslist.getId() + "') and proj.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("c_project_id"));
          }
        }
      }
      // Create Include Code List for Project.,
      OBQuery<EfinSecurityRulesproj> includeCodeProjlines = OBDal.getInstance().createQuery(
          EfinSecurityRulesproj.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='INC' ");
      log.debug("include Code Project size:" + includeCodeProjlines.list().size());
      if (includeCodeProjlines.list().size() > 0) {
        for (EfinSecurityRulesproj Projlineslist : includeCodeProjlines.list()) {
          query = "select proj.c_project_id from c_project proj "
              + "where to_number(proj.value) between ? and ? and proj.ad_client_id=?";
          ps = conn.prepareStatement(query);
          ps.setInt(1, Projlineslist.getFromcode().intValue());
          ps.setInt(2, Projlineslist.getTocode().intValue());
          ps.setString(3, vars.getClient());
          rs = ps.executeQuery();
          while (rs.next()) {
            includeCodeList.add(rs.getString("c_project_id"));
          }
        }
      }
      // Create Exclude List for Project
      OBQuery<EfinSecurityRulesproj> excludeProjlines = OBDal.getInstance().createQuery(
          EfinSecurityRulesproj.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      log.debug("exclude Project size:" + excludeProjlines.list().size());
      if (excludeProjlines.list().size() > 0) {
        for (EfinSecurityRulesproj ProjLineslist : excludeProjlines.list()) {
          query = "select proj.c_project_id from c_project proj where to_number(proj.value) between (select to_number(pro.value)"
              + " from efin_security_rulesproj line  join c_project pro on pro.c_project_id =line.fromproject where efin_security_rulesproj_id='"
              + ProjLineslist.getId() + "') " + "and (select to_number(pro.value)"
              + " from efin_security_rulesproj line  join c_project pro on pro.c_project_id =line.toproject where efin_security_rulesproj_id='"
              + ProjLineslist.getId() + "') and proj.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("c_project_id"));
          }
        }
      }
      log.debug("Project include list size:" + includeList.size());
      log.debug("Project exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.addAll(includeCodeList);
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesProjectList().size() > 0) {
        for (SecurityRuleProject pro : Rules.getEfinSecurityRulesProjectList()) {
          includeList.add(pro.getProject().getId());
          if (pro.isManual())
            map.put(pro.getProject().getId(), "Y");
          else {
            map.put(pro.getProject().getId(), "N");
          }
        }
      }
      // Finally Delete the Exist project
      ps = conn
          .prepareStatement("delete from efin_security_rules_proj where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> ProjectincluedSet = new HashSet<String>(includeList);
      log.debug("Project include set size :" + ProjectincluedSet.size());
      Iterator<String> Projectiterator = ProjectincluedSet.iterator();
      SecurityRuleProject Proj = null;
      while (Projectiterator.hasNext()) {
        Proj = OBProvider.getInstance().get(SecurityRuleProject.class);
        Proj.setManual(false);
        Proj.setProject(
            OBDal.getInstance().get(EfinSecurityprojpickV.class, Projectiterator.next()));
        Proj.setRule(Rules);
        OBDal.getInstance().save(Proj);
        // records inserted in security Project
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Project tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_proj set Efin_Manual='"
            + map.get(key) + "' where C_Project_ID='" + key + "' and efin_security_rules_id='"
            + Rules.getId() + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Budget Type
       * 
       */
      includeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Budget Type
      OBQuery<SecurityRuleBudgetType> securityBudgetType = OBDal.getInstance().createQuery(
          SecurityRuleBudgetType.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityBudgetType.list().size() > 0) {
        ps = conn.prepareStatement(
            "delete from efin_security_rules_budtype where efin_security_rules_id='" + Rules.getId()
                + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Budget Type
      OBQuery<EfinSecurityRulesbudg> includeBudgetTypelines = OBDal.getInstance().createQuery(
          EfinSecurityRulesbudg.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include BudgetType size:" + includeBudgetTypelines.list().size());
      if (includeBudgetTypelines.list().size() > 0) {
        for (EfinSecurityRulesbudg BudgetTypeLineslist : includeBudgetTypelines.list()) {
          query = "select budg.c_campaign_id from c_campaign budg where to_number(budg.value) between (select to_number(bud.value)"
              + " from efin_security_rulesbudg line  join c_campaign bud on bud.c_campaign_id =line.frombudget where efin_security_rulesbudg_id='"
              + BudgetTypeLineslist.getId() + "') " + "and (select to_number(bud.value)"
              + " from efin_security_rulesbudg line  join c_campaign bud on bud.c_campaign_id =line.tobudget where efin_security_rulesbudg_id='"
              + BudgetTypeLineslist.getId() + "')  and budg.ad_client_id='" + vars.getClient()
              + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("c_campaign_id"));
          }
        }
      }
      // Create Exclude List for Budget Type
      OBQuery<EfinSecurityRulesbudg> excludeBudgetTypelines = OBDal.getInstance().createQuery(
          EfinSecurityRulesbudg.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      log.debug("exclude BudgetType size:" + excludeBudgetTypelines.list().size());
      if (excludeBudgetTypelines.list().size() > 0) {
        for (EfinSecurityRulesbudg BudgetTypeLineslist : excludeBudgetTypelines.list()) {
          query = "select budg.c_campaign_id from c_campaign budg where to_number(budg.value) between (select to_number(bud.value)"
              + " from efin_security_rulesbudg line  join c_campaign bud on bud.c_campaign_id =line.frombudget where efin_security_rulesbudg_id='"
              + BudgetTypeLineslist.getId() + "') " + "and (select to_number(bud.value)"
              + " from efin_security_rulesbudg line  join c_campaign bud on bud.c_campaign_id =line.tobudget where efin_security_rulesbudg_id='"
              + BudgetTypeLineslist.getId() + "')  and budg.ad_client_id='" + vars.getClient()
              + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("c_campaign_id"));
          }
        }
      }
      log.debug("Budget Type include list size:" + includeList.size());
      log.debug("Budget Type exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesBudtypeList().size() > 0) {
        for (SecurityRuleBudgetType bud : Rules.getEfinSecurityRulesBudtypeList()) {
          includeList.add(bud.getBudgetType().getId());
          if (bud.isManual())
            map.put(bud.getBudgetType().getId(), "Y");
          else {
            map.put(bud.getBudgetType().getId(), "N");
          }
        }
      }
      // Finally Delete the Exist Budget Type
      ps = conn
          .prepareStatement("delete from efin_security_rules_budtype where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> BudgetTypeincluedSet = new HashSet<String>(includeList);
      log.debug("Budget Type include set size :" + ProjectincluedSet.size());
      Iterator<String> BudgetTypeiterator = BudgetTypeincluedSet.iterator();
      SecurityRuleBudgetType BudgetType = null;
      while (BudgetTypeiterator.hasNext()) {
        BudgetType = OBProvider.getInstance().get(SecurityRuleBudgetType.class);
        BudgetType.setManual(false);
        BudgetType
            .setBudgetType(OBDal.getInstance().get(Campaign.class, BudgetTypeiterator.next()));
        BudgetType.setRule(Rules);
        OBDal.getInstance().save(BudgetType);
        // records inserted in security BudgetType
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(BudgetType tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_budtype set Efin_Manual='"
            + map.get(key) + "' where C_Campaign_ID='" + key + "' and efin_security_rules_id='"
            + Rules.getId() + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Functional Classification
       * 
       */
      includeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Functional Classification
      OBQuery<SecurityRulesActivity> securityActivity = OBDal.getInstance()
          .createQuery(SecurityRulesActivity.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityActivity.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from efin_security_rules_activ where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Functional Classification
      OBQuery<EfinSecurityRulesactiv> includeActivitylines = OBDal.getInstance().createQuery(
          EfinSecurityRulesactiv.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include Activity size:" + includeActivitylines.list().size());
      if (includeActivitylines.list().size() > 0) {
        for (EfinSecurityRulesactiv ActivLineslist : includeActivitylines.list()) {
          query = "select activ.c_activity_id from c_activity activ where to_number(activ.value) between (select to_number(act.value)"
              + " from efin_security_rulesactiv line  join c_activity act on act.c_activity_id =line.fromactivity where efin_security_rulesactiv_id='"
              + ActivLineslist.getId() + "') " + "and (select to_number(act.value)"
              + " from efin_security_rulesactiv line  join c_activity act on act.c_activity_id =line.toactivity where efin_security_rulesactiv_id='"
              + ActivLineslist.getId() + "')  and activ.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("c_activity_id"));
          }
        }
      }
      // Create Exclude List for Functional Classification
      OBQuery<EfinSecurityRulesactiv> excludeActivitylines = OBDal.getInstance().createQuery(
          EfinSecurityRulesactiv.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      log.debug("exclude Activity size:" + excludeActivitylines.list().size());
      if (excludeActivitylines.list().size() > 0) {
        for (EfinSecurityRulesactiv ActivLineslist : excludeActivitylines.list()) {
          query = "select activ.c_activity_id from c_activity activ where to_number(activ.value) between (select to_number(act.value)"
              + " from efin_security_rulesactiv line  join c_activity act on act.c_activity_id =line.fromactivity where efin_security_rulesactiv_id='"
              + ActivLineslist.getId() + "') " + "and (select to_number(act.value)"
              + " from efin_security_rulesactiv line  join c_activity act on act.c_activity_id =line.toactivity where efin_security_rulesactiv_id='"
              + ActivLineslist.getId() + "')  and activ.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("c_activity_id"));
          }
        }
      }
      log.debug("Functional include list size:" + includeList.size());
      log.debug("Functional exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesActivityList().size() > 0) {
        for (SecurityRulesActivity func : Rules.getEfinSecurityRulesActivityList()) {
          includeList.add(func.getFunctionalClassification().getId());
          if (func.isManual())
            map.put(func.getFunctionalClassification().getId(), "Y");
          else {
            map.put(func.getFunctionalClassification().getId(), "N");
          }
        }
      }
      // Finally Delete the Exist Functional Classification
      ps = conn
          .prepareStatement("delete from efin_security_rules_activ where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> ActivityincluedSet = new HashSet<String>(includeList);
      log.debug("Functional include set size :" + ActivityincluedSet.size());
      Iterator<String> ActivityIterator = ActivityincluedSet.iterator();
      SecurityRulesActivity Activity = null;
      while (ActivityIterator.hasNext()) {
        Activity = OBProvider.getInstance().get(SecurityRulesActivity.class);
        Activity.setManual(false);
        Activity.setFunctionalClassification(
            OBDal.getInstance().get(ABCActivity.class, ActivityIterator.next()));
        Activity.setRule(Rules);
        OBDal.getInstance().save(Activity);
        // records inserted in security Functional Classification
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Functional Classification tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_activ set Efin_Manual='"
            + map.get(key) + "' where C_Activity_ID='" + key + "' and efin_security_rules_id='"
            + Rules.getId() + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Future1
       * 
       */
      includeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Future1
      OBQuery<SecurityRuleFutureOne> securityFutureone = OBDal.getInstance()
          .createQuery(SecurityRuleFutureOne.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityFutureone.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from efin_security_rules_fut1 where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Future1
      OBQuery<EfinSecurityRulesFutureOne> includeFutureOneLines = OBDal.getInstance().createQuery(
          EfinSecurityRulesFutureOne.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include Futureone size:" + includeFutureOneLines.list().size());
      if (includeFutureOneLines.list().size() > 0) {
        for (EfinSecurityRulesFutureOne ActivLineslist : includeFutureOneLines.list()) {
          query = "select us.user1_id from user1 us where to_number(us.value) between (select to_number(act.value)"
              + " from efin_security_rulesfuture1 line  join user1 act on act.user1_id =line.fromfuture1 where efin_security_rulesfuture1_id='"
              + ActivLineslist.getId() + "') " + "and (select to_number(act.value)"
              + " from efin_security_rulesfuture1 line  join user1 act on act.user1_id =line.tofuture1 where efin_security_rulesfuture1_id='"
              + ActivLineslist.getId() + "')  and us.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("user1_id"));
          }
        }
      }
      // Create Exclude List for Future1
      OBQuery<EfinSecurityRulesFutureOne> excludeFutureOneLines = OBDal.getInstance().createQuery(
          EfinSecurityRulesFutureOne.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      log.debug("exclude Futureone size:" + excludeFutureOneLines.list().size());
      if (excludeFutureOneLines.list().size() > 0) {
        for (EfinSecurityRulesFutureOne ActivLineslist : excludeFutureOneLines.list()) {
          query = "select us.user1_id from user1 us where to_number(us.value) between (select to_number(act.value)"
              + " from efin_security_rulesfuture1 line  join user1 act on act.user1_id =line.fromfuture1 where efin_security_rulesfuture1_id='"
              + ActivLineslist.getId() + "') " + "and (select to_number(act.value)"
              + " from efin_security_rulesfuture1 line  join user1 act on act.user1_id =line.tofuture1 where efin_security_rulesfuture1_id='"
              + ActivLineslist.getId() + "')  and us.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("user1_id"));
          }
        }
      }
      log.debug("Futureone include list size:" + includeList.size());
      log.debug("Futureone exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesFut1List().size() > 0) {
        for (SecurityRuleFutureOne fut1 : Rules.getEfinSecurityRulesFut1List()) {
          includeList.add(fut1.getFuture1().getId());
          if (fut1.isManual())
            map.put(fut1.getFuture1().getId(), "Y");
          else {
            map.put(fut1.getFuture1().getId(), "N");
          }
        }
      }
      // Finally Delete the Exist Future1
      ps = conn
          .prepareStatement("delete from efin_security_rules_fut1 where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> FutureOneincluedSet = new HashSet<String>(includeList);
      log.debug("Project include set size :" + FutureOneincluedSet.size());
      Iterator<String> FutureOneIterator = FutureOneincluedSet.iterator();
      SecurityRuleFutureOne futureOne = null;
      while (FutureOneIterator.hasNext()) {
        futureOne = OBProvider.getInstance().get(SecurityRuleFutureOne.class);
        futureOne.setManual(false);
        futureOne
            .setFuture1(OBDal.getInstance().get(UserDimension1.class, FutureOneIterator.next()));
        futureOne.setRule(Rules);
        OBDal.getInstance().save(futureOne);
        // records inserted in security Futureone
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Future1 tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_fut1 set Efin_Manual='"
            + map.get(key) + "' where User1_ID='" + key + "' and efin_security_rules_id='"
            + Rules.getId() + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Future2
       * 
       */
      includeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Future2
      OBQuery<SecurityRuleFutureSecond> securityFutureTwo = OBDal.getInstance().createQuery(
          SecurityRuleFutureSecond.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityFutureTwo.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from efin_security_rules_fut2 where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Future2
      OBQuery<EfinSecurityRulesfutureTwo> includeFutureTwoLines = OBDal.getInstance().createQuery(
          EfinSecurityRulesfutureTwo.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include FutureTwo size:" + includeFutureTwoLines.list().size());
      if (includeFutureTwoLines.list().size() > 0) {
        for (EfinSecurityRulesfutureTwo ActivLineslist : includeFutureTwoLines.list()) {
          query = "select us.user2_id from user2 us where to_number(us.value) between (select to_number(act.value)"
              + " from efin_security_rulesfuture2 line  join user2 act on act.user2_id =line.fromfuture2 where efin_security_rulesfuture2_id='"
              + ActivLineslist.getId() + "') " + "and (select to_number(act.value)"
              + " from efin_security_rulesfuture2 line  join user2 act on act.user2_id =line.tofuture2 where efin_security_rulesfuture2_id='"
              + ActivLineslist.getId() + "')  and us.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("user2_id"));
          }

        }

      }
      // Create Exclude List for Future2
      OBQuery<EfinSecurityRulesfutureTwo> excludeFutureTwoLines = OBDal.getInstance().createQuery(
          EfinSecurityRulesfutureTwo.class,
          "as e where e.efinSecurityRules.id='" + Rules.getId() + "' and e.type='EX' ");
      log.debug("exclude FutureTwo size:" + excludeFutureTwoLines.list().size());
      if (excludeFutureTwoLines.list().size() > 0) {
        for (EfinSecurityRulesfutureTwo ActivLineslist : excludeFutureTwoLines.list()) {
          query = "select us.user2_id from user2 us where to_number(us.value) between (select to_number(act.value)"
              + " from efin_security_rulesfuture2 line  join user2 act on act.user2_id =line.fromfuture2 where efin_security_rulesfuture2_id='"
              + ActivLineslist.getId() + "') " + "and (select to_number(act.value)"
              + " from efin_security_rulesfuture2 line  join user2 act on act.user2_id =line.tofuture2 where efin_security_rulesfuture2_id='"
              + ActivLineslist.getId() + "')  and us.ad_client_id='" + vars.getClient() + "'";

          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("user2_id"));
          }

        }

      }
      log.debug("Futureone include list size:" + includeList.size());
      log.debug("Futureone exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesFut2List().size() > 0) {
        for (SecurityRuleFutureSecond fut2 : Rules.getEfinSecurityRulesFut2List()) {
          includeList.add(fut2.getFuture2().getId());
          if (fut2.isManual())
            map.put(fut2.getFuture2().getId(), "Y");
          else {
            map.put(fut2.getFuture2().getId(), "N");
          }
        }
      }
      // Finally Delete the Exist Future2
      ps = conn
          .prepareStatement("delete from efin_security_rules_fut2 where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> FutureTwoincluedSet = new HashSet<String>(includeList);
      log.debug("FutureTwo include set size :" + FutureTwoincluedSet.size());
      Iterator<String> FutureTwoIterator = FutureTwoincluedSet.iterator();
      SecurityRuleFutureSecond futureTwo = null;
      while (FutureTwoIterator.hasNext()) {
        futureTwo = OBProvider.getInstance().get(SecurityRuleFutureSecond.class);
        futureTwo.setManual(false);
        futureTwo
            .setFuture2(OBDal.getInstance().get(UserDimension2.class, FutureTwoIterator.next()));
        futureTwo.setRule(Rules);
        OBDal.getInstance().save(futureTwo);
        // records inserted in security Futuretwo
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Future2 tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_fut2 set Efin_Manual='"
            + map.get(key) + "' where User2_ID='" + key + "' and efin_security_rules_id='"
            + Rules.getId() + "'");
        ps.executeUpdate();
      }

      /*
       * Creating security Rule for Entity
       * 
       */
      includeList = new ArrayList<String>();
      includeCodeList = new ArrayList<String>();
      excludeList = new ArrayList<String>();

      // Delete lines from Security Entity
      OBQuery<SecurityRulesBpartner> securityBpartner = OBDal.getInstance()
          .createQuery(SecurityRulesBpartner.class, "as e where e.rule.id='" + Rules.getId() + "'");
      if (securityBpartner.list().size() > 0) {
        ps = conn
            .prepareStatement("delete from Efin_Security_Rules_Bp where efin_security_rules_id='"
                + Rules.getId() + "' and efin_manual='N'");
        ps.executeUpdate();
      }
      // Create Include List for Entity
      OBQuery<EfinSecurityRulesbpartner> includeBpartner = OBDal.getInstance().createQuery(
          EfinSecurityRulesbpartner.class,
          "as e where e.rule.id='" + Rules.getId() + "' and e.type='IN' ");
      log.debug("include bpartner size:" + includeBpartner.list().size());
      if (includeBpartner.list().size() > 0) {
        for (EfinSecurityRulesbpartner ActivLineslist : includeBpartner.list()) {
          query = "select bp.c_bpartner_id from c_bpartner bp where to_number(bp.em_efin_documentno) "
              + "between (select to_number(act.em_efin_documentno) from efin_security_rulesbpartner line "
              + "join c_bpartner act on act.c_bpartner_id =line.frombpartner "
              + "where efin_security_rulesbpartner_id='" + ActivLineslist.getId() + "') "
              + "and (select to_number(act.em_efin_documentno) from efin_security_rulesbpartner line join c_bpartner act on act.c_bpartner_id =line.tobpartner where efin_security_rulesbpartner_id='"
              + ActivLineslist.getId() + "') " + "and bp.ad_client_id='" + vars.getClient() + "'";
          System.out.println("query bpartner inc:" + query.toString());
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          while (rs.next()) {
            includeList.add(rs.getString("c_bpartner_id"));
          }

        }

      }
      // Create Include Code List for Entity
      OBQuery<EfinSecurityRulesbpartner> includeCodeBpartner = OBDal.getInstance().createQuery(
          EfinSecurityRulesbpartner.class,
          "as e where e.rule.id='" + Rules.getId() + "' and e.type='INC' ");
      log.debug("include Code bpartner size:" + includeCodeBpartner.list().size());
      if (includeCodeBpartner.list().size() > 0) {
        for (EfinSecurityRulesbpartner ActivLineslist : includeCodeBpartner.list()) {
          query = "select bp.c_bpartner_id from c_bpartner bp "
              + "where to_number(bp.em_efin_documentno) between ? and ? and bp.ad_client_id=?";
          ps = conn.prepareStatement(query);
          ps.setInt(1, ActivLineslist.getFromcode().intValue());
          ps.setInt(2, ActivLineslist.getTocode().intValue());
          ps.setString(3, vars.getClient());
          rs = ps.executeQuery();
          while (rs.next()) {
            includeCodeList.add(rs.getString("c_bpartner_id"));
          }
        }
      }
      // Create Exclude List for Entity
      OBQuery<EfinSecurityRulesbpartner> excludeBpartner = OBDal.getInstance().createQuery(
          EfinSecurityRulesbpartner.class,
          "as e where e.rule.id='" + Rules.getId() + "' and e.type='EX' ");
      log.debug("exclude bpartner size:" + excludeBpartner.list().size());
      if (excludeBpartner.list().size() > 0) {
        for (EfinSecurityRulesbpartner ActivLineslist : excludeBpartner.list()) {
          query = "select bp.c_bpartner_id from c_bpartner bp where to_number(bp.em_efin_documentno) "
              + "between (select to_number(act.em_efin_documentno) from efin_security_rulesbpartner line "
              + "join c_bpartner act on act.c_bpartner_id =line.frombpartner "
              + "where efin_security_rulesbpartner_id='" + ActivLineslist.getId() + "') "
              + "and (select to_number(act.em_efin_documentno) from efin_security_rulesbpartner line join c_bpartner act on act.c_bpartner_id =line.tobpartner where efin_security_rulesbpartner_id='"
              + ActivLineslist.getId() + "') " + "and bp.ad_client_id='" + vars.getClient() + "'";
          ps = conn.prepareStatement(query);
          System.out.println("query bpartner Exc:" + query.toString());
          rs = ps.executeQuery();
          while (rs.next()) {
            excludeList.add(rs.getString("c_bpartner_id"));
          }

        }

      }
      log.debug("Bpartner include list size:" + includeList.size());
      log.debug("Bpartner exclude List size:" + excludeList.size());
      // compare two lists and remove the exclude records from include
      includeList.addAll(includeCodeList);
      includeList.removeAll(excludeList);
      // avoid duplication
      map = new HashMap<String, String>();
      if (Rules.getEfinSecurityRulesBpList().size() > 0) {
        for (SecurityRulesBpartner bp : Rules.getEfinSecurityRulesBpList()) {
          includeList.add(bp.getBusinessPartner().getId());
          if (bp.isManual())
            map.put(bp.getBusinessPartner().getId(), "Y");
          else {
            map.put(bp.getBusinessPartner().getId(), "N");
          }
        }
      }
      // Finally Delete the Exist Entity
      ps = conn
          .prepareStatement("delete from efin_security_rules_bp  where efin_security_rules_id='"
              + Rules.getId() + "'");
      ps.executeUpdate();

      HashSet<String> bPartnerincluedSet = new HashSet<String>(includeList);
      log.debug("Bpartner include set size :" + bPartnerincluedSet.size());
      Iterator<String> bPartnerIterator = bPartnerincluedSet.iterator();
      SecurityRulesBpartner bPartner = null;
      while (bPartnerIterator.hasNext()) {
        bPartner = OBProvider.getInstance().get(SecurityRulesBpartner.class);
        bPartner.setManual(false);
        bPartner.setBusinessPartner(
            OBDal.getInstance().get(BusinessPartner.class, bPartnerIterator.next()));
        bPartner.setRule(Rules);
        OBDal.getInstance().save(bPartner);
        // records inserted in security Entity
      }
      OBDal.getInstance().flush();
      // update the manual field in dimension tab(Entity tab) after insert
      for (String key : map.keySet()) {
        ps = conn.prepareStatement("update efin_security_rules_bp set Efin_Manual='" + map.get(key)
            + "' where c_bpartner_id='" + key + "' and efin_security_rules_id='" + Rules.getId()
            + "'");
        ps.executeUpdate();
      }

      Rules.setCreateact(true);
      OBDal.getInstance().save(Rules);
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_InitiateRuleOK@");
      bundle.setResult(result);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Exeception in Security Rule Account:" + e);
      OBDal.getInstance().rollbackAndClose();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
