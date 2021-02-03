package sa.elm.ob.finance.ad_callouts;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;

/**
 * @author Gopalakrishnan on 15/11/2017
 */

public class DistributeAllCallout extends BaseActionHandler {
  /**
   * This Servlet Class is responsible to distribute organization in Budget lines
   */
  private static Logger log4j = Logger.getLogger(DistributeAllCallout.class);

  @SuppressWarnings("rawtypes")
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);
      String strCostCentre = "", distOrg = "";
      String strDistOrg = null;

      String BudgetId = "";
      Boolean isDistributed = false;

      if (parameters.containsKey("budgetId")) {
        BudgetId = (String) parameters.get("budgetId");
      }

      if (jsonData.has("action")) {
        String action = jsonData.getString("action");

        // Enter Into Auto Copy of Budget
        if ("setDistributeAll".equals(action) && !BudgetId.isEmpty()) {
          EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, BudgetId);
          if (budget.getDistributionLinkOrg() != null) {
            distOrg = budget.getDistributionLinkOrg().getId();
          }
          // get cost centre
          SQLQuery costCentreQuery = OBDal.getInstance().getSession().createSQLQuery(
              "select budgetcontrol_costcenter from efin_budget_ctrl_param where ad_client_Id = :clientID ");
          costCentreQuery.setParameter("clientID", budget.getClient().getId());
          List costCenterList = costCentreQuery.list();
          if (costCentreQuery != null && costCenterList.size() > 0) {
            Object objCostCentre = costCenterList.get(0);
            if (objCostCentre != null) {
              strCostCentre = objCostCentre.toString();
            }
          }
          for (EFINBudgetLines objLines : budget.getEFINBudgetLinesList()) {
            AccountingCombination objActCombination = objLines.getAccountingCombination();
            // fetch distribution org
            String query = " select ad_org_id from ad_org where ad_org_id in (select ad_org_id from c_validcombination "
                + " where c_salesregion_id= :salesregionID and account_id= :accountID) and ad_org_id = :orgID";
            SQLQuery distOrgQuery = OBDal.getInstance().getSession().createSQLQuery(query);
            distOrgQuery.setParameter("salesregionID", strCostCentre);
            distOrgQuery.setParameter("accountID", objActCombination.getAccount().getId());
            distOrgQuery.setParameter("orgID", distOrg);

            List distOrgList = distOrgQuery.list();
            if (distOrgQuery != null && distOrgList.size() > 0) {
              Object objDistOrg = distOrgList.get(0);
              if (objDistOrg != null) {
                strDistOrg = objDistOrg.toString();
              }
            }
            if (strDistOrg != null) {
              objLines.setDistribute(true);
              objLines
                  .setDistributionLinkOrg(OBDal.getInstance().get(Organization.class, strDistOrg));
            } else {
              objLines.setDistribute(false);
              objLines.setDistributionLinkOrg(null);
            }
            isDistributed = true;
          }
          if (isDistributed) {
            result.put("Message", "Success");
          } else {
            result.put("Message", "NoLines");
          }

        }
        if ("getDistributeFlag".equals(action) && !BudgetId.isEmpty()) {
          EFINBudget bud = OBDal.getInstance().get(EFINBudget.class, BudgetId);
          if (bud != null) {
            if (bud.getEFINBudgetLinesList().size() > 0) {
              result.put("isNoFlag", false);
            } else {
              result.put("isNoFlag", true);
            }
          }
        }

      }

    } catch (Exception e) {
      log4j.error("Exception in DistributeAllCallout :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
