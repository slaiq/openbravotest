package sa.elm.ob.finance.ad_process.projectuploader.header;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.ad_process.projectuploader.dao.ProjectUploaderDAO;

public class ProjectUploader extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  String TABLEID = "";
  String pageType = "";

  public ProjectUploader() {
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
    RequestDispatcher dispatch = null;
    Connection con = null;
    ProjectUploaderDAO dao = null;

    Logger log4j = Logger.getLogger(ProjectUploader.class);
    try {
      con = getConnection();
      dao = new ProjectUploaderDAO(con);
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));

      if (action != null && action.equals("UploadProject")) {
        HashMap<String, String> formValues = new HashMap<String, String>();
        File attachmentFile = null;
        JSONObject isValidFile = null;

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
        isValidFile = dao.processValidateFile(attachmentFile, vars);
        if (isValidFile.has("status") && "1".equals(isValidFile.getString("status"))) {
          isValidFile = dao.processUploadFile(attachmentFile, vars);
        }

        if (isValidFile.has("status") && "1".equals(isValidFile.getString("status"))) {
          advisePopUpRefresh(request, response, "SUCCESS", "SUCCESS",
              "File has been uploaded successfully");
        } else {
          String errorCom = "Error while uploading project!!";
          if (isValidFile.has("statusMessage")) {
            errorCom = isValidFile.getString("statusMessage");
          }
          advisePopUpRefresh(request, response, "ERROR", "ERROR", errorCom);
        }

      } else if (action.equals("Close")) {
        vars = new VariablesSecureApp(request);
        if (request.getParameter("pageType") != null
            && request.getParameter("pageType").equals("Grid"))
          printPageClosePopUp(response, vars);
        else
          printPageClosePopUpAndRefreshParent(response, vars);
      } else if (action.equals("")) {
        Calendar cal = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        request.setAttribute("CurrentDate", dateFormat.format(cal.getTime()));
        request.setAttribute("CurrentYear", cal.get(Calendar.YEAR));

        String tabId = request.getParameter("inpTabId");
        String windowId = (request.getParameter("inpWindowID") == null ? ""
            : request.getParameter("inpWindowID"));
        String formId = (request.getParameter("inpFormID") == null ? ""
            : request.getParameter("inpFormID"));
        pageType = request.getParameter("pageType") == null ? "" : request.getParameter("pageType");
        TABLEID = dao.getTableId(tabId);

        request.setAttribute("pageType", pageType);
        request.setAttribute("inpTabId", tabId);
        request.setAttribute("inpWindowID", windowId);
        request.setAttribute("inpTableId", TABLEID);
        request.setAttribute("inpFormID", formId);
      }
      dispatch = request
          .getRequestDispatcher("../web/sa.elm.ob.finance/jsp/projectuploader/ProjectUploader.jsp");
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Error file", e);
      e.printStackTrace();
      vars = new VariablesSecureApp(request);
      if (request.getParameter("pageType") != null
          && request.getParameter("pageType").equals("Grid"))
        printPageClosePopUp(response, vars);
      else
        printPageClosePopUpAndRefreshParent(response, vars);
    } finally {
      try {
        con.close();
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        } else {
          vars = new VariablesSecureApp(request);
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          if (request.getParameter("pageType") != null
              && request.getParameter("pageType").equals("Grid"))
            printPageClosePopUp(response, vars);
          else
            printPageClosePopUpAndRefreshParent(response, vars);
        }
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Error file", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }
}
