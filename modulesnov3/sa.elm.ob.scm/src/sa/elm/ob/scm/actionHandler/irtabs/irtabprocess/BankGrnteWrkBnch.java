package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class BankGrnteWrkBnch extends IRTabIconVariables {
  Logger log = Logger.getLogger(BankGrnteWrkBnch.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String action = jsonData.optString("action") == "" ? null : jsonData
          .getString("action");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      /* Bank Guarantee Workbench-Release, Confiscation, Extention, Amount Revision */
      if (tabId.equals("C1779EE84BE44C30B4385F367742CE7F")
          || tabId.equals("008692D1D80444E78AAB4FDFFFA41476")
          || tabId.equals("E579C036C1C2401FA439F0F858FA8DE3")
          || tabId.equals("4E2C60BDF7894C32BF27E6CAC7684625")) {
        if (!recordId.equals("")) {
          if (tabId.equals("C1779EE84BE44C30B4385F367742CE7F")) {
            count = BGWorkbenchDAO.getCountofBgRelorBgConfi(recordId, "REL");
            if (count > 0 && action != null && !action.equals("") && !action.equals("del")) {
              enable = 1;
            }
          } else if (tabId.equals("008692D1D80444E78AAB4FDFFFA41476")) {
            count = BGWorkbenchDAO.getCountofBgRelorBgConfi(recordId, "CON");
            if (count > 0 && action != null && !action.equals("") && !action.equals("del")) {
              enable = 1;
            }
          }

          Escmbankguaranteedetail bankguarantee = OBDal.getInstance().get(
              Escmbankguaranteedetail.class, recordId);
          if (bankguarantee.getBgstatus() != null
              && (bankguarantee.getBgstatus().equals("REL")
                  || bankguarantee.getBgstatus().equals("EXP") || bankguarantee.getBgstatus()
                  .equals("CON")) || bankguarantee.getBgstatus().equals("DR")) {
            enable = 1;
          }
        }
      }
      /* Bank Guarantee Workbench-Lines */
      else if (tabId.equals("6732339A97874A85BF73542C2B5AFF88")) {
        if (!recordId.equals("")) {
          ESCMBGWorkbench bg = OBDal.getInstance().get(ESCMBGWorkbench.class, recordId);
          if (bg != null && bg.getBghdstatus() != null && bg.getBghdstatus().equals("CO")) {
            enable = 1;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
