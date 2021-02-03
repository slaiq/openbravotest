package sa.elm.ob.finance.ad_reports.gladjustment.header;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

public class GLAdjustmentReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/gladjustment/GLAdjustment.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String strReportName = "";
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));

      VariablesSecureApp vars = new VariablesSecureApp(request);
      OBContext.setAdminMode();

      if (action.equals("")) {
        log4j.debug("action");
        response.setCharacterEncoding("UTF-8");
        request.setAttribute("inpglJournalId", vars.getStringParameter("inpglJournalId"));
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("pdf")) {
        // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
        // "");
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/gladjustment/GLAdjustment.jrxml";
        String strOutput = "pdf";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        String inpglJournalId = request.getParameter("inpglJournalId");
        parameters.put("inpGlJournalId", inpglJournalId);

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("xls")) {
        // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
        // "");
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/gladjustment/GLAdjustment.jrxml";
        String strOutput = "xls";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        String inpglJournalId = request.getParameter("inpglJournalId");
        parameters.put("inpGlJournalId", inpglJournalId);

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }

    } catch (Exception e) {
      log4j.error("Exception in GLAdjustmentReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
