package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author oalbader
 *
 */
public class TechnicalEvaluationEventPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(TechnicalEvaluationEventPrint.class);

  private static TechnicalEvaluationEventPrint proposalEvaluationEventPrint;

  public static TechnicalEvaluationEventPrint getInstance() {
    if (proposalEvaluationEventPrint == null) {
      proposalEvaluationEventPrint = new TechnicalEvaluationEventPrint();
    }
    return proposalEvaluationEventPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String reportDir = "", basedesign = "";
      EscmTechnicalevlEvent proposalEvl = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
          request.getParameter("inpRecordId"));
      reportDir = paramObject.getString("reportDir");
      basedesign = paramObject.getString("basedesign");
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));

      if (action.equals("")) {
        request.setAttribute("pageType", paramObject.getString("pageType"));
        request.setAttribute("inpTabId", paramObject.getString("tabId"));
        request.setAttribute("inpWindowID", paramObject.getString("inpWindowID"));
        request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
        request.setAttribute("inpReceivingType", paramObject.getString("receiveType"));
        request.setAttribute("inpDocNo", paramObject.getString("documentNo"));
        log.debug("Enter into");
        log.debug("recordid" + request.getParameter("inpRecordId"));
        isJasper = Boolean.FALSE;
        strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/TechnicalEvaluationEventReport.jsp";
      } else {
        isJasper = Boolean.TRUE;
        connection = (Connection) paramObject.get("connection");
        designParameters.put("inpOrgId", proposalEvl.getOrganization().getId());
        designParameters.put("inpEventId", request.getParameter("inpRecordId"));
        designParameters.put("BASE_DESIGN", basedesign);
        String inpTechnicalReport = request.getParameter("inpTechnicalReport");
        if (inpTechnicalReport.equals("TSR")) {
          String inpParamOne = request.getParameter("inpParamOne");
          String inpParamTwo = request.getParameter("inpParamTwo");
          String inpParamThree = request.getParameter("inpParamThree");
          designParameters.put("inpParamOne", inpParamOne);
          designParameters.put("inpParamTwo", inpParamTwo);
          designParameters.put("inpParamThree", inpParamThree);
          strReportName = reportDir + "technicalstudyrequest/TechnicalStudyRequest.jrxml";
          strFileName = "TechnicalStudyRequest" + " " + " " + hijriDate;
        } else if (inpTechnicalReport.equals("TSRR")) {
          String replyParamOne = request.getParameter("replyParamOne");
          String replyParamTwo = request.getParameter("replyParamTwo");
          String replyParamThree = request.getParameter("replyParamThree");
          String replyParamFour = request.getParameter("replyParamFour");
          String replyParamFive = request.getParameter("replyParamFive");
          String replyParamSix = request.getParameter("replyParamSix");
          String replyParamSeven = request.getParameter("replyParamSeven");
          designParameters.put("inpParamOne", replyParamOne);
          designParameters.put("inpParamTwo", replyParamTwo);
          designParameters.put("inpParamThree", replyParamThree);
          designParameters.put("inpParamFour", replyParamFour);
          designParameters.put("inpParamFive", replyParamFive);
          designParameters.put("inpParamSix", replyParamSix);
          designParameters.put("inpParamSeven", replyParamSeven);
          strReportName = reportDir + "technicalstudyreply/TechnicalStudyReply.jrxml";
          strFileName = "TechnicalStudyWithReply" + " " + " " + hijriDate;
        }

      }

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
