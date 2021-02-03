package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

/**
 * 
 * @author gopalakrishnan on 27/12/2016
 * 
 */
public class MonthlyClosingReportAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    MonthlyClosingReportDAO dao = null;
    JSONArray jsonArray = null;
    JSONObject jsonResponse = null;
    try {
      OBContext.setAdminMode();
      con = getConnection();
      // commonDAO = new CommonDAO(con);
      dao = new MonthlyClosingReportDAO(con);
      String action = (request.getParameter("action") == null ? "" : request.getParameter("action"));
      String yearId = (request.getParameter("yearId") == null ? "" : request.getParameter("yearId"));

      if (action.equals("getGeneralLedger")) {
        List<AcctSchema> schemaList = null;
        schemaList = dao.getSchema(
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
        jsonArray = new JSONArray();
        if (schemaList != null && schemaList.size() > 0) {
          for (AcctSchema schema : schemaList) {
            jsonResponse = new JSONObject();
            jsonResponse.put("schemaId", schema.getId());
            jsonResponse.put("schemaName", schema.getName());
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getYear")) {
        List<Year> yearList = null;
        yearList = dao.getYear(
            Utility.getContext(this, vars, "#AccessibleOrgTree", "ReportTrialBalance"),
            Utility.getContext(this, vars, "#User_Client", "ReportTrialBalance"));
        jsonArray = new JSONArray();
        if (yearList != null && yearList.size() > 0) {
          for (Year year : yearList) {
            jsonResponse = new JSONObject();
            jsonResponse.put("id", year.getId());
            jsonResponse.put("name", year.getFiscalYear());
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());

      } else if (action.equals("getPeriod")) {
        List<Period> periodList = null;
        periodList = dao.getPeriod(yearId);
        jsonArray = new JSONArray();
        if (periodList != null && periodList.size() > 0) {
          for (Period period : periodList) {
            jsonResponse = new JSONObject();
            jsonResponse.put("id", period.getId());
            jsonResponse.put("name", period.getName());
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());

      }
    } catch (final Exception e) {
      log4j.error("Error in MonthlyClosingReportAjax : ", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        con.close();
      } catch (final SQLException e) {
        log4j.error("Error in MonthlyClosingReportAjax : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    doPost(request, response);
  }

  public String getServletInfo() {
    return "MonthlyClosingReportAjax Servlet";
  }
}