package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class BidManagement extends IRTabIconVariables {
  Logger log = Logger.getLogger(BidManagement.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");
      final String btnName = jsonData.optString("button") == "" ? null
          : jsonData.getString("button");
      VariablesSecureApp vars = new VariablesSecureApp(request);

      /* Bid Management-Lines, Bid dates, Suppliers, Bid Terms and Conditions, Header */
      if (tabId.equals("D54F30C8AD574A2A84999F327EF0E3A4")
          || tabId.equals("754D4F75D3F54A3EBBC69496D27B9C3B")
          || tabId.equals("0D0A5AFFF5EA480DAB978052AD2198D3")
          || tabId.equals("9165D36805BC4B6E8B7CDE4420D09B4B")
          || tabId.equals("31960EC365D746A180594FFB7B403ABB")) {
        if (!recordId.equals("")) {
          EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class, recordId);
          try {
            String preferenceValue = Preferences.getPreferenceValue("ESCM_ProcurementDirector",
                true, vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
                "E509200618424FD099BAB1D4B34F96B8");
            if (preferenceValue != null && preferenceValue.equals("Y"))
              ispreference = true;
          } catch (PropertyException e) {
            ispreference = false;
          }

          if (bidmgmt != null) {
            if (btnName == null || btnName.equals("delete")) {
              if (bidmgmt.getBidappstatus().equals("ESCM_AP")
                  || bidmgmt.getBidappstatus().equals("ESCM_IP"))
                enable = 1;
              if (tabId.equals("754D4F75D3F54A3EBBC69496D27B9C3B")) {

                if (bidmgmt.getBidappstatus().equals("ESCM_IP")
                    && tabId.equals("754D4F75D3F54A3EBBC69496D27B9C3B")) {
                  enable = 1;
                }

                if (bidmgmt.getTabadulTenderID() != null
                    && bidmgmt.getBidappstatus().equals("ESCM_AP")
                    && bidmgmt.getBidstatus().equals("ACT")
                    && tabId.equals("754D4F75D3F54A3EBBC69496D27B9C3B")) {
                  enable = 0;
                }
                if (btnName != null && btnName.equals("delete")) {
                  if ((bidmgmt.getBidappstatus().equals("ESCM_IP")
                      || bidmgmt.getBidappstatus().equals("ESCM_AP")
                      || bidmgmt.getBidstatus().equals("EXT") || bidmgmt.getBidstatus().equals("CL")
                      || bidmgmt.getBidstatus().equals("CD"))
                      && tabId.equals("754D4F75D3F54A3EBBC69496D27B9C3B"))
                    enable = 1;
                }
              }
            } else {
              if ((!bidmgmt.getBidtype().equals("TR")) && bidmgmt.getBidstatus().equals("ACT")) {
                enable = 1;
              }
            }
          }
        }
      }
      /* Bid Management-Source Reference */
      else if (tabId.equals("FC8BC787053F4759A9C2129C324834FE")) {
        if (!recordId.equals("")) {
          Escmbidmgmtline bidmgmtline = OBDal.getInstance().get(Escmbidmgmtline.class, recordId);
          if (bidmgmtline != null) {
            if (bidmgmtline.getEscmBidmgmt().getBidappstatus().equals("ESCM_AP")
                || bidmgmtline.getEscmBidmgmt().getBidappstatus().equals("ESCM_IP"))
              enable = 1;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
