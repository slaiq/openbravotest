package sa.elm.ob.finance.charts.GLAdjustmentReport;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.jettison.json.JSONArray;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * @author Priyanka Ranjan on 13/10/2016
 */

public class GLAdjustmentReportAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    VariablesSecureApp vars = null;
    GLAdjustmentReportDAO dao = null;
    String action = "", orgId = "", inphDoctypelist = "";

    JSONArray jsonArray = null;
    if (log4j.isDebugEnabled())
      log4j.debug(" GLAdjustmentReportAjax ");
    try {
      dao = new GLAdjustmentReportDAO(getConnection());
      vars = new VariablesSecureApp(request);
      action = vars.getStringParameter("action");
      orgId = request.getParameter("orgId");
      inphDoctypelist = vars.getStringParameter("inphDoctypelist");
      if (log4j.isDebugEnabled())
        log4j
            .debug("Action: " + action + "OrgId: " + orgId + "inphDoctypelist: " + inphDoctypelist);

      if (action.equals("getOrganization")) {
        jsonArray = dao.getOrganization(vars.getClient());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getDocumentNo")) {
        log4j.debug("action");
        jsonArray = dao.getDocumentNo(inphDoctypelist, orgId, vars.getClient());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      }
    }

    catch (Exception e) {
      log4j.error("Exception in GLAdjustmentReportAjax ", e);
    }

  }
}
