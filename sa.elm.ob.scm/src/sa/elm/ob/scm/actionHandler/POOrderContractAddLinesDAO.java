package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.EscmRequisitionlineV;

/**
 * 
 * @author Gopalakrishnan on 13/07/2017
 * 
 */
public class POOrderContractAddLinesDAO {
  @SuppressWarnings("unused")
  private Connection conn = null;

  public POOrderContractAddLinesDAO(Connection conn) {
    this.conn = conn;
  }

  private static final Logger log = LoggerFactory.getLogger(POOrderContractAddLinesDAO.class);

  /**
   * Method to insert the PO source reference
   * 
   * @param line
   * @param purchasereqId
   * @param purLineId
   * @param unitprice
   * @param paramNeedByDate
   * @param deptId
   * @param qty
   * @param description
   * @param updateqtyflag
   * @return success 1 else 0
   */
  public static int insertsourceref(OrderLine Objline, String purchasereqId, String purLineId,
      String unitprice, String paramNeedByDate, String deptId, String qty, String description,
      Boolean updateqtyflag, Connection conn) {
    int count = 0;
    long lineno = 10;
    EscmOrderSourceRef source = null;
    BigDecimal updQty = BigDecimal.ZERO, unitPrice = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      OrderLine line = Objline;
      // get the next line no to insert the record in Order source ref
      final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select coalesce(max(line),0)+10   as lineno from escm_ordersource_ref where c_orderline_id=:orderId");
      query.setParameter("orderId", line.getId());
      lineno = ((BigDecimal) (Object) query.list().get(0)).longValue();

      // check already line is exists or not in Order soucre ref based on bid management
      // line with Purchase req line. If exists then update the source qty otherwise insert a new
      // record.
      OBQuery<EscmOrderSourceRef> chklineexistQry = OBDal.getInstance().createQuery(
          EscmOrderSourceRef.class,
          "as e where e.salesOrderLine.id=:orderLineID and e.requisitionLine.id=:reqLineID");
      chklineexistQry.setNamedParameter("orderLineID", line.getId());
      chklineexistQry.setNamedParameter("reqLineID", purLineId);
      chklineexistQry.setMaxResult(1);
      List<EscmOrderSourceRef> sourceList = chklineexistQry.list();
      // update the existing line of order line source ref
      if (sourceList.size() > 0) {
        source = sourceList.get(0);
        source.setUpdated(new java.util.Date());
        source.setUpdatedBy(line.getUpdatedBy());
        if (source.getReservedQuantity().compareTo(new BigDecimal(qty)) > 0) {
          updQty = (source.getReservedQuantity().subtract(new BigDecimal(qty))).negate();
        } else {
          updQty = (source.getReservedQuantity().subtract(new BigDecimal(qty))).abs();
        }
        unitPrice = line.getUnitPrice();
        source.setReservedQuantity(new BigDecimal(qty));
        OBDal.getInstance().save(source);
      }
      // insert a new record in Order Source Ref

      else {
        // if (line.getEscmOrdersourceRefList().size() > 0) {
        // existingSourceCount = new BigDecimal(line.getEscmOrdersourceRefList().size())
        // .add(BigDecimal.ONE);
        // } else {
        // existingSourceCount = BigDecimal.ONE;
        // }
        RequisitionLine objReqLine = OBDal.getInstance().get(RequisitionLine.class, purLineId);
        source = OBProvider.getInstance().get(EscmOrderSourceRef.class);
        source.setClient(line.getClient());
        source.setOrganization(line.getOrganization());
        source.setCreationDate(new java.util.Date());
        source.setCreatedBy(line.getCreatedBy());
        source.setUpdated(new java.util.Date());
        source.setUpdatedBy(line.getUpdatedBy());
        source.setLineNo(lineno);
        source.setRequisitionLine(objReqLine);
        source.setRequisition(OBDal.getInstance().get(Requisition.class, purchasereqId));
        source.setReservedQuantity(new BigDecimal(qty));
        source.setSalesOrderLine(line);
        // Task no.-5411 noteno.- 15613 // unitPrice = ((line.getUnitPrice() == null ?
        // BigDecimal.ZERO :
        // line.getUnitPrice())
        // .add(objReqLine.getUnitPrice())).divide(existingSourceCount);
        unitPrice = line.getUnitPrice();

        updQty = new BigDecimal(qty);
        OBDal.getInstance().save(source);
        OBDal.getInstance().save(line);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(line);
      }
      // update reqline orderQty
      updateReqLineOrderQty(line, purLineId, updQty);
      //
      if (updateqtyflag) {

        // compare the unit price and bring minimum unit price - Task no.6911
        RequisitionLine objReqLine = OBDal.getInstance().get(RequisitionLine.class, purLineId);
        unitPrice = getMinimumOfUnitPriceAndCompareWithCurrentUnitPrice(line,
            objReqLine.getUnitPrice());

        BigDecimal oldQty = line.getOrderedQuantity();
        BigDecimal lineTotalPrice = unitPrice.multiply(oldQty.add(updQty));
        // update order line
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        line.setUnitPrice(unitPrice);
        line.setOrderedQuantity(line.getOrderedQuantity().add(updQty));
        line.setUnitPrice(unitPrice);
        line.setLineNetAmount(lineTotalPrice);
        OBDal.getInstance().save(line);
        OBDal.getInstance().flush();
        // // update shipment
        // OBQuery<Escmordershipment> objShipmentQuery = OBDal.getInstance().createQuery(
        // Escmordershipment.class,
        // "as e where e.salesOrderLine.id='" + line.getId() + "' order by e.creationDate asc");
        // objShipmentQuery.setMaxResult(1);
        // Escmordershipment objShipment = objShipmentQuery.list().get(0);
        // if (!line.isEscmIsmanual()) {
        // objShipment.setMovementQuantity(objShipment.getMovementQuantity().add(updQty));
        // }
        // objShipment.setQuantityaccepted(new BigDecimal(0));
        // objShipment.setQuantityrejected(new BigDecimal(0));
        // objShipment.setQuantityreturned(new BigDecimal(0));
        // objShipment.setQuantitycanceled(new BigDecimal(0));
        // objShipment.setQuantitydelivred(new BigDecimal(0));
        //
        // objShipment.setUpdatedBy(OBContext.getOBContext().getUser());
        // OBDal.getInstance().save(objShipment);

      }
      count = 1;
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception while insertsourceref() in POOrderContractAddLinesDAO ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * get minimum of unit price based on existing source ref of reqline unit price + current reqline
   * unit price
   * 
   * @param line(orderline)
   * @param currentUnitPrice(
   *          current reqline unit price)
   * @return
   */
  private static BigDecimal getMinimumOfUnitPriceAndCompareWithCurrentUnitPrice(OrderLine line,
      BigDecimal currentUnitPrice) {
    BigDecimal finalUnitPrice = BigDecimal.ZERO;
    BigDecimal minUnitPrice = BigDecimal.ZERO;
    BigDecimal currentunitPrice = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      currentunitPrice = currentUnitPrice;
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(
          " select min(priceactual) as minimumunitprice from escm_ordersource_ref ref "
              + " join m_requisitionline ln on ln.m_requisitionline_id= ref.m_requisitionline_id "
              + " where ref.c_orderline_id=? " + " and ref.m_requisitionline_id is not null ");
      Query.setParameter(0, line.getId());
      if (Query.list().size() > 0) {
        Object row = (Object) Query.list().get(0);
        minUnitPrice = (BigDecimal) row;
        if (minUnitPrice.compareTo(currentunitPrice) < 0) {
          finalUnitPrice = minUnitPrice;
        } else {
          finalUnitPrice = currentunitPrice;
        }
      } else {
        finalUnitPrice = currentunitPrice;
      }
      return finalUnitPrice;

    } catch (Exception e) {
      log.error("Exception while getMinimumOfUnitPriceAndCompareWithCurrentUnitPrice() "
          + " in POOrderContractAddLinesDAO ", e);
      OBDal.getInstance().rollbackAndClose();
      return finalUnitPrice;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Update the BidmgmtQty in Purchase Requisition line once requisition line associated with bid.
   * 
   * @param bid
   *          management line object
   * @param purLineId
   * @param qty
   * @return count , if successfully updated then return 1 otherwise 0
   */
  private static int updateReqLineOrderQty(OrderLine line, String purLineId, BigDecimal qty) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      // update the requisition line orderQty
      RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class, purLineId);
      reqline.setEscmPoQty(reqline.getEscmPoQty().add(qty));
      reqline.setUpdated(new java.util.Date());
      reqline.setUpdatedBy(line.getUpdatedBy());
      OBDal.getInstance().save(reqline);
      count = 1;

    } catch (Exception e) {
      log.error("Exception while updateReqLineOrderQty() in POOrderContractAddLinesDAO ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * To check whether all selected record belongs to same dept.
   * 
   * @param selectedlines
   *          selected JSONArray
   * @return true, if department mismatched.
   */
  public boolean checkSameDept(JSONArray selectedlines) {
    try {
      JSONObject firstRecord = selectedlines.getJSONObject(0);
      String firstDept = firstRecord.getString("department");
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        if (!firstDept.equals(selectedRow.getString("department"))) {
          return true;
        }
      }
    } catch (Exception e) {
      log.error("Exception while checking all records belongs to same dept ", e);
    }
    return false;
  }

  public static void insertParentLines(ArrayList<String> parentList, Order objorder, long line,
      Connection conn, JSONObject selectedRow, OrderLine orderline, boolean updateQtyflag) {

    Long lineNo = line;
    String qty = null;
    OrderLine OrderLineId = null;
    OrderLine originalLine = orderline;
    OrderLine oldsalesOrderLine = null;
    OrderLine salesOrderLine = null;
    EscmOrderlineV parentLine = null;
    boolean updateqtyflag = updateQtyflag;
    List<TaxRate> objTaxList = new ArrayList<TaxRate>();

    for (int i = parentList.size() - 1; i >= 0; i--) {
      RequisitionLine parentReqLine = OBDal.getInstance().get(RequisitionLine.class,
          parentList.get(i));
      OBQuery<EscmOrderSourceRef> chkLineExists = OBDal.getInstance().createQuery(
          EscmOrderSourceRef.class, "as e where e.salesOrderLine.salesOrder.id =:orderID"
              + " and e.requisitionLine.id =:parentID ");
      chkLineExists.setNamedParameter("orderID", objorder.getId());
      chkLineExists.setNamedParameter("parentID", parentList.get(i));

      if (chkLineExists.list().size() > 0) {
        EscmOrderSourceRef ref = chkLineExists.list().get(0);
        OrderLineId = ref.getSalesOrderLine();
        updateqtyflag = true;
        try {
          POOrderContractAddLinesDAO.insertsourceref(OrderLineId,
              selectedRow.getString("requisition"), selectedRow.getString("id"),
              selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
              selectedRow.getString("department"), selectedRow.getString("quantity"),
              selectedRow.getString("linedescription"), updateqtyflag, conn);
        } catch (JSONException e) {
          // TODO Auto-generated catch block
          log.error("Exception while insertParentLines() in POOrderContractAddLinesDAO ", e);
        }
      }

      else {
        salesOrderLine = OBProvider.getInstance().get(OrderLine.class);
        salesOrderLine.setClient(objorder.getClient());
        salesOrderLine.setOrganization(objorder.getOrganization());
        salesOrderLine.setCreationDate(new java.util.Date());
        salesOrderLine.setCreatedBy(objorder.getCreatedBy());
        salesOrderLine.setUpdated(new java.util.Date());
        salesOrderLine.setUpdatedBy(objorder.getUpdatedBy());
        salesOrderLine.setActive(true);
        salesOrderLine.setOrderDate(objorder.getOrderDate());
        salesOrderLine.setWarehouse(objorder.getWarehouse());
        Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
        salesOrderLine.setCurrency(
            objorder.getCurrency() == null
                ? (objorder.getOrganization().getCurrency() == null ? objCurrency
                    : objorder.getOrganization().getCurrency())
                : objorder.getCurrency());

        // salesOrderLine.setCurrency(objorder.getCurrency());
        salesOrderLine.setEscmNeedbydate(parentReqLine.getNeedByDate());
        OBQuery<TaxRate> objTaxQry = OBDal.getInstance().createQuery(TaxRate.class,
            "as e order by e.creationDate desc");
        objTaxQry.setMaxResult(1);
        objTaxList = objTaxQry.list();
        if (objTaxList.size() > 0) {
          salesOrderLine.setTax(objTaxList.get(0));
        }
        salesOrderLine.setSalesOrder(objorder);
        salesOrderLine.setLineNo(lineNo);
        salesOrderLine.setProduct(parentReqLine.getProduct());
        salesOrderLine.setEscmProductCategory(parentReqLine.getEscmProdcate());
        salesOrderLine.setEscmProdescription(parentReqLine.getDescription());
        if (oldsalesOrderLine == null && orderline == null) {
          salesOrderLine.setEscmParentline(null);
        } else {
          if (originalLine == null) {
            parentLine = OBDal.getInstance().get(EscmOrderlineV.class, oldsalesOrderLine.getId());
            salesOrderLine.setEscmParentline(parentLine);
          } else {
            parentLine = OBDal.getInstance().get(EscmOrderlineV.class, originalLine.getId());
            salesOrderLine.setEscmParentline(parentLine);
            originalLine = null;
          }
        }
        salesOrderLine.setUOM(parentReqLine.getUOM());
        salesOrderLine.setDescription(parentReqLine.getDescription());
        salesOrderLine.setUnitPrice(parentReqLine.getUnitPrice());
        salesOrderLine.setEFINUniqueCode(parentReqLine.getEfinCValidcombination());
        salesOrderLine.setEFINUniqueCodeName(parentReqLine.getEfinUniquecodename());
        salesOrderLine.setEfinMRequisitionline(parentReqLine);
        try {
          if (selectedRow.getString("id").equals(parentReqLine.getId())) {
            qty = selectedRow.getString("quantity");
            salesOrderLine.setOrderedQuantity(new BigDecimal(selectedRow.getString("quantity")));

          } else {
            qty = parentReqLine.getQuantity().toPlainString();
            salesOrderLine.setOrderedQuantity(parentReqLine.getQuantity());

          }
        } catch (JSONException e) {

        }

        salesOrderLine.setEscmIsmanual(false);
        salesOrderLine.setEscmIssummarylevel(parentReqLine.isEscmIssummary());
        OBDal.getInstance().save(salesOrderLine);
        OBDal.getInstance().flush();
        oldsalesOrderLine = salesOrderLine;

        lineNo = lineNo + 10;

        POOrderContractAddLinesDAO
            .insertsourceref(oldsalesOrderLine, parentReqLine.getRequisition().getId(),
                parentReqLine.getId(), parentReqLine.getUnitPrice().toPlainString(),
                parentReqLine.getNeedByDate() != null ? parentReqLine.getNeedByDate().toString()
                    : "",
                parentReqLine.getRequisition().getEscmDepartment().getId(), qty,
                parentReqLine.getDescription(), updateqtyflag, conn);
      }
    }

  }

  public static void getParentLines(RequisitionLine reqline, ArrayList<String> parentList,
      Order objOrder, long line, Connection conn, JSONObject selectedRow) {
    EscmRequisitionlineV parentLine = reqline.getEscmParentlineno();
    if (parentLine != null) {
      String ParentId = parentLine.getId();
      RequisitionLine parentReqLine = OBDal.getInstance().get(RequisitionLine.class, ParentId);

      OBQuery<EscmOrderSourceRef> chkLineExists = OBDal.getInstance().createQuery(
          EscmOrderSourceRef.class, "as e where e.salesOrderLine.salesOrder.id=:orderID "
              + " and e.requisitionLine.id =:parentID ");
      chkLineExists.setNamedParameter("orderID", objOrder.getId());
      chkLineExists.setNamedParameter("parentID", ParentId);

      chkLineExists.setMaxResult(1);

      if (chkLineExists.list().size() > 0) {
        insertParentLines(parentList, objOrder, line, conn, selectedRow,
            chkLineExists.list().get(0).getSalesOrderLine(), false);
      } else {
        parentList.add(ParentId);
        getParentLines(parentReqLine, parentList, objOrder, line, conn, selectedRow);
      }

    } else {
      insertParentLines(parentList, objOrder, line, conn, selectedRow, null, false);
    }

  }

  /**
   * Get receive type for the contract category
   * 
   * @param contractCategoryId
   * 
   * @return receiveTypeVal
   */
  public static String getReceiveType(String contractCategoryId) {

    String receiveTypeVal = "";

    ESCMDefLookupsTypeLn contractCategory = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
        contractCategoryId);
    if (contractCategory != null) {
      ESCMDefLookupsTypeLn receiveType = contractCategory.getReceiveType();
      if (receiveType != null) {
        receiveTypeVal = receiveType.getSearchKey();
      }
    }
    return receiveTypeVal;
  }

}
