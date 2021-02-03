package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.http.HttpServletRequest;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.finance.EfinSecurityRulesAct;
import sa.elm.ob.finance.SecurityRuleBudgetType;
import sa.elm.ob.finance.SecurityRuleDepartment;
import sa.elm.ob.finance.SecurityRuleFutureOne;
import sa.elm.ob.finance.SecurityRuleFutureSecond;
import sa.elm.ob.finance.SecurityRuleProject;
import sa.elm.ob.finance.SecurityRulesActivity;
import sa.elm.ob.finance.SecurityRulesBpartner;

/**
 * @author Qualian on 30/06/2106
 */

public class SecurityRuleReactivate extends DalBaseProcess {

	/**
	 * Create account Entries in Security Rule Account Tab
	 * Table(efin_security_rules_act)
	 * 
	 */
	private static final Logger log = LoggerFactory.getLogger(SecurityRuleReactivate.class);

	@Override
	public void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub

		HttpServletRequest request = RequestContext.get().getRequest();
		VariablesSecureApp vars = new VariablesSecureApp(request);
		Connection conn = OBDal.getInstance().getConnection();
		PreparedStatement ps = null;
		// Create Accounts
		log.debug("entering into SecurityRuleReactivate");
		try {
			OBContext.setAdminMode();
			String SecurityRuleId = (String) bundle.getParams().get("Efin_Security_Rules_ID");
			EfinSecurityRules Rules = OBDal.getInstance().get(EfinSecurityRules.class, SecurityRuleId);
			//Delete lines from security account 
			OBQuery<EfinSecurityRulesAct> securityact = OBDal.getInstance().createQuery(EfinSecurityRulesAct.class, "as e where e.efinSecurityRules.id='" + Rules.getId() + "'");
			if(securityact.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_act where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}
			//Delete lines from Security Department
			OBQuery<SecurityRuleDepartment> securityDept = OBDal.getInstance().createQuery(SecurityRuleDepartment.class, "as e where e.rule.id='" + Rules.getId() + "'");
			if(securityDept.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_dept where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}
			//Delete lines from Security Project
			OBQuery<SecurityRuleProject> securityProject = OBDal.getInstance().createQuery(SecurityRuleProject.class, "as e where e.rule.id='" + Rules.getId() + "'");
			if(securityProject.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_proj where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}

			//Delete lines from Security Budget Type
			OBQuery<SecurityRuleBudgetType> securityBudgetType = OBDal.getInstance().createQuery(SecurityRuleBudgetType.class, "as e where e.rule.id='" + Rules.getId() + "'");
			if(securityBudgetType.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_budtype where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}

			//Delete lines from Security Functional Classification
			OBQuery<SecurityRulesActivity> securityActivity = OBDal.getInstance().createQuery(SecurityRulesActivity.class, "as e where e.rule.id='" + Rules.getId() + "'");
			if(securityActivity.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_activ where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}
			//Delete lines from Security Future1
			OBQuery<SecurityRuleFutureOne> securityFutureone = OBDal.getInstance().createQuery(SecurityRuleFutureOne.class, "as e where e.rule.id='" + Rules.getId() + "'");
			if(securityFutureone.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_fut1 where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}
			//Delete lines from Security Future2
			OBQuery<SecurityRuleFutureSecond> securityFutureTwo = OBDal.getInstance().createQuery(SecurityRuleFutureSecond.class, "as e where e.rule.id='" + Rules.getId() + "'");
			if(securityFutureTwo.list().size() > 0) {
				ps = conn.prepareStatement("delete from efin_security_rules_fut2 where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
				ps.executeUpdate();
			}
	                //Delete lines from Security Entity
                        OBQuery<SecurityRulesBpartner> securityBpartner = OBDal.getInstance().createQuery(SecurityRulesBpartner.class, "as e where e.rule.id='" + Rules.getId() + "'");
                        if(securityBpartner.list().size() > 0) {
                                ps = conn.prepareStatement("delete from efin_security_rules_bp where efin_security_rules_id='" + Rules.getId() + "' and efin_manual='N'");
                                ps.executeUpdate();
                        }
			
			Rules.setEfinProcessbutton(false);
			Rules.setCreateact(false);
			OBDal.getInstance().save(Rules);
			OBError result = OBErrorBuilder.buildMessage(null, "success", "@ProcessOK@");
			bundle.setResult(result);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();

		}
		catch (Exception e) {
			log.debug("Exeception in SecurityRuleReactivate:" + e);
			OBDal.getInstance().rollbackAndClose();
			Throwable t = DbUtility.getUnderlyingSQLException(e);
			final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars, vars.getLanguage(), t.getMessage());
			bundle.setResult(error);
		}
		finally {
			OBContext.restorePreviousMode();
		}

	}

}
