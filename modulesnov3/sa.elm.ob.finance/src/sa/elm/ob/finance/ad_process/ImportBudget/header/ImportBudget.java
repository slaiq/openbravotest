package sa.elm.ob.finance.ad_process.ImportBudget.header;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.exception.NoConnectionAvailableException;

import sa.elm.ob.finance.ad_process.ImportBudget.dao.ImportBudgetDAO;

public class ImportBudget extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to import the budget
   */
  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/ImportBudget/ImportBudget.jsp";

  public void init(ServletConfig config) {

    super.init(config);
    boolHist = false;

    tmpDir = new File(TMP_DIR_PATH);
    if (!tmpDir.isDirectory()) {
      new File(TMP_DIR_PATH).mkdir();
    }

    String realPath = getServletContext().getRealPath(DESTINATION_DIR_PATH);
    destinationDir = new File(realPath);
    if (!destinationDir.isDirectory()) {
      new File(realPath).mkdir();
    }
  }

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ImportBudget() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @SuppressWarnings("rawtypes")
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Connection con = null;
    try {
      con = getConnection();
    } catch (NoConnectionAvailableException e) {
      log4j.error("Exception in conection  : " + e);
    }

    String inpAction = (request.getParameter("action") == null
        || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));
    if (inpAction.equals("")) {
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String inpBudgetId = vars.getStringParameter("Efin_Budget_ID");
      request.setAttribute("inpBudgetId", inpBudgetId);

    }

    else if (inpAction.equals("uploadCsv") || inpAction.equals("validateCsv")) {
      String inpBudgetId = request.getParameter("inpBudgetId");
      JSONObject isValidCSV = null;
      String strQuery = "";
      PreparedStatement ps = null;
      try {
        // parses the request's content to extract file data

        ImportBudgetDAO dao = new ImportBudgetDAO(con);

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {

          File attachmentFile = null;
          HashMap<String, String> formValues = new HashMap<String, String>();

          //

          DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
          fileItemFactory.setSizeThreshold(1 * 124 * 124); // 1 MB
          fileItemFactory.setRepository(tmpDir);
          ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);

          try {
            List items = uploadHandler.parseRequest(request);
            Iterator itr = items.iterator();

            while (itr.hasNext()) {
              FileItem item = (FileItem) itr.next();

              if (item.isFormField()) {
                formValues.put(item.getFieldName(), item.getString());
              } else {
                attachmentFile = new File(destinationDir, item.getName());
                item.write(attachmentFile);
                log4j.debug("File Name:" + attachmentFile.getName());
              }
            }
          } catch (Exception e) {
            log4j.error("Exception while uploading the excel file:" + e);
            throw new ServletException(e);
          }

          VariablesSecureApp vars = new VariablesSecureApp(request);
          if (inpAction.equals("validateCsv")) {
            JSONObject resultJSON = dao.processValidateCsvFile(attachmentFile, vars, inpBudgetId);
            strQuery = "delete from efin_csv_budget_import";
            ps = con.prepareStatement(strQuery);
            ps.executeUpdate();
            request.setAttribute("Result", resultJSON);
          }

          else {

            JSONObject resultJSON = dao.processValidateCsvFile(attachmentFile, vars, inpBudgetId);
            if ("1".equals(resultJSON.getString("status"))) {
              isValidCSV = dao.processUploadedCsvFile(inpBudgetId, vars);
              strQuery = "delete from efin_csv_budget_import";
              ps = con.prepareStatement(strQuery);
              ps.executeUpdate();
              request.setAttribute("Result", isValidCSV);
            } else {
              strQuery = "delete from efin_csv_budget_import";
              ps = con.prepareStatement(strQuery);
              ps.executeUpdate();

              request.setAttribute("Result", resultJSON);
            }

          }
        }
        // Localization support

        request.setAttribute("inpBudgetId", inpBudgetId);
      } catch (Exception ex) {
        request.setAttribute("message", "There was an error: " + ex.getMessage());
      }

    } else if (inpAction.equals("Close")) {
      VariablesSecureApp vars = new VariablesSecureApp(request);

      printPageClosePopUp(response, vars);
    } else {
      request.setAttribute("Result", null);
    }

    request.getRequestDispatcher(includeIn).forward(request, response);

  }
}
