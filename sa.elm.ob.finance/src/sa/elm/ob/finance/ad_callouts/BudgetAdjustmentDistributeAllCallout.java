package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
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

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;

/**
 * @author Priyanka Ranjan on 15/11/2017
 */

public class BudgetAdjustmentDistributeAllCallout extends BaseActionHandler {
  /**
   * This Servlet Class is responsible to distribute organization in Budget Adjustment lines
   */
  private static Logger log4j = Logger.getLogger(BudgetAdjustmentDistributeAllCallout.class);

  @SuppressWarnings("rawtypes")
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);
      String strCostCentre = "", distOrg = "", strDistOrg = null;

      String BudgetAdjustId = "";
      Boolean isDistributed = false;

      if (parameters.containsKey("BudgetAdjustId")) {
        BudgetAdjustId = (String) parameters.get("BudgetAdjustId");
      }

      if (jsonData.has("action")) {
        String action = jsonData.getString("action");

        // Enter Into Auto Copy of Budget Adjustment
        if ("setDistributeAll".equals(action) && !BudgetAdjustId.isEmpty()) {
          BudgetAdjustment budAdjust = OBDal.getInstance().get(BudgetAdjustment.class,
              BudgetAdjustId);
          if (budAdjust.getDistributionLinkOrg() != null) {
            distOrg = budAdjust.getDistributionLinkOrg().getId();
          }
          // get cost centre
          SQLQuery costCentreQuery = OBDal.getInstance().getSession().createSQLQuery(
              "select budgetcontrol_costcenter from efin_budget_ctrl_param where ad_client_Id ='"
                  + budAdjust.getClient().getId() + "' ");
          List costCenterList = costCentreQuery.list();
          if (costCentreQuery != null && costCenterList.size() > 0) {
            Object objCostCentre = costCenterList.get(0);
            if (objCostCentre != null) {
              strCostCentre = objCostCentre.toString();
            }
          }
          for (BudgetAdjustmentLine objLines : budAdjust.getEfinBudgetAdjlineList()) {
            if (objLines.getIncrease().compareTo(BigDecimal.ZERO) == 1) {
              AccountingCombination objActCombination = objLines.getAccountingCombination();
              // fetch distribution org
              String query = "select ad_org_id from ad_org where ad_org_id in (select ad_org_id from c_validcombination "
                  + "where c_salesregion_id = '" + strCostCentre + "' and account_id ='"
                  + objActCombination.getAccount().getId() + "') " + " and ad_org_id ='" + distOrg
                  + "'";
              SQLQuery distOrgQuery = OBDal.getInstance().getSession().createSQLQuery(query);
              List distOrgList = distOrgQuery.list();
              if (distOrgQuery != null && distOrgList.size() > 0) {
                Object objDistOrg = distOrgList.get(0);
                if (objDistOrg != null) {
                  strDistOrg = objDistOrg.toString();
                }
              }
              if (strDistOrg != null) {
                objLines.setDistribute(true);
                objLines.setDislinkorg(OBDal.getInstance().get(Organization.class, strDistOrg));
              } else {
                objLines.setDistribute(false);
                objLines.setDislinkorg(null);
              }
              isDistributed = true;

            }

          }
          if (isDistributed) {
            result.put("Message", "Success");
          } else {
            result.put("Message", "NoLines");
          }
        }

        if ("getDistributeFlag".equals(action) && !BudgetAdjustId.isEmpty()) {
          BudgetAdjustment budAdjust = OBDal.getInstance().get(BudgetAdjustment.class,
              BudgetAdjustId);
          if (budAdjust != null) {
            if (budAdjust.getEfinBudgetAdjlineList().size() > 0) {
              result.put("isNoFlag", false);
            } else {
              result.put("isNoFlag", true);
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in BudgetAdjustmentDistributeAllCallout :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
