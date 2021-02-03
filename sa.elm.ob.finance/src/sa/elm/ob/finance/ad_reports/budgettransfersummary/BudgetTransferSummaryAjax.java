package sa.elm.ob.finance.ad_reports.budgettransfersummary;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class BudgetTransferSummaryAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);  
    try {
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("getBudgetTransfer")) {    	  
        JSONObject jsob = BudgetTransferSummaryDAO.getBudgetTransfer(vars.getClient(), vars.getOrg(), request.getParameter("yearId"), null);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } 
    } catch (final Exception e) {
      log4j.error("Exception in BudgetTransferSummaryAjax : ", e);
    }
  }
}