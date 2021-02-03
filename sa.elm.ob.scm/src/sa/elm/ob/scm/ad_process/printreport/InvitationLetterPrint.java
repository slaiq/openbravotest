package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.Location;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.ad_reports.InvitationLetterReport.InvitationLetterReport;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

/**
 * 
 */
public class InvitationLetterPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(InvitationLetterPrint.class);

  private static InvitationLetterPrint InvitationLetterPrint;

  public static InvitationLetterPrint getInstance() {
    if (InvitationLetterPrint == null) {
      InvitationLetterPrint = new InvitationLetterPrint();
    }
    return InvitationLetterPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action")), reportDir = "", supplierIds = "";
      List<InvitationLetterReport> groupList = new ArrayList<InvitationLetterReport>();
      EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class,
          request.getParameter("inpRecordId"));
      if (action.equals("")) {
        isJasper = Boolean.FALSE;

        InvitationLetterReport es = null;

        for (Escmbidsuppliers suppliers : bidMgmt.getEscmBidsuppliersList()) {
          es = new InvitationLetterReport();
          es.setSuppliersId(suppliers.getId());
          es.setBpName(suppliers.getSupplier());

          Location loc = OBDal.getInstance().get(Location.class,
              suppliers.getLocationAddress().getId());
          es.setBpAddress((loc.getAddressLine1() == null ? " " : loc.getAddressLine1()) + " "
              + (loc.getAddressLine2() == null ? " " : loc.getAddressLine2()));
          groupList.add(es);
        }
      } else {
        isJasper = Boolean.TRUE;
        designParameters.put("inpBidManId", request.getParameter("inpRecordId"));
        String inpSupList = request.getParameter("inpSupList");

        if (inpSupList.equals("0")) {
          supplierIds = "";
        } else {
          supplierIds = "and bidsup.escm_bidsuppliers_id = '" + inpSupList + "'";
        }
      }
      reportDir = paramObject.getString("reportDir");
      connection = (Connection) paramObject.get("connection");
      designParameters.put("inpSupId", supplierIds);
      if (bidMgmt.getBidtype().equals("DR")) {
        strReportName = reportDir + "InvitationLetterReport/InvitationLetterDirect.jrxml";
        strFileName = "InvitationLetterDirect" + " " + " " + hijriDate;
      } else {
        strReportName = reportDir + "InvitationLetterReport/InvitationLetterReport.jrxml";
        strFileName = "InvitationLetterReport" + " " + " " + hijriDate;
      }

      request.setAttribute("pageType", paramObject.getString("pageType"));
      request.setAttribute("inpTabId", paramObject.getString("tabId"));
      request.setAttribute("inpWindowID", paramObject.getString("inpWindowID"));
      request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
      request.setAttribute("inpSupplierList", groupList);
      strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/PrintReport.jsp";
    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
