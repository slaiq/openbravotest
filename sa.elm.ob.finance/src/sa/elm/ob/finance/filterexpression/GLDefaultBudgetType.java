package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.marketing.Campaign;

/**
 * 
 * This class is to apply Default filter as Funds to Budget Type
 * 
 */
public class GLDefaultBudgetType implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    String clientId = requestMap.get("inpadClientId");
    try {
      OBContext.setAdminMode();
      String budgettype = "F";
      Campaign objFC = null;
      OBQuery<Campaign> campaign = OBDal.getInstance().createQuery(Campaign.class,
          " as e where e.efinBudgettype =:budgettype and e.client.id=:clientId");
      campaign.setNamedParameter("clientId", clientId);
      campaign.setNamedParameter("budgettype", budgettype);
      List<Campaign> campaignList = campaign.list();

      if (campaignList.size() > 0) {
        objFC = campaignList.get(0);
      }
      if (objFC != null) {
        return objFC.getId();
      } else {
        return "";
      }

    } catch (OBException e) {
      e.printStackTrace();
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
