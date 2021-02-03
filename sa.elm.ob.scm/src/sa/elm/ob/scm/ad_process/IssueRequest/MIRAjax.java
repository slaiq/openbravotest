package sa.elm.ob.scm.ad_process.IssueRequest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.ad_process.printreport.PrintReportDAO;

public class MIRAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    PrintReportDAO printDao = new PrintReportDAO();
    String mirId = request.getParameter("recordId");
    MaterialIssueRequest mir = printDao.getMIR(mirId);
    String username = "";
    String printedDate = "";

    if (mir.getPrintedby() != null) {
      username = printDao.getUserName(mir.getPrintedby());
    }
    if (printDao.getPrintedDate(mirId) != null) {
      printedDate = printDao.getPrintedDate(mirId);
    }

    JSONObject data = new JSONObject();
    try {
      data.put("username", username);
      data.put("printedDate", printedDate);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(data.toString());
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

  }

  public String getServletInfo() {
    return "MIRAjax Servlet";
  }
}