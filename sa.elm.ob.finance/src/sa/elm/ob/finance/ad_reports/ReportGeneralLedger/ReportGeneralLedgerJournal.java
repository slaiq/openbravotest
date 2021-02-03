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
package sa.elm.ob.finance.ad_reports.ReportGeneralLedger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesHistory;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.AccountingSchemaMiscData;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.finance.properties.Resource;
import sa.elm.ob.utility.util.UtilityDAO;

public class ReportGeneralLedgerJournal extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String tempDefault = "";

  /**
   * Keeps a comma-separated list of the accounting entries that has been shown, from the newest one
   * to the oldest one. Used for navigation purposes
   */
  private static final String PREVIOUS_ACCTENTRIES = "ReportGeneralLedgerJournal.previousAcctEntries";
  private static final String PREVIOUS_ACCTENTRIES_OLD = "ReportGeneralLedgerJournal.previousAcctEntriesOld";

  /**
   * Keeps a comma-separated list of the line's range that has been shown, from the newest one to
   * the oldest one. Used for navigation purposes
   */
  private static final String PREVIOUS_RANGE = "ReportGeneralLedgerJournal.previousRange";
  private static final String PREVIOUS_RANGE_OLD = "ReportGeneralLedgerJournal.previousRangeOld";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (log4j.isDebugEnabled())
      log4j.debug("Command: " + vars.getStringParameter("Command"));

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = "", strDateTo = "";
      String strcAcctSchemaId = vars.getGlobalVariable("inpcAcctSchemaId",
          "EfinReportGeneralLedger|cAcctSchemaId", "");
      String strFromPeriod = vars.getGlobalVariable("inpcFromPeriodId",
          "ReportGeneralLedgerJournal|cFromPeriodId", "");
      if (strFromPeriod == "") {
        strDateFrom = "09-05-1834";
      } else {
        strDateFrom = getDate(vars, strFromPeriod, null);
      }
      String strToPeriod = vars.getGlobalVariable("inpcToPeriodId",
          "ReportGeneralLedgerJournal|cToPeriodId", "");
      if (strToPeriod == "") {
        strDateTo = "21-06-2058";
      } else {
        strDateTo = getDate(vars, null, strToPeriod);
      }
      String currentPeriodId = getCurrentPeriod(vars,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
      String strInitRecord = vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber");
      tempDefault = strInitRecord;
      // String strDateFrom = vars.getGlobalVariable("inpDateFrom",
      // "ReportGeneralLedgerJournal|DateFrom", "");
      // String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportGeneralLedgerJournal|DateTo",
      // "");
      String strDocument = vars.getGlobalVariable("inpDocumenttype",
          "ReportGeneralLedgerJournal|Documenttype", "");
      String strDocumentNo = vars.getGlobalVariable("inpDocumentNo",
          "ReportGeneralLedgerJournal|DocumentNo", "");
      String strDocno = vars.getGlobalVariable("inpDocumentno", "EfinReportGeneralLedger|DocNo",
          "");
      String strAccSeq = vars.getGlobalVariable("inpAccountingSeq",
          "EfinReportGeneralLedger|AccSeq", "");
      String orderby = vars.getGlobalVariable("inpOrderby", "ReportGeneralLedgerJournal|orderby",
          "");
      String ascdesc = vars.getGlobalVariable("inpascdesc", "ReportGeneralLedgerJournal|ascdesc",
          "");
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedgerJournal|Org", "0");
      String strShowClosing = vars.getGlobalVariable("inpShowClosing",
          "ReportGeneralLedgerJournal|ShowClosing", "Y");
      String strShowReg = vars.getGlobalVariable("inpShowReg", "ReportGeneralLedgerJournal|ShowReg",
          "Y");
      String strShowOpening = vars.getGlobalVariable("inpShowOpening",
          "ReportGeneralLedgerJournal|ShowOpening", "Y");
      String strShowRegular = vars.getGlobalVariable("inpShowRegular",
          "ReportGeneralLedgerJournal|ShowRegular", "Y");
      String strShowDivideUp = vars.getGlobalVariable("inpShowDivideUp",
          "ReportGeneralLedgerJournal|ShowDivideUp", "Y");
      String strRecord = vars.getGlobalVariable("inpRecord", "ReportGeneralLedgerJournal|Record",
          "");
      String strTable = vars.getGlobalVariable("inpTable", "ReportGeneralLedgerJournal|Table", "");
      log4j.debug("********DEFAULT***************  strShowClosing: " + strShowClosing);
      log4j.debug("********DEFAULT***************  strShowReg: " + strShowReg);
      log4j.debug("********DEFAULT***************  strShowOpening: " + strShowOpening);
      String initRecordNumberOld = vars
          .getSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "0");
      if (vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0").equals("0")) {
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
        vars.setSessionValue(PREVIOUS_ACCTENTRIES, "0");
        vars.setSessionValue(PREVIOUS_RANGE, "");
      } else if (!"-1".equals(initRecordNumberOld)) {
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", initRecordNumberOld);
        vars.setSessionValue(PREVIOUS_ACCTENTRIES, vars.getSessionValue(PREVIOUS_ACCTENTRIES_OLD));
        vars.setSessionValue(PREVIOUS_RANGE, vars.getSessionValue(PREVIOUS_RANGE_OLD));
      }
      String strPageNo = vars.getGlobalVariable("inpPageNo", "ReportGeneralLedgerJournal|PageNo",
          "1");
      String strEntryNo = vars.getGlobalVariable("inpEntryNo", "ReportGeneralLedgerJournal|EntryNo",
          "1");
      String strShowDescription = vars.getGlobalVariable("inpShowDescription",
          "ReportGeneralLedgerJournal|ShowDescription", "");
      String strcelementvaluefrom = vars.getGlobalVariable("inpcElementValueIdFrom",
          "ReportGeneralLedgerJournal|C_ElementValue_IDFROM", "");
      String strcelementvalueto = vars.getGlobalVariable("inpcElementValueIdTo",
          "ReportGeneralLedgerJournal|C_ElementValue_IDTO", "");
      String strcelementvaluefromdes = "", strcelementvaluetodes = "";
      if (!strcelementvaluefrom.equals(""))
        strcelementvaluefromdes = ReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvaluefrom);
      if (!strcelementvalueto.equals(""))
        strcelementvaluetodes = ReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvalueto);
      strcelementvaluefromdes = (strcelementvaluefromdes.equals("null")) ? ""
          : strcelementvaluefromdes;
      strcelementvaluetodes = (strcelementvaluetodes.equals("null")) ? "" : strcelementvaluetodes;
      vars.setSessionValue("inpElementValueIdFrom_DES", strcelementvaluefromdes);
      vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);
      printPageDataSheet(response, vars, strFromPeriod, strToPeriod, currentPeriodId, strDateFrom,
          strDateTo, strDocument, strDocumentNo, strOrg, strTable, strRecord, "", strcAcctSchemaId,
          strShowClosing, strShowReg, strShowOpening, strPageNo, strEntryNo, strShowDescription,
          strShowRegular, strShowDivideUp, "", "", strcelementvaluefrom, strcelementvalueto,
          strDocno, strAccSeq, orderby, ascdesc, strcelementvaluefromdes, strcelementvaluetodes);
    } else if (vars.commandIn("DIRECT")) {
      String strTable = vars.getGlobalVariable("inpTable", "ReportGeneralLedgerJournal|Table");
      String strRecord = vars.getGlobalVariable("inpRecord", "ReportGeneralLedgerJournal|Record");
      String strAccSchemas = vars.getGlobalVariable("inpAccSchemas",
          "ReportGeneralLedgerJournal|AccSchemas");
      String paramschemas = vars.getStringParameter("inpParamschemas");
      String strPosted = vars.getStringParameter("posted");
      String recordPeriod = getPeriodId(vars, strRecord);
      if (strPosted == "") {
        if (paramschemas != "") {
          strAccSchemas = paramschemas;
        }
      }

      String[] accSchemas = strAccSchemas.split(",");
      String strcAcctSchemaId = accSchemas[0];
      String schemas = "";
      for (int i = 1; i < accSchemas.length; i++) {
        if (i + 1 == accSchemas.length) {
          schemas = schemas + accSchemas[i];
        } else {
          schemas = schemas + accSchemas[i] + ",";
        }
      }
      setHistoryCommand(request, "DIRECT");
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      printPageDataSheet(response, vars, recordPeriod, recordPeriod, "", "", "", "", "", "",
          strTable, strRecord, "", strcAcctSchemaId, "", "", "", "1", "1", "", "Y", "", schemas,
          strPosted, "", "", "", "", "", "", "", "");
    } else if (vars.commandIn("DIRECT2")) {
      String strFactAcctGroupId = vars.getGlobalVariable("inpFactAcctGroupId",
          "ReportGeneralLedgerJournal|FactAcctGroupId");
      String strcAcctSchemaId = vars.getGlobalVariable("inpschemaId",
          "ReportGeneralLedgerJournal|schemaId");
      String strperiod = vars.getGlobalVariable("inpPeriodId",
          "ReportGeneralLedgerJournal|PeriodId");
      String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
          "EfinReportGeneralLedger|C_ElementValue_IDFROM");
      String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
          "EfinReportGeneralLedger|C_ElementValue_IDTO");
      String strcelementvaluefromdes = ReportGeneralLedgerData.selectSubaccountDescription(this,
          strcelementvaluefrom);
      String strcelementvaluetodes = ReportGeneralLedgerData.selectSubaccountDescription(this,
          strcelementvalueto);
      setHistoryCommand(request, "DIRECT2");
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      printPageDataSheet(response, vars, strperiod, strperiod, "", "", "", "", "", "", "", "",
          strFactAcctGroupId, strcAcctSchemaId, "", "", "", "1", "1", "", "Y", "", "", "",
          strcelementvaluefrom, strcelementvalueto, "", "", "", "", strcelementvaluefromdes,
          strcelementvaluetodes);
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = "", strDateTo = "";
      String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
          "EfinReportGeneralLedger|cAcctSchemaId");

      String strFromPeriod = vars.getRequestGlobalVariable("inpcFromPeriodId",
          "ReportGeneralLedgerJournal|cFromPeriodId");
      if (strFromPeriod == "") {
        strDateFrom = "09-05-1834";
      } else {
        strDateFrom = getDate(vars, strFromPeriod, null);
      }
      String strToPeriod = vars.getRequestGlobalVariable("inpcToPeriodId",
          "ReportGeneralLedgerJournal|cToPeriodId");
      if (strToPeriod == "") {
        strDateTo = "21-06-2058";
      } else {
        strDateTo = getDate(vars, null, strToPeriod);
      }
      // String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
      // "ReportGeneralLedgerJournal|DateFrom");
      // String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
      // "ReportGeneralLedgerJournal|DateTo");
      String strDocument = vars.getRequestGlobalVariable("inpDocumenttype",
          "ReportGeneralLedgerJournal|Documenttype");
      String orderby = vars.getRequestGlobalVariable("inpOrderby",
          "ReportGeneralLedgerJournal|orderby");
      String ascdesc = vars.getRequestGlobalVariable("inpascdesc",
          "ReportGeneralLedgerJournal|ascdesc");
      String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
          "ReportGeneralLedgerJournal|DocumentNo");
      String strDocno = vars.getRequestGlobalVariable("inpDocumentno",
          "EfinReportGeneralLedger|DocNo");
      String strAccSeq = vars.getRequestGlobalVariable("inpAccountingSeq",
          "EfinReportGeneralLedger|AccSeq");
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedgerJournal|Org", "0");
      String strShowClosing = vars.getRequestGlobalVariable("inpShowClosing",
          "ReportGeneralLedgerJournal|ShowClosing");
      if (strShowClosing == null || "".equals(strShowClosing))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowClosing", "N");
      String strShowDivideUp = vars.getRequestGlobalVariable("inpShowDivideUp",
          "ReportGeneralLedgerJournal|ShowDivideUp");
      if (strShowDivideUp == null || "".equals(strShowDivideUp))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowDivideUp", "N");
      String strShowRegular = vars.getRequestGlobalVariable("inpShowRegular",
          "ReportGeneralLedgerJournal|ShowRegular");
      if (strShowRegular == null || "".equals(strShowRegular))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "N");
      String strShowReg = vars.getRequestGlobalVariable("inpShowReg",
          "ReportGeneralLedgerJournal|ShowReg");
      if (strShowReg == null || "".equals(strShowReg))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowReg", "N");
      String strShowOpening = vars.getRequestGlobalVariable("inpShowOpening",
          "ReportGeneralLedgerJournal|ShowOpening");
      if (strShowOpening == null || "".equals(strShowOpening))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowOpening", "N");
      if (!("Y".equals(strShowOpening)) && !("Y".equals(strShowReg))
          && !("Y".equals(strShowRegular)) && !("Y".equals(strShowClosing))
          && !("Y".equals(strShowDivideUp))) {
        strShowRegular = "Y";
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "Y");
      }
      String strShowClosing1 = vars.getStringParameter("inpShowClosing");
      String strShowReg1 = vars.getStringParameter("inpShowReg");
      String strShowOpening1 = vars.getStringParameter("inpShowOpening");
      String strShowDivideUp1 = vars.getStringParameter("inpShowDivideUp");
      log4j.debug("********FIND***************  strShowClosing: " + strShowClosing);
      log4j.debug("********FIND***************  strShowReg: " + strShowReg);
      log4j.debug("********FIND***************  strShowOpening: " + strShowOpening);
      log4j.debug("********FIND***************  strShowDivideUp: " + strShowDivideUp);
      log4j.debug("********FIND***************  strShowClosing1: " + strShowClosing1);
      log4j.debug("********FIND***************  strShowReg1: " + strShowReg1);
      log4j.debug("********FIND***************  strShowOpening1: " + strShowOpening1);
      log4j.debug("********FIND***************  strShowDivideUp1: " + strShowDivideUp1);
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      vars.setSessionValue(PREVIOUS_ACCTENTRIES, "0");
      vars.setSessionValue(PREVIOUS_RANGE, "");
      setHistoryCommand(request, "DEFAULT");
      String strPageNo = vars.getRequestGlobalVariable("inpPageNo",
          "ReportGeneralLedgerJournal|PageNo");
      String strEntryNo = vars.getRequestGlobalVariable("inpEntryNo",
          "ReportGeneralLedgerJournal|EntryNo");
      String strShowDescription = vars.getRequestGlobalVariable("inpShowDescription",
          "ReportGeneralLedgerJournal|ShowDescription");
      if (strShowDescription == null || "".equals(strShowDescription))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowDescription", "N");
      String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
          "ReportGeneralLedgerJournal|C_ElementValue_IDFROM");
      String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
          "ReportGeneralLedgerJournal|C_ElementValue_IDTO");
      String strcelementvaluefromdes = "", strcelementvaluetodes = "";
      if (!strcelementvaluefrom.equals(""))
        strcelementvaluefromdes = ReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvaluefrom);
      if (!strcelementvalueto.equals(""))
        strcelementvaluetodes = ReportGeneralLedgerData.selectSubaccountDescription(this,
            strcelementvalueto);
      vars.setSessionValue("inpElementValueIdFrom_DES", strcelementvaluefromdes);
      vars.setSessionValue("inpElementValueIdTo_DES", strcelementvaluetodes);
      printPageDataSheet(response, vars, strFromPeriod, strToPeriod, "", strDateFrom, strDateTo,
          strDocument, strDocumentNo, strOrg, "", "", "", strcAcctSchemaId, strShowClosing,
          strShowReg, strShowOpening, strPageNo, strEntryNo, strShowDescription, strShowRegular,
          strShowDivideUp, "", "", strcelementvaluefrom, strcelementvalueto, strDocno, strAccSeq,
          orderby, ascdesc, strcelementvaluefromdes, strcelementvaluetodes);
    } else if (vars.commandIn("PDF", "XLS")) {
      if (log4j.isDebugEnabled())
        log4j.debug("PDF");
      String strDateFrom = "", strDateTo = "";
      String strcAcctSchemaId = vars.getRequestGlobalVariable("inpcAcctSchemaId",
          "ReportGeneralLedger|cAcctSchemaId");
      String strFromPeriod = vars.getRequestGlobalVariable("inpcFromPeriodId",
          "ReportTrialBalance|cFromPeriodId");
      if (strFromPeriod == "") {
        strDateFrom = "09-05-1834";
      } else {
        strDateFrom = getDate(vars, strFromPeriod, null);
      }
      String strToPeriod = vars.getRequestGlobalVariable("inpcToPeriodId",
          "ReportTrialBalance|cToPeriodId");
      if (strToPeriod == "") {
        strDateTo = "21-06-2058";
      } else {
        strDateTo = getDate(vars, null, strToPeriod);
      }
      String strDocument = vars.getRequestGlobalVariable("inpDocumenttype",
          "ReportGeneralLedgerJournal|Documenttype");
      String orderby = vars.getRequestGlobalVariable("inpOrderby",
          "ReportGeneralLedgerJournal|orderby");
      String ascdesc = vars.getRequestGlobalVariable("inpascdesc",
          "ReportGeneralLedgerJournal|ascdesc");
      String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
          "ReportGeneralLedgerJournal|DocumentNo");
      String strDocno = vars.getRequestGlobalVariable("inpDocumentno",
          "EfinReportGeneralLedger|DocNo");
      String strAccSeq = vars.getRequestGlobalVariable("inpAccountingSeq",
          "EfinReportGeneralLedger|AccSeq");
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportGeneralLedgerJournal|Org", "0");
      String strShowClosing = vars.getRequestGlobalVariable("inpShowClosing",
          "ReportGeneralLedgerJournal|ShowClosing");
      if (strShowClosing == null || "".equals(strShowClosing))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowClosing", "N");
      String strShowRegular = vars.getRequestGlobalVariable("inpShowRegular",
          "ReportGeneralLedgerJournal|ShowRegular");
      if (strShowRegular == null || "".equals(strShowRegular))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "N");
      String strShowReg = vars.getRequestGlobalVariable("inpShowReg",
          "ReportGeneralLedgerJournal|ShowReg");
      if (strShowReg == null || "".equals(strShowReg))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowReg", "N");
      String strShowOpening = vars.getRequestGlobalVariable("inpShowOpening",
          "ReportGeneralLedgerJournal|ShowOpening");
      if (strShowOpening == null || "".equals(strShowOpening))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowOpening", "N");
      String strShowDivideUp = vars.getRequestGlobalVariable("inpShowDivideUp",
          "ReportGeneralLedgerJournal|ShowDivideUp");
      if (strShowDivideUp == null || "".equals(strShowDivideUp))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowDivideUp", "N");
      // In case all flags "Type" are deactivated, the "Regular" one is activated by default
      if (!("Y".equals(strShowOpening)) && !("Y".equals(strShowReg))
          && !("Y".equals(strShowRegular)) && !("Y".equals(strShowClosing))
          && !("Y".equals(strShowDivideUp))) {
        strShowRegular = "Y";
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowRegular", "Y");
      }

      // String strRecord = vars.getGlobalVariable("inpRecord",
      // "ReportGeneralLedgerJournal|Record");
      // String strTable = vars.getGlobalVariable("inpTable",
      // "ReportGeneralLedgerJournal|Table");
      String strTable = vars.getStringParameter("inpTable");
      String strRecord = vars.getStringParameter("inpRecord");
      String strPageNo = vars.getGlobalVariable("inpPageNo", "ReportGeneralLedgerJournal|PageNo",
          "1");
      String strEntryNo = vars.getGlobalVariable("inpEntryNo", "ReportGeneralLedgerJournal|EntryNo",
          "1");
      String strShowDescription = vars.getRequestGlobalVariable("inpShowDescription",
          "ReportGeneralLedgerJournal|ShowDescription");
      if (strShowDescription == null || "".equals(strShowDescription))
        vars.setSessionValue("ReportGeneralLedgerJournal|ShowDescription", "N");
      /*
       * Scenario 1: We will have FactAcctGroupId while the request redirect from
       * ReportGeneralLedger Report. Otherwise we don't need to use FactAcctGroupId for PDF or Excel
       * report. So we have to check the immediate history command has DIRECT2 (It means previous
       * request from ReportGeneralLedger Report) Scenario 2: If we print once in PDF, it will reset
       * the history of COMMAND with DEFAULT, so same record of redirect wont print more than one
       * time. It will consider as default in second time.Scenario 3: If user change the filter
       * criteria, however he has come from ReportGeneralLedger Report(DIRECT2) We don't take
       * strFactAcctGroupId(will take care of criteria from current screen)
       */
      String strFactAcctGroupId = "";
      if (strcAcctSchemaId.equals("") && strDateFrom.equals("") && strDocument.equals("")
          && strOrg.equals("0") && strShowClosing.equals("") && strShowReg.equals("")
          && strShowOpening.equals("") && strRecord.equals("")) {

        int currentHistoryIndex = new Integer(
            new VariablesHistory(request).getCurrentHistoryIndex()).intValue();
        String currentCommand = vars.getSessionValue("reqHistory.command" + currentHistoryIndex);
        if (currentCommand.equals("DIRECT2")) {
          strFactAcctGroupId = vars.getGlobalVariable("inpFactAcctGroupId",
              "ReportGeneralLedgerJournal|FactAcctGroupId");
        }
      }
      // vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
      setHistoryCommand(request, "DEFAULT");
      String strcelementvaluefrom = vars.getRequestGlobalVariable("inpcElementValueIdFrom",
          "ReportGeneralLedgerJournal|C_ElementValue_IDFROM");
      String strcelementvalueto = vars.getRequestGlobalVariable("inpcElementValueIdTo",
          "ReportGeneralLedgerJournal|C_ElementValue_IDTO");
      printPagePDF(request, response, vars, strFromPeriod, strToPeriod, strDateFrom, strDateTo,
          strDocument, strDocumentNo, strOrg, strTable, strRecord, strFactAcctGroupId,
          strcAcctSchemaId, strShowClosing, strShowReg, strShowOpening, strPageNo, strEntryNo,
          "Y".equals(strShowDescription) ? "Y" : "", strShowRegular, strShowDivideUp,
          strcelementvaluefrom, strcelementvalueto, strDocno, strAccSeq, orderby, ascdesc);
    } else if (vars.commandIn("PREVIOUS_RELATION")) {
      String strInitRecord = vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber");
      String strPreviousRecordRange = vars.getSessionValue(PREVIOUS_RANGE);
      String[] previousRecord = strPreviousRecordRange.split(",");
      if (previousRecord.length >= 2) {
        strPreviousRecordRange = previousRecord[0];
        int intRecordRange = strPreviousRecordRange.equals("") ? 0
            : Integer.parseInt(strPreviousRecordRange);
        strPreviousRecordRange = previousRecord[1];
        intRecordRange += strPreviousRecordRange.equals("") ? 0
            : Integer.parseInt(strPreviousRecordRange);
        // Remove parts of the previous range
        StringBuffer sb_previousRange = new StringBuffer();
        for (int i = 2; i < previousRecord.length; i++) {
          sb_previousRange.append(previousRecord[i] + ",");
        }
        vars.setSessionValue(PREVIOUS_RANGE, sb_previousRange.toString());
        // Remove parts of the previous accounting entries
        String[] previousAcctEntries = vars.getSessionValue(PREVIOUS_ACCTENTRIES).split(",");
        StringBuffer sb_previousAcctEntries = new StringBuffer();
        for (int i = 2; i < previousAcctEntries.length; i++) {
          sb_previousAcctEntries.append(previousAcctEntries[i] + ",");
        }

        if (strInitRecord.equals("") || strInitRecord.equals("0"))
          vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", "0");
        else {
          int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
          initRecord -= intRecordRange;
          strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
          vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber", strInitRecord);
          vars.setSessionValue(PREVIOUS_ACCTENTRIES, sb_previousAcctEntries.toString());
        }

        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "-1");
        response.sendRedirect(strDireccion + request.getServletPath());
      } else
        response.sendRedirect(strDireccion + request.getServletPath());

    } else if (vars.commandIn("NEXT_RELATION")) {
      vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "-1");
      response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("DOC")) {
      /*
       * String org = vars.getStringParameter("inpOrg"); String accSchema =
       * vars.getStringParameter("inpcAcctSchemaId"); String strDocument =
       * vars.getRequestGlobalVariable("inpDocument", "ReportGeneralLedgerJournal|Document");
       * Set<String> docbasetypes = getDocuments(org, accSchema); String combobox =
       * getJSONComboBox(docbasetypes, strDocument, false, vars);
       * 
       * response.setContentType("text/html; charset=UTF-8"); PrintWriter out =
       * response.getWriter(); out.println("objson = " + combobox); out.close();
       */

    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strFromPeriodId, String strToPeriodId, String currentPeriodId, String strDateFrom,
      String strDateTo, String strDocument, String strDocumentNo, String strOrg, String strTable,
      String strRecord, String strFactAcctGroupId, String strcAcctSchemaId, String strShowClosing,
      String strShowReg, String strShowOpening, String strPageNo, String strEntryNo,
      String strShowDescription, String strShowRegular, String strShowDivideUp, String accShemas,
      String strPosted, String strcelementvaluefrom, String strcelementvalueto, String strDocno,
      String strAccSeq, String orderby, String ascdesc, String strcelementvaluefromdes,
      String strcelementvaluetodes) throws IOException, ServletException {
    String ascdesctext = "";
    String orderbytext = "";

    if (ascdesc != null && ascdesc != "") {
      ascdesctext = ReportGeneralLedgerJournalData.getValue(this, ascdesc);
    } else {
      ascdesctext = "asc";
    }

    if (orderby != null && orderby != "") {
      orderbytext = ReportGeneralLedgerJournalData.getValue(this, orderby);
      if (orderbytext.equals("acctdatetime")) {
        orderbytext += " " + ascdesctext + ",DOCNO " + ascdesctext;
      } else {
        orderbytext += " " + ascdesctext + ",acctdatetime " + ascdesctext;
      }
    } else {
      orderbytext = " acctdatetime desc, DOCNO desc ";
    }
    String strDocumenttext = ReportGeneralLedgerJournalData.getValue(this, strDocument);
    String Orderbyclause = orderbytext + " ";
    String strAllaccounts = "Y";
    if (strcelementvaluefrom != null && !strcelementvaluefrom.equals(""))
      strAllaccounts = "N";
    String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
        "ReportGeneralLedgerJournal");
    int intRecordRangePredefined = (strRecordRange.equals("") ? 0
        : Integer.parseInt(strRecordRange));
    String strInitRecord = vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber");
    int initRecordNumber = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    ReportGeneralLedgerJournalData[] data = null;
    ReportGeneralLedgerJournalData[] dataCountLines = null;
    ReportGeneralLedgerJournalData[] dataOrg = null;

    String strPosition = "0";
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportGeneralLedgerJournal", false, "",
        "", "imprimir();return false;", false, "ad_reports", strReplaceWith, false, true);
    toolbar.setEmail(false);
    int totalAcctEntries = 0;
    int lastRecordNumber = 0;
    if (vars.commandIn("FIND") || vars.commandIn("DEFAULT")
        && (!vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumber").equals("0") || "0"
            .equals(vars.getSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", "")))) {
      String strCheck = buildCheck(strShowClosing, strShowReg, strShowOpening, strShowRegular,
          strShowDivideUp);
      String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
      String strOrgFamily = getFamily(strTreeOrg, strOrg);
      if (strRecord.equals("")) {
        // Stores the number of lines per accounting entry
        dataCountLines = ReportGeneralLedgerJournalData.selectCountGroupedLines(this,
            Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strDocumenttext, strDocno,
            strAccSeq, getDocumentNo(vars.getClient(), strDocumenttext, strDocumentNo),
            strcAcctSchemaId, strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom,
            strcelementvalueto, vars.getRole());
        String strInitAcctEntries = vars.getSessionValue(PREVIOUS_ACCTENTRIES);
        int acctEntries = (strInitAcctEntries.equals("") ? 0
            : Integer.parseInt(strInitAcctEntries.split(",")[0]));
        for (ReportGeneralLedgerJournalData i : dataCountLines)
          totalAcctEntries += Integer.parseInt(i.groupedlines);
        int groupedLines[] = new int[intRecordRangePredefined + 1];
        int i = 1;
        while (groupedLines[i - 1] <= intRecordRangePredefined
            && dataCountLines.length >= acctEntries) {
          if (dataCountLines.length > acctEntries) {
            groupedLines[i] = groupedLines[i - 1]
                + Integer.parseInt(dataCountLines[acctEntries].groupedlines);
            i++;
          }
          acctEntries++;
        }

        int intRecordRangeUsed = 0;
        if (dataCountLines.length != acctEntries - 1) {
          if (i == 2) {
            // The first entry is bigger than the predefined range
            intRecordRangeUsed = groupedLines[i - 1];
            acctEntries++;
          } else if (i - 2 >= 0) {
            intRecordRangeUsed = groupedLines[i - 2];
          }
        } else {
          // Include also the last entry
          intRecordRangeUsed = groupedLines[i - 1];
        }

        // Hack for sqlC first record
        if (initRecordNumber == 0) {
          lastRecordNumber = initRecordNumber + intRecordRangeUsed + 1;
        } else {
          lastRecordNumber = initRecordNumber + intRecordRangeUsed;
        }
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumber",
            String.valueOf(lastRecordNumber));
        vars.setSessionValue("ReportGeneralLedgerJournal.initRecordNumberOld", strInitRecord);

        // Stores historical for navigation purposes
        vars.setSessionValue(PREVIOUS_ACCTENTRIES_OLD, vars.getSessionValue(PREVIOUS_ACCTENTRIES));
        vars.setSessionValue(PREVIOUS_ACCTENTRIES,
            String.valueOf(acctEntries - 1) + "," + vars.getSessionValue(PREVIOUS_ACCTENTRIES));
        vars.setSessionValue(PREVIOUS_RANGE_OLD, vars.getSessionValue(PREVIOUS_RANGE));
        vars.setSessionValue(PREVIOUS_RANGE,
            String.valueOf(intRecordRangeUsed) + "," + vars.getSessionValue(PREVIOUS_RANGE));
        data = ReportGeneralLedgerJournalData.select(this, "'N'",
            Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strDocumenttext, strDocno,
            strAccSeq, getDocumentNo(vars.getClient(), strDocumenttext, strDocumentNo),
            strcAcctSchemaId, strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom,
            strcelementvalueto, vars.getRole(), vars.getLanguage(), Orderbyclause, initRecordNumber,
            intRecordRangeUsed);
        if (data != null && data.length > 0)
          strPosition = ReportGeneralLedgerJournalData.selectCount(this,
              Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
              strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strDocumenttext,
              getDocumentNo(vars.getClient(), strDocumenttext, strDocumentNo), strcAcctSchemaId,
              strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
              vars.getRole(), data[0].dateacct, data[0].identifier);
      } else {
        data = ReportGeneralLedgerJournalData.selectDirect(this,
            "Y".equals(strShowDescription) ? "'Y'" : "'N'",
            Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"), strTable,
            strRecord, strcAcctSchemaId, vars.getRole(), vars.getLanguage(), initRecordNumber,
            intRecordRangePredefined);
        if (data != null && data.length > 0)
          strPosition = ReportGeneralLedgerJournalData.selectCountDirect(this,
              Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"), strTable,
              strRecord, strFactAcctGroupId, vars.getRole(), data[0].dateacct, data[0].identifier);
      }
    } else if (vars.commandIn("DIRECT")) {
      data = ReportGeneralLedgerJournalData.selectDirect(this,
          "Y".equals(strShowDescription) ? "'Y'" : "'N'",
          Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"), strTable,
          strRecord, strcAcctSchemaId, vars.getRole(), vars.getLanguage());
      if (data != null && data.length > 0)
        strPosition = ReportGeneralLedgerJournalData.selectCountDirect(this,
            Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"), strTable,
            strRecord, strFactAcctGroupId, vars.getRole(), data[0].dateacct, data[0].identifier);
    } else if (vars.commandIn("DIRECT2")) {
      data = ReportGeneralLedgerJournalData.selectDirect2(this,
          Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
          strFactAcctGroupId, vars.getRole(), vars.getLanguage());
      if (data != null && data.length > 0)
        strPosition = ReportGeneralLedgerJournalData.selectCountDirect2(this,
            Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            strFactAcctGroupId, vars.getRole(), data[0].dateacct, data[0].identifier);
    }
    if (data == null || data.length == 0) {
      String discard[] = { "secTable" };
      toolbar.prepareRelationBarTemplate(false, false,
          "submitCommandForm('XLS', false, null, 'ReportGeneralLedgerJournal.xls', 'EXCEL');return false;");
      xmlDocument = xmlEngine.readXmlTemplate(
          "sa/elm/ob/finance/ad_reports/ReportGeneralLedger/ReportGeneralLedgerJournal", discard)
          .createXmlDocument();
      data = ReportGeneralLedgerJournalData.set("0");
      data[0].rownum = "0";
    } else {
      data = notshow(data, vars);
      boolean hasPrevious = !(data == null || data.length == 0 || initRecordNumber <= 1);
      boolean hasNext = !(data == null || data.length == 0 || lastRecordNumber >= totalAcctEntries);
      toolbar.prepareRelationBarTemplate(true, true,
          "submitCommandForm('XLS', false, null, 'ReportGeneralLedgerJournal.xls', 'EXCEL');return false;");
      xmlDocument = xmlEngine
          .readXmlTemplate(
              "sa/elm/ob/finance/ad_reports/ReportGeneralLedger/ReportGeneralLedgerJournal")
          .createXmlDocument();

      String jsDisablePreviousNext = "function checkPreviousNextButtons(){";
      if (!hasPrevious)
        jsDisablePreviousNext += "disableToolBarButton('linkButtonPrevious');";
      if (!hasNext)
        jsDisablePreviousNext += "disableToolBarButton('linkButtonNext');";
      jsDisablePreviousNext += "}";
      xmlDocument.setParameter("jsDisablePreviousNext", jsDisablePreviousNext);
    }

    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "sa.elm.ob.finance.ad_reports.ReportGeneralLedger.ReportGeneralLedgerJournal");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "ReportGeneralLedgerJournal.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(),
          "ReportGeneralLedgerJournal.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportGeneralLedgerJournal");
      vars.removeMessage("ReportGeneralLedgerJournal");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("strdocument", strDocument);
    xmlDocument.setParameter("cAcctschemaId", strcAcctSchemaId);
    xmlDocument.setParameter("cAcctschemas", accShemas);
    xmlDocument.setParameter("posted", strPosted);
    xmlDocument.setParameter("DocNo", strDocno);
    xmlDocument.setParameter("AccSeq", strAccSeq);
    xmlDocument.setParameter("orderby", orderby);
    xmlDocument.setParameter("ascdesc", ascdesc);

    try {
      dataOrg = ReportGeneralLedgerJournalData.selectOrgValidation(this,
          Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"));
      String orgList = "";
      for (int i = 0; i < dataOrg.length; i++) {
        if (i == 0) {
          orgList = "'" + dataOrg[i].adOrgId + "'";
        } else {
          orgList += "," + "'" + dataOrg[i].adOrgId + "'";
        }
      }
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_ORG_ID", "",
          "", orgList, Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedgerJournal"),
          '*');
      comboTableData.fillParameters(null, "ReportGeneralLedgerJournal", "");
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setData("reportC_ACCTSCHEMA_ID", "liststructure",
        AccountingSchemaMiscData.selectC_ACCTSCHEMA_ID(this,
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
            Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
            strcAcctSchemaId));
    // xmlDocument.setData("reportC_ACCTSCHEMA_ID", "liststructure",
    // AccountingSchemaMiscData.selectC_ACCTSCHEMA_ID(this, Utility.getContext(this, vars,
    // "#AccessibleOrgTree", "ReportGeneralLedger"), Utility.getContext(this, vars, "#User_Client",
    // "ReportGeneralLedger"), strcAcctSchemaId));
    if (vars.commandIn("DEFAULT")) {
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure",
          ReportGeneralLedgerJournalData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              currentPeriodId));
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure",
          ReportGeneralLedgerJournalData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              currentPeriodId));
    } else {
      xmlDocument.setData("reportC_FROMPERIOD_ID", "liststructure",
          ReportGeneralLedgerJournalData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"),
              strFromPeriodId));
      xmlDocument.setData("reportC_TOPERIOD_ID", "liststructure",
          ReportGeneralLedgerJournalData.selectC_PERIOD_ID(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
              Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strToPeriodId));
    }
    if ((vars.commandIn("DEFAULT")) && (tempDefault == "" || tempDefault == "0")) {
      /*
       * xmlDocument.setParameter("cFromPeriodId", currentPeriodId);
       * xmlDocument.setParameter("cToPeriodId", currentPeriodId);
       */
    } else {
      xmlDocument.setParameter("cFromPeriodId", strFromPeriodId);
      xmlDocument.setParameter("cToPeriodId", strToPeriodId);
    }
    xmlDocument.setData("reportOrderby", "liststructure", ReportGeneralLedgerJournalData
        .selectReferencelist(this, "465F5BCCE4154CF7865B7FC0AF4CA128", orderby));
    xmlDocument.setData("reportascdesc", "liststructure", ReportGeneralLedgerJournalData
        .selectReferencelist(this, "B63335F3540849B095B75D6117AFFF63", ascdesc));
    xmlDocument.setData("reportDocument", "liststructure", ReportGeneralLedgerJournalData
        .selectReferencelist(this, "9E075867CEC44FD3984AF76FAB5199B8", strDocument));

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("adOrgId", strOrg);
    xmlDocument.setParameter("groupId", strPosition);
    xmlDocument.setParameter("paramRecord", strRecord);
    xmlDocument.setParameter("paramTable", strTable);
    vars.setSessionValue("ReportGeneralLedgerJournal|Record", strRecord);
    vars.setSessionValue("ReportGeneralLedgerJournal|Table", strTable);
    xmlDocument.setParameter("inpPageNo", strPageNo);
    xmlDocument.setParameter("inpDocumentNo", strDocumentNo);
    xmlDocument.setParameter("inpEntryNo", strEntryNo);
    // If none of the "show" flags is active, then regular is checked
    xmlDocument.setParameter("showRegular", ("".equals(strShowRegular)) ? "N" : strShowRegular);
    xmlDocument.setParameter("showClosing", ("".equals(strShowClosing)) ? "N" : strShowClosing);
    xmlDocument.setParameter("showReg", ("".equals(strShowReg)) ? "N" : strShowReg);
    xmlDocument.setParameter("showOpening", ("".equals(strShowOpening)) ? "N" : strShowOpening);
    xmlDocument.setParameter("showDivideUp", ("".equals(strShowDivideUp)) ? "N" : strShowDivideUp);
    xmlDocument.setParameter("showDescription",
        ("".equals(strShowDescription)) ? "N" : strShowDescription);
    xmlDocument.setParameter("paramElementvalueIdTo", strcelementvalueto);
    xmlDocument.setParameter("paramElementvalueIdFrom", strcelementvaluefrom);
    xmlDocument.setParameter("inpElementValueIdTo_DES", strcelementvaluetodes);
    xmlDocument.setParameter("inpElementValueIdFrom_DES", strcelementvaluefromdes);
    xmlDocument.setParameter("primaryfilter",
        Resource.getProperty("finance.PrimaryFilters", vars.getLanguage()));
    xmlDocument.setParameter("strFromPeriod",
        Resource.getProperty("finance.g/lreport.fromperiod", vars.getLanguage()));
    xmlDocument.setParameter("strToPeriod",
        Resource.getProperty("finance.g/lreport.toperiod", vars.getLanguage()));
    xmlDocument.setParameter("strorganization",
        Resource.getProperty("finance.organization", vars.getLanguage()));
    xmlDocument.setParameter("strgeneralledger",
        Resource.getProperty("finance.generalledger", vars.getLanguage()));
    xmlDocument.setParameter("strFromAmount",
        Resource.getProperty("finance.g/lreport.fromamount", vars.getLanguage()));
    xmlDocument.setParameter("strToAmount",
        Resource.getProperty("finance.g/lreport.toamount", vars.getLanguage()));
    xmlDocument.setParameter("strFromAccount",
        Resource.getProperty("finance.g/lreport.fromaccount", vars.getLanguage()));
    xmlDocument.setParameter("strToAccount",
        Resource.getProperty("finance.g/lreport.toaccount", vars.getLanguage()));
    xmlDocument.setParameter("document",
        Resource.getProperty("finance.journalentries.Document", vars.getLanguage()));
    xmlDocument.setParameter("initpageno",
        Resource.getProperty("finance.journalentries.initpageno", vars.getLanguage()));
    xmlDocument.setParameter("strDocumentNo",
        Resource.getProperty("finance.documentno", vars.getLanguage()));
    xmlDocument.setParameter("strorderby",
        Resource.getProperty("finance.sortby", vars.getLanguage()));
    xmlDocument.setParameter("strAcctseq",
        Resource.getProperty("finance.Accountseq", vars.getLanguage()));
    xmlDocument.setParameter("initentryno",
        Resource.getProperty("finance.journalentries.initentryno", vars.getLanguage()));
    xmlDocument.setParameter("reularentries",
        Resource.getProperty("finance.journalentries.showregularentries", vars.getLanguage()));
    xmlDocument.setParameter("plclosing",
        Resource.getProperty("finance.journalentries.plcloseentry", vars.getLanguage()));
    xmlDocument.setParameter("openentry",
        Resource.getProperty("finance.journalentries.openentry", vars.getLanguage()));
    xmlDocument.setParameter("closeentry",
        Resource.getProperty("finance.journalentries.closeentry", vars.getLanguage()));
    xmlDocument.setParameter("entrydesc",
        Resource.getProperty("finance.journalentries.entrydesc", vars.getLanguage()));
    xmlDocument.setParameter("divideup",
        Resource.getProperty("finance.journalentries.divideupentry", vars.getLanguage()));
    xmlDocument.setParameter("viewresult",
        Resource.getProperty("finance.viewresults", vars.getLanguage()));
    xmlDocument.setParameter("search", Resource.getProperty("finance.search", vars.getLanguage()));
    xmlDocument.setParameter("advfilter",
        Resource.getProperty("finance.advancedfilters", vars.getLanguage()));
    xmlDocument.setParameter("entry",
        Resource.getProperty("finance.journalentries.entry", vars.getLanguage()));
    xmlDocument.setParameter("date", Resource.getProperty("finance.date", vars.getLanguage()));
    xmlDocument.setParameter("detail",
        Resource.getProperty("finance.journalentries.detail", vars.getLanguage()));
    xmlDocument.setParameter("acctseq",
        Resource.getProperty("finance.journalentries.acctseq", vars.getLanguage()));
    xmlDocument.setParameter("acctno",
        Resource.getProperty("finance.accountno", vars.getLanguage()));
    xmlDocument.setParameter("name",
        Resource.getProperty("finance.journalentries.name", vars.getLanguage()));
    xmlDocument.setParameter("debit",
        Resource.getProperty("finance.g/lreport.debit", vars.getLanguage()));
    xmlDocument.setParameter("credit",
        Resource.getProperty("finance.g/lreport.credit", vars.getLanguage()));

    if (vars.commandIn("FIND")) {
      SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
      Date formatdate = new Date();
      Date todate = new Date();
      try {
        formatdate = in.parse(strDateFrom);
        todate = in.parse(strDateTo);
      } catch (ParseException e) {
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
      if (data[0].rownum.equals("0")) {
        if (vars.commandIn("FIND") || vars.commandIn("DIRECT2")) {
          // No data has been found. Show warning message.
          xmlDocument.setParameter("messageType", "WARNING");
          xmlDocument.setParameter("messageTitle",
              Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()));
          xmlDocument.setParameter("messageMessage",
              Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
        }
      } else {
        xmlDocument.setData("structure1", data);
      }
    } else {
      if (vars.commandIn("FIND") || vars.commandIn("DIRECT2")) {
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

  private ReportGeneralLedgerJournalData[] notshow(ReportGeneralLedgerJournalData[] data,
      VariablesSecureApp vars) {
    for (int i = 0; i < data.length - 1; i++) {
      if ((data[i].identifier.toString().equals(data[i + 1].identifier.toString()))
          && (data[i].dateacct.toString().equals(data[i + 1].dateacct.toString()))
          && (data[i].docbasetype.toString().equals(data[i + 1].docbasetype.toString()))) {
        data[i + 1].newstyle = "visibility: hidden";
      }
    }
    return data;
  }

  private void printPagePDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFromPeriod, String strToPeriod, String strDateFrom,
      String strDateTo, String strDocument, String strDocumentNo, String strOrg, String strTable,
      String strRecord, String strFactAcctGroupId, String strcAcctSchemaId, String strShowClosing,
      String strShowReg, String strShowOpening, String strPageNo, String strEntryNo,
      String strShowDescription, String strShowRegular, String strShowDivideUp,
      String strcelementvaluefrom, String strcelementvalueto, String strDocno, String strAccSeq,
      String orderby, String ascdesc) throws IOException, ServletException {

    ReportGeneralLedgerJournalData[] data = null;
    String strAllaccounts = "Y";
    if (strcelementvaluefrom != null && !strcelementvaluefrom.equals(""))
      strAllaccounts = "N";
    String strTreeOrg = TreeData.getTreeOrg(this, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrg);
    String strDocumenttext = ReportGeneralLedgerJournalData.getValue(this, strDocument);
    String ascdesctext = "";
    String orderbytext = "";
   /* if (orderby != null && orderby != "") {
      orderbytext = ReportGeneralLedgerJournalData.getValue(this, orderby);
    } else {
      // orderbytext = "DOCBASETYPE, acctdatetime";
      orderbytext = "acctdatetime, DOCNO ";
    }
    if (ascdesc != null && ascdesc != "") {
      ascdesctext = ReportGeneralLedgerJournalData.getValue(this, ascdesc);
    } else {
      // ascdesctext = "asc";
      ascdesctext = "desc";
    }*/
    
    if (orderby != null && orderby != "") {
      orderbytext = ReportGeneralLedgerJournalData.getValue(this, orderby);
      if (orderbytext.equals("acctdatetime")) {
        orderbytext += " " + ascdesctext + ",DOCNO " + ascdesctext;
      } else {
        orderbytext += " " + ascdesctext + ",acctdatetime " + ascdesctext;
      }
    } else {
      orderbytext = " acctdatetime desc, DOCNO desc ";
    }
    
   // String Orderbyclause = orderbytext + " " + ascdesctext;
    String Orderbyclause = orderbytext + " ";

    if (!strFactAcctGroupId.equals("")) {
      data = ReportGeneralLedgerJournalData.selectDirect2(this,
          Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"),
          strFactAcctGroupId, vars.getRole(), vars.getLanguage());

    } else if (strRecord.equals("")) {
      String strCheck = buildCheck(strShowClosing, strShowReg, strShowOpening, strShowRegular,
          strShowDivideUp);

      data = ReportGeneralLedgerJournalData.select(this,
          "Y".equals(strShowDescription) ? "'Y'" : "'N'",
          Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"), strDateFrom,
          DateTimeData.nDaysAfter(this, strDateTo, "1"), strDocumenttext, strDocno, strAccSeq,
          getDocumentNo(vars.getClient(), strDocumenttext, strDocumentNo), strcAcctSchemaId,
          strOrgFamily, strCheck, strAllaccounts, strcelementvaluefrom, strcelementvalueto,
          vars.getRole(), vars.getLanguage(), Orderbyclause);
    } else
      data = ReportGeneralLedgerJournalData.selectDirect(this,
          "Y".equals(strShowDescription) ? "'Y'" : "'N'",
          Utility.getContext(this, vars, "#User_Client", "ReportGeneralLedger"),
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportGeneralLedger"), strTable,
          strRecord, strcAcctSchemaId, vars.getRole(), vars.getLanguage());

    if (data == null || data.length == 0) {
      advisePopUp(request, response, "Warning",
          Utility.messageBD(this, "ProcessStatus-W", vars.getLanguage()),
          Utility.messageBD(this, "NoDataFound", vars.getLanguage()));
    }

    else if (vars.commandIn("XLS") && data.length > 65532) {
      advisePopUp(request, response, "Error",
          Utility.messageBD(this, "ProcessStatus-E", vars.getLanguage()),
          Utility.messageBD(this, "numberOfRowsExceeded", vars.getLanguage()));
    }

    else {

      String strSubtitle = (Utility.messageBD(this, "LegalEntity", vars.getLanguage()) + ": ")
          + ReportGeneralLedgerJournalData.selectCompany(this, vars.getClient()) + "\n";
      ;

      /*
       * SimpleDateFormat javaSDF = new SimpleDateFormat(vars.getJavaDateFormat()); SimpleDateFormat
       * sqlSDF = new SimpleDateFormat(vars.getSqlDateFormat().replace('Y', 'y').replace('D', 'd'));
       */

      if (!("0".equals(strOrg)))
        strSubtitle += (Utility.messageBD(this, "OBUIAPP_Organization", vars.getLanguage()) + ": ")
            + ReportGeneralLedgerJournalData.selectOrg(this, strOrg) + "\n";

      SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");

      Date formatdate, formattodate;
      String to_date = "", from_date = "";
      try {
        formatdate = in.parse(strDateTo);
        to_date = out.format(formatdate);
        formattodate = in.parse(strDateFrom);
        from_date = out.format(formattodate);
      } catch (ParseException e) {
      }

      String hijiritodate = UtilityDAO.convertToHijriDate(to_date);
      String hijirifromdate = UtilityDAO.convertToHijriDate(from_date);

      Date date = new Date();
      String currentdate = UtilityDAO.convertToHijriDate(out.format(date));

      if (!"".equals(strDateFrom) || !"".equals(strDateTo))
        strSubtitle += (Utility.messageBD(this, "From", vars.getLanguage()) + ": ")
            + (hijirifromdate.substring(0, 10).equals("01-01-1250") ? ""
                : hijirifromdate.substring(0, 10))
            + "  " + (Utility.messageBD(this, "OBUIAPP_To", vars.getLanguage()) + ": ")
            + (hijiritodate.substring(0, 10).equals("30-12-1480") ? ""
                : hijiritodate.substring(0, 10))
            + "\n";

      if (!"".equals(strcAcctSchemaId)) {
        AcctSchema financialMgmtAcctSchema = OBDal.getInstance().get(AcctSchema.class,
            strcAcctSchemaId);
        strSubtitle += Utility.messageBD(this, "generalLedger", vars.getLanguage()) + ": "
            + financialMgmtAcctSchema.getName();
      }

      String strOutput;
      String strReportName;
      if (vars.commandIn("PDF")) {
        strOutput = "pdf";
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/ReportGeneralLedger/ReportGeneralLedgerJournal.jrxml";
      } else {
        strOutput = "xls";
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/ReportGeneralLedger/ReportGeneralLedgerJournalExcel.jrxml";
      }

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("ShowDescription", strShowDescription);
      parameters.put("Subtitle", strSubtitle);
      parameters.put("PageNo", strPageNo);
      parameters.put("CURRENT_DATE", currentdate);
      parameters.put("InitialEntryNumber", strEntryNo);
      parameters.put("TaxID", ReportGeneralLedgerJournalData.selectOrgTaxID(this, strOrg));
      parameters.put("strDateFormat", vars.getJavaDateFormat());
      renderJR(vars, response, strReportName, "JournalEntriesReport", strOutput, parameters, data,
          null);
    }
  }

  private String getFamily(String strTree, String strChild) throws IOException, ServletException {
    return Tree.getMembers(this, strTree,
        (strChild == null || strChild.equals("")) ? "0" : strChild);
    /*
     * ReportGeneralLedgerData [] data = ReportGeneralLedgerData.selectChildren(this, strTree,
     * strChild); String strFamily = ""; if(data!=null && data.length>0) { for (int i =
     * 0;i<data.length;i++){ if (i>0) strFamily = strFamily + ","; strFamily = strFamily +
     * data[i].id; } return strFamily += ""; }else return "'1'";
     */
  }

  public String getDate(VariablesSecureApp vars, String strFromPeriodId, String strToPeriodId)
      throws IOException, ServletException {
    ReportGeneralLedgerJournalData[] data = null;
    String strDate = null;
    if (strFromPeriodId != null) {
      data = ReportGeneralLedgerJournalData.selectFromToDate(this,
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
          Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"), strFromPeriodId);
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          strDate = data[i].startdate;
        }

      }
    }
    if (strToPeriodId != null) {
      data = ReportGeneralLedgerJournalData.selectFromToDate(this,
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

  private String buildCheck(String strShowClosing, String strShowReg, String strShowOpening,
      String strShowRegular, String strShowDivideUp) {
    String[] strElements = { strShowClosing.equals("Y") ? "'C'" : "",
        strShowReg.equals("Y") ? "'R'" : "", strShowOpening.equals("Y") ? "'O'" : "",
        strShowRegular.equals("Y") ? "'N'" : "", strShowDivideUp.equals("Y") ? "'D'" : "" };
    int no = 0;
    String strCheck = "";
    for (int i = 0; i < strElements.length; i++) {
      if (!strElements[i].equals("")) {
        if (no != 0)
          strCheck = strCheck + ", ";
        strCheck = strCheck + strElements[i];
        no++;
      }
    }
    return strCheck;
  }

  @SuppressWarnings("unused")
  private <T extends BaseOBObject> String getJSONComboBox(Set<String> docbseTypes,
      String selectedValue, boolean isMandatory, VariablesSecureApp vars) {

    JSONObject json = new JSONObject();
    JSONArray select = new JSONArray();
    Map<String, String> attr = null;
    try {
      int i = 0;
      if (!isMandatory) {
        attr = new HashMap<String, String>();
        attr.put("value", "");
        attr.put("selected", "false");
        attr.put("text", "");
        select.put(i, attr);
        i++;
      }
      for (String dbt : docbseTypes) {
        attr = new HashMap<String, String>();
        attr.put("value", dbt);
        attr.put("selected", (dbt.equals(selectedValue)) ? "true" : "false");
        attr.put("text",
            Utility.getListValueName("C_DocType DocBaseType", dbt, vars.getLanguage()));
        select.put(i, attr);
        json.put("optionlist", select);
        i++;
      }
      json.put("ismandatory", String.valueOf(isMandatory));

    } catch (JSONException e) {
      log4j.error("Error creating JSON object for representing subaccount lines", e);
    }

    return json.toString();
  }

  public static Set<String> getDocuments(String org, String accSchema) {

    final StringBuilder whereClause = new StringBuilder();
    final List<Object> parameters = new ArrayList<Object>();
    OBContext.setAdminMode();
    try {
      // Set<String> orgStrct = OBContext.getOBContext().getOrganizationStructureProvider()
      // .getChildTree(org, true);
      Set<String> orgStrct = OBContext.getOBContext().getOrganizationStructureProvider()
          .getNaturalTree(org);
      whereClause.append(" as cd ,");
      whereClause.append(AcctSchemaTable.ENTITY_NAME);
      whereClause.append(" as ca ");
      whereClause.append(" where cd.");
      whereClause.append(DocumentType.PROPERTY_TABLE + ".id");
      whereClause.append("= ca.");
      whereClause.append(AcctSchemaTable.PROPERTY_TABLE + ".id");
      whereClause.append(" and ca.");
      whereClause.append(AcctSchemaTable.PROPERTY_ACCOUNTINGSCHEMA + ".id");
      whereClause.append(" = ? ");
      parameters.add(accSchema);
      whereClause.append("and ca.");
      whereClause.append(AcctSchemaTable.PROPERTY_ACTIVE + "='Y'");
      whereClause.append(" and cd.");
      whereClause.append(DocumentType.PROPERTY_ORGANIZATION + ".id");
      whereClause.append(" in (" + Utility.getInStrSet(orgStrct) + ")");
      whereClause.append(" and ca." + AcctSchemaTable.PROPERTY_ORGANIZATION + ".id");
      whereClause.append(" in (" + Utility.getInStrSet(orgStrct) + ")");
      whereClause.append(" order by cd." + DocumentType.PROPERTY_DOCUMENTCATEGORY);
      final OBQuery<DocumentType> obqDt = OBDal.getInstance().createQuery(DocumentType.class,
          whereClause.toString());
      obqDt.setParameters(parameters);
      obqDt.setFilterOnReadableOrganization(false);
      TreeSet<String> docBaseTypes = new TreeSet<String>();
      for (DocumentType doc : obqDt.list()) {
        docBaseTypes.add(doc.getDocumentCategory());
      }
      return docBaseTypes;

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Get current Period
   * 
   * @param vars
   * @return current period
   */
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
   * Get period based on Record Id
   * 
   * @param vars
   * @return
   */
  public String getPeriodId(VariablesSecureApp vars, String recordid) {
    String PeriodId = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn.prepareStatement(
          "select c_period_id from fact_acct where record_id  ='" + recordid + "' limit 1");
      rs = ps.executeQuery();
      if (rs.next()) {
        PeriodId = rs.getString("c_period_id");
      }
    } catch (Exception e) {
    }
    return PeriodId;
  }

  /**
   * Builds dynamic SQL to filter by document No
   */
  private String getDocumentNo(String strClient, String strDocument, String strDocumentNo) {
    if (StringUtils.isBlank(strDocument) || StringUtils.isBlank(strDocumentNo)) {
      return null;
    }

    try {
      OBContext.setAdminMode();
      String documentNo = StringEscapeUtils.escapeSql(strDocumentNo);
      documentNo = documentNo.replaceAll(";", "");

      StringBuffer where = new StringBuffer();
      where.append(" select t." + Table.PROPERTY_DBTABLENAME);
      where.append(" from " + DocumentType.ENTITY_NAME + " as d");
      where.append(" join d." + DocumentType.PROPERTY_TABLE + " as t");
      where.append(" where d." + DocumentType.PROPERTY_DOCUMENTCATEGORY + " = :document");
      where.append(" and d." + DocumentType.PROPERTY_CLIENT + ".id = :client");
      where.append(" group by d." + DocumentType.PROPERTY_DOCUMENTCATEGORY);
      where.append(" , t." + Table.PROPERTY_DBTABLENAME);
      Query qry = OBDal.getInstance().getSession().createQuery(where.toString());
      qry.setMaxResults(1);
      qry.setParameter("document", strDocument);
      qry.setParameter("client", strClient);
      String tablename = (String) qry.uniqueResult();

      if (StringUtils.isBlank(tablename)) {
        return null;
      }

      StringBuffer existsSubQuery = new StringBuffer("( SELECT 1 FROM ");
      existsSubQuery.append(tablename);
      existsSubQuery.append(" dt WHERE f.record_id = dt.").append(tablename).append("_id");
      existsSubQuery.append(" AND dt.documentno = '").append(documentNo).append("' )");
      return existsSubQuery.toString();
    } catch (Exception ignore) {
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportGeneralLedgerJournal. This Servlet was made by Pablo Sarobe modified by everybody";
  } // end of getServletInfo() method
}
