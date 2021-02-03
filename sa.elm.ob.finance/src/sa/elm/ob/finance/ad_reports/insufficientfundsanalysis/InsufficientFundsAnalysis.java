package sa.elm.ob.finance.ad_reports.insufficientfundsanalysis;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

import sa.elm.ob.finance.util.DAO.CommonReportDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Kousalya on 03/02/2018
 * 
 */
public class InsufficientFundsAnalysis extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/fundsavailableanalysis/FundsAvailableAnalysis.jsp";

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
        request.setAttribute("inpBudgetAccounts", CommonReportDAO
            .getBudgetAccounts(vars.getClient(), vars.getRole(), vars.getOrg(), null));

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("Submit")) {
        String inpBudgetYear = request.getParameter("inpBudgetYear");
        String inpBudgetRev = request.getParameter("inpBudgetRev");
        String inpView = request.getParameter("inpView");
        String inpBudgetRevNo = "", inpBudgetRevTranxTyp = "";
        String inpFromAccount = request.getParameter("inpFromAccount"); // account 32 bit id
        ElementValue objFromElement = OBDal.getInstance().get(ElementValue.class, inpFromAccount); // single
                                                                                                   // relation
                                                                                                   // object
                                                                                                   // row

        String fromActValue = objFromElement.getSearchKey(); // getting value

        String inpToAccount = request.getParameter("inpToAccount");
        ElementValue objToElement = OBDal.getInstance().get(ElementValue.class, inpToAccount); // single
                                                                                               // relation
                                                                                               // object
                                                                                               // row
        String toActValue = objToElement.getSearchKey(); // getting value

        System.out.println("toActValue : " + toActValue + " inpFromAccount  " + inpFromAccount
            + " objFromElement " + objFromElement.getSearchKey());
        System.out.println("fromActValue : " + fromActValue);
        System.out.println("fromActValue : " + fromActValue + " inpview " + inpView
            + " inpBudgetYear " + inpBudgetYear);

        /*
         * JSONObject jsob=BudgetProjectSummaryDAO.getBudgetRevisions(vars.getClient(),
         * vars.getOrg(), inpBudgetYear, inpBudgetRev); JSONArray jsonArray = (JSONArray)
         * jsob.get("data"); if(jsonArray.length() > 0){ for (int i = 0; i < jsonArray.length();
         * i++) { JSONObject objects = jsonArray.getJSONObject(0); inpBudgetRevTranxTyp = (String)
         * objects.get("tranxType"); inpBudgetRevNo = (String) objects.get("recordIdentifier"); } }
         */
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
        parameters.put("inpFromAccount", Integer.valueOf(fromActValue.toString())); // passing to
                                                                                    // your report
        parameters.put("inpView",
            inpView.equals("mof")
                ? " and org.ad_org_id  in (select a.ad_org_id from ehcm_hrorg_classfication a join ehcm_org_classfication b on b.ehcm_org_classfication_id=a.ehcm_org_classfication_id where b.classification='FIN' and enabled='Y' and a.isactive='Y'"
                    + "and a.ad_client_id='" + vars.getClient()
                    + "') and org.ad_orgtype_id='1'  and org.ad_client_id='" + vars.getClient()
                    + "' and bicp.c_salesregion_id in (select hq_budgetcontrolunit from efin_budget_ctrl_param where ad_client_id='"
                    + vars.getClient() + "')"
                : "and org.ad_org_id  in (select a.ad_org_id from ehcm_hrorg_classfication a join ehcm_org_classfication b on b.ehcm_org_classfication_id=a.ehcm_org_classfication_id where b.classification='FIN' and enabled='Y' and a.isactive='Y'"
                    + "and a.ad_client_id='" + vars.getClient() + "') and org.ad_client_id='"
                    + vars.getClient()
                    + "' and bicp.c_salesregion_id in (select budgetcontrol_costcenter from efin_budget_ctrl_param where ad_client_id='"
                    + vars.getClient() + "')");
        parameters.put("inpToAccount", Integer.valueOf(toActValue.toString()));
        parameters.put("inpImageFlag", imageFlag);
        parameters.put("inpOrgId", vars.getOrg());

        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/fundsavailableanalysis/MoTFundsAvailableAnalysis.jrxml";
        String strOutput = "pdf";

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in Budget Revision Summary :", e);
    }
  }
}