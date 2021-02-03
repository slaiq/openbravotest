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

import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class ReturnTransactionPrint extends GenerateJasperPrint {
  private static Logger log = Logger.getLogger(InventoryCountPrint.class);

  private static ReturnTransactionPrint returnTransactionPrint;

  public static ReturnTransactionPrint getInstance() {
    if (returnTransactionPrint == null) {
      returnTransactionPrint = new ReturnTransactionPrint();
    }
    return returnTransactionPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String imageFlag = "N", reportDir = "", inpDocNo = "";
      inpDocNo = (request.getParameter("documentNo") == null ? "" : request
          .getParameter("documentNo"));

      // find organisation image
      ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class,
          request.getParameter("inpRecordId"));

      OrganizationInformation objInfo = objInout.getOrganization().getOrganizationInformationList()
          .get(0);
      // check org have image
      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }

      reportDir = paramObject.getString("reportDir");
      isJasper = Boolean.TRUE;
      connection = (Connection) paramObject.get("connection");
      designParameters.put("BASE_DESIGN", paramObject.getString("basedesign"));
      designParameters.put("inpInOutId", request.getParameter("inpRecordId"));
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpInventoryCountId", request.getParameter("inpRecordId"));
      strReportName = reportDir + "returntransaction/ReturnTransaction.jrxml";
      strFileName = "Return Transaction" + " " + inpDocNo + " " + hijriDate;

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
