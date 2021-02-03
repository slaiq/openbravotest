
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
import org.openbravo.model.procurement.Requisition;

/**
 * This class is to apply default filter in Purchase requisition
 */
@SuppressWarnings("unused")
public class PRActionSupplierFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(PRActionSupplierFilterExpression.class);
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
      String inpRequistionId = (String) context.get("inpmRequisitionId");
      String contractCategoryId = null;
      String IsContractCategory = "N";

      Requisition req = OBDal.getInstance().get(Requisition.class, inpRequistionId);

      if (strCurrentParam.equals("Contract Category") && req != null) {
        if (req.getEscmContactType() != null)
          contractCategoryId = req.getEscmContactType().getId();
        return contractCategoryId;
      }
      if (strCurrentParam.equals("IsContractCategory") && req != null) {
        if (req.getEscmContactType() != null) {
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
