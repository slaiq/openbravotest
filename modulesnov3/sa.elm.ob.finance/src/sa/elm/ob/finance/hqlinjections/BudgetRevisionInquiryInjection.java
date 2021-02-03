package sa.elm.ob.finance.hqlinjections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

@ComponentProvider.Qualifier("EE7E97D5C15E4721BEFEFE3572EE535B")
public class BudgetRevisionInquiryInjection extends HqlQueryTransformer {

  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "EE7E97D5C15E4721BEFEFE3572EE535B";
  Logger log = Logger.getLogger(BudgetRevisionInquiryInjection.class);

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    List<String> selectedPSDs = new ArrayList<String>();
    boolean hasCriteria = requestParameters.containsKey("criteria");

    log.debug("hascriteria:" + hasCriteria);

    JSONObject criteria = new JSONObject();
    if (hasCriteria) {
      try {
        criteria = JsonUtils.buildCriteria(requestParameters);
        transformCriteria(criteria, selectedPSDs);
      } catch (JSONException ignore) {
      }
    }
    boolean hasSelectedIds = !selectedPSDs.isEmpty();
    String hqlQuery = _hqlQuery;
    // Retrieve Parameters
    String transactionType = requestParameters.get("doctype");

    if (hasCriteria) {
      hqlQuery = removeGridFilters(hqlQuery);
      queryNamedParameters.clear();
      hqlQuery = calculateHavingClause(hqlQuery, transactionType, criteria, queryNamedParameters);
    } else {
      hqlQuery = hqlQuery.replace("@havingClause@", "");
    }

    StringBuffer selectClause = getSelectClause(hasSelectedIds);
    StringBuffer whereClause = getWhereClause(requestParameters);
    StringBuffer groupByClause = getGroupByClause();

    String transformedHql = hqlQuery.replace("@selectClause@", selectClause.toString());
    transformedHql = transformedHql.replace("@whereClause@", whereClause.toString());
    transformedHql = transformedHql.replace("@groupByClause@", groupByClause.toString());

    log.debug("transformedHql:" + transformedHql);

