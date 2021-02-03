package sa.elm.ob.finance.filterexpression;

/**
 * 
 * @author Kiruthika
 * 
 */
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

import jxl.common.Logger;
import sa.elm.ob.finance.EFINBudget;

/**
 * 
 * This class is to get Budget Type from header
 * 
 */
public class HeaderBudgetType implements FilterExpression {
  private final static Logger log4j = Logger.getLogger(HeaderBudgetType.class);

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    try {
      OBContext.setAdminMode();
      requestMap = _requestMap;
      JSONObject context = new JSONObject(requestMap.get("context"));
      String efinBudgetId = context.optString("inpefinBudgetId", "");

      String budgetType = null;
      EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, efinBudgetId);

      if (budget != null) {
        budgetType = budget.getSalesCampaign().getId();
      }
      if (!budgetType.isEmpty()) {
        return budgetType;
      } else {
        return "";
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception in HeaderBudgetType  :" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
