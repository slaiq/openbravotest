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
 * All portions are Copyright (C) 2001-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.ScrollableFieldProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.AccountingSchemaMiscData;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.AbstractScrollableFieldProviderFilter;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.LimitRowsScrollableFieldProviderFilter;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.finance.properties.Resource;
import sa.elm.ob.utility.util.UtilityDAO;

public class EfinReportGeneralLedger extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String tempDefault = "";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strInitRecord = vars.getSessionValue("EfinReportGeneralLedger.initRecordNumber");
      tempDefault = strInitRecord;
      String strcAcctSchemaId = vars.getGlobalVariable("inpcAcctSchemaId",
          "EfinReportGeneralLedger|cAcctSchemaId", "");
      String strcCampaignId = vars.getGlobalVariable("inpcCampaignId_IN",
          "EfinReportGeneralLedger|cCampaignId", "");
      String strFromPeriod = vars.getGlobalVariable("inpcFromPeriodId",
          "EfinReportGeneralLedger|cFromPeriodId", "");
      String strToPeriod = vars.getGlobalVariable("inpcToPeriodId",
          "EfinReportGeneralLedger|cToPeriodId", "");
      /*
       * String strDateFrom = vars.getGlobalVariable("inpDateFrom",
       * "EfinReportGeneralLedger|DateFrom", ""); String strDateTo =
       * vars.getGlobalVariable("inpDateTo", "EfinReportGeneralLedger|DateTo", "");
       */
      String currentPeriodId = getCurrentPeriod(vars,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
      String strDateFrom = getDate(vars, strFromPeriod, null);
      String strDateTo = getDate(vars, null, strToPeriod);
      String strPageNo = vars.getGlobalVariable("inpPageNo", "EfinReportGeneralLedger|PageNo", "1");
      String strAmtFrom = vars.getNumericGlobalVariable("inpAmtFrom",
          "EfinReportGeneralLedger|AmtFrom", "");
      String strAmtTo = vars.getNumericGlobalVariable("inpAmtTo", "EfinReportGeneralLedger|AmtTo",
          "");
      String strDocno = vars.getGlobalVariable("inpDocumentno", "EfinReportGeneralLedger|DocNo",
          "");
      String strAccSeq = vars.getGlobalVariable("inpAccountingSeq",
          "EfinReportGeneralLedger|AccSeq", "");
      // String strcelementvaluefrom =
      // vars.getGlobalVariable("inpcElementValueIdFrom","EfinReportGeneralLedger|C_ElementValue_IDFROM",
      // "");
      // String strcelementvalueto =
      // vars.getGlobalVariable("inpcElementValueIdTo","EfinReportGeneralLedger|C_ElementValue_IDTO",
      // "");
      String strcelementvaluefrom = vars.getGlobalVariable("inpcElementValueIdFrom",
          "EfinReportGeneralLedger|C_ElementValue_IDFROM", "");
      String strcelementvalueto = vars.getGlobalVariable("inpcElementValueIdTo",
          "EfinReportGeneralLedger|C_ElementValue_IDTO", "");
      String strcelementvaluefromdes = "", strcelementvaluetodes = "";
      if (!strcelementvaluefrom.equals(""))
        strcelementvaluefromdes = EfinReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvaluefrom);
      if (!strcelementvalueto.equals(""))
        strcelementvaluetodes = EfinReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvalueto);
      strcelementvaluefromdes = (strcelementvaluefromdes.equals("null")) ? ""
          : strcelementvaluefromdes;
      strcelementvaluetodes = (strcelementvaluetodes.equals("null")) ? "" : strcelementvaluetodes;
      vars.setSessionValue("inpElementValueIdFrom_DES", strcelementvaluefromdes);
      vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);
      String strOrg = vars.getGlobalVariable("inpOrg", "EfinReportGeneralLedger|Org", "0");
      String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
          "EfinReportGeneralLedger|cBpartnerId", "", IsIDFilter.instance);
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "EfinReportGeneralLedger|mProductId", "", IsIDFilter.instance);
      // String strcProjectId =
      // vars.getInGlobalVariable("inpcProjectId_IN","EfinReportGeneralLedger|cProjectId", "",
      // IsIDFilter.instance);
      String strcProjectId = vars.getGlobalVariable("inpcProjectId_IN",
          "EfinReportGeneralLedger|cProjectId", "");

      String strcSalesregionId = vars.getGlobalVariable("inpcSalesregionId_IN",
          "EfinReportGeneralLedger|cSalesregionId", "");
      // String strcCampaignId = vars.getRequestInGlobalVariable("inpcCampaignId_IN",
      // "EfinReportGeneralLedger|cCampaignId", IsIDFilter.instance);
      String strcActivityId = vars.getGlobalVariable("inpcActivityId_IN",
          "EfinReportGeneralLedger|cActivityId", "");
      String strUser1Id = vars.getGlobalVariable("inpcUser1Id_IN",
          "EfinReportGeneralLedger|user1Id", "");
      String strUser2Id = vars.getGlobalVariable("inpcUser2Id_IN",
          "EfinReportGeneralLedger|user2Id", "");
      String strGroupBy = vars.getGlobalVariable("inpGroupBy", "EfinReportGeneralLedger|GroupBy",
          "");
      String strShowOpenBalances = vars.getGlobalVariable("inpShowOpenBalances",
          "EfinReportGeneralLedger|showOpenBalances", "");
      printPageDataSheet(response, vars, strFromPeriod, strToPeriod, currentPeriodId, strDateFrom,
          strDateTo, strPageNo, strAmtFrom, strAmtTo, strDocno, strAccSeq, strcelementvaluefrom,
          strcelementvalueto, strOrg, strcBpartnerId, strmProductId, strcProjectId,
          strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strGroupBy,
          strcAcctSchemaId, strcelementvaluefromdes, strcelementvaluetodes, strShowOpenBalances,
          null);
    } else if (vars.commandIn("FIND") || vars.commandIn("FINDLINK")) {
      String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
          "EfinReportGeneralLedger|cAcctSchemaId");
      String strcCampaignId = vars.getGlobalVariable("inpcCampaignId_IN",
          "EfinReportGeneralLedger|cCampaignId", "");
      /*
       * String strDateFrom =
       * vars.getRequestGlobalVariable("inpDateFrom","EfinReportGeneralLedger|DateFrom"); String
       * strDateTo = vars.getRequestGlobalVariable("inpDateTo", "EfinReportGeneralLedger|DateTo");
       */
      String strFromPeriod = vars.getRequestGlobalVariable("inpcFromPeriodId",
          "EfinReportGeneralLedger|cFromPeriodId");
      String strToPeriod = vars.getRequestGlobalVariable("inpcToPeriodId",
          "EfinReportGeneralLedger|cToPeriodId");
      String strUniquecode = vars.getRequestGlobalVariable("inpUniqueCode",
          "EfinReportGeneralLedger|inpUniqueCode");
      String strDateFrom = getDate(vars, strFromPeriod, null);
      String strDateTo = getDate(vars, null, strToPeriod);
      String strPageNo = vars.getRequestGlobalVariable("inpPageNo",
          "EfinReportGeneralLedger|PageNo");
      String strAmtFrom = vars.getNumericParameter("inpAmtFrom");
      vars.setSessionValue("EfinReportGeneralLedger|AmtFrom", strAmtFrom);
      String strAmtTo = vars.getNumericParameter("inpAmtTo");
      vars.setSessionValue("EfinReportGeneralLedger|AmtTo", strAmtTo);
      /*
       * String strDocno = vars.getNumericParameter("inpDocumentno");
       * vars.setSessionValue("EfinReportGeneralLedger|DocNo", strDocno); String strAccSeq =
       * vars.getNumericParameter("inpAccountingSeq");
       * vars.setSessionValue("EfinReportGeneralLedger|AccSeq", strAccSeq);
       */
      String strDocno = vars.getRequestGlobalVariable("inpDocumentno",
          "EfinReportGeneralLedger|DocNo");
      String strAccSeq = vars.getRequestGlobalVariable("inpAccountingSeq",
          "EfinReportGeneralLedger|AccSeq");
      String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
          "EfinReportGeneralLedger|C_ElementValue_IDFROM");
      String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
          "EfinReportGeneralLedger|C_ElementValue_IDTO");
      String strcelementvaluefromdes = "", strcelementvaluetodes = "";
      if (!strcelementvaluefrom.equals(""))
        strcelementvaluefromdes = EfinReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvaluefrom);
      if (!strcelementvalueto.equals(""))
        strcelementvaluetodes = EfinReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvalueto);
      vars.setSessionValue("inpElementValueIdFrom_DES", strcelementvaluefromdes);
      vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);
      String strShowOpenBalances = vars.getRequestGlobalVariable("inpShowOpenBalances",
          "EfinReportGeneralLedger|showOpenBalances");
      String strOrg = vars.getGlobalVariable("inpOrg", "EfinReportGeneralLedger|Org", "0");
      log4j.debug("strorg:" + strOrg);
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "EfinReportGeneralLedger|cBpartnerId", IsIDFilter.instance);
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "EfinReportGeneralLedger|mProductId", IsIDFilter.instance);
      String strcProjectId = vars.getRequestInGlobalVariable("inpcProjectId_IN",
          "EfinReportGeneralLedger|cProjectId", IsIDFilter.instance);
      String strcSalesregionId = vars.getRequestInGlobalVariable("inpcSalesregionId_IN",
          "EfinReportGeneralLedger|cSalesregionId", IsIDFilter.instance);
      String strcActivityId = vars.getRequestInGlobalVariable("inpcActivityId_IN",
          "EfinReportGeneralLedger|cActivityId", IsIDFilter.instance);
      String strUser1Id = vars.getRequestInGlobalVariable("inpcUser1Id_IN",
          "EfinReportGeneralLedger|user1Id", IsIDFilter.instance);
      String strUser2Id = vars.getRequestInGlobalVariable("inpcUser2Id_IN",
          "EfinReportGeneralLedger|user2Id", IsIDFilter.instance);

      String strGroupBy = vars.getRequestGlobalVariable("inpGroupBy",
          "EfinReportGeneralLedger|GroupBy");
      log4j.debug("##################### DoPost - Find - strcBpartnerId= " + strcBpartnerId);
      log4j.debug(
          "##################### DoPost - XLS - strcelementvaluefrom= " + strcelementvaluefrom);
      log4j.debug("##################### DoPost - XLS - strcelementvalueto= " + strcelementvalueto);
      vars.setSessionValue("EfinReportGeneralLedger.initRecordNumber", "0");
      printPageDataSheet(response, vars, strFromPeriod, strToPeriod, "", strDateFrom, strDateTo,
          strPageNo, strAmtFrom, strAmtTo, strDocno, strAccSeq, strcelementvaluefrom,
          strcelementvalueto, strOrg, strcBpartnerId, strmProductId, strcProjectId,
          strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strGroupBy,
          strcAcctSchemaId, strcelementvaluefromdes, strcelementvaluetodes, strShowOpenBalances,
          strUniquecode);
    } else if (vars.commandIn("PREVIOUS_RELATION")) {
      String strInitRecord = vars.getSessionValue("EfinReportGeneralLedger.initRecordNumber");
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
          "EfinReportGeneralLedger");
      int intRecordRange = strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange);
      if (strInitRecord.equals("") || strInitRecord.equals("0"))
        vars.setSessionValue("EfinReportGeneralLedger.initRecordNumber", "0");
      else {
        int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
        initRecord -= intRecordRange;
        strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
        vars.setSessionValue("EfinReportGeneralLedger.initRecordNumber", strInitRecord);
      }
      response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("NEXT_RELATION")) {
      String strInitRecord = vars.getSessionValue("EfinReportGeneralLedger.initRecordNumber");
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
          "EfinReportGeneralLedger");
      int intRecordRange = strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange);
      int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
      // if (initRecord == 0)
      // initRecord = 1; Removed by DAL 30/4/09
      initRecord += intRecordRange;
      strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
      vars.setSessionValue("EfinReportGeneralLedger.initRecordNumber", strInitRecord);
      response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("PDF", "XLS")) {
      String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
          "EfinReportGeneralLedger|cAcctSchemaId");
      /*
       * String strDateFrom =
       * vars.getRequestGlobalVariable("inpDateFrom","EfinReportGeneralLedger|DateFrom"); String
       * strDateTo = vars.getRequestGlobalVariable("inpDateTo", "EfinReportGeneralLedger|DateTo");
       */
      String strFromPeriod = vars.getGlobalVariable("inpcFromPeriodId",
          "EfinReportGeneralLedger|cFromPeriodId", "");
      String strToPeriod = vars.getGlobalVariable("inpcToPeriodId",
          "EfinReportGeneralLedger|cToPeriodId", "");
      String strUniquecode = vars.getRequestGlobalVariable("inpUniqueCode",
          "EfinReportGeneralLedger|inpUniqueCode");
      String strDateFrom = getDate(vars, strFromPeriod, null);
      String strDateTo = getDate(vars, null, strToPeriod);
      String strAmtFrom = vars.getNumericParameter("inpAmtFrom");
      vars.setSessionValue("EfinReportGeneralLedger|AmtFrom", strAmtFrom);
      String strAmtTo = vars.getNumericParameter("inpAmtTo");
      vars.setSessionValue("EfinReportGeneralLedger|AmtTo", strAmtTo);
      /*
       * String strDocno = vars.getNumericParameter("inpDocumentno");
       * vars.setSessionValue("EfinReportGeneralLedger|DocNo", strDocno); String strAccSeq =
       * vars.getNumericParameter("inpAccountingSeq");
       * vars.setSessionValue("EfinReportGeneralLedger|AccSeq", strAccSeq);
       */
      String strDocno = vars.getRequestGlobalVariable("inpDocumentno",
          "EfinReportGeneralLedger|DocNo");
      String strAccSeq = vars.getRequestGlobalVariable("inpAccountingSeq",
          "EfinReportGeneralLedger|AccSeq");
      String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
          "EfinReportGeneralLedger|C_ElementValue_IDFROM");
      String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
          "EfinReportGeneralLedger|C_ElementValue_IDTO");
      String strOrg = vars.getGlobalVariable("inpOrg", "EfinReportGeneralLedger|Org", "0");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "EfinReportGeneralLedger|cBpartnerId", IsIDFilter.instance);
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "EfinReportGeneralLedger|mProductId", "", IsIDFilter.instance);
      String strcProjectId = vars.getInGlobalVariable("inpcProjectId_IN",
          "EfinReportGeneralLedger|cProjectId", "", IsIDFilter.instance);
      String strcSalesregionId = vars.getInGlobalVariable("inpcSalesregionId_IN",
          "EfinReportGeneralLedger|cSalesregionId", "", IsIDFilter.instance);
      String strcCampaignId = vars.getRequestGlobalVariable("inpcCampaignId_IN",
          "EfinReportGeneralLedger|cCampaignId");
      // String strcCampaignId = vars.getInGlobalVariable("inpcCampaignId_IN",
      // "EfinReportGeneralLedger|cCampaignId","", IsIDFilter.instance);
      String strcActivityId = vars.getInGlobalVariable("inpcActivityId_IN",
          "EfinReportGeneralLedger|cActivityId", "", IsIDFilter.instance);
      String strUser1Id = vars.getInGlobalVariable("inpcUser1Id_IN",
          "EfinReportGeneralLedger|user1Id", "", IsIDFilter.instance);
      String strUser2Id = vars.getInGlobalVariable("inpcUser2Id_IN",
          "EfinReportGeneralLedger|user2Id", "", IsIDFilter.instance);

      String strShowOpenBalances = vars.getRequestGlobalVariable("inpShowOpenBalances",
          "EfinReportGeneralLedger|showOpenBalances");
      String strGroupBy = vars.getRequestGlobalVariable("inpGroupBy",
          "EfinReportGeneralLedger|GroupBy");
      String strPageNo = vars.getGlobalVariable("inpPageNo", "EfinReportGeneralLedger|PageNo", "1");
      if (vars.commandIn("PDF"))
        printPageDataPDF(request, response, vars, strFromPeriod, strToPeriod, strDateFrom,
            strDateTo, strAmtFrom, strAmtTo, strDocno, strAccSeq, strcelementvaluefrom,
            strcelementvalueto, strOrg, strcBpartnerId, strmProductId, strcProjectId,
            strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strGroupBy,
            strcAcctSchemaId, strPageNo, strShowOpenBalances, strUniquecode);
      else
        printPageDataXLS(request, response, vars, strFromPeriod, strToPeriod, strDateFrom,
            strDateTo, strAmtFrom, strAmtTo, strDocno, strAccSeq, strcelementvaluefrom,
            strcelementvalueto, strOrg, strcBpartnerId, strmProductId, strcProjectId,
            strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strGroupBy,
            strcAcctSchemaId, strShowOpenBalances, strUniquecode);
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strFromPeriodId, String strToPeriodId, String currentPeriodId, String strDateFrom,
      String strDateTo, String strPageNo, String strAmtFrom, String strAmtTo, String strDocno,
      String strAccSeq, String strcelementvaluefrom, String strcelementvalueto, String strOrg,
      String strcBpartnerId, String strmProductId, String strcProjectId, String strcSalesregionId,
      String strcCampaignId, String strcActivityId, String strUser1Id, String strUser2Id,
      String strGroupBy, String strcAcctSchemaId, String strcelementvaluefromdes,
      String strcelementvaluetodes, String strShowOpenBalances, String strUniquecode)
      throws IOException, ServletException {
    String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
        "EfinReportGeneralLedger");
    int intRecordRange = (strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange));
    String strInitRecord = vars.getSessionValue("EfinReportGeneralLedger.initRecordNumber");
    int initRecordNumber = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
    String tempDepartmentId = strcSalesregionId;
    String count = EfinReportGeneralLedgerData.checkdept(this,
        Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
        Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
        strcSalesregionId);
    if (Integer.parseInt(count) > 0) {
      strcSalesregionId = null;
    }

    // built limit/offset parameters for oracle/postgres
    String rowNum = "0";
    String oraLimit1 = null;
    String oraLimit2 = null;
    String pgLimit = null;
    String strFrmPerStrDate = "";
    if (intRecordRange != 0) {
      if (this.myPool.getRDBMS().equalsIgnoreCase("ORACLE")) {
        rowNum = "ROWNUM";
        oraLimit1 = String.valueOf(initRecordNumber + intRecordRange);
        oraLimit2 = (initRecordNumber + 1) + " AND " + oraLimit1;
      } else {
        rowNum = "0";
        pgLimit = intRecordRange + " OFFSET " + initRecordNumber;
      }
    }
    log4j.debug("offset= " + initRecordNumber + " pageSize= " + intRecordRange);
    log4j.debug("Output: dataSheet");
    log4j.debug("Date From:" + strDateFrom + "- To:" + strDateTo + " - Schema:" + strcAcctSchemaId);
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    EfinReportGeneralLedgerData[] data = null;
    EfinReportGeneralLedgerData[] dataOrg = null;

    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    String toDatePlusOne = DateTimeData.nDaysAfter(this, strDateTo, "1");
    // Remember values
    String strcBpartnerIdAux = strcBpartnerId;
    String strmProductIdAux = strmProductId;
    String strcProjectIdAux = strcProjectId;
    String strcSalesregionIdAux = tempDepartmentId;
    String strcCampaignIdAux = strcCampaignId;
    String strcActivityIdAux = strcActivityId;
    String strUser1IdAux = strUser1Id;
    String strUser2IdAux = strUser2Id;

    String strGroupByText = (strGroupBy.equals("BPartner")
        ? Utility.messageBD(this, "BusPartner", vars.getLanguage())
        : (strGroupBy.equals("Product") ? Utility.messageBD(this, "Product", vars.getLanguage())
            : (strGroupBy.equals("Project") ? Utility.messageBD(this, "Project", vars.getLanguage())
                : "")));

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "EfinReportGeneralLedger", true, "", "",
        "imprimir();return false;", false, "ad_reports", strReplaceWith, false, true);
    /*
     * String strcBpartnerIdAux = strcBpartnerId; String strmProductIdAux = strmProductId; String
     * strcProjectIdAux = strcProjectId;
     */
    if (strDateFrom != null && strDateTo != null && strDateFrom.equals("")
        && strDateTo.equals("")) {
      String discard[] = { "sectionAmount", "sectionPartner" };
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/EfinReportGeneralLedger", discard)
          .createXmlDocument();
      toolbar.prepareRelationBarTemplate(false, false,
          "submitCommandForm('XLS', false, frmMain, 'ReportGeneralLedgerExcel.xls', 'EXCEL');return false;");
      data = EfinReportGeneralLedgerData.set();
    } else {
      String[] discard = { "discard" };
      if (strGroupBy.equals(""))
        discard[0] = "sectionPartner";
      else
        discard[0] = "sectionAmount";
      BigDecimal previousDebit = BigDecimal.ZERO;
      BigDecimal previousCredit = BigDecimal.ZERO;
      String strAllaccounts = "Y";
      if (strcelementvaluefrom != null && !strcelementvaluefrom.equals("")) {
        if (strcelementvalueto.equals("")) {
          strcelementvalueto = strcelementvaluefrom;
          strcelementvaluetodes = EfinReportGeneralLedgerData.selectSubaccountDescription(this,
              strcelementvalueto);
          vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);

        }
        strAllaccounts = "N";
        log4j.debug("##################### strcelementvaluefrom= " + strcelementvaluefrom);
        log4j.debug("##################### strcelementvalueto= " + strcelementvalueto);
      } else {
        strcelementvalueto = "";
        strcelementvaluetodes = "";
        vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);
      }
      Long initMainSelect = System.currentTimeMillis();
      EfinReportGeneralLedgerData scroll = null;
      if ((!vars.commandIn("DEFAULT"))
          || (vars.commandIn("DEFAULT") && tempDefault != "" && tempDefault != "0")) {
        try {
          scroll = EfinReportGeneralLedgerData.select2(this, rowNum, strGroupByText,
              vars.getLanguage(), strGroupBy, strAllaccounts, strcelementvaluefrom,
              strcelementvalueto,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
              "Y".equals(strShowOpenBalances) ? strDateTo : null, strcAcctSchemaId, strDateFrom,
              toDatePlusOne, strOrgFamily, strcBpartnerId, strmProductId, strcProjectId,
              strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id,
              strUniquecode, strAmtFrom, strAmtTo, strDocno, strAccSeq, vars.getRole(), null, null,
              pgLimit, oraLimit1, oraLimit2, null);
          Vector<EfinReportGeneralLedgerData> res = new Vector<EfinReportGeneralLedgerData>();
          while (scroll.next()) {
            res.add(scroll.get());
          }
          data = new EfinReportGeneralLedgerData[res.size()];
          res.copyInto(data);
        } finally {
          if (scroll != null) {
            scroll.close();
          }
        }
      }

      log4j.debug("Select2. Time in mils: " + (System.currentTimeMillis() - initMainSelect));
      log4j.debug("RecordNo: " + initRecordNumber);

      EfinReportGeneralLedgerData[] dataTotal = null;
      EfinReportGeneralLedgerData[] dataSubtotal = null;

      String strOld = "";
      // boolean firstPagBlock = false;
      EfinReportGeneralLedgerData[] subreportElement = new EfinReportGeneralLedgerData[1];
      Connection conn = null;

      try {
        conn = this.getConnection();
      } catch (NoConnectionAvailableException e1) {
        e1.printStackTrace();
      }
      String temp = null;
      int tempcount = 0;
      for (int i = 0; data != null && i < data.length; i++) {
        if (!strOld.equals(data[i].groupbyid + data[i].value)) {
          String dateacct = data[i].dateacctgregorian.split("-")[2].toString();
          if (temp != null) {
            if (temp != dateacct) {
              tempcount++;
              temp = dateacct;
            }
          } else {
            temp = dateacct;
          }
          String acctType = EfinReportGeneralLedgerData.checkaccount(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
              data[i].factAcctId);
          log4j.info("account type in general ledger:" + acctType);
          log4j.info("temp count in general ledger:" + tempcount);

          if (acctType.equalsIgnoreCase("E") || acctType.equalsIgnoreCase("R")) {
            if (tempcount > 0) {
              strFrmPerStrDate = EfinReportGeneralLedgerData.selectFrmPerStrtDate(this,
                  Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                  Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                  data[i].periodid);
            } else {
              strFrmPerStrDate = EfinReportGeneralLedgerData.selectFrmPerStrtDate(this,
                  Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                  Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                  strFromPeriodId);
            }
          } else {
            strFrmPerStrDate = EfinReportGeneralLedgerData.selectStrtDateFC(this,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"));
          }
          try {
            strFrmPerStrDate = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmPerStrDate));
          } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          log4j.info("Date pasing in general ledger:" + strFrmPerStrDate);

          subreportElement = new EfinReportGeneralLedgerData[1];
          // firstPagBlock = false;
          if (i == 0 && initRecordNumber > 0) {
            // firstPagBlock = true;
            Long init = System.currentTimeMillis();

            dataTotal = EfinReportGeneralLedgerData.select2Total(this, rowNum, strGroupByText,
                strGroupBy, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                vars.getRole(), strcAcctSchemaId, "",
                DateTimeData.nDaysAfter(this, data[0].dateacctgregorian, "1"), strOrgFamily,
                strcBpartnerId, strmProductId, strcProjectId, strcSalesregionId, strcCampaignId,
                strcActivityId, strUser1Id, strUser2Id, strAmtFrom, strAmtTo, strDocno, strAccSeq,
                data[0].value, data[0].groupbyid, null, null, null, null);
            dataSubtotal = EfinReportGeneralLedgerData.select2sum(this, rowNum, strGroupByText,
                strGroupBy, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                vars.getRole(), strcAcctSchemaId, strDateFrom, toDatePlusOne, strOrgFamily,
                (strGroupBy.equals("BPartner") ? "('" + data[i].groupbyid + "')" : strcBpartnerId),
                (strGroupBy.equals("Product") ? "('" + data[i].groupbyid + "')" : strmProductId),
                (strGroupBy.equals("Project") ? "('" + data[i].groupbyid + "')" : strcProjectId),
                (strGroupBy.equals("Department") ? "('" + data[i].groupbyid + "')"
                    : strcSalesregionId),
                (strGroupBy.equals("BudgetType") ? "('" + data[i].groupbyid + "')"
                    : strcCampaignId),
                (strGroupBy.equals("FunClass") ? "('" + data[i].groupbyid + "')" : strcActivityId),
                (strGroupBy.equals("Future1") ? "('" + data[i].groupbyid + "')" : strUser1Id),
                (strGroupBy.equals("Future2") ? "('" + data[i].groupbyid + "')" : strUser2Id),
                strAmtFrom, strAmtTo, strDocno, strAccSeq, null, null, null, null, null, null,
                data[0].value);
            log4j.debug("Select2Total. Time in mils: " + (System.currentTimeMillis() - init));
            // Now dataTotal is covered adding debit and credit amounts
            for (int j = 0; dataTotal != null && j < dataTotal.length; j++) {
              previousDebit = previousDebit.add(new BigDecimal(dataTotal[j].amtacctdr));
              previousCredit = previousCredit.add(new BigDecimal(dataTotal[j].amtacctcr));
            }
            subreportElement = new EfinReportGeneralLedgerData[1];
            subreportElement[0] = new EfinReportGeneralLedgerData();
            subreportElement[0].totalacctdr = previousDebit.toPlainString();
            subreportElement[0].totalacctcr = previousCredit.toPlainString();
            data[0].amtacctdrprevsum = (dataSubtotal != null) ? dataSubtotal[0].amtacctdr
                : data[0].amtacctdrprevsum;
            data[0].amtacctcrprevsum = (dataSubtotal != null) ? dataSubtotal[0].amtacctcr
                : data[0].amtacctcrprevsum;
            // data[0].amtacctdrprevsum = (dataSubtotal != null) ? data[0].amtacctdrprevsum :
            // data[0].amtacctdrprevsum;
            // data[0].amtacctcrprevsum = (dataSubtotal != null) ? data[0].amtacctcrprevsum :
            // data[0].amtacctcrprevsum;

            subreportElement[0].total = previousDebit.subtract(previousCredit).toPlainString();
          } else {
            if ("".equals(data[i].groupbyid)) {
              // The argument " " is used to simulate one value and put the optional parameter-->
              // AND FACT_ACCT.C_PROJECT_ID IS NULL for example
              Long init = System.currentTimeMillis();
              subreportElement = EfinReportGeneralLedgerData.selectTotal2(this, strcBpartnerId,
                  (strGroupBy.equals("BPartner") ? " " : null), strmProductId,
                  (strGroupBy.equals("Product") ? " " : null), strcProjectId,
                  (strGroupBy.equals("Project") ? " " : null), strcAcctSchemaId, strDocno,
                  strAccSeq, data[i].value, "", strDateFrom,
                  Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                  strOrgFamily, vars.getRole());
              log4j.debug("SelectTotalNew. Time in mils: " + (System.currentTimeMillis() - init));
            } else {
              Long init = System.currentTimeMillis();
              subreportElement = EfinReportGeneralLedgerData.selectTotal2(this,
                  (strGroupBy.equals("BPartner") ? "('" + data[i].groupbyid + "')"
                      : strcBpartnerId),
                  null,
                  (strGroupBy.equals("Product") ? "('" + data[i].groupbyid + "')" : strmProductId),
                  null,
                  (strGroupBy.equals("Project") ? "('" + data[i].groupbyid + "')" : strcProjectId),
                  null, strcAcctSchemaId, strDocno, strAccSeq, data[i].value, "", strDateFrom,
                  Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                  strOrgFamily, vars.getRole());
              log4j.debug("SelectTotalNew. Time in mils: " + (System.currentTimeMillis() - init));
            }
          }
          data[i].totalacctdr = subreportElement[0].totalacctdr;
          data[i].totalacctcr = subreportElement[0].totalacctcr;

        }

        data[i].totalacctsub = subreportElement[0].total;
        data[i].previousdebit = subreportElement[0].totalacctdr;
        data[i].previouscredit = subreportElement[0].totalacctcr;
        data[i].previoustotal = subreportElement[0].total;

        strOld = data[i].groupbyid + data[i].value;

        Map<String, String> docTypeMap = new HashMap<String, String>();
        docTypeMap.put("GLJ", "GL Journal");
        docTypeMap.put("GLD", "GL Document");
        docTypeMap.put("API", "AP Prepayment Application");
        docTypeMap.put("APP", "AP Payment");
        docTypeMap.put("ARI", "AR Invoice");
        docTypeMap.put("ARR", "AR Receipt");
        docTypeMap.put("SOO", "Sales Order");
        docTypeMap.put("ARF", "AR Pro Forma Invoice");
        docTypeMap.put("MMS", "Material Delivery");
        docTypeMap.put("MMR", "Material Receipt");
        docTypeMap.put("MMM", "Material Movement");
        docTypeMap.put("POO", "Purchase Order");
        docTypeMap.put("POR", "Purchase Requisition");
        docTypeMap.put("MMI", "Material Physical Inventory");
        docTypeMap.put("APC", "AP Credit Memo");
        docTypeMap.put("ARC", "AR Credit Memo");
        docTypeMap.put("CMB", "Bank Statement");
        docTypeMap.put("CMC", "Cash Journal");
        docTypeMap.put("CMA", "Payment Allocation");
        docTypeMap.put("MMP", "Material Production");
        docTypeMap.put("MXI", "Match Invoice");
        docTypeMap.put("MXP", "Match PO");
        docTypeMap.put("PJI", "Project Issue");
        docTypeMap.put("STT", "Settlement): Settlement of debt-payments");
        docTypeMap.put("STM", "Settlement manual");
        docTypeMap.put("AMZ", "Amortization");
        docTypeMap.put("FAT", "Financial Account Transaction");
        docTypeMap.put("ARRP", "AR Receivable Proposal");
        docTypeMap.put("REC", "Reconciliation");
        // --- (** New **)
        docTypeMap.put("DPM", "Debt Payment Management");
        docTypeMap.put("BSF", "Bank Statement File");
        docTypeMap.put("PPR", "Payment proposal");
        docTypeMap.put("APPP", "AP Payment Proposal");
        docTypeMap.put("PRJ", "Payment Reverse Journal");
        docTypeMap.put("PPA", "AP Prepayment Application");

        String factAcctId = data[i].factAcctId;

        /*
         * if(factAcctId != null) { try {
         * 
         * ResultSet factRS = conn.createStatement().executeQuery(
         * " select fact.ad_table_id,fact.record_id,fact.docbasetype,tab.tablename from fact_acct  fact"
         * + " join ad_table tab on tab.ad_table_id=fact.ad_table_id" +
         * " where  fact.fact_acct_id='" + factAcctId + "'");
         * 
         * String tableName = null, docbasetype = null, recordId = null;
         * 
         * while (factRS.next()) { tableName = factRS.getString(4); docbasetype =
         * factRS.getString(3); recordId = factRS.getString(2); }
         * 
         * factRS.close();
         * 
         * data[i].transactiontype = docTypeMap.get(docbasetype);
         * 
         * //if(!tableName.equalsIgnoreCase("FIN_Finacc_Transaction")) {
         * 
         * ResultSet docRS = conn.createStatement().executeQuery(
         * "select em_efin_acctseq from fact_acct where fact_acct_id='" + factAcctId + "'");
         * 
         * while (docRS.next()) { data[i].accountseq = docRS.getString(1); }
         * 
         * docRS.close(); //}
         * 
         * } catch (Exception e) { log4j.error("Exception in General ledger Report:" + e); }
         * 
         * }
         */

      }
      // TODO: What is strTotal?? is this the proper variable name?
      String strTotal = "";
      subreportElement = new EfinReportGeneralLedgerData[1];
      for (int i = 0; data != null && i < data.length; i++) {
        String dateacct = data[i].dateacctgregorian.split("-")[2].toString();
        if (temp != null) {
          if (temp != dateacct) {
            tempcount++;
            temp = dateacct;
          }
        } else {
          temp = dateacct;
        }

        String acctType = EfinReportGeneralLedgerData.checkaccount(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
            Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
            data[i].factAcctId);
        log4j.info("account type in general ledger:" + acctType);
        if (acctType.equalsIgnoreCase("E") || acctType.equalsIgnoreCase("R")) {
          if (tempcount > 0) {
            strFrmPerStrDate = EfinReportGeneralLedgerData.selectFrmPerStrtDate(this,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                data[i].periodid);
          } else {
            strFrmPerStrDate = EfinReportGeneralLedgerData.selectFrmPerStrtDate(this,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                strFromPeriodId);
          }
        } else {
          strFrmPerStrDate = EfinReportGeneralLedgerData.selectStrtDateFC(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"));
        }
        try {
          strFrmPerStrDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmPerStrDate));
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        log4j.info("Date pasing in general ledger:" + strFrmPerStrDate);

        if (!strTotal.equals(data[i].groupbyid + data[i].value)) {
          subreportElement = new EfinReportGeneralLedgerData[1];
          if ("".equals(data[i].groupbyid)) {
            // The argument " " is used to simulate one value and put the optional parameter--> AND
            // FACT_ACCT.C_PROJECT_ID IS NULL for example
            Long init = System.currentTimeMillis();
            subreportElement = EfinReportGeneralLedgerData.selectTotal2(this, strcBpartnerId,
                (strGroupBy.equals("BPartner") ? " " : null), strmProductId,
                (strGroupBy.equals("Product") ? " " : null), strcProjectId,
                (strGroupBy.equals("Project") ? " " : null), strcAcctSchemaId, strDocno, strAccSeq,
                data[i].value, strFrmPerStrDate, toDatePlusOne,
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                strOrgFamily, vars.getRole());
            log4j.debug("SelectTotal2. Time in mils: " + (System.currentTimeMillis() - init));
          } else {
            Long init = System.currentTimeMillis();
            subreportElement = EfinReportGeneralLedgerData.selectTotal2(this,
                (strGroupBy.equals("BPartner") ? "('" + data[i].groupbyid + "')" : strcBpartnerId),
                null,
                (strGroupBy.equals("Product") ? "('" + data[i].groupbyid + "')" : strmProductId),
                null,
                (strGroupBy.equals("Project") ? "('" + data[i].groupbyid + "')" : strcProjectId),
                null, strcAcctSchemaId, strDocno, strAccSeq, data[i].value, strFrmPerStrDate,
                toDatePlusOne,
                Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
                strOrgFamily, vars.getRole());
            log4j.debug("SelectTotal2. Time in mils: " + (System.currentTimeMillis() - init));
          }
        }

        data[i].finaldebit = subreportElement[0].totalacctdr;
        data[i].finalcredit = subreportElement[0].totalacctcr;
        data[i].finaltotal = subreportElement[0].total;

        strTotal = data[i].groupbyid + data[i].value;
      }
      boolean hasPrevious = !(data == null || data.length == 0 || initRecordNumber <= 1);
      boolean hasNext = !(data == null || data.length == 0 || data.length < intRecordRange);
      toolbar.prepareRelationBarTemplate(hasPrevious, hasNext,
          "submitCommandForm('XLS', true, frmMain, 'ReportGeneralLedgerExcel.xls', 'EXCEL');return false;");
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/EfinReportGeneralLedger", discard)
          .createXmlDocument();
    }
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_reports.EfinReportGeneralLedger");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "EfinReportGeneralLedger.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "EfinReportGeneralLedger.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("EfinReportGeneralLedger");
      vars.removeMessage("EfinReportGeneralLedger");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));

    try {
      String orgList = "";

      dataOrg = EfinReportGeneralLedgerData.selectOrgValidation(this,
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalanceData"));

      for (int i = 0; i < dataOrg.length; i++) {
        if (i == 0) {
          orgList = "'" + dataOrg[i].adOrgId + "'";
        } else {
          orgList += "," + "'" + dataOrg[i].adOrgId + "'";
        }
      }

      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_ORG_ID", "",
          "", orgList, Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
          '*');
      comboTableData.fillParameters(null, "EfinReportGeneralLedger", "");
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    if (vars.commandIn("DEFAULT")) {
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure",
          EfinReportGeneralLedgerData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
              currentPeriodId));
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure",
          EfinReportGeneralLedgerData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
              currentPeriodId));
    } else {
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure",
          EfinReportGeneralLedgerData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
              strFromPeriodId));
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure",
          EfinReportGeneralLedgerData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
              strToPeriodId));
    }

    xmlDocument.setData("reportCCampaignId_IN", "liststructure",
        EfinReportGeneralLedgerData.selectC_CAMPAIGN_ID(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
            Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
            strcCampaignId, vars.getRole()));

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("PageNo", strPageNo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    if ((vars.commandIn("DEFAULT")) && (tempDefault == "" || tempDefault == "0")) {
      xmlDocument.setParameter("cFromPeriodId", currentPeriodId);
      xmlDocument.setParameter("cToPeriodId", currentPeriodId);
    } else {
      xmlDocument.setParameter("cFromPeriodId", strFromPeriodId);
      xmlDocument.setParameter("cToPeriodId", strToPeriodId);
    }
    xmlDocument.setParameter("amtFrom", strAmtFrom);
    xmlDocument.setParameter("amtTo", strAmtTo);
    xmlDocument.setParameter("DocNo", strDocno);
    xmlDocument.setParameter("AccSeq", strAccSeq);
    xmlDocument.setParameter("adOrgId", strOrg);
    xmlDocument.setParameter("cAcctschemaId", strcAcctSchemaId);
    xmlDocument.setParameter("cCampaignId", strcCampaignId);

    xmlDocument.setParameter("paramElementvalueIdTo", strcelementvalueto);
    xmlDocument.setParameter("paramElementvalueIdFrom", strcelementvaluefrom);
    xmlDocument.setParameter("inpElementValueIdTo_DES", strcelementvaluetodes);
    xmlDocument.setParameter("inpElementValueIdFrom_DES", strcelementvaluefromdes);
    xmlDocument.setParameter("groupbyselected", strGroupBy);
    xmlDocument.setParameter("showOpenBalances", strShowOpenBalances);
    xmlDocument.setData("reportCBPartnerId_IN", "liststructure",
        SelectorUtilityData.selectBpartner(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcBpartnerIdAux));
    xmlDocument.setData("reportMProductId_IN", "liststructure",
        SelectorUtilityData.selectMproduct(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strmProductIdAux));
    if (vars.commandIn("FINDLINK")) {
      xmlDocument.setData("reportCBPartnerId_IN", "liststructure",
          selectBpartnerFromUniqueCode(this, strUniquecode));
      xmlDocument.setData("reportCProjectId_IN", "liststructure",
          EfinReportGeneralLedgerData.selectprojectUniquecode(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUniquecode));
      xmlDocument.setData("reportCSalesRegionId_IN", "liststructure",
          EfinReportGeneralLedgerData.selectCsalesRegionUniquecode(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUniquecode));
      xmlDocument.setData("reportCActivityId_IN", "liststructure",
          EfinReportGeneralLedgerData.selectactivityUniquecode(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUniquecode));
      xmlDocument.setData("reportCUser1Id_IN", "liststructure",
          EfinReportGeneralLedgerData.selectuserUniquecode(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUniquecode));
      xmlDocument.setData("reportCUser2Id_IN", "liststructure",
          EfinReportGeneralLedgerData.selectuser2Uniquecode(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUniquecode));
    } else {
      xmlDocument.setData("reportCProjectId_IN", "liststructure",
          SelectorUtilityData.selectProject(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strcProjectIdAux));
      xmlDocument.setData("reportCSalesRegionId_IN", "liststructure",
          EfinReportGeneralLedgerData.selectCsalesRegion(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strcSalesregionIdAux));
      xmlDocument.setData("reportCActivityId_IN", "liststructure",
          EfinReportGeneralLedgerData.selectCActivity(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strcActivityIdAux));
      xmlDocument.setData("reportCUser1Id_IN", "liststructure",
          EfinReportGeneralLedgerData.selectUser1(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUser1IdAux));
      xmlDocument.setData("reportCUser2Id_IN", "liststructure",
          EfinReportGeneralLedgerData.selectUser2(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
              Utility.getContext(this, vars, "#User_Client", ""), strUser2IdAux));
    }
    xmlDocument.setData("reportC_ACCTSCHEMA_ID", "liststructure",
        AccountingSchemaMiscData.selectC_ACCTSCHEMA_ID(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
            Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
            strcAcctSchemaId));
    xmlDocument.setParameter("strFromPeriod",
        Resource.getProperty("finance.g/lreport.fromperiod", vars.getLanguage()));
    xmlDocument.setParameter("strToPeriod",
        Resource.getProperty("finance.g/lreport.toperiod", vars.getLanguage()));
    xmlDocument.setParameter("strFromAmount",
        Resource.getProperty("finance.g/lreport.fromamount", vars.getLanguage()));
    xmlDocument.setParameter("strToAmount",
        Resource.getProperty("finance.g/lreport.toamount", vars.getLanguage()));
    xmlDocument.setParameter("strorganization",
        Resource.getProperty("finance.organization", vars.getLanguage()));
    xmlDocument.setParameter("strgeneralledger",
        Resource.getProperty("finance.generalledger", vars.getLanguage()));
    xmlDocument.setParameter("strFromAccount",
        Resource.getProperty("finance.g/lreport.fromaccount", vars.getLanguage()));
    xmlDocument.setParameter("strToAccount",
        Resource.getProperty("finance.g/lreport.toaccount", vars.getLanguage()));
    xmlDocument.setParameter("strDocumentNo",
        Resource.getProperty("finance.documentno", vars.getLanguage()));
    xmlDocument.setParameter("strAcctseq",
        Resource.getProperty("finance.Accountseq", vars.getLanguage()));
    xmlDocument.setParameter("strPostingSeq",
        Resource.getProperty("finance.g/lreport.postingseq", vars.getLanguage()));
    xmlDocument.setParameter("strDepartment",
        Resource.getProperty("finance.department", vars.getLanguage()));
    xmlDocument.setParameter("strBudgettype",
        Resource.getProperty("finance.budgetdetailreport.field.budgettyp", vars.getLanguage()));
    xmlDocument.setParameter("strProject",
        Resource.getProperty("finance.project", vars.getLanguage()));
    xmlDocument.setParameter("strFunctioanlClassification",
        Resource.getProperty("finance.functionalclassification", vars.getLanguage()));
    xmlDocument.setParameter("strFuture1",
        Resource.getProperty("finance.future1", vars.getLanguage()));
    xmlDocument.setParameter("strFuture2",
        Resource.getProperty("finance.future2", vars.getLanguage()));
    xmlDocument.setParameter("primaryfilter",
        Resource.getProperty("finance.PrimaryFilters", vars.getLanguage()));
    xmlDocument.setParameter("Dimension",
        Resource.getProperty("finance.dimensions", vars.getLanguage()));
    xmlDocument.setParameter("viewresult",
        Resource.getProperty("finance.viewresults", vars.getLanguage()));
    xmlDocument.setParameter("search", Resource.getProperty("finance.search", vars.getLanguage()));
    xmlDocument.setParameter("account",
        Resource.getProperty("finance.account", vars.getLanguage()));
    xmlDocument.setParameter("date", Resource.getProperty("finance.date", vars.getLanguage()));
    xmlDocument.setParameter("category",
        Resource.getProperty("finance.g/lreport.category", vars.getLanguage()));
    xmlDocument.setParameter("description",
        Resource.getProperty("finance.g/lreport.description", vars.getLanguage()));
    xmlDocument.setParameter("debit",
        Resource.getProperty("finance.g/lreport.debit", vars.getLanguage()));
    xmlDocument.setParameter("credit",
        Resource.getProperty("finance.g/lreport.credit", vars.getLanguage()));
    xmlDocument.setParameter("balance",
        Resource.getProperty("finance.g/lreport.balance", vars.getLanguage()));
    xmlDocument.setParameter("previous",
        Resource.getProperty("finance.g/lreport.previous", vars.getLanguage()));
    xmlDocument.setParameter("subtotal",
        Resource.getProperty("finance.g/lreport.subtotal", vars.getLanguage()));
    xmlDocument.setParameter("total",
        Resource.getProperty("finance.g/lreport.total", vars.getLanguage()));

    // log4j.debug("data.length: " + data.length);
    /*
     * if(vars.commandIn("DEFAULT")){ //Default process will empty data section data = new
     * EfinReportGeneralLedgerData[0]; }
     */
    if (vars.commandIn("FIND")) {
      SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
      Date formatdate = new Date();
      Date todate = new Date();
      try {
        formatdate = in.parse(strDateFrom);
        todate = in.parse(strDateTo);
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (formatdate.after(todate)) {
        xmlDocument.setParameter("messageType", "WARNING");
        xmlDocument.setParameter("messageTitle",
            Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()));
        xmlDocument.setParameter("messageMessage",
            Utility.messageBD(this, "Efin_InvalidPeriod", vars.getLanguage()));
        out.println(xmlDocument.print());
        out.close();
      }
    }
    if (data != null && data.length > 0) {
      if (strGroupBy.equals(""))
        xmlDocument.setData("structure1", data);
      else
        xmlDocument.setData("structure2", data);
    } else {
      if (vars.commandIn("FIND") || vars.commandIn("FINDLINK")) {
        // No data has been found. Show warning message.
        xmlDocument.setParameter("messageType", "WARNING");
        xmlDocument.setParameter("messageTitle",
            Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()));
        xmlDocument.setParameter("messageMessage",
            Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
      }
    }

    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageDataPDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFromPeriodId, String ToPeriodId, String strDateFrom,
      String strDateTo, String strAmtFrom, String strAmtTo, String strDocno, String strAccSeq,
      String strcelementvaluefrom, String strcelementvalueto, String strOrg, String strcBpartnerId,
      String strmProductId, String strcProjectId, String strcSalesregionId, String strcCampaignId,
      String strcActivityId, String strUser1Id, String strUser2Id, String strGroupBy,
      String strcAcctSchemaId, String strPageNo, String strShowOpenBalances, String strUniquecode)
      throws IOException, ServletException {
    log4j.debug("Output: PDF");
    response.setContentType("text/html; charset=UTF-8");
    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    String toDatePlusOne = DateTimeData.nDaysAfter(this, strDateTo, "1");

    String strGroupByText = (strGroupBy.equals("BPartner")
        ? Utility.messageBD(this, "BusPartner", vars.getLanguage())
        : (strGroupBy.equals("Product") ? Utility.messageBD(this, "Product", vars.getLanguage())
            : (strGroupBy.equals("Project") ? Utility.messageBD(this, "Project", vars.getLanguage())
                : "")));
    String strAllaccounts = "Y";

    if (strDateFrom != null && strDateTo != null && strDateFrom.equals("")
        && strDateTo.equals("")) {
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
      return;
    }

    if (strcelementvaluefrom != null && !strcelementvaluefrom.equals("")) {
      if (strcelementvalueto.equals(""))
        strcelementvalueto = strcelementvaluefrom;
      strAllaccounts = "N";
    }

    EfinReportGeneralLedgerData data = null;
    try {
      data = EfinReportGeneralLedgerData.select2(this, "0", strGroupByText, vars.getLanguage(),
          strGroupBy, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
          "Y".equals(strShowOpenBalances) ? strDateTo : null, strcAcctSchemaId, strDateFrom,
          toDatePlusOne, strOrgFamily, strcBpartnerId, strmProductId, strcProjectId,
          strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strUniquecode,
          strAmtFrom, strAmtTo, strDocno, strAccSeq, vars.getRole(), null, null, null, null, null,
          null);

      if (!data.hasData()) {
        advisePopUp(request, response, "WARNING",
            Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
            Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
        return;
      }

      // augment data with totals
      AddTotals dataWithTotals = new AddTotals(data, strGroupBy, strcBpartnerId, strmProductId,
          strcProjectId, strcAcctSchemaId, strDocno, strAccSeq, strDateFrom, strOrgFamily, vars,
          this);

      String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/EfinReportGeneralLedger.jrxml";
      response.setHeader("Content-disposition", "inline; filename=ReportGeneralLedgerPDF.pdf");
      String strOutputFileName = "ReportGeneralLedgerPDF";
      HashMap<String, Object> parameters = new HashMap<String, Object>();

      String strLanguage = vars.getLanguage();

      parameters.put("ShowGrouping", new Boolean(!strGroupBy.equals("")));
      StringBuilder strSubTitle = new StringBuilder();
      // in report to print date as hijiri
      SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");

      Date formatdate = in.parse(strDateTo);
      String to_date = out.format(formatdate);

      Date formattodate = in.parse(strDateFrom);
      String from_date = out.format(formattodate);

      String hijiritodate = UtilityDAO.convertToHijriDate(to_date);
      String hijirifromdate = UtilityDAO.convertToHijriDate(from_date);

      Date date = new Date();
      String currentdate = UtilityDAO.convertToHijriDate(out.format(date));

      strSubTitle.append(Utility.messageBD(this, "DateFrom", strLanguage) + ": "
          + hijirifromdate.substring(0, 10) + " - " + Utility.messageBD(this, "DateTo", strLanguage)
          + ": " + hijiritodate.substring(0, 10) + " (");
      strSubTitle.append(EfinReportGeneralLedgerData.selectCompany(this, vars.getClient()) + " - ");
      log4j.debug("enteesdfs");

      strSubTitle.append(EfinReportGeneralLedgerData.selectOrganization(this, strOrg) + ")");
      parameters.put("REPORT_SUBTITLE", strSubTitle.toString());
      parameters.put("Previous", Utility.messageBD(this, "Initial Balance", strLanguage));
      parameters.put("CURRENT_DATE", currentdate);
      parameters.put("Total", Utility.messageBD(this, "Total", strLanguage));
      parameters.put("PageNo", strPageNo);
      String strDateFormat;
      strDateFormat = vars.getJavaDateFormat();
      parameters.put("strDateFormat", strDateFormat);

      renderJR(vars, response, strReportName, strOutputFileName, "pdf", parameters, dataWithTotals,
          null);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (data != null) {
        data.close();
      }
    }
  }

  private void printPageDataXLS(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFromPeriodId, String ToPeriodId, String strDateFrom,
      String strDateTo, String strAmtFrom, String strAmtTo, String strDocno, String strAccSeq,
      String strcelementvaluefrom, String strcelementvalueto, String strOrg, String strcBpartnerId,
      String strmProductId, String strcProjectId, String strcSalesregionId, String strcCampaignId,
      String strcActivityId, String strUser1Id, String strUser2Id, String strGroupBy,
      String strcAcctSchemaId, String strShowOpenBalances, String strUniquecode)
      throws IOException, ServletException {
    log4j.debug("Output: XLS");
    response.setContentType("text/html; charset=UTF-8");
    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = "";
    strOrgFamily = getFamily(strTreeOrg, strOrg);
    String toDatePlusOne = DateTimeData.nDaysAfter(this, strDateTo, "1");

    String strAllaccounts = "Y";

    if (strDateFrom != null && strDateTo != null && strDateFrom.equals("")
        && strDateTo.equals("")) {
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
      return;
    }

    if (strcelementvaluefrom != null && !strcelementvaluefrom.equals("")) {
      if (strcelementvalueto.equals(""))
        strcelementvalueto = strcelementvaluefrom;
      strAllaccounts = "N";
    }

    EfinReportGeneralLedgerData data = null;
    try {
      /*
       * data = EfinReportGeneralLedgerData.selectXLS2(this, strAllaccounts, strcelementvaluefrom,
       * strcelementvalueto, Utility.getContext(this, vars, "#AccessibleOrgTree",
       * "EfinReportGeneralLedger"), Utility.getContext(this, vars, "#User_Client",
       * "EfinReportGeneralLedger"), "Y".equals(strShowOpenBalances) ? strDateTo : null,
       * strcAcctSchemaId, strDateFrom, toDatePlusOne, strOrgFamily, strcBpartnerId, strmProductId,
       * strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id,
       * strAmtFrom, strAmtTo, strDocno, strAccSeq);
       */
      data = EfinReportGeneralLedgerData.select2(this, "0", null, vars.getLanguage(), strGroupBy,
          strAllaccounts, strcelementvaluefrom, strcelementvalueto,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
          "Y".equals(strShowOpenBalances) ? strDateTo : null, strcAcctSchemaId, strDateFrom,
          toDatePlusOne, strOrgFamily, strcBpartnerId, strmProductId, strcProjectId,
          strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strUniquecode,
          strAmtFrom, strAmtTo, strDocno, strAccSeq, vars.getRole(), null, null, null, null, null,
          null);
      if (!data.hasData()) {
        advisePopUp(request, response, "WARNING",
            Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
            Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
        return;
      }

      AddTotals dataWithTotals = new AddTotals(data, strGroupBy, strcBpartnerId, strmProductId,
          strcProjectId, strcAcctSchemaId, strDocno, strAccSeq, strDateFrom, strOrgFamily, vars,
          this);

      ScrollableFieldProvider limitedData = new LimitRowsScrollableFieldProviderFilter(
          dataWithTotals, 65532);

      String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportGeneralLedgerExcel.jrxml";

      HashMap<String, Object> parameters = new HashMap<String, Object>();

      String strLanguage = vars.getLanguage();

      StringBuilder strSubTitle = new StringBuilder();
      // in report to print date as hijiri
      SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");

      Date formatdate = in.parse(strDateTo);
      String to_date = out.format(formatdate);

      Date formattodate = in.parse(strDateFrom);
      String from_date = out.format(formattodate);

      String hijiritodate = UtilityDAO.convertToHijriDate(to_date);
      String hijirifromdate = UtilityDAO.convertToHijriDate(from_date);

      strSubTitle.append(Utility.messageBD(this, "DateFrom", strLanguage) + ": "
          + hijirifromdate.substring(0, 10) + " - " + Utility.messageBD(this, "DateTo", strLanguage)
          + ": " + hijiritodate.substring(0, 10) + " (");
      strSubTitle.append(EfinReportGeneralLedgerData.selectCompany(this, vars.getClient()) + " - ");
      strSubTitle.append(EfinReportGeneralLedgerData.selectOrganization(this, strOrg) + ")");
      parameters.put("REPORT_SUBTITLE", strSubTitle.toString());
      String strDateFormat;
      strDateFormat = vars.getJavaDateFormat();
      parameters.put("strDateFormat", strDateFormat);

      renderJR(vars, response, strReportName, null, "xls", parameters, limitedData, null);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (data != null) {
        data.close();
      }
    }
  }

  public String getDate(VariablesSecureApp vars, String strFromPeriodId, String strToPeriodId)
      throws IOException, ServletException {
    EfinReportGeneralLedgerData[] data = null;
    String strDate = null;
    if (strFromPeriodId != null) {
      data = EfinReportGeneralLedgerData.selectFromToDate(this,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"),
          strFromPeriodId);
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          strDate = data[i].startdate;
        }

      }
    }
    if (strToPeriodId != null) {
      data = EfinReportGeneralLedgerData.selectFromToDate(this,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
          Utility.getContext(this, vars, "#User_Client", "EfinReportGeneralLedger"), strToPeriodId);
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          strDate = data[i].enddate;
        }

      }
    }
    return strDate;
  }

  public String getCurrentPeriod(VariablesSecureApp vars, String OrgId, String ClientId) {
    String CurrentPeriodId = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn.prepareStatement(
          "select c_period_id from c_period where to_date(to_char(now(),'dd-MM-yyyy'),'dd-MM-yyyy') between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') and ad_org_id in("
              + OrgId + ") and ad_client_id in(" + ClientId + ")");
      rs = ps.executeQuery();
      if (rs.next()) {
        CurrentPeriodId = rs.getString("c_period_id");
      }
    } catch (Exception e) {
      // TODO: handle exception
    }
    return CurrentPeriodId;
  }

  private static class AddTotals extends AbstractScrollableFieldProviderFilter {
    public AddTotals(ScrollableFieldProvider input, String strGroupBy, String strcBpartnerId,
        String strmProductId, String strcProjectId, String strcAcctSchemaId, String strDocno,
        String strAccSeq, String strDateFrom, String strOrgFamily, VariablesSecureApp vars,
        ConnectionProvider conn) {
      super(input);
      this.strGroupBy = strGroupBy;
      this.strcBpartnerId = strcBpartnerId;
      this.strmProductId = strmProductId;
      this.strcProjectId = strcProjectId;
      this.strcAcctSchemaId = strcAcctSchemaId;
      this.strDocno = strDocno;
      this.strAccSeq = strAccSeq;
      this.strDateFrom = strDateFrom;
      this.strOrgFamily = strOrgFamily;
      this.vars = vars;
      this.conn = conn;
    }

    String strGroupBy;
    String strcBpartnerId;
    String strmProductId;
    String strcProjectId;
    String strcAcctSchemaId;
    String strDateFrom;
    String strOrgFamily;
    VariablesSecureApp vars;
    String strDocno;
    String strAccSeq;
    ConnectionProvider conn;
    String strOld = "";
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    BigDecimal subTotal = BigDecimal.ZERO;
    EfinReportGeneralLedgerData subreport[] = new EfinReportGeneralLedgerData[1];

    @Override
    public FieldProvider get() throws ServletException {

      FieldProvider data = input.get();

      EfinReportGeneralLedgerData cur = (EfinReportGeneralLedgerData) data;

      // adjust data as needed
      if (!strOld.equals(cur.groupbyid + cur.id)) {
        if ("".equals(cur.groupbyid)) {
          // The argument " " is used to simulate one value and put the optional parameter--> AND
          // FACT_ACCT.C_PROJECT_ID IS NULL for example
          subreport = EfinReportGeneralLedgerData.selectTotal2(conn, strcBpartnerId,
              (strGroupBy.equals("BPartner") ? " " : null), strmProductId,
              (strGroupBy.equals("Product") ? " " : null), strcProjectId,
              (strGroupBy.equals("Project") ? " " : null), strcAcctSchemaId, strDocno, strAccSeq,
              cur.id, "", strDateFrom,
              Utility.getContext(conn, vars, "#User_Client", "EfinReportGeneralLedger"),
              strOrgFamily, vars.getRole());
        } else {
          subreport = EfinReportGeneralLedgerData.selectTotal2(conn,
              (strGroupBy.equals("BPartner") ? "('" + cur.groupbyid + "')" : strcBpartnerId), null,
              (strGroupBy.equals("Product") ? "('" + cur.groupbyid + "')" : strmProductId), null,
              (strGroupBy.equals("Project") ? "('" + cur.groupbyid + "')" : strcProjectId), null,
              strcAcctSchemaId, strDocno, strAccSeq, cur.id, "", strDateFrom,
              Utility.getContext(conn, vars, "#User_Client", "EfinReportGeneralLedger"),
              strOrgFamily, vars.getRole());
        }
        totalDebit = BigDecimal.ZERO;
        totalCredit = BigDecimal.ZERO;
        subTotal = BigDecimal.ZERO;
      }
      totalDebit = totalDebit.add(new BigDecimal(cur.amtacctdr));
      cur.totalacctdr = new BigDecimal(subreport[0].totalacctdr).add(totalDebit).toString();
      totalCredit = totalCredit.add(new BigDecimal(cur.amtacctcr));
      cur.totalacctcr = new BigDecimal(subreport[0].totalacctcr).add(totalCredit).toString();
      subTotal = subTotal.add(new BigDecimal(cur.total));
      cur.totalacctsub = new BigDecimal(subreport[0].total).add(subTotal).toString();
      cur.previousdebit = subreport[0].totalacctdr;
      cur.previouscredit = subreport[0].totalacctcr;
      cur.previoustotal = subreport[0].total;
      strOld = cur.groupbyid + cur.id;

      Map<String, String> docTypeMap = new HashMap<String, String>();
      docTypeMap.put("GLJ", "GL Journal");
      docTypeMap.put("GLD", "GL Document");
      docTypeMap.put("API", "AP Invoice");
      docTypeMap.put("APP", "AP Payment");
      docTypeMap.put("ARI", "AR Invoice");
      docTypeMap.put("ARR", "AR Receipt");
      docTypeMap.put("SOO", "Sales Order");
      docTypeMap.put("ARF", "AR Pro Forma Invoice");
      docTypeMap.put("MMS", "Material Delivery");
      docTypeMap.put("MMR", "Material Receipt");
      docTypeMap.put("MMM", "Material Movement");
      docTypeMap.put("POO", "Purchase Order");
      docTypeMap.put("POR", "Purchase Requisition");
      docTypeMap.put("MMI", "Material Physical Inventory");
      docTypeMap.put("APC", "AP Credit Memo");
      docTypeMap.put("ARC", "AR Credit Memo");
      docTypeMap.put("CMB", "Bank Statement");
      docTypeMap.put("CMC", "Cash Journal");
      docTypeMap.put("CMA", "Payment Allocation");
      docTypeMap.put("MMP", "Material Production");
      docTypeMap.put("MXI", "Match Invoice");
      docTypeMap.put("MXP", "Match PO");
      docTypeMap.put("PJI", "Project Issue");
      docTypeMap.put("STT", "Settlement): Settlement of debtpayments");
      docTypeMap.put("STM", "Settlement manual");
      docTypeMap.put("AMZ", "Amortization");
      docTypeMap.put("FAT", "Financial Account Transaction");
      docTypeMap.put("ARRP", "AR Receivable Proposal");
      docTypeMap.put("REC", "Reconciliation");
      // --- (** New **)
      docTypeMap.put("DPM", "Debt Payment Management");
      docTypeMap.put("BSF", "Bank Statement File");
      docTypeMap.put("PPR", "Payment proposal");
      docTypeMap.put("APPP", "AP Payment Proposal");

      String factAcctId = cur.factAcctId;
      /*
       * if(factAcctId != null) { try {
       * 
       * ResultSet factRS = conn.getConnection().createStatement().executeQuery(
       * " select fact.ad_table_id,fact.record_id,fact.docbasetype,tab.tablename from fact_acct  fact"
       * + " join ad_table tab on tab.ad_table_id=fact.ad_table_id" + " where  fact.fact_acct_id='"
       * + factAcctId + "'"); String tableName = null, docbasetype = null, recordId = null;
       * 
       * while (factRS.next()) { tableName = factRS.getString(4); docbasetype = factRS.getString(3);
       * recordId = factRS.getString(2); }
       * 
       * factRS.close();
       * 
       * cur.transactiontype = docTypeMap.get(docbasetype);
       * 
       * //if(!tableName.equalsIgnoreCase("FIN_Finacc_Transaction")) {
       * 
       * ResultSet docRS = conn.getConnection().createStatement().executeQuery(
       * "select em_efin_acctseq from fact_acct where fact_acct_id='" + factAcctId + "'");
       * 
       * while (docRS.next()) { cur.accountseq = docRS.getString(1); } docRS.close();
       * 
       * //}
       * 
       * } catch (Exception e) { System.err.println("Exception in General ledger Report:" + e); } }
       */
      return data;
    }

  }

  private String getFamily(String strTree, String strChild) throws IOException, ServletException {
    return Tree.getMembers(this, strTree, strChild);
  }

  public static EfinReportGeneralLedgerData[] selectBpartnerFromUniqueCode(
      ConnectionProvider connectionProvider, String uniquecode) throws ServletException {
    return selectBpartnerFromUniqueCode(connectionProvider, uniquecode, 0, 0);
  }

  public static EfinReportGeneralLedgerData[] selectBpartnerFromUniqueCode(
      ConnectionProvider connectionProvider, String uniquecode, int firstRegister,
      int numberRegisters) throws ServletException {
    String strSql = "select bp.c_bpartner_id as id, bp.name from c_bpartner bp "
        + "         join c_validcombination acc on acc.c_bpartner_id = bp.c_bpartner_id "
        + "         where acc.em_efin_uniquecode ='" + uniquecode + "' and acc.ad_client_id = '"
        + OBContext.getOBContext().getCurrentClient().getId() + "'";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

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
        EfinReportGeneralLedgerData objectEfinReportGeneralLedgerData = new EfinReportGeneralLedgerData();
        objectEfinReportGeneralLedgerData.id = UtilSql.getValue(result, "id");
        objectEfinReportGeneralLedgerData.name = UtilSql.getValue(result, "name");
        vector.addElement(objectEfinReportGeneralLedgerData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch (SQLException e) {
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    EfinReportGeneralLedgerData objectEfinReportGeneralLedgerData[] = new EfinReportGeneralLedgerData[vector
        .size()];
    vector.copyInto(objectEfinReportGeneralLedgerData);
    return (objectEfinReportGeneralLedgerData);
  }

  @Override
  public String getServletInfo() {
    return "Servlet EfinReportGeneralLedger. This Servlet was made by Pablo Sarobe";
  } // end of getServletInfo() method
}
