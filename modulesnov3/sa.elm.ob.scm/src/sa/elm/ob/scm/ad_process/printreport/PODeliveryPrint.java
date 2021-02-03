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
 * @author Gopinagh. R
 * 
 */
public class PODeliveryPrint extends GenerateJasperPrint {
  /**
   * This class is responsible to print all reports from PO Receipt
   */
  Logger log = Logger.getLogger(PODeliveryPrint.class);

  private static PODeliveryPrint poDeliveryPrint;

  public static PODeliveryPrint getInstance() {
    if (poDeliveryPrint == null) {
      poDeliveryPrint = new PODeliveryPrint();
    }
    return poDeliveryPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String imageFlag = "N", inpOutputType = "", reportDir = "", inpDocNo = "", paramType = "",
          inpReceivingType = "", receiveType = "", documentNo = "";
      ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class,
          request.getParameter("inpRecordId"));
      inpDocNo = objInout.getDocumentNo();
      OrganizationInformation objInfo = Utility.getOrgInfo(objInout.getOrganization().getId());

      if (objInfo != null) {
        if (objInfo.getYourCompanyDocumentImage() != null) {
          imageFlag = "Y";
        }
      }

      reportDir = paramObject.getString("reportDir");
      isJasper = Boolean.TRUE;
      connection = (Connection) paramObject.get("connection");
      designParameters.put("inpImageFlag", imageFlag);
      designParameters.put("inpOrgId", objInout.getOrganization().getId());
      designParameters.put("inpInOutId", request.getParameter("inpRecordId"));
      inpOutputType = request.getParameter("inpOutputType") == null ? ""
          : request.getParameter("inpOutputType");
      paramType = paramObject.getString("paramType") == null ? ""
          : paramObject.getString("paramType");
      inpReceivingType = paramObject.getString("inpReceivingType") == null ? ""
          : paramObject.getString("inpReceivingType");
      receiveType = paramObject.getString("receiveType") == null ? ""
          : paramObject.getString("receiveType");
      if (paramType.equals("InventoryDelivery") && inpReceivingType.equals("DEL")) {
        if (inpOutputType.equals("A4")) {
          strReportName = reportDir + "InventoryDeliverReport/InventoryDeliverUsingA4.jrxml";
          strFileName = "Inventory Delivery" + " " + inpDocNo + " " + hijriDate;
        } else if (inpOutputType.equals("DM")) {
          strReportName = reportDir + "InventoryDeliverReport/InventoryDeliverUsingDotMatrix.jrxml";
          strFileName = "Inventory Delivery" + " " + inpDocNo + " " + hijriDate;
        }
      } else if (paramType.equals("SiteDelivery") && inpReceivingType.equals("SR")) {
        if (inpOutputType.equals("A4")) {
          strReportName = reportDir + "SiteDeliverNoteReport/SiteDeliverNote.jrxml";
          strFileName = "Site Receipt" + " " + inpDocNo + " " + hijriDate;
        } else if (inpOutputType.equals("DM")) {
          strReportName = reportDir + "SiteDeliverNoteReport/SiteDeliverNoteDotMatrix.jrxml";
          strFileName = "Site Receipt" + " " + inpDocNo + " " + hijriDate;
        }
      } else if (receiveType.equals("IR")) {
        strReportName = reportDir + "initialreceiving/InitialReceiving.jrxml";
        strFileName = "Initial Receipt" + " " + documentNo + " " + hijriDate;
      } else if (receiveType.equals("INS")) {
        strReportName = reportDir + "InspectionReport/Inspection.jrxml";
        strFileName = "Inspection" + " " + documentNo + " " + hijriDate;
      }
    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
