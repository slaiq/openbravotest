package sa.elm.ob.scm.ad_process.BidManagement.ImportBid;

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

/**
 * 
 * @author Gokul on 22/05/2020
 *
 */

public class ImportBidLines extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to import Bid Lines
   */
  private static final long serialVersionUID = 1L;

  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  private static final String includeIn = "../web/sa.elm.ob.scm/jsp/ImportBid/ImportBid.jsp";

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
  public ImportBidLines() {
    super();
    // TODO Auto-generated constructor stub
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
    VariablesSecureApp vars = null;

    try {
      con = getConnection();
    } catch (NoConnectionAvailableException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    String inpAction = (request.getParameter("action") == null
        || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));

    if (inpAction.equals("")) {
      vars = new VariablesSecureApp(request);
      String inpBidmgmtId = vars.getStringParameter("inpRecordId");
      request.setAttribute("inpBidmgmtId", inpBidmgmtId);
    } else if (inpAction.equals("uploadCsv") || inpAction.equals("validateCsv")) {
      String inpBidmgmtId = request.getParameter("inpBidmgmtId");
      JSONObject isValidCSV = null;
      try {
        // parses the request's content to extract file data

        ImportBidLinesDAO dao = new ImportBidLinesDAO(con);

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
            log4j.error("Exception while uploading the excel file:" + e);
            throw new ServletException(e);
          }

          vars = new VariablesSecureApp(request);
          // clear old entries
          if (inpAction.equals("validateCsv")) {
            JSONObject resultJSON = dao.processValidateCsvFile(attachmentFile, vars, inpBidmgmtId,
                false);
            if (resultJSON != null) {
              request.setAttribute("Result", resultJSON);
            } else {
              JSONObject resJSON = new JSONObject();
              resJSON.put("status", "0");
              resJSON.put("statusMessage",
                  OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_Validation"));
              request.setAttribute("Result", resJSON);
            }
          } else {
            JSONObject resultJSON = dao.processValidateCsvFile(attachmentFile, vars, inpBidmgmtId,
                true);
            if (resultJSON != null) {
              if ("1".equals(resultJSON.getString("status"))) {
                isValidCSV = dao.processUploadedCsvFile(inpBidmgmtId, vars);
                request.setAttribute("Result", isValidCSV);
              } else {
                request.setAttribute("Result", resultJSON);
              }
            } else {
              JSONObject resJSON = new JSONObject();
              resJSON.put("status", "0");
              resJSON.put("statusMessage",
                  OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_Validation"));
              request.setAttribute("Result", resJSON);
            }
          }
        }
        request.setAttribute("inpBidmgmtId", inpBidmgmtId);
      } catch (Exception ex) {
        request.setAttribute("message", "There was an error: " + ex.getMessage());
      }
    } else if (inpAction.equals("Close")) {

      vars = new VariablesSecureApp(request);
      printPageClosePopUp(response, vars);
    } else {
      request.setAttribute("Result", null);
    }

    response.setContentType("text/html; charset=UTF-8");

    request.getRequestDispatcher(includeIn).include(request, response);
  }
}
