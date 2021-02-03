package sa.elm.ob.scm.ad_process.printreport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Base64;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;

import net.sf.jasperreports.engine.JasperExportManager;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.utility.dms.consumer.GRPDmsImplementation;
import sa.elm.ob.utility.dms.consumer.GRPDmsInterface;
import sa.elm.ob.utility.dms.consumer.dto.GetAttachmentGRPResponse;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSUtility;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class PrintReport extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  String pageType = "";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = null;
    RequestDispatcher dispatch = null;
    Connection con = null;
    try {
      con = getConnection();
      // dao = new PrintReportDAO();
      vars = new VariablesSecureApp(request);
      ServletOutputStream os = null;

      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      log4j.debug("action" + action);
      HashMap<String, Object> designParameters = new HashMap<String, Object>();
      final String basedesign = getBaseDesignPath(vars.getLanguage());
      final String reportDir = basedesign + "/sa/elm/ob/scm/ad_reports/";
      designParameters.put("BASE_DESIGN", basedesign);

      String strFileName = "";

      JSONObject paramObject = new JSONObject();
      if (action.equals("Close")) {
        if (request.getParameter("pageType") != null
            && request.getParameter("pageType").equals("Grid"))
          printPageClosePopUp(response, vars);
        else
          printPageClosePopUpAndRefreshParent(response, vars);
      } else {
        try {

          String tabId = request.getParameter("inpTabId");
          String inpWindowID = (request.getParameter("inpWindowID") == null ? ""
              : request.getParameter("inpWindowID"));
          String inpRecordId = (request.getParameter("inpRecordId") == null ? ""
              : request.getParameter("inpRecordId"));

          String receiveType = (request.getParameter("receiveType") == null ? ""
              : request.getParameter("receiveType"));
          String warehouseType = (request.getParameter("warehouseType") == null ? ""
              : request.getParameter("warehouseType"));
          String documentNo = (request.getParameter("documentNo") == null ? ""
              : request.getParameter("documentNo"));

          String inpReceivingType = request.getParameter("inpReceivingType") == null ? ""
              : request.getParameter("inpReceivingType");
          String paramType = request.getParameter("report") == null ? ""
              : request.getParameter("report");

          log4j.debug(
              inpReceivingType + "//" + paramType + "//" + request.getParameter("inpRecordId"));

          paramObject.put("paramType", paramType);
          paramObject.put("inpReceivingType", inpReceivingType);
          paramObject.put("reportDir", reportDir);
          paramObject.put("connection", con);
          paramObject.put("tabId", tabId);
          paramObject.put("inpWindowID", inpWindowID);
          paramObject.put("inpRecordId", inpRecordId);
          paramObject.put("pageType", pageType);
          paramObject.put("receiveType", receiveType);
          paramObject.put("warehouseType", warehouseType);
          paramObject.put("documentNo", documentNo);
          paramObject.put("basedesign", basedesign);

          GetPrintFactory printFactory = new GetPrintFactory();
          GenerateJasperPrint print = printFactory.getPrint(request, paramObject);
          GRPDmsInterface dmsGRP = new GRPDmsImplementation();
          GetAttachmentGRPResponse dmsResponse = null;

          if (print.isJasper()) {
            strFileName = print.getFileName();

            if ("A0F3A7D17A834A93B3BD4D2C40E77AFE".equals(tabId)) {

              Boolean isPrint = true;
              EfinRDVTransaction rdvTransaction = OBDal.getInstance().get(EfinRDVTransaction.class,
                  inpRecordId);
              if (rdvTransaction != null) {
                if (!"DR".equals(rdvTransaction.getAppstatus())
                    && rdvTransaction.getEutAttachPath() != null) {

                  String attachmentURI = rdvTransaction.getEutAttachPath();
                  DMSIntegrationLog dmslog = DMSUtility.createDMSIntegrationLog(null,
                      DMSConstants.DMS_GET, rdvTransaction);

                  dmsResponse = dmsGRP.getReportFromDMS(attachmentURI);

                  if (dmsResponse != null) {
                    if (!dmsResponse.isError()) {
                      isPrint = false;
                      int i = 0;

                      response.setContentType("application/pdf");
                      ServletOutputStream out = response.getOutputStream();
                      response.setHeader("Content-disposition",
                          "attachment" + "; filename=" + strFileName + ".pdf");

                      // update dms log
                      dmslog.setRequest(dmsResponse.getRequest());
                      dmslog.setResponsemessage(dmsResponse.getResponse());
                      dmslog.setAlertStatus(DMSConstants.DMS_SUCCESS);
                      OBDal.getInstance().save(dmslog);

                      // Download report.
                      String fileData = dmsResponse.getBase64Str();
                      byte[] bytes = Base64.getDecoder().decode(fileData);
                      try (InputStream input = new ByteArrayInputStream(bytes)) {
                        while ((i = input.read()) != -1) {
                          out.write(i);
                        }
                      } catch (Exception e) {
                        log4j.error("Exception in Downloading report: ", e);
                        e.printStackTrace();
                      }
                      out.close();
                    } else {
                      dmslog.setRequest(dmsResponse.getRequest());
                      dmslog.setResponsemessage(dmsResponse.getErrorMsg());
                      dmslog.setAlertStatus(DMSConstants.DMS_FAILED);
                      OBDal.getInstance().save(dmslog);
                    }

                  }
                }

                if (isPrint) {
                  os = response.getOutputStream();
                  response.setContentType("application/octet-stream");
                  response.setHeader("Content-disposition",
                      "inline" + "; filename=" + strFileName + ".pdf" + "");

                  JasperExportManager.exportReportToPdfStream(print.getJasperPrint(), os);
                  print.getDesignParameters().clear();
                } else {

                  response.setContentType("application/octet-stream");
                  response.setHeader("Content-disposition",
                      "inline" + "; filename=" + strFileName + ".pdf" + "");
                  print.getDesignParameters().clear();
                }

              }
            } else {
              os = response.getOutputStream();
              response.setContentType("application/octet-stream");
              response.setHeader("Content-disposition",
                  "inline" + "; filename=" + strFileName + ".pdf" + "");

              JasperExportManager.exportReportToPdfStream(print.getJasperPrint(), os);
              print.getDesignParameters().clear();

            }

          } else if (print.isZip()) {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                "attachment;filename=BidNewspaperAnnouncementReport.zip");
            response.getOutputStream().write(print.getBaos().toByteArray());
            print.getDesignParameters().clear();
          }

          else {
            dispatch = request.getRequestDispatcher(print.getJspPage());
          }

        } catch (Exception e) {
          log4j.error("Exception while downloading : ", e);
        }

        finally {
          try {
            if (os != null) {
              os.flush();
              os.close();
            }
          } catch (Exception e) {
            log4j.error("Exception while downloading print report : ", e);
          }
        }
        return;
      }
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Error file", e);
      vars = new VariablesSecureApp(request);
      if (request.getParameter("pageType") != null
          && request.getParameter("pageType").equals("Grid"))
        printPageClosePopUp(response, vars);
      else
        printPageClosePopUpAndRefreshParent(response, vars);
    } finally {
      try {

        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
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
