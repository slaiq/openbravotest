package sa.elm.ob.finance.ad_forms.clearbankcheque.header;

import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;

import sa.elm.ob.finance.ad_forms.clearbankcheque.dao.ClearChequeDao;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gowtham
 */
public class ClearBankCheque extends HttpSecureAppServlet {

  /**
   * Servlet implementation class to Clearing Bank Cheque Details
   */
  private static final long serialVersionUID = 1L;
  private static final String includeIn = "../web/sa.elm.ob.finance/jsp/clearbankcheque/ClearBankCheque.jsp";

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    Connection con = null;
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      con = getConnection();
    } catch (NoConnectionAvailableException e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    String action = (request.getParameter("inpAction") == null ? ""
        : request.getParameter("inpAction"));
    RequestDispatcher dispatch = null;

    try {
      OBContext.setAdminMode();
      if (action.equals("")) {
        ClearChequeDao dao = new ClearChequeDao(con);
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(new Date());
        date = dateYearFormat.format(df.parse(date));
        date = UtilityDAO.convertTohijriDate(date);
        request.setAttribute("today", date);
        // getOrg
        JSONObject orgJson = dao.getOrganization(vars.getClient(), "", 0, 0),
            firstOrg = new JSONObject();
        JSONArray jsonOrgArray = orgJson.getJSONArray("data");
        if (jsonOrgArray.length() > 0) {
          firstOrg = jsonOrgArray.getJSONObject(0);
        }
        // get bank
        JSONObject bankJson = dao.getBank(vars.getClient(), "", 0, 0), firstBank = new JSONObject();
        JSONArray jsonBankArray = bankJson.getJSONArray("data");
        if (jsonBankArray.length() > 0) {
          firstBank = jsonBankArray.getJSONObject(0);
        }
        // get chequeStatus
        JSONObject cheStatus = dao.getChequeStatus(vars.getClient(), "", 0, 0);
        request.setAttribute("chqStatus", cheStatus);

        request.setAttribute("bank", firstBank.toString());
        request.setAttribute("org", firstOrg.toString());

        dispatch = request.getRequestDispatcher(includeIn);
      }

    } catch (final Exception e) {
      dispatch = request.getRequestDispatcher("../web/jsp/ErrorPage.jsp");
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
      try {
        if (dispatch != null) {
          response.setContentType("text/html; charset=UTF-8");
          response.setCharacterEncoding("UTF-8");
          dispatch.include(request, response);
        } else
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } catch (final Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }
}
