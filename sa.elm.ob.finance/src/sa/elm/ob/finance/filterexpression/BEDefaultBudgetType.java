package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.marketing.Campaign;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.EfinBudgetTransfertrx;

/**
 * 
 * This class is to apply Default filter as Cost to Budget Type
 * 
 * @author Mouli.K
 */
public class BEDefaultBudgetType implements FilterExpression {

  private Map<String, String> requestMap;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    // String clientId = requestMap.get("inpadClientId");
    try {
      JSONObject client = new JSONObject(requestMap.get("context"));
      String clientId = client.getString("inpadClientId");
      String tabId = client.getString("inpTabId");

      // If it is fund and cost adjustment tab then we should get budget type from header
      if ("A9D394A5BE374ADC815DABBAF3D6D591".equals(tabId)) {
        String budgetAdjId = client.optString("Efin_Budgetadj_ID", "");
        if (StringUtils.isNotEmpty(budgetAdjId)) {
          BudgetAdjustment budAdj = OBDal.getInstance().get(BudgetAdjustment.class, budgetAdjId);
          if (budAdj != null) {
            return budAdj.getBudgetType() != null ? budAdj.getBudgetType().getId() : "";
          }
        }
      }

      // If it is budget revision tab then we should get budget type from header
      if ("B50C35C1DB7B4E30A6324FBB4D9CCA5D".equals(tabId)) {
        String budgetRevId = client.optString("Efin_Budget_Transfertrx_ID", "");
        if (StringUtils.isNotEmpty(budgetRevId)) {
          EfinBudgetTransfertrx budRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              budgetRevId);
          if (budRev != null) {
            return budRev.getSalesCampaign() != null ? budRev.getSalesCampaign().getId() : "";
          }
        }
      }

      OBContext.setAdminMode();
      String budgettype = "C";
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
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
