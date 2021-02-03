package sa.elm.ob.scm.charts.WarehouseActivities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

public class WarehouseActivitiesAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    VariablesSecureApp vars = null;
    WarehouseActivitiesDao dao = null;

    String action = "";
    if (log4j.isDebugEnabled())
      log4j.debug(" WarehouseActivitiesAjax ");
    try {
      dao = new WarehouseActivitiesDao(getConnection());
      vars = new VariablesSecureApp(request);
      String clientId = vars.getClient();
      String userId = vars.getUser();
      String roleId = vars.getRole();
      action = vars.getStringParameter("action");

      if (action.equals("getWarehouseActivities")) {
        List<Integer> monthsThisYear = new ArrayList<Integer>();
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-01-1438",
            "30-01-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-02-1438",
            "29-02-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-03-1438",
            "30-03-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-04-1438",
            "30-04-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-05-1438",
            "30-05-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-06-1438",
            "29-06-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-07-1438",
            "29-07-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-08-1438",
            "30-08-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-09-1438",
            "29-09-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-10-1438",
            "29-10-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-11-1438",
            "30-11-1438"));
        monthsThisYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-12-1438",
            "29-12-1438"));
        List<Integer> monthsLastYear = new ArrayList<Integer>();
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-01-1437",
            "30-01-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-02-1437",
            "29-02-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-03-1437",
            "30-03-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-04-1437",
            "30-04-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-05-1437",
            "29-05-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-06-1437",
            "29-06-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-07-1437",
            "30-07-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-08-1437",
            "29-08-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-09-1437",
            "30-09-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-10-1437",
            "29-10-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-11-1437",
            "29-11-1437"));
        monthsLastYear.add(dao.getMaterialIssueCount(clientId, userId, roleId, "01-12-1437",
            "30-12-1437"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("thisYear", monthsThisYear);
        jsonObject.put("lastYear", monthsLastYear);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonObject.toString());
      } else if (action.equals("getReturnTransaction")) {
        List<Integer> monthsThisYear = new ArrayList<Integer>();
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-01-1438", "30-01-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-02-1438", "29-02-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-03-1438", "30-03-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-04-1438", "30-04-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-05-1438", "30-05-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-06-1438", "29-06-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-07-1438", "29-07-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-08-1438", "30-08-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-09-1438", "29-09-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-10-1438", "29-10-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-11-1438", "30-11-1438"));
        monthsThisYear.add(dao.getReturnTransactionCount(clientId, "01-12-1438", "29-12-1438"));
        List<Integer> monthsLastYear = new ArrayList<Integer>();
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-01-1437", "30-01-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-02-1437", "29-02-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-03-1437", "30-03-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-04-1437", "30-04-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-05-1437", "29-05-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-06-1437", "29-06-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-07-1437", "30-07-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-08-1437", "29-08-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-09-1437", "30-09-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-10-1437", "29-10-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-11-1437", "29-11-1437"));
        monthsLastYear.add(dao.getReturnTransactionCount(clientId, "01-12-1437", "30-12-1437"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("thisYear", monthsThisYear);
        jsonObject.put("lastYear", monthsLastYear);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonObject.toString());

      } else if (action.equals("getIssueReturnTransaction")) {
        List<Integer> monthsThisYear = new ArrayList<Integer>();
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-01-1438", "30-01-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-02-1438", "29-02-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-03-1438", "30-03-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-04-1438", "30-04-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-05-1438", "30-05-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-06-1438", "29-06-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-07-1438", "29-07-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-08-1438", "30-08-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-09-1438", "29-09-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-10-1438", "29-10-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-11-1438", "30-11-1438"));
        monthsThisYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-12-1438", "29-12-1438"));
        List<Integer> monthsLastYear = new ArrayList<Integer>();
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-01-1437", "30-01-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-02-1437", "29-02-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-03-1437", "30-03-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-04-1437", "30-04-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-05-1437", "29-05-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-06-1437", "29-06-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-07-1437", "30-07-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-08-1437", "29-08-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-09-1437", "30-09-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-10-1437", "29-10-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-11-1437", "29-11-1437"));
        monthsLastYear
            .add(dao.getIssueReturnTransactionCount(clientId, "01-12-1437", "30-12-1437"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("thisYear", monthsThisYear);
        jsonObject.put("lastYear", monthsLastYear);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonObject.toString());

      } else if (action.equals("getPOReceipt")) {
        List<Integer> monthsThisYear = new ArrayList<Integer>();
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-01-1438", "30-01-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-02-1438", "29-02-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-03-1438", "30-03-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-04-1438", "30-04-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-05-1438", "30-05-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-06-1438", "29-06-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-07-1438", "29-07-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-08-1438", "30-08-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-09-1438", "29-09-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-10-1438", "29-10-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-11-1438", "30-11-1438"));
        monthsThisYear.add(dao.getPOReceiptCount(clientId, "01-12-1438", "29-12-1438"));
        List<Integer> monthsLastYear = new ArrayList<Integer>();
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-01-1437", "30-01-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-02-1437", "29-02-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-03-1437", "30-03-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-04-1437", "30-04-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-05-1437", "29-05-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-06-1437", "29-06-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-07-1437", "30-07-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-08-1437", "29-08-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-09-1437", "30-09-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-10-1437", "29-10-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-11-1437", "29-11-1437"));
        monthsLastYear.add(dao.getPOReceiptCount(clientId, "01-12-1437", "30-12-1437"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("thisYear", monthsThisYear);
        jsonObject.put("lastYear", monthsLastYear);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonObject.toString());
      }

    } catch (Exception e) {
      log4j.error("Exception in WarehouseActivitiesAjax", e);
    }
  }
}