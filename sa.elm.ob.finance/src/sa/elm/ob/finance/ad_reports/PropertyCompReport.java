package sa.elm.ob.finance.ad_reports;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

public class PropertyCompReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String strReportName = "";
      VariablesSecureApp vars = new VariablesSecureApp(request);

      OBContext.setAdminMode();

      // String strOutputFileName = "Site Attendance Report_" + sf.format(today).replaceAll("-",
      // "");
      strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/PropertyCompensation/PropertyCompensation.jrxml";
      String strOutput = "pdf";
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String inp_property_comp_id = request.getParameter("Efin_Property_Compensation_ID");
      String inpClientId = vars.getClient();
      String inpOrgId = vars.getOrg();
      parameters.put("inp_property_comp_id", inp_property_comp_id);
      parameters.put("inpClientId", inpClientId);
      parameters.put("inpOrgId", inpOrgId);

      renderJR(vars, response, strReportName, strOutput, parameters, null, null);

    } catch (Exception e) {
      log4j.error("Exception in PropertyCompReport :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
