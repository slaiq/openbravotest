package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationInformation;

import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author divya
 * 
 */
public class OpenEnvelopCommitteePrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(OpenEnvelopCommitteePrint.class);
  private static OpenEnvelopCommitteePrint openEnvelopCommitteePrint;

  public static OpenEnvelopCommitteePrint getInstance() {
    if (openEnvelopCommitteePrint == null) {
      openEnvelopCommitteePrint = new OpenEnvelopCommitteePrint();
    }
    return openEnvelopCommitteePrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();

      String imageFlag = "N", reportDir = "", realPath = "";
      realPath = OBConfigFileProvider.getInstance().getServletContext().getRealPath("/");
      String opnEnvCmtRpt = (request.getParameter("openenvevt_report") == null ? ""
          : request.getParameter("openenvevt_report"));
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));

      Escmopenenvcommitee opnEnvCmtCount = OBDal.getInstance().get(Escmopenenvcommitee.class,
          request.getParameter("inpRecordId"));
      OrganizationInformation objInfo = Utility
          .getOrgInfo(opnEnvCmtCount.getOrganization().getId());

      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }

      if (action.equals("")) {
        if (opnEnvCmtCount.getAlertStatus().equals("CO"))
          isJasper = Boolean.FALSE;
        else
          isJasper = Boolean.TRUE;
      } else if (action.equals("DownloadReport"))
        isJasper = Boolean.FALSE;

      connection = (Connection) paramObject.get("connection");
      reportDir = paramObject.getString("reportDir");
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", opnEnvCmtCount.getOrganization().getId());
      designParameters.put("inpOpnEnvCmtId", request.getParameter("inpRecordId"));
      designParameters.put("RealPath", realPath);
      designParameters.put("BASE_DESIGN", paramObject.get("basedesign"));
      designParameters.put("CheckBoxImagePath",
          realPath + "web/sa.elm.ob.scm/images/checkboxchecked.jpg");
      designParameters.put("UncheckedImagePath",
          realPath + "web/sa.elm.ob.scm/images/unchecked.jpg");
      designParameters.put("status", opnEnvCmtCount.getAlertStatus());

      strReportName = reportDir + "openenvelopproposal/OpenEnvelopProposal.jrxml";
      strFileName = "OpenEnvelopProposalList" + " " + " " + hijriDate;

      if (opnEnvCmtRpt.equals("PROP")) {
        isJasper = Boolean.TRUE;
        strReportName = reportDir + "openenvelopproposal/OpenEnvelopProposal.jrxml";
        strFileName = "OpenEnvelopProposalList" + " " + " " + hijriDate;
      } else if (opnEnvCmtRpt.equals("COMT")) {
        isJasper = Boolean.TRUE;
        strReportName = reportDir + "openenvelopcommittee/OpenEnvelopeCommittee.jrxml";
        strFileName = "OpenEnvelopCommittee" + " " + " " + hijriDate;
      }

      request.setAttribute("pageType", paramObject.getString("pageType"));
      request.setAttribute("inpTabId", paramObject.getString("tabId"));
      request.setAttribute("inpWindowID", paramObject.getString("inpWindowID"));
      request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
      strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/PrintReport.jsp";

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
