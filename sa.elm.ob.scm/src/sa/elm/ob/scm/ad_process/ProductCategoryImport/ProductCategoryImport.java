package sa.elm.ob.scm.ad_process.ProductCategoryImport;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
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
import org.apache.commons.io.FileUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

/**
 * 
 * @author Divya on 26/04/2017
 * 
 */
public class ProductCategoryImport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String TMP_DIR_PATH = "/tmp";
  /* private static final String DESTINATION_DIR_PATH = "/files"; */
  private File tmpDir = null, destinationDir = null;
  private String jspPage = "../web/sa.elm.ob.scm/jsp/ProductCategoryImport/ProductCategoryImport.jsp";
  File attachmentFile = null;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public ProductCategoryImport() {
    super();
  }

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;

    tmpDir = new File(TMP_DIR_PATH);
    if (!tmpDir.isDirectory()) {
      new File(TMP_DIR_PATH).mkdir();
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      OBContext.setAdminMode();
      ProductCategoryImportDAO dao = null;
      Connection con = getConnection();
      dao = new ProductCategoryImportDAO(con);
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      log4j.debug("action" + action);
      if (action.equals("")) {

        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Validate") || (action.equals("Upload"))) {
        DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();

        fileItemFactory.setSizeThreshold(1 * 124 * 124); // 1 MB
        fileItemFactory.setRepository(tmpDir);
        ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
        HashMap<File, FileItem> fileList = new HashMap<File, FileItem>();
        try {
          String errorCom = "";
          List items = uploadHandler.parseRequest(request);
          Iterator itr = items.iterator();
          String realPath = "";
          realPath = globalParameters.strFTPDirectory + "/ProductcategoryUpload/";
          destinationDir = new File(realPath);
          itr = items.iterator();
          while (itr.hasNext()) {
            FileItem item = (FileItem) itr.next();
            if (!item.isFormField() && item.getName() != null && !item.getName().equals("")
                && !item.getName().equals("null")) {
              File tmp = new File(realPath);
              if (tmp.isDirectory())
                FileUtils.forceDelete(tmp);

              if (!destinationDir.isDirectory()) {
                new File(realPath).mkdirs();
              }
            }

            if (!item.isFormField()) {
              if ("".equals(item.getName().trim()))
                continue;
              attachmentFile = new File(destinationDir, item.getName());
              // String name = item.getName();
              if (attachmentFile.exists()) {
                attachmentFile.delete();
              }
              item.write(attachmentFile);

              fileList.put(attachmentFile, item);
            }
          }
          VariablesSecureApp vars = new VariablesSecureApp(request);
          ArrayList<ProductCategoryImportVO> rsLs = new ArrayList<ProductCategoryImportVO>();
          rsLs = dao.validateCsvFile(attachmentFile, vars.getOrg(), vars.getUser(),
              vars.getClient(), action);
          if (rsLs != null) {
            for (ProductCategoryImportVO VO : rsLs) {
              if (VO.getMaincatcode() == null) {
                errorCom += " Main Category Code should not be Empty at line No : <b>"
                    + VO.getLineno() + "</b> @";
              }
              if (VO.getMaincatcode() == null) {
                errorCom += " Main Category Name should not be Empty at line No : <b>"
                    + VO.getLineno() + "</b> @";
              }
              if (VO.getMaincatcode() == null) {
                errorCom += " Sub Category Code should not be Empty at line No : <b>"
                    + VO.getLineno() + "</b> @";
              }
              if (VO.getMaincatcode() == null) {
                errorCom += " Sub Category Name should not be Empty at line No : <b>"
                    + VO.getLineno() + "</b> @";
              }
              if (VO.getMessage() != null) {
                errorCom += VO.getMessage() + VO.getLineno() + "</b> @";
              }

            }
          }
          if (errorCom.equals("")) {

            if (action.equals("Validate")) {
              request.setAttribute("successmsg", "File has been validated successfully");
              attachmentFile.delete();
              request.setAttribute("successflag", "1");
            } else {

              int Uploadcount = dao.uploadCSVFile(attachmentFile, vars.getOrg(), vars.getUser(),
                  vars.getClient());
              if (Uploadcount == 1) {
                request.setAttribute("successmsg", "File has been uploaded successfully");
                request.setAttribute("successflag", "1");
              }
              attachmentFile.delete();
            }
          } else {

            // errorCom = errorCom.replaceAll(",", "");
            // errorCom = errorCom.replaceFirst(";", "");

            request.setAttribute("errormsg", errorCom);
            request.setAttribute("errorflag", "1");
            attachmentFile.delete();
          }
        } catch (Exception e) {
          log4j.error("Error during file validation", e);
          throw new ServletException(e);
        } finally {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          request.getRequestDispatcher(jspPage).include(request, response);
        }
      } else if (action.equals("Close")) {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);
      }
    } catch (Exception e) {
      log4j.error("Exception in ProductCategoryImport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}