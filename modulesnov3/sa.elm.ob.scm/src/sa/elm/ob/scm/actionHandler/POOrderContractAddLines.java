package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author gopalakrishnan on 13/07/2017
 * 
 */
public class POOrderContractAddLines extends BaseActionHandler {
  /**
   * This is responsible to add lines in po order and contract window
   */
  private static Logger log = Logger.getLogger(POOrderContractAddLines.class);
  Boolean updatenewqtyflag = false, updateqtyflag = false;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();

      // get the connection
      Connection conn = OBDal.getInstance().getConnection();
      POOrderContractAddLinesDAO dao = new POOrderContractAddLinesDAO(conn);
      // declaring JSONObject & variables
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject purreqline = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = purreqline.getJSONArray("_selection");
      List<OrderLine> chklineexistQryList = new ArrayList<>();
      List<OrderLine> chklinedescexistQryList = new ArrayList<>();
      Order objOrder = null;
      OrderLine objOrderLine = null;
      boolean result = false;
      long line = 10;
      final String strOrderId = jsonRequest.getString("C_Order_ID");
      String reqId = "";
      String receiveType = null;
      // getting order
      objOrder = OBDal.getInstance().get(Order.class, strOrderId);
      List<TaxRate> objTaxList = new ArrayList<TaxRate>();

