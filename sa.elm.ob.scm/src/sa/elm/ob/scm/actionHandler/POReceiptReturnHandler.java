package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;

/*
 * Inserting records in retuen to vendor tab and updating quantity in intial receipt.
 */
public class POReceiptReturnHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptReturnHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    // int checkCount = 0;
    int countOfLineWithZeroQty = 0;
    String deliverref = null;
    BigDecimal quantity = BigDecimal.ZERO;
    BigDecimal amount = BigDecimal.ZERO;
    ShipmentInOut originalInout = null;
    String receivingType = null;
    String receiveType = null;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("Return");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      final String inoutId = jsonRequest.getString("inpmInoutId");
      EscmInitialReceipt IR = null;
      EscmInitialReceipt deliver = null;
      // long line = 0;

      // ShipmentInOut inout = null;
      String supplierBatch = "";
      // Date currentDate = new Date();
      Connection conn = OBDal.getInstance().getConnection();
      // for (int a = 0; a < selectedlines.length(); a++) {
      // JSONObject selectedRowCheck = selectedlines.getJSONObject(a);
      // if (selectedRowCheck.getString("summaryLevel").equals("false")) {
      // checkCount++;
      // }
      // }
      ShipmentInOut sinout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);

      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRow = selectedlines.getJSONObject(a);
        log.debug("selectedRow:" + selectedRow);
        String initialreceipt = selectedRow.getString("escmInitialreceipt");

        if (selectedRow.has("newamount")) {
          amount = new BigDecimal(selectedRow.getString("newamount"));
        }

        if (selectedRow.has("delref")) {
          deliverref = selectedRow.getString("delref");
        }
        log.debug("initialreceipt:" + initialreceipt);

        sinout = OBDal.getInstance().get(ShipmentInOut.class,
            selectedRow.getString("goodsShipment"));
        // inout = OBDal.getInstance()
        // .get(ShipmentInOut.class, selectedRow.getString("goodsShipment"));

        // ps = conn.prepareStatement(
        // " select coalesce(max(line),0)+10 as lineno from escm_initialreceipt where
        // m_inout_id=?");
        // ps.setString(1, inoutId);
        // rs = ps.executeQuery();
        // if (rs.next()) {
        // line = rs.getLong("lineno");
        // }
        String status = selectedRow.getString("status");
        if (selectedRow.has("quantity")) {
          quantity = new BigDecimal(selectedRow.getString("quantity"));
        }
        // BigDecimal availablequantity = new BigDecimal(selectedRow.getString("reservedQuantity"));

        // String returnDate = selectedRow.getString("returnDate");
        if (selectedRow.getString("supplierBatch").equals("null")) {
          supplierBatch = "";
        } else {
          supplierBatch = selectedRow.getString("supplierBatch");
        }
        EscmInitialReceipt initial = OBDal.getInstance().get(EscmInitialReceipt.class,
            initialreceipt);
        EscmInitialreceiptView initOldView = initial.getParentLine();
        if (deliverref != null) {
          deliver = OBDal.getInstance().get(EscmInitialReceipt.class, deliverref);
        }
        originalInout = initial.getGoodsShipment();

        receivingType = originalInout.getEscmReceivingtype();
        receiveType = originalInout.getEscmReceivetype();
        // validating Quantity should not be <= 0
        if (originalInout != null && ((receivingType != null && !receiveType.equals("AMT")
            && quantity.compareTo(BigDecimal.ZERO) <= 0)
            || (receivingType != null && receiveType.equals("AMT")
                && amount.compareTo(BigDecimal.ZERO) <= 0))) {

          countOfLineWithZeroQty = countOfLineWithZeroQty + 1;
        } else {

          String whereClause = " as e where e.sourceRef.id=:srcRefID and e.goodsShipment.id=:inoutID ";
          if (status.equals("I")) {
            whereClause += " and e.alertStatus='I' ";
          } else if (status.equals("A")) {
            whereClause += " and e.alertStatus='A' ";
          } else if (status.equals("R")) {
            whereClause += " and e.alertStatus='R' ";
          } else if (status.equals("D") && deliver == null && (receivingType != null
              && (receivingType.equals("SR") || receivingType.equals("PROJ")))) {
            whereClause += " and e.alertStatus='D' ";
          } else if (status.equals("D") && deliver != null)
            whereClause += " and e.alertStatus='D' and e.deliverRef.id='" + deliver.getId() + "' ";

          if ((status.equals("D") && deliver == null
              && (receivingType != null
                  && (receivingType.equals("SR") || receiveType.equals("AMT"))))
              || (status.equals("I") || status.equals("A") || status.equals("R")
                  || (status.equals("D") && deliver != null))) {
            OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance()
                .createQuery(EscmInitialReceipt.class, whereClause);
            chklineexistQry.setNamedParameter("srcRefID", initial.getId());
            chklineexistQry.setNamedParameter("inoutID", inoutId);
            if (chklineexistQry.list().size() > 0) {
              IR = chklineexistQry.list().get(0);
              if (selectedRow.has("quantity") && receiveType != null
                  && !receiveType.equals("AMT")) {
                if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                  OBDal.getInstance().remove(IR);
                } else {
                  IR.setQuantity(quantity);
                  IR.setNotes(selectedRow.getString("notes"));
                  OBDal.getInstance().save(IR);
                }
              } else if (selectedRow.has("newamount") && receivingType != null
                  && receiveType.equals("AMT")) {
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                  OBDal.getInstance().remove(IR);
                } else {
                  IR.setTOTLineAmt(amount);
                  IR.setUnitprice(initial.getUnitprice());
                  IR.setQuantity(initial.getQuantity());
                  IR.setNotes(selectedRow.getString("notes"));
                  String recType = IR.getGoodsShipment().getEscmReceivingtype();
                  if (receivingType != null && receiveType.equals("AMT")
                      && (recType.equals("RET"))) {
                    IR.setUnitprice(IR.getTOTLineAmt());
                    IR.setReceivedAmount(IR.getTOTLineAmt());
                  }
                  OBDal.getInstance().save(IR);
                }

              }

            } else {
              if (receivingType != null
                  && ((quantity.compareTo(BigDecimal.ZERO) > 0 && !receiveType.equals("AMT"))
                      || (amount.compareTo(BigDecimal.ZERO) > 0 && receiveType.equals("AMT")))) {
                if (initOldView == null) {

                  // update quantity in inspection
                  /*
                   * OBQuery<ESCMInspection> inspection = OBDal.getInstance().createQuery(
                   * ESCMInspection.class, "escmInitialreceipt.id='" + initial.getId() +
                   * "' and status = 'A' "); ESCMInspection insp = inspection.list().get(0);
                   * insp.setQuantity(insp.getQuantity().subtract(quantity));
                   * OBDal.getInstance().save(insp); OBDal.getInstance().flush();
                   */
                  // insertion in return to vendor line
                  // String recType = IR.getGoodsShipment().getEscmReceivingtype();

                  IR = OBProvider.getInstance().get(EscmInitialReceipt.class);
                  // BigDecimal totalineAmount = IR.getTOTLineAmt();

                  IR.setOrganization(sinout.getOrganization());
                  IR.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  IR.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
                  IR.setLineNo(initial.getLineNo());
                  if (receivingType != null && !receiveType.equals("AMT")) {
                    IR.setQuantity(quantity);
                  } else if (receivingType != null && receiveType.equals("AMT")) {
                    IR.setTOTLineAmt(amount);
                    IR.setUnitprice(amount);
                    IR.setQuantity(BigDecimal.ONE);
                    IR.setReceivedAmount(amount);
                  }
                  IR.setProduct(
                      OBDal.getInstance().get(Product.class, selectedRow.getString("product")));
                  if (IR.getProduct() != null) {
                    IR.setImage(IR.getProduct().getImage());
                  }

                  IR.setManual(false);
                  IR.setDescription(selectedRow.getString("description"));
                  IR.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
                  IR.setSupplierbatch(supplierBatch);
                  // IR.setLineNumber(selectedRow.getLong("lineNo"));
                  IR.setNotes(selectedRow.getString("notes"));
                  IR.setGoodsShipment(sinout);
                  IR.setSourceRef(initial);
                  // returnVendor.setEscmInspection(null);
                  // returnVendor.setGoodsShipmentLine(null);
                  if (status.equals("D") && deliver != null) {
                    IR.setDeliverRef(deliver);
                  }
                  IR.setAlertStatus(status);
                  OBDal.getInstance().save(IR);
                } else {
                  // if line is not already present then check line's parent is already present
                  // if present then insert the selected line in already exists hierarchy
                  // if parent is not already exist, then insert its whole hierarchy

                  ArrayList<String> parentList = new ArrayList<String>();
                  parentList.add(initial.getId());
                  // line = PoReceiptDeliveryDAO.getLineNo(conn, sinout.getId());
                  PoReceiptDeliveryDAO.getParentLines(initial, parentList, sinout, 0, conn,
                      selectedRow);
                }
              }
            }
            OBDal.getInstance().flush();
          }
        }
      }
      // if all selected line with qty 0 then throw error
      if (countOfLineWithZeroQty == selectedlines.length()) {
        OBDal.getInstance().rollbackAndClose();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        if (originalInout != null && receivingType != null && !receiveType.equals("AMT")) {
          errorMessage.put("text", OBMessageUtils.messageBD("Escm_IR_Quantity"));
        } else {
          errorMessage.put("text", OBMessageUtils.messageBD("Escm_RetAmtBase_NotLessThanZero"));
        }
        json.put("message", errorMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      }

    } catch (Exception e) {
      log.error("Exception in POReceiptReturnHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }
}
