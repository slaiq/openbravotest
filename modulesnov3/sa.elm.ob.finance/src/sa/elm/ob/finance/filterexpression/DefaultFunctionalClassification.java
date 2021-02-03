package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.materialmgmt.cost.ABCActivity;

/**
 * 
 * This class is to apply Default Functional Classification as filter in unique code
 * 
 */
public class DefaultFunctionalClassification implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    String clientId = requestMap.get("inpadClientId");
    try {
      OBContext.setAdminMode();
      requestMap = _requestMap;
      Boolean isDefault = true;
      ABCActivity objFC = null;
      OBQuery<ABCActivity> c_activity = OBDal.getInstance().createQuery(ABCActivity.class,
          " as e where e.efinIsdefault = :default and e.client.id=:clientId");
      c_activity.setNamedParameter("clientId", clientId);
      c_activity.setNamedParameter("default", isDefault);
      List<ABCActivity> c_activityList = c_activity.list();

      if (c_activityList.size() > 0) {
        objFC = c_activityList.get(0);
      }
      if (objFC != null) {
        return objFC.getId();
      } else {
        return "";
      }

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
