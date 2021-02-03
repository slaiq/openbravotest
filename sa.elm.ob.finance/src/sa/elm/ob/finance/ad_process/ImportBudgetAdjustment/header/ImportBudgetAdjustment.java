package sa.elm.ob.finance.ad_process.ImportBudgetAdjustment.header;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
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

import sa.elm.ob.finance.ad_process.ImportBudgetAdjustment.dao.ImportBudgetAdjustmentDAO;

public class ImportBudgetAdjustment extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/ImportBudgetAdjustment/ImportBudgetAdjustment.jsp";

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
  public ImportBudgetAdjustment() {
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
    }
    try {
      String inpAction = (request.getParameter("action") == null
          || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));
      if (inpAction.equals("")) {

        VariablesSecureApp vars = new VariablesSecureApp(request);
        String inpBudgetAdjID = vars.getStringParameter("Efin_Budgetadj_ID");
        request.setAttribute("inpBudgetAdjID", inpBudgetAdjID);

        // ImportBudgetRevisionDAO dao = new ImportBudgetRevisionDAO(con);
      }

      else if (inpAction.equals("uploadCsv") || inpAction.equals("validateCsv")) {
        String inpBudgetAdjID = request.getParameter("inpBudgetAdjID");
        JSONObject isValidCSV = null;

        try {
          // parses the request's content to extract file data

          ImportBudgetAdjustmentDAO dao = new ImportBudgetAdjustmentDAO(con);

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
              isValidCSV = dao.processValidateCsvFile(attachmentFile, vars, inpBudgetAdjID);
              request.setAttribute("Result", isValidCSV);
            }

            else {

              isValidCSV = new JSONObject();
              isValidCSV = dao.processValidateCsvFile(attachmentFile, vars, inpBudgetAdjID);
              if ("1".equals(isValidCSV.getString("status"))) {
                isValidCSV = dao.processUploadedCsvFile(attachmentFile, vars, inpBudgetAdjID);

              }
            }
            // Localization support
            request.setAttribute("Result", isValidCSV);
            request.setAttribute("inpBudgetAdjID", inpBudgetAdjID);
          }
        } catch (Exception ex) {
          request.setAttribute("message", "There was an error: " + ex.getMessage());
        }

      } else if (inpAction.equals("Close")) {
        VariablesSecureApp vars = new VariablesSecureApp(request);
        printPageClosePopUp(response, vars);
      } else {
        request.setAttribute("Result", null);
      }
    } catch (Exception e) {
      log4j.error("Exception in ImportBudgetAdjustment: ", e);
    } finally {
      try {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(includeIn).forward(request, response);
      } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Exception in adjustment : ", e);
      }
    }

  }
}
