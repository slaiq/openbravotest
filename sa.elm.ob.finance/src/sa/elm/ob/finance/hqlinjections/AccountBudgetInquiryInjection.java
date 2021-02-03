package sa.elm.ob.finance.hqlinjections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

@ComponentProvider.Qualifier("F371FF06AAA84358AAFDD98CD14C4DC4")
public class AccountBudgetInquiryInjection extends HqlQueryTransformer {

	final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
	final static String TABLE_ID = "F371FF06AAA84358AAFDD98CD14C4DC4";
	Logger log = Logger.getLogger(AccountBudgetInquiryInjection.class);

	@Override
	public String transformHqlQuery(String _hqlQuery,
			Map<String, String> requestParameters,
			Map<String, Object> queryNamedParameters) {
		List<String> selectedPSDs = new ArrayList<String>();
		boolean hasCriteria = requestParameters.containsKey("criteria");
		JSONObject criteria = new JSONObject();
		if (hasCriteria) {
			try {
				criteria = JsonUtils.buildCriteria(requestParameters);
				transformCriteria(criteria, selectedPSDs);
			} catch (JSONException ignore) {
			}
		}
		String hqlQuery = _hqlQuery;
		// Retrieve Parameters
		String transactionType = requestParameters.get("doctype");
		String strJustCount = requestParameters.get("_justCount");
		boolean justCount = strJustCount.equalsIgnoreCase("true");

		StringBuffer whereClause = getWhereClause(requestParameters);
		List<String> orderByClauses = new ArrayList<String>();
		if (!justCount) {
			orderByClauses = getOrderByClauses(transactionType, selectedPSDs,
					requestParameters);
		}

		// Remove alias @@ from Order By clause
		if (requestParameters.containsKey("_sortBy")) {
			String sortBy = requestParameters.get("_sortBy");
			if (sortBy.startsWith("-")) {
				sortBy = sortBy.substring(1);
			}
			hqlQuery = hqlQuery.replace("@" + sortBy + "@", sortBy);
		}

		String transformedHql = hqlQuery.replace("@whereClause@",
				whereClause.toString());
		transformedHql = appendOrderByClauses(transformedHql, orderByClauses,
				justCount);

		log.debug("transformedHql:" + transformedHql);

		return transformedHql;
	}

	protected StringBuffer getWhereClause(Map<String, String> requestParameters) {
		String Id = requestParameters.get("@EFIN_BudgetLines.id@");
		StringBuffer whereClause = new StringBuffer();
		// Create WhereClause
		whereClause.append(" tr.id='" + Id + "'");
		return whereClause;
	}

	private List<String> getOrderByClauses(String transactionType,List<String> selectedPSDs, Map<String, String> requestParameters) {
		List<String> orderByClauses = new ArrayList<String>();
		if (!requestParameters.containsKey("_sortBy")) {
			orderByClauses.add("AccountDate");
		} else {
			orderByClauses.add(" ");
		}
		return orderByClauses;
	}

	private String appendOrderByClauses(String _hqlQuery,List<String> orderByClauses, boolean justCount) {
		final String orderby = " ORDER BY ";
		String hqlQuery = _hqlQuery;
		if (!justCount) {
			if (hqlQuery.contains(orderby)) {
				int offset = hqlQuery.indexOf(orderby) + orderby.length();
				StringBuilder sb = new StringBuilder(hqlQuery);
				sb.insert(offset, orderByClauses.get(0) + " ");
				hqlQuery = sb.toString();
				orderByClauses.remove(0);
			} else {
				hqlQuery = hqlQuery.concat(orderby);
			}
			for (String clause : orderByClauses) {
				hqlQuery = hqlQuery.concat(clause);
			}
		}
		return hqlQuery;
	}

	protected String removeGridFilters(String _hqlQuery) {
		String hqlQuery = _hqlQuery;
		// Get the substring of grid filter inside where clause, if transaction
		// type is "Orders" or
		// "Invoices", put in the having clause
		int whereIndex = hqlQuery.indexOf(" where ");
		int orgFilterIndex = hqlQuery.indexOf(" tr.organization in ",
				whereIndex);
		int beginIndex = hqlQuery.indexOf(" AND ", orgFilterIndex);
		int endIndex = hqlQuery.indexOf("and @whereClause@");
		if (beginIndex != -1) {
			String gridFilters = hqlQuery.substring(beginIndex, endIndex);
			hqlQuery = hqlQuery.replace(gridFilters, " ");
		}
		return hqlQuery;
	}

	protected void transformCriteria(JSONObject buildCriteria,List<String> selectedPSDs) throws JSONException {
		JSONArray criteriaArray = buildCriteria.getJSONArray("criteria");
		JSONArray newCriteriaArray = new JSONArray();
		for (int i = 0; i < criteriaArray.length(); i++) {
			JSONObject criteria = criteriaArray.getJSONObject(i);
			if (criteria.has("fieldName")
					&& criteria.getString("fieldName").equals("id")
					&& criteria.has("value")) {
				String value = criteria.getString("value");
				for (String psdID : value.split(",")) {
					JSONObject newCriteria = criteria;
					newCriteria.put("value", psdID.trim());
					newCriteria.put("operator", "iContains");
					selectedPSDs.add(psdID.trim());
					newCriteriaArray.put(newCriteria);
				}
			} else {
				newCriteriaArray.put(criteria);
			}
		}
		buildCriteria.put("criteria", newCriteriaArray);
	}

	protected String getAggregatorFunction(String expression) {
		return " hqlagg(" + expression + ")";
	}

}
