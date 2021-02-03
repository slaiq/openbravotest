package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author divya
 * 
 */
public class InsuranceCertificateWorkbenchPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(InsuranceCertificateWorkbenchPrint.class);
  private static InsuranceCertificateWorkbenchPrint insuranceCertificateWorkbenchPrint;

  public static InsuranceCertificateWorkbenchPrint getInstance() {
    if (insuranceCertificateWorkbenchPrint == null) {
      insuranceCertificateWorkbenchPrint = new InsuranceCertificateWorkbenchPrint();
    }
    return insuranceCertificateWorkbenchPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String imageFlag = "N", inpICwrkbenchRpt = "", reportDir = "";

      reportDir = paramObject.getString("reportDir");
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));

      if (action.equals(""))
        isJasper = Boolean.FALSE;
      else if (action.equals("DownloadReport"))
        isJasper = Boolean.TRUE;

      connection = (Connection) paramObject.get("connection");
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpInsCertfId", request.getParameter("inpRecordId"));
      inpICwrkbenchRpt = (request.getParameter("inpICWorkbenchRpt") == null ? ""
          : request.getParameter("inpICWorkbenchRpt"));

      if (inpICwrkbenchRpt.equals("ICDTL")) {
        strReportName = reportDir + "insurancecertificatedetails/InsuranceCertificateDetails.jrxml";
        strFileName = "ICDetails" + " " + " " + hijriDate;
      } else if (inpICwrkbenchRpt.equals("ICRL")) {
        strReportName = reportDir
            + "insurancecertificaterelease/InsuranceCertificateReleaseLetter.jrxml";
        strFileName = "ICReleaseLetter" + " " + " " + hijriDate;
      } else if (inpICwrkbenchRpt.equals("IEXR")) {
        strReportName = reportDir
            + "InsuranceCertificateExtension/InsuranceCertificateExtensionLetterReport.jrxml";
        strFileName = "ICExtensionLetter" + " " + " " + hijriDate;
      }

      request.setAttribute("pageType", paramObject.getString("pageType"));
      request.setAttribute("inpTabId", paramObject.getString("tabId"));
      request.setAttribute("inpWindowID", paramObject.getString("inpWindowID"));
      request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
      strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/InsuranceCertificate.jsp";

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
