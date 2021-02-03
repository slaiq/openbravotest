package sa.elm.ob.finance.actionHandler.irtabs.irtabprocess;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.finance.EfinSecurityRules;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

/**
 * 
 * @author Gowtham.V
 *
 */

public class SecurityRuleEntityTabDisableProcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(SecurityRuleEntityTabDisableProcess.class);

  /**
   * This class is used to disable and all button in Entity and Entity rule tab present in Security
   * rule window.
   * 
   */

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      SecurityRulesTabList securityTab = new SecurityRulesTabList();
      List<String> tabList = securityTab.tabList;

      // Entity rule and Entity tab
      if (tabList.contains(tabId)) {

        EfinSecurityRules secRule = OBDal.getInstance().get(EfinSecurityRules.class, recordId);
        if (secRule != null && secRule.isEfinProcessbutton()) {
          enable = 1;
        } else {
          enable = 0;
        }
      }
    } catch (Exception e) {
      log.error("Exception in SecurityRuleEntityTabDisableProcess: " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
