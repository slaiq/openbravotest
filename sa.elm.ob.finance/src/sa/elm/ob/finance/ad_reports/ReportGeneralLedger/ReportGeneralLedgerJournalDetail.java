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
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.finance.properties.Resource;
import sa.elm.ob.utility.util.UtilityDAO;

public class ReportGeneralLedgerJournalDetail extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strFactAcctGroupId = vars.getGlobalVariable("inpFactAcctGroupId",
          "ReportGeneralLedgerJournalDetail|FactAcctGroupId", "");
      String strDateAcct = getValue(strFactAcctGroupId, 0);
      strFactAcctGroupId = getValue(strFactAcctGroupId, 1);
      printPageDataSheet(response, vars, strFactAcctGroupId, strDateAcct, null, "");
    } else if (vars.commandIn("DIRECT")) {
      String strFactAcctGroupId = vars.getGlobalVariable("inpFactAcctGroupId",
          "ReportGeneralLedgerJournalDetail|FactAcctGroupId");
      String strDateAcct = strFactAcctGroupId.substring(0, strFactAcctGroupId.lastIndexOf("/"));
      strFactAcctGroupId = strFactAcctGroupId.substring(strFactAcctGroupId.lastIndexOf("/") + 1);
      try {
        strDateAcct = new SimpleDateFormat("dd-MM-yyyy")
            .format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .parse(UtilityDAO.convertToGregorian(strDateAcct)));
      } catch (ParseException e) {

      }
      printPageDataSheet(response, vars, strFactAcctGroupId, strDateAcct, null, "");
    } else if (vars.commandIn("DP")) {
      String strDPId = vars.getStringParameter("inpDPid");
      String strcAcctSchemaId = getValue(strDPId, 0);
      strDPId = getValue(strDPId, 1);
      printPageDataSheet(response, vars, null, null, strDPId, strcAcctSchemaId);
    } else if (vars.commandIn("PREVIOUS_RELATION")) {
      String strInitRecord = vars
          .getSessionValue("ReportGeneralLedgerJournalDetail.initRecordNumber");
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
          "ReportGeneralLedgerJournalDetail");
      int intRecordRange = strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange);
      if (strInitRecord.equals("") || strInitRecord.equals("0"))
        vars.setSessionValue("ReportGeneralLedgerJournalDetail.initRecordNumber", "0");
      else {
        int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
        initRecord -= intRecordRange;
        strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
        vars.setSessionValue("ReportGeneralLedgerJournalDetail.initRecordNumber", strInitRecord);
      }
      response.sendRedirect(strDireccion + request.getServletPath());
    } else if (vars.commandIn("NEXT_RELATION")) {
      String strInitRecord = vars
          .getSessionValue("ReportGeneralLedgerJournalDetail.initRecordNumber");
      String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
          "ReportGeneralLedgerJournalDetail");
      int intRecordRange = strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange);
      int initRecord = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));
      if (initRecord == 0)
        initRecord = 1;
      initRecord += intRecordRange;
      strInitRecord = ((initRecord < 0) ? "0" : Integer.toString(initRecord));
      vars.setSessionValue("ReportGeneralLedgerJournalDetail.initRecordNumber", strInitRecord);
      response.sendRedirect(strDireccion + request.getServletPath());
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strFactAcctGroupId, String strDateacct, String strDPId, String strcAcctSchemaId)
      throws IOException, ServletException {
    String strRecordRange = Utility.getContext(this, vars, "#RecordRange",
        "ReportGeneralLedgerJournalDetail");
    int intRecordRange = (strRecordRange.equals("") ? 0 : Integer.parseInt(strRecordRange));
    String strInitRecord = vars
        .getSessionValue("ReportGeneralLedgerJournalDetail.initRecordNumber");
    int initRecordNumber = (strInitRecord.equals("") ? 0 : Integer.parseInt(strInitRecord));

    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    ReportGeneralLedgerJournalDetailData[] data = null;
    log4j.debug("strDateacct:" + strDateacct);
    log4j.debug("after conversion strDateacct:" + strDateacct);
    if (strDPId == null)
      data = ReportGeneralLedgerJournalDetailData.select(this, strFactAcctGroupId, strDateacct,
          vars.getRole(), initRecordNumber, intRecordRange);
    else
      data = ReportGeneralLedgerJournalDetailData.selectByDP(this, strDPId, strcAcctSchemaId,
          vars.getRole());
    data = notshow(data, vars);
    boolean hasPrevious = !(data == null || data.length == 0 || initRecordNumber <= 1);
    boolean hasNext = !(data == null || data.length == 0 || data.length < intRecordRange);
    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ReportGeneralLedgerJournalDetail",
        false, "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareRelationBarTemplate(hasPrevious, hasNext);
    xmlDocument = xmlEngine
        .readXmlTemplate(
            "sa/elm/ob/finance/ad_reports/ReportGeneralLedger/ReportGeneralLedgerJournalDetail")
        .createXmlDocument();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "sa.elm.ob.finance.ad_reports.ReportGeneralLedger.ReportGeneralLedgerJournalDetail");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(),
          "ReportGeneralLedgerJournalDetail.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(),
          "ReportGeneralLedgerJournalDetail.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportGeneralLedgerJournalDetail");
      vars.removeMessage("ReportGeneralLedgerJournalDetail");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("strDocumentNo",
        Resource.getProperty("finance.documentno", vars.getLanguage()));
    xmlDocument.setParameter("acctno",
        Resource.getProperty("finance.accountno", vars.getLanguage()));
    xmlDocument.setParameter("name",
        Resource.getProperty("finance.journalentries.name", vars.getLanguage()));
    xmlDocument.setParameter("debit",
        Resource.getProperty("finance.g/lreport.debit", vars.getLanguage()));
    xmlDocument.setParameter("credit",
        Resource.getProperty("finance.g/lreport.credit", vars.getLanguage()));
    xmlDocument.setParameter("date", Resource.getProperty("finance.date", vars.getLanguage()));
    xmlDocument.setParameter("description",
        Resource.getProperty("finance.g/lreport.description", vars.getLanguage()));
    xmlDocument.setParameter("entry",
        Resource.getProperty("finance.journalentries.entry", vars.getLanguage()));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setData("structure1", data);
    vars.setSessionValue("ReportGeneralLedgerJournalDetail|FactAcctGroupId",
        strDateacct + "/" + strFactAcctGroupId);
    out.println(xmlDocument.print());
    out.close();
  }

  private ReportGeneralLedgerJournalDetailData[] notshow(
      ReportGeneralLedgerJournalDetailData[] data, VariablesSecureApp vars) {
    for (int i = 0; i < data.length; i++) {
      if ((data[i].ord.toString().equals("0") && data[i].docbasetype.toString().equals("ARR"))
          || !data[i].docbasetype.toString().equals("ARR")) {
        data[i].newstyle = "visibility: hidden";
        // log4j.info("style>>" + data[i].seqno + "///" + data[i].newstyle);
      }
    }
    return data;
  }

  private String getValue(String strText, int index) {
    log4j.warn("***************strText: " + strText);
    String[] tokens = strText.split("/");
    log4j.warn("***************size: " + tokens.length);
    return tokens[index];
  } // end of getServletInfo() method

  public String getServletInfo() {
    return "Servlet ReportGeneralLedgerJournalDetail.";
  } // end of getServletInfo() method
}