    return transformedHql;
  }

  protected StringBuffer getSelectClause(boolean hasSelectedIds) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append(getAggregatorFunction("revln.id") + " as  Efin_Budget_Transfertrxline, ");
    selectClause.append(
        " revln.efinBudgetTransfertrx.id as Efin_Budget_Transfertrx_id , rev.documentNo as documentno,rev.client.id as ad_client_id , rev.organization.id as ad_org_id ,");
    selectClause.append(" rev.active as active,");
    selectClause.append(
        " rev.docType as doctype, rev.salesCampaign.id as C_Campaign_ID , (rev.salesCampaign.searchKey||'-'||rev.salesCampaign.name) as budgetType, rev.year.fiscalYear as yearvalue ,rev.year.id as C_Year_ID ,rev.description as description, rev.comments as note, ");
    selectClause.append(
        "  rev.decisionnumber as decisionnumber, rev.reportnumber as reportnumber ,rev.add1 as add1, rev.add2 as add2 , rev.add3 as add3 , rev.add4 as add4  ,revln.createdBy.id as createdBy , revln.createdBy.name as creater, ");

    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(revln.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,");
    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(revln.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))  as creationDate ,");
    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(rev.accountingDate,'YYYY-MM-DD'),'YYYY-MM-DD'))  as acctDate , ");
    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(rev.trxdate,'YYYY-MM-DD'),'YYYY-MM-DD'))  as trxdate ,  ");

    selectClause.append(
        " rev.docStatus as status, rev.transferSource as transferrsoruce ,revln.accountingCombination.efinUniqueCode as uniquecode , revln.accountingCombination.account.searchKey as account, revln.increase as increase , revln.decrease as decrease"
            + ",rev.action as action ,revln.budgetLines.id as budgetlines");
    /*
     * if(hasSelectedIds) { // if there are selected ids selection is done in the client.
     * selectClause.append(" case when 1 < 0 then true else false end as OB_Selected "); } else {
     * selectClause .append(
     * " case when max(revln.id) is not null then true else false end as OB_Selected "); }
     */

    return selectClause;
  }

  protected StringBuffer getWhereClause(Map<String, String> requestParameters) {
    String doctype = requestParameters.get("doctype");
    String budgetTypeId = requestParameters.get("C_Campaign_ID");
    String yearId = requestParameters.get("c_year_id");
    String fromAcct = requestParameters.get("From Account");
    String toAcct = requestParameters.get("To Account");

    StringBuffer whereClause = new StringBuffer();
    // Create WhereClause
    whereClause.append("  rev.docStatus in ('CO','WFA') ");
    if (doctype != null && !"null".equals(doctype)) {
      whereClause.append(" and rev.docType='" + doctype + "'");
    }
    if (budgetTypeId != null && !"null".equals(budgetTypeId)) {
      whereClause.append(" and rev.salesCampaign.id='" + budgetTypeId + "'");
    }

    if (yearId != null && !"null".equals(yearId)) {
      whereClause.append(" and rev.year.id='" + yearId + "'");
    }
    if (fromAcct != null && toAcct != null && !"null".equals(fromAcct) && !"null".equals(toAcct)) {
      whereClause.append(
          " and revln.accountingCombination.account.searchKey >= ( select searchKey from FinancialMgmtElementValue where id = '"
              + fromAcct
              + "')  and revln.accountingCombination.account.searchKey  <= (  select searchKey from FinancialMgmtElementValue where id = '"
              + toAcct + "') ");
    }
    if (fromAcct != null && !"null".equals(fromAcct)) {
      whereClause.append(
          " and revln.accountingCombination.account.searchKey >= ( select searchKey from FinancialMgmtElementValue where id = '"
              + fromAcct + "')");

    }

    log.debug("whereClause:" + whereClause);
    return whereClause;

  }

  protected StringBuffer getGroupByClause() {
    StringBuffer groupByClause = new StringBuffer();
    groupByClause.append(" revln.efinBudgetTransfertrx.id, ");
    groupByClause.append(
        " rev.documentNo,rev.client.id, rev.organization.id ,rev.active, rev.salesCampaign.name , rev.year.fiscalYear,revln.createdBy.name,");
    groupByClause.append(
        "  rev.trxdate,rev.docType,rev.salesCampaign.id, rev.year.id,rev.description ,rev.comments, ");
    groupByClause.append(
        "  rev.decisionnumber ,rev.reportnumber ,rev.add1, rev.add2,rev.add3,rev.add4,rev.accountingDate,revln.createdBy.id,revln.creationDate, revln.updated,rev.docStatus, rev.transferSource,");
    groupByClause.append(
        " revln.accountingCombination.efinUniqueCode,  revln.accountingCombination.account.searchKey ,rev.salesCampaign.searchKey,revln.increase,revln.decrease,rev.action,revln.budgetLines.id");
    return groupByClause;

  }

  protected String removeGridFilters(String _hqlQuery) {
    String hqlQuery = _hqlQuery;
    // Get the substring of grid filter inside where clause, if transaction type is "Orders" or
    // "Invoices", put in the having clause
    int whereIndex = hqlQuery.indexOf(" where ");
    int orgFilterIndex = hqlQuery.indexOf(" revln.organization in ", whereIndex);
    int beginIndex = hqlQuery.indexOf(" AND ", orgFilterIndex);
    int endIndex = hqlQuery.indexOf("and @whereClause@");
    if (beginIndex != -1) {
      String gridFilters = hqlQuery.substring(beginIndex, endIndex);
      hqlQuery = hqlQuery.replace(gridFilters, " ");
    }
    return hqlQuery;
  }

  protected String calculateHavingClause(String _hqlQuery, String transactionType,
      JSONObject criteria, Map<String, Object> queryNamedParameters) {
    String hqlQuery = _hqlQuery;
    StringBuffer havingClause = new StringBuffer();
    AdvancedQueryBuilder queryBuilder = new AdvancedQueryBuilder();
    queryBuilder.setEntity(ModelProvider.getInstance().getEntityByTableId(TABLE_ID));
    queryBuilder.setCriteria(criteria);
    String havingGridFilters = queryBuilder.getWhereClause();
    queryNamedParameters.putAll(queryBuilder.getNamedParameters());
    if (!havingGridFilters.trim().isEmpty()) {
      // if the filter where clause contains the string 'where', get rid of it
      havingGridFilters = havingGridFilters.replaceAll("(?i)WHERE", " ");
    }

    // replace the property names with the column alias
    Table table = OBDal.getInstance().get(Table.class, TABLE_ID);
    havingGridFilters = replaceParametersWithAlias(table, havingGridFilters);

    if (havingGridFilters.contains("transferrsoruce")) {
      havingGridFilters = havingGridFilters.replaceAll("transferrsoruce", "rev.transferSource");
    }
    if (havingGridFilters.contains("yearvalue")) {
      havingGridFilters = havingGridFilters.replaceAll("yearvalue", "rev.year.fiscalYear");
    }
    if (havingGridFilters.contains("budgetType")) {
      havingGridFilters = havingGridFilters.replaceAll("budgetType",
          "rev.salesCampaign.searchKey||'-'||rev.salesCampaign.name");
    }
    if (havingGridFilters.contains("acctDate")) {
      havingGridFilters = havingGridFilters.replaceAll("acctDate", "rev.accountingDate");
    }

    if (havingGridFilters.contains("description")) {
      havingGridFilters = havingGridFilters.replaceAll("description", "rev.description");
    }
    if (havingGridFilters.contains("status")) {
      havingGridFilters = havingGridFilters.replaceAll("status", "rev.docStatus");
    }
    if (havingGridFilters.contains("creater")) {
      havingGridFilters = havingGridFilters.replaceAll("creater", "revln.createdBy.name");
    }
    if (havingGridFilters.contains("creationDate")) {
      havingGridFilters = havingGridFilters.replaceAll("creationDate", "revln.creationDate");
    }
    if (havingGridFilters != null && !"".equals(havingGridFilters.trim())) {
      havingClause.append(" having ( " + havingGridFilters + " )");
    }
    hqlQuery = hqlQuery.replace("@havingClause@", havingClause.toString());
    return hqlQuery;
  }

  protected String replaceParametersWithAlias(Table table, String whereClause) {
    if (whereClause.trim().isEmpty()) {
      return whereClause;
    }
    String updatedWhereClause = whereClause.toString();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    for (Column column : table.getADColumnList()) {
      // look for the property name, replace it with the column alias
      Property property = entity.getPropertyByColumnName(column.getDBColumnName());
      Map<String, String> replacementMap = new HashMap<String, String>();
      String propertyNameBefore = null;
      String propertyNameAfter = null;
      if (property.isPrimitive()) {
        // if the property is a primitive, just replace the property name with the column alias
        propertyNameBefore = property.getName();
        propertyNameAfter = column.getEntityAlias();
      } else {
        // if the property is a FK, then the name of the identifier property of the referenced
        // entity has to be appended

        if (column.isLinkToParentColumn()) {
          propertyNameBefore = property.getName() + "." + JsonConstants.ID;
          propertyNameAfter = column.getEntityAlias() + "." + JsonConstants.ID;
        } else {
          Entity refEntity = property.getReferencedProperty().getEntity();
          String identifierPropertyName = refEntity.getIdentifierProperties().get(0).getName();
          propertyNameBefore = property.getName() + "." + identifierPropertyName;
          propertyNameAfter = column.getEntityAlias() + "." + identifierPropertyName;
        }

      }
      replacementMap.put(" " + propertyNameBefore + " ", " " + propertyNameAfter + " ");
      replacementMap.put("(" + propertyNameBefore + ")", "(" + propertyNameAfter + ")");
      replacementMap.put("(" + propertyNameBefore + " ", "(" + propertyNameAfter + " ");
      for (String toBeReplaced : replacementMap.keySet()) {
        if (updatedWhereClause.contains(toBeReplaced)) {
          updatedWhereClause = updatedWhereClause.replace(toBeReplaced,
              replacementMap.get(toBeReplaced));
        }
      }
    }
    return updatedWhereClause;
  }

  protected void transformCriteria(JSONObject buildCriteria, List<String> selectedPSDs)
      throws JSONException {
    JSONArray criteriaArray = buildCriteria.getJSONArray("criteria");
    JSONArray newCriteriaArray = new JSONArray();
    for (int i = 0; i < criteriaArray.length(); i++) {
      JSONObject criteria = criteriaArray.getJSONObject(i);
      if (criteria.has("fieldName") && criteria.getString("fieldName").equals("id")
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
