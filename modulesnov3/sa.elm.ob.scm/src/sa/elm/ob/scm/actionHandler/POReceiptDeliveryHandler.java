package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;

public class POReceiptDeliveryHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(POReceiptDeliveryHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    // PreparedStatement ps = null;
    int checkCount = 0;
    // ResultSet rs = null;
    int countOfLineWithZeroQty = 0;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject encumlines = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = encumlines.getJSONArray("_selection");
      ShipmentInOut inout = null;
      Connection conn = OBDal.getInstance().getConnection();
      // long line = 10;
      final String inoutId = jsonRequest.getString("inpmInoutId");
      inout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
      for (int a = 0; a < selectedlines.length(); a++) {
        JSONObject selectedRowCheck = selectedlines.getJSONObject(a);
        if (selectedRowCheck.getString("summaryLevel").equals("false")) {
          checkCount++;
        }
      }
      // delete exisiting lines
      if (selectedlines.length() > 0 && checkCount > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          String initialid = selectedRow.getString("id");
          inout = OBDal.getInstance().get(ShipmentInOut.class,
              selectedRow.getString("goodsShipment"));
          String custodyitem = selectedRow.getString("custodyItem");

          EscmInitialReceipt initold = OBDal.getInstance().get(EscmInitialReceipt.class, initialid);
          EscmInitialreceiptView initOldView = initold.getParentLine();

          if (new BigDecimal(selectedRow.getString("quantity"))
              .compareTo((new BigDecimal(selectedRow.getString("acceptedQuantity")))
                  .subtract(new BigDecimal(selectedRow.getString("deliveredqty")))) > 0) {
            OBDal.getInstance().rollbackAndClose();
            JSONObject errorMessage = new JSONObject();
            errorMessage.put("severity", "error");
            errorMessage.put("text", OBMessageUtils.messageBD("ESCM_POFinalRec_GrtActQty"));
            json.put("message", errorMessage);
            return json;
          }
          if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) <= 0) {
            // OBDal.getInstance().rollbackAndClose();
            // JSONObject errorMessage = new JSONObject();
            // errorMessage.put("severity", "error");
            // errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
            // json.put("message", errorMessage);
            // return json;
            countOfLineWithZeroQty = countOfLineWithZeroQty + 1;
          } else {
            OBQuery<EscmInitialReceipt> chklineexistQry = OBDal.getInstance().createQuery(
                EscmInitialReceipt.class,
                "as e where e.goodsShipment.id=:inoutID and e.sourceRef.id=:initialID");
            chklineexistQry.setNamedParameter("inoutID", inoutId);
            chklineexistQry.setNamedParameter("initialID", initialid);

            chklineexistQry.setMaxResult(1);
            log.debug("initial size:" + chklineexistQry.list().size());
            if (chklineexistQry.list().size() > 0) {
              EscmInitialReceipt upinitial = chklineexistQry.list().get(0);
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) == 0) {
                OBDal.getInstance().remove(upinitial);
              } else {

                upinitial.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
                upinitial.setDescription(selectedRow.getString("description"));
                upinitial.setFailurereason(null);
                upinitial.setNotes(selectedRow.getString("notes"));
                if (custodyitem.equals("false")) {
                  upinitial.setCustodyItem(false);
                } else
                  upinitial.setCustodyItem(true);
                OBDal.getInstance().save(upinitial);
              }
            } else {
              if (new BigDecimal(selectedRow.getString("quantity"))
                  .compareTo(BigDecimal.ZERO) > 0) {
                if (initOldView == null) {
                  EscmInitialReceipt initialnew = (EscmInitialReceipt) DalUtil.copy(initold, false);
                  initialnew.setRemainingQuantity(BigDecimal.ZERO);
                  initialnew.setRemainingAmt(BigDecimal.ZERO);
                  // ps = conn.prepareStatement(
                  // " select coalesce(max(line),0)+10 as lineno from escm_initialreceipt where
                  // m_inout_id=?");
                  // ps.setString(1, inoutId);
                  // rs = ps.executeQuery();
                  // if (rs.next()) {
                  // line = rs.getLong("lineno");
                  // log.debug("line:" + line);
                  // }
                  initialnew.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
                  initialnew.setSourceRef(initold);
                  initialnew.setGoodsShipment(inout);
                  initialnew.setLineNo(initold.getLineNo());
                  initialnew.setNotes(selectedRow.getString("notes"));
                  initialnew.setDescription(selectedRow.getString("description"));
                  if (custodyitem.equals("false")) {
                    initialnew.setCustodyItem(false);
                  } else
                    initialnew.setCustodyItem(true);
                  OBDal.getInstance().save(initialnew);
                  OBDal.getInstance().flush();
                  log.debug("initialnew:" + initialnew.getId());
                } else {
                  /*
                   * if (!PoReceiptDeliveryDAO.checkSelectedLineAlreadyExists(initold, inout,
                   * selectedRow, conn)) {
                   */
                  // if line is not already present then check line's parent is already present
                  // if present then insert the selected line in already exists hierarchy
                  // if parent is not already exist, then insert its whole hierarchy
                  ArrayList<String> parentList = new ArrayList<String>();
                  parentList.add(initold.getId());
                  // line = PoReceiptDeliveryDAO.getLineNo(conn, inout.getId());
                  PoReceiptDeliveryDAO.getParentLines(initold, parentList, inout, 0, conn,
                      selectedRow);
                  // }
                }

              }
            }
          }
        }
        // if all selected line with qty 0 then throw error
        if (countOfLineWithZeroQty == selectedlines.length()) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject errorMessage = new JSONObject();
          errorMessage.put("severity", "error");
          errorMessage.put("text", OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
          json.put("message", errorMessage);
          return json;
        } else {
          OBDal.getInstance().flush();

          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
          json.put("message", successMessage);
          return json;
        }
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }

    } catch (Exception e) {
      log.error("Exception in POReceiptDeliveryHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      // try {
      // if (rs != null) {
      // rs.close();
      // }
      // if (ps != null) {
      // ps.close();
      // }
      // } catch (Exception e) {
      // }
      // OBContext.restorePreviousMode();
    }
  }
}
