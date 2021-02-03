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
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmProposalMgmt;

@ComponentProvider.Qualifier("0E34446974B34B85889537878CEF8829")
public class BidMangmentAddLinesInjection extends HqlQueryTransformer {

  final static String RDBMS = new DalConnectionProvider(false).getRDBMS();
  final static String TABLE_ID = "0E34446974B34B85889537878CEF8829";
  Logger log = Logger.getLogger(BidMangmentAddLinesInjection.class);

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    List<String> selectedPSDs = new ArrayList<String>();
    boolean hasCriteria = requestParameters.containsKey("criteria");
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    log.debug("entered");
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
    StringBuffer selectClause = new StringBuffer();

    StringBuffer whereClause = new StringBuffer();

    if (requestParameters.get("@Order.id@") != null) {
      whereClause = getPOWhereClause(requestParameters, vars);
      selectClause = getPOSelectClause(hasSelectedIds, requestParameters);
    } else if (requestParameters.get("@Escm_Proposal_Management.id@") != null) {
      whereClause = getProposalWhereClause(requestParameters, vars);
      selectClause = getProposalSelectClause(hasSelectedIds, requestParameters);
    } else {
      whereClause = getWhereClause(requestParameters, vars);
      selectClause = getSelectClause(hasSelectedIds, requestParameters);
      if (!justCount) {
        orderByClauses = getRequistionOrderByClauses(selectedPSDs, requestParameters);
      }
    }

    // StringBuffer groupByClause = getGroupByClause();

    String transformedHql = hqlQuery.replace("@selectClause@", selectClause.toString());
    transformedHql = transformedHql.replace("@whereClause@", whereClause.toString());
    String additionalfilter = getaddfilter(transformedHql);

