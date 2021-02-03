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
package sa.elm.ob.finance.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.xmlEngine.XmlDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.utility.util.UtilityDAO;

public class SL_Journal_Period extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      String strWindowId = vars.getStringParameter("inpWindowId");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strDateAcct = vars.getStringParameter("inpdateacct");
      String year = strDateAcct.split("-")[2];
      if (new BigDecimal(year).compareTo(new BigDecimal(1250)) >= 0
          && (new BigDecimal(year).compareTo(new BigDecimal(1500)) <= 0))
        strDateAcct = UtilityDAO.convertToGregorian_tochar(strDateAcct);
      String strDateDoc = vars.getStringParameter("inpdatedoc");
      strDateDoc = UtilityDAO.convertToGregorian_tochar(strDateDoc);
      String strcPeriodId = vars.getStringParameter("inpcPeriodId");
      String strTabId = vars.getStringParameter("inpTabId");
      String strCurrencyId = vars.getStringParameter("inpcCurrencyId");
      String strAcctSchemaId = vars.getStringParameter("inpcAcctschemaId");
      String strCurrencyRateType = vars.getStringParameter("inpcurrencyratetype", "S");
      try {
        printPage(response, vars, strDateAcct, strDateDoc, strcPeriodId, strWindowId, strChanged,
            strTabId, strCurrencyId, strAcctSchemaId, strCurrencyRateType);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strDateAcctNew, String strDateDocNew, String strcPeriodIdNew, String strWindowId,
      String strChanged, String strTabId, String strCurrencyId, String strAcctSchemaId,
      String strCurrencyRateType) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    String stradClientId = vars.getClient();
    final String stradOrgId = vars.getGlobalVariable("inpadOrgId", "Efin_JournalCallout|adOrgId",
        "");
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeYearFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    OBError myMessage = null;
    String currencyRate = null;
    String budgetReferenceId = null;
    String originalDate = strDateAcctNew;
    if (strAcctSchemaId != null && !strAcctSchemaId.isEmpty()) {
      AcctSchema acctSchema = OBDal.getInstance().get(AcctSchema.class, strAcctSchemaId);
      try {
        currencyRate = SLJournalPeriodData.getCurrencyRate(this, strCurrencyId,
            acctSchema.getCurrency().getId(), strDateAcctNew, strCurrencyRateType, stradClientId,
            stradOrgId, strAcctSchemaId);
      } catch (Exception e) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), e.getMessage());
        log4j.warn("Currency does not exist. Exception:" + e);
      }
    }
    String strDateAcct = strDateAcctNew;
    String strcPeriodId = strcPeriodIdNew;
    // When DateDoc is changed, update DateAcct
    if (strChanged.equals("inpdatedoc")) {
      strDateAcct = strDateDocNew;
      strChanged = "inpdateacct";
      originalDate = strDateAcct;
    }
    // When Currency is changed, update DateAcct
    if (strChanged.equals("inpcCurrencyId")) {
      strDateAcct = strDateDocNew;
      strChanged = "inpdateacct";
    }
    // When DateAcct is changed, set C_Period_ID
    if (strChanged.equals("inpdateacct")) {
      try {
        strcPeriodId = SLJournalPeriodData.period(this, stradClientId, stradOrgId, strDateAcct);

        if (strcPeriodId.equals("")) {
          // StringBuffer resultado = new StringBuffer();
          // resultado.append("var calloutName='Efin_JournalCallout';\n\n");
          // resultado.append("var respuesta = new Array(");
          // resultado.append("new Array(\"ERROR\", \""
          // + Utility.messageBD(this, "PeriodNotValid", vars.getLanguage()) + "\")");
          // resultado.append(
          // ", new Array(\"JSEXECUTE\",
          // \"form.getFieldFromColumnName('EM_Efin_Budgetint_ID').setValue('')\")");
          // resultado.append(");");
          // xmlDocument.setParameter("array", resultado.toString());
          // xmlDocument.setParameter("frameName", "appFrame");
          // response.setContentType("text/html; charset=UTF-8");
          // PrintWriter out = response.getWriter();
          // out.println(xmlDocument.print());
          // out.close();
          // return;
        }
        strDateAcct = UtilityDAO
            .convertToHijriDate(yearFormat.format(dateFormat.parse(strDateAcct)));
      } catch (ParseException e) {
      }
    }
    boolean isStandardPeriod = true;
    if (strChanged.equals("inpcPeriodId") && !strcPeriodId.equals("")) {

      // When C_Period_ID is changed, check if in DateAcct range and set
      // to end date if not
      SLJournalPeriodData[] data = SLJournalPeriodData.select(this, strcPeriodId);
      String PeriodType = data[0].periodtype;
      String StartDate = data[0].startdate;
      String EndDate = data[0].enddate;
      SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      if (PeriodType.equals("S")) { // Standard Periods
        // out of range - set to last day
        if (DateTimeData.compare(this, StartDate, strDateAcct).equals("1")
            || DateTimeData.compare(this, EndDate, strDateAcct).equals("-1"))
          try {
            strDateAcct = UtilityDAO
                .convertToHijriDate(timeYearFormat.format(dateFormat.parse(EndDate)));
          } catch (ParseException e) {
          }

      } else {
        isStandardPeriod = false;
        try {
          strDateAcct = UtilityDAO
              .convertToHijriDate(timeYearFormat.format(timeFormat.parse(EndDate)));
        } catch (ParseException e) {
        }
      }
    }

    if (!strChanged.equals("inpcPeriodId") && !strChanged.equals("inpdateacct")) {
      try {
        strDateAcct = UtilityDAO
            .convertToHijriDate(timeYearFormat.format(dateFormat.parse(strDateAcct)));
        strDateAcct = dateFormat.format(dateFormat.parse(strDateAcct));
      } catch (ParseException e) {
      }
    }

    if (strChanged.equals("inpdateacct")) {
      Date endDate = null;
      budgetReferenceId = "1";
      try {
        endDate = dateFormat.parse(originalDate);
        budgetReferenceId = getPeriodStartDate(endDate, stradClientId);
      } catch (Exception e) {
        log4j.info("Process failed populating year based on accounting date", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='Efin_JournalCallout';\n\n");
    resultado.append("var respuesta = new Array(");
    resultado.append("new Array(\"inpdateacct\", \"" + strDateAcct + "\"),");
    if (!isStandardPeriod) {
      resultado.append("new Array(\"inpdatedoc\", \"" + strDateAcct + "\"),");
    }
    resultado.append("new Array(\"inpcPeriodId\", \"" + strcPeriodId + "\"),");
    if (myMessage != null) {
      resultado.append("new Array('MESSAGE', \"" + myMessage.getMessage() + "\"),");
    }
    if (currencyRate == null) {
      resultado.append("new Array(\"inpcurrencyrate\", \"" + "1" + "\")");
    } else {
      resultado.append("new Array(\"inpcurrencyrate\", \"" + currencyRate.toString() + "\")");
    }
    if (budgetReferenceId != null && !budgetReferenceId.equals("1")) {
      resultado.append(",new Array(\"inpemEfinBudgetintId\", \"" + budgetReferenceId + "\")");
    }

    if (budgetReferenceId != null && budgetReferenceId.equals("1")) {
      resultado.append(
          ", new Array(\"JSEXECUTE\", \"form.getFieldFromColumnName('EM_Efin_Budgetint_ID').setValue('')\")");
    }
    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  /**
   * 
   * @param date
   * @param clientId
   * @return budgetReferenceid
   */
  public static String getPeriodStartDate(Date date, String clientId) {
    final Logger log = LoggerFactory.getLogger(HijiridateDAO.class);
    PreparedStatement ps = null, ps1 = null;
    ResultSet rs = null, rs1 = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String startingDate = "", budgetReferenceId = "1";
    Connection conn = OBDal.getInstance().getConnection();
    try {
      ps = conn.prepareStatement(
          "select to_char(startdate,'dd-MM-yyyy') as startingDate from c_period where to_date('"
              + dateFormat.format(date) + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id ='" + clientId + "'");
      rs = ps.executeQuery();
      if (rs.next()) {
        startingDate = rs.getString("startingDate") == null ? "" : rs.getString("startingDate");
      }
      ps1 = conn.prepareStatement("select br.efin_budgetint_id from efin_budgetint br "
          + " join c_period fp on fp.c_period_id =br.fromperiod "
          + " join c_period tp on tp.c_period_id =br.toperiod " + " where to_date('" + startingDate
          + "','dd-MM-yyyy') between fp.startdate and tp.enddate and br.status ='OP' and br.ad_client_id ='"
          + clientId + "' limit 1");
      rs1 = ps1.executeQuery();
      if (rs1.next()) {
        budgetReferenceId = rs1.getString("efin_budgetint_id");
      }
    } catch (Exception e) {
      log.info("Error while getting Budget Reference in Adjustement callout", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
        if (rs1 != null)
          rs1.close();
        if (ps1 != null)
          ps1.close();

      } catch (Exception e) {
        log.debug("error while getting budget Reference", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
    return budgetReferenceId;
  }
}
