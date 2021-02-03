package sa.elm.ob.scm.charts.OnhandStatus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;

public class OnhandStatus extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String includeIn = "../web/sa.elm.ob.scm/jsp/charts/OnhandStatus/OnhandStatus.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.getRequestDispatcher(includeIn).forward(request, response);
  }

}