    // transformedHql = transformedHql.replace("@groupByClause@", groupByClause.toString());
    transformedHql = appendOrderByClauses(additionalfilter, orderByClauses, justCount);
    log.debug("transformedHql:" + transformedHql);
    return transformedHql;
  }

  protected StringBuffer getSelectClause(boolean hasSelectedIds,
      Map<String, String> requestParameters) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append("lines.id as  escm_purreq_lines_v_id, ");
    selectClause.append(
        "  lines.client.id as ad_client_id,lines.organization.id as ad_org_id,lines.organization.name as orgName,   lines.active  as isactive, lines.documentNo as documentNo,lines.createdBy.id as createdBy , "
            + "   lines.createdBy.name as creater,   (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,       (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "           from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))        as creationDate ,lines.updatedBy.id as updatedBy ,    lines.updatedBy.name as updater, "
            + "      lines.userContact.id as ad_user_id , lines.userContact.name as userName,(select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.needByDate,'YYYY-MM-DD'),'YYYY-MM-DD'))   as needbydate ,lines.reqqty as reqqty,lines.unitPrice as unitPrice,lines.lineNetAmount as lineNetAmount,lines.description as description,lines.businessPartner.id as c_bpartner_id,coalesce(lines.financialYear.description,lines.financialYear.fiscalYear)  as financialYear,lines.currency.id as c_currency_id,lines.attributeSetValue.id as m_attributesetinstance_id, "
            + "       lines.lineNo as lineNo ,( case when  lines.escmDepartment.id is not null then lines.escmDepartment.id else null end )as department_id,( case when  lines.escmDepartment.name is not null then (lines.escmDepartment.searchKey||'-'||lines.escmDepartment.name) else null end ) as reqDept,lines.escmAdRole.id as ad_role_id ,( case when  lines.product.id is not null then lines.product.id else null end ) as m_product_id,lines.commercialName as productdescription,lines.uOM.id as c_uom_id,lines.uOM.name as uomname,"
            + "  lines.requisition.id as m_requisition_id,lines.linedescription as linedescription , lines.eFINUniqueCode.id as c_validcombination_id ");
    String headerId = requestParameters.get("@escm_bidmgmt.id@");
    if (headerId != null) {
      selectClause.append(
          " ,(lines.escmBidmgmtQty-coalesce( (select e.reservedQuantity  from escm_bidsourceref e  left join e.escmBidmgmtLine as bidline "
              + " left join  bidline.escmBidmgmt as bid where bid.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0)) as escmBidmgmtQty , (lines.quantity + coalesce( (select e.reservedQuantity  from escm_bidsourceref e  left join e.escmBidmgmtLine as bidline "
              + " left join  bidline.escmBidmgmt as bid where bid.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0))  as quantity ");
    }
    return selectClause;
  }

  protected StringBuffer getPOSelectClause(boolean hasSelectedIds,
      Map<String, String> requestParameters) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append("lines.id as  escm_purreq_lines_v_id, ");
    selectClause.append(
        "  lines.client.id as ad_client_id,lines.organization.id as ad_org_id,lines.organization.name as orgName,   lines.active  as isactive, lines.documentNo as documentNo,lines.createdBy.id as createdBy , "
            + "   lines.createdBy.name as creater,   (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,       (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "           from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))        as creationDate ,lines.updatedBy.id as updatedBy ,    lines.updatedBy.name as updater, "
            + "      lines.userContact.id as ad_user_id , lines.userContact.name as userName,(select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.needByDate,'YYYY-MM-DD'),'YYYY-MM-DD'))   as needbydate ,lines.reqqty as reqqty,lines.unitPrice as unitPrice,lines.lineNetAmount as lineNetAmount,lines.description as description,lines.businessPartner.id as c_bpartner_id,coalesce(lines.financialYear.description,lines.financialYear.fiscalYear) as financialYear,lines.currency.id as c_currency_id,lines.attributeSetValue.id as m_attributesetinstance_id, "
            + "       lines.lineNo as lineNo ,( case when  lines.escmDepartment.id is not null then lines.escmDepartment.id else null end )as department_id,( case when  lines.escmDepartment.name is not null then (lines.escmDepartment.searchKey||'-'||lines.escmDepartment.name) else null end ) as reqDept,lines.escmAdRole.id as ad_role_id ,( case when  lines.product.id is not null then lines.product.id else null end ) as m_product_id,lines.commercialName as productdescription,lines.uOM.id as c_uom_id,lines.uOM.name as uomname,"
            + "  lines.requisition.id as m_requisition_id,lines.linedescription as linedescription ");
    String headerId = requestParameters.get("@Order.id@");
    if (headerId != null) {
      selectClause.append(
          " ,(lines.escmPoQty-coalesce( (select e.reservedQuantity  from Escm_Ordersource_Ref e  left join e.salesOrderLine as orderline "
              + " left join  orderline.salesOrder as order where order.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0)) as escmOrderQty , (lines.quantity + coalesce( (select e.reservedQuantity  from Escm_Ordersource_Ref e  left join e.salesOrderLine as orderline "
              + " left join orderline.salesOrder as order where order.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0))  as quantity ");
    }
    return selectClause;
  }

  protected StringBuffer getProposalSelectClause(boolean hasSelectedIds,
      Map<String, String> requestParameters) {
    StringBuffer selectClause = new StringBuffer();
    // Create Select Clause
    selectClause.append("lines.id as  escm_purreq_lines_v_id, ");
    selectClause.append(
        "  lines.client.id as ad_client_id,lines.organization.id as ad_org_id,lines.organization.name as orgName,   lines.active  as isactive, lines.documentNo as documentNo,lines.createdBy.id as createdBy , "
            + "   lines.createdBy.name as creater,   (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.updated,'YYYY-MM-DD'),'YYYY-MM-DD'))  as updated ,       (select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "           from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.creationDate,'YYYY-MM-DD'),'YYYY-MM-DD'))        as creationDate ,lines.updatedBy.id as updatedBy ,    lines.updatedBy.name as updater, "
            + "      lines.userContact.id as ad_user_id , lines.userContact.name as userName,(select  max(to_date(substr(hijri_date, 7,2)||'-'||substr(hijri_date, 5,2)||'-'||substr(hijri_date, 1,4),'DD-MM-YYYY')) "
            + "     from EUT_HijiriDates where gregorianDate = TO_DATE( to_char(lines.needByDate,'YYYY-MM-DD'),'YYYY-MM-DD'))   as needbydate ,lines.reqqty as reqqty,lines.unitPrice as unitPrice,lines.lineNetAmount as lineNetAmount,lines.description as description,lines.businessPartner.id as c_bpartner_id,coalesce(lines.financialYear.description,lines.financialYear.fiscalYear) as financialYear,lines.currency.id as c_currency_id,lines.attributeSetValue.id as m_attributesetinstance_id, "
            + "       lines.lineNo as lineNo ,( case when  lines.escmDepartment.id is not null then lines.escmDepartment.id else null end )as department_id,( case when  lines.escmDepartment.name is not null then (lines.escmDepartment.searchKey||'-'||lines.escmDepartment.name) else null end ) as reqDept,lines.escmAdRole.id as ad_role_id ,( case when  lines.product.id is not null then lines.product.id else null end ) as m_product_id,lines.commercialName as productdescription,lines.uOM.id as c_uom_id,lines.uOM.name as uomname,"
            + "  lines.requisition.id as m_requisition_id,lines.linedescription as linedescription ");
    String headerId = requestParameters.get("@Escm_Proposal_Management.id@");
    if (headerId != null) {
      selectClause.append(
          " ,0 as escmProposalQty , (lines.quantity + coalesce( (select e.reservedQuantity  from Escm_Proposalsource_Ref e  left join e.escmProposalmgmtLine as proposalLine "
              + " left join proposalLine.escmProposalmgmt as proposal where proposal.id ='"
              + headerId + "'  and e.requisitionLine.id=lines.id ),0))  as quantity, ");
      selectClause.append("coalesce((lines.reqqty-lines.escmAwardedQty),0) as remainingquantity ");
    }
    return selectClause;
  }

  protected StringBuffer getWhereClause(Map<String, String> requestParameters,
      VariablesSecureApp vars) {
    String headerId = requestParameters.get("@escm_bidmgmt.id@");
    String bidtype = requestParameters.get("@escm_bidmgmt.bidtype@");
    String orgId = requestParameters.get("@escm_bidmgmt.organization@");
    String clientId = requestParameters.get("@escm_bidmgmt.client@");
    String contractType = requestParameters.get("@escm_bidmgmt.contractType@");

    StringBuffer whereClause = new StringBuffer();
    if (headerId != null) {
      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id='" + clientId
              + "' and e.active='Y' ");
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      if (encumcontrol.list().size() > 0) {
        whereClause.append(
            " and lines.efinBudgetint.id = (select coalesce(a.efinBudgetinitial.id,'') from escm_bidmgmt a where a.id = '"
                + headerId + "' ) ");
      }

      whereClause.append(
          " and 1=1 and ((select requ.escmPrReturn from ProcurementRequisitionLine lin join lin.requisition requ where lin.id=lines.id)='N')  and lines.organization.id = '"
              + orgId + "'  "
              + " and lines.escmIssummary ='N' and ( lines.reqqty -(lines.escmBidmgmtQty-coalesce( (select e.reservedQuantity  from escm_bidsourceref e  left join e.escmBidmgmtLine as bidline "
              + " left join  bidline.escmBidmgmt as bid where bid.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0))) > 0  and lines.requisition.id  not in ( select ln.requisition.id from  ProcurementRequisitionLine ln where coalesce(ln.escmPoQty,0) > 0 or ln.escmIsproposal= 'Y') ");

    }
    if (bidtype != null) {
      if (bidtype.equals("DR"))
        whereClause.append(" and lines.processType='DP' ");
      else if (bidtype.equals("TR"))
        whereClause.append(" and lines.processType='PB' ");
      else if (bidtype.equals("LD"))
        whereClause.append(" and lines.processType='LB' ");
    }

    if (!(contractType.equals("null")) && contractType != "") {
      whereClause.append(
          " and lines.id in ( select ln.id from ProcurementRequisitionLine ln join ln.requisition req"
              + " where (req.escmContactType is not null and req.escmPrReturn='N' and req.escmContactType.id = '"
              + contractType + "') or ( req.escmContactType is null and ( ln.product is null or "
              + " ( ln.product is not null and ( ( ( select count(cat.id) from Product pdt  "
              + "join pdt.eSCMPRODCONTCATGList  as cat where pdt.id = ln.product.id and "
              + "cat.contractCategory.id = '" + contractType
              + "' ) > 0 ) or ((select count(cat.id) from Product pdt  join pdt.eSCMPRODCONTCATGList  as cat "
              + " where pdt.id = ln.product.id) = 0) ) ) ) ) )");
    }
    return whereClause;
  }

  protected StringBuffer getPOWhereClause(Map<String, String> requestParameters,
      VariablesSecureApp vars) {
    String headerId = requestParameters.get("@Order.id@");
    String clientId = requestParameters.get("@Order.client@");
    String contractType = requestParameters.get("@Order.escmContactType@");
    String orgId = requestParameters.get("@Order.organization@");
    String bpId = requestParameters.get("@Order.businessPartner@");

    StringBuffer whereClause = new StringBuffer();
    if (headerId != null) {

      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id='" + clientId
              + "' and e.active='Y' ");
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      if (encumcontrol.list().size() > 0) {
        whereClause.append(
            " and lines.efinBudgetint.id = (select coalesce(a.efinBudgetint.id,'') from Order a where a.id = '"
                + headerId + "' ) ");
      }

      whereClause.append(
          " and 1=1 and lines.processType='DP' and ((select requ.escmPrReturn from ProcurementRequisitionLine lin join lin.requisition requ where lin.id=lines.id)='N') and ( lines.reqqty -(lines.escmPoQty-coalesce( (select e.reservedQuantity  from Escm_Ordersource_Ref e  left join e.salesOrderLine as orderline "
              + " left join  orderline.salesOrder as order where order.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0))) > 0 and coalesce(lines.escmBidmgmtQty,0) =0 and coalesce(lines.escmProposalqty,0) =0 and lines.requisition.id not in (select requisition.id from ProcurementRequisitionLine where coalesce(escmBidmgmtQty,0) > 0 or escmIsproposal= 'Y')");

      if (contractType != null && !contractType.equals("null"))
        whereClause.append(
            " and lines.id in ( select ln.id from ProcurementRequisitionLine ln join ln.requisition req"
                + " where (req.escmContactType is not null and req.escmPrReturn='N' and req.escmContactType.id = '"
                + contractType + "') or ( req.escmContactType is null and ( ln.product is null or "
                + " ( ln.product is not null and ( ( ( select count(cat.id) from Product pdt  "
                + "join pdt.eSCMPRODCONTCATGList  as cat where pdt.id = ln.product.id and "
                + "cat.contractCategory.id = '" + contractType
                + "' ) > 0 ) or ((select count(cat.id) from Product pdt  join pdt.eSCMPRODCONTCATGList  as cat "
                + " where pdt.id = ln.product.id) = 0) ) ) ) ) )");

      if (orgId != null && !orgId.equals("null")) {
        whereClause.append(" and lines.organization.id='" + orgId + "'");
      }

      whereClause.append(
          " and (lines.requisition.id in ( select a.requisition.id from ESCM_prsuppliers a where a.requisition.id in (select f.requisition.id from ESCM_Purreq_Lines_V f ) and a.supplierNumber.id in (select o.businessPartner.id from Order o where o.id = '"
              + headerId
              + "')) or (lines.requisition.id in (select g.requisition.id from ESCM_Purreq_Lines_V g where g.requisition.id  not in (select a.requisition.id from ESCM_prsuppliers a) )))");

    }

    return whereClause;
  }

  protected StringBuffer getProposalWhereClause(Map<String, String> requestParameters,
      VariablesSecureApp vars) {
    String headerId = requestParameters.get("@Escm_Proposal_Management.id@");
    String clientId = requestParameters.get("@Escm_Proposal_Management.client@");
    String orgId = requestParameters.get("@Escm_Proposal_Management.organization@");
    String supplierId = requestParameters.get("@Escm_Proposal_Management.businessPartner@");

    StringBuffer whereClause = new StringBuffer();
    if (headerId != null) {

      EscmProposalMgmt proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, headerId);
      String contractTypeId = (proMgmt.getContractType() == null) ? "null"
          : proMgmt.getContractType().getId();

      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id='" + clientId
              + "' and e.active='Y' ");
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      if (encumcontrol.list().size() > 0) {
        whereClause.append(
            " and lines.efinBudgetint.id = (select coalesce(a.efinBudgetinitial.id,'') from Escm_Proposal_Management a where a.id = '"
                + headerId + "' ) ");
      }

      whereClause.append(
          " and 1=1  and lines.processType='DP' and ((select requ.escmPrReturn from ProcurementRequisitionLine lin join lin.requisition requ where lin.id=lines.id)='N') and ( lines.reqqty -(lines.escmProposalqty-coalesce( "
              + "(select e.reservedQuantity  from Escm_Ordersource_Ref e  left join e.salesOrderLine as orderline "
              + " left join  orderline.salesOrder as order where order.id ='" + headerId
              + "'  and e.requisitionLine.id=lines.id ),0))) > 0  and coalesce(lines.escmBidmgmtQty,0) =0 "
              + "and coalesce(lines.escmPoQty,0) =0 "
              + " and lines.requisition.id  not in (select requisition.id from ProcurementRequisitionLine where coalesce(escmBidmgmtQty,0) > 0 "
              + "or coalesce(escmPoQty,0) > 0 )");

      if (contractTypeId != null && !contractTypeId.equals("null"))
        whereClause.append(
            " and lines.id in ( select ln.id from ProcurementRequisitionLine ln join ln.requisition req"
                + " where (req.escmContactType is not null and req.escmPrReturn='N' and req.escmContactType.id = '"
                + contractTypeId
                + "') or ( req.escmContactType is null and ( ln.product is null or "
                + " ( ln.product is not null and ( ( ( select count(cat.id) from Product pdt  "
                + "join pdt.eSCMPRODCONTCATGList  as cat where pdt.id = ln.product.id and "
                + "cat.contractCategory.id = '" + contractTypeId
                + "' ) > 0 ) or ((select count(cat.id) from Product pdt  join pdt.eSCMPRODCONTCATGList  as cat "
                + " where pdt.id = ln.product.id) = 0) ) ) ) ) )");

      if (proMgmt.getProposalType().equals("DR")) {
        whereClause.append(" and  lines.id in (select e.id from ProcurementRequisitionLine as e "
            + " join e. requisition as req left join req.eSCMPrsuppliersList as supp "
            + " where supp. supplierNumber='" + proMgmt.getSupplier().getId() + "')");
      }

      if (orgId != null && !orgId.equals("null")) {
        whereClause.append(" and lines.organization.id='" + orgId + "'");
      }
    }

    return whereClause;
  }

  protected String getaddfilter(String _hqlQuery) {
    // StringBuffer addclause = new StringBuffer();
    String hqlQuery = _hqlQuery;
    hqlQuery = _hqlQuery.replace("productdescription)", "lines.commercialName)");
    hqlQuery = hqlQuery.replace("reqDept)",
        "lines.escmDepartment.name||'-'||lines.escmDepartment.searchKey)");
    hqlQuery = hqlQuery.replace("uomname)", "lines.uOM.name)");
    return hqlQuery;
  }

  private List<String> getOrderByClauses(final List<String> selectedPSDs,
      final Map<String, String> requestParameters) {
    List<String> orderByClauses = new ArrayList<String>();
    if (!requestParameters.containsKey("_sortBy")) {
      orderByClauses.add("lines.documentNo desc");
    } else {
      orderByClauses.add(" ");
    }
    return orderByClauses;
  }

  private List<String> getRequistionOrderByClauses(final List<String> selectedPSDs,
      final Map<String, String> requestParameters) {
    List<String> orderByClauses = new ArrayList<String>();
    if (!requestParameters.containsKey("_sortBy")) {
      orderByClauses.add("coalesce(lines.documentNo,lines.reqdocno), lines.lineNo ");
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
    int orgFilterIndex = hqlQuery.indexOf(" lines.organization in ", whereIndex);
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