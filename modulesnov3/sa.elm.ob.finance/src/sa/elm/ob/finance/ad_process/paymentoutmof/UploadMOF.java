package sa.elm.ob.finance.ad_process.paymentoutmof;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class UploadMOF extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private static final String TMP_DIR_PATH = "/tmp";
  private static final String DESTINATION_DIR_PATH = "/files";
  private File tmpDir = null, destinationDir = null;
  String TABLEID = "";
  String pageType = "";

  public UploadMOF() {
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
    UploadMOFDAO dao = null;

    Logger log4j = Logger.getLogger(UploadMOF.class);
    try {
      con = getConnection();
      dao = new UploadMOFDAO(con);
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));

      if (action != null && action.equals("UploadMOF")) {
        HashMap<String, String> formValues = new HashMap<String, String>();
        File file = null;
        String fileType = "xls";
        DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
        fileItemFactory.setSizeThreshold(1 * 124 * 124); // 1 MB
        fileItemFactory.setRepository(tmpDir);
        ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
        try {
          List items = uploadHandler.parseRequest(request);
          vars = new VariablesSecureApp(request);
          Iterator itr = items.iterator();
          if (itr != null) {
            while (itr.hasNext()) {
              FileItem item = (FileItem) itr.next();
              if (item.isFormField()) {
                formValues.put(item.getFieldName(), item.getString());
                if (item.getFieldName() != null && item.getFieldName().equals("inpFileType"))
                  fileType = item.getString();
              } else {
                try {
                  file = new File(destinationDir, vars.getUser() + "." + fileType);
                  item.write(file);
                } catch (Exception e) {
                  log4j.error("Error during file creation", e);
                }
              }
            }
          }
        } catch (Exception e) {
          log4j.error("Error during file upload", e);
          throw new ServletException(e);
        }

        try {
          ArrayList<UploadMOFVO> rsLs = new ArrayList<UploadMOFVO>();

          if (fileType.equals("xls") || fileType.equals("xlsx"))
            rsLs = dao.validateFile(file, vars.getJavaDateFormat(), fileType, vars.getUser(),
                vars.getClient());
          else if (fileType.equals("csv"))
            rsLs = dao.validateCsvFile(file, vars.getJavaDateFormat(), vars.getUser(),
                vars.getClient());

          String errorCom = "";
          String docNo = "", bankName = "", chequeNo = "", chequeStat = "", chequeDate = "",
              payRecevBy = "";
          String meAccType = "", sanAccType = "";
          boolean hasEmptyDoc = false;

          if (rsLs != null) {
            for (UploadMOFVO VO : rsLs) {
              if (VO.getResult() == 2) {
                if (VO.getColName() != null && VO.getColName().equals("Empty Sheet"))
                  errorCom = "Empty sheet cannot be uploaded";
              } else if (VO.getResult() == 0) {
                if (VO.getColName() != null && VO.getColName().contains("Document No")) {
                  if (VO.getDocNo() != null && !VO.getDocNo().equals(""))
                    docNo += "," + VO.getDocNo();
                  else
                    hasEmptyDoc = true;
                } else if (VO.getColName() != null && VO.getColName().contains("MOF/EXT Acc"))
                  meAccType += "," + VO.getDocNo();
                else if (VO.getColName() != null && VO.getColName().contains("SAN Acc"))
                  sanAccType += "," + VO.getDocNo();

                if (VO.getAccType() != null && VO.getAccType().equals("SAN")) {
                  if (VO.getColName() != null && VO.getColName().contains("Payment Received Date"))
                    chequeDate += "," + VO.getDocNo();
                  if (VO.getColName() != null && VO.getColName().contains("Payment Received By"))
                    payRecevBy += "," + VO.getDocNo();
                } else if (VO.getAccType() != null && VO.getAccType().equals("MOF/EXT")) {
                  if (VO.getColName() != null && VO.getColName().contains("Bank Name"))
                    bankName += "," + VO.getDocNo();
                  if (VO.getColName() != null && VO.getColName().contains("Cheque No"))
                    chequeNo += "," + VO.getDocNo();
                  if (VO.getColName() != null && VO.getColName().contains("Cheque Status"))
                    chequeStat += "," + VO.getDocNo();
                  if (VO.getColName() != null && VO.getColName().contains("Cheque Date"))
                    chequeDate += "," + VO.getDocNo();
                }
              } else if (VO.getUploadMoF() == 0)
                errorCom = "File cannot be uploaded in status Cancelled, Payment Cleared and Void. Check document no "
                    + VO.getDocNo();
            }

            if (hasEmptyDoc)
              errorCom = ";Document No cannot be empty ";
            docNo = docNo.replaceFirst(",", "");
            bankName = bankName.replaceFirst(",", "");
            chequeNo = chequeNo.replaceFirst(",", "");
            chequeStat = chequeStat.replaceFirst(",", "");
            chequeDate = chequeDate.replaceFirst(",", "");
            payRecevBy = payRecevBy.replaceFirst(",", "");
            meAccType = meAccType.replaceFirst(",", "");
            sanAccType = sanAccType.replaceFirst(",", "");
          }

          if (docNo != null && !docNo.equals(""))
            errorCom += ";Document No not matches for document no " + docNo;
          if (!bankName.equals(""))
            errorCom += ";  MoF Bank Name cannot be empty for document no " + bankName;
          if (!chequeNo.equals(""))
            errorCom += ";  MoF Cheque No cannot be empty for document no " + chequeNo;
          if (!chequeStat.equals(""))
            errorCom += ";  Cheque Status should be Issued/Cancelled for document no " + chequeStat;
          if (!chequeDate.equals(""))
            errorCom += ";  Invalid Date format for document no " + chequeDate;
          if (!payRecevBy.equals(""))
            errorCom += ";  Payment Received By cannot be empty for document no " + payRecevBy;
          if (!meAccType.equals(""))
            errorCom += ";  Only MoF/External account details should be provided for document no "
                + meAccType;
          if (!sanAccType.equals(""))
            errorCom += ";  Only Sandook account details should be provided for document no "
                + sanAccType;

          if (errorCom.equals(""))
            advisePopUpRefresh(request, response, "SUCCESS", "SUCCESS",
                "File has been uploaded successfully");
          else {
            // errorCom = errorCom.replaceAll(",", "");
            errorCom = errorCom.replaceFirst(";", "");
            advisePopUpRefresh(request, response, "ERROR", "ERROR", errorCom);
          }
        } catch (Exception e) {
          log4j.error("Error during file validation", e);
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
          .getRequestDispatcher("../web/sa.elm.ob.finance/jsp/paymentoutmof/UploadMOF.jsp");
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
