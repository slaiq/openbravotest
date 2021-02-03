/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s): Qualian Technologies Pvt Ltd.
 *************************************************************************
 */

package sa.elm.ob.finance.hqlinjections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.datasource.HQLDataSourceService;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * @author Gopalakrishnan on 09/08/2016
 * 
 */
@ComponentProvider.Qualifier("58AF4D3E594B421A9A7307480736F03E")
public class EfinAddPaymentOrderInvoicesTransformer extends HqlQueryTransformer {
  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "58AF4D3E594B421A9A7307480736F03E";

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
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
    boolean hasSelectedIds = !selectedPSDs.isEmpty();
    String hqlQuery = _hqlQuery;
    // Retrieve Parameters
    /*
     * for (Map.Entry<String,String> entry : requestParameters.entrySet()) { String key =
     * entry.getKey(); String value = entry.getValue();
     * 
     * System.out.println("Key: "+key+" - "+value); }
     */
    String transactionType = requestParameters.get("transaction_type");
    String strJustCount = requestParameters.get("_justCount");
    boolean justCount = strJustCount.equalsIgnoreCase("true");

    StringBuffer selectClause = getSelectClause(transactionType, hasSelectedIds, requestParameters);
    StringBuffer joinClauseOrder = getJoinClauseOrder(requestParameters);
    StringBuffer joinClauseInvoice = getJoinClauseInvoice(requestParameters);
    StringBuffer whereClause = getWhereClause(transactionType, requestParameters, selectedPSDs);
    StringBuffer groupByClause = getGroupByClause(transactionType);
    List<String> orderByClauses = new ArrayList<String>();

    if (!justCount) {
      orderByClauses = getOrderByClauses(transactionType, selectedPSDs, requestParameters);
    }

    // grid filters need to be removed from where clause and added as a having criteria.
    if (hasCriteria) {
      hqlQuery = removeGridFilters(hqlQuery);
      queryNamedParameters.clear();
      hqlQuery = calculateHavingClause(hqlQuery, transactionType, criteria, queryNamedParameters);
    } else {
      hqlQuery = hqlQuery.replace("@havingCustomClause@", "");
    }

    // Remove alias @@ from Order By clause
    if (requestParameters.containsKey("_sortBy")) {
      String sortBy = requestParameters.get("_sortBy");
      if (sortBy.startsWith("-")) {
        sortBy = sortBy.substring(1);
      }
      hqlQuery = hqlQuery.replace("@" + sortBy + "@", sortBy);
    }

