package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author Sathish Kumar.P
 * 
 */

public class CustodyTransferPrint extends GenerateJasperPrint {
  private static Logger log = Logger.getLogger(CustodyTransferPrint.class);

  private static CustodyTransferPrint CTPrintReport;

  public static CustodyTransferPrint getInstance() {
    if (CTPrintReport == null) {
      CTPrintReport = new CustodyTransferPrint();
    }
    return CTPrintReport;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      @SuppressWarnings("unused")
      String imageFlag = "N", inpOutputType = "", reportDir = "", basedesign = "";
      ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class,
          request.getParameter("inpRecordId"));
      OrganizationInformation objInfo = Utility.getOrgInfo(objInout.getOrganization().getId());

      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }

      reportDir = paramObject.getString("reportDir");
      basedesign = paramObject.getString("basedesign");
      isJasper = Boolean.TRUE;
      connection = (Connection) paramObject.get("connection");
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", objInout.getOrganization().getId());
      designParameters.put("inpInOutId", request.getParameter("inpRecordId"));
      designParameters.put("BASE_DESIGN", basedesign);
      inpOutputType = request.getParameter("inpOutputType");

      strReportName = reportDir + "CustodyReport/custodyreport.jrxml";
      strFileName = "Custody Transfer" + " " + objInout.getDocumentNo() + " " + hijriDate;

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
