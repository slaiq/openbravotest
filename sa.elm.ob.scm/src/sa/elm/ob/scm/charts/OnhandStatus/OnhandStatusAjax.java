package sa.elm.ob.scm.charts.OnhandStatus;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class OnhandStatusAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    VariablesSecureApp vars = null;
    OnhandStatusDao dao = null;

    String action = "";
    if (log4j.isDebugEnabled())
      log4j.debug(" OnhandStatusAjax ");
    try {
      dao = new OnhandStatusDao(getConnection());
      vars = new VariablesSecureApp(request);
      String clientId = vars.getClient();
      String userId = vars.getUser();
      String roleId = vars.getRole();
      action = vars.getStringParameter("action");

      if (action.equals("getOnhandStatus")) {
        List<Object> rows = dao.getOnhandStatus(clientId, userId, roleId);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(rows);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
      }
    } catch (Exception e) {
      log4j.error("Exception in OnhandStatusAjax", e);
    }
  }
}