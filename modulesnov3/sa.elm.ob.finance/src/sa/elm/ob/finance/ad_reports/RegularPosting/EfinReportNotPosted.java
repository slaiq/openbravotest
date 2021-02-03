/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package sa.elm.ob.finance.ad_reports.RegularPosting;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.DocAmortization;
import org.openbravo.erpCommon.ad_forms.DocBank;
import org.openbravo.erpCommon.ad_forms.DocCash;
import org.openbravo.erpCommon.ad_forms.DocDPManagement;
import org.openbravo.erpCommon.ad_forms.DocFINFinAccTransaction;
import org.openbravo.erpCommon.ad_forms.DocFINPayment;
import org.openbravo.erpCommon.ad_forms.DocFINReconciliation;
import org.openbravo.erpCommon.ad_forms.DocGLJournal;
import org.openbravo.erpCommon.ad_forms.DocInOut;
import org.openbravo.erpCommon.ad_forms.DocInternalConsumption;
import org.openbravo.erpCommon.ad_forms.DocInventory;
import org.openbravo.erpCommon.ad_forms.DocInvoice;
import org.openbravo.erpCommon.ad_forms.DocMatchInv;
import org.openbravo.erpCommon.ad_forms.DocMovement;
import org.openbravo.erpCommon.ad_forms.DocOrder;
import org.openbravo.erpCommon.ad_forms.DocPayment;
import org.openbravo.erpCommon.ad_forms.DocProduction;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.finance.properties.Resource;

