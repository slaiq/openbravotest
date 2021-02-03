package sa.elm.ob.utility.ad_forms.ApprovalRevoke.header;

import java.io.IOException;
import java.sql.Connection;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.GetFactoryMassRevoke;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ProcessMassRevoke;

/**
 * 
 * @author gopalakrishnan on 04/05/2017
 */

public class ApprovalRevoke extends HttpSecureAppServlet {

  /**
   * Employment form details
   */
  private static final long serialVersionUID = 1L;

  @Inject
  @Any
  private Instance<ProcessMassRevoke> hooks;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    MassRevoke dao = null;
    Connection con = null;
    RequestDispatcher dispatch = null;
    VariablesSecureApp vars = null;

    try {
      OBContext.setAdminMode();
      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      String inpWindowId = (request.getParameter("inpWindowId") == null ? "MIR"
          : request.getParameter("inpWindowId"));

      con = getConnection();
      vars = new VariablesSecureApp(request);

      if (action.equals("")) {
        request.setAttribute("inpWindowId", inpWindowId);
        request.setAttribute("SaveStatus", "");
        dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.utility/jsp/ApprovalRevoke/ApprovalRevoke.jsp");
      } else if (action.equals("Revoke")) {
        GetFactoryMassRevoke massRevoke = new GetFactoryMassRevoke(hooks);

        dao = massRevoke.getRevoke(inpWindowId, con);
        String selectIds = request.getParameter("selectIds");
        inpWindowId = request.getParameter("inpWindowId");
        request.setAttribute("inpWindowId", inpWindowId);
        String processedIds = dao.validateRecord(selectIds, inpWindowId);
        if (processedIds != null) {
          request.setAttribute("SaveStatus", "AlreadyProcessed");
          request.setAttribute("DocumentNo", processedIds);
        } else {
          String success = "";
          success = dao.updateRecord(vars, selectIds, inpWindowId);
          request.setAttribute("SaveStatus", success);
        }

        dispatch = request
            .getRequestDispatcher("../web/sa.elm.ob.utility/jsp/ApprovalRevoke/ApprovalRevoke.jsp");
      }
    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      log4j.error("Error in ApprovalRevoke : ", e);
    } finally {
      OBContext.restorePreviousMode();
      try {
        if(con != null) {
          con.close();
        }
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        } else
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log4j.error("Error in ApprovalRevoke : ", e);
      }
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doPost(request, response);
  }
}
