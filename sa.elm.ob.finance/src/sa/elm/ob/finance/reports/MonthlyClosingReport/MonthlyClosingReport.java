package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.WindowTreeData;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.OrganizationClosing;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.openbravo.model.financialmgmt.accounting.coa.Element;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * 
 * @author Gopalakrishnan
 * 
 */
public class MonthlyClosingReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/MonthlyClosingReport/MonthlyClosingReport.jsp";
  static Logger log4j = Logger.getLogger(MonthlyClosingReport.class);

  private static final String TMP_DIR_PATH = "/tmp";
  private static String strFileDirectory = "hcm/MonthlyClosingReport";
  private static String strFile = "MonthlyClosing.xls";
  private File tmpDir = null;
  private String InitRecordNumber = "0";
  public String id;
  public String name;
  public String isbalanced;
  public String pagebreak;
  public String padre;
  public String begining;
  public String end;
  public String previousYear;
  public String previousYearId;
  private static final String C_ELEMENT_VALUE_TABLE_ID = "188";

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("id"))
      return id;
    else if (fieldName.equalsIgnoreCase("name"))
      return name;
    else if (fieldName.equalsIgnoreCase("isbalanced"))
      return isbalanced;
    else if (fieldName.equals("pagebreak"))
      return pagebreak;
    else if (fieldName.equals("padre"))
      return padre;
    else if (fieldName.equals("begining"))
      return begining;
    else if (fieldName.equals("end"))
      return end;
    else if (fieldName.equals("previousYear"))
      return previousYear;
    else if (fieldName.equals("previousYearId"))
      return previousYearId;
    else {
      log4j.debug("Field does not exist: " + fieldName);
      return null;
    }
  }

  public MonthlyClosingReport() {
    super();
  }

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
    tmpDir = new File(TMP_DIR_PATH);
    if (!tmpDir.isDirectory()) {
      new File(TMP_DIR_PATH).mkdir();
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      MonthlyClosingReportDAO dao = null;

      VariablesSecureApp vars = new VariablesSecureApp(request);
      Connection con = getConnection();
      OBContext.setAdminMode();

      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

      if (action.equals("")) {
        log4j.debug("action:" + action);
        // request.setAttribute("inpglJournalId", vars.getStringParameter("inpglJournalId"));
        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("DownloadReport")) {
        try {
          dao = new MonthlyClosingReportDAO(con);
          String filePath = globalParameters.strFTPDirectory + "/" + strFileDirectory + "/"
              + strFile;
          File excelFile = null;

          String lang = vars.getLanguage().split("_")[0].toString();
          String inpAcctSchemaId = request.getParameter("inpAcctSchemaId");
          String inpYearId = request.getParameter("inpYearId");
          String inpPeriodId = request.getParameter("inpPeriodId");
          String periodEndDate = "", periodStDate = "";
          String yearStartDate = "";
          JSONObject jsonInput = new JSONObject();
          jsonInput.put("inpAcctSchemaId", inpAcctSchemaId.toString());
          jsonInput.put("inpYearId", inpYearId);
          // get accountId(c_elementid)
          AcctSchemaElement objAcctSchema = null;
          Element objElement = null;
          OBQuery<AcctSchemaElement> elementQuery = OBDal.getInstance().createQuery(
              AcctSchemaElement.class,
              "as e where e.type='AC' and e.accountingSchema.id='" + inpAcctSchemaId + "'");
          elementQuery.setMaxResult(1);
          if (elementQuery.list().size() > 0) {
            objAcctSchema = elementQuery.list().get(0);
          }
          if (objAcctSchema != null) {
            objElement = objAcctSchema.getAccountingElement();
          }
          // get end date of selected period
          Period objPeriod = OBDal.getInstance().get(Period.class, inpPeriodId);
          periodEndDate = dateFormat.format(objPeriod.getEndingDate());
          periodStDate = dateFormat.format(objPeriod.getStartingDate());
          /*
           * List<HashMap<String, String>> hashmaplist = loadData( request, response, vars,
           * strYearId, strYearRefId, strDateFrom, strDateTo, strDateFromRef, strDateToRef,
           * strAsDateTo, strAsDateToRef, strElementValue, strConImporte, strOrg, strLevel,
           * strConCodigo, strcAcctSchemaId, strPageNo)
           */
          // For each year, the initial and closing date is obtained
          Year year = OBDal.getInstance().get(Year.class, inpYearId);
          HashMap<String, Date> startingEndingDate = getStartingEndingDate(year);
          Date startingDate = startingEndingDate.get("startingDate");
          yearStartDate = dateFormat.format(startingDate);
          String strTreeOrg = treeOrg(this, vars.getClient());

          /*
           * BigDecimal openingBalance = dao.getopeningBalance(dateFormat.format(startingDate),
           * objAcctSchema.getId());
           */
          BigDecimal openingBalance = dao.getopeningBalance(
              Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
              Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
              Tree.getMembers(this, strTreeOrg, vars.getOrg()), inpAcctSchemaId, yearStartDate,
              periodStDate);

          log4j.debug("inpYearId:" + inpYearId + ">>inpAcctSchemaId:" + inpAcctSchemaId
              + ">>objElement.getId():" + objElement.getId());
          // add excel Sheet YTD trail Balance
          List<HashMap<String, String>> hashmaplist = loadData(request, response, vars, inpYearId,
              inpYearId, yearStartDate, periodEndDate, "", "", "", "", "", "N", vars.getOrg(), "S",
              "Y", inpAcctSchemaId, "", objElement.getId());

          excelFile = MonthlyClosingReportSheet.downloadMonthlyClosingYTD(filePath,
              vars.getClient(), vars.getOrg(), vars.getUser(), lang, jsonInput, hashmaplist,
              openingBalance);

          // add adjustment balance Sheet
          Date periodStartDate = objPeriod.getStartingDate();
          List<HashMap<String, String>> adjustmentBalanceList = loadAdjustmentBalance(request,
              response, vars, inpYearId, inpYearId, dateFormat.format(periodStartDate),
              periodEndDate, "", "", "", "", "", "N", vars.getOrg(), "S", "Y", inpAcctSchemaId, "",
              objElement.getId());
          excelFile = MonthlyClosingAdjustment.downloadAdjustmentBalance(excelFile,
              adjustmentBalanceList);

          // Add prev Month YTD Summary Sheet
          excelFile = MonthlyPreviousPTD.downloadPTDSummary(excelFile, adjustmentBalanceList);

          // add Current Month YTD Details Sheet
          excelFile = MonthlyCurrentMonthYTD.downloadCurrentMonthYTD(excelFile, hashmaplist);
          // add sheet 5 (31 expenses)
          List<HashMap<String, String>> expenesesOneList = loadExpenses(request, response, vars,
              inpYearId, inpYearId, dateFormat.format(periodStartDate), periodEndDate, "", "", "",
              "", "", "N", vars.getOrg(), "S", "Y", inpAcctSchemaId, "", objElement.getId(), "S5");

          excelFile = ExpenseSheet.downloadExpenseSheet(excelFile, expenesesOneList, "S5",
              vars.getClient());

          // add sheet 6 (22 expenses)
          List<HashMap<String, String>> expenesesTwoList = loadExpenses(request, response, vars,
              inpYearId, inpYearId, dateFormat.format(periodStartDate), periodEndDate, "", "", "",
              "", "", "N", vars.getOrg(), "S", "Y", inpAcctSchemaId, "", objElement.getId(), "S6");
          excelFile = ExpenseSheet.downloadExpenseSheet(excelFile, expenesesTwoList, "S6",
              vars.getClient());
          // add sheet 7 (21 expenses)
          List<HashMap<String, String>> expenesesThreeList = loadExpenses(request, response, vars,
              inpYearId, inpYearId, dateFormat.format(periodStartDate), periodEndDate, "", "", "",
              "", "", "N", vars.getOrg(), "S", "Y", inpAcctSchemaId, "", objElement.getId(), "S7");
          excelFile = ExpenseSheet.downloadExpenseSheet(excelFile, expenesesThreeList, "S7",
              vars.getClient());
          // add expense Summary
          List<HashMap<String, String>> expenseSummaryList = loadExpenseSummaryData(request,
              response, vars, inpYearId, inpYearId, yearStartDate, periodEndDate, "", "", "", "",
              "", "N", vars.getOrg(), "S", "Y", inpAcctSchemaId, "", objElement.getId());
          excelFile = ExpenseSummary.downloadExpenseSummary(excelFile, expenseSummaryList);
          // add revenue

          List<HashMap<String, String>> revenueList = loadRevenue(request, response, vars,
              inpYearId, inpYearId, dateFormat.format(periodStartDate), periodEndDate, "", "", "",
              "", "", "N", vars.getOrg(), "S", "Y", inpAcctSchemaId, "", objElement.getId());
          excelFile = RevenueSheet.downloadRevenueSheet(excelFile, revenueList);

          // download Monthly Closing report
          Cookie cookie = new Cookie("MonthlyClosingReport",
              request.getParameter("MonthlyClosingReport"));
          cookie.setMaxAge(10);
          cookie.setPath("/");
          response.addCookie(cookie);

          response.setHeader("Content-Type", getServletContext().getMimeType(excelFile.getName()));
          response.setHeader("Content-Disposition",
              "inline; filename=\"" + excelFile.getName() + "\"");
          BufferedInputStream input = null;
          BufferedOutputStream output = null;
          try {
            input = new BufferedInputStream(new FileInputStream(excelFile));
            output = new BufferedOutputStream(response.getOutputStream());

            byte[] buffer = new byte[response.getBufferSize()];
            for (int length = 0; (length = input.read(buffer)) > 0;) {
              output.write(buffer, 0, length);
            }
          } finally {
            if (output != null) {
              try {
                output.flush();
                output.close();
              } catch (IOException ignore) {

              }
            }
            if (input != null) {
              try {
                input.close();
              } catch (IOException ignore) {

              }
            }
          }
          return;
        } catch (final Exception e) {
          log4j.error("Exception in MonthlyClosingReport - Download : ", e);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in MonthlyClosingReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param request
   * @param response
   * @param vars
   * @param strYearId
   * @param strYearRefId
   * @param strDateFrom
   * @param strDateTo
   * @param strDateFromRef
   * @param strDateToRef
   * @param strAsDateTo
   * @param strAsDateToRef
   * @param strElementValue
   * @param strConImporte
   * @param strOrg
   * @param strLevel
   * @param strConCodigo
   * @param strcAcctSchemaId
   * @param strPageNo
   * @param elementId
   * @return YTD Trail Balance
   * @throws IOException
   * @throws ServletException
   */
  private List<HashMap<String, String>> loadExpenseSummaryData(HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, String strYearId, String strYearRefId,
      String strDateFrom, String strDateTo, String strDateFromRef, String strDateToRef,
      String strAsDateTo, String strAsDateToRef, String strElementValue, String strConImporte,
      String strOrg, String strLevel, String strConCodigo, String strcAcctSchemaId,
      String strPageNo, String elementId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: excel");
      log4j.debug("startDate:" + strDateFrom);
      log4j.debug("strDateTo:" + strDateTo);
    }

    List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
    String strCalculateOpening = "Y";
    try {

      String[][] strElementValueDes = new String[1][];
      for (int i = 0; i < 1; i++) {
        MonthlyClosingReport[] strElements = selectExpenseSummaryElements(this, elementId);

        strElementValueDes[i] = new String[strElements.length];
        for (int j = 0; j < strElements.length; j++) {
          strElementValueDes[i][j] = strElements[j].id;
        }
      }

      String strTreeOrg = treeOrg(this, vars.getClient());
      AccountTree[] acct = new AccountTree[1];

      AccountTreeData[][] elements = new AccountTreeData[1][];

      WindowTreeData[] dataTree = WindowTreeData.selectTreeIDWithTableId(this,
          Utility.stringList(vars.getClient()), C_ELEMENT_VALUE_TABLE_ID);
      String TreeID = "";
      if (dataTree != null && dataTree.length != 0)
        TreeID = dataTree[0].id;
      OBContext.setAdminMode(false);
      try {
        String openingEntryOwner = "";
        String openingEntryOwnerRef = "";
        // For each year, the initial and closing date is obtained
        Year year = OBDal.getInstance().get(Year.class, strYearId);
        Year yearRef = OBDal.getInstance().get(Year.class, strYearRefId);
        HashMap<String, Date> startingEndingDate = getStartingEndingDate(year);
        HashMap<String, Date> startingEndingDateRef = getStartingEndingDate(yearRef);
        // Years to be included as no closing is present
        String strYearsToClose = "";
        String strYearsToCloseRef = "";
        if (strCalculateOpening.equals("Y")) {
          strCalculateOpening = "N";
          String[] yearsInfo = getYearsToClose(startingEndingDate.get("startingDate"), strOrg,
              year.getCalendar(), strcAcctSchemaId, false);
          strYearsToClose = yearsInfo[0];
          openingEntryOwner = yearsInfo[1];
          if (strYearsToClose.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToClose = "," + strYearsToClose;
          }
          yearsInfo = getYearsToClose(startingEndingDateRef.get("startingDate"), strOrg,
              yearRef.getCalendar(), strcAcctSchemaId, true);
          strYearsToCloseRef = yearsInfo[0];
          openingEntryOwnerRef = yearsInfo[1];
          if (strYearsToCloseRef.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToCloseRef = "," + strYearsToCloseRef;
          }
        }

        for (int i = 0; i < 1; i++) {
          // All account tree is obtained
          elements[i] = AccountTreeData.select(this, TreeID);
          // For each account with movements in the year, debit and credit total amounts are
          // calculated according to fact_acct movements.
          AccountTreeData[] accounts = AccountTreeData.selectFactAcct(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
              Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
              strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strcAcctSchemaId,
              Tree.getMembers(this, strTreeOrg, strOrg), "'" + year.getFiscalYear() + "'",
              openingEntryOwner, DateTimeData.nDaysAfter(this, strDateTo, "1"),
              openingEntryOwnerRef);

          // Report tree is built with given the account tree, and the amounts obtained from
          // fact_acct
          log4j.debug("elements Length:>>" + elements.length);
          log4j.debug("accounts length:>>" + accounts.length);
          log4j.debug("elementNode:" + strElementValueDes[i].length);
          acct[i] = new AccountTree(vars, this, elements[i], accounts, strElementValueDes[i]);
          if (acct[i] != null) {
            acct[i].filterSVC();
            acct[i].filter(false, "S", false);
          } else if (log4j.isDebugEnabled())
            log4j.debug("acct null!!!");
        }

        AccountTreeData[][] trees = new AccountTreeData[1][];
        for (int i = 0; i < 1; i++)
          trees[i] = acct[i].getAccounts();

        for (int i = 0; i < trees.length; i++) {
          for (int j = 0; j < trees[i].length; j++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("elementLevel", trees[i][j].elementLevel);
            hashMap.put("account", trees[i][j].account);
            hashMap.put("name", trees[i][j].name);
            hashMap.put("qty", trees[i][j].qty);
            hashMap.put("qtyRef", trees[i][j].qtyRef);
            hashMap.put("level", trees[i][j].level);
            hashMapList.add(hashMap);
          }
        }
      } finally {
        OBContext.restorePreviousMode();
      }

    } catch (ArrayIndexOutOfBoundsException e) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(this, "ReportWithoutNodes", vars.getLanguage()));

    }
    return hashMapList;
  }

  private List<HashMap<String, String>> loadData(HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, String strYearId, String strYearRefId,
      String strDateFrom, String strDateTo, String strDateFromRef, String strDateToRef,
      String strAsDateTo, String strAsDateToRef, String strElementValue, String strConImporte,
      String strOrg, String strLevel, String strConCodigo, String strcAcctSchemaId,
      String strPageNo, String elementId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: excel");
      log4j.debug("startDate:" + strDateFrom);
      log4j.debug("strDateTo:" + strDateTo);
    }

    List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
    String strCalculateOpening = "Y";
    try {

      String[][] strElementValueDes = new String[1][];
      for (int i = 0; i < 1; i++) {
        MonthlyClosingReport[] strElements = selectElements(this, elementId);

        strElementValueDes[i] = new String[strElements.length];
        for (int j = 0; j < strElements.length; j++) {
          strElementValueDes[i][j] = strElements[j].id;
        }
      }

      String strTreeOrg = treeOrg(this, vars.getClient());
      AccountTree[] acct = new AccountTree[1];

      AccountTreeData[][] elements = new AccountTreeData[1][];

      WindowTreeData[] dataTree = WindowTreeData.selectTreeIDWithTableId(this,
          Utility.stringList(vars.getClient()), C_ELEMENT_VALUE_TABLE_ID);
      String TreeID = "";
      if (dataTree != null && dataTree.length != 0)
        TreeID = dataTree[0].id;
      OBContext.setAdminMode(false);
      try {
        String openingEntryOwner = "";
        String openingEntryOwnerRef = "";
        // For each year, the initial and closing date is obtained
        Year year = OBDal.getInstance().get(Year.class, strYearId);
        Year yearRef = OBDal.getInstance().get(Year.class, strYearRefId);
        HashMap<String, Date> startingEndingDate = getStartingEndingDate(year);
        HashMap<String, Date> startingEndingDateRef = getStartingEndingDate(yearRef);
        // Years to be included as no closing is present
        String strYearsToClose = "";
        String strYearsToCloseRef = "";
        if (strCalculateOpening.equals("Y")) {
          strCalculateOpening = "N";
          String[] yearsInfo = getYearsToClose(startingEndingDate.get("startingDate"), strOrg,
              year.getCalendar(), strcAcctSchemaId, false);
          strYearsToClose = yearsInfo[0];
          openingEntryOwner = yearsInfo[1];
          if (strYearsToClose.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToClose = "," + strYearsToClose;
          }
          yearsInfo = getYearsToClose(startingEndingDateRef.get("startingDate"), strOrg,
              yearRef.getCalendar(), strcAcctSchemaId, true);
          strYearsToCloseRef = yearsInfo[0];
          openingEntryOwnerRef = yearsInfo[1];
          if (strYearsToCloseRef.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToCloseRef = "," + strYearsToCloseRef;
          }
        }

        for (int i = 0; i < 1; i++) {
          // All account tree is obtained

          elements[i] = AccountTreeData.select(this, TreeID);

          // For each account with movements in the year, debit and credit total amounts are
          // calculated according to fact_acct movements.
          /*
           * AccountTreeData[] accounts = AccountTreeData.selectFactAcct(this,
           * Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
           * Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
           * strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strcAcctSchemaId,
           * Tree.getMembers(this, strTreeOrg, strOrg), "'" + year.getFiscalYear() + "'" +
           * strYearsToClose, openingEntryOwner, strDateFromRef, DateTimeData.nDaysAfter(this,
           * strDateToRef, "1"), "'" + yearRef.getFiscalYear() + "'", openingEntryOwnerRef);
           */

          AccountTreeData[] accounts = AccountTreeData.selectFactAcct(this,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
              Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
              strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strcAcctSchemaId,
              Tree.getMembers(this, strTreeOrg, strOrg), "'" + year.getFiscalYear() + "'",
              openingEntryOwner, DateTimeData.nDaysAfter(this, strDateTo, "1"),
              openingEntryOwnerRef);

          // Report tree is built with given the account tree, and the amounts obtained from
          // fact_acct
          log4j.debug("elements Length:>>" + elements.length);
          log4j.debug("accounts length:>>" + accounts.length);
          log4j.debug("elementNode:" + strElementValueDes[i].length);
          acct[i] = new AccountTree(vars, this, elements[i], accounts, strElementValueDes[i]);
          if (acct[i] != null) {
            acct[i].filterSVC();
            acct[i].filter(false, "S", false);
          } else if (log4j.isDebugEnabled())
            log4j.debug("acct null!!!");
        }

        AccountTreeData[][] trees = new AccountTreeData[1][];
        for (int i = 0; i < 1; i++)
          trees[i] = acct[i].getAccounts();

        for (int i = 0; i < trees.length; i++) {
          for (int j = 0; j < trees[i].length; j++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("elementLevel", trees[i][j].elementLevel);
            hashMap.put("account", trees[i][j].account);
            hashMap.put("name", trees[i][j].name);
            hashMap.put("qty", trees[i][j].qty);
            hashMap.put("qtyRef", trees[i][j].qtyRef);
            hashMap.put("level", trees[i][j].level);
            hashMapList.add(hashMap);
          }
        }
      } finally {
        OBContext.restorePreviousMode();
      }

    } catch (ArrayIndexOutOfBoundsException e) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(this, "ReportWithoutNodes", vars.getLanguage()));

    }
    return hashMapList;
  }

  /**
   * 
   * @param request
   * @param response
   * @param vars
   * @param strYearId
   * @param strYearRefId
   * @param strDateFrom
   * @param strDateTo
   * @param strDateFromRef
   * @param strDateToRef
   * @param strAsDateTo
   * @param strAsDateToRef
   * @param strElementValue
   * @param strConImporte
   * @param strOrg
   * @param strLevel
   * @param strConCodigo
   * @param strcAcctSchemaId
   * @param strPageNo
   * @param elementId
   * @return adjustmentBalance List
   * @throws IOException
   * @throws ServletException
   */
  private List<HashMap<String, String>> loadAdjustmentBalance(HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, String strYearId, String strYearRefId,
      String strDateFrom, String strDateTo, String strDateFromRef, String strDateToRef,
      String strAsDateTo, String strAsDateToRef, String strElementValue, String strConImporte,
      String strOrg, String strLevel, String strConCodigo, String strcAcctSchemaId,
      String strPageNo, String elementId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: excel");
      log4j.debug("startDate:" + strDateFrom);
      log4j.debug("strDateTo:" + strDateTo);
    }

    List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
    String strCalculateOpening = "Y";
    try {

      String[][] strElementValueDes = new String[1][];
      for (int i = 0; i < 1; i++) {
        MonthlyClosingReport[] strElements = selectElements(this, elementId);

        strElementValueDes[i] = new String[strElements.length];
        for (int j = 0; j < strElements.length; j++) {
          strElementValueDes[i][j] = strElements[j].id;
        }
      }

      String strTreeOrg = treeOrg(this, vars.getClient());
      AccountTree[] acct = new AccountTree[1];

      AccountTreeData[][] elements = new AccountTreeData[1][];

      WindowTreeData[] dataTree = WindowTreeData.selectTreeIDWithTableId(this,
          Utility.stringList(vars.getClient()), C_ELEMENT_VALUE_TABLE_ID);
      String TreeID = "";
      if (dataTree != null && dataTree.length != 0)
        TreeID = dataTree[0].id;
      OBContext.setAdminMode(false);
      try {
        String openingEntryOwner = "";
        String openingEntryOwnerRef = "";
        // For each year, the initial and closing date is obtained
        Year year = OBDal.getInstance().get(Year.class, strYearId);
        Year yearRef = OBDal.getInstance().get(Year.class, strYearRefId);
        HashMap<String, Date> startingEndingDate = getStartingEndingDate(year);
        HashMap<String, Date> startingEndingDateRef = getStartingEndingDate(yearRef);
        // Years to be included as no closing is present
        String strYearsToClose = "";
        String strYearsToCloseRef = "";
        if (strCalculateOpening.equals("Y")) {
          strCalculateOpening = "N";
          String[] yearsInfo = getYearsToClose(startingEndingDate.get("startingDate"), strOrg,
              year.getCalendar(), strcAcctSchemaId, false);
          strYearsToClose = yearsInfo[0];
          openingEntryOwner = yearsInfo[1];
          if (strYearsToClose.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToClose = "," + strYearsToClose;
          }
          yearsInfo = getYearsToClose(startingEndingDateRef.get("startingDate"), strOrg,
              yearRef.getCalendar(), strcAcctSchemaId, true);
          strYearsToCloseRef = yearsInfo[0];
          openingEntryOwnerRef = yearsInfo[1];
          if (strYearsToCloseRef.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToCloseRef = "," + strYearsToCloseRef;
          }
        }

        for (int i = 0; i < 1; i++) {
          // All account tree is obtained
          elements[i] = AccountTreeData.selectAdjustment(this, TreeID);
          // For each account with movements in the year, debit and credit total amounts are
          // calculated according to fact_acct movements.
          log4j.debug("strDateFrom:" + strDateFrom);
          log4j.debug("OrgIds:"
              + Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"));
          log4j.debug("ClientId's:"
              + Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"));
          log4j.debug("strcAcctSchemaId:" + strcAcctSchemaId);
          log4j.debug("strDateTo:" + strDateTo);
          log4j.debug("year.getFiscalYear():" + year.getFiscalYear());
          log4j.debug("strYearsToClose:" + strYearsToClose);
          log4j.debug("yearRef.getFiscalYear():" + yearRef.getFiscalYear());
          log4j.debug("strYearsToCloseRef:" + strYearsToCloseRef);

          AccountTreeData[] accounts = AccountTreeData.selectAdjustmentBalance(this, strDateFrom,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
              Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
              strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strcAcctSchemaId,
              Tree.getMembers(this, strTreeOrg, strOrg),
              "'" + year.getFiscalYear() + "'" + strYearsToClose, openingEntryOwner, strDateFromRef,
              DateTimeData.nDaysAfter(this, strDateToRef, "1"), "'" + yearRef.getFiscalYear() + "'",
              openingEntryOwnerRef);

          // Report tree is built with given the account tree, and the amounts obtained from
          // fact_acct
          log4j.debug("elements Length:>>" + elements.length);
          log4j.debug("accounts length:>>" + accounts.length);
          log4j.debug("elementNode:" + strElementValueDes[i].length);
          acct[i] = new AccountTree(vars, this, elements[i], accounts, strElementValueDes[i]);
          if (acct[i] != null) {
            acct[i].filterSVC();
            acct[i].filter(false, "S", false);
          } else if (log4j.isDebugEnabled())
            log4j.debug("acct null!!!");
        }

        AccountTreeData[][] trees = new AccountTreeData[1][];
        for (int i = 0; i < 1; i++)
          trees[i] = acct[i].getAccounts();

        for (int i = 0; i < trees.length; i++) {
          for (int j = 0; j < trees[i].length; j++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("elementLevel", trees[i][j].elementLevel);
            hashMap.put("account", trees[i][j].account);
            hashMap.put("name", trees[i][j].name);
            hashMap.put("qty", trees[i][j].qty);
            hashMap.put("qtycredit", trees[i][j].qtycredit);
            hashMap.put("qtyRef", trees[i][j].qtyRef);
            hashMap.put("qtycreditRef", trees[i][j].qtycreditRef);
            hashMap.put("level", trees[i][j].level);
            hashMapList.add(hashMap);
            // qty--current month dr
            // qtyref - current month opening dr
            // qtyCredit--current month cr
            // qtycreditRef - current month opening cr
          }
        }
      } finally {
        OBContext.restorePreviousMode();
      }

    } catch (ArrayIndexOutOfBoundsException e) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(this, "ReportWithoutNodes", vars.getLanguage()));

    }
    return hashMapList;
  }

  /**
   * 
   * @param request
   * @param response
   * @param vars
   * @param strYearId
   * @param strYearRefId
   * @param strDateFrom
   * @param strDateTo
   * @param strDateFromRef
   * @param strDateToRef
   * @param strAsDateTo
   * @param strAsDateToRef
   * @param strElementValue
   * @param strConImporte
   * @param strOrg
   * @param strLevel
   * @param strConCodigo
   * @param strcAcctSchemaId
   * @param strPageNo
   * @param elementId
   * @return adjustmentBalance List
   * @throws IOException
   * @throws ServletException
   */
  private List<HashMap<String, String>> loadExpenses(HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, String strYearId, String strYearRefId,
      String strDateFrom, String strDateTo, String strDateFromRef, String strDateToRef,
      String strAsDateTo, String strAsDateToRef, String strElementValue, String strConImporte,
      String strOrg, String strLevel, String strConCodigo, String strcAcctSchemaId,
      String strPageNo, String elementId, String SheetNo) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: excel");
      log4j.debug("startDate:" + strDateFrom);
      log4j.debug("strDateTo:" + strDateTo);
    }

    List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
    String strCalculateOpening = "Y";
    try {

      String[][] strElementValueDes = new String[1][];
      for (int i = 0; i < 1; i++) {
        MonthlyClosingReport[] strElements = selectExpenseElements(this, vars.getClient(), SheetNo);

        strElementValueDes[i] = new String[strElements.length];
        for (int j = 0; j < strElements.length; j++) {
          strElementValueDes[i][j] = strElements[j].id;
        }
      }

      String strTreeOrg = treeOrg(this, vars.getClient());
      AccountTree[] acct = new AccountTree[1];

      AccountTreeData[][] elements = new AccountTreeData[1][];

      WindowTreeData[] dataTree = WindowTreeData.selectTreeIDWithTableId(this,
          Utility.stringList(vars.getClient()), C_ELEMENT_VALUE_TABLE_ID);
      String TreeID = "";
      if (dataTree != null && dataTree.length != 0)
        TreeID = dataTree[0].id;
      OBContext.setAdminMode(false);
      try {
        String openingEntryOwner = "";
        String openingEntryOwnerRef = "";
        // For each year, the initial and closing date is obtained
        Year year = OBDal.getInstance().get(Year.class, strYearId);
        Year yearRef = OBDal.getInstance().get(Year.class, strYearRefId);
        HashMap<String, Date> startingEndingDate = getStartingEndingDate(year);
        HashMap<String, Date> startingEndingDateRef = getStartingEndingDate(yearRef);
        // Years to be included as no closing is present
        String strYearsToClose = "";
        String strYearsToCloseRef = "";
        if (strCalculateOpening.equals("Y")) {
          strCalculateOpening = "N";
          String[] yearsInfo = getYearsToClose(startingEndingDate.get("startingDate"), strOrg,
              year.getCalendar(), strcAcctSchemaId, false);
          strYearsToClose = yearsInfo[0];
          openingEntryOwner = yearsInfo[1];
          if (strYearsToClose.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToClose = "," + strYearsToClose;
          }
          yearsInfo = getYearsToClose(startingEndingDateRef.get("startingDate"), strOrg,
              yearRef.getCalendar(), strcAcctSchemaId, true);
          strYearsToCloseRef = yearsInfo[0];
          openingEntryOwnerRef = yearsInfo[1];
          if (strYearsToCloseRef.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToCloseRef = "," + strYearsToCloseRef;
          }
        }

        for (int i = 0; i < 1; i++) {
          // All account tree is obtained
          elements[i] = AccountTreeData.selectAdjustment(this, TreeID);
          // For each account with movements in the year, debit and credit total amounts are
          // calculated according to fact_acct movements.
          log4j.debug("strDateFrom:" + strDateFrom);
          log4j.debug("OrgIds:"
              + Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"));
          log4j.debug("ClientId's:"
              + Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"));
          log4j.debug("strcAcctSchemaId:" + strcAcctSchemaId);
          log4j.debug("strDateTo:" + strDateTo);
          log4j.debug("year.getFiscalYear():" + year.getFiscalYear());
          log4j.debug("strYearsToClose:" + strYearsToClose);
          AccountTreeData[] accounts = AccountTreeData.selectExpenses(this, strDateFrom,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
              Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
              strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strcAcctSchemaId,
              Tree.getMembers(this, strTreeOrg, strOrg),
              "'" + year.getFiscalYear() + "'" + strYearsToClose, openingEntryOwner, strDateFromRef,
              DateTimeData.nDaysAfter(this, strDateToRef, "1"), "'" + yearRef.getFiscalYear() + "'",
              openingEntryOwnerRef, "'" + year.getFiscalYear() + "'");

          // Report tree is built with given the account tree, and the amounts obtained from
          // fact_acct
          log4j.debug("elements Length:>>" + elements.length);
          log4j.debug("accounts length:>>" + accounts.length);
          log4j.debug("elementNode:" + strElementValueDes[i].length);
          acct[i] = new AccountTree(vars, this, elements[i], accounts, strElementValueDes[i]);
          if (acct[i] != null) {
            acct[i].filterSVC();
            acct[i].filter(false, "S", false);
          } else if (log4j.isDebugEnabled())
            log4j.debug("acct null!!!");
        }

        AccountTreeData[][] trees = new AccountTreeData[1][];
        for (int i = 0; i < 1; i++)
          trees[i] = acct[i].getAccounts();

        for (int i = 0; i < trees.length; i++) {
          for (int j = 0; j < trees[i].length; j++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("elementLevel", trees[i][j].elementLevel);
            hashMap.put("account", trees[i][j].account);
            hashMap.put("name", trees[i][j].name);
            hashMap.put("qty", trees[i][j].qty);
            hashMap.put("qtycredit", trees[i][j].qtycredit);
            hashMap.put("qtyRef", trees[i][j].qtyRef);
            hashMap.put("qtycreditRef", trees[i][j].qtycreditRef);
            hashMap.put("level", trees[i][j].level);
            hashMap.put("project", trees[i][j].project);
            hashMapList.add(hashMap);
            // qty--Actual Balance
            // qtyref - Opening Balance or Prev Month Closing Balance
            // qtyCredit--current Budget Balance
          }
        }
      } finally {
        OBContext.restorePreviousMode();
      }

    } catch (ArrayIndexOutOfBoundsException e) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(this, "ReportWithoutNodes", vars.getLanguage()));

    }
    return hashMapList;
  }

  private List<HashMap<String, String>> loadRevenue(HttpServletRequest request,
      HttpServletResponse response, VariablesSecureApp vars, String strYearId, String strYearRefId,
      String strDateFrom, String strDateTo, String strDateFromRef, String strDateToRef,
      String strAsDateTo, String strAsDateToRef, String strElementValue, String strConImporte,
      String strOrg, String strLevel, String strConCodigo, String strcAcctSchemaId,
      String strPageNo, String elementId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: excel");
      log4j.debug("startDate:" + strDateFrom);
      log4j.debug("strDateTo:" + strDateTo);
    }

    List<HashMap<String, String>> hashMapList = new ArrayList<HashMap<String, String>>();
    String strCalculateOpening = "Y";
    try {

      String[][] strElementValueDes = new String[1][];
      for (int i = 0; i < 1; i++) {
        MonthlyClosingReport[] strElements = selectRevenueElements(this, elementId);

        strElementValueDes[i] = new String[strElements.length];
        for (int j = 0; j < strElements.length; j++) {
          strElementValueDes[i][j] = strElements[j].id;
        }
      }

      String strTreeOrg = treeOrg(this, vars.getClient());
      AccountTree[] acct = new AccountTree[1];

      AccountTreeData[][] elements = new AccountTreeData[1][];

      WindowTreeData[] dataTree = WindowTreeData.selectTreeIDWithTableId(this,
          Utility.stringList(vars.getClient()), C_ELEMENT_VALUE_TABLE_ID);
      String TreeID = "";
      if (dataTree != null && dataTree.length != 0)
        TreeID = dataTree[0].id;
      OBContext.setAdminMode(false);
      try {
        String openingEntryOwner = "";
        String openingEntryOwnerRef = "";
        // For each year, the initial and closing date is obtained
        Year year = OBDal.getInstance().get(Year.class, strYearId);
        Year yearRef = OBDal.getInstance().get(Year.class, strYearRefId);
        HashMap<String, Date> startingEndingDate = getStartingEndingDate(year);
        HashMap<String, Date> startingEndingDateRef = getStartingEndingDate(yearRef);
        // Years to be included as no closing is present
        String strYearsToClose = "";
        String strYearsToCloseRef = "";
        if (strCalculateOpening.equals("Y")) {
          strCalculateOpening = "N";
          String[] yearsInfo = getYearsToClose(startingEndingDate.get("startingDate"), strOrg,
              year.getCalendar(), strcAcctSchemaId, false);
          strYearsToClose = yearsInfo[0];
          openingEntryOwner = yearsInfo[1];
          if (strYearsToClose.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToClose = "," + strYearsToClose;
          }
          yearsInfo = getYearsToClose(startingEndingDateRef.get("startingDate"), strOrg,
              yearRef.getCalendar(), strcAcctSchemaId, true);
          strYearsToCloseRef = yearsInfo[0];
          openingEntryOwnerRef = yearsInfo[1];
          if (strYearsToCloseRef.length() > 0) {
            strCalculateOpening = "Y";
            strYearsToCloseRef = "," + strYearsToCloseRef;
          }
        }

        for (int i = 0; i < 1; i++) {
          // All account tree is obtained
          elements[i] = AccountTreeData.select(this, TreeID);
          // For each account with movements in the year, debit and credit total amounts are
          // calculated according to fact_acct movements.
          log4j.debug("strDateFrom:" + strDateFrom);
          log4j.debug("OrgIds:"
              + Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"));
          log4j.debug("ClientId's:"
              + Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"));
          log4j.debug("strcAcctSchemaId:" + strcAcctSchemaId);
          log4j.debug("strDateTo:" + strDateTo);
          log4j.debug("year.getFiscalYear():" + year.getFiscalYear());
          log4j.debug("strYearsToClose:" + strYearsToClose);
          AccountTreeData[] accounts = AccountTreeData.selectFactAcctRevenue(this, strDateFrom,
              Utility.getContext(this, vars, "#AccessibleOrgTree", "GeneralAccountingReports"),
              Utility.getContext(this, vars, "#User_Client", "GeneralAccountingReports"),
              strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strcAcctSchemaId,
              Tree.getMembers(this, strTreeOrg, strOrg),
              "'" + year.getFiscalYear() + "'" + strYearsToClose, openingEntryOwner, strDateFromRef,
              DateTimeData.nDaysAfter(this, strDateToRef, "1"), "'" + yearRef.getFiscalYear() + "'",
              openingEntryOwnerRef);

          // Report tree is built with given the account tree, and the amounts obtained from
          // fact_acct
          log4j.debug("elements Length:>>" + elements.length);
          log4j.debug("accounts length:>>" + accounts.length);
          log4j.debug("elementNode:" + strElementValueDes[i].length);
          acct[i] = new AccountTree(vars, this, elements[i], accounts, strElementValueDes[i]);
          if (acct[i] != null) {
            acct[i].filterSVC();
            acct[i].filter(false, "S", false);
          } else if (log4j.isDebugEnabled())
            log4j.debug("acct null!!!");
        }

        AccountTreeData[][] trees = new AccountTreeData[1][];
        for (int i = 0; i < 1; i++)
          trees[i] = acct[i].getAccounts();

        for (int i = 0; i < trees.length; i++) {
          for (int j = 0; j < trees[i].length; j++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("elementLevel", trees[i][j].elementLevel);
            hashMap.put("account", trees[i][j].account);
            hashMap.put("name", trees[i][j].name);
            hashMap.put("qty", trees[i][j].qty);
            hashMap.put("qtycredit", trees[i][j].qtycredit);
            hashMap.put("qtyRef", trees[i][j].qtyRef);
            hashMap.put("qtycreditRef", trees[i][j].qtycreditRef);
            hashMap.put("level", trees[i][j].level);
            hashMap.put("project", trees[i][j].project);
            hashMapList.add(hashMap);
            // qty--Actual Balance
            // qtyref - Opening Balance or Prev Month Closing Balance
            // qtyCredit--current Budget Balance
          }
        }
      } finally {
        OBContext.restorePreviousMode();
      }

    } catch (ArrayIndexOutOfBoundsException e) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(this, "ReportWithoutNodes", vars.getLanguage()));

    }
    return hashMapList;
  }

  public MonthlyClosingReport[] selectElements(ConnectionProvider connectionProvider,
      String accountTreeid) throws ServletException {
    return selectElements(connectionProvider, accountTreeid, 0, 0);
  }

  public MonthlyClosingReport[] selectElements(ConnectionProvider connectionProvider, String grp,
      int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql + " select c_elementvalue_id AS ID from c_elementvalue "
        + " where c_elementvalue_id in " + " (select node_id from ad_tree tree "
        + " join ad_treenode node on node.ad_tree_id=tree.ad_tree_id "
        + " where parent_id='0' and tree.ad_table_id='188') "
        + "  and c_element_id=? and elementlevel ='E' order by value";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, grp);

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
        MonthlyClosingReport objectGeneralAccountingReportsData = new MonthlyClosingReport();
        objectGeneralAccountingReportsData.id = UtilSql.getValue(result, "id");
        objectGeneralAccountingReportsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectGeneralAccountingReportsData);
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
    MonthlyClosingReport objectGeneralAccountingReportsData[] = new MonthlyClosingReport[vector
        .size()];
    vector.copyInto(objectGeneralAccountingReportsData);
    return (objectGeneralAccountingReportsData);
  }

  public MonthlyClosingReport[] selectExpenseElements(ConnectionProvider connectionProvider,
      String ClientId, String SheetNo) throws ServletException {
    return selectExpenseElements(connectionProvider, ClientId, SheetNo, 0, 0);
  }

  public MonthlyClosingReport[] selectExpenseElements(ConnectionProvider connectionProvider,
      String Client, String SheetNo, int firstRegister, int numberRegisters)
      throws ServletException {
    String strSql = "";
    strSql = strSql + " select c_elementvalue_id as ID from efin_monthly_rpt_node nde "
        + " join efin_monthlyreport_group grp on grp.efin_monthlyreport_group_id=nde.efin_monthlyreport_group_id "
        + " where grp.sheet='" + SheetNo + "' and nde.ad_client_id=? ";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, Client);

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
        MonthlyClosingReport objectGeneralAccountingReportsData = new MonthlyClosingReport();
        objectGeneralAccountingReportsData.id = UtilSql.getValue(result, "id");
        objectGeneralAccountingReportsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectGeneralAccountingReportsData);
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
    MonthlyClosingReport objectGeneralAccountingReportsData[] = new MonthlyClosingReport[vector
        .size()];
    vector.copyInto(objectGeneralAccountingReportsData);
    return (objectGeneralAccountingReportsData);
  }

  public MonthlyClosingReport[] selectExpenseSummaryElements(ConnectionProvider connectionProvider,
      String elementId) throws ServletException {
    return selectExpenseSummaryElements(connectionProvider, elementId, 0, 0);
  }

  public MonthlyClosingReport[] selectExpenseSummaryElements(ConnectionProvider connectionProvider,
      String elementId, int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql + " select c_elementvalue_id AS ID from c_elementvalue "
        + " where c_elementvalue_id in " + " (select node_id from ad_tree tree "
        + " join ad_treenode node on node.ad_tree_id=tree.ad_tree_id "
        + " where tree.ad_table_id='188') "
        + "  and c_element_id=? and elementlevel ='E' and accounttype ='E' order by value";
    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, elementId);

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
        MonthlyClosingReport objectGeneralAccountingReportsData = new MonthlyClosingReport();
        objectGeneralAccountingReportsData.id = UtilSql.getValue(result, "id");
        objectGeneralAccountingReportsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectGeneralAccountingReportsData);
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
    MonthlyClosingReport objectGeneralAccountingReportsData[] = new MonthlyClosingReport[vector
        .size()];
    vector.copyInto(objectGeneralAccountingReportsData);
    return (objectGeneralAccountingReportsData);
  }

  public MonthlyClosingReport[] selectRevenueElements(ConnectionProvider connectionProvider,
      String ElementId) throws ServletException {
    return selectRevenueElements(connectionProvider, ElementId, 0, 0);
  }

  public MonthlyClosingReport[] selectRevenueElements(ConnectionProvider connectionProvider,
      String ElementId, int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql + " select c_elementvalue_id AS ID from c_elementvalue "
        + " where c_elementvalue_id in " + " (select node_id from ad_tree tree "
        + " join ad_treenode node on node.ad_tree_id=tree.ad_tree_id "
        + " where tree.ad_table_id='188') "
        + "  and c_element_id=? and elementlevel ='E' and accounttype ='R' order by value";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ElementId);

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
        MonthlyClosingReport objectGeneralAccountingReportsData = new MonthlyClosingReport();
        objectGeneralAccountingReportsData.id = UtilSql.getValue(result, "id");
        objectGeneralAccountingReportsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectGeneralAccountingReportsData);
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
    MonthlyClosingReport objectGeneralAccountingReportsData[] = new MonthlyClosingReport[vector
        .size()];
    vector.copyInto(objectGeneralAccountingReportsData);
    return (objectGeneralAccountingReportsData);
  }

  public static String treeOrg(ConnectionProvider connectionProvider, String client)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "        SELECT AD_TREE_ORG_ID FROM AD_CLIENTINFO"
        + "        WHERE AD_CLIENT_ID = ?";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, client);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "ad_tree_org_id");
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

  private HashMap<String, Date> getStartingEndingDate(Year year) {
    final StringBuilder hqlString = new StringBuilder();
    HashMap<String, Date> result = new HashMap<String, Date>();
    result.put("startingDate", null);
    result.put("endingDate", null);
    hqlString.append("select min(p.startingDate) as startingDate, max(p.endingDate) as endingDate");
    hqlString.append(" from FinancialMgmtPeriod as p");
    hqlString.append(" where p.year = :year");

    final Session session = OBDal.getInstance().getSession();
    final Query query = session.createQuery(hqlString.toString());
    query.setParameter("year", year);
    for (Object resultObject : query.list()) {
      if (resultObject.getClass().isArray()) {
        final Object[] values = (Object[]) resultObject;
        result.put("startingDate", (Date) values[0]);
        result.put("endingDate", (Date) values[1]);
      }
    }
    return result;
  }

  private String[] getYearsToClose(Date startingDate, String strOrg, Calendar calendar,
      String strcAcctSchemaId, boolean isYearRef) {
    String openingEntryOwner = "";
    ArrayList<Year> previousYears = getOrderedPreviousYears(startingDate, calendar);
    Set<String> notClosedYears = new HashSet<String>();
    for (Year previousYear : previousYears) {
      for (Organization org : getCalendarOwnerOrgs(strOrg)) {
        if (isNotClosed(previousYear, org, strcAcctSchemaId)) {
          notClosedYears.add(previousYear.getFiscalYear());
        } else {
          openingEntryOwner = previousYear.getFiscalYear();
        }
      }
    }
    String[] result = { Utility.getInStrSet(notClosedYears), openingEntryOwner };
    return result;
  }

  private ArrayList<Year> getOrderedPreviousYears(Date startingDate, Calendar calendar) {
    final StringBuilder hqlString = new StringBuilder();
    ArrayList<Year> result = new ArrayList<Year>();
    hqlString.append("select y");
    hqlString.append(" from FinancialMgmtYear y, FinancialMgmtPeriod as p");
    hqlString.append(
        " where p.year = y  and p.endingDate < :date and y.calendar = :calendar order by p.startingDate");
    final Session session = OBDal.getInstance().getSession();
    final Query query = session.createQuery(hqlString.toString());
    query.setParameter("date", startingDate);
    query.setParameter("calendar", calendar);
    for (Object resultObject : query.list()) {
      final Year previousYear = (Year) resultObject;
      if (!(result.contains(previousYear))) {
        result.add(previousYear);
      }
    }
    return result;
  }

  private Set<Organization> getCalendarOwnerOrgs(String strOrg) {
    Set<Organization> calendarOwnerOrgs = new HashSet<Organization>();
    Organization organization = OBDal.getInstance().get(Organization.class, strOrg);
    if (organization.isAllowPeriodControl()) {
      calendarOwnerOrgs.add(organization);
    }
    for (String child : new OrganizationStructureProvider().getChildTree(strOrg, false)) {
      calendarOwnerOrgs.addAll(getCalendarOwnerOrgs(child));
    }
    return calendarOwnerOrgs;
  }

  private boolean isNotClosed(Year year, Organization org, String strcAcctSchemaId) {
    OBContext.setAdminMode(false);
    try {
      OBCriteria<OrganizationClosing> obc = OBDal.getInstance()
          .createCriteria(OrganizationClosing.class);
      obc.createAlias(OrganizationClosing.PROPERTY_ORGACCTSCHEMA, "oa");
      obc.add(Restrictions.eq("organization", org));
      obc.add(Restrictions.eq(OrganizationClosing.PROPERTY_YEAR, year));
      obc.add(Restrictions.eq("oa.accountingSchema.id", strcAcctSchemaId));
      obc.add(Restrictions.isNotNull(OrganizationClosing.PROPERTY_CLOSINGFACTACCTGROUP));
      obc.setMaxResults(1);
      return obc.uniqueResult() == null ? true : false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static String selectPyG(ConnectionProvider connectionProvider, String accountType,
      String dateFrom, String dateTo, String acctschema, String year, String adOrgId)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "      SELECT COALESCE(SUM(AMTACCTCR-AMTACCTDR), 0) AS NAME"
        + "      FROM FACT_ACCT, C_PERIOD, C_YEAR, (SELECT C_ELEMENTVALUE_ID"
        + "                                         FROM C_ELEMENTVALUE"
        + "                                         WHERE C_ELEMENTVALUE.ACCOUNTTYPE = ?) AA"
        + "     WHERE FACT_ACCT.C_PERIOD_ID = C_PERIOD.C_PERIOD_ID"
        + "     AND C_PERIOD.C_YEAR_ID = C_YEAR.C_YEAR_ID"
        + "     AND FACT_ACCT.ACCOUNT_ID = AA.C_ELEMENTVALUE_ID"
        + "     AND FACT_ACCT.FACTACCTTYPE <> 'R'" + "     AND 1=1";
    strSql = strSql + ((dateFrom == null || dateFrom.equals("")) ? ""
        : "  AND FACT_ACCT.DATEACCT >= TO_DATE(?) ");
    strSql = strSql
        + ((dateTo == null || dateTo.equals("")) ? "" : "  AND FACT_ACCT.DATEACCT < TO_DATE(?) ");
    strSql = strSql + ((acctschema == null || acctschema.equals("")) ? ""
        : "  AND FACT_ACCT.C_ACCTSCHEMA_ID = ? ");
    strSql = strSql + "     AND C_YEAR.YEAR IN (";
    strSql = strSql + ((year == null || year.equals("")) ? "" : year);
    strSql = strSql + ")" + "     AND FACT_ACCT.AD_ORG_ID IN (";
    strSql = strSql + ((adOrgId == null || adOrgId.equals("")) ? "" : adOrgId);
    strSql = strSql + ")";

    ResultSet result;
    String strReturn = "";
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, accountType);
      if (dateFrom != null && !(dateFrom.equals(""))) {
        iParameter++;
        UtilSql.setValue(st, iParameter, 12, null, dateFrom);
      }
      if (dateTo != null && !(dateTo.equals(""))) {
        iParameter++;
        UtilSql.setValue(st, iParameter, 12, null, dateTo);
      }
      if (acctschema != null && !(acctschema.equals(""))) {
        iParameter++;
        UtilSql.setValue(st, iParameter, 12, null, acctschema);
      }
      if (year != null && !(year.equals(""))) {
      }
      if (adOrgId != null && !(adOrgId.equals(""))) {
      }

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "name");
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

  public static String incomesummary(ConnectionProvider connectionProvider, String cAcctschemaId)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "      SELECT C_VALIDCOMBINATION.ACCOUNT_ID AS ID"
        + "      FROM C_ACCTSCHEMA_GL, C_VALIDCOMBINATION, C_ELEMENTVALUE"
        + "      WHERE C_ACCTSCHEMA_GL.INCOMESUMMARY_ACCT = C_VALIDCOMBINATION.C_VALIDCOMBINATION_ID"
        + "      AND C_VALIDCOMBINATION.ACCOUNT_ID = C_ELEMENTVALUE.C_ELEMENTVALUE_ID"
        + "      AND C_ACCTSCHEMA_GL.C_ACCTSCHEMA_ID = ?";

    ResultSet result;
    String strReturn = "";
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, cAcctschemaId);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "id");
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
}