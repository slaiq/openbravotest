package sa.elm.ob.finance.ad_process.SecurityRuleRefresh;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinSecurityRules;

/**
 * @author Sowmiya N S
 */

public class SecurityRuleRefreshAction extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(EfinSecurityRules.class);
  static int counter = 0;
  HttpServletRequest request = RequestContext.get().getRequest();
  VariablesSecureApp vars = new VariablesSecureApp(request);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    log.debug("Refresh Action Started!!");
    try {

      OBContext.setAdminMode();
      String securityRuleId = (String) bundle.getParams().get("Efin_Security_Rules_ID");
      EfinSecurityRules rules = OBDal.getInstance().get(EfinSecurityRules.class, securityRuleId);
      Boolean isAccountInserted = SecurityRulesRefreshAccount.getRefreshAccountInstance()
          .insertProcess(rules);
      Boolean isDepartmentInserted = SecurityRulesRefreshDepartment.getRefreshdepartmentInstance()
          .insertProcess(rules);
      Boolean isSubAccountInserted = SecurityRulesRefreshSubaccount.getRefreshSubaccountInstance()
          .insertProcess(rules);
      Boolean isEntityInserted = SecurityRulesRefreshEntity.getRefreshEntityInstance()
          .insertProcess(rules);
      // Check Whether the Process has been completed or not
      if (!isAccountInserted || !isDepartmentInserted) {

        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_SecurityRuleRefreshError@");
        bundle.setResult(result);
      } else if (!isSubAccountInserted || !isEntityInserted) {
        // exception bundle.set or get message
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_securityruleRefreshError@");
        bundle.setResult(result);
      } else {

        OBError result = OBErrorBuilder.buildMessage(null, "success",
            "@Efin_SecurityRuleRefreshOK@");
        bundle.setResult(result);
      }
    } catch (Exception e) {

      OBDal.getInstance().rollbackAndClose();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      e.printStackTrace();
    } finally {

      OBContext.restorePreviousMode();
    }
  }
}
