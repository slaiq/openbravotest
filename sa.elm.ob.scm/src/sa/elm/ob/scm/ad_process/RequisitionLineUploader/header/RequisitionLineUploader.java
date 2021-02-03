package sa.elm.ob.scm.ad_process.RequisitionLineUploader.header;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;

import sa.elm.ob.scm.ad_process.RequisitionLineUploader.dao.RequisitionLineUploaderDAOImpl;

/**
 * 
 * @author Gopalakrishnan on 06/11/2017
 *
 */

public class RequisitionLineUploader extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to import the Requisition Line details in requisition Window
   */
  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.scm/jsp/RequisitionLineUploader/RequisitionLineUploader.jsp";

  @Override
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
  public RequisitionLineUploader() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  @SuppressWarnings("rawtypes")
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Connection con = null;
    try {
      con = getConnection();
    } catch (NoConnectionAvailableException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      request.setAttribute("message",
          "There was an error: " + OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

    String inpAction = (request.getParameter("action") == null
        || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));
    if (inpAction.equals("")) {
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String inpRequistionId = vars.getStringParameter("M_Requisition_ID");
      request.setAttribute("inpRequistionId", inpRequistionId);
    }

    else if (inpAction.equals("uploadCsv") || inpAction.equals("validateCsv")) {
      String inpRequistionId = request.getParameter("inpRequistionId");
      JSONObject isValidCSV = null;
      try {
        // parses the request's content to extract file data
        RequisitionLineUploaderDAOImpl dao = new RequisitionLineUploaderDAOImpl(con);

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {

          File attachmentFile = null;
          HashMap<String, String> formValues = new HashMap<String, String>();

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
            // e.printStackTrace();
            log4j.error("Exception while uploading the excel file:" + e);
            request.setAttribute("message",
                "There was an error: " + OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
            // throw new ServletException(e);
          }

          VariablesSecureApp vars = new VariablesSecureApp(request);

          if (inpAction.equals("validateCsv")) {
            isValidCSV = dao.processValidateCsvFile(attachmentFile, vars, inpRequistionId);
            // request.setAttribute("isValidated", "true");
          } else {
            isValidCSV = new JSONObject();
            isValidCSV = dao.processValidateCsvFile(attachmentFile, vars, inpRequistionId);
            if ("1".equals(isValidCSV.getString("status"))) {
              isValidCSV = dao.processUploadedCsvFile(attachmentFile, vars, inpRequistionId);
            }
            // request.setAttribute("isValidated", "false");
          }

          if (!inpAction.equals("validateCsv")) {
            request.setAttribute("isValidated", "false");
          } else {
            if ("1".equals(isValidCSV.getString("status"))) {
              request.setAttribute("isValidated", "true");
            } else {
              request.setAttribute("isValidated", "false");
            }
          }

        }
        // Localization support
        request.setAttribute("Result", isValidCSV);
        request.setAttribute("inpRequistionId", inpRequistionId);
      } catch (Exception ex) {
        ex.printStackTrace();
        request.setAttribute("message",
            "There was an error: " + OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
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
