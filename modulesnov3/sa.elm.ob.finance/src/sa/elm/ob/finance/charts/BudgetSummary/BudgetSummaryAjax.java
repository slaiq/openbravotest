package sa.elm.ob.finance.charts.BudgetSummary;

import java.io.IOException;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.myob.WidgetInstance;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.marketing.Campaign;

import sa.elm.ob.finance.EFINBudgetLines;

/**
 * @author Gopalakrishnan on 20/09/2016
 */
public class BudgetSummaryAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    VariablesSecureApp vars = null;
    BudgetSummaryDAO dao = null;
    String action = "", orgId = "", budgetTypeId = "", elementId = "", accountId = "", yearId = "", projectId = "", preferences = "";
    JSONObject jsonResponse = null;
    JSONArray jsonArray = null;
    if (log4j.isDebugEnabled())
      log4j.debug(" BudgetSummaryAjax ");
    try {

      dao = new BudgetSummaryDAO(getConnection());
      vars = new VariablesSecureApp(request);
      preferences = vars.getStringParameter("preferences");
      action = vars.getStringParameter("action");
      orgId = vars.getStringParameter("orgId");
      budgetTypeId = vars.getStringParameter("budgetTypeId");
      elementId = vars.getStringParameter("elementId");
      accountId = vars.getStringParameter("accountId");
      projectId = vars.getStringParameter("projectId");
      yearId = vars.getStringParameter("yearId");
      if (log4j.isDebugEnabled())
        log4j.debug("Action: " + action + " budgetTypeId:" + budgetTypeId + "OrgId: " + orgId
            + "elementId: " + elementId + "accountId: " + accountId + "projectId: " + projectId
            + "yearId:" + yearId);

      if (action.equals("getBudgetDetails")) {
        List<EFINBudgetLines> budgetLines = null;
        // budgetLines = dao.getBudgetDetails(vars.getClient(), orgId, yearId, budgetTypeId,
        // elementId, accountId, projectId);
        response.setCharacterEncoding("UTF-8");
        budgetLines = dao.getBudgetDetailsForAll(vars.getClient(), orgId, yearId, budgetTypeId);
        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        StringBuffer data = new StringBuffer();
        data.append("<?xml version=\"1.0\"?>\n");
        data.append("<BudgetDetails>");
        if (budgetLines.size() > 0) {
          for (EFINBudgetLines budget : budgetLines) {

            data.append("<Budget>");
            data.append("<code>" + budget.getUniquecode() + "</code>");
            // data.append("<element>" + budget.getAccountElement().getValue() + "</element>");
            data.append("<accountName>" + budget.getAccountElement().getIdentifier()
                + "</accountName>");
            data.append("<amount>" + budget.getAmount() + "</amount>");
            data.append("<currentAmount>" + budget.getCurrentBudget() + "</currentAmount>");
            data.append("<encAmount>" + budget.getEncumbrance() + "</encAmount>");
            data.append("<amountSpent>" + budget.getAmountSpent() + "</amountSpent>");
            data.append("<available>" + budget.getFundsAvailable() + "</available>");
            data.append("</Budget>");
          }
        }
        data.append("</BudgetDetails>");
        response.getWriter().write(data.toString());

        if (log4j.isDebugEnabled())
          log4j.debug("XML Response : " + data.toString());
      } else if (action.equals("getOrganization")) {
        List<Organization> orgList = null;
        orgList = dao.getOrganization(vars.getClient());
        jsonArray = new JSONArray();
        if (orgList != null && orgList.size() > 0) {
          for (Organization org : orgList) {
            jsonResponse = new JSONObject();
            jsonResponse.put("OrgId", org.getId());
            jsonResponse.put("OrgName", org.getName());
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getYear")) {
        List<Year> yearList = null;
        yearList = dao.getYear(vars.getClient());
        jsonArray = new JSONArray();
        if (yearList != null && yearList.size() > 0) {
          for (Year year : yearList) {
            jsonResponse = new JSONObject();
            jsonResponse.put("YearId", year.getId());
            jsonResponse.put("YearName", year.getFiscalYear());
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getBudgetType")) {
        List<Campaign> bTypeList = null;
        bTypeList = dao.getBudgetType(vars.getClient());
        jsonArray = new JSONArray();
        if (bTypeList != null && bTypeList.size() > 0) {
          for (Campaign bType : bTypeList) {
            jsonResponse = new JSONObject();
            jsonResponse.put("bTypeId", bType.getId());
            jsonResponse.put("bTypeName", bType.getName());
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getAccountElement")) {
        jsonArray = dao.getAccountElement(budgetTypeId);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getAccounts")) {
        jsonArray = dao.getAccounts(elementId);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getProjects")) {
        jsonArray = dao.getProjects(accountId, orgId, vars.getClient());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("getPreferences")) {

        List<WidgetInstance> wInstanceList = null;
        wInstanceList = dao.getPreferences(OBContext.getOBContext().getUser().getId(), OBContext
            .getOBContext().getRole().getId());
        jsonArray = new JSONArray();
        if (wInstanceList != null && wInstanceList.size() > 0) {
          for (WidgetInstance wList : wInstanceList) {
            jsonResponse = new JSONObject();
            String[] splitArray = null;
            try {
              splitArray = wList.getEutPreferences().split(",");
            } catch (PatternSyntaxException ex) {
              //
            }
            jsonResponse.put("org", splitArray[0]);
            jsonResponse.put("year", splitArray[1]);
            jsonResponse.put("btype", splitArray[2]);
            jsonArray.put(jsonResponse);
          }
        }

        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());

      } else if (action.equals("saveDefault")) {
        dao = new BudgetSummaryDAO(getConnection());
        dao.saveDefaultPreferences(OBContext.getOBContext().getUser().getId(), OBContext
            .getOBContext().getRole().getId(), preferences);
      }
    } catch (Exception e) {
      log4j.error("Exception in BudgetSummary ajax", e);
    }
  }

}