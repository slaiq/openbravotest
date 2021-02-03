package sa.elm.ob.scm.ad_process.ImportProductStock.header;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

import sa.elm.ob.scm.ad_process.ImportProductStock.dao.ImportProductStockDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 28/04/2017
 */
public class ImportProductStock extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to import the product stock in physical inventory
   */
  private static final long serialVersionUID = 1L;

  // location to store file uploaded
  private static final String UPLOAD_DIRECTORY = "ProductStock";

  // upload settings
  private static final int MEMORY_THRESHOLD = 1024 * 1024 * 3; // 3MB
  private static final int MAX_FILE_SIZE = 1024 * 1024 * 40; // 40MB
  private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 50; // 50MB
  private static final String includeIn = "../web/sa.elm.ob.scm/jsp/ImportProductStock/ImportProductStock.jsp";

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ImportProductStock() {
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String inpAction = (request.getParameter("action") == null
        || "".equals(request.getParameter("action")) ? "" : request.getParameter("action"));
    log4j.debug("action" + inpAction);
    if (inpAction.equals("")) {
      // VariablesSecureApp vars = new VariablesSecureApp(request);
      String inpmInventoryId = request.getParameter("inpmInventoryId");

      log4j.debug("inpmInventoryId" + inpmInventoryId);

      String defaultBinId = "";
      InventoryCount header = OBDal.getInstance().get(InventoryCount.class, inpmInventoryId);
      if (inpmInventoryId != null) {
        defaultBinId = Utility.GetDefaultBin(header.getWarehouse().getId());
        if (defaultBinId.equals("")) {
          if (header.getWarehouse().getLocatorList().size() > 0) {
            request.setAttribute("defaultfin", "Y");
          } else {
            request.setAttribute("defaultfin", "N");
          }
        } else {
          request.setAttribute("defaultfin", "Y");
        }
      }
      // Localization support
      request.setAttribute("inpmInventoryId", inpmInventoryId);
      response.setContentType("text/html; charset=UTF-8");
      response.setCharacterEncoding("UTF-8");
      // request.getRequestDispatcher(includeIn).forward(request, response);

    }

    else if (inpAction.equals("uploadCsv") || inpAction.equals("validateCsv")) {
      String inpmInventoryId = request.getParameter("inpmInventoryId");
      ImportProductStockDAO dao = null;
      JSONObject isValidCSV = null;
      // checks if the request actually contains upload file
      if (!ServletFileUpload.isMultipartContent(request)) {
        // if not, we stop here
        PrintWriter writer = response.getWriter();
        writer.println("Error: Form must has enctype=multipart/form-data.");
        writer.flush();
        writer.close();
        return;
      }

      // configures upload settings
      DiskFileItemFactory factory = new DiskFileItemFactory();
      // sets memory threshold - beyond which files are stored in disk
      factory.setSizeThreshold(MEMORY_THRESHOLD);
      // sets temporary location to store files
      factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

      ServletFileUpload upload = new ServletFileUpload(factory);

      // sets maximum size of upload file
      upload.setFileSizeMax(MAX_FILE_SIZE);

      // sets maximum size of request (include file + form data)
      upload.setSizeMax(MAX_REQUEST_SIZE);

      // constructs the directory path to store upload file
      // this path is relative to application's directory

      String uploadPath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .get("attach.path")
          + File.separator + UPLOAD_DIRECTORY;

      // creates the directory if it does not exist
      File uploadDir = new File(uploadPath);
      FileUtils.deleteDirectory(new File(uploadPath));
      if (!uploadDir.exists()) {
        uploadDir.mkdir();

      }

      try {
        // parses the request's content to extract file data
        @SuppressWarnings("unchecked")
        List<FileItem> formItems = upload.parseRequest(request);
        String filePath = "";
        if (formItems != null && formItems.size() > 0) {
          // iterates over form's fields
          for (FileItem item : formItems) {
            // processes only fields that are not form fields
            if (!item.isFormField()) {
              String fileName = new File(item.getName()).getName();
              filePath = uploadPath + File.separator + fileName;
              File storeFile = new File(filePath);

              // saves the file on disk
              item.write(storeFile);
              // request.setAttribute("message","Upload has been done successfully!");
            }
          }
        }
        dao = new ImportProductStockDAO();
        if (inpAction.equals("validateCsv")) {
          isValidCSV = dao.processValidateCsvFile(filePath, inpmInventoryId);
          log4j.debug("isValidCSV" + isValidCSV);
        } else {
          isValidCSV = new JSONObject();
          isValidCSV = dao.processValidateCsvFile(filePath, inpmInventoryId);
          log4j.debug("isValidCSV" + isValidCSV);
          if ("1".equals(isValidCSV.getString("status"))) {
            VariablesSecureApp vars = new VariablesSecureApp(request);
            isValidCSV = dao.processUploadedCsvFile(filePath, inpmInventoryId, vars);
          }

        }
        request.setAttribute("Result", isValidCSV);
        request.setAttribute("inpmInventoryId", inpmInventoryId);
      } catch (Exception ex) {
        ex.printStackTrace();
        request.setAttribute("message", "There was an error: " + ex.getMessage());
      }

    } else {
      request.setAttribute("Result", null);
    }

    request.getRequestDispatcher(includeIn).forward(request, response);

  }
}