      /* Validate encumbrance cannot be empty if encumbrance method is manual */
      if (objOrder.getEFINEncumbranceMethod().equals("M")) {
        if (objOrder.getEfinBudgetManencum() == null) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("ESCM_Enucm_Missing_Po"));
          json.put("message", erorMessage);
          return json;
        }
      }

      // delete exisiting lines
      if (selectedlines.length() > 0) {

        // check all the selected record belongs to same department.
        result = dao.checkSameDept(selectedlines);
        if (result) {
          JSONObject erorMessage = new JSONObject();
          erorMessage.put("severity", "error");
          erorMessage.put("text", OBMessageUtils.messageBD("Escm_PR_Dept_Mismatch"));
          json.put("message", erorMessage);
          return json;
        }

        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          updateqtyflag = false;
          RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class,
              selectedRow.getString("id"));
          reqId = reqline.getRequisition().getId();
          EscmRequisitionlineV objParent = reqline.getEscmParentlineno() != null
              ? reqline.getEscmParentlineno()
              : null;
          log.debug("objParent" + objParent);
          if (objParent == null) {
            final String id = reqline.getId();
            // chk line already presented or not based on product id
            OBQuery<OrderLine> chklineexistQry = OBDal.getInstance().createQuery(OrderLine.class,
                "as e where e.salesOrder.id=:orderID and e.product.id=:prdID and e.efinMRequisitionline.id is not null");
            chklineexistQry.setNamedParameter("orderID", strOrderId);
            chklineexistQry.setNamedParameter("prdID", selectedRow.getString("product"));
            if (chklineexistQry != null) {
              chklineexistQryList = chklineexistQry.list().stream()
                  .filter(o -> o.getEfinMRequisitionline().getId().equals(id))
                  .collect(Collectors.toList());
            }

            // chk line already presented or not based on product NAME
            OBQuery<OrderLine> chklinedescexistQry = OBDal.getInstance().createQuery(
                OrderLine.class,
                "as e where  e.salesOrder.id=:orderID and e.escmProdescription=:desc and e.efinMRequisitionline.id is not null");
            chklinedescexistQry.setNamedParameter("orderID", strOrderId);
            chklinedescexistQry.setNamedParameter("desc",
                selectedRow.getString("linedescription").replace("'", "''"));
            if (chklinedescexistQry != null) {
              chklinedescexistQryList = chklinedescexistQry.list().stream()
                  .filter(o -> o.getEfinMRequisitionline().getId().equals(id))
                  .collect(Collectors.toList());
            }

            // if line already exists then chk source ref is same and if same just update the qty in
            // line level as well as sourceref
            if (chklineexistQryList.size() > 0 || chklinedescexistQryList.size() > 0) {

              if (chklineexistQryList.size() > 0) {
                objOrderLine = chklineexistQryList.get(0);
              } else if (chklinedescexistQryList.size() > 0) {
                objOrderLine = chklinedescexistQryList.get(0);
              }
              updateqtyflag = true;

              // if entered qty is zero then delete the lines
              // from orderline as well as
              // source ref
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) == 0) {
                OBQuery<EscmOrderSourceRef> srcrefline = OBDal.getInstance().createQuery(
                    EscmOrderSourceRef.class,
                    "as e where e.salesOrderLine.id=:orderLnID and e.requisitionLine.id=:reqLnID");
                srcrefline.setNamedParameter("orderLnID", objOrderLine.getId());
                srcrefline.setNamedParameter("reqLnID", selectedRow.getString("id"));
                srcrefline.setMaxResult(1);
                List<EscmOrderSourceRef> srcreflineList = srcrefline.list();
                if (srcreflineList.size() > 0) {
                  // delete the source ref line
                  EscmOrderSourceRef sourcerefline = srcreflineList.get(0);
                  OBDal.getInstance().remove(sourcerefline);
                  OBDal.getInstance().flush();

                  // update the order line qty
                  objOrderLine.setUpdated(new java.util.Date());
                  objOrderLine.setUpdatedBy(objOrder.getUpdatedBy());
                  objOrderLine.setOrderedQuantity(objOrderLine.getOrderedQuantity()
                      .subtract(sourcerefline.getReservedQuantity()));
                  objOrderLine.setEscmIsmanual(false);
                  OBDal.getInstance().save(objOrderLine);

                  // delete the Order line , if order line qty is zero
                  if (objOrderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    OBDal.getInstance().remove(objOrderLine);
                  }

                }
              }
              // if entered qty greater than zero than update the ref qty / insert a record and
              // update the order line qty
              else {
                POOrderContractAddLinesDAO.insertsourceref(objOrderLine,
                    selectedRow.getString("requisition"), selectedRow.getString("id"),
                    selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
                    selectedRow.getString("department"), selectedRow.getString("quantity"),
                    selectedRow.getString("linedescription"), updateqtyflag, conn);
              }
            }
            // if line is not presented with selected product then insert a new line in order
            // line as well as source ref
            else {
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) > 0) {

                // checking in proposal soruce ref whether the pr is already present are not
                OBQuery<EscmProposalsourceRef> proposalSourceRef = OBDal.getInstance().createQuery(
                    EscmProposalsourceRef.class,
                    "as e where e.requisition.id= :purReqId and e.escmProposalmgmtLine.escmProposalmgmt.proposalstatus != 'CL' "
                        + " and e.escmProposalmgmtLine.escmProposalmgmt.proposalstatus != 'DIS' "
                        + " and e.escmProposalmgmtLine.escmProposalmgmt.proposalstatus != 'WD' ");
                proposalSourceRef.setNamedParameter("purReqId",
                    selectedRow.getString("requisition"));
                if (proposalSourceRef.list().size() == 0) {

                  // Check PR is already used in Bid
                  OBQuery<Escmbidsourceref> bidSourceRef = OBDal.getInstance().createQuery(
                      Escmbidsourceref.class,
                      " as e where e.requisition.id = :purReqId and e.escmBidmgmtLine.escmBidmgmt.bidstatus != 'CL' "
                          + " and e.escmBidmgmtLine.escmBidmgmt.bidstatus != 'CD' ");
                  bidSourceRef.setNamedParameter("purReqId", selectedRow.getString("requisition"));
                  if (bidSourceRef != null) {
                    if (bidSourceRef.list().size() > 0) {
                      // throw error message as pr is already added in bid
                      JSONObject errorMessage = new JSONObject();
                      errorMessage.put("severity", "error");
                      errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PR_AlreadyAddedBid"));
                      json.put("message", errorMessage);
                      return json;
                    }
                  }

                  // get the next line no based on Order id
                  final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
                      "select coalesce(max(line),0)+10 as lineno from c_orderline  where c_order_id=:id");
                  query.setParameter("id", strOrderId);
                  line = ((BigDecimal) query.list().get(0)).longValue();

                  Product objProduct = OBDal.getInstance().get(Product.class,
                      selectedRow.getString("product"));
                  objOrderLine = OBProvider.getInstance().get(OrderLine.class);
                  objOrderLine.setClient(objOrder.getClient());
                  objOrderLine.setOrganization(objOrder.getOrganization());
                  objOrderLine.setCreationDate(new java.util.Date());
                  objOrderLine.setCreatedBy(objOrder.getCreatedBy());
                  objOrderLine.setUpdated(new java.util.Date());
                  objOrderLine.setUpdatedBy(objOrder.getUpdatedBy());
                  objOrderLine.setActive(true);
                  objOrderLine.setOrderDate(objOrder.getOrderDate());
                  Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
                  objOrderLine
                      .setCurrency(objOrder.getCurrency() == null
                          ? (objOrder.getOrganization().getCurrency() == null ? objCurrency
                              : objOrder.getOrganization().getCurrency())
                          : objOrder.getCurrency());

                  // objOrderLine.setCurrency(objOrder.getCurrency());
                  objOrderLine.setWarehouse(objOrder.getWarehouse());
                  objOrderLine.setSalesOrder(objOrder);
                  objOrderLine.setLineNo(line);
                  objOrderLine.setEscmIsmanual(false);
                  OBQuery<TaxRate> objTaxQry = OBDal.getInstance().createQuery(TaxRate.class,
                      "as e order by e.creationDate desc");
                  objTaxQry.setMaxResult(1);
                  objTaxList = objTaxQry.list();
                  if (objTaxList.size() > 0) {
                    objOrderLine.setTax(objTaxList.get(0));
                  }
                  /*
                   * objOrderLine.setTax(OBDal.getInstance().get(TaxRate.class,
                   * "97E8BE0B96364939A4515D7FEFFA41BF")); // imported via data set
                   */ if (objProduct != null)
                    objOrderLine.setProduct(objProduct);
                  else
                    objOrderLine.setProduct(null);
                  objOrderLine.setUOM(reqline.getUOM());
                  objOrderLine.setEscmProdescription(selectedRow.getString("linedescription"));
                  objOrderLine
                      .setOrderedQuantity(new BigDecimal(selectedRow.getString("quantity")));
                  objOrderLine.setEFINUniqueCode(reqline.getEfinCValidcombination());
                  objOrderLine.setEFINUniqueCodeName(reqline.getEfinUniquecodename());
                  objOrderLine.setEfinBudEncumlines(reqline.getEfinBudEncumlines());
                  objOrderLine.setEfinMRequisitionline(reqline);
                  objOrderLine.setEscmNeedbydate(reqline.getNeedByDate());
                  // Task no. 5411 noteno.- 15613
                  objOrderLine.setUnitPrice(reqline.getUnitPrice());
                  // Task no. 0006911
                  // objOrderLine.setUnitPrice(BigDecimal.ZERO);
                  if (reqline.getUnitPrice() != null)
                    objOrderLine.setLineNetAmount(reqline.getUnitPrice()
                        .multiply(new BigDecimal(selectedRow.getString("quantity"))));
                  if (reqline.getUnitPrice() != null)
                    objOrderLine.setEscmLineTotalUpdated(reqline.getUnitPrice()
                        .multiply(new BigDecimal(selectedRow.getString("quantity"))));
                  objOrder.setEscmProposalmgmt(null);
                  objOrder.setEscmAddproposal(false);
                  OBDal.getInstance().save(objOrderLine);
                  OBDal.getInstance().flush();

                  // insert into Shipment Attribute
                  /*
                   * Escmordershipment objShipment = OBProvider.getInstance()
                   * .get(Escmordershipment.class); objShipment.setClient(objOrder.getClient());
                   * objShipment.setOrganization(objOrder.getOrganization());
                   * objShipment.setCreationDate(new java.util.Date());
                   * objShipment.setCreatedBy(objOrder.getCreatedBy()); objShipment.setUpdated(new
                   * java.util.Date()); objShipment.setUpdatedBy(objOrder.getUpdatedBy());
                   * objShipment.setActive(true); objShipment.setLineNo(line);
                   * objShipment.setSalesOrderLine(objOrderLine); if (objProduct != null)
                   * objShipment.setProduct(objProduct); else objShipment.setProduct(null);
                   * objShipment.setDescription(selectedRow.getString("linedescription"));
                   * objShipment.setUOM(reqline.getUOM()); objShipment.setNeedByDate(new Date());
                   * objShipment.setMovementQuantity(new
                   * BigDecimal(selectedRow.getString("quantity")));
                   * OBDal.getInstance().save(objShipment); OBDal.getInstance().flush();
                   */
                  // insert a record in order source ref
                  POOrderContractAddLinesDAO.insertsourceref(objOrderLine,
                      selectedRow.getString("requisition"), selectedRow.getString("id"),
                      selectedRow.getString("unitPrice"), selectedRow.getString("needByDate"),
                      selectedRow.getString("department"), selectedRow.getString("quantity"),
                      selectedRow.getString("linedescription"), updateqtyflag, conn);
                } else {
                  // throw error message as pr is already added in proposal
                  JSONObject successMessage = new JSONObject();
                  successMessage.put("severity", "error");
                  successMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_AlreadyAdded")
                      .replace("@", "Proposal Management"));
                  json.put("message", successMessage);
                  return json;
                }
              }
            }
          } else {
            // get the next line no based on Order id

            final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
                "select coalesce(max(line),0)+10 as lineno from c_orderline  where c_order_id=:id");
            query.setParameter("id", strOrderId);
            line = ((BigDecimal) query.list().get(0)).longValue();
            ArrayList<String> parentList = new ArrayList<String>();
            parentList.add(reqline.getId());
            POOrderContractAddLinesDAO.getParentLines(reqline, parentList, objOrder, line, conn,
                selectedRow);
          }
        }
        objOrder.setUpdated(new Date());
        objOrder.setEscmAddrequisition(true);
        if (!"".equals(reqId)) {
          Requisition req = Utility.getObject(Requisition.class, reqId);
          if (req.getEscmContactType() != null) {
            objOrder.setEscmContactType(req.getEscmContactType());
            receiveType = POOrderContractAddLinesDAO
                .getReceiveType(req.getEscmContactType().getId());
            objOrder.setEscmReceivetype(receiveType);
            if (selectedlines.length() == 1) {
              objOrder.setEscmMaintenanceProject(req.getEscmMaintenanceProject());
              objOrder.setEscmMaintenanceCntrctNo(req.getESCMMaintenanceContractNo());
            }
          }
        }
        OBDal.getInstance().save(objOrder);
        OBDal.getInstance().flush();
        // setting success message
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }
      // setting error message
      else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

    } /*
       * catch (Exception e) { log.error("Exception in PO and Contract Summary Add Line :", e);
       * OBDal.getInstance().rollbackAndClose(); e.printStackTrace(); throw new OBException(e); }
       */
    catch (Exception e) {
      log.error("Exception in PO and Contract Summary Add Line :", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        json.put("message", errorMessage);
        return json;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        log.error("Exception in PO and Contract Summary Add Line :", e1);
        throw new OBException(e1);

      }
    }

    finally {
      OBDal.getInstance().commitAndClose();
      OBDal.getInstance().getSession().clear();
      OBContext.restorePreviousMode();
    }
  }
}
