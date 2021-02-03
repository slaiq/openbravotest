package sa.elm.ob.finance.ad_reports.payment;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

import sa.elm.ob.finance.EfinLookupLine;

public class PaymentReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/payment/PaymentReport.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String strReportName = "";
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));

      OBContext.setAdminMode();

      if (action.equals("")) {
        log4j.debug("action");
        request.setAttribute("inpfinPaymentId", vars.getStringParameter("inpfinPaymentId"));
        request.setAttribute("inpClientId", vars.getClient());
        request.setAttribute("inpOrgId", vars.getOrg());
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("pdf")) {
        // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
        // "");
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/payment/PaymentReport.jrxml";
        String strOutput = "pdf";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        String inpfinPaymentId = request.getParameter("inpfinPaymentId");
        FIN_Payment paymentOut = OBDal.getInstance().get(FIN_Payment.class, inpfinPaymentId);
        String inpClientId = request.getParameter("inpClientId");
        String inpOrgId = request.getParameter("inpOrgId");
        parameters.put("inpfinPaymentId", inpfinPaymentId);
        parameters.put("inpClientId", inpClientId);
        parameters.put("inpOrgId", inpOrgId);
        if (paymentOut.getEfinLocationCode() != null) {
          EfinLookupLine line = OBDal.getInstance().get(EfinLookupLine.class,
              paymentOut.getEfinLocationCode().getId());
          if (line.getSearchKey() != null)
            parameters.put("inpLocationCode", line.getSearchKey());
          else
            parameters.put("inpLocationCode", "");
        }
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      } else if (action.equals("xls")) {
        // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
        // "");
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/payment/PaymentReport.jrxml";
        String strOutput = "xls";
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        String inpfinPaymentId = request.getParameter("inpfinPaymentId");
        FIN_Payment paymentOut = OBDal.getInstance().get(FIN_Payment.class, inpfinPaymentId);
        String inpClientId = request.getParameter("inpClientId");
        String inpOrgId = request.getParameter("inpOrgId");
        parameters.put("inpfinPaymentId", inpfinPaymentId);
        parameters.put("inpClientId", inpClientId);
        parameters.put("inpOrgId", inpOrgId);
        if (paymentOut.getEfinLocationCode() != null) {
          EfinLookupLine line = OBDal.getInstance().get(EfinLookupLine.class,
              paymentOut.getEfinLocationCode().getId());
          if (line.getSearchKey() != null)
            parameters.put("inpLocationCode", line.getSearchKey());
          else
            parameters.put("inpLocationCode", "");
        }
        // log4j.info("inpfinPaymentId>"+inpfinPaymentId);
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      log4j.error("Exception in PaymentReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
