package sa.elm.ob.finance.util.DAO;

import java.util.Iterator;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kousalya on 03/02/2018
 */

public class CommonReportDAO {
  private static final Logger log4j = LoggerFactory.getLogger(CommonReportDAO.class);

  /**
   * Get Budget Year
   * 
   * @param clientId
   * @param orgId
   * @return JSONArray
   */
  @SuppressWarnings("rawtypes")
  public static JSONArray getBudgetYear(String clientId, String orgId) {
    JSONArray jsonArray = new JSONArray();
    StringBuilder query = null;
    Query budYrQuery = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuilder();
      query.append(
          " select budyr.year.id, yr.fiscalYear, budyr.id, budyr.commercialName from Efin_budgetint budyr "
              + " left join budyr.year yr where  budyr.client.id=:clientId "
              + " group by budyr.year.id, yr.fiscalYear, budyr.id, budyr.commercialName order by yr.fiscalYear desc");
      budYrQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      if (budYrQuery != null) {
        budYrQuery.setParameter("clientId", clientId);
      }
      log4j.debug(" Query : " + query.toString());
      log4j.debug(" orgId:" + orgId + "//clientId:" + clientId);
      if (budYrQuery != null) {
        if (budYrQuery.list().size() > 0) {
          for (Iterator iterator = budYrQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            JSONObject jsonData = new JSONObject();
            jsonData.put("yearId", objects[2].toString());
            jsonData.put("year", objects[3].toString());
            jsonArray.put(jsonData);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getBudgetYear ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonArray;
  }

  /**
   * Get Budget Accounts
   * 
   * @param clientId
   * @param roleId
   * @return JSONArray
   */
  @SuppressWarnings("rawtypes")
  public static JSONArray getBudgetAccounts(String clientId, String roleId, String orgId,
      String yearId) {
    JSONArray jsonArray = new JSONArray();
    StringBuilder query = null;
    Query budAcctQuery = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuilder();
      query.append(
          " select (value||'-'||name||'-'||accounttype) as account, e.id as accountid from FinancialMgmtElementValue e where "
              + " e.summaryLevel='N' and e.id in (select a.account.id from FinancialMgmtAccountingCombination a "
              + " where a.client.id =:clientId and a.efinDimensiontype ='E') "
              + " and e.id in (select act.elementvalue.id from Efin_Security_Rules_Act act "
              + " join act.efinSecurityRules ru where ru.id=(select efinSecurityRules.id from ADRole rl where rl.id=:roleId )and efin_processbutton='Y')");
      // Chapter 3 and 4
      if (yearId != null) {
        query.append(" and e.id in (select bgln.accountElement.id from EFIN_Budget bg "
            + " left join bg.eFINBudgetLinesList bgln left join bg.salesCampaign camp "
            + " where camp.efinBudgettype in ('C', 'F') and bg.client.id=:clientId and bg.organization.id=:orgId and bg.efinBudgetint.id=:yearId)");
      }

      budAcctQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      if (budAcctQuery != null) {
        budAcctQuery.setParameter("clientId", clientId);
        budAcctQuery.setParameter("roleId", roleId);
        if (yearId != null) {
          budAcctQuery.setParameter("orgId", orgId);
          budAcctQuery.setParameter("yearId", yearId);
        }
      }
      log4j.debug(" clientId>" + clientId);
      log4j.debug(" roleId>" + roleId);
      log4j.debug(" orgId>" + orgId);
      log4j.debug(" yearId>" + yearId);
      log4j.debug(" Query : " + query.toString());
      if (budAcctQuery != null) {
        if (budAcctQuery.list().size() > 0) {
          for (Iterator iterator = budAcctQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            JSONObject jsonData = new JSONObject();
            jsonData.put("recordIdentifier", objects[0].toString());
            jsonData.put("id", objects[1].toString());
            jsonArray.put(jsonData);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getBudgetYear ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonArray;
  }
}
