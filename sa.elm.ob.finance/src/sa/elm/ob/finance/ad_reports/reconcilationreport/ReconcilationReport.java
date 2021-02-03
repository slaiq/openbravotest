package sa.elm.ob.finance.ad_reports.reconcilationreport;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

public class ReconcilationReport extends HttpSecureAppServlet {

  /**
   * process for getting report in reconciliation tab for the current transaction
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/reconcilation/reconcilationrep.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String strReportName = "";
      String action = (request.getParameter("inpAction") == null ? "" : request
          .getParameter("inpAction"));

      VariablesSecureApp vars = new VariablesSecureApp(request);
      Connection con = getConnection();
      OBContext.setAdminMode();

      String sql = "";
      PreparedStatement st = null;
      ResultSet rs = null;

      String inpClientId = "";
      inpClientId = vars.getClient();

      if (action.equals("")) {
        log4j.debug("action");

        // localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        request.setAttribute("inpfinReconciliationId",
            vars.getStringParameter("inpfinReconciliationId"));
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("pdf")) {

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        String inpfinReconId = request.getParameter("inpfinReconciliationId");
        String inphDoctypelist = "recon";
        String inpRequestNo = "";
        sql = "SELECT documentno FROM fin_reconciliation  WHERE  fin_reconciliation_id = ? and ad_client_id = ?  ";
        st = con.prepareStatement(sql);
        st.setString(1, inpfinReconId);
        st.setString(2, inpClientId);

        rs = st.executeQuery();

        if (rs.next()) {
          inpRequestNo = rs.getString("documentno");
        }

        parameters.put("inpGlJournalId", inpfinReconId);
        parameters.put("inphDoctypelist", inphDoctypelist);
        parameters.put("inpRequestNo", inpRequestNo);
        parameters.put("inpClientId", inpClientId);
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/Manualadjustment/GLAdjustment.jrxml";
        String strOutput = "pdf";
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);

      } else if (action.equals("xls")) {

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        String inpfinReconId = request.getParameter("inpfinReconciliationId");
        String inphDoctypelist = "recon";
        String inpRequestNo = "";
        sql = "SELECT documentno FROM fin_reconciliation  WHERE  fin_reconciliation_id = ?  and ad_client_id= ? ";
        st = con.prepareStatement(sql);
        st.setString(1, inpfinReconId);
        st.setString(2, inpClientId);
        rs = st.executeQuery();

        if (rs.next()) {
          inpRequestNo = rs.getString("documentno");
        }

        parameters.put("inpGlJournalId", inpfinReconId);
        parameters.put("inphDoctypelist", inphDoctypelist);
        parameters.put("inpRequestNo", inpRequestNo);
        parameters.put("inpClientId", inpClientId);
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/Manualadjustment/GLAdjustment.jrxml";
        String strOutput = "xls";
        renderJR(vars, response, strReportName, strOutput, parameters, null, null);

      }

    } catch (Exception e) {
      log4j.error("Exception in Reconcilation Report :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
