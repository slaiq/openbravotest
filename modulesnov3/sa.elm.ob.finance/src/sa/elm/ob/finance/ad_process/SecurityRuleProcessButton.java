package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinSecurityRules;

/**
 * @author Priyanka Ranjan on 18/08/2106
 */

public class SecurityRuleProcessButton extends DalBaseProcess {

  /**
   * Create account Entries in Security Rule Account Tab Table(efin_security_rules_act)
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(SecurityRuleProcessButton.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = "";
    // Create Accounts
    log.debug("entering into SecurityRuleAccount");
    try {
      OBContext.setAdminMode();
      String SecurityRuleId = (String) bundle.getParams().get("Efin_Security_Rules_ID");
      EfinSecurityRules Rules = OBDal.getInstance().get(EfinSecurityRules.class, SecurityRuleId);

      if (SecurityRuleId != null) {
        query = "select  count(he.efin_security_rules_id) as count from efin_security_rules as he "
            + "  join (select Efin_Security_Rules_Act_id,efin_security_rules_id from Efin_Security_Rules_Act where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as acc "
            + "   on acc.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select Efin_Security_Rules_Dept_id,efin_security_rules_id from Efin_Security_Rules_Dept where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as dept "
            + "   on dept.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select Efin_Security_Rules_Proj_id,efin_security_rules_id from Efin_Security_Rules_Proj where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as pro "
            + "   on pro.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select Efin_Security_Rules_Budtype_id,efin_security_rules_id from Efin_Security_Rules_Budtype where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as bud "
            + "   on bud.efin_security_rules_id=he.efin_security_rules_id  "
            + "  join (select Efin_Security_Rules_Activ_id,efin_security_rules_id from Efin_Security_Rules_Activ where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as fun "
            + "   on fun.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select Efin_Security_Rules_Fut1_id,efin_security_rules_id from Efin_Security_Rules_Fut1 where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as fut1 "
            + "   on fut1.efin_security_rules_id=he.efin_security_rules_id "
            + "  join (select Efin_Security_Rules_Fut2_id,efin_security_rules_id from Efin_Security_Rules_Fut2 where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as fut2 "
            + "   on fut2.efin_security_rules_id=he.efin_security_rules_id  "
            + "  join (select efin_security_rules_bp_id,efin_security_rules_id from efin_security_rules_bp where "
            + "   efin_security_rules_id ='" + SecurityRuleId + "' limit 1) as bp "
            + "   on bp.efin_security_rules_id=he.efin_security_rules_id  "
            + "  where he.efin_security_rules_id='" + SecurityRuleId + "'";
        ps = conn.prepareStatement(query);
        rs = ps.executeQuery();
        if (rs.next()) {
          int count = rs.getInt("count");
          if (count == 0) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_securityrule_dimEmpty@");
            bundle.setResult(result);
            return;
          }

        }
      }

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
            + "   where he.efin_security_rules_id='" + SecurityRuleId + "'";
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

      Rules.setEfinProcessbutton(true);
      Rules.setCreateact(true);
      Rules.setReactivate(true);

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_FinalizeOk@");
      bundle.setResult(result);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
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
