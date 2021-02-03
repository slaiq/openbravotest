package sa.elm.ob.scm.hqlinjections;

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

@ComponentProvider.Qualifier("562B518361184DB793F4C873EED54B02")
public class ESCMAddReceiptInspectionInjection extends HqlQueryTransformer {

  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "562B518361184DB793F4C873EED54B02";
  Logger log = Logger.getLogger(ESCMAddReceiptInspectionInjection.class);

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
    String strJustCount = requestParameters.get("_justCount");
    boolean justCount = strJustCount.equalsIgnoreCase("true");
    boolean hasSelectedIds = !selectedPSDs.isEmpty();
    String hqlQuery = _hqlQuery;

    /*
     * if (hasCriteria) { hqlQuery = removeGridFilters(hqlQuery); queryNamedParameters.clear(); //
     * hqlQuery = calculateHavingClause(hqlQuery, transactionType, criteria, //
     * queryNamedParameters); } else { // hqlQuery = hqlQuery.replace("@havingClause@", ""); }
     */
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

    StringBuffer selectClause = getSelectClause(hasSelectedIds);
    StringBuffer whereClause = getWhereClause(requestParameters);
    // StringBuffer groupByClause = getGroupByClause();

    String transformedHql = hqlQuery.replace("@selectClause@", selectClause.toString());
    transformedHql = transformedHql.replace("@whereClause@", whereClause.toString());
    // transformedHql = transformedHql.replace("@groupByClause@", groupByClause.toString());
    transformedHql = appendOrderByClauses(transformedHql, orderByClauses, justCount);
    log.debug("transformedHql:" + transformedHql);

    return transformedHql;
  }

  protected StringBuffer getSelectClause(boolean hasSelectedIds) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append("inout.id as  ESCM_AddReceiptOrInspect_ID, ");
    selectClause
        .append(" inout.id as m_inout_id, inout.client.id as ad_client_id,inout.organization.id as ad_org_id,   inout.active , inout.escmReceivingtype as receivingtype,inout.documentNo as documentNo,inout.createdBy.id as createdBy , "
            + "   inout.createdBy.name as creater,   (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(inout.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,       (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "           from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(inout.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))        as creationDate ,inout.updatedBy.id as updatedBy ,    inout.updatedBy.name as updater,  (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "           from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(inout.accountingDate,'YYYY-MM-DD'),'YYYY-MM-DD'))        as accountingDate ,inout.updatedBy.id as updatedBy ,inout.escmTranscertifno  as escmTranscertifno ,inout.businessPartner.id as c_bpartner_id,        inout.businessPartner.name as bpname,inout.escmIsinspected as em_escm_isinspected,inout.description as description, "
            + "       inout.orderReference as refno ");
    /*
     * if(hasSelectedIds) { // if there are selected ids selection is done in the client.
     * selectClause.append(" case when 1 < 0 then true else false end as OB_Selected "); } else {
     * selectClause .append(
     * " case when max(revln.id) is not null then true else false end as OB_Selected "); }
     */

    return selectClause;
  }

  protected StringBuffer getWhereClause(Map<String, String> requestParameters) {
    String headerId = requestParameters.get("@MaterialMgmtShipmentInOut.id@");
    String receivingType = requestParameters.get("@MaterialMgmtShipmentInOut.escmReceivingtype@");
    String inoutId = requestParameters.get("@MaterialMgmtShipmentInOut.id@");
    String orgId = requestParameters.get("@MaterialMgmtShipmentInOut.organization@");
    String bpartnerId = requestParameters.get("@MaterialMgmtShipmentInOut.businessPartner@");
    log.debug("orgId:" + orgId);
    log.debug("bpartnerId:" + bpartnerId);
    StringBuffer whereClause = new StringBuffer();
    // Create WhereClause
    whereClause.append(" inout.escmReceivingtype  is not null  and inout.organization.id='" + orgId
        + "' and inout.businessPartner.id='" + bpartnerId + "'  and inout.escmDocstatus='CO'  ");
    if (receivingType.equals("INS")) {
      whereClause
          .append(" and inout.escmReceivingtype='IR'  and  inout.id in (select distinct e.goodsShipment.id from Escm_InitialReceipt e where ( e.quantity  - (e.acceptedQty+e.rejectedQty+e.deliveredQty+e.returnQty+e.returnQuantity)  > 0 ))  and inout.escmIsinspected='N'");

      whereClause
          .append("and inout.id not in (select coalesce(e.receipt.id,'') from Escm_Addreceipt e where e.goodsShipment.id ='"
              + inoutId + "')");
    }

    else if (receivingType.equals("DEL")) {
      whereClause
          .append("and inout.escmReceivingtype='INS' and  inout.id in (select distinct rec.goodsShipment.id from Escm_Addreceipt rec left join rec.receipt.escmInitialReceiptList init   where init.acceptedQty>0     )  and inout.id not in (select e.inspection.id from Escm_Addreceipt e where e.goodsShipment.id='"
              + headerId + "')");

    } else if (receivingType.equals("RET")) {
      whereClause
          .append(" and inout.escmReceivingtype='IR' and inout.id in (select distinct e.goodsShipment.id from Escm_InitialReceipt e where ((e.acceptedQty+e.rejectedQty+e.deliveredQty) > 0 ) or (((e.acceptedQty+e.rejectedQty+e.deliveredQty+e.returnQty) = 0 ) and ((e.quantity - e.returnQuantity) > 0 ))) and inout.id not in (select e.receipt.id from Escm_Addreceipt e where e.goodsShipment.id='"
              + headerId + "')");
    }
    return whereClause;
  }

  private List<String> getOrderByClauses(final List<String> selectedPSDs,
      final Map<String, String> requestParameters) {
    log.debug("_sortBy:" + requestParameters.get("_sortBy"));
    List<String> orderByClauses = new ArrayList<String>();
    if (!requestParameters.containsKey("_sortBy")) {
      orderByClauses.add("documentNo");
    } else {
      orderByClauses.add(requestParameters.get("_sortBy"));
    }
    return orderByClauses;
  }

  private String appendOrderByClauses(String _hqlQuery, List<String> orderByClauses,
      boolean justCount) {
    log.debug("orderByClauses:" + orderByClauses);
    final String orderby = " ORDER BY ";
    String hqlQuery = _hqlQuery;
    log.debug("justCount:" + justCount);
    if (!justCount) {
      log.debug("contains:" + hqlQuery.contains(orderby));
      if (hqlQuery.contains(orderby)) {
        int offset = hqlQuery.indexOf(orderby) + orderby.length();
        StringBuilder sb = new StringBuilder(hqlQuery);
        log.debug("sb:" + sb);
        sb.insert(offset, orderByClauses.get(0) + " ");
        log.debug("orderByClauses" + orderByClauses.get(0));
        log.debug("offset" + offset);
        hqlQuery = sb.toString();
        log.debug("hqlQuery" + hqlQuery);
        orderByClauses.remove(0);
      } else {
        hqlQuery = hqlQuery.concat(orderby);
      }
      for (String clause : orderByClauses) {
        log.debug("clause" + clause);
        hqlQuery = hqlQuery.concat(clause);
      }
      log.debug("hqlQuery" + hqlQuery);
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