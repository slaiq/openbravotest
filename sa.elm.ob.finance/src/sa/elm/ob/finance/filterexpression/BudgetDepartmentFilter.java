package sa.elm.ob.finance.filterexpression;

/**
 * 
 * @author Kiruthika
 * 
 */
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
 * This class is to apply default filter for department from HQ Budget Control Unit
 * 
 */
public class BudgetDepartmentFilter implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
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
        return objFC.getBudgetcontrolunit().getId();
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