public class EfinReportNotPosted extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  public int errors = 0;
  public int success = 0;

  // static Category log4j = Category.getInstance(EfinReportNotPosted.class);

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strFromPeriodId = vars.getGlobalVariable("inpcFromPeriodId",
          "EfinReportNotPosted|DateFrom", "");
      String strToPeriodId = vars.getGlobalVariable("inpcToPeriodId", "EfinReportNotPosted|DateTo",
          "");
      String strDocTypeId = vars.getGlobalVariable("inpcDocTypeId",
          "EfinReportNotPosted|inpcDocTypeId", "");
      String currentPeriodId = getCurrentPeriod(vars);
      String strStartDate = getFromDate(strFromPeriodId);
      String strEndDate = getEndDate(strToPeriodId);
      printPageDataSheet(response, vars, strFromPeriodId, strToPeriodId, strDocTypeId, strStartDate,
          strEndDate, currentPeriodId, "");
    } else if (vars.commandIn("FIND")) {
      String strFromPeriodId = vars.getGlobalVariable("inpcFromPeriodId",
          "EfinReportNotPosted|DateFrom", "");
      String strToPeriodId = vars.getGlobalVariable("inpcToPeriodId", "EfinReportNotPosted|DateTo",
          "");
      String strDocTypeId = vars.getGlobalVariable("inpcDocTypeId",
          "EfinReportNotPosted|inpcDocTypeId", "");
      String strStartDate = getFromDate(strFromPeriodId);
      String strEndDate = getEndDate(strToPeriodId);
      printPageDataSheet(response, vars, strFromPeriodId, strToPeriodId, strDocTypeId, strStartDate,
          strEndDate, "", "");
    } else if (vars.commandIn("PROCESS")) {
      String strSelectedData = vars.getGlobalVariable("inpSelectedData",
          "EfinReportNotPosted|inpSelectedData", "");
      String strDocTypeId = vars.getGlobalVariable("inpcDocTypeId",
          "EfinReportNotPosted|inpcDocTypeId", "");
      String strFromPeriodId = vars.getGlobalVariable("inpcFromPeriodId",
          "EfinReportNotPosted|DateFrom", "");
      String strToPeriodId = vars.getGlobalVariable("inpcToPeriodId", "EfinReportNotPosted|DateTo",
          "");
      String strStartDate = getFromDate(strFromPeriodId);
      String strEndDate = getEndDate(strToPeriodId);
      printPageDataSheet(response, vars, strFromPeriodId, strToPeriodId, strDocTypeId, strStartDate,
          strEndDate, "", strSelectedData);
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strFromPeriodId, String strToPeriodId, String strDocTypeId, String strStartDate,
      String strEndDate, String strCurrentPeriodId, String strSelectedData)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    EfinReportNotPostedData[] data = null;
    Connection con = null;
    // if (strDateFrom.equals("") && strDateTo.equals("")) {
    // xmlDocument =
    // xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_reports/EfinReportNotPosted",
    // discard).createXmlDocument();
    // data = EfinReportNotPostedData.set();
    // } else {
    xmlDocument = xmlEngine
        .readXmlTemplate("sa/elm/ob/finance/ad_reports/RegularPosting/EfinReportNotPosted")
        .createXmlDocument();
    if (vars.commandIn("PROCESS") || vars.commandIn("DEFAULT")) {
      // Processed or Default Action then empty the data
      data = new EfinReportNotPostedData[0];
    } else {
      data = EfinReportNotPostedData.select(this, vars.getLanguage(), vars.getClient(),
          strStartDate, strEndDate, strDocTypeId);
      // }// DateTimeData.nDaysAfter
    }

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "EfinReportNotPosted", false, "", "",
        "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();

    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "sa.elm.ob.finance.ad_reports.RegularPosting.EfinReportNotPosted");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "EfinReportNotPosted.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "EfinReportNotPosted.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("EfinReportNotPosted");
      vars.removeMessage("EfinReportNotPosted");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    if (vars.commandIn("FIND") && data.length == 0) {
      // No data has been found. Show warning message.
      xmlDocument.setParameter("messageType", "WARNING");
      xmlDocument.setParameter("messageTitle",
          Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()));
      xmlDocument.setParameter("messageMessage",
          Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("strFromPeriod",
        Resource.getProperty("finance.g/lreport.fromperiod", vars.getLanguage()));
    xmlDocument.setParameter("strToPeriod",
        Resource.getProperty("finance.g/lreport.toperiod", vars.getLanguage()));
    xmlDocument.setParameter("strGlCatgory",
        Resource.getProperty("finanace.G/LCategory", vars.getLanguage()));
    xmlDocument.setParameter("primaryfilter",
        Resource.getProperty("finance.PrimaryFilters", vars.getLanguage()));
    xmlDocument.setParameter("viewresult",
        Resource.getProperty("finance.viewresults", vars.getLanguage()));
    xmlDocument.setParameter("search", Resource.getProperty("finance.search", vars.getLanguage()));
    xmlDocument.setParameter("process",
        Resource.getProperty("finance.process", vars.getLanguage()));
    xmlDocument.setParameter("date", Resource.getProperty("finance.date", vars.getLanguage()));
    xmlDocument.setParameter("document",
        Resource.getProperty("finance.journalentries.Document", vars.getLanguage()));
    xmlDocument.setParameter("description",
        Resource.getProperty("finance.g/lreport.description", vars.getLanguage()));
    xmlDocument.setParameter("amount", Resource.getProperty("finance.amount", vars.getLanguage()));

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    if (vars.commandIn("FIND")) {
      xmlDocument.setParameter("cFromPeriodId", strFromPeriodId);
      xmlDocument.setParameter("cToPeriodId", strToPeriodId);
    } else {
      xmlDocument.setParameter("cFromPeriodId", strCurrentPeriodId);
      xmlDocument.setParameter("cToPeriodId", strCurrentPeriodId);
    }
    if (vars.commandIn("PROCESS")) {
      try {
        ConnectionProvider connectionProvider = new DalConnectionProvider(true);
        con = connectionProvider.getConnection();
        String TableId = getTableId(strDocTypeId);
        for (String recordId : strSelectedData.split(",")) {
          if (!recordId.equalsIgnoreCase("headercheck")) {
            AcctServer tempServer = get(TableId, vars.getClient(), vars.getOrg(), recordId,
                strDocTypeId, connectionProvider);
            boolean postSuccess = false;
            postSuccess = tempServer.post(recordId, false, vars, connectionProvider, con);
            errors = errors + tempServer.errors;
            // success =+ success + tempServer.success;

            if (!postSuccess) {
              // Process Failed. Show Error message.
              xmlDocument.setParameter("messageType", "ERROR");
              xmlDocument.setParameter("messageTitle",
                  Utility.messageBD(this, "ProcessStatus-E", vars.getLanguage()));
              xmlDocument.setParameter("messageMessage",
                  Utility.messageBD(this, "ProcessFailed", vars.getLanguage()));
              connectionProvider.releaseRollbackConnection(con);
              return;
            } else {
              // Process Success. Show Success message.
              xmlDocument.setParameter("messageType", "SUCCESS");
              xmlDocument.setParameter("messageTitle",
                  Utility.messageBD(this, "EFIN_Process Status-S", vars.getLanguage()));
              xmlDocument.setParameter("messageMessage",
                  Utility.messageBD(this, "ProcessOK", vars.getLanguage()));

            }
          }
        }
        connectionProvider.releaseCommitConnection(con);
        OBDal.getInstance().commitAndClose();
      } catch (Exception e) {
        log4j.debug("error in process action of EfinReportNotPosted", e);
      }

    }
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("cDocTypeId", strDocTypeId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "EFIN_PERIOD_ID",
          "56AF1E52897E4288A101AAC48F3629ED", "",
          Utility.getContext(this, vars, "#User_Org", "EfinReportNotPosted"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportNotPosted"), '*');
      if (vars.commandIn("FIND") || vars.commandIn("DEFAULT")) {
        comboTableData.fillParameters(null, "EfinReportNotPosted", strFromPeriodId);
      } else {
        comboTableData.fillParameters(null, "EfinReportNotPosted", strCurrentPeriodId);
      }
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure", comboTableData.select(true));

    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "EFIN_PERIOD_ID",
          "56AF1E52897E4288A101AAC48F3629ED", "",
          Utility.getContext(this, vars, "#User_Org", "EfinReportNotPosted"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportNotPosted"), '*');
      if (vars.commandIn("FIND") || vars.commandIn("DEFAULT")) {
        comboTableData.fillParameters(null, "EfinReportNotPosted", strToPeriodId);
      } else {
        comboTableData.fillParameters(null, "EfinReportNotPosted", strCurrentPeriodId);
      }
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure", comboTableData.select(true));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "GL_CATEGORY_ID",
          "", "", Utility.getContext(this, vars, "#User_Org", "EfinReportNotPosted"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportNotPosted"), '*');
      comboTableData.fillParameters(null, "EfinReportNotPosted", "");
      xmlDocument.setData("reportC_DOCTYPE_ID", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    if (vars.commandIn("PROCESS") || vars.commandIn("DEFAULT")) {
      // Processed then empty the data
      data = new EfinReportNotPostedData[0];
      xmlDocument.setData("structure1", data);
    } else {
      xmlDocument.setData("structure1", data);
    }
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet EfinReportNotPosted. This Servlet was made by Qualian";
  } // end of the getServletInfo() method

  /**
   * 
   * @param FromPeriodID
   * @return StartingDate
   */
  public String getFromDate(String FromPeriodID) {
    String StrDate = "";
    try {
      Period objPeriod = OBDal.getInstance().get(Period.class, FromPeriodID);
      if (objPeriod != null) {
        StrDate = new SimpleDateFormat("dd-MM-yyyy").format(objPeriod.getStartingDate());
      }
    } catch (Exception e) {
      log4j.debug("error in Regular Posting get StartDate:", e);
    }
    return StrDate;
  }

  /**
   * 
   * 
   * @param ToPeriodID
   * @return End date
   */
  public String getEndDate(String ToPeriodID) {
    String StrDate = "";
    try {
      Period objPeriod = OBDal.getInstance().get(Period.class, ToPeriodID);
      if (objPeriod != null) {
        StrDate = new SimpleDateFormat("dd-MM-yyyy").format(objPeriod.getEndingDate());
      }
    } catch (Exception e) {
      log4j.debug("error in Regular Posting getEnd Date:", e);
    }
    return StrDate;
  }

  /**
   * 
   * @param vars
   * @return CurrentPeriod
   */
  public String getCurrentPeriod(VariablesSecureApp vars) {
    String CurrentPeriodId = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn.prepareStatement(
          "select c_period_id from c_period where now() between startdate and enddate and ad_org_id='"
              + vars.getOrg() + "' and ad_client_id='" + vars.getClient() + "'");
      rs = ps.executeQuery();
      if (rs.next()) {
        CurrentPeriodId = rs.getString("c_period_id");
      }
    } catch (Exception e) {
    }
    return CurrentPeriodId;
  }

  /**
   * 
   * 
   * @param vars
   * @param AD_Client_ID
   * @return GroupLines
   */
  public String getGroupLines(String AD_Client_ID) {
    String GroupLines = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn
          .prepareStatement(" SELECT GROUPACCTINVLINES FROM AD_CLIENTINFO WHERE AD_CLIENT_ID = '"
              + AD_Client_ID + "'");
      rs = ps.executeQuery();
      if (rs.next()) {
        GroupLines = rs.getString("groupacctinvlines");
      }
    } catch (Exception e) {
      log4j.debug("error in GetGroupLines of EfinReportNotPosted", e);
    }
    return GroupLines;
  }

  /**
   * 
   * @param inpDocTypeId
   * @return tableId
   */
  public String getTableId(String inpDocTypeId) {
    String tableId = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn.prepareStatement(
          "select ad_table_id from c_doctype where gl_category_id ='" + inpDocTypeId + "' limit 1");
      rs = ps.executeQuery();
      if (rs.next()) {
        tableId = rs.getString("ad_table_id");
      }
    } catch (Exception e) {
      log4j.debug("error in getTableId of EfinReportNotPosted", e);
    }
    return tableId;
  }

  /**
   * Factory - Create Posting document
   * 
   * @param AD_Table_ID
   *          Table ID of Documents
   * @param AD_Client_ID
   *          Client ID of Documents
   * @param connectionProvider
   *          Database connection provider
   * @return Document
   */
  public AcctServer get(String AD_Table_ID, String AD_Client_ID, String AD_Org_ID, String Record_ID,
      String strDocTypeId, ConnectionProvider connectionProvider) throws ServletException {
    AcctServer acct = null;
    if (log4j.isDebugEnabled())
      log4j.debug("get - table: " + AD_Table_ID);
    if (AD_Table_ID.equals("D1A97202E832470285C9B1EB026D54E2")) {
      AD_Table_ID = "100";
    }
    if (AD_Table_ID.equals("4D8C3B3C31D1410DA046140C9F024D17")) {
      AD_Table_ID = "101";
    }
    if (AD_Table_ID.equals("B1B7075C46934F0A9FD4C4D0F1457B42")) {
      AD_Table_ID = "102";
    }
    if (AD_Table_ID.equals("318") || AD_Table_ID.equals("800060") || AD_Table_ID.equals("800176")
        || AD_Table_ID.equals("407") || AD_Table_ID.equals("392") || AD_Table_ID.equals("259")
        || AD_Table_ID.equals("800019") || AD_Table_ID.equals("319") || AD_Table_ID.equals("321")
        || AD_Table_ID.equals("323") || AD_Table_ID.equals("325") || AD_Table_ID.equals("224")
        || AD_Table_ID.equals("472") || AD_Table_ID.equals("800168") || AD_Table_ID.equals("100")
        || AD_Table_ID.equals("101") || AD_Table_ID.equals("102")) {
      switch (Integer.parseInt(AD_Table_ID)) {
      case 100:
        acct = new DocFINPayment(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "FIN_Payment";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "D1A97202E832470285C9B1EB026D54E2";
        acct.Record_ID = Record_ID;
        // acct.C_DocType_ID=strDocTypeId;
        acct.reloadAcctSchemaArray();
        break;
      case 101:
        acct = new DocFINFinAccTransaction(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "FIN_Finacc_Transaction";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "4D8C3B3C31D1410DA046140C9F024D17";
        acct.Record_ID = Record_ID;
        // acct.C_DocType_ID=strDocTypeId;
        acct.reloadAcctSchemaArray();
        break;
      case 102:
        acct = new DocFINReconciliation(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "FIN_Reconciliation";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "B1B7075C46934F0A9FD4C4D0F1457B42";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 318:
        acct = new DocInvoice(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Invoice";
        acct.AD_Table_ID = "318";
        acct.strDateColumn = "DateAcct";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        acct.groupLines = getGroupLines(AD_Client_ID);
        break;
      /*
       * case 390: acct = new DocAllocation (AD_Client_ID); acct.strDateColumn = "";
       * acct.AD_Table_ID = "390"; acct.reloadAcctSchemaArray(); acct.break;
       */
      case 800060:
        acct = new DocAmortization(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "A_Amortization";
        acct.AD_Table_ID = "800060";
        acct.strDateColumn = "DateAcct";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;

      case 800176:
        if (log4j.isDebugEnabled())
          log4j.debug("AcctServer - Get DPM");
        acct = new DocDPManagement(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_DP_Management";
        acct.AD_Table_ID = "800176";
        acct.strDateColumn = "DateAcct";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 407:
        acct = new DocCash(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Cash";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "407";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 392:
        acct = new DocBank(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Bankstatement";
        acct.strDateColumn = "StatementDate";
        acct.AD_Table_ID = "392";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 259:
        acct = new DocOrder(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Order";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "259";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 800019:
        acct = new DocPayment(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "C_Settlement";
        acct.strDateColumn = "Dateacct";
        acct.AD_Table_ID = "800019";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 319:
        acct = new DocInOut(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_InOut";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "319";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 321:
        acct = new DocInventory(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Inventory";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "321";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 323:
        acct = new DocMovement(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Movement";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "323";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 325:
        acct = new DocProduction(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Production";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "325";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 224:
        if (log4j.isDebugEnabled())
          log4j.debug("AcctServer - Before OBJECT CREATION");
        acct = new DocGLJournal(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "GL_Journal";
        acct.strDateColumn = "DateAcct";
        acct.AD_Table_ID = "224";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 472:
        acct = new DocMatchInv(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_MatchInv";
        acct.strDateColumn = "DateTrx";
        acct.AD_Table_ID = "472";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      case 800168:
        acct = new DocInternalConsumption(AD_Client_ID, AD_Org_ID, connectionProvider);
        acct.tableName = "M_Internal_Consumption";
        acct.strDateColumn = "MovementDate";
        acct.AD_Table_ID = "800168";
        acct.Record_ID = Record_ID;
        acct.reloadAcctSchemaArray();
        break;
      // case 473: acct = new
      // DocMatchPO (AD_Client_ID); acct.strDateColumn = "MovementDate";
      // acct.reloadAcctSchemaArray(); break; case DocProjectIssue.AD_TABLE_ID: acct = new
      // DocProjectIssue (AD_Client_ID); acct.strDateColumn = "MovementDate";
      // acct.reloadAcctSchemaArray(); break;

      }
    }

    if (acct == null)
      log4j.warn("AcctServer - get - Unknown AD_Table_ID=" + AD_Table_ID);
    else if (log4j.isDebugEnabled())
      log4j.debug("AcctServer - get - AcctSchemaArray length=" + (acct.m_as).length);
    if (log4j.isDebugEnabled())
      log4j.debug("AcctServer - get - AD_Table_ID=" + AD_Table_ID);
    return acct;
  } // get
}
