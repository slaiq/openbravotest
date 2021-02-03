package sa.elm.ob.scm.filterexpression;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmBidMgmt;

/**
 * This class is to apply default filter for supplier in Proposal management.
 */
@SuppressWarnings("unused")
public class BidActionSupplierFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(BidActionSupplierFilterExpression.class);
  private Map<String, String> requestMap;
  private HttpSession httpSession;

  private String windowId;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      httpSession = RequestContext.get().getSession();
      windowId = requestMap.get(OBBindingsConstants.WINDOW_ID_PARAM);
      String strCurrentParam = requestMap.get("currentParam");
      String inpescmBidmgmtId = (String) context.get("inpescmBidmgmtId");
      String contractCategoryId = null;
      String IsContractCategory = "N";
      String Bidtype = null;
      if (context.has("inpbidtype"))
        Bidtype = (String) context.get("inpbidtype");
      EscmBidMgmt bidMgmt = null;
      if (inpescmBidmgmtId != null) {
        bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, inpescmBidmgmtId);
      }
      if (strCurrentParam.equals("Bidtype") && bidMgmt != null) {
        if (Bidtype == null) {
          Bidtype = bidMgmt.getBidtype();
        }
        return Bidtype;
      }
      if (strCurrentParam.equals("Escm_Bidmgmt_ID") && inpescmBidmgmtId != null) {
        return inpescmBidmgmtId;
      }
      if (strCurrentParam.equals("Contract_Category_ID") && bidMgmt != null) {
        if (bidMgmt.getContractType() != null)
          contractCategoryId = bidMgmt.getContractType().getId();
        return contractCategoryId;
      }
      if (strCurrentParam.equals("IsContractCategory") && bidMgmt != null) {
        if (bidMgmt.getContractType() != null) {
          IsContractCategory = "Y";
          return IsContractCategory;
        } else {
          return IsContractCategory;
        }
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;

  }

}
