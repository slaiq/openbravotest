package sa.elm.ob.scm.actionHandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * 
 * @author DivyaPrakash 08/03/2019
 *
 */

public class AddPOContractCategories extends BaseActionHandler {
  private static Logger log = Logger.getLogger(AddPOContractCategories.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject inspectLines = jsonparams.getJSONObject("contract_category");
      final String inpProducttId = jsonRequest.getString("M_Product_ID");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");
      AddPOContractCategoriesDAO dao = new AddPOContractCategoriesDAO();
      // check selected line should be greater than zero
      if (selectedlines.length() == 0) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
      int result = dao.insertContractCategories(inpProducttId, selectedlines);
      if (result == 1) {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in AddPOContractCategories :", e);

      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

}
