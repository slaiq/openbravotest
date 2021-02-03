package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import jxl.common.Logger;
import sa.elm.ob.finance.EfinBudgetControlParam;

public class EfinUniqueCodeFilterByOrganization implements FilterExpression {
  private Map<String, String> requestMap;
  private final static Logger log4j = Logger.getLogger(EfinUniqueCodeFilterByOrganization.class);

  /**
   * This class is used to filter unique code by organization
   */
  @Override
  public String getExpression(Map<String, String> _requestMap) {
    try {
      requestMap = _requestMap;
      String clientId = requestMap.get("inpadClientId");
      EfinBudgetControlParam objBC = null;
      OBQuery<EfinBudgetControlParam> obqry = OBDal.getInstance()
          .createQuery(EfinBudgetControlParam.class, " as e where e.client.id=:clientId");
      obqry.setNamedParameter("clientId", clientId);
      obqry.setMaxResult(1);
      List<EfinBudgetControlParam> obqryList = obqry.list();
      if (obqryList.size() > 0) {
        objBC = obqryList.get(0);
      }
      if (objBC != null) {
        return objBC.getAgencyHqOrg().getId();
      } else {
        return "";
      }
    } catch (Exception e) {
      log4j.error("Exception in EfinUniqueCodeFilterByOrganization : " + e);
      return "";
    }
  }
}
