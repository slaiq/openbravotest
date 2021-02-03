package sa.elm.ob.finance.actionHandler.irtabs;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.CostCenterTabDisablePorcess;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.EnclinesDeleteDisableProcess;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.EncumbranceCopyIconDisableProcess;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.InvoiceLineDisable;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.InvoiceTabDisableProcess;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.RevisionLineDisableProcess;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.SecurityRuleEntityTabDisableProcess;
import sa.elm.ob.finance.actionHandler.irtabs.irtabprocess.SecurityRulesTabList;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class IrTabDisableFactory {
  Logger log4j = Logger.getLogger(IrTabDisableFactory.class);

  public IRTabIconVariables getTab(HttpServletRequest request, JSONObject jsonData) {
    IRTabIconVariables irtabIcon = null;
    try {

      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      // Cost center linking tab
      if (tabId.equals("57AA8C32AA2E4A36819DAA3AFEF2DC1C")) {
        irtabIcon = new CostCenterTabDisablePorcess();
      }
      // encumbrance lines
      if (tabId.equals("A2E25351FBFF41CB949EDF35DE875B73")) {
        irtabIcon = new EnclinesDeleteDisableProcess();
      }

      // Entity rule, Entity tab in security rule.
      if (tabId.equals("CD3CA08EC1E342B18E438C840325CBFD")
          || tabId.equals("25AF62AB8F89499287C44DDC58D3EB02")) {
        irtabIcon = new SecurityRuleEntityTabDisableProcess();
      }
      // copy button in encumbrance
      if (tabId.equals("9CBD55F879EA4DCAA4E944C0B7DC03D4")) {
        irtabIcon = new EncumbranceCopyIconDisableProcess();
      }
      // print button in invoice
      if (tabId.equals("290")) {
        irtabIcon = new InvoiceTabDisableProcess();
      }
      // budget Revision Line
      if (tabId.equals("E68453B4E62548C6B5E79FEDE3C36586")) {
        irtabIcon = new RevisionLineDisableProcess();
      }
      // Invoice Line
      if (tabId.equals("291")) {
        irtabIcon = new InvoiceLineDisable();
      }

      // All new and delete button in security rules window
      SecurityRulesTabList securityTab = new SecurityRulesTabList();
      List<String> tabList = securityTab.tabList;

      if (tabList.contains(tabId)) {
        irtabIcon = new SecurityRuleEntityTabDisableProcess();
      }

      if (irtabIcon != null) {
        irtabIcon.getIconVariables(request, jsonData);
      }
    } catch (Exception e) {
      log4j.error("Excpetion in getTab(): " + e);
      return null;
    }

    return irtabIcon;

  }

}
