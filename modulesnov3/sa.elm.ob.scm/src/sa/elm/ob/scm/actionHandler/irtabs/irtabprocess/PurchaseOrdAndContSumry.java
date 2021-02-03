package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class PurchaseOrdAndContSumry extends IRTabIconVariables {
  Logger log = Logger.getLogger(PurchaseOrdAndContSumry.class);

  @Override
  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");
      final String btnName = jsonData.optString("button") == "" ? null
          : jsonData.getString("button");
      VariablesSecureApp vars = new VariablesSecureApp(request);

      /* Purchase Order and Contracts Summary- Lines Attributes */
      if (tabId.equals("8F35A05BFBB34C34A80E9DEF769613F7")) {
        if (btnName != null && btnName.equals("delete")) {
          if (!recordId.equals("")) {
            // Order order = OBDal.getInstance().get(Order.class, recordId);
            enable = getIconState(recordId);
            /*
             * if (order.getEscmOldOrder() != null) { enable = 1; }
             */
          }
        }
        /* New Button disable if proposal are added */
        else if (btnName == null) {
          if (!recordId.equals("")) {
            Order order = OBDal.getInstance().get(Order.class, recordId);
            if (order.isEscmAddproposal()) {
              enable = 1;
            } else if (order.getEscmAppstatus() != null
                && ((order.getEscmAppstatus().equals("ESCM_AP")
                    || order.getEscmAppstatus().equals("ESCM_IP")
                    || order.getEscmAppstatus().equals("ESCM_CA"))
                // || (((order.getEscmAppstatus().equals("ESCM_RA")
                // || order.getEscmAppstatus().equals("ESCM_REJ"))
                // && order.getEscmOrdertype().equals("PUR"))
                // && (order.getEscmOldOrder() != null))
                )) {
              enable = 1;
            }
          }
        }
      }
      /* Purchase Order and Contracts Summary- Bank Guarantee Detail */
      else if (tabId.equals("07AF133F4E2E45AAA53D7FEA71656DD4")) {
        if (!recordId.equals("")) {
          Order order = OBDal.getInstance().get(Order.class, recordId);
          if (order.getEscmProposalmgmt() == null) {
            enable = 1;
          }
        }
      }
      /* Purchase Order and Contracts Summary- Header */
      else if (tabId.equals("62248BBBCF644C18A75B92AD8E50238C")) {
        if (btnName != null && btnName.equals("delete")) {
          if (!recordId.equals("")) {
            enable = getIconState(recordId);
            Order order = OBDal.getInstance().get(Order.class, recordId);
            if (order.getEscmOldOrder() != null) {
              enable = 1;
            }
          }
        } else if (btnName != null && btnName.equals("distribution")) {
          Boolean recordstatus = true;
          if (!recordId.equals("")) {
            Order order = OBDal.getInstance().get(Order.class, recordId);
            try {
              String preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                  vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
                  "2ADDCB0DD2BF4F6DB13B21BBCCC3038C");
              if (preferenceValue != null && preferenceValue.equals("Y"))
                ispreference = true;
            } catch (PropertyException e) {
              ispreference = false;
            }
            if (order != null) {
              if ((order.getEscmAppstatus() != null && order.getEscmAppstatus().equals("ESCM_AP"))
                  || (order.getEscmAppstatus() != null
                      && order.getEscmAppstatus().equals("ESCM_IP"))) {
                recordstatus = false;
              }
            }
            if (ispreference && recordstatus) {
              enable = 0;
            } else {
              enable = 1;
            }
          }
        }
        /* Print Button */
        else {
          if (!recordId.equals("")) {
            Order order = OBDal.getInstance().get(Order.class, recordId);
            if (order != null) {
              String status = order.getEscmOrdertype() != null ? ""
                  : order.getEscmOrdertype().toString();
              if (order.getEscmAppstatus() != null && !order.getEscmAppstatus().equals("ESCM_AP")
                  && status.equals("CR")) {
                enable = 0;
              } else {
                enable = 1;
              }
            }
          }
        }
      }
      /* Purchase Order and Contracts Summary- Shipment Attributes, Source Reference */
      else if (tabId.equals("EFD9C9C596D24068ABEB15062EE2EDBC")
          || tabId.equals("832ED077041D47F49BB8AA9EB70F14EC")) {
        if (!recordId.equals("")) {
          OrderLine orderline = OBDal.getInstance().get(OrderLine.class, recordId);
          if (orderline != null) {
            if ((orderline.getSalesOrder().getEscmAppstatus() != null
                && orderline.getSalesOrder().getEscmAppstatus().equals("ESCM_IP"))
                || (orderline.getSalesOrder().getEscmAppstatus() != null
                    && orderline.getSalesOrder().getEscmAppstatus().equals("ESCM_AP"))
                || ((orderline.getSalesOrder().getEscmAppstatus() != null
                    && orderline.getSalesOrder().getEscmAppstatus().equals("ESCM_RA")
                    || orderline.getSalesOrder().getEscmAppstatus().equals("ESCM_REJ"))
                    && (orderline.getSalesOrder().getEscmOldOrder() != null)))
              enable = 1;
          }
        }
      }
      /* Purchase Order and Contracts Summary- Payment Terms, PO Amendment */
      else if (tabId.equals("02F79A626AEE4BB4B8B12D345FFB164C")
          || tabId.equals("1CEC4F8FFBCC41AD86E0A830880CBFF3")) {
        if (!recordId.equals("")) {
          Order poorder = OBDal.getInstance().get(Order.class, recordId);
          if (poorder != null) {
            enable = getIconState(recordId);

            if (tabId.equals("1CEC4F8FFBCC41AD86E0A830880CBFF3")) {
              if (btnName == null) {
                if ((poorder.getEscmAppstatus().equals("ESCM_RA")
                    || poorder.getEscmAppstatus().equals("ESCM_REJ"))
                    && (poorder.getEscmOldOrder() != null)) {
                  enable = 0;
                } else {
                  enable = 1;
                }
              } else if (btnName != null && btnName.equals("delete")) {
                if ((poorder.getEscmAppstatus().equals("ESCM_AP"))
                    || (poorder.getEscmAppstatus().equals("ESCM_IP"))
                    || (poorder.getEscmAppstatus().equals("ESCM_CA"))) {
                  enable = 1;
                }
              }
            }
            if (tabId.equals("02F79A626AEE4BB4B8B12D345FFB164C")
                && poorder.getEscmAppstatus().equals("ESCM_RA")) {
              enable = 1;
            }
          }
        }
      }
      // Payment Schedule
      else if (tabId.equals("283293291F49463A905E37366C799426")) {
        if (!recordId.equals("")) {
          Order poorder = OBDal.getInstance().get(Order.class, recordId);
          String orgId = poorder.getOrganization().getId();
          String clientId = OBContext.getOBContext().getCurrentClient().getId();
          String userId = OBContext.getOBContext().getUser().getId();
          String roleId = OBContext.getOBContext().getRole().getId();
          String windowId = Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W;
          String propertyName = "Escm_Contract_Manager";
          String pre = "N";
          try {
            pre = sa.elm.ob.utility.util.Preferences.getPreferenceValue(propertyName, true,
                clientId, orgId, userId, roleId, windowId, "N");
          } catch (Exception e) {
            // Preference Not Found
            pre = "N";
          }
          if (poorder != null) {
            enable = getIconState(recordId);
            if (btnName == null) {
              if ((poorder.getEscmAppstatus().equals("ESCM_AP")
                  || poorder.getEscmAppstatus().equals("ESCM_CA")
                  || poorder.getEscmAppstatus().equals("ESCM_IP")
                  || poorder.getEscmAppstatus().equals("ESCM_REJ")) & pre.equals("N")) {
                enable = 1;
              } else {
                enable = 0;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public int getIconState(String recordId) {
    int state = 0;
    try {
      OBContext.setAdminMode(true);
      Order order = OBDal.getInstance().get(Order.class, recordId);
      if (order != null) {
        if ((order.getEscmAppstatus() != null && order.getEscmAppstatus().equals("ESCM_AP"))
            || (order.getEscmAppstatus() != null && order.getEscmAppstatus().equals("ESCM_IP"))) {
          state = 1;
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return state;
  }
}