package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 * 
 * This class is to apply Default Entity as filter in unique code
 * 
 */
public class DefaultEntity implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    String clientId = requestMap.get("inpadClientId");
    try {
      OBContext.setAdminMode();
      Boolean isDefault = true;
      BusinessPartner objFC = null;
      OBQuery<BusinessPartner> bpartner = OBDal.getInstance().createQuery(BusinessPartner.class,
          " as e where e.escmDefaultpartner = :default and e.client.id=:clientId");
      bpartner.setNamedParameter("clientId", clientId);
      bpartner.setNamedParameter("default", isDefault);
      List<BusinessPartner> bpartnerList = bpartner.list();

      if (bpartnerList.size() > 0) {
        objFC = bpartnerList.get(0);
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
