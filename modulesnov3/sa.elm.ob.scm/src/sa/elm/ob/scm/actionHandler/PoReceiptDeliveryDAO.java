package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;

/**
 * 
 * @author Sathish kumar on oct-27-2017
 *
 */

public class PoReceiptDeliveryDAO {

  private static Logger LOG = Logger.getLogger(POReceiptDeliveryHandler.class);

  /**
   * 
   * @param oldIntId
   * @param inout
   * @param selectedRow
   * @param conn
   * @return true or false
   */

  public static boolean checkSelectedLineAlreadyExists(EscmInitialReceipt oldIntId,
      ShipmentInOut inout, JSONObject selectedRow, Connection conn) {

    // While inserting the tree hierarchy, check whether the line is already exist.
    // if exists then check qty, if qty is zero then remove that source ref line and bidmgmtline
    // if bidmgmtline's parent has child
    EscmInitialReceipt originalReceipt = null;

    OBQuery<EscmInitialReceipt> chkLineExists = OBDal.getInstance().createQuery(
        EscmInitialReceipt.class,
        "as e where e.sourceRef =:srcrefId and e.goodsShipment.id =:inoutID");
    chkLineExists.setNamedParameter("srcrefId", oldIntId.getId());
    chkLineExists.setNamedParameter("inoutID", inout.getId());
    chkLineExists.setMaxResult(1);
    List<EscmInitialReceipt> intialReceiptList = chkLineExists.list();
    if (intialReceiptList != null && intialReceiptList.size() > 0) {
      originalReceipt = intialReceiptList.get(0);
      try {
        if (new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) == 0) {
          OBDal.getInstance().remove(originalReceipt);
        } else {
          oldIntId.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
        }
        OBDal.getInstance().flush();
      } catch (JSONException e) {
        if (LOG.isDebugEnabled()) {
          LOG.error("Exception in POReceiptDeliveryAddLinesHandler :", e);
        }
        OBDal.getInstance().rollbackAndClose();
      }
      return true;
    }
    return false;
  }

  /**
   * Method to insert ParentLines
   * 
   * @param parentList
   * @param inout
   * @param line
   * @param conn
   * @param selectedRow
   * @param parentIntialLine
   * @param updateqtyflag
   * 
   */
  public static void insertParentLines(ArrayList<String> parentList, ShipmentInOut inout, long line,
      Connection conn, JSONObject selectedRow, EscmInitialReceipt parentIntialLine,
      boolean updateqtyflag) {

    Long lineNo = line;
    EscmInitialReceipt originalLine = parentIntialLine;
    EscmInitialReceipt oldIntialRecline = null;
    EscmInitialreceiptView parentLine = null;
    String custodyitem = null;
    String receiveingType = null;
    BigDecimal amount = BigDecimal.ZERO;
    String deliverref = null;
    EscmInitialReceipt deliver = null;
    EscmInitialReceipt sourceRef = null;
    try {

      if (selectedRow.has("escmInitialreceipt")) {
        sourceRef = OBDal.getInstance().get(EscmInitialReceipt.class,
            selectedRow.getString("escmInitialreceipt"));
      }
      if (selectedRow.has("custodyItem")) {
        custodyitem = selectedRow.getString("custodyItem");
      }
      if (selectedRow.has("newamount")) {
        amount = new BigDecimal(selectedRow.getString("newamount"));
      }
      for (int i = parentList.size() - 1; i >= 0; i--) {
        EscmInitialReceipt parentRecLine = OBDal.getInstance().get(EscmInitialReceipt.class,
            parentList.get(i));
        if (sourceRef != null)
          receiveingType = inout.getEscmReceivetype();
        if ((selectedRow.has("quantity")
            && new BigDecimal(selectedRow.getString("quantity")).compareTo(BigDecimal.ZERO) > 0)
            || (receiveingType != null && receiveingType.equals("AMT")
                && selectedRow.has("newamount") && amount.compareTo(BigDecimal.ZERO) > 0)) {
          EscmInitialReceipt initialnew = (EscmInitialReceipt) DalUtil.copy(parentRecLine, false);
          initialnew.setRemainingQuantity(BigDecimal.ZERO);
          initialnew.setRemainingAmt(BigDecimal.ZERO);
          LOG.debug("trxtype>" + inout.getEscmReceivingtype());
          if (inout.getEscmReceivingtype().equals("INS")) {
            if (parentRecLine.getParentLine() != null) {

              initialnew.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              initialnew.setDescription(selectedRow.getString("description"));
              initialnew.setNotes(selectedRow.getString("notes"));
              if (selectedRow.getString("qualityCode") != null
                  && !selectedRow.getString("qualityCode").equals(""))
                initialnew.setQualityCode(selectedRow.getString("qualityCode"));
            }

            if (selectedRow.getString("alertStatus") != null
                && selectedRow.getString("alertStatus").equals("A")) {
              initialnew.setAlertStatus("A");
            } else if (selectedRow.getString("alertStatus") != null
                && selectedRow.getString("alertStatus").equals("R")) {
              initialnew.setAlertStatus("R");
            }
          } else if (inout.getEscmReceivingtype().equals("RET")) {
            String status = selectedRow.getString("status");
            if (selectedRow.has("delref")) {
              deliverref = selectedRow.getString("delref");
            }
            if (deliverref != null) {
              deliver = OBDal.getInstance().get(EscmInitialReceipt.class, deliverref);
            }
            if (parentRecLine.getParentLine() != null) {
              if (selectedRow.has("quantity")) {
                initialnew.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              }
              initialnew.setNotes(selectedRow.getString("notes"));
              if (selectedRow.getString("supplierBatch").equals("null")) {
                initialnew.setSupplierbatch("");
              } else {
                initialnew.setSupplierbatch(selectedRow.getString("supplierBatch"));
              }
              if (status.equals("D") && deliver != null) {
                initialnew.setDeliverRef(deliver);
              }
            }
            if (receiveingType != null && receiveingType.equals("AMT")
                && selectedRow.has("newamount")) {
              initialnew.setTOTLineAmt(amount);
              initialnew.setUnitprice(amount);
              initialnew.setQuantity(BigDecimal.ONE);
              initialnew.setReceivedAmount(amount);
            }
            initialnew.setAlertStatus(status);
          } else {
            if (selectedRow.getString("id").equals(parentRecLine.getId())) {
              initialnew.setQuantity(new BigDecimal(selectedRow.getString("quantity")));
              initialnew.setDescription(selectedRow.getString("description"));
              initialnew.setNotes(selectedRow.getString("notes"));
            }
          }

          initialnew.setSourceRef(parentRecLine);
          initialnew.setGoodsShipment(inout);
          initialnew.setLineNo(parentRecLine.getLineNo());

          if (custodyitem != null && custodyitem.equals("false")) {
            initialnew.setCustodyItem(false);
          } else {
            initialnew.setCustodyItem(true);
          }
          initialnew.setManual(false);
          if (oldIntialRecline == null && originalLine == null) {
            initialnew.setParentLine(null);
          } else {
            if (originalLine == null) {
              parentLine = OBDal.getInstance().get(EscmInitialreceiptView.class,
                  oldIntialRecline.getId());
              initialnew.setParentLine(parentLine);
            } else {
              parentLine = OBDal.getInstance().get(EscmInitialreceiptView.class,
                  originalLine.getId());
              initialnew.setParentLine(parentLine);
              originalLine = null;
            }
          }
          OBDal.getInstance().save(initialnew);
          OBDal.getInstance().flush();
          oldIntialRecline = initialnew;
          lineNo = lineNo + 10;
        }
      }

    } catch (JSONException e) {
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception in POReceiptDeliveryAddLinesHandler :", e);
      }
      OBDal.getInstance().rollbackAndClose();
    }

  }

  /**
   * Method to get Parent Lines
   * 
   * @param initId
   * @param parentList
   * @param inout
   * @param line
   * @param conn
   * @param selectedRow
   */

  public static void getParentLines(EscmInitialReceipt initId, ArrayList<String> parentList,
      ShipmentInOut inout, long line, Connection conn, JSONObject selectedRow) {
    EscmInitialreceiptView parentLine = initId.getParentLine();
    if (parentLine != null) {
      String parentId = parentLine.getId();
      EscmInitialReceipt parentReqLine = OBDal.getInstance().get(EscmInitialReceipt.class,
          parentId);

      OBQuery<EscmInitialReceipt> chkLineExists = OBDal.getInstance().createQuery(
          EscmInitialReceipt.class,
          "as e where e.sourceRef.id =:parentID and e.goodsShipment.id =:inoutID");
      chkLineExists.setNamedParameter("parentID", parentId);
      chkLineExists.setNamedParameter("inoutID", inout.getId());
      chkLineExists.setMaxResult(1);

      List<EscmInitialReceipt> intialLineList = chkLineExists.list();
      // check its parent is already exist if exists then insert the selected line in parent tree
      if (intialLineList.size() > 0) {
        insertParentLines(parentList, inout, line, conn, selectedRow, intialLineList.get(0), false);
      } else {
        parentList.add(parentId);
        getParentLines(parentReqLine, parentList, inout, line, conn, selectedRow);
      }
    } else {
      insertParentLines(parentList, inout, line, conn, selectedRow, null, false);
    }

  }

  /**
   * Method to get the next line no based on bid management id
   * 
   * @param conn
   * @param inoutId
   * @return lineno
   */
  public static long getLineNo(Connection conn, String inoutId) {

    PreparedStatement ps = null;
    ResultSet rs = null;
    long lineNo = 10;

    // get the next line no based on bid management id
    try {
      ps = conn.prepareStatement(
          " select coalesce(max(line),0)+10 as lineno from escm_initialreceipt where m_inout_id=?");
      ps.setString(1, inoutId);
      rs = ps.executeQuery();
      if (rs.next()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("st:" + ps.toString());
        }
        return rs.getLong("lineno");
      }
    } catch (SQLException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error while getting id in POReceiptDeliveryAddLinesHandler" + e, e);
      }
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {

      }
    }

    return lineNo;
  }

  /**
   * Method to check whether the parent has leaf
   * 
   * @param parentMgmtLine
   * @param inout
   * @return true or false
   */

  public static boolean checkparentHasLeaf(EscmInitialReceipt parentMgmtLine, ShipmentInOut inout) {

    OBQuery<EscmInitialReceipt> chkLeafExists = OBDal.getInstance().createQuery(
        EscmInitialReceipt.class,
        "as e where e.goodsShipment.id =:inoutID and e.parentLine.id =:parentLnID");
    chkLeafExists.setNamedParameter("parentLnID", parentMgmtLine.getId());
    chkLeafExists.setNamedParameter("inoutID", inout.getId());
    chkLeafExists.setMaxResult(1);

    if (chkLeafExists.list().size() > 1) {
      return true;
    }
    return false;
  }

}
