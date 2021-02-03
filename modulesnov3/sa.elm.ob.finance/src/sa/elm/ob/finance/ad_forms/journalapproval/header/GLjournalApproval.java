package sa.elm.ob.finance.ad_forms.journalapproval.header;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

//import com.qualiantech.paymentoutenhancements.properties.PaymentOutDocumentRule;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;

public class GLjournalApproval extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String formGlJournalApproval = "7FD666950C7542BE985F3237A62EA34A";

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    RequestDispatcher dispatch = null;
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      if (Utility.haveAccesstoWindow(vars.getClient(), vars.getOrg(), vars.getRole(),
          vars.getUser(), Resource.GLJOURNAL_RULE)) {
        dispatch = request.getRequestDispatcher(
            "../web/sa.elm.ob.finance/jsp/journalapproval/GLJournalApproval.jsp");
      } else
        dispatch = request.getRequestDispatcher("../web/jsp/Restrict.jsp");
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Exception in GLJournalApproval: ", e);
    } finally {
      try {
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        }
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Exception in GLJournalApproval : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }
}