package sa.elm.ob.finance.ad_process.accounttreeuploader.header;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.ad_process.accounttreeuploader.dao.AccountTreeUploaderDAO;

public class AccountTreeUploader extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/accounttreeuploader/AccountTreeUploader.jsp";
  private File tmpDir = null, destinationDir = null;
  String TABLEID = "";
  String pageType = "";

  public AccountTreeUploader() {
    super();
  }

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

  @SuppressWarnings("rawtypes")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = null;
    Connection con = null;
    AccountTreeUploaderDAO dao = null;

    String inpcElementId = request.getParameter("inpcElementId");
    String action = (request.getParameter("inpAction") == null ? ""
        : request.getParameter("inpAction"));

    if (action.equals("upload") || action.equals("validate")) {
      try {
        con = getConnection();
        dao = new AccountTreeUploaderDAO(con);
        JSONObject isValidFile = new JSONObject();
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
            log4j.error("Error during file upload", e);
            throw new ServletException(e);
          }

          vars = new VariablesSecureApp(request);
          isValidFile = dao.processValidateFile(attachmentFile, vars, inpcElementId);
          if (action.equals("upload") && isValidFile.has("status")
              && "1".equals(isValidFile.getString("status"))) {
            isValidFile = dao.processUploadFile(attachmentFile, vars, inpcElementId);
          }

        }
        request.setAttribute("Result", isValidFile);
        request.setAttribute("inpcElementId", inpcElementId);
      } catch (Exception ex) {
        request.setAttribute("message",
            "There is an error in AccountTreeUploader : " + ex.getMessage());
      }
    } else if (action.equals("Close")) {
      vars = new VariablesSecureApp(request);
      printPageClosePopUp(response, vars);
    } else if (action.equals("")) {
      request.setAttribute("Result", null);
      request.setAttribute("inpcElementId", inpcElementId);
    }

    request.getRequestDispatcher(includeIn).forward(request, response);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}
