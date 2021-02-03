package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ad_callouts.dao.SalesVoucherDAO;

/**
 * Callout to update the supplier details in Bid Management sales voucher tab
 * 
 * @author qualian
 */

@SuppressWarnings("serial")
public class SalesVoucherCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(SalesVoucherCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;
    String inpbidId = vars.getStringParameter("inpescmBidmgmtId");
    String inpbidsupplier = vars.getStringParameter("inpsuppliernumber");
    String inpbranch = vars.getStringParameter("inpbranchname");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");

    try {

      if (inpLastFieldChanged.equals("inpescmBidmgmtId")) {
        // get BidName,rfpprice for selected bidno
        JSONObject result = SalesVoucherDAO.getBidNameAndRFPPrice(inpbidId);
        info.addResult("inpbidname", result.getString("bidname"));
        if (result.has("rfpprice")) {
          info.addResult("inprfpprice", result.getString("rfpprice"));
          String rfpPrice = result.getString("rfpprice");
          if (Double.parseDouble(rfpPrice) > 0) {
            info.addResult("inpamountsar", result.getString("rfpprice"));
          }

        } else {
          info.addResult("inprfpprice", null);
          info.addResult("inpamountsar", null);
        }

      }
      if (inpLastFieldChanged.equals("inpsuppliernumber")) {
        // get supplier name for selected supplier number from bpartner
        String SupplierName = SalesVoucherDAO.getSupplierName(inpbidsupplier);
        info.addResult("inpsuppliername", SupplierName);

        // get Branch name for selected supplier number from C_BPartner_Location
        String BranchName = SalesVoucherDAO.getBranchName(inpbidsupplier);
        info.addResult("inpbranchname", BranchName);
        // set location,phone and fax for given BranchName
        JSONObject result = SalesVoucherDAO.getLocationPhoneandFax(BranchName);
        if (result.has("c_location_id")) {
          info.addResult("inpcLocationId", result.getString("c_location_id"));
        } else {
          info.addResult("inpcLocationId", " ");
        }
        if (result.has("phone")) {
          info.addResult("inpsupplierPhone", result.getString("phone"));
        } else {
          info.addResult("inpsupplierPhone", null);
        }
        if (result.has("fax")) {
          info.addResult("inpfax", result.getString("fax"));
        } else {
          info.addResult("inpfax", null);
        }

        // get Commercial Registery No for selected supplier from bpartner and set in
        // "Commercial Registery No"
        // field in RFP Sales Voucher
        String CommerialRegisteryNo = SalesVoucherDAO.getCommercialRegisteryNo(inpbidsupplier);
        info.addResult("inpcommercialregistoryno", CommerialRegisteryNo);

      }

      if (inpLastFieldChanged.equals("inpbranchname")) {
        // get location,phone and fax for selected branch name
        JSONObject result = SalesVoucherDAO.getLocationPhoneandFax(inpbranch);
        if (result.has("c_location_id")) {
          info.addResult("inpcLocationId", result.getString("c_location_id"));
        } else {
          info.addResult("inpcLocationId", " ");
        }
        if (result.has("phone")) {
          info.addResult("inpsupplierPhone", result.getString("phone"));
        } else {
          info.addResult("inpsupplierPhone", null);
        }
        if (result.has("fax")) {
          info.addResult("inpfax", result.getString("fax"));
        } else {
          info.addResult("inpfax", null);
        }
      }
    } catch (Exception e) {
      log.error("Exception in SalesVoucherCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
