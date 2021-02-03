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
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;

public class POInspectionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POInspectionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    int checkCount = 0;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject inspectLines = jsonparams.getJSONObject("Inspection");
      final String inoutId = jsonRequest.getString("inpmInoutId");
      JSONArray selectedlines = inspectLines.getJSONArray("_selection");

      // long lineno = 10;
      Connection conn = OBDal.getInstance().getConnection();

      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRowCheck = selectedlines.getJSONObject(a);
        if (selectedRowCheck.getString("summary").equals("false")) {
          checkCount++;
        }
      }
      // shipment
      ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
      // get recent lineno
      OBQuery<EscmInitialReceipt> linesQry = OBDal.getInstance().createQuery(
          EscmInitialReceipt.class,
          "as e where e.goodsShipment.id=:inoutID order by e.lineNo desc");
      linesQry.setNamedParameter("inoutID", inoutId);
      linesQry.setMaxResult(1);
      // if (linesQry.list().size() > 0) {
      // EscmInitialReceipt objExistLine = linesQry.list().get(0);
      // lineno = objExistLine.getLineNo() + 10;
      // }
      if (selectedlines.length() > 0 && checkCount > 0) {
        for (int i = 0; i < selectedlines.length(); i++) {
          JSONObject selectedRow = selectedlines.getJSONObject(i);
          String escmInitialreceipt = selectedRow.getString("escmInitialreceipt");
          String strStatus = selectedRow.getString("alertStatus"),
              description = selectedRow.getString("description");
          objInout = OBDal.getInstance().get(ShipmentInOut.class,
              selectedRow.getString("goodsShipment"));
          // int qty = selectedRow.getInt("quantity");
          // get Initial receipt line
          EscmInitialReceipt objIRLine = OBDal.getInstance().get(EscmInitialReceipt.class,
              escmInitialreceipt);
          EscmInitialreceiptView initOldView = objIRLine.getParentLine();

          if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) < 0) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
            json.put("message", errorMessage);
            return json;
          }
          // check already present in the lines
          // exist update quantity
          if (escmInitialreceipt.length() == 32) {
            String whereClause = " as e where e.sourceRef.id=:srcrefID and e.goodsShipment.id=:inoutID";
            if (strStatus.equals("A")) {
              whereClause += " and e.alertStatus='A' ";
            } else if (strStatus.equals("R")) {
              whereClause += " and e.alertStatus='R' ";
            }
            OBQuery<EscmInitialReceipt> existLineQry = OBDal.getInstance()
                .createQuery(EscmInitialReceipt.class, whereClause);
            existLineQry.setNamedParameter("srcrefID", escmInitialreceipt);
            existLineQry.setNamedParameter("inoutID", inoutId);
            existLineQry.setMaxResult(1);
            if (existLineQry.list().size() > 0) {
              EscmInitialReceipt objInitialReceipt = existLineQry.list().get(0);
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) == 0) {
                OBDal.getInstance().remove(objInitialReceipt);
              } else {
                objInitialReceipt.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
                objInitialReceipt.setDescription(description);
                if (strStatus.equals("A"))
                  objInitialReceipt.setAlertStatus("A");
                else if (strStatus.equals("R"))
                  objInitialReceipt.setAlertStatus("R");
                objInitialReceipt.setNotes(selectedRow.getString("notes"));
                objInitialReceipt.setFailurereason(null);
                if (selectedRow.getString("qualityCode") != null
                    && !selectedRow.getString("qualityCode").equals(""))
                  objInitialReceipt.setQualityCode(selectedRow.getString("qualityCode"));
                OBDal.getInstance().save(objInitialReceipt);
              }
            } else if (new BigDecimal(selectedRow.getString("quantity"))
                .compareTo(BigDecimal.ZERO) > 0) {
              if (initOldView == null) {
                EscmInitialReceipt newObject = OBProvider.getInstance()
                    .get(EscmInitialReceipt.class);
                newObject.setGoodsShipment(objInout);
                newObject.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
                newObject.setOrganization(objInout.getOrganization());
                newObject.setClient(objInout.getClient());
                if (strStatus.equals("A"))
                  newObject.setAlertStatus("A");
                else if (strStatus.equals("R"))
                  newObject.setAlertStatus("R");

                newObject.setUnitprice(BigDecimal.ZERO);
                newObject.setProduct(objIRLine.getProduct());
                newObject.setManual(false);
                newObject.setImage(newObject.getProduct().getImage());
                newObject.setSourceRef(objIRLine);
                newObject.setDescription(description);
                newObject.setLineNo(objIRLine.getLineNo());
                newObject.setNotes(selectedRow.getString("notes"));
                /*
                 * if (objIRLine != null) {
                 * newObject.setEscmOrdershipment(objIRLine.getEscmOrdershipment()); }
                 */
                if (selectedRow.getString("qualityCode") != null
                    && !selectedRow.getString("qualityCode").equals("")
                    && !selectedRow.getString("qualityCode").equals("null"))
                  newObject.setQualityCode(selectedRow.getString("qualityCode"));
                newObject.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
                OBDal.getInstance().save(newObject);
                // lineno = lineno + 10;
              } else {
                // if line is not already present then check line's parent is already present
                // if present then insert the selected line in already exists hierarchy
                // if parent is not already exist, then insert its whole hierarchy

                ArrayList<String> parentList = new ArrayList<String>();
                parentList.add(objIRLine.getId());
                // lineno = PoReceiptDeliveryDAO.getLineNo(conn, objInout.getId());
                PoReceiptDeliveryDAO.getParentLines(objIRLine, parentList, objInout, 0, conn,
                    selectedRow);
              }
            }
          }
          OBDal.getInstance().flush();
        }

        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;

      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in POInspectionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }
}
