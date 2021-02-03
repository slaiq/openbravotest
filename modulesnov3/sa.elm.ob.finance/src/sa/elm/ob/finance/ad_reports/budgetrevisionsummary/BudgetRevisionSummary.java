package sa.elm.ob.finance.ad_reports.budgetrevisionsummary;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.enterprise.OrganizationInformation;

import sa.elm.ob.finance.util.DAO.CommonReportDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Kousalya on 01/02/2018
 * 
 */
public class BudgetRevisionSummary extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/budgetrevisionsummary/BudgetRevisionSummary.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      OBContext.setAdminMode();
      String strReportName = "";
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      if (action.equals("")) {
        request.setAttribute("inpBudgetYears",
            CommonReportDAO.getBudgetYear(vars.getClient(), vars.getOrg()));
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpBudgetYear = request.getParameter("inpBudgetYear");
        String inpBudgetRev = request.getParameter("inpBudgetRev");
        String inpBudgetRevNo = "", inpBudgetRevTranxTyp = "";
        JSONObject jsob = BudgetRevisionSummaryDAO.getBudgetRevisions(vars.getClient(),
            vars.getOrg(), inpBudgetYear, inpBudgetRev, "");
        JSONArray jsonArray = (JSONArray) jsob.get("data");
        if (jsonArray.length() > 0) {
          for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject objects = jsonArray.getJSONObject(0);
            inpBudgetRevTranxTyp = (String) objects.get("tranxType");
            inpBudgetRevNo = (String) objects.get("recordIdentifier");
          }
        }
        System.out.println("inpBudgetYear " + inpBudgetYear + " inpBudgetRev " + inpBudgetRev);
        String imageFlag = "N";
        OrganizationInformation objInfo = Utility.getOrgInfo(vars.getOrg());

        if (objInfo != null) {
          if (objInfo.getYourCompanyDocumentImage() != null) {
            imageFlag = "Y";
          }
        }
        inpBudgetRevTranxTyp = inpBudgetRevTranxTyp.equals("REV") ? "Revision"
            : inpBudgetRevTranxTyp.equals("TRS") ? "Transfer" : "Original Budget Adjustment";

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpBudgetYear", inpBudgetYear);
        parameters.put("inpBudgetRev", inpBudgetRev);
        parameters.put("inpBudgetRevNo", inpBudgetRevNo);
        parameters.put("inpBudgetRevTranxTyp", inpBudgetRevTranxTyp);
        parameters.put("inpImageFlag", imageFlag);
        parameters.put("inpOrgId", vars.getOrg());

        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/budgetrevisionsummary/BudgetRevisionSummary.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in Budget Revision Summary :", e);
    }
  }
}