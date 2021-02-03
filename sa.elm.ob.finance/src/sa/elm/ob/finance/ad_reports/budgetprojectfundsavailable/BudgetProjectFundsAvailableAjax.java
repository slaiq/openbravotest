package sa.elm.ob.finance.ad_reports.budgetprojectfundsavailable;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.util.DAO.CommonReportDAO;

public class BudgetProjectFundsAvailableAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);  
    try {
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("getBudgetAccounts")) { 
    	  JSONObject jsob = null;
        JSONArray jsonArray = CommonReportDAO.getBudgetAccounts(vars.getClient(), vars.getRole(), vars.getOrg(), request.getParameter("yearId"));
        jsob = new JSONObject();
        if (jsonArray.length() > 0)        	
            jsob.put("data", jsonArray);
          else
            jsob.put("data", "");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } 
    } catch (final Exception e) {
      log4j.error("Exception in BudgetProjectFundsAvailableAjax : ", e);
    }
  }
}