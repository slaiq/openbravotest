package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationInformation;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author Sathish Kumar.P
 * 
 */

public class MIRPrint extends GenerateJasperPrint {
  private static Logger log = Logger.getLogger(MIRPrint.class);

  private static MIRPrint mirPrintReport;

  public static MIRPrint getInstance() {
    if (mirPrintReport == null) {
      mirPrintReport = new MIRPrint();
    }
    return mirPrintReport;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String imageFlag = "N", inpOutputType = "", reportDir = "", inpDocNo = "", basedesign = "";
      MaterialIssueRequest objMir = OBDal.getInstance().get(MaterialIssueRequest.class,
          request.getParameter("inpRecordId"));
      inpDocNo = objMir.getDocumentNo();
      OrganizationInformation objInfo = Utility.getOrgInfo(objMir.getOrganization().getId());
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String user = vars.getUser();
      PrintReportDAO printDao = new PrintReportDAO();

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
      designParameters.put("inpOrgId", objMir.getOrganization().getId());
      designParameters.put("inpMaterialReqId", request.getParameter("inpRecordId"));
      inpOutputType = request.getParameter("inpOutputType");
      designParameters.put("BASE_DESIGN", basedesign);
      String warehouseType = "";
      if (objMir.getWarehouse() != null) {
        warehouseType = objMir.getWarehouse().getEscmWarehouseType();
      }
      if (inpOutputType != null) {
        if (inpOutputType.equals("A4")) {
          if (warehouseType.equals("RTW")) {
            strReportName = reportDir
                + "MIRIssueReturnTransaction/MIRIssueReturnTransactionReport.jrxml";
            strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
          } else {
            strReportName = reportDir + "materialissuerequest/MaterialIssueRequestA4.jrxml";
            strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
          }
        } else if (inpOutputType.equals("DM")) {
          printDao.updateMIRPrinted(user, objMir.getId());
          strReportName = reportDir + "materialissuerequest/MaterialIssueRequestDotMatrix.jrxml";
          strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
        }
      } else {
        if (objMir.getWarehouse().getEscmWarehouseType().equals("RTW")) {
          strReportName = reportDir
              + "MIRIssueReturnTransaction/MIRIssueReturnTransactionReport.jrxml";
          strFileName = "Material Req" + " " + inpDocNo + " " + hijriDate;
        }
      }

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
