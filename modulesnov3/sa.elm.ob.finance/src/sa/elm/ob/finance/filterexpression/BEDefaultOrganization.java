package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * 
 * This class is to apply Default Agency Hq Org from Budget Controller Parameter to Organization in
 * Budget Enquiry (Filter Unique Code)
 * 
 * @author Mouli.K
 */
public class BEDefaultOrganization implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    // TODO Auto-generated method stub
    requestMap = _requestMap;
    // String clientId = requestMap.get("inpadClientId");
    try {
      OBContext.setAdminMode();
      JSONObject client = new JSONObject(requestMap.get("context"));
      String clientId = client.getString("inpadClientId");
      EfinBudgetControlParam objFC = null;
      OBQuery<EfinBudgetControlParam> budgetcontrolparam = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, " as e where  e.client.id=:clientId");
      budgetcontrolparam.setNamedParameter("clientId", clientId);

      List<EfinBudgetControlParam> budgetcontrolparamList = budgetcontrolparam.list();

      if (budgetcontrolparamList.size() > 0) {
        objFC = budgetcontrolparamList.get(0);
      }
      if (objFC != null) {
        return objFC.getAgencyHqOrg().getId();
      } else {
        return "";
      }

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
