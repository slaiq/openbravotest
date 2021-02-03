package sa.elm.ob.utility.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.utility.EUT_OrclConnection;

public class ConnectionUtility {
  private static final String ORACLE_JDBC_DRIVER_CLASS = "oracle.jdbc.driver.OracleDriver";
  private static final Logger log4j = Logger.getLogger(ConnectionUtility.class);
  public static final String HR_CONNECTION_ID = "EUT_Orcl_Connection_ID";

  public static JSONObject getConnection(String strURL, String strUsername, String strPwd) {
    Connection connection = null;
    JSONObject jsonObject = new JSONObject();

    try {
      Class.forName(ORACLE_JDBC_DRIVER_CLASS);
      log4j.debug("Connecting to database...");
      connection = DriverManager.getConnection(strURL, strUsername, strPwd);
      if (connection != null) {
        jsonObject.put("Connection", connection);
      }
    } catch (Exception e) {
      connection = null;
      try {
        jsonObject.put("message", e.getMessage());
      } catch (JSONException e1) {
      }
      log4j.error("Exception while establishing connection:", e);
    }
    return jsonObject;
  }

  public static JSONObject testConnection(String strURL, String strUsername, String strPwd,
      boolean close) {
    Connection connection = null;
    Boolean isValid = Boolean.FALSE;
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject = getConnection(strURL, strUsername, strPwd);

      if (jsonObject.has("Connection")) {
        connection = (Connection) jsonObject.get("Connection");

        isValid = connection.isValid(3);
        jsonObject.put("isValid", isValid);
      } else {
        jsonObject.put("isValid", isValid);
      }
    } catch (Exception e) {
      try {
        jsonObject.put("isValid", Boolean.FALSE);
        jsonObject.put("message", e.getMessage());
      } catch (JSONException e1) {
      }
      log4j.error("Exception while getting connection:", e);
    } finally {
      if (connection != null && close)
        try {
          connection.close();
        } catch (SQLException e) {
        }
    }
    return jsonObject;
  }

  public static JSONObject getConnectionObject() {
    JSONObject connectionObject = new JSONObject();
    EUT_OrclConnection connection = null;
    try {
      OBContext.setAdminMode();

      OBQuery<EUT_OrclConnection> connectionQuery = OBDal.getInstance()
          .createQuery(EUT_OrclConnection.class, " order by creationDate desc");
      connectionQuery.setMaxResult(1);

      if (connectionQuery != null && connectionQuery.list().size() > 0) {
        connection = connectionQuery.list().get(0);
        connectionObject.put("result", "S");
        connectionObject.put("Connection", connection);
      } else {
        connectionObject.put("result", "E");
        connectionObject.put("Message", "No Connection Available");
      }
    } catch (Exception e) {
      log4j.error("Exception while getConnectionObject: ", e);
      try {
        connectionObject.put("result", "E");
        connectionObject.put("Message", e.getMessage());
      } catch (JSONException e1) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return connectionObject;
  }

  public static Connection getHrConnection(String connectionId)
      throws ClassNotFoundException, SQLException {
    EUT_OrclConnection oracleConnectionInfo = getHRConnectionDetails(connectionId);
    Connection connection = null;
    if (oracleConnectionInfo != null) {
      Class.forName(ORACLE_JDBC_DRIVER_CLASS);
      log4j.debug("Connecting to database...");
      connection = DriverManager.getConnection(oracleConnectionInfo.getConnectionUrl(),
          oracleConnectionInfo.getUsername(), oracleConnectionInfo.getPassword());
    }
    return connection;
  }

  private static EUT_OrclConnection getHRConnectionDetails(String connectionId) {
    log4j.info("Connection Id from proceess parameters = :" + connectionId);
    EUT_OrclConnection oracleConnectionInfo = null;
    if (StringUtils.isEmpty(connectionId)) {
      log4j.info("HR Connection id in process parameters is empty, retreiving from database.");
      OBQuery<EUT_OrclConnection> connectionQuery = OBDal.getInstance()
          .createQuery(EUT_OrclConnection.class, " order by creationDate desc");
      connectionQuery.setMaxResult(1);
      if (connectionQuery != null && connectionQuery.list().size() > 0) {
        oracleConnectionInfo = connectionQuery.list().get(0);
        log4j.info(
            "HR database Connection Name to be used :" + oracleConnectionInfo.getConnectionName());
      } else {
        log4j.error("Can't get Connection Information from database");
      }
    } else {
      oracleConnectionInfo = Utility.getObject(EUT_OrclConnection.class, connectionId);
    }
    return oracleConnectionInfo;
  }
}