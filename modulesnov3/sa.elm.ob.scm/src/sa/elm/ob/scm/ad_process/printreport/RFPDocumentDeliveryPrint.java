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

import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * 
 */
public class RFPDocumentDeliveryPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(RFPDocumentDeliveryPrint.class);

  private static RFPDocumentDeliveryPrint RFPDocumentDeliveryPrint;

  public static RFPDocumentDeliveryPrint getInstance() {
    if (RFPDocumentDeliveryPrint == null) {
      RFPDocumentDeliveryPrint = new RFPDocumentDeliveryPrint();
    }
    return RFPDocumentDeliveryPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String imageFlag = "N", reportDir = "";
      Escmsalesvoucher salesVouchCount = OBDal.getInstance().get(Escmsalesvoucher.class,
          request.getParameter("inpRecordId"));
      log.debug(request.getParameter("inpRecordId"));

      OrganizationInformation objInfo = salesVouchCount.getOrganization()
          .getOrganizationInformationList().get(0);
      // check org have image
      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }
      reportDir = paramObject.getString("reportDir");
      isJasper = Boolean.TRUE;
      connection = (Connection) paramObject.get("connection");
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", salesVouchCount.getOrganization().getId());
      designParameters.put("inpSalesVoucherId", request.getParameter("inpRecordId"));

      strReportName = reportDir + "rfpdocumentdelivery/RFPDocumentDeliveryNote.jrxml";
      strFileName = "RFPSalesVoucher" + " " + " " + hijriDate;

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
