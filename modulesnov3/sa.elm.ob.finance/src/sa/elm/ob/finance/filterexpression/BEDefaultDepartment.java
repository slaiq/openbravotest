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
 * This class is to apply Default Budget Controller Cost Center from Budget Controller Parameter to
 * Department in Budget Enquiry (Filter Unique Code)
 * 
 * @author Mouli.K
 */
public class BEDefaultDepartment implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    try {
      OBContext.setAdminMode();

      JSONObject client = new JSONObject(requestMap.get("context"));
      String clientId = client.getString("inpadClientId");
      String tabId = client.getString("inpTabId");

      EfinBudgetControlParam objFC = null;
      OBQuery<EfinBudgetControlParam> budgetcontrolparam = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, " as e where  e.client.id=:clientId");
      budgetcontrolparam.setNamedParameter("clientId", clientId);

      List<EfinBudgetControlParam> budgetcontrolparamList = budgetcontrolparam.list();

      if (budgetcontrolparamList.size() > 0) {
        objFC = budgetcontrolparamList.get(0);
      }
      if (objFC != null) {
        // If it is fund and cost adjustment tab or budget revision then we should apply different
        // logic
        if ("A9D394A5BE374ADC815DABBAF3D6D591".equals(tabId)
            || "B50C35C1DB7B4E30A6324FBB4D9CCA5D".equals(tabId)) {
          return objFC.getBudgetcontrolunit().getId();
        }
        return objFC.getBudgetcontrolCostcenter().getId();
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
