package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.materialmgmt.cost.ABCActivity;

import jxl.common.Logger;

public class EfinUniqueCodeFilterrByFunctional implements FilterExpression {
  private Map<String, String> requestMap;
  private final static Logger log4j = Logger.getLogger(EfinUniqueCodeFilterrByFunctional.class);

  /**
   * This class is used to filter unique code by functional
   */

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    try {
      requestMap = _requestMap;
      String clientId = requestMap.get("inpadClientId");
      ABCActivity objFC = null;
      OBQuery<ABCActivity> obqry = OBDal.getInstance().createQuery(ABCActivity.class,
          " as e where e.efinIsdefault = true " + "and e.client.id=:clientId");
      obqry.setNamedParameter("clientId", clientId);
      obqry.setMaxResult(1);
      List<ABCActivity> obqryLIst = obqry.list();
      if (obqryLIst.size() > 0) {
        objFC = obqryLIst.get(0);
      }
      if (objFC != null) {
        return objFC.getId();
      } else {
        return "";
      }
    } catch (Exception e) {
      log4j.error("Exception in EfinUniqueCodeFilterrByFunctional : " + e);
      return "";
    }
  }
}
