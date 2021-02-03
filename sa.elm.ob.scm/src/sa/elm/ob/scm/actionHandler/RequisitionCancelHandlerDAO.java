package sa.elm.ob.scm.actionHandler;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmRequisitionlineV;

public class RequisitionCancelHandlerDAO {
  private static Logger log = Logger.getLogger(RequisitionCancelHandlerDAO.class);

  public static int updateChildAsCancel(RequisitionLine Line, JSONObject selectedRow, String lineNo,
      Date currentDate, User user) {
    // TODO Auto-generated method stub
    int i = 0;
    try {
      int parentLinesize = 0;
      String reqline = null;
      RequisitionLine reqlineobj = null;
      String sql = null;
      Query qry = null;
      // String sql1 = null;
      // Query qry1 = null;
      // String sql2 = null;
      // Query qry2 = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      PreparedStatement st1 = null;
      // ResultSet rs1 = null;
      String LineNo = lineNo;
      EscmRequisitionlineV parentLine = Line.getEscmParentlineno();
      String CancelReason = selectedRow.getString("escmCancelReason");
      if (CancelReason.equals("null"))
        CancelReason = "";
      if (!LineNo.equals(""))
        LineNo = LineNo + "," + selectedRow.getString("lineNo");
      else
        LineNo = selectedRow.getString("lineNo");
      st1 = OBDal.getInstance().getConnection().prepareStatement(
          "update m_requisitionline set em_escm_status ='ESCM_CA',em_escm_cancel_reason='"
              + CancelReason + "',em_escm_cancel_date='" + currentDate + "',em_escm_cancelledby='"
              + user.getId() + "' " + "where m_requisitionline_id = '" + Line.getId() + "' ");
      st1.executeUpdate();
      if (parentLine == null) {
        sql = " (select replace(unnest(string_to_array(escm_getchildrequisitionline('"
            + Line.getId() + "'),',')),'''',''))";
        qry = OBDal.getInstance().getSession().createSQLQuery(sql);
        parentLinesize = qry.list().size();
        if (qry.list() != null && qry.list().size() > 0) {
          for (i = 0; i < parentLinesize; i++) {
            reqline = (String) qry.list().get(i);
            String alteredText = reqline.replaceAll("[-+.^:']", "");

            st = OBDal.getInstance().getConnection().prepareStatement(
                "select m_requisitionline_id as reqlineId from m_requisitionline where m_requisitionline_id = '"
                    + alteredText + "' ");
            rs = st.executeQuery();
            // qry1 = OBDal.getInstance().getSession().createSQLQuery(sql1);
            if (rs.next()) {
              String reqId = rs.getString("reqlineId");
              reqlineobj = OBDal.getInstance().get(RequisitionLine.class, reqId);
            }
            // reqlineobj = OBDal.getInstance().get(RequisitionLine.class, alteredText);
            if (reqlineobj != null) {
              String CancelReason1 = selectedRow.getString("escmCancelReason");
              if (CancelReason1.equals("null"))
                CancelReason1 = "";
              if (!LineNo.equals(""))
                LineNo = LineNo + "," + selectedRow.getString("lineNo");
              else
                LineNo = selectedRow.getString("lineNo");

              st1 = OBDal.getInstance().getConnection().prepareStatement(
                  "update m_requisitionline set em_escm_status ='ESCM_CA',em_escm_cancel_reason='"
                      + CancelReason1 + "',em_escm_cancel_date='" + currentDate
                      + "',em_escm_cancelledby='" + user.getId() + "' "
                      + "where m_requisitionline_id = '" + reqlineobj.getId() + "'");
              st1.executeUpdate();
              // reqlineobj.setEscmStatus("ESCM_CA");
              // reqlineobj.setEscmCancelReason(CancelReason);
              // reqlineobj.setEscmCancelDate(currentDate);
              // reqlineobj.setEscmCancelledby(user);
              // OBDal.getInstance().save(reqlineobj);
            }
          }

          return i + 1;
        }
      } else {
        PreparedStatement st4 = null;
        ArrayList<String> parentList = new ArrayList<String>();
        // String CancelReason1 = selectedRow.getString("escmCancelReason");
        if (CancelReason.equals("null"))
          CancelReason = "";
        if (!LineNo.equals(""))
          LineNo = LineNo + "," + selectedRow.getString("lineNo");
        else
          LineNo = selectedRow.getString("lineNo");
        st4 = OBDal.getInstance().getConnection().prepareStatement(
            "update m_requisitionline set em_escm_status ='ESCM_CA',em_escm_cancel_reason='"
                + CancelReason + "',em_escm_cancel_date='" + currentDate + "',em_escm_cancelledby='"
                + user.getId() + "' " + "where m_requisitionline_id = '" + Line.getId() + "' ");

        st4.executeUpdate();
        getParentLines(Line, parentList, LineNo, selectedRow, currentDate, user);
        // Cancel child lines also
        updateChildLines(Line, selectedRow, currentDate, user);
        getchildForParent(parentList, selectedRow, LineNo, currentDate, user);

      }
    } catch (Exception e) {
      log.error("Exception in RequisitionCancelHandler :", e);
    }
    return i + 1;
  }

