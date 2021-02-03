package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.sales.SalesRegion;

/**
 * 
 * This class is to apply Default Department as filter in unique code
 * 
 */
public class DefaultDepartment implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    String clientId = requestMap.get("inpadClientId");
    try {
      OBContext.setAdminMode();
      Boolean isDefault = true;
      SalesRegion objFC = null;
      OBQuery<SalesRegion> salesregion = OBDal.getInstance().createQuery(SalesRegion.class,
          " as e where e.efinDefault = :default and e.client.id=:clientId");
      salesregion.setNamedParameter("clientId", clientId);
      salesregion.setNamedParameter("default", isDefault);
      List<SalesRegion> salesregionList = salesregion.list();

      if (salesregionList.size() > 0) {
        objFC = salesregionList.get(0);
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
