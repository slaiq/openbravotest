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
package sa.elm.ob.finance.ad_reports.TrialBalance;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.AccountingSchemaMiscData;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.finance.properties.Resource;
import sa.elm.ob.utility.util.UtilityDAO;

public class ReportTrialBalance extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      if (vars.commandIn("DEFAULT")) {
        String strcAcctSchemaId = vars.getGlobalVariable("inpcAcctSchemaId",
            "ReportTrialBalance|cAcctSchemaId", "");
        String strcCampaignId = vars.getGlobalVariable("inpcCampaignId_IN",
            "ReportTrialBalance|cCampaignId", "");

        String strFromPeriod = vars.getGlobalVariable("inpcFromPeriodId",
            "ReportTrialBalance|cFromPeriodId", "");
        String strToPeriod = vars.getGlobalVariable("inpcToPeriodId",
            "ReportTrialBalance|cToPeriodId", "");
        String currentPeriodId = getCurrentPeriod(vars,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
        String strDateFrom = getDate(vars, strFromPeriod, null);
        String strDateTo = getDate(vars, null, strToPeriod);
        String strPageNo = vars.getGlobalVariable("inpPageNo", "ReportTrialBalance|PageNo", "1");
        String strOrg = vars.getGlobalVariable("inpOrg", "ReportTrialBalance|Org", "");
        String strLevel = vars.getGlobalVariable("inpLevel", "ReportTrialBalance|Level", "");
        String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
            "ReportTrialBalance|cBpartnerId", "", IsIDFilter.instance);
        String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
            "ReportTrialBalance|mProductId", "", IsIDFilter.instance);
        String strcProjectId = vars.getInGlobalVariable("inpcProjectId_IN",
            "ReportTrialBalance|cProjectId", "", IsIDFilter.instance);
        String strcSalesregionId = vars.getInGlobalVariable("inpcSalesregionId_IN",
            "ReportTrialBalance|cSalesregionId", "", IsIDFilter.instance);
        String strcActivityId = vars.getInGlobalVariable("inpcActivityId_IN",
            "ReportTrialBalance|cActivityId", "", IsIDFilter.instance);
        String strUser1Id = vars.getInGlobalVariable("inpcUser1Id_IN", "ReportTrialBalance|user1Id",
            "", IsIDFilter.instance);
        String strUser2Id = vars.getInGlobalVariable("inpcUser2Id_IN", "ReportTrialBalance|user2Id",
            "", IsIDFilter.instance);
        String strGroupBy = vars.getGlobalVariable("inpGroupBy", "ReportTrialBalance|GroupBy", "");
        String strcElementValueFrom = vars.getGlobalVariable("inpcElementValueIdFrom",
            "ReportTrialBalance|C_ElementValue_IDFROM", "");
        String strcElementValueTo = vars.getGlobalVariable("inpcElementValueIdTo",
            "ReportTrialBalance|C_ElementValue_IDTO", "");
        /*
         * String strcElementValueTo = vars.getGlobalVariable("inpcElementValueIdTo",
         * "ReportTrialBalance|C_ElementValue_IDTO", ReportTrialBalanceData.selectLastAccount(this,
         * Utility.getContext(this, vars, "#AccessibleOrgTree", "Account"), Utility.getContext(this,
         * vars, "#User_Client", "Account")));
         */
        String strNotInitialBalance = vars.getGlobalVariable("inpNotInitialBalance",
            "ReportTrialBalance|notInitialBalance", "Y");
        String strIncludeZeroFigures = vars.getGlobalVariable("inpIncludeZeroFigures",
            "ReportTrialBalance|includeZeroFigures", "");
        String strcElementValueFromDes = "", strcElementValueToDes = "";
        if (!strcElementValueFrom.equals(""))
          strcElementValueFromDes = ReportTrialBalanceData.selectSubaccountDescription(this,
              strcElementValueFrom);
        if (!strcElementValueTo.equals(""))
          strcElementValueToDes = ReportTrialBalanceData.selectSubaccountDescription(this,
              strcElementValueTo);
        strcElementValueFromDes = (strcElementValueFromDes == null) ? "" : strcElementValueFromDes;
        strcElementValueToDes = (strcElementValueToDes == null) ? "" : strcElementValueToDes;
        vars.setSessionValue("inpElementValueIdFrom_DES", strcElementValueFromDes);
        vars.setSessionValue("inpElementValueIdTo_DES", strcElementValueToDes);

        printPageDataSheet(response, vars, strFromPeriod, strToPeriod, strDateFrom, strDateTo,
            currentPeriodId, strPageNo, strOrg, strLevel, strcElementValueFrom, strcElementValueTo,
            strcElementValueFromDes, strcElementValueToDes, strcBpartnerId, strmProductId,
            strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id,
            strUser2Id, strcAcctSchemaId, strNotInitialBalance, strGroupBy, strIncludeZeroFigures);

      } else if (vars.commandIn("FIND")) {
        String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
            "ReportTrialBalance|cAcctSchemaId");
        String strcCampaignId = vars.getGlobalVariable("inpcCampaignId_IN",
            "ReportTrialBalance|cCampaignId", "");
        String strFromPeriod = vars.getRequestGlobalVariable("inpcFromPeriodId",
            "ReportTrialBalance|cFromPeriodId");
        String strToPeriod = vars.getRequestGlobalVariable("inpcToPeriodId",
            "ReportTrialBalance|cToPeriodId");
        String strDateFrom = getDate(vars, strFromPeriod, null);
        String strDateTo = getDate(vars, null, strToPeriod);
        String strPageNo = vars.getRequestGlobalVariable("inpPageNo", "ReportTrialBalance|PageNo");
        String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportTrialBalance|Org");
        String strLevel = vars.getRequestGlobalVariable("inpLevel", "ReportTrialBalance|Level");
        String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
            "ReportTrialBalance|cBpartnerId", IsIDFilter.instance);
        String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
            "ReportTrialBalance|mProductId", IsIDFilter.instance);
        String strcProjectId = vars.getRequestInGlobalVariable("inpcProjectId_IN",
            "ReportTrialBalance|cProjectId", IsIDFilter.instance);
        String strcSalesregionId = vars.getRequestInGlobalVariable("inpcSalesregionId_IN",
            "ReportTrialBalance|cSalesregionId", IsIDFilter.instance);
        String strcActivityId = vars.getRequestInGlobalVariable("inpcActivityId_IN",
            "ReportTrialBalance|cActivityId", IsIDFilter.instance);
        String strUser1Id = vars.getRequestInGlobalVariable("inpcUser1Id_IN",
            "ReportTrialBalance|user1Id", IsIDFilter.instance);
        String strUser2Id = vars.getRequestInGlobalVariable("inpcUser2Id_IN",
            "ReportTrialBalance|user2Id", IsIDFilter.instance);
        String strGroupBy = vars.getRequestGlobalVariable("inpGroupBy",
            "ReportTrialBalance|GroupBy");
        String strcElementValueFrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
            "ReportTrialBalance|C_ElementValue_IDFROM");
        String strcElementValueTo = vars.getRequestGlobalVariable("inpcElementValueIdTo",
            "ReportTrialBalance|C_ElementValue_IDTO");
        String strNotInitialBalance = vars.getStringParameter("inpNotInitialBalance", "N");
        vars.setSessionValue("ReportTrialBalance|notInitialBalance", strNotInitialBalance);
        String strIncludeZeroFigures = vars.getRequestGlobalVariable("inpIncludeZeroFigures",
            "ReportTrialBalance|includeZeroFigures");
        String strcElementValueFromDes = "", strcElementValueToDes = "";
        if (!strcElementValueFrom.equals(""))
          strcElementValueFromDes = ReportTrialBalanceData.selectSubaccountDescription(this,
              strcElementValueFrom);
        if (!strcElementValueTo.equals(""))
          strcElementValueToDes = ReportTrialBalanceData.selectSubaccountDescription(this,
              strcElementValueTo);
        vars.setSessionValue("inpElementValueIdFrom_DES", strcElementValueFromDes);
        vars.setSessionValue("inpElementValueIdTo_DES", strcElementValueToDes);

        printPageDataSheet(response, vars, strFromPeriod, strToPeriod, strDateFrom, strDateTo, null,
            strPageNo, strOrg, strLevel, strcElementValueFrom, strcElementValueTo,
            strcElementValueFromDes, strcElementValueToDes, strcBpartnerId, strmProductId,
            strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id,
            strUser2Id, strcAcctSchemaId, strNotInitialBalance, strGroupBy, strIncludeZeroFigures);

      } else if (vars.commandIn("PDF", "XLS")) {
        String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
            "ReportTrialBalance|cAcctSchemaId");
        String strcCampaignId = vars.getGlobalVariable("inpcCampaignId_IN",
            "ReportTrialBalance|cCampaignId", "");
        String strFromPeriod = vars.getRequestGlobalVariable("inpcFromPeriodId",
            "ReportTrialBalance|cFromPeriodId");
        String strToPeriod = vars.getRequestGlobalVariable("inpcToPeriodId",
            "ReportTrialBalance|cToPeriodId");
        String strDateFrom = getDate(vars, strFromPeriod, null);
        String strDateTo = getDate(vars, null, strToPeriod);
        // String strPageNo = vars.getRequestGlobalVariable("inpPageNo",
        // "ReportTrialBalance|PageNo");
        String strPageNo = vars.getGlobalVariable("inpPageNo", "ReportTrialBalance|PageNo", "1");

        String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportTrialBalance|Org");
        String strLevel = vars.getRequestGlobalVariable("inpLevel", "ReportTrialBalance|Level");
        String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
            "ReportTrialBalance|cBpartnerId", IsIDFilter.instance);
        String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
            "ReportTrialBalance|mProductId", IsIDFilter.instance);
        String strcProjectId = vars.getRequestInGlobalVariable("inpcProjectId_IN",
            "ReportTrialBalance|cProjectId", IsIDFilter.instance);
        String strcSalesregionId = vars.getRequestInGlobalVariable("inpcSalesregionId_IN",
            "ReportTrialBalance|cSalesregionId", IsIDFilter.instance);
        String strcActivityId = vars.getRequestInGlobalVariable("inpcActivityId_IN",
            "ReportTrialBalance|cActivityId", IsIDFilter.instance);
        String strUser1Id = vars.getRequestInGlobalVariable("inpcUser1Id_IN",
            "ReportTrialBalance|user1Id", IsIDFilter.instance);
        String strUser2Id = vars.getRequestInGlobalVariable("inpcUser2Id_IN",
            "ReportTrialBalance|user2Id", IsIDFilter.instance);
        String strGroupBy = vars.getRequestGlobalVariable("inpGroupBy",
            "ReportTrialBalance|GroupBy");
        String strcElementValueFrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
            "ReportTrialBalance|C_ElementValue_IDFROM");
        String strcElementValueTo = vars.getRequestGlobalVariable("inpcElementValueIdTo",
            "ReportTrialBalance|C_ElementValue_IDTO");
        String strNotInitialBalance = vars.getStringParameter("inpNotInitialBalance", "N");
        vars.setSessionValue("ReportTrialBalance|notInitialBalance", strNotInitialBalance);
        String strIncludeZeroFigures = vars.getRequestGlobalVariable("inpIncludeZeroFigures",
            "ReportTrialBalance|includeZeroFigures");
        String strcElementValueFromDes = "", strcElementValueToDes = "";
        if (!strcElementValueFrom.equals(""))
          strcElementValueFromDes = ReportTrialBalanceData.selectSubaccountDescription(this,
              strcElementValueFrom);
        if (!strcElementValueTo.equals(""))
          strcElementValueToDes = ReportTrialBalanceData.selectSubaccountDescription(this,
              strcElementValueTo);
        vars.setSessionValue("inpElementValueIdFrom_DES", strcElementValueFromDes);
        vars.setSessionValue("inpElementValueIdTo_DES", strcElementValueToDes);

        if (vars.commandIn("PDF"))
          printPageDataPDF(request, response, vars, strFromPeriod, strToPeriod, strDateFrom,
              strDateTo, strOrg, strLevel, strcElementValueFrom, strcElementValueFromDes,
              strcElementValueTo, strcElementValueToDes, strcBpartnerId, strmProductId,
              strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id,
              strUser2Id, strcAcctSchemaId, strNotInitialBalance, strGroupBy, strPageNo,
              strIncludeZeroFigures);
        else
          printPageDataXLS(request, response, vars, strFromPeriod, strToPeriod, strDateFrom,
              strDateTo, strOrg, strLevel, strcElementValueFrom, strcElementValueTo, strcBpartnerId,
              strmProductId, strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId,
              strUser1Id, strUser2Id, strcAcctSchemaId, strNotInitialBalance, strGroupBy,
              strIncludeZeroFigures);

      } else if (vars.commandIn("OPEN")) {
        String strAccountId = vars.getRequiredStringParameter("inpcAccountId");
        String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
            "ReportTrialBalance|cAcctSchemaId");
        String strcCampaignId = vars.getRequestGlobalVariable("inpcCampaignId_IN",
            "ReportTrialBalance|cCampaignId");
        String strFromPeriod = vars.getRequestGlobalVariable("inpcFromPeriodId",
            "ReportTrialBalance|cFromPeriodId");
        String strToPeriod = vars.getRequestGlobalVariable("inpcToPeriodId",
            "ReportTrialBalance|cToPeriodId");

        String strDateFrom = getDate(vars, strFromPeriod, null);
        String strDateTo = getDate(vars, null, strToPeriod);

        String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportTrialBalance|Org");
        String strLevel = vars.getRequestGlobalVariable("inpLevel", "ReportTrialBalance|Level");
        String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
            "ReportTrialBalance|cBpartnerId", "", IsIDFilter.instance);
        String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
            "ReportTrialBalance|mProductId", "", IsIDFilter.instance);
        String strcProjectId = vars.getInGlobalVariable("inpcProjectId_IN",
            "ReportTrialBalance|cProjectId", "", IsIDFilter.instance);
        String strcSalesregionId = vars.getInGlobalVariable("inpcSalesregionId_IN",
            "ReportTrialBalance|cSalesregionId", "", IsIDFilter.instance);

        String strcActivityId = vars.getInGlobalVariable("inpcActivityId_IN",
            "ReportTrialBalance|cActivityId", "", IsIDFilter.instance);
        String strUser1Id = vars.getInGlobalVariable("inpcUser1Id_IN", "ReportTrialBalance|user1Id",
            "", IsIDFilter.instance);
        String strUser2Id = vars.getInGlobalVariable("inpcUser2Id_IN", "ReportTrialBalance|user2Id",
            "", IsIDFilter.instance);
        String strGroupBy = vars.getRequestGlobalVariable("inpGroupBy",
            "ReportTrialBalance|GroupBy");
        String strNotInitialBalance = vars.getStringParameter("inpNotInitialBalance", "N");
        vars.setSessionValue("ReportTrialBalance|notInitialBalance", strNotInitialBalance);

        printPageOpen(response, vars, strFromPeriod, strToPeriod, strDateFrom, strDateTo, strOrg,
            strLevel, strcBpartnerId, strmProductId, strcProjectId, strcSalesregionId,
            strcCampaignId, strcActivityId, strUser1Id, strUser2Id, strcAcctSchemaId, strGroupBy,
            strAccountId, strNotInitialBalance);

      } else {
        pageError(response);
      }
    } catch (Exception e) {
      log4j.debug("error in Trial Balance Report", e);
    }

  }

  private void printPageOpen(HttpServletResponse response, VariablesSecureApp vars,
      String strFromPerId, String strToPerId, String strDateFrom, String strDateTo, String strOrg,
      String strLevel, String strcBpartnerId, String strmProductId, String strcProjectId,
      String strcSalesregionId, String strcCampaignId, String strcActivityId, String strUser1Id,
      String strUser2Id, String strcAcctSchemaId, String strGroupBy, String strAccountId,
      String strNotInitialBalance) throws IOException, ServletException {

    ReportTrialBalanceData[] data = null;
    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);

    // get the start date of selected From Period
    String strFrmPerStrDate = ReportTrialBalanceData.selectFrmPerStrtDate(this,
        Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
        Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"), strFromPerId);

    data = ReportTrialBalanceData.selectLines(this,
        Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strFrmPerStrDate,
        (strNotInitialBalance.equals("Y") ? "O" : null),
        (strNotInitialBalance.equals("Y") ? null : "O"),
        Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strOrgFamily,
        Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
        DateTimeData.nDaysAfter(this, strDateTo, "1"), strAccountId, strcBpartnerId, strmProductId,
        strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId, strUser1Id, strUser2Id,
        strcAcctSchemaId, strLevel, vars.getRole(),
        "BPartner".equals(strGroupBy) ? ", f.c_bpartner_id"
            : ("Product".equals(strGroupBy) ? ", f.m_product_id"
                : ("Project".equals(strGroupBy) ? ", f.c_project_id" : " ")),
        null, null);

    if (data == null) {
      data = ReportTrialBalanceData.set();
    }

    // response.setContentType("text/plain");
    response.setContentType("text/html; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();

    // Create JSON object
    // { "rows" : [ {"td1":"Bellen Ent.","td3":"0,00","td2":"0,00","td5":"-48,59","td4":"48,59"},
    // {"td1":"Mafalda Corporation","td3":"34,56","td2":"0,00","td5":"-334,79","td4":"369,35"}],
    // "config" : {"classDefault":"DataGrid_Body_Cell","classAmount":"DataGrid_Body_Cell_Amount"}
    // }
    DecimalFormat df = Utility.getFormat(vars, "euroInform");
    JSONObject table = new JSONObject();
    JSONArray tr = new JSONArray();
    Map<String, String> tds = null;
    try {

      for (int i = 0; i < data.length; i++) {
        tds = new HashMap<String, String>();
        tds.put("td1", data[i].groupbyname);
        tds.put("td2", df.format(new BigDecimal(data[i].saldoInicial)));
        tds.put("td3", df.format(new BigDecimal(data[i].amtacctdr)));
        tds.put("td4", df.format(new BigDecimal(data[i].amtacctcr)));
        tds.put("td5", df.format(new BigDecimal(data[i].saldoFinal)));
        tr.put(data.length - (i + 1), tds);
        table.put("rows", tr);
      }
      Map<String, String> props = new HashMap<String, String>();
      props.put("classAmount", "DataGrid_Body_Cell_Amount");
      props.put("classDefault", "DataGrid_Body_Cell");
      table.put("config", props);

    } catch (JSONException e) {
      log4j.error("Error creating JSON object for representing subaccount lines", e);
      throw new ServletException(e);
    }

    log4j.debug("JSON string: " + table.toString());

    out.println("jsonTable = " + table.toString());
    out.close();
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strFromPeriodId, String strToPeriodId, String strDateFrom, String strDateTo,
      String currentPeriodId, String strPageNo, String strOrg, String strLevel,
      String strcElementValueFrom, String strcElementValueTo, String strcElementValueFromDes,
      String strcElementValueToDes, String strcBpartnerId, String strmProductId,
      String strcProjectId, String strcSalesregionId, String strcCampaignId, String strcActivityId,
      String strUser1Id, String strUser2Id, String strcAcctSchemaId, String strNotInitialBalance,
      String strGroupBy, String strIncludeZeroFigures)
      throws IOException, ServletException, ParseException {

    String strMessage = "";
    XmlDocument xmlDocument = null;
    ReportTrialBalanceData[] data = null;
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    String discard[] = { "sectionGridView", "discard", "discard", "discard" };

    if (strLevel.equals("C")) {
      discard[1] = "fieldId1";
    } else {
      discard[1] = "fieldDescAccount";
    }

    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    String strTreeAccount = ReportTrialBalanceData.treeAccount(this, vars.getClient());
    // Remember values
    String strcBpartnerIdAux = strcBpartnerId;
    String strmProductIdAux = strmProductId;
    String strcProjectIdAux = strcProjectId;
    String strcSalesregionIdAux = strcSalesregionId;
    String strcActivityIdAux = strcActivityId;
    String strUser1IdAux = strUser1Id;
    String strUser2IdAux = strUser2Id;
    ReportTrialBalanceData[] dataOrg = null;

    String strAccountFromValue = ReportTrialBalanceData.selectAccountValue(this,
        strcElementValueFrom);
    String strAccountToValue = ReportTrialBalanceData.selectAccountValue(this, strcElementValueTo);

    log4j.debug("Output: DataSheet");
    log4j.debug("strTreeOrg: " + strTreeOrg + "strOrgFamily: " + strOrgFamily + "strTreeAccount: "
        + strTreeAccount);
    log4j.debug("strcBpartnerId: " + strcBpartnerId + "strmProductId: " + strmProductId
        + "strcProjectId: " + strcProjectId + "strcSalesregionId: " + strcSalesregionId
        + "strcCampaignId: " + strcCampaignId);

    if (strcSalesregionId != null && !strcSalesregionId.equals("")) {
      ReportTrialBalanceData[] count = ReportTrialBalanceData.selectC_SALESREGION_ID(this,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strcSalesregionId);
      for (int i = 0; i < count.length; i++) {
        if (new BigDecimal(count[i].count).compareTo(new BigDecimal(0)) > 0) {
          strcSalesregionId = null;
        }
      }
    }

    if (strDateFrom != null && strDateTo != null && strDateFrom.equals("")
        && strDateTo.equals("")) {
      xmlDocument = xmlEngine
          .readXmlTemplate("sa/elm/ob/finance/ad_reports/TrialBalance/ReportTrialBalance", discard)
          .createXmlDocument();
      data = ReportTrialBalanceData.set();
      if (vars.commandIn("FIND")) {
        strMessage = Utility.messageBD(this, "BothDatesCannotBeBlank", vars.getLanguage());
        log4j.warn("Both dates are blank");
      }
    } else {
      if (strLevel.equals("S")) { // SubAccount selected
        String strFrmPerStrDate = "", strStrDateFC = "";

        try {

          // get the start date of selected From Period
          strFrmPerStrDate = ReportTrialBalanceData.selectFrmPerStrtDate(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromPeriodId);
          strFrmPerStrDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmPerStrDate));
          log4j.debug("strFrmPerStrDate:" + strFrmPerStrDate);
          log4j.debug("strDateFrom:" + strDateFrom);
          log4j.debug("strDateTo:" + strDateTo);

          // Get the Last Opening and closing Balance Entry Date
          // If closing and opening is not happened then assign financial year start date
          strStrDateFC = ReportTrialBalanceData.selectLastOpeningBalanceDate(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromPeriodId);
          if (StringUtils.isNotEmpty(strStrDateFC)) {
            strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
            // strStrDateFC = DateTimeData.nDaysAfter(this, strStrDateFC, "1"); // send one day
            // after
            // opening balance
            // entry date as fiscal
            // calendar start date
          }
          // get start date of Fiscal Calendar
          else {
            strStrDateFC = ReportTrialBalanceData.selectStrtDateFC(this,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
            strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
          }

          log4j.debug("strStrDateFC:" + strStrDateFC);
        } catch (ParseException e) {
        }
        String strClientId = "("
            + Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance") + ")";
        log4j.debug("strClientId:" + strClientId);
        data = ReportTrialBalanceData.selectLines(this,
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strFrmPerStrDate,
            DateTimeData.nDaysAfter(this, strDateTo, "1"),
            (strNotInitialBalance.equals("Y") ? "O" : null),
            (strNotInitialBalance.equals("Y") ? null : "O"), strClientId, null, strcBpartnerId,
            strmProductId, strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId,
            strUser1Id, strUser2Id, strcAcctSchemaId, strOrgFamily,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            strAccountFromValue, strAccountToValue, strLevel, vars.getRole(), strStrDateFC);
        if (strGroupBy.equals(""))
          discard[2] = "showExpand";

      } else {
        discard[2] = "showExpand";
        data = getDataWhenNotSubAccount(vars, strDateFrom, strDateTo, strOrg, strOrgFamily,
            strcAcctSchemaId, strLevel, strTreeAccount, strNotInitialBalance);
      }

      if (data != null && data.length > 0)
        discard[0] = "discard";

    }

    xmlDocument = xmlEngine
        .readXmlTemplate("sa/elm/ob/finance/ad_reports/TrialBalance/ReportTrialBalance", discard)
        .createXmlDocument();

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "LIST", "",
          "C_ElementValue level", "",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ReportTrialBalance", "");
      xmlDocument.setData("reportLevel", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportTrialBalance", false, "", "",
        "if (validate()) { imprimir(); } return false;", false, "ad_reports", strReplaceWith, false,
        true);
    toolbar.setEmail(false);
    toolbar.prepareSimpleToolBarTemplate();
    toolbar.prepareRelationBarTemplate(false, false,
        "if (validate()) { submitCommandForm('XLS', false, frmMain, 'ReportTrialBalanceExcel.xls', 'EXCEL'); } return false;");
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "sa.elm.ob.finance.ad_reports.TrialBalance.ReportTrialBalance");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ReportTrialBalance.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ReportTrialBalance.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    OBError myMessage = vars.getMessage("ReportTrialBalance");
    vars.removeMessage("ReportTrialBalance");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    try {
      dataOrg = ReportTrialBalanceData.selectOrgValidation(this,
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalanceData"));
      String orgList = "";
      for (int i = 0; i < dataOrg.length; i++) {
        if (i == 0) {
          orgList = "'" + dataOrg[i].adOrgId + "'";
        } else {
          orgList += "," + "'" + dataOrg[i].adOrgId + "'";
        }
      }
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_ORG_ID", "",
          "", orgList, Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), '*');
      comboTableData.fillParameters(null, "ReportTrialBalance", "");
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setData("reportC_ACCTSCHEMA_ID", "liststructure",
        AccountingSchemaMiscData.selectC_ACCTSCHEMA_ID(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
            strcAcctSchemaId));
    if (vars.commandIn("DEFAULT")) {
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure",
          ReportTrialBalanceData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              currentPeriodId));
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure",
          ReportTrialBalanceData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "EfinReportGeneralLedger"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              currentPeriodId));
    } else {
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure",
          ReportTrialBalanceData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromPeriodId));
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure",
          ReportTrialBalanceData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strToPeriodId));
    }
    xmlDocument.setData("reportCCampaignId_IN", "liststructure",
        ReportTrialBalanceData.selectC_CAMPAIGN_ID(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strcCampaignId,
            vars.getRole()));

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    if (strDateFrom != null) {
      try {
        xmlDocument.setParameter("dateFrom",
            new SimpleDateFormat("dd-MM-yyyy").format(new SimpleDateFormat("dd-MM-yyyy")
                .parse(UtilityDAO.convertToHijriDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new SimpleDateFormat("dd-MM-yyyy").parse(strDateFrom))))));
      } catch (ParseException e) {
      }
    }
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    if (strDateTo != null) {
      try {
        xmlDocument.setParameter("dateTo",
            new SimpleDateFormat("dd-MM-yyyy").format(new SimpleDateFormat("dd-MM-yyyy")
                .parse(UtilityDAO.convertToHijriDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new SimpleDateFormat("dd-MM-yyyy").parse(strDateTo))))));
      } catch (ParseException e) {
      }
    }

    xmlDocument.setParameter("PageNo", strPageNo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("adOrgId", strOrg);
    xmlDocument.setParameter("Level", "".equals(strLevel) ? "S" : strLevel);
    xmlDocument.setParameter("cAcctschemaId", strcAcctSchemaId);
    if (vars.commandIn("DEFAULT")) {
      xmlDocument.setParameter("cFromPeriodId", currentPeriodId);
      xmlDocument.setParameter("cToPeriodId", currentPeriodId);
    } else {
      xmlDocument.setParameter("cFromPeriodId", strFromPeriodId);
      xmlDocument.setParameter("cToPeriodId", strToPeriodId);
    }
    xmlDocument.setParameter("cCampaignId", strcCampaignId);
    xmlDocument.setParameter("paramElementvalueIdFrom", strcElementValueFrom);
    xmlDocument.setParameter("paramElementvalueIdTo", strcElementValueTo);
    xmlDocument.setParameter("inpElementValueIdFrom_DES", strcElementValueFromDes);
    xmlDocument.setParameter("inpElementValueIdTo_DES", strcElementValueToDes);
    xmlDocument.setParameter("paramFromDate", strDateFrom);
    xmlDocument.setParameter("paramToDate", strDateTo);
    xmlDocument.setParameter("inpElementValueIdTo_DES", strcElementValueToDes);
    xmlDocument.setParameter("paramMessage",
        (strMessage.equals("") ? "" : "alert('" + strMessage + "');"));
    xmlDocument.setParameter("groupbyselected", strGroupBy);
    xmlDocument.setParameter("notInitialBalance", strNotInitialBalance);
    xmlDocument.setParameter("paramZeroFigures", strIncludeZeroFigures);
    xmlDocument.setParameter("paramToPeriodName",
        ReportTrialBalanceData.selectPeriodName(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strToPeriodId));
    xmlDocument.setParameter("strFromPeriod",
        Resource.getProperty("finance.g/lreport.fromperiod", vars.getLanguage()));
    xmlDocument.setParameter("strToPeriod",
        Resource.getProperty("finance.g/lreport.toperiod", vars.getLanguage()));
    xmlDocument.setParameter("strorganization",
        Resource.getProperty("finance.organization", vars.getLanguage()));
    xmlDocument.setParameter("strgeneralledger",
        Resource.getProperty("finance.generalledger", vars.getLanguage()));
    xmlDocument.setParameter("strFromAccount",
        Resource.getProperty("finance.g/lreport.fromaccount", vars.getLanguage()));
    xmlDocument.setParameter("strToAccount",
        Resource.getProperty("finance.g/lreport.toaccount", vars.getLanguage()));
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
    xmlDocument.setParameter("acctlevel",
        Resource.getProperty("finance.trialbalance.acctlevel", vars.getLanguage()));
    xmlDocument.setParameter("additionalfilter",
        Resource.getProperty("finance.advancedfilters", vars.getLanguage()));
    xmlDocument.setParameter("ytdas",
        Resource.getProperty("finance.trialbalance.ytdasof", vars.getLanguage()));
    xmlDocument.setParameter("dr", Resource.getProperty("finance.ptd.dr", vars.getLanguage()));
    xmlDocument.setParameter("cr", Resource.getProperty("finance.ptd.cr", vars.getLanguage()));
    xmlDocument.setParameter("net", Resource.getProperty("finance.ptd.net", vars.getLanguage()));

    ReportTrialBalanceData[] strDefRegionId = ReportTrialBalanceData.selectC_SALESREGION_ID(this,
        Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
        Utility.getContext(this, vars, "#User_Client", ""), null);
    for (int i = 0; i < strDefRegionId.length; i++) {
      xmlDocument.setParameter("paramDefRegion", strDefRegionId[0].defregion);
    }

    xmlDocument.setData("reportCBPartnerId_IN", "liststructure",
        SelectorUtilityData.selectBpartner(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcBpartnerIdAux));

    xmlDocument.setData("reportMProductId_IN", "liststructure",
        SelectorUtilityData.selectMproduct(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strmProductIdAux));

    xmlDocument.setData("reportCProjectId_IN", "liststructure",
        SelectorUtilityData.selectProject(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcProjectIdAux));

    xmlDocument.setData("reportCSalesRegionId_IN", "liststructure",
        ReportTrialBalanceData.selectCsalesRegion(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcSalesregionIdAux));

    // xmlDocument.setData("reportCCampaignId_IN", "liststructure",
    // ReportTrialBalanceData.selectCCampaign(this, Utility.getContext(this, vars,
    // "#AccessibleOrgTree", ""), Utility.getContext(this, vars, "#User_Client", ""),
    // strcCampaignIdAux));
    xmlDocument.setData("reportCActivityId_IN", "liststructure",
        ReportTrialBalanceData.selectCActivity(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strcActivityIdAux));
    xmlDocument.setData("reportCUser1Id_IN", "liststructure",
        ReportTrialBalanceData.selectUser1(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strUser1IdAux));
    xmlDocument.setData("reportCUser2Id_IN", "liststructure",
        ReportTrialBalanceData.selectUser2(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(this, vars, "#User_Client", ""), strUser2IdAux));
    if (vars.commandIn("FIND")) {
      SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
      Date formatdate = in.parse(strDateFrom);
      Date todate = in.parse(strDateTo);
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
      if ("Y".equals(strIncludeZeroFigures) && "S".equals(strLevel))
        data = includeZeroFigures(data, strcAcctSchemaId, strOrg, strAccountFromValue,
            strAccountToValue);
      xmlDocument.setData("structure1", data);
    } else {
      if (vars.commandIn("FIND")) {
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

  private void printPageDataXLS(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFromperiodId, String ToPeriodId, String strDateFrom,
      String strDateTo, String strOrg, String strLevel, String strcElementValueFrom,
      String strcElementValueTo, String strcBpartnerId, String strmProductId, String strcProjectId,
      String strcSalesregionId, String strcCampaignId, String strcActivityId, String strUser1Id,
      String strUser2Id, String strcAcctSchemaId, String strNotInitialBalance, String strGroupBy,
      String strIncludeZeroFigures) throws IOException, ServletException, ParseException {

    response.setContentType("text/html; charset=UTF-8");
    ReportTrialBalanceData[] data = null;
    boolean showbpartner = false;
    boolean showproduct = false;
    boolean showProject = false;
    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    String strTreeAccount = ReportTrialBalanceData.treeAccount(this, vars.getClient());
    String strAccountFromValue = ReportTrialBalanceData.selectAccountValue(this,
        strcElementValueFrom);
    String strAccountToValue = ReportTrialBalanceData.selectAccountValue(this, strcElementValueTo);

    log4j.debug("Output: XLS report");
    log4j.debug("strTreeOrg: " + strTreeOrg + "strOrgFamily: " + strOrgFamily + "strTreeAccount: "
        + strTreeAccount);
    log4j.debug("strcBpartnerId: " + strcBpartnerId + "strmProductId: " + strmProductId
        + "strcProjectId: " + strcProjectId);

    if (!strDateFrom.equals("") && !strDateTo.equals("") && !strOrg.equals("")
        && !strcAcctSchemaId.equals("")) {

      if (strLevel.equals("S")) {

        String strFrmPerStrDate = "", strStrDateFC = "";

        try {
          // get the start date of selected From Period
          strFrmPerStrDate = ReportTrialBalanceData.selectFrmPerStrtDate(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromperiodId);
          strFrmPerStrDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmPerStrDate));
          log4j.debug("strFrmPerStrDate:" + strFrmPerStrDate);
          log4j.debug("strDateFrom:" + strDateFrom);
          log4j.debug("strDateTo:" + strDateTo);
          // // get start date of Fiscal Calendar
          // strStrDateFC = ReportTrialBalanceData.selectStrtDateFC(this,
          // Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          // Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
          // strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
          // .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));

          // Get the Last Opening and closing Balance Entry Date
          // If closing and opening is not happened then assign financial year start date
          strStrDateFC = ReportTrialBalanceData.selectLastOpeningBalanceDate(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromperiodId);
          if (StringUtils.isNotEmpty(strStrDateFC)) {
            strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
            // strStrDateFC = DateTimeData.nDaysAfter(this, strStrDateFC, "1"); // send one day
            // after
            // opening balance
            // entry date as fiscal
            // calendar start date
          }
          // get start date of Fiscal Calendar
          else {
            strStrDateFC = ReportTrialBalanceData.selectStrtDateFC(this,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
            strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
          }

          log4j.debug("strStrDateFC:" + strStrDateFC);
        } catch (ParseException e) {
        }

        String strClientId = "("
            + Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance") + ")";
        data = ReportTrialBalanceData.selectLines(this,
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strFrmPerStrDate,
            DateTimeData.nDaysAfter(this, strDateTo, "1"),
            (strNotInitialBalance.equals("Y") ? "O" : null),
            (strNotInitialBalance.equals("Y") ? null : "O"), strClientId, null, strcBpartnerId,
            strmProductId, strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId,
            strUser1Id, strUser2Id, strcAcctSchemaId, strOrgFamily,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            strAccountFromValue, strAccountToValue, strLevel, vars.getRole(), strStrDateFC);
      } else {
        data = getDataWhenNotSubAccount(vars, strDateFrom, strDateTo, strOrg, strOrgFamily,
            strcAcctSchemaId, strLevel, strTreeAccount, strNotInitialBalance);
      }

      if (data == null || data.length == 0) {
        advisePopUp(request, response, "WARNING",
            Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
            Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
      } else if (data.length > 65532) {
        advisePopUp(request, response, "ERROR",
            Utility.messageBD(this, "ProcessStatus-E", vars.getLanguage()),
            Utility.messageBD(this, "numberOfRowsExceeded", vars.getLanguage()));
      } else {
        if ("Y".equals(strIncludeZeroFigures) && "S".equals(strLevel)) {
          data = includeZeroFigures(data, strcAcctSchemaId, strOrg, strAccountFromValue,
              strAccountToValue);
        }

        AcctSchema acctSchema = OBDal.getInstance().get(AcctSchema.class, strcAcctSchemaId);

        String strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/TrialBalance/ReportTrialBalanceExcel.jrxml";

        HashMap<String, Object> parameters = new HashMap<String, Object>();

        StringBuilder strSubTitle = new StringBuilder();

        SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");

        Date formatdate = in.parse(strDateTo);
        String to_date = out.format(formatdate);

        String hijiridate = UtilityDAO.convertToHijriDate(to_date);

        strSubTitle.append(Utility.messageBD(this, "LegalEntity", vars.getLanguage()) + ": ");
        strSubTitle.append(ReportTrialBalanceData.selectCompany(this, vars.getClient()) + " (");
        strSubTitle.append(Utility.messageBD(this, "ACCS_AD_ORG_ID_D", vars.getLanguage()) + ": ");
        strSubTitle.append(ReportTrialBalanceData.selectOrgName(this, strOrg) + ") \n");

        strSubTitle.append(Utility.messageBD(this, "asof", vars.getLanguage()) + ": "
            + hijiridate.substring(0, 10) + " \n");
        strSubTitle.append(Utility.messageBD(this, "generalLedger", vars.getLanguage()) + ": "
            + acctSchema.getName());

        parameters.put("REPORT_SUBTITLE", strSubTitle.toString());
        parameters.put("SHOWTOTALS", false);
        parameters.put("SHOWBPARTNER", showbpartner);
        parameters.put("SHOWPRODUCT", showproduct);
        parameters.put("SHOWPROJECT", showProject);
        parameters.put("DATE_FROM", strDateFrom);
        parameters.put("DATE_TO", strDateTo);

        renderJR(vars, response, strReportName, "xls", parameters, data, null);
      }
    } else {
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
    }

  }

  private void printPageDataPDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFromperiodId, String strToPeriodId, String strDateFrom,
      String strDateTo, String strOrg, String strLevel, String strcElementValueFrom,
      String strcElementValueFromDes, String strcElementValueTo, String strcElementValueToDes,
      String strcBpartnerId, String strmProductId, String strcProjectId, String strcSalesregionId,
      String strcCampaignId, String strcActivityId, String strUser1Id, String strUser2Id,
      String strcAcctSchemaId, String strNotInitialBalance, String strGroupBy, String strPageNo,
      String strIncludeZeroFigures) throws IOException, ServletException, ParseException {

    response.setContentType("text/html; charset=UTF-8");
    ReportTrialBalanceData[] data = null;
    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    String strTreeAccount = ReportTrialBalanceData.treeAccount(this, vars.getClient());
    boolean strIsSubAccount = false;

    String strAccountFromValue = ReportTrialBalanceData.selectAccountValue(this,
        strcElementValueFrom);
    String strAccountToValue = ReportTrialBalanceData.selectAccountValue(this, strcElementValueTo);

    log4j.debug("Output: PDF report");
    log4j.debug("strTreeOrg: " + strTreeOrg + "strOrgFamily: " + strOrgFamily + "strTreeAccount: "
        + strTreeAccount);
    log4j.debug("strcBpartnerId: " + strcBpartnerId + "strmProductId: " + strmProductId
        + "strcProjectId: " + strcProjectId);

    if (!strDateFrom.equals("") && !strDateTo.equals("") && !strOrg.equals("")
        && !strcAcctSchemaId.equals("")) {

      if (strLevel.equals("S")) {
        String strFrmPerStrDate = "", strStrDateFC = "";

        try {
          // get the start date of selected From Period
          strFrmPerStrDate = ReportTrialBalanceData.selectFrmPerStrtDate(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromperiodId);
          strFrmPerStrDate = new SimpleDateFormat("dd-MM-yyyy")
              .format(new SimpleDateFormat("yyyy-MM-dd").parse(strFrmPerStrDate));
          // // get start date of Fiscal Calendar
          // strStrDateFC = ReportTrialBalanceData.selectStrtDateFC(this,
          // Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          // Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
          // strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
          // .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));

          // Get the Last Opening and closing Balance Entry Date
          // If closing and opening is not happened then assign financial year start date
          strStrDateFC = ReportTrialBalanceData.selectLastOpeningBalanceDate(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromperiodId);
          if (StringUtils.isNotEmpty(strStrDateFC)) {
            strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
            // strStrDateFC = DateTimeData.nDaysAfter(this, strStrDateFC, "1"); // send one day
            // after
            // opening balance
            // entry date as fiscal
            // calendar start date
          }
          // get start date of Fiscal Calendar
          else {
            strStrDateFC = ReportTrialBalanceData.selectStrtDateFC(this,
                Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
                Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
            strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
          }

        } catch (ParseException e) {
        }
        String strClientId = "("
            + Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance") + ")";
        data = ReportTrialBalanceData.selectLines(this,
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strFrmPerStrDate,
            DateTimeData.nDaysAfter(this, strDateTo, "1"),
            (strNotInitialBalance.equals("Y") ? "O" : null),
            (strNotInitialBalance.equals("Y") ? null : "O"), strClientId, null, strcBpartnerId,
            strmProductId, strcProjectId, strcSalesregionId, strcCampaignId, strcActivityId,
            strUser1Id, strUser2Id, strcAcctSchemaId, strOrgFamily,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            strAccountFromValue, strAccountToValue, strLevel, vars.getRole(), strStrDateFC);
        if (!strGroupBy.equals(""))
          strIsSubAccount = true;

      } else {
        data = getDataWhenNotSubAccount(vars, strDateFrom, strDateTo, strOrg, strOrgFamily,
            strcAcctSchemaId, strLevel, strTreeAccount, strNotInitialBalance);
      }
      if (data == null || data.length == 0) {
        advisePopUp(request, response, "WARNING",
            Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
            Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
      } else {
        if ("Y".equals(strIncludeZeroFigures) && "S".equals(strLevel)) {
          data = includeZeroFigures(data, strcAcctSchemaId, strOrg, strAccountFromValue,
              strAccountToValue);
        }

        AcctSchema acctSchema = OBDal.getInstance().get(AcctSchema.class, strcAcctSchemaId);

        String strLanguage = vars.getLanguage();
        String strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/TrialBalance/ReportTrialBalancePDF.jrxml";
        HashMap<String, Object> parameters = new HashMap<String, Object>();

        parameters.put("TOTAL", Utility.messageBD(this, "Total", strLanguage));
        StringBuilder strSubTitle = new StringBuilder();

        // in report to print date as hijiri
        SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");

        Date formatdate = in.parse(strDateTo);
        String to_date = out.format(formatdate);

        String hijiridate = UtilityDAO.convertToHijriDate(to_date);

        Date date = new Date();
        String currentdate = UtilityDAO.convertToHijriDate(out.format(date));

        strSubTitle.append(Utility.messageBD(this, "LegalEntity", vars.getLanguage()) + ": ");
        strSubTitle.append(ReportTrialBalanceData.selectCompany(this, vars.getClient()) + " \n");
        strSubTitle.append(Utility.messageBD(this, "asof", vars.getLanguage()) + ": "
            + hijiridate.substring(0, 10) + " \n");

        if (!("0".equals(strOrg)))
          strSubTitle.append(Utility.messageBD(this, "ACCS_AD_ORG_ID_D", vars.getLanguage()) + ": "
              + ReportTrialBalanceData.selectOrgName(this, strOrg) + " \n");

        strSubTitle.append(Utility.messageBD(this, "generalLedger", vars.getLanguage()) + ": "
            + acctSchema.getName());

        parameters.put("REPORT_SUBTITLE", strSubTitle.toString());

        parameters.put("DEFAULTVIEW", !strIsSubAccount);
        parameters.put("SUBACCOUNTVIEW", strIsSubAccount);
        parameters.put("DUMMY", true);
        parameters.put("PageNo", strPageNo);
        parameters.put("CURRENT_DATE", currentdate);
        parameters.put("DATE_FROM", strDateFrom);
        parameters.put("DATE_TO", strDateTo);
        parameters.put("PAGEOF", Utility.messageBD(this, "PageOfNumber", vars.getLanguage()));

        renderJR(vars, response, strReportName, "pdf", parameters, data, null);
      }

    } else {
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
    }

  }

  private ReportTrialBalanceData[] getDataWhenNotSubAccount(VariablesSecureApp vars,
      String strDateFrom, String strDateTo, String strOrg, String strOrgFamily,
      String strcAcctSchemaId, String strLevel, String strTreeAccount, String strNotInitialBalance)
      throws IOException, ServletException {
    ReportTrialBalanceData[] data = null;
    ReportTrialBalanceData[] dataAux = null;

    if (strDateFrom != null && strDateTo != null) {
      dataAux = ReportTrialBalanceData.select(this,
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strDateFrom,
          strDateTo, strOrg, strTreeAccount, strcAcctSchemaId,
          strNotInitialBalance.equals("Y") ? "O" : "P", strOrgFamily,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"), strDateFrom,
          DateTimeData.nDaysAfter(this, strDateTo, "1"), "", "");
      ReportTrialBalanceData[] dataInitialBalance = ReportTrialBalanceData.selectInitialBalance(
          this, strDateFrom, strcAcctSchemaId, "", "", "", strOrgFamily,
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
          strNotInitialBalance.equals("Y") ? "initial" : "notinitial",
          strNotInitialBalance.equals("Y") ? "initial" : "notinitial");

      log4j.debug("Calculating tree...");
      dataAux = calculateTree(dataAux, null, new Vector<Object>(), dataInitialBalance,
          strNotInitialBalance);
      dataAux = levelFilter(dataAux, null, false, strLevel);
      dataAux = dataFilter(dataAux);

      log4j.debug("Tree calculated");

      if (dataAux != null && dataAux.length > 0) {
        data = filterTree(dataAux, strLevel);
        Arrays.sort(data, new ReportTrialBalanceDataComparator());
        for (int i = 0; i < data.length; i++) {
          data[i].rownum = "" + i;
        }
      } else {
        data = dataAux;
      }
    }
    return data;

  }

  private ReportTrialBalanceData[] filterTree(ReportTrialBalanceData[] data, String strLevel) {
    ArrayList<Object> arrayList = new ArrayList<Object>();
    for (int i = 0; data != null && i < data.length; i++) {
      if (data[i].elementlevel.equals(strLevel))
        arrayList.add(data[i]);
    }
    ReportTrialBalanceData[] new_data = new ReportTrialBalanceData[arrayList.size()];
    arrayList.toArray(new_data);
    return new_data;
  }

  private ReportTrialBalanceData[] calculateTree(ReportTrialBalanceData[] data, String indice,
      Vector<Object> vecTotal, ReportTrialBalanceData[] dataIB, String strNotInitialBalance) {
    if (data == null || data.length == 0)
      return data;
    if (indice == null)
      indice = "0";
    ReportTrialBalanceData[] result = null;
    Vector<Object> vec = new Vector<Object>();
    // if (log4j.isDebugEnabled())
    // log4j.debug("ReportTrialBalanceData.calculateTree() - data: " +
    // data.length);
    if (vecTotal == null)
      vecTotal = new Vector<Object>();
    if (vecTotal.size() == 0) {
      vecTotal.addElement("0");
      vecTotal.addElement("0");
      vecTotal.addElement("0");
      vecTotal.addElement("0");
    }
    BigDecimal totalDR = new BigDecimal((String) vecTotal.elementAt(0));
    BigDecimal totalCR = new BigDecimal((String) vecTotal.elementAt(1));
    BigDecimal totalInicial = new BigDecimal((String) vecTotal.elementAt(2));
    BigDecimal totalFinal = new BigDecimal((String) vecTotal.elementAt(3));
    boolean encontrado = false;
    for (int i = 0; i < data.length; i++) {
      if (data[i].parentId.equals(indice)) {
        encontrado = true;
        Vector<Object> vecParcial = new Vector<Object>();
        vecParcial.addElement("0");
        vecParcial.addElement("0");
        vecParcial.addElement("0");
        vecParcial.addElement("0");
        ReportTrialBalanceData[] dataChilds = calculateTree(data, data[i].id, vecParcial, dataIB,
            strNotInitialBalance);
        BigDecimal parcialDR = new BigDecimal((String) vecParcial.elementAt(0));
        BigDecimal parcialCR = new BigDecimal((String) vecParcial.elementAt(1));
        BigDecimal parcialInicial = new BigDecimal((String) vecParcial.elementAt(2));
        data[i].amtacctdr = (new BigDecimal(data[i].amtacctdr).add(parcialDR)).toPlainString();
        data[i].amtacctcr = (new BigDecimal(data[i].amtacctcr).add(parcialCR)).toPlainString();
        data[i].saldoInicial = (new BigDecimal(data[i].saldoInicial).add(parcialInicial))
            .toPlainString();
        // Edit how the final balance is calculated
        data[i].saldoFinal = (new BigDecimal(data[i].saldoInicial).add(parcialDR)
            .subtract(parcialCR)).toPlainString();

        // Set calculated Initial Balances
        for (int k = 0; k < dataIB.length; k++) {
          if (dataIB[k].accountId.equals(data[i].id)) {
            if (strNotInitialBalance.equals("Y")) {
              data[i].saldoInicial = (new BigDecimal(dataIB[k].saldoInicial).add(parcialInicial))
                  .toPlainString();
            } else {
              data[i].amtacctdr = (new BigDecimal(dataIB[k].amtacctdr).add(parcialDR)
                  .add(new BigDecimal(data[i].amtacctdr))).toPlainString();
              data[i].amtacctcr = (new BigDecimal(dataIB[k].amtacctcr).add(parcialCR)
                  .add(new BigDecimal(data[i].amtacctcr))).toPlainString();
            }
            data[i].saldoFinal = (new BigDecimal(dataIB[k].saldoInicial).add(parcialDR)
                .subtract(parcialCR)).toPlainString();
          }
        }

        totalDR = totalDR.add(new BigDecimal(data[i].amtacctdr));
        totalCR = totalCR.add(new BigDecimal(data[i].amtacctcr));
        totalInicial = totalInicial.add(new BigDecimal(data[i].saldoInicial));
        totalFinal = totalFinal.add(new BigDecimal(data[i].saldoFinal));

        vec.addElement(data[i]);
        if (dataChilds != null && dataChilds.length > 0) {
          for (int j = 0; j < dataChilds.length; j++)
            vec.addElement(dataChilds[j]);
        }
      } else if (encontrado)
        break;
    }
    vecTotal.set(0, totalDR.toPlainString());
    vecTotal.set(1, totalCR.toPlainString());
    vecTotal.set(2, totalInicial.toPlainString());
    vecTotal.set(3, totalFinal.toPlainString());
    result = new ReportTrialBalanceData[vec.size()];
    vec.copyInto(result);
    return result;
  }

  /**
   * Filters positions with amount credit, amount debit, initial balance and final balance distinct
   * to zero.
   * 
   * @param data
   * @return ReportTrialBalanceData array filtered.
   */
  private ReportTrialBalanceData[] dataFilter(ReportTrialBalanceData[] data) {
    if (data == null || data.length == 0)
      return data;
    Vector<Object> dataFiltered = new Vector<Object>();
    for (int i = 0; i < data.length; i++) {
      if (new BigDecimal(data[i].amtacctdr).compareTo(BigDecimal.ZERO) != 0
          || new BigDecimal(data[i].amtacctcr).compareTo(BigDecimal.ZERO) != 0
          || new BigDecimal(data[i].saldoInicial).compareTo(BigDecimal.ZERO) != 0
          || new BigDecimal(data[i].saldoFinal).compareTo(BigDecimal.ZERO) != 0) {
        dataFiltered.addElement(data[i]);
      }
    }
    ReportTrialBalanceData[] result = new ReportTrialBalanceData[dataFiltered.size()];
    dataFiltered.copyInto(result);
    return result;
  }

  private ReportTrialBalanceData[] levelFilter(ReportTrialBalanceData[] data, String indice,
      boolean found, String strLevel) {
    if (data == null || data.length == 0 || strLevel == null || strLevel.equals(""))
      return data;
    ReportTrialBalanceData[] result = null;
    Vector<Object> vec = new Vector<Object>();
    // if (log4j.isDebugEnabled())
    // log4j.debug("ReportTrialBalanceData.levelFilter() - data: " +
    // data.length);

    if (indice == null)
      indice = "0";
    for (int i = 0; i < data.length; i++) {
      if (data[i].parentId.equals(indice)
          && (!found || data[i].elementlevel.equalsIgnoreCase(strLevel))) {
        ReportTrialBalanceData[] dataChilds = levelFilter(data, data[i].id,
            (found || data[i].elementlevel.equals(strLevel)), strLevel);
        vec.addElement(data[i]);
        if (dataChilds != null && dataChilds.length > 0)
          for (int j = 0; j < dataChilds.length; j++)
            vec.addElement(dataChilds[j]);
      }
    }
    result = new ReportTrialBalanceData[vec.size()];
    vec.copyInto(result);
    vec.clear();
    return result;
  }

  private String getFamily(String strTree, String strChild) throws IOException, ServletException {
    return Tree.getMembers(this, strTree, strChild);
  }

  public String getDate(VariablesSecureApp vars, String strFromPeriodId, String strToPeriodId)
      throws IOException, ServletException {
    ReportTrialBalanceData[] data = null;
    String strDate = null;
    if (strFromPeriodId != null) {
      data = ReportTrialBalanceData.selectFromToDate(this,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strFromPeriodId);
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          strDate = data[i].startdate;
        }

      }
    }
    if (strToPeriodId != null) {
      data = ReportTrialBalanceData.selectFromToDate(this,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strToPeriodId);
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
    }
    return CurrentPeriodId;
  }

  /**
   * Adds zero figures (all CoA) to the report data
   * 
   * @param data
   * @param strcAcctSchemaId
   * @param strOrg
   * @return ReportTrialBalanceData array with zero figures.
   * @throws ServletException
   */
  private ReportTrialBalanceData[] includeZeroFigures(ReportTrialBalanceData[] data,
      String strcAcctSchemaId, String strOrg, String strAccountFromValue, String strAccountToValue)
      throws ServletException {
    ReportTrialBalanceData[] dataZeroFigures = null;
    ReportTrialBalanceData[] dataAccountCombinations = ReportTrialBalanceData
        .selectAccountCombinations(this, strcAcctSchemaId, strAccountFromValue, strAccountToValue);
    if (dataAccountCombinations.length > 0) {
      Vector<Object> vec = new Vector<Object>();
      List<String> dataAccounts = new ArrayList<String>(data.length);
      for (int i = 0; i < data.length; i++) {
        dataAccounts.add(data[i].id);
      }
      int j = 0;
      int extra = 0;
      for (int i = 0; i < dataAccountCombinations.length; i++) {
        String accountId = dataAccountCombinations[i].id;
        if (dataAccounts.contains(accountId)) {
          int lastAccountMatch = dataAccounts.lastIndexOf(accountId);
          int extraCount = 0;
          for (int k = j; k <= lastAccountMatch; k++) {
            vec.addElement(data[k]);
            j++;
            if (extraCount > 0)
              extra++;
            extraCount++;
          }
        } else {
          ReportTrialBalanceData[] dataProcess = ReportTrialBalanceData.set();
          dataProcess[0].accountId = dataAccountCombinations[i].accountId;
          dataProcess[0].amtacctcr = "0";
          dataProcess[0].amtacctdr = "0";
          dataProcess[0].id = dataAccountCombinations[i].id;
          dataProcess[0].name = dataAccountCombinations[i].name;
          dataProcess[0].saldoFinal = "0";
          dataProcess[0].saldoInicial = "0";
          vec.addElement(dataProcess[0]);
        }
      }
      dataZeroFigures = new ReportTrialBalanceData[dataAccountCombinations.length + extra];
      vec.copyInto(dataZeroFigures);

    }
    return dataZeroFigures;
  }

  public String getServletInfo() {
    return "Servlet ReportTrialBalance. This Servlet was made by Eduardo Argal and mirurita";
  }
}