  public static List<String> getParentLines(RequisitionLine reqlineobj,
      ArrayList<String> parentList, String LineNo, JSONObject selectedRow, Date currentDate,
      User user) {
    RequisitionLine parentReqLine = null;
    try {
      EscmRequisitionlineV parentLine = reqlineobj.getEscmParentlineno();

      if (parentLine != null) {
        String parentId = parentLine.getId();
        if (parentId != null) {
          parentList.add(parentId);
          parentReqLine = OBDal.getInstance().get(RequisitionLine.class, parentId);
          getParentLines(parentReqLine, parentList, LineNo, selectedRow, currentDate, user);
        }
      }
    } catch (Exception e) {
      log.error("Exception in RequisitionCancelHandler :", e);
    }
    return parentList;
  }

  public static void getchildForParent(List<String> parentList, JSONObject selectedRow,
      String lineNo, Date currentDate, User user) {
    String sql = null;
    Query qry = null;
    int parentLinesize = 0;
    PreparedStatement st = null;
    PreparedStatement st4 = null;
    ResultSet rs = null;
    RequisitionLine line = null;
    int i = 0;
    BigInteger countline = BigInteger.ZERO;
    String LineNo = lineNo;
    try {
      if (parentList.size() > 0) {
        for (String parentId : parentList) {
          RequisitionLine reqHeader = OBDal.getInstance().get(RequisitionLine.class, parentId);
          sql = " select count(m_requisitionline_id) as count from m_requisitionline where m_requisitionline_id IN "
              + " (select replace(unnest(string_to_array(escm_getchildrequisitionline('" + parentId
              + "'),',')) ,'''','')) "
              + " and em_escm_status NOT IN ('ESCM_CA' ) and  m_requisition_id ='"
              + reqHeader.getRequisition().getId() + "' ";
          qry = OBDal.getInstance().getSession().createSQLQuery(sql);
          parentLinesize = qry.list().size();
          if (parentLinesize > 0) {
            countline = (BigInteger) qry.list().get(i);
          }
          if (countline.compareTo(BigInteger.ZERO) == 0) {

            st = OBDal.getInstance().getConnection().prepareStatement(
                "select m_requisitionline_id as reqlineId from m_requisitionline where m_requisitionline_id = '"
                    + parentId + "' ");
            rs = st.executeQuery();
            // RequisitionLine line = OBDal.getInstance().get(RequisitionLine.class, parentId);
            if (rs.next()) {
              String reqId = rs.getString("reqlineId");
              line = OBDal.getInstance().get(RequisitionLine.class, reqId);
            }

            String CancelReason = selectedRow.getString("escmCancelReason");
            if (CancelReason.equals("null"))
              CancelReason = "";
            if (!LineNo.equals(""))
              LineNo = LineNo + "," + selectedRow.getString("lineNo");
            else
              LineNo = selectedRow.getString("lineNo");

            st4 = OBDal.getInstance().getConnection().prepareStatement(
                "update m_requisitionline set em_escm_status ='ESCM_CA',em_escm_cancel_reason='"
                    + CancelReason + "',em_escm_cancel_date='" + currentDate
                    + "',em_escm_cancelledby='" + user.getId() + "' "
                    + "where m_requisitionline_id = '" + line.getId() + "' ");
            st4.executeUpdate();
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception in RequisitionCancelHandler :", e);
    }

  }

  public static void updateChildLines(RequisitionLine reqline, JSONObject selectedRow,
      Date currentDate, User user) {
    String sql = null;
    String reqLineId = null;
    Query qry = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    PreparedStatement st1 = null;
    RequisitionLine reqLine = reqline;
    try {
      sql = " (select replace(unnest(string_to_array(escm_getchildrequisitionline('"
          + reqLine.getId() + "'),',')),'''',''))";
      qry = OBDal.getInstance().getSession().createSQLQuery(sql);
      if (qry.list() != null && qry.list().size() > 0) {
        for (Object reqObj : qry.list()) {
          reqLineId = (String) reqObj;
          String alteredText = reqLineId.replaceAll("[-+.^:']", "");

          st = OBDal.getInstance().getConnection().prepareStatement(
              "select m_requisitionline_id as reqlineId from m_requisitionline where m_requisitionline_id = '"
                  + alteredText + "' ");
          rs = st.executeQuery();
          if (rs.next()) {
            String reqId = rs.getString("reqlineId");
            reqLine = OBDal.getInstance().get(RequisitionLine.class, reqId);
          }
          if (reqLine != null) {
            String CancelReason = selectedRow.getString("escmCancelReason");
            if (CancelReason.equals("null"))
              CancelReason = "";

            st1 = OBDal.getInstance().getConnection().prepareStatement(
                "update m_requisitionline set em_escm_status ='ESCM_CA',em_escm_cancel_reason='"
                    + CancelReason + "',em_escm_cancel_date='" + currentDate
                    + "',em_escm_cancelledby='" + user.getId() + "' "
                    + "where m_requisitionline_id = '" + reqLine.getId() + "'");
            st1.executeUpdate();
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in RequisitionCancelHandlerDAO in updateChildLines() :", e);
    }
  }
}
