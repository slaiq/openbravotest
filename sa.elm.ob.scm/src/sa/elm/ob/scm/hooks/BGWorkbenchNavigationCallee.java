package sa.elm.ob.scm.hooks;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.WindowTrl;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.hook.NavigationLinkHook;

import sa.elm.ob.scm.EscmProposalMgmt;

public class BGWorkbenchNavigationCallee implements NavigationLinkHook {
  public static String proposalWindowId = "CAF2D3EEF3B241018C8F65E8F877B29F";
  public static String proposalTabId = "D6115C9AF1DD4C4C9811D2A69E42878B";
  public static String poWindowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
  public static String poTabId = "62248BBBCF644C18A75B92AD8E50238C";
  public static String bgDocumentnoVId = "77652349421843059FBFFC2CFA64FF88";

  private static final Logger log = Logger.getLogger(BGWorkbenchNavigationCallee.class);

  @Override
  public void exec(VariablesSecureApp vars, Table table, JSONObject result) throws Exception {
    try {
      OBContext.setAdminMode();
      if (table.getId().equals(bgDocumentnoVId)) {
        String recordId = vars.getStringParameter("inpKeyReferenceId");

        Tab tab = null;

        // Check whether record is proposal or PO
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
        if (proposal != null) {
          tab = OBDal.getInstance().get(Tab.class, proposalTabId);
        } else {
          Order order = OBDal.getInstance().get(Order.class, recordId);
          if (order != null) {
            tab = OBDal.getInstance().get(Tab.class, poTabId);
          }
        }

        final String userLanguageId = OBContext.getOBContext().getLanguage().getId();
        String tabTitle = null;
        for (WindowTrl windowTrl : tab.getWindow().getADWindowTrlList()) {
          final String trlLanguageId = (String) DalUtil.getId(windowTrl.getLanguage());
          if (trlLanguageId.equals(userLanguageId)) {
            tabTitle = windowTrl.getName();
          }
        }
        if (tabTitle == null) {
          tabTitle = tab.getWindow().getName();
        }

        result.put("tabTitle", tabTitle);
        result.put("recordId", recordId);
        result.put("tabId", tab.getId());
        result.put("windowId", tab.getWindow().getId());
      }
    } catch (Exception e) {
      log.debug("Error while getting navigation details for bgworkbench" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
