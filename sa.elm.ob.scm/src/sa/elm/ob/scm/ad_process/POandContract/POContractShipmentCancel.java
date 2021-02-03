package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.Escmordershipment;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author gopalakrishnan on 01/08/2017
 * 
 */

public class POContractShipmentCancel implements Process {
  /**
   * This servlet class is responsible to Cancel PO and Contract Summary Records
   */
  private static Logger log = Logger.getLogger(POContractShipmentCancel.class);

  @SuppressWarnings("unchecked")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      String strShipmentid = (String) bundle.getParams().get("Escm_Ordershipment_ID");
      String comments = (String) bundle.getParams().get("notes").toString();
      log.debug("comments" + comments);
      BigDecimal qtyCount = BigDecimal.ZERO;
      Escmordershipment objShipment = OBDal.getInstance().get(Escmordershipment.class,
          strShipmentid);
      OrderLine objOrderLine = objShipment.getSalesOrderLine();
      Order objOrder = objOrderLine.getSalesOrder();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objOrderLine.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();

      String sqlQuery = " select  count(distinct ship.escm_ordershipment_id) from c_order ord "
          + " join c_orderline line on line.c_order_id=ord.c_order_id "
          + " join escm_ordershipment ship on ship.c_orderline_id=line.c_orderline_id "
          + " where (coalesce(movementqty,0)-(coalesce(quantityporec,0)- coalesce(quantityreturned,0)-coalesce(quantityrejected,0)-coalesce(quantityirr,0))-coalesce(quantitycanceled ,0)) > 0 "
          + " and ord.c_order_id='" + objOrder.getId() + "' ";
      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      log.debug("sqlQuery" + sqlQuery);
      if (queryList != null) {
        List<Object> rows = queryList.list();
        if (rows.size() > 0) {
          qtyCount = new BigDecimal(Integer.valueOf(rows.get(0).toString()));
        }

      }
      if (objShipment != null) {
        BigDecimal poreceivedQty = objShipment.getQuantityporec()
            .subtract(objShipment.getQuantityreturned()).subtract(objShipment.getQuantityrejected())
            .subtract(objShipment.getQuantityirr());
        BigDecimal cancelledQty = objShipment.getMovementQuantity().subtract(poreceivedQty)
            .subtract(objShipment.getQuantitycanceled());
        objShipment.setQuantitycanceled(cancelledQty);
        // revert quantity in PR line
        Boolean isReleased = Utility.releasePROrderQty(objOrderLine.getId(), cancelledQty);
        if (!isReleased) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PO_Cancel_Failed@");
          bundle.setResult(result);
          return;
        }

        // objShipment.setQuantitycanceled(cancelledQty);
        objShipment.setUpdated(new java.util.Date());
        objShipment.setUpdatedBy(OBContext.getOBContext().getUser());

        objShipment.setCancelDate(new java.util.Date());
        objShipment.setCancelreason(comments);
        objShipment.setCancelledby(OBContext.getOBContext().getUser());
        OBDal.getInstance().save(objShipment);

        if (qtyCount.compareTo(BigDecimal.ONE) == 0) {
          objOrder.setUpdated(new java.util.Date());
          objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
          objOrder.setEscmAppstatus("ESCM_CA");
          objOrder.setEutNextRole(null);

          if (!StringUtils.isEmpty(objOrder.getId())) {
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", objOrder.getId());
            historyData.put("Comments", "");
            historyData.put("Status", "CA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
            historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);
          }

        }
        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Shipment_Cancelled@");
        bundle.setResult(result);
        return;
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in Purchase Order shipment Cancel:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().getSession().clear();
    }
  }
}