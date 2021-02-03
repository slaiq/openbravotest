package sa.elm.ob.finance.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationInformation;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class RDVSummaryReport extends GenerateJasperPrint {
  Logger log = Logger.getLogger(RDVSummaryReport.class);

  private static RDVSummaryReport rdvSummaryPrint;

  public static RDVSummaryReport getInstance() {
    if (rdvSummaryPrint == null) {
      rdvSummaryPrint = new RDVSummaryReport();
    }
    return rdvSummaryPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String basedesign = paramObject.getString("basedesign");
      final String financeReportDir = basedesign
          + "/sa/elm/ob/finance/ad_reports/RDVSummary/RDVSummary_1.jrxml";

      String imageFlag = "N";

      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class,
          request.getParameter("inpRecordId"));
      log.debug(request.getParameter("inpRecordId"));

      OrganizationInformation objInfo = transaction.getOrganization()
          .getOrganizationInformationList().get(0);
      // check org have image
      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }

      isJasper = Boolean.TRUE;
      connection = (Connection) paramObject.get("connection");
      designParameters.put("BASE_DESIGN", basedesign);
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", transaction.getOrganization().getId());
      designParameters.put("Version_Id", transaction.getId());
      designParameters.put("Efin_Rdv_ID", transaction.getEfinRdv().getId());
      designParameters.put("TXN_Type", transaction.getEfinRdv().getTXNType());
      designParameters.put("status", transaction.getAppstatus());

      strReportName = financeReportDir;
      strFileName = "RDVSummary " + "-" + " " + hijriDate;

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
