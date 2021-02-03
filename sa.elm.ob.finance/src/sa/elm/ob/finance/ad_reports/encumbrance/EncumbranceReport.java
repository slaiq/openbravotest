package sa.elm.ob.finance.ad_reports.encumbrance;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.xmlEngine.XmlDocument;

public class EncumbranceReport extends HttpSecureAppServlet {

  /**
   * process for getting report in reconciliation tab for the current transaction
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/encumbrancereport/EncumbranceReport.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));

      VariablesSecureApp vars = new VariablesSecureApp(request);
      OBContext.setAdminMode();

      if (action.equals("")) {
        log4j.debug("action");

        // localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("xls")) {

        EncumbranceReportService encum = new EncumbranceReportServiceImpl();

        String fileName = "EncumbranceReport1";
        String filedir = globalParameters.strFTPDirectory + "/";
        XSSFWorkbook workbook = new XSSFWorkbook();

        // encum.createSheet1(workbook, filedir + fileName + "-.xlsx");
        encum.createSheet2(workbook, filedir + fileName + "-.xlsx");

        // Set Response Type and Send File in Servlet Output Stream
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        if (log4j.isDebugEnabled())
          log4j.debug("Output: PopUp Download");
        String href = strDireccion + "/utility/DownloadReport.html?report=" + fileName + "-.xlsx";
        XmlDocument xmlDocument = xmlEngine
            .readXmlTemplate("sa/elm/ob/finance/ad_reports/encumbrance/EncumbranceReport")
            .createXmlDocument();
        xmlDocument.setParameter("href", href);
        ServletOutputStream os = response.getOutputStream();
        os.println(xmlDocument.print());
        os.close();

      }
    } catch (Exception e) {
      log4j.error("Exception in Reconcilation Report :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}