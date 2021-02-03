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
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

@ComponentProvider.Qualifier("BD021391872940469DB3ACAD335FD702")
public class EfinApplyPrepaymentInjection extends HqlQueryTransformer {

  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "BD021391872940469DB3ACAD335FD702";
  Logger log = Logger.getLogger(EfinApplyPrepaymentInjection.class);

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
    StringBuffer selectClause = getSelectClause(hasSelectedIds);
    StringBuffer whereClause = getWhereClause(requestParameters);

    String transformedHql = hqlQuery.replace("@selectClause@", selectClause.toString());
    transformedHql = transformedHql.replace("@whereClause@", whereClause.toString());

    log.debug("transformedHql:" + transformedHql);

    return transformedHql;
  }

  protected StringBuffer getSelectClause(boolean hasSelectedIds) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append(" inv.id as  Efin_Apply_Prepayment_ID, ");
    selectClause.append(
        " inv.documentNo as documentno ,inv.efinPreRemainingamount as em_efin_pre_remainingamount, ");
    selectClause.append(
        " inv.efinPreUsedamount as em_efin_pre_usedamount,inv.efinPreRemainingamount as amount,");
    selectClause.append(" inv.client.id as ad_client_id , inv.organization.id as ad_org_id ,");
    selectClause.append(" inv.active as active, ");
    selectClause.append(" inv.updated as updated ,");
    selectClause.append(" inv.creationDate as creationDate ");
    return selectClause;
  }

  protected StringBuffer getWhereClause(Map<String, String> requestParameters) {
    String inpEncumbranceId = requestParameters.get("@Invoice.efinManualencumbrance@");
    String invoiceId = requestParameters.get("@Invoice.id@");
    Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
    String inpCurrencyId = requestParameters.get("@Invoice.currency@");

    StringBuffer whereClause = new StringBuffer();
    // Create WhereClause
    whereClause
        .append(" inv.outstandingAmount=0 and inv.efinPreRemainingamount > 0 and inv.posted='Y' "
            + " and inv.id not in (select gl.efinCInvoice.id from FinancialMgmtGLJournal as gl where gl.efinCInvoice is not null) ");
    if (inpEncumbranceId != null && !"null".equals(inpEncumbranceId)) {
      whereClause.append(" and enc.id='" + inpEncumbranceId + "'");
    }
    if (invoice.getBusinessPartner() != null) {
      whereClause
          .append(" and inv.businessPartner.id='" + invoice.getBusinessPartner().getId() + "'");
    }
    if (inpCurrencyId != null && !"null".equals(inpCurrencyId)) {
      whereClause.append(" and inv.currency.id='" + inpCurrencyId + "'");
    } else {
      whereClause.append(" and 1=1");
    }
    log.debug("whereClause:" + whereClause);
    return whereClause;

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
