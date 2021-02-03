package sa.elm.ob.finance.ad_process;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.data.FieldProvider;

/**
 * 
 * @author Gopalakrishnan
 * 
 */
public class FundDeptEnquiry extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/FundDeptEnquiry/FundDeptEnquiry.jsp";

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      OBContext.setAdminMode();
      String strReportName = "";
      String strFileName = "";
      VariablesSecureApp vars = new VariablesSecureApp(request);
      FieldProvider[] data = null;

      String action = (request.getParameter("inpAction") == null ? ""
          : request.getParameter("inpAction"));
      // VariablesSecureApp vars = new VariablesSecureApp(request);

      log4j.debug("action");
      if (action.equals("")) {
        // Localization support
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);

      } else if (action.equals("generateReport")) {

        String inpLoanPaid = request.getParameter("inpLoanPaid");
        String inpCitizenNameHide = request.getParameter("inpCitizenNameHide");
        String inpCitizenIdHide = request.getParameter("inpCitizenIdHide");
        String inpBankTypeHide = request.getParameter("inpBankTypeHide");
        String inpErrorNameHide = request.getParameter("inpErrorNameHide");

        // Department Name && childDept

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpLoanPaid", inpLoanPaid);
        parameters.put("inpCitizenName", inpCitizenNameHide);
        parameters.put("inpCitizenId", inpCitizenIdHide);
        parameters.put("inpErrorName", inpErrorNameHide);
        if (inpBankTypeHide.equals("agri")) {
          strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/FundDeptEnquiry/AgricultureDevFund.jrxml";
          strFileName = "AgriDevelopmentFund";
        } else if (inpBankTypeHide.equals("rst")) {
          strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/FundDeptEnquiry/RealEstDevFund.jrxml";
          strFileName = "RealEstateFund";
        }
        String strOutput = "pdf";
        renderJR(vars, response, strReportName, strFileName, strOutput, parameters, data, null);

      }

    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception in FundDeptEnquiry :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}