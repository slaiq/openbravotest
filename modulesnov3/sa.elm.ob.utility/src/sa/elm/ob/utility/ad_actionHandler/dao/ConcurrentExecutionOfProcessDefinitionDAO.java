package sa.elm.ob.utility.ad_actionHandler.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * @author Divya created on 20/06/2019
 */

public class ConcurrentExecutionOfProcessDefinitionDAO {

  private static Logger log4j = Logger.getLogger(ConcurrentExecutionOfProcessDefinitionDAO.class);

  /**
   * This method is used to insert Process monitor
   * 
   * @param conn
   * @param vars
   * @param parameters
   * @param status
   * @param params
   * @param starttime
   * @param description
   * @param id
   * @param processId
   * @return processMonitorId
   * @throws ServletException
   */
  public static String insertProcessMonitor(Connection conn, VariablesSecureApp vars,
      Map<String, Object> parameters, String status, String params, long starttime,
      String description, String id, String processId) throws ServletException {
    String strSql = "";
    String processMonitorId = null;
    strSql = strSql + "         INSERT INTO eut_processdef_monitor"
        + "        (eut_processdef_monitor_id,AD_Org_ID, AD_Client_ID, Isactive, Created, Createdby, Updated, UpdatedBy,"
        + "        obuiapp_process_id, description, AD_User_ID, start_time,status, params,recordid)"
        + "        VALUES (?, ?, ?,'Y', NOW(), ?, NOW(), ?, ?, ?, ?, ? ,?, ?,?)";
    PreparedStatement st = null;

    try {
      processMonitorId = SequenceIdData.getUUID();

      st = conn.prepareStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      st.setString(1, processMonitorId);
      st.setString(2, vars.getOrg());
      st.setString(3, vars.getClient());
      st.setString(4, vars.getUser());
      st.setString(5, vars.getUser());
      st.setString(6, processId);
      st.setString(7, description);
      st.setString(8, vars.getUser());
      st.setTimestamp(9, new java.sql.Timestamp(starttime));
      st.setString(10, status);
      st.setString(11, params);
      st.setString(12, id);
      st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return processMonitorId;
  }

  /**
   * This method is used to update process monitor
   * 
   * @param conn
   * @param vars
   * @param status
   * @param endtime
   * @param runtime
   * @param processmonitorId
   * @param log
   * @return processMonitorId
   * @throws ServletException
   */
  public static String updateProcessMonitor(Connection conn, VariablesSecureApp vars, String status,
      long endtime, String runtime, String processmonitorId, String log) throws ServletException {
    String strSql = "";
    String processMonitorId = null;
    strSql = strSql + "        update eut_processdef_monitor set  updated= now(), updatedby=? ,"
        + "    end_time=?,log=?,runtime=? ,status=? where  eut_processdef_monitor_id =?  ";
    PreparedStatement st = null;

    try {

      st = conn.prepareStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      st.setString(1, vars.getUser());
      st.setTimestamp(2, new java.sql.Timestamp(endtime));
      st.setString(3, log);
      st.setString(4, runtime);
      st.setString(5, status);
      st.setString(6, processmonitorId);
      st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return processMonitorId;
  }

  /**
   * This method is used to get duration
   * 
   * @param duration
   * @return duration
   */
  public static String getDuration(long duration) {

    final int milliseconds = (int) (duration % 1000);
    final int seconds = (int) ((duration / 1000) % 60);
    final int minutes = (int) ((duration / 60000) % 60);
    final int hours = (int) ((duration / 3600000) % 24);

    final String m = (milliseconds < 10 ? "00" : (milliseconds < 100 ? "0" : "")) + milliseconds;
    final String sec = (seconds < 10 ? "0" : "") + seconds;
    final String min = (minutes < 10 ? "0" : "") + minutes;
    final String hr = (hours < 10 ? "0" : "") + hours;

    return hr + ":" + min + ":" + sec + "." + m;
  }

  /**
   * This method is used to check Concurrent Execution
   * 
   * @param conn
   * @param id
   * @param processId
   * @return boolean
   * @throws ServletException
   */
  public static boolean checkConcurrentExecution(Connection conn, String id, String processId)
      throws ServletException {
    String strSql = "";
    boolean concurrentExecute = false;
    strSql = strSql
        + " select  count(eut_processdef_monitor_id) as count from  eut_processdef_monitor "
        + "  where obuiapp_process_id=? and recordid=? and (status='SCH'  "
        + " or (   created > now() - interval '2 s' ))  ";
    PreparedStatement st = null;
    ResultSet rs = null;

    try {

      st = conn.prepareStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      st.setString(1, processId);
      st.setString(2, id);
      rs = st.executeQuery();
      if (rs.next()) {
        if (rs.getInt("count") > 1) {
          concurrentExecute = true;
        }
      }
    } catch (SQLException e) {
      concurrentExecute = true;
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      concurrentExecute = true;
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection :", e);
      }
    }
    return concurrentExecute;
  }
}
