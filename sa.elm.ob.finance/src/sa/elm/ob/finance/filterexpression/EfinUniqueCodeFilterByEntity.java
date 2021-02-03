package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import jxl.common.Logger;

public class EfinUniqueCodeFilterByEntity implements FilterExpression {
  private Map<String, String> requestMap;
  private final static Logger log4j = Logger.getLogger(EfinUniqueCodeFilterByEntity.class);

  /**
   * This class is used to filter unique code by entity
   */
  @Override
  public String getExpression(Map<String, String> _requestMap) {
    try {
      requestMap = _requestMap;
      String clientId = requestMap.get("inpadClientId");
      BusinessPartner objBP = null;
      OBQuery<BusinessPartner> obqry = OBDal.getInstance().createQuery(BusinessPartner.class,
          " as e where e.escmDefaultpartner =  true " + "and e.client.id=:clientId");
      obqry.setNamedParameter("clientId", clientId);
      obqry.setMaxResult(1);
      List<BusinessPartner> obqryList = obqry.list();
      if (obqryList.size() > 0) {
        objBP = obqryList.get(0);
      }
      if (objBP != null) {
        return objBP.getId();
      } else {
        return "";
      }
    } catch (Exception e) {
      log4j.error("Exception in EfinUniqueCodeFilterByEntity : " + e);
      return "";
    }
  }
}
