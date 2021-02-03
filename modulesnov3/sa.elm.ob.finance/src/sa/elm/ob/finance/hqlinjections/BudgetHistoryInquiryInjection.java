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

/**
 * 
 * @author Gopalakrishnan on 13/11/2017
 *
 */
@ComponentProvider.Qualifier("DC207E812535404E95CBEB110A82B32A")
public class BudgetHistoryInquiryInjection extends HqlQueryTransformer {
  /**
   * This Injection class was responsible to feed data to table Efin_BudgetHistory
   */

  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "DC207E812535404E95CBEB110A82B32A";
  Logger log = Logger.getLogger(BudgetHistoryInquiryInjection.class);

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
    String transactionType = requestParameters.get("Doctype");

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
    selectClause.append(getAggregatorFunction("his.id") + " as  Efin_budgethistory_ID, ");
    selectClause.append(
        "  his.documentNo as documentno,his.client.id as ad_client_id , his.organization.id as ad_org_id ,");
    selectClause.append(" his.active as isactive,");
    selectClause.append(
        " his.documenttype as documenttype, his.salesCampaign.id as c_campaign_id ,(his.salesCampaign.searchKey||'-'||his.salesCampaign.name) as budgetType, his.year.fiscalYear as yearvalue,his.year.id as c_year_id ,coalesce( his.description,'') as description, ");
    selectClause.append(" his.createdBy.id as createdBy,  ");

    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(his.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,");
    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(his.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))  as creationDate ,");
    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(his.acctDate,'YYYY-MM-DD'),'YYYY-MM-DD'))  as acctDate , ");
    selectClause.append(
        " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(his.transactiondate,'YYYY-MM-DD'),'YYYY-MM-DD'))  as transactiondate ,  ");

    selectClause.append(
        " his.alertStatus as status, his.transferSource as transferrsoruce ,his.accountingCombination.id as c_validcombination_id , his.accountingCombination.efinUniqueCode as uniquecode, his.increase as increase , his.decrease as decrease ");
    /*
     * if(hasSelectedIds) { // if there are selected ids selection is done in the client.
     * selectClause.append(" case when 1 < 0 then true else false end as OB_Selected "); } else {
     * selectClause .append(
     * " case when max(revln.id) is not null then true else false end as OB_Selected "); }
     */

    return selectClause;
  }

  protected StringBuffer getWhereClause(Map<String, String> requestParameters) {
    String doctype = requestParameters.get("Doctype");
    String budgetTypeId = requestParameters.get("C_Campaign_ID");
    String yearId = requestParameters.get("C_Year_ID");
    String fromAcct = requestParameters.get("FromAccount");
    String toAcct = requestParameters.get("ToAccount");

    StringBuffer whereClause = new StringBuffer();
    // Create WhereClausehis.accountingCombination.efinUniqueCode as uniquecode
    whereClause.append("  his.alertStatus in ('CO','EFIN_WFA','EFIN_AP','APP') ");
    if (doctype != null && !"null".equals(doctype)) {
      whereClause.append(" and his.documenttype='" + doctype + "'");
    }
    if (budgetTypeId != null && !"null".equals(budgetTypeId)) {
      whereClause.append(" and his.salesCampaign.id='" + budgetTypeId + "'");
    }

    if (yearId != null && !"null".equals(yearId)) {
      whereClause.append(" and his.year.id='" + yearId + "'");
    }
    if (fromAcct != null && toAcct != null && !"null".equals(fromAcct) && !"null".equals(toAcct)) {
      whereClause.append(
          " and his.accountingCombination.account.searchKey >= ( select searchKey from FinancialMgmtElementValue where id = '"
              + fromAcct
              + "')  and his.accountingCombination.account.searchKey  <= (  select searchKey from FinancialMgmtElementValue where id = '"
              + toAcct + "') ");
    }
    if (fromAcct != null && !"null".equals(fromAcct)) {
      whereClause.append(
          " and his.accountingCombination.account.searchKey >= ( select searchKey from FinancialMgmtElementValue where id = '"
              + fromAcct + "')");

    }

    log.debug("whereClause:" + whereClause);
    return whereClause;

  }

  protected StringBuffer getGroupByClause() {
    StringBuffer groupByClause = new StringBuffer();
    groupByClause.append(" his.id, ");
    groupByClause.append(
        " his.documentNo,his.client.id, his.organization.id ,his.active, his.salesCampaign.name , his.year.fiscalYear,his.createdBy.name,");
    groupByClause.append(
        "  his.transactiondate,his.documenttype,his.salesCampaign.id, his.year.id,coalesce( his.description,''),");
    groupByClause.append(
        " his.createdBy.id,his.creationDate, his.updated,his.alertStatus, his.transferSource,");
    groupByClause.append(
        " his.accountingCombination.id,  his.accountingCombination.efinUniqueCode ,his.salesCampaign.searchKey,his.increase,his.decrease,his.acctDate,his.transactiondate");
    return groupByClause;

  }

  protected String removeGridFilters(String _hqlQuery) {
    String hqlQuery = _hqlQuery;
    // Get the substring of grid filter inside where clause, if transaction type is "Orders" or
    // "Invoices", put in the having clause
    int whereIndex = hqlQuery.indexOf(" where ");
    int orgFilterIndex = hqlQuery.indexOf(" his.organization in ", whereIndex);
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
      havingGridFilters = havingGridFilters.replaceAll("transferrsoruce", "his.transferrsoruce");
    }
    if (havingGridFilters.contains("yearvalue")) {
      havingGridFilters = havingGridFilters.replaceAll("yearvalue", "his.year.fiscalYear");
    }
    if (havingGridFilters.contains("budgetType")) {
      havingGridFilters = havingGridFilters.replaceAll("budgetType",
          "his.salesCampaign.searchKey||'-'||his.salesCampaign.name");
    }
    if (havingGridFilters.contains("acctDate")) {
      havingGridFilters = havingGridFilters.replaceAll("acctDate", "his.acctDate");
    }

    if (havingGridFilters.contains("description")) {
      havingGridFilters = havingGridFilters.replaceAll("description",
          "coalesce( his.description,'')");
    }
    if (havingGridFilters.contains("status")) {
      havingGridFilters = havingGridFilters.replaceAll("status", "his.alertStatus");
    }
    if (havingGridFilters.contains("uniquecode")) {
      havingGridFilters = havingGridFilters.replaceAll("uniquecode",
          "  his.accountingCombination.efinUniqueCode");
    }

    if (havingGridFilters.contains("creationDate")) {
      havingGridFilters = havingGridFilters.replaceAll("creationDate", "his.creationDate");
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
