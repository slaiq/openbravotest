package sa.elm.ob.finance.ad_reports.budgetrevisionsummary;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class BudgetRevisionSummaryDAO {
  private static Logger log4j = Logger.getLogger(BudgetRevisionSummaryDAO.class);

  @SuppressWarnings("rawtypes")
  public static JSONObject getBudgetRevisions(String clientId, String orgId, String yearId,
      String revId, String transacType) {
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    StringBuilder query = null;
    Query budRevQuery = null;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      query = new StringBuilder();

      query.append(" select rev.id, rev.documentNo, rev.docType from Efin_Budget_Transfertrx rev "
          + " where rev.efinBudgetint.id=:yearId and rev.organization.id=:orgId and rev.client.id=:clientId ");
      if (transacType.equals("TRS"))
        query.append(" and rev.docType='TRS' ");
      if (revId != null)
        query.append(" and rev.id=:revId");
      budRevQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      if (budRevQuery != null) {
        budRevQuery.setParameter("yearId", yearId);
        budRevQuery.setParameter("orgId", orgId);
        budRevQuery.setParameter("clientId", clientId);
        if (revId != null)
          budRevQuery.setParameter("revId", revId);
      }
      log4j.debug(" Query : " + query.toString());
      log4j.debug(" orgId> " + orgId + ":clientId>" + clientId + ":yearId>" + yearId);
      if (budRevQuery != null) {
        if (budRevQuery.list().size() > 0) {
          for (Iterator iterator = budRevQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            JSONObject jsonData = new JSONObject();
            jsonData.put("id", objects[0].toString());
            jsonData.put("recordIdentifier", objects[1].toString());
            jsonData.put("tranxType", objects[2].toString());
            jsonArray.put(jsonData);
          }
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");
    } catch (Exception e) {
      log4j.error("Exception in getBudgetRevisions ", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsob;
  }
}