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
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * @author Sathish Kumar.P
 *
 */

public class InventoryCountPrint extends GenerateJasperPrint {
  private static Logger log = Logger.getLogger(InventoryCountPrint.class);

  private static InventoryCountPrint inventoryCountReport;

  public static InventoryCountPrint getInstance() {
    if (inventoryCountReport == null) {
      inventoryCountReport = new InventoryCountPrint();
    }
    return inventoryCountReport;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String imageFlag = "N", reportDir = "", inpDocNo = "";
      inpDocNo = (request.getParameter("documentNo") == null ? "" : request
          .getParameter("documentNo"));
      InventoryCount invcount = OBDal.getInstance().get(InventoryCount.class,
          request.getParameter("inpRecordId"));

      OrganizationInformation objInfo = invcount.getOrganization().getOrganizationInformationList()
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
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", invcount.getOrganization().getId());
      designParameters.put("inpInOutId", request.getParameter("inpRecordId"));
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", invcount.getOrganization().getId());
      designParameters.put("inpInventoryCountId", request.getParameter("inpRecordId"));
      if (invcount.getEscmStatus().equals("DR")) {
        strReportName = reportDir + "CountingTicketlistReport/CountingTicketlistReport.jrxml";
        strFileName = "Counting Ticket list" + " " + inpDocNo + " " + hijriDate;
      } else if (invcount.getEscmStatus().equals("CO")) {
        strReportName = reportDir + "InventoryCountingReport/InventoryCountingReport.jrxml";
        strFileName = "Inventory Counting" + " " + inpDocNo + " " + hijriDate;
      }

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}