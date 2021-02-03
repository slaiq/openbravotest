package sa.elm.ob.finance.ad_forms.poapproval.header;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;

public class PaymentOutApproval extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String formPaymentOutApproval = "C68768E71E8C477F90D3C479B7869959";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    RequestDispatcher dispatch = null;
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      if (Utility.haveAccesstoWindow(vars.getClient(), vars.getOrg(), vars.getRole(),
          vars.getUser(), Resource.PAYMENT_OUT_RULE)) {
        dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.finance/jsp/poapproval/POApproval.jsp");
      } else
        dispatch = request.getRequestDispatcher("../web/jsp/Restrict.jsp");

    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Exception in POApproval : ", e);
    } finally {
      try {
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        }
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Exception in POApproval : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }
}