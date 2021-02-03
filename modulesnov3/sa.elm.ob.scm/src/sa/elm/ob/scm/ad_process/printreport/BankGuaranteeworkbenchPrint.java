package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;

import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author Gowtham.V
 *
 */
public class BankGuaranteeworkbenchPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(BankGuaranteeworkbenchPrint.class);

  private static BankGuaranteeworkbenchPrint bankGuaranteeworkbenchPrint;

  public static BankGuaranteeworkbenchPrint getInstance() {
    if (bankGuaranteeworkbenchPrint == null) {
      bankGuaranteeworkbenchPrint = new BankGuaranteeworkbenchPrint();
    }
    return bankGuaranteeworkbenchPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("")) {
        String tabId = request.getParameter("inpTabId");
        if (tabId.equals("6732339A97874A85BF73542C2B5AFF88")) {
          Tab tab = OBDal.getInstance().get(Tab.class, tabId);

          request.setAttribute("pageType", paramObject.getString("pageType"));
          request.setAttribute("inpTabId", paramObject.getString("tabId"));
          request.setAttribute("inpWindowID", tab.getWindow().getId());
          request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
          isJasper = Boolean.FALSE;
          strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/BGWorkBenchReport.jsp";
        }
      } else {
        String reportDir = "";
        String inpBGWorkbenchRpt = request.getParameter("inpBGWorkbenchRpt");
        reportDir = paramObject.getString("reportDir");
        isJasper = Boolean.TRUE;
        connection = (Connection) paramObject.get("connection");
        designParameters.put("inpBgDetailId", request.getParameter("inpRecordId"));

        if (inpBGWorkbenchRpt.equals("BGVLR")) {
          strReportName = reportDir + "BGVerficationLetterReport/BGVerficationLetterReport.jrxml";
          strFileName = "BGVerficationLetter" + " " + " " + hijriDate;
        } else if (inpBGWorkbenchRpt.equals("BGELR")) {
          strReportName = reportDir + "BGExtentionLetterReport/BGExtentionLetterReport.jrxml";
          strFileName = "BGExtentionLetterReport" + " " + " " + hijriDate;
        } else if (inpBGWorkbenchRpt.equals("BGCLR")) {
          strReportName = reportDir + "BGConfiscationLetterReport/BGConfiscationLetterReport.jrxml";
          strFileName = "BGConfiscationLetterReport" + " " + " " + hijriDate;
        } else if (inpBGWorkbenchRpt.equals("BGRLR")) {
          strReportName = reportDir + "bgreleaseletter/BGReleaseLetter.jrxml";
          strFileName = "BGReleaseLetter" + " " + " " + hijriDate;
        } else if (inpBGWorkbenchRpt.equals("BGDR")) {
          strReportName = reportDir + "BGDetailsReport/BGDetailsReport.jrxml";
          strFileName = "BGDetailsReport" + " " + " " + hijriDate;
        } else if (inpBGWorkbenchRpt.equals("BGARLR")) {
          strReportName = reportDir + "bgamountreductionletter/BGAmountReductionLetter.jrxml";
          strFileName = "BGAmountReductionLetter" + " " + " " + hijriDate;
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
