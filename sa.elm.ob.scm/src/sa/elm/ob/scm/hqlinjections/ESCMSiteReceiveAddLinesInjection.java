package sa.elm.ob.scm.hqlinjections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

@ComponentProvider.Qualifier("DA51A1A4754F4C72AA3E8C172807E9E9")
public class ESCMSiteReceiveAddLinesInjection extends HqlQueryTransformer {

  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "DA51A1A4754F4C72AA3E8C172807E9E9";
  Logger log = Logger.getLogger(ESCMSiteReceiveAddLinesInjection.class);

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    List<String> selectedPSDs = new ArrayList<String>();
    boolean hasCriteria = requestParameters.containsKey("criteria");
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

    JSONObject criteria = new JSONObject();
    if (hasCriteria) {
      try {
        criteria = JsonUtils.buildCriteria(requestParameters);
        transformCriteria(criteria, selectedPSDs);
      } catch (JSONException ignore) {
      }
    }
    String strJustCount = requestParameters.get("_justCount");
    boolean justCount = strJustCount.equalsIgnoreCase("true");
    boolean hasSelectedIds = !selectedPSDs.isEmpty();
    String hqlQuery = _hqlQuery;
    List<String> orderByClauses = new ArrayList<String>();

    if (!justCount) {
      orderByClauses = getOrderByClauses(selectedPSDs, requestParameters);
    }

    // Remove alias @@ from Order By clause
    if (requestParameters.containsKey("_sortBy")) {
      String sortBy = requestParameters.get("_sortBy");
      if (sortBy.startsWith("-")) {
        sortBy = sortBy.substring(1);
      }
      hqlQuery = hqlQuery.replace("@" + sortBy + "@", sortBy);
    }
    StringBuffer selectClause = getSelectClause(hasSelectedIds, requestParameters);
    StringBuffer whereClause = getWhereClause(requestParameters, vars);
    // StringBuffer groupByClause = getGroupByClause();

    String transformedHql = hqlQuery.replace("@selectClause@", selectClause.toString());
    transformedHql = transformedHql.replace("@whereClause@", whereClause.toString());
    // transformedHql = transformedHql.replace("@groupByClause@", groupByClause.toString());
    transformedHql = appendOrderByClauses(transformedHql, orderByClauses, justCount);
    log.debug("transformedHql:" + transformedHql);

    return transformedHql;
  }

  protected StringBuffer getSelectClause(boolean hasSelectedIds,
      Map<String, String> requestParameters) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append("receive.id as  escm_sitereceive_v_id, ");
    selectClause.append(
        "  receive.client.id as ad_client_id,receive.organization.id as ad_org_id,   receive.active  as isactive, receive.documentNo as documentNo,receive.createdBy.id as createdBy , "
            + "   receive.createdBy.name as creater,   (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(receive.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,       (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "           from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(receive.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))        as creationDate ,receive.updatedBy.id as updatedBy ,    receive.updatedBy.name as updater, "
            + "      receive.rownum as line ,TO_DATE(receive.movementDate,'DD-MM-YYYY') as movementdate ,receive.searchKey as value,receive.product.id as m_product_id,receive.description as description,receive.issuedqty as issuedqty, "
            + "       receive.reservedQuantity as reservedQuantity ,receive.userContact.id as ad_user_id,receive.goodsShipment.id as m_inout_id ,receive.uOM.id as c_uom_id,receive.uOM.name as uomname");
    String headerId = requestParameters.get("@Escm_Material_Request.id@");
    if (headerId != null) {
      selectClause.append(
          " , coalesce( ( select e.requestedQty from Escm_Material_Reqln e left join e.escmMaterialRequest req "
              + " where req.id='" + headerId
              + "'  and e.escmInitialreceipt.id= receive.id ),receive.reqqty) as reqqty ,  (receive.inprgqty -(coalesce( ( select e.requestedQty from Escm_Material_Reqln e left join e.escmMaterialRequest req "
              + " where req.id='" + headerId
              + "' and req.alertStatus ='ESCM_IP'  and e.escmInitialreceipt.id= receive.id ),receive.reqqty)) )as inprgqty ");
    }
    return selectClause;
  }

  protected StringBuffer getWhereClause(Map<String, String> requestParameters,
      VariablesSecureApp vars) {
    // String headerId = requestParameters.get("@Escm_Material_Request.id@");
    Boolean ispreference = false;
    /*
     * Boolean ispreference = Preferences.existsPreference("ESCM_LineManager", true, null, null,
     * null, vars.getRole(), null);
     */

    String preferenceValue = "";
    try {
      preferenceValue = Preferences.getPreferenceValue("ESCM_LineManager", true, vars.getClient(),
          vars.getOrg(), vars.getUser(), vars.getRole(), "B81CF41736534BA796E4A9D729CF9F65");
    } catch (PropertyException e) {
    }
    if (preferenceValue != null && preferenceValue.equals("Y"))
      ispreference = true;

    StringBuffer whereClause = new StringBuffer();
    // Create WhereClause
    log.debug("ispreference:" + ispreference);
    if (!ispreference) {
      // Removed for Production live comments
      /*
       * whereClause .append("  receive.userContact.id='" + vars.getUser() +
       * "'   AND (receive.reservedQuantity - COALESCE(receive.issuedqty, 0) - COALESCE(receive.inprgqty, 0)) > 0 "
       * );
       */

      whereClause.append(
          " 1=1 and (receive.reservedQuantity - COALESCE(receive.issuedqty, 0) - COALESCE(receive.inprgqty, 0)) > 0 ");

    } else {
      // Removed for Production live comments
      /*
       * whereClause
       * .append("  1=1  AND (receive.reservedQuantity - COALESCE(receive.issuedqty, 0)) > 0  and receive.userContact.id= (  select e.createdBy.id from Escm_Material_Request e  where e.id='"
       * + headerId + "'  )");
       */
      whereClause
          .append("  1=1  AND (receive.reservedQuantity - COALESCE(receive.issuedqty, 0)) > 0");

    }
    return whereClause;
  }

  private List<String> getOrderByClauses(final List<String> selectedPSDs,
      final Map<String, String> requestParameters) {
    List<String> orderByClauses = new ArrayList<String>();
    if (!requestParameters.containsKey("_sortBy")) {
      orderByClauses.add("line");
    } else {
      orderByClauses.add(" ");
    }
    return orderByClauses;
  }

  private String appendOrderByClauses(String _hqlQuery, List<String> orderByClauses,
      boolean justCount) {
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
    int orgFilterIndex = hqlQuery.indexOf(" inout.organization in ", whereIndex);
    int beginIndex = hqlQuery.indexOf(" AND ", orgFilterIndex);
    int endIndex = hqlQuery.indexOf("and @whereClause@");
    if (beginIndex != -1) {
      String gridFilters = hqlQuery.substring(beginIndex, endIndex);
      hqlQuery = hqlQuery.replace(gridFilters, " ");
    }
    return hqlQuery;
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