//Sqlc generated V1.O00-1
package sa.elm.ob.utility.ad_forms.auditTrail;

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

/**
 * This is a runtime accessible copy of some methods normally used only inside WAD
 */
class AuditTrailPopupData implements FieldProvider {
  static Logger log4j = Logger.getLogger(AuditTrailPopupData.class);
  private String InitRecordNumber = "0";
  public String adTabId;
  public String name;
  public String reference;
  public String referencevalue;
  public String tablename;
  public String parentTabName;
  public String tablemodule;
  public String columnmodule;
  public String tabid;
  public String seqno;
  public String tablevel;
  public String tabname;
  public String tabnamecompact;
  public String tdClass;
  public String tabnametrl;
  public String key;
  public String href;
  public String tdHeight;
  public String parentKey;
  public String isinfotab;
  public String istranslationtab;
  public String nametab;
  public String editreference;
  public String tabmodule;
  public String adFieldId;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("ad_tab_id") || fieldName.equals("adTabId"))
      return adTabId;
    else if (fieldName.equals("name"))
      return name;
    else if (fieldName.equals("reference"))
      return reference;
    else if (fieldName.equals("referencevalue"))
      return referencevalue;
    else if (fieldName.equals("tablename"))
      return tablename;
    else if (fieldName.equals("parentTabName"))
      return parentTabName;
    else if (fieldName.equals("tablemodule"))
      return tablemodule;
    else if (fieldName.equals("columnmodule"))
      return columnmodule;
    else if (fieldName.equals("tabid"))
      return tabid;
    else if (fieldName.equals("seqno"))
      return seqno;
    else if (fieldName.equals("tablevel"))
      return tablevel;
    else if (fieldName.equals("tabname"))
      return tabname;
    else if (fieldName.equals("tabnamecompact"))
      return tabnamecompact;
    else if (fieldName.equals("tdClass"))
      return tdClass;
    else if (fieldName.equals("tabnametrl"))
      return tabnametrl;
    else if (fieldName.equals("key"))
      return key;
    else if (fieldName.equals("href"))
      return href;
    else if (fieldName.equals("tdHeight"))
      return tdHeight;
    else if (fieldName.equals("parentKey"))
      return parentKey;
    else if (fieldName.equals("isinfotab"))
      return isinfotab;
    else if (fieldName.equals("istranslationtab"))
      return istranslationtab;
    else if (fieldName.equals("nametab"))
      return nametab;
    else if (fieldName.equals("editreference"))
      return editreference;
    else if (fieldName.equals("tabmodule"))
      return tabmodule;
    else if (fieldName.equals("adFieldId"))
      return adFieldId;
    else {
      log4j.debug("Field does not exist: " + fieldName);
      return null;
    }
  }

  public static String selectParentTab(ConnectionProvider connectionProvider, String adTabId)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "        select t.ad_tab_id" + "         from ad_tab t, ad_tab t1"
        + "        where t1.ad_window_id = t.ad_window_id" + "          and t1.ad_tab_id = ?"
        + "          and t.seqno < t1.seqno" + "          and t.tablevel < t1.tablevel"
        + "          and t.seqno = (select max(t2.seqno)"
        + "                           from ad_tab t2, ad_tab t3"
        + "                          where t3.ad_window_id = t2.ad_window_id"
        + "                            and t3.ad_tab_id = t1.ad_tab_id"
        + "                            and t2.seqno < t3.seqno"
        + "                            and t2.tablevel < t3.tablevel) ";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, adTabId);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "ad_tab_id");
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
    return (strReturn);
  }

  /**
   * Name of the columns parent of the tab
   */
  public static AuditTrailPopupData[] parentsColumnName(ConnectionProvider connectionProvider,
      String parentTab, String tab) throws ServletException {
    return parentsColumnName(connectionProvider, parentTab, tab, 0, 0);
  }

  /**
   * Name of the columns parent of the tab
   */
  public static AuditTrailPopupData[] parentsColumnName(ConnectionProvider connectionProvider,
      String parentTab, String tab, int firstRegister, int numberRegisters)
      throws ServletException {
    String strSql = "";
    strSql = strSql
        + "        SELECT AD_FIELD.AD_FIELD_ID, ColumnName AS NAME, AD_REFERENCE_id AS reference, ad_reference_value_id AS referencevalue,"
        + "        (SELECT tableNAME FROM AD_TABLE, AD_TAB WHERE AD_TABLE.ad_table_id = AD_TAB.ad_table_id"
        + "        AND AD_TAB.ad_tab_id=?) AS tablename, ? as AD_Tab_ID, (select name from ad_tab where ad_tab_id = ?) as parent_tab_name,"
        + "        (SELECT P.ad_module_id FROM AD_TABLE T, AD_PACKAGE P WHERE T.ad_table_id = AD_COLUMN.ad_table_id AND T.AD_PACKAGE_ID = P.AD_PACKAGE_ID) as tableModule,"
        + "        AD_COLUMN.AD_Module_ID as columnModule" + "        FROM AD_FIELD, AD_COLUMN "
        + "        WHERE AD_FIELD.ad_column_id = AD_COLUMN.ad_column_id AND ad_tab_id = ? AND isParent='Y' "
        + "        AND EXISTS(SELECT 1 FROM AD_COLUMN c, AD_FIELD f WHERE c.ad_column_id = f.ad_column_id AND (c.iskey='Y' OR c.issecondarykey='Y')"
        + "        AND ad_tab_id=? AND UPPER(c.columnname) = UPPER(AD_COLUMN.columnname))";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, tab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);

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
        AuditTrailPopupData objectAuditTrailPopupData = new AuditTrailPopupData();
        objectAuditTrailPopupData.adFieldId = UtilSql.getValue(result, "ad_field_id");
        objectAuditTrailPopupData.name = UtilSql.getValue(result, "name");
        objectAuditTrailPopupData.reference = UtilSql.getValue(result, "reference");
        objectAuditTrailPopupData.referencevalue = UtilSql.getValue(result, "referencevalue");
        objectAuditTrailPopupData.tablename = UtilSql.getValue(result, "tablename");
        objectAuditTrailPopupData.adTabId = UtilSql.getValue(result, "ad_tab_id");
        objectAuditTrailPopupData.parentTabName = UtilSql.getValue(result, "parent_tab_name");
        objectAuditTrailPopupData.tablemodule = UtilSql.getValue(result, "tablemodule");
        objectAuditTrailPopupData.columnmodule = UtilSql.getValue(result, "columnmodule");
        objectAuditTrailPopupData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectAuditTrailPopupData);
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
    AuditTrailPopupData objectAuditTrailPopupData[] = new AuditTrailPopupData[vector.size()];
    vector.copyInto(objectAuditTrailPopupData);
    return (objectAuditTrailPopupData);
  }

  /**
   * Name of the columns parent of the tab
   */
  public static AuditTrailPopupData[] parentsColumnReal(ConnectionProvider connectionProvider,
      String parentTab, String tab) throws ServletException {
    return parentsColumnReal(connectionProvider, parentTab, tab, 0, 0);
  }

  /**
   * Name of the columns parent of the tab
   */
  public static AuditTrailPopupData[] parentsColumnReal(ConnectionProvider connectionProvider,
      String parentTab, String tab, int firstRegister, int numberRegisters)
      throws ServletException {
    String strSql = "";
    strSql = strSql
        + "        SELECT AD_FIELD.AD_FIELD_ID, ColumnName AS NAME, AD_REFERENCE_id AS reference, ad_reference_value_id AS referencevalue,"
        + "        (SELECT tableNAME FROM AD_TABLE, AD_TAB WHERE AD_TABLE.ad_table_id = AD_TAB.ad_table_id"
        + "        AND AD_TAB.ad_tab_id=?) AS tablename,"
        + "        (SELECT P.ad_module_id FROM AD_TABLE T, AD_PACKAGE P WHERE T.ad_table_id = AD_COLUMN.ad_table_id AND T.AD_PACKAGE_ID = P.AD_PACKAGE_ID) as tableModule,"
        + "        AD_COLUMN.AD_Module_ID as columnModule" + "        FROM AD_FIELD, AD_COLUMN "
        + "        WHERE AD_FIELD.ad_column_id = AD_COLUMN.ad_column_id AND ad_tab_id = ?"
        + "        AND (UPPER(columnname) IN (SELECT UPPER(columnname) "
        + "                                    FROM AD_FIELD, AD_COLUMN "
        + "                                   WHERE AD_FIELD.ad_column_id = AD_COLUMN.ad_column_id "
        + "                                     AND AD_COLUMN.iskey='Y' "
        + "                                     AND AD_FIELD.ad_tab_id=?)"
        + "            OR (UPPER(columnname) LIKE 'EM_%'  "
        + "               AND UPPER(SUBSTR(COLUMNNAME,4)) IN  (SELECT UPPER(columnname) "
        + "                                    FROM AD_FIELD, AD_COLUMN "
        + "                                   WHERE AD_FIELD.ad_column_id = AD_COLUMN.ad_column_id "
        + "                                     AND AD_COLUMN.iskey='Y' "
        + "                                     AND AD_FIELD.ad_tab_id=?)))";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, tab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentTab);

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
        AuditTrailPopupData objectAuditTrailPopupData = new AuditTrailPopupData();
        objectAuditTrailPopupData.adFieldId = UtilSql.getValue(result, "ad_field_id");
        objectAuditTrailPopupData.name = UtilSql.getValue(result, "name");
        objectAuditTrailPopupData.reference = UtilSql.getValue(result, "reference");
        objectAuditTrailPopupData.referencevalue = UtilSql.getValue(result, "referencevalue");
        objectAuditTrailPopupData.tablename = UtilSql.getValue(result, "tablename");
        objectAuditTrailPopupData.tablemodule = UtilSql.getValue(result, "tablemodule");
        objectAuditTrailPopupData.columnmodule = UtilSql.getValue(result, "columnmodule");
        objectAuditTrailPopupData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectAuditTrailPopupData);
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
    AuditTrailPopupData objectAuditTrailPopupData[] = new AuditTrailPopupData[vector.size()];
    vector.copyInto(objectAuditTrailPopupData);
    return (objectAuditTrailPopupData);
  }

  /**
   * Subtabs of the tab of the parameter
   */
  public static AuditTrailPopupData[] selectSubtabs(ConnectionProvider connectionProvider,
      String parentId) throws ServletException {
    return selectSubtabs(connectionProvider, parentId, 0, 0);
  }

  /**
   * Subtabs of the tab of the parameter
   */
  public static AuditTrailPopupData[] selectSubtabs(ConnectionProvider connectionProvider,
      String parentId, int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "        SELECT a2.ad_tab_id AS tabId, a2.seqno AS seqNo, a2.tablevel AS tabLevel, a2.NAME AS tabName, a2.NAME AS tabNameCompact, '' AS TD_Class, a2.NAME AS tabNameTrl, "
        + "        (SELECT MAX(AD_COLUMN.columnname) " + "        FROM AD_TABLE, AD_COLUMN "
        + "        WHERE AD_TABLE.ad_table_id = AD_COLUMN.ad_table_id " + "        AND iskey='Y' "
        + "        AND AD_TABLE.ad_table_id = a2.ad_table_id) AS KEY, '' AS href, '' AS Td_Height, "
        + "        COALESCE(a1.ad_tab_id,'-1') AS Parent_Key, a2.isInfoTab, a2.isTranslationTab, AD_ShortName(a2.Name) as NameTab, "
        + "        a2.EDITREFERENCE, a2.AD_MODULE_ID as tabmodule"
        + "        FROM AD_TAB a1, AD_TAB a2" + "        WHERE a1.ad_tab_id = ?"
        + "        AND a1.ad_window_id = a2.ad_window_id" + "        AND a2.seqno > a1.seqno"
        + "        AND a2.isactive = 'Y'"
        + "        AND a2.seqno < (SELECT COALESCE(MIN(a3.seqno),1000)"
        + "                     FROM AD_TAB a3 "
        + "                     WHERE a3.ad_window_id = a1.ad_window_id "
        + "                     AND a3.tablevel = a1.tablevel"
        + "                     AND a3.seqno > a1.seqno)"
        + "        AND a2.tablevel = (a1.tablevel + 1)" + "        AND a2.ad_table_id IN "
        + "        (SELECT AD_TABLE.ad_table_id FROM AD_COLUMN, AD_TABLE "
        + "        WHERE AD_COLUMN.ad_table_id = AD_TABLE.ad_table_id"
        + "        AND (AD_COLUMN.iskey='Y' OR AD_COLUMN.issecondarykey='Y')"
        + "        AND (isparent='N' OR NOT EXISTS ("
        + "        SELECT c.ad_column_id FROM AD_TAB at1, AD_FIELD f, AD_COLUMN c"
        + "        WHERE at1.ad_table_id = c.ad_table_id"
        + "        AND at1.ad_window_id = a2.ad_window_id"
        + "        AND f.ad_column_id = c.ad_column_id" + "        AND at1.tablevel=a2.tablevel -1"
        + "        AND at1.ad_tab_id=COALESCE(a1.ad_tab_id,'-1')"
        + "        AND c.columnname = AD_COLUMN.columnname "
        + "        AND (c.isKey = 'Y' OR c.isSecondaryKey='Y')" + "        ))" + "        )"
        + "        ORDER BY a2.seqno";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, parentId);

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
        AuditTrailPopupData objectAuditTrailPopupData = new AuditTrailPopupData();
        objectAuditTrailPopupData.tabid = UtilSql.getValue(result, "tabid");
        objectAuditTrailPopupData.seqno = UtilSql.getValue(result, "seqno");
        objectAuditTrailPopupData.tablevel = UtilSql.getValue(result, "tablevel");
        objectAuditTrailPopupData.tabname = UtilSql.getValue(result, "tabname");
        objectAuditTrailPopupData.tabnamecompact = UtilSql.getValue(result, "tabnamecompact");
        objectAuditTrailPopupData.tdClass = UtilSql.getValue(result, "td_class");
        objectAuditTrailPopupData.tabnametrl = UtilSql.getValue(result, "tabnametrl");
        objectAuditTrailPopupData.key = UtilSql.getValue(result, "key");
        objectAuditTrailPopupData.href = UtilSql.getValue(result, "href");
        objectAuditTrailPopupData.tdHeight = UtilSql.getValue(result, "td_height");
        objectAuditTrailPopupData.parentKey = UtilSql.getValue(result, "parent_key");
        objectAuditTrailPopupData.isinfotab = UtilSql.getValue(result, "isinfotab");
        objectAuditTrailPopupData.istranslationtab = UtilSql.getValue(result, "istranslationtab");
        objectAuditTrailPopupData.nametab = UtilSql.getValue(result, "nametab");
        objectAuditTrailPopupData.editreference = UtilSql.getValue(result, "editreference");
        objectAuditTrailPopupData.tabmodule = UtilSql.getValue(result, "tabmodule");
        objectAuditTrailPopupData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectAuditTrailPopupData);
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
    AuditTrailPopupData objectAuditTrailPopupData[] = new AuditTrailPopupData[vector.size()];
    vector.copyInto(objectAuditTrailPopupData);
    return (objectAuditTrailPopupData);
  }
}
