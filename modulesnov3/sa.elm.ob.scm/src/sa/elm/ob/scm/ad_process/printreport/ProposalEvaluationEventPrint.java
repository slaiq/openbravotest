package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author oalbader
 *
 */
public class ProposalEvaluationEventPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(ProposalEvaluationEventPrint.class);

  private static ProposalEvaluationEventPrint proposalEvaluationEventPrint;

  public static ProposalEvaluationEventPrint getInstance() {
    if (proposalEvaluationEventPrint == null) {
      proposalEvaluationEventPrint = new ProposalEvaluationEventPrint();
    }
    return proposalEvaluationEventPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String reportDir = "", basedesign = "";
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      ESCMProposalEvlEvent proposalEvl = OBDal.getInstance().get(ESCMProposalEvlEvent.class,
          request.getParameter("inpRecordId"));
      reportDir = paramObject.getString("reportDir");
      basedesign = paramObject.getString("basedesign");

      if (proposalEvl.getBidNo() == null || proposalEvl.getBidNo().getBidtype().equals("DR")) {
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
          strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/ProposalEvaluationEvent.jsp";

        } else {
          isJasper = Boolean.TRUE;
          connection = (Connection) paramObject.get("connection");
          designParameters.put("inpProposalCounts",
              "" + proposalEvl.getEscmProposalAttrList().size());
          designParameters.put("inpOrgId", proposalEvl.getOrganization().getId());
          designParameters.put("inpEventId", request.getParameter("inpRecordId"));
          designParameters.put("BASE_DESIGN", basedesign);
          if (proposalEvl.getEscmCommittee() != null)
            designParameters.put("committeeId", proposalEvl.getEscmCommittee().getId());
          else
            designParameters.put("committeeId", "");
          String paramOne = request.getParameter("evaParamInput");
          designParameters.put("paramOne", paramOne);
          strReportName = reportDir
              + "proposalEvaluationEventDirect/ProposalEvaluationEventDirect.jrxml";
          strFileName = "proposalEvaluationEventDirect" + " " + " " + hijriDate;

        }

      } else {
        isJasper = Boolean.TRUE;
        connection = (Connection) paramObject.get("connection");
        designParameters.put("inpOrgId", proposalEvl.getOrganization().getId());
        designParameters.put("inpEventId", request.getParameter("inpRecordId"));
        designParameters.put("BASE_DESIGN", basedesign);
        designParameters.put("committeeId", proposalEvl.getEscmCommittee().getId());
        strReportName = reportDir + "proposalevaluation/ProposalEvaluationCommittee.jrxml";
        strFileName = "proposalEvaluationEvent" + " " + " " + hijriDate;
      }

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
