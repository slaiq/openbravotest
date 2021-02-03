package sa.elm.ob.utility.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.db.QueryTimeOutUtil;

public class TreeDatasourceServiceData implements FieldProvider {
  static Logger log4j = Logger.getLogger(TreeDatasourceServiceData.class);
  private String InitRecordNumber = "0";
  public String name;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("name"))
      return name;
    else {
      log4j.debug("Field does not exist: " + fieldName);
      return null;
    }
  }

  public static TreeDatasourceServiceData[] select(ConnectionProvider connectionProvider)
      throws ServletException {
    return select(connectionProvider, 0, 0);
  }

  public static TreeDatasourceServiceData[] select(ConnectionProvider connectionProvider,
      int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql + "        select 1 as name from dual";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());

      result = st.executeQuery();
      long countRecord = 0;
      long countRecordSkip = 1;
      boolean continueResult = true;
      while (countRecordSkip < firstRegister && continueResult) {
        continueResult = result.next();
        countRecordSkip++;
      }
      while (continueResult && result.next()) {
        countRecord++;
        TreeDatasourceServiceData objectTreeDatasourceServiceData = new TreeDatasourceServiceData();
        objectTreeDatasourceServiceData.name = UtilSql.getValue(result, "name");
        objectTreeDatasourceServiceData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectTreeDatasourceServiceData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    TreeDatasourceServiceData objectTreeDatasourceServiceData[] = new TreeDatasourceServiceData[vector
        .size()];
    vector.copyInto(objectTreeDatasourceServiceData);
    return (objectTreeDatasourceServiceData);
  }

  public static int reparentChildrenADTree(ConnectionProvider connectionProvider,
      String newParentId, String adTreeId, String oldParentId) throws ServletException {
    String strSql = "";
    strSql = strSql + "      UPDATE ad_treenode set parent_id = ?" + "      WHERE ad_tree_id = ?"
        + "      AND parent_id= ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, newParentId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, adTreeId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, oldParentId);

      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (updateCount);
  }

  public static int reparentChildrenLinkToParent(ConnectionProvider connectionProvider,
      String tableName, String parentColumn, String newParentId, String oldParentId)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "      UPDATE ";
    strSql = strSql + ((tableName == null || tableName.equals("")) ? "" : tableName);
    strSql = strSql + " set ";
    strSql = strSql + ((parentColumn == null || parentColumn.equals("")) ? "" : parentColumn);
    strSql = strSql + " = ?" + "      WHERE ";
    strSql = strSql + ((parentColumn == null || parentColumn.equals("")) ? "" : parentColumn);
    strSql = strSql + " = ?";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      if (tableName != null && !(tableName.equals(""))) {
      }
      if (parentColumn != null && !(parentColumn.equals(""))) {
      }
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, newParentId);
      if (parentColumn != null && !(parentColumn.equals(""))) {
      }
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, oldParentId);

      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (updateCount);
  }

  public static int deleteChilds(ConnectionProvider connectionProvider, String tableName,
      String columnName, String id) throws ServletException {
    String strSql = "";

    strSql = strSql + "      DELETE FROM ";
    strSql = strSql + ((tableName == null || tableName.equals("")) ? "" : tableName);

    strSql = strSql + "    WHERE ";
    strSql = strSql + ((columnName == null || columnName.equals("")) ? "" : columnName);
    strSql = strSql + " in (" + id + ")";

    int updateCount = 0;
    PreparedStatement st = null;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (updateCount);
  }

  public static int updateParentInChilds(ConnectionProvider connectionProvider, String tableName,
      String parentColumn, String idColumn, String oldParentId) throws ServletException {
    String strSql = "";
    strSql = strSql + "      UPDATE ";
    strSql = strSql + ((tableName == null || tableName.equals("")) ? "" : tableName);
    strSql = strSql + " set ";
    strSql = strSql + ((parentColumn == null || parentColumn.equals("")) ? "" : parentColumn);
    strSql = strSql + " = null" + "      WHERE ";
    strSql = strSql + ((idColumn == null || idColumn.equals("")) ? "" : idColumn);
    strSql = strSql + " in (" + oldParentId + ")";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (updateCount);
  }

}