    String transformedHql = hqlQuery.replace("@selectCustomClause@", selectClause.toString());
    transformedHql = transformedHql.replace("@joinCustomClause@", " ");
    transformedHql = transformedHql.replace("@joinCustomClauseOrder@", joinClauseOrder.toString());
    transformedHql = transformedHql.replace("@joinCustomClauseInvoice@",
        joinClauseInvoice.toString());
    transformedHql = transformedHql.replace("@whereCustomClause@", whereClause.toString());
    transformedHql = transformedHql.replace("@groupByCustomClause@", groupByClause.toString());
    transformedHql = appendOrderByClauses(transformedHql, orderByClauses, justCount);
    // Sets parameters
    queryNamedParameters.put("currencyId", requestParameters.get("c_currency_id"));
    queryNamedParameters.put("isSalesTransaction",
        "true".equals(requestParameters.get("issotrx")) ? true : false);
    queryNamedParameters.put("businessPartnerId", requestParameters.get("received_from"));
    queryNamedParameters.put("paymentId", requestParameters.get("fin_payment_id"));
    queryNamedParameters.put("orgIds",
        new OrganizationStructureProvider().getChildTree(requestParameters.get("ad_org_id"), true));
    return transformedHql;
  }

  protected StringBuffer getSelectClause(String transactionType, boolean hasSelectedIds,
      Map<String, String> requestParameters) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append(getAggregatorFunction("psd.id") + " as paymentScheduleDetail, ");
    if ("I".equals(transactionType)) {
      selectClause.append(getAggregatorFunction("ord.documentNo") + " as salesOrderNo, ");
      selectClause.append(" case when (inv." + Invoice.PROPERTY_SALESTRANSACTION
          + " = false and oinfo is not null and oinfo."
          + OrganizationInformation.PROPERTY_APRMPAYMENTDESCRIPTION
          + " like 'Supplier Reference') then inv." + Invoice.PROPERTY_ORDERREFERENCE + " else inv."
          + Invoice.PROPERTY_DOCUMENTNO + " end as invoiceNo, ");
      selectClause
          .append(" COALESCE(ips.finPaymentmethod.id, ops.finPaymentmethod.id) as paymentMethod, ");
      selectClause.append(" COALESCE(ipsfp.name, opsfp.name) as paymentMethodName, ");
      selectClause.append(" COALESCE(invbp.id, ordbp.id) as businessPartner, ");
      selectClause.append(" COALESCE(invbp.name, ordbp.name) as businessPartnerName, ");
      selectClause.append(" COALESCE(ips.expectedDate, ops.expectedDate) as expectedDate, ");
      selectClause.append(
          " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(COALESCE(inv.invoiceDate, ord.orderDate),'YYYY-MM-DD'),'YYYY-MM-DD') and client.id= coalesce(inv.client.id, ord.client.id)  )  as em_efin_invoiceDate , ");
      // selectClause.append(" COALESCE(inv.invoiceDate, ord.orderDate) as em_efin_invoiceDate, ");
      selectClause.append(" max(COALESCE(ips.amount, ops.amount)) as expectedAmount, ");
      selectClause.append(" max(COALESCE(inv.grandTotalAmount, 0)) as invoicedAmount, ");
    } else if ("O".equals(transactionType)) {
      selectClause.append(" ord.documentNo as salesOrderNo, ");
      selectClause.append(getAggregatorFunction(" case when (inv."
          + Invoice.PROPERTY_SALESTRANSACTION + " = false and oinfo is not null and oinfo."
          + OrganizationInformation.PROPERTY_APRMPAYMENTDESCRIPTION
          + " like 'Supplier Reference') then inv." + Invoice.PROPERTY_ORDERREFERENCE + " else inv."
          + Invoice.PROPERTY_DOCUMENTNO + " end") + " as invoiceNo, ");
      selectClause
          .append(" COALESCE(ops.finPaymentmethod.id, ips.finPaymentmethod.id) as paymentMethod, ");
      selectClause.append(" COALESCE(opsfp.name, ipsfp.name) as paymentMethodName, ");
      selectClause.append(" COALESCE(invbp.id, ordbp.id) as businessPartner, ");
      selectClause.append(" COALESCE(invbp.name, ordbp.name) as businessPartnerName, ");
      selectClause.append(" COALESCE(ops.expectedDate, ips.expectedDate) as expectedDate, ");
      selectClause.append(
          " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(COALESCE(ord.orderDate, inv.invoiceDate),'YYYY-MM-DD'),'YYYY-MM-DD') and client.id= coalesce(ord.client.id, inv.client.id) )  as em_efin_invoiceDate , ");
      // selectClause.append(" COALESCE(ord.orderDate, inv.invoiceDate) as em_efin_invoiceDate, ");
      selectClause.append(" max(COALESCE(ips.amount, ops.amount)) as expectedAmount, ");
      selectClause.append(" sum(COALESCE(inv.grandTotalAmount, 0)) as invoicedAmount, ");
    } else {
      selectClause.append(" ord.documentNo as salesOrderNo, ");
      selectClause.append(" case when (inv." + Invoice.PROPERTY_SALESTRANSACTION
          + " = false and oinfo is not null and oinfo."
          + OrganizationInformation.PROPERTY_APRMPAYMENTDESCRIPTION
          + " like 'Supplier Reference') then inv." + Invoice.PROPERTY_ORDERREFERENCE + " else inv."
          + Invoice.PROPERTY_DOCUMENTNO + " end as invoiceNo, ");
      selectClause
          .append(" COALESCE(ips.finPaymentmethod.id, ops.finPaymentmethod.id) as paymentMethod, ");
      selectClause.append(" COALESCE(ipsfp.name, opsfp.name) as paymentMethodName, ");
      selectClause.append(" COALESCE(invbp.id, ordbp.id) as businessPartner, ");
      selectClause.append(" COALESCE(invbp.name, ordbp.name) as businessPartnerName, ");
      selectClause.append(" COALESCE(ips.expectedDate, ops.expectedDate) as expectedDate, ");
      selectClause.append(
          " (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY'))	from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(COALESCE(inv.invoiceDate, ord.orderDate),'YYYY-MM-DD'),'YYYY-MM-DD') and client.id= coalesce(inv.client.id, ord.client.id) )  as em_efin_invoiceDate , ");
      // selectClause.append(" COALESCE(inv.invoiceDate, ord.orderDate) as em_efin_invoiceDate, ");
      selectClause.append(" max(COALESCE(ips.amount, ops.amount)) as expectedAmount, ");
      selectClause.append(" max(COALESCE(inv.grandTotalAmount, 0)) as invoicedAmount, ");
    }
    selectClause.append(" SUM(psd.amount + psd.writeoffAmount) as outstandingAmount, ");
    selectClause.append(" COALESCE(sum(pd.amount), 0) as amount, ");
    selectClause
        .append(" case when sum(psd.writeoffAmount) <> 0 then true else false end as writeoff, ");

    if (hasSelectedIds) {
      // if there are selected ids selection is done in the client.
      selectClause.append(" case when 1 < 0 then true else false end as OB_Selected, ");
    } else {
      selectClause
          .append(" case when max(fp.id) is not null then true else false end as OB_Selected, ");
    }

    if ("true".equals(requestParameters.get("issotrx"))) {
      selectClause.append(" 'Y' as EM_Efin_IsSOTrx ");
    } else {
      selectClause.append(" 'N' as EM_Efin_IsSOTrx ");
    }

    return selectClause;
  }

  protected StringBuffer getJoinClauseOrder(Map<String, String> requestParameters) {
    String strBusinessPartnerId = requestParameters.get("received_from");
    StringBuffer joinClauseOrder = new StringBuffer();
    joinClauseOrder.append(
        " with ord.salesTransaction = :isSalesTransaction and ord.currency.id = :currencyId");
    if (strBusinessPartnerId != null && !"null".equals(strBusinessPartnerId)) {
      joinClauseOrder.append(" and ord.businessPartner.id = :businessPartnerId");
    }
    return joinClauseOrder;
  }

  protected StringBuffer getJoinClauseInvoice(Map<String, String> requestParameters) {
    String strBusinessPartnerId = requestParameters.get("received_from");
    StringBuffer joinClauseInvoice = new StringBuffer();
    joinClauseInvoice.append(
        " with inv.salesTransaction = :isSalesTransaction and inv.currency.id = :currencyId");
    if (strBusinessPartnerId != null && !"null".equals(strBusinessPartnerId)) {
      joinClauseInvoice.append(" and inv.businessPartner.id = :businessPartnerId");
    }
    return joinClauseInvoice;
  }

  protected StringBuffer getWhereClause(String transactionType,
      Map<String, String> requestParameters, List<String> selectedPSDs) {
    String strBusinessPartnerId = requestParameters.get("received_from");
    String strFinPaymentId = requestParameters.get("fin_payment_id");
    String strOrganizationId = requestParameters.get("ad_org_id");
    String strPaymentBeneficiary = requestParameters.get("@FIN_Payment.eFINPaymentBeneficiary@");
    String strPrepayment = requestParameters.get("@FIN_Payment.efinPrepayment@");
    String strSecondaryBeneficiary = requestParameters
        .get("@FIN_Payment.eFINSecondaryBeneficiary@");

    StringBuffer whereClause = new StringBuffer();
    // Create WhereClause
    if (strFinPaymentId != null) {
      whereClause.append(" (psd.paymentDetails is null or fp.id = :paymentId)");
    } else {
      whereClause.append(" psd.paymentDetails is null");
    }
    whereClause.append(" and coalesce(ips,ops) is not null ");

    if (strOrganizationId != null) {
      whereClause.append(" and psd.organization.id in :orgIds ");
    }

    whereClause.append(" and (oinfo is null or oinfo.active = true) ");
    whereClause.append("  and ( ");
    if (!selectedPSDs.isEmpty()) {
      whereClause.append(" psd.id in (");
      boolean isFirst = true;
      int i = 1;
      for (String strPSD : selectedPSDs) {
        if (isFirst) {
          isFirst = false;
        } else {
          whereClause.append(",");
        }
        whereClause.append("'" + strPSD + "'");

        i++;
        if (i % 2000 == 0) {
          whereClause.append(")");
          whereClause.append(" or psd.id in (");
          isFirst = true;
        }
      }
      whereClause.append(") or ");
    }
    if ("I".equals(transactionType)) {

      whereClause.append(" ( ");
      whereClause.append(" inv.salesTransaction = :isSalesTransaction");
      if (strBusinessPartnerId != null && !"null".equals(strBusinessPartnerId)) {
        whereClause.append(" and invbp.id = :businessPartnerId ");
      }
      whereClause.append(" and inv.currency.id = :currencyId ) ");

      if (strPrepayment != null && strPrepayment.equals("true")) {
        whereClause.append(" and inv.documentType.efinIsprepayinv='Y'");
      } else {
        whereClause.append(
            " and inv.documentType.efinIsprepayinv<>'Y' and inv.documentType.efinIsprepayinvapp<>'Y'");
      }
      if (requestParameters.containsKey("c_invoice_id")
          && requestParameters.get("issotrx").equalsIgnoreCase("true")) {
        whereClause
            .append(" and inv.id='" + requestParameters.get("c_invoice_id").toString() + "' ");
      } else if (!requestParameters.containsKey("c_invoice_id")
          && requestParameters.get("issotrx").equalsIgnoreCase("false")) {
        if (strPaymentBeneficiary != null && !"null".equals(strPaymentBeneficiary)) {
          whereClause.append("and ips.efinBpartner.id='" + strPaymentBeneficiary + "'");
        }
        if (strPaymentBeneficiary == null || strPaymentBeneficiary.equals("null")
            || "null".equals(strPaymentBeneficiary)) {
          whereClause.append("and ips.efinBpartner is null");
        }

        if (StringUtils.isNotEmpty(strSecondaryBeneficiary)
            && !"null".equals(strSecondaryBeneficiary)) {
          whereClause.append(" and ips.eFINSecondaryBeneficiary.id ='")
              .append(strSecondaryBeneficiary).append("' ");
        } else {
          whereClause.append(" and ips.eFINSecondaryBeneficiary.id is null ");
        }

      }
    } else if ("O".equals(transactionType)) {
      whereClause.append(" ( ");
      whereClause.append(" ord.salesTransaction = :isSalesTransaction");
      if (strBusinessPartnerId != null && !"null".equals(strBusinessPartnerId)) {
        whereClause.append(" and ordbp.id = :businessPartnerId ");
      }
      whereClause.append(" and ord.currency.id = :currencyId ) ");

    } else {

      whereClause.append(" ( ");
      whereClause.append(" inv.salesTransaction = :isSalesTransaction");
      if (strBusinessPartnerId != null && !"null".equals(strBusinessPartnerId)) {
        whereClause.append(" and invbp.id = :businessPartnerId ");
      }

      whereClause.append(" and inv.currency.id = :currencyId ) ");
      if (strPaymentBeneficiary == null || strPaymentBeneficiary.equals("null")
          || "null".equals(strPaymentBeneficiary)) {
        whereClause.append(" and ips.efinBpartner is null");
      }

      if (StringUtils.isNotEmpty(strSecondaryBeneficiary)
          && !"null".equals(strSecondaryBeneficiary)) {
        whereClause.append(" and ips.eFINSecondaryBeneficiary.id ='")
            .append(strSecondaryBeneficiary).append("' ");
      } else {
        whereClause.append(" and ips.eFINSecondaryBeneficiary.id is null ");
      }
      /*
       * whereClause.append(" or ( "); whereClause.append(
       * " ord.salesTransaction = :isSalesTransaction"); if(strBusinessPartnerId != null &&
       * !"null".equals(strBusinessPartnerId)) { whereClause.append(
       * " and ordbp.id = :businessPartnerId"); }
       * 
       * whereClause.append(" and ord.currency.id = :currencyId ) ");
       */
      if (strPrepayment != null && strPrepayment.equals("true")) {
        whereClause.append(" and inv.documentType.efinIsprepayinv='Y'");
      } else {
        whereClause.append(
            " and inv.documentType.efinIsprepayinv<>'Y' and inv.documentType.efinIsprepayinvapp<>'Y'");
      }
    }

    whereClause.append(")");

    return whereClause;

  }

  protected StringBuffer getGroupByClause(String transactionType) {
    StringBuffer groupByClause = new StringBuffer();
    // Create GroupBy Clause
    if ("I".equals(transactionType)) {
      groupByClause.append(" inv.id, ");
      groupByClause.append(" ord.id, ");
      groupByClause.append(" inv.documentNo, ");
      groupByClause.append(" inv.documentType, ");
      groupByClause.append(" COALESCE(ips.finPaymentmethod.id, ops.finPaymentmethod.id), ");
      groupByClause.append(" COALESCE(ipsfp.name, opsfp.name), ");
      groupByClause.append(" COALESCE(ips.expectedDate, ops.expectedDate), ");
      groupByClause.append(" COALESCE(inv.invoiceDate, ord.orderDate), ");
      groupByClause.append(" COALESCE(ipriority.priority, opriority.priority), ");
      groupByClause.append(" inv.salesTransaction, ");
      groupByClause.append(" oinfo.organization, ");
      groupByClause.append(" oinfo.aPRMPaymentDescription, ");
      groupByClause.append(" inv.orderReference, ");
      groupByClause.append(" ips.efinBpartner.id, ");
    } else if ("O".equals(transactionType)) {
      groupByClause.append(" ord.id, ");
      groupByClause.append(" inv.id, ");
      groupByClause.append(" ord.documentNo, ");
      groupByClause.append(" ord.documentType, ");
      groupByClause.append(" COALESCE(ops.finPaymentmethod.id, ips.finPaymentmethod.id), ");
      groupByClause.append(" COALESCE(opsfp.name, ipsfp.name), ");
      groupByClause.append(" COALESCE(ops.expectedDate, ips.expectedDate), ");
      groupByClause.append(" COALESCE(ord.orderDate, inv.invoiceDate), ");
      groupByClause.append(" COALESCE(opriority.priority, ipriority.priority), ");
    } else {
      groupByClause.append(" inv.id, ");
      groupByClause.append(" inv.documentNo, ");
      groupByClause.append(" inv.documentType, ");
      groupByClause.append(" ord.id, ");
      groupByClause.append(" ord.documentNo, ");
      groupByClause.append(" ord.documentType, ");
      groupByClause.append(" COALESCE(ips.finPaymentmethod.id, ops.finPaymentmethod.id), ");
      groupByClause.append(" COALESCE(ipsfp.name, opsfp.name), ");
      groupByClause.append(" COALESCE(ips.expectedDate, ops.expectedDate), ");
      groupByClause.append(" COALESCE(inv.invoiceDate, ord.orderDate), ");
      groupByClause.append(" COALESCE(ipriority.priority, opriority.priority), ");
      groupByClause.append(" inv.salesTransaction, ");
      groupByClause.append(" oinfo.organization, ");
      groupByClause.append(" oinfo.aPRMPaymentDescription, ");
      groupByClause.append(" inv.orderReference, ");
    }
    groupByClause.append(" COALESCE(invbp.id, ordbp.id), ");
    groupByClause.append(" COALESCE(invbp.name, ordbp.name) ");
    return groupByClause;
  }

  /**
   * Order by selectedPSDs, scheduled date and document number
   */
  private List<String> getOrderByClauses(String transactionType, List<String> selectedPSDs,
      Map<String, String> requestParameters) {
    List<String> orderByClauses = new ArrayList<String>();
    if (selectedPSDs.size() == 0) {
      String strInvoiceId = requestParameters.get("c_invoice_id");
      String strOrderId = requestParameters.get("c_order_id");
      if (strInvoiceId != null) {
        orderByClauses.add(" CASE WHEN MAX(inv.id) = '" + strInvoiceId + "' THEN 0 ELSE 1 END ");
      } else if (strOrderId != null) {
        orderByClauses.add(" CASE WHEN MAX(ord.id) = '" + strOrderId + "' THEN 0 ELSE 1 END ");
      } else {
        orderByClauses.add(" CASE WHEN MAX(fp.id) IS NOT NULL THEN 0 ELSE 1 END ");
      }
    } else {
      StringBuffer selectedOrderBy = new StringBuffer();
      String strAggId = getAggregatorFunction("psd.id");
      selectedOrderBy.append(" CASE WHEN ");
      boolean isFirst = true;
      for (String strPSDId : selectedPSDs) {
        if (!isFirst) {
          selectedOrderBy.append(" OR ");
        }
        selectedOrderBy.append(strAggId + " LIKE '%" + strPSDId + "%'");
        isFirst = false;
      }
      selectedOrderBy.append(" THEN 0 ELSE 1 END ");
      orderByClauses.add(selectedOrderBy.toString());
    }
    if ("O".equals(transactionType)) {
      orderByClauses.add(", COALESCE(opriority.priority, ipriority.priority) ");
      orderByClauses.add(", COALESCE(ops.expectedDate, ips.expectedDate) ");
      orderByClauses.add(", COALESCE(ord.orderDate, inv.invoiceDate) ");
    } else {
      orderByClauses.add(", COALESCE(ipriority.priority, opriority.priority) ");
      orderByClauses.add(", COALESCE(ips.expectedDate, ops.expectedDate) ");
      orderByClauses.add(", COALESCE(inv.invoiceDate, ord.orderDate) ");
    }
    if ("O".equals(transactionType)) {
      orderByClauses.add(", ord.documentNo ");
    } else {
      orderByClauses.add(", inv.documentNo ");
    }

    return orderByClauses;
  }

  protected String removeGridFilters(String _hqlQuery) {
    String hqlQuery = _hqlQuery;
    // Get the substring of grid filter inside where clause, if transaction type is "Orders" or
    // "Invoices", put in the having clause
    int whereIndex = hqlQuery.indexOf(" where ");
    int orgFilterIndex = hqlQuery.indexOf(" psd.organization in ", whereIndex);
    int beginIndex = hqlQuery.indexOf(" AND ", orgFilterIndex);
    int endIndex = hqlQuery.indexOf("and @whereCustomClause@");
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

    if (havingGridFilters.contains("@paymentScheduleDetail@")) {
      havingGridFilters = havingGridFilters.replaceAll("@paymentScheduleDetail@",
          getAggregatorFunction("psd.id"));
    }
    if ("I".equals(transactionType)) {
      if (havingGridFilters.contains("@salesOrderNo@")) {
        havingGridFilters = havingGridFilters.replaceAll("@salesOrderNo@",
            getAggregatorFunction("ord.documentNo"));
      }
      if (havingGridFilters.contains("@invoiceNo@")) {
        havingGridFilters = havingGridFilters.replaceAll("@invoiceNo@",
            " case when (inv.salesTransaction = false and oinfo is not null"
                + " and oinfo.aPRMPaymentDescription like 'Supplier Reference')"
                + " then inv.orderReference else inv.documentNo end");
      }
      if (havingGridFilters.contains("@paymentMethod@")) {
        havingGridFilters = havingGridFilters.replaceAll("@paymentMethod@",
            "COALESCE(ips.finPaymentmethod.id, ops.finPaymentmethod.id)");
      }
      if (havingGridFilters.contains("@paymentMethodName@")) {
        havingGridFilters = havingGridFilters.replaceAll("@paymentMethodName@",
            "COALESCE(ipsfp.name, opsfp.name)");
      }
      if (havingGridFilters.contains("@expectedAmount@")) {
        havingGridFilters = havingGridFilters.replaceAll("@expectedAmount@",
            "max(COALESCE(ips.amount, ops.amount))");
      }
      if (havingGridFilters.contains("@expectedDate@")) {
        havingGridFilters = havingGridFilters.replaceAll("@expectedDate@",
            "COALESCE(ips.expectedDate, ops.expectedDate)");
      }
      if (havingGridFilters.contains("@em_efin_invoiceDate@")) {
        havingGridFilters = havingGridFilters.replaceAll("@em_efin_invoiceDate@",
            "COALESCE(inv.invoiceDate, ord.orderDate)");
      }
      if (havingGridFilters.contains("@invoicedAmount@")) {
        havingGridFilters = havingGridFilters.replaceAll("@invoicedAmount@",
            "max(COALESCE(inv.grandTotalAmount, 0))");
      }
    } else if ("O".equals(transactionType)) {
      if (havingGridFilters.contains("@salesOrderNo@")) {
        havingGridFilters = havingGridFilters.replaceAll("@salesOrderNo@", "ord.documentNo");
      }
      if (havingGridFilters.contains("@invoiceNo@")) {
        getAggregatorFunction("inv.documentNo");
        havingGridFilters = havingGridFilters.replaceAll("@invoiceNo@",
            " hqlagg(case when (inv.salesTransaction = false and oinfo is not null"
                + " and oinfo.aPRMPaymentDescription like 'Supplier Reference')"
                + " then inv.orderReference else inv.documentNo end)");
      }
      if (havingGridFilters.contains("@paymentMethod@")) {
        havingGridFilters = havingGridFilters.replaceAll("@paymentMethod@",
            "COALESCE(ops.finPaymentmethod.id, ips.finPaymentmethod.id)");
      }
      if (havingGridFilters.contains("@paymentMethodName@")) {
        havingGridFilters = havingGridFilters.replaceAll("@paymentMethodName@",
            "COALESCE(opsfp.name, ipsfp.name)");
      }
      if (havingGridFilters.contains("@expectedAmount@")) {
        havingGridFilters = havingGridFilters.replaceAll("@expectedAmount@",
            "max(COALESCE(ips.amount, ops.amount))");
      }
      if (havingGridFilters.contains("@expectedDate@")) {
        havingGridFilters = havingGridFilters.replaceAll("@expectedDate@",
            "COALESCE(ops.expectedDate, ips.expectedDate)");
      }
      if (havingGridFilters.contains("@em_efin_invoiceDate@")) {
        havingGridFilters = havingGridFilters.replaceAll("@em_efin_invoiceDate@",
            "COALESCE(ord.orderDate,inv.invoiceDate)");
      }
      if (havingGridFilters.contains("@invoicedAmount@")) {
        havingGridFilters = havingGridFilters.replaceAll("@invoicedAmount@",
            "sum(COALESCE(inv.grandTotalAmount, 0))");
      }
    } else {
      if (havingGridFilters.contains("@salesOrderNo@")) {
        havingGridFilters = havingGridFilters.replaceAll("@salesOrderNo@", "ord.documentNo");
      }
      if (havingGridFilters.contains("@invoiceNo@")) {
        havingGridFilters = havingGridFilters.replaceAll("@invoiceNo@",
            " case when (inv.salesTransaction = false and oinfo is not null"
                + " and oinfo.aPRMPaymentDescription like 'Supplier Reference')"
                + " then inv.orderReference else inv.documentNo end");
      }
      if (havingGridFilters.contains("@paymentMethod@")) {
        havingGridFilters = havingGridFilters.replaceAll("@paymentMethod@",
            "COALESCE(ips.finPaymentmethod.id, ops.finPaymentmethod.id)");
      }
      if (havingGridFilters.contains("@paymentMethodName@")) {
        havingGridFilters = havingGridFilters.replaceAll("@paymentMethodName@",
            "COALESCE(ipsfp.name, opsfp.name)");
      }
      if (havingGridFilters.contains("@expectedAmount@")) {
        havingGridFilters = havingGridFilters.replaceAll("@expectedAmount@",
            "max(COALESCE(ips.amount, ops.amount))");
      }
      if (havingGridFilters.contains("@expectedDate@")) {
        havingGridFilters = havingGridFilters.replaceAll("@expectedDate@",
            "COALESCE(ips.expectedDate, ops.expectedDate)");
      }
      if (havingGridFilters.contains("@em_efin_invoiceDate@")) {
        havingGridFilters = havingGridFilters.replaceAll("@em_efin_invoiceDate@",
            "COALESCE(inv.invoiceDate, ord.orderDate)");
      }
      if (havingGridFilters.contains("@invoicedAmount@")) {
        havingGridFilters = havingGridFilters.replaceAll("@invoicedAmount@",
            "max(COALESCE(inv.grandTotalAmount, 0))");
      }
    }
    if (havingGridFilters.contains("@outstandingAmount@")) {
      havingGridFilters = havingGridFilters.replaceAll("@outstandingAmount@", "SUM(psd.amount)");
    }
    if (havingGridFilters.contains("@amount@")) {
      havingGridFilters = havingGridFilters.replaceAll("@amount@", "COALESCE(sum(fp.amount), 0)");
    }
    if (havingGridFilters != null && !"".equals(havingGridFilters.trim())) {
      havingClause.append(" having ( " + havingGridFilters + " )");
    }
    hqlQuery = hqlQuery.replace("@havingCustomClause@", havingClause.toString());
    return hqlQuery;
  }

  private String appendOrderByClauses(String _hqlQuery, List<String> orderByClauses,
      boolean justCount) {
    final String orderby = " ORDER BY ";
    String hqlQuery = _hqlQuery;
    if (!justCount) {
      if (hqlQuery.contains(orderby)) {
        int offset = hqlQuery.indexOf(orderby) + orderby.length();
        StringBuilder sb = new StringBuilder(hqlQuery);
        sb.insert(offset, orderByClauses.get(0) + ", ");
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

  /**
   * @see HQLDataSourceService#replaceParametersWithAlias(Table, String)
   */
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

}
