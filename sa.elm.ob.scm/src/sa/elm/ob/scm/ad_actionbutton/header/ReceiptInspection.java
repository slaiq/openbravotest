package sa.elm.ob.scm.ad_actionbutton.header;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.hcm.ad_forms.employee.dao.EmployeeDAO;
import sa.elm.ob.scm.ESCMInspection;
import sa.elm.ob.scm.EscmInitialReceipt;

/**
 * Servlet implementation class Receipt Inspection
 * 
 * @author Gopalakrishnan created on 22/02/2017
 */

public class ReceiptInspection extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String includeIn = "../web/sa.elm.ob.scm/jsp/ReceiptInspection/ReceiptInspection.jsp";

  // private static Logger log4j = Logger.getLogger(ReceiptInspectionDAO.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public ReceiptInspection() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Connection con = null;
    EmployeeDAO employeedao = null;
    // ReceiptInspectionDAO dao = null;

    try {
      VariablesSecureApp vars = new VariablesSecureApp(request);
      con = getConnection();
      // dao = new ReceiptInspectionDAO(con);
      boolean isinsepected = true;
      String action = vars.getStringParameter("action") == null ? "" : vars
          .getStringParameter("action");
      log4j.debug("action:" + action);

      if (action.equals("processInspection")) {

        int result = 0;

        if (result > 0)
          advisePopUpRefresh(request, response, "Success", "Success",
              "Process completed successfully");
        else
          advisePopUpRefresh(request, response, "Error", "Error",
              "Error while processing Receipt Inspection.");

      } else if (action.equals("UpdateInsLines")) {
        int count = 1;
        employeedao = new EmployeeDAO(con);

        OBContext.setAdminMode();
        JSONObject jsonObject = new JSONObject(request.getParameter("inpSelList").toString()), json = null;
        JSONArray jsonArray = jsonObject.getJSONArray("List");
        String receiptId = request.getParameter("inpReceiptId");
        if (receiptId != null) {
          for (int i = 0; i < jsonArray.length(); i++) {
            json = jsonArray.getJSONObject(i);
            ESCMInspection inspection = OBDal.getInstance().get(ESCMInspection.class,
                json.getString("id"));
            if (inspection != null) {
              inspection.setUpdated(new java.util.Date());
              inspection.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              if (json.getString("notes") != null
                  && StringUtils.isNotEmpty(json.getString("notes")))
                inspection.setNotes(json.getString("notes"));
              if (json.getString("qty") != null && StringUtils.isNotEmpty(json.getString("qty")))
                inspection.setQuantity(new BigDecimal(json.getString("qty")));

              if (json.getString("inspectiondate") != null
                  && StringUtils.isNotEmpty(json.getString("inspectiondate"))) {
                inspection.setInspectionDate(employeedao.convertGregorian(json.getString(
                    "inspectiondate").toString()));
              }
              if (json.getString("inspectedby") != null
                  && StringUtils.isNotEmpty(json.getString("inspectedby")))
                inspection.setInspectedBy(json.getString("inspectedby"));
              if (json.getString("qualitycode") != null
                  && StringUtils.isNotEmpty(json.getString("qualitycode")))
                inspection.setQualityCode(json.getString("qualitycode"));
              if (json.getString("isinspected") != null
                  && StringUtils.isNotEmpty(json.getString("isinspected"))) {
                if (json.getString("isinspected").equals("Y"))
                  inspection.setInspected(true);
                else
                  inspection.setInspected(false);
              }

              OBDal.getInstance().save(inspection);
              OBDal.getInstance().flush();
              if (inspection.getEscmInitialreceipt() != null) {
                EscmInitialReceipt intial = inspection.getEscmInitialreceipt();
                log4j.debug("st:" + json.getString("status"));
                if (json.getString("status").equals("Accept"))
                  intial.setAcceptedQty(new BigDecimal(json.getString("qty")));
                else
                  intial.setRejectedQty(new BigDecimal(json.getString("qty")));
                OBDal.getInstance().save(intial);
                OBDal.getInstance().flush();
              }
            }
          }

          ShipmentInOut receipt = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
          for (ESCMInspection line : receipt.getESCMInspectionList()) {
            if (!line.isInspected()) {
              isinsepected = false;
            }
          }
          if (isinsepected) {
            receipt.setEscmIsinspected(true);
            OBDal.getInstance().save(receipt);
            OBDal.getInstance().flush();

          }

          receipt.setEscmDocstatus("ESCM_INS");
          OBDal.getInstance().save(receipt);
          OBDal.getInstance().flush();
          if (count == 1) {
            advisePopUpRefresh(request, response, "SUCCESS", "Success",
                "Process Completed Successfully");
          } else
            advisePopUpRefresh(request, response, "ERROR", "Error",
                "Process not Completed Successfully");

        }
      } else {

        String inpReceiptId = vars.getStringParameter("M_InOut_ID");
        // int result = dao.insertInspectionRecords(vars.getClient(), inpReceiptId, vars);
        if (inpReceiptId != null) {
          log4j.debug("rece:" + inpReceiptId);
          request.setAttribute("inpReceiptId", inpReceiptId);
          request.getRequestDispatcher(includeIn).include(request, response);
        } else {
          advisePopUpRefresh(request, response, "Error", "Error",
              "Error while processing Receipt Inspection.Please Contact System Administrator");
        }
      }
    } catch (Exception e) {
      log4j.debug("Exception in ReceiptInsepction", e);
    } finally {
      try {
        if (con != null) {
          con.close();
        }
      } catch (Exception e) {

      }
    }
  }
}